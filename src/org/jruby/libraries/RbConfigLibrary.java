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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter
 * Copyright (C) 2006 Nick Sieger
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
package org.jruby.libraries;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.CompatVersion;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.posix.util.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.load.Library;
import org.jruby.util.NormalizedFile;
import org.jruby.util.SafePropertyAccessor;
import org.jruby.anno.JRubyModule;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

@JRubyModule(name="Config")
public class RbConfigLibrary implements Library {
    // Ruby's designation for some platforms, minus version numbers in some cases
    private static final String RUBY_DARWIN = "darwin";
    private static final String RUBY_LINUX = "linux";
    private static final String RUBY_WIN32 = "mswin32";
    private static final String RUBY_SOLARIS = "solaris";
    private static final String RUBY_FREEBSD = "freebsd";
    private static final String RUBY_AIX = "aix";
   
    private static String normalizedHome;
    
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
    
    public static String getOSName() {
        if (Platform.IS_WINDOWS) {
            return RUBY_WIN32;
        }
        
        String OSName = Platform.getOSName();
        String theOSName = RUBY_OS_NAMES.get(OSName);
        
        return theOSName == null ? OSName : theOSName;
    }

    public static String getArchitecture() {
        String architecture = Platform.ARCH;
        if (architecture == null) architecture = "unknown";
        if (architecture.equals("amd64")) architecture = "x86_64";
        
        return architecture;
    }
    /**
     * Just enough configuration settings (most don't make sense in Java) to run the rubytests
     * unit tests. The tests use <code>bindir</code>, <code>RUBY_INSTALL_NAME</code> and
     * <code>EXEEXT</code>.
     */
    public void load(Ruby runtime, boolean wrap) {
        RubyModule configModule = runtime.defineModule("Config");
        
        configModule.defineAnnotatedMethods(RbConfigLibrary.class);
        
        RubyHash configHash = RubyHash.newHash(runtime);
        configModule.defineConstant("CONFIG", configHash);
        runtime.getObject().defineConstant("RbConfig", configModule);

        String[] versionParts;
        if (runtime.is1_9()) {
            versionParts = Constants.RUBY1_9_VERSION.split("\\.");
        } else {
            versionParts = Constants.RUBY_VERSION.split("\\.");
        }
        
        setConfig(configHash, "MAJOR", versionParts[0]);
        setConfig(configHash, "MINOR", versionParts[1]);
        setConfig(configHash, "TEENY", versionParts[2]);
        setConfig(configHash, "ruby_version", versionParts[0] + '.' + versionParts[1]);
        // Rubygems is too specific on host cpu so until we have real need lets default to universal
        //setConfig(configHash, "arch", System.getProperty("os.arch") + "-java" + System.getProperty("java.specification.version"));
        setConfig(configHash, "arch", "universal-java" + System.getProperty("java.specification.version"));

        normalizedHome = runtime.getJRubyHome();
        if ((normalizedHome == null) && Ruby.isSecurityRestricted()) {
            normalizedHome = "SECURITY RESTRICTED";
        }

        // Use property for binDir if available, otherwise fall back to common bin default
        String binDir = SafePropertyAccessor.getProperty("jruby.bindir");
        if (binDir == null) {
            binDir = new NormalizedFile(normalizedHome, "bin").getPath();
        }
        setConfig(configHash, "bindir", binDir);

        setConfig(configHash, "RUBY_INSTALL_NAME", jrubyScript());
        setConfig(configHash, "ruby_install_name", jrubyScript());
        setConfig(configHash, "SHELL", jrubyShell());
        setConfig(configHash, "prefix", normalizedHome);
        setConfig(configHash, "exec_prefix", normalizedHome);

        setConfig(configHash, "host_os", getOSName());
        setConfig(configHash, "host_vendor", System.getProperty("java.vendor"));
        setConfig(configHash, "host_cpu", getArchitecture());
        
        setConfig(configHash, "target_os", getOSName());
        
        setConfig(configHash, "target_cpu", getArchitecture());
        
        String jrubyJarFile = "jruby.jar";
        URL jrubyPropertiesUrl = Ruby.getClassLoader().getResource("/org/jruby/Ruby.class");
        if (jrubyPropertiesUrl != null) {
            Pattern jarFile = Pattern.compile("jar:file:.*?([a-zA-Z0-9.\\-]+\\.jar)!" + "/org/jruby/Ruby.class");
            Matcher jarMatcher = jarFile.matcher(jrubyPropertiesUrl.toString());
            jarMatcher.find();
            if (jarMatcher.matches()) {
                jrubyJarFile = jarMatcher.group(1);
            }
        }
        setConfig(configHash, "LIBRUBY", jrubyJarFile);
        setConfig(configHash, "LIBRUBY_SO", jrubyJarFile);
        setConfig(configHash, "LIBRUBY_SO", jrubyJarFile);
        setConfig(configHash, "LIBRUBY_ALIASES", jrubyJarFile);
        
        setConfig(configHash, "build", Constants.BUILD);
        setConfig(configHash, "target", Constants.TARGET);
        
        String libdir = SafePropertyAccessor.getProperty("jruby.lib");
        if (libdir == null) {
            libdir = new NormalizedFile(normalizedHome, "lib").getPath();
        } else {
            try {
            // Our shell scripts pass in non-canonicalized paths, but even if we didn't
            // anyone who did would become unhappy because Ruby apps expect no relative
            // operators in the pathname (rubygems, for example).
                libdir = new NormalizedFile(libdir).getCanonicalPath();
            } catch (IOException e) {
                libdir = new NormalizedFile(libdir).getAbsolutePath();
            }
        }
        String rubyLibDir = new NormalizedFile(libdir, "ruby/1.8").getPath();
        String siteDir = new NormalizedFile(libdir, "ruby/site_ruby").getPath();
        String siteLibDir = new NormalizedFile(libdir, "ruby/site_ruby/1.8").getPath();
        String siteArchDir = new NormalizedFile(libdir, "ruby/site_ruby/1.8/java").getPath();
        String archDir = new NormalizedFile(libdir, "ruby/1.8/java").getPath();
        String shareDir = new NormalizedFile(normalizedHome, "share").getPath();
        String includeDir = new NormalizedFile(normalizedHome, "lib/native/" + getOSName()).getPath();

        setConfig(configHash, "libdir", libdir);
        if (runtime.is1_9()) setConfig(configHash, "rubylibprefix",     libdir + "/ruby");
        setConfig(configHash, "rubylibdir",     rubyLibDir);
        setConfig(configHash, "sitedir",        siteDir);
        setConfig(configHash, "sitelibdir",     siteLibDir);
        setConfig(configHash, "sitearch", "java");
        setConfig(configHash, "sitearchdir",    siteArchDir);
        setConfig(configHash, "archdir",   archDir);
        setConfig(configHash, "topdir",   archDir);
        setConfig(configHash, "includedir",   includeDir);
        setConfig(configHash, "configure_args", "");
        setConfig(configHash, "datadir", shareDir);
        setConfig(configHash, "mandir", new NormalizedFile(normalizedHome, "man").getPath());
        setConfig(configHash, "sysconfdir", new NormalizedFile(normalizedHome, "etc").getPath());
        setConfig(configHash, "localstatedir", new NormalizedFile(normalizedHome, "var").getPath());
        setConfig(configHash, "DLEXT", "jar");

        if (Platform.IS_WINDOWS) {
            setConfig(configHash, "EXEEXT", ".exe");
        } else {
            setConfig(configHash, "EXEEXT", "");
        }

        if (runtime.is1_9()) {
            setConfig(configHash, "ridir", new NormalizedFile(shareDir, "ri").getPath());
        }

        // These will be used as jruby defaults for rubygems if found
        String gemhome = SafePropertyAccessor.getProperty("jruby.gem.home");
        String gempath = SafePropertyAccessor.getProperty("jruby.gem.path");
        if (gemhome != null) setConfig(configHash, "default_gem_home", gemhome);
        if (gempath != null) setConfig(configHash, "default_gem_path", gempath);
        
        setConfig(configHash, "joda-time.version", Constants.JODA_TIME_VERSION);
        setConfig(configHash, "tzdata.version",    Constants.TZDATA_VERSION);
        
        RubyHash mkmfHash = RubyHash.newHash(runtime);
        

        setConfig(mkmfHash, "libdir", libdir);
        setConfig(mkmfHash, "arch", "java");
        setConfig(mkmfHash, "rubylibdir",     rubyLibDir);
        setConfig(mkmfHash, "sitedir",        siteDir);
        setConfig(mkmfHash, "sitelibdir",     siteLibDir);
        setConfig(mkmfHash, "sitearch", "java");
        setConfig(mkmfHash, "sitearchdir",    siteArchDir);
        setConfig(mkmfHash, "archdir",    archDir);
        setConfig(mkmfHash, "topdir",    archDir);
        setConfig(mkmfHash, "configure_args", "");
        setConfig(mkmfHash, "datadir", new NormalizedFile(normalizedHome, "share").getPath());
        setConfig(mkmfHash, "mandir", new NormalizedFile(normalizedHome, "man").getPath());
        setConfig(mkmfHash, "sysconfdir", new NormalizedFile(normalizedHome, "etc").getPath());
        setConfig(mkmfHash, "localstatedir", new NormalizedFile(normalizedHome, "var").getPath());
        
        setupMakefileConfig(configModule, mkmfHash);
    }
    
    private static void setupMakefileConfig(RubyModule configModule, RubyHash mkmfHash) {
        Ruby ruby = configModule.getRuntime();

        RubyHash envHash = (RubyHash) ruby.getObject().fastFetchConstant("ENV".intern());
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

        String archflags = " -m" + (Platform.IS_64_BIT ? "64" : "32");

        String hdr_dir = new NormalizedFile(normalizedHome, "lib/native/include/").getPath();

        // A few platform specific values
        if (Platform.IS_WINDOWS) {
            ldflags += " -L" + new NormalizedFile(normalizedHome, "lib/native/" + (Platform.IS_64_BIT ? "x86_64" : "i386") + "-Windows").getPath();
            ldflags += " -ljruby-cext";
            ldsharedflags += " $(if $(filter-out -g -g0,$(debugflags)),,-s)";
            dldflags = "-Wl,--enable-auto-image-base,--enable-auto-import $(DEFFILE)";
            archflags += " -march=native -mtune=native";
            setConfig(mkmfHash, "DLEXT", "dll");
        } else if (Platform.IS_MAC) {
            ldsharedflags = " -dynamic -bundle -undefined dynamic_lookup ";
            cflags = " -fPIC -DTARGET_RT_MAC_CFM=0 " + cflags;
            ldflags += " -bundle -framework JavaVM -Wl,-syslibroot,$(SDKROOT) -mmacosx-version-min=10.5 ";
            archflags = " -arch " + Platform.ARCH;
            cppflags = " -D_XOPEN_SOURCE -D_DARWIN_C_SOURCE " + cppflags;
            setConfig(mkmfHash, "DLEXT", "bundle");
        } else {
            cflags = " -fPIC " + cflags;
            setConfig(mkmfHash, "DLEXT", "so");
        }

        String libext = "a";
        String objext = "o";
        
        setConfig(mkmfHash, "configure_args", "");
        setConfig(mkmfHash, "CFLAGS", cflags);
        setConfig(mkmfHash, "CPPFLAGS", cppflags);
        setConfig(mkmfHash, "CXXFLAGS", cxxflags);
        setConfig(mkmfHash, "ARCH_FLAG", archflags);
        setConfig(mkmfHash, "LDFLAGS", ldflags);
        setConfig(mkmfHash, "DLDFLAGS", dldflags);
        setConfig(mkmfHash, "DEFS", "");
        setConfig(mkmfHash, "LIBEXT", libext);
        setConfig(mkmfHash, "OBJEXT", objext);
        setConfig(mkmfHash, "LIBRUBYARG_STATIC", "");
        setConfig(mkmfHash, "LIBRUBYARG_SHARED", "");
        setConfig(mkmfHash, "LIBS", "");
        setConfig(mkmfHash, "DLDLIBS", "");
        setConfig(mkmfHash, "ENABLED_SHARED", "");
        setConfig(mkmfHash, "LIBRUBY", "");
        setConfig(mkmfHash, "LIBRUBY_A", "");
        setConfig(mkmfHash, "LIBRUBYARG", "");
        setConfig(mkmfHash, "prefix", " "); // This must not be empty for some extconf.rb's to work
        setConfig(mkmfHash, "ruby_install_name", jrubyScript());
        setConfig(mkmfHash, "LDSHARED", cc + ldsharedflags);
        setConfig(mkmfHash, "LDSHAREDXX", cxx + ldsharedflags);
        setConfig(mkmfHash, "RUBY_PLATFORM", getOSName());
        setConfig(mkmfHash, "CC", cc);
        setConfig(mkmfHash, "CPP", cpp);
        setConfig(mkmfHash, "CXX", cxx);
        setConfig(mkmfHash, "OUTFLAG", "-o ");
        setConfig(mkmfHash, "COMMON_HEADERS", "ruby.h");
        setConfig(mkmfHash, "PATH_SEPARATOR", ":");
        setConfig(mkmfHash, "INSTALL", "install -c ");
        setConfig(mkmfHash, "RM", "rm -f");
        setConfig(mkmfHash, "CP", "cp ");
        setConfig(mkmfHash, "MAKEDIRS", "mkdir -p ");
        setConfig(mkmfHash, "includedir", hdr_dir);
        setConfig(mkmfHash, "rubyhdrdir", hdr_dir);
        setConfig(mkmfHash, "archdir", hdr_dir);
        
        ruby.getObject().defineConstant("CROSS_COMPILING", ruby.getNil());
        
        configModule.defineConstant("MAKEFILE_CONFIG", mkmfHash);
    }

    private static void setConfig(RubyHash hash, String key, String value) {
        Ruby runtime = hash.getRuntime();
        hash.op_aset(runtime.getCurrentContext(), runtime.newString(key), runtime.newString(value));
    }

    public static String jrubyScript() {
        return SafePropertyAccessor.getProperty("jruby.script", "jruby").replace('\\', '/');
    }

    // TODO: note lack of command.com support for Win 9x...
    public static String jrubyShell() {
        return SafePropertyAccessor.getProperty("jruby.shell", Platform.IS_WINDOWS ? "cmd.exe" : "/bin/sh").replace('\\', '/');
    }

    @JRubyMethod(name = "ruby", module = true, compat = CompatVersion.RUBY1_9)
    public static IRubyObject ruby(ThreadContext context, IRubyObject recv) {
        Ruby runtime = context.getRuntime();
        RubyHash configHash = (RubyHash) runtime.getModule("Config").getConstant("CONFIG");

        IRubyObject bindir            = configHash.op_aref(context, runtime.newString("bindir"));
        IRubyObject ruby_install_name = configHash.op_aref(context, runtime.newString("ruby_install_name"));
        IRubyObject exeext            = configHash.op_aref(context, runtime.newString("EXEEXT"));

        return RuntimeHelpers.invoke(context, runtime.getClass("File"), "join", bindir, ruby_install_name.callMethod(context, "+", exeext));
    }

    private static String getRubyEnv(RubyHash envHash, String var, String default_value) {
        var = (String) envHash.get(var);
        return var == null ? default_value : var;
    }
}
