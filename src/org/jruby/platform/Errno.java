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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.platform;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.jruby.IErrno;

/**
 * Holds the platform specific errno values.
 */
public final class Errno {
    /** The platform errno value => errno name map */
    private static final Map<Integer, String> errnoToName;
    /** The errno name => platform errno value map */
    private static final Map<String, Integer> nameToErrno;
    
    static {
        Map<String, Integer> n2e;
        try {
            n2e = getConstantsMap(Class.forName(Platform.getPlatform().getPackageName() + ".Errno"));
        } catch (ClassNotFoundException ex) {
            n2e = getConstantsFromFields(IErrno.class);
        }
        Map<Integer, String> e2n = new HashMap<Integer, String>(n2e.size());
        for (Map.Entry<String, Integer> entry : n2e.entrySet()) {
            e2n.put(entry.getValue(), entry.getKey());
        }
        errnoToName = Collections.unmodifiableMap(e2n);
        nameToErrno = Collections.unmodifiableMap(n2e);
    }
    /**
     * Gets the platform specific errno value for a POSIX errno name.
     * @param name The name of the errno constant.
     * @return The platform errno value.
     */
    public static int getErrno(String name) {
        Integer errno = nameToErrno.get(name);
        return errno != null ? errno : 0;
    }

    /**
     * Gets the POSIX errno constant name for a platform specific errno value.
     * @param errno The platform errno value to lookup.
     * @return The errno constant name.
     */
    public static String getName(int errno) {
        String name = errnoToName.get(errno);
        return name != null ? name : "unknown";
    }

    /**
     * Gets a collection of all the names of errno constant for the current platform.
     * @return A collection of Strings representing the errno constant names.
     */
    public static Collection<String> names() {
        return nameToErrno.keySet();
    }

    /**
     * Gets a Map of all the errno constant names to values.
     * @return a Map
     */
    public static Map<String, Integer> entries() {
        return nameToErrno;
    }

    /**
     * Loads the errno values from a static field called 'CONSTANTS' in the class.
     * @param errnoClass The class to load errno constants from.
     * @return A map of errno name to errno value.
     */
    private static Map<String, Integer> getConstantsMap(Class errnoClass) {
        try {
            Object constants = errnoClass.getField("CONSTANTS").get(errnoClass);
            return (Map<String, Integer>) constants;
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Loads the errno values from a static field called 'CONSTANTS' in the class.
     * @param errnoClass The class to load errno constants from.
     * @return A map of errno name to errno value.
     */
    private static Map<String, Integer> getConstantsFromFields(Class errnoClass) {
        Map<String, Integer> n2e = new HashMap<String, Integer>();
        Field[] fields = errnoClass.getFields();
        for (int i = 0; i < fields.length; ++i) {
            try {
                n2e.put(fields[i].getName(), fields[i].getInt(errnoClass));
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Non public constant in " + errnoClass.getName(), ex);
            }
        }
        return Collections.unmodifiableMap(n2e);
    }
}
