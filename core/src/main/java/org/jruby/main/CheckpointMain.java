package org.jruby.main;

import com.headius.options.Option;
import org.crac.CheckpointException;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.crac.RestoreException;
import org.jruby.Main;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CheckpointMain extends PrebootMain {
    public static void main(String[] args) {
        preboot(new CheckpointMain(), args);
    }

    protected String[] jrubyWarmup(Ruby ruby, String[] args) {
        // If more arguments were provided, run them as normal before checkpointing
        if (args.length > 0) {
            RubyInstanceConfig config = ruby.getInstanceConfig();
            InputStream in   = config.getScriptSource();
            String filename  = config.displayedFileName();

            Main.Status status = null;
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

            // arguments should have been consumed, so return empty array
            return new String[0];
        }

        // otherwise fall back on PrebootMain warmup
        return super.jrubyWarmup(ruby, args);
    }

    @Override
    protected void endPreboot(String[] args) {
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
