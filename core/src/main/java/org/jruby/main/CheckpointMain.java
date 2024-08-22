package org.jruby.main;

import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.util.cli.Options;

import java.io.IOException;

public class CheckpointMain extends PrebootMain {
    public static void main(String[] args) throws IOException {
        CheckpointMain dm = new CheckpointMain();
        dm.warmup();
    }

    @Override
    void prepareOptions() {
        // Disable native stdio when running under Drip (#4942)
        Options.NATIVE_STDIO.force("false");
    }

    @Override
    void endPreboot() {
        try {
            Core.checkpointRestore();
        } catch (CheckpointException | RestoreException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
