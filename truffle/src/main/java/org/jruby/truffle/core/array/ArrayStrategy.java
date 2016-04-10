package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.Layouts;

public abstract class ArrayStrategy {

    public static ArrayStrategy of(DynamicObject array) {
        CompilerAsserts.neverPartOfCompilation();
        if (ArrayGuards.isIntArray(array)) {
            return IntArrayStrategy.INSTANCE;
        } else if (ArrayGuards.isLongArray(array)) {
            return LongArrayStrategy.INSTANCE;
        } else if (ArrayGuards.isDoubleArray(array)) {
            return DoubleArrayStrategy.INSTANCE;
        } else if (ArrayGuards.isObjectArray(array)) {
            return ObjectArrayStrategy.INSTANCE;
        } else {
            return NullArrayStrategy.INSTANCE;
            // throw new UnsupportedOperationException();
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

    private static class IntArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new IntArrayStrategy();

        public ArrayMirror newArray(int size) {
            return new IntegerArrayMirror(new int[size]);
        }

        public Object newArrayWith(Object value) {
            return new int[] { (int) value };
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new IntegerArrayMirror((int[]) Layouts.ARRAY.getStore(array));
        }

        public boolean accepts(Object value) {
            return value instanceof Integer;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isIntArray(array);
        }

    }

    private static class LongArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new LongArrayStrategy();

        public ArrayMirror newArray(int size) {
            return new LongArrayMirror(new long[size]);
        }

        public Object newArrayWith(Object value) {
            return new long[] { (long) value };
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new LongArrayMirror((long[]) Layouts.ARRAY.getStore(array));
        }

        public boolean accepts(Object value) {
            return value instanceof Long;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isLongArray(array);
        }

    }

    private static class DoubleArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new DoubleArrayStrategy();

        public ArrayMirror newArray(int size) {
            return new DoubleArrayMirror(new double[size]);
        }

        public Object newArrayWith(Object value) {
            return new double[] { (double) value };
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new DoubleArrayMirror((double[]) Layouts.ARRAY.getStore(array));
        }

        public boolean accepts(Object value) {
            return value instanceof Double;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isDoubleArray(array);
        }

    }

    private static class ObjectArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new ObjectArrayStrategy();

        public ArrayMirror newArray(int size) {
            return new ObjectArrayMirror(new Object[size]);
        }

        public Object newArrayWith(Object value) {
            return new Object[] { value };
        }

        public ArrayMirror newMirror(DynamicObject array) {
            return new ObjectArrayMirror((Object[]) Layouts.ARRAY.getStore(array));
        }

        public boolean accepts(Object value) {
            return true;
        }

        public boolean matches(DynamicObject array) {
            return ArrayGuards.isObjectArray(array);
        }

    }

    private static class NullArrayStrategy extends ArrayStrategy {

        static final ArrayStrategy INSTANCE = new NullArrayStrategy();

        public ArrayMirror newArray(int size) {
            throw new UnsupportedOperationException();
        }

        public Object newArrayWith(Object value) {
            throw new UnsupportedOperationException();
        }

        public ArrayMirror newMirror(DynamicObject array) {
            throw new UnsupportedOperationException();
        }

        public boolean accepts(Object value) {
            return false;
        }

        public boolean matches(DynamicObject array) {
            throw new UnsupportedOperationException();
        }

    }

    public abstract ArrayMirror newArray(int size);

    public abstract Object newArrayWith(Object value);

    public abstract ArrayMirror newMirror(DynamicObject array);

    public abstract boolean accepts(Object value);

    public abstract boolean matches(DynamicObject array);

}
