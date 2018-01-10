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

/**
 * The used {@link org.jruby.runtime.profile.ProfilingService ProfilingService} will create an instance of this class
 * at the end of each {@link org.jruby.RubyThread} or at jruby shutdown the {@link org.jruby.runtime.profile.ProfileReporter#report(ProfileCollection)}
 * is called with the {@link org.jruby.runtime.profile.ProfileCollection ProfileCollection} associated with the current Thread.
 *
 * Add your implementation of {@link org.jruby.runtime.profile.ProfilingService} to the classpath and add the command line --profile.service my.impl.class
 *
 * @author Andre Kullmann
 */
public interface ProfileReporter {

    /**
     * At the end of each {@link org.jruby.RubyThread} or at jruby shutdown this method
     * is called with the {@link org.jruby.runtime.profile.ProfileCollection ProfileCollection} associated with the current Thread.
     *
     * @param collection the profile information to report. print to file, system out, send ot a profile server, ...
     */
    public void report( ProfileCollection collection );
}
