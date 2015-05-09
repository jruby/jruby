/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.array;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.cli.Options;

import java.util.Arrays;

/*
 * TODO(CS): how does this work when when multithreaded? Could a node get replaced by someone else and
 * then suddenly you're passing it a store type it doesn't expect?
 */

public abstract class ArrayBuilderNode extends Node {

    public static final int ARRAYS_UNINITIALIZED_SIZE = Options.TRUFFLE_ARRAYS_UNINITIALIZED_SIZE.load();

    private final RubyContext context;

    public ArrayBuilderNode(RubyContext context) {
        this.context = context;
    }

    public abstract Object start();
    public abstract Object start(int length);
    public abstract Object ensure(Object store, int length);
    public abstract Object append(Object store, int index, RubyArray array);
    public abstract Object append(Object store, int index, Object value);
    public abstract Object finish(Object store, int length);

    protected RubyContext getContext() {
        return context;
    }

    public static class UninitializedArrayBuilderNode extends ArrayBuilderNode {

        private boolean couldUseInteger = true;
        private boolean couldUseLong = true;
        private boolean couldUseDouble = true;

        public UninitializedArrayBuilderNode(RubyContext context) {
            super(context);
        }

        public void resume(Object[] store) {
            for (Object value : store) {
                screen(value);
            }
        }

        @Override
        public Object start() {
            CompilerDirectives.transferToInterpreter();
            return new Object[ARRAYS_UNINITIALIZED_SIZE];
        }

        @Override
        public Object start(int length) {
            CompilerDirectives.transferToInterpreter();
            return new Object[length];
        }

        @Override
        public Object ensure(Object store, int length) {
            // All appends go through append(Object, int, Object), which is always happy to make space
            return store;
        }

        @Override
        public Object append(Object store, int index, RubyArray array) {
            CompilerDirectives.transferToInterpreter();

            for (Object value : array.slowToArray()) {
                store = append(store, index, value);
                index++;
            }

            return store;
        }

        @Override
        public Object append(Object store, int index, Object value) {
            CompilerDirectives.transferToInterpreter();

            screen(value);

            Object[] storeArray = (Object[]) store;

            if (index >= storeArray.length) {
                storeArray = Arrays.copyOf(storeArray, ArrayUtils.capacity(storeArray.length, index + 1));
            }

            storeArray[index] = value;
            return storeArray;
        }

        @Override
        public Object finish(Object store, int length) {
            if (couldUseInteger) {
                replace(new IntegerArrayBuilderNode(getContext(), length));
                return ArrayUtils.unboxInteger((Object[]) store, length);
            } else if (couldUseLong) {
                replace(new LongArrayBuilderNode(getContext(), length));
                return ArrayUtils.unboxLong((Object[]) store, length);
            } else if (couldUseDouble) {
                replace(new DoubleArrayBuilderNode(getContext(), length));
                return ArrayUtils.unboxDouble((Object[]) store, length);
            } else {
                replace(new ObjectArrayBuilderNode(getContext(), length));
                return store;
            }
        }

        private void screen(Object value) {
            if (value instanceof Integer) {
                couldUseDouble = false;
            } else if (value instanceof Long) {
                couldUseInteger = false;
                couldUseDouble = false;
            } else if (value instanceof Double) {
                couldUseInteger = false;
                couldUseLong = false;
            } else {
                couldUseInteger = false;
                couldUseLong = false;
                couldUseDouble = false;
            }
        }

    }

    public static class IntegerArrayBuilderNode extends ArrayBuilderNode {

        private final int expectedLength;

        @CompilerDirectives.CompilationFinal private boolean hasAppendedIntegerArray = false;

        public IntegerArrayBuilderNode(RubyContext context, int expectedLength) {
            super(context);
            this.expectedLength = expectedLength;
        }

        @Override
        public Object start() {
            return new int[expectedLength];
        }

        @Override
        public Object start(int length) {
            if (length > expectedLength) {
                CompilerDirectives.transferToInterpreter();

                final UninitializedArrayBuilderNode newNode = new UninitializedArrayBuilderNode(getContext());
                replace(newNode);
                return newNode.start(length);
            }

            return new int[expectedLength];
        }

        @Override
        public Object ensure(Object store, int length) {
            if (length > ((int[]) store).length) {
                CompilerDirectives.transferToInterpreter();

                final Object[] newStore = ArrayUtils.box((int[]) store);

                final UninitializedArrayBuilderNode newNode = new UninitializedArrayBuilderNode(getContext());
                replace(newNode);
                newNode.resume(newStore);
                return newNode.ensure(newStore, length);
            }

            return store;
        }

        @Override
        public Object append(Object store, int index, RubyArray array) {
            Object otherStore = array.getStore();

            if (otherStore == null) {
                return store;
            }

            if (hasAppendedIntegerArray && otherStore instanceof int[]) {
                System.arraycopy(otherStore, 0, store, index, array.getSize());
                return store;
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();

            if (otherStore instanceof int[]) {
                hasAppendedIntegerArray = true;
                System.arraycopy(otherStore, 0, store, index, array.getSize());
                return store;
            }

            CompilerDirectives.transferToInterpreter();

            replace(new ObjectArrayBuilderNode(getContext(), expectedLength));
            final Object[] newStore = ArrayUtils.box((int[]) store);
            System.arraycopy(otherStore, 0, newStore, index, array.getSize());

            return newStore;
        }

        @Override
        public Object append(Object store, int index, Object value) {
            // TODO(CS): inject probability
            if (store instanceof int[] && value instanceof Integer) {
                ((int[]) store)[index] = (int) value;
                return store;
            } else {
                CompilerDirectives.transferToInterpreter();

                replace(new ObjectArrayBuilderNode(getContext(), expectedLength));

                // TODO(CS): not sure why this happens - need to investigate

                final Object[] newStore;

                if (store instanceof int[]) {
                    newStore = ArrayUtils.box((int[]) store);
                } else if (store instanceof Object[]) {
                    newStore = (Object[]) store;
                } else {
                    throw new UnsupportedOperationException();
                }

                newStore[index] = value;
                return newStore;
            }
        }

        public Object finish(Object store, int length) {
            return store;
        }

    }

    public static class LongArrayBuilderNode extends ArrayBuilderNode {

        private final int expectedLength;

        public LongArrayBuilderNode(RubyContext context, int expectedLength) {
            super(context);
            this.expectedLength = expectedLength;
        }

        @Override
        public Object start() {
            return new long[expectedLength];
        }

        @Override
        public Object start(int length) {
            if (length > expectedLength) {
                CompilerDirectives.transferToInterpreter();

                final UninitializedArrayBuilderNode newNode = new UninitializedArrayBuilderNode(getContext());
                replace(newNode);
                return newNode.start(length);
            }

            return new long[expectedLength];
        }

        @Override
        public Object ensure(Object store, int length) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        @Override
        public Object append(Object store, int index, RubyArray array) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object append(Object store, int index, Object value) {
            // TODO(CS): inject probability
            if (store instanceof long[] && value instanceof Long) {
                ((long[]) store)[index] = (long) value;
                return store;
            } else if (value instanceof Integer) {
                ((long[]) store)[index] = (int) value;
                return store;
            } else {
                CompilerDirectives.transferToInterpreter();

                replace(new ObjectArrayBuilderNode(getContext(), expectedLength));

                final Object[] newStore = ArrayUtils.box((long[]) store);
                newStore[index] = value;
                return newStore;
            }
        }

        public Object finish(Object store, int length) {
            return store;
        }

    }

    public static class DoubleArrayBuilderNode extends ArrayBuilderNode {

        private final int expectedLength;

        public DoubleArrayBuilderNode(RubyContext context, int expectedLength) {
            super(context);
            this.expectedLength = expectedLength;
        }

        @Override
        public Object start() {
            return new double[expectedLength];
        }

        @Override
        public Object start(int length) {
            if (length > expectedLength) {
                CompilerDirectives.transferToInterpreter();

                final UninitializedArrayBuilderNode newNode = new UninitializedArrayBuilderNode(getContext());
                replace(newNode);
                return newNode.start(length);
            }

            return new double[expectedLength];
        }

        @Override
        public Object ensure(Object store, int length) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        @Override
        public Object append(Object store, int index, RubyArray array) {
            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        @Override
        public Object append(Object store, int index, Object value) {
            // TODO(CS): inject probability
            if (store instanceof double[] && value instanceof Double) {
                ((double[]) store)[index] = (double) value;
                return store;
            } else {
                CompilerDirectives.transferToInterpreter();

                replace(new ObjectArrayBuilderNode(getContext(), expectedLength));

                final Object[] newStore = ArrayUtils.box((double[]) store);
                newStore[index] = value;
                return newStore;
            }
        }

        public Object finish(Object store, int length) {
            return store;
        }

    }

    public static class ObjectArrayBuilderNode extends ArrayBuilderNode {

        private final int expectedLength;

        @CompilerDirectives.CompilationFinal private boolean hasAppendedObjectArray = false;
        @CompilerDirectives.CompilationFinal private boolean hasAppendedIntArray = false;

        public ObjectArrayBuilderNode(RubyContext context, int expectedLength) {
            super(context);
            this.expectedLength = expectedLength;
        }

        @Override
        public Object start() {
            return new Object[expectedLength];
        }

        @Override
        public Object start(int length) {
            if (length > expectedLength) {
                CompilerDirectives.transferToInterpreter();

                final UninitializedArrayBuilderNode newNode = new UninitializedArrayBuilderNode(getContext());
                replace(newNode);
                return newNode.start(length);
            }

            return new Object[expectedLength];
        }

        @Override
        public Object ensure(Object store, int length) {
            if (length > ((Object[]) store).length) {
                CompilerDirectives.transferToInterpreter();

                final UninitializedArrayBuilderNode newNode = new UninitializedArrayBuilderNode(getContext());
                replace(newNode);
                newNode.resume((Object[]) store);
                return newNode.ensure(store, length);
            }

            return store;
        }

        @Override
        public Object append(Object store, int index, RubyArray array) {
            Object otherStore = array.getStore();

            if (otherStore == null) {
                return store;
            }

            if (hasAppendedObjectArray && otherStore instanceof Object[]) {
                System.arraycopy(otherStore, 0, store, index, array.getSize());
                return store;
            }

            if (hasAppendedIntArray && otherStore instanceof int[]) {
                final Object[] objectStore = (Object[]) store;
                final int[] otherIntStore = (int[]) otherStore;

                for (int n = 0; n < array.getSize(); n++) {
                    objectStore[index + n] = otherIntStore[n];
                }

                return store;
            }

            CompilerDirectives.transferToInterpreterAndInvalidate();

            if (otherStore instanceof int[]) {
                hasAppendedIntArray = true;
                for (int n = 0; n < array.getSize(); n++) {
                    ((Object[]) store)[index + n] = ((int[]) otherStore)[n];
                }

                return store;
            }

            if (otherStore instanceof long[]) {
                for (int n = 0; n < array.getSize(); n++) {
                    ((Object[]) store)[index + n] = ((long[]) otherStore)[n];
                }

                return store;
            }

            if (otherStore instanceof double[]) {
                for (int n = 0; n < array.getSize(); n++) {
                    ((Object[]) store)[index + n] = ((double[]) otherStore)[n];
                }

                return store;
            }

            if (otherStore instanceof Object[]) {
                hasAppendedObjectArray = true;
                System.arraycopy(otherStore, 0, store, index, array.getSize());
                return store;
            }

            throw new UnsupportedOperationException(array.getStore().getClass().getName());
        }

        @Override
        public Object append(Object store, int index, Object value) {
            if (index >= ((Object[]) store).length) {
                new Exception().printStackTrace();
            }

            ((Object[]) store)[index] = value;
            return store;
        }

        public Object finish(Object store, int length) {
            return store;
        }

    }

}
