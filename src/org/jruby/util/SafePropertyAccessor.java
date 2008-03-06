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
 * Copyright (C) 2007 Vladimir Sizikov <vsizikov@gmail.com>
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

package org.jruby.util;

/**
 * Utility class to safely access system properties in security-sensitive
 * environments.
 */
public class SafePropertyAccessor {

    /**
     * An extension over <code>Boolean.getBoolean</code> method.
     * Returns true if and only if the system property
     * named by the argument exists and is equal to the string "true".
     * If there is no property with the specified name, or if the specified
     * name is empty or null, or if the access to the property is
     * restricted, then false is returned.
     * @param property The system property name.
     * @return The boolean value of the system property.
     */
    public static boolean getBoolean(String property) {
        try {
            return Boolean.getBoolean(property);
        } catch (SecurityException se) {
            return false;
        }
    }

    /**
     * An extension over <code>Boolean.getBoolean</code> method.
     * Handles security restrictions, and provides default value
     * in case when access to the property is restricted,
     * of when the property does not exist.
     * @param property The system property name.
     * @param defValue The default value.
     * @return The boolean value of the system property,
     *         or the default value.
     */
    public static boolean getBoolean(String property, boolean defValue) {
        try {
            if (System.getProperty(property) != null) {
                return Boolean.getBoolean(property);
            } else {
                return defValue;
            }
        } catch (SecurityException se) {
            return defValue;
        }
    }

    /**
     * An extension over <code>System.getProperty</code> method.
     * Handles security restrictions, and returns <code>null</code>
     * if the access to the property is restricted.
     * @param property The system property name.
     * @return The value of the system property,
     *         or the default value.
     */
    public static String getProperty(String property) {
        return getProperty(property, null);
    }

    /**
     * An extension over <code>System.getProperty</code> method.
     * Handles security restrictions, and returns the default
     * value if the access to the property is restricted.
     * @param property The system property name.
     * @param defValue The default value.
     * @return The value of the system property,
     *         or the default value.
     */
    public static String getProperty(String property, String defValue) {
        try {
            return System.getProperty(property, defValue);
        } catch (SecurityException se) {
            return defValue;
        }
    }

    /**
     * An extension over <code>System.getProperty</code> method
     * that additionally produces an int value.
     * Handles security restrictions, and returns <code>0</code>
     * if the access to the property is restricted.
     * @param property The system property name.
     * @return The value of the system property,
     *         or the default value.
     */
    public static int getInt(String property) {
        return getInt(property, 0);
    }

    /**
     * An extension over <code>System.getProperty</code> method
     * that additionally produces an int value.
     * Handles security restrictions, and returns the default
     * value if the access to the property is restricted.
     * @param property The system property name.
     * @param defValue The default value.
     * @return The value of the system property,
     *         or the default value.
     */
    public static int getInt(String property, int defValue) {
        try {
            return Integer.parseInt(System.getProperty(property, String.valueOf(defValue)));
        } catch (SecurityException se) {
            return defValue;
        }
    }

    /**
     * Returns <code>true</code> if the access to the system property
     * is restricted (i.e., when System.getProperty()
     * throws SecurityException).
     * @param property The system property name.
     * @return <code>true</code> if the access is restricted,
     *         <code>false</code> otherwise.
     */
    public static boolean isSecurityProtected(String property) {
        try {
            System.getProperty(property);
            return false;
        } catch (SecurityException se) {
            return true;
        }
    }
}
