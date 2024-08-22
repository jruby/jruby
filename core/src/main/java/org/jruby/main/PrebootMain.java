package org.jruby.main;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

import java.io.File;
import java.io.IOException;

public abstract class PrebootMain {
    public static RubyInstanceConfig PREBOOT_CONFIG;
    public static Ruby PREBOOT_RUNTIME;

    public static final String JRUBY_PREBOOT_WARMUP_ENV = "JRUBY_PREBOOT_WARMUP";
    public static final String JRUBY_PREBOOT_WARMUP_DEFAULT = "1 + 1";
    public static final String JRUBY_PREBOOT_FILE = "./prebootmain.rb";

    public void preboot(String[] args) {
        // warmup JVM first
        args = jvmWarmup(args);

        // preboot actual runtime
        Ruby.clearGlobalRuntime();

        args = prepareOptions(args);

        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby ruby = Ruby.newInstance(config);

        args = jrubyWarmup(ruby, args);

        // use config and runtime from preboot process
        PREBOOT_CONFIG = config;
        PREBOOT_RUNTIME = ruby;

        endPreboot(args);
    }

    protected String[] jvmWarmup(String[] args) {
        Ruby ruby = Ruby.newInstance();

        String envWarmup = System.getenv(JRUBY_PREBOOT_WARMUP_ENV);
        if (envWarmup != null && envWarmup.length() > 0) {
            ruby.evalScriptlet(envWarmup);
        } else {
            ruby.evalScriptlet(JRUBY_PREBOOT_WARMUP_DEFAULT);
        }

        return args;
    }

    protected String[] jrubyWarmup(Ruby ruby, String[] args) {
        File prebootMain = new File(JRUBY_PREBOOT_FILE);
        if (prebootMain.exists()) {
            ruby.getLoadService().load(prebootMain.getAbsolutePath(), false);
        }

        return args;
    }

    protected String[] prepareOptions(String[] args) {
        return args;
    }

    protected abstract void endPreboot(String[] args);
}
