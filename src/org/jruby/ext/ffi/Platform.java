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
package org.jruby.ext.ffi;

import java.util.HashMap;
import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyModule;

/**
 *
 */
public abstract class Platform {

    public static final Platform getPlatform() {
        return Factory.getInstance().getPlatform();
    }
    private static final String DARWIN = "darwin";
    private static final String WINDOWS = "windows";
    private static final String LINUX = "linux";
    private static final String FREEBSD = "freebsd";
    private static final String OPENBSD = "openbsd";
    private static final String SOLARIS = "solaris";
    
    public static final Map<String, String> OS_NAMES = new HashMap<String, String>() {{
        put("Mac OS X", DARWIN);
    }};
    private static final String getOperatingSystem() {
        String osname = System.getProperty("os.name").toLowerCase();
        for (String s : OS_NAMES.keySet()) {
            if (s.equalsIgnoreCase(osname)) {
                return OS_NAMES.get(s);
            }
        }
        if (osname.startsWith("windows")) {
            return WINDOWS;
        }
        return osname;
    }
    public static final String ARCH = System.getProperty("os.arch");
    public static final String OS = getOperatingSystem();

   
    public static final boolean IS_WINDOWS = OS.equals(WINDOWS);
    
    public static final boolean IS_MAC = OS.equals(DARWIN);
    public static final boolean IS_FREEBSD = OS.equals(FREEBSD);
    public static final boolean IS_OPENBSD = OS.equals(OPENBSD);
    public static final boolean IS_LINUX = OS.equals(LINUX);
    public static final boolean IS_SOLARIS = OS.equals(SOLARIS);
    public static final boolean IS_BSD = IS_MAC || IS_FREEBSD || IS_OPENBSD;

    protected Platform() {
    }

    public void init(Ruby runtime, RubyModule ffi) {
        RubyModule platform = ffi.defineModuleUnder("Platform");
        platform.defineConstant("ADDRESS_SIZE", runtime.newFixnum(addressSize()));
        platform.defineConstant("LONG_SIZE", runtime.newFixnum(longSize()));
        platform.defineConstant("OS", runtime.newString(OS));
        platform.defineConstant("ARCH", runtime.newString(ARCH));
        platform.defineConstant("IS_WINDOWS", runtime.newBoolean(IS_WINDOWS));
        platform.defineConstant("IS_BSD", runtime.newBoolean(IS_BSD));
        platform.defineConstant("IS_FREEBSD", runtime.newBoolean(IS_FREEBSD));
        platform.defineConstant("IS_OPENBSD", runtime.newBoolean(IS_OPENBSD));
        platform.defineConstant("IS_SOLARIS", runtime.newBoolean(IS_SOLARIS));
        platform.defineConstant("IS_LINUX", runtime.newBoolean(IS_LINUX));
        platform.defineConstant("IS_MAC", runtime.newBoolean(IS_MAC));
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
    public abstract int addressSize();

    public abstract int longSize();
}
