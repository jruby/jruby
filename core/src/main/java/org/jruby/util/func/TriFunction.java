package org.jruby.util.func;

import org.jruby.runtime.load.LibrarySearcher;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Extension of {@link java.util.function.BiFunction} to three arguments.
 *
 * @param <T> first argument type for apply
 * @param <U> second argument type for apply
 * @param <V> third argument type for apply
 * @param <R> return value type for apply
 */
public interface TriFunction<T, U, V, R> {
    default <W> TriFunction<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
        return (t, u, v) -> after.apply(apply(t, u, v));
    }

    R apply(T t, U u, V v);
}
