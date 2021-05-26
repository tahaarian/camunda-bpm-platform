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
package org.camunda.bpm.cockpit.impl.plugin.base.sub.resources;

import static org.camunda.bpm.engine.authorization.Permissions.READ;
import static org.camunda.bpm.engine.authorization.Permissions.READ_INSTANCE;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.camunda.bpm.cockpit.impl.plugin.base.dto.ProcessDefinitionDto;
import org.camunda.bpm.cockpit.impl.plugin.base.dto.query.ProcessDefinitionQueryDto;
import org.camunda.bpm.cockpit.plugin.resource.AbstractPluginResource;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.impl.ProcessEngineImpl;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.history.HistoryLevel;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.rest.exception.InvalidRequestException;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.CallActivity;

public class ProcessDefinitionResource extends AbstractPluginResource {

  protected String id;
  protected ProcessEngine engine;

  public ProcessDefinitionResource(String engineName, String id) {
    super(engineName);
    this.engine = getProcessEngine();
    this.id = id;
  }

  @GET
  @Path("/called-process-definitions")
  @Produces(MediaType.APPLICATION_JSON)
  public List<ProcessDefinitionDto> getCalledProcessDefinitions(@Context UriInfo uriInfo) {
    ProcessDefinitionQueryDto queryParameter = new ProcessDefinitionQueryDto(uriInfo.getQueryParameters());
    return queryCalledProcessDefinitions(queryParameter);
  }

  @GET
  @Path("/called-element-definitions")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, String> getCalledElementDefinitions() {
    RepositoryService repoService = engine.getRepositoryService();

    ProcessDefinition definition;
    try {
      definition = repoService.getProcessDefinition(id);
      BpmnModelInstance model = repoService.getBpmnModelInstance(definition.getId());
      Collection<CallActivity> callActivities = model.getModelElementsByType(CallActivity.class);
      Map<String, String> activityIDtoProcessIDMap = new HashMap<>();
      for (CallActivity activity : callActivities) {
        // not super optimal looped query here. also beware of binding and version, that's still todo
        List<ProcessDefinition> calledDefinitions = repoService.createProcessDefinitionQuery()
          .processDefinitionKey(activity.getCalledElement()).orderByVersionTag().asc().unlimitedList();
        for (ProcessDefinition processDefinition : calledDefinitions) {
          activityIDtoProcessIDMap.put(activity.getId(), processDefinition.getId());
        }
      }
      return activityIDtoProcessIDMap;
    } catch (ProcessEngineException e) {
      throw new InvalidRequestException(Response.Status.NOT_FOUND, e, "Ups " +e );
    }

  }

  @POST
  @Path("/called-process-definitions")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public List<ProcessDefinitionDto> queryCalledProcessDefinitions(ProcessDefinitionQueryDto queryParameter) {
    return getCommandExecutor().executeCommand(new QueryCalledProcessDefinitionsCmd(queryParameter));
  }

  private void injectEngineConfig(ProcessDefinitionQueryDto parameter) {

    ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) getProcessEngine()).getProcessEngineConfiguration();
    if (processEngineConfiguration.getHistoryLevel().equals(HistoryLevel.HISTORY_LEVEL_NONE)) {
      parameter.setHistoryEnabled(false);
    }

    parameter.initQueryVariableValues(processEngineConfiguration.getVariableSerializers(), processEngineConfiguration.getDatabaseType());
  }

  protected void configureExecutionQuery(ProcessDefinitionQueryDto query) {
    configureAuthorizationCheck(query);
    configureTenantCheck(query);
    addPermissionCheck(query, PROCESS_INSTANCE, "EXEC2.PROC_INST_ID_", READ);
    addPermissionCheck(query, PROCESS_DEFINITION, "PROCDEF.KEY_", READ_INSTANCE);
  }

  protected class QueryCalledProcessDefinitionsCmd implements Command<List<ProcessDefinitionDto>> {

    protected ProcessDefinitionQueryDto queryParameter;

    public QueryCalledProcessDefinitionsCmd(ProcessDefinitionQueryDto queryParameter) {
      this.queryParameter = queryParameter;
    }

    @Override
    public List<ProcessDefinitionDto> execute(CommandContext commandContext) {
      queryParameter.setParentProcessDefinitionId(id);
      injectEngineConfig(queryParameter);
      configureExecutionQuery(queryParameter);
      queryParameter.disableMaxResultsLimit();
      return getQueryService().executeQuery("selectCalledProcessDefinitions", queryParameter);
    }
  }
}
