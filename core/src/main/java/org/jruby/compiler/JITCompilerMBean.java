package org.jruby.compiler;

public interface JITCompilerMBean {
    public long getSuccessCount();
    public long getCompileCount();
    public long getFailCount();
    public long getAbandonCount();
    public long getCompileTime();
    public long getCodeSize();
    public long getAverageCodeSize();
    public long getAverageCompileTime();
    public long getLargestCodeSize();
    public String[] frameAwareMethods();
    public String[] scopeAwareMethods();
}
