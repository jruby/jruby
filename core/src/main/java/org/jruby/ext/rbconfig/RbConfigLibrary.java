/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2006 Nick Sieger
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

package org.jruby.ext.rbconfig;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodings.Config;
import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyModule;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.load.Library;
import org.jruby.util.SafePropertyAccessor;

@JRubyModule(name="Config")
public class RbConfigLibrary implements Library {
    // Ruby's designation for some platforms, minus version numbers in some cases
    private static final String RUBY_DARWIN = "darwin";
    private static final String RUBY_LINUX = "linux";
    private static final String RUBY_WIN32 = "mswin32";
    private static final String RUBY_SOLARIS = "solaris";
    private static final String RUBY_FREEBSD = "freebsd";
    private static final String RUBY_DRAGONFLYBSD = "dragonflybsd";
    private static final String RUBY_AIX = "aix";

    private static String normalizedHome;

    /** This is a map from Java's "friendly" OS names to those used by Ruby */
    public static final Map<String, String> RUBY_OS_NAMES = new HashMap<>(24, 1);
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
        RUBY_OS_NAMES.put("DragonFlyBSD", RUBY_DRAGONFLYBSD);
        RUBY_OS_NAMES.put("AIX", RUBY_AIX);
    }

    public static String getOSName() {
        if (Platform.IS_LINUX) return RUBY_LINUX;
        if (Platform.IS_MAC) return RUBY_DARWIN;
        if (Platform.IS_WINDOWS) return RUBY_WIN32;

        String osName = SafePropertyAccessor.getProperty("os.name");
        String rubyName = RUBY_OS_NAMES.get(osName);
        return rubyName == null ? osName : rubyName;
    }

    public static String getArchitecture() {
        String architecture = Platform.ARCH;
        if (architecture == null) architecture = "unknown";
        if ("amd64".equals(architecture)) architecture = "x86_64";

        return architecture;
    }

    public static String getRuntimeVerStr(Ruby runtime) {
        return Constants.RUBY_MAJOR_VERSION;
    }

    public static String getNormalizedHome(Ruby runtime) {
        normalizedHome = runtime.getJRubyHome();
        if (normalizedHome == null && Ruby.isSecurityRestricted()) {
            normalizedHome = "SECURITY RESTRICTED";
        }
        return normalizedHome;
    }

    public static String getLibDir(Ruby runtime) {
        String libdir = SafePropertyAccessor.getProperty("jruby.lib");
        if (libdir == null) {
            String home = getNormalizedHome(runtime);
            if (home.startsWith("uri:")) {
                libdir = home + "/lib";
            }
            else {
                libdir = newFile(home, "lib").getPath();
            }
        } else {
            try {
            // Our shell scripts pass in non-canonicalized paths, but even if we didn't
            // anyone who did would become unhappy because Ruby apps expect no relative
            // operators in the pathname (rubygems, for example).
                libdir = newFile(libdir).getCanonicalPath();
            }
            catch (IOException e) {
                libdir = newFile(libdir).getAbsolutePath();
            }
        }

        return libdir;
    }

    public static String getVendorDirGeneral(Ruby runtime) {
        // vendorDirGeneral example: /usr/share/jruby/lib/ - commonly the same as libdir
        return newFile(SafePropertyAccessor.getProperty("vendor.dir.general", getLibDir(runtime))).getPath();
    }

    public static String getSiteDirGeneral(Ruby runtime) {
        // siteDirGeneral example: /usr/local/share/jruby/lib/
        return newFile(SafePropertyAccessor.getProperty("site.dir.general", getLibDir(runtime))).getPath();
    }

    public static Boolean isSiteVendorSame(Ruby runtime) {
        return getVendorDirGeneral(runtime).equals(getSiteDirGeneral(runtime));
    }

    public static String getRubygemsDir(Ruby runtime) {
        // used when integrating JRuby with system RubyGems - example: /usr/share/rubygems
        return SafePropertyAccessor.getProperty("vendor.dir.rubygems", null);
    }

    public static String getRubySharedLibDir(Ruby runtime) {
        return newFile(getVendorDirGeneral(runtime), "ruby/shared").getPath();
    }

    public static String getRubyLibDir(Ruby runtime) {
        return getRubyLibDirFor(runtime, "stdlib");
    }

    public static String getRubyLibDirFor(Ruby runtime, String runtimeVerStr) {
        return newFile(getVendorDirGeneral(runtime), String.format("ruby/%s", runtimeVerStr)).getPath();
    }

    public static String getArchDir(Ruby runtime) {
        return getRubyLibDir(runtime);
    }

    public static String getVendorDir(Ruby runtime) {
        return newFile(getRubyLibDir(runtime), "vendor_ruby").getPath();
    }

    public static String getVendorLibDir(Ruby runtime) {
        return getVendorDir(runtime);
    }

    public static String getVendorArchDir(Ruby runtime) {
        return getVendorDir(runtime);
    }

    public static String getSiteDir(Ruby runtime) {
        return newFile(getSiteDirGeneral(runtime), String.format("ruby/%s/site_ruby", getRuntimeVerStr(runtime))).getPath();
    }

    public static String getSiteLibDir(Ruby runtime) {
        return getSiteDir(runtime);
    }

    public static String getSiteArchDir(Ruby runtime) {
        return getSiteDir(runtime);
    }

    public static String getSysConfDir(Ruby runtime) {
        return newFile(getNormalizedHome(runtime), "etc").getPath();
    }

    /**
     * Just enough configuration settings (most don't make sense in Java) to run the rubytests
     * unit tests. The tests use <code>bindir</code>, <code>RUBY_INSTALL_NAME</code> and
     * <code>EXEEXT</code>.
     */
    public void load(Ruby runtime, boolean wrap) {
        ThreadContext context = runtime.getCurrentContext();

        final RubyModule rbConfig = runtime.defineModule("RbConfig");

        normalizedHome = getNormalizedHome(runtime);

        // Ruby installed directory.
        rbConfig.setConstant("TOPDIR", RubyString.newString(runtime, normalizedHome));
        RubyString destDir = RubyString.newEmptyString(runtime);
        // DESTDIR on make install.
        rbConfig.setConstant("DESTDIR", destDir);

        // The hash configurations stored.
        final RubyHash CONFIG = new RubyHash(runtime, 48);

        CONFIG.fastASetCheckString(runtime, runtime.newString("DESTDIR"), destDir);

        String[] versionParts;
        versionParts = Constants.RUBY_VERSION.split("\\.");

        String major = versionParts[0];
        String minor = versionParts[1];
        String teeny = versionParts[2];
        setConfig(context, CONFIG, "MAJOR", major);
        setConfig(context, CONFIG, "MINOR", minor);
        setConfig(context, CONFIG, "TEENY", teeny);
        setConfig(context, CONFIG, "ruby_version", major + '.' + minor + ".0");
        // Rubygems is too specific on host cpu so until we have real need lets default to universal
        //setConfig(CONFIG, "arch", System.getProperty("os.arch") + "-java" + System.getProperty("java.specification.version"));
        setConfig(context, CONFIG, "arch", "universal-java" + System.getProperty("java.specification.version"));

        // Use property for binDir if available, otherwise fall back to common bin default
        String binDir = SafePropertyAccessor.getProperty("jruby.bindir");
        if (binDir == null) {
            binDir = newFile(normalizedHome, "bin").getPath();
        }
        setConfig(context, CONFIG, "bindir", binDir);

        setConfig(context, CONFIG, "RUBY_INSTALL_NAME", jrubyScript());
        setConfig(context, CONFIG, "RUBY_BASE_NAME", jrubyScript());
        setConfig(context, CONFIG, "RUBYW_INSTALL_NAME", Platform.IS_WINDOWS ? "jrubyw.exe" : jrubyScript());
        setConfig(context, CONFIG, "ruby_install_name", jrubyScript());
        setConfig(context, CONFIG, "rubyw_install_name", Platform.IS_WINDOWS ? "jrubyw.exe" : jrubyScript());
        setConfig(context, CONFIG, "SHELL", jrubyShell());
        setConfig(context, CONFIG, "prefix", normalizedHome);
        setConfig(context, CONFIG, "exec_prefix", normalizedHome);

        final String osName = getOSName();
        final String arch = getArchitecture();
        final String vendor = SafePropertyAccessor.getProperty("java.vendor");

        setConfig(context, CONFIG, "host_os", osName);
        setConfig(context, CONFIG, "host_vendor", vendor);
        setConfig(context, CONFIG, "host_cpu", arch);

        String host = String.format("%s-%s-%s", osName, vendor, arch);
        setConfig(context, CONFIG, "host", host);
        setConfig(context, CONFIG, "host_alias", host);

        setConfig(context, CONFIG, "target_os", osName);

        setConfig(context, CONFIG, "target_cpu", arch);

        String jrubyJarFile = "jruby.jar";
        URL jrubyPropertiesUrl = Ruby.getClassLoader().getResource("org/jruby/Ruby.class");
        if (jrubyPropertiesUrl != null) {
            Pattern jarFile = Pattern.compile("jar:file:.*?([a-zA-Z0-9.\\-]+\\.jar)!" + "/org/jruby/Ruby.class");
            Matcher jarMatcher = jarFile.matcher(jrubyPropertiesUrl.toString());
            jarMatcher.find();
            if (jarMatcher.matches()) {
                jrubyJarFile = jarMatcher.group(1);
            }
        }
        setConfig(context, CONFIG, "LIBRUBY", jrubyJarFile);
        setConfig(context, CONFIG, "LIBRUBY_SO", jrubyJarFile);
        setConfig(context, CONFIG, "LIBRUBY_SO", jrubyJarFile);
        setConfig(context, CONFIG, "LIBRUBY_ALIASES", jrubyJarFile);

        setConfig(context, CONFIG, "build", Constants.BUILD);
        setConfig(context, CONFIG, "target", Constants.TARGET);


        String shareDir = newFile(normalizedHome, "share").getPath();
        String includeDir = newFile(normalizedHome, "lib/ruby/include").getPath();

        String vendorDirGeneral = getVendorDirGeneral(runtime);
        String siteDirGeneral = getSiteDirGeneral(runtime);
        String rubySharedLibDir = getRubySharedLibDir(runtime);
        String rubyLibDir = getRubyLibDir(runtime);
        String archDir = getArchDir(runtime);
        String vendorDir = getVendorDir(runtime);
        String vendorLibDir = getVendorLibDir(runtime);
        String vendorArchDir = getVendorArchDir(runtime);
        String siteDir = getSiteDir(runtime);
        String siteLibDir = getSiteLibDir(runtime);
        String siteArchDir = getSiteArchDir(runtime);
        String sysConfDir = getSysConfDir(runtime);

        setConfig(context, CONFIG, "libdir", vendorDirGeneral);
        setConfig(context, CONFIG, "rubylibprefix", vendorDirGeneral + "/ruby");
        setConfig(context, CONFIG, "rubylibdir",     rubyLibDir);
        setConfig(context, CONFIG, "rubysharedlibdir", rubySharedLibDir);
        if (!isSiteVendorSame(runtime)) {
            setConfig(context, CONFIG, "vendordir",      vendorDir);
            setConfig(context, CONFIG, "vendorlibdir",   vendorLibDir);
            setConfig(context, CONFIG, "vendorarchdir",    vendorArchDir);
        }
        setConfig(context, CONFIG, "sitedir",        siteDir);
        setConfig(context, CONFIG, "sitelibdir",     siteLibDir);
        setConfig(context, CONFIG, "sitearchdir",    siteArchDir);
        setConfig(context, CONFIG, "sitearch", "java");
        setConfig(context, CONFIG, "archdir",   archDir);
        setConfig(context, CONFIG, "topdir",   archDir);
        setConfig(context, CONFIG, "includedir",   includeDir);
        setConfig(context, CONFIG, "rubyhdrdir",   includeDir);
        setConfig(context, CONFIG, "configure_args", "");
        setConfig(context, CONFIG, "datadir", shareDir);
        setConfig(context, CONFIG, "mandir", newFile(normalizedHome, "man").getPath());
        setConfig(context, CONFIG, "sysconfdir", sysConfDir);
        setConfig(context, CONFIG, "localstatedir", newFile(normalizedHome, "var").getPath());
        setConfig(context, CONFIG, "DLEXT", "jar");
        if (Platform.IS_WINDOWS) {
            setConfig(context, CONFIG, "RUBY_SO_NAME", ((arch.equals("x86_64")) ? "x64-" : "") + "msvcrt-" + jrubyScript());
        } else {
            setConfig(context, CONFIG, "RUBY_SO_NAME", "ruby");
        }

        final String rubygemsDir = getRubygemsDir(runtime);
        if (rubygemsDir != null) {
            setConfig(context, CONFIG, "rubygemsdir", newFile(rubygemsDir).getPath());
        }

        if (Platform.IS_WINDOWS) {
            setConfig(context, CONFIG, "EXEEXT", ".exe");
        } else {
            setConfig(context, CONFIG, "EXEEXT", "");
        }

        setConfig(context, CONFIG, "ridir", newFile(shareDir, "ri").getPath());

        // These will be used as jruby defaults for rubygems if found
        String gemhome = SafePropertyAccessor.getProperty("jruby.gem.home");
        String gempath = SafePropertyAccessor.getProperty("jruby.gem.path");
        if (gemhome != null) setConfig(context, CONFIG, "default_gem_home", gemhome);
        if (gempath != null) setConfig(context, CONFIG, "default_gem_path", gempath);

        setConfig(context, CONFIG, "joda-time.version", Constants.JODA_TIME_VERSION);
        setConfig(context, CONFIG, "tzdata.version",    Constants.TZDATA_VERSION);

        setConfig(context, CONFIG, "UNICODE_VERSION", Config.UNICODE_VERSION_STRING);
        setConfig(context, CONFIG, "UNICODE_EMOJI_VERSION", Config.UNICODE_EMOJI_VERSION_STRING);

        rbConfig.defineConstant("CONFIG", CONFIG);


        // TODO CONFIG and MAKEFILE_CONFIG seems to be the same Hash in Ruby 2.5
        final RubyHash mkmfHash = new RubyHash(runtime, 64);

        setConfig(context, mkmfHash, "libdir", vendorDirGeneral);
        setConfig(context, mkmfHash, "arch", "java");
        setConfig(context, mkmfHash, "rubylibdir",     rubyLibDir);
        setConfig(context, mkmfHash, "rubysharedlibdir", rubySharedLibDir);
        if (!isSiteVendorSame(runtime)) {
            setConfig(context, mkmfHash, "vendordir",      vendorDir);
            setConfig(context, mkmfHash, "vendorlibdir",   vendorLibDir);
            setConfig(context, mkmfHash, "vendorarchdir",  vendorArchDir);
        }
        setConfig(context, mkmfHash, "sitedir",        siteDir);
        setConfig(context, mkmfHash, "sitelibdir",     siteLibDir);
        setConfig(context, mkmfHash, "sitearchdir",    siteArchDir);
        setConfig(context, mkmfHash, "sitearch", "java");
        setConfig(context, mkmfHash, "archdir",    archDir);
        setConfig(context, mkmfHash, "topdir",    archDir);
        setConfig(context, mkmfHash, "configure_args", "");
        setConfig(context, mkmfHash, "datadir", newFile(normalizedHome, "share").getPath());
        setConfig(context, mkmfHash, "mandir", newFile(normalizedHome, "man").getPath());
        setConfig(context, mkmfHash, "sysconfdir", sysConfDir);
        setConfig(context, mkmfHash, "localstatedir", newFile(normalizedHome, "var").getPath());
        if (rubygemsDir != null) {
            setConfig(context, mkmfHash, "rubygemsdir", newFile(rubygemsDir).getPath());
        }

        setupMakefileConfig(context, mkmfHash);

        rbConfig.defineConstant("MAKEFILE_CONFIG", mkmfHash);

        runtime.getLoadService().load("jruby/kernel/rbconfig.rb", false);
    }

    private static final boolean IS_64_BIT = jnr.posix.util.Platform.IS_64_BIT;

    private static void setupMakefileConfig(ThreadContext context, final RubyHash mkmfHash) {

        RubyHash envHash = (RubyHash) context.runtime.getObject().fetchConstant("ENV");
        String cc = getRubyEnv(envHash, "CC", "cc");
        String cpp = getRubyEnv(envHash, "CPP", "cc -E");
        String cxx = getRubyEnv(envHash, "CXX", "c++");

        String jflags = " -fno-omit-frame-pointer -fno-strict-aliasing ";
        // String oflags = " -O2  -DNDEBUG";
        // String wflags = " -W -Werror -Wall -Wno-unused -Wno-parentheses ";
        // String picflags = true ? "" : " -fPIC -pthread ";
        // String iflags = " -I\"$(JDK_HOME)/include\" -I\"$(JDK_HOME)/include/$(OS)\" -I\"$(BUILD_DIR)\" ";
        // String soflags = true ? "" : " -shared -static-libgcc -mimpure-text -Wl,-O1 ";

        String cflags = jflags + " -fexceptions" /* + picflags */ + " $(cflags)";
        String cppflags = " $(DEFS) $(cppflags)";
        String cxxflags = cflags + " $(cxxflags)";
        String ldflags = ""; // + soflags;
        String dldflags = "";
        String ldsharedflags = " -shared ";

        String archflags = " -m" + (IS_64_BIT ? "64" : "32");

        String hdr_dir = newFile(normalizedHome, "lib/native/include/").getPath();

        // A few platform specific values
        if (Platform.IS_WINDOWS) {
            ldflags += " -L" + newFile(normalizedHome, "lib/native/" + (IS_64_BIT ? "x86_64" : "i386") + "-Windows").getPath();
            ldsharedflags += " $(if $(filter-out -g -g0,$(debugflags)),,-s)";
            dldflags = "-Wl,--enable-auto-image-base,--enable-auto-import $(DEFFILE)";
            archflags += " -march=native -mtune=native";
            setConfig(context, mkmfHash, "DLEXT", "dll");
            setConfig(context, mkmfHash, "EXEEXT", ".exe");
        } else if (Platform.IS_MAC) {
            ldsharedflags = " -dynamic -bundle -undefined dynamic_lookup ";
            cflags = " -DTARGET_RT_MAC_CFM=0 " + cflags;
            archflags = " -arch " + getArchitecture();
            cppflags = " -D_XOPEN_SOURCE -D_DARWIN_C_SOURCE " + cppflags;
            setConfig(context, mkmfHash, "DLEXT", "bundle");
	        setConfig(context, mkmfHash, "EXEEXT", "");
        } else {
            setConfig(context, mkmfHash, "DLEXT", "so");
	        setConfig(context, mkmfHash, "EXEEXT", "");
        }

        String libext = "a";
        String objext = "o";

        setConfig(context, mkmfHash, "configure_args", "");
        setConfig(context, mkmfHash, "CCDLFLAGS", "-fPIC");
        setConfig(context, mkmfHash, "CFLAGS", cflags);
        setConfig(context, mkmfHash, "CPPFLAGS", cppflags);
        setConfig(context, mkmfHash, "CXXFLAGS", cxxflags);
        setConfig(context, mkmfHash, "ARCH_FLAG", archflags);
        setConfig(context, mkmfHash, "LDFLAGS", ldflags);
        setConfig(context, mkmfHash, "DLDFLAGS", dldflags);
        setConfig(context, mkmfHash, "DEFS", "");
        setConfig(context, mkmfHash, "LIBEXT", libext);
        setConfig(context, mkmfHash, "OBJEXT", objext);
        setConfig(context, mkmfHash, "LIBRUBYARG_STATIC", "");
        setConfig(context, mkmfHash, "LIBRUBYARG_SHARED", "");
        setConfig(context, mkmfHash, "LIBS", "");
        setConfig(context, mkmfHash, "DLDLIBS", "");
        setConfig(context, mkmfHash, "ENABLED_SHARED", "");
        setConfig(context, mkmfHash, "LIBRUBY", "");
        setConfig(context, mkmfHash, "LIBRUBY_A", "");
        setConfig(context, mkmfHash, "LIBRUBYARG", "");
        setConfig(context, mkmfHash, "prefix", " "); // This must not be empty for some extconf.rb's to work
        setConfig(context, mkmfHash, "ruby_install_name", jrubyScript());
        setConfig(context, mkmfHash, "LDSHARED", cc + ldsharedflags);
        setConfig(context, mkmfHash, "LDSHAREDXX", cxx + ldsharedflags);
        setConfig(context, mkmfHash, "RUBY_PLATFORM", getOSName());
        setConfig(context, mkmfHash, "RUBY_SO_NAME", "jruby");
        setConfig(context, mkmfHash, "CC", cc);
        setConfig(context, mkmfHash, "CPP", cpp);
        setConfig(context, mkmfHash, "CXX", cxx);
        setConfig(context, mkmfHash, "OUTFLAG", "-o ");
        setConfig(context, mkmfHash, "COUTFLAG", "-o ");
        setConfig(context, mkmfHash, "COMMON_HEADERS", "ruby.h");
        setConfig(context, mkmfHash, "PATH_SEPARATOR", ":");
        setConfig(context, mkmfHash, "INSTALL", "install -c ");
        setConfig(context, mkmfHash, "RM", "rm -f");
        setConfig(context, mkmfHash, "CP", "cp ");
        setConfig(context, mkmfHash, "MAKEDIRS", "mkdir -p ");
        setConfig(context, mkmfHash, "includedir", hdr_dir);
        setConfig(context, mkmfHash, "rubyhdrdir", hdr_dir);
        setConfig(context, mkmfHash, "archdir", hdr_dir);

        context.runtime.getObject().defineConstant("CROSS_COMPILING", context.nil);
    }

    private static void setConfig(ThreadContext context, RubyHash hash, String key, String value) {
        final Ruby runtime = context.runtime;
        hash.fastASetCheckString(runtime, runtime.newString(key), runtime.newString(value));
    }

    public static String jrubyScript() {
        return SafePropertyAccessor.getProperty("jruby.script", "jruby").replace('\\', '/');
    }

    // This differs from MRI where they always return /bin/sh on windows even though that is
    // not going to be a useful value on windows.
    public static String jrubyShell() {
        if (Platform.IS_WINDOWS) {
            // We ignore what the launcher provides since it is hardcoded as 'cmd.exe'.  MRI in all
            // invocations just defers to what COMSPEC returns so we will as well.
            String comspec = SafePropertyAccessor.getenv("COMSPEC");
            // FIXME: Why do we forward slash in rbconfig and not in place which uses it and expects / vs \?
            return comspec == null ? "cmd.exe" : comspec.replace('\\', '/');
        } else {
            return SafePropertyAccessor.getProperty("jruby.shell", "/bin/sh");
        }
    }

    private static String getRubyEnv(RubyHash envHash, String var, String default_value) {
        var = (String) envHash.get(var);
        return var == null ? default_value : var;
    }

    private static File newFile(final String path) {
        return new org.jruby.util.NormalizedFile(path);
    }

    private static File newFile(final String parent, final String child) {
        return new org.jruby.util.NormalizedFile(parent, child);
    }

}
