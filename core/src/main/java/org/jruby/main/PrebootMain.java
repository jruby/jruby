package org.jruby.main;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

public abstract class PrebootMain extends Main {
    private static PrebootMain prebootMain;
    private RubyInstanceConfig config;
    private Ruby runtime;

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
        prebootMain = main;

        main.preboot(args);
    }

    public void preboot(String[] args) {
        args = warmup(args);
        args = prepareOptions(args);

        RubyInstanceConfig config = prepareConfig(args);
        Ruby ruby = prepareRuntime(config, args);

        this.config = config;
        this.runtime = ruby;

        endPreboot(config, ruby, args);
    }

    protected String[] warmup(String[] args) {
        return args;
    }

    protected String[] prepareOptions(String[] args) {
        return args;
    }

    protected RubyInstanceConfig prepareConfig(String[] args) {
        return new RubyInstanceConfig();
    }

    protected Ruby prepareRuntime(RubyInstanceConfig config, String[] args) {
        return Ruby.newInstance(config);
    }

    protected void endPreboot(RubyInstanceConfig config, Ruby ruby, String[] args) {
    }
}
