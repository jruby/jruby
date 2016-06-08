/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.RubyMath;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.language.dispatch.MissingBehavior;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;

@CoreClass("Math")
public abstract class MathNodes {

    @CoreMethod(names = "acos", isModuleFunction = true, required = 1)
    public abstract static class ACosNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (a < -1.0 || a > 1.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("acos", this));
            }

            return Math.acos(a);
        }

    }

    @CoreMethod(names = "acosh", isModuleFunction = true, required = 1)
    public abstract static class ACosHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (a < 1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("acosh", this));
            } else if (a < 94906265.62) {
                return Math.log(a + Math.sqrt(a * a - 1.0));
            } else{
                return 0.69314718055994530941723212145818 + Math.log(a);
            }
        }

    }

    @CoreMethod(names = "asin", isModuleFunction = true, required = 1)
    public abstract static class ASinNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (a < -1.0 || a > 1.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("asin", this));
            }

            return Math.asin(a);
        }

    }

    @CoreMethod(names = "asinh", isModuleFunction = true, required = 1)
    public abstract static class ASinHNode extends SimpleMonadicMathNode {

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
    public abstract static class ATanNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.atan(a);
        }

    }

    @CoreMethod(names = "atan2", isModuleFunction = true, required = 2)
    public abstract static class ATan2Node extends SimpleDyadicMathNode {

        @Override
        protected double doFunction(double a, double b) {
            return Math.atan2(a, b);
        }

    }

    @CoreMethod(names = "atanh", isModuleFunction = true, required = 1)
    public abstract static class ATanHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            if (a < -1.0 || a > 1.0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("atanh", this));
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
    public abstract static class CbRtNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.cbrt(a);
        }

    }

    @CoreMethod(names = "cos", isModuleFunction = true, required = 1)
    public abstract static class CosNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.cos(a);
        }

    }

    @CoreMethod(names = "cosh", isModuleFunction = true, required = 1)
    public abstract static class CosHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.cosh(a);
        }

    }

    @CoreMethod(names = "erf", isModuleFunction = true, required = 1)
    public abstract static class ErfNode extends SimpleMonadicMathNode {

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
    public abstract static class ErfcNode extends SimpleMonadicMathNode {

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
    public abstract static class ExpNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.exp(a);
        }

    }

    @CoreMethod(names = "frexp", isModuleFunction = true, required = 1)
    public abstract static class FrExpNode extends CoreMethodArrayArgumentsNode {

        private final BranchProfile errorProfile = BranchProfile.create();

        @Child private IsANode isANode;
        @Child private CallDispatchHeadNode floatNode;

        public FrExpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
            floatNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
        }

        @Specialization
        public DynamicObject frexp(int a) {
            return frexp((double) a);
        }

        @Specialization
        public DynamicObject frexp(long a) {
            return frexp((double) a);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public DynamicObject frexp(DynamicObject a) {
            return frexp(Layouts.BIGNUM.getValue(a).doubleValue());
        }

        @Specialization
        public DynamicObject frexp(double a) {
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

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{sign * mantissa, exponent}, 2);
        }

        @Fallback
        public DynamicObject frexp(VirtualFrame frame, Object a) {
            if (isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                return frexp(floatNode.callFloat(frame, a, "to_f", null));
            } else {
                errorProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
        }

    }

    @CoreMethod(names = "gamma", isModuleFunction = true, required = 1)
    public abstract static class GammaNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            // Copied from RubyMath - see copyright notices there

            if (a == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("gamma", this));
            }

            if (Double.isNaN(a)) {
                return Double.NaN;
            }

            if (Double.isInfinite(a)) {
                if (a > 0) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new RaiseException(coreExceptions().mathDomainError("gamma", this));
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("gamma", this));
            }

            return result;
        }

    }

    @CoreMethod(names = "hypot", isModuleFunction = true, required = 2)
    public abstract static class HypotNode extends SimpleDyadicMathNode {

        @Override
        protected double doFunction(double a, double b) {
            return Math.hypot(a, b);
        }

    }

    @CoreMethod(names = "ldexp", isModuleFunction = true, required = 2)
    public abstract static class LdexpNode extends CoreMethodArrayArgumentsNode {

        @Child private IsANode isANode;
        @Child private CallDispatchHeadNode floatANode;
        @Child private CallDispatchHeadNode integerBNode;

        protected LdexpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
            floatANode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
            integerBNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
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

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, int b) {
            return function(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, long b) {
            return function(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, double b) {
            return function(Layouts.BIGNUM.getValue(a).doubleValue(), b);
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
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().rangeError("float", Double.toString(b), "integer", this));
            }

            return a * Math.pow(2, b);
        }

        @Fallback
        public double function(VirtualFrame frame, Object a, Object b) {
            if (isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                return function(
                        floatANode.callFloat(frame, a, "to_f", null),
                        integerBNode.callLongFixnum(frame, b, "to_int", null));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
        }

    }



    @CoreMethod(names = "lgamma", isModuleFunction = true, required = 1)
    public abstract static class LGammaNode extends CoreMethodArrayArgumentsNode {

        @Child private IsANode isANode;
        @Child private CallDispatchHeadNode floatNode;

        public LGammaNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
            floatNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
        }

        @Specialization
        public DynamicObject lgamma(int a) {
            return lgamma((double) a);
        }

        @Specialization
        public DynamicObject lgamma(long a) {
            return lgamma((double) a);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public DynamicObject lgamma(DynamicObject a) {
            return lgamma(Layouts.BIGNUM.getValue(a).doubleValue());
        }

        @Specialization
        public DynamicObject lgamma(double a) {
            // Copied from RubyMath - see copyright notices there

            if (a < 0 && Double.isInfinite(a)) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("log2", this));
            }

            final RubyMath.NemesLogGamma l = new RubyMath.NemesLogGamma(a);

            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new Object[]{l.value, l.sign}, 2);
        }

        @Fallback
        public DynamicObject lgamma(VirtualFrame frame, Object a) {
            if (isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                return lgamma(floatNode.callFloat(frame, a, "to_f", null));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
        }

    }

    @CoreMethod(names = "log", isModuleFunction = true, required = 1, optional = 1)
    public abstract static class LogNode extends SimpleDyadicMathNode {

        @Specialization
        public double function(int a, NotProvided b) {
            return doFunction(a);
        }

        @Specialization
        public double function(long a, NotProvided b) {
            return doFunction(a);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, NotProvided b) {
            return doFunction(Layouts.BIGNUM.getValue(a).doubleValue());
        }

        @Specialization
        public double function(double a, NotProvided b) {
            return doFunction(a);
        }

        @Specialization
        public double function(VirtualFrame frame, Object a, NotProvided b) {
            if (isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                return doFunction(floatANode.callFloat(frame, a, "to_f", null));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
        }

        private double doFunction(double a) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("log", this));
            }

            return Math.log(a);
        }

        @Override
        protected double doFunction(double a, double b) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("log", this));
            }

            return Math.log(a) / Math.log(b);
        }

    }

    @CoreMethod(names = "log10", isModuleFunction = true, required = 1)
    public abstract static class Log10Node extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("log10", this));
            }

            return Math.log10(a);
        }

    }

    @CoreMethod(names = "log2", isModuleFunction = true, required = 1)
    public abstract static class Log2Node extends SimpleMonadicMathNode {

        private final double LOG2 = Math.log(2);

        @Override
        protected double doFunction(double a) {
            if (a < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().mathDomainError("log2", this));
            }

            return Math.log(a) / LOG2;
        }

    }

    @CoreMethod(names = "sin", isModuleFunction = true, required = 1)
    public abstract static class SinNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.sin(a);
        }

    }

    @CoreMethod(names = "sinh", isModuleFunction = true, required = 1)
    public abstract static class SinHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.sinh(a);
        }

    }

    @CoreMethod(names = "tan", isModuleFunction = true, required = 1)
    public abstract static class TanNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.tan(a);
        }

    }

    @CoreMethod(names = "tanh", isModuleFunction = true, required = 1)
    public abstract static class TanHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.tanh(a);
        }

    }

    @CoreMethod(names = "sqrt", isModuleFunction = true, required = 1)
    public abstract static class SqrtNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            return Math.sqrt(a);
        }

    }

    protected abstract static class SimpleMonadicMathNode extends CoreMethodArrayArgumentsNode {

        @Child private IsANode isANode;
        @Child private CallDispatchHeadNode floatNode;

        protected SimpleMonadicMathNode() {
            this(null, null);
        }

        protected SimpleMonadicMathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
            floatNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
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

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a) {
            return doFunction(Layouts.BIGNUM.getValue(a).doubleValue());
        }

        @Specialization
        public double function(double a) {
            return doFunction(a);
        }

        @Fallback
        public double function(VirtualFrame frame, Object a) {
            if (isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                return doFunction(floatNode.callFloat(frame, a, "to_f", null));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
        }

    }

    protected abstract static class SimpleDyadicMathNode extends CoreMethodArrayArgumentsNode {

        @Child protected IsANode isANode;
        @Child protected CallDispatchHeadNode floatANode;
        @Child protected CallDispatchHeadNode floatBNode;

        protected SimpleDyadicMathNode() {
            this(null, null);
        }

        protected SimpleDyadicMathNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            isANode = IsANodeGen.create(context, sourceSection, null, null);
            floatANode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
            floatBNode = DispatchHeadNodeFactory.createMethodCall(context, MissingBehavior.RETURN_MISSING);
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

        @Specialization(guards = "isRubyBignum(b)")
        public double function(int a, DynamicObject b) {
            return doFunction(a, Layouts.BIGNUM.getValue(b).doubleValue());
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

        @Specialization(guards = "isRubyBignum(a)")
        public double function(long a, DynamicObject b) {
            return doFunction(a, Layouts.BIGNUM.getValue(b).doubleValue());
        }

        @Specialization
        public double function(long a, double b) {
            return doFunction(a, b);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, int b) {
            return doFunction(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, long b) {
            return doFunction(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization(guards = {"isRubyBignum(a)", "isRubyBignum(b)"})
        public double function(DynamicObject a, DynamicObject b) {
            return doFunction(Layouts.BIGNUM.getValue(a).doubleValue(), Layouts.BIGNUM.getValue(b).doubleValue());
        }

        @Specialization(guards = "isRubyBignum(a)")
        public double function(DynamicObject a, double b) {
            return doFunction(Layouts.BIGNUM.getValue(a).doubleValue(), b);
        }

        @Specialization
        public double function(double a, int b) {
            return doFunction(a, b);
        }

        @Specialization
        public double function(double a, long b) {
            return doFunction(a, b);
        }

        @Specialization(guards = "isRubyBignum(b)")
        public double function(double a, DynamicObject b) {
            return doFunction(a, Layouts.BIGNUM.getValue(b).doubleValue());
        }

        @Specialization
        public double function(double a, double b) {
            return doFunction(a, b);
        }

        @Fallback
        public double function(VirtualFrame frame, Object a, Object b) {
            if (isANode.executeIsA(a, coreLibrary().getNumericClass()) &&
                    isANode.executeIsA(b, coreLibrary().getNumericClass())) {
                return doFunction(
                        floatANode.callFloat(frame, a, "to_f", null),
                        floatBNode.callFloat(frame, b, "to_f", null));
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();

                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
        }

    }

}
