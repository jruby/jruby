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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import jnr.posix.util.Platform;
import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyString;

public class OSEnvironment {

    /**
     * Returns the environment as a hash of Ruby strings.
     *
     * @param runtime
     */
    public static Map<RubyString, RubyString> environmentVariableMap(Ruby runtime) {
        @SuppressWarnings("unchecked")
        Map<String, String> env = runtime.getInstanceConfig().getEnvironment();
        if ( env != null ) return asMapOfRubyStrings(runtime, env);

        if ( Ruby.isSecurityRestricted() ) return Collections.emptyMap();

        return asMapOfRubyStrings(runtime, System.getenv());
    }

    public Map<RubyString, RubyString> getEnvironmentVariableMap(Ruby runtime) {
        Map envMap = OSEnvironment.environmentVariableMap(runtime);
        return envMap == Collections.EMPTY_MAP ? new HashMap(4) : envMap;
    }

    public static Map<String, String> propertiesToStringMap(Properties properties) {
        Map<String, String> map = new HashMap<String, String>();
        for (Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
                map.put((String) entry.getKey(), (String) entry.getValue());
            }
        }
        return map;
    }

    /**
    * Returns java system properties as a Map<RubyString,RubyString>.
     * @param runtime
     * @return the java system properties as a Map<RubyString,RubyString>.
     */
    public static Map<RubyString, RubyString> systemPropertiesMap(Ruby runtime) {
        if ( Ruby.isSecurityRestricted() ) return Collections.emptyMap();
        return asMapOfRubyStrings(runtime, (Properties) System.getProperties().clone());
    }

    public Map<RubyString, RubyString> getSystemPropertiesMap(Ruby runtime) {
        Map sysMap = OSEnvironment.systemPropertiesMap(runtime);
        return sysMap == Collections.EMPTY_MAP ? new HashMap(4) : sysMap;
    }

    private static Map<RubyString, RubyString> asMapOfRubyStrings(final Ruby runtime, final Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        final Map<RubyString, RubyString> rubyMap = new HashMap(map.size() + 2);
        Encoding keyEncoding = runtime.getEncodingService().getLocaleEncoding();

        // On Windows, map doesn't have corresponding keys for these
        if (Platform.IS_WINDOWS) {
            // these may be null when in a restricted environment (JRUBY-6514)
            String home = SafePropertyAccessor.getProperty("user.home");
            String user = SafePropertyAccessor.getProperty("user.name");
            putRubyKeyValuePair(runtime, rubyMap, "HOME", keyEncoding, home == null ? "/" : home, keyEncoding);
            putRubyKeyValuePair(runtime, rubyMap, "USER", keyEncoding, user == null ? "" : user, keyEncoding);
        }

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object val = entry.getKey();

            if ( ! (val instanceof String) ) continue; // Java devs can stuff non-string objects into env
            final String key = (String) val;

            if (Platform.IS_WINDOWS && key.startsWith("=")) continue;

            val = entry.getValue();
            if ( ! (val instanceof String) ) continue; // Java devs can stuff non-string objects into env

            // Ensure PATH is encoded like filesystem
            Encoding valueEncoding = keyEncoding;
            if ( org.jruby.platform.Platform.IS_WINDOWS ?
                    key.toString().equalsIgnoreCase("PATH") :
                    key.toString().equals("PATH") ) {
                valueEncoding = runtime.getEncodingService().getFileSystemEncoding();
            }

            putRubyKeyValuePair(runtime, rubyMap, key, keyEncoding, (String) val, valueEncoding);
        }

        return rubyMap;
    }

    private static void putRubyKeyValuePair(Ruby runtime,
        final Map<RubyString, RubyString> map,
        String key, Encoding keyEncoding, String value, Encoding valueEncoding) {
        ByteList keyBytes = RubyString.encodeBytelist(key, keyEncoding);
        ByteList valueBytes = RubyString.encodeBytelist(value, valueEncoding);

        RubyString keyString = runtime.newString(keyBytes);
        RubyString valueString = runtime.newString(valueBytes);

        keyString.setFrozen(true);
        keyString.setTaint(true);
        valueString.setFrozen(true);
        valueString.setTaint(true);

        map.put(keyString, valueString);
    }

}
