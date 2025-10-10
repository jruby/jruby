/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009-2012 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed.util;


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyInstanceConfig.CompileMode;
import org.jruby.embed.internal.LocalContextProvider;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.PropertyName;
import org.jruby.embed.ScriptingContainer;
import org.jruby.util.SafePropertyAccessor;

import static org.jruby.util.URLUtil.getPath;

/**
 * Utility methods to retrieve System properties or environment variables to
 * get configuration parameters.
 *
 * @author Yoko Harada &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
public class SystemPropertyCatcher {

    /**
     * Gets a local context scope from System property. If no value is assigned to
     * PropertyName.LOCALCONTEXT_SCOPE, given default value is applied.
     *
     * @param defaultScope a default scope.
     * @return one of three local context scopes.
     */
    public static LocalContextScope getScope(LocalContextScope defaultScope) {
        LocalContextScope scope = defaultScope;
        String s = SafePropertyAccessor.getProperty(PropertyName.LOCALCONTEXT_SCOPE.toString());
        if (s == null) return scope;

        if ("singlethread".equalsIgnoreCase(s)) return LocalContextScope.SINGLETHREAD;
        if ("singleton".equalsIgnoreCase(s)) return LocalContextScope.SINGLETON;
        if ("threadsafe".equalsIgnoreCase(s)) return LocalContextScope.THREADSAFE;
        if ("concurrent".equalsIgnoreCase(s)) return LocalContextScope.CONCURRENT;
        return scope;
    }

    /**
     * Gets a local variable behavior from System property. If no value is assigned to
     * PropertyName.LOCALVARIABLE_BEHAVIOR, given default value is applied.
     *
     * @param defaultBehavior a default local variable behavior
     * @return a local variable behavior
     */
    public static LocalVariableBehavior getBehavior(LocalVariableBehavior defaultBehavior) {
        LocalVariableBehavior behavior = defaultBehavior;
        String s = SafePropertyAccessor.getProperty(PropertyName.LOCALVARIABLE_BEHAVIOR.toString());
        if (s == null) return behavior;

        if ("global".equalsIgnoreCase(s)) return LocalVariableBehavior.GLOBAL;
        if ("persistent".equalsIgnoreCase(s)) return LocalVariableBehavior.PERSISTENT;
        if ("transient".equalsIgnoreCase(s)) return LocalVariableBehavior.TRANSIENT;
        if ("bsf".equalsIgnoreCase(s)) return LocalVariableBehavior.BSF;
        return behavior;
    }

    /**
     * Gets a local variable behavior from System property. If no value is assigned to
     * PropertyName.LOCALVARIABLE_BEHAVIOR, given default value is applied.
     *
     * @param defaultLaziness a default local variable behavior
     * @return a local variable behavior
     */
    public static boolean isLazy(boolean defaultLaziness) {
        boolean lazy = defaultLaziness;
        String s = SafePropertyAccessor.getProperty(PropertyName.LAZINESS.toString());
        if (s == null) return lazy;
        return Boolean.parseBoolean(s);
    }

    /**
     * Sets classloader based on System property. This is only used from
     * JRubyEgnineFactory.
     *
     * @param container ScriptingContainer to be set classloader
     */

    public static void setClassLoader(ScriptingContainer container) {
        String loader = SafePropertyAccessor.getProperty(PropertyName.CLASSLOADER.toString());

        // current should be removed later
        if (loader == null || "container".equals(loader) || "current".equals(loader)) { // default
            container.setClassLoader(container.getClass().getClassLoader());
            return;
        }
        if ("context".equals(loader)) {
            container.setClassLoader(Thread.currentThread().getContextClassLoader());
            return;
        }
        if ("none".equals(loader)) {
            return;
        }
        // if incorrect value is set, no classloader will set by ScriptingContainer.
        // allows RubyInstanceConfig to set whatever preferable
    }

    /**
     * Sets configuration parameters given by System properties. Compile mode and
     * Compat version can be set.
     *
     * @param container ScriptingContainer to be set configurations.
     */
    public static void setConfiguration(ScriptingContainer container) {
        LocalContextProvider provider = container.getProvider();
        RubyInstanceConfig config = provider.getRubyInstanceConfig();
        String mode = SafePropertyAccessor.getProperty(PropertyName.COMPILEMODE.toString());
        if (mode != null) {
            if ("jit".equalsIgnoreCase(mode)) {
                config.setCompileMode(CompileMode.JIT);
            }
            else if ("force".equalsIgnoreCase(mode)) {
                config.setCompileMode(CompileMode.FORCE);
            }
            else {
                config.setCompileMode(CompileMode.OFF);
            }
        }
        // NOTE: no JRuby COMPAT version setting, since it no longer makes sense in 9K !
        // String compat = SafePropertyAccessor.getProperty(PropertyName.COMPATVERSION.toString());
    }

    /**
     * Sets JRuby home if it is given by a JRUBY_HOME environment variable,
     * jruby.home system property, or jury.home in jruby-complete.jar
     *
     * @param container ScriptingContainer to be set jruby home.
     */
    @Deprecated(since = "1.5.0")
    public static void setJRubyHome(ScriptingContainer container) {
        String jrubyhome = findJRubyHome(container);
        if (jrubyhome != null) {
            container.getProvider().getRubyInstanceConfig().setJRubyHome(jrubyhome);
        }
    }

    /**
     * Tries to find JRuby home from the order of JRUBY_HOME environment variable,
     * jruby.home System property, then "/META-INF/jruby.home" if jruby-complete.jar
     * is used.
     *
     * @param instance any instance to get a resource
     * @return JRuby home path if exists, null when failed to find it.
     */
    @Deprecated(since = "9.0.3.0")
    public static String findJRubyHome(Object instance) {
        String jrubyhome;
        if ((jrubyhome = SafePropertyAccessor.getenv("JRUBY_HOME")) != null) {
            return jrubyhome;
        }
        if ((jrubyhome = SafePropertyAccessor.getProperty("jruby.home")) != null) {
            return jrubyhome;
        }
        return "uri:classloader://META-INF/jruby.home";
    }

    @Deprecated(since = "9.0.3.0")
    public static String findFromJar(Object instance) {
        URL resource = instance.getClass().getResource("/META-INF/jruby.home");
        if (resource == null) {
            // on IBM WebSphere getResource for a dir returns null but an actual
            // file if it's there returns e.g. wsjar:file:/opt/IBM/WebSphere/...
            // .../jruby-stdlib-1.6.7.dev.jar!/META-INF/jruby.home/bin/jrubyc
            resource = instance.getClass().getResource("/META-INF/jruby.home/bin/jrubyc");
            if (resource == null) return null;
        }

        String location;
        if (resource.getProtocol().equals("jar")) {
            location = getPath(resource);
            if (!location.startsWith("file:")) {
                // for remote-sourced classpath resources, just use classpath:
                location = "classpath:/META-INF/jruby.home";
            }
            if (location.startsWith("file:") && location.contains(" ")) {
                location = location.replaceAll( " ", "%20" );
            }
        } else {
            location = "classpath:/META-INF/jruby.home";
        }

        // Trim trailing slash. It confuses OSGi containers...
        if (location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }

        return location;
    }

    /**
     * Tries to find load paths for ruby files and/or libraries. This methods
     * sees org.jruby.embed.class.path system property first, then java.class.path.
     *
     * @return a list of load paths.
     */
    @Deprecated(since = "9.0.3.0")
    public static List<String> findLoadPaths() {
        String paths = SafePropertyAccessor.getProperty(PropertyName.CLASSPATH.toString());
        List<String> loadPaths = new ArrayList<String>();
        if (paths == null) {
            paths = SafePropertyAccessor.getProperty("java.class.path");
        }
        if (paths == null) return loadPaths;
        String[] possiblePaths = paths.split(File.pathSeparator);
        String[] prefixes = {"file", "url"};
        for (int i=0; i<possiblePaths.length; i++) {
            int startIndex = i;
            for (int j=0; j < prefixes.length; j++) {
                if (prefixes[j].equals(possiblePaths[i]) && i < possiblePaths.length - 1) {
                    loadPaths.add(possiblePaths[i] + ":" + possiblePaths[++i]);
                    break;
                }
            }
            if (startIndex == i) {
                loadPaths.add(possiblePaths[i]);
            }
        }
        return loadPaths;
    }

    private static boolean matches(final String name, final String pattern) {
        Matcher matcher = Pattern.compile(pattern).matcher( name );
        return matcher.matches();
    }

    /**
     * Returns a possible base directory. PWD environment variables is looked up first,
     * then user.dir System property second. This directory is used as a default value
     * when base directory is not given.
     *
     * @return a base directory.
     */
    public static String getBaseDir() {
        String baseDir = SafePropertyAccessor.getenv("PWD");
        if (baseDir == null || "/".equals(baseDir)) {
            baseDir = SafePropertyAccessor.getProperty("user.dir");
        }
        return baseDir;
    }
}
