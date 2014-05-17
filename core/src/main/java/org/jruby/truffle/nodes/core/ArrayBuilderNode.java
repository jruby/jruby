/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;
import org.jruby.truffle.runtime.ArrayUtils;
import org.jruby.truffle.runtime.RubyContext;

public abstract class ArrayBuilderNode extends Node {

    private final RubyContext context;

    public ArrayBuilderNode(RubyContext context) {
        this.context = context;
    }

    public abstract Object startExactLength(int length);

    public abstract Object append(Object store, int index, Object value);

    public void finish() {
    }

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

        @Override
        public Object startExactLength(int length) {
            CompilerDirectives.transferToInterpreter();

            return new Object[length];
        }

        @Override
        public Object append(Object store, int index, Object value) {
            CompilerDirectives.transferToInterpreter();

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

            ((Object[]) store)[index] = value;
            return store;
        }

        @Override
        public void finish() {
            if (couldUseInteger) {
                replace(new IntegerArrayBuilderNode(getContext()));
            } else if (couldUseLong) {
                replace(new LongArrayBuilderNode(getContext()));
            } else if (couldUseDouble) {
                replace(new DoubleArrayBuilderNode(getContext()));
            } else {
                replace(new ObjectArrayBuilderNode(getContext()));
            }
        }

    }

    public static class IntegerArrayBuilderNode extends ArrayBuilderNode {

        public IntegerArrayBuilderNode(RubyContext context) {
            super(context);
        }

        @Override
        public Object startExactLength(int length) {
            return new int[length];
        }

        @Override
        public Object append(Object store, int index, Object value) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY, value instanceof Integer)) {
                ((int[]) store)[index] = (int) value;
                return store;
            } else {
                CompilerDirectives.transferToInterpreter();

                replace(new ObjectArrayBuilderNode(getContext()));

                final Object[] newStore = ArrayUtils.box((int[]) store);
                newStore[index] = value;
                return newStore;
            }
        }

    }

    public static class LongArrayBuilderNode extends ArrayBuilderNode {

        public LongArrayBuilderNode(RubyContext context) {
            super(context);
        }

        @Override
        public Object startExactLength(int length) {
            return new long[length];
        }

        @Override
        public Object append(Object store, int index, Object value) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.LIKELY_PROBABILITY, value instanceof Integer)) {
                ((long[]) store)[index] = (long) value;
                return store;
            } else if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY, value instanceof Integer)) {
                ((long[]) store)[index] = (int) value;
                return store;
            } else {
                CompilerDirectives.transferToInterpreter();

                replace(new ObjectArrayBuilderNode(getContext()));

                final Object[] newStore = ArrayUtils.box((long[]) store);
                newStore[index] = value;
                return newStore;
            }
        }

    }

    public static class DoubleArrayBuilderNode extends ArrayBuilderNode {

        public DoubleArrayBuilderNode(RubyContext context) {
            super(context);
        }

        @Override
        public Object startExactLength(int length) {
            return new double[length];
        }

        @Override
        public Object append(Object store, int index, Object value) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.FASTPATH_PROBABILITY, value instanceof Double)) {
                ((double[]) store)[index] = (double) value;
                return store;
            } else {
                CompilerDirectives.transferToInterpreter();

                replace(new ObjectArrayBuilderNode(getContext()));

                final Object[] newStore = ArrayUtils.box((double[]) store);
                newStore[index] = value;
                return newStore;
            }
        }

    }

    public static class ObjectArrayBuilderNode extends ArrayBuilderNode {

        public ObjectArrayBuilderNode(RubyContext context) {
            super(context);
        }

        @Override
        public Object startExactLength(int length) {
            return new Object[length];
        }

        @Override
        public Object append(Object store, int index, Object value) {
            ((Object[]) store)[index] = value;
            return store;
        }

    }

}
