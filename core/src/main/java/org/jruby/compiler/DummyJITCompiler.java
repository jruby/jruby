package org.jruby.compiler;

import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;

public class DummyJITCompiler implements JITCompiler {
    @Override
    public long getSuccessCount() {
        return 0;
    }

    @Override
    public long getCompileCount() {
        return 0;
    }

    @Override
    public long getFailCount() {
        return 0;
    }

    @Override
    public long getAbandonCount() {
        return 0;
    }

    @Override
    public long getCompileTime() {
        return 0;
    }

    @Override
    public long getCodeSize() {
        return 0;
    }

    @Override
    public long getCodeAverageSize() {
        return 0;
    }

    @Override
    public long getCompileTimeAverage() {
        return 0;
    }

    @Override
    public long getCodeLargestSize() {
        return 0;
    }

    @Override
    public long getIRSize() {
        return 0;
    }

    @Override
    public long getIRAverageSize() {
        return 0;
    }

    @Override
    public long getIRLargestSize() {
        return 0;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public Runnable getTaskFor(ThreadContext context, Compilable method) {
        return () -> {};
    }

    @Override
    public void queueForJIT(ThreadContext context, Compilable method) {

    }
}
