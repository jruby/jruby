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

    public void preboot() throws IOException {
        // warmup JVM first
        jvmWarmup();

        // preboot actual runtime
        Ruby.clearGlobalRuntime();
        File prebootMain = new File(JRUBY_PREBOOT_FILE);

        prepareOptions();

        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby ruby = Ruby.newInstance(config);

        jrubyWarmup(prebootMain, ruby);

        // use config and runtime from preboot process
        PREBOOT_CONFIG = config;
        PREBOOT_RUNTIME = ruby;

        endPreboot();
    }

    private static void jrubyWarmup(File prebootMain, Ruby ruby) {
        if (prebootMain.exists()) {
            ruby.getLoadService().load(prebootMain.getAbsolutePath(), false);
        }
    }

    private static void jvmWarmup() {
        Ruby ruby = Ruby.newInstance();

        String envWarmup = System.getenv(JRUBY_PREBOOT_WARMUP_ENV);
        if (envWarmup != null && envWarmup.length() > 0) {
            ruby.evalScriptlet(envWarmup);
        } else {
            ruby.evalScriptlet(JRUBY_PREBOOT_WARMUP_DEFAULT);
        }
    }

    abstract void prepareOptions();
    abstract void endPreboot();
}
