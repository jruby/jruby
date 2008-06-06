package org.jruby.compiler;

public interface JITCompilerMBean {
    public long getSuccessCount();
    public long getCompileCount();
    public long getFailCount();
    public long getAbandonCount();
    public long getCompileTime();
}
