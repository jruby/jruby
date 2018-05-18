/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010 Charles Oliver Nutter <headius@headius.com>
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

package org.jruby.ext.jruby;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import org.jruby.util.ClasspathLauncher;

import static org.jruby.util.URLUtil.getPath;

/**
 * Utilities library for all those methods that don't need the full 'java' library
 * to be loaded. This is done mostly for performance reasons. For example, for those
 * who only need to enable the object space, not loading 'java' might save 200-300ms
 * of startup time, like in case of jirb.
 */
public class JRubyUtilLibrary implements Library {

    // JRuby::Util no longer used by JRuby itself
    public void load(Ruby runtime, boolean wrap) throws IOException {
        RubyModule JRubyUtil = runtime.getOrCreateModule("JRuby").defineModuleUnder("Util");
        JRubyUtil.defineAnnotatedMethods(JRubyUtilLibrary.class);
        JRubyUtil.setConstant("SEPARATOR", runtime.newString(org.jruby.util.cli.ArgumentProcessor.SEPARATOR));
    }

    @JRubyMethod(module = true)
    public static IRubyObject gc(ThreadContext context, IRubyObject recv) {
        System.gc();
        return context.nil;
    }

    @JRubyMethod(name = { "objectspace", "object_space?" }, alias = { "objectspace?" }, module = true)
    public static IRubyObject getObjectSpaceEnabled(IRubyObject recv) {
        final Ruby runtime = recv.getRuntime();
        return RubyBoolean.newBoolean(runtime, runtime.isObjectSpaceEnabled());
    }

    @JRubyMethod(name = { "objectspace=", "object_space=" }, module = true)
    public static IRubyObject setObjectSpaceEnabled(IRubyObject recv, IRubyObject arg) {
        final Ruby runtime = recv.getRuntime();
        boolean enabled = arg.isTrue();
        if (enabled) {
            runtime.getWarnings().warn("ObjectSpace impacts performance. See http://wiki.jruby.org/PerformanceTuning#dont-enable-objectspace");
        }
        runtime.setObjectSpaceEnabled(enabled);
        return runtime.newBoolean(enabled);
    }

    @JRubyMethod(name = "classloader_resources", module = true) // used from RGs' JRuby defaults
    public static IRubyObject getClassLoaderResources(IRubyObject recv, IRubyObject arg) {
        Ruby runtime = recv.getRuntime();
        String resource = arg.convertToString().toString();
        final List<RubyString> urlStrings = new ArrayList<>();
        try {
            Enumeration<URL> urls = runtime.getJRubyClassLoader().getResources(resource);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                String urlString = getPath(url);
                urlStrings.add(runtime.newString(urlString));
            }
            return RubyArray.newArray(runtime, urlStrings);
        }
        catch (IOException ignore) {
            return runtime.newEmptyArray();
        }
    }

    @JRubyMethod(module = true) // for RubyGems' JRuby defaults
    public static IRubyObject classpath_launcher(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.runtime;
        String launcher = runtime.getInstanceConfig().getEnvironment().get("RUBY");
        if ( launcher == null ) launcher = ClasspathLauncher.jrubyCommand(runtime);
        return runtime.newString(launcher);
    }

    @JRubyMethod(name = "extra_gem_paths", module = true) // used from RGs' JRuby defaults
    public static IRubyObject extra_gem_paths(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.runtime;
        final List<String> extraGemPaths = runtime.getInstanceConfig().getExtraGemPaths();
        IRubyObject[] extra_gem_paths = new IRubyObject[extraGemPaths.size()];
        int i = 0; for (String gemPath : extraGemPaths) {
            extra_gem_paths[i++] = runtime.newString(gemPath);
        }
        return RubyArray.newArrayNoCopy(runtime, extra_gem_paths);
    }

    @Deprecated // since 9.2 only loaded with require 'core_ext/string.rb'
    public static class StringUtils {
        public static IRubyObject unseeded_hash(ThreadContext context, IRubyObject recv) {
            return CoreExt.String.unseeded_hash(context, recv);
        }
    }

    /**
     * Provide stats on how many method and constant invalidations have occurred globally.
     *
     * This was added for Pry in https://github.com/jruby/jruby/issues/4384
     */
    @JRubyMethod(name = "cache_stats", module = true)
    public static IRubyObject cache_stats(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;

        RubyHash stat = RubyHash.newHash(runtime);
        stat.op_aset(context, runtime.newSymbol("method_invalidation_count"), runtime.newFixnum(runtime.getCaches().getMethodInvalidationCount()));
        stat.op_aset(context, runtime.newSymbol("constant_invalidation_count"), runtime.newFixnum(runtime.getCaches().getConstantInvalidationCount()));

        return stat;
    }

    /**
     * Return a list of files and extensions that JRuby treats as internal (or "built-in"), skipping load path and
     * filesystem search.
     *
     * This was added for Bootsnap in https://github.com/Shopify/bootsnap/issues/162
     */
    @JRubyMethod(module = true)
    public static RubyArray internal_libraries(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;
        List<String> builtinLibraries = runtime.getLoadService().getBuiltinLibraries();

        IRubyObject[] names = builtinLibraries.stream().map(name -> runtime.newString(name)).toArray(i->new IRubyObject[i]);

        return runtime.newArrayNoCopy(names);
    }
}
