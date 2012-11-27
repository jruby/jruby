package org.jruby.main;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DripMain {
    public static RubyInstanceConfig DRIP_CONFIG;
    public static Ruby DRIP_RUNTIME;

    public static void main(String[] args) throws IOException {
        File dripMain = new File("./dripmain.rb");

        // pre-boot our "Drip" runtime
        RubyInstanceConfig config = new RubyInstanceConfig();
        Ruby ruby = Ruby.newInstance(config);

        if (dripMain.exists()) {
            FileInputStream fis = new FileInputStream(dripMain);
            try {
                ruby.getLoadService().load(dripMain.getAbsolutePath(), false);
            } finally {
                fis.close();
            }
        }

        DRIP_CONFIG = config;
        DRIP_RUNTIME = ruby;
    }
}
