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
import org.jruby.common.IRubyWarnings;
import org.jruby.common.RubyWarnings;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.util.collections.NonBlockingHashMapLong;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * This is a collection af all methods which will be profiled.
 * Current it's just a wrapper for a {@link NonBlockingHashMapLong},
 * but the implementation can be changed in the future without changing the interface.
 *
 * @author Andre Kullmann
 */
public class ProfiledMethods {

    private final NonBlockingHashMapLong<ProfiledMethod> methods;

    private final Ruby runtime;

    public ProfiledMethods( final Ruby runtime ) {

        if( runtime == null )
            throw new IllegalArgumentException( "Given runtime must not be null." );

        this.runtime = runtime;
        // TODO is 10000 a good value ?
        this.methods = new NonBlockingHashMapLong<>(10000);
    }

    private Ruby getRuntime() {
        return runtime;
    }

    private RubyInstanceConfig getConfig() {
        return getRuntime().getInstanceConfig();
    }

    private RubyWarnings getWarnings() {
        return getRuntime().getWarnings();
    }

    private int getProfileMaxMethods() {
        return getConfig().getProfileMaxMethods();
    }

    private ConcurrentMap<Long,ProfiledMethod> getMethods() {
        return methods;
    }

    public void addProfiledMethod( final String name, final DynamicMethod method ) {

        final long serial = method.getSerialNumber();

        if ( getMethods().size() >= getProfileMaxMethods()) {
            getWarnings().warnOnce(IRubyWarnings.ID.PROFILE_MAX_METHODS_EXCEEDED, "method count exceeds max of " + getConfig().getProfileMaxMethods() + "; no new methods will be profiled");
            return;
        }

        getMethods().putIfAbsent( serial, new ProfiledMethod(name, method) );
    }

    public ProfiledMethod getProfiledMethod( final long serial ) {

        return getMethods().get( serial );
    }
}
