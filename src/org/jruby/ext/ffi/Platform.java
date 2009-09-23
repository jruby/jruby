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
    public static final CPU CPU = determineCPU();
    public static final OS OS = determineOS();

    public static final String NAME = CPU + "-" + OS;
    public static final String LIBC = determineLibC();
    public static final String LIBPREFIX = OS == OS.WINDOWS ? "" : "lib";
    public static final String LIBSUFFIX = determineLibExt();
    public static final int BIG_ENDIAN = 4321;
    public static final int LITTLE_ENDIAN = 1234;
    public static final int BYTE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? BIG_ENDIAN : LITTLE_ENDIAN;


    protected final int addressSize, longSize;
    private final long addressMask;
    protected final Pattern libPattern;
    private final int javaVersionMajor;

    public enum OS {
        DARWIN,
        FREEBSD,
        NETBSD,
        OPENBSD,
        LINUX,
        SOLARIS,
        AIX,
        WINDOWS,

        UNKNOWN;
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    public enum CPU {
        I386,
        X86_64,
        POWERPC,
        POWERPC64,
        SPARC,
        SPARCV9,
        S390X,
        UNKNOWN;
        @Override
        public String toString() { return name().toLowerCase(); }
    }

    private static final class SingletonHolder {
        private static final Platform PLATFORM = determinePlatform(determineOS());
    }

    private static final OS determineOS() {
        String osName = System.getProperty("os.name").split(" ")[0].toLowerCase();
        if (osName.startsWith("mac") || osName.startsWith("darwin")) {
            return OS.DARWIN;
        } else if (osName.startsWith("sunos") || osName.startsWith("solaris")) {
            return OS.SOLARIS;
        }
        for (OS os : OS.values()) {
            if (osName.startsWith(os.toString().toLowerCase())) {
                return os;
            }
        }
        return OS.UNKNOWN;
    }

    private static final Platform determinePlatform(OS os) {
        switch (os) {
            case DARWIN:
                return new Darwin();
            case LINUX:
                return new Linux();
            case AIX:
                return new AIX();
            case WINDOWS:
                return new Windows();
            case UNKNOWN:
                return new Unsupported(os);
            default:
                return new Default(os);
        }
    }

    private static final CPU determineCPU() {
        String archString = System.getProperty("os.arch").toLowerCase();
        if ("x86".equals(archString) || "i386".equals(archString) || "i86pc".equals(archString)) {
            return CPU.I386;
        } else if ("x86_64".equals(archString) || "amd64".equals(archString)) {
            return CPU.X86_64;
        } else if ("ppc".equals(archString) || "powerpc".equals(archString)) {
            return CPU.POWERPC;
        } else if ("ppc64".equals(archString)) {
            return CPU.POWERPC64;
        } else if ("sparc".equals(archString)) {
            return CPU.SPARC;
        } else if ("sparcv9".equals(archString)) {
            return CPU.SPARCV9;
        } else if ("s390x".equals(archString)) {
            return CPU.S390X;
	} else {
            return CPU.UNKNOWN;
        }
    }

    private static final String determineLibC() {
        switch (OS) {
            case WINDOWS:
                return "msvcrt";
            case LINUX:
                return "libc.so.6";
            case AIX:
                if (Integer.getInteger("sun.arch.data.model") == 32) {
                    return "libc.a(shr.o)";
                } else {
                    return "libc.a(shr_64.o)";
                }
            default:
                return "c";
        }
    }

    private static final String determineLibExt() {
        switch (OS) {
            case WINDOWS:
                return "dll";
            case AIX:
                return "a";
            case DARWIN:
                return "dylib";
            default:
                return "so";
        }
    }

    protected Platform(OS os) {
        int dataModel = Integer.getInteger("sun.arch.data.model");
        if (dataModel != 32 && dataModel != 64) {
            switch (CPU) {
                case I386:
                case POWERPC:
                case SPARC:
                    dataModel = 32;
                    break;
                case X86_64:
                case POWERPC64:
                case SPARCV9:
                case S390X:
                    dataModel = 64;
                    break;
                default:
                    dataModel = 0;
            }
        }
        addressSize = dataModel;
        addressMask = addressSize == 32 ? 0xffffffffL : 0xffffffffffffffffL;
        longSize = os == OS.WINDOWS ? 32 : addressSize; // Windows is LLP64
        String libpattern = null;
        switch (os) {
            case WINDOWS:
                libpattern = ".*\\.dll$";
                break;
            case DARWIN:
                libpattern = "lib.*\\.(dylib|jnilib)$";
                break;
            case AIX:
                libpattern = "lib.*\\.a$";
                break;
            default:
                libpattern = "lib.*\\.so.*$";
                break;
        }
        libPattern = Pattern.compile(libpattern);
        int version = 5;
        try {
            String versionString = System.getProperty("java.version");
            if (versionString != null) {
                String[] v = versionString.split("\\.");
                version = Integer.valueOf(v[1]);
            }
        } catch (Exception ex) {
            version = 0;
        }
        javaVersionMajor = version;
    }

    /**
     * Gets the current <tt>Platform</tt>
     *
     * @return The current platform.
     */
    public static final Platform getPlatform() {
        return SingletonHolder.PLATFORM;
    }

    /**
     * Gets the current Operating System.
     *
     * @return A <tt>OS</tt> value representing the current Operating System.
     */
    public final OS getOS() {
        return OS;
    }

    /**
     * Gets the current processor architecture the JVM is running on.
     *
     * @return A <tt>CPU</tt> value representing the current processor architecture.
     */
    public final CPU getCPU() {
        return CPU;
    }

    /**
     * Gets the version of the Java Virtual Machine (JVM) jffi is running on.
     *
     * @return A number representing the java version.  e.g. 5 for java 1.5, 6 for java 1.6
     */
    public final int getJavaMajorVersion() {
        return javaVersionMajor;
    }
    public final boolean isBSD() {
        return OS == OS.FREEBSD || OS == OS.OPENBSD || OS == OS.NETBSD || OS == OS.DARWIN;
    }
    public final boolean isUnix() {
        return OS != OS.WINDOWS;
    }
    public final boolean isSupported() {
        return OS != OS.UNKNOWN 
                && CPU != CPU.UNKNOWN
                && (addressSize == 32 || addressSize == 64)
                && javaVersionMajor >= 5;
    }
    public static void createPlatformModule(Ruby runtime, RubyModule ffi) {
        RubyModule module = ffi.defineModuleUnder("Platform");
        Platform platform = Platform.getPlatform();
        OS os = platform.getOS();
        module.defineConstant("ADDRESS_SIZE", runtime.newFixnum(platform.addressSize));
        module.defineConstant("LONG_SIZE", runtime.newFixnum(platform.longSize));
        module.defineConstant("OS", runtime.newString(OS.toString()));
        module.defineConstant("ARCH", runtime.newString(platform.getCPU().toString()));
        module.defineConstant("NAME", runtime.newString(platform.getName()));
        module.defineConstant("IS_WINDOWS", runtime.newBoolean(os == OS.WINDOWS));
        module.defineConstant("IS_BSD", runtime.newBoolean(platform.isBSD()));
        module.defineConstant("IS_FREEBSD", runtime.newBoolean(os == OS.FREEBSD));
        module.defineConstant("IS_OPENBSD", runtime.newBoolean(os == OS.OPENBSD));
        module.defineConstant("IS_SOLARIS", runtime.newBoolean(os == OS.SOLARIS));
        module.defineConstant("IS_LINUX", runtime.newBoolean(os == OS.LINUX));
        module.defineConstant("IS_MAC", runtime.newBoolean(os == OS.DARWIN));
        module.defineConstant("LIBC", runtime.newString(LIBC));
        module.defineConstant("LIBPREFIX", runtime.newString(LIBPREFIX));
        module.defineConstant("LIBSUFFIX", runtime.newString(LIBSUFFIX));
        module.defineConstant("BYTE_ORDER", runtime.newFixnum(BYTE_ORDER));
        module.defineConstant("BIG_ENDIAN", runtime.newFixnum(BIG_ENDIAN));
        module.defineConstant("LITTLE_ENDIAN", runtime.newFixnum(LITTLE_ENDIAN));
        module.defineAnnotatedMethods(Platform.class);
    }
    @JRubyMethod(name = "windows?", module=true)
    public static IRubyObject windows_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(OS == OS.WINDOWS);
    }
    @JRubyMethod(name = "mac?", module=true)
    public static IRubyObject mac_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(OS == OS.DARWIN);
    }
    @JRubyMethod(name = "unix?", module=true)
    public static IRubyObject unix_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(Platform.getPlatform().isUnix());
    }
    @JRubyMethod(name = "bsd?", module=true)
    public static IRubyObject bsd_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(Platform.getPlatform().isBSD());
    }
    @JRubyMethod(name = "linux?", module=true)
    public static IRubyObject linux_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(OS == OS.LINUX);
    }
    @JRubyMethod(name = "solaris?", module=true)
    public static IRubyObject solaris_p(ThreadContext context, IRubyObject recv) {
        return context.getRuntime().newBoolean(OS == OS.SOLARIS);
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
     * Gets the size of a C 'long' on the native platform.
     *
     * @return the size of a long in bits
     */
    public final int longSize() {
        return addressSize;
    }

    /**
     * Gets the size of a C address/pointer on the native platform.
     *
     * @return the size of a pointer in bits
     */
    public final int addressSize() {
        return addressSize;
    }
    
    /**
     * Gets the 32/64bit mask of a C address/pointer on the native platform.
     *
     * @return the size of a pointer in bits
     */
    public final long addressMask() {
        return addressMask;
    }

    /**
     * Gets the name of this <tt>Platform</tt>.
     *
     * @return The name of this platform.
     */
    public String getName() {
        return CPU + "-" + OS;
    }

    public String mapLibraryName(String libName) {
        //
        // A specific version was requested - use as is for search
        //
        if (libPattern.matcher(libName).find()) {
            return libName;
        }
        return System.mapLibraryName(libName);
    }
    private static class Supported extends Platform {
        public Supported(OS os) {
            super(os);
        }
    }
    private static class Unsupported extends Platform {
        public Unsupported(OS os) {
            super(os);
        }
    }
    private static final class Default extends Platform {

        public Default(OS os) {
            super(os);
        }

    }
    /**
     * A {@link Platform} subclass representing the MacOS system.
     */
    private static final class Darwin extends Supported {

        public Darwin() {
            super(OS.DARWIN);
        }

        @Override
        public String mapLibraryName(String libName) {
            //
            // A specific version was requested - use as is for search
            //
            if (libPattern.matcher(libName).find()) {
                return libName;
            }
            return "lib" + libName + ".dylib";
        }
    }

    /**
     * A {@link Platform} subclass representing the Linux operating system.
     */
    private static final class Linux extends Supported {

        public Linux() {
            super(OS.LINUX);
        }

        
        @Override
        public String mapLibraryName(String libName) {
            // Older JDK on linux map 'c' to 'libc.so' which doesn't work
            return "c".equals(libName) || "libc.so".equals(libName)
                    ? "libc.so.6" : super.mapLibraryName(libName);
        }
    }

    /**
     * A {@link Platform} subclass representing the Linux operating system.
     */
    private static final class AIX extends Supported {

        public AIX() {
            super(OS.AIX);
        }


        @Override
        public String mapLibraryName(String libName) {
            return "c".equals(libName) || "libc.so".equals(libName)
                    ? LIBC : super.mapLibraryName(libName);
        }
    }

    /**
     * A {@link Platform} subclass representing the Windows system.
     */
    private static class Windows extends Supported {

        public Windows() {
            super(OS.WINDOWS);
        }
    }
}
