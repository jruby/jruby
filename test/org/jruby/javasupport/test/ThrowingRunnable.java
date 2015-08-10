package org.jruby.javasupport.test;

/**
 * https://github.com/jruby/jruby/issues/3177
 * 
 * @see test_backtraces.rb
 */
public class ThrowingRunnable {

    final Runnable runnable;

    public ThrowingRunnable(Runnable run) {
        runnable = run;
    }

    public void doRun(boolean fail) throws Exception {
        if (fail) throw new Exception();
        runnable.run();
    }

}
