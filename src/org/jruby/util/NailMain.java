package org.jruby.util;

import com.martiansoftware.nailgun.NGContext;
import org.jruby.Main;
import org.jruby.RubyInstanceConfig;

public class NailMain {
    public static void nailMain(NGContext context) {
        NailMain main = new NailMain();
        int status = main.run(context);
        if (status != 0) {
            context.exit(status);
        }
    }

    public int run(NGContext context) {
        context.assertLoopbackClient();

        RubyInstanceConfig config = new RubyInstanceConfig();
        // populate commandline with NG-provided stuff
        config.processArguments(context.getArgs());
        config.setCurrentDirectory(context.getWorkingDirectory());
        config.setEnvironment(context.getEnv());

        Main main = new Main(config);
        return main.run();
    }
}