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

import java.util.Date;

import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.SuspensionState;
import org.camunda.bpm.engine.impl.persistence.entity.TimerEntity;

/**
 * @author Roman Smirnov
 *
 */
public abstract class AbstractSetStateCmd implements Command<Void> {

  protected static final String SUSPENSION_STATE_PROPERTY = "suspensionState";

  protected boolean includeSubResources;
  protected boolean isLogUserOperationDisabled;
  protected Date executionDate;

  public AbstractSetStateCmd(boolean includeSubResources, Date executionDate) {
    this.includeSubResources = includeSubResources;
    this.executionDate = executionDate;
  }

  public Void execute(CommandContext commandContext) {
    checkParameters(commandContext);

    if (executionDate == null) {
      updateSuspensionState(commandContext, getNewSuspensionState());

      if (isIncludeSubResources()) {
        AbstractSetStateCmd cmd = getNextCommand();
        if (cmd != null) {
          cmd.disableLogUserOperation();
          cmd.execute(commandContext);
        }
      }

    }
    else {
      scheduleSuspensionStateUpdate(commandContext);
    }

    if (!isLogUserOperationDisabled()) {
      logUserOperation(commandContext);
    }

    return null;
  }

  public void disableLogUserOperation() {
    this.isLogUserOperationDisabled = true;
  }

  protected boolean isLogUserOperationDisabled() {
    return isLogUserOperationDisabled;
  }

  protected boolean isIncludeSubResources() {
    return includeSubResources;
  }

  protected void scheduleSuspensionStateUpdate(CommandContext commandContext) {
    TimerEntity timer = new TimerEntity();

    String jobHandlerConfiguration = getJobHandlerConfiguration();

    timer.setDuedate(executionDate);
    timer.setJobHandlerType(getDelayedExecutionJobHandlerType());
    timer.setJobHandlerConfiguration(jobHandlerConfiguration);

    commandContext.getJobManager().schedule(timer);
  }

  protected String getDelayedExecutionJobHandlerType() {
    return null;
  }

  protected String getJobHandlerConfiguration() {
    return null;
  }

  protected AbstractSetStateCmd getNextCommand() {
    return null;
  }

  protected abstract void checkParameters(CommandContext commandContext);

  protected abstract void updateSuspensionState(CommandContext commandContext, SuspensionState suspensionState);

  protected abstract void logUserOperation(CommandContext commandContext);

  protected abstract String getLogEntryOperation();

  protected abstract SuspensionState getNewSuspensionState();

}
