/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.unittest;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author Daniel Meyer
 *
 */
public class SimpleTestCase {

  @Rule
  public ProcessEngineRule rule = new ProcessEngineRule();

  @Test
  @Deployment(resources = {"testProcess.bpmn"})
  public void shouldExecuteProcess() {

    RuntimeService runtimeService = rule.getRuntimeService();
    TaskService taskService = rule.getTaskService();

    ProcessInstance pi = runtimeService.startProcessInstanceByKey("testProcess");
    assertFalse("Process instance should not be ended", pi.isEnded());
    assertEquals(1, runtimeService.createProcessInstanceQuery().count());

    Task task = taskService.createTaskQuery().singleResult();
    assertNotNull("Task should exist", task);

    // complete the task
    taskService.complete(task.getId());

    // now the process instance should be ended
    assertEquals(0, runtimeService.createProcessInstanceQuery().count());

  }

}
