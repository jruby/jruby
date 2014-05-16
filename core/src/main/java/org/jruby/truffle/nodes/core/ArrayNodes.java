/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.SlowPath;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.call.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.BreakException;
import org.jruby.truffle.runtime.control.NextException;
import org.jruby.truffle.runtime.control.RedoException;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.core.array.RubyArray;
import org.jruby.truffle.runtime.core.range.IntegerFixnumRange;

import java.util.Arrays;
import java.util.Comparator;

@CoreClass(name = "Array")
public abstract class ArrayNodes {

    @CoreMethod(names = "+", minArgs = 1, maxArgs = 1)
    public abstract static class AddNode extends ArrayCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothIntegerFixnum", order = 1)
        public RubyArray addIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int combinedSize = a.size + b.size;
            final int[] combined = new int[combinedSize];
            System.arraycopy(a.store, 0, combined, 0, a.size);
            System.arraycopy(b.store, 0, combined, a.size, b.size);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothLongFixnum", order = 2)
        public RubyArray addLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int combinedSize = a.size + b.size;
            final long[] combined = new long[combinedSize];
            System.arraycopy(a.store, 0, combined, 0, a.size);
            System.arraycopy(b.store, 0, combined, a.size, b.size);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothFloat", order = 3)
        public RubyArray addFloat(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int combinedSize = a.size + b.size;
            final double[] combined = new double[combinedSize];
            System.arraycopy(a.store, 0, combined, 0, a.size);
            System.arraycopy(b.store, 0, combined, a.size, b.size);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(guards = "areBothObject", order = 4)
        public RubyArray addObject(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int combinedSize = a.size + b.size;
            final Object[] combined = new Object[combinedSize];
            System.arraycopy(a.store, 0, combined, 0, a.size);
            System.arraycopy(b.store, 0, combined, a.size, b.size);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

        @Specialization(order = 5)
        public RubyArray add(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int combinedSize = a.size + b.size;
            final Object[] combined = new Object[combinedSize];
            ArrayUtils.copy(a.store, combined, 0);
            ArrayUtils.copy(b.store, combined, a.size);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), combined, combinedSize);
        }

    }

    @CoreMethod(names = "-", minArgs = 1, maxArgs = 1)
    public abstract static class SubNode extends ArrayCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothIntegerFixnum", order = 1)
        public RubyArray subIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int[] as = (int[]) a.store;
            final int[] bs = (int[]) b.store;

            final int[] sub = new int[a.size];

            int i = 0;

            for (int n = 0; n < a.size; n++) {
                if (!ArrayUtils.contains(bs, as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization(guards = "areBothLongFixnum", order = 2)
        public RubyArray subLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final long[] as = (long[]) a.store;
            final long[] bs = (long[]) b.store;

            final long[] sub = new long[a.size];

            int i = 0;

            for (int n = 0; n < a.size; n++) {
                if (!ArrayUtils.contains(bs, as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

        @Specialization(guards = "areBothFloat", order = 3)
        public RubyArray subDouble(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final double[] as = (double[]) a.store;
            final double[] bs = (double[]) b.store;

            final double[] sub = new double[a.size];

            int i = 0;

            for (int n = 0; n < a.size; n++) {
                if (!ArrayUtils.contains(bs, as[n])) {
                    sub[i] = as[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), sub, i);
        }

    }

    @CoreMethod(names = "*", minArgs = 1, maxArgs = 1)
    public abstract static class MulNode extends ArrayCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyArray mulEmpty(RubyArray array, int count) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum", order = 2)
        public RubyArray mulIntegerFixnum(RubyArray array, int count) {
            final int[] store = (int[]) array.store;
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final int[] newStore = new int[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isLongFixnum", order = 3)
        public RubyArray mulLongFixnum(RubyArray array, int count) {
            notDesignedForCompilation();

            final long[] store = (long[]) array.store;
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final long[] newStore = new long[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isFloat", order = 4)
        public RubyArray mulFloat(RubyArray array, int count) {
            notDesignedForCompilation();

            final double[] store = (double[]) array.store;
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final double[] newStore = new double[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, newStoreLength);
        }

        @Specialization(guards = "isObject", order = 5)
        public RubyArray mulObject(RubyArray array, int count) {
            notDesignedForCompilation();

            final Object[] store = (Object[]) array.store;
            final int storeLength = store.length;
            final int newStoreLength = storeLength * count;
            final Object[] newStore = new Object[newStoreLength];

            for (int n = 0; n < count; n++) {
                System.arraycopy(store, 0, newStore, storeLength * n, storeLength);
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), newStore, newStoreLength);
        }

    }

    @CoreMethod(names = "|", minArgs = 1, maxArgs = 1)
    public abstract static class UnionNode extends ArrayCoreMethodNode {

        public UnionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UnionNode(UnionNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothIntegerFixnum", order = 1)
        public RubyArray orIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final int[] as = (int[]) a.store;
            final int[] bs = (int[]) b.store;

            final int[] or = Arrays.copyOf(as, a.size + b.size);

            int i = a.size;

            for (int n = 0; n < b.size; n++) {
                if (!ArrayUtils.contains(as, bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

        @Specialization(guards = "areBothLongFixnum", order = 2)
        public RubyArray orLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final long[] as = (long[]) a.store;
            final long[] bs = (long[]) b.store;

            final long[] or = Arrays.copyOf(as, a.size + b.size);

            int i = a.size;

            for (int n = 0; n < b.size; n++) {
                if (!ArrayUtils.contains(as, bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

        @Specialization(guards = "areBothFloat", order = 3)
        public RubyArray orDouble(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            final double[] as = (double[]) a.store;
            final double[] bs = (double[]) b.store;

            final double[] or = Arrays.copyOf(as, a.size + b.size);

            int i = a.size;

            for (int n = 0; n < b.size; n++) {
                if (!ArrayUtils.contains(as, bs[n])) {
                    or[i] = bs[n];
                    i++;
                }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), or, i);
        }

    }

    @CoreMethod(names = "==", minArgs = 1, maxArgs = 1)
    public abstract static class EqualNode extends ArrayCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothIntegerFixnum", order = 1)
        public boolean equalIntegerFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.size != b.size) {
                return false;
            }

            return Arrays.equals((int[]) a.store, (int[]) b.store);
        }

        @Specialization(guards = "areBothLongFixnum", order = 2)
        public boolean equalLongFixnum(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.size != b.size) {
                return false;
            }

            return Arrays.equals((long[]) a.store, (long[]) b.store);
        }

        @Specialization(guards = "areBothFloat", order = 3)
        public boolean equalFloat(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.size != b.size) {
                return false;
            }

            return Arrays.equals((float[]) a.store, (float[]) b.store);
        }

        @Specialization(guards = "areBothObject", order = 4)
        public boolean equalObject(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.size != b.size) {
                return false;
            }

            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

        @Specialization(order = 5)
        public boolean equal(RubyArray a, RubyArray b) {
            notDesignedForCompilation();

            if (a == b) {
                return true;
            }

            if (a.size != b.size) {
                return false;
            }

            CompilerDirectives.transferToInterpreter();
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = {"[]", "at"}, minArgs = 1, maxArgs = 2)
    public abstract static class IndexNode extends ArrayCoreMethodNode {

        public IndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IndexNode(IndexNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public NilPlaceholder getNull(RubyArray array, int index, UndefinedPlaceholder undefined) {
            return NilPlaceholder.INSTANCE;
        }

        @Specialization(guards = "isIntegerFixnum", rewriteOn=UnexpectedResultException.class, order = 2)
        public int getIntegerFixnumInBounds(RubyArray array, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
            } else {
                return ((int[]) array.store)[normalisedIndex];
            }
        }

        @Specialization(guards = "isIntegerFixnum", order = 3)
        public Object getIntegerFixnum(RubyArray array, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                return NilPlaceholder.INSTANCE;
            } else {
                return ((int[]) array.store)[normalisedIndex];
            }
        }

        @Specialization(guards = "isLongFixnum", rewriteOn=UnexpectedResultException.class, order = 4)
        public long getLongFixnumInBounds(RubyArray array, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
            } else {
                return ((long[]) array.store)[normalisedIndex];
            }
        }

        @Specialization(guards = "isLongFixnum", order = 5)
        public Object getLongFixnum(RubyArray array, int index, UndefinedPlaceholder undefined) {

            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                return NilPlaceholder.INSTANCE;
            } else {
                return ((long[]) array.store)[normalisedIndex];
            }
        }

        @Specialization(guards = "isFloat", rewriteOn=UnexpectedResultException.class, order = 6)
        public double getFloatInBounds(RubyArray array, int index, UndefinedPlaceholder undefined) throws UnexpectedResultException {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
            } else {
                return ((double[]) array.store)[normalisedIndex];
            }
        }

        @Specialization(guards = "isIntegerFixnum", order = 7)
        public Object getFloat(RubyArray array, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                return NilPlaceholder.INSTANCE;
            } else {
                return ((int[]) array.store)[normalisedIndex];
            }
        }

        @Specialization(guards = "isObject", order = 8)
        public Object getObject(RubyArray array, int index, UndefinedPlaceholder undefined) {
            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0 || normalisedIndex >= array.size) {
                return NilPlaceholder.INSTANCE;
            } else {
                return ((Object[]) array.store)[normalisedIndex];
            }
        }

    }

    @CoreMethod(names = "[]=", minArgs = 2, maxArgs = 3)
    public abstract static class IndexSetNode extends ArrayCoreMethodNode {

        private final BranchProfile tooSmallBranch = new BranchProfile();
        private final BranchProfile pastEndBranch = new BranchProfile();
        private final BranchProfile appendBranch = new BranchProfile();
        private final BranchProfile beyondBranch = new BranchProfile();
        private final BranchProfile reallocateBranch = new BranchProfile();

        public IndexSetNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IndexSetNode(IndexSetNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum", order = 1)
        public int setIntegerFixnum(RubyArray array, int index, int value) {
            final int normalisedIndex = array.normaliseIndex(index);
            int[] store = (int[]) array.store;

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (normalisedIndex >= array.size) {
                pastEndBranch.enter();

                if (normalisedIndex == array.size) {
                    appendBranch.enter();

                    if (normalisedIndex >= store.length) {
                        reallocateBranch.enter();
                        array.store = store = Arrays.copyOf(store, ArrayUtils.capacity(store.length));
                    }

                    store[normalisedIndex] = value;
                    array.size++;
                } else if (normalisedIndex > array.size) {
                    beyondBranch.enter();
                    throw new UnsupportedOperationException();
                }
            } else {
                store[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isLongFixnum", order = 2)
        public long setLongFixnum(RubyArray array, int index, long value) {
            notDesignedForCompilation();

            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (normalisedIndex >= array.size) {
                pastEndBranch.enter();
                throw new UnsupportedOperationException();
            } else {
                ((long[]) array.store)[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isFloat", order = 3)
        public double setFloat(RubyArray array, int index, double value) {
            notDesignedForCompilation();

            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (normalisedIndex >= array.size) {
                pastEndBranch.enter();
                throw new UnsupportedOperationException();
            } else {
                ((double[]) array.store)[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isObject", order = 4)
        public Object setObject(RubyArray array, int index, Object value) {
            notDesignedForCompilation();

            int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (normalisedIndex >= array.size) {
                pastEndBranch.enter();
                throw new UnsupportedOperationException();
            } else {
                ((Object[]) array.store)[normalisedIndex] = value;
            }

            return value;
        }

        @Specialization(guards = "isIntegerFixnum", order = 5)
        public RubyArray setIntegerFixnumRange(RubyArray array, IntegerFixnumRange range, RubyArray other) {
            // TODO(CS): why can't this be a guard?
            if (other.store instanceof int[]) {
                if (range.doesExcludeEnd()) {
                    CompilerDirectives.transferToInterpreter();
                    throw new UnsupportedOperationException();
                } else {
                    int normalisedBegin = array.normaliseIndex(range.getBegin());
                    int normalisedEnd = array.normaliseIndex(range.getEnd());

                    if (normalisedBegin == 0 && normalisedEnd == array.size - 1) {
                        array.store = Arrays.copyOf((int[]) other.store, other.size);
                        array.size = other.size;
                    } else {
                        CompilerDirectives.transferToInterpreter();
                        throw new UnsupportedOperationException();
                    }
                }
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            }

            return other;
        }

    }

    @CoreMethod(names = "all?", needsBlock = true, maxArgs = 0)
    public abstract static class AllNode extends YieldingArrayCoreMethodNode {

        public AllNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AllNode(AllNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public boolean allNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return true;
        }

        @Specialization(guards = "isIntegerFixnum", order = 2)
        public boolean allIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n : (int[]) array.store) {
                if (!yieldBoolean(frame, block, n)) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "isLongFixnum", order = 3)
        public boolean allLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (long n : (long[]) array.store) {
                if (!yieldBoolean(frame, block, n)) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "isFloat", order = 4)
        public boolean allFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (double n : (double[]) array.store) {
                if (!yieldBoolean(frame, block, n)) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = "isObject", order = 5)
        public boolean allObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (Object n : (Object[]) array.store) {
                if (!yieldBoolean(frame, block, n)) {
                    return false;
                }
            }

            return true;
        }

    }

    @CoreMethod(names = "any?", needsBlock = true, maxArgs = 0)
    public abstract static class AnyNode extends YieldingArrayCoreMethodNode {

        public AnyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AnyNode(AnyNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public boolean anyNull(VirtualFrame frame, RubyArray array, RubyProc block) {
            return false;
        }

        @Specialization(guards = "isIntegerFixnum", order = 2)
        public boolean allIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (int n : (int[]) array.store) {
                if (yieldBoolean(frame, block, n)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isLongFixnum", order = 3)
        public boolean anyLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (long n : (long[]) array.store) {
                if (yieldBoolean(frame, block, n)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isFloat", order = 4)
        public boolean anyFloat(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (double n : (double[]) array.store) {
                if (yieldBoolean(frame, block, n)) {
                    return true;
                }
            }

            return false;
        }

        @Specialization(guards = "isObject", order = 5)
        public boolean anyObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            for (Object n : (Object[]) array.store) {
                if (yieldBoolean(frame, block, n)) {
                    return true;
                }
            }

            return false;
        }

    }

    @CoreMethod(names = "clear", maxArgs = 0)
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

            array.size = 0;
            return array;
        }

    }

    @CoreMethod(names = "compact", maxArgs = 0)
    public abstract static class CompactNode extends ArrayCoreMethodNode {

        public CompactNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompactNode(CompactNode prev) {
            super(prev);
        }

        @Specialization(guards = "!isObject", order = 1)
        public RubyArray compatNotObjects(RubyArray array) {
            return array;
        }

        @Specialization(guards = "isObject", order = 2)
        public RubyArray compatObjects(RubyArray array) {
            notDesignedForCompilation();

            final Object[] compacted = new Object[array.size];
            int compactedSize = 0;

            for (Object object : (Object[]) array.store) {
                if (object != NilPlaceholder.INSTANCE) {
                    compacted[compactedSize] = object;
                    compactedSize++;
                }
            }

            array.store = compacted;
            array.size = compactedSize;

            return array;
        }

    }

    @CoreMethod(names = "concat", minArgs = 1, maxArgs = 1)
    public abstract static class ConcatNode extends ArrayCoreMethodNode {

        public ConcatNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ConcatNode(ConcatNode prev) {
            super(prev);
        }

        @Specialization(guards = "areBothNull", order = 1)
        public RubyArray concatNull(RubyArray array, RubyArray other) {
            return array;
        }

        @Specialization(guards = "areBothIntegerFixnum", order = 2)
        public RubyArray concatIntegerFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.store = Arrays.copyOf((int[]) array.store, array.size + other.size);
            System.arraycopy(other.store, 0, array.store, array.size, other.size);
            return array;
        }

        @Specialization(guards = "areBothLongFixnum", order = 3)
        public RubyArray concatLongFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.store = Arrays.copyOf((long[]) array.store, array.size + other.size);
            System.arraycopy(other.store, 0, array.store, array.size, other.size);
            return array;
        }

        @Specialization(guards = "areBothFloat", order = 4)
        public RubyArray concatDouble(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.store = Arrays.copyOf((double[]) array.store, array.size + other.size);
            System.arraycopy(other.store, 0, array.store, array.size, other.size);
            return array;
        }

        @Specialization(guards = "areBothObject", order = 5)
        public RubyArray concatObject(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            array.store = Arrays.copyOf((Object[]) array.store, array.size + other.size);
            System.arraycopy(other.store, 0, array.store, array.size, other.size);
            return array;
        }

        @Specialization(order = 6)
        public RubyArray concat(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            // TODO(CS): is there already space in array?
            // TODO(CS): if array is Object[], use Arrays.copyOf
            final Object[] newStore = new Object[array.size + other.size];
            ArrayUtils.copy(array.store, newStore, 0);
            ArrayUtils.copy(other.store, newStore, array.size);
            array.store = newStore;
            return array;
        }

    }

    @CoreMethod(names = "delete", minArgs = 1, maxArgs = 1)
    public abstract static class DeleteNode extends CoreMethodNode {

        public DeleteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteNode(DeleteNode prev) {
            super(prev);
        }

        @Specialization
        public Object delete(RubyArray array, Object value) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "delete_at", minArgs = 1, maxArgs = 1)
    public abstract static class DeleteAtNode extends ArrayCoreMethodNode {

        private static final BranchProfile tooSmallBranch = new BranchProfile();
        private static final BranchProfile beyondEndBranch = new BranchProfile();

        public DeleteAtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DeleteAtNode(DeleteAtNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum", rewriteOn = UnexpectedResultException.class, order = 1)
        public int deleteAtIntegerFixnumInBounds(RubyArray array, int index) throws UnexpectedResultException {
            final int normalisedIndex = array.normaliseIndex(index);

            if (normalisedIndex < 0) {
                throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
            } else if (normalisedIndex >= array.size) {
                throw new UnexpectedResultException(NilPlaceholder.INSTANCE);
            } else {
                final int[] store = (int[]) array.store;
                final int value = store[normalisedIndex];
                System.arraycopy(store, normalisedIndex + 1, store, normalisedIndex, array.size - normalisedIndex - 1);
                array.size -= 1;
                return value;
            }
        }

        @Specialization(guards = "isIntegerFixnum", order = 2)
        public Object deleteAtIntegerFixnum(RubyArray array, int index) {
            notDesignedForCompilation();

            int normalisedIndex = index;

            if (normalisedIndex < 0) {
                normalisedIndex = array.size + index;
            }

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else if (normalisedIndex >= array.size) {
                beyondEndBranch.enter();
                throw new UnsupportedOperationException();
            } else {
                final int[] store = (int[]) array.store;
                final int value = store[normalisedIndex];
                System.arraycopy(store, normalisedIndex + 1, store, normalisedIndex, array.size - normalisedIndex - 1);
                array.size -= 1;
                return value;
            }
        }

    }

    @CoreMethod(names = "dup", maxArgs = 0)
    public abstract static class DupNode extends ArrayCoreMethodNode {

        public DupNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DupNode(DupNode prev) {
            super(prev);
        }

        @Specialization(guards = "isNull", order = 1)
        public Object dupNull(RubyArray array) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum", order = 2)
        public Object dupIntegerFixnum(RubyArray array) {
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((int[]) array.store, array.size), array.size);
        }

        @Specialization(guards = "isLongFixnum", order = 3)
        public Object dupLongFixnum(RubyArray array) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((long[]) array.store, array.size), array.size);
        }

        @Specialization(guards = "isFloat", order = 4)
        public Object dupFloat(RubyArray array) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((double[]) array.store, array.size), array.size);
        }

        @Specialization(guards = "isObject", order = 5)
        public Object dupObject(RubyArray array) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), Arrays.copyOf((Object[]) array.store, array.size), array.size);
        }

    }

    @CoreMethod(names = "each", needsBlock = true, maxArgs = 0)
    public abstract static class EachNode extends YieldingArrayCoreMethodNode {

        private final BranchProfile breakProfile = new BranchProfile();
        private final BranchProfile nextProfile = new BranchProfile();
        private final BranchProfile redoProfile = new BranchProfile();

        public EachNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachNode(EachNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum", order = 1)
        public Object eachIntegerFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final int[] store = (int[]) array.store;

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.size; n++) {
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
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isLongFixnum", order = 2)
        public Object eachLongFixnum(VirtualFrame frame, RubyArray array, RubyProc block) {
            final long[] store = (long[]) array.store;

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.size; n++) {
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
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return array;
        }

        @Specialization(guards = "isObject", order = 3)
        public Object eachObject(VirtualFrame frame, RubyArray array, RubyProc block) {
            final Object[] store = (Object[]) array.store;

            int count = 0;

            try {
                outer:
                for (int n = 0; n < array.size; n++) {
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
                    ((RubyRootNode) getRootNode()).reportLoopCountThroughBlocks(count);
                }
            }

            return array;
        }

        @Specialization(order = 4)
        public Object each(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "each_with_index", needsBlock = true, maxArgs = 0)
    public abstract static class EachWithIndexNode extends YieldingCoreMethodNode {

        public EachWithIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EachWithIndexNode(EachWithIndexNode prev) {
            super(prev);
        }

        @Specialization
        public NilPlaceholder eachWithIndex(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "empty?", maxArgs = 0)
    public abstract static class EmptyNode extends ArrayCoreMethodNode {

        public EmptyNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EmptyNode(EmptyNode prev) {
            super(prev);
        }

        @Specialization
        public boolean isEmpty(RubyArray array) {
            return array.size == 0;
        }

    }

    @CoreMethod(names = "find", needsBlock = true, maxArgs = 0)
    public abstract static class FindNode extends YieldingCoreMethodNode {

        public FindNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FindNode(FindNode prev) {
            super(prev);
        }

        @Specialization
        public Object find(VirtualFrame frame, RubyArray array, RubyProc block) {
            notDesignedForCompilation();

            throw new UnsupportedOperationException();
        }
    }

    @CoreMethod(names = "first", maxArgs = 0)
    public abstract static class FirstNode extends ArrayCoreMethodNode {

        public FirstNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FirstNode(FirstNode prev) {
            super(prev);
        }

        @Specialization(guards = "isIntegerFixnum")
        public int first(RubyArray array) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "flatten", maxArgs = 0)
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

    @CoreMethod(names = "include?", minArgs = 1, maxArgs = 1)
    public abstract static class IncludeNode extends ArrayCoreMethodNode {

        public IncludeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public IncludeNode(IncludeNode prev) {
            super(prev);
        }

        @Specialization
        public boolean include(RubyArray array, Object value) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "initialize", needsBlock = true, minArgs = 1, maxArgs = 2)
    public abstract static class InitializeNode extends CoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InitializeNode(InitializeNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, UndefinedPlaceholder defaultValue) {
            return initialize(array, size, NilPlaceholder.INSTANCE);
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, int defaultValue) {
            notDesignedForCompilation();

            final int[] store = new int[size];
            Arrays.fill(store, defaultValue);
            array.store = store;
            array.size = size;
            return array;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, long defaultValue) {
            notDesignedForCompilation();

            final long[] store = new long[size];
            Arrays.fill(store, defaultValue);
            array.store = store;
            array.size = size;
            return array;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, double defaultValue) {
            notDesignedForCompilation();

            final double[] store = new double[size];
            Arrays.fill(store, defaultValue);
            array.store = store;
            array.size = size;
            return array;
        }

        @Specialization
        public RubyArray initialize(RubyArray array, int size, Object defaultValue) {
            notDesignedForCompilation();

            final Object[] store = new Object[size];
            Arrays.fill(store, defaultValue);
            array.store = store;
            array.size = size;
            return array;
        }

    }

    @CoreMethod(names = {"inject", "reduce"}, needsBlock = true, minArgs = 0, maxArgs = 1)
    public abstract static class InjectNode extends YieldingCoreMethodNode {

        public InjectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InjectNode(InjectNode prev) {
            super(prev);
        }

        @Specialization
        public Object inject(VirtualFrame frame, RubyArray array, @SuppressWarnings("unused") UndefinedPlaceholder initial, RubyProc block) {
            notDesignedForCompilation();

            throw new UnsupportedOperationException();
        }

        @Specialization
        public Object inject(VirtualFrame frame, RubyArray array, Object initial, RubyProc block) {
            notDesignedForCompilation();

            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "insert", minArgs = 2, maxArgs = 2)
    public abstract static class InsertNode extends ArrayCoreMethodNode {

        private static final BranchProfile tooSmallBranch = new BranchProfile();

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
            Arrays.fill(store, NilPlaceholder.INSTANCE);
            store[index] = value;
            array.size++;
            return array;
        }

        @Specialization(guards = "isIntegerFixnum")
        public Object insert(RubyArray array, int index, int value) {
            final int normalisedIndex = array.normaliseIndex(index);
            final int[] store = (int[]) array.store;

            if (normalisedIndex < 0) {
                tooSmallBranch.enter();
                throw new UnsupportedOperationException();
            } else if (array.size > store.length + 1) {
                CompilerDirectives.transferToInterpreter();
                throw new UnsupportedOperationException();
            } else {
                System.arraycopy(store, normalisedIndex, store, normalisedIndex + 1, array.size - normalisedIndex);
                store[normalisedIndex] = value;
                array.size++;
            }

            return array;
        }

    }

    @CoreMethod(names = {"inspect", "to_s"}, maxArgs = 0)
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString inspect(RubyArray array) {
            return getContext().makeString(createInspectString(array));
        }

        private String createInspectString(RubyArray array) {
            return array.inspect();
        }

    }

    @CoreMethod(names = "join", minArgs = 1, maxArgs = 1)
    public abstract static class JoinNode extends ArrayCoreMethodNode {

        public JoinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public JoinNode(JoinNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString join(RubyArray array, RubyString separator) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "last", maxArgs = 0)
    public abstract static class LastNode extends ArrayCoreMethodNode {

        public LastNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LastNode(LastNode prev) {
            super(prev);
        }

        @Specialization
        public Object last(RubyArray array) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = {"map", "collect"}, needsBlock = true, maxArgs = 0)
    public abstract static class MapNode extends YieldingCoreMethodNode {

        public MapNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MapNode(MapNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray map(VirtualFrame frame, RubyArray array, RubyProc block) {
            throw new UnsupportedOperationException();
        }
    }

    @CoreMethod(names = {"map!", "collect!"}, needsBlock = true, maxArgs = 0)
    public abstract static class MapInPlaceNode extends YieldingCoreMethodNode {

        public MapInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MapInPlaceNode(MapInPlaceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray mapInPlace(VirtualFrame frame, RubyArray array, RubyProc block) {
            throw new UnsupportedOperationException();
        }
    }

    @CoreMethod(names = "min", maxArgs = 0)
    public abstract static class MinNode extends ArrayCoreMethodNode {

        @Child protected DispatchHeadNode compareDispatchNode;

        public MinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = new DispatchHeadNode(context, "<=>", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public MinNode(MinNode prev) {
            super(prev);
            compareDispatchNode = prev.compareDispatchNode;
        }

        @Specialization
        public int minFixnum(VirtualFrame frame, RubyArray array) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "pack", minArgs = 1, maxArgs = 1)
    public abstract static class PackNode extends ArrayCoreMethodNode {

        public PackNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PackNode(PackNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString pack(RubyArray array, RubyString format) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "pop", maxArgs = 0)
    public abstract static class PopNode extends ArrayCoreMethodNode {

        public PopNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PopNode(PopNode prev) {
            super(prev);
        }

        @Specialization
        public Object pop(RubyArray array) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "product", isSplatted = true)
    public abstract static class ProductNode extends ArrayCoreMethodNode {

        public ProductNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ProductNode(ProductNode prev) {
            super(prev);
        }

        @Specialization
        @SlowPath
        public Object product(RubyArray array, Object... args) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = {"push", "<<"}, isSplatted = true)
    public abstract static class PushNode extends CoreMethodNode {

        public PushNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PushNode(PushNode prev) {
            super(prev);
        }

        @SlowPath
        @Specialization
        public RubyArray push(RubyArray array, Object... args) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "reject!", needsBlock = true, maxArgs = 0)
    public abstract static class RejectInPlaceNode extends YieldingCoreMethodNode {

        public RejectInPlaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RejectInPlaceNode(RejectInPlaceNode prev) {
            super(prev);
        }

        @Specialization
        public Object rejectInPlace(VirtualFrame frame, RubyArray array, RubyProc block) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "replace", minArgs = 1, maxArgs = 1)
    public abstract static class ReplaceNode extends ArrayCoreMethodNode {

        public ReplaceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ReplaceNode(ReplaceNode prev) {
            super(prev);
        }

        @Specialization(guards = "isOtherNull", order = 1)
        public RubyArray replace(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.size = 0;
            return array;
        }

        @Specialization(guards = "isOtherIntegerFixnum", order = 2)
        public RubyArray replaceIntegerFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.store = Arrays.copyOf((int[]) other.store, other.size);
            array.size = other.size;
            return array;
        }

        @Specialization(guards = "isOtherLongFixnum", order = 3)
        public RubyArray replaceLongFixnum(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.store = Arrays.copyOf((long[]) other.store, other.size);
            array.size = other.size;
            return array;
        }

        @Specialization(guards = "isOtherFloat", order = 4)
        public RubyArray replaceFloat(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.store = Arrays.copyOf((double[]) other.store, other.size);
            array.size = other.size;
            return array;
        }

        @Specialization(guards = "isOtherObject", order = 5)
        public RubyArray replaceObject(RubyArray array, RubyArray other) {
            notDesignedForCompilation();

            array.store = Arrays.copyOf((Object[]) other.store, other.size);
            array.size = other.size;
            return array;
        }

    }

    @CoreMethod(names = "select", needsBlock = true, maxArgs = 0)
    public abstract static class SelectNode extends YieldingCoreMethodNode {

        public SelectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SelectNode(SelectNode prev) {
            super(prev);
        }

        @Specialization
        public Object select(VirtualFrame frame, RubyArray array, RubyProc block) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "shift", maxArgs = 0)
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

    @CoreMethod(names = {"size", "length"}, maxArgs = 0)
    public abstract static class SizeNode extends ArrayCoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size(RubyArray array) {
            return array.size;
        }

    }

    @CoreMethod(names = "slice", minArgs = 2, maxArgs = 2)
    public abstract static class SliceNode extends ArrayCoreMethodNode {

        public SliceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SliceNode(SliceNode prev) {
            super(prev);
        }

        @Specialization
        public RubyArray slice(RubyArray array, int start, int length) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "sort", maxArgs = 0)
    public abstract static class SortNode extends ArrayCoreMethodNode {

        @Child protected DispatchHeadNode compareDispatchNode;

        public SortNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            compareDispatchNode = new DispatchHeadNode(context, "<=>", false, DispatchHeadNode.MissingBehavior.CALL_METHOD_MISSING);
        }

        public SortNode(SortNode prev) {
            super(prev);
            compareDispatchNode = prev.compareDispatchNode;
        }

        @Specialization(guards = "isNull", order = 1)
        public RubyArray sortNull(RubyArray array) {
            notDesignedForCompilation();

            return new RubyArray(getContext().getCoreLibrary().getArrayClass());
        }

        @Specialization(guards = "isIntegerFixnum", order = 2)
        public RubyArray sortIntegerFixnum(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Integer[] boxed = ArrayUtils.box((int[]) array.store);
            sort(frame, boxed);
            final int[] unboxed = ArrayUtils.unbox(boxed);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.size);
        }

        @Specialization(guards = "isLongFixnum", order = 3)
        public RubyArray sortLongFixnum(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Long[] boxed = ArrayUtils.box((long[]) array.store);
            sort(frame, boxed);
            final long[] unboxed = ArrayUtils.unbox(boxed);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.size);
        }

        @Specialization(guards = "isFloat", order = 4)
        public RubyArray sortDouble(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Double[] boxed = ArrayUtils.box((double[]) array.store);
            sort(frame, boxed);
            final double[] unboxed = ArrayUtils.unbox(boxed);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), unboxed, array.size);
        }

        @Specialization(guards = "isObject", order = 5)
        public RubyArray sortObject(VirtualFrame frame, RubyArray array) {
            notDesignedForCompilation();

            final Object[] store = Arrays.copyOf((Object[]) array.store, array.size);
            sort(frame, store);
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, array.size);
        }

        private <T> void sort(VirtualFrame frame, T[] objects) {
            final VirtualFrame finalFrame = frame;

            Arrays.sort(objects, new Comparator<Object>() {

                @Override
                public int compare(Object a, Object b) {
                    // TODO(CS): node for this cast
                    return (int) compareDispatchNode.dispatch(finalFrame, a, null, b);
                }

            });
        }

    }

    @CoreMethod(names = "to_a", maxArgs = 0)
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

    @CoreMethod(names = "unshift", isSplatted = true)
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

    @CoreMethod(names = "zip", isSplatted = true)
    public abstract static class ZipNode extends CoreMethodNode {

        public ZipNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ZipNode(ZipNode prev) {
            super(prev);
        }

        @SlowPath
        @Specialization
        public RubyArray zip(RubyArray array, Object... args) {
            throw new UnsupportedOperationException();
        }

    }

    @CoreMethod(names = "hash", maxArgs = 0)
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
