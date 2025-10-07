package org.camunda.bpm.unittest;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class MyDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {

    RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();
    String id = runtimeService.createProcessInstanceQuery().processInstanceBusinessKey("bussKey").singleResult()
        .getId();
    runtimeService.setVariable(id, "conditionVariable", true);
    System.out.println("Variable set to true");
  }

}