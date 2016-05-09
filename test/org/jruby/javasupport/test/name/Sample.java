package org.jruby.javasupport.test.name;

public class Sample {
    public Sample() { }

    public Sample(int param) { // @see test_backtraces.rb
        if (param == -1) {
            throw new IllegalStateException("param == -1");
        }
    }
}