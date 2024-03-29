package org.jruby.util.func;

/**
 * It consumes three things.
 */
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
