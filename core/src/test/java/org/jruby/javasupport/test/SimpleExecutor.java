package org.jruby.javasupport.test;

public class SimpleExecutor {
    public static class WrappedByMethodCall {
        public void execute(Runnable r) {
            r.run();
        }
    }

    public static class WrappedByConstructor {
        private Runnable r;
        public WrappedByConstructor(Runnable r) {
            this.r = r;
        }
        public void execute() {
            r.run();
        }
    }
    
    public static class MultipleArguments {
        public void execute(int count, Runnable r) {
            for (int i = 0; i < count; i++) {
                r.run();
            }
        }
    }
}