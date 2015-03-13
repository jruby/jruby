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
import org.jruby.ir.IRMethod;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.threading.DaemonThreadFactory;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JITCompiler implements JITCompilerMBean {
    private static final Logger LOG = LoggerFactory.getLogger("JITCompiler");

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

    public void tearDown() {
        if (executor != null) {
            try {
                executor.shutdown();
            } catch (SecurityException se) {
                // ignore, can't shut down executor
            }
        }
    }

    public void fullBuildThresholdReached(final FullBuildSource method, final RubyInstanceConfig config) {
        // Disable any other jit tasks from entering queue
        method.setCallCount(-1);

        Runnable jitTask = new FullBuildTask(method);

        if (config.getJitThreshold() > 0) {
            if (config.getJitBackground() && executor != null) {
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
        }
    }
    
    public void jitThresholdReached(final MixedModeIRMethod method, final RubyInstanceConfig config, ThreadContext context, final String className, final String methodName) {
        // Disable any other jit tasks from entering queue
        method.setCallCount(-1);
        
        Runnable jitTask = new JITTask(className, method, methodName);

        // if background JIT is enabled and threshold is > 0 and we have an executor...
        if (config.getJitBackground() &&
                config.getJitThreshold() > 0 &&
                executor != null) {
            // JIT in background
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
    }

    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup().in(Ruby.class);

    private class FullBuildTask implements Runnable {
        private final FullBuildSource method;

        public FullBuildTask(FullBuildSource method) {
            this.method = method;
        }

        public void run() {
            try {
                method.switchToFullBuild(method.getIRScope().prepareFullBuild());

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

    private class JITTask implements Runnable {
        private final String className;
        private final MixedModeIRMethod method;
        private final String methodName;

        public JITTask(String className, MixedModeIRMethod method, String methodName) {
            this.className = className;
            this.method = method;
            this.methodName = methodName;
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
                            || config.getExcludedMethods().contains(excludeModuleName + "#" + methodName)
                            || config.getExcludedMethods().contains(methodName))) {
                        method.setCallCount(-1);
                        log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "skipping method: " + excludeModuleName + "#" + methodName);
                        return;
                    }
                }

                String key = SexpMaker.sha1(method.getIRMethod());
                JVMVisitor visitor = new JVMVisitor();
                JITClassGenerator generator = new JITClassGenerator(className, methodName, key, runtime, method, visitor);

                generator.compile();

                // FIXME: reinstate active bytecode size check
                // At this point we still need to reinstate the bytecode size check, to ensure we're not loading code
                // that's so big that JVMs won't even try to compile it. Removed the check because with the new IR JIT
                // bytecode counts often include all nested scopes, even if they'd be different methods. We need a new
                // mechanism of getting all method sizes.
                Class sourceClass = visitor.defineFromBytecode(method.getIRMethod(), generator.bytecode(), new OneShotClassLoader(runtime.getJRubyClassLoader()));

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
                    log(method.getImplementationClass(), method.getFile(), method.getLine(), className + "." + methodName, "done jitting");
                }

                Map<Integer, MethodType> signatures = ((IRMethod)method.getIRMethod()).getNativeSignatures();
                String jittedName = ((IRMethod)method.getIRMethod()).getJittedName();
                if (signatures.size() == 1) {
                    // only variable-arity
                    method.switchToJitted(
                            new CompiledIRMethod(
                                    PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, signatures.get(-1)),
                                    method.getIRMethod(),
                                    method.getVisibility(),
                                    method.getImplementationClass()));
                } else {
                    // also specific-arity
                    for (Map.Entry<Integer, MethodType> entry : signatures.entrySet()) {
                        if (entry.getKey() == -1) continue; // variable arity handle pushed above

                        method.switchToJitted(
                                new CompiledIRMethod(
                                        PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, signatures.get(-1)),
                                        PUBLIC_LOOKUP.findStatic(sourceClass, jittedName, entry.getValue()),
                                        entry.getKey(),
                                        method.getIRMethod(),
                                        method.getVisibility(),
                                        method.getImplementationClass()));
                        break;
                    }
                }

                return;
            } catch (Throwable t) {
                if (config.isJitLogging()) {
                    log(method.getImplementationClass(), method.getFile(), method.getLine(), className + "." + methodName, "Could not compile; passes run: " + method.getIRMethod().getExecutedPasses(), t.getMessage());
                    if (config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }

                counts.failCount.incrementAndGet();
                return;
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
            byte[] digest = sha1.digest();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                builder.append(Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 ));
            }
            return builder.toString().toUpperCase(Locale.ENGLISH);
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException(nsae);
        }
    }
    
    public static class JITClassGenerator {
        public JITClassGenerator(String className, String methodName, String key, Ruby ruby, MixedModeIRMethod method, JVMVisitor visitor) {
            this.packageName = JITCompiler.RUBY_JIT_PREFIX;
            if (RubyInstanceConfig.JAVA_VERSION == Opcodes.V1_7 || Options.COMPILE_INVOKEDYNAMIC.load() == true) {
                // Some versions of Java 7 seems to have a bug that leaks definitions across cousin classloaders
                // so we force the class name to be unique to this runtime.

                // Also, invokedynamic forces us to make jitted bytecode unique to each runtime, since the call sites cache
                // at class level rather than at our runtime level. This makes it impossible to share jitted bytecode
                // across runtimes.
                
                digestString = key + Math.abs(ruby.hashCode());
            } else {
                digestString = key;
            }
            this.className = packageName + "/" + className.replace('.', '/') + CLASS_METHOD_DELIMITER + JavaNameMangler.mangleMethodName(methodName) + "_" + digestString;
            this.name = this.className.replaceAll("/", ".");
            this.methodName = methodName;
            this.ruby = ruby;
            this.method = method;
            this.visitor = visitor;
        }
        
        @SuppressWarnings("unchecked")
        protected void compile() {
            if (bytecode != null) return;
            
            // Time the compilation
            long start = System.nanoTime();

            method.ensureInstrsReady();

            // This may not be ok since we'll end up running passes specific to JIT
            // CON FIXME: Really should clone scope before passes in any case
            bytecode = visitor.compileToBytecode(method.getIRMethod());

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

        public void generate() {
            compile();
        }
        
        public byte[] bytecode() {
            return bytecode;
        }

        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return methodName + "() at " + method.getFile() + ":" + method.getLine();
        }

        private final Ruby ruby;
        private final String packageName;
        private final String className;
        private final String methodName;
        private final String digestString;
        private final MixedModeIRMethod method;
        private final JVMVisitor visitor;

        private byte[] bytecode;
        private long compileTime;
        private String name;
    }

    static void log(RubyModule implementationClass, String file, int line, String name, String message, String... reason) {
        boolean isBlock = implementationClass == null;
        String className = isBlock ? "<block>" : implementationClass.getBaseName();
        if (className == null) className = "<anon class>";

        name = isBlock ? "" : "." + name;

        StringBuilder builder = new StringBuilder(message + ":" + className + name + " at " + file + ":" + line);
        
        if (reason.length > 0) {
            builder.append(" because of: \"");
            for (int i = 0; i < reason.length; i++) {
                builder.append(reason[i]);
            }
            builder.append('"');
        }
        
        LOG.info(builder.toString());
    }
}
