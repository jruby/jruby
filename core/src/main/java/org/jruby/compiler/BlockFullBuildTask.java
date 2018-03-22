package org.jruby.compiler;

import org.jruby.runtime.MixedModeIRBlockBody;
import org.jruby.runtime.OptInterpretedIRBlockBody;

/**
 * Created by headius on 12/8/16.
 */
class BlockFullBuildTask implements Runnable {
    private JITCompiler jitCompiler;
    private final MixedModeIRBlockBody method;

    BlockFullBuildTask(JITCompiler jitCompiler, MixedModeIRBlockBody method) {
        this.jitCompiler = jitCompiler;
        this.method = method;
    }

    public void run() {
        try {
            method.completeBuild(new OptInterpretedIRBlockBody(method.getScope(), method.getSignature()));

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
