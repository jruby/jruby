/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core.fixnum;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.ExactMath;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.BranchProfile;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyString;

import java.math.BigInteger;

@CoreClass(name = "Fixnum")
public abstract class FixnumNodes {

    private static final int BITS = 64;

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends CoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignumNode;

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int neg(int value) {
            return ExactMath.subtractExact(0, value);
        }

        @Specialization(contains = "neg")
        public Object negWithOverflow(int value) {
            if (value == Integer.MIN_VALUE) {
                return -((long) value);
            }
            return -value;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long neg(long value) {
            return ExactMath.subtractExact(0, value);
        }

        @Specialization
        public Object negWithOverflow(long value) {
            if (fixnumOrBignumNode == null) {
                CompilerDirectives.transferToInterpreter();
                fixnumOrBignumNode = insert(new FixnumOrBignumNode(getContext(), getSourceSection()));
            }

            return fixnumOrBignumNode.fixnumOrBignum(BigInteger.valueOf(value).negate());
        }

    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddNode extends BignumNodes.BignumCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
            return fixnumOrBignum(BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object add(int a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).add(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object addCoerced(VirtualFrame frame, int a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :+, b", "b", b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(long a, int b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(long a, int b) {
            return fixnumOrBignum(BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long add(long a, long b) {
            return ExactMath.addExact(a, b);
        }

        @Specialization
        public Object addWithOverflow(long a, long b) {
            return fixnumOrBignum(BigInteger.valueOf(a).add(BigInteger.valueOf(b)));
        }

        @Specialization
        public double add(long a, double b) {
            return a + b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object add(long a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).add(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object addCoerced(VirtualFrame frame, long a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :+, b", "b", b);
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubNode extends BignumNodes.BignumCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
            return fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object sub(int a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).subtract(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization
        public double sub(int a, double b) {
            return a - b;
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object subCoerced(VirtualFrame frame, int a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :-, b", "b", b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(long a, int b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(long a, int b) {
            return fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long sub(long a, long b) {
            return ExactMath.subtractExact(a, b);
        }

        @Specialization
        public Object subWithOverflow(long a, long b) {
            return fixnumOrBignum(BigInteger.valueOf(a).subtract(BigInteger.valueOf(b)));
        }

        @Specialization
        public double sub(long a, double b) {
            return a - b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object sub(long a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).subtract(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object subCoerced(VirtualFrame frame, long a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :-, b", "b", b);
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MulNode extends BignumNodes.BignumCoreMethodNode {

        public MulNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @TruffleBoundary
        @Specialization
        public Object mulWithOverflow(int a, long b) {
            return fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(int a, double b) {
            return a * b;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyBignum(b)")
        public Object mul(int a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).multiply(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object mulCoerced(VirtualFrame frame, int a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :*, b", "b", b);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long mul(long a, int b) {
            return ExactMath.multiplyExact(a, b);
        }

        @TruffleBoundary
        @Specialization
        public Object mulWithOverflow(long a, int b) {
            return fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long mul(long a, long b) {
            return ExactMath.multiplyExact(a, b);
        }

        @TruffleBoundary
        @Specialization
        public Object mulWithOverflow(long a, long b) {
            return fixnumOrBignum(BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)));
        }

        @Specialization
        public double mul(long a, double b) {
            return a * b;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyBignum(b)")
        public Object mul(long a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).multiply(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object mulCoerced(VirtualFrame frame, long a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :*, b", "b", b);
        }
    }

    @CoreMethod(names = {"/", "__slash__"}, required = 1)
    public abstract static class DivNode extends CoreMethodArrayArgumentsNode {

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

        @Specialization(guards = "isRubyBignum(b)")
        public int div(int a, RubyBasicObject b) {
            return 0;
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object divCoerced(VirtualFrame frame, int a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :/, b", "b", b);
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

        @Specialization(guards = "isRubyBignum(b)")
        public int div(long a, RubyBasicObject b) {
            // TODO(CS): not entirely sure this is correct
            return 0;
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object divCoerced(VirtualFrame frame, long a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :/, b", "b", b);
        }

    }

    @CoreMethod(names = "%", required = 1)
    public abstract static class ModNode extends BignumNodes.BignumCoreMethodNode {

        private final BranchProfile adjustProfile = BranchProfile.create();

        public ModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization(guards = "isRubyBignum(b)")
        public Object mod(int a, RubyBasicObject b) {
            return mod((long) a, b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object mod(long a, RubyBasicObject b) {
            CompilerDirectives.transferToInterpreter();

            // TODO(CS): why are we getting this case?

            long mod = BigInteger.valueOf(a).mod(BignumNodes.getBigIntegerValue(b)).longValue();

            if (mod < 0 && BignumNodes.getBigIntegerValue(b).compareTo(BigInteger.ZERO) > 0 || mod > 0 && BignumNodes.getBigIntegerValue(b).compareTo(BigInteger.ZERO) < 0) {
                adjustProfile.enter();
                return new RubyBignum(getContext().getCoreLibrary().getBignumClass(), BigInteger.valueOf(mod).add(BignumNodes.getBigIntegerValue(b)));
            }

            return mod;
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
        public RubyArray divMod(int a, int b) {
            return divModNode.execute(a, b);
        }

        @Specialization
        public RubyArray divMod(int a, long b) {
            return divModNode.execute(a, b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public RubyArray divMod(int a, RubyBasicObject b) {
            return divModNode.execute(a, BignumNodes.getBigIntegerValue(b));
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

        @Specialization(guards = "isRubyBignum(b)")
        public RubyArray divMod(long a, RubyBasicObject b) {
            return divModNode.execute(a, BignumNodes.getBigIntegerValue(b));
        }

        @Specialization
        public RubyArray divMod(long a, double b) {
            return divModNode.execute(a, b);
        }

    }

    @CoreMethod(names = "<", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class LessNode extends CoreMethodArrayArgumentsNode {

        public LessNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean less(int a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) < 0;
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean less(long a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) < 0;
        }

        @Specialization(guards = {"!isRubyBignum(b)", "!isInteger(b)", "!isLong(b)", "!isDouble(b)"})
        public Object less(VirtualFrame frame, int a, Object b) {
            return ruby(frame, "b, a = math_coerce other, :compare_error; a < b", "other", b);
        }

        @Specialization(guards = {"!isRubyBignum(b)", "!isInteger(b)", "!isLong(b)", "!isDouble(b)"})
        public Object less(VirtualFrame frame, long a, Object b) {
            return ruby(frame, "b, a = math_coerce other, :compare_error; a < b", "other", b);
        }

    }

    @CoreMethod(names = "<=", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class LessEqualNode extends CoreMethodArrayArgumentsNode {

        public LessEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean lessEqual(int a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) <= 0;
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean lessEqual(long a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) <= 0;
        }
    }

    @CoreMethod(names = {"==", "==="}, required = 1)
    public abstract static class EqualNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode reverseCallNode;

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            reverseCallNode = DispatchHeadNodeFactory.createMethodCall(context);
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean equal(int a, RubyBasicObject b) {
            return BigInteger.valueOf(a).equals(BignumNodes.getBigIntegerValue(b));
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean equal(long a, RubyBasicObject b) {
            return BigInteger.valueOf(a).equals(BignumNodes.getBigIntegerValue(b));
        }

        @Specialization(guards = {
                "!isInteger(b)",
                "!isLong(b)",
                "!isRubyBignum(b)"
        })
        public Object equal(VirtualFrame frame, Object a, Object b) {
            return reverseCallNode.call(frame, b, "==", null, a);
        }

    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends CoreMethodArrayArgumentsNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization(guards = "isRubyBignum(b)")
        public int compare(int a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b));
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

        @Specialization(guards = "isRubyBignum(b)")
        public int compare(long a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b));
        }

        @Specialization(guards = {
            "!isInteger(b)",
            "!isLong(b)",
            "!isDouble(b)",
            "!isRubyBignum(b)"})
        public Object compare(VirtualFrame frame, Object a, Object b) {
            return ruby(frame, "begin; b, a = math_coerce(other, :compare_error); a <=> b; rescue ArgumentError; nil; end", "other", b);
        }

    }

    @CoreMethod(names = ">=", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class GreaterEqualNode extends CoreMethodArrayArgumentsNode {

        public GreaterEqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greaterEqual(int a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) >= 0;
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greaterEqual(long a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) >= 0;
        }
    }

    @CoreMethod(names = ">", required = 1, unsupportedOperationBehavior = UnsupportedOperationBehavior.ARGUMENT_ERROR)
    public abstract static class GreaterNode extends CoreMethodArrayArgumentsNode {

        public GreaterNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greater(int a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)
            ) > 0;
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

        @Specialization(guards = "isRubyBignum(b)")
        public boolean greater(long a, RubyBasicObject b) {
            return BigInteger.valueOf(a).compareTo(BignumNodes.getBigIntegerValue(b)) > 0;
        }

    }

    @CoreMethod(names = "~")
    public abstract static class ComplementNode extends CoreMethodArrayArgumentsNode {

        public ComplementNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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

        @Specialization
        public int bitAnd(int a, int b) {
            return a & b;
        }

        @Specialization
        public long bitAnd(int a, long b) {
            return a & b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitAnd(int a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).and(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization
        public long bitAnd(long a, int b) {
            return a & b;
        }

        @Specialization
        public long bitAnd(long a, long b) {
            return a & b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitAnd(long a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).and(BignumNodes.getBigIntegerValue(b)));
        }
    }

    @CoreMethod(names = "|", required = 1)
    public abstract static class BitOrNode extends BignumNodes.BignumCoreMethodNode {

        public BitOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int bitOr(int a, int b) {
            return a | b;
        }

        @Specialization
        public long bitOr(int a, long b) {
            return a | b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitOr(int a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).or(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization
        public long bitOr(long a, int b) {
            return a | b;
        }

        @Specialization
        public long bitOr(long a, long b) {
            return a | b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitOr(long a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).or(BignumNodes.getBigIntegerValue(b)));
        }
    }

    @CoreMethod(names = "^", required = 1)
    public abstract static class BitXOrNode extends BignumNodes.BignumCoreMethodNode {

        public BitXOrNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int bitXOr(int a, int b) {
            return a ^ b;
        }

        @Specialization
        public long bitXOr(int a, long b) {
            return a ^ b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitXOr(int a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).xor(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization
        public long bitXOr(long a, int b) {
            return a ^ b;
        }

        @Specialization
        public long bitXOr(long a, long b) {
            return a ^ b;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public Object bitXOr(long a, RubyBasicObject b) {
            return fixnumOrBignum(BigInteger.valueOf(a).xor(BignumNodes.getBigIntegerValue(b)));
        }

        @Specialization(guards = "!isRubyBignum(b)")
        public Object bitXOr(VirtualFrame frame, Object a, RubyBasicObject b) {
            return ruby(frame, "a ^ Rubinius::Type.coerce_to_bitwise_operand(b)", "a", a, "b", b);
        }

    }

    @CoreMethod(names = "<<", required = 1)
    public abstract static class LeftShiftNode extends BignumNodes.BignumCoreMethodNode {

        @Child private CallDispatchHeadNode fallbackCallNode;

        public LeftShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        protected Object lower(BigInteger value) {
            return fixnumOrBignum(value);
        }

        public abstract Object executeLeftShift(VirtualFrame frame, Object a, Object b);

        @Specialization(guards = {"isPositive(b)", "canShiftIntoInt(a, b)"})
        public int leftShift(int a, int b) {
            return a << b;
        }

        @Specialization(guards = {"isPositive(b)", "canShiftIntoInt(a, b)"})
        public int leftShift(int a, long b) {
            return a << b;
        }

        @Specialization(guards = {"isPositive(b)", "canShiftIntoLong(a, b)"})
        public long leftShiftToLong(int a, int b) {
            return leftShiftToLong((long) a, b);
        }

        @Specialization(guards = {"isPositive(b)"})
        public Object leftShiftWithOverflow(int a, int b) {
            return leftShiftWithOverflow((long) a, b);
        }

        @Specialization(guards = "isStrictlyNegative(b)")
        public int leftShiftNeg(int a, int b) {
            if (-b >= Integer.SIZE) {
                return 0;
            } else if (b == Integer.MIN_VALUE) {
                return 0;
            } else {
                return a >> -b;
            }
        }

        @Specialization(guards = {"isPositive(b)", "canShiftIntoLong(a, b)"})
        public long leftShiftToLong(long a, int b) {
            return a << b;
        }

        @Specialization(guards = {"isPositive(b)"})
        public Object leftShiftWithOverflow(long a, int b) {
            if (canShiftIntoLong(a, b)) {
                return leftShiftToLong(a, b);
            } else {
                return lower(BigInteger.valueOf(a).shiftLeft(b));
            }
        }

        @Specialization(guards = "isStrictlyNegative(b)")
        public long leftShiftNeg(long a, int b) {
            if (-b >= Integer.SIZE) {
                return 0;
            } else {
                return a >> -b;
            }
        }

        @Specialization(guards = {"!isInteger(b)", "!isLong(b)"})
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

        static boolean canShiftIntoInt(int a, long b) {
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

        static boolean isPositive(long value) {
            return value >= 0;
        }

        static boolean isStrictlyNegative(int value) {
            return value < 0;
        }

    }

    @CoreMethod(names = ">>", required = 1)
    public abstract static class RightShiftNode extends CoreMethodArrayArgumentsNode {

        @Child private CallDispatchHeadNode fallbackCallNode;
        @Child private LeftShiftNode leftShiftNode;

        public RightShiftNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
        public Object rightShift(VirtualFrame frame, int a, long b) {
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

        @Specialization(guards = "isRubyBignum(b)")
        public int rightShift(int a, RubyBasicObject b) {
            return 0;
        }

        @Specialization(guards = "isRubyBignum(b)")
        public int rightShift(long a, RubyBasicObject b) {
            return 0;
        }

        @Specialization(guards = {"!isInteger(b)", "!isLong(b)"})
        public Object rightShiftFallback(VirtualFrame frame, Object a, Object b) {
            if (fallbackCallNode == null) {
                CompilerDirectives.transferToInterpreter();
                fallbackCallNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            return fallbackCallNode.call(frame, a, "right_shift_fallback", null, b);
        }

    }

    @CoreMethod(names = {"abs", "magnitude"})
    public abstract static class AbsNode extends CoreMethodArrayArgumentsNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public int absIntInBounds(int n) {
            return (n < 0) ? ExactMath.subtractExact(0, n) : n;
        }

        @Specialization(contains = "absIntInBounds")
        public Object abs(int n) {
            if (n == Integer.MIN_VALUE) {
                return -((long) n);
            }
            return (n < 0) ? -n : n;
        }

        @Specialization(rewriteOn = ArithmeticException.class)
        public long absInBounds(long n) {
            return (n < 0) ? ExactMath.subtractExact(0, n) : n;
        }

        @Specialization(contains = "absInBounds")
        public Object abs(long n) {
            if (n == Long.MIN_VALUE) {
                return new RubyBignum(getContext().getCoreLibrary().getBignumClass(), BigInteger.valueOf(n).abs());
            }
            return (n < 0) ? -n : n;
        }

    }

    @CoreMethod(names = "bit_length")
    public abstract static class BitLengthNode extends CoreMethodArrayArgumentsNode {

        private static final int INTEGER_BITS = Integer.numberOfLeadingZeros(0);
        private static final int LONG_BITS = Long.numberOfLeadingZeros(0);

        public BitLengthNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class FloorNode extends CoreMethodArrayArgumentsNode {

        public FloorNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class InspectNode extends CoreMethodArrayArgumentsNode {

        public InspectNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class SizeNode extends CoreMethodArrayArgumentsNode {

        public SizeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public int size() {
            return Long.SIZE / Byte.SIZE;
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends CoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
    public abstract static class ZeroNode extends CoreMethodArrayArgumentsNode {

        public ZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
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
