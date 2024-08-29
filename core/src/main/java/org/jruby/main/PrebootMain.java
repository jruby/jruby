package org.jruby.main;

import org.jruby.Main;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

import java.io.File;

public abstract class PrebootMain extends Main {
    private static PrebootMain prebootMain;
    private RubyInstanceConfig config;
    private Ruby runtime;

    public static final String JRUBY_PREBOOT_WARMUP_ENV = "JRUBY_PREBOOT_WARMUP";
    public static final String JRUBY_PREBOOT_WARMUP_DEFAULT = "1 + 1";
    public static final String JRUBY_PREBOOT_FILE = "./prebootmain.rb";

    public static PrebootMain getPrebootMain() {
        return prebootMain;
    }

    public RubyInstanceConfig getPrebootConfig() {
        return config;
    }

    public Ruby getPrebootRuntime() {
        return runtime;
    }

    public static void preboot(PrebootMain main, String[] args) {
        main.preboot(args);

        prebootMain = main;
    }

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
        this.config = config;
        this.runtime = ruby;

        endPreboot(args);
    }

    protected String[] jvmWarmup(String[] args) {
        Ruby ruby = Ruby.newInstance();

        String envWarmup = System.getenv(prebootWarmupEnvVar());
        if (envWarmup != null && envWarmup.length() > 0) {
            ruby.evalScriptlet(envWarmup);
        } else {
            ruby.evalScriptlet(prebootWarmupDefault());
        }

        return args;
    }

    protected String[] jrubyWarmup(Ruby ruby, String[] args) {
        File prebootMain = new File(prebootFile());
        if (prebootMain.exists()) {
            ruby.getLoadService().load(prebootMain.getAbsolutePath(), false);
        }

        return args;
    }

    protected String prebootWarmupEnvVar() {
        return JRUBY_PREBOOT_WARMUP_ENV;
    }

    protected static String prebootWarmupDefault() {
        return JRUBY_PREBOOT_WARMUP_DEFAULT;
    }

    protected static String prebootFile() {
        return JRUBY_PREBOOT_FILE;
    }

    protected String[] prepareOptions(String[] args) {
        return args;
    }

    protected abstract void endPreboot(String[] args);
}
