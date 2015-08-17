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
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;

import java.math.BigInteger;

@CoreClass(name = "Bignum")
public abstract class BignumNodes {

    public static final BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    public static final BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);

    @Layout
    public interface BignumLayout extends BasicObjectNodes.BasicObjectLayout {

        DynamicObjectFactory createBignumShape(DynamicObject logicalClass, DynamicObject metaClass);

        DynamicObject createBignum(DynamicObjectFactory factory, BigInteger value);

        boolean isBignum(DynamicObject object);

        BigInteger getValue(DynamicObject object);

    }

    public static final BignumLayout BIGNUM_LAYOUT = BignumLayoutImpl.INSTANCE;

    public static DynamicObject createRubyBignum(DynamicObject rubyClass, BigInteger value) {
        assert value.compareTo(LONG_MIN) < 0 || value.compareTo(LONG_MAX) > 0 : String.format("%s not in Bignum range", value);
        return BIGNUM_LAYOUT.createBignum(ClassNodes.CLASS_LAYOUT.getInstanceFactory(rubyClass), value);
    }

    public static BigInteger getBigIntegerValue(DynamicObject bignum) {
        return BIGNUM_LAYOUT.getValue(bignum);
    }

    public static abstract class BignumCoreMethodNode extends CoreMethodArrayArgumentsNode {

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
        public Object neg(DynamicObject value) {
            return fixnumOrBignum(getBigIntegerValue(value).negate());
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object add(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).add(BigInteger.valueOf(b)));
        }

        @Specialization
        public double add(DynamicObject a, double b) {
            return getBigIntegerValue(a).doubleValue() + b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object add(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).add(getBigIntegerValue(b)));
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sub(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization
        public double sub(DynamicObject a, double b) {
            return getBigIntegerValue(a).doubleValue() - b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object sub(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).subtract(getBigIntegerValue(b)));
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends BignumCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object mul(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(DynamicObject a, double b) {
            return getBigIntegerValue(a).doubleValue() * b;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyBignum(b)")
        public Object mul(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).multiply(getBigIntegerValue(b)));
        }

    }

    @CoreMethod(names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends BignumCoreMethodNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object div(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).divide(BigInteger.valueOf(b)));
        }

        @Specialization
        public double div(DynamicObject a, double b) {
            return getBigIntegerValue(a).doubleValue() / b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object div(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).divide(getBigIntegerValue(b)));
        }

    }

    @CoreMethod(names = {"%", "modulo"}, required = 1)
    public abstract static class ModNode extends BignumCoreMethodNode {

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object mod(DynamicObject a, long b) {
            if (b == 0) {
                throw new ArithmeticException("divide by zero");
            } else if (b < 0) {
                final BigInteger bigint = BigInteger.valueOf(b);
                final BigInteger mod = getBigIntegerValue(a).mod(bigint.negate());
                return fixnumOrBignum(mod.add(bigint));
            }
            return fixnumOrBignum(getBigIntegerValue(a).mod(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object mod(DynamicObject a, DynamicObject b) {
            final BigInteger bigint = getBigIntegerValue(b);
            final int compare = bigint.compareTo(BigInteger.ZERO);
            if (compare == 0) {
                throw new ArithmeticException("divide by zero");
            } else if (compare < 0) {
                final BigInteger mod = getBigIntegerValue(a).mod(bigint.negate());
                return fixnumOrBignum(mod.add(bigint));
            }
            return fixnumOrBignum(getBigIntegerValue(a).mod(getBigIntegerValue(b)));
        }

        @Specialization(guards = {"!isInteger(b)", "!isLong(b)", "!isRubyBignum(b)"})
        public Object mod(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby(frame, "redo_coerced :%, other", "other", b);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodArrayArgumentsNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean less(DynamicObject a, long b) {
            return getBigIntegerValue(a).compareTo(BigInteger.valueOf(b)) < 0;
        }

        @Specialization
        public boolean less(DynamicObject a, double b) {
            return Double.compare(getBigIntegerValue(a).doubleValue(), b) < 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean less(DynamicObject a, DynamicObject b) {
            return getBigIntegerValue(a).compareTo(getBigIntegerValue(b)) < 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object lessCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby(frame, "b, a = math_coerce other, :compare_error; a < b", "other", b);
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodArrayArgumentsNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lessEqual(DynamicObject a, long b) {
            return getBigIntegerValue(a).compareTo(BigInteger.valueOf(b)) <= 0;
        }

        @Specialization
        public boolean lessEqual(DynamicObject a, double b) {
            return getBigIntegerValue(a).compareTo(BigInteger.valueOf((long) b)) <= 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean lessEqual(DynamicObject a, DynamicObject b) {
            return getBigIntegerValue(a).compareTo(getBigIntegerValue(b)) <= 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object lessEqualCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby(frame, "b, a = math_coerce other, :compare_error; a <= b", "other", b);
        }
    }

    @CoreMethod(names = {"==", "eql?"}, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child private BooleanCastNode booleanCastNode;
        @Child private CallDispatchHeadNode reverseCallNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean equal(DynamicObject a, int b) {
            return false;
        }

        @Specialization
        public boolean equal(DynamicObject a, long b) {
            return false;
        }

        @Specialization
        public boolean equal(DynamicObject a, double b) {
            return getBigIntegerValue(a).doubleValue() == b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return getBigIntegerValue(a).equals(getBigIntegerValue(b));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public boolean equal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            if (booleanCastNode == null) {
                CompilerDirectives.transferToInterpreter();
                booleanCastNode = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }

            if (reverseCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                reverseCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object reversedResult = reverseCallNode.call(frame, b, "==", null, a);

            return booleanCastNode.executeBoolean(frame, reversedResult);
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        private final ConditionProfile negativeInfinityProfile = ConditionProfile.createBinaryProfile();

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int compare(DynamicObject a, long b) {
            return getBigIntegerValue(a).compareTo(BigInteger.valueOf(b));
        }

        @Specialization
        public int compare(DynamicObject a, double b) {
            if (negativeInfinityProfile.profile(Double.isInfinite(b) && b < 0)) {
                return 1;
            } else {
                return Double.compare(getBigIntegerValue(a).doubleValue(), b);
            }
        }

        @Specialization(guards = "isRubyBignum(b)")
        public int compare(DynamicObject a, DynamicObject b) {
            return getBigIntegerValue(a).compareTo(getBigIntegerValue(b));
        }
    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodArrayArgumentsNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greaterEqual(DynamicObject a, long b) {
            return getBigIntegerValue(a).compareTo(BigInteger.valueOf(b)) >= 0;
        }

        @Specialization
        public boolean greaterEqual(DynamicObject a, double b) {
            return Double.compare(getBigIntegerValue(a).doubleValue(), b) >= 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greaterEqual(DynamicObject a, DynamicObject b) {
            return getBigIntegerValue(a).compareTo(getBigIntegerValue(b)) >= 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object greaterEqualCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby(frame, "b, a = math_coerce other, :compare_error; a >= b", "other", b);
        }
    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodArrayArgumentsNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greater(DynamicObject a, long b) {
            return getBigIntegerValue(a).compareTo(BigInteger.valueOf(b)) > 0;
        }

        @Specialization
        public boolean greater(DynamicObject a, double b) {
            return Double.compare(getBigIntegerValue(a).doubleValue(), b) > 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greater(DynamicObject a, DynamicObject b) {
            return getBigIntegerValue(a).compareTo(getBigIntegerValue(b)) > 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object greaterCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby(frame, "b, a = math_coerce other, :compare_error; a > b", "other", b);
        }
    }

    @CoreMethod(names = "&", required = 1)
    public abstract static class BitAndNode extends BignumCoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitAnd(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).and(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitAnd(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).and(getBigIntegerValue(b)));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitOr(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).or(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitOr(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).or(getBigIntegerValue(a)));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitXOr(DynamicObject a, long b) {
            return fixnumOrBignum(getBigIntegerValue(a).xor(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitXOr(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(getBigIntegerValue(a).xor(getBigIntegerValue(b)));
        }
    }

    @CoreMethod(names = "<<", required = 1, lowerFixnumParameters = 0)
    public abstract static class LeftShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object leftShift(DynamicObject a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(getBigIntegerValue(a).shiftLeft(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(getBigIntegerValue(a).shiftRight(-b));
            }
        }

    }

    @CoreMethod(names = ">>", required = 1, lowerFixnumParameters = 0)
    public abstract static class RightShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object leftShift(DynamicObject a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(getBigIntegerValue(a).shiftRight(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(getBigIntegerValue(a).shiftLeft(-b));
            }
        }

    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends BignumCoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object abs(DynamicObject value) {
            return fixnumOrBignum(getBigIntegerValue(value).abs());
        }

    }

    @CoreMethod(names = "bit_length")
    public abstract static class BitLengthNode extends CoreMethodArrayArgumentsNode {

        public BitLengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int bitLength(DynamicObject value) {
            return getBigIntegerValue(value).bitLength();
        }

    }

    @CoreMethod(names = "coerce", required = 1)
    public abstract static class CoerceNode extends CoreMethodArrayArgumentsNode {

        public CoerceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject coerce(DynamicObject a, int b) {
            CompilerDirectives.transferToInterpreter();

            // TODO (eregon, 16 Feb. 2015): This is NOT spec, but let's try to see if we can make it work.
            // b is converted to a Bignum here in other implementations.
            Object[] store = new Object[] { b, a };
            return createArray(store, store.length);
        }

        @Specialization
        public DynamicObject coerce(DynamicObject a, long b) {
            CompilerDirectives.transferToInterpreter();

            // TODO (eregon, 16 Feb. 2015): This is NOT spec, but let's try to see if we can make it work.
            // b is converted to a Bignum here in other implementations.
            Object[] store = new Object[] { b, a };
            return createArray(store, store.length);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public DynamicObject coerce(DynamicObject a, DynamicObject b) {
            CompilerDirectives.transferToInterpreter();

            Object[] store = new Object[] { b, a };
            return createArray(store, store.length);
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodArrayArgumentsNode {

        @Child private GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context, sourceSection);
        }

        @Specialization
        public DynamicObject divMod(DynamicObject a, long b) {
            return divModNode.execute(getBigIntegerValue(a), b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public DynamicObject divMod(DynamicObject a, DynamicObject b) {
            return divModNode.execute(getBigIntegerValue(a), getBigIntegerValue(b));
        }

    }

    @CoreMethod(names = "even?")
    public abstract static class EvenNode extends BignumCoreMethodNode {

        public EvenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean even(DynamicObject value) {
            return !getBigIntegerValue(value).testBit(0);
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(DynamicObject value) {
            return getBigIntegerValue(value).hashCode();
        }

    }

    @CoreMethod(names = "odd?")
    public abstract static class OddNode extends BignumCoreMethodNode {

        public OddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean odd(DynamicObject value) {
            return getBigIntegerValue(value).testBit(0);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(DynamicObject value) {
            return (getBigIntegerValue(value).bitLength() + 7) / 8;
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double toF(DynamicObject value) {
            return getBigIntegerValue(value).doubleValue();
        }

    }

    @CoreMethod(names = {"to_s", "inspect"}, optional = 1)
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject value, NotProvided base) {
            return createString(getBigIntegerValue(value).toString(), USASCIIEncoding.INSTANCE);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject value, int base) {
            if (base < 2 || base > 36) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorInvalidRadix(base, this));
            }

            return createString(getBigIntegerValue(value).toString(base), USASCIIEncoding.INSTANCE);
        }

    }

}
