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
            IRScope hardScope = method.getIRScope().getNearestTopLocalVariableScope();

            // define_method may capture something outside itself and we need parents and children to compile
            // to agreement with respect to local variable access (e.g. dynscopes).
            if (hardScope != method.getIRScope()) hardScope.prepareFullBuild();

            method.completeBuild(method.getIRScope().prepareFullBuild());

            if (IRRuntimeHelpers.shouldPrintIR(jitCompiler.runtime)) {
                ByteArrayOutputStream baos = IRDumper.printIR(method.getIRScope(), true, true);
                LOG.info("Printing full IR for " + method.getIRScope().getId() + ":\n" + new String(baos.toByteArray()));
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
