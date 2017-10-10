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
 * Copyright (C) 2014 Timur Duehr <tduehr@gmail.com>
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

package org.jruby.platform;

import org.jruby.runtime.builtin.IRubyObject;

import java.lang.Class;
import java.lang.reflect.Method;

public class SolarisPlatform extends Platform {
    private final Class systemClass;
    private final Object system;
    private final Method groupsMethod;

    protected SolarisPlatform() {
        Class sClass = null;
        Object s = null;
        Method g = null;
        try {
            sClass = Class.forName("com.sun.security.auth.module.SolarisSystem");
            s = sClass.getDeclaredConstructor().newInstance();
            g = sClass.getDeclaredMethod("getGroups");
        } catch (Exception e) {
            throw new UnsupportedOperationException(e.getMessage(), e);
        }

        systemClass = sClass;
        system = s;
        groupsMethod = g;
    }

    @Override
    public long[] getGroups(IRubyObject recv) {
        if (groupsMethod == null)
            throw recv.getRuntime().newNotImplementedError("groups() function is unimplemented on this platform");

        try {
            return (long[])groupsMethod.invoke(system);
        } catch (Exception e) {
            throw new UnsupportedOperationException("groups() function is unimplemented on this platform", e);
        }
    }
}
