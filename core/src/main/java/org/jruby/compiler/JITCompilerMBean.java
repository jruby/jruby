package org.jruby.compiler;

public interface JITCompilerMBean {
    public long getSuccessCount();
    public long getCompileCount();
    public long getFailCount();
    public long getAbandonCount();
    public long getCompileTime();
    public long getCodeSize();
    public long getCodeAverageSize();
    public long getCompileTimeAverage();
    public long getCodeLargestSize();
    public long getIRSize();
    public long getIRAverageSize();
    public long getIRLargestSize();
    public String[] getFrameAwareMethods();
    public String[] getScopeAwareMethods();
}
