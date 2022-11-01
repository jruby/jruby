package org.jruby.util.func;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An aggregate interface for {@link Function} and {@link BiFunction} that properly replaces the default
 * {@link #andThen(Function)} for both superinterfaces.
 *
 * @param <T> first argument type for apply
 * @param <U> second argument type for apply
 * @param <V> third argument type for apply
 * @param <R> return value type for apply
 */
public interface FunctionOneOrTwoOrThree<T, U, V, R> extends Function<T, R>, BiFunction<T, U, R>, TriFunction<T, U, V, R> {
    @Override
    default <W> FunctionOneOrTwoOrThree<T, U, V, W> andThen(Function<? super R, ? extends W> after) {
        return new FunctionOneOrTwoOrThree<T, U, V, W>() {
            @Override
            public W apply(final T t, final U u, final V v) {
                return after.apply(FunctionOneOrTwoOrThree.this.apply(t, u, v));
            }

            @Override
            public W apply(final T t, final U u) {
                return after.apply(FunctionOneOrTwoOrThree.this.apply(t, u));
            }

            @Override
            public W apply(final T t) {
                return after.apply(FunctionOneOrTwoOrThree.this.apply(t));
            }
        };
    }
}