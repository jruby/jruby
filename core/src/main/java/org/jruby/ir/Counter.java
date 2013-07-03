package org.jruby.ir;

// Just an int-wrapper so that we can modify the counter in-place
// (unlike an Integer which is immutable)
public class Counter {
    public long count = 0;
}
