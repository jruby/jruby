/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 */
package org.jruby.truffle.core;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.Primitive;
import org.jruby.truffle.builtins.PrimitiveArrayArgumentsNode;
import org.jruby.truffle.core.cast.ToFNode;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.objects.IsANode;
import org.jruby.truffle.language.objects.IsANodeGen;

@CoreClass("Math")
public abstract class MathNodes {

    @CoreMethod(names = "acos", isModuleFunction = true, required = 1)
    public abstract static class ACosNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (a < -1.0 || a > 1.0) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorAcos(this));
            }

            return Math.acos(a);
        }

    }

    @CoreMethod(names = "acosh", isModuleFunction = true, required = 1)
    public abstract static class ACosHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (a < 1) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorAcosh(this));
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
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorAsin(this));
            }

            return Math.asin(a);
        }

    }

    @CoreMethod(names = "asinh", isModuleFunction = true, required = 1)
    public abstract static class ASinHNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            final double y = Math.abs(a);

            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (y <= 1.05367e-08) {
                return a;
            } else if (y <= 1.0) {
                return a * (1.0 + chebylevSerie(2.0 * a * a - 1.0, ASINH_COEF));
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
            if (a < -1.0 || a > 1.0) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorAtanh(this));
            }

            final double y = Math.abs(a);

            if (Double.isNaN(a)) {
                return Double.NaN;
            } else if (y < 1.82501e-08) {
                return a;
            } else if (y <= 0.5) {
                return a * (1.0 + chebylevSerie(8.0 * a * a - 1.0, ATANH_COEF));
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
            final double y = Math.abs(a);

            if (y <= 1.49012e-08) {
                return 2 * a / 1.77245385090551602729816748334;
            } else if (y <= 1) {
                return a * (1 + chebylevSerie(2 * a * a - 1, ERFC_COEF));
            } else if (y < 6.013687357) {
                return sign(1 - ErfcNode.erfc(y), a);
            } else if (Double.isNaN(y)) {
                return Double.NaN;
            } else {
                return sign(1, a);
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
            final double y = Math.abs(a);

            if (a <= -6.013687357) {
                return 2;
            } else if (y < 1.49012e-08) {
                return 1 - 2 * a / 1.77245385090551602729816748334;
            } else {
                double ysq = y*y;
                if (y < 1) {
                    return 1 - a * (1 + chebylevSerie(2 * ysq - 1, ERFC_COEF));
                } else if (y <= 4.0) {
                    double result = Math.exp(-ysq)/y*(0.5+chebylevSerie((8.0 / ysq - 5.0) / 3.0, ERFC2_COEF));
                    if (a < 0) result = 2.0 - result;
                    if (a < 0) result = 2.0 - result;
                    if (a < 0) result = 2.0 - result;
                    return result;
                } else {
                    double result = Math.exp(-ysq) / y * (0.5 + chebylevSerie(8.0 / ysq - 1, ERFCC_COEF));
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

    @Primitive(name = "math_frexp", needsSelf = false)
    public abstract static class FrExpNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public DynamicObject frexp(double a) {
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

            return createArray(new Object[] { sign * mantissa, exponent }, 2);
        }

        @Fallback
        public Object frexp(Object a) {
            return FAILURE;
        }

    }

    @CoreMethod(names = "gamma", isModuleFunction = true, required = 1)
    public abstract static class GammaNode extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (a == -1) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorGamma(this));
            }

            if (Double.isNaN(a)) {
                return Double.NaN;
            }

            if (Double.isInfinite(a)) {
                if (a > 0) {
                    return Double.POSITIVE_INFINITY;
                } else {
                    exceptionProfile.enter();
                    throw new RaiseException(coreExceptions().mathDomainErrorGamma(this));
                }
            }

            double result = nemes_gamma(a);

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
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorGamma(this));
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

    @Primitive(name = "math_ldexp", needsSelf = false)
    public abstract static class LdexpNode extends PrimitiveArrayArgumentsNode {

        @Specialization
        public double ldexp(double a, int b) {
            return a * Math.pow(2, b);
        }

        @Fallback
        public Object ldexp(Object a, Object b) {
            return FAILURE;
        }

    }

    @CoreMethod(names = "lgamma", isModuleFunction = true, required = 1)
    public abstract static class LGammaNode extends CoreMethodArrayArgumentsNode {

        @Child private IsANode isANode = IsANodeGen.create(null, null);
        @Child private ToFNode toFNode = ToFNode.create();

        private final BranchProfile exceptionProfile = BranchProfile.create();

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
            if (a < 0 && Double.isInfinite(a)) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorLog2(this));
            }

            final NemesLogGamma l = new NemesLogGamma(a);

            return createArray(new Object[] { l.value, l.sign }, 2);
        }

        @Fallback
        public DynamicObject lgamma(VirtualFrame frame, Object a) {
            if (!isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }

            return lgamma(toFNode.doDouble(frame, a));
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
        public double function(VirtualFrame frame, Object a, NotProvided b,
                        @Cached("create()") ToFNode toFNode) {
            if (!isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }
            return doFunction(toFNode.doDouble(frame, a));
        }

        private double doFunction(double a) {
            if (a < 0) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorLog(this));
            }

            return Math.log(a);
        }

        @Override
        protected double doFunction(double a, double b) {
            if (a < 0) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorLog(this));
            }

            return Math.log(a) / Math.log(b);
        }

    }

    @CoreMethod(names = "log10", isModuleFunction = true, required = 1)
    public abstract static class Log10Node extends SimpleMonadicMathNode {

        @Override
        protected double doFunction(double a) {
            if (a < 0) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorLog10(this));
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
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().mathDomainErrorLog2(this));
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

        @Child private IsANode isANode = IsANodeGen.create(null, null);
        @Child private ToFNode toFNode = ToFNode.create();

        protected final BranchProfile exceptionProfile = BranchProfile.create();

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
            if (!isANode.executeIsA(a, coreLibrary().getNumericClass())) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }

            return doFunction(toFNode.doDouble(frame, a));
        }

    }

    protected abstract static class SimpleDyadicMathNode extends CoreMethodArrayArgumentsNode {

        @Child protected IsANode isANode = IsANodeGen.create(null, null);
        @Child protected ToFNode floatANode = ToFNode.create();
        @Child protected ToFNode floatBNode = ToFNode.create();

        protected final BranchProfile exceptionProfile = BranchProfile.create();

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
            if (!(isANode.executeIsA(a, coreLibrary().getNumericClass()) &&
                    isANode.executeIsA(b, coreLibrary().getNumericClass()))) {
                exceptionProfile.enter();
                throw new RaiseException(coreExceptions().typeErrorCantConvertInto(a, "Float", this));
            }

            return doFunction(floatANode.doDouble(frame, a), floatBNode.doDouble(frame, b));
        }

    }

    public static final double[] ASINH_COEF = {
            -.12820039911738186343372127359268e+0,
            -.58811761189951767565211757138362e-1,
            .47274654322124815640725249756029e-2,
            -.49383631626536172101360174790273e-3,
            .58506207058557412287494835259321e-4,
            -.74669983289313681354755069217188e-5,
            .10011693583558199265966192015812e-5,
            -.13903543858708333608616472258886e-6,
            .19823169483172793547317360237148e-7,
            -.28847468417848843612747272800317e-8,
            .42672965467159937953457514995907e-9,
            -.63976084654366357868752632309681e-10,
            .96991686089064704147878293131179e-11,
            -.14844276972043770830246658365696e-11,
            .22903737939027447988040184378983e-12,
            -.35588395132732645159978942651310e-13,
            .55639694080056789953374539088554e-14,
            -.87462509599624678045666593520162e-15,
            .13815248844526692155868802298129e-15,
            -.21916688282900363984955142264149e-16,
            .34904658524827565638313923706880e-17
    };

    public static final double[] ATANH_COEF = {
            .9439510239319549230842892218633e-1,
            .4919843705578615947200034576668e-1,
            .2102593522455432763479327331752e-2,
            .1073554449776116584640731045276e-3,
            .5978267249293031478642787517872e-5,
            .3505062030889134845966834886200e-6,
            .2126374343765340350896219314431e-7,
            .1321694535715527192129801723055e-8,
            .8365875501178070364623604052959e-10,
            .5370503749311002163881434587772e-11,
            .3486659470157107922971245784290e-12,
            .2284549509603433015524024119722e-13,
            .1508407105944793044874229067558e-14,
            .1002418816804109126136995722837e-15,
            .6698674738165069539715526882986e-17,
            .4497954546494931083083327624533e-18
    };

    public static final double[] ERFC_COEF = {
            -.490461212346918080399845440334e-1,
            -.142261205103713642378247418996e0,
            .100355821875997955757546767129e-1,
            -.576876469976748476508270255092e-3,
            .274199312521960610344221607915e-4,
            -.110431755073445076041353812959e-5,
            .384887554203450369499613114982e-7,
            -.118085825338754669696317518016e-8,
            .323342158260509096464029309534e-10,
            -.799101594700454875816073747086e-12,
            .179907251139614556119672454866e-13,
            -.371863548781869263823168282095e-15,
            .710359900371425297116899083947e-17,
            -.126124551191552258324954248533e-18
    };

    public static final double[] ERFC2_COEF = {
            -.69601346602309501127391508262e-1,
            -.411013393626208934898221208467e-1,
            .391449586668962688156114370524e-2,
            -.490639565054897916128093545077e-3,
            .715747900137703638076089414183e-4,
            -.115307163413123283380823284791e-4,
            .199467059020199763505231486771e-5,
            -.364266647159922287393611843071e-6,
            .694437261000501258993127721463e-7,
            -.137122090210436601953460514121e-7,
            .278838966100713713196386034809e-8,
            -.581416472433116155186479105032e-9,
            .123892049175275318118016881795e-9,
            -.269063914530674343239042493789e-10,
            .594261435084791098244470968384e-11,
            -.133238673575811957928775442057e-11,
            .30280468061771320171736972433e-12,
            -.696664881494103258879586758895e-13,
            .162085454105392296981289322763e-13,
            -.380993446525049199987691305773e-14,
            .904048781597883114936897101298e-15,
            -.2164006195089607347809812047e-15,
            .522210223399585498460798024417e-16,
            -.126972960236455533637241552778e-16,
            .310914550427619758383622741295e-17,
            -.766376292032038552400956671481e-18,
            .190081925136274520253692973329e-18
    };

    public static final double[] ERFCC_COEF = {
            .715179310202924774503697709496e-1,
            -.265324343376067157558893386681e-1,
            .171115397792085588332699194606e-2,
            -.163751663458517884163746404749e-3,
            .198712935005520364995974806758e-4,
            -.284371241276655508750175183152e-5,
            .460616130896313036969379968464e-6,
            -.822775302587920842057766536366e-7,
            .159214187277090112989358340826e-7,
            -.329507136225284321486631665072e-8,
            .72234397604005554658126115389e-9,
            -.166485581339872959344695966886e-9,
            .401039258823766482077671768814e-10,
            -.100481621442573113272170176283e-10,
            .260827591330033380859341009439e-11,
            -.699111056040402486557697812476e-12,
            .192949233326170708624205749803e-12,
            -.547013118875433106490125085271e-13,
            .158966330976269744839084032762e-13,
            -.47268939801975548392036958429e-14,
            .14358733767849847867287399784e-14,
            -.444951056181735839417250062829e-15,
            .140481088476823343737305537466e-15,
            -.451381838776421089625963281623e-16,
            .147452154104513307787018713262e-16,
            -.489262140694577615436841552532e-17,
            .164761214141064673895301522827e-17,
            -.562681717632940809299928521323e-18,
            .194744338223207851429197867821e-18
    };

    public static double chebylevSerie(double x, double coef[]) {
        double  b0, b1, b2, twox;
        int i;
        b1 = 0.0;
        b0 = 0.0;
        b2 = 0.0;
        twox = 2.0 * x;
        for (i = coef.length-1; i >= 0; i--) {
            b2 = b1;
            b1 = b0;
            b0 = twox * b1 - b2 + coef[i];
        }
        return 0.5*(b0 - b2);
    }

    public static double sign(double x, double y) {
        double abs = ((x < 0) ? -x : x);
        return (y < 0.0) ? -abs : abs;
    }

    public static double nemes_gamma(double x) {
        double int_part = (int) x;

        if ((x - int_part) == 0.0 && 0 < int_part && int_part <= FACTORIAL.length) {
            return FACTORIAL[(int) int_part - 1];
        }
        NemesLogGamma l = new NemesLogGamma(x);
        return l.sign * Math.exp(l.value);
    }

    public static class NemesLogGamma {
        public final double value;
        public final double sign;

        public NemesLogGamma(double x) {
            if (Double.isInfinite(x)) {
                value = Double.POSITIVE_INFINITY; sign = 1;
                return;
            }

            if (Double.isNaN(x)) {
                value = Double.NaN; sign = 1;
                return;
            }

            double int_part = (int) x;
            sign = signum(x, int_part);
            if ((x - int_part) == 0.0 && 0 < int_part && int_part <= FACTORIAL.length) {
                value = Math.log(FACTORIAL[(int) int_part - 1]);
            }
            else if (x < 10) {
                double rising_factorial = 1;
                for (int i = 0; i < (int) Math.abs(x) - int_part + 10; i++) {
                    rising_factorial *= (x + i);
                }
                NemesLogGamma l = new NemesLogGamma(x + (int) Math.abs(x) - int_part + 10);
                value = l.value - Math.log(Math.abs(rising_factorial));
            } else {
                double temp = 0.0;
                for (int i = 0; i < NEMES_GAMMA_COEFF.length; i++) {
                    temp += NEMES_GAMMA_COEFF[i] * 1.0 / Math.pow(x, i);
                }

                value = x * (Math.log(x) - 1 + Math.log(temp)) +
                        (Math.log(2) + Math.log(Math.PI) - Math.log(x)) / 2.0;
            }
        }

        private static int signum(final double x, final double int_part) {
            return ( (int_part % 2 == 0 && (x - int_part) != 0.0 && (x < 0)) || negZero(x) ) ? -1 : 1;
        }

        private static boolean negZero(final double x) {
            return x == 0.0 && Double.doubleToRawLongBits(x) != 0; // detect -0.0 (since in Java: `0.0 == -0.0`)
        }

    }

    private static final double FACTORIAL[] = {
        /*  0! */ 1.0,
        /*  1! */ 1.0,
        /*  2! */ 2.0,
        /*  3! */ 6.0,
        /*  4! */ 24.0,
        /*  5! */ 120.0,
        /*  6! */ 720.0,
        /*  7! */ 5040.0,
        /*  8! */ 40320.0,
        /*  9! */ 362880.0,
        /* 10! */ 3628800.0,
        /* 11! */ 39916800.0,
        /* 12! */ 479001600.0,
        /* 13! */ 6227020800.0,
        /* 14! */ 87178291200.0,
        /* 15! */ 1307674368000.0,
        /* 16! */ 20922789888000.0,
        /* 17! */ 355687428096000.0,
        /* 18! */ 6402373705728000.0,
        /* 19! */ 121645100408832000.0,
        /* 20! */ 2432902008176640000.0,
        /* 21! */ 51090942171709440000.0,
        /* 22! */ 1124000727777607680000.0
    };

    private static final double NEMES_GAMMA_COEFF[] = {
            1.00000000000000000000000000000000000,
            0                                    ,
            0.08333333333333333333333333333333333,
            0                                    ,
            0.00069444444444444444444444444444444,
            0                                    ,
            0.00065861992945326278659611992945326,
            0                                    ,
            -0.00053287817827748383303938859494415,
            0                                    ,
            0.00079278588700608376534302460228386,
            0                                    ,
            -0.00184758189322033028400606295961969,
            0                                    ,
            0.00625067824784941846328836824623616,
            0                                    ,
            -0.02901710246301150993444701506844402,
            0                                    ,
            0.17718457242491308890302832366796470,
            0                                    ,
            -1.37747681703993534399676348903067470
    };

}
