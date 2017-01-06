/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.core.cast.ToIntNode;
import org.jruby.truffle.core.numeric.BignumNodesFactory.DivNodeFactory;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.math.BigInteger;

@CoreClass("Bignum")
public abstract class BignumNodes {

    public static abstract class BignumCoreMethodNode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public Object fixnumOrBignum(BigInteger value) {
            if (fixnumOrBignum == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                fixnumOrBignum = insert(new FixnumOrBignumNode());
            }
            return fixnumOrBignum.fixnumOrBignum(value);
        }

    }

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BignumCoreMethodNode {

        @Specialization
        public Object neg(DynamicObject value) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(value).negate());
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumCoreMethodNode {

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

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumCoreMethodNode {

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

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends BignumCoreMethodNode {

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
        public Object mul(
                VirtualFrame frame,
                DynamicObject a,
                Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "redo_coerced :*, other", "other", b);
        }

    }

    @CoreMethod(names = { "/", "__slash__" }, required = 1)
    public abstract static class DivNode extends BignumCoreMethodNode {

        public abstract Object executeDiv(VirtualFrame frame, Object a, Object b);

        @TruffleBoundary
        @Specialization
        public Object div(DynamicObject a, long b) {
            if (b == 0) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            }
            final BigInteger bBigInt = BigInteger.valueOf(b);
            final BigInteger aBigInt = Layouts.BIGNUM.getValue(a);
            final BigInteger result = aBigInt.divide(bBigInt);
            if (result.signum() == -1 && !aBigInt.mod(bBigInt.abs()).equals(BigInteger.ZERO)) {
                return fixnumOrBignum(result.subtract(BigInteger.ONE));
            } else {
                return fixnumOrBignum(result);
            }
        }

        @TruffleBoundary
        @Specialization
        public double div(DynamicObject a, double b) {
            return Layouts.BIGNUM.getValue(a).doubleValue() / b;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyBignum(b)")
        public Object div(DynamicObject a, DynamicObject b) {
            final BigInteger aBigInt = Layouts.BIGNUM.getValue(a);
            final BigInteger bBigInt = Layouts.BIGNUM.getValue(b);
            if (b.equals(BigInteger.ZERO)) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            }
            final BigInteger result = aBigInt.divide(bBigInt);
            if (result.signum() == -1 && !aBigInt.mod(bBigInt.abs()).equals(BigInteger.ZERO)) {
                return fixnumOrBignum(result.subtract(BigInteger.ONE));
            } else {
                return fixnumOrBignum(result);
            }
        }

    }

    // Defined in Java as we need to statically call #/
    @CoreMethod(names = "div", required = 1)
    public abstract static class IDivNode extends BignumNodes.BignumCoreMethodNode {

        @Child private DivNode divNode = DivNodeFactory.create(null);
        @Child private FloatNodes.FloorNode floorNode = FloatNodesFactory.FloorNodeFactory.create(null);

        @Specialization
        public Object idiv(VirtualFrame frame, Object a, Object b,
                @Cached("createBinaryProfile()") ConditionProfile zeroProfile) {
            Object quotient = divNode.executeDiv(frame, a, b);
            if (quotient instanceof Double) {
                if (zeroProfile.profile((double) b == 0.0)) {
                    throw new RaiseException(coreExceptions().zeroDivisionError(this));
                }
                return floorNode.executeFloor((double) quotient);
            } else {
                return quotient;
            }
        }

    }

    @CoreMethod(names = {"%", "modulo"}, required = 1)
    public abstract static class ModNode extends BignumCoreMethodNode {

        @TruffleBoundary
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

        @TruffleBoundary
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
        public Object mod(
                VirtualFrame frame,
                DynamicObject a,
                Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "redo_coerced :%, other", "other", b);
        }

    }

    @CoreMethod(names = "<", required = 1)
    public abstract static class LessNode extends CoreMethodArrayArgumentsNode {

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
        public Object lessCoerced(
                VirtualFrame frame,
                DynamicObject a,
                Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "b, a = math_coerce other, :compare_error; a < b", "other", b);
        }

    }

    @CoreMethod(names = "<=", required = 1)
    public abstract static class LessEqualNode extends CoreMethodArrayArgumentsNode {

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
        public Object lessEqualCoerced(
                VirtualFrame frame,
                DynamicObject a,
                Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "b, a = math_coerce other, :compare_error; a <= b", "other", b);
        }
    }

    @CoreMethod(names = "==" , required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child private BooleanCastNode booleanCastNode;
        @Child private CallDispatchHeadNode reverseCallNode;

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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                booleanCastNode = insert(BooleanCastNodeGen.create(null));
            }

            if (reverseCallNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                reverseCallNode = insert(DispatchHeadNodeFactory.createMethodCall());
            }

            final Object reversedResult = reverseCallNode.call(frame, b, "==", a);

            return booleanCastNode.executeToBoolean(reversedResult);
        }
    }

    @CoreMethod(names = ">=", required = 1)
    public abstract static class GreaterEqualNode extends CoreMethodArrayArgumentsNode {

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
        public Object greaterEqualCoerced(
                VirtualFrame frame,
                DynamicObject a,
                Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "b, a = math_coerce other, :compare_error; a >= b", "other", b);
        }
    }

    @CoreMethod(names = ">", required = 1)
    public abstract static class GreaterNode extends CoreMethodArrayArgumentsNode {

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
        public Object greaterCoerced(
                VirtualFrame frame,
                DynamicObject a,
                Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "b, a = math_coerce other, :compare_error; a > b", "other", b);
        }
    }

    @CoreMethod(names = "~")
    public abstract static class ComplementNode extends BignumCoreMethodNode {

        @Specialization
        public Object complement(DynamicObject value) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(value).not());
        }

    }

    @CoreMethod(names = "&", required = 1)
    public abstract static class BitAndNode extends BignumCoreMethodNode {

        @Specialization
        public Object bitAnd(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).and(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitAnd(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).and(Layouts.BIGNUM.getValue(b)));
        }

        @Specialization(guards = { "!isInteger(b)", "!isLong(b)", "!isRubyBignum(b)" })
        public Object bitAnd(VirtualFrame frame, DynamicObject a, Object b,
                             @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "self & bit_coerce(b)[1]", "b", b);
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumCoreMethodNode {

        @Specialization
        public Object bitOr(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).or(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitOr(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).or(Layouts.BIGNUM.getValue(b)));
        }

        @Specialization(guards = { "!isInteger(b)", "!isLong(b)", "!isRubyBignum(b)" })
        public Object bitAnd(VirtualFrame frame, DynamicObject a, Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "self | bit_coerce(b)[1]", "b", b);
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumCoreMethodNode {

        @Specialization
        public Object bitXOr(DynamicObject a, long b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).xor(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitXOr(DynamicObject a, DynamicObject b) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(a).xor(Layouts.BIGNUM.getValue(b)));
        }

        @Specialization(guards = { "!isInteger(b)", "!isLong(b)", "!isRubyBignum(b)" })
        public Object bitAnd(VirtualFrame frame, DynamicObject a, Object b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "self ^ bit_coerce(b)[1]", "b", b);
        }

    }

    @CoreMethod(names = "<<", required = 1, lowerFixnum = 1)
    public abstract static class LeftShiftNode extends BignumCoreMethodNode {

        public abstract Object executeLeftShift(VirtualFrame frame, DynamicObject a, Object b);

        @Specialization
        public Object leftShift(DynamicObject a, int b,
                                @Cached("createBinaryProfile()") ConditionProfile bPositive) {
            if (bPositive.profile(b >= 0)) {
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftLeft(b));
            } else {
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftRight(-b));
            }
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object leftShift(VirtualFrame frame, DynamicObject a, DynamicObject b,
                                @Cached("create()") ToIntNode toIntNode) {
            final BigInteger bBigInt = Layouts.BIGNUM.getValue(b);
            if (bBigInt.signum() == -1) {
                return 0;
            } else {
                // MRI would raise a NoMemoryError; JRuby would raise a coercion error.
                return executeLeftShift(frame, a, toIntNode.doInt(frame, b));
            }
        }

        @Specialization(guards = {"!isRubyBignum(b)", "!isInteger(b)", "!isLong(b)"})
        public Object leftShift(VirtualFrame frame, DynamicObject a, Object b,
                                @Cached("create()") ToIntNode toIntNode) {
            return executeLeftShift(frame, a, toIntNode.doInt(frame, b));
        }

    }

    @CoreMethod(names = ">>", required = 1, lowerFixnum = 1)
    public abstract static class RightShiftNode extends BignumCoreMethodNode {

        public abstract Object executeRightShift(VirtualFrame frame, DynamicObject a, Object b);

        @Specialization
        public Object rightShift(DynamicObject a, int b,
                @Cached("createBinaryProfile()") ConditionProfile bPositive) {
            if (bPositive.profile(b >= 0)) {
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftRight(b));
            } else {
                return fixnumOrBignum(Layouts.BIGNUM.getValue(a).shiftLeft(-b));
            }
        }

        @Specialization
        public Object rightShift(VirtualFrame frame, DynamicObject a, long b) {
            assert !CoreLibrary.fitsIntoInteger(b);
            return 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public int rightShift(DynamicObject a, DynamicObject b) {
            return 0;
        }

        @Specialization(guards = {"!isRubyBignum(b)", "!isInteger(b)", "!isLong(b)"})
        public Object rightShift(VirtualFrame frame, DynamicObject a, Object b,
                @Cached("create()") ToIntNode toIntNode) {
            return executeRightShift(frame, a, toIntNode.doInt(frame, b));
        }


    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends BignumCoreMethodNode {

        @Specialization
        public Object abs(DynamicObject value) {
            return fixnumOrBignum(Layouts.BIGNUM.getValue(value).abs());
        }

    }

    @CoreMethod(names = "bit_length")
    public abstract static class BitLengthNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int bitLength(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).bitLength();
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodArrayArgumentsNode {

        @Child private GeneralDivModNode divModNode = new GeneralDivModNode();

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

    @CoreMethod(names = "even?")
    public abstract static class EvenNode extends BignumCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public boolean even(DynamicObject value) {
            return !Layouts.BIGNUM.getValue(value).testBit(0);
        }

    }

    @CoreMethod(names = "hash")
    public abstract static class HashNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int hash(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).hashCode();
        }

    }

    @CoreMethod(names = "odd?")
    public abstract static class OddNode extends BignumCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public boolean odd(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).testBit(0);
        }

    }

    @CoreMethod(names = "size")
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public int size(DynamicObject value) {
            return (Layouts.BIGNUM.getValue(value).bitLength() + 7) / 8;
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public double toF(DynamicObject value) {
            return Layouts.BIGNUM.getValue(value).doubleValue();
        }

    }

    @CoreMethod(names = { "to_s", "inspect" }, optional = 1, lowerFixnum = 1)
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject value, NotProvided base) {
            return create7BitString(Layouts.BIGNUM.getValue(value).toString(), USASCIIEncoding.INSTANCE);
        }

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject value, int base) {
            if (base < 2 || base > 36) {
                throw new RaiseException(coreExceptions().argumentErrorInvalidRadix(base, this));
            }

            return create7BitString(Layouts.BIGNUM.getValue(value).toString(base), USASCIIEncoding.INSTANCE);
        }

    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            throw new RaiseException(coreExceptions().typeErrorAllocatorUndefinedFor(rubyClass, this));
        }

    }

    @Primitive(name = "bignum_compare")
    public abstract static class BignumCompareNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public int compare(DynamicObject a, long b) {
            return Layouts.BIGNUM.getValue(a).compareTo(BigInteger.valueOf(b));
        }

        @Specialization(guards = "!isInfinity(b)")
        public int compare(DynamicObject a, double b) {
            return Double.compare(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isInfinity(b)")
        public int compareInfinity(DynamicObject a, double b) {
            if (b < 0) {
                return +1;
            } else {
                return -1;
            }
        }

        @Specialization(guards = "isRubyBignum(b)")
        public int compare(DynamicObject a, DynamicObject b) {
            return Layouts.BIGNUM.getValue(a).compareTo(Layouts.BIGNUM.getValue(b));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object compareFallback(DynamicObject a, DynamicObject b) {
            return null; // Primitive failure
        }

    }

    @Primitive(name = "bignum_pow")
    public static abstract class BignumPowPrimitiveNode extends PrimitiveArrayArgumentsNode {

        private final ConditionProfile negativeProfile = ConditionProfile.createBinaryProfile();

        @Specialization
        public DynamicObject pow(DynamicObject a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public DynamicObject pow(DynamicObject a, long b) {
            if (negativeProfile.profile(b < 0)) {
                return null; // Primitive failure
            } else {
                // TODO CS 15-Feb-15 what about this cast?
                return createBignum(Layouts.BIGNUM.getValue(a).pow((int) b));
            }
        }

        @TruffleBoundary
        @Specialization
        public double pow(DynamicObject a, double b) {
            return Math.pow(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Void pow(DynamicObject a, DynamicObject b) {
            throw new UnsupportedOperationException();
        }

    }

}
