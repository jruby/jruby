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

import java.math.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.utilities.*;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.util.RuntimeBigInteger;

@CoreClass(name = "Fixnum")
public abstract class FixnumNodes {

    @CoreMethod(names = "+@")
    public abstract static class PosNode extends CoreMethodNode {

        public PosNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public PosNode(PosNode prev) {
            super(prev);
        }

        @Specialization
        public int pos(int value) {
            return value;
        }

        @Specialization
        public long pos(long value) {
            return value;
        }

    }

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
        public BigInteger negWithOverflow(long value) {
            return BigInteger.valueOf(value).negate();
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public AddNode(AddNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
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
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.add(BigInteger.valueOf(a), BigInteger.valueOf(b)));
        }

        @Specialization
        public Object add(int a, BigInteger b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.add(BigInteger.valueOf(a), b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(long a, int b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(long a, int b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.add(BigInteger.valueOf(a), BigInteger.valueOf(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(long a, long b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(long a, long b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.add(BigInteger.valueOf(a), BigInteger.valueOf(b)));
        }

        @Specialization
        public double add(long a, double b) {
            return a + b;
        }

        @Specialization
        public Object add(long a, BigInteger b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.add(BigInteger.valueOf(a), b));
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public SubNode(SubNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int sub(int a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public long subWithOverflow(int a, int b) {
            return (long) a - (long) b;
        }

        @Specialization
        public double sub(int a, double b) {
            return a - b;
        }

        @Specialization
        public long sub(int a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object sub(int a, BigInteger b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.subtract(BigInteger.valueOf(a), b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(long a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(long a, int b) {
            return fixnumOrBignum.fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(long a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(long a, long b) {
            return fixnumOrBignum.fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization
        public double sub(long a, double b) {
            return a - b;
        }

        @Specialization
        public Object sub(long a, BigInteger b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.subtract(BigInteger.valueOf(a), b));
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public MulNode(MulNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
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
            return fixnumOrBignum.fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(int a, double b) {
            return a * b;
        }

        @Specialization
        public Object mul(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.multiply(BigInteger.valueOf(a), b));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long mul(long a, int b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization
        public Object mulWithOverflow(long a, int b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.multiply(BigInteger.valueOf(a), BigInteger.valueOf(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long mul(long a, long b) {
            return ExactMath.multiplyExact(a, b);
        }

        @Specialization
        public Object mulWithOverflow(long a, long b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.multiply(BigInteger.valueOf(a), BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(long a, double b) {
            return a * b;
        }

        @Specialization
        public Object mul(long a, BigInteger b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.multiply(BigInteger.valueOf(a), b));
        }

    }

    @CoreMethod(names = "**", required = 1, lowerFixnumSelf = true, lowerFixnumParameters = 0)
    public abstract static class PowNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        public PowNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public PowNode(PowNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization(guards = "canShiftIntoInt")
        public int pow2(int a, int b) {
            return 1 << b;
        }

        @Specialization
        public Object pow(int a, int b) {
            return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.pow(BigInteger.valueOf(a), b));
        }

        @Specialization
        public double pow(int a, double b) {
            return Math.pow(a, b);
        }

        @Specialization
        public Object pow(int a, BigInteger b) {
            notDesignedForCompilation();

            final BigInteger bigA = BigInteger.valueOf(a);

            BigInteger result = BigInteger.ONE;

            for (BigInteger n = BigInteger.ZERO; b.compareTo(n) < 0; n = n.add(BigInteger.ONE)) {
                result = result.multiply(bigA);
            }

            return result;
        }

        protected static boolean canShiftIntoInt(int a, int b) {
            // Highest bit we can set is the 30th due to sign
            return a == 2 && b <= 30;
        }

    }

    @CoreMethod(names = "/", required = 1)
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
                    throw new UnexpectedResultException(BigInteger.valueOf(a).negate());
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
                    return BigInteger.valueOf(a).negate();
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
        public int div(@SuppressWarnings("unused") int a, @SuppressWarnings("unused") BigInteger b) {
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
                    throw new UnexpectedResultException(BigInteger.valueOf(a).negate());
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
                    return BigInteger.valueOf(a).negate();
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
        public int div(@SuppressWarnings("unused") long a, @SuppressWarnings("unused") BigInteger b) {
            // TODO(CS): not entirely sure this is correct
            return 0;
        }
    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends CoreMethodNode {

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
        public Object mod(int a, BigInteger b) {
            return mod(BigInteger.valueOf(a), b);
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
        public Object mod(long a, BigInteger b) {
            return mod(BigInteger.valueOf(a), b);
        }

        public Object mod(BigInteger a, BigInteger b) {
            notDesignedForCompilation();
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.mod(a, b));
        }
    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends CoreMethodNode {

        @Child protected GeneralDivModNode divModNode;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            divModNode = new GeneralDivModNode(context);
        }

        public DivModNode(DivModNode prev) {
            super(prev);
            divModNode = new GeneralDivModNode(getContext());
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
        public RubyArray divMod(int a, BigInteger b) {
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
        public RubyArray divMod(long a, BigInteger b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "<", required = 1)
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
        public boolean less(int a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) < 0;
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
        public boolean less(long a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) < 0;
        }
    }

    @CoreMethod(names = "<=", required = 1)
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
        public boolean lessEqual(int a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) <= 0;
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
        public boolean lessEqual(long a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) <= 0;
        }
    }

    @CoreMethod(names = {"==", "==="}, required = 1)
    public abstract static class EqualNode extends CoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public EqualNode(EqualNode prev) {
            super(prev);
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
        public boolean equal(int a, BigInteger b) {
            return BigInteger.valueOf(a).equals(b);
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
        public boolean equal(long a, BigInteger b) {
            return BigInteger.valueOf(a).equals(b);
        }

        @Fallback
        public boolean equal(Object a, Object b) {
            return false;
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
        public int compare(int a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b);
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
        public int compare(long a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b);
        }
    }

    @CoreMethod(names = ">=", required = 1)
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
        public boolean greaterEqual(int a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) >= 0;
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
        public boolean greaterEqual(long a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) >= 0;
        }
    }

    @CoreMethod(names = ">", required = 1)
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
        public boolean greater(int a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) > 0;
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
        public boolean greater(long a, BigInteger b) {
            return RuntimeBigInteger.compareTo(BigInteger.valueOf(a), b) > 0;
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
    public abstract static class BitAndNode extends CoreMethodNode {

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
        public Object bitAnd(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.and(BigInteger.valueOf(a), b));
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
        public Object bitAnd(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.and(BigInteger.valueOf(a), b));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends CoreMethodNode {

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
        public Object bitOr(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.or(BigInteger.valueOf(a), b));
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
        public Object bitOr(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.or(BigInteger.valueOf(a), b));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends CoreMethodNode {

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
        public Object bitXOr(int a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.xor(BigInteger.valueOf(a), b));
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
        public Object bitXOr(long a, BigInteger b) {
            return RubyFixnum.fixnumOrBignum(RuntimeBigInteger.xor(BigInteger.valueOf(a), b));
        }
    }

    @CoreMethod(names = "<<", required = 1, lowerFixnumParameters = 0)
    public abstract static class LeftShiftNode extends CoreMethodNode {

        @Child protected FixnumOrBignumNode fixnumOrBignum;

        private final BranchProfile bAboveZeroProfile = BranchProfile.create();
        private final BranchProfile bNotAboveZeroProfile = BranchProfile.create();
        private final BranchProfile useBignumProfile = BranchProfile.create();

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode();
        }

        public LeftShiftNode(LeftShiftNode prev) {
            super(prev);
            fixnumOrBignum = prev.fixnumOrBignum;
        }

        @Specialization(guards = "canShiftIntoInt")
        public int leftShift(int a, int b) {
            if (b > 0) {
                bAboveZeroProfile.enter();

                return a << b;
            } else {
                bNotAboveZeroProfile.enter();
                if (-b >= Integer.SIZE) {
                    return 0;
                } else {
                    return a >> -b;
                }
            }
        }

        @Specialization
        public Object leftShiftWithOverflow(int a, int b) {
            return leftShiftWithOverflow((long) a, b);
        }

        @Specialization(guards = "canShiftIntoLong")
        public long leftShift(long a, int b) {
            if (b > 0) {
                bAboveZeroProfile.enter();

                return a << b;
            } else {
                bNotAboveZeroProfile.enter();

                if (-b >= Long.SIZE) {
                    return 0;
                } else {
                    return a >> -b;
                }
            }
        }

        @Specialization
        public Object leftShiftWithOverflow(long a, int b) {
            if (b > 0) {
                bAboveZeroProfile.enter();

                if (canShiftIntoLong(a, b)) {
                    return a << b;
                } else {
                    useBignumProfile.enter();
                    return fixnumOrBignum.fixnumOrBignum(RuntimeBigInteger.shiftLeft(BigInteger.valueOf(a), b));
                }
            } else {
                bNotAboveZeroProfile.enter();

                if (-b >= Long.SIZE) {
                    return 0;
                } else {
                    return a >> -b;
                }
            }
        }

        static boolean canShiftIntoInt(int a, int b) {
            return Integer.numberOfLeadingZeros(a) - b > 0;
        }

        static boolean canShiftIntoLong(long a, int b) {
            return Long.numberOfLeadingZeros(a) - b > 0;
        }

    }

    @CoreMethod(names = ">>", required = 1, lowerFixnumParameters = 0)
    public abstract static class RightShiftNode extends CoreMethodNode {

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public RightShiftNode(RightShiftNode prev) {
            super(prev);
        }

        @Specialization
        public int rightShift(int a, int b) {
            if (b > 0) {
                return a >> b;
            } else {
                if (-b >= Long.SIZE) {
                    return 0;
                } else {
                    return a << -b;
                }
            }
        }

        @Specialization
        public long rightShift(long a, int b) {
            if (b > 0) {
                return a >> b;
            } else {
                if (-b >= Long.SIZE) {
                    return 0;
                } else {
                    return a << -b;
                }
            }
        }

    }

    @CoreMethod(names = "[]", required = 1)
    public abstract static class GetIndexNode extends CoreMethodNode {

        public GetIndexNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GetIndexNode(GetIndexNode prev) {
            super(prev);
        }

        @Specialization
        public int getIndex(int self, int index) {
            notDesignedForCompilation();

            if ((self & (1 << index)) == 0) {
                return 0;
            } else {
                return 1;
            }
        }

    }

    @CoreMethod(names = "abs")
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

    @CoreMethod(names = "floor")
    public abstract static class FloorNode extends CoreMethodNode {

        public FloorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public FloorNode(FloorNode prev) {
            super(prev);
        }

        @Specialization
        public int abs(int n) {
            return n;
        }

        @Specialization
        public long abs(long n) {
            return n;
        }

    }

    @CoreMethod(names = "chr")
    public abstract static class ChrNode extends CoreMethodNode {

        public ChrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ChrNode(ChrNode prev) {
            super(prev);
        }

        @Specialization
        public RubyString chr(int n) {
            notDesignedForCompilation();

            // TODO(CS): not sure about encoding here
            return getContext().makeString((char) n);
        }

    }

    @CoreMethod(names = "nonzero?")
    public abstract static class NonZeroNode extends CoreMethodNode {

        public NonZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public NonZeroNode(NonZeroNode prev) {
            super(prev);
        }

        @Specialization
        public Object nonZero(int value) {
            if (value == 0) {
                return false;
            } else {
                return value;
            }
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
            return Integer.SIZE / Byte.SIZE;
        }

    }

    @CoreMethod(names = "step", needsBlock = true, required = 2)
    public abstract static class StepNode extends YieldingCoreMethodNode {

        public StepNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public StepNode(StepNode prev) {
            super(prev);
        }

        @Specialization
        public RubyNilClass step(VirtualFrame frame, int from, int to, int step, RubyProc block) {
            for (int i = from; i <= to; i += step) {
                yield(frame, block, i);
            }

            return getContext().getCoreLibrary().getNilObject();
        }

    }

    @CoreMethod(names = "times", needsBlock = true)
    public abstract static class TimesNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public TimesNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TimesNode(TimesNode prev) {
            super(prev);
        }

        @Specialization
        public Object times(VirtualFrame frame, int n, RubyProc block) {
            int count = 0;

            try {
                outer: for (int i = 0; i < n; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
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

            return n;
        }

        @Specialization
        public RubyArray times(VirtualFrame frame, int n, UndefinedPlaceholder block) {
            notDesignedForCompilation();

            final int[] array = new int[n];

            for (int i = 0; i < n; i++) {
                array[i] = i;
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), array, n);
        }

        @Specialization
        public Object times(VirtualFrame frame, long n, RubyProc block) {
            int count = 0;

            try {
                outer: for (long i = 0; i < n; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
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

            return n;
        }

    }

    @CoreMethod(names = {"to_i", "to_int"})
    public abstract static class ToINode extends CoreMethodNode {

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToINode(ToINode prev) {
            super(prev);
        }

        @Specialization
        public int toI(int n) {
            return n;
        }

        @Specialization
        public long toI(long n) {
            return n;
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

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ToSNode(ToSNode prev) {
            super(prev);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(int n) {
            return getContext().makeString(Integer.toString(n));
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(long n) {
            return getContext().makeString(Long.toString(n));
        }

    }

    @CoreMethod(names = "upto", needsBlock = true, required = 1)
    public abstract static class UpToNode extends YieldingCoreMethodNode {

        private final BranchProfile breakProfile = BranchProfile.create();
        private final BranchProfile nextProfile = BranchProfile.create();
        private final BranchProfile redoProfile = BranchProfile.create();

        public UpToNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public UpToNode(UpToNode prev) {
            super(prev);
        }

        @Specialization
        public Object upto(VirtualFrame frame, int from, int to, RubyProc block) {
            int count = 0;

            try {
                outer:
                for (int i = from; i <= to; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
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

            return getContext().getCoreLibrary().getNilObject();
        }

        @Specialization
        public Object upto(VirtualFrame frame, long from, long to, RubyProc block) {
            notDesignedForCompilation();

            int count = 0;

            try {
                outer:
                for (long i = from; i <= to; i++) {
                    while (true) {
                        if (CompilerDirectives.inInterpreter()) {
                            count++;
                        }

                        try {
                            yield(frame, block, i);
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

            return getContext().getCoreLibrary().getNilObject();
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
