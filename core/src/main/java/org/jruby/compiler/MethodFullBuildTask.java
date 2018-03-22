package org.jruby.compiler;

import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.internal.runtime.methods.OptInterpretedIRMethod;
import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.OptInterpretedIRBlockBody;

class MethodFullBuildTask implements Runnable {
    private JITCompiler jitCompiler;
    private final MixedModeIRMethod method;

    MethodFullBuildTask(JITCompiler jitCompiler, MixedModeIRMethod method) {
        this.jitCompiler = jitCompiler;
        this.method = method;
    }

    public void run() {
        try {
            method.completeBuild(new OptInterpretedIRMethod(method.getIRScope(), method.getVisibility(), method.getImplementationClass()));

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
