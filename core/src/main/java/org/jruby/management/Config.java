package org.jruby.management;

import java.lang.ref.SoftReference;

import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.OutputStrings;

public class Config implements ConfigMBean {
    private final SoftReference<Ruby> ruby;
    
    public Config(Ruby ruby) {
        this.ruby = new SoftReference<Ruby>(ruby);
    }
    
    public String getVersionString() {
        return OutputStrings.getVersionString();
    }

    public String getCopyrightString() {
        return OutputStrings.getCopyrightString();
    }

    public String getCompileMode() {
        return ruby.get().getInstanceConfig().getCompileMode().name();
    }

    public boolean isJitLogging() {
        return ruby.get().getInstanceConfig().isJitLogging();
    }

    public boolean isJitLoggingVerbose() {
        return ruby.get().getInstanceConfig().isJitLoggingVerbose();
    }

    public int getJitLogEvery() {
        return ruby.get().getInstanceConfig().getJitLogEvery();
    }

    public int getJitThreshold() {
        return ruby.get().getInstanceConfig().getJitThreshold();
    }

    public void setJitThreshold(int threshold) {
        ruby.get().getInstanceConfig().setJitThreshold(threshold);
    }

    public int getJitMax() {
        return ruby.get().getInstanceConfig().getJitMax();
    }

    public void setJitMax(int max) {
        ruby.get().getInstanceConfig().setJitMax(max);
    }

    public int getJitMaxSize() {
        return ruby.get().getInstanceConfig().getJitMaxSize();
    }

    public void setJitMaxSize(int maxSize) {
        ruby.get().getInstanceConfig().setJitMaxSize(maxSize);
    }

    public long getJitTimeDelta() {
        return ruby.get().getInstanceConfig().getJitTimeDelta();
    }

    public void setJitTimeDelta(long timeDelta) {
        ruby.get().getInstanceConfig().setJitTimeDelta(timeDelta);
    }

    public boolean isRunRubyInProcess() {
        return ruby.get().getInstanceConfig().isRunRubyInProcess();
    }

    public String getCurrentDirectory() {
        return ruby.get().getInstanceConfig().getCurrentDirectory();
    }

    public boolean isObjectSpaceEnabled() {
        return ruby.get().getInstanceConfig().isObjectSpaceEnabled();
    }

    public String getEnvironment() {
        return ruby.get().getInstanceConfig().getEnvironment().toString();
    }

    public String getArgv() {
        return Arrays.deepToString(ruby.get().getInstanceConfig().getArgv());
    }

    public String getJRubyHome() {
        return ruby.get().getInstanceConfig().getJRubyHome();
    }

    public String getRequiredLibraries() {
        return ruby.get().getInstanceConfig().getRequiredLibraries().toString();
    }

    public String getLoadPaths() {
        return ruby.get().getInstanceConfig().getLoadPaths().toString();
    }

    public String getDisplayedFileName() {
        return ruby.get().getInstanceConfig().displayedFileName();
    }

    public String getScriptFileName() {
        return ruby.get().getInstanceConfig().getScriptFileName();
    }

    public boolean isAssumeLoop() {
        return ruby.get().getInstanceConfig().isAssumeLoop();
    }

    public boolean isAssumePrinting() {
        return ruby.get().getInstanceConfig().isAssumePrinting();
    }

    public boolean isProcessLineEnds() {
        return ruby.get().getInstanceConfig().isProcessLineEnds();
    }

    public boolean isSplit() {
        return ruby.get().getInstanceConfig().isSplit();
    }

    public boolean isVerbose() {
        return ruby.get().getInstanceConfig().isVerbose();
    }

    public boolean isDebug() {
        return ruby.get().getInstanceConfig().isDebug();
    }

    public String getInputFieldSeparator() {
        return ruby.get().getInstanceConfig().getInputFieldSeparator();
    }

    public String getKCode() {
        return ruby.get().getInstanceConfig().getKCode().name();
    }

    public String getRecordSeparator() {
        return ruby.get().getInstanceConfig().getRecordSeparator();
    }

    public int getSafeLevel() {
        return 0;
    }

    public String getOptionGlobals() {
        return ruby.get().getInstanceConfig().getOptionGlobals().toString();
    }
    
    public boolean isManagementEnabled() {
        return ruby.get().getInstanceConfig().isManagementEnabled();
    }
    
    public boolean isFullTraceEnabled() {
        return RubyInstanceConfig.FULL_TRACE_ENABLED;
    }
    
    public boolean isShowBytecode() {
        return ruby.get().getInstanceConfig().isShowBytecode();
    }
    
    public String getExcludedMethods() {
        return ruby.get().getInstanceConfig().getExcludedMethods().toString();
    }

}
