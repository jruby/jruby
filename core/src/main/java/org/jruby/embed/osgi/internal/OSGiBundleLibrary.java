/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2011 JRuby Community
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
package org.jruby.embed.osgi.internal;

import org.jruby.Ruby;
import org.jruby.util.JRubyClassLoader;
import org.jruby.runtime.load.Library;
import org.osgi.framework.Bundle;

/**
 * @author hmalphettes
 * 
 * Attempt at making an osgi bundle an acceptable container for a jruby library
 * TODO: track the state of the bundle and remove/add the library.
 */
public class OSGiBundleLibrary implements Library {
    
    private final Bundle bundle;

    public OSGiBundleLibrary(Bundle bundle) {
        this.bundle = bundle;
    }

    public void load(Ruby runtime, boolean wrap) {
        // Make Java class files in the bundle and resources reachable from Ruby

        //don't remove this and in-line it: we need this bundle to explicitly
        //import the package org.jruby.util otherwise we get some weird
        //classnotfound exception 'org.jruby.util.JRubyClassLoader'
        JRubyClassLoader jrubycl = (JRubyClassLoader)runtime.getJRubyClassLoader();
        ClassLoader cl = jrubycl.getParent();
        if (cl instanceof JRubyOSGiBundleClassLoader) {
            ((JRubyOSGiBundleClassLoader)cl).addBundle(this.bundle);
        } else {
            throw new IllegalArgumentException("osgi libraries are only" +
            		" supported with a JRubyOSGiBundleClassLoader as the " +
            		" loader of the ScriptingContainer.");
        }
    }


}
