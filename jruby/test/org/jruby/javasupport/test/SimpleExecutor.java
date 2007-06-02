package org.jruby.javasupport.test;

public class SimpleExecutor {
    public void execute(Runnable r) {
        r.run();
    }
}