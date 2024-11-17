package org.jruby.management;

public interface ConfigMBean {
    public String getVersionString();
    public String getCopyrightString();
    public String getCompileMode();
    public boolean isJitLogging();
    public boolean isJitLoggingVerbose();
    public int getJitLogEvery();
    public int getJitThreshold();
    void setJitThreshold(int threshold);
    public int getJitMax();
    void setJitMax(int max);
    public int getJitMaxSize();
    void setJitMaxSize(int maxSize);
    public long getJitTimeDelta();
    void setJitTimeDelta(long timeDelta);
    public boolean isRunRubyInProcess();
    public String getCurrentDirectory();
    public boolean isObjectSpaceEnabled();
    public String getEnvironment();
    public String getArgv();
    public String getJRubyHome();
    public String getRequiredLibraries();
    public String getLoadPaths();
    public String getDisplayedFileName();
    public String getScriptFileName();
    public boolean isAssumeLoop();
    public boolean isAssumePrinting();
    public boolean isProcessLineEnds();
    public boolean isSplit();
    public boolean isVerbose();
    public boolean isDebug();
    public String getInputFieldSeparator();
    public String getKCode();
    public String getRecordSeparator();
    public int getSafeLevel();
    public String getOptionGlobals();
    public boolean isManagementEnabled();
    public boolean isFullTraceEnabled();
    public boolean isShowBytecode();
    public String getExcludedMethods();

    @Deprecated
    default String getCompatVersion() {
        return org.jruby.CompatVersion.RUBY2_1.name();
    }
}
