package org.jruby.compiler;

public interface JITCompilerMBean {
    long getSuccessCount();
    long getCompileCount();
    long getFailCount();
    long getAbandonCount();
    long getCompileTime();
    long getCodeSize();
    long getCodeAverageSize();
    long getCompileTimeAverage();
    long getCodeLargestSize();
    long getIRSize();
    long getIRAverageSize();
    long getIRLargestSize();
    String[] getFrameAwareMethods();
    String[] getScopeAwareMethods();
}
