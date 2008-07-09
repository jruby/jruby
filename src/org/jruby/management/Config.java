package org.jruby.management;

import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

public class Config implements ConfigMBean {
    private Ruby ruby;
    
    public Config(Ruby ruby) {
        this.ruby = ruby;
    }
    
    public String getVersionString() {
        return ruby.getInstanceConfig().getVersionString();
    }

    public String getCopyrightString() {
        return ruby.getInstanceConfig().getCopyrightString();
    }

    public String getCompileMode() {
        return ruby.getInstanceConfig().getCompileMode().name();
    }

    public boolean isJitLogging() {
        return ruby.getInstanceConfig().isJitLogging();
    }

    public boolean isJitLoggingVerbose() {
        return ruby.getInstanceConfig().isJitLoggingVerbose();
    }

    public int getJitLogEvery() {
        return ruby.getInstanceConfig().getJitLogEvery();
    }

    public boolean isSamplingEnabled() {
        return ruby.getInstanceConfig().isSamplingEnabled();
    }

    public int getJitThreshold() {
        return ruby.getInstanceConfig().getJitThreshold();
    }

    public int getJitMax() {
        return ruby.getInstanceConfig().getJitMax();
    }

    public int getJitMaxSize() {
        return ruby.getInstanceConfig().getJitMaxSize();
    }

    public boolean isRunRubyInProcess() {
        return ruby.getInstanceConfig().isRunRubyInProcess();
    }

    public String getCompatVersion() {
        return ruby.getInstanceConfig().getCompatVersion().name();
    }

    public String getCurrentDirectory() {
        return ruby.getInstanceConfig().getCurrentDirectory();
    }

    public boolean isObjectSpaceEnabled() {
        return ruby.getInstanceConfig().isObjectSpaceEnabled();
    }

    public String getEnvironment() {
        return ruby.getInstanceConfig().getEnvironment().toString();
    }

    public String getArgv() {
        return Arrays.deepToString(ruby.getInstanceConfig().getArgv());
    }

    public String getJRubyHome() {
        return ruby.getInstanceConfig().getJRubyHome();
    }

    public String getRequiredLibraries() {
        return ruby.getInstanceConfig().requiredLibraries().toString();
    }

    public String getLoadPaths() {
        return ruby.getInstanceConfig().loadPaths().toString();
    }

    public String getDisplayedFileName() {
        return ruby.getInstanceConfig().displayedFileName();
    }

    public String getScriptFileName() {
        return ruby.getInstanceConfig().getScriptFileName();
    }

    public boolean isBenchmarking() {
        return ruby.getInstanceConfig().isBenchmarking();
    }

    public boolean isAssumeLoop() {
        return ruby.getInstanceConfig().isAssumeLoop();
    }

    public boolean isAssumePrinting() {
        return ruby.getInstanceConfig().isAssumePrinting();
    }

    public boolean isProcessLineEnds() {
        return ruby.getInstanceConfig().isProcessLineEnds();
    }

    public boolean isSplit() {
        return ruby.getInstanceConfig().isSplit();
    }

    public boolean isVerbose() {
        return ruby.getInstanceConfig().isVerbose();
    }

    public boolean isDebug() {
        return ruby.getInstanceConfig().isDebug();
    }

    public boolean isYARVEnabled() {
        return ruby.getInstanceConfig().isYARVEnabled();
    }

    public String getInputFieldSeparator() {
        return ruby.getInstanceConfig().getInputFieldSeparator();
    }

    public boolean isRubiniusEnabled() {
        return ruby.getInstanceConfig().isRubiniusEnabled();
    }

    public boolean isYARVCompileEnabled() {
        return ruby.getInstanceConfig().isYARVCompileEnabled();
    }

    public String getKCode() {
        return ruby.getInstanceConfig().getKCode().name();
    }

    public String getRecordSeparator() {
        return ruby.getInstanceConfig().getRecordSeparator();
    }

    public int getSafeLevel() {
        return ruby.getInstanceConfig().getSafeLevel();
    }

    public String getOptionGlobals() {
        return ruby.getInstanceConfig().getOptionGlobals().toString();
    }
    
    public boolean isManagementEnabled() {
        return ruby.getInstanceConfig().isManagementEnabled();
    }
    
    public boolean isFullTraceEnabled() {
        return RubyInstanceConfig.FULL_TRACE_ENABLED;
    }
    
    public boolean isLazyHandlesEnabled() {
        return RubyInstanceConfig.LAZYHANDLES_COMPILE;
    }
    
    public boolean isShowBytecode() {
        return ruby.getInstanceConfig().isShowBytecode();
    }
}
