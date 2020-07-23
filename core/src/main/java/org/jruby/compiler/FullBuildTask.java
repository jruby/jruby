package org.jruby.compiler;

import org.jruby.ir.IRScope;
import org.jruby.ir.interpreter.InterpreterContext;

/**
 * Created by headius on 12/8/16.
 */
class FullBuildTask implements Runnable {

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
