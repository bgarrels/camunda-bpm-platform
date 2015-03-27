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
package org.camunda.bpm.engine.test.authorization;

import static org.camunda.bpm.engine.authorization.Authorization.ANY;
import static org.camunda.bpm.engine.authorization.Permissions.CREATE;
import static org.camunda.bpm.engine.authorization.Permissions.CREATE_INSTANCES;
import static org.camunda.bpm.engine.authorization.Permissions.DELETE;
import static org.camunda.bpm.engine.authorization.Permissions.DELETE_INSTANCES;
import static org.camunda.bpm.engine.authorization.Permissions.READ;
import static org.camunda.bpm.engine.authorization.Permissions.READ_INSTANCES;
import static org.camunda.bpm.engine.authorization.Permissions.UPDATE;
import static org.camunda.bpm.engine.authorization.Permissions.UPDATE_INSTANCES;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_INSTANCE;

import java.util.List;

import org.camunda.bpm.engine.AuthorizationException;
import org.camunda.bpm.engine.impl.AbstractQuery;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.camunda.bpm.engine.task.Task;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessInstanceAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String MESSAGE_START_PROCESS_KEY = "messageStartProcess";
  protected static final String MESSAGE_BOUNDARY_PROCESS_KEY = "messageBoundaryProcess";
  protected static final String SIGNAL_BOUNDARY_PROCESS_KEY = "signalBoundaryProcess";

  protected String deploymentId;

  public void setUp() {
    deploymentId = repositoryService
      .createDeployment()
      .addClasspathResource("org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
      .addClasspathResource("org/camunda/bpm/engine/test/authorization/messageStartEventProcess.bpmn20.xml")
      .addClasspathResource("org/camunda/bpm/engine/test/authorization/messageBoundaryEventProcess.bpmn20.xml")
      .addClasspathResource("org/camunda/bpm/engine/test/authorization/signalBoundaryEventProcess.bpmn20.xml")
      .deploy()
      .getId();
  }

  public void tearDown() {
    super.tearDown();
    repositoryService.deleteDeployment(deploymentId, true);
  }

  // process instance query //////////////////////////////////////////////////////////

  public void testQueryWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testQueryWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, READ, userId);

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    ProcessInstance instance = query.singleResult();
    assertNotNull(instance);
    assertEquals(processInstanceId, instance.getId());
  }

  public void testQueryWithReadInstancesPermissionOnOneTaskProcess() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, READ_INSTANCES, userId);

    // when
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();

    // then
    verifyQueryResults(query, 1);

    ProcessInstance instance = query.singleResult();
    assertNotNull(instance);
    assertEquals(processInstanceId, instance.getId());
  }

  // start process instance by key //////////////////////////////////////////////

  public void testStartProcessInstanceByKeyWithoutAuthorization() {
    // given
    // no authorization to start a process instance

    try {
      // when
      runtimeService.startProcessInstanceByKey(PROCESS_KEY);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByKeyWithCreatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    try {
      // when
      runtimeService.startProcessInstanceByKey(PROCESS_KEY);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE_INSTANCES' permission on resource 'oneTaskProcess' of type 'ProcessDefinition'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByKeyWithCreateInstancesPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, CREATE_INSTANCES, userId);

    try {
      // when
      runtimeService.startProcessInstanceByKey(PROCESS_KEY);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByKey() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, CREATE_INSTANCES, userId);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    // when
    runtimeService.startProcessInstanceByKey(PROCESS_KEY);

    // then
    disableAuthorization();
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    verifyQueryResults(query, 1);
    enableAuthorization();
  }

  // start process instance by id //////////////////////////////////////////////

  public void testStartProcessInstanceByIdWithoutAuthorization() {
    // given
    // no authorization to start a process instance

    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.startProcessInstanceById(processDefinitionId);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByIdWithCreatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.startProcessInstanceById(processDefinitionId);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE_INSTANCES' permission on resource 'oneTaskProcess' of type 'ProcessDefinition'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByIdWithCreateInstancesPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, CREATE_INSTANCES, userId);

    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.startProcessInstanceById(processDefinitionId);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceById() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, CREATE_INSTANCES, userId);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    String processDefinitionId = selectProcessDefinitionByKey(PROCESS_KEY).getId();

    // when
    runtimeService.startProcessInstanceById(processDefinitionId);

    // then
    disableAuthorization();
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    verifyQueryResults(query, 1);
    enableAuthorization();
  }

  // start process instance by message //////////////////////////////////////////////

  public void testStartProcessInstanceByMessageWithoutAuthorization() {
    // given
    // no authorization to start a process instance

    try {
      // when
      runtimeService.startProcessInstanceByMessage("startInvoiceMessage");
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByMessageWithCreatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    try {
      // when
      runtimeService.startProcessInstanceByMessage("startInvoiceMessage");
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE_INSTANCES' permission on resource 'messageStartProcess' of type 'ProcessDefinition'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByMessageWithCreateInstancesPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);

    try {
      // when
      runtimeService.startProcessInstanceByMessage("startInvoiceMessage");
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByMessage() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    // when
    runtimeService.startProcessInstanceByMessage("startInvoiceMessage");

    // then
    disableAuthorization();
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    verifyQueryResults(query, 1);
    enableAuthorization();
  }

  // start process instance by message and process definition id /////////////////////////////

  public void testStartProcessInstanceByMessageAndProcDefIdWithoutAuthorization() {
    // given
    // no authorization to start a process instance

    String processDefinitionId = selectProcessDefinitionByKey(MESSAGE_START_PROCESS_KEY).getId();

    try {
      // when
      runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("startInvoiceMessage", processDefinitionId);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByMessageAndProcDefIdWithCreatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    String processDefinitionId = selectProcessDefinitionByKey(MESSAGE_START_PROCESS_KEY).getId();

    try {
      // when
      runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("startInvoiceMessage", processDefinitionId);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE_INSTANCES' permission on resource 'messageStartProcess' of type 'ProcessDefinition'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByMessageAndProcDefIdWithCreateInstancesPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);

    String processDefinitionId = selectProcessDefinitionByKey(MESSAGE_START_PROCESS_KEY).getId();

    try {
      // when
      runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("startInvoiceMessage", processDefinitionId);
      fail("Exception expected: It should not be possible to start a process instance");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testStartProcessInstanceByMessageAndProcDefId() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    String processDefinitionId = selectProcessDefinitionByKey(MESSAGE_START_PROCESS_KEY).getId();

    // when
    runtimeService.startProcessInstanceByMessageAndProcessDefinitionId("startInvoiceMessage", processDefinitionId);

    // then
    disableAuthorization();
    ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
    verifyQueryResults(query, 1);
    enableAuthorization();
  }

  // delete process instance /////////////////////////////

  public void testDeleteProcessInstanceWithoutAuthorization() {
    // given
    // no authorization to delete a process instance

    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.deleteProcessInstance(processInstanceId, null);
      fail("Exception expected: It should not be possible to delete a process instance");
    } catch (AuthorizationException e) {

      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(DELETE.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testDeleteProcessInstanceWithDeletePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, DELETE, userId);

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then
    disableAuthorization();
    assertProcessEnded(processInstanceId);
    enableAuthorization();
  }

  public void testDeleteProcessInstanceWithDeletePermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, DELETE, userId);

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then
    disableAuthorization();
    assertProcessEnded(processInstanceId);
    enableAuthorization();
  }

  public void testDeleteProcessInstanceWithDeleteInstancesPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, DELETE_INSTANCES, userId);

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then
    disableAuthorization();
    assertProcessEnded(processInstanceId);
    enableAuthorization();
  }

  public void testDeleteProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, DELETE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, DELETE_INSTANCES, userId);

    // when
    runtimeService.deleteProcessInstance(processInstanceId, null);

    // then
    disableAuthorization();
    assertProcessEnded(processInstanceId);
    enableAuthorization();
  }

  // get active activity ids ///////////////////////////////////

  public void testGetActiveActivityIdsWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.getActiveActivityIds(processInstanceId);
      fail("Exception expected: It should not be possible to retrieve active ativity ids");
    } catch (AuthorizationException e) {

      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(READ.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testGetActiveActivityIdsWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, READ, userId);

    // when
    List<String> activityIds = runtimeService.getActiveActivityIds(processInstanceId);

    // then
    assertNotNull(activityIds);
    assertFalse(activityIds.isEmpty());
  }

  public void testGetActiveActivityIdsWithReadPermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, READ, userId);

    // when
    List<String> activityIds = runtimeService.getActiveActivityIds(processInstanceId);

    // then
    assertNotNull(activityIds);
    assertFalse(activityIds.isEmpty());
  }

  public void testGetActiveActivityIdsWithReadInstancesPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, READ_INSTANCES, userId);

    // when
    List<String> activityIds = runtimeService.getActiveActivityIds(processInstanceId);

    // then
    assertNotNull(activityIds);
    assertFalse(activityIds.isEmpty());
  }

  public void testGetActiveActivityIds() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, READ, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, READ_INSTANCES, userId);

    // when
    List<String> activityIds = runtimeService.getActiveActivityIds(processInstanceId);

    // then
    assertNotNull(activityIds);
    assertFalse(activityIds.isEmpty());
  }

  // get activity instance ///////////////////////////////////////////

  public void testGetActivityInstanceWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.getActivityInstance(processInstanceId);
      fail("Exception expected: It should not be possible to retrieve ativity instances");
    } catch (AuthorizationException e) {

      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(READ.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testGetActivityInstanceWithReadPermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, READ, userId);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstanceId);

    // then
    assertNotNull(activityInstance);
  }

  public void testGetActivityInstanceWithReadPermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, READ, userId);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstanceId);

    // then
    assertNotNull(activityInstance);
  }

  public void testGetActivityInstanceWithReadInstancesPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, READ_INSTANCES, userId);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstanceId);

    // then
    assertNotNull(activityInstance);
  }

  public void testGetActivityInstanceIds() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, READ, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, READ_INSTANCES, userId);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstanceId);

    // then
    assertNotNull(activityInstance);
  }

  // signal execution ///////////////////////////////////////////

  public void testSignalWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.signal(processInstanceId);
      fail("Exception expected: It should not be possible to signal an execution");
    } catch (AuthorizationException e) {
      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(UPDATE.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSignalWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    // when
    runtimeService.signal(processInstanceId);

    // then
    assertProcessEnded(processInstanceId);
  }

  public void testSignalWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    // when
    runtimeService.signal(processInstanceId);

    // then
    assertProcessEnded(processInstanceId);
  }

  public void testSignalWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.signal(processInstanceId);

    // then
    assertProcessEnded(processInstanceId);
  }

  public void testSignal() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, READ, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    ActivityInstance activityInstance = runtimeService.getActivityInstance(processInstanceId);

    // then
    assertNotNull(activityInstance);
  }

  // signal event received //////////////////////////////////////

  public void testSignalEventReceivedWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();

    try {
      // when
      runtimeService.signalEventReceived("alert");
      fail("Exception expected: It should not be possible to trigger a signal event");
    } catch (AuthorizationException e) {
      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(UPDATE.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSignalEventReceivedWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    // when
    runtimeService.signalEventReceived("alert");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceivedWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    // when
    runtimeService.signalEventReceived("alert");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceivedWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, SIGNAL_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.signalEventReceived("alert");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceived() {
    // given
    String processInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, SIGNAL_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.signalEventReceived("alert");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceivedTwoExecutionsShouldFail() {
    // given
    String firstProcessInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    String secondProcessInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_INSTANCE, firstProcessInstanceId, UPDATE, userId);

    try {
      // when
      runtimeService.signalEventReceived("alert");
      fail("Exception expected: It should not be possible to trigger a signal event");
    } catch (AuthorizationException e) {
      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(UPDATE.getName(), message);
//      assertTextPresent(secondProcessInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSignalEventReceivedTwoExecutionsShouldSuccess() {
    // given
    String firstProcessInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    String secondProcessInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_INSTANCE, firstProcessInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_INSTANCE, secondProcessInstanceId, UPDATE, userId);

    // when
    runtimeService.signalEventReceived("alert");

    // then
    disableAuthorization();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertFalse(tasks.isEmpty());
    for (Task task : tasks) {
      assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
    }
    enableAuthorization();
  }

  // signal event received by execution id //////////////////////////////////////

  public void testSignalEventReceivedByExecutionIdWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    String executionId = selectSingleTask().getExecutionId();

    try {
      // when
      runtimeService.signalEventReceived("alert", executionId);
      fail("Exception expected: It should not be possible to trigger a signal event");
    } catch (AuthorizationException e) {
      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(UPDATE.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSignalEventReceivedByExecutionIdWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.signalEventReceived("alert", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceivedByExecutionIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.signalEventReceived("alert", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceivedByExecutionIdWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, SIGNAL_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.signalEventReceived("alert", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testSignalEventReceivedByExecutionId() {
    // given
    String processInstanceId = startProcessInstanceByKey(SIGNAL_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, SIGNAL_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.signalEventReceived("alert", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  // message event received /////////////////////////////////////

  public void testMessageEventReceivedWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    String executionId = selectSingleTask().getExecutionId();

    try {
      // when
      runtimeService.messageEventReceived("boundaryInvoiceMessage", executionId);
      fail("Exception expected: It should not be possible to trigger a message event");
    } catch (AuthorizationException e) {
      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(UPDATE.getName(), message);
//      assertTextPresent(processInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testMessageEventReceivedByExecutionIdWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.messageEventReceived("boundaryInvoiceMessage", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testMessageEventReceivedByExecutionIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.messageEventReceived("boundaryInvoiceMessage", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testMessageEventReceivedByExecutionIdWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.messageEventReceived("boundaryInvoiceMessage", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testMessageEventReceivedByExecutionId() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    String executionId = selectSingleTask().getExecutionId();

    // when
    runtimeService.messageEventReceived("boundaryInvoiceMessage", executionId);

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  // correlate message (correlates to an execution) /////////////

  public void testCorrelateMessageExecutionWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();

    try {
      // when
      runtimeService.correlateMessage("boundaryInvoiceMessage");
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {
      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testCorrelateMessageExecutionWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    // when
    runtimeService.correlateMessage("boundaryInvoiceMessage");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateMessageExecutionWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    // when
    runtimeService.correlateMessage("boundaryInvoiceMessage");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateMessageExecutionWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.correlateMessage("boundaryInvoiceMessage");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateMessageExecution() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.correlateMessage("boundaryInvoiceMessage");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  // correlate message (correlates to a process definition) /////////////

  public void testCorrelateMessageProcessDefinitionWithoutAuthorization() {
    // given

    try {
      // when
      runtimeService.correlateMessage("startInvoiceMessage");
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {
      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testCorrelateMessageProcessDefinitionWithCreatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    try {
      // when
      runtimeService.correlateMessage("startInvoiceMessage");
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE_INSTANCES' permission on resource 'messageStartProcess' of type 'ProcessDefinition'", e.getMessage());
    }
  }

  public void testCorrelateMessageProcessDefinitionWithCreateInstancesPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);

    try {
      // when
      runtimeService.correlateMessage("startInvoiceMessage");
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testCorrelateMessageProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);

    // when
    runtimeService.correlateMessage("startInvoiceMessage");

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("task", task.getTaskDefinitionKey());
  }

  // correlate all (correlates to executions) ///////////////////

  public void testCorrelateAllExecutionWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();

    try {
      // when
      runtimeService
        .createMessageCorrelation("boundaryInvoiceMessage")
        .correlateAll();
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {
      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testCorrelateAllExecutionWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    // when
    runtimeService
      .createMessageCorrelation("boundaryInvoiceMessage")
      .correlateAll();

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateAllExecutionWithUpdatePermissionOnAnyProcessInstance() {
    // given
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    // when
    runtimeService
      .createMessageCorrelation("boundaryInvoiceMessage")
      .correlateAll();

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateAllExecutionWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService
      .createMessageCorrelation("boundaryInvoiceMessage")
      .correlateAll();

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateAllExecution() {
    // given
    String processInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_BOUNDARY_PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService
      .createMessageCorrelation("boundaryInvoiceMessage")
      .correlateAll();

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
  }

  public void testCorrelateAllTwoExecutionsShouldFail() {
    // given
    String firstProcessInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    String secondProcessInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_INSTANCE, firstProcessInstanceId, UPDATE, userId);

    try {
      // when
      runtimeService
        .createMessageCorrelation("boundaryInvoiceMessage")
        .correlateAll();
      fail("Exception expected: It should not be possible to trigger a signal event");
    } catch (AuthorizationException e) {
      // then
//      String message = e.getMessage();
//      assertTextPresent(userId, message);
//      assertTextPresent(UPDATE.getName(), message);
//      assertTextPresent(secondProcessInstanceId, message);
//      assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testCorrelateAllTwoExecutionsShouldSuccess() {
    // given
    String firstProcessInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();
    String secondProcessInstanceId = startProcessInstanceByKey(MESSAGE_BOUNDARY_PROCESS_KEY).getId();

    createGrantAuthorization(PROCESS_INSTANCE, firstProcessInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_INSTANCE, secondProcessInstanceId, UPDATE, userId);

    // when
    runtimeService
      .createMessageCorrelation("boundaryInvoiceMessage")
      .correlateAll();

    // then
    disableAuthorization();
    List<Task> tasks = taskService.createTaskQuery().list();
    assertFalse(tasks.isEmpty());
    for (Task task : tasks) {
      assertEquals("taskAfterBoundaryEvent", task.getTaskDefinitionKey());
    }
    enableAuthorization();
  }

  // correlate all (correlates to a process definition) /////////////

  public void testCorrelateAllProcessDefinitionWithoutAuthorization() {
    // given

    try {
      // when
      runtimeService
        .createMessageCorrelation("startInvoiceMessage")
        .correlateAll();
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {
      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testCorrelateAllProcessDefinitionWithCreatePermissionOnProcessInstance() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);

    try {
      // when
      runtimeService
        .createMessageCorrelation("startInvoiceMessage")
        .correlateAll();
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE_INSTANCES' permission on resource 'messageStartProcess' of type 'ProcessDefinition'", e.getMessage());
    }
  }

  public void testCorrelateAllProcessDefinitionWithCreateInstancesPermissionOnProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);

    try {
      // when
      runtimeService
        .createMessageCorrelation("startInvoiceMessage")
        .correlateAll();
      fail("Exception expected: It should not be possible to correlate a message.");
    } catch (AuthorizationException e) {

      // then
      assertTextPresent("The user with id 'test' does not have 'CREATE' permission on resource 'ProcessInstance'", e.getMessage());
    }
  }

  public void testCorrelateAllProcessDefinition() {
    // given
    createGrantAuthorization(PROCESS_INSTANCE, ANY, CREATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, MESSAGE_START_PROCESS_KEY, CREATE_INSTANCES, userId);

    // when
    runtimeService
      .createMessageCorrelation("startInvoiceMessage")
      .correlateAll();

    // then
    Task task = selectSingleTask();
    assertNotNull(task);
    assertEquals("task", task.getTaskDefinitionKey());
  }

  // suspend process instance by id /////////////////////////////

  public void testSuspendProcessInstanceByIdWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();

    try {
      // when
      runtimeService.suspendProcessInstanceById(processInstanceId);
      fail("Exception expected: It should not be posssible to suspend a process instance.");
    } catch (AuthorizationException e) {

      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSuspendProcessInstanceByIdWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    // when
    runtimeService.suspendProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertTrue(instance.isSuspended());
  }

  public void testSuspendProcessInstanceByIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    // when
    runtimeService.suspendProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertTrue(instance.isSuspended());
  }

  public void testSuspendProcessInstanceByIdWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.suspendProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertTrue(instance.isSuspended());
  }

  public void testSuspendProcessInstanceById() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.suspendProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertTrue(instance.isSuspended());
  }

  // activate process instance by id /////////////////////////////

  public void testActivateProcessInstanceByIdWithoutAuthorization() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);

    try {
      // when
      runtimeService.activateProcessInstanceById(processInstanceId);
      fail("Exception expected: It should not be posssible to activate a process instance.");
    } catch (AuthorizationException e) {

      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testActivateProcessInstanceByIdWithUpdatePermissionOnProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    // when
    runtimeService.activateProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertFalse(instance.isSuspended());
  }

  public void testActivateProcessInstanceByIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    // when
    runtimeService.activateProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertFalse(instance.isSuspended());
  }

  public void testActivateProcessInstanceByIdWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.activateProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertFalse(instance.isSuspended());
  }

  public void testActivateProcessInstanceById() {
    // given
    String processInstanceId = startProcessInstanceByKey(PROCESS_KEY).getId();
    suspendProcessInstanceById(processInstanceId);
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.activateProcessInstanceById(processInstanceId);

    // then
    ProcessInstance instance = selectSingleProcessInstance();
    assertFalse(instance.isSuspended());
  }

  // suspend process instance by process definition id /////////////////////////////

  public void testSuspendProcessInstanceByProcessDefinitionIdWithoutAuthorization() {
    // given
    String processDefinitionId = startProcessInstanceByKey(PROCESS_KEY).getProcessDefinitionId();

    try {
      // when
      runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinitionId);
      fail("Exception expected: It should not be posssible to suspend a process instance.");
    } catch (AuthorizationException e) {

      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSuspendProcessInstanceByProcessDefinitionIdWithUpdatePermissionOnProcessInstance() {
    // given
    ProcessInstance instance = startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = instance.getId();
    String processDefinitionId = instance.getProcessDefinitionId();
    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);

    try {
      // when
      runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinitionId);
      fail("Exception expected: It should not be posssible to suspend a process instance.");
    } catch (AuthorizationException e) {

      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSuspendProcessInstanceByProcessDefinitionIdWithUpdatePermissionOnAnyProcessInstance() {
    // given
    ProcessInstance instance = startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = instance.getId();
    String processDefinitionId = instance.getProcessDefinitionId();
    createGrantAuthorization(PROCESS_INSTANCE, ANY, UPDATE, userId);

    try {
      // when
      runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinitionId);
      fail("Exception expected: It should not be posssible to suspend a process instance.");
    } catch (AuthorizationException e) {

      // then
      // String message = e.getMessage();
      // assertTextPresent(userId, message);
      // assertTextPresent(UPDATE.getName(), message);
      // assertTextPresent(processInstanceId, message);
      // assertTextPresent(PROCESS_INSTANCE.resourceName(), message);
    }
  }

  public void testSuspendProcessInstanceByProcessDefinitionIdWithUpdateInstancesPermissionOnProcessDefinition() {
    // given
    ProcessInstance instance = startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = instance.getId();
    String processDefinitionId = instance.getProcessDefinitionId();
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinitionId);

    // then
    instance = selectSingleProcessInstance();
    assertTrue(instance.isSuspended());
  }

  public void testSuspendProcessInstanceByProcessDefinitionId() {
    // given
    ProcessInstance instance = startProcessInstanceByKey(PROCESS_KEY);
    String processInstanceId = instance.getId();
    String processDefinitionId = instance.getProcessDefinitionId();

    createGrantAuthorization(PROCESS_INSTANCE, processInstanceId, UPDATE, userId);
    createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY, UPDATE_INSTANCES, userId);

    // when
    runtimeService.suspendProcessInstanceByProcessDefinitionId(processDefinitionId);

    // then
    instance = selectSingleProcessInstance();
    assertTrue(instance.isSuspended());
  }

  // helper /////////////////////////////////////////////////////

  protected void verifyQueryResults(ProcessInstanceQuery query, int countExpected) {
    verifyQueryResults((AbstractQuery<?, ?>) query, countExpected);
  }

}
