/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.numeric;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.UnaryCoreMethodNode;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.math.BigInteger;

@CoreClass(name = "Bignum")
public abstract class BignumNodes {

    public static abstract class BignumCoreMethodNode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public BignumCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public Object fixnumOrBignum(BigInteger value) {
            if (fixnumOrBignum == null) {
                CompilerDirectives.transferToInterpreter();
                fixnumOrBignum = insert(new FixnumOrBignumNode(getContext(), getSourceSection()));
            }
            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "-@")
    public abstract static class NegNode extends BignumCoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object neg(DynamicObject value) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(value).negate());
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "+", required = 1)
    public abstract static class AddNode extends BignumCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object add(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).add(BigInteger.valueOf(b)));
        }

        @Specialization
        public double add(DynamicObject a, double b) {
            return Layouts.BIGNUM.getValue(a).doubleValue() + b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object add(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).add(Layouts.BIGNUM.getValue(b)));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "-", required = 1)
    public abstract static class SubNode extends BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object sub(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization
        public double sub(DynamicObject a, double b) {
            return Layouts.BIGNUM.getValue(a).doubleValue() - b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object sub(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).subtract(Layouts.BIGNUM.getValue(b)));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "*", required = 1)
    public abstract static class MulNode extends BignumCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public Object mul(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(DynamicObject a, double b) {
            return Layouts.BIGNUM.getValue(a).doubleValue() * b;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyBignum(b)")
        public Object mul(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).multiply(Layouts.BIGNUM.getValue(b)));
        }

        @Specialization(guards = {"!isInteger(b)", "!isLong(b)", "!isDouble(b)", "!isRubyBignum(b)"})
        public Object mul(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby("redo_coerced :*, other", "other", b);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends BignumCoreMethodNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object div(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).divide(BigInteger.valueOf(b)));
        }

        @Specialization
        public double div(DynamicObject a, double b) {
            return Layouts.BIGNUM.getValue(a).doubleValue() / b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object div(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).divide(Layouts.BIGNUM.getValue(b)));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = {"%", "modulo"}, required = 1)
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
                final BigInteger mod = Layouts.BIGNUM.getValue(a).mod(bigint.negate());
                return fixnumOrBignum(mod.add(bigint));
            }
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).mod(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object mod(DynamicObject a, DynamicObject b) {
            final BigInteger bigint = Layouts.BIGNUM.getValue(b);
            final int compare = bigint.compareTo(BigInteger.ZERO);
            if (compare == 0) {
                throw new ArithmeticException("divide by zero");
            } else if (compare < 0) {
                final BigInteger mod = Layouts.BIGNUM.getValue(a).mod(bigint.negate());
                return fixnumOrBignum(mod.add(bigint));
            }
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).mod(Layouts.BIGNUM.getValue(b)));
        }

        @Specialization(guards = {"!isInteger(b)", "!isLong(b)", "!isRubyBignum(b)"})
        public Object mod(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby("redo_coerced :%, other", "other", b);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodArrayArgumentsNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean less(DynamicObject a, long b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf(b)) < 0;
        }

        @Specialization
        public boolean less(DynamicObject a, double b) {
            return Double.compare(Layouts.BIGNUM.getValue(a).doubleValue(), b) < 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean less(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).compareTo(Layouts.BIGNUM.getValue(b)) < 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object lessCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a < b", "other", b);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodArrayArgumentsNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean lessEqual(DynamicObject a, long b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf(b)) <= 0;
        }

        @Specialization
        public boolean lessEqual(DynamicObject a, double b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf((long) b)) <= 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean lessEqual(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).compareTo(Layouts.BIGNUM.getValue(b)) <= 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object lessEqualCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a <= b", "other", b);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = {"==", "eql?"}, required = 1)
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
            return Layouts.BIGNUM.getValue(a).doubleValue() == b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean equal(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).equals(Layouts.BIGNUM.getValue(b));
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

    @CoreMethod(unsafeNeedsAudit = true, names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodArrayArgumentsNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greaterEqual(DynamicObject a, long b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf(b)) >= 0;
        }

        @Specialization
        public boolean greaterEqual(DynamicObject a, double b) {
            return Double.compare(Layouts.BIGNUM.getValue(a).doubleValue(), b) >= 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greaterEqual(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).compareTo(Layouts.BIGNUM.getValue(b)) >= 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object greaterEqualCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a >= b", "other", b);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodArrayArgumentsNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean greater(DynamicObject a, long b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf(b)) > 0;
        }

        @Specialization
        public boolean greater(DynamicObject a, double b) {
            return Double.compare(Layouts.BIGNUM.getValue(a).doubleValue(), b) > 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greater(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).compareTo(Layouts.BIGNUM.getValue(b)) > 0;
        }

        @Specialization(guards = {
                "!isRubyBignum(b)",
                "!isInteger(b)",
                "!isLong(b)",
                "!isDouble(b)"})
        public Object greaterCoerced(VirtualFrame frame, DynamicObject a, Object b) {
            return ruby("b, a = math_coerce other, :compare_error; a > b", "other", b);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "~")
    public abstract static class ComplementNode extends BignumCoreMethodNode {

        public ComplementNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object complement(DynamicObject value) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(value).not());
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "&", required = 1)
    public abstract static class BitAndNode extends BignumCoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitAnd(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).and(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitAnd(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).and(Layouts.BIGNUM.getValue(b)));
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "|", required = 1)
    public abstract static class BitOrNode extends BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitOr(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).or(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitOr(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).or(Layouts.BIGNUM.getValue(a)));
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object bitXOr(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).xor(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitXOr(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).xor(Layouts.BIGNUM.getValue(b)));
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "<<", required = 1, lowerFixnumParameters = 0)
    public abstract static class LeftShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object leftShift(DynamicObject a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftLeft(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftRight(-b));
            }
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = ">>", required = 1, lowerFixnumParameters = 0)
    public abstract static class RightShiftNode extends BignumCoreMethodNode {

        private final BranchProfile bLessThanZero = BranchProfile.create();

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object leftShift(DynamicObject a, int b) {
            if (b >= 0) {
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftRight(b));
            } else {
                bLessThanZero.enter();
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftLeft(-b));
            }
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = { "abs", "magnitude" })
    public abstract static class AbsNode extends BignumCoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object abs(DynamicObject value) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(value).abs());
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "bit_length")
    public abstract static class BitLengthNode extends CoreMethodArrayArgumentsNode {

        public BitLengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int bitLength(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).bitLength();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "coerce", required = 1)
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
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

        @Specialization
        public DynamicObject coerce(DynamicObject a, long b) {
            CompilerDirectives.transferToInterpreter();

            // TODO (eregon, 16 Feb. 2015): This is NOT spec, but let's try to see if we can make it work.
            // b is converted to a Bignum here in other implementations.
            Object[] store = new Object[] { b, a };
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public DynamicObject coerce(DynamicObject a, DynamicObject b) {
            CompilerDirectives.transferToInterpreter();

            Object[] store = new Object[] { b, a };
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodArrayArgumentsNode {

        @Child private GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context, sourceSection);
        }

        @Specialization
        public DynamicObject divMod(DynamicObject a, long b) {
            return divModNode.execute(Layouts.BIGNUM.getValue(a), b);
        }

        @Specialization
        public DynamicObject divMod(DynamicObject a, double b) {
            return divModNode.execute(Layouts.BIGNUM.getValue(a), b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public DynamicObject divMod(DynamicObject a, DynamicObject b) {
            return divModNode.execute(Layouts.BIGNUM.getValue(a), Layouts.BIGNUM.getValue(b));
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "even?")
    public abstract static class EvenNode extends BignumCoreMethodNode {

        public EvenNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean even(DynamicObject value) {
            return !Layouts.BIGNUM.getValue(value).testBit(0);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        public HashNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int hash(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).hashCode();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "odd?")
    public abstract static class OddNode extends BignumCoreMethodNode {

        public OddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public boolean odd(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).testBit(0);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size(DynamicObject value) {
            return (Layouts.BIGNUM.getValue(value).bitLength() + 7) / 8;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public double toF(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).doubleValue();
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = {"to_s", "inspect"}, optional = 1)
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject value, NotProvided base) {
            return create7BitString(Layouts.BIGNUM.getValue(value).toString(), USASCIIEncoding.INSTANCE);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject value, int base) {
            if (base < 2 || base > 36) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().argumentErrorInvalidRadix(base, this));
            }

            return create7BitString(Layouts.BIGNUM.getValue(value).toString(base), USASCIIEncoding.INSTANCE);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreLibrary().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

}
