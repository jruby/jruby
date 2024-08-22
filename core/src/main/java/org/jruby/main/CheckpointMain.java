package org.jruby.main;

import org.crac.CheckpointException;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.crac.RestoreException;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.Options;

import java.io.IOException;

public class CheckpointMain extends PrebootMain {
    public static void main(String[] args) throws IOException {
        CheckpointMain dm = new CheckpointMain();
        dm.preboot(args);
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
        public void beforeCheckpoint(Context<? extends Resource> context) throws CheckpointException {

        }

        @Override
        public void afterRestore(Context<? extends Resource> context) throws RestoreException {
            Options.PARSER_SUMMARY.reload();
        }

        @Override
        public void register(Resource resource) {

        }
    }
}
