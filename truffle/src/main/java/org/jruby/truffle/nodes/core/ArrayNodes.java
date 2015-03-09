/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;

import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.array.*;
import org.jruby.truffle.nodes.dispatch.*;
import org.jruby.truffle.nodes.methods.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.methods.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.methods.locals.ReadLevelVariableNodeFactory;
import org.jruby.truffle.nodes.yield.YieldDispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.ByteList;
import org.jruby.util.Memo;

import java.util.Arrays;
import java.util.Comparator;

@CoreClass(name = "Array")
public abstract class ArrayNodes {

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends ArrayCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isNull(a)", "isNull(b)"})
        public RubyArray addNull(RubyArray a, RubyArray b) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);
        }

        @Specialization(guards = {"isObject(a)", "isNull(b)"})
        public RubyArray addObjectNull(RubyArray a, RubyArray b) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((Object[]) a.getStore(), a.getSize()), a.getSize());
        }

        @Specialization(guards = "areBothIntegerFixnum(a, b)")
        public RubyArray addBothIntegerFixnum(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final int[] combined = new int[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothLongFixnum(a, b)")
        public RubyArray addBothLongFixnum(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final long[] combined = new long[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothFloat(a, b)")
        public RubyArray addBothFloat(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final double[] combined = new double[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothObject(a, b)")
        public RubyArray addBothObject(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = {"isNull(a)", "isOtherIntegerFixnum(a, b)"})
        public RubyArray addNullIntegerFixnum(RubyArray a, RubyArray b) {
            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((int[]) b.getStore(), size), size);
        }

        @Specialization(guards = {"isNull(a)", "isOtherLongFixnum(a, b)"})
        public RubyArray addNullLongFixnum(RubyArray a, RubyArray b) {
            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((long[]) b.getStore(), size), size);
        }

        @Specialization(guards = {"isNull(a)", "isOtherObject(a, b)"})
        public RubyArray addNullObject(RubyArray a, RubyArray b) {
            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((Object[]) b.getStore(), size), size);
        }

        @Specialization(guards = "isIntegerFixnum(a)")
        public RubyArray addEmptyIntegerFixnum(RubyArray a, RubyArray b) {
            // TODO CS 5-Feb-15 hack to get things working with empty int[] store

            if (a.getSize() != 0) {
                throw new UnsupportedOperationException();
            }

            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.box(b.getStore()), size);
        }

        @Specialization(guards = "isLongFixnum(a)")
        public RubyArray addEmptyLongFixnum(RubyArray a, RubyArray b) {
            // TODO CS 5-Feb-15 hack to get things working with empty long[] store

            if (a.getSize() != 0) {
                throw new UnsupportedOperationException();
            }

            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.box(b.getStore()), size);
        }

        @Specialization(guards = "isFloat(a)")
        public RubyArray addEmptyDouble(RubyArray a, RubyArray b) {
            // TODO CS 5-Feb-15 hack to get things working with empty double[] store

            if (a.getSize() != 0) {
                throw new UnsupportedOperationException();
            }

            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.box(b.getStore()), size);
        }

        @Specialization(guards = "isObject(a)")
        public RubyArray addEmptyObject(RubyArray a, RubyArray b) {
            // TODO CS 5-Feb-15 hack to get things working with empty Object[] store

            if (a.getSize() != 0) {
                throw new UnsupportedOperationException();
            }

            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), ArrayUtils.box(b.getStore()), size);
        }

    }

    @CoreMethod(names = "*", required = 1, lowerFixnumParameters = 0)
    public abstract static class MulNode extends ArrayCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray mulEmpty(RubyArray array, int count) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public RubyArray mulIntegerFixnum(RubyArray array, int count) {
            final int[] store = (int[]) array.getStore();
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final int[] newStore = new int[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array.getAllocationSite(), newStore, newStoreLength);
        }

        @Specialization(guards = "isLongFixnum(array)")
        public RubyArray mulLongFixnum(RubyArray array, int count) {
            final long[] store = (long[]) array.getStore();
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final long[] newStore = new long[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array.getAllocationSite(), newStore, newStoreLength);
        }

        @Specialization(guards = "isFloat(array)")
        public RubyArray mulFloat(RubyArray array, int count) {
            final double[] store = (double[]) array.getStore();
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final double[] newStore = new double[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array.getAllocationSite(), newStore, newStoreLength);
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray mulObject(RubyArray array, int count) {
            final Object[] store = (Object[]) array.getStore();
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final Object[] newStore = new Object[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array.getAllocationSite(), newStore, newStoreLength);
        }

    }

    @CoreMethod(names = { "[]", "slice" }, required = 1, optional = 1, lowerFixnumParameters = { 0, 1 })
    public abstract static class IndexNode extends ArrayCoreMethodNode {

        @Child protected ArrayReadDenormalizedNode readNode;
        @Child protected ArrayReadSliceDenormalizedNode readSliceNode;
        @Child protected ArrayReadSliceNormalizedNode readNormalizedSliceNode;
        @Child protected CallDispatchHeadNode fallbackNode;

        public IndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IndexNode(IndexNode prev) {
            super(prev);
            readNode = prev.readNode;
            readSliceNode = prev.readSliceNode;
            readNormalizedSliceNode = prev.readNormalizedSliceNode;
            fallbackNode = prev.fallbackNode;
        }

        @Specialization
        public Object index(VirtualFrame frame, RubyArray array, int index, UndefinedPlaceholder undefined) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeFactory.create(getContext(), getSourceSection(), null, null));
            }

            return readNode.executeRead(frame, array, index);
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyArray array, int start, int length) {
            if (length < 0) {
                return getContext().getCoreLibrary().getNilObject();
            }

            if (readSliceNode == null) {
                CompilerDirectives.transferToInterpreter();
                readSliceNode = insert(ArrayReadSliceDenormalizedNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }

            return readSliceNode.executeReadSlice(frame, array, start, length);
        }

        @Specialization
        public Object slice(VirtualFrame frame, RubyArray array, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder undefined) {
            final int normalizedIndex = array.normalizeIndex(range.getBegin());

            if (normalizedIndex < 0 || normalizedIndex > array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final int end = array.normalizeIndex(range.getEnd());
                final int exclusiveEnd = array.clampExclusiveIndex(range.doesExcludeEnd() ? end : end + 1);

                if (exclusiveEnd <= normalizedIndex) {
                    return new RubyArray(array.getLogicalClass(), null, 0);
                }

                final int length = exclusiveEnd - normalizedIndex;

                if (readNormalizedSliceNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    readNormalizedSliceNode = insert(ArrayReadSliceNormalizedNodeFactory.create(getContext(), getSourceSection(), null, null, null));
                }

                return readNormalizedSliceNode.executeReadSlice(frame, array, normalizedIndex, length);
            }
        }

        @Specialization(guards = {"!isInteger(a)", "!isIntegerFixnumRange(a)"})
        public Object fallbackIndex(VirtualFrame frame, RubyArray array, Object a, UndefinedPlaceholder undefined) {
            return fallback(frame, array, RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), a));
        }

        @Specialization(guards = {"!isIntegerFixnumRange(a)", "!isUndefinedPlaceholder(b)"})
        public Object fallbackSlice(VirtualFrame frame, RubyArray array, Object a, Object b) {
            return fallback(frame, array, RubyArray.fromObjects(getContext().getCoreLibrary().getArrayClass(), a, b));
        }

        public Object fallback(VirtualFrame frame, RubyArray array, RubyArray args) {
            if (fallbackNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return fallbackNode.call(frame, array, "element_reference_fallback", null,
                    getContext().makeString(getName()), args);
        }

    }

    @CoreMethod(names = "[]=", required = 2, optional = 1, lowerFixnumParameters = 0, raiseIfFrozenSelf = true)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        @Child private ArrayWriteDenormalizedNode writeNode;

        private final BranchProfile tooSmallBranch = BranchProfile.create();

        public IndexSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IndexSetNode(IndexSetNode prev) {
            super(prev);
            writeNode = prev.writeNode;
        }

        @Specialization
        public Object set(VirtualFrame frame, RubyArray array, int index, Object value, UndefinedPlaceholder unused) {
            if (writeNode == null) {
                CompilerDirectives.transferToInterpreter();
                writeNode = insert(ArrayWriteDenormalizedNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }

            return writeNode.executeWrite(frame, array, index, value);
        }

        // Set a slice of the array to a particular value

        @Specialization(guards = { "isObject(array)", "!isRubyArray(value)", "!isUndefinedPlaceholder(value)" })
        public Object setObject(VirtualFrame frame, RubyArray array, int start, int length, Object value) {
            notDesignedForCompilation();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexNegativeLength(length, this));
            }

            final int begin = array.normalizeIndex(start);

            if (begin >= array.getSize()) {
                if (writeNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    writeNode = insert(ArrayWriteDenormalizedNodeFactory.create(getContext(), getSourceSection(), null, null, null));
                }

                return writeNode.executeWrite(frame, array, start, value);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        // Set a slice of the array to another array

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object setIntegerFixnum(VirtualFrame frame, RubyArray array, int start, int length, RubyArray value) {
            notDesignedForCompilation();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexNegativeLength(length, this));
            }

            if (value.getSize() == 0) {
                final int begin = array.normalizeIndex(start);
                final int exclusiveEnd = begin + length;
                int[] store = (int[]) array.getStore();

                if (begin < 0) {
                    tooSmallBranch.enter();
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", start, array.getSize(), this));
                } else if (exclusiveEnd > array.getSize()) {
                    throw new UnsupportedOperationException();
                }

                // TODO: This is a moving overlapping memory, should we use sth else instead?
                System.arraycopy(store, exclusiveEnd, store, begin, array.getSize() - exclusiveEnd);
                array.setStore(store, array.getSize() - length);

                return value;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object setIntegerFixnumRange(VirtualFrame frame, RubyArray array, RubyRange.IntegerFixnumRange range, RubyArray other, UndefinedPlaceholder unused) {
            if (range.doesExcludeEnd()) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else {
                int normalizedBegin = array.normalizeIndex(range.getBegin());
                int normalizedEnd = array.normalizeIndex(range.getEnd());

                if (normalizedBegin == 0 && normalizedEnd == array.getSize() - 1) {
                    array.setStore(Arrays.copyOf((int[]) other.getStore(), other.getSize()), other.getSize());
                } else {
                    throw new RuntimeException();
                }
            }

            return other;
        }

    }

    @CoreMethod(names = "at", required = 1, lowerFixnumParameters = 0)
    public abstract static class AtNode extends ArrayCoreMethodNode {

        @Child private ArrayReadDenormalizedNode readNode;

        public AtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AtNode(AtNode prev) {
            super(prev);
            readNode = prev.readNode;
        }

        @Specialization
        public Object at(VirtualFrame frame, RubyArray array, int index) {
            if (readNode == null) {
                CompilerDirectives.transferToInterpreter();
                readNode = insert(ArrayReadDenormalizedNodeFactory.create(getContext(), getSourceSection(), null, null));
            }

            return readNode.executeRead(frame, array, index);
        }

    }

    @CoreMethod(names = "clear")
    public abstract static class ClearNode extends ArrayCoreMethodNode {

        public ClearNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ClearNode(ClearNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray clear(RubyArray array) {
            array.setStore(array.getStore(), 0);
            return array;
        }

    }

    @CoreMethod(names = "compact")
    @ImportStatic(ArrayGuards.class)
    public abstract static class CompactNode extends ArrayCoreMethodNode {

        public CompactNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompactNode(CompactNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntArray(array)")
        public RubyArray compactInt(RubyArray array) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(),
                    Arrays.copyOf((int[]) array.getStore(), array.getSize()), array.getSize());
        }

        @Specialization(guards = "isLongArray(array)")
        public RubyArray compactLong(RubyArray array) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(),
                    Arrays.copyOf((long[]) array.getStore(), array.getSize()), array.getSize());
        }

        @Specialization(guards = "isDoubleArray(array)")
        public RubyArray compactDouble(RubyArray array) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(),
                    Arrays.copyOf((double[]) array.getStore(), array.getSize()), array.getSize());
        }

        @Specialization(guards = "isObjectArray(array)")
        public Object compactObjects(RubyArray array) {
            // TODO CS 9-Feb-15 by removing nil we could make this array suitable for a primitive array storage class

            final Object[] store = (Object[]) array.getStore();
            final Object[] newStore = new Object[store.length];
            final int size = array.getSize();

            int m = 0;

            for (int n = 0; n < size; n++) {
                if (store[n] != getContext().getCoreLibrary().getNilObject()) {
                    newStore[m] = store[n];
                    m++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, m);
        }

    }

    @CoreMethod(names = "compact!", raiseIfFrozenSelf = true)
    public abstract static class CompactBangNode extends ArrayCoreMethodNode {

        public CompactBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompactBangNode(CompactBangNode prev) {
            super(prev);
        }

        @Specialization(guards = "!isObject(array)")
        public RubyNilClass compactNotObjects(RubyArray array) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isObject(array)")
        public Object compactObjects(RubyArray array) {
            final Object[] store = (Object[]) array.getStore();
            final int size = array.getSize();

            int m = 0;

            for (int n = 0; n < size; n++) {
                if (store[n] != getContext().getCoreLibrary().getNilObject()) {
                    store[m] = store[n];
                    m++;
                }
            }

            array.setStore(store, m);

            if (m == size) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return array;
            }
        }

    }

    @CoreMethod(names = "concat", required = 1)
    public abstract static class ConcatNode extends ArrayCoreMethodNode {

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConcatNode(ConcatNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothNull(array, other)")
        public RubyArray concatNull(RubyArray array, RubyArray other) {
            return array;
        }

        @Specialization(guards = "areBothIntegerFixnum(array, other)")
        public RubyArray concatIntegerFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setStore(Arrays.copyOf((int[]) array.getStore(), array.getSize() + other.getSize()), array.getSize() + other.getSize());
            return array;
        }

        @Specialization(guards = "areBothLongFixnum(array, other)")
        public RubyArray concatLongFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setStore(Arrays.copyOf((long[]) array.getStore(), array.getSize() + other.getSize()), array.getSize() + other.getSize());
            return array;
        }

        @Specialization(guards = "areBothFloat(array, other)")
        public RubyArray concatDouble(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setStore(Arrays.copyOf((double[]) array.getStore(), array.getSize() + other.getSize()), array.getSize() + other.getSize());
            return array;
        }

        @Specialization(guards = "areBothObject(array, other)")
        public RubyArray concatObject(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            int size = array.getSize();
            int newSize = size + other.getSize();
            Object[] store = (Object[]) array.getStore();

            if (newSize > store.length) {
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            System.arraycopy(other.getStore(), 0, store, size, other.getSize());
            array.setStore(store, newSize);
            return array;
        }

        @Specialization
        public RubyArray concat(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            // TODO(CS): if array is Object[], use Arrays.copyOf
            final Object[] newStore = new Object[array.getSize() + other.getSize()];
            ArrayUtils.copy(array.getStore(), newStore, 0, array.getSize());
            ArrayUtils.copy(other.getStore(), newStore, array.getSize(), other.getSize());
            array.setStore(newStore, array.getSize() + other.getSize());
            return array;
        }

    }

    @CoreMethod(names = "delete", required = 1)
    public abstract static class DeleteNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null,null});
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object deleteIntegerFixnum(VirtualFrame frame, RubyArray array, Object value) {
            final int[] store = (int[]) array.getStore();

            Object found = getContext().getCoreLibrary().getNilObject();

            int i = 0;

            for (int n = 0; n < array.getSize(); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    found = store[n];
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }

            array.setStore(store, i);
            return found;
        }

        @Specialization(guards = "isObject(array)")
        public Object deleteObject(VirtualFrame frame, RubyArray array, Object value) {
            final Object[] store = (Object[]) array.getStore();

            Object found = getContext().getCoreLibrary().getNilObject();

            int i = 0;

            for (int n = 0; n < array.getSize(); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    found = store[n];
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }

            array.setStore(store, i);
            return found;
        }

    }

    @CoreMethod(names = "delete_at", required = 1)
    public abstract static class DeleteAtNode extends ArrayCoreMethodNode {

        private final BranchProfile tooSmallBranch = BranchProfile.create();
        private final BranchProfile beyondEndBranch = BranchProfile.create();

        public DeleteAtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteAtNode(DeleteAtNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum(array)", rewriteOn = UnexpectedResultException.class)
        public int deleteAtIntegerFixnumInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalizedIndex = array.normalizeIndex(index);

            if (normalizedIndex < 0) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else if (normalizedIndex >= array.getSize()) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final int[] store = (int[]) array.getStore();
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, array.getSize() - normalizedIndex - 1);
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtIntegerFixnumInBounds", guards = "isIntegerFixnum(array)")
        public Object deleteAtIntegerFixnum(RubyArray array, int index) {
            notDesignedForCompilation();

            int normalizedIndex = index;

            if (normalizedIndex < 0) {
                normalizedIndex = array.getSize() + index;
            }

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else if (normalizedIndex >= array.getSize()) {
                beyondEndBranch.enter();
                throw new UnsupportedOperationException();
            } else {
                final int[] store = (int[]) array.getStore();
                final int value = store[normalizedIndex];
                System.arraycopy(store, normalizedIndex + 1, store, normalizedIndex, array.getSize() - normalizedIndex - 1);
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

    }

    @CoreMethod(names = "each", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode toEnumNode;

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
            toEnumNode = prev.toEnumNode;
        }

        @Specialization
        public Object eachEnumerator(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            if (toEnumNode == null) {
                CompilerDirectives.transferToInterpreter();
                toEnumNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return toEnumNode.call(frame, array, "to_enum", null, getContext().getCoreLibrary().getEachSymbol());
        }

        @Specialization(guards = "isNull(array)")
        public Object eachNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object eachIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) array.getStore();

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.getSize(); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongFixnum(array)")
        public Object eachLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final long[] store = (long[]) array.getStore();

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.getSize(); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isFloat(array)")
        public Object eachFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            final double[] store = (double[]) array.getStore();

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.getSize(); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObject(array)")
        public Object eachObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.getSize(); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n]);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "each_with_index", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class EachWithIndexNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public EachWithIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachWithIndexNode(EachWithIndexNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray eachWithEmpty(VirtualFrame frame, RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isObject(array)")
        public Object eachWithIndexObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.getSize(); n++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, store[n], n);
                            continue outer;
                        } catch (BreakException e) {
                            breakProfile.enter();
                            return e.getResult();
                        } catch (NextException e) {
                            nextProfile.enter();
                            continue outer;
                        } catch (RedoException e) {
                            redoProfile.enter();
                        }
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludeNode extends ArrayCoreMethodNode {

        @Child private KernelNodes.SameOrEqualNode equalNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null,null});
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization(guards = "isNull(array)")
        public boolean includeNull(VirtualFrame frame, RubyArray array, Object value) {
            return false;
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public boolean includeIntegerFixnum(VirtualFrame frame, RubyArray array, Object value) {
            final int[] store = (int[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                final Object stored = store[n];

                notDesignedForCompilation();

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isLongFixnum(array)")
        public boolean includeLongFixnum(VirtualFrame frame, RubyArray array, Object value) {
            final long[] store = (long[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                final Object stored = store[n];

                notDesignedForCompilation();

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isFloat(array)")
        public boolean includeFloat(VirtualFrame frame, RubyArray array, Object value) {
            final double[] store = (double[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                final Object stored = store[n];

                notDesignedForCompilation();

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isObject(array)")
        public boolean includeObject(VirtualFrame frame, RubyArray array, Object value) {
            final Object[] store = (Object[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                final Object stored = store[n];

                if (equalNode.executeSameOrEqual(frame, stored, value)) {
                    return true;
                }
            }

            return false;
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, optional = 2)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InitializeNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, UndefinedPlaceholder size, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            return initialize(array, 0, getContext().getCoreLibrary().getNilObject(), block);
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            return initialize(array, size, getContext().getCoreLibrary().getNilObject(), block);
        }

        @Specialization
        public RubyArray initialize(RubyArray array, long size, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            if (size > Integer.MAX_VALUE) {
                throw new IllegalStateException();
            }
            return initialize(array, (int) size, getContext().getCoreLibrary().getNilObject(), block);
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, int defaultValue, UndefinedPlaceholder block) {
            final int[] store = new int[size];
            Arrays.fill(store, defaultValue);
            array.setStore(store, size);
            return array;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, long defaultValue, UndefinedPlaceholder block) {
            final long[] store = new long[size];
            Arrays.fill(store, defaultValue);
            array.setStore(store, size);
            return array;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, double defaultValue, UndefinedPlaceholder block) {
            final double[] store = new double[size];
            Arrays.fill(store, defaultValue);
            array.setStore(store, size);
            return array;
        }

        @Specialization(guards = "!isUndefinedPlaceholder(defaultValue)")
        public RubyArray initialize(RubyArray array, int size, Object defaultValue, UndefinedPlaceholder block) {
            final Object[] store = new Object[size];
            Arrays.fill(store, defaultValue);
            array.setStore(store, size);
            return array;
        }

        @Specialization
        public RubyArray initialize(VirtualFrame frame, RubyArray array, int size, UndefinedPlaceholder defaultValue, RubyProc block) {
            Object store = arrayBuilder.start(size);

            int count = 0;
            try {
                for (int n = 0; n < size; n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    store = arrayBuilder.append(store, n, yield(frame, block, n));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            array.setStore(arrayBuilder.finish(store, size), size);
            return array;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, RubyArray copy, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            notDesignedForCompilation();
            array.setStore(copy.slowToArray(), copy.getSize());
            return array;
        }

    }

    @CoreMethod(names = "initialize_copy", visibility = Visibility.PRIVATE, required = 1)
    public abstract static class InitializeCopyNode extends ArrayCoreMethodNode {
        // TODO(cs): what about allocationSite ?

        public InitializeCopyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeCopyNode(InitializeCopyNode prev) {
            super(prev);
        }

        @Specialization(guards = "isOtherNull(self, from)")
        public RubyArray initializeCopyNull(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(null, 0);
            return self;
        }

        @Specialization(guards = "isOtherIntegerFixnum(self, from)")
        public RubyArray initializeCopyIntegerFixnum(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((int[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

        @Specialization(guards = "isOtherLongFixnum(self, from)")
        public RubyArray initializeCopyLongFixnum(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((long[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

        @Specialization(guards = "isOtherFloat(self, from)")
        public RubyArray initializeCopyFloat(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((double[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

        @Specialization(guards = "isOtherObject(self, from)")
        public RubyArray initializeCopyObject(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((Object[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

    }

    @CoreMethod(names = {"inject", "reduce"}, needsBlock = true, optional = 1)
    @ImportStatic(ArrayGuards.class)
    public abstract static class InjectNode extends YieldingCoreMethodNode {

        @Child private CallDispatchHeadNode dispatch;

        public InjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.CALL_METHOD_MISSING);
        }

        public InjectNode(InjectNode prev) {
            super(prev);
            dispatch = prev.dispatch;
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object injectIntegerFixnum(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final int[] store = (int[]) array.getStore();

            Object accumulator = initial;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isLongFixnum(array)")
        public Object injectLongFixnum(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final long[] store = (long[]) array.getStore();

            Object accumulator = initial;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isFloat(array)")
        public Object injectFloat(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final double[] store = (double[]) array.getStore();

            Object accumulator = initial;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization(guards = "isObject(array)")
        public Object injectObject(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            int count = 0;

            final Object[] store = (Object[]) array.getStore();

            Object accumulator = initial;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    accumulator = yield(frame, block, accumulator, store[n]);
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization
        public Object inject(VirtualFrame frame, RubyArray array, RubySymbol symbol, UndefinedPlaceholder unused) {
            notDesignedForCompilation();

            final Object[] store = array.slowToArray();

            if (store.length < 2) {
                if (store.length == 1) {
                    return store[0];
                } else {
                    return getContext().getCoreLibrary().getNilObject();
                }
            }

            Object accumulator = dispatch.call(frame, store[0], symbol, null, store[1]);

            for (int n = 2; n < array.getSize(); n++) {
                accumulator = dispatch.call(frame, accumulator, symbol, null, store[n]);
            }

            return accumulator;
        }

    }

    @CoreMethod(names = "insert", required = 2)
    public abstract static class InsertNode extends ArrayCoreMethodNode {

        private final BranchProfile tooSmallBranch = BranchProfile.create();

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InsertNode(InsertNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(array)")
        public Object insert(RubyArray array, int index, Object value) {
            notDesignedForCompilation();

            final Object[] store = new Object[index + 1];
            Arrays.fill(store, getContext().getCoreLibrary().getNilObject());
            store[index] = value;
            array.setStore(store, array.getSize() + 1);
            return array;
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object insert(RubyArray array, int index, int value) {
            final int normalizedIndex = array.normalizeIndex(index);
            final int[] store = (int[]) array.getStore();

            if (normalizedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (array.getSize() > store.length + 1) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else {
                System.arraycopy(store, normalizedIndex, store, normalizedIndex + 1, array.getSize() - normalizedIndex);
                store[normalizedIndex] = value;
                array.setStore(store, array.getSize() + 1);
            }

            return array;
        }

    }

    @CoreMethod(names = {"inspect", "to_s"})
    public abstract static class InspectNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode inspect;

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            inspect = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
            inspect = prev.inspect;
        }

        @Specialization
        public RubyString inspect(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final StringBuilder builder = new StringBuilder();
            final Object[] objects = array.slowToArray();

            builder.append("[");

            for (int n = 0; n < objects.length; n++) {
                if (n > 0) {
                    builder.append(", ");
                }

                // TODO(CS): cast

                final RubyString string = (RubyString) inspect.call(frame, objects[n], "inspect", null);
                builder.append(string.getBytes().toString());
            }

            builder.append("]");

            return getContext().makeString(builder.toString());
        }

    }

    @CoreMethod(names = "join", optional = 1)
    public abstract static class JoinNode extends ArrayCoreMethodNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public JoinNode(JoinNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString join(RubyArray array, UndefinedPlaceholder unused) {
            Object separator = getContext().getCoreLibrary().getGlobalVariablesObject().getInstanceVariable("$,");
            if (separator == getContext().getCoreLibrary().getNilObject()) {
                separator = getContext().makeString("");
            }

            if (separator instanceof RubyString) {
                return join(array, (RubyString) separator);
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Specialization
        public RubyString join(RubyArray array, RubyString separator) {
            notDesignedForCompilation();

            final StringBuilder builder = new StringBuilder();

            final Object[] objects = array.slowToArray();

            for (int n = 0; n < objects.length; n++) {
                if (n > 0) {
                    builder.append(separator);
                }

                builder.append(objects[n]);
            }

            return getContext().makeString(builder.toString());
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public MapNode(MapNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray mapNull(RubyArray array, RubyProc block) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public RubyArray mapIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) array.getStore();
            final int arraySize = array.getSize();
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isLongFixnum(array)")
        public RubyArray mapLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final long[] store = (long[]) array.getStore();
            final int arraySize = array.getSize();
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isFloat(array)")
        public RubyArray mapFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            final double[] store = (double[]) array.getStore();
            final int arraySize = array.getSize();
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray mapObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();
            final int arraySize = array.getSize();
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }
    }

    @CoreMethod(names = {"map!", "collect!"}, needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public MapInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public MapInPlaceNode(MapInPlaceNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray mapInPlaceNull(RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public RubyArray mapInPlaceFixnumInteger(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) array.getStore();
            final int arraySize = array.getSize();
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            array.setStore(arrayBuilder.finish(mappedStore, arraySize), arraySize);

            return array;
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray mapInPlaceObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();
            final int arraySize = array.getSize();
            Object mappedStore = arrayBuilder.start(arraySize);

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    mappedStore = arrayBuilder.append(mappedStore, n, yield(frame, block, store[n]));
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            array.setStore(arrayBuilder.finish(mappedStore, arraySize), arraySize);

            return array;
        }
    }

    // TODO: move into Enumerable?

    @CoreMethod(names = "max")
    public abstract static class MaxNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode eachNode;
        private final MaxBlock maxBlock;

        public MaxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = DispatchHeadNodeFactory.createMethodCall(context);
            maxBlock = context.getCoreLibrary().getArrayMaxBlock();
        }

        public MaxNode(MaxNode prev) {
            super(prev);
            eachNode = prev.eachNode;
            maxBlock = prev.maxBlock;
        }

        @Specialization
        public Object max(VirtualFrame frame, RubyArray array) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> maximum = new Memo<>();

            final VirtualFrame maximumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, array, null, new Object[] {}), maxBlock.getFrameDescriptor());
            maximumClosureFrame.setObject(maxBlock.getFrameSlot(), maximum);

            final RubyProc block = new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    maxBlock.getSharedMethodInfo(), maxBlock.getCallTarget(), maxBlock.getCallTarget(),
                    maxBlock.getCallTarget(), maximumClosureFrame.materialize(), null, array, null);

            eachNode.call(frame, array, "each", block);

            if (maximum.get() == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return maximum.get();
            }
        }

    }

    public abstract static class MaxBlockNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode compareNode;

        public MaxBlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public MaxBlockNode(MaxBlockNode prev) {
            super(prev);
            compareNode = prev.compareNode;
        }

        @Specialization
        public RubyNilClass max(VirtualFrame frame, Object maximumObject, Object value) {
            final Memo<Object> maximum = (Memo<Object>) maximumObject;

            // TODO(CS): cast

            final Object current = maximum.get();

            if (current == null || (int) compareNode.call(frame, value, "<=>", null, current) < 0) {
                maximum.set(value);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    public static class MaxBlock {

        private final FrameDescriptor frameDescriptor;
        private final FrameSlot frameSlot;
        private final SharedMethodInfo sharedMethodInfo;
        private final CallTarget callTarget;

        public MaxBlock(RubyContext context) {
            final SourceSection sourceSection = new CoreSourceSection("Array", "max");

            frameDescriptor = new FrameDescriptor();
            frameSlot = frameDescriptor.addFrameSlot("maximum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, "max", false, null, false);

            callTarget = Truffle.getRuntime().createCallTarget(new RubyRootNode(
                    context, sourceSection, null, sharedMethodInfo,
                    ArrayNodesFactory.MaxBlockNodeFactory.create(context, sourceSection, new RubyNode[]{
                            ReadLevelVariableNodeFactory.create(context, sourceSection, frameSlot, 1),
                            new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR)
                    })));
        }

        public FrameDescriptor getFrameDescriptor() {
            return frameDescriptor;
        }

        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        public SharedMethodInfo getSharedMethodInfo() {
            return sharedMethodInfo;
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }
    }

    @CoreMethod(names = "min")
    public abstract static class MinNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode eachNode;
        private final MinBlock minBlock;

        public MinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = DispatchHeadNodeFactory.createMethodCall(context);
            minBlock = context.getCoreLibrary().getArrayMinBlock();
        }

        public MinNode(MinNode prev) {
            super(prev);
            eachNode = prev.eachNode;
            minBlock = prev.minBlock;
        }

        @Specialization
        public Object min(VirtualFrame frame, RubyArray array) {
            // TODO: can we just write to the frame instead of having this indirect object?

            final Memo<Object> minimum = new Memo<>();

            final VirtualFrame minimumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(null, null, array, null, new Object[] {}), minBlock.getFrameDescriptor());
            minimumClosureFrame.setObject(minBlock.getFrameSlot(), minimum);

            final RubyProc block = new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    minBlock.getSharedMethodInfo(), minBlock.getCallTarget(), minBlock.getCallTarget(),
                    minBlock.getCallTarget(), minimumClosureFrame.materialize(), null, array, null);

            eachNode.call(frame, array, "each", block);

            if (minimum.get() == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return minimum.get();
            }
        }

    }

    public abstract static class MinBlockNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode compareNode;

        public MinBlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public MinBlockNode(MinBlockNode prev) {
            super(prev);
            compareNode = prev.compareNode;
        }

        @Specialization
        public RubyNilClass min(VirtualFrame frame, Object minimumObject, Object value) {
            final Memo<Object> minimum = (Memo<Object>) minimumObject;

            // TODO(CS): cast

            final Object current = minimum.get();

            if (current == null || (int) compareNode.call(frame, value, "<=>", null, current) < 0) {
                minimum.set(value);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    public static class MinBlock {

        private final FrameDescriptor frameDescriptor;
        private final FrameSlot frameSlot;
        private final SharedMethodInfo sharedMethodInfo;
        private final CallTarget callTarget;

        public MinBlock(RubyContext context) {
            final SourceSection sourceSection = new CoreSourceSection("Array", "min");

            frameDescriptor = new FrameDescriptor();
            frameSlot = frameDescriptor.addFrameSlot("minimum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, Arity.NO_ARGUMENTS, "min", false, null, false);

            callTarget = Truffle.getRuntime().createCallTarget(new RubyRootNode(
                    context, sourceSection, null, sharedMethodInfo,
                    ArrayNodesFactory.MinBlockNodeFactory.create(context, sourceSection, new RubyNode[]{
                            ReadLevelVariableNodeFactory.create(context, sourceSection, frameSlot, 1),
                            new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR)
                    })));
        }

        public FrameDescriptor getFrameDescriptor() {
            return frameDescriptor;
        }

        public FrameSlot getFrameSlot() {
            return frameSlot;
        }

        public SharedMethodInfo getSharedMethodInfo() {
            return sharedMethodInfo;
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }
    }

    @CoreMethod(names = "pack", required = 1)
    public abstract static class PackNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode toStringNode;

        public PackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PackNode(PackNode prev) {
            super(prev);
            toStringNode = prev.toStringNode;
        }
        
        // TODO CS 3-Mar-15 to be honest these two specialisations are a bit sneaky - we'll get rid of them ASAP

        @Specialization(guards = {"arrayIsInts(array)", "formatIsXN2000(array, format)"})
        public RubyString packXN2000(RubyArray array, RubyString format) {
            final int size = array.getSize();
            final int[] store = (int[]) array.getStore();
            final byte[] bytes = new byte[1 + size * 4];
            
            // bytes[0] = 0 is implicit

            for (int n = 0; n < size; n++) {
                final int value = store[n];
                final int byteOffset = 1 + n * 4;
                bytes[byteOffset + 3] = (byte) (value >>> 24);
                bytes[byteOffset + 2] = (byte) (value >>> 16);
                bytes[byteOffset + 1] = (byte) (value >>> 8);
                bytes[byteOffset + 0] = (byte) value;
            }

            // TODO CS 3-Mar-15 should be tainting here - but ideally have a pack node, and then taint on top of that

            return new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(bytes));
        }

        @Specialization(guards = {"arrayIsLongs(array)", "formatIsLStar(array, format)"})
        public RubyString packLStar(RubyArray array, RubyString format) {
            final int size = array.getSize();
            final long[] store = (long[]) array.getStore();
            final byte[] bytes = new byte[size * 4];

            for (int n = 0; n < size; n++) {
                final int value = (int) store[n]; // happy to truncate
                final int byteOffset = n * 4;
                // TODO CS 3-Mar-15 this should be native endian
                bytes[byteOffset + 3] = (byte) (value >>> 24);
                bytes[byteOffset + 2] = (byte) (value >>> 16);
                bytes[byteOffset + 1] = (byte) (value >>> 8);
                bytes[byteOffset + 0] = (byte) value;
            }

            // TODO CS 1-Mar-15 should be tainting here - but ideally have a pack node, and then taint on top of that

            return new RubyString(getContext().getCoreLibrary().getStringClass(), new ByteList(bytes));
        }

        @Specialization
        public RubyString pack(VirtualFrame frame, RubyArray array, RubyString format) {
            notDesignedForCompilation();

            final Object[] objects = array.slowToArray();
            final IRubyObject[] jrubyObjects = new IRubyObject[objects.length];

            for (int n = 0; n < objects.length; n++) {
                if (objects[n] instanceof RubyNilClass || objects[n] instanceof Integer || objects[n] instanceof Long
                        || objects[n] instanceof RubyBignum || objects[n] instanceof Double || objects[n] instanceof RubyString) {
                    jrubyObjects[n] = getContext().toJRuby(objects[n]);
                } else {
                    if (toStringNode == null) {
                        CompilerDirectives.transferToInterpreter();
                        toStringNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), MissingBehavior.RETURN_MISSING));
                    }

                    final Object result = toStringNode.call(frame, objects[n], "to_str", null);

                    if (result == DispatchNode.MISSING) {
                        throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(objects[n], "String", this));
                    } else if (result instanceof RubyString) {
                        jrubyObjects[n] = getContext().toJRuby((RubyString) result);
                    } else {
                        throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(objects[n], "String", this));
                    }
                }
            }

            try {
                return getContext().toTruffle(
                        org.jruby.util.Pack.pack(
                                getContext().getRuntime().getCurrentContext(),
                                getContext().getRuntime(),
                                getContext().getRuntime().newArray(jrubyObjects),
                                getContext().toJRuby(format)));
            } catch (org.jruby.exceptions.RaiseException e) {
                throw new RaiseException(getContext().toTruffle(e.getException(), this));
            }
        }

        @Specialization(guards = "!isRubyString(format)")
        public RubyString pack(VirtualFrame frame, RubyArray array, Object format) {
            // TODO CS 1-Mar-15 sloppy until I can get @CreateCast to work

            if (toStringNode == null) {
                CompilerDirectives.transferToInterpreter();
                toStringNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), MissingBehavior.RETURN_MISSING));
            }

            final Object result = toStringNode.call(frame, format, "to_str", null);

            if (result == DispatchNode.MISSING) {
                throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(format, "String", this));
            }

            if (result instanceof RubyString) {
                return pack(frame, array, (RubyString) result);
            }

            throw new UnsupportedOperationException();
        }

        protected boolean arrayIsInts(RubyArray array) {
            return array.getStore() instanceof int[];
        }

        protected boolean arrayIsLongs(RubyArray array) {
            return array.getStore() instanceof long[];
        }

        protected boolean formatIsLStar(RubyArray array, RubyString format) {
            final ByteList byteList = format.getByteList();
            
            if (!byteList.getEncoding().isAsciiCompatible()) {
                return false;
            }
            
            if (byteList.length() != 2) {
                return false;
            }
            
            final byte[] bytes = byteList.unsafeBytes();
            return bytes[0] == 'L' && bytes[1] == '*';
        }

        protected boolean formatIsXN2000(RubyArray array, RubyString format) {
            final ByteList byteList = format.getByteList();

            if (!byteList.getEncoding().isAsciiCompatible()) {
                return false;
            }

            if (byteList.length() != 6) {
                return false;
            }

            final byte[] bytes = byteList.unsafeBytes();
            return bytes[0] == 'x' && bytes[1] == 'N' && bytes[2] == '2' && bytes[3] == '0' && bytes[4] == '0' && bytes[5] == '0';
        }

    }

    @CoreMethod(names = "pop")
    public abstract static class PopNode extends ArrayCoreMethodNode {

        public PopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PopNode(PopNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(array)")
        public Object popNil(RubyArray array) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum(array)", rewriteOn = UnexpectedResultException.class)
        public int popIntegerFixnumInBounds(RubyArray array) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final int[] store = ((int[]) array.getStore());
                final int value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBounds", guards = "isIntegerFixnum(array)")
        public Object popIntegerFixnum(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final int[] store = ((int[]) array.getStore());
                final int value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongFixnum(array)", rewriteOn = UnexpectedResultException.class)
        public long popLongFixnumInBounds(RubyArray array) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final long[] store = ((long[]) array.getStore());
                final long value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "popLongFixnumInBounds", guards = "isLongFixnum(array)")
        public Object popLongFixnum(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final long[] store = ((long[]) array.getStore());
                final long value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(guards = "isFloat(array)", rewriteOn = UnexpectedResultException.class)
        public double popFloatInBounds(RubyArray array) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final double[] store = ((double[]) array.getStore());
                final double value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "popFloatInBounds", guards = "isFloat(array)")
        public Object popFloat(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final double[] store = ((double[]) array.getStore());
                final double value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

        @Specialization(guards = "isObject(array)")
        public Object popObject(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final Object[] store = ((Object[]) array.getStore());
                final Object value = store[array.getSize() - 1];
                array.setStore(store, array.getSize() - 1);
                return value;
            }
        }

    }

    @CoreMethod(names = "product", required = 1)
    public abstract static class ProductNode extends ArrayCoreMethodNode {

        public ProductNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ProductNode(ProductNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isObject(array)", "isOtherObject(array, other)"})
        public Object product(RubyArray array, RubyArray other) {
            final Object[] a = (Object[]) array.getStore();
            final int aLength = array.getSize();

            final Object[] b = (Object[]) other.getStore();
            final int bLength = other.getSize();

            final Object[] pairs = new Object[aLength * bLength];

            for (int an = 0; an < aLength; an++) {
                for (int bn = 0; bn < bLength; bn++) {
                    pairs[an * bLength + bn] = new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{a[an], b[bn]}, 2);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), pairs, pairs.length);
        }

    }

    @CoreMethod(names = {"push", "<<", "__append__"}, argumentsAsArray = true)
    public abstract static class PushNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PushNode(PushNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isNull(array)", "isSingleIntegerFixnum(array, values)"})
        public RubyArray pushEmptySingleIntegerFixnum(RubyArray array, Object... values) {
            array.setStore(new int[]{(int) values[0]}, 1);
            return array;
        }

        @Specialization(guards = {"isNull(array)", "isSingleLongFixnum(array, values)"})
        public RubyArray pushEmptySingleIntegerLong(RubyArray array, Object... values) {
            array.setStore(new long[]{(long) values[0]}, 1);
            return array;
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray pushEmptyObjects(RubyArray array, Object... values) {
            array.setStore(values, values.length);
            return array;
        }

        @Specialization(guards = {"isIntegerFixnum(array)", "isSingleIntegerFixnum(array, values)"})
        public RubyArray pushIntegerFixnumSingleIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            int[] store = (int[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (int) values[0];
            array.setStore(store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntegerFixnum(array)", "!isSingleIntegerFixnum(array, values)", "!isSingleLongFixnum(array, values)" })
        public RubyArray pushIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + values.length;

            int[] oldStore = (int[]) array.getStore();
            final Object[] store;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                store = ArrayUtils.box(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);
            } else {
                store = ArrayUtils.box(oldStore);
            }

            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            array.setStore(store, newSize);
            return array;
        }

        @Specialization(guards = {"isLongFixnum(array)", "isSingleIntegerFixnum(array, values)"})
        public RubyArray pushLongFixnumSingleIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            long[] store = (long[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (long) (int) values[0];
            array.setStore(store, newSize);
            return array;
        }

        @Specialization(guards = {"isLongFixnum(array)", "isSingleLongFixnum(array, values)"})
        public RubyArray pushLongFixnumSingleLongFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            long[] store = (long[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            store[oldSize] = (long) values[0];
            array.setStore(store, newSize);
            return array;
        }

        @Specialization(guards = "isLongFixnum(array)")
        public RubyArray pushLongFixnum(RubyArray array, Object... values) {
            // TODO CS 5-Feb-15 hack to get things working with empty long[] store

            if (array.getSize() != 0) {
                throw new UnsupportedOperationException();
            }

            array.setStore(values, values.length);
            return array;
        }

        @Specialization(guards = "isFloat(array)")
        public RubyArray pushFloat(RubyArray array, Object... values) {
            // TODO CS 5-Feb-15 hack to get things working with empty double[] store

            if (array.getSize() != 0) {
                throw new UnsupportedOperationException();
            }

            array.setStore(values, values.length);
            return array;
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray pushObject(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + values.length;

            Object[] store = (Object[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
            }

            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            array.setStore(store, newSize);
            return array;
        }

        protected boolean isSingleIntegerFixnum(RubyArray array, Object... values) {
            return values.length == 1 && values[0] instanceof Integer;
        }

        protected boolean isSingleLongFixnum(RubyArray array, Object... values) {
            return values.length == 1 && values[0] instanceof Long;
        }

    }

    // Not really a core method - used internally

    public abstract static class PushOneNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushOneNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PushOneNode(PushOneNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray pushEmpty(RubyArray array, Object value) {
            array.setStore(new Object[]{value}, 1);
            return array;
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public RubyArray pushIntegerFixnumIntegerFixnum(RubyArray array, int value) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            int[] store = (int[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), array.getSize());
            }

            store[oldSize] = value;
            array.setStore(store, newSize);
            return array;
        }

        @Specialization(guards = { "isIntegerFixnum(array)", "!isInteger(value)" })
        public RubyArray pushIntegerFixnumObject(RubyArray array, Object value) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            final int[] oldStore = (int[]) array.getStore();
            final Object[] newStore;

            if (oldStore.length < newSize) {
                extendBranch.enter();
                newStore = ArrayUtils.box(oldStore, ArrayUtils.capacity(oldStore.length, newSize) - oldStore.length);
            } else {
                newStore = ArrayUtils.box(oldStore);
            }

            newStore[oldSize] = value;
            array.setStore(newStore, newSize);
            return array;
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray pushObjectObject(RubyArray array, Object value) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            Object[] store = (Object[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), array.getSize());
            }

            store[oldSize] = value;
            array.setStore(store, newSize);
            return array;
        }

    }

    @CoreMethod(names = "reject", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public RejectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public RejectNode(RejectNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isNull(array)")
        public Object selectNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isObject(array)")
        public Object selectObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();

            Object selectedStore = arrayBuilder.start(array.getSize());
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    notDesignedForCompilation();

                    if (! yieldIsTruthy(frame, block,  new Object[]{value})) {
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object selectFixnumInteger(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) array.getStore();

            Object selectedStore = arrayBuilder.start(array.getSize());
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    notDesignedForCompilation();

                    if (! yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = { "reject!", "delete_if" }, needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class RejectInPlaceNode extends YieldingCoreMethodNode {

        public RejectInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RejectInPlaceNode(RejectInPlaceNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull(array)")
        public Object rejectInPlaceNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isObject(array)")
        public Object rejectInPlaceObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();

            int i = 0;

            for (int n = 0; n < array.getSize(); n++) {
                if (yieldIsTruthy(frame, block, store[n])) {
                    continue;
                }

                if (i != n) {
                    store[i] = store[n];
                }

                i++;
            }

            array.setStore(store, i);
            return array;
        }

    }

    @CoreMethod(names = "replace", required = 1)
    public abstract static class ReplaceNode extends ArrayCoreMethodNode {

        public ReplaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReplaceNode(ReplaceNode prev) {
            super(prev);
        }

        @Specialization(guards = "isOtherNull(array, other)")
        public RubyArray replace(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(null, 0);
            return array;
        }

        @Specialization(guards = "isOtherIntegerFixnum(array, other)")
        public RubyArray replaceIntegerFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((int[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

        @Specialization(guards = "isOtherLongFixnum(array, other)")
        public RubyArray replaceLongFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((long[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

        @Specialization(guards = "isOtherFloat(array, other)")
        public RubyArray replaceFloat(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((double[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

        @Specialization(guards = "isOtherObject(array, other)")
        public RubyArray replaceObject(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((Object[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true)
    @ImportStatic(ArrayGuards.class)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        @Child private ArrayBuilderNode arrayBuilder;

        public SelectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public SelectNode(SelectNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isNull(array)")
        public Object selectNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isObject(array)")
        public Object selectObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.getStore();

            Object selectedStore = arrayBuilder.start(array.getSize());
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    notDesignedForCompilation();

                    if (yieldIsTruthy(frame, block,  new Object[]{value})) {
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public Object selectFixnumInteger(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) array.getStore();

            Object selectedStore = arrayBuilder.start(array.getSize());
            int selectedSize = 0;

            int count = 0;

            try {
                for (int n = 0; n < array.getSize(); n++) {
                    if (CompilerDirectives.inInterpreter()) {
                        count++;
                    }

                    final Object value = store[n];

                    notDesignedForCompilation();

                    if (yieldIsTruthy(frame, block, value)) {
                        selectedStore = arrayBuilder.append(selectedStore, selectedSize, value);
                        selectedSize++;
                    }
                }
            } finally {
                if (CompilerDirectives.inInterpreter()) {
                    getRootNode().reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

    }

    @CoreMethod(names = "shift")
    public abstract static class ShiftNode extends CoreMethodNode {

        public ShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ShiftNode(ShiftNode prev) {
            super(prev);
        }

        @Specialization
        public Object shift(RubyArray array) {
            notDesignedForCompilation();

            return array.slowShift();
        }

    }

    @CoreMethod(names = {"size", "length"})
    public abstract static class SizeNode extends ArrayCoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubyArray array) {
            return array.getSize();
        }

    }

    @CoreMethod(names = "sort", needsBlock = true)
    public abstract static class SortNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode compareDispatchNode;
        @Child private YieldDispatchHeadNode yieldNode;

        public SortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = DispatchHeadNodeFactory.createMethodCall(context);
            yieldNode = new YieldDispatchHeadNode(context);
        }

        public SortNode(SortNode prev) {
            super(prev);
            compareDispatchNode = prev.compareDispatchNode;
            yieldNode = prev.yieldNode;
        }

        @Specialization(guards = "isNull(array)")
        public RubyArray sortNull(RubyArray array, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @ExplodeLoop
        @Specialization(guards = {"isIntegerFixnum(array)", "isSmall(array)"})
        public RubyArray sortVeryShortIntegerFixnum(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            final int[] store = (int[]) array.getStore();
            final int[] newStore = new int[store.length];

            final int size = array.getSize();

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < RubyArray.ARRAYS_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < RubyArray.ARRAYS_SMALL; j++) {
                        if (j < size) {
                            if ((int) compareDispatchNode.call(frame, store[j], "<=>", null, store[i]) < 0) {
                                final int temp = store[j];
                                store[j] = store[i];
                                store[i] = temp;
                            }
                        }
                    }
                    newStore[i] = store[i];
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, size);
        }

        @Specialization(guards = "isIntegerFixnum(array)")
        public RubyArray sortIntegerFixnum(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Object[] boxed = ArrayUtils.box((int[]) array.getStore());
            sort(frame, boxed);
            final int[] unboxed = ArrayUtils.unboxInteger(boxed, array.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.getSize());
        }

        @ExplodeLoop
        @Specialization(guards = {"isLongFixnum(array)", "isSmall(array)"})
        public RubyArray sortVeryShortLongFixnum(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            final long[] store = (long[]) array.getStore();
            final long[] newStore = new long[store.length];

            final int size = array.getSize();

            // Selection sort - written very carefully to allow PE

            for (int i = 0; i < RubyArray.ARRAYS_SMALL; i++) {
                if (i < size) {
                    for (int j = i + 1; j < RubyArray.ARRAYS_SMALL; j++) {
                        if (j < size) {
                            if ((int) compareDispatchNode.call(frame, store[j], "<=>", null, store[i]) < 0) {
                                final long temp = store[j];
                                store[j] = store[i];
                                store[i] = temp;
                            }
                        }
                    }
                    newStore[i] = store[i];
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, size);
        }

        @Specialization(guards = "isLongFixnum(array)")
        public RubyArray sortLongFixnum(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Object[] boxed = ArrayUtils.box((long[]) array.getStore());
            sort(frame, boxed);
            final long[] unboxed = ArrayUtils.unboxLong(boxed, array.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.getSize());
        }

        @Specialization(guards = "isFloat(array)")
        public RubyArray sortDouble(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Object[] boxed = ArrayUtils.box((double[]) array.getStore());
            sort(frame, boxed);
            final double[] unboxed = ArrayUtils.unboxDouble(boxed, array.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.getSize());
        }

        @Specialization(guards = {"isObject(array)", "isSmall(array)"})
        public RubyArray sortVeryShortObject(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            final Object[] oldStore = (Object[]) array.getStore();
            final Object[] store = Arrays.copyOf(oldStore, oldStore.length);

            // Insertion sort

            final int size = array.getSize();

            for (int i = 1; i < size; i++) {
                final Object x = store[i];
                int j = i;
                // TODO(CS): node for this cast
                while (j > 0 && (int) compareDispatchNode.call(frame, store[j - 1], "<=>", null, x) > 0) {
                    store[j] = store[j - 1];
                    j--;
                }
                store[j] = x;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, size);
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray sortObject(VirtualFrame frame, RubyArray array, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final Object[] store = Arrays.copyOf((Object[]) array.getStore(), array.getSize());
            sort(frame, store);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, array.getSize());
        }

        @Specialization(guards = "isObject(array)")
        public RubyArray sortWithCompareBlock(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            final Object[] store = Arrays.copyOf((Object[]) array.getStore(), array.getSize());
            sort(frame, store, block);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, array.getSize());
        }

        private <T> void sort(VirtualFrame frame, T[] objects) {
            final VirtualFrame finalFrame = frame;

            Arrays.sort(objects, new Comparator<Object>() {

                @Override
                public int compare(Object a, Object b) {
                    // TODO(CS): node for this cast
                    return (int) compareDispatchNode.call(finalFrame, a, "<=>", null, b);
                }

            });
        }

        private <T> void sort(VirtualFrame frame, T[] objects, RubyProc compare) {
            final VirtualFrame finalFrame = frame;
            final RubyProc finalCompare = compare;

            Arrays.sort(objects, new Comparator<Object>() {

                @Override
                public int compare(Object a, Object b) {
                    // TODO(CS): node for this cast
                    return (int) yieldNode.dispatch(finalFrame, finalCompare, a, b);
                }

            });
        }

        protected static boolean isSmall(RubyArray array) {
            return array.getSize() <= RubyArray.ARRAYS_SMALL;
        }

    }

    @CoreMethod(names = "sort!")
    public abstract static class SortBangNode extends ArrayCoreMethodNode {

        @Child private CallDispatchHeadNode compareDispatchNode;

        public SortBangNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public SortBangNode(SortBangNode prev) {
            super(prev);
            compareDispatchNode = prev.compareDispatchNode;
        }

        @Specialization
        public RubyArray sort(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Object[] store = array.slowToArray();
            sort(frame, store);
            array.setStore(store, array.getSize());
            return array;
        }

        // TODO(CS): copied from #sort

        private <T> void sort(VirtualFrame frame, T[] objects) {
            final VirtualFrame finalFrame = frame;

            Arrays.sort(objects, new Comparator<Object>() {

                @Override
                public int compare(Object a, Object b) {
                    // TODO(CS): node for this cast
                    return (int) compareDispatchNode.call(finalFrame, a, "<=>", null, b);
                }

            });
        }

    }

    @CoreMethod(names = "uniq")
    public abstract static class UniqNode extends CoreMethodNode {

        public UniqNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UniqNode(UniqNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray uniq(RubyArray array) {
            notDesignedForCompilation();

            final RubyArray uniq = new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0);

            for (Object value : array.slowToArray()) {
                boolean duplicate = false;

                for (Object compare : uniq.slowToArray()) {
                    if ((boolean) DebugOperations.send(getContext(), value, "==", null, compare)) {
                        duplicate = true;
                        break;
                    }
                }

                if (!duplicate) {
                    uniq.slowPush(value);
                }
            }

            return uniq;
        }

    }

    @CoreMethod(names = "unshift", argumentsAsArray = true)
    public abstract static class UnshiftNode extends CoreMethodNode {

        public UnshiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnshiftNode(UnshiftNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray unshift(RubyArray array, Object... args) {
            notDesignedForCompilation();

            array.slowUnshift(args);
            return array;
        }

    }

    @CoreMethod(names = "zip", required = 1)
    public abstract static class ZipNode extends ArrayCoreMethodNode {

        public ZipNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ZipNode(ZipNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isObject(array)", "isOtherIntegerFixnum(array, other)"})
        public RubyArray zipObjectIntegerFixnum(RubyArray array, RubyArray other) {
            final Object[] a = (Object[]) array.getStore();
            final int aLength = array.getSize();

            final int[] b = (int[]) other.getStore();
            final int bLength = other.getSize();

            final int zippedLength = Math.min(aLength, bLength);
            final Object[] zipped = new Object[zippedLength];

            for (int n = 0; n < zippedLength; n++) {
                zipped[n] = new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{a[n], b[n]}, 2);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), zipped, zippedLength);
        }

        @Specialization(guards = {"isObject(array)", "isOtherObject(array, other)"})
        public RubyArray zipObjectObject(RubyArray array, RubyArray other) {
            final Object[] a = (Object[]) array.getStore();
            final int aLength = array.getSize();

            final Object[] b = (Object[]) other.getStore();
            final int bLength = other.getSize();

            final int zippedLength = Math.min(aLength, bLength);
            final Object[] zipped = new Object[zippedLength];

            for (int n = 0; n < zippedLength; n++) {
                zipped[n] = new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{a[n], b[n]}, 2);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), zipped, zippedLength);
        }

    }

}
