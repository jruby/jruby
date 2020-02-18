package org.jruby.main;

import org.jruby.Main;
import org.jruby.Ruby;

public class NativeMain {
    // unused at runtime
    private static final Ruby NATIVE_INIT_RUNTIME = Ruby.newEphemeralInstance();

    static {
        NATIVE_INIT_RUNTIME.evalScriptlet("1");
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
