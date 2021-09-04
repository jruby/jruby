package org.jruby.util.func;

public interface ObjectIntIntFunction<T, R> {
    R apply(T t, int i, int j);
}
