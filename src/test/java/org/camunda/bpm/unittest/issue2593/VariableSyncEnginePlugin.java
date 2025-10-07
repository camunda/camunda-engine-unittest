package org.camunda.bpm.unittest.issue2593;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEnginePlugin;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.impl.history.event.HistoryEventType;
import org.camunda.bpm.engine.impl.history.producer.CacheAwareHistoryEventProducer;
import org.camunda.bpm.engine.impl.interceptor.Session;
import org.camunda.bpm.engine.impl.interceptor.SessionFactory;
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.EventSubscriptionManager;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.VariableInstanceEntity;

public class VariableSyncEnginePlugin implements ProcessEnginePlugin {

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    wrapHistoryProducer(processEngineConfiguration);

    List<BpmnParseListener> parseListeners = new ArrayList<>();
    parseListeners.add(new VariableSyncParseListener());

    processEngineConfiguration.setCustomPostBPMNParseListeners(parseListeners);
  }

  /**
   * Wraps the history event producer so that we synchronize on the process instance
   * whenever a variable value is set
   */
  private void wrapHistoryProducer(ProcessEngineConfigurationImpl processEngineConfiguration) {
    processEngineConfiguration.setHistoryEventProducer(new CacheAwareHistoryEventProducer() {
      @Override
      protected HistoryEvent createHistoricVariableEvent(VariableInstanceEntity variableInstance, VariableScope sourceVariableScope,
          HistoryEventType eventType) {
        ExecutionEntity execution = variableInstance.getExecution();
        ManagementService managementService = processEngineConfiguration.getManagementService();
        forceUpdateProcessInstance(execution, managementService);

        return super.createHistoricVariableEvent(variableInstance, sourceVariableScope, eventType);
      }

    });
  }

  /**
   * Enforces an UPDATE statement on the process instance execution
   * and this way enforces OptimisticLockingException in case any concurrent modification happens.
   * @param managementService
   */
  private void forceUpdateProcessInstance(ExecutionEntity execution, ManagementService managementService) {
    if (execution == null) {
      // cmmn and task variables
      return;
    }

    ProcessDefinitionEntity processDefinition = execution.getProcessDefinition();
    Boolean hasConditionalEvents = (Boolean) processDefinition.getProperty(VariableSyncParseListener.HAS_CONDITIONAL_EVENTS);

    if (hasConditionalEvents == true) {
      System.out.println("Triggering force update");
      execution.getProcessInstance().forceUpdate();
    }
  }

  @Override
  public void postInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    wrapEventSubscriptionManagerSessionFactory(processEngineConfiguration);
  }

  /**
   * Wraps the event subscription manager factory so that we synchronize on the process instance
   * whenever a conditional event subscription is created (i.e. an execution starts listening to a conditional
   * event)
   */
  private void wrapEventSubscriptionManagerSessionFactory(ProcessEngineConfigurationImpl processEngineConfiguration) {
    Map<Class<?>, SessionFactory> sessionFactories = processEngineConfiguration.getSessionFactories();
    sessionFactories.put(EventSubscriptionManager.class, new SessionFactory() {

      @Override
      public Class<?> getSessionType() {
        return EventSubscriptionManager.class;
      }

      @Override
      public Session openSession() {
        return new EventSubscriptionManager() {
          @Override
          public void insert(EventSubscriptionEntity subscription) {

            if (EventType.CONDITONAL.name().equals(subscription.getEventType())) {
              ExecutionEntity executionEntity = subscription.getExecution();
              forceUpdateProcessInstance(executionEntity, processEngineConfiguration.getManagementService());
            }

            super.insert(subscription);
          }
        };
      }
    });
  }

  @Override
  public void postProcessEngineBuild(ProcessEngine processEngine) {
  }

}
