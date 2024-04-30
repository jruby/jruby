package org.jruby.util.func;

public interface ObjectObjectIntFunction<T, U, R> {
    R apply(T t, U u, int j);
}
