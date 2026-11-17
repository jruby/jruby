package org.jruby.util.func;

/**
 * It consumes three things.
 * @param <A> a
 * @param <B> b
 * @param <C> c
 */
public interface TriConsumer<A, B, C> {
    void accept(A a, B b, C c);
}
