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
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import com.oracle.truffle.api.utilities.ConditionProfile;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyNilClass;
import org.jruby.truffle.runtime.core.RubyString;

import java.math.BigInteger;

@CoreClass(name = "Fixnum")
public abstract class FixnumNodes {

    private static final int BITS = 64;

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends CoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NegNode(NegNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int neg(int value) {
            return ExactMath.subtractExact(0, value);
        }

        @Specialization
        public long negWithOverflow(int value) {
            return -(long) (value);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long neg(long value) {
            return ExactMath.subtractExact(0, value);
        }

        @Specialization
        public RubyBignum negWithOverflow(long value) {
            return bignum(value).negate();
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumNodes.BignumCoreMethodNode {

        @Child private CallDispatchHeadNode rationalAdd;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AddNode(AddNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int add(int a, int b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public long addWithOverflow(int a, int b) {
            return (long) a + (long) b;
        }

        @Specialization
        public double add(int a, double b) {
            return a + b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(int a, long b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(int a, long b) {
            return fixnumOrBignum(bignum(a).add(bignum(b)));
        }

        @Specialization
        public Object add(int a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).add(b));
        }

        @Specialization(guards = "isRational(arguments[1])")
        public Object add(VirtualFrame frame, int a, RubyBasicObject b) {
            if (rationalAdd == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();

                rationalAdd = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return rationalAdd.call(frame, b, "+", null, a);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(long a, int b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(long a, int b) {
            return fixnumOrBignum(bignum(a).add(bignum(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(long a, long b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(long a, long b) {
            return fixnumOrBignum(bignum(a).add(bignum(b)));
        }

        @Specialization
        public double add(long a, double b) {
            return a + b;
        }

        @Specialization
        public Object add(long a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).add(b));
        }

        @Specialization(guards = "isRational(arguments[1])")
        public Object add(VirtualFrame frame, long a, RubyBasicObject b) {
            if (rationalAdd == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();

                rationalAdd = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            return rationalAdd.call(frame, b, "+", null, a);
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumNodes.BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SubNode(SubNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int sub(int a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public long subWithOverflow(int a, int b) {
            return (long) a - (long) b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(int a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(int a, long b) {
            return fixnumOrBignum(bignum(a).subtract(bignum(b)));
        }

        @Specialization
        public Object sub(int a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).subtract(b));
        }

        @Specialization
        public double sub(int a, double b) {
            return a - b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(long a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(long a, int b) {
            return fixnumOrBignum(bignum(a).subtract(bignum(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(long a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(long a, long b) {
            return fixnumOrBignum(bignum(a).subtract(bignum(b)));
        }

        @Specialization
        public double sub(long a, double b) {
            return a - b;
        }

        @Specialization
        public Object sub(long a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).subtract(b));
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends BignumNodes.BignumCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public MulNode(MulNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int mul(int a, int b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization
        public long mulWithOverflow(int a, int b) {
            return (long) a * (long) b;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public Object mul(int a, long b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization
        public Object mulWithOverflow(int a, long b) {
            return fixnumOrBignum(bignum(a).multiply(bignum(b)));
        }

        @Specialization
        public double mul(int a, double b) {
            return a * b;
        }

        @Specialization
        public Object mul(int a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).multiply(b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long mul(long a, int b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization
        public Object mulWithOverflow(long a, int b) {
            return fixnumOrBignum(bignum(a).multiply(bignum(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long mul(long a, long b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization
        public Object mulWithOverflow(long a, long b) {
            return fixnumOrBignum(bignum(a).multiply(bignum(b)));
        }

        @Specialization
        public double mul(long a, double b) {
            return a * b;
        }

        @Specialization
        public Object mul(long a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).multiply(b));
        }

    }

    @CoreMethod(names = "**", required = 1)
    public abstract static class PowNode extends BignumNodes.BignumCoreMethodNode {

        @Child private CallDispatchHeadNode complexConvertNode;
        @Child private CallDispatchHeadNode complexPowNode;

        @Child private CallDispatchHeadNode rationalConvertNode;
        @Child private CallDispatchHeadNode rationalPowNode;

        private final ConditionProfile negativeProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile complexProfile = ConditionProfile.createBinaryProfile();

        public PowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PowNode(PowNode prev) {
            super(prev);
            complexConvertNode = prev.complexConvertNode;
            complexPowNode = prev.complexPowNode;
            rationalConvertNode = prev.rationalConvertNode;
            rationalPowNode = prev.rationalPowNode;
        }

        @Specialization(guards = "canShiftIntoInt")
        public int powTwo(int a, int b) {
            return 1 << b;
        }

        @Specialization(guards = "canShiftIntoInt")
        public int powTwo(int a, long b) {
            return 1 << b;
        }

        @Specialization
        public Object pow(int a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public Object pow(int a, long b) {
            return pow((long) a, b);
        }

        @Specialization
        public Object pow(VirtualFrame frame, int a, double b) {
            return pow(frame, (long) a, b);
        }

        @Specialization
        public Object pow(int a, RubyBignum b) {
            return pow((long) a, b);
        }

        @Specialization(guards = "canShiftIntoLong")
        public long powTwo(long a, int b) {
            return 1 << b;
        }

        @Specialization(guards = "canShiftIntoLong")
        public long powTwo(long a, long b) {
            return 1 << b;
        }

        @Specialization
        public Object pow(long a, int b) {
            return pow(a, (long) b);
        }

        @Specialization
        public Object pow(long a, long b) {
            if (negativeProfile.profile(b < 0)) {
                return Math.pow(a, b);
            } else {
                return fixnumOrBignum(bignum(a).pow(b));
            }
        }

        @Specialization
        public Object pow(VirtualFrame frame, long a, double b) {
            if (complexProfile.profile(a < 0 && b != Math.round(b))) {
                if (complexConvertNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    complexConvertNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                    complexPowNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
                }

                final Object aComplex = complexConvertNode.call(frame, getContext().getCoreLibrary().getComplexClass(), "convert", null, a, 0);

                return complexPowNode.call(frame, aComplex, "**", null, b);
            } else {
                return Math.pow(a, b);
            }
        }

        @Specialization
        public Object pow(long a, RubyBignum b) {
            notDesignedForCompilation();

            if (a == 0) {
                return 0;
            }

            if (a == 1) {
                return 1;
            }

            if (a == -1) {
                if (b.bigIntegerValue().testBit(0)) {
                    return -1;
                } else {
                    return 1;
                }
            }

            return Math.pow(a, b.doubleValue());
        }

        @Specialization(guards = "isRational(arguments[1])")
        public Object pow(VirtualFrame frame, Object a, RubyBasicObject b) {
            if (rationalConvertNode == null) {
                CompilerDirectives.transferToInterpreter();
                rationalConvertNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
                rationalPowNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }

            final Object aRational = rationalConvertNode.call(frame, getContext().getCoreLibrary().getRationalClass(), "convert", null, a, 1);

            return rationalPowNode.call(frame, aRational, "**", null, b);
        }

        protected static boolean canShiftIntoInt(int a, int b) {
            return canShiftIntoInt(a, (long) b);
        }

        protected static boolean canShiftIntoInt(int a, long b) {
            // Highest bit we can set is the 30th due to sign
            return a == 2 && b <= 32 - 2;
        }

        protected static boolean canShiftIntoLong(long a, int b) {
            return canShiftIntoLong(a, (long) b);
        }

        protected static boolean canShiftIntoLong(long a, long b) {
            // Highest bit we can set is the 30th due to sign
            return a == 2 && b <= 64 - 2;
        }

    }

    @CoreMethod(names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends CoreMethodNode {

        private final BranchProfile bGreaterZero = BranchProfile.create();
        private final BranchProfile bGreaterZeroAGreaterEqualZero = BranchProfile.create();
        private final BranchProfile bGreaterZeroALessZero = BranchProfile.create();
        private final BranchProfile aGreaterZero = BranchProfile.create();
        private final BranchProfile bMinusOne = BranchProfile.create();
        private final BranchProfile bMinusOneAMinimum = BranchProfile.create();
        private final BranchProfile bMinusOneANotMinimum = BranchProfile.create();
        private final BranchProfile finalCase = BranchProfile.create();

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public DivNode(DivNode prev) {
            super(prev);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        public int div(int a, int b) throws UnexpectedResultException {
            if (b > 0) {
                bGreaterZero.enter();
                if (a >= 0) {
                    bGreaterZeroAGreaterEqualZero.enter();
                    return a / b;
                } else {
                    bGreaterZeroALessZero.enter();
                    return (a + 1) / b - 1;
                }
            } else if (a > 0) {
                aGreaterZero.enter();
                return (a - 1) / b - 1;
            } else if (b == -1) {
                bMinusOne.enter();
                if (a == Integer.MIN_VALUE) {
                    bMinusOneAMinimum.enter();
                    throw new UnexpectedResultException(bignum(a).negate());
                } else {
                    bMinusOneANotMinimum.enter();
                    return -a;
                }
            } else {
                finalCase.enter();
                return a / b;
            }
        }

        @Specialization
        public Object divEdgeCase(int a, int b) {
            if (b > 0) {
                bGreaterZero.enter();
                if (a >= 0) {
                    bGreaterZeroAGreaterEqualZero.enter();
                    return a / b;
                } else {
                    bGreaterZeroALessZero.enter();
                    return (a + 1) / b - 1;
                }
            } else if (a > 0) {
                aGreaterZero.enter();
                return (a - 1) / b - 1;
            } else if (b == -1) {
                bMinusOne.enter();
                if (a == Integer.MIN_VALUE) {
                    bMinusOneAMinimum.enter();
                    return bignum(a).negate();
                } else {
                    bMinusOneANotMinimum.enter();
                    return -a;
                }
            } else {
                finalCase.enter();
                return a / b;
            }
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        public long div(int a, long b) throws UnexpectedResultException {
            return div((long) a, b);
        }

        @Specialization
        public Object divEdgeCase(int a, long b) {
            return divEdgeCase((long) a, b);
        }

        @Specialization
        public double div(int a, double b) {
            return a / b;
        }

        @Specialization
        public int div(@SuppressWarnings("unused") int a, @SuppressWarnings("unused") RubyBignum b) {
            // TODO(CS): not entirely sure this is correct
            return 0;
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        public long div(long a, int b) throws UnexpectedResultException {
            return div(a, (long) b);
        }

        @Specialization
        public Object divEdgeCase(long a, int b) {
            return divEdgeCase(a, (long) b);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        public long div(long a, long b) throws UnexpectedResultException {
            if (b > 0) {
                bGreaterZero.enter();
                if (a >= 0) {
                    bGreaterZeroAGreaterEqualZero.enter();
                    return a / b;
                } else {
                    bGreaterZeroALessZero.enter();
                    return (a + 1) / b - 1;
                }
            } else if (a > 0) {
                aGreaterZero.enter();
                return (a - 1) / b - 1;
            } else if (b == -1) {
                bMinusOne.enter();
                if (a == Long.MIN_VALUE) {
                    bMinusOneAMinimum.enter();
                    throw new UnexpectedResultException(bignum(a).negate());
                } else {
                    bMinusOneANotMinimum.enter();
                    return -a;
                }
            } else {
                finalCase.enter();
                return a / b;
            }
        }

        @Specialization
        public Object divEdgeCase(long a, long b) {
            if (b > 0) {
                bGreaterZero.enter();
                if (a >= 0) {
                    bGreaterZeroAGreaterEqualZero.enter();
                    return a / b;
                } else {
                    bGreaterZeroALessZero.enter();
                    return (a + 1) / b - 1;
                }
            } else if (a > 0) {
                aGreaterZero.enter();
                return (a - 1) / b - 1;
            } else if (b == -1) {
                bMinusOne.enter();
                if (a == Long.MIN_VALUE) {
                    bMinusOneAMinimum.enter();
                    return bignum(a).negate();
                } else {
                    bMinusOneANotMinimum.enter();
                    return -a;
                }
            } else {
                finalCase.enter();
                return a / b;
            }
        }

        @Specialization
        public double div(long a, double b) {
            return a / b;
        }

        @Specialization
        public int div(@SuppressWarnings("unused") long a, @SuppressWarnings("unused") RubyBignum b) {
            // TODO(CS): not entirely sure this is correct
            return 0;
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends BignumNodes.BignumCoreMethodNode {

        private final BranchProfile adjustProfile = BranchProfile.create();

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ModNode(ModNode prev) {
            super(prev);
        }

        @Specialization
        public int mod(int a, int b) {
            int mod = a % b;

            if (mod < 0 && b > 0 || mod > 0 && b < 0) {
                adjustProfile.enter();
                mod += b;
            }

            return mod;
        }

        @Specialization
        public long mod(int a, long b) {
            return mod((long) a, b);
        }

        @Specialization
        public double mod(int a, double b) {
            return mod((long) a, b);
        }

        @Specialization
        public double mod(long a, double b) {
            if (b == 0) {
                throw new ArithmeticException("divide by zero");
            }

            double mod = a % b;

            if (mod < 0 && b > 0 || mod > 0 && b < 0) {
                adjustProfile.enter();
                mod += b;
            }

            return mod;
        }

        @Specialization
        public long mod(long a, int b) {
            return mod(a, (long) b);
        }

        @Specialization
        public long mod(long a, long b) {
            long mod = a % b;

            if (mod < 0 && b > 0 || mod > 0 && b < 0) {
                adjustProfile.enter();
                mod += b;
            }

            return mod;
        }

        @Specialization
        public Object mod(int a, RubyBignum b) {
            return mod((long) a, b);
        }

        @Specialization
        public Object mod(long a, RubyBignum b) {
            notDesignedForCompilation();

            // TODO(CS): why are we getting this case?

            long mod = BigInteger.valueOf(a).mod(b.bigIntegerValue()).longValue();

            if (mod < 0 && b.bigIntegerValue().compareTo(BigInteger.ZERO) > 0 || mod > 0 && b.bigIntegerValue().compareTo(BigInteger.ZERO) < 0) {
                adjustProfile.enter();
                return new RubyBignum(getContext().getCoreLibrary().getBignumClass(), BigInteger.valueOf(mod).add(b.bigIntegerValue()));
            }

            return mod;
        }

    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodNode {

        @Child private GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context, sourceSection);
        }

        public DivModNode(DivModNode prev) {
            super(prev);
            divModNode = prev.divModNode;
        }

        @Specialization
        public RubyArray divMod(int a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(int a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(int a, RubyBignum b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(int a, double b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(long a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(long a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(long a, RubyBignum b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(long a, double b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "<", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class LessNode extends CoreMethodNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessNode(LessNode prev) {
            super(prev);
        }

        @Specialization
        public boolean less(int a, int b) {
            return a < b;
        }

        @Specialization
        public boolean less(int a, long b) {
            return a < b;
        }

        @Specialization
        public boolean less(int a, double b) {
            return a < b;
        }

        @Specialization
        public boolean less(int a, RubyBignum b) {
            return bignum(a).compare(b) < 0;
        }

        @Specialization
        public boolean less(long a, int b) {
            return a < b;
        }

        @Specialization
        public boolean less(long a, long b) {
            return a < b;
        }

        @Specialization
        public boolean less(long a, double b) {
            return a < b;
        }

        @Specialization
        public boolean less(long a, RubyBignum b) {
            return bignum(a).compare(b) < 0;
        }
    }

    @CoreMethod(names = "<=", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class LessEqualNode extends CoreMethodNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LessEqualNode(LessEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean lessEqual(int a, int b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(int a, long b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(int a, double b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(int a, RubyBignum b) {
            return bignum(a).compare(b) <= 0;
        }

        @Specialization
        public boolean lessEqual(long a, int b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(long a, long b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(long a, double b) {
            return a <= b;
        }

        @Specialization
        public boolean lessEqual(long a, RubyBignum b) {
            return bignum(a).compare(b) <= 0;
        }
    }

    @CoreMethod(names = {"==", "==="}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode reverseCallNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            reverseCallNode = DispatchHeadNodeFactory.createMethodCall(context);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
            reverseCallNode = prev.reverseCallNode;
        }

        @Specialization
        public boolean equal(int a, int b) {
            return a == b;
        }

        @Specialization
        public boolean equal(int a, long b) {
            return a == b;
        }

        @Specialization
        public boolean equal(int a, double b) {
            return a == b;
        }

        @Specialization
        public boolean equal(int a, RubyBignum b) {
            return bignum(a).equals(b);
        }

        @Specialization
        public boolean equal(long a, int b) {
            return a == b;
        }

        @Specialization
        public boolean equal(long a, long b) {
            return a == b;
        }

        @Specialization
        public boolean equal(long a, double b) {
            return a == b;
        }

        @Specialization
        public boolean equal(long a, RubyBignum b) {
            return bignum(a).equals(b);
        }

        @Specialization(guards = {
                "!isInteger(arguments[1])",
                "!isLong(arguments[1])",
                "!isRubyBignum(arguments[1])"
        })
        public Object equal(VirtualFrame frame, Object a, Object b) {
            return reverseCallNode.call(frame, b, getName(), null, a);
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CompareNode(CompareNode prev) {
            super(prev);
        }

        @Specialization
        public int compare(int a, int b) {
            return Integer.compare(a, b);
        }

        @Specialization
        public int compare(int a, long b) {
            return Long.compare(a, b);
        }

        @Specialization
        public int compare(int a, double b) {
            return Double.compare(a, b);
        }

        @Specialization
        public int compare(int a, RubyBignum b) {
            return bignum(a).compare(b);
        }

        @Specialization
        public int compare(long a, int b) {
            return Long.compare(a, b);
        }

        @Specialization
        public int compare(long a, long b) {
            return Long.compare(a, b);
        }

        @Specialization
        public int compare(long a, double b) {
            return Double.compare(a, b);
        }

        @Specialization
        public int compare(long a, RubyBignum b) {
            return bignum(a).compare(b);
        }

        @Specialization(guards = {
                "!isInteger(arguments[1])",
                "!isLong(arguments[1])",
                "!isDouble(arguments[1])",
                "!isRubyBignum(arguments[1])"})
        public RubyNilClass compare(Object a, Object b) {
            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = ">=", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class GreaterEqualNode extends CoreMethodNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterEqualNode(GreaterEqualNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greaterEqual(int a, int b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(int a, long b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(int a, double b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(int a, RubyBignum b) {
            return bignum(a).compare(b) >= 0;
        }

        @Specialization
        public boolean greaterEqual(long a, int b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(long a, long b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(long a, double b) {
            return a >= b;
        }

        @Specialization
        public boolean greaterEqual(long a, RubyBignum b) {
            return bignum(a).compare(b) >= 0;
        }
    }

    @CoreMethod(names = ">", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class GreaterNode extends CoreMethodNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GreaterNode(GreaterNode prev) {
            super(prev);
        }

        @Specialization
        public boolean greater(int a, int b) {
            return a > b;
        }

        @Specialization
        public boolean greater(int a, long b) {
            return a > b;
        }

        @Specialization
        public boolean greater(int a, double b) {
            return a > b;
        }

        @Specialization
        public boolean greater(int a, RubyBignum b) {
            return bignum(a).compare(b) > 0;
        }

        @Specialization
        public boolean greater(long a, int b) {
            return a > b;
        }

        @Specialization
        public boolean greater(long a, long b) {
            return a > b;
        }

        @Specialization
        public boolean greater(long a, double b) {
            return a > b;
        }

        @Specialization
        public boolean greater(long a, RubyBignum b) {
            return bignum(a).compare(b) > 0;
        }

    }

    @CoreMethod(names = "~")
    public abstract static class ComplementNode extends CoreMethodNode {

        public ComplementNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ComplementNode(ComplementNode prev) {
            super(prev);
        }

        @Specialization
        public int complement(int n) {
            return ~n;
        }

        @Specialization
        public long complement(long n) {
            return ~n;
        }

    }

    @CoreMethod(names = "&", required = 1)
    public abstract static class BitAndNode extends BignumNodes.BignumCoreMethodNode {

        public BitAndNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitAndNode(BitAndNode prev) {
            super(prev);
        }

        @Specialization
        public int bitAnd(int a, int b) {
            return a & b;
        }

        @Specialization
        public long bitAnd(int a, long b) {
            return a & b;
        }

        @Specialization
        public Object bitAnd(int a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).and(b));
        }

        @Specialization
        public long bitAnd(long a, int b) {
            return a & b;
        }

        @Specialization
        public long bitAnd(long a, long b) {
            return a & b;
        }

        @Specialization
        public Object bitAnd(long a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).and(b));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumNodes.BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitOrNode(BitOrNode prev) {
            super(prev);
        }

        @Specialization
        public int bitOr(int a, int b) {
            return a | b;
        }

        @Specialization
        public long bitOr(int a, long b) {
            return a | b;
        }

        @Specialization
        public Object bitOr(int a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).or(b));
        }

        @Specialization
        public long bitOr(long a, int b) {
            return a | b;
        }

        @Specialization
        public long bitOr(long a, long b) {
            return a | b;
        }

        @Specialization
        public Object bitOr(long a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).or(b));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumNodes.BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitXOrNode(BitXOrNode prev) {
            super(prev);
        }

        @Specialization
        public int bitXOr(int a, int b) {
            return a ^ b;
        }

        @Specialization
        public long bitXOr(int a, long b) {
            return a ^ b;
        }

        @Specialization
        public Object bitXOr(int a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).xor(b));
        }

        @Specialization
        public long bitXOr(long a, int b) {
            return a ^ b;
        }

        @Specialization
        public long bitXOr(long a, long b) {
            return a ^ b;
        }

        @Specialization
        public Object bitXOr(long a, RubyBignum b) {
            return fixnumOrBignum(bignum(a).xor(b));
        }
    }

    @CoreMethod(names = "<<", required = 1)
    public abstract static class LeftShiftNode extends BignumNodes.BignumCoreMethodNode {

        @Child private CallDispatchHeadNode fallbackCallNode;

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LeftShiftNode(LeftShiftNode prev) {
            super(prev);
            fallbackCallNode = prev.fallbackCallNode;
        }

        protected Object lower(RubyBignum value) {
            return fixnumOrBignum(value);
        }

        public abstract Object executeLeftShift(VirtualFrame frame, Object a, Object b);

        @Specialization(guards = {"isPositive(arguments[1])", "canShiftIntoInt"})
        public int leftShift(int a, int b) {
            return a << b;
        }

        @Specialization(guards = {"isPositive(arguments[1])", "canShiftIntoLong"})
        public long leftShiftToLong(int a, int b) {
            return leftShiftToLong((long) a, b);
        }

        @Specialization(guards = {"isPositive(arguments[1])"})
        public Object leftShiftWithOverflow(int a, int b) {
            return leftShiftWithOverflow((long) a, b);
        }

        @Specialization(guards = "isStrictlyNegative(arguments[1])")
        public int leftShiftNeg(int a, int b) {
            if (-b >= Integer.SIZE) {
                return 0;
            } else {
                return a >> -b;
            }
        }

        @Specialization(guards = {"isPositive(arguments[1])", "canShiftIntoLong"})
        public long leftShiftToLong(long a, int b) {
            return a << b;
        }

        @Specialization(guards = {"isPositive(arguments[1])"})
        public Object leftShiftWithOverflow(long a, int b) {
            if (canShiftIntoLong(a, b)) {
                return leftShiftToLong(a, b);
            } else {
                return lower(bignum(a).shiftLeft(b));
            }
        }

        @Specialization(guards = "isStrictlyNegative(arguments[1])")
        public long leftShiftNeg(long a, int b) {
            if (-b >= Integer.SIZE) {
                return 0;
            } else {
                return a >> -b;
            }
        }

        @Specialization(guards = {"!isInteger(arguments[1])", "!isLong(arguments[1])"})
        public Object leftShiftFallback(VirtualFrame frame, Object a, Object b) {
            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return fallbackCallNode.call(frame, a, "left_shift_fallback", null, b);
        }

        static boolean canShiftIntoInt(int a, int b) {
            return Integer.numberOfLeadingZeros(a) - b > 0;
        }

        static boolean canShiftIntoLong(int a, int b) {
            return canShiftIntoLong((long) a, b);
        }

        static boolean canShiftIntoLong(long a, int b) {
            return Long.numberOfLeadingZeros(a) - b > 0;
        }

        static boolean isPositive(int value) {
            return value >= 0;
        }

        static boolean isStrictlyNegative(int value) {
            return value < 0;
        }

    }

    @CoreMethod(names = ">>", required = 1)
    public abstract static class RightShiftNode extends CoreMethodNode {

        @Child private CallDispatchHeadNode fallbackCallNode;
        @Child private LeftShiftNode leftShiftNode;

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RightShiftNode(RightShiftNode prev) {
            super(prev);
            fallbackCallNode = prev.fallbackCallNode;
            leftShiftNode = prev.leftShiftNode;
        }

        protected abstract Object executeRightShift(VirtualFrame frame, Object a, Object b);

        @Specialization
        public Object rightShift(VirtualFrame frame, int a, int b) {
            if (b > 0) {
                if (b >= BITS - 1) {
                    if (a < 0) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    return a >> b;
                }
            } else {
                if (leftShiftNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    leftShiftNode = insert(FixnumNodesFactory.LeftShiftNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null}));
                }

                return leftShiftNode.executeLeftShift(frame, a, -b);
            }
        }

        @Specialization
        public Object rightShift(VirtualFrame frame, long a, int b) {
            if (b > 0) {
                if (b >= BITS - 1) {
                    if (a < 0) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    return a >> b;
                }
            } else {
                if (leftShiftNode == null) {
                    CompilerDirectives.transferToInterpreter();
                    leftShiftNode = insert(FixnumNodesFactory.LeftShiftNodeFactory.create(getContext(), getSourceSection(), new RubyNode[]{null, null}));
                }

                return leftShiftNode.executeLeftShift(frame, a, -b);
            }
        }

        @Specialization
        public int rightShift(int a, RubyBignum b) {
            return 0;
        }

        @Specialization
        public int rightShift(long a, RubyBignum b) {
            return 0;
        }

        @Specialization(guards = {"!isInteger(arguments[1])", "!isLong(arguments[1])"})
        public Object rightShiftFallback(VirtualFrame frame, Object a, Object b) {
            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return fallbackCallNode.call(frame, a, "right_shift_fallback", null, b);
        }

    }

    @CoreMethod(names = { "abs", "magnitude" })
    public abstract static class AbsNode extends CoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public AbsNode(AbsNode prev) {
            super(prev);
        }

        @Specialization
        public int abs(int n) {
            return Math.abs(n);
        }

        @Specialization
        public long abs(long n) {
            return Math.abs(n);
        }

    }

    @CoreMethod(names = "bit_length")
    public abstract static class BitLengthNode extends CoreMethodNode {

        private static final int INTEGER_BITS = Integer.numberOfLeadingZeros(0);
        private static final int LONG_BITS = Long.numberOfLeadingZeros(0);

        public BitLengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public BitLengthNode(BitLengthNode prev) {
            super(prev);
        }

        @Specialization
        public int bitLength(int n) {
            return bitLength((long) n);
        }

        @Specialization
        public int bitLength(long n) {
            if (n < 0) {
                n = ~n;
            }

            if (n == Long.MAX_VALUE) {
                return LONG_BITS - 1;
            }

            return LONG_BITS - Long.numberOfLeadingZeros(n);
        }

    }

    @CoreMethod(names = "floor")
    public abstract static class FloorNode extends CoreMethodNode {

        public FloorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FloorNode(FloorNode prev) {
            super(prev);
        }

        @Specialization
        public int floor(int n) {
            return n;
        }

        @Specialization
        public long floor(long n) {
            return n;
        }

    }

    @CoreMethod(names = "inspect")
    public abstract static class InspectNode extends CoreMethodNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public InspectNode(InspectNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString inspect(int n) {
            return getContext().makeString(Integer.toString(n));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString inspect(long n) {
            return getContext().makeString(Long.toString(n));
        }

    }

    @CoreMethod(names = "size", needsSelf = false)
    public abstract static class SizeNode extends CoreMethodNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SizeNode(SizeNode prev) {
            super(prev);
        }

        @Specialization
        public int size() {
            return Long.SIZE / Byte.SIZE;
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToFNode(ToFNode prev) {
            super(prev);
        }

        @Specialization
        public double toF(int n) {
            return n;
        }

        @Specialization
        public double toF(long n) {
            return n;
        }

    }

    @CoreMethod(names = "to_s", optional = 1)
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(int n, UndefinedPlaceholder undefined) {
            return getContext().makeString(Integer.toString(n));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(long n, UndefinedPlaceholder undefined) {
            return getContext().makeString(Long.toString(n));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(int n, int base) {
            return toS((long) n, base);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(long n, int base) {
            if (base < 2 || base > 36) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().argumentErrorInvalidRadix(base, this));
            }

            return getContext().makeString(Long.toString(n, base));
        }

    }

    @CoreMethod(names = "zero?")
    public abstract static class ZeroNode extends CoreMethodNode {

        public ZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ZeroNode(ZeroNode prev) {
            super(prev);
        }

        @Specialization
        public boolean zero(int n) {
            return n == 0;
        }

        @Specialization
        public boolean zero(long n) {
            return n == 0;
        }

    }

}
