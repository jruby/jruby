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
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
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

package org.jruby.util;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import jnr.posix.util.Platform;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.jruby.RubyString;

public class OSEnvironment {
    /**
     * Returns the environment as a hash of Ruby strings.
     *
     * @param runtime
     */
    public Map getEnvironmentVariableMap(Ruby runtime) {
        Map envs = null;

        if (runtime.getInstanceConfig().getEnvironment() != null) {
            return getAsMapOfRubyStrings(runtime, runtime.getInstanceConfig().getEnvironment().entrySet());
        }

        // fall back on empty env when security disallows environment var access (like in an applet)
        if (Ruby.isSecurityRestricted()) {
            envs = new HashMap();
        } else {
            Map variables = System.getenv();
            envs = getAsMapOfRubyStrings(runtime, variables.entrySet());
        }

        return envs;

    }

    /**
    * Returns java system properties as a Map<RubyString,RubyString>.
     * @param runtime
     * @return the java system properties as a Map<RubyString,RubyString>.
     */
    public Map getSystemPropertiesMap(Ruby runtime) {
        if (Ruby.isSecurityRestricted()) {
            return new HashMap();
        } else {
            return getAsMapOfRubyStrings(runtime, ((Properties)System.getProperties().clone()).entrySet());
        }
    }

    private static Map getAsMapOfRubyStrings(Ruby runtime, Set<Map.Entry<Object, Object>> entrySet) {
        Map envs = new HashMap();
        Encoding encoding = runtime.getEncodingService().getLocaleEncoding();

        // On Windows, entrySet doesn't have corresponding keys for these
        if (Platform.IS_WINDOWS) {
            // these may be null when in a restricted environment (JRUBY-6514)
            String home = SafePropertyAccessor.getProperty("user.home");
            String user = SafePropertyAccessor.getProperty("user.name");
            addRubyKeyValuePair(runtime, envs, "HOME", home == null ? "/" : home, encoding);
            addRubyKeyValuePair(runtime, envs, "USER", user == null ? "" : user, encoding);
        }

        for (Map.Entry<Object, Object> entry : entrySet) {
            Object tmp = entry.getKey();
            
            if (!(tmp instanceof String)) continue; // Java devs can stuff non-string objects into env
            String key = (String) tmp;
            
            if (Platform.IS_WINDOWS && key.startsWith("=")) continue;
            
            tmp = entry.getValue();
            if (!(tmp instanceof String)) continue; // Java devs can stuff non-string objects into env

            addRubyKeyValuePair(runtime, envs, key, (String) tmp, encoding);
        }

        return envs;
    }
    
    private static void addRubyKeyValuePair(Ruby runtime, Map map, String key, String value, Encoding encoding) {
        ByteList keyBytes = new ByteList(key.getBytes(), encoding);
        ByteList valueBytes = new ByteList(value.getBytes(), encoding);
        
        RubyString keyString = runtime.newString(keyBytes);
        RubyString valueString = runtime.newString(valueBytes);
        
        keyString.setFrozen(true);
        valueString.setFrozen(true);

        map.put(keyString, valueString);
    }
}
