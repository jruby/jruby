package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyGuards;

public abstract class ArrayStrategy {

    // ArrayStrategy interface

    public Class<?> type() {
        throw unsupported();
    }

    public boolean canStore(Class<?> type) {
        throw unsupported();
    }

    public abstract boolean accepts(Object value);

    public boolean specializesFor(Object value) {
        throw unsupported();
    }

    public abstract boolean matches(DynamicObject array);

    public abstract ArrayMirror newArray(int size);

    public abstract ArrayMirror newMirror(DynamicObject array);

    public abstract String toString();

    public ArrayStrategy generalize(ArrayStrategy other) {
        CompilerAsserts.neverPartOfCompilation();
        if (other == this) {
            return this;
        }
        for (ArrayStrategy generalized : TYPE_STRATEGIES) {
            if (generalized.canStore(type()) && generalized.canStore(other.type())) {
                return generalized;
            }
        }
        throw unsupported();
    }

    public ArrayStrategy generalizeFor(Object value) {
        return generalize(ArrayStrategy.forValue(value));
    }

    // Helpers

    protected RuntimeException unsupported() {
        return new UnsupportedOperationException(toString());
    }

    public static final ArrayStrategy[] TYPE_STRATEGIES = {
            IntArrayStrategy.INSTANCE,
            LongArrayStrategy.INSTANCE,
            DoubleArrayStrategy.INSTANCE,
            ObjectArrayStrategy.INSTANCE
    };

    public static ArrayStrategy of(DynamicObject array) {
        CompilerAsserts.neverPartOfCompilation();

        if (!RubyGuards.isRubyArray(array)) {
            return NullArrayStrategy.INSTANCE;
        }

        if (ArrayGuards.isIntArray(array)) {
            return IntArrayStrategy.INSTANCE;
        } else if (ArrayGuards.isLongArray(array)) {
            return LongArrayStrategy.INSTANCE;
        } else if (ArrayGuards.isDoubleArray(array)) {
            return DoubleArrayStrategy.INSTANCE;
        } else if (ArrayGuards.isObjectArray(array)) {
            return ObjectArrayStrategy.INSTANCE;
        } else {
            assert ArrayGuards.isNullArray(array);
            return NullArrayStrategy.INSTANCE;
        }
    }

    public static ArrayStrategy of(DynamicObject array, Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (ArrayGuards.isLongArray(array) && value instanceof Integer) {
            return LongIntArrayStrategy.INSTANCE;
        } else {
            return of(array);
        }
    }

    public static ArrayStrategy forValue(Object value) {
        CompilerAsserts.neverPartOfCompilation();
        if (value instanceof Integer) {
            return IntArrayStrategy.INSTANCE;
        } else if (value instanceof Long) {
            return LongArrayStrategy.INSTANCE;
        } else if (value instanceof Double) {
            return DoubleArrayStrategy.INSTANCE;
        } else {
            return ObjectArrayStrategy.INSTANCE;
        }
    }

    // Type strategies (int, long, double, Object)

    private static class IntArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new IntArrayStrategy();

        public Class<?> type() {
            return Integer.class;
        }

        public boolean canStore(Class<?> type) {
            return type == Integer.class;
        }

        public boolean accepts(Object value) {
            return value instanceof Integer;
        }

        public boolean specializesFor(Object value) {
            return value instanceof Integer;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isIntArray(array);
        }

        @Override
        public ArrayStrategy generalize(ArrayStrategy other) {
            CompilerAsserts.neverPartOfCompilation();
            if (other == this) {
                return this;
            } else if (other == LongArrayStrategy.INSTANCE) {
                return LongArrayStrategy.INSTANCE;
            } else {
                return IntToObjectGeneralizationArrayStrategy.INSTANCE;
            }
        }

        public ArrayMirror newArray(int size) {
            return new IntegerArrayMirror(new int[size]);
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new IntegerArrayMirror((int[]) Layouts.ARRAY.getStore(array));
        }

        public String toString() {
            return "int[]";
        }

    }

    private static class LongArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new LongArrayStrategy();

        public Class<?> type() {
            return Long.class;
        }

        public boolean canStore(Class<?> type) {
            return type == Long.class || type == Integer.class;
        }

        public boolean accepts(Object value) {
            return value instanceof Long;
        }

        public boolean specializesFor(Object value) {
            return value instanceof Long;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isLongArray(array);
        }

        public ArrayMirror newArray(int size) {
            return new LongArrayMirror(new long[size]);
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new LongArrayMirror((long[]) Layouts.ARRAY.getStore(array));
        }

        public String toString() {
            return "long[]";
        }

    }

    private static class DoubleArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new DoubleArrayStrategy();

        public Class<?> type() {
            return Double.class;
        }

        public boolean canStore(Class<?> type) {
            return type == Double.class;
        }

        public boolean accepts(Object value) {
            return value instanceof Double;
        }

        public boolean specializesFor(Object value) {
            return value instanceof Double;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isDoubleArray(array);
        }

        public ArrayMirror newArray(int size) {
            return new DoubleArrayMirror(new double[size]);
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new DoubleArrayMirror((double[]) Layouts.ARRAY.getStore(array));
        }

        public String toString() {
            return "double[]";
        }

    }

    private static class ObjectArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new ObjectArrayStrategy();

        public Class<?> type() {
            return Object.class;
        }

        public boolean canStore(Class<?> type) {
            return true;
        }

        public boolean accepts(Object value) {
            return true;
        }

        public boolean specializesFor(Object value) {
            return !(value instanceof Integer) && !(value instanceof Long) && !(value instanceof Double);
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isObjectArray(array);
        }

        public ArrayMirror newArray(int size) {
            return new ObjectArrayMirror(new Object[size]);
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new ObjectArrayMirror((Object[]) Layouts.ARRAY.getStore(array));
        }

        public String toString() {
            return "Object[]";
        }

    }

    // Specific generalization strategies to handle int => long

    /** long[] accepting int */
    private static class LongIntArrayStrategy extends LongArrayStrategy {

        static final ArrayStrategy INSTANCE = new LongIntArrayStrategy();

        public boolean accepts(Object value) {
            return value instanceof Integer;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isLongArray(array);
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new LongIntArrayMirror((long[]) Layouts.ARRAY.getStore(array));
        }

        public String toString() {
            return "(long[], int)";
        }

    }

    /** Object[] not accepting long */
    private static class IntToObjectGeneralizationArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new IntToObjectGeneralizationArrayStrategy();

        public boolean accepts(Object value) {
            return !(value instanceof Long);
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isObjectArray(array);
        }

        public ArrayMirror newArray(int size) {
            return new ObjectArrayMirror(new Object[size]);
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new ObjectArrayMirror((Object[]) Layouts.ARRAY.getStore(array));
        }

        public String toString() {
            return "Object[] (not accepting long)";
        }

    }

    // Null-pattern strategy

    private static class NullArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new NullArrayStrategy();

        public boolean accepts(Object value) {
            return false;
        }

        public boolean matches(DynamicObject array) {
            return false;
        }

        @Override
        public ArrayStrategy generalize(ArrayStrategy other) {
            return other;
        }

        public ArrayMirror newArray(int size) {
            throw unsupported();
        }

        public ArrayMirror newMirror(DynamicObject array) {
            throw unsupported();
        }

        public String toString() {
            return "null";
        }

    }

}
