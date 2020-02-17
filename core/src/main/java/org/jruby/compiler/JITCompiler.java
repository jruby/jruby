package org.jruby.compiler;

import org.jruby.runtime.ThreadContext;

public interface JITCompiler extends JITCompilerMBean {
    void shutdown();

    boolean isShutdown();

    Runnable getTaskFor(ThreadContext context, Compilable method);

    void queueForJIT(ThreadContext context, Compilable method);
}
