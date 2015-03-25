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

import static org.camunda.bpm.engine.authorization.Permissions.READ;
import static org.camunda.bpm.engine.authorization.Permissions.READ_TASKS;
import static org.camunda.bpm.engine.authorization.Resources.PROCESS_DEFINITION;
import static org.camunda.bpm.engine.authorization.Resources.TASK;

import org.apache.ibatis.logging.LogFactory;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.impl.AbstractQuery;
import org.camunda.bpm.engine.task.TaskQuery;

/**
 * @author Roman Smirnov
 *
 */
public class TaskAuthorizationTest extends AuthorizationTest {

  protected static final String PROCESS_KEY = "oneTaskProcess";
  protected static final String CASE_KEY = "oneTaskCase";

  protected String deploymentId;

  static {
    LogFactory.useJdkLogging();
  }

  public void setUp() {
    deploymentId = repositoryService
      .createDeployment()
      .addClasspathResource("org/camunda/bpm/engine/test/api/oneTaskProcess.bpmn20.xml")
      .addClasspathResource("org/camunda/bpm/engine/test/authorization/oneTaskCase.cmmn")
      .deploy()
      .getId();
  }

  public void tearDown() {
    super.tearDown();
    repositoryService.deleteDeployment(deploymentId, true);
  }

  public void testQueryWithTaskInsideProcessWithoutAuthorization() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);
  }

  public void testQueryWithTaskInsideProcessWithReadPermissionOnTask() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);
    String taskId = selectSingleTask().getId();

    Authorization authorization = createGrantAuthorization(TASK, taskId);
    authorization.setUserId(userId);
    authorization.addPermission(READ);
    saveAuthorization(authorization);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testQueryWithTaskInsideProcessWithReadPermissionOnOneTaskProcess() {
    // given
    startProcessInstanceByKey(PROCESS_KEY);

    Authorization authorization = createGrantAuthorization(PROCESS_DEFINITION, PROCESS_KEY);
    authorization.setUserId(userId);
    authorization.addPermission(READ_TASKS);
    saveAuthorization(authorization);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testQueryWithTaskInsideCaseWithoutAuthorization() {
    // given
    createCaseInstanceByKey(CASE_KEY);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);
  }

  public void testQueryWithStandaloneTaskWithoutAuthorization() {
    // given
    String taskId = "newTask";
    createTask(taskId);

    Authorization authorization = createGrantAuthorization(TASK, taskId);
    authorization.setUserId(userId);
    authorization.addPermission(READ);
    saveAuthorization(authorization);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 1);

    deleteTask(taskId, true);
  }

  public void testQueryWithStandaloneTaskWithReadPermissionOnTask() {
    // given
    String taskId = "newTask";
    createTask(taskId);

    // when
    TaskQuery query = taskService.createTaskQuery();

    // then
    verifyQueryResults(query, 0);

    deleteTask(taskId, true);
  }

  protected void verifyQueryResults(TaskQuery query, int countExpected) {
    verifyQueryResults((AbstractQuery<?, ?>) query, countExpected);
  }

}
