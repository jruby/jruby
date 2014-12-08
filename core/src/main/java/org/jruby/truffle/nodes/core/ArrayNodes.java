/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.ImportGuards;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.CoreSourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.PredicateDispatchHeadNode;
import org.jruby.truffle.nodes.methods.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.methods.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.methods.locals.ReadLevelVariableNodeFactory;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyRange;
import org.jruby.truffle.runtime.methods.MethodLike;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.runtime.util.ArrayUtils;
import org.jruby.util.Memo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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

        @Specialization(guards = "areBothIntegerFixnum")
        public RubyArray addBothIntegerFixnum(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final int[] combined = new int[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothLongFixnum")
        public RubyArray addBothLongFixnum(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final long[] combined = new long[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothFloat")
        public RubyArray addBothFloat(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final double[] combined = new double[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothObject")
        public RubyArray addBothObject(RubyArray a, RubyArray b) {
            final int combinedSize = a.getSize() + b.getSize();
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(a.getStore(), 0, combined, 0, a.getSize());
            System.arraycopy(b.getStore(), 0, combined, a.getSize(), b.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = {"isNull", "isOtherIntegerFixnum"})
        public RubyArray addNullIntegerFixnum(RubyArray a, RubyArray b) {
            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((int[]) b.getStore(), size), size);
        }

        @Specialization(guards = {"isNull", "isOtherLongFixnum"})
        public RubyArray addNullLongFixnum(RubyArray a, RubyArray b) {
            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((long[]) b.getStore(), size), size);
        }

        @Specialization(guards = {"isNull", "isOtherObject"})
        public RubyArray addNullObject(RubyArray a, RubyArray b) {
            final int size = b.getSize();
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((Object[]) b.getStore(), size), size);
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends ArrayCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothIntegerFixnum")
        public RubyArray subIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int[] as = (int[]) a.getStore();
            final int[] bs = (int[]) b.getStore();

            final int[] sub = new int[a.getSize()];

            int i = 0;

            for (int n = 0; n < a.getSize(); n++) {
                if (!ArrayUtils.contains(bs, as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization(guards = "areBothLongFixnum")
        public RubyArray subLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final long[] as = (long[]) a.getStore();
            final long[] bs = (long[]) b.getStore();

            final long[] sub = new long[a.getSize()];

            int i = 0;

            for (int n = 0; n < a.getSize(); n++) {
                if (!ArrayUtils.contains(bs, as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization(guards = "areBothFloat")
        public RubyArray subDouble(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final double[] as = (double[]) a.getStore();
            final double[] bs = (double[]) b.getStore();

            final double[] sub = new double[a.getSize()];

            int i = 0;

            for (int n = 0; n < a.getSize(); n++) {
                if (!ArrayUtils.contains(bs, as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization(guards = "areBothObject")
        public RubyArray subObject(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final Object[] as = (Object[]) a.getStore();
            final Object[] bs = (Object[]) b.getStore();

            final Object[] sub = new Object[a.getSize()];

            int i = 0;

            for (int n = 0; n < a.getSize(); n++) {
                if (!ArrayUtils.contains(bs, b.getSize(), as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization(guards = {"isObject", "isOtherIntegerFixnum"})
        public RubyArray subObjectIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final Object[] as = (Object[]) a.getStore();
            final Object[] bs = ArrayUtils.box((int[]) b.getStore());

            final Object[] sub = new Object[a.getSize()];

            int i = 0;

            for (int n = 0; n < a.getSize(); n++) {
                if (!ArrayUtils.contains(bs, b.getSize(), as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization
        public RubyArray sub(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final Object[] as = a.slowToArray();
            final Object[] bs = b.slowToArray();

            final Object[] sub = new Object[a.getSize()];

            int i = 0;

            for (int n = 0; n < a.getSize(); n++) {
                if (!ArrayUtils.contains(bs, b.getSize(), as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
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

        @Specialization(guards = "isNull")
        public RubyArray mulEmpty(RubyArray array, int count) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum")
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

        @Specialization(guards = "isLongFixnum")
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

        @Specialization(guards = "isFloat")
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

        @Specialization(guards = "isObject")
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

    @CoreMethod(names = "|", required = 1)
    public abstract static class UnionNode extends ArrayCoreMethodNode {

        public UnionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnionNode(UnionNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothIntegerFixnum")
        public RubyArray orIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int[] as = (int[]) a.getStore();
            final int[] bs = (int[]) b.getStore();

            final int[] or = Arrays.copyOf(as, a.getSize() + b.getSize());

            int i = a.getSize();

            for (int n = 0; n < b.getSize(); n++) {
                if (!ArrayUtils.contains(as, bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

        @Specialization(guards = "areBothLongFixnum")
        public RubyArray orLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final long[] as = (long[]) a.getStore();
            final long[] bs = (long[]) b.getStore();

            final long[] or = Arrays.copyOf(as, a.getSize() + b.getSize());

            int i = a.getSize();

            for (int n = 0; n < b.getSize(); n++) {
                if (!ArrayUtils.contains(as, bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

        @Specialization(guards = "areBothFloat")
        public RubyArray orDouble(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final double[] as = (double[]) a.getStore();
            final double[] bs = (double[]) b.getStore();

            final double[] or = Arrays.copyOf(as, a.getSize() + b.getSize());

            int i = a.getSize();

            for (int n = 0; n < b.getSize(); n++) {
                if (!ArrayUtils.contains(as, bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

        @Specialization(guards = "areBothObject")
        public RubyArray orObject(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final Object[] as = (Object[]) a.getStore();
            final Object[] bs = (Object[]) b.getStore();

            final Object[] or = Arrays.copyOf(as, a.getSize() + b.getSize());

            int i = a.getSize();

            for (int n = 0; n < b.getSize(); n++) {
                if (!ArrayUtils.contains(as, a.getSize(), bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

    }

    @CoreMethod(names = {"==", "eql?"}, required = 1)
    public abstract static class EqualNode extends ArrayCoreMethodNode {

        @Child protected PredicateDispatchHeadNode equals;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equals = new PredicateDispatchHeadNode(context);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
            equals = prev.equals;
        }

        @Specialization(guards = "areBothIntegerFixnum")
        public boolean equalIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.getSize() != b.getSize()) {
                return false;
            }

            return Arrays.equals((int[]) a.getStore(), (int[]) b.getStore());
        }

        @Specialization(guards = "areBothLongFixnum")
        public boolean equalLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.getSize() != b.getSize()) {
                return false;
            }

            return Arrays.equals((long[]) a.getStore(), (long[]) b.getStore());
        }

        @Specialization(guards = "areBothFloat")
        public boolean equalFloat(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.getSize() != b.getSize()) {
                return false;
            }

            return Arrays.equals((double[]) a.getStore(), (double[]) b.getStore());
        }

        @Specialization
        public boolean equal(VirtualFrame frame, RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.getSize() != b.getSize()) {
                return false;
            }

            final Object[] as = a.slowToArray();
            final Object[] bs = b.slowToArray();

            for (int n = 0; n < a.getSize(); n++) {
                if (!equals.call(frame, as[n], "==", null, bs[n])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization
        public boolean equal(VirtualFrame frame, RubyArray a, Object b) {
            notDesignedForCompilation();

            if (!(b instanceof RubyArray)) {
                return false;
            } else {
                return equal(frame, a, (RubyArray) b);
            }
        }

    }

    @CoreMethod(names = {"[]", "at"}, required = 1, optional = 1, lowerFixnumParameters = {0, 1})
    public abstract static class IndexNode extends ArrayCoreMethodNode {

        public IndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IndexNode(IndexNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public RubyNilClass getNull(RubyArray array, int index, UndefinedPlaceholder undefined) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum", rewriteOn=UnexpectedResultException.class)
        public int getIntegerFixnumInBounds(RubyArray array, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                return ((int[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(contains = "getIntegerFixnumInBounds", guards = "isIntegerFixnum")
        public Object getIntegerFixnum(RubyArray array, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return ((int[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(guards = "isLongFixnum", rewriteOn=UnexpectedResultException.class)
        public long getLongFixnumInBounds(RubyArray array, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                return ((long[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(contains = "getLongFixnumInBounds", guards = "isLongFixnum")
        public Object getLongFixnum(RubyArray array, int index, UndefinedPlaceholder undefined) {

            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return ((long[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(guards = "isFloat", rewriteOn=UnexpectedResultException.class)
        public double getFloatInBounds(RubyArray array, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                return ((double[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(contains = "getFloatInBounds", guards = "isFloat")
        public Object getFloat(RubyArray array, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return ((double[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(guards = "isObject")
        public Object getObject(RubyArray array, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return ((Object[]) array.getStore())[normalisedIndex];
            }
        }

        @Specialization(guards = "isObject")
        public Object getObject(RubyArray array, int index, int length) {
            notDesignedForCompilation();

            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange((Object[]) array.getStore(), normalisedIndex, normalisedIndex + length), length);
            }
        }

        @Specialization(guards = "isObject")
        public Object getObject(RubyArray array, RubyRange.IntegerFixnumRange range, UndefinedPlaceholder undefined) {
            notDesignedForCompilation();

            final int normalisedIndex = array.normaliseIndex(range.getBegin());

            if (normalisedIndex < 0 || normalisedIndex >= array.getSize()) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final int end = array.normaliseIndex(range.getEnd());
                final int excludingEnd = array.clampExclusiveIndex(range.doesExcludeEnd() ? end : end+1);

                return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange((Object[]) array.getStore(), normalisedIndex, excludingEnd), excludingEnd - normalisedIndex);
            }
        }

    }

    @CoreMethod(names = "[]=", required = 2, optional = 1, lowerFixnumParameters = 0)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        private final BranchProfile tooSmallBranch = BranchProfile.create();
        private final BranchProfile pastEndBranch = BranchProfile.create();
        private final BranchProfile appendBranch = BranchProfile.create();
        private final BranchProfile beyondBranch = BranchProfile.create();
        private final BranchProfile reallocateBranch = BranchProfile.create();

        public IndexSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IndexSetNode(IndexSetNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public Object setNullIntegerFixnum(RubyArray array, int index, int value, UndefinedPlaceholder unused) {
            if (index == 0) {
                array.setStore(new int[]{value}, 1);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }

            return value;
        }

        @Specialization(guards = "isNull")
        public Object setNullLongFixnum(RubyArray array, int index, long value, UndefinedPlaceholder unused) {
            if (index == 0) {
                array.setStore(new long[]{value}, 1);
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }

            return value;
        }

        @Specialization(guards = "isNull")
        public Object setNullObject(RubyArray array, int index, Object value, UndefinedPlaceholder unused) {
            notDesignedForCompilation();

            if (index == 0) {
                array.slowPush(value);
            } else {
                throw new UnsupportedOperationException();
            }

            return value;
        }

        @Specialization(guards = "isIntegerFixnum")
        public int setIntegerFixnum(RubyArray array, int index, int value, UndefinedPlaceholder unused) {
            final int normalisedIndex = array.normaliseIndex(index);
            int[] store = (int[]) array.getStore();

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", index, array.getSize(), this));
            } else if (normalisedIndex >= array.getSize()) {
                pastEndBranch.enter();

                if (normalisedIndex == array.getSize()) {
                    appendBranch.enter();

                    if (normalisedIndex >= store.length) {
                        reallocateBranch.enter();
                        array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, normalisedIndex + 1)), array.getSize());
                    }

                    store[normalisedIndex] = value;
                    array.setSize(array.getSize() + 1);
                } else if (normalisedIndex > array.getSize()) {
                    beyondBranch.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                store[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isIntegerFixnum")
        public long setLongInIntegerFixnum(RubyArray array, int index, long value, UndefinedPlaceholder unused) {
            if (array.getAllocationSite() != null) {
                array.getAllocationSite().convertedIntToLong();
            }

            final int normalisedIndex = array.normaliseIndex(index);

            long[] store = ArrayUtils.longCopyOf((int[]) array.getStore());
            array.setStore(store, array.getSize());

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", index, array.getSize(), this));
            } else if (normalisedIndex >= array.getSize()) {
                pastEndBranch.enter();

                if (normalisedIndex == array.getSize()) {
                    appendBranch.enter();

                    if (normalisedIndex >= store.length) {
                        reallocateBranch.enter();
                        array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, normalisedIndex + 1)), array.getSize());
                    }

                    store[normalisedIndex] = value;
                    array.setSize(array.getSize() + 1);
                } else if (normalisedIndex > array.getSize()) {
                    beyondBranch.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                store[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isIntegerFixnum")
        public RubyArray setIntegerFixnum(RubyArray array, int start, int length, RubyArray value) {
            notDesignedForCompilation();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexNegativeLength(length, this));
            }

            if (value.getSize() == 0) {
                final int begin = array.normaliseIndex(start);
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
                array.setSize(array.getSize() - length);

                return value;
            } else {
                throw new UnsupportedOperationException();
            }
        }

        @Specialization(guards = "isLongFixnum")
        public int setLongFixnum(RubyArray array, int index, int value, UndefinedPlaceholder unused) {
            setLongFixnum(array, index, (long) value, unused);
            return value;
        }

        @Specialization(guards = "isLongFixnum")
        public long setLongFixnum(RubyArray array, int index, long value, UndefinedPlaceholder unused) {
            final int normalisedIndex = array.normaliseIndex(index);
            long[] store = (long[]) array.getStore();

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", index, array.getSize(), this));
            } else if (normalisedIndex >= array.getSize()) {
                pastEndBranch.enter();

                if (normalisedIndex == array.getSize()) {
                    appendBranch.enter();

                    if (normalisedIndex >= store.length) {
                        reallocateBranch.enter();
                        array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, normalisedIndex + 1)), array.getSize());
                    }

                    store[normalisedIndex] = value;
                    array.setSize(array.getSize() + 1);
                } else if (normalisedIndex > array.getSize()) {
                    beyondBranch.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                store[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isFloat")
        public double setFloat(RubyArray array, int index, double value, UndefinedPlaceholder unused) {
            final int normalisedIndex = array.normaliseIndex(index);
            double[] store = (double[]) array.getStore();

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", index, array.getSize(), this));
            } else if (normalisedIndex >= array.getSize()) {
                pastEndBranch.enter();

                if (normalisedIndex == array.getSize()) {
                    appendBranch.enter();

                    if (normalisedIndex >= store.length) {
                        reallocateBranch.enter();
                        array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, normalisedIndex + 1)), array.getSize());
                    }

                    store[normalisedIndex] = value;
                    array.setSize(array.getSize() + 1);
                } else if (normalisedIndex > array.getSize()) {
                    beyondBranch.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                store[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isObject")
        public Object setObject(RubyArray array, int index, Object value, UndefinedPlaceholder unused) {
            final int normalisedIndex = array.normaliseIndex(index);
            Object[] store = (Object[]) array.getStore();

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexTooSmallError("array", index, array.getSize(), this));
            } else if (normalisedIndex >= array.getSize()) {
                pastEndBranch.enter();

                if (normalisedIndex == array.getSize()) {
                    appendBranch.enter();

                    if (normalisedIndex >= store.length) {
                        reallocateBranch.enter();
                        array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, normalisedIndex + 1)), array.getSize());
                    }

                    store[normalisedIndex] = value;
                    array.setSize(array.getSize() + 1);
                } else if (normalisedIndex > array.getSize()) {
                    beyondBranch.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                store[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = {"isObject", "!isRubyArray(arguments[3])"})
        public Object setObject(RubyArray array, int start, int length, Object value) {
            notDesignedForCompilation();

            if (length < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().indexNegativeLength(length, this));
            }

            final int begin = array.normaliseIndex(start);

            if (begin >= array.getSize()) {
                // We don't care of length in this case
                return setObject(array, start, value, UndefinedPlaceholder.INSTANCE);
            } else {
                throw  new UnsupportedOperationException();
            }
        }

        @Specialization(guards = "isIntegerFixnum")
        public RubyArray setIntegerFixnumRange(RubyArray array, RubyRange.IntegerFixnumRange range, RubyArray other, UndefinedPlaceholder unused) {
            if (range.doesExcludeEnd()) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else {
                int normalisedBegin = array.normaliseIndex(range.getBegin());
                int normalisedEnd = array.normaliseIndex(range.getEnd());

                if (normalisedBegin == 0 && normalisedEnd == array.getSize() - 1) {
                    array.setStore(Arrays.copyOf((int[]) other.getStore(), other.getSize()), other.getSize());
                } else {
                    throw new RuntimeException();
                }
            }

            return other;
        }

    }

    @CoreMethod(names = "all?", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class AllNode extends YieldingCoreMethodNode {

        public AllNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AllNode(AllNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public boolean allNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return true;
        }

        @Specialization(guards = "isIntegerFixnum")
        public boolean allIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (!yieldIsTruthy(frame, block, ((int[]) array.getStore())[n])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "isLongFixnum")
        public boolean allLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (!yieldIsTruthy(frame, block, ((long[]) array.getStore())[n])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "isFloat")
        public boolean allFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (!yieldIsTruthy(frame, block, ((double[]) array.getStore())[n])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "isObject")
        public boolean allObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (!yieldIsTruthy(frame, block, ((Object[]) array.getStore())[n])) {
                    return false;
                }
            }

            return true;
        }

    }

    @CoreMethod(names = "any?", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class AnyNode extends YieldingCoreMethodNode {

        public AnyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AnyNode(AnyNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public boolean anyNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return false;
        }

        @Specialization(guards = "isIntegerFixnum")
        public boolean allIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (yieldIsTruthy(frame, block, ((int[]) array.getStore())[n])) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isLongFixnum")
        public boolean anyLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (yieldIsTruthy(frame, block, ((long[]) array.getStore())[n])) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isFloat")
        public boolean anyFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (yieldIsTruthy(frame, block, ((double[]) array.getStore())[n])) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isObject")
        public boolean anyObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n = 0; n < array.getSize(); n++) {
                if (yieldIsTruthy(frame, block, ((Object[]) array.getStore())[n])) {
                    return true;
                }
            }

            return false;
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
            notDesignedForCompilation();

            array.setSize(0);
            return array;
        }

    }

    @CoreMethod(names = "compact")
    public abstract static class CompactNode extends ArrayCoreMethodNode {

        public CompactNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompactNode(CompactNode prev) {
            super(prev);
        }

        @Specialization(guards = "!isObject")
        public RubyArray compatNotObjects(RubyArray array) {
            return array;
        }

        @Specialization(guards = "isObject")
        public RubyArray compatObjects(RubyArray array) {
            notDesignedForCompilation();

            final Object[] compacted = new Object[array.getSize()];
            int compactedSize = 0;

            for (Object object : array.slowToArray()) {
                if (object != getContext().getCoreLibrary().getNilObject()) {
                    compacted[compactedSize] = object;
                    compactedSize++;
                }
            }

            array.setStore(compacted, compactedSize);

            return array;
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

        @Specialization(guards = "areBothNull")
        public RubyArray concatNull(RubyArray array, RubyArray other) {
            return array;
        }

        @Specialization(guards = "areBothIntegerFixnum")
        public RubyArray concatIntegerFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.setStore(Arrays.copyOf((int[]) array.getStore(), array.getSize() + other.getSize()), array.getSize());
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setSize(array.getSize() + other.getSize());
            return array;
        }

        @Specialization(guards = "areBothLongFixnum")
        public RubyArray concatLongFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.setStore(Arrays.copyOf((long[]) array.getStore(), array.getSize() + other.getSize()), array.getSize());
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setSize(array.getSize() + other.getSize());
            return array;
        }

        @Specialization(guards = "areBothFloat")
        public RubyArray concatDouble(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.setStore(Arrays.copyOf((double[]) array.getStore(), array.getSize() + other.getSize()), array.getSize());
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setSize(array.getSize() + other.getSize());
            return array;
        }

        @Specialization(guards = "areBothObject")
        public RubyArray concatObject(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.setStore(Arrays.copyOf((Object[]) array.getStore(), array.getSize() + other.getSize()), array.getSize());
            System.arraycopy(other.getStore(), 0, array.getStore(), array.getSize(), other.getSize());
            array.setSize(array.getSize() + other.getSize());
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

        @Child protected KernelNodes.SameOrEqualNode equalNode;

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null,null});
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization(guards = "isIntegerFixnum")
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

            array.setSize(i);
            return found;
        }

        @Specialization(guards = "isObject")
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

            array.setSize(i);
            return found;
        }

    }

    @CoreMethod(names = "delete_at", required = 1)
    public abstract static class DeleteAtNode extends ArrayCoreMethodNode {

        private static final BranchProfile tooSmallBranch = BranchProfile.create();
        private static final BranchProfile beyondEndBranch = BranchProfile.create();

        public DeleteAtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteAtNode(DeleteAtNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum", rewriteOn = UnexpectedResultException.class)
        public int deleteAtIntegerFixnumInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else if (normalisedIndex >= array.getSize()) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final int[] store = (int[]) array.getStore();
                final int value = store[normalisedIndex];
                System.arraycopy(store, normalisedIndex + 1, store, normalisedIndex, array.getSize() - normalisedIndex - 1);
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "deleteAtIntegerFixnumInBounds", guards = "isIntegerFixnum")
        public Object deleteAtIntegerFixnum(RubyArray array, int index) {
            notDesignedForCompilation();

            int normalisedIndex = index;

            if (normalisedIndex < 0) {
                normalisedIndex = array.getSize() + index;
            }

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else if (normalisedIndex >= array.getSize()) {
                beyondEndBranch.enter();
                throw new UnsupportedOperationException();
            } else {
                final int[] store = (int[]) array.getStore();
                final int value = store[normalisedIndex];
                System.arraycopy(store, normalisedIndex + 1, store, normalisedIndex, array.getSize() - normalisedIndex - 1);
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

    }

    @CoreMethod(names = "each", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class EachNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public Object eachNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongFixnum")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isFloat")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObject")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "each_with_index", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
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

        @Specialization(guards = "isNull")
        public RubyArray eachWithEmpty(VirtualFrame frame, RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isObject")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return array;
        }

    }

    @CoreMethod(names = "empty?")
    public abstract static class EmptyNode extends ArrayCoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isEmpty(RubyArray array) {
            return array.getSize() == 0;
        }

    }

    @CoreMethod(names = "find", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class FindNode extends YieldingCoreMethodNode {

        public FindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FindNode(FindNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public Object findNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum")
        public Object findIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            final int[] store = (int[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                try {
                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block, value)) {
                        return value;
                    }
                } catch (BreakException e) {
                    break;
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isLongFixnum")
        public Object findLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            final long[] store = (long[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                try {
                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block, value)) {
                        return value;
                    }
                } catch (BreakException e) {
                    break;
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isFloat")
        public Object findFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            final double[] store = (double[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                try {
                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block, value)) {
                        return value;
                    }
                } catch (BreakException e) {
                    break;
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isObject")
        public Object findObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) array.getStore();

            for (int n = 0; n < array.getSize(); n++) {
                try {
                    final Object value = store[n];

                    if (yieldIsTruthy(frame, block, value)) {
                        return value;
                    }
                } catch (BreakException e) {
                    break;
                }
            }

            return getContext().getCoreLibrary().getNilObject();
        }
    }

    @CoreMethod(names = "first")
    public abstract static class FirstNode extends ArrayCoreMethodNode {

        public FirstNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FirstNode(FirstNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public RubyNilClass firstNull(RubyArray array) {
            notDesignedForCompilation();

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum")
        public Object firstIntegerFixnum(RubyArray array) {
            notDesignedForCompilation();

            if (array.getSize() == 0) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return ((int[]) array.getStore())[0];
            }
        }

        @Specialization(guards = "isObject")
        public Object firstObject(RubyArray array) {
            notDesignedForCompilation();

            if (array.getSize() == 0) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return ((Object[]) array.getStore())[0];
            }
        }

    }

    @CoreMethod(names = "flatten")
    public abstract static class FlattenNode extends ArrayCoreMethodNode {

        public FlattenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FlattenNode(FlattenNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray flatten(RubyArray array) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "include?", required = 1)
    public abstract static class IncludeNode extends ArrayCoreMethodNode {

        @Child protected KernelNodes.SameOrEqualNode equalNode;

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            equalNode = KernelNodesFactory.SameOrEqualNodeFactory.create(context, sourceSection, new RubyNode[]{null,null});
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
            equalNode = prev.equalNode;
        }

        @Specialization(guards = "isNull")
        public boolean includeNull(VirtualFrame frame, RubyArray array, Object value) {
            return false;
        }

        @Specialization(guards = "isIntegerFixnum")
        public boolean includeFixnum(VirtualFrame frame, RubyArray array, Object value) {
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

        @Specialization(guards = "isObject")
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

    @CoreMethod(names = "initialize", needsBlock = true, required = 1, optional = 1)
    @ImportGuards(ArrayGuards.class)
    public abstract static class InitializeNode extends YieldingCoreMethodNode {

        @Child protected ArrayBuilderNode arrayBuilder;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, UndefinedPlaceholder defaultValue, UndefinedPlaceholder block) {
            return initialize(array, size, getContext().getCoreLibrary().getNilObject(), block);
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

        @Specialization
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

        @Specialization(guards = "isOtherNull")
        public RubyArray initializeCopyNull(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(null, 0);
            return self;
        }

        @Specialization(guards = "isOtherIntegerFixnum")
        public RubyArray initializeCopyIntegerFixnum(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((int[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

        @Specialization(guards = "isOtherLongFixnum")
        public RubyArray initializeCopyLongFixnum(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((long[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

        @Specialization(guards = "isOtherFloat")
        public RubyArray initializeCopyFloat(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((double[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

        @Specialization(guards = "isOtherObject")
        public RubyArray initializeCopyObject(RubyArray self, RubyArray from) {
            if (self == from) {
                return self;
            }
            self.setStore(Arrays.copyOf((Object[]) from.getStore(), from.getSize()), from.getSize());
            return self;
        }

    }

    @CoreMethod(names = {"inject", "reduce"}, needsBlock = true, optional = 1)
    @ImportGuards(ArrayGuards.class)
    public abstract static class InjectNode extends YieldingCoreMethodNode {

        @Child protected DispatchHeadNode dispatch;

        public InjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            dispatch = new DispatchHeadNode(context, Dispatch.MissingBehavior.CALL_METHOD_MISSING);
        }

        public InjectNode(InjectNode prev) {
            super(prev);
            dispatch = prev.dispatch;
        }

        @Specialization(guards = "isObject")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return accumulator;
        }

        @Specialization
        public Object inject(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            notDesignedForCompilation();

            final Object[] store = array.slowToArray();

            if (store.length < 2) {
                throw new UnsupportedOperationException();
            }

            Object accumulator = initial;

            for (int n = 0; n < array.getSize(); n++) {
                accumulator = yield(frame, block, accumulator, store[n]);
            }

            return accumulator;
        }

        @Specialization
        public Object inject(VirtualFrame frame, RubyArray array, RubySymbol symbol, UndefinedPlaceholder unused) {
            notDesignedForCompilation();

            final Object[] store = array.slowToArray();

            if (store.length < 2) {
                throw new UnsupportedOperationException();
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

        private static final BranchProfile tooSmallBranch = BranchProfile.create();

        public InsertNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InsertNode(InsertNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public Object insert(RubyArray array, int index, Object value) {
            notDesignedForCompilation();

            final Object[] store = new Object[index + 1];
            Arrays.fill(store, getContext().getCoreLibrary().getNilObject());
            store[index] = value;
            array.setSize(array.getSize() + 1);
            return array;
        }

        @Specialization(guards = "isIntegerFixnum")
        public Object insert(RubyArray array, int index, int value) {
            final int normalisedIndex = array.normaliseIndex(index);
            final int[] store = (int[]) array.getStore();

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (array.getSize() > store.length + 1) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else {
                System.arraycopy(store, normalisedIndex, store, normalisedIndex + 1, array.getSize() - normalisedIndex);
                store[normalisedIndex] = value;
                array.setSize(array.getSize() + 1);
            }

            return array;
        }

    }

    @CoreMethod(names = {"inspect", "to_s"})
    public abstract static class InspectNode extends CoreMethodNode {

        @Child protected DispatchHeadNode inspect;

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            inspect = new DispatchHeadNode(context);
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

                // TODO(CS): to string

                builder.append(inspect.call(frame, objects[n], "inspect", null));
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

    @CoreMethod(names = "last")
    public abstract static class LastNode extends ArrayCoreMethodNode {

        public LastNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LastNode(LastNode prev) {
            super(prev);
        }

        @Specialization
        public Object last(RubyArray array) {
            notDesignedForCompilation();

            if (array.getSize() == 0) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return array.slowToArray()[array.getSize() - 1];
            }
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        @Child protected ArrayBuilderNode arrayBuilder;

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public MapNode(MapNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isNull")
        public RubyArray mapNull(RubyArray array, RubyProc block) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isLongFixnum")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }

        @Specialization(guards = "isFloat")
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

        @Specialization(guards = "isObject")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(mappedStore, arraySize), arraySize);
        }
    }

    @CoreMethod(names = {"map!", "collect!"}, needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        @Child protected ArrayBuilderNode arrayBuilder;

        public MapInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public MapInPlaceNode(MapInPlaceNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isIntegerFixnum")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            array.setStore(arrayBuilder.finish(mappedStore, arraySize), arraySize);

            return array;
        }

        @Specialization(guards = "isObject")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            array.setStore(arrayBuilder.finish(mappedStore, arraySize), arraySize);

            return array;
        }
    }

    // TODO: move into Enumerable?

    @CoreMethod(names = "max")
    public abstract static class MaxNode extends ArrayCoreMethodNode {

        @Child protected DispatchHeadNode eachNode;
        private final MaxBlock maxBlock;

        public MaxNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = new DispatchHeadNode(context);
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

            final VirtualFrame maximumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(maxBlock, null, array, null, new Object[]{}), maxBlock.getFrameDescriptor());
            maximumClosureFrame.setObject(maxBlock.getFrameSlot(), maximum);

            final RubyProc block = new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    maxBlock.getSharedMethodInfo(), maxBlock.getCallTarget(), maxBlock.getCallTarget(),
                    maximumClosureFrame.materialize(), null, null, array, null);

            eachNode.call(frame, array, "each", block);

            if (maximum.get() == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return maximum.get();
            }
        }

    }

    public abstract static class MaxBlockNode extends CoreMethodNode {

        @Child protected DispatchHeadNode compareNode;

        public MaxBlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = new DispatchHeadNode(context);
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

    public static class MaxBlock implements MethodLike {

        private final FrameDescriptor frameDescriptor;
        private final FrameSlot frameSlot;
        private final SharedMethodInfo sharedMethodInfo;
        private final CallTarget callTarget;

        public MaxBlock(RubyContext context) {
            final SourceSection sourceSection = new CoreSourceSection("Array", "max");

            frameDescriptor = new FrameDescriptor();
            frameSlot = frameDescriptor.addFrameSlot("maximum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, "max", false, null, false);

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

        @Override
        public SharedMethodInfo getSharedMethodInfo() {
            return sharedMethodInfo;
        }

        @Override
        public RubyModule getDeclaringModule() {
            throw new UnsupportedOperationException();
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }
    }

    @CoreMethod(names = "min")
    public abstract static class MinNode extends ArrayCoreMethodNode {

        @Child protected DispatchHeadNode eachNode;
        private final MinBlock minBlock;

        public MinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            eachNode = new DispatchHeadNode(context);
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

            final VirtualFrame minimumClosureFrame = Truffle.getRuntime().createVirtualFrame(RubyArguments.pack(minBlock, null, array, null, new Object[]{}), minBlock.getFrameDescriptor());
            minimumClosureFrame.setObject(minBlock.getFrameSlot(), minimum);

            final RubyProc block = new RubyProc(getContext().getCoreLibrary().getProcClass(), RubyProc.Type.PROC,
                    minBlock.getSharedMethodInfo(), minBlock.getCallTarget(), minBlock.getCallTarget(),
                    minimumClosureFrame.materialize(), null, null, array, null);

            eachNode.call(frame, array, "each", block);

            if (minimum.get() == null) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                return minimum.get();
            }
        }

    }

    public abstract static class MinBlockNode extends CoreMethodNode {

        @Child protected DispatchHeadNode compareNode;

        public MinBlockNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareNode = new DispatchHeadNode(context);
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

    public static class MinBlock implements MethodLike {

        private final FrameDescriptor frameDescriptor;
        private final FrameSlot frameSlot;
        private final SharedMethodInfo sharedMethodInfo;
        private final CallTarget callTarget;

        public MinBlock(RubyContext context) {
            final SourceSection sourceSection = new CoreSourceSection("Array", "min");

            frameDescriptor = new FrameDescriptor();
            frameSlot = frameDescriptor.addFrameSlot("minimum_memo");

            sharedMethodInfo = new SharedMethodInfo(sourceSection, null, "min", false, null, false);

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

        @Override
        public SharedMethodInfo getSharedMethodInfo() {
            return sharedMethodInfo;
        }

        @Override
        public RubyModule getDeclaringModule() {
            throw new UnsupportedOperationException();
        }

        public CallTarget getCallTarget() {
            return callTarget;
        }
    }

    @CoreMethod(names = "pack", required = 1)
    public abstract static class PackNode extends ArrayCoreMethodNode {

        public PackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PackNode(PackNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString pack(RubyArray array, RubyString format) {
            notDesignedForCompilation();

            return new RubyString(
                    getContext().getCoreLibrary().getStringClass(),
                    org.jruby.util.Pack.pack(
                            getContext().getRuntime(),
                            getContext().toJRuby(array),
                            getContext().toJRuby(format).getByteList()).getByteList());

        }

    }

    @CoreMethod(names = "permutation", required = 1)
    public abstract static class PermutationNode extends ArrayCoreMethodNode {

        public PermutationNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PermutationNode(PermutationNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray permutation(RubyArray array, int n) {
            notDesignedForCompilation();

            final List<RubyArray> permutations = new ArrayList<>();
            permutationCommon(n, false, array.slowToArray(), permutations);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), permutations.toArray(), permutations.size());
        }

        // Apdapted from JRuby's RubyArray - see attribution there

        private void permutationCommon(int r, boolean repeat, Object[] values, List<RubyArray> permutations) {
            if (r == 0) {
                permutations.add(new RubyArray(getContext().getCoreLibrary().getArrayClass(), null, 0));
            } else if (r == 1) {
                for (int i = 0; i < values.length; i++) {
                    permutations.add(new RubyArray(getContext().getCoreLibrary().getArrayClass(), values[i], 1));
                }
            } else if (r >= 0) {
                int n = values.length;
                permute(n, r,
                        new int[r], 0,
                        new boolean[n],
                        repeat,
                        values, permutations);
            }
        }

        private void permute(int n, int r, int[]p, int index, boolean[]used, boolean repeat, Object[] values, List<RubyArray> permutations) {
            for (int i = 0; i < n; i++) {
                if (repeat || !used[i]) {
                    p[index] = i;
                    if (index < r - 1) {
                        used[i] = true;
                        permute(n, r, p, index + 1, used, repeat, values, permutations);
                        used[i] = false;
                    } else {
                        Object[] result = new Object[r];

                        for (int j = 0; j < r; j++) {
                            result[j] = values[p[j]];
                        }

                        permutations.add(new RubyArray(getContext().getCoreLibrary().getArrayClass(), result, r));
                    }
                }
            }
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

        @Specialization(guards = "isNull")
        public Object popNil(RubyArray array) {
            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization(guards = "isIntegerFixnum", rewriteOn = UnexpectedResultException.class)
        public int popIntegerFixnumInBounds(RubyArray array) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final int value = ((int[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "popIntegerFixnumInBounds", guards = "isIntegerFixnum")
        public Object popIntegerFixnum(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final int value = ((int[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(guards = "isLongFixnum", rewriteOn = UnexpectedResultException.class)
        public long popLongFixnumInBounds(RubyArray array) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final long value = ((long[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "popLongFixnumInBounds", guards = "isLongFixnum")
        public Object popLongFixnum(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final long value = ((long[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(guards = "isFloat", rewriteOn = UnexpectedResultException.class)
        public double popFloatInBounds(RubyArray array) throws UnexpectedResultException {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                throw new UnexpectedResultException(getContext().getCoreLibrary().getNilObject());
            } else {
                final double value = ((double[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(contains = "popFloatInBounds", guards = "isFloat")
        public Object popFloat(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final double value = ((double[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
                return value;
            }
        }

        @Specialization(guards = "isObject")
        public Object popObject(RubyArray array) {
            if (CompilerDirectives.injectBranchProbability(CompilerDirectives.UNLIKELY_PROBABILITY, array.getSize() == 0)) {
                return getContext().getCoreLibrary().getNilObject();
            } else {
                final Object value = ((Object[]) array.getStore())[array.getSize() - 1];
                array.setSize(array.getSize() - 1);
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

        @Specialization(guards = {"isObject", "isOtherObject"})
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

    @CoreMethod(names = {"push", "<<"}, argumentsAsArray = true)
    public abstract static class PushNode extends ArrayCoreMethodNode {

        private final BranchProfile extendBranch = BranchProfile.create();

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PushNode(PushNode prev) {
            super(prev);
        }

        @Specialization(guards = {"isNull", "isSingleIntegerFixnum"})
        public RubyArray pushEmptySingleIntegerFixnum(RubyArray array, Object... values) {
            array.setStore(new int[]{(int) values[0]}, 1);
            return array;
        }

        @Specialization(guards = {"isNull", "isSingleLongFixnum"})
        public RubyArray pushEmptySingleIntegerLong(RubyArray array, Object... values) {
            array.setStore(new long[]{(long) values[0]}, 1);
            return array;
        }

        @Specialization(guards = "isNull")
        public RubyArray pushEmptyObjects(RubyArray array, Object... values) {
            array.setStore(values, values.length);
            return array;
        }

        @Specialization(guards = {"isIntegerFixnum", "isSingleIntegerFixnum"})
        public RubyArray pushIntegerFixnumSingleIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            int[] store = (int[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
                array.setStore(store, array.getSize());
            }

            store[oldSize] = (int) values[0];
            array.setSize(newSize);
            return array;
        }

        @Specialization(guards = {"isLongFixnum", "isSingleIntegerFixnum"})
        public RubyArray pushLongFixnumSingleIntegerFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            long[] store = (long[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
                array.setStore(store, array.getSize());
            }

            store[oldSize] = (long) (int) values[0];
            array.setSize(newSize);
            return array;
        }

        @Specialization(guards = {"isLongFixnum", "isSingleLongFixnum"})
        public RubyArray pushLongFixnumSingleLongFixnum(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            long[] store = (long[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
                array.setStore(store, array.getSize());
            }

            store[oldSize] = (long) values[0];
            array.setSize(newSize);
            return array;
        }

        @Specialization(guards = "isObject")
        public RubyArray pushObject(RubyArray array, Object... values) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + values.length;

            Object[] store = (Object[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize));
                array.setStore(store, oldSize);
            }

            for (int n = 0; n < values.length; n++) {
                store[oldSize + n] = values[n];
            }

            array.setSize(newSize);
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

        @Specialization(guards = "isNull")
        public RubyArray pushEmpty(RubyArray array, Object value) {
            array.setStore(new Object[]{value}, 1);
            return array;
        }

        @Specialization(guards = "isIntegerFixnum")
        public RubyArray pushIntegerFixnumIntegerFixnum(RubyArray array, int value) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            int[] store = (int[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), array.getSize());
            }

            store[oldSize] = value;
            array.setSize(newSize);
            return array;
        }

        @Specialization(guards = "isIntegerFixnum")
        public RubyArray pushIntegerFixnumObject(RubyArray array, Object value) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            final int[] oldStore = (int[]) array.getStore();
            final Object[] newStore = ArrayUtils.box(oldStore, newSize);
            newStore[oldSize] = value;
            array.setStore(newStore, newSize);
            return array;
        }

        @Specialization(guards = "isObject")
        public RubyArray pushObjectObject(RubyArray array, Object value) {
            final int oldSize = array.getSize();
            final int newSize = oldSize + 1;

            Object[] store = (Object[]) array.getStore();

            if (store.length < newSize) {
                extendBranch.enter();
                array.setStore(store = Arrays.copyOf(store, ArrayUtils.capacity(store.length, newSize)), array.getSize());
            }

            store[oldSize] = value;
            array.setSize(newSize);
            return array;
        }

    }

    @CoreMethod(names = "reject!", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class RejectInPlaceNode extends YieldingCoreMethodNode {

        public RejectInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RejectInPlaceNode(RejectInPlaceNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull")
        public Object rejectInPlaceNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return array;
        }

        @Specialization(guards = "isObject")
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

            array.setSize(i);
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

        @Specialization(guards = "isOtherNull")
        public RubyArray replace(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setSize(0);
            return array;
        }

        @Specialization(guards = "isOtherIntegerFixnum")
        public RubyArray replaceIntegerFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((int[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

        @Specialization(guards = "isOtherLongFixnum")
        public RubyArray replaceLongFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((long[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

        @Specialization(guards = "isOtherFloat")
        public RubyArray replaceFloat(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((double[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

        @Specialization(guards = "isOtherObject")
        public RubyArray replaceObject(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.setStore(Arrays.copyOf((Object[]) other.getStore(), other.getSize()), other.getSize());
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true)
    @ImportGuards(ArrayGuards.class)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        @Child protected ArrayBuilderNode arrayBuilder;

        public SelectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            arrayBuilder = new ArrayBuilderNode.UninitializedArrayBuilderNode(context);
        }

        public SelectNode(SelectNode prev) {
            super(prev);
            arrayBuilder = prev.arrayBuilder;
        }

        @Specialization(guards = "isNull")
        public Object selectNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isObject")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), arrayBuilder.finish(selectedStore, selectedSize), selectedSize);
        }

        @Specialization(guards = "isIntegerFixnum")
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
                    ((RubyRootNode) getRootNode()).reportLoopCount(count);
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

    @CoreMethod(names = "slice", required = 2)
    public abstract static class SliceNode extends ArrayCoreMethodNode {

        public SliceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SliceNode(SliceNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum")
        public RubyArray sliceIntegerFixnum(RubyArray array, int start, int length) {
            final int[] store = (int[]) array.getStore();

            final int normalisedStart = array.normaliseIndex(start);
            final int normalisedEnd = Math.min(normalisedStart + length, array.getSize() + length);
            final int sliceLength = normalisedEnd - normalisedStart;

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange(store, normalisedStart, normalisedEnd), sliceLength);
        }

        @Specialization(guards = "isLongFixnum")
        public RubyArray sliceLongFixnum(RubyArray array, int start, int length) {
            final long[] store = (long[]) array.getStore();

            final int normalisedStart = array.normaliseIndex(start);
            final int normalisedEnd = Math.min(normalisedStart + length, array.getSize() + length);
            final int sliceLength = normalisedEnd - normalisedStart;

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOfRange(store, normalisedStart, normalisedEnd), sliceLength);
        }

    }

    @CoreMethod(names = "sort")
    public abstract static class SortNode extends ArrayCoreMethodNode {

        @Child protected DispatchHeadNode compareDispatchNode;

        public SortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = new DispatchHeadNode(context);
        }

        public SortNode(SortNode prev) {
            super(prev);
            compareDispatchNode = prev.compareDispatchNode;
        }

        @Specialization(guards = "isNull")
        public RubyArray sortNull(RubyArray array) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @ExplodeLoop
        @Specialization(guards = {"isIntegerFixnum", "isSmall"})
        public RubyArray sortVeryShortIntegerFixnum(VirtualFrame frame, RubyArray array) {
            final int[] store = (int[]) array.getStore();

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
                }
            }

            return array;
        }

        @Specialization(guards = "isIntegerFixnum")
        public RubyArray sortIntegerFixnum(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Object[] boxed = ArrayUtils.box((int[]) array.getStore());
            sort(frame, boxed);
            final int[] unboxed = ArrayUtils.unboxInteger(boxed, array.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.getSize());
        }

        @ExplodeLoop
        @Specialization(guards = {"isLongFixnum", "isSmall"})
        public RubyArray sortVeryShortLongFixnum(VirtualFrame frame, RubyArray array) {
            final long[] store = (long[]) array.getStore();

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
                }
            }

            return array;
        }

        @Specialization(guards = "isLongFixnum")
        public RubyArray sortLongFixnum(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Object[] boxed = ArrayUtils.box((long[]) array.getStore());
            sort(frame, boxed);
            final long[] unboxed = ArrayUtils.unboxLong(boxed, array.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.getSize());
        }

        @Specialization(guards = "isFloat")
        public RubyArray sortDouble(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Object[] boxed = ArrayUtils.box((double[]) array.getStore());
            sort(frame, boxed);
            final double[] unboxed = ArrayUtils.unboxDouble(boxed, array.getSize());
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.getSize());
        }

        @Specialization(guards = {"isObject", "isSmall"})
        public RubyArray sortVeryShortObject(VirtualFrame frame, RubyArray array) {
            final Object[] store = (Object[]) array.getStore();

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

            return array;
        }

        @Specialization(guards = "isObject")
        public RubyArray sortObject(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Object[] store = Arrays.copyOf((Object[]) array.getStore(), array.getSize());
            sort(frame, store);
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

        protected static boolean isSmall(RubyArray array) {
            return array.getSize() <= RubyArray.ARRAYS_SMALL;
        }

    }

    @CoreMethod(names = "to_a")
    public abstract static class ToANode extends CoreMethodNode {

        public ToANode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToANode(ToANode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray toA(RubyArray array) {
            return array;
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

        @Specialization(guards = {"isObject", "isOtherIntegerFixnum"})
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

        @Specialization(guards = {"isObject", "isOtherObject"})
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

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HashNode(HashNode prev) {
            super(prev);
        }

        @Specialization
        public long hashNumber(RubyArray array) {
            return array.hashCode();
        }

    }
}
