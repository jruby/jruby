package org.jruby.management;

public interface ConfigMBean {
    public String getVersionString();
    public String getCopyrightString();
    public String getCompileMode();
    public boolean isJitLogging();
    public boolean isJitLoggingVerbose();
    public int getJitLogEvery();
    public int getJitThreshold();
    public int getJitMax();
    public int getJitMaxSize();
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
    public boolean isLazyHandlesEnabled();
    public boolean isShowBytecode();
    public String getExcludedMethods();

    @Deprecated
    public String getCompatVersion();
}
