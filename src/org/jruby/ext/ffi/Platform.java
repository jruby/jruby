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

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class Platform {
    public enum OS {
        DARWIN,
        FREEBSD,
        LINUX,
        MAC,
        NETBSD,
        OPENBSD,
        SUNOS,
        WINDOWS,

        UNKNOWN;
    }
    public enum ARCH {
        I386,
        X86_64,
        PPC,
        SPARC,
        SPARCV9,
        UNKNOWN;
    }
    private static final class SingletonHolder {
        private static final Platform INSTANCE = new Platform();
    }
    public static final Platform getPlatform() {
        return SingletonHolder.INSTANCE;
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
    public static final Map<String, String> ARCH_NAMES = new HashMap<String, String>() {{
        put("x86", "i386");
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
    private static final String getArchitecture() {
        String arch = System.getProperty("os.arch").toLowerCase();
        for (String s : ARCH_NAMES.keySet()) {
            if (s.equalsIgnoreCase(arch)) {
                return ARCH_NAMES.get(s);
            }
        }       
        return arch;
    }
    public static final String ARCH = getArchitecture();
    public static final String OS = getOperatingSystem();
    private static final String MACOS_LIBREGEX = "lib.*\\.(dylib|jnilib)$";
    private static final String WIN32_LIBREGEX = ".*\\.dll$";
    private static final String UNIX_LIBREGEX = "lib.*\\.so.*$";
   
    public static final boolean IS_WINDOWS = OS.equals(WINDOWS);
    
    public static final boolean IS_MAC = OS.equals(DARWIN);
    public static final boolean IS_FREEBSD = OS.equals(FREEBSD);
    public static final boolean IS_OPENBSD = OS.equals(OPENBSD);
    public static final boolean IS_LINUX = OS.equals(LINUX);
    public static final boolean IS_SOLARIS = OS.equals(SOLARIS);
    public static final boolean IS_BSD = IS_MAC || IS_FREEBSD || IS_OPENBSD;
    public static final String LIBC = IS_WINDOWS ? "msvcrt" : IS_LINUX ? "libc.so.6" : "c";
    public static final String LIBPREFIX = IS_WINDOWS ? "" : "lib";
    public static final String LIBSUFFIX = IS_WINDOWS ? "dll" : IS_MAC ? "dylib" : "so";
    public static final String NAME = String.format("%s-%s", ARCH, OS);
    public static final int BIG_ENDIAN = 4321;
    public static final int LITTLE_ENDIAN = 1234;
    public static final int BYTE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? BIG_ENDIAN : LITTLE_ENDIAN;


    private final int addressSize, longSize;
    private final Pattern libPattern;
    protected Platform() {
        final int dataModel = Integer.getInteger("sun.arch.data.model");
        if (dataModel != 32 && dataModel != 64) {
            throw new IllegalArgumentException("Unsupported data model");
        }
        addressSize = dataModel;
        longSize = IS_WINDOWS ? 32 : addressSize; // Windows is LLP64
        String libpattern = null;
        if (IS_WINDOWS) {
            libpattern = WIN32_LIBREGEX;
        } else if (IS_MAC) {
            libpattern = MACOS_LIBREGEX;
        } else {
            libpattern = UNIX_LIBREGEX;
        }
        libPattern = Pattern.compile(libpattern);
    }

    public void init(Ruby runtime, RubyModule ffi) {
        RubyModule platform = ffi.defineModuleUnder("Platform");
        platform.defineConstant("ADDRESS_SIZE", runtime.newFixnum(addressSize()));
        platform.defineConstant("LONG_SIZE", runtime.newFixnum(longSize()));
        platform.defineConstant("OS", runtime.newString(OS));
        platform.defineConstant("ARCH", runtime.newString(ARCH));
        platform.defineConstant("NAME", runtime.newString(NAME));
        platform.defineConstant("IS_WINDOWS", runtime.newBoolean(IS_WINDOWS));
        platform.defineConstant("IS_BSD", runtime.newBoolean(IS_BSD));
        platform.defineConstant("IS_FREEBSD", runtime.newBoolean(IS_FREEBSD));
        platform.defineConstant("IS_OPENBSD", runtime.newBoolean(IS_OPENBSD));
        platform.defineConstant("IS_SOLARIS", runtime.newBoolean(IS_SOLARIS));
        platform.defineConstant("IS_LINUX", runtime.newBoolean(IS_LINUX));
        platform.defineConstant("IS_MAC", runtime.newBoolean(IS_MAC));
        platform.defineConstant("LIBC", runtime.newString(LIBC));
        platform.defineConstant("LIBPREFIX", runtime.newString(LIBPREFIX));
        platform.defineConstant("LIBSUFFIX", runtime.newString(LIBSUFFIX));
        platform.defineConstant("BYTE_ORDER", runtime.newFixnum(BYTE_ORDER));
        platform.defineConstant("BIG_ENDIAN", runtime.newFixnum(BIG_ENDIAN));
        platform.defineConstant("LITTLE_ENDIAN", runtime.newFixnum(LITTLE_ENDIAN));
        platform.defineAnnotatedMethods(Platform.class);
    }
    @JRubyMethod(name = "windows?", module=true)
    public static IRubyObject windows_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(IS_WINDOWS);
    }
    @JRubyMethod(name = "mac?", module=true)
    public static IRubyObject mac_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(IS_MAC);
    }
    @JRubyMethod(name = "unix?", module=true)
    public static IRubyObject unix_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(IS_BSD || IS_LINUX || IS_SOLARIS);
    }
    @JRubyMethod(name = "bsd?", module=true)
    public static IRubyObject bsd_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(IS_BSD);
    }
    @JRubyMethod(name = "linux?", module=true)
    public static IRubyObject linux_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(IS_LINUX);
    }
    @JRubyMethod(name = "solaris?", module=true)
    public static IRubyObject solaris_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(IS_SOLARIS);
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
    public int addressSize() {
        return addressSize;
    }

    public int longSize() {
        return longSize;
    }

    public String mapLibraryName(String libName) {
        //
        // A specific version was requested - use as is for search
        //
        if (libPattern.matcher(libName).matches()) {
            return libName;
        }
        return System.mapLibraryName(libName);
    }
}
