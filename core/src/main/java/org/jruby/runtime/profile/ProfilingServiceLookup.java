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
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.profile.builtin.BuiltinProfilingService;

import java.lang.reflect.InvocationTargetException;

/**
 * This helper is used to get the configured {@link org.jruby.runtime.profile.ProfilingService} for the current {@link Ruby} instance.
 * Each {@link Ruby} instance has a {@link org.jruby.runtime.profile.ProfilingServiceLookup} property.
 *
 * @author Andre Kullmann
 */
public class ProfilingServiceLookup {

    private final Ruby runtime;

    /**
     * The service which is configured. Will be lazy loaded, and never changed.
     */
    private ProfilingService service;

    /**
     *
     * @param runtime the ruby instance this instance belongs to
     * @throws java.lang.IllegalArgumentException if given runtime is null
     */
    public ProfilingServiceLookup(final Ruby runtime) {

        if( runtime == null )
            throw new IllegalArgumentException( "Given runtime must not be null." );

        this.runtime = runtime;
    }

    /**
     * Returns {@link #runtime}. At first invocation {@link #newProfiler()} is called to create a new instance
     * of the configured {@link org.jruby.runtime.profile.ProfilingService} instance.
     *
     * @return the configured {@link org.jruby.runtime.profile.ProfilingService} for the {@link #runtime}.
     */
    public ProfilingService getService() {
        return service == null ? newProfiler() : service;
    }

    /**
     * Getter to avoid direct property access.
     */
    private Ruby getRuntime() {
        return runtime;
    }

    /**
     * Getter to avoid direct property access.
     */
    private RubyInstanceConfig getConfig() {
        return getRuntime().getInstanceConfig();
    }

    /**
     * Getter to avoid direct property access.
     */
    private RubyInstanceConfig.ProfilingMode getProfilingMode() {
        return getConfig().getProfilingMode();
    }

    /**
     * Getter to avoid direct property access.
     */
    private String getServiceClassName() {
        return getConfig().getProfilingService();
    }

    /**
     * Creates a new instance of the configured {@link org.jruby.runtime.profile.ProfilingService} instance.
     * @return The service instance which should be used.
     */
    private synchronized ProfilingService newProfiler(  ) {
        if( service == null ) {
            switch( getProfilingMode() ) {
                case SERVICE:
                    if( getServiceClassName() == null || getServiceClassName().trim().isEmpty() ) {
                        throw new RuntimeException( "No profiling service property found.");
                    }

                    service = newServiceInstance();
                    break;

                default:
                    service = new BuiltinProfilingService(runtime);
                    break;
            }
        }

        return service;
    }

    /**
     * @return new instance created by reflection
     */
    private ProfilingService newServiceInstance() {

        Class<? extends ProfilingService> clazz = loadServiceClass();

        try {
            return clazz.getConstructor(Ruby.class).newInstance(runtime);
        } catch (InvocationTargetException
                |NoSuchMethodException
                |InstantiationException
                |IllegalAccessException e) {
            throw new RuntimeException( "Can't create service service. " + e.getClass().getSimpleName() + ": " + e.getMessage() );
        }
    }

    /**
     * @return the configured {@link org.jruby.runtime.profile.ProfilingService} class.
     */
    @SuppressWarnings( "unchecked" )
    private Class<? extends ProfilingService> loadServiceClass() {

        ClassLoader cl = getRuntime().getJRubyClassLoader();

        try {
            return (Class<? extends ProfilingService>) cl.loadClass( getServiceClassName() );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException( "Can't load service service class. " + e.getClass().getSimpleName() + ": " + e.getMessage() );
        }

    }

}
