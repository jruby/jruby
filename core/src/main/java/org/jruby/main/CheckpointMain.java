package org.jruby.main;

import com.headius.options.Option;
import org.crac.CheckpointException;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.crac.RestoreException;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.RaiseException;
import org.jruby.util.cli.Options;

import java.io.InputStream;

import static org.jruby.main.Main.handleRaiseException;

public class CheckpointMain extends PrebootMain {
    public static void main(String[] args) {
        preboot(new CheckpointMain(), args);
    }

    protected String[] warmup(String[] args) {
        Ruby ruby = Ruby.newInstance();

        ruby.evalScriptlet("1 + 1");

        Ruby.clearGlobalRuntime();

        return args;
    }

    protected RubyInstanceConfig prepareConfig(String[] args) {
        RubyInstanceConfig config = super.prepareConfig(args);
        if (args.length > 0) {
            config.processArguments(args);
        }
        return config;
    }

    protected Ruby prepareRuntime(RubyInstanceConfig config, String[] args) {
        Ruby ruby = super.prepareRuntime(config, args);

        // If more arguments were provided, run them as normal before checkpointing
        if (args.length > 0) {
            InputStream in   = config.getScriptSource();
            String filename  = config.displayedFileName();

            try {
                if (in == null || config.getShouldCheckSyntax()) {
                    // ignore if there's no code to run
                } else {
                    // proceed to run the script
                    ruby.runFromMain(in, filename);
                }
            } catch (RaiseException rj) {
                handleRaiseException(rj);
            }
        }

        return ruby;
    }

    @Override
    protected void endPreboot(RubyInstanceConfig config, Ruby ruby, String[] args) {
        super.endPreboot(config, ruby, args);

        try {
            Core.getGlobalContext().register(new JRubyContext());
            Core.checkpointRestore();
        } catch (CheckpointException | RestoreException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static class JRubyContext extends Context<Resource> {
        @Override
        public void beforeCheckpoint(Context<? extends Resource> context) throws CheckpointException {}

        @Override
        public void afterRestore(Context<? extends Resource> context) throws RestoreException {
            Options.PROPERTIES.forEach(Option::reload);
        }

        @Override
        public void register(Resource resource) {}
    }
}
