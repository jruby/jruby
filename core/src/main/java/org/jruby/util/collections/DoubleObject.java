package org.jruby.util.collections;

/**
 * A carrier object with two fields
 * @param <T> one
 * @param <U> two
 */
public class DoubleObject<T, U> {
    public DoubleObject() {}

    public DoubleObject(T object1, U object2) {
        this.object1 = object1;
        this.object2 = object2;
    }

    public T object1;
    public U object2;
}
