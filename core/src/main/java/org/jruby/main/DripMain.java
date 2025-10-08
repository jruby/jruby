package org.jruby.main;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.Options;

import java.io.File;

public class DripMain extends PrebootMain {
    public static RubyInstanceConfig DRIP_CONFIG;
    public static Ruby DRIP_RUNTIME;

    public static final String JRUBY_DRIP_WARMUP_ENV = "JRUBY_DRIP_WARMUP";
    public static final String JRUBY_DRIP_WARMUP_DEFAULT = "1 + 1";
    public static final String JRUBY_DRIP_FILE = "./dripmain.rb";

    public static void main(String[] args) {
        preboot(new DripMain(), args);
    }

    @Override
    protected String[] warmup(String[] args) {
        Ruby ruby = Ruby.newInstance();

        String envWarmup = System.getenv(JRUBY_DRIP_WARMUP_ENV);
        if (envWarmup != null && envWarmup.length() > 0) {
            ruby.evalScriptlet(envWarmup);
        } else {
            ruby.evalScriptlet(JRUBY_DRIP_WARMUP_DEFAULT);
        }

        return args;
    }

    @Override
    protected String[] prepareOptions(String[] args) {
        // Disable native stdio when running under Drip (#4942)
        Options.NATIVE_STDIO.force("false");

        return args;
    }

    @Override
    protected Ruby prepareRuntime(RubyInstanceConfig config, String[] args) {
        Ruby ruby = super.prepareRuntime(config, args);

        File dripMain = new File(JRUBY_DRIP_FILE);
        if (dripMain.exists()) {
            ruby.getLoadService().load(dripMain.getAbsolutePath(), false);
        }

        return ruby;
    }

    @Override
    protected void endPreboot(RubyInstanceConfig config, Ruby ruby, String[] args) {
        super.endPreboot(config, ruby, args);

        // backward compat
        DRIP_CONFIG = config;
        DRIP_RUNTIME = ruby;
    }
}
