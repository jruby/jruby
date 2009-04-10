package org.jruby.management;

import java.lang.ref.SoftReference;

import java.util.Arrays;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

public class Config implements ConfigMBean {
    private final SoftReference<Ruby> ruby;
    
    public Config(Ruby ruby) {
        this.ruby = new SoftReference<Ruby>(ruby);
    }
    
    public String getVersionString() {
        return ruby.get().getInstanceConfig().getVersionString();
    }

    public String getCopyrightString() {
        return ruby.get().getInstanceConfig().getCopyrightString();
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

    public boolean isSamplingEnabled() {
        return ruby.get().getInstanceConfig().isSamplingEnabled();
    }

    public int getJitThreshold() {
        return ruby.get().getInstanceConfig().getJitThreshold();
    }

    public int getJitMax() {
        return ruby.get().getInstanceConfig().getJitMax();
    }

    public int getJitMaxSize() {
        return ruby.get().getInstanceConfig().getJitMaxSize();
    }

    public boolean isRunRubyInProcess() {
        return ruby.get().getInstanceConfig().isRunRubyInProcess();
    }

    public String getCompatVersion() {
        return ruby.get().getInstanceConfig().getCompatVersion().name();
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
        return ruby.get().getInstanceConfig().requiredLibraries().toString();
    }

    public String getLoadPaths() {
        return ruby.get().getInstanceConfig().loadPaths().toString();
    }

    public String getDisplayedFileName() {
        return ruby.get().getInstanceConfig().displayedFileName();
    }

    public String getScriptFileName() {
        return ruby.get().getInstanceConfig().getScriptFileName();
    }

    public boolean isBenchmarking() {
        return ruby.get().getInstanceConfig().isBenchmarking();
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
        return ruby.get().getInstanceConfig().getSafeLevel();
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
    
    public boolean isLazyHandlesEnabled() {
        return RubyInstanceConfig.LAZYHANDLES_COMPILE;
    }
    
    public boolean isShowBytecode() {
        return ruby.get().getInstanceConfig().isShowBytecode();
    }
    
    public String getExcludedMethods() {
        return ruby.get().getInstanceConfig().getExcludedMethods().toString();
    }
}
