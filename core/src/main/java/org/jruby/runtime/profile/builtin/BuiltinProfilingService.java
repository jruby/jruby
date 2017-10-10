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
package org.jruby.runtime.profile.builtin;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.ProfilingDynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.profile.MethodEnhancer;
import org.jruby.runtime.profile.ProfileCollection;
import org.jruby.runtime.profile.ProfileReporter;
import org.jruby.runtime.profile.ProfilingService;

/**
 * This implementation of {@link org.jruby.runtime.profile.ProfilingService} will be used for all profiling methods
 * which are shipped with jruby.
 *
 * @author Andre Kullmann
 */
public class BuiltinProfilingService implements ProfilingService {

    // The method objects for serial numbers
    private final ProfiledMethods profiledMethods;

    public BuiltinProfilingService(Ruby runtime) {
        this.profiledMethods = new ProfiledMethods(runtime);
    }

    @Override
    public ProfileData newProfileCollection(ThreadContext context) {
        return new ProfileData( context, profiledMethods );
    }

    @Override
    public DefaultMethodEnhancer newMethodEnhancer( final Ruby runtime ) {
        return new DefaultMethodEnhancer( );
    }

    @Override
    public DefaultProfileReporter newProfileReporter(ThreadContext context) {
        return new DefaultProfileReporter( context );
    }

    @Override
    public void addProfiledMethod(String name, DynamicMethod method) {
        profiledMethods.addProfiledMethod(name, method);
    }

    /**
     * @author Andre Kullmann
     */
    private final class DefaultMethodEnhancer implements MethodEnhancer {
        @Override
        @SuppressWarnings("deprecation")
        public DynamicMethod enhance( String name, DynamicMethod delegate ) {
            profiledMethods.addProfiledMethod( name, delegate );
            return new ProfilingDynamicMethod(delegate);
        }
    }

    /**
     * @author Andre Kullmann
     */
    private static final class DefaultProfileReporter implements ProfileReporter {

        private final ThreadContext context;

        public DefaultProfileReporter(ThreadContext context) {

            if( context == null )
                throw new IllegalArgumentException( "Given context must not be null." );

            this.context = context;
        }

        private Ruby getRuntime() {
            return context.runtime;
        }

        private RubyInstanceConfig getConfig() {
            return getRuntime().getInstanceConfig();
        }

        @Override
        public void report(ProfileCollection collector) {

            if(!(collector instanceof ProfileData))
                throw new IllegalArgumentException( "Given collector must be an instance of " + ProfileData.class.getName() + "." );

            RubyInstanceConfig config = getConfig();
            ProfileData   profileData = (ProfileData) collector;
            ProfileOutput      output = config.getProfileOutput();

            ProfilePrinter profilePrinter = ProfilePrinter.newPrinter( config.getProfilingMode(), profileData );
            if (profilePrinter != null) {
                output.printProfile(profilePrinter);
            } else {
                getRuntime().getOut().println("\nno printer for profile mode: " + config.getProfilingMode() + " !");
            }
        }

    }
}
