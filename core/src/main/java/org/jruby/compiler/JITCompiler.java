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


import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyEncoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ast.util.SexpMaker;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.IRClosure;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.IntHashMap;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JITCompiler implements JITCompilerMBean {
    private static final Logger LOG = LoggerFactory.getLogger(JITCompiler.class);

    public static final String RUBY_JIT_PREFIX = "rubyjit";

    public static final String CLASS_METHOD_DELIMITER = "$$";

    public static class JITCounts {
        private final AtomicLong compiledCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong failCount = new AtomicLong(0);
        private final AtomicLong abandonCount = new AtomicLong(0);
        private final AtomicLong compileTime = new AtomicLong(0);
        private final AtomicLong averageCompileTime = new AtomicLong(0);
        private final AtomicLong codeSize = new AtomicLong(0);
        private final AtomicLong averageCodeSize = new AtomicLong(0);
        private final AtomicLong largestCodeSize = new AtomicLong(0);
    }

    private final JITCounts counts = new JITCounts();
    private final ExecutorService executor;

    private final Ruby runtime;
    private final RubyInstanceConfig config;

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

    public long getAverageCodeSize() {
        return counts.averageCodeSize.get();
    }

    public long getAverageCompileTime() {
        return counts.averageCompileTime.get() / 1000;
    }

    public long getLargestCodeSize() {
        return counts.largestCodeSize.get();
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
        try {
            executor.shutdown();
        } catch (SecurityException se) {
            // ignore, can't shut down executor
        }
    }

    public Runnable getTaskFor(ThreadContext context, Compilable method) {
        if (method instanceof MixedModeIRMethod) {
            return new MethodJITTask((MixedModeIRMethod) method, method.getClassName(context));
        } else if (method instanceof MixedModeIRBlockBody) {
            return new BlockJITTask((MixedModeIRBlockBody) method, method.getClassName(context));
        }

        return new FullBuildTask(method);
    }

    public void buildThresholdReached(ThreadContext context, final Compilable method) {
        final RubyInstanceConfig config = context.runtime.getInstanceConfig();

        // Disable any other jit tasks from entering queue
        method.setCallCount(-1);

        Runnable jitTask = getTaskFor(context, method);

        try {
            if (config.getJitBackground() && config.getJitThreshold() > 0) {
                try {
                    executor.submit(jitTask);
                } catch (RejectedExecutionException ree) {
                    // failed to submit, just run it directly
                    jitTask.run();
                }
            } else {
                // Because are non-asynchonously build if the JIT threshold happens to be 0 we will have no ic yet.
                method.ensureInstrsReady();
                // just run directly
                jitTask.run();
            }
        } catch (Exception e) {
            throw new NotCompilableException(e);
        }
    }

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup().in(Ruby.class);

    private class FullBuildTask implements Runnable {
        private final Compilable<InterpreterContext> method;

        FullBuildTask(Compilable<InterpreterContext> method) {
            this.method = method;
        }

        public void run() {
            try {
                method.completeBuild(method.getIRScope().prepareFullBuild());

                if (config.isJitLogging()) {
                    log(method.getImplementationClass(), method.getFile(), method.getLine(),  method.getName(), "done building");
                }
            } catch (Throwable t) {
                if (config.isJitLogging()) {
                    log(method.getImplementationClass(), method.getFile(), method.getLine(), method.getName(),
                            "Could not build; passes run: " + method.getIRScope().getExecutedPasses(), t.getMessage());
                    if (config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    private class MethodJITTask implements Runnable {
        private final String className;
        private final MixedModeIRMethod method;
        private final String methodName;

        public MethodJITTask(MixedModeIRMethod method, String className) {
            this.method = method;
            this.className = className;
            this.methodName = method.getName();
        }

        public void run() {
            try {
                // Check if the method has been explicitly excluded
                if (config.getExcludedMethods().size() > 0) {
                    String excludeModuleName = className;
                    if (method.getImplementationClass().getMethodLocation().isSingleton()) {
                        IRubyObject possibleRealClass = ((MetaClass) method.getImplementationClass()).getAttached();
                        if (possibleRealClass instanceof RubyModule) {
                            excludeModuleName = "Meta:" + ((RubyModule) possibleRealClass).getName();
                        }
                    }

                    if ((config.getExcludedMethods().contains(excludeModuleName)
                            || config.getExcludedMethods().contains(excludeModuleName + '#' + methodName)
                            || config.getExcludedMethods().contains(methodName))) {
                        method.setCallCount(-1);

                        if (config.isJitLogging()) {
                            log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "skipping method: " + excludeModuleName + '#' + methodName);
                        }
                        return;
                    }
                }

                String key = SexpMaker.sha1(method.getIRScope());
                JVMVisitor visitor = new JVMVisitor();
                MethodJITClassGenerator generator = new MethodJITClassGenerator(className, methodName, key, runtime, method, visitor);

                JVMVisitorMethodContext context = new JVMVisitorMethodContext();
                generator.compile(context);

                // FIXME: reinstate active bytecode size check
                // At this point we still need to reinstate the bytecode size check, to ensure we're not loading code
                // that's so big that JVMs won't even try to compile it. Removed the check because with the new IR JIT
                // bytecode counts often include all nested scopes, even if they'd be different methods. We need a new
                // mechanism of getting all method sizes.
                Class sourceClass = visitor.defineFromBytecode(method.getIRScope(), generator.bytecode(), new OneShotClassLoader(runtime.getJRubyClassLoader()));

                if (sourceClass == null) {
                    // class could not be found nor generated; give up on JIT and bail out
                    counts.failCount.incrementAndGet();
                    return;
                } else {
                    generator.updateCounters(counts);
                }

                // successfully got back a jitted method
                long methodCount = counts.successCount.incrementAndGet();

                // logEvery n methods based on configuration
                if (config.getJitLogEvery() > 0) {
                    if (methodCount % config.getJitLogEvery() == 0) {
                        log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "live compiled methods: " + methodCount);
                    }
                }

                if (config.isJitLogging()) {
                    log(method.getImplementationClass(), method.getFile(), method.getLine(), className + '.' + methodName, "done jitting");
                }

                final String jittedName = context.getJittedName();
                MethodHandle variable = PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, context.getNativeSignature(-1));
                IntHashMap<MethodType> signatures = context.getNativeSignaturesExceptVariable();

                if (signatures.size() == 0) {
                    // only variable-arity
                    method.completeBuild(
                            new CompiledIRMethod(
                                    variable,
                                    method.getIRScope(),
                                    method.getVisibility(),
                                    method.getImplementationClass(),
                                    method.getIRScope().receivesKeywordArgs()));

                } else {
                    // also specific-arity
                    for (IntHashMap.Entry<MethodType> entry : signatures.entrySet()) {
                        method.completeBuild(
                                new CompiledIRMethod(
                                        variable,
                                        PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, entry.getValue()),
                                        entry.getKey(),
                                        method.getIRScope(),
                                        method.getVisibility(),
                                        method.getImplementationClass(),
                                        method.getIRScope().receivesKeywordArgs()));
                        break; // FIXME: only supports one arity
                    }
                }
            } catch (Throwable t) {
                if (config.isJitLogging()) {
                    log(method.getImplementationClass(), method.getFile(), method.getLine(), className + '.' + methodName, "Could not compile; passes run: " + method.getIRScope().getExecutedPasses(), t.getMessage());
                    if (config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }

                counts.failCount.incrementAndGet();
            }
        }
    }

    private class BlockJITTask implements Runnable {
        private final String className;
        private final MixedModeIRBlockBody body;
        private final String methodName;

        public BlockJITTask(MixedModeIRBlockBody body, String className) {
            this.body = body;
            this.className = className;
            this.methodName = body.getName();
        }

        public void run() {
            try {
                String key = SexpMaker.sha1(body.getIRScope());
                JVMVisitor visitor = new JVMVisitor();
                BlockJITClassGenerator generator = new BlockJITClassGenerator(className, methodName, key, runtime, body, visitor);

                JVMVisitorMethodContext context = new JVMVisitorMethodContext();
                generator.compile(context);

                // FIXME: reinstate active bytecode size check
                // At this point we still need to reinstate the bytecode size check, to ensure we're not loading code
                // that's so big that JVMs won't even try to compile it. Removed the check because with the new IR JIT
                // bytecode counts often include all nested scopes, even if they'd be different methods. We need a new
                // mechanism of getting all body sizes.
                Class sourceClass = visitor.defineFromBytecode(body.getIRScope(), generator.bytecode(), new OneShotClassLoader(runtime.getJRubyClassLoader()));

                if (sourceClass == null) {
                    // class could not be found nor generated; give up on JIT and bail out
                    counts.failCount.incrementAndGet();
                    return;
                } else {
                    generator.updateCounters(counts);
                }

                // successfully got back a jitted body

                if (config.isJitLogging()) {
                    log(body.getImplementationClass(), body.getFile(), body.getLine(), className + "." + methodName, "done jitting");
                }

                String jittedName = context.getJittedName();

                // blocks only have variable-arity
                body.completeBuild(
                        new CompiledIRBlockBody(
                                PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, JVMVisitor.CLOSURE_SIGNATURE.type()),
                                body.getIRScope(),
                                ((IRClosure) body.getIRScope()).getSignature().encode()));
            } catch (Throwable t) {
                if (config.isJitLogging()) {
                    log(body.getImplementationClass(), body.getFile(), body.getLine(), className + "." + methodName, "Could not compile; passes run: " + body.getIRScope().getExecutedPasses(), t.getMessage());
                    if (config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }

                counts.failCount.incrementAndGet();
            }
        }
    }

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

    private static boolean java7InvokeDynamic() {
        return RubyInstanceConfig.JAVA_VERSION == Opcodes.V1_7 || Options.COMPILE_INVOKEDYNAMIC.load() == true;
    }

    public static class MethodJITClassGenerator {
        public MethodJITClassGenerator(String className, String methodName, String key, Ruby ruby, MixedModeIRMethod method, JVMVisitor visitor) {
            this.packageName = JITCompiler.RUBY_JIT_PREFIX;
            if ( java7InvokeDynamic() ) {
                // Some versions of Java 7 seems to have a bug that leaks definitions across cousin classloaders
                // so we force the class name to be unique to this runtime.

                // Also, invokedynamic forces us to make jitted bytecode unique to each runtime, since the call sites cache
                // at class level rather than at our runtime level. This makes it impossible to share jitted bytecode
                // across runtimes.

                digestString = key + Math.abs(ruby.hashCode());
            } else {
                digestString = key;
            }
            this.className = packageName + '/' + className.replace('.', '/') + CLASS_METHOD_DELIMITER + JavaNameMangler.mangleMethodName(methodName) + '_' + digestString;
            this.name = this.className.replace('/', '.');
            this.methodName = methodName;
            this.method = method;
            this.visitor = visitor;
        }

        @SuppressWarnings("unchecked")
        protected void compile(JVMVisitorMethodContext context) {
            if (bytecode != null) return;

            // Time the compilation
            long start = System.nanoTime();

            InterpreterContext ic = method.ensureInstrsReady();

            int insnCount = ic.getInstructions().length;
            if (insnCount > Options.JIT_MAXSIZE.load()) {
                // methods with more than our limit of basic blocks are likely too large to JIT, so bail out
                throw new NotCompilableException("Could not compile " + method + "; instruction count " + insnCount + " exceeds threshold of " + Options.JIT_MAXSIZE.load());
            }

            // This may not be ok since we'll end up running passes specific to JIT
            // CON FIXME: Really should clone scope before passes in any case
            bytecode = visitor.compileToBytecode(method.getIRScope(), context);

//            try {
//                java.io.FileOutputStream fos = new java.io.FileOutputStream(className + '#' + methodName + ".class");
//                fos.write(bytecode);
//                fos.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

            compileTime = System.nanoTime() - start;
        }

        void updateCounters(JITCounts counts) {
            counts.compiledCount.incrementAndGet();
            counts.compileTime.addAndGet(compileTime);
            counts.codeSize.addAndGet(bytecode.length);
            counts.averageCompileTime.set(counts.compileTime.get() / counts.compiledCount.get());
            counts.averageCodeSize.set(counts.codeSize.get() / counts.compiledCount.get());
            synchronized (counts) {
                if (counts.largestCodeSize.get() < bytecode.length) {
                    counts.largestCodeSize.set(bytecode.length);
                }
            }
        }

        // FIXME: Does anything call this?  If so we should document it.
        public void generate() {
            compile(new JVMVisitorMethodContext());
        }

        public byte[] bytecode() {
            return bytecode;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return methodName + "() at " + method.getFile() + ':' + method.getLine();
        }

        private final String packageName;
        private final String className;
        private final String methodName;
        private final String digestString;
        private final MixedModeIRMethod method;
        private final JVMVisitor visitor;

        private byte[] bytecode;
        private long compileTime;
        private final String name;
    }

    public static class BlockJITClassGenerator {
        public BlockJITClassGenerator(String className, String methodName, String key, Ruby ruby, MixedModeIRBlockBody body, JVMVisitor visitor) {
            this.packageName = JITCompiler.RUBY_JIT_PREFIX;
            if ( java7InvokeDynamic() ) {
                // Some versions of Java 7 seems to have a bug that leaks definitions across cousin classloaders
                // so we force the class name to be unique to this runtime.

                // Also, invokedynamic forces us to make jitted bytecode unique to each runtime, since the call sites cache
                // at class level rather than at our runtime level. This makes it impossible to share jitted bytecode
                // across runtimes.

                digestString = key + Math.abs(ruby.hashCode());
            } else {
                digestString = key;
            }
            this.className = packageName + '/' + className.replace('.', '/') + CLASS_METHOD_DELIMITER + JavaNameMangler.mangleMethodName(methodName) + '_' + digestString;
            this.name = this.className.replace('/', '.');
            this.methodName = methodName;
            this.body = body;
            this.visitor = visitor;
        }

        @SuppressWarnings("unchecked")
        protected void compile(JVMVisitorMethodContext context) {
            if (bytecode != null) return;

            // Time the compilation
            long start = System.nanoTime();

            InterpreterContext ic = body.ensureInstrsReady();

            int insnCount = ic.getInstructions().length;
            if (insnCount > Options.JIT_MAXSIZE.load()) {
                // methods with more than our limit of basic blocks are likely too large to JIT, so bail out
                throw new NotCompilableException("Could not compile " + body + "; instruction count " + insnCount + " exceeds threshold of " + Options.JIT_MAXSIZE.load());
            }

            // This may not be ok since we'll end up running passes specific to JIT
            // CON FIXME: Really should clone scope before passes in any case
            bytecode = visitor.compileToBytecode(body.getIRScope(), context);

            compileTime = System.nanoTime() - start;
        }

        void updateCounters(JITCounts counts) {
            counts.compiledCount.incrementAndGet();
            counts.compileTime.addAndGet(compileTime);
            counts.codeSize.addAndGet(bytecode.length);
            counts.averageCompileTime.set(counts.compileTime.get() / counts.compiledCount.get());
            counts.averageCodeSize.set(counts.codeSize.get() / counts.compiledCount.get());
            synchronized (counts) {
                if (counts.largestCodeSize.get() < bytecode.length) {
                    counts.largestCodeSize.set(bytecode.length);
                }
            }
        }

        // FIXME: Does anything call this?  If so we should document it.
        public void generate() {
            compile(new JVMVisitorMethodContext());
        }

        public byte[] bytecode() {
            return bytecode;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "{} at " + body.getFile() + ':' + body.getLine();
        }

        private final String packageName;
        private final String className;
        private final String methodName;
        private final String digestString;
        private final MixedModeIRBlockBody body;
        private final JVMVisitor visitor;

        private byte[] bytecode;
        private long compileTime;
        private final String name;
    }

    static void log(RubyModule implementationClass, String file, int line, String name, String message, String... reason) {
        boolean isBlock = implementationClass == null;
        String className = isBlock ? "<block>" : implementationClass.getBaseName();
        if (className == null) className = "<anon class>";

        StringBuilder builder = new StringBuilder(32);
        builder.append(message).append(": ").append(className)
               .append(' ').append(name == null ? "" : name)
               .append(" at ").append(file).append(':').append(line);

        if (reason.length > 0) {
            builder.append(" because of: \"");
            for (String aReason : reason) builder.append(aReason);
            builder.append('"');
        }

        LOG.info(builder.toString());
    }
}
