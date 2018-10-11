package org.jruby.compiler;

import org.jruby.ir.interpreter.InterpreterContext;

/**
 * Created by headius on 12/8/16.
 */
class FullBuildTask implements Runnable {
    private JITCompiler jitCompiler;
    private final Compilable<InterpreterContext> method;

    FullBuildTask(JITCompiler jitCompiler, Compilable<InterpreterContext> method) {
        this.jitCompiler = jitCompiler;
        this.method = method;
    }

    public void run() {
        try {
            method.getIRScope().getRootLexicalScope().prepareFullBuild();

            method.completeBuild(method.getIRScope().prepareFullBuild());

            if (jitCompiler.config.isJitLogging()) {
                JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), method.getName(), "done building");
            }
        } catch (Throwable t) {
            if (jitCompiler.config.isJitLogging()) {
                JITCompiler.log(method.getImplementationClass(), method.getFile(), method.getLine(), method.getName(),
                        "Could not build; passes run: " + method.getIRScope().getExecutedPasses(), t.getMessage());
                if (jitCompiler.config.isJitLoggingVerbose()) {
                    t.printStackTrace();
                }
            }
        }
    }
}
