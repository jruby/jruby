package org.jruby.compiler;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.util.SexpMaker;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.targets.JVMVisitor;
import org.jruby.ir.targets.JVMVisitorMethodContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.OneShotClassLoader;
import org.jruby.util.collections.IntHashMap;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

/**
 * Created by enebo on 10/30/18.
 */
public class MethodCompiledJITTask implements Runnable {
    private final JITCompiler jitCompiler;
    private final String className;
    private final CompiledIRMethod method;
    private final String methodName;

    public MethodCompiledJITTask(JITCompiler jitCompiler, CompiledIRMethod method, String className) {
        this.jitCompiler = jitCompiler;
        this.method = method;
        this.className = className;
        this.methodName = method.getName();
    }

    @Override
    public void run() {
        // We synchronize against the JITCompiler object so at most one code body will jit at once in a given runtime.
        // This works around unsolved concurrency issues within the process of preparing and jitting the IR.
        // See #4739 for a reproduction script that produced various errors without this.
        synchronized (jitCompiler) {
            try {
                // Check if the method has been explicitly excluded
                if (jitCompiler.config.getExcludedMethods().size() > 0) {
                    String excludeModuleName = className;
                    if (method.getImplementationClass().getMethodLocation().isSingleton()) {
                        IRubyObject possibleRealClass = ((MetaClass) method.getImplementationClass()).getAttached();
                        if (possibleRealClass instanceof RubyModule) {
                            excludeModuleName = "Meta:" + ((RubyModule) possibleRealClass).getName();
                        }
                    }

                    if ((jitCompiler.config.getExcludedMethods().contains(excludeModuleName)
                            || jitCompiler.config.getExcludedMethods().contains(excludeModuleName + '#' + methodName)
                            || jitCompiler.config.getExcludedMethods().contains(methodName))) {
                        method.setCallCount(-1);

                        if (jitCompiler.config.isJitLogging()) {
                            JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "skipping method: " + excludeModuleName + '#' + methodName);
                        }
                        return;
                    }
                }

                String key = SexpMaker.sha1(method.getIRScope());
                Ruby runtime = jitCompiler.runtime;
                JVMVisitor visitor = new JVMVisitor(runtime);
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
                    jitCompiler.counts.failCount.incrementAndGet();
                    return;
                } else {
                    generator.updateCounters(jitCompiler.counts, method.ensureInstrsReady());
                }

                // successfully got back a jitted method
                long methodCount = jitCompiler.counts.successCount.incrementAndGet();

                // logEvery n methods based on configuration
                if (jitCompiler.config.getJitLogEvery() > 0) {
                    if (methodCount % jitCompiler.config.getJitLogEvery() == 0) {
                        JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), methodName, "live compiled methods: " + methodCount);
                    }
                }

                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), className + '.' + methodName, "done jitting");
                }

                String variableName = context.getVariableName();
                MethodHandle variable = JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, variableName, context.getNativeSignature(-1));
                IntHashMap<MethodType> signatures = context.getNativeSignaturesExceptVariable();

                method.setVariable(variable);
                if (signatures.size() != 0) {
                    for (IntHashMap.Entry<MethodType> entry : signatures.entrySet()) {
                        method.setSpecific(JITCompiler.PUBLIC_LOOKUP.findStatic(sourceClass, context.getSpecificName(), entry.getValue()));
                        break; // FIXME: only supports one arity
                    }
                }
            } catch (Throwable t) {
                if (jitCompiler.config.isJitLogging()) {
                    JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), className + '.' + methodName, "Could not compile; passes run: " + method.getIRScope().getExecutedPasses(), t.getMessage());
                    if (jitCompiler.config.isJitLoggingVerbose()) {
                        t.printStackTrace();
                    }
                }

                jitCompiler.counts.failCount.incrementAndGet();
            }
        }
    }
}
