/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.core.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.runtime.NilPlaceholder;

import java.util.Arrays;

/**
 * A store for an array of {@code long}.
 */
public final class LongArrayStore extends BaseArrayStore {

    private long[] values;

    public LongArrayStore() {
        this(new long[]{});
    }

    public LongArrayStore(long[] values) {
        this.values = values;
        size = values.length;
        capacity = values.length;
    }

    @Override
    public Object get(int normalisedIndex) {
        try {
            return getLongFixnum(normalisedIndex);
        } catch (UnexpectedResultException e) {
            return e.getResult();
        }
    }

    public long getLongFixnum(int normalisedIndex) throws UnexpectedResultException {
        if (normalisedIndex >= size) {
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        }

        return values[normalisedIndex];
    }

    @Override
    public ArrayStore getRange(int normalisedBegin, int truncatedNormalisedExclusiveEnd) {
        if (normalisedBegin >= size) {
            return null; // Represents Nil
        }

        return new LongArrayStore(Arrays.copyOfRange(values, normalisedBegin, truncatedNormalisedExclusiveEnd));
    }

    @Override
    public void set(int normalisedIndex, Object value) throws GeneraliseArrayStoreException {
        if (value instanceof Integer) {
            setLongFixnum(normalisedIndex, (int) value);
        } else if (value instanceof Long) {
            setLongFixnum(normalisedIndex, (long) value);
        } else {
            throw new GeneraliseArrayStoreException();
        }
    }

    public void setLongFixnum(int normalisedIndex, long value) throws GeneraliseArrayStoreException {
        if (normalisedIndex > size) {
            throw new GeneraliseArrayStoreException();
        }

        if (normalisedIndex == size) {
            push(value);
        } else {
            values[normalisedIndex] = value;
        }
    }

    @Override
    public void setRangeSingle(int normalisedBegin, int truncatedNormalisedExclusiveEnd, Object value) throws GeneraliseArrayStoreException {
        if (value instanceof Integer) {
            setRangeSingleFixnum(normalisedBegin, truncatedNormalisedExclusiveEnd, (int) value);
        } else if (value instanceof Long) {
            setRangeSingleFixnum(normalisedBegin, truncatedNormalisedExclusiveEnd, (long) value);
        } else {
            throw new GeneraliseArrayStoreException();
        }
    }

    public void setRangeSingleFixnum(int normalisedBegin, int truncatedNormalisedExclusiveEnd, long value) {
        // Is the range the whole array?

        if (normalisedBegin == 0 && truncatedNormalisedExclusiveEnd == size) {
            // Reset length and set the value.
            size = 1;
            values[0] = value;
        } else {
            // Delete the range, except for the first value.
            deleteSpace(normalisedBegin + 1, truncatedNormalisedExclusiveEnd - normalisedBegin - 1);

            // Set the value we left in.
            values[normalisedBegin] = value;
        }
    }

    @Override
    public void setRangeArray(int normalisedBegin, int normalisedExclusiveEnd, ArrayStore other) throws GeneraliseArrayStoreException {
        if (other instanceof LongArrayStore) {
            setRangeArrayFixnum(normalisedBegin, normalisedExclusiveEnd, (LongArrayStore) other);
        } else {
            throw new GeneraliseArrayStoreException();
        }
    }

    public void setRangeArrayFixnum(int normalisedBegin, int normalisedExclusiveEnd, LongArrayStore other) {
        setRangeArrayMatchingTypes(normalisedBegin, normalisedExclusiveEnd, other.values, other.size);
    }

    @Override
    public void insert(int normalisedIndex, Object value) throws GeneraliseArrayStoreException {
        if (value instanceof Integer) {
            insertLongFixnum(normalisedIndex, (int) value);
        } else if (value instanceof Long) {
            insertLongFixnum(normalisedIndex, (long) value);
        } else {
            throw new GeneraliseArrayStoreException();
        }
    }

    public void insertLongFixnum(int normalisedIndex, long value) throws GeneraliseArrayStoreException {
        if (normalisedIndex > size) {
            throw new GeneraliseArrayStoreException();
        }

        createSpace(normalisedIndex, 1);
        values[normalisedIndex] = value;
    }

    @Override
    public void push(Object value) throws GeneraliseArrayStoreException {
        if (value instanceof Integer) {
            pushLongFixnum((int) value);
        } else if (value instanceof Long) {
            pushLongFixnum((long) value);
        } else {
            throw new GeneraliseArrayStoreException();
        }
    }

    public void pushLongFixnum(long value) {
        createSpaceAtEnd(1);
        values[size - 1] = value;
    }

    @Override
    public Object deleteAt(int normalisedIndex) {
        try {
            return deleteAtLongFixnum(normalisedIndex);
        } catch (UnexpectedResultException e) {
            return e.getResult();
        }
    }

    public long deleteAtLongFixnum(int normalisedIndex) throws UnexpectedResultException {
        if (normalisedIndex >= size) {
            CompilerDirectives.transferToInterpreter();
            throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
        }

        final long value = values[normalisedIndex];

        deleteSpace(normalisedIndex, 1);

        return value;
    }

    @Override
    public ArrayStore dup() {
        return new LongArrayStore(Arrays.copyOf(values, size));
    }

    @Override
    public boolean contains(Object value) {
        final long longValue;

        if (value instanceof Integer) {
            longValue = (int) value;
        } else if (value instanceof Long) {
            longValue = (long) value;
        } else {
            return false;
        }

        for (int n = 0; n < size; n++) {
            if (values[n] == longValue) {
                return true;
            }
        }

        return false;
    }

    @Override
    public ArrayStore generalizeFor(Object type) {
        return new ObjectArrayStore(toObjectArray());
    }

    @Override
    public Object getIndicativeValue() {
        return 0;
    }

    @Override
    protected void setCapacityByCopying(int newCapacity) {
        values = Arrays.copyOf(values, newCapacity);
        capacity = values.length;
    }

    @Override
    protected void setCapacityWithNewArray(int newCapacity) {
        values = new long[newCapacity];
        capacity = values.length;
    }

    public long[] getValues() {
        return values;
    }

    @Override
    protected Object getValuesArrayObject() {
        return values;
    }

    @Override
    public Object[] toObjectArray() {
        final Object[] objectValues = new Object[size];

        // System.arraycopy will not box.

        for (int n = 0; n < size; n++) {
            objectValues[n] = values[n];
        }

        return objectValues;
    }

    @Override
    public boolean equals(ArrayStore other) {
        if (other instanceof LongArrayStore) {
            return equals((LongArrayStore) other);
        } else {
            return super.equals(other);
        }
    }

    public boolean equals(LongArrayStore other) {
        if (other == null) {
            return false;
        } else if (other == this) {
            return true;
        } else if (other.size != size) {
            return false;
        } else if (other.capacity == capacity) {
            return Arrays.equals(other.values, values);
        } else {
            for (int n = 0; n < size; n++) {
                if (other.values[n] != values[n]) {
                    return false;
                }
            }

            return true;
        }
    }
}
