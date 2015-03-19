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
package org.camunda.bpm.engine.runtime;

/**
 * <p>A transition instance represents an execution token that
 * has just completed a transition (sequence flow in BPMN) or is about
 * to take an outgoing transition. This happens before starting or after
 * leaving an activity. The execution token
 * is not actually executing the activity that this instance points to
 * which is why the corresponding activity instance does not exist.</p>
 *
 * <p>Transition instances are the result of
 * asynchronous continuations, asyncBefore or asyncAfter.</p>
 *
 * @author Daniel Meyer
 * @author Thorben Lindhauer
 *
 */
public interface TransitionInstance extends ProcessElementInstance {

  /**
   * returns the id of the target activity
   *
   * @deprecated a transition instances represents a transition <b>to</b> or <b>from</b>
   *  an activity; use {@link #getActivityId()} instead.
   */
  @Deprecated
  String getTargetActivityId();

  /**
   * returns the id of the activity a transition is made from/to
   */
  String getActivityId();

  /** returns the id of of the execution that is
   * executing this transition instance */
  String getExecutionId();

}
