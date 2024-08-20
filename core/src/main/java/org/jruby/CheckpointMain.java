package org.jruby;

import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;

public class CheckpointMain {
    static RubyInstanceConfig checkpointConfig;
    static Ruby checkpointRuby;

    public static void main(String[] args) {
        // warm up JRuby internals
        String loopsEnv = System.getenv("JRUBY_CHECKPOINT_LOOPS");
        int loops = loopsEnv == null ? 10 : Integer.valueOf(loopsEnv);

        String codeEnv = System.getenv("JRUBY_CHECKPOINT_CODE");
        String code = codeEnv == null ? "require 'rubygems'" : codeEnv;

        for (int i = 0; i < loops; i++) {
            Ruby ruby = Ruby.newInstance();
            ruby.evalScriptlet(code);
            ruby.tearDown();
        }

        Ruby.clearGlobalRuntime();

        checkpointConfig = new RubyInstanceConfig();
        checkpointRuby = Ruby.newInstance(checkpointConfig);

        try {
            Core.checkpointRestore();
        } catch (CheckpointException | RestoreException e) {
            e.printStackTrace();
        }

        System.exit(1);

        // if no restore main used, proceed with normal main and prepared runtime
//        Main main = new Main(true);
//        Main.checkpointMain(true, args);
    }
}
