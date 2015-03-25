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
package org.camunda.bpm.engine.impl.persistence.entity;

import static org.camunda.bpm.engine.authorization.Permissions.CREATE;
import static org.camunda.bpm.engine.authorization.Permissions.CREATE_INSTANCES;
import static org.camunda.bpm.engine.authorization.Permissions.DELETE;
import static org.camunda.bpm.engine.authorization.Permissions.DELETE_INSTANCES;
import static org.camunda.bpm.engine.authorization.Permissions.READ;
import static org.camunda.bpm.engine.authorization.Permissions.READ_INSTANCES;
import static org.camunda.bpm.engine.authorization.Permissions.UPDATE;
import static org.camunda.bpm.engine.authorization.Permissions.UPDATE_INSTANCES;
import static org.camunda.bpm.engine.authorization.Resources.AUTHORIZATION;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.AuthorizationException;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.impl.AbstractQuery;
import org.camunda.bpm.engine.impl.AuthorizationQueryImpl;
import org.camunda.bpm.engine.impl.db.AuthorizationCheck;
import org.camunda.bpm.engine.impl.db.DbEntity;
import org.camunda.bpm.engine.impl.db.PermissionCheck;
import org.camunda.bpm.engine.impl.identity.Authentication;
import org.camunda.bpm.engine.impl.persistence.AbstractManager;

/**
 * @author Daniel Meyer
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AuthorizationManager extends AbstractManager {

  public static final String DEFAULT_AUTHORIZATION_CHECK = "defaultAuthorizationCheck";

  public Authorization createNewAuthorization(int type) {
    checkAuthorization(CREATE, AUTHORIZATION, null);
    return new AuthorizationEntity(type);
  }

  public void insert(DbEntity authorization) {
    checkAuthorization(CREATE, AUTHORIZATION, null);
    getDbEntityManager().insert(authorization);
  }

  public List<Authorization> selectAuthorizationByQueryCriteria(AuthorizationQueryImpl authorizationQuery) {
    configureQuery(authorizationQuery, AUTHORIZATION);
    return getDbEntityManager().selectList("selectAuthorizationByQueryCriteria", authorizationQuery);
  }

  public Long selectAuthorizationCountByQueryCriteria(AuthorizationQueryImpl authorizationQuery) {
    configureQuery(authorizationQuery, AUTHORIZATION);
    return (Long) getDbEntityManager().selectOne("selectAuthorizationCountByQueryCriteria", authorizationQuery);
  }

  public List<Authorization> selectAuthorizationsByResourceTypeAndResourceIdIncludingAny(Resource resource, String resourceId) {
    return selectAuthorizationsByResourceTypeAndResourceIdIncludingAny(resource.resourceType(), resourceId);
  }

  public List<Authorization> selectAuthorizationsByResourceTypeAndResourceIdIncludingAny(int resource, String resourceId) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("resource", resource);
    params.put("resourceId", resourceId);
    params.put("anyResourceId", Authorization.ANY);
    return getDbEntityManager().selectList("selectAuthorizationsByResourceTypeAndResourceIdIncludingAny", params);
  }

  public void update(AuthorizationEntity authorization) {
    checkAuthorization(UPDATE, AUTHORIZATION, authorization.getId());
    getDbEntityManager().merge(authorization);
  }

  public void delete(DbEntity authorization) {
    checkAuthorization(DELETE, AUTHORIZATION, authorization.getId());
    deleteAuthorizationsByResourceId(AUTHORIZATION, authorization.getId());
    super.delete(authorization);
  }

  // authorization checks ///////////////////////////////////////////

  public void checkAuthorization(List<PermissionCheck> parameters) {
    Authentication currentAuthentication = getCurrentAuthentication();

    if(isAuthorizationEnabled() && currentAuthentication != null) {

      boolean isAuthorized = isAuthorized(currentAuthentication.getUserId(), currentAuthentication.getGroupIds(), parameters);
      if (!isAuthorized) {
        throw new AuthorizationException(currentAuthentication.getUserId(), parameters);
      }
    }

  }

  public void checkAuthorization(Permission permission, Resource resource) {
    checkAuthorization(permission, resource, null);
  }

  public void checkAuthorization(Permission permission, Resource resource, String resourceId) {

    final Authentication currentAuthentication = getCurrentAuthentication();

    if(isAuthorizationEnabled() && currentAuthentication != null) {

      boolean isAuthorized = isAuthorized(currentAuthentication.getUserId(), currentAuthentication.getGroupIds(), permission, resource, resourceId);
      if (!isAuthorized) {
        throw new AuthorizationException(currentAuthentication.getUserId(), permission.getName(), resource.resourceName(), resourceId);
      }
    }

  }

  public boolean isAuthorized(Permission permission, Resource resource, String resourceId) {

    final Authentication currentAuthentication = getCurrentAuthentication();

    if(isAuthorizationEnabled() && currentAuthentication != null) {
      return isAuthorized(currentAuthentication.getUserId(), currentAuthentication.getGroupIds(), permission, resource, resourceId);

    } else {
      return true;

    }
  }

  public boolean isAuthorized(String userId, List<String> groupIds, Permission permission, Resource resource, String resourceId) {
    PermissionCheck param = new PermissionCheck();
    param.setAuthPerms(permission.getValue());
    param.setAuthResourceType(resource.resourceType());
    param.setAuthResourceId(resourceId);
    return isAuthorized(userId, groupIds, Arrays.asList(param));
  }

  public boolean isAuthorized(String userId, List<String> groupIds, List<PermissionCheck> parameters) {
    AuthorizationCheck authCheck = new AuthorizationCheck();
    authCheck.setAuthUserId(userId);
    authCheck.setAuthGroupIds(groupIds);
    authCheck.setAuthCheckParameters(parameters);
    return getDbEntityManager().selectBoolean("isUserAuthorizedForResource", authCheck);
  }

  // authorization checks on queries ////////////////////////////////

  public void configureQuery(AbstractQuery query) {
    final Authentication currentAuthentication = getCurrentAuthentication();

    query.getAuthCheckParameters().clear();

    if(isAuthorizationEnabled() && currentAuthentication != null) {

      query.setAuthorizationCheckEnabled(true);

      String currentUserId = currentAuthentication.getUserId();
      List<String> currentGroupIds = currentAuthentication.getGroupIds();

      query.setAuthUserId(currentUserId);
      query.setAuthGroupIds(currentGroupIds);
    }
    else {
      query.setAuthorizationCheckEnabled(false);
      query.setAuthUserId(null);
      query.setAuthGroupIds(null);
    }
  }

  public void configureQuery(AbstractQuery query, Resource resource) {
    configureQuery(query, resource, "RES.ID_");
  }

  public void configureQuery(AbstractQuery query, Resource resource, String queryParam) {
    configureQuery(query, resource, queryParam, Permissions.READ);
  }

  public void configureQuery(AbstractQuery query, Resource resource, String queryParam, Permission permission) {
    configureQuery(query);
    addAuthorizationCheckParameter(query, resource, queryParam, permission);
  }

  public void addAuthorizationCheckParameter(AbstractQuery query, Resource resource, String queryParam, Permission permission) {
    if (isAuthorizationEnabled() && getCurrentAuthentication() != null) {
      PermissionCheck parameter = new PermissionCheck();

      int resourceType = resource.resourceType();
      int permissionValue = permission.getValue();

      parameter.setAuthResourceType(resourceType);
      parameter.setAuthResourceIdQueryParam(queryParam);
      parameter.setAuthPerms(permissionValue);

      query.addAuthCheckParameter(parameter);
    }
  }

  // delete authorizations //////////////////////////////////////////////////

  public void deleteAuthorizationsByResourceId(Resource resource, String resourceId) {

    if(resourceId == null) {
      throw new IllegalArgumentException("Resource id cannot be null");
    }

    if(isAuthorizationEnabled()) {
      Map<String, Object> deleteParams = new HashMap<String, Object>();
      deleteParams.put("resourceType", resource.resourceType());
      deleteParams.put("resourceId", resourceId);
      getDbEntityManager().delete(AuthorizationEntity.class, "deleteAuthorizationsForResourceId", deleteParams);
    }

  }

  // predefined authorization checks

  /* PROCESS INSTANCE */

  // create permission ///////////////////////////////////////////////////

  public void checkCreateProcessInstance(ProcessDefinitionEntity definition) {
    checkAuthorization(CREATE, PROCESS_INSTANCE);
    checkAuthorization(CREATE_INSTANCES, PROCESS_DEFINITION, definition.getKey());
  }

  public void checkReadProcessInstance(String processInstanceId) {
    ExecutionEntity execution = getProcessInstanceManager().findExecutionById(processInstanceId);
    checkReadProcessInstance(execution);
  }

  public void checkReadProcessInstance(ExecutionEntity execution) {
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) execution.getProcessDefinition();

    PermissionCheck firstCheck = new PermissionCheck();
    firstCheck.setAuthPerms(READ.getValue());
    firstCheck.setAuthResourceType(PROCESS_INSTANCE.resourceType());
    firstCheck.setAuthResourceId(execution.getProcessInstanceId());

    PermissionCheck secondCheck = new PermissionCheck();
    secondCheck.setAuthPerms(READ_INSTANCES.getValue());
    secondCheck.setAuthResourceType(PROCESS_DEFINITION.resourceType());
    secondCheck.setAuthResourceId(processDefinition.getKey());
    secondCheck.setNoAuthFound(0l);

    checkAuthorization(Arrays.asList(firstCheck, secondCheck));
  }

  // update permission //////////////////////////////////////////////////

  public void checkUpdateProcessInstance(String processInstanceId) {
    ExecutionEntity execution = getProcessInstanceManager().findExecutionById(processInstanceId);
    checkUpdateProcessInstance(execution);
  }

  public void checkUpdateProcessInstance(ExecutionEntity execution) {
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) execution.getProcessDefinition();

    PermissionCheck firstCheck = new PermissionCheck();
    firstCheck.setAuthPerms(UPDATE.getValue());
    firstCheck.setAuthResourceType(PROCESS_INSTANCE.resourceType());
    firstCheck.setAuthResourceId(execution.getProcessInstanceId());

    PermissionCheck secondCheck = new PermissionCheck();
    secondCheck.setAuthPerms(UPDATE_INSTANCES.getValue());
    secondCheck.setAuthResourceType(PROCESS_DEFINITION.resourceType());
    secondCheck.setAuthResourceId(processDefinition.getKey());
    secondCheck.setNoAuthFound(0l);

    checkAuthorization(Arrays.asList(firstCheck, secondCheck));
  }

  public void checkUpdateInstancesOnProcessDefinitionById(String processDefinitionId) {
    ProcessDefinitionEntity definition = getProcessDefinitionManager().findLatestProcessDefinitionById(processDefinitionId);
    String processDefinitionKey = definition.getKey();
    checkUpdateInstancesOnProcessDefinitionByKey(processDefinitionKey);
  }

  public void checkUpdateInstancesOnProcessDefinitionByKey(String processDefinitionKey) {
    checkAuthorization(UPDATE_INSTANCES, PROCESS_DEFINITION, processDefinitionKey);
  }

  // delete permission /////////////////////////////////////////////////

  public void checkDeleteProcessInstance(ExecutionEntity execution) {
    ProcessDefinitionEntity processDefinition = (ProcessDefinitionEntity) execution.getProcessDefinition();

    PermissionCheck firstCheck = new PermissionCheck();
    firstCheck.setAuthPerms(DELETE.getValue());
    firstCheck.setAuthResourceType(PROCESS_INSTANCE.resourceType());
    firstCheck.setAuthResourceId(execution.getProcessInstanceId());

    PermissionCheck secondCheck = new PermissionCheck();
    secondCheck.setAuthPerms(DELETE_INSTANCES.getValue());
    secondCheck.setAuthResourceType(PROCESS_DEFINITION.resourceType());
    secondCheck.setAuthResourceId(processDefinition.getKey());
    secondCheck.setNoAuthFound(0l);

    checkAuthorization(Arrays.asList(firstCheck, secondCheck));
  }

}
