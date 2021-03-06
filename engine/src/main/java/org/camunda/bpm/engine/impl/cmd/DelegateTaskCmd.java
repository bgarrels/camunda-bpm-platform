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

package org.camunda.bpm.engine.impl.cmd;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.camunda.bpm.engine.history.UserOperationLogEntry;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.AuthorizationManager;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.impl.persistence.entity.TaskManager;


/**
 * @author Tom Baeyens
 */
public class DelegateTaskCmd implements Command<Object>, Serializable {

  private static final long serialVersionUID = 1L;

  protected String taskId;
  protected String userId;

  public DelegateTaskCmd(String taskId, String userId) {
    this.taskId = taskId;
    this.userId = userId;
  }

  public Object execute(CommandContext commandContext) {
    ensureNotNull("taskId", taskId);

    final TaskManager taskManager = commandContext.getTaskManager();
    TaskEntity task = commandContext.runWithoutAuthentication(new Callable<TaskEntity>() {
      public TaskEntity call() throws Exception {
        return taskManager.findTaskById(taskId);
      }
    });

    ensureNotNull("Cannot find task with id " + taskId, "task", task);

    AuthorizationManager authorizationManager = commandContext.getAuthorizationManager();
    authorizationManager.checkUpdateTask(task);

    task.delegate(userId);

    task.createHistoricTaskDetails(UserOperationLogEntry.OPERATION_TYPE_DELEGATE);

    return null;
  }

}
