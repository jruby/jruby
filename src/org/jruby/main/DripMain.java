package org.jruby.main;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DripMain {
    public static RubyInstanceConfig DRIP_CONFIG;
    public static Ruby DRIP_RUNTIME;

    public static final String JRUBY_DRIP_WARMUP_ENV = "JRUBY_DRIP_WARMUP";
    public static final String JRUBY_DRIP_WARMUP_DEFAULT = "1 + 1";
    public static final String JRUBY_DRIP_PREBOOT_FILE = "./dripmain.rb";

    public static void main(String[] args) throws IOException {
        // warmup JVM first
        Ruby ruby = Ruby.newInstance();

        String envWarmup = System.getenv(JRUBY_DRIP_WARMUP_ENV);
        if (envWarmup != null && envWarmup.length() > 0) {
            ruby.evalScriptlet(envWarmup);
        } else {
            ruby.evalScriptlet(JRUBY_DRIP_WARMUP_DEFAULT);
        }

        // preboot actual runtime
        Ruby.clearGlobalRuntime();
        File dripMain = new File(JRUBY_DRIP_PREBOOT_FILE);

        RubyInstanceConfig config = new RubyInstanceConfig();
        ruby = Ruby.newInstance(config);

        if (dripMain.exists()) {
            FileInputStream fis = new FileInputStream(dripMain);
            try {
                ruby.getLoadService().load(dripMain.getAbsolutePath(), false);
            } finally {
                fis.close();
            }
        }

        // use config and runtime from preboot process
        DRIP_CONFIG = config;
        DRIP_RUNTIME = ruby;
    }
}
