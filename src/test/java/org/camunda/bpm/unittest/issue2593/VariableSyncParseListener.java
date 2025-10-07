package org.camunda.bpm.unittest.issue2593;

import java.util.List;

import org.camunda.bpm.engine.impl.bpmn.behavior.ConditionalEventBehavior;
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.xml.Element;

/**
 * Stores in the process definition if the process has conditional event.
 * That way, we can restrict the enforced synchronization to only
 * processes that need it.
 */
public class VariableSyncParseListener extends AbstractBpmnParseListener {

  public static final String HAS_CONDITIONAL_EVENTS = "hasConditionalEvents";

  @Override
  public void parseProcess(Element processElement, ProcessDefinitionEntity processDefinition) {
    boolean hasConditionalEvents = hasConditionalEvents(processDefinition);

    processDefinition.setProperty(HAS_CONDITIONAL_EVENTS, hasConditionalEvents);
  }

  private boolean hasConditionalEvents(ScopeImpl scope) {
    List<ActivityImpl> activities = scope.getActivities();

    for (ActivityImpl activityImpl : activities) {
      if (activityImpl.getActivityBehavior() instanceof ConditionalEventBehavior || hasConditionalEvents(activityImpl)) {
        return true;
      }
    }

    return false;
  }
}
