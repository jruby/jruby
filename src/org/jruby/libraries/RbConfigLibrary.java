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

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.runtime.Constants;
import org.jruby.runtime.load.Library;
import org.jruby.util.NormalizedFile;

public class RbConfigLibrary implements Library {
    /** This is a map from Java's "friendly" OS names to those used by Ruby */
    public static final Map<String, String> OS_NAMES = new HashMap();
    
    static {
        OS_NAMES.put("Mac OS X", "darwin");
        OS_NAMES.put("Darwin", "darwin");
        OS_NAMES.put("Linux", "linux");
    }
    /**
     * Just enough configuration settings (most don't make sense in Java) to run the rubytests
     * unit tests. The tests use <code>bindir</code>, <code>RUBY_INSTALL_NAME</code> and
     * <code>EXEEXT</code>.
     */
    public void load(Ruby runtime, boolean wrap) {
        RubyModule configModule = runtime.defineModule("Config");
        RubyHash configHash = RubyHash.newHash(runtime);
        configModule.defineConstant("CONFIG", configHash);
        runtime.getObject().defineConstant("RbConfig", configModule);

        String[] versionParts = Constants.RUBY_VERSION.split("\\.");
        setConfig(configHash, "MAJOR", versionParts[0]);
        setConfig(configHash, "MINOR", versionParts[1]);
        setConfig(configHash, "TEENY", versionParts[2]);
        setConfig(configHash, "ruby_version", versionParts[0] + '.' + versionParts[1]);
        // Rubygems is too specific on host cpu so until we have real need lets default to universal
        //setConfig(configHash, "arch", System.getProperty("os.arch") + "-java" + System.getProperty("java.specification.version"));
        setConfig(configHash, "arch", "universal-java" + System.getProperty("java.specification.version"));

        String normalizedHome;
        if (Ruby.isSecurityRestricted()) {
            normalizedHome = "SECURITY RESTRICTED";
        } else {
            normalizedHome = new NormalizedFile(runtime.getJRubyHome()).getAbsolutePath();
        }
        setConfig(configHash, "bindir", new NormalizedFile(normalizedHome, "bin").getAbsolutePath());
        setConfig(configHash, "RUBY_INSTALL_NAME", jruby_script());
        setConfig(configHash, "ruby_install_name", jruby_script());
        setConfig(configHash, "SHELL", jruby_shell());
        setConfig(configHash, "prefix", normalizedHome);
        setConfig(configHash, "exec_prefix", normalizedHome);

        String osName = OS_NAMES.get(System.getProperty("os.name"));
        if (osName == null) {
            osName = System.getProperty("os.name");
        }
        setConfig(configHash, "host_os", osName);
        setConfig(configHash, "host_vendor", System.getProperty("java.vendor"));
        setConfig(configHash, "host_cpu", System.getProperty("os.arch"));
        
        setConfig(configHash, "target_os", osName);
        
        setConfig(configHash, "target_cpu", System.getProperty("os.arch"));
        
        String jrubyJarFile = "jruby.jar";
        URL jrubyPropertiesUrl = Ruby.class.getClassLoader().getResource("jruby.properties");
        if (jrubyPropertiesUrl != null) {
            Pattern jarFile = Pattern.compile("jar:file:.*?([a-zA-Z0-9.\\-]+\\.jar)!/jruby.properties");
            Matcher jarMatcher = jarFile.matcher(jrubyPropertiesUrl.toString());
            jarMatcher.find();
            if (jarMatcher.matches()) {
                jrubyJarFile = jarMatcher.group(1);
            }
        }
        setConfig(configHash, "LIBRUBY", jrubyJarFile);
        setConfig(configHash, "LIBRUBY_SO", jrubyJarFile);
        
        setConfig(configHash, "build", Constants.BUILD);
        setConfig(configHash, "target", Constants.TARGET);
        
        String libdir = System.getProperty("jruby.lib");
        if (libdir == null) {
            libdir = new NormalizedFile(normalizedHome, "lib").getAbsolutePath();
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

        setConfig(configHash, "libdir", libdir);
        setConfig(configHash, "rubylibdir",     new NormalizedFile(libdir, "ruby/1.8").getAbsolutePath());
        setConfig(configHash, "sitedir",        new NormalizedFile(libdir, "ruby/site_ruby").getAbsolutePath());
        setConfig(configHash, "sitelibdir",     new NormalizedFile(libdir, "ruby/site_ruby/1.8").getAbsolutePath());
        setConfig(configHash, "sitearchdir",    new NormalizedFile(libdir, "ruby/site_ruby/1.8/java").getAbsolutePath());
        setConfig(configHash, "archdir",    new NormalizedFile(libdir, "ruby/site_ruby/1.8/java").getAbsolutePath());
        setConfig(configHash, "configure_args", "");
        setConfig(configHash, "datadir", new NormalizedFile(normalizedHome, "share").getAbsolutePath());
        setConfig(configHash, "mandir", new NormalizedFile(normalizedHome, "man").getAbsolutePath());
        setConfig(configHash, "sysconfdir", new NormalizedFile(normalizedHome, "etc").getAbsolutePath());
        setConfig(configHash, "localstatedir", new NormalizedFile(normalizedHome, "var").getAbsolutePath());
        setConfig(configHash, "DLEXT", "jar");

        if (isWindows()) {
            setConfig(configHash, "EXEEXT", ".exe");
        } else {
            setConfig(configHash, "EXEEXT", "");
        }
    }

    private static void setConfig(RubyHash configHash, String key, String value) {
        Ruby runtime = configHash.getRuntime();
        configHash.op_aset(runtime.newString(key), runtime.newString(value));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").startsWith("Windows");
    }

    private String jruby_script() {
        return System.getProperty("jruby.script", isWindows() ? "jruby.bat" : "jruby").replace('\\', '/');
    }

    private String jruby_shell() {
        return System.getProperty("jruby.shell", isWindows() ? "cmd.exe" : "/bin/sh").replace('\\', '/');
    }
}
