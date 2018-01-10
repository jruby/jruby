/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.profile;

import org.jruby.Ruby;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;

/**
 * A ProfilingService is used to profile jruby programs.
 * Using this interface you can implement your own profiling implementation.
 * You can collect application specified data e.g. group profiling information by user.
 * You can collect jdbc data to, where an how many time a specific query was executed.
 *
 * @author Andre Kullmann
 */
public interface ProfilingService {

    /**
     *
     * @param context the {@link org.jruby.runtime.ThreadContext} the new created {@link org.jruby.runtime.profile.ProfileCollection} belongs to.
     * @return a new {@link org.jruby.runtime.profile.ProfileCollection} instance, which will be associated with the given context
     */
    public ProfileCollection newProfileCollection(ThreadContext context);

    /**
     *
     * @param runtime The ruby instance the returned {@link org.jruby.runtime.profile.MethodEnhancer} belongs to
     * @return a new {@link org.jruby.runtime.profile.MethodEnhancer} instance. will be used to add profiling information to all methods in the given runtime.
     */
    public MethodEnhancer newMethodEnhancer( Ruby runtime );

    /**
     *
     * @param context the {@link org.jruby.runtime.ThreadContext} the returned {@link org.jruby.runtime.profile.ProfileReporter} will belongs to.
     * @return a new instance of {@link org.jruby.runtime.profile.ProfileReporter} which can be used to process the collected profile information.
     */
    public ProfileReporter newProfileReporter(ThreadContext context);

    /**
     * Add a named method to the profiling service to be monitored.
     *
     * @param name the name
     * @param method the method
     */
    public void addProfiledMethod(String name, DynamicMethod method);
}
