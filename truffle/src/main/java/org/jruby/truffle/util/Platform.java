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
 * Copyright (C) 2008 JRuby project
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
package org.jruby.truffle.util;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 *
 */
public class Platform {
    private static final java.util.Locale LOCALE = java.util.Locale.ENGLISH;
    public static final CPU_TYPE CPU = determineCPU();
    public static final OS_TYPE OS = determineOS();

    public static final String LIBPREFIX = OS == OS.WINDOWS ? "" : "lib";
    public static final String LIBSUFFIX = determineLibExt();
    public static final String LIBC = determineLibC();
    
    public static final int BIG_ENDIAN = 4321;
    public static final int LITTLE_ENDIAN = 1234;
    public static final int BYTE_ORDER = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN) ? BIG_ENDIAN : LITTLE_ENDIAN;


    public static final boolean IS_WINDOWS = OS.equals(OS_TYPE.WINDOWS);
    public static final boolean IS_BSD = OS.equals(OS_TYPE.WINDOWS);

    protected final int addressSize, longSize;
    private final long addressMask;
    protected final Pattern libPattern;
    private final int javaVersionMajor;

    public enum OS_TYPE {
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
        public String toString() { return name().toLowerCase(LOCALE); }
    }

    public enum CPU_TYPE {
        I386,
        X86_64,
        POWERPC,
        POWERPC64,
        POWERPC64LE,
        SPARC,
        SPARCV9,
        S390X,
        ARM,
        UNKNOWN;
        @Override
        public String toString() { return name().toLowerCase(LOCALE); }
    }

    private static final class SingletonHolder {
        private static final Platform PLATFORM = determinePlatform(determineOS());
    }

    private static final OS_TYPE determineOS() {
        String osName = System.getProperty("os.name").split(" ")[0];
        if (startsWithIgnoreCase(osName, "mac") || startsWithIgnoreCase(osName, "darwin")) {
            return OS_TYPE.DARWIN;
        } else if (startsWithIgnoreCase(osName, "sunos") || startsWithIgnoreCase(osName, "solaris")) {
            return OS_TYPE.SOLARIS;
        }
        for (OS_TYPE os : OS_TYPE.values()) {
            if (startsWithIgnoreCase(osName, os.toString())) {
                return os;
            }
        }
        return OS_TYPE.UNKNOWN;
    }

    private static final Platform determinePlatform(OS_TYPE os) {
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

    private static final CPU_TYPE determineCPU() {
        String archString = System.getProperty("os.arch").toLowerCase(LOCALE);
        if ("x86".equals(archString) || "i386".equals(archString) || "i86pc".equals(archString)) {
            return CPU.I386;
        } else if ("x86_64".equals(archString) || "amd64".equals(archString)) {
            return CPU.X86_64;
        } else if ("ppc".equals(archString) || "powerpc".equals(archString)) {
            return CPU.POWERPC;
        } else if ("ppc64".equals(archString)) {
            return CPU.POWERPC64;
        } else if ("ppc64le".equals(archString)) {
            return CPU.POWERPC64LE;
        } else if ("sparc".equals(archString)) {
            return CPU.SPARC;
        } else if ("sparcv9".equals(archString)) {
            return CPU.SPARCV9;
        } else if ("s390x".equals(archString)) {
            return CPU.S390X;
        } else if ("arm".equals(archString)) {
            return CPU.ARM;
        } else if ("universal".equals(archString)) {
            // OS X OpenJDK7 builds report "universal" right now
            String bits = System.getProperty("sun.arch.data.model", null);
            if ("32".equals(bits)) {
                System.setProperty("os.arch", "i386");
                return CPU.I386;
            } else if ("64".equals(bits)) {
                System.setProperty("os.arch", "x86_64");
                return CPU.X86_64;
            }
        }
        return CPU.UNKNOWN;
    }

    private static final String determineLibC() {
        switch (OS) {
            case WINDOWS:
                return "msvcrt.dll";
            case LINUX:
                return "libc.so.6";
            case AIX:
                if (Integer.getInteger("sun.arch.data.model") == 32) {
                    return "libc.a(shr.o)";
                } else {
                    return "libc.a(shr_64.o)";
                }
            default:
                return LIBPREFIX + "c." + LIBSUFFIX;
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

    protected Platform(OS_TYPE os) {
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
                case POWERPC64LE:
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
    public final OS_TYPE getOS() {
        return OS;
    }

    /**
     * Gets the current processor architecture the JVM is running on.
     *
     * @return A <tt>CPU</tt> value representing the current processor architecture.
     */
    public final CPU_TYPE getCPU() {
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
        return longSize;
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
        public Supported(OS_TYPE os) {
            super(os);
        }
    }
    private static class Unsupported extends Platform {
        public Unsupported(OS_TYPE os) {
            super(os);
        }
    }
    private static final class Default extends Platform {

        public Default(OS_TYPE os) {
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

    private static boolean startsWithIgnoreCase(String s1, String s2) {
        return s1.startsWith(s2)
            || s1.toUpperCase(LOCALE).startsWith(s2.toUpperCase(LOCALE))
            || s1.toLowerCase(LOCALE).startsWith(s2.toLowerCase(LOCALE));
    }

    public static String getOSName() {
        if (jnr.posix.util.Platform.IS_WINDOWS) {
            return RUBY_WIN32;
        }

        String OSName = jnr.posix.util.Platform.getOSName();
        String theOSName = RUBY_OS_NAMES.get(OSName);

        return theOSName == null ? OSName : theOSName;
    }

    public static String getArchitecture() {
        String architecture = jnr.posix.util.Platform.ARCH;
        if (architecture == null) architecture = "unknown";
        if (architecture.equals("amd64")) architecture = "x86_64";

        return architecture;
    }

    private static final String RUBY_DARWIN = "darwin";
    private static final String RUBY_LINUX = "linux";
    private static final String RUBY_WIN32 = "mswin32";
    private static final String RUBY_SOLARIS = "solaris";
    private static final String RUBY_FREEBSD = "freebsd";
    private static final String RUBY_AIX = "aix";

    /** This is a map from Java's "friendly" OS names to those used by Ruby */
    public static final Map<String, String> RUBY_OS_NAMES = new HashMap<String, String>();
    static {
        RUBY_OS_NAMES.put("Mac OS X", RUBY_DARWIN);
        RUBY_OS_NAMES.put("Darwin", RUBY_DARWIN);
        RUBY_OS_NAMES.put("Linux", RUBY_LINUX);
        RUBY_OS_NAMES.put("Windows 95", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows 98", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows Me", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows NT", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows 2000", RUBY_WIN32);
        // that's what JDK5 produces on Windows Vista
        RUBY_OS_NAMES.put("Windows NT (unknown)", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows XP", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows 2003", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows Vista", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows 7", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows Server 2008", RUBY_WIN32);
        RUBY_OS_NAMES.put("Solaris", RUBY_SOLARIS);
        RUBY_OS_NAMES.put("SunOS", RUBY_SOLARIS);
        RUBY_OS_NAMES.put("FreeBSD", RUBY_FREEBSD);
        RUBY_OS_NAMES.put("AIX", RUBY_AIX);
    }

}
