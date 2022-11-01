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
import java.util.Enumeration;
import java.util.List;

import org.jruby.*;
import org.jruby.anno.JRubyMethod;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.javasupport.Java;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.BasicLibraryService;
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

    public void load(Ruby runtime, boolean wrap) {
        RubyModule JRuby = runtime.getOrCreateModule("JRuby");
        RubyModule JRubyUtil = JRuby.defineModuleUnder("Util");
        JRubyUtil.defineAnnotatedMethods(JRubyUtilLibrary.class);
        JRubyUtil.setConstant("SEPARATOR", runtime.newString(org.jruby.util.cli.ArgumentProcessor.SEPARATOR));
        JRubyUtil.setConstant("ON_WINDOWS", runtime.newBoolean(org.jruby.platform.Platform.IS_WINDOWS));
        JRubyUtil.setConstant("ON_SOLARIS", runtime.newBoolean(org.jruby.platform.Platform.IS_SOLARIS));
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
            runtime.getWarnings().warn("ObjectSpace impacts performance. See https://github.com/jruby/jruby/wiki/PerformanceTuning#dont-enable-objectspace");
        }
        runtime.setObjectSpaceEnabled(enabled);
        return runtime.newBoolean(enabled);
    }

    @JRubyMethod(meta = true, name = "native_posix?")
    public static IRubyObject native_posix_p(ThreadContext context, IRubyObject self) {
        return RubyBoolean.newBoolean(context, context.runtime.getPosix().isNative());
    }

    @Deprecated
    public static IRubyObject getClassLoaderResources(IRubyObject recv, IRubyObject name) {
        return class_loader_resources(recv.getRuntime().getCurrentContext(), recv, name);
    }

    /**
     * Loads a (Java) class.
     * @param context
     * @param recv
     * @param name the class name
     * @return Java class (wrapper) or raises a NameError if loading fails or class is not found
     */
    @JRubyMethod(meta = true)
    public static IRubyObject load_java_class(ThreadContext context, IRubyObject recv, IRubyObject name) {
        Class<?> klass = Java.getJavaClass(context.runtime, name.convertToString().asJavaString());
        return Java.getInstance(context.runtime, klass);
    }

    /**
     * @note class_loader_resources alias exists since 9.2
     * @param context
     * @param recv
     * @param args (name, raw: false, path: false)
     * @return an enumerable of class-loader resources
     */ // used from RGs' JRuby defaults (as well as jar_dependencies)
    @JRubyMethod(module = true, name = "class_loader_resources", alias = "classloader_resources", required = 1, optional = 1)
    public static IRubyObject class_loader_resources(ThreadContext context, IRubyObject recv, IRubyObject... args) {
        final Ruby runtime = context.runtime;

        final ClassLoader loader = runtime.getJRubyClassLoader();
        final String name = args[0].convertToString().asJavaString();
        final RubyArray resources = RubyArray.newArray(runtime);

        boolean raw = false, path = false;
        if (args.length > 1 && args[1] instanceof RubyHash) {
            IRubyObject[] values = ArgsUtil.extractKeywordArgs(context, (RubyHash) args[1], "raw", "path");
            raw  = values[0] != null && values[0].isTrue();
            path = values[1] != null && values[1].isTrue();
        }
        
        try {
            Enumeration<URL> e = loader.getResources(name);
            while (e.hasMoreElements()) {
                final URL entry = e.nextElement();
                if (path) {
                    resources.append( RubyString.newString(runtime, getPath(entry)) );
                } else if (raw) {
                    resources.append( Java.getInstance(runtime, entry) );
                } else {
                    resources.append( RubyString.newString(runtime, entry.toString()) ); // toExternalForm
                }
            }
        } catch (IOException ex) {
            throw context.runtime.newIOErrorFromException(ex);
        }
        return resources;
    }

    @JRubyMethod(meta = true) // for RubyGems' JRuby defaults
    public static IRubyObject classpath_launcher(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.runtime;
        String launcher = runtime.getInstanceConfig().getEnvironment().get("RUBY");
        if ( launcher == null ) launcher = ClasspathLauncher.jrubyCommand(runtime);
        return runtime.newString(launcher);
    }

    @JRubyMethod(name = "extra_gem_paths", meta = true) // used from RGs' JRuby defaults
    public static IRubyObject extra_gem_paths(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.runtime;
        final List<String> extraGemPaths = runtime.getInstanceConfig().getExtraGemPaths();
        IRubyObject[] extra_gem_paths = new IRubyObject[extraGemPaths.size()];
        int i = 0; for (String gemPath : extraGemPaths) {
            extra_gem_paths[i++] = runtime.newString(gemPath);
        }
        return RubyArray.newArrayNoCopy(runtime, extra_gem_paths);
    }

    @JRubyMethod(name = "current_directory", meta = true) // used from JRuby::ProcessManager
    public static IRubyObject current_directory(ThreadContext context, IRubyObject recv) {
        final Ruby runtime = context.runtime;
        return runtime.newString(runtime.getCurrentDirectory());
    }

    @JRubyMethod(name = "set_last_exit_status", meta = true) // used from JRuby::ProcessManager
    public static IRubyObject set_last_exit_status(ThreadContext context, IRubyObject recv,
                                                   IRubyObject status, IRubyObject pid) {
        RubyProcess.RubyStatus processStatus = RubyProcess.RubyStatus.newProcessStatus(context.runtime,
                status.convertToInteger().getLongValue(),
                pid.convertToInteger().getLongValue()
        );
        context.setLastExitStatus(processStatus);
        return processStatus;
    }

    // used from jruby/kernel/proc.rb
    @JRubyMethod(meta = true, name = "set_meta_class")
    public static IRubyObject set_meta_class(ThreadContext context, IRubyObject recv, IRubyObject obj, IRubyObject klass) {
        if (!(klass instanceof RubyClass)) {
            klass = klass.getMetaClass();
        }
        ((RubyObject) obj).setMetaClass((RubyClass) klass);
        return context.nil;
    }

    /**
     * Preffered way to boot-up JRuby extensions (available as <code>>JRuby.load_ext</code>).
     * @param context
     * @param recv
     * @param klass
     * @return loading outcome
     */
    @JRubyMethod(module = true, name = { "load_ext" })
    public static IRubyObject load_ext(ThreadContext context, IRubyObject recv, IRubyObject klass) {
        if (klass instanceof RubySymbol) {
            switch(((RubySymbol) klass).asJavaString()) {
                case "string" : CoreExt.loadStringExtensions(context.runtime); return context.tru;
                default : throw context.runtime.newArgumentError(':' + ((RubySymbol) klass).asJavaString());
            }
        }
        return loadExtension(context.runtime, klass.convertToString().toString()) ? context.tru : context.fals;
    }

    private static boolean loadExtension(final Ruby runtime, final String className) {
        Class<?> clazz = Java.getJavaClass(runtime, className);
        // 1. BasicLibraryService interface
        if (BasicLibraryService.class.isAssignableFrom(clazz)) {
            try {
                return ((BasicLibraryService) clazz.getConstructor().newInstance()).basicLoad(runtime);
            } catch (org.jruby.exceptions.Exception e) {
                // propagate Ruby exceptions as-is
                throw e;
            } catch (ReflectiveOperationException e) {
                final RaiseException ex = runtime.newNameError("cannot instantiate (ext) Java class " + className, className, e, true);
                ex.initCause(e); throw ex;
            } catch (Exception e) {
                final RaiseException ex = runtime.newNameError("cannot load (ext) (" + className + ")", (String) null, e, true);
                ex.initCause(e); throw ex;
            }
        }
        // 2 org.jruby.runtime.load.Library
        if (Library.class.isAssignableFrom(clazz)) {
            try {
                ((Library) clazz.getConstructor().newInstance()).load(runtime, false);
                return true;
            } catch (org.jruby.exceptions.Exception e) {
                // propagate Ruby exceptions as-is
                throw e;
            } catch (ReflectiveOperationException e) {
                final RaiseException ex = runtime.newNameError("cannot instantiate (ext) Java class " + className, className, e, true);
                ex.initCause(e);
                throw ex;
            } catch (java.lang.Exception e) {
                final RaiseException ex = runtime.newNameError("cannot load (ext) (" + className + ")", (String) null, e, true);
                ex.initCause(e); throw ex;
            }
        }
        // 3. public static void/boolean load(Ruby runtime) convention
        try {
            Object result = clazz.getMethod("load", Ruby.class).invoke(null, runtime);
            return (result instanceof Boolean) && ! ((Boolean) result).booleanValue() ? false : true;
        } catch (org.jruby.exceptions.Exception e) {
            // propagate Ruby exceptions as-is
            throw e;
        } catch (java.lang.Exception e) {
            final RaiseException ex = runtime.newNameError("cannot load (ext) (" + className + ")", (String) null, e, true);
            ex.initCause(e); throw ex;
        }
    }

    /**
     * Invoke the given block under synchronized lock, using standard Java synchronization.
     *
     * @param context the current context
     * @param recv the JRuby module
     * @param arg the object against which to synchronize
     * @param block the block to execute
     * @return the return value of the block
     */
    @JRubyMethod(name = "synchronized", module = true)
    public static IRubyObject rbSynchronized(ThreadContext context, IRubyObject recv, IRubyObject arg, Block block) {
        synchronized (getSyncObject(arg)) {
            return block.call(context);
        }
    }

    /**
     * Wait for a locked object to notify, as in Object {@link #wait()}.
     *
     * @param context the current context
     * @param recv the JRuby module
     * @param arg the object to wait for
     * @return the object given
     * @throws InterruptedException if interrupted
     */
    @JRubyMethod(module = true)
    public static IRubyObject wait(ThreadContext context, IRubyObject recv, IRubyObject arg) throws InterruptedException {
        Object obj = getSyncObject(arg);

        obj.wait();

        return arg;
    }

    /**
     * Wait for a locked object to notify, as in Object {@link #wait(long)}.
     *
     * The object given must be locked using JRuby.synchronized or a similar Java monitor-based locking mechanism.
     *
     * @param context the current context
     * @param recv the JRuby module
     * @param arg the object to wait for
     * @param timeoutMillis the time in millis to wait (converted using #to_int)
     * @return the object given
     * @throws InterruptedException if interrupted
     */
    @JRubyMethod(module = true)
    public static IRubyObject wait(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject timeoutMillis) throws InterruptedException {
        Object obj = getSyncObject(arg);

        obj.wait(timeoutMillis.convertToInteger().getLongValue());

        return arg;
    }

    /**
     * Wait for a locked object to notify, as in Object {@link #wait(long, int)}.
     *
     * The object given must be locked using JRuby.synchronized or a similar Java monitor-based locking mechanism.
     *
     * @param context the current context
     * @param recv the JRuby module
     * @param arg the object to wait for
     * @param timeoutMillis the time in millis to wait (converted using #to_int)
     * @param timeoutNanos the time in nanos to wait (converted using #to_int, and truncated to a signed 32-bit integer)
     * @return the object given
     * @throws InterruptedException if interrupted
     */
    @JRubyMethod(module = true)
    public static IRubyObject wait(ThreadContext context, IRubyObject recv, IRubyObject arg, IRubyObject timeoutMillis, IRubyObject timeoutNanos) throws InterruptedException {
        Object obj = getSyncObject(arg);

        obj.wait(
                timeoutMillis.convertToInteger().getLongValue(),
                timeoutNanos.convertToInteger().getIntValue());

        return arg;
    }

    /**
     * Notify one waiter on a locked object, as in Object {@link #notify()}
     *
     * The object given must be locked using JRuby.synchronized or a similar Java monitor-based locking mechanism.
     *
     * @param context the current context
     * @param recv the JRuby module
     * @param arg the object to notify
     * @return the object given
     */
    @JRubyMethod(module = true)
    public static IRubyObject notify(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Object obj = getSyncObject(arg);

        obj.notify();
        
        return arg;
    }

    /**
     * Notify all waiters on a locked object, as in Object {@link #notifyAll()}
     *
     * The object given must be locked using JRuby.synchronized or a similar Java monitor-based locking mechanism.
     *
     * @param context the current context
     * @param recv the JRuby module
     * @param arg the object to notify
     * @return the object given
     */
    @JRubyMethod(name = "notify_all", module = true)
    public static IRubyObject notifyAll(ThreadContext context, IRubyObject recv, IRubyObject arg) {
        Object obj = getSyncObject(arg);

        obj.notifyAll();

        return arg;
    }

    private static Object getSyncObject(IRubyObject arg) {
        Object obj = arg;

        if (arg instanceof ConcreteJavaProxy) {
            obj = ((ConcreteJavaProxy) arg).getObject();
        }
        return obj;
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
     * Return a list of files and extensions that JRuby treats as internal (or "built-in"),
     * skipping load path and filesystem search.
     *
     * This was added for Bootsnap in https://github.com/Shopify/bootsnap/issues/162
     */
    @JRubyMethod(module = true)
    @Deprecated
    public static RubyArray internal_libraries(ThreadContext context, IRubyObject self) {
        Ruby runtime = context.runtime;

        runtime.getWarnings().warn("JRuby::Util.internal_libraries is deprecated");

        return runtime.newEmptyArray();
    }
}
