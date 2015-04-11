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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyString;

import java.math.BigDecimal;
import java.math.BigInteger;

@CoreClass(name = "Bignum")
public abstract class BignumNodes {

    public static abstract class BignumCoreMethodNode extends CoreMethodNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public BignumCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        public Object fixnumOrBignum(BigInteger value) {
            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BignumCoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object neg(RubyBignum value) {
            return fixnumOrBignum(value.bigIntegerValue().negate());
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object add(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().add(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object add(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().add(BigInteger.valueOf(b)));
        }

        @Specialization
        public double add(RubyBignum a, double b) {
            return a.bigIntegerValue().doubleValue() + b;
        }

        @Specialization
        public Object add(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().add(b.bigIntegerValue()));
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sub(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().subtract(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object sub(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().subtract(BigInteger.valueOf(b)));
        }

        @Specialization
        public double sub(RubyBignum a, double b) {
            return a.bigIntegerValue().doubleValue() - b;
        }

        @Specialization
        public Object sub(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().subtract(b.bigIntegerValue()));
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends BignumCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object mul(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().multiply(BigInteger.valueOf(b)));
        }

        @TruffleBoundary
        @Specialization
        public Object mul(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().multiply(BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(RubyBignum a, double b) {
            return a.bigIntegerValue().doubleValue() * b;
        }

        @TruffleBoundary
        @Specialization
        public Object mul(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().multiply(b.bigIntegerValue()));
        }

    }

    @CoreMethod(names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends BignumCoreMethodNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object div(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().divide(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object div(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().divide(BigInteger.valueOf(b)));
        }

        @Specialization
        public double div(RubyBignum a, double b) {
            return a.bigIntegerValue().doubleValue() / b;
        }

        @Specialization
        public Object div(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().divide(b.bigIntegerValue()));
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends BignumCoreMethodNode {

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object mod(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().mod(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object mod(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().mod(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object mod(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().mod(b.bigIntegerValue()));
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean less(RubyBignum a, int b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, long b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, double b) {
            return Double.compare(a.bigIntegerValue().doubleValue(), b) < 0;
        }

        @Specialization
        public boolean less(RubyBignum a, RubyBignum b) {
            return a.bigIntegerValue().compareTo(b.bigIntegerValue()) < 0;
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object lessCoerced(VirtualFrame frame, RubyBignum a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :<, b", "b", b);
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, int b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, long b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, double b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf((long) b)) <= 0;
        }

        @Specialization
        public boolean lessEqual(RubyBignum a, RubyBignum b) {
            return a.bigIntegerValue().compareTo(b.bigIntegerValue()) <= 0;
        }
    }

    @CoreMethod(names = {"==", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean equal(RubyBignum a, int b) {
            return a.bigIntegerValue().equals(BigInteger.valueOf(b));
        }

        @Specialization
        public boolean equal(RubyBignum a, long b) {
            return a.bigIntegerValue().equals(BigInteger.valueOf(b));
        }

        @Specialization
        public boolean equal(RubyBignum a, double b) {
            return a.bigIntegerValue().doubleValue() == b;
        }

        @Specialization
        public boolean equal(RubyBignum a, RubyBignum b) {
            return a.bigIntegerValue().equals(b.bigIntegerValue());
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        private final ConditionProfile negativeInfinityProfile = ConditionProfile.createBinaryProfile();

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int compare(RubyBignum a, int b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b));
        }

        @Specialization
        public int compare(RubyBignum a, long b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b));
        }

        @Specialization
        public int compare(RubyBignum a, double b) {
            if (negativeInfinityProfile.profile(Double.isInfinite(b) && b < 0)) {
                return 1;
            } else {
                return Double.compare(a.bigIntegerValue().doubleValue(), b);
            }
        }

        @Specialization
        public int compare(RubyBignum a, RubyBignum b) {
            return a.bigIntegerValue().compareTo(b.bigIntegerValue());
        }
    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, int b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, long b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, double b) {
            return Double.compare(a.bigIntegerValue().doubleValue(), b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(RubyBignum a, RubyBignum b) {
            return a.bigIntegerValue().compareTo(b.bigIntegerValue()) >= 0;
        }
    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greater(RubyBignum a, int b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, long b) {
            return a.bigIntegerValue().compareTo(BigInteger.valueOf(b)) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, double b) {
            return Double.compare(a.bigIntegerValue().doubleValue(), b) > 0;
        }

        @Specialization
        public boolean greater(RubyBignum a, RubyBignum b) {
            return a.bigIntegerValue().compareTo(b.bigIntegerValue()) > 0;
        }
    }

    @CoreMethod(names = "&", required = 1)
    public abstract static class BitAndNode extends BignumCoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitAnd(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().and(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object bitAnd(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().and(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object bitAnd(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().and(b.bigIntegerValue()));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitOr(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().or(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object bitOr(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().or(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object bitOr(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().or(a.bigIntegerValue()));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitXOr(RubyBignum a, int b) {
            return fixnumOrBignum(a.bigIntegerValue().xor(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object bitXOr(RubyBignum a, long b) {
            return fixnumOrBignum(a.bigIntegerValue().xor(BigInteger.valueOf(b)));
        }

        @Specialization
        public Object bitXOr(RubyBignum a, RubyBignum b) {
            return fixnumOrBignum(a.bigIntegerValue().xor(b.bigIntegerValue()));
        }
    }

    @CoreMethod(names = "<<", required = 1)
    public abstract static class LeftShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object leftShift(RubyBignum a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(a.bigIntegerValue().shiftLeft(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(a.bigIntegerValue().shiftRight(-b));
            }
        }

    }

    @CoreMethod(names = ">>", required = 1)
    public abstract static class RightShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object leftShift(RubyBignum a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(a.bigIntegerValue().shiftRight(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(a.bigIntegerValue().shiftLeft(-b));
            }
        }

    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends BignumCoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object abs(RubyBignum value) {
            return fixnumOrBignum(value.bigIntegerValue().abs());
        }

    }

    @CoreMethod(names = "bit_length")
    public abstract static class BitLengthNode extends CoreMethodNode {

        public BitLengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int bitLength(RubyBignum value) {
            return value.bigIntegerValue().bitLength();
        }

    }

    @CoreMethod(names = "coerce", required = 1)
    public abstract static class CoerceNode extends CoreMethodNode {

        public CoerceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyArray coerce(RubyBignum a, int b) {
            notDesignedForCompilation();

            // TODO (eregon, 16 Feb. 2015): This is NOT spec, but let's try to see if we can make it work.
            // b is converted to a Bignum here in other implementations.
            Object[] store = new Object[] { b, a };
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, store.length);
        }

        @Specialization
        public RubyArray coerce(RubyBignum a, long b) {
            notDesignedForCompilation();

            // TODO (eregon, 16 Feb. 2015): This is NOT spec, but let's try to see if we can make it work.
            // b is converted to a Bignum here in other implementations.
            Object[] store = new Object[] { b, a };
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, store.length);
        }

        @Specialization
        public RubyArray coerce(RubyBignum a, RubyBignum b) {
            notDesignedForCompilation();

            Object[] store = new Object[] { b, a };
            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), store, store.length);
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodNode {

        @Child private GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context, sourceSection);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(RubyBignum a, RubyBignum b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "even?")
    public abstract static class EvenNode extends BignumCoreMethodNode {

        public EvenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public boolean even(RubyBignum value) {
            return value.bigIntegerValue().getLowestSetBit() != 0;
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(RubyBignum self) {
            return self.bigIntegerValue().hashCode();
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(RubyBignum value) {
            return (value.bigIntegerValue().bitLength() + 7) / 8;
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double toF(RubyBignum value) {
            return value.bigIntegerValue().doubleValue();
        }

    }

    @CoreMethod(names = {"to_s", "inspect"}, optional = 1)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public RubyString toS(RubyBignum value, UndefinedPlaceholder undefined) {
            return getContext().makeString(value.bigIntegerValue().toString());
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyBignum value, int base) {
            if (base < 2 || base > 36) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorInvalidRadix(base, this));
            }

            return getContext().makeString(value.bigIntegerValue().toString(base));
        }

    }

}
