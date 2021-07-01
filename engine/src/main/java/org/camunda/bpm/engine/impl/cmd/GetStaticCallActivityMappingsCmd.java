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
package org.camunda.bpm.engine.impl.cmd;

import org.camunda.bpm.engine.AuthorizationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.exception.NullValueException;
import org.camunda.bpm.engine.impl.bpmn.behavior.CallActivityBehavior;
import org.camunda.bpm.engine.impl.cfg.CommandChecker;
import org.camunda.bpm.engine.impl.core.model.CallableElement;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.deploy.cache.DeploymentCache;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.repository.CallActivityMappingImpl;
import org.camunda.bpm.engine.impl.util.CallableElementUtil;
import org.camunda.bpm.engine.repository.CallActivityMapping;
import org.camunda.bpm.engine.repository.ProcessDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetStaticCallActivityMappingsCmd implements Command<List<CallActivityMapping>> {

  protected String processDefinitionId;

  public GetStaticCallActivityMappingsCmd(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @Override
  public List<CallActivityMapping> execute(CommandContext commandContext) {
    ProcessDefinition definition = commandContext.getProcessEngineConfiguration().getRepositoryService()
      .getProcessDefinition(processDefinitionId);

    List<ActivityImpl> activities = commandContext.getProcessEngineConfiguration().getDeploymentCache()
      .findDeployedProcessDefinitionById(processDefinitionId).getActivities();

    List<ActivityImpl> callActivities = activities.stream()
      .filter(act -> act.getActivityBehavior() instanceof CallActivityBehavior).collect(Collectors.toList());
    // todo check for CaseCallActivityBehavior, alternatively we just need ActivityBehavior actually,
    //  or we just ignore them as instances are also not taken into account for cmmn
    String tenantId = definition.getTenantId();

    List<CallActivityMapping> mappings = new ArrayList<>();

    for (ActivityImpl activity : callActivities) {
      CallActivityBehavior behavior = (CallActivityBehavior) activity.getActivityBehavior();
      CallableElement callableElement = behavior.getCallableElement();

      try {
        ProcessDefinitionEntity calledProcessDef = CallableElementUtil
          .getStaticallyBoundProcessDefinition(callableElement, tenantId);
        if (calledProcessDef == null) {
          mappings.add(new CallActivityMappingImpl(activity.getActivityId(), null));
        } else {
          for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
            checker.checkReadProcessDefinition(calledProcessDef);
          }
          mappings.add(new CallActivityMappingImpl(activity.getActivityId(), calledProcessDef.getId()));
        }
      } catch (ProcessEngineException e) {
        mappings.add(new CallActivityMappingImpl(activity.getActivityId(), null));
      }
    }
    return mappings;
  }
}
