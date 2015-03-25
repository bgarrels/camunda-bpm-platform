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
package org.camunda.bpm.engine.impl.db;


/**
 * @author Roman Smirnov
 *
 */
public class PermissionCheck {

  /** the permission(s) to check for */
  protected int authPerms;

  /** the type of the resource to check permissions for */
  protected int authResourceType;

  /** the id of the resource to check permissions for */
  protected String authResourceId;

  /** query parameter for resaource Id. Is injected as RAW parameter into the query */
  protected String authResourceIdQueryParam;

  protected Long noAuthFound = null;

  public int getAuthPerms() {
    return authPerms;
  }

  public void setAuthPerms(int authPerms) {
    this.authPerms = authPerms;
  }

  public int getAuthResourceType() {
    return authResourceType;
  }

  public void setAuthResourceType(int authResourceType) {
    this.authResourceType = authResourceType;
  }

  public String getAuthResourceId() {
    return authResourceId;
  }

  public void setAuthResourceId(String authResourceId) {
    this.authResourceId = authResourceId;
  }

  public String getAuthResourceIdQueryParam() {
    return authResourceIdQueryParam;
  }

  public void setAuthResourceIdQueryParam(String authResourceIdQueryParam) {
    this.authResourceIdQueryParam = authResourceIdQueryParam;
  }

  public Long getNoAuthFound() {
    return noAuthFound;
  }

  public void setNoAuthFound(Long noAuthFound) {
    this.noAuthFound = noAuthFound;
  }

}
