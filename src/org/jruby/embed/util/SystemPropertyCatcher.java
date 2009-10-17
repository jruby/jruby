/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.util;

import java.net.URISyntaxException;
import java.net.URL;
import org.jruby.embed.PropertyName;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.LocalContextProvider;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class SystemPropertyCatcher {
    public static LocalContextScope getScope(LocalContextScope defaultScope) {
        LocalContextScope scope = defaultScope;
        String s = System.getProperty(PropertyName.LOCALCONTEXT_SCOPE.toString());
        if (s == null) {
            return scope;
        }
        if ("singlethread".equalsIgnoreCase(s)) {
            return LocalContextScope.SINGLETHREAD;
        } else if ("singleton".equalsIgnoreCase(s)) {
            return LocalContextScope.SINGLETON;
        } else if ("threadsafe".equalsIgnoreCase(s)) {
            return LocalContextScope.THREADSAFE;
        }
        return scope;
    }

    public static LocalVariableBehavior getBehavior(LocalVariableBehavior defaultBehavior) {
        LocalVariableBehavior behavior = defaultBehavior;
        String s = System.getProperty(PropertyName.LOCALVARIABLE_BEHAVIOR.toString());
        if (s == null) {
            return behavior;
        }
        if ("global".equalsIgnoreCase(s)) {
            return LocalVariableBehavior.GLOBAL;
        } else if ("persistent".equalsIgnoreCase(s)) {
            return LocalVariableBehavior.PERSISTENT;
        } else if ("transient".equalsIgnoreCase(s)) {
            return LocalVariableBehavior.TRANSIENT;
        } else if ("bsf".equalsIgnoreCase(s)) {
            return LocalVariableBehavior.BSF;
        }
        return behavior;
    }

    public static void setConfiguration(ScriptingContainer container) {
        LocalContextProvider provider = container.getProvider();
        RubyInstanceConfig config = provider.getRubyInstanceConfig();
        String s = System.getProperty(PropertyName.COMPILEMODE.toString());
        if (s != null) {
            if ("jit".equalsIgnoreCase(s)) {
                config.setCompileMode(CompileMode.JIT);
            } else if ("force".equalsIgnoreCase(s)) {
                config.setCompileMode(CompileMode.FORCE);
            } else {
                config.setCompileMode(CompileMode.OFF);
            }
        }
        s = System.getProperty(PropertyName.COMPATVERSION.toString());
        if (s != null) {
            if (isRuby19(s)) {
                config.setCompatVersion(CompatVersion.RUBY1_9);
            }
        }
    }

    public static void setJRubyHome(ScriptingContainer container) throws URISyntaxException {
        String jrubyhome = findJRubyHome(container);
        if (jrubyhome != null) {
            container.getProvider().getRubyInstanceConfig().setJRubyHome(jrubyhome);
        }
    }

    private static String findJRubyHome(ScriptingContainer container) throws URISyntaxException {
        String jrubyhome;
        if ((jrubyhome = System.getenv("JRUBY_HOME")) != null) {
            return jrubyhome;
        } else if ((jrubyhome = System.getProperty("jruby.home")) != null) {
            return jrubyhome;
        } else if ((jrubyhome = findFromJar(container)) != null) {
            return jrubyhome;
        } else {
            return null;
        }
    }

    private static String findFromJar(ScriptingContainer container) throws URISyntaxException {
        URL resource = container.getClass().getResource("/META-INF/jruby.home/bin/jruby");
        if (resource == null) {
            return null;
        }
        String location = resource.toURI().getSchemeSpecificPart();
        if (location == null) {
            return null;
        }
        Pattern p = Pattern.compile("jruby\\.home");
        Matcher m = p.matcher(location);
        while(m.find()) {
            location = location.substring(0, m.end());
            return location;
        }
        return null;
    }

    public static boolean isRuby19(String name) {
        Pattern p = Pattern.compile("[jJ]?(r|R)(u|U)(b|B)(y|Y)1[\\._]?9");
        Matcher m = p.matcher(name);
        if (m.matches()) {
            return true;
        } else {
            return false;
        }
    }

    public static String getBaseDir() {
        String baseDir = System.getenv("PWD");
        if (baseDir == null || "/".equals(baseDir)) {
            baseDir = System.getProperty("user.dir");
        }
        return baseDir;
    }
}
