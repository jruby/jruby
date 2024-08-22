package org.jruby.main;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.Options;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class DripMain extends PrebootMain {
    public static void main(String[] args) throws IOException {
        DripMain dm = new DripMain();
        dm.preboot(args);
    }

    @Override
    protected String[] prepareOptions(String[] args) {
        // Disable native stdio when running under Drip (#4942)
        Options.NATIVE_STDIO.force("false");

        return args;
    }

    @Override
    protected void endPreboot(String[] args) {
    }
}
