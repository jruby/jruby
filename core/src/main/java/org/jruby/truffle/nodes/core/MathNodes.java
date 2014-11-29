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
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.dsl.*;
import org.jruby.RubyMath;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.UseMethodMissingException;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyArray;
import org.jruby.truffle.runtime.util.SlowPathBigInteger;

@CoreClass(name = "Math")
public abstract class MathNodes {

    @CoreMethod(names = "acos", isModuleFunction = true, required = 1)
    public abstract static class ACosNode extends SimpleMonadicMathFunction {

        public ACosNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ACosNode(ACosNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            if (a < -1.0 || a > 1.0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("acos", this));
            }

            return Math.acos(a);
        }

    }

    @CoreMethod(names = "acosh", isModuleFunction = true, required = 1)
    public abstract static class ACosHNode extends SimpleMonadicMathFunction {

        public ACosHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ACosHNode(ACosHNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (a < 1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("acosh", this));
            } else if (a < 94906265.62) {
                return Math.log(a + Math.sqrt(a * a - 1.0));
            } else{
                return 0.69314718055994530941723212145818 + Math.log(a);
            }
        }

    }

    @CoreMethod(names = "asin", isModuleFunction = true, required = 1)
    public abstract static class ASinNode extends SimpleMonadicMathFunction {

        public ASinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ASinNode(ASinNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            if (a < -1.0 || a > 1.0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("asin", this));
            }

            return Math.asin(a);
        }

    }

    @CoreMethod(names = "asinh", isModuleFunction = true, required = 1)
    public abstract static class ASinHNode extends SimpleMonadicMathFunction {

        public ASinHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ASinHNode(ASinHNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            final double y = Math.abs(a);

            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (y <= 1.05367e-08) {
                return a;
            } else if (y <= 1.0) {
                return a * (1.0 + RubyMath.chebylevSerie(2.0 * a * a - 1.0, RubyMath.ASINH_COEF));
            } else if (y < 94906265.62) {
                return Math.log(a + Math.sqrt(a * a + 1.0));
            } else {
                double result = 0.69314718055994530941723212145818 + Math.log(y);
                if (a < 0) result *= -1;
                return result;
            }
        }

    }

    @CoreMethod(names = "atan", isModuleFunction = true, required = 1)
    public abstract static class ATanNode extends SimpleMonadicMathFunction {

        public ATanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ATanNode(ATanNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.atan(a);
        }

    }

    @CoreMethod(names = "atan2", isModuleFunction = true, required = 2)
    public abstract static class ATan2Node extends SimpleDyadicMathFunction {

        public ATan2Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ATan2Node(ATan2Node prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a, double b) {
            return Math.atan2(a, b);
        }

    }

    @CoreMethod(names = "atanh", isModuleFunction = true, required = 1)
    public abstract static class ATanHNode extends SimpleMonadicMathFunction {

        public ATanHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ATanHNode(ATanHNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            if (a < -1.0 || a > 1.0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("atanh", this));
            }

            final double y = Math.abs(a);

            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (y < 1.82501e-08) {
                return a;
            } else if (y <= 0.5) {
                return a * (1.0 + RubyMath.chebylevSerie(8.0 * a * a - 1.0, RubyMath.ATANH_COEF));
            } else if (y < 1.0) {
                return 0.5 * Math.log((1.0 + a) / (1.0 - a));
            } else if (y == 1.0) {
                return a * Double.POSITIVE_INFINITY;
            } else {
                return Double.NaN;
            }
        }

    }

    @CoreMethod(names = "cbrt", isModuleFunction = true, required = 1)
    public abstract static class CbRtNode extends SimpleMonadicMathFunction {

        public CbRtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CbRtNode(CbRtNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.cbrt(a);
        }

    }

    @CoreMethod(names = "cos", isModuleFunction = true, required = 1)
    public abstract static class CosNode extends SimpleMonadicMathFunction {

        public CosNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CosNode(CosNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.cos(a);
        }

    }

    @CoreMethod(names = "cosh", isModuleFunction = true, required = 1)
    public abstract static class CosHNode extends SimpleMonadicMathFunction {

        public CosHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public CosHNode(CosHNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.cosh(a);
        }

    }

    @CoreMethod(names = "erf", isModuleFunction = true, required = 1)
    public abstract static class ErfNode extends SimpleMonadicMathFunction {

        public ErfNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ErfNode(ErfNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            final double y = Math.abs(a);

            if (y <= 1.49012e-08) {
                return 2 * a / 1.77245385090551602729816748334;
            } else if (y <= 1) {
                return a * (1 + RubyMath.chebylevSerie(2 * a * a - 1, RubyMath.ERFC_COEF));
            } else if (y < 6.013687357) {
                return RubyMath.sign(1 - ErfcNode.erfc(y), a);
            } else if (Double.isNaN(y)) {
                return Double.NaN;
            } else {
                return RubyMath.sign(1, a);
            }
        }

    }

    @CoreMethod(names = "erfc", isModuleFunction = true, required = 1)
    public abstract static class ErfcNode extends SimpleMonadicMathFunction {

        public ErfcNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ErfcNode(ErfcNode prev) {
            super(prev);
        }

        @Override
        public double doFunction(double a) {
            return erfc(a);
        }

        public static double erfc(double a) {
            // Copied from RubyMath - see copyright notices there

            final double y = Math.abs(a);

            if (a <= -6.013687357) {
                return 2;
            } else if (y < 1.49012e-08) {
                return 1 - 2 * a / 1.77245385090551602729816748334;
            } else {
                double ysq = y*y;
                if (y < 1) {
                    return 1 - a * (1 + RubyMath.chebylevSerie(2 * ysq - 1, RubyMath.ERFC_COEF));
                } else if (y <= 4.0) {
                    double result = Math.exp(-ysq)/y*(0.5+RubyMath.chebylevSerie((8.0 / ysq - 5.0) / 3.0, RubyMath.ERFC2_COEF));
                    if (a < 0) result = 2.0 - result;
                    if (a < 0) result = 2.0 - result;
                    if (a < 0) result = 2.0 - result;
                    return result;
                } else {
                    double result = Math.exp(-ysq) / y * (0.5 + RubyMath.chebylevSerie(8.0 / ysq - 1, RubyMath.ERFCC_COEF));
                    if (a < 0) result = 2.0 - result;
                    return result;
                }
            }
        }

    }

    @CoreMethod(names = "exp", isModuleFunction = true, required = 1)
    public abstract static class ExpNode extends SimpleMonadicMathFunction {

        public ExpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public ExpNode(ExpNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.exp(a);
        }

    }

    @CoreMethod(names = "frexp", isModuleFunction = true, required = 1)
    public abstract static class FrExpNode extends CoreMethodNode {

        @Child protected KernelNodes.IsANode isANode;
        @Child protected DispatchHeadNode floatNode;

        public FrExpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            floatNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        }

        public FrExpNode(FrExpNode prev) {
            super(prev);
            isANode = prev.isANode;
            floatNode = prev.floatNode;
        }

        @Specialization
        public RubyArray frexp(int a) {
            return frexp((double) a);
        }

        @Specialization
        public RubyArray frexp(long a) {
            return frexp((double) a);
        }

        @Specialization
        public RubyArray frexp(BigInteger a) {
            return frexp(SlowPathBigInteger.doubleValue(a));
        }

        @Specialization
        public RubyArray frexp(double a) {
            // Copied from RubyMath - see copyright notices there

            double mantissa = a;
            short sign = 1;
            long exponent = 0;

            if (!Double.isInfinite(mantissa) && mantissa != 0.0) {
                // Make mantissa same sign so we only have one code path.
                if (mantissa < 0) {
                    mantissa = -mantissa;
                    sign = -1;
                }

                // Increase value to hit lower range.
                for (; mantissa < 0.5; mantissa *= 2.0, exponent -=1) { }

                // Decrease value to hit upper range.
                for (; mantissa >= 1.0; mantissa *= 0.5, exponent +=1) { }
            }

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{sign * mantissa, exponent}, 2);
        }

        @Fallback
        public RubyArray frexp(VirtualFrame frame, Object a) {
            if (isANode.executeBoolean(a, getContext().getCoreLibrary().getNumericClass())) {
                try {
                    return frexp(floatNode.callFloat(frame, a, "to_f", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(a).getName(),
                            getContext().getCoreLibrary().getFloatClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        getContext().getCoreLibrary().getLogicalClass(a).getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

    }

    @CoreMethod(names = "gamma", isModuleFunction = true, required = 1)
    public abstract static class GammaNode extends SimpleMonadicMathFunction {

        public GammaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public GammaNode(GammaNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            if (a == -1) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("gamma", this));
            }

            if (Double.isNaN(a)) {
                return Double.NaN;
            }

            if (Double.isInfinite(a)) {
                if (a > 0) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().mathDomainError("gamma", this));
                }
            }

            double result = RubyMath.nemes_gamma(a);

            if (Double.isInfinite(result)) {
                if (a < 0) {
                    result = Double.NaN;
                } else {
                    if (a == 0 && 1 / a < 0) {
                        result = Double.NEGATIVE_INFINITY;
                    } else {
                        result = Double.POSITIVE_INFINITY;
                    }
                }
            }

            if (Double.isNaN(a)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("gamma", this));
            }

            return result;
        }

    }

    @CoreMethod(names = "hypot", isModuleFunction = true, required = 2)
    public abstract static class HypotNode extends SimpleDyadicMathFunction {

        public HypotNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public HypotNode(HypotNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a, double b) {
            return Math.hypot(a, b);
        }

    }

    @CoreMethod(names = "ldexp", isModuleFunction = true, required = 2)
    public abstract static class LdexpNode extends CoreMethodNode {

        @Child protected KernelNodes.IsANode isANode;
        @Child protected DispatchHeadNode floatANode;
        @Child protected DispatchHeadNode integerBNode;

        protected LdexpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            floatANode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
            integerBNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        }

        protected LdexpNode(LdexpNode prev) {
            super(prev);
            isANode = prev.isANode;
            floatANode = prev.floatANode;
            integerBNode = prev.integerBNode;
        }

        @Specialization
        public double function(int a, int b) {
            return function((double) a, b);
        }

        @Specialization
        public double function(int a, long b) {
            return function((double) a, b);
        }

        @Specialization
        public double function(int a, double b) {
            return function((double) a, b);
        }

        @Specialization
        public double function(long a, int b) {
            return function((double) a, b);
        }

        @Specialization
        public double function(long a, long b) {
            return function((double) a, b);
        }

        @Specialization
        public double function(long a, double b) {
            return function((double) a, b);
        }

        @Specialization
        public double function(BigInteger a, int b) {
            return function(SlowPathBigInteger.doubleValue(a), b);
        }

        @Specialization
        public double function(BigInteger a, long b) {
            return function(SlowPathBigInteger.doubleValue(a), b);
        }

        @Specialization
        public double function(BigInteger a, double b) {
            return function(SlowPathBigInteger.doubleValue(a), b);
        }

        @Specialization
        public double function(double a, int b) {
            return function(a, (double) b);
        }

        @Specialization
        public double function(double a, long b) {
            return function(a, (double) b);
        }

        @Specialization
        public double function(double a, double b) {
            if (Double.isNaN(b)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().rangeError("float", Double.toString(b), "integer", this));
            }

            return a * Math.pow(2, b);
        }

        @Fallback
        public double function(VirtualFrame frame, Object a, Object b) {
            if (isANode.executeBoolean(a, getContext().getCoreLibrary().getNumericClass())) {
                try {
                    return function(
                            floatANode.callFloat(frame, a, "to_f", null),
                            integerBNode.callLongFixnum(frame, b, "to_int", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(a).getName(),
                            getContext().getCoreLibrary().getIntegerClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        getContext().getCoreLibrary().getLogicalClass(a).getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

    }



    @CoreMethod(names = "lgamma", isModuleFunction = true, required = 1)
    public abstract static class LGammaNode extends CoreMethodNode {

        @Child protected KernelNodes.IsANode isANode;
        @Child protected DispatchHeadNode floatNode;

        public LGammaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            floatNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        }

        public LGammaNode(LGammaNode prev) {
            super(prev);
            isANode = prev.isANode;
            floatNode = prev.floatNode;
        }

        @Specialization
        public RubyArray lgamma(int a) {
            return lgamma((double) a);
        }

        @Specialization
        public RubyArray lgamma(long a) {
            return lgamma((double) a);
        }

        @Specialization
        public RubyArray lgamma(BigInteger a) {
            return lgamma(SlowPathBigInteger.doubleValue(a));
        }

        @Specialization
        public RubyArray lgamma(double a) {
            // Copied from RubyMath - see copyright notices there

            if (a < 0 && Double.isInfinite(a)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("log2", this));
            }

            final RubyMath.NemesLogGamma l = new RubyMath.NemesLogGamma(a);

            return new RubyArray(getContext().getCoreLibrary().getArrayClass(), new Object[]{l.value, l.sign}, 2);
        }

        @Fallback
        public RubyArray lgamma(VirtualFrame frame, Object a) {
            if (isANode.executeBoolean(a, getContext().getCoreLibrary().getNumericClass())) {
                try {
                    return lgamma(floatNode.callFloat(frame, a, "to_f", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(a).getName(),
                            getContext().getCoreLibrary().getFloatClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        getContext().getCoreLibrary().getLogicalClass(a).getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

    }

    @CoreMethod(names = "log", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class LogNode extends SimpleDyadicMathFunction {

        public LogNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public LogNode(LogNode prev) {
            super(prev);
        }

        @Specialization
        public double function(int a, UndefinedPlaceholder b) {
            return doFunction(a);
        }

        @Specialization
        public double function(long a, UndefinedPlaceholder b) {
            return doFunction(a);
        }

        @Specialization
        public double function(BigInteger a, UndefinedPlaceholder b) {
            return doFunction(SlowPathBigInteger.doubleValue(a));
        }

        @Specialization
        public double function(double a, UndefinedPlaceholder b) {
            return doFunction(a);
        }

        @Specialization
        public double function(VirtualFrame frame, Object a, UndefinedPlaceholder b) {
            if (isANode.executeBoolean(a, getContext().getCoreLibrary().getNumericClass())) {
                try {
                    return doFunction(
                            floatANode.callFloat(frame, a, "to_f", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(a).getName(),
                            getContext().getCoreLibrary().getFloatClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        getContext().getCoreLibrary().getLogicalClass(a).getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

        private double doFunction(double a) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("log", this));
            }

            return Math.log(a);
        }

        @Override
        protected double doFunction(double a, double b) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("log", this));
            }

            return Math.log(a) / Math.log(b);
        }

    }

    @CoreMethod(names = "log10", isModuleFunction = true, required = 1)
    public abstract static class Log10Node extends SimpleMonadicMathFunction {

        public Log10Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public Log10Node(Log10Node prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("log10", this));
            }

            return Math.log10(a);
        }

    }

    @CoreMethod(names = "log2", isModuleFunction = true, required = 1)
    public abstract static class Log2Node extends SimpleMonadicMathFunction {

        private final double LOG2 = Math.log(2);

        public Log2Node(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public Log2Node(Log2Node prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().mathDomainError("log2", this));
            }

            return Math.log(a) / LOG2;
        }

    }

    @CoreMethod(names = "sin", isModuleFunction = true, required = 1)
    public abstract static class SinNode extends SimpleMonadicMathFunction {

        public SinNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SinNode(SinNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.sin(a);
        }

    }

    @CoreMethod(names = "sinh", isModuleFunction = true, required = 1)
    public abstract static class SinHNode extends SimpleMonadicMathFunction {

        public SinHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SinHNode(SinHNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.sinh(a);
        }

    }

    @CoreMethod(names = "tan", isModuleFunction = true, required = 1)
    public abstract static class TanNode extends SimpleMonadicMathFunction {

        public TanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TanNode(TanNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.tan(a);
        }

    }

    @CoreMethod(names = "tanh", isModuleFunction = true, required = 1)
    public abstract static class TanHNode extends SimpleMonadicMathFunction {

        public TanHNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public TanHNode(TanHNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.tanh(a);
        }

    }

    @CoreMethod(names = "sqrt", isModuleFunction = true, required = 1)
    public abstract static class SqrtNode extends SimpleMonadicMathFunction {

        public SqrtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public SqrtNode(SqrtNode prev) {
            super(prev);
        }

        @Override
        protected double doFunction(double a) {
            return Math.sqrt(a);
        }

    }

    protected abstract static class SimpleMonadicMathFunction extends CoreMethodNode {

        @Child protected KernelNodes.IsANode isANode;
        @Child protected DispatchHeadNode floatNode;

        protected SimpleMonadicMathFunction(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            floatNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        }

        protected SimpleMonadicMathFunction(SimpleMonadicMathFunction prev) {
            super(prev);
            isANode = prev.isANode;
            floatNode = prev.floatNode;
        }

        // TODO: why can't we leave this abstract?

        protected double doFunction(double a) {
            throw new UnsupportedOperationException();
        }

        @Specialization
        public double function(int a) {
            return doFunction(a);
        }

        @Specialization
        public double function(long a) {
            return doFunction(a);
        }

        @Specialization
        public double function(BigInteger a) {
            return doFunction(SlowPathBigInteger.doubleValue(a));
        }

        @Specialization
        public double function(double a) {
            return doFunction(a);
        }

        @Fallback
        public double function(VirtualFrame frame, Object a) {
            if (isANode.executeBoolean(a, getContext().getCoreLibrary().getNumericClass())) {
                try {
                    return doFunction(floatNode.callFloat(frame, a, "to_f", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(a).getName(),
                            getContext().getCoreLibrary().getFloatClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        getContext().getCoreLibrary().getLogicalClass(a).getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

    }

    protected abstract static class SimpleDyadicMathFunction extends CoreMethodNode {

        @Child protected KernelNodes.IsANode isANode;
        @Child protected DispatchHeadNode floatANode;
        @Child protected DispatchHeadNode floatBNode;

        protected SimpleDyadicMathFunction(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = KernelNodesFactory.IsANodeFactory.create(context, sourceSection, new RubyNode[]{null, null});
            floatANode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
            floatBNode = new DispatchHeadNode(context, Dispatch.MissingBehavior.RETURN_MISSING);
        }

        protected SimpleDyadicMathFunction(SimpleDyadicMathFunction prev) {
            super(prev);
            isANode = prev.isANode;
            floatANode = prev.floatANode;
            floatBNode = prev.floatBNode;
        }

        // TODO: why can't we leave this abstract?

        protected double doFunction(double a, double b) {
            throw new UnsupportedOperationException();
        }

        @Specialization
        public double function(int a, int b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(int a, long b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(int a, BigInteger b) {
            return doFunction(a, SlowPathBigInteger.doubleValue(b));
        }

        @Specialization
        public double function(int a, double b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(long a, int b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(long a, long b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(long a, BigInteger b) {
            return doFunction(a, SlowPathBigInteger.doubleValue(b));
        }

        @Specialization
        public double function(long a, double b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(BigInteger a, int b) {
            return doFunction(SlowPathBigInteger.doubleValue(a), b);
        }

        @Specialization
        public double function(BigInteger a, long b) {
            return doFunction(SlowPathBigInteger.doubleValue(a), b);
        }

        @Specialization
        public double function(BigInteger a, BigInteger b) {
            return doFunction(SlowPathBigInteger.doubleValue(a), SlowPathBigInteger.doubleValue(b));
        }

        @Specialization
        public double function(BigInteger a, double b) {
            return doFunction(SlowPathBigInteger.doubleValue(a), b);
        }

        @Specialization
        public double function(double a, int b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(double a, long b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(double a, BigInteger b) {
            return doFunction(a, SlowPathBigInteger.doubleValue(b));
        }

        @Specialization
        public double function(double a, double b) {
            return doFunction(a, b);
        }

        @Fallback
        public double function(VirtualFrame frame, Object a, Object b) {
            if (isANode.executeBoolean(a, getContext().getCoreLibrary().getNumericClass()) && isANode.executeBoolean(b, getContext().getCoreLibrary().getNumericClass())) {
                try {
                    return doFunction(
                            floatANode.callFloat(frame, a, "to_f", null),
                            floatBNode.callFloat(frame, b, "to_f", null));
                } catch (UseMethodMissingException e) {
                    throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                            getContext().getCoreLibrary().getLogicalClass(a).getName(),
                            getContext().getCoreLibrary().getFloatClass().getName(),
                            this));
                }
            } else {
                CompilerDirectives.transferToInterpreter();

                throw new RaiseException(getContext().getCoreLibrary().typeErrorCantConvertInto(
                        getContext().getCoreLibrary().getLogicalClass(a).getName(),
                        getContext().getCoreLibrary().getFloatClass().getName(),
                        this));
            }
        }

    }

}
