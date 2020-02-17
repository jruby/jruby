package org.jruby.compiler;

import org.jruby.runtime.MethodIndex;

import java.util.Arrays;

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

    default String[] getFrameAwareMethods() {
        String[] frameAwareMethods = MethodIndex.FRAME_AWARE_METHODS.toArray(new String[0]);
        Arrays.sort(frameAwareMethods);
        return frameAwareMethods;
    }

    default String[] getScopeAwareMethods() {
        String[] scopeAwareMethods = MethodIndex.SCOPE_AWARE_METHODS.toArray(new String[0]);
        Arrays.sort(scopeAwareMethods);
        return scopeAwareMethods;
    }
}
