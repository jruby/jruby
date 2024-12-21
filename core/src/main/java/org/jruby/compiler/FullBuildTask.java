package org.jruby.compiler;

import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

import java.io.ByteArrayOutputStream;

/**
 * Created by headius on 12/8/16.
 */
class FullBuildTask implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(FullBuildTask.class);

    private final JITCompiler jitCompiler;
    private final Compilable<InterpreterContext> method;

    FullBuildTask(JITCompiler jitCompiler, Compilable<InterpreterContext> method) {
        this.jitCompiler = jitCompiler;
        this.method = method;
    }

    public void run() {
        try {
            var scope = method.getIRScope();
            IRScope hardScope = scope.getNearestTopLocalVariableScope();

            // define_method may capture something outside itself and we need parents and children to compile
            // to agreement with respect to local variable access (e.g. dynscopes).
            if (hardScope != scope) hardScope.prepareFullBuild();

            method.completeBuild(scope.getManager().getRuntime().getCurrentContext(), scope.prepareFullBuild());

            if (IRRuntimeHelpers.shouldPrintIR(jitCompiler.runtime) && IRRuntimeHelpers.shouldPrintScope(scope)) {
                ByteArrayOutputStream baos = IRDumper.printIR(scope, true, true);
                LOG.info("Printing full IR for " + scope.getId() + ":\n" + new String(baos.toByteArray()));
            }

            if (jitCompiler.config.isJitLogging()) {
                JITCompiler.log(method, method.getName(), "done building");
            }
        } catch (Throwable t) {
            if (jitCompiler.config.isJitLogging()) {
                //JITCompiler.log(method, method.getName(), "could not build; passes run: " + method.getIRScope().getExecutedPasses(), t);
                if (jitCompiler.config.isJitLoggingVerbose()) {
                    t.printStackTrace();
                }
            }
        }
    }
}
