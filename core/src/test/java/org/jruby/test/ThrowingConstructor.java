package org.jruby.test;

public class ThrowingConstructor {
    // public ThrowingConstructor() { }

    public ThrowingConstructor(Integer param) {
        if (param == null) throw new IllegalStateException();
        if (param < 0) throw new IllegalStateException("param == " + param);
    }
}