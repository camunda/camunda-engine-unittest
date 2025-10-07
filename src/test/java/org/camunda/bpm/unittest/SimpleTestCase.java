/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.unittest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.interceptor.CommandExecutor;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.junit5.ProcessEngineExtension;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


/**
 * @author Daniel Meyer
 * @author Martin Schimak
 */
public class SimpleTestCase {

  @RegisterExtension
  ProcessEngineExtension extension = ProcessEngineExtension.builder()
    .build();

  @Test
  @Deployment(resources = {"testProcess.bpmn","testProcess2.bpmn"})
  public void shouldExecuteProcess() throws InterruptedException {
    ProcessEngine processEngine = extension.getProcessEngine();
    TaskService taskService = processEngine.getTaskService();
    RuntimeService runtimeService = processEngine.getRuntimeService();
    ManagementService managementService = processEngine.getManagementService();

    // Given we create a new process instance
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess", "bussKey",
        Variables.createVariables().putValue("conditionVariable", false));
    ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("testProcess2");
    Task task = taskService.createTaskQuery().singleResult();

    Object monitorForTaskCompletion = new Object();
    AtomicBoolean isWaitingBeforeTaskCompletionCommit = new AtomicBoolean(false);

    Object monitorForSetVariable = new Object();
    AtomicBoolean isWaitingBeforeSetVariableCommit = new AtomicBoolean(false);

    ProcessEngineConfigurationImpl engineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
    CommandExecutor commandExecutor = engineConfiguration.getCommandExecutorTxRequired();

    Thread taskCompletionThread = new Thread(() -> {

      commandExecutor.execute(new Command<Void>() {

        @Override
        public Void execute(CommandContext commandContext) {
          taskService.complete(task.getId());

          synchronized (monitorForTaskCompletion) {
            try {
              isWaitingBeforeTaskCompletionCommit.set(true);
              monitorForTaskCompletion.wait();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
          return null;
        }
      });
    }, "task completion");

    Thread variableSetInAnotherProcessThread = new Thread(() -> {
      commandExecutor.execute(new Command<Void>() {

        @Override
        public Void execute(CommandContext commandContext) {
          String id = managementService.createJobQuery().singleResult().getId();
          managementService.executeJob(id);

          synchronized (monitorForSetVariable) {
            try {
              isWaitingBeforeSetVariableCommit.set(true);
              monitorForSetVariable.wait();
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }
          return null;
        }

      });
    }, "variable set in other process");

    AtomicReference<Throwable> conditionEvaluationException = new AtomicReference<>();
    taskCompletionThread.setUncaughtExceptionHandler((t, e) -> conditionEvaluationException.set(e));

    taskCompletionThread.start();
    variableSetInAnotherProcessThread.start();

    while (!isWaitingBeforeTaskCompletionCommit.get()) {
      synchronized (monitorForTaskCompletion) {
        // obtain monitor once to rule out race conditions
      }
    }

    while (!isWaitingBeforeSetVariableCommit.get()) {
      synchronized (monitorForSetVariable) {
        // obtain monitor once to rule out race conditions
      }
    }

    // both transactions are now waiting before commit

    synchronized (monitorForSetVariable) {
      monitorForSetVariable.notify();
    }

    // set variable fails with OptimisticLockingException on the process instance
    // and can be retried, e.g. via job executor in a real-world process
    variableSetInAnotherProcessThread.join();

    synchronized (monitorForTaskCompletion) {
      monitorForTaskCompletion.notify();
    }

    // task completion finishes successfully
    taskCompletionThread.join();


    long runningInstances = runtimeService.createProcessInstanceQuery().count();
    assertThat(runningInstances).isEqualTo(1L);

    assertThat(conditionEvaluationException.get()).isNotNull();
    assertThat(conditionEvaluationException.get()).isInstanceOf(OptimisticLockingException.class);
    System.out.println(conditionEvaluationException.toString());
  }


}
