/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.array;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.language.objects.AllocateObjectNodeGen;

import java.util.Arrays;

public abstract class ArrayLiteralNode extends RubyNode {

    @Children protected final RubyNode[] values;
    @Child protected AllocateObjectNode allocateObjectNode;

    public ArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
        super(context, sourceSection);
        this.values = values;
        allocateObjectNode = AllocateObjectNodeGen.create(context, sourceSection, false, null, null);
    }

    protected DynamicObject makeGeneric(VirtualFrame frame, Object[] alreadyExecuted) {
        CompilerAsserts.neverPartOfCompilation();

        replace(new ObjectArrayLiteralNode(getContext(), getSourceSection(), values));

        final Object[] executedValues = new Object[values.length];

        for (int n = 0; n < values.length; n++) {
            if (n < alreadyExecuted.length) {
                executedValues[n] = alreadyExecuted[n];
            } else {
                executedValues[n] = values[n].execute(frame);
            }
        }

        return allocateObjectNode.allocate(coreLibrary().getArrayClass(), executedValues, executedValues.length);
    }

    @Override
    public abstract Object execute(VirtualFrame frame);

    @ExplodeLoop
    @Override
    public void executeVoid(VirtualFrame frame) {
        for (RubyNode value : values) {
            value.executeVoid(frame);
        }
    }

    @ExplodeLoop
    @Override
    public Object isDefined(VirtualFrame frame) {
        for (RubyNode value : values) {
            if (value.isDefined(frame) == nil()) {
                return nil();
            }
        }

        return super.isDefined(frame);
    }

    // TODO(CS): remove this - shouldn't be fiddling with nodes from the outside
    public RubyNode[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    public static class EmptyArrayLiteralNode extends ArrayLiteralNode {

        public EmptyArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
            super(context, sourceSection, values);
        }

        @Override
        public Object execute(VirtualFrame frame) {
            return allocateObjectNode.allocate(coreLibrary().getArrayClass(), null, 0);
        }

    }

    public static class FloatArrayLiteralNode extends ArrayLiteralNode {

        public FloatArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
            super(context, sourceSection, values);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            final double[] executedValues = new double[values.length];

            for (int n = 0; n < values.length; n++) {
                try {
                    executedValues[n] = values[n].executeDouble(frame);
                } catch (UnexpectedResultException e) {
                    return makeGeneric(frame, executedValues, n);
                }
            }

            return allocateObjectNode.allocate(coreLibrary().getArrayClass(), executedValues, values.length);
        }

        private DynamicObject makeGeneric(VirtualFrame frame,
                final double[] executedValues, int n) {
            final Object[] executedObjects = new Object[n];

            for (int i = 0; i < n; i++) {
                executedObjects[i] = executedValues[i];
            }

            return makeGeneric(frame, executedObjects);
        }

    }

    public static class IntegerFixnumArrayLiteralNode extends ArrayLiteralNode {

        public IntegerFixnumArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
            super(context, sourceSection, values);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            final int[] executedValues = new int[values.length];

            for (int n = 0; n < values.length; n++) {
                try {
                    executedValues[n] = values[n].executeInteger(frame);
                } catch (UnexpectedResultException e) {
                    return makeGeneric(frame, executedValues, n);
                }
            }

            return allocateObjectNode.allocate(coreLibrary().getArrayClass(), executedValues, values.length);
        }

        private DynamicObject makeGeneric(VirtualFrame frame,
                final int[] executedValues, int n) {
            final Object[] executedObjects = new Object[n];

            for (int i = 0; i < n; i++) {
                executedObjects[i] = executedValues[i];
            }

            return makeGeneric(frame, executedObjects);
        }

    }

    public static class LongFixnumArrayLiteralNode extends ArrayLiteralNode {

        public LongFixnumArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
            super(context, sourceSection, values);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            final long[] executedValues = new long[values.length];

            for (int n = 0; n < values.length; n++) {
                try {
                    executedValues[n] = values[n].executeLong(frame);
                } catch (UnexpectedResultException e) {
                    return makeGeneric(frame, executedValues, n);
                }
            }

            return allocateObjectNode.allocate(coreLibrary().getArrayClass(), executedValues, values.length);
        }

        private DynamicObject makeGeneric(VirtualFrame frame,
                final long[] executedValues, int n) {
            final Object[] executedObjects = new Object[n];

            for (int i = 0; i < n; i++) {
                executedObjects[i] = executedValues[i];
            }

            return makeGeneric(frame, executedObjects);
        }

    }

    public static class ObjectArrayLiteralNode extends ArrayLiteralNode {

        public ObjectArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
            super(context, sourceSection, values);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            final Object[] executedValues = new Object[values.length];

            for (int n = 0; n < values.length; n++) {
                executedValues[n] = values[n].execute(frame);
            }

            return allocateObjectNode.allocate(coreLibrary().getArrayClass(), executedValues, values.length);
        }

    }

    public static class UninitialisedArrayLiteralNode extends ArrayLiteralNode {

        public UninitialisedArrayLiteralNode(RubyContext context, SourceSection sourceSection, RubyNode[] values) {
            super(context, sourceSection, values);
        }

        @ExplodeLoop
        @Override
        public Object execute(VirtualFrame frame) {
            CompilerDirectives.transferToInterpreter();

            final Object[] executedValues = new Object[values.length];

            for (int n = 0; n < values.length; n++) {
                executedValues[n] = values[n].execute(frame);
            }

            final DynamicObject array = allocateObjectNode.allocate(coreLibrary().getArrayClass(), storeSpecialisedFromObjects(executedValues), executedValues.length);
            final Object store = Layouts.ARRAY.getStore(array);

            if (store == null) {
                replace(new EmptyArrayLiteralNode(getContext(), getSourceSection(), values));
            } if (store instanceof int[]) {
                replace(new IntegerFixnumArrayLiteralNode(getContext(), getSourceSection(), values));
            } else if (store instanceof long[]) {
                replace(new LongFixnumArrayLiteralNode(getContext(), getSourceSection(), values));
            } else if (store instanceof double[]) {
                replace(new FloatArrayLiteralNode(getContext(), getSourceSection(), values));
            } else {
                replace(new ObjectArrayLiteralNode(getContext(), getSourceSection(), values));
            }

            return array;
        }

        public Object storeSpecialisedFromObjects(Object... objects) {
            if (objects.length == 0) {
                return null;
            }

            boolean canUseInteger = true;
            boolean canUseLong = true;
            boolean canUseDouble = true;

            for (Object object : objects) {
                if (object instanceof Integer) {
                    canUseDouble = false;
                } else if (object instanceof Long) {
                    canUseInteger = canUseInteger && CoreLibrary.fitsIntoInteger((long) object);
                    canUseDouble = false;
                } else if (object instanceof Double) {
                    canUseInteger = false;
                    canUseLong = false;
                } else {
                    canUseInteger = false;
                    canUseLong = false;
                    canUseDouble = false;
                }
            }

            if (canUseInteger) {
                final int[] store = new int[objects.length];

                for (int n = 0; n < objects.length; n++) {
                    final Object object = objects[n];
                    if (object instanceof Integer) {
                        store[n] = (int) object;
                    } else if (object instanceof Long) {
                        store[n] = (int) (long) object;
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }

                return store;
            } else if (canUseLong) {
                final long[] store = new long[objects.length];

                for (int n = 0; n < objects.length; n++) {
                    final Object object = objects[n];
                    if (object instanceof Integer) {
                        store[n] = (long) (int) object;
                    } else if (object instanceof Long) {
                        store[n] = (long) object;
                    } else {
                        throw new UnsupportedOperationException();
                    }
                }

                return store;
            } else if (canUseDouble) {
                final double[] store = new double[objects.length];

                for (int n = 0; n < objects.length; n++) {
                    store[n] = CoreLibrary.toDouble(objects[n], coreLibrary().getNilObject());
                }

                return store;
            } else {
                return objects;
            }
        }

    }
}
