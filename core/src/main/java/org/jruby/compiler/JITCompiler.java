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
 * Copyright (C) 2006-2008 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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

package org.jruby.compiler;


import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.ClassDefiningJRubyClassLoader;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.WeakValuedMap;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.jruby.api.Access.instanceConfig;

public class JITCompiler implements JITCompilerMBean {
    private static final Logger LOG = LoggerFactory.getLogger(JITCompiler.class);

    public static final String RUBY_JIT_PREFIX = "rubyjit";

    final JITCounts counts = new JITCounts();
    private final ExecutorService executor;

    final Ruby runtime;
    final RubyInstanceConfig config;

    final Map<String, ClassDefiningClassLoader> loaderMap; // weak valued map

    public JITCompiler(Ruby runtime) {
        this.runtime = runtime;
        this.config = runtime.getInstanceConfig();

        this.executor = new ThreadPoolExecutor(
                0, // don't start threads until needed
                2, // two max
                60, // stop then if no jitting for 60 seconds
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new DaemonThreadFactory("Ruby-" + runtime.getRuntimeNumber() + "-JIT", Thread.MIN_PRIORITY));

        switch (Options.JIT_LOADER_MODE.load()) {
            case SHARED:
                this.loaderMap = new SharedClassLoaderMap(new ClassDefiningJRubyClassLoader(runtime.getJRubyClassLoader()));
                break;
            case SHARED_SOURCE:
                this.loaderMap = new WeakValuedMap<>();
                break;
            case UNIQUE:
            default:
                this.loaderMap = null;
                break;
        }
    }

    public long getSuccessCount() {
        return counts.successCount.get();
    }

    public long getCompileCount() {
        return counts.compiledCount.get();
    }

    public long getFailCount() {
        return counts.failCount.get();
    }

    public long getCompileTime() {
        return counts.compileTime.get() / 1000;
    }

    public long getAbandonCount() {
        return counts.abandonCount.get();
    }

    public long getCodeSize() {
        return counts.codeSize.get();
    }

    public long getCodeAverageSize() {
        return counts.codeAverageSize.get();
    }

    public long getCompileTimeAverage() {
        return counts.compileTimeAverage.get() / 1000;
    }

    public long getCodeLargestSize() {
        return counts.codeLargestSize.get();
    }

    public long getIRSize() {
        return counts.irSize.get();
    }

    public long getIRAverageSize() {
        return counts.irAverageSize.get();
    }

    public long getIRLargestSize() {
        return counts.irLargestSize.get();
    }

    public String[] getFrameAwareMethods() {
        String[] frameAwareMethods = MethodIndex.FRAME_AWARE_METHODS.toArray(new String[0]);
        Arrays.sort(frameAwareMethods);
        return frameAwareMethods;
    }

    public String[] getScopeAwareMethods() {
        String[] scopeAwareMethods = MethodIndex.SCOPE_AWARE_METHODS.toArray(new String[0]);
        Arrays.sort(scopeAwareMethods);
        return scopeAwareMethods;
    }

    public void tearDown() {
        shutdown();
    }

    /**
     * Shut down this JIT compiler and its resources. No more JIT jobs will be submitted for optimization.
     */
    public void shutdown() {
        try {
            executor.shutdown();
        } catch (SecurityException se) {
            // ignore, can't shut down executor
        }
    }

    /**
     * Return the shutdown status of the JIT compiler.
     *
     * @return true if already shut down, false otherwise
     */
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    public Runnable getTaskFor(ThreadContext context, Compilable method) {
        if (method instanceof MixedModeIRMethod) {
            return new MethodJITTask(this, (MixedModeIRMethod) method, method.getOwnerName());
        } else if (method instanceof MixedModeIRBlockBody) {
            return new BlockJITTask(this, (MixedModeIRBlockBody) method, method.getOwnerName());
        } else if (method instanceof CompiledIRMethod) {
            return new MethodCompiledJITTask(this, (CompiledIRMethod) method, method.getOwnerName());
        }

        return new FullBuildTask(this, method);
    }

    public void buildThresholdReached(ThreadContext context, final Compilable method) {
        final RubyInstanceConfig config = instanceConfig(context);

        final Runnable task = getTaskFor(context, method);

        if (task instanceof Task && config.getJitMax() >= 0 && config.getJitMax() < getSuccessCount()) {
            if (config.isJitLogging()) {
                JITCompiler.log(method, method.getName(), "skipping: jit.max threshold reached");
            }
            return;
        }

        try {
            if (config.getJitBackground() && config.getJitThreshold() > 0) {
                try {
                    executor.submit(task);
                } catch (RejectedExecutionException ree) {
                    // failed to submit, just run it directly
                    task.run();
                }
            } else {
                // Because are non-asynchonously build if the JIT threshold happens to be 0 we will have no ic yet.
                method.ensureInstrsReady();
                // just run directly
                task.run();
            }
        } catch (RaiseException e) {
            throw e;
        } catch (Exception e) {
            throw new NotCompilableException(e);
        }
    }

    static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup().in(Ruby.class);

    public static String getHashForString(String str) {
        return getHashForBytes(RubyEncoding.encodeUTF8(str));
    }

    public static String getHashForBytes(byte[] bytes) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte aByte : sha1.digest()) {
                builder.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
            }
            return builder.toString().toUpperCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }

    ClassDefiningClassLoader getLoaderFor(final String source) {
        if (source != null && loaderMap != null) {
            ClassDefiningClassLoader loader = loaderMap.get(source);
            if (loader == null) {
                synchronized (loaderMap) {
                    loader = loaderMap.get(source);
                    if (loader == null) {
                        loaderMap.put(source, loader = new ClassDefiningJRubyClassLoader(runtime.getJRubyClassLoader()));
                    }
                }
            }
            return loader;
        }
        return new OneShotClassLoader(runtime.getJRubyClassLoader());
    }

    static void log(Compilable<?> target, String name, String message, Object... reason) {
        String className = target.getImplementationClass().getName();
        StringBuilder builder = new StringBuilder(32);
        builder.append(message).append(": ").append(className);
        if (name != null) builder.append(' ').append(name);
        builder.append(" at ").append(target.getFile()).append(':').append(target.getLine());

        if (reason.length > 0) {
            builder.append(" because of: \"");
            for (Object aReason : reason) builder.append(aReason);
            builder.append('"');
        }

        LOG.info(builder.toString());
    }

    static abstract class Task implements Runnable {

        protected final JITCompiler jitCompiler;

        public Task(JITCompiler jitCompiler) {
            this.jitCompiler = jitCompiler;
        }

        public void run() {
            // We synchronize against the JITCompiler object so at most one code body will jit at once in a given runtime.
            // This works around unsolved concurrency issues within the process of preparing and jitting the IR.
            // See #4739 for a reproduction script that produced various errors without this.
            synchronized (jitCompiler) {
                try {
                    exec();
                } catch (Throwable ex) {
                    jitFailed(ex);
                }
            }
        }

        protected abstract void exec() throws Exception ;

        protected String getSourceFile() {
            return null; // unknown by default
        }

        // shared helper methods :

        protected void jitFailed(final Throwable ex) {
            if (jitCompiler.config.isJitLogging()) {
                logFailed(ex);
                if (jitCompiler.config.isJitLoggingVerbose()) {
                    ex.printStackTrace();
                }
            }

            jitCompiler.counts.failCount.incrementAndGet();
        }

        protected Class<?> defineClass(final JITClassGenerator generator, final JVMVisitor visitor,
                                       final IRScope scope, final InterpreterContext interpreterContext) {
            // FIXME: reinstate active bytecode size check
            // At this point we still need to reinstate the bytecode size check, to ensure we're not loading code
            // that's so big that JVMs won't even try to compile it. Removed the check because with the new IR JIT
            // bytecode counts often include all nested scopes, even if they'd be different methods. We need a new
            // mechanism of getting all body sizes.
            Class sourceClass = visitor.defineFromBytecode(scope, generator.bytecode(), getCodeLoader());

            if (sourceClass == null) {
                // class could not be found nor generated; give up on JIT and bail out
                jitCompiler.counts.failCount.incrementAndGet();
                return null;
            }
            generator.updateCounters(jitCompiler.counts, interpreterContext);

            // successfully got back a jitted method/block
            long methodCount = jitCompiler.counts.successCount.incrementAndGet();

            if (jitCompiler.config.isJitLogging()) logJitted();

            // logEvery n methods based on configuration
            if (jitCompiler.config.getJitLogEvery() > 0) {
                if (methodCount % jitCompiler.config.getJitLogEvery() == 0) {
                    logImpl("live compiled count: " + methodCount);
                }
            }

            return sourceClass;
        }

        ClassDefiningClassLoader getCodeLoader() {
            return jitCompiler.getLoaderFor(getSourceFile());
        }

        protected void logJitted() {
            logImpl("done jitting");
        }

        protected void logFailed(Throwable ex) {
            logImpl("could not compile", ex);
        }

        protected abstract void logImpl(String msg, Object... cause) ;

    }

    // a fake (read-only) map which returns same value for whatever key
    private static class SharedClassLoaderMap extends AbstractMap<String, ClassDefiningClassLoader> {

        final ClassDefiningClassLoader value; // keeping a hard reference for the shared class-loader

        SharedClassLoaderMap(ClassDefiningClassLoader value) {
            this.value = value;
        }

        @Override
        public ClassDefiningClassLoader get(final Object key) {
            return value;
        }

        @Override
        public ClassDefiningClassLoader put(String key, ClassDefiningClassLoader value) {
            return this.value;
        }

        @Override
        public int size() {
            return -1; // unknown
        }

        @Override
        public void clear() {
            /* no-op */
        }

        @Override
        public Set<Entry<String, ClassDefiningClassLoader>> entrySet() {
            throw new UnsupportedOperationException();
        }

    }

}
