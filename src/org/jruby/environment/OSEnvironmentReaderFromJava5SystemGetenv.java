/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.environment;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;

import org.jruby.IRuby;

class OSEnvironmentReaderFromJava5SystemGetenv implements IOSEnvironmentReader {


    /**
     * Gets the new Java 5 Sytem.getenv method via reflection.
     * @return
     */
    protected Method getSystemGetenvMethod() {
        Method getenvMethod = null;
        try {
            getenvMethod = java.lang.System.class.getMethod("getenv", (Class[]) null);
            if (!getenvMethod.getReturnType().equals(java.util.Map.class)) {
                // wrong return type
                getenvMethod = null;
            }
        } catch (NoSuchMethodException e) {
            // This is the error you get if using Java 1.4.2
            getenvMethod = null;
        } catch (Exception e) {
            // Some other e.g. security problem
            getenvMethod = null;
        }
        return getenvMethod;
    }

    /* (non-Javadoc)
     * @see org.jruby.IOSEnvironment#isAccessible()
     */
    public boolean isAccessible(IRuby runtime) {
        return getSystemGetenvMethod() != null;
    }

    /* (non-Javadoc)
     * @see org.jruby.IOSEnvironment#getVariables()
     */
    public Map getVariables(IRuby runtime) {
        Map returnMap = null;
        Method getenvMethod = getSystemGetenvMethod();
        try {
            if (getenvMethod != null) {
                returnMap = (Map) getenvMethod.invoke(null, (Object[]) null);
            }
        } catch (Exception e) {
            new OSEnvironment().handleException(e);
        }
        return returnMap;
    }

    public static void main(String[] args) {
        OSEnvironmentReaderFromJava5SystemGetenv getenv = new OSEnvironmentReaderFromJava5SystemGetenv();
        Map envs = getenv.getVariables(null);
        for (Iterator i = envs.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        System.out.println();
    }
}
