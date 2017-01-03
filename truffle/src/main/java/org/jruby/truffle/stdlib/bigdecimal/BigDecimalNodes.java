/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.bigdecimal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.builtins.NonStandard;
import org.jruby.truffle.core.cast.IntegerCastNode;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.Visibility;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.objects.AllocateObjectNode;
import org.jruby.truffle.parser.SafeDoubleParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@CoreClass("Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    // TODO (pitr 2015-jun-16): lazy setup when required, see https://github.com/jruby/jruby/pull/3048#discussion_r32413656

    // TODO (pitr 21-Jun-2015): Check for missing coerce on OpNodes

    @CoreMethod(names = "initialize", required = 1, optional = 1, lowerFixnum = 2)
    public abstract static class InitializeNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Specialization
        public Object initialize(VirtualFrame frame, DynamicObject self, Object value, NotProvided digits) {
            return initializeBigDecimal(frame, value, self, digits);
        }

        @Specialization
        public Object initialize(VirtualFrame frame, DynamicObject self, Object value, int digits) {
            return initializeBigDecimal(frame, value, self, digits);
        }
    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddOpNode extends AbstractAddNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object add(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return add(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        public Object addSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return addSpecial(frame, a, b, 0);
        }

    }

    @CoreMethod(names = "add", required = 2, lowerFixnum = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class AddNode extends AbstractAddNode {

        @Override
        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        protected Object add(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.add(frame, a, b, precision);
        }

        @Override
        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        protected Object addSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.addSpecial(frame, a, b, precision);
        }
    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubOpNode extends AbstractSubNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return subNormal(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        public Object subSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return subSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(names = "sub", required = 2, lowerFixnum = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class SubNode extends AbstractSubNode {

        @Override
        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.subNormal(frame, a, b, precision);
        }

        @Override
        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        public Object subSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.subSpecial(frame, a, b, precision);
        }
    }

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"
        })
        public Object negNormal(VirtualFrame frame, DynamicObject value) {
            return createBigDecimal(frame, Layouts.BIG_DECIMAL.getValue(value).negate());
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"
        })
        public Object negNormalZero(VirtualFrame frame, DynamicObject value) {
            return createBigDecimal(frame, BigDecimalType.NEGATIVE_ZERO);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object negSpecial(
                VirtualFrame frame,
                DynamicObject value,
                @Cached("createBinaryProfile()") ConditionProfile nanProfile,
                @Cached("createBinaryProfile()") ConditionProfile negZeroProfile,
                @Cached("createBinaryProfile()") ConditionProfile infProfile) {
            final BigDecimalType type = Layouts.BIG_DECIMAL.getType(value);

            if (nanProfile.profile(type == BigDecimalType.NAN)) {
                return value;
            }

            if (negZeroProfile.profile(type == BigDecimalType.NEGATIVE_ZERO)) {
                return createBigDecimal(frame, BigDecimal.ZERO);
            }

            final BigDecimalType resultType;

            if (infProfile.profile(type == BigDecimalType.NEGATIVE_INFINITY)) {
                resultType = BigDecimalType.POSITIVE_INFINITY;
            } else {
                resultType = BigDecimalType.NEGATIVE_INFINITY;
            }

            return createBigDecimal(frame, resultType);
        }

    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MultOpNode extends AbstractMultNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return mult(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return multSpecialNormal(frame, b, a, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return multSpecialNormal(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return multSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(names = "mult", required = 2, lowerFixnum = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class MultNode extends AbstractMultNode {

        @Override
        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.mult(frame, a, b, precision);
        }

        @Override
        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multNormalSpecial(frame, a, b, precision);
        }

        @Override
        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multSpecialNormal(frame, a, b, precision);
        }

        @Override
        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multSpecial(frame, a, b, precision);
        }
    }

    @CoreMethod(names = { "/", "quo" }, required = 1)
    public abstract static class DivOpNode extends AbstractDivNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object div(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final int precision = defaultDivisionPrecision(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b), getLimit(frame));
            return div(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return divNormalSpecial(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return divSpecialNormal(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return divSpecialSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(names = "div", required = 1, optional = 1, lowerFixnum = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class DivNode extends AbstractDivNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object div(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                NotProvided precision,
                @Cached("createBinaryProfile()") ConditionProfile bZeroProfile,
                @Cached("createMethodCall()") CallDispatchHeadNode floorNode) {
            if (bZeroProfile.profile(isNormalZero(b))) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            } else {
                final Object result = div(frame, a, b, 0);
                return floorNode.call(frame, result, "floor");
            }
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object div(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                int precision,
                @Cached("createBinaryProfile()") ConditionProfile zeroPrecisionProfile) {
            final int newPrecision;

            if (zeroPrecisionProfile.profile(precision == 0)) {
                newPrecision = defaultDivisionPrecision(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b), getLimit(frame));
            } else {
                newPrecision = precision;
            }

            return super.div(frame, a, b, newPrecision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object divNormalSpecial(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                NotProvided precision,
                @Cached("createBinaryProfile()") ConditionProfile negativeZeroProfile,
                @Cached("createBinaryProfile()") ConditionProfile nanProfile) {
            if (negativeZeroProfile.profile(Layouts.BIG_DECIMAL.getType(b) == BigDecimalType.NEGATIVE_ZERO)) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            } else if (nanProfile.profile(Layouts.BIG_DECIMAL.getType(b) == BigDecimalType.NAN)) {
                throw new RaiseException(coreExceptions().floatDomainErrorResultsToNaN(this));
            } else {
                return divNormalSpecial(frame, a, b, 0);
            }
        }

        @Override
        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.divNormalSpecial(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object divSpecialNormal(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                NotProvided precision,
                @Cached("createBinaryProfile()") ConditionProfile zeroDivisionProfile,
                @Cached("createBinaryProfile()") ConditionProfile nanProfile,
                @Cached("createBinaryProfile()") ConditionProfile infinityProfile) {
            if (zeroDivisionProfile.profile(isNormalZero(b))) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            } else if (nanProfile.profile(Layouts.BIG_DECIMAL.getType(a) == BigDecimalType.NAN)) {
                throw new RaiseException(coreExceptions().floatDomainErrorResultsToNaN(this));
            } else if (infinityProfile.profile(
                    Layouts.BIG_DECIMAL.getType(a) == BigDecimalType.POSITIVE_INFINITY
                            || Layouts.BIG_DECIMAL.getType(a) == BigDecimalType.NEGATIVE_INFINITY)) {
                throw new RaiseException(coreExceptions().floatDomainErrorResultsToInfinity(this));
            } else {
                return divSpecialNormal(frame, a, b, 0);
            }
        }

        @Override
        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.divSpecialNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object divSpecialSpecial(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                NotProvided precision,
                @Cached("createBinaryProfile()") ConditionProfile negZeroProfile,
                @Cached("createBinaryProfile()") ConditionProfile nanProfile) {
            if (negZeroProfile.profile(Layouts.BIG_DECIMAL.getType(b) == BigDecimalType.NEGATIVE_ZERO)) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            } else if (nanProfile.profile(
                    Layouts.BIG_DECIMAL.getType(a) == BigDecimalType.NAN
                            || Layouts.BIG_DECIMAL.getType(b) == BigDecimalType.NAN)) {
                throw new RaiseException(coreExceptions().floatDomainErrorResultsToNaN(this));
            } else {
                return divSpecialSpecial(frame, a, b, 0);
            }
        }

        @Override
        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.divSpecialSpecial(frame, a, b, precision);
        }
    }

    @CoreMethod(names = "divmod", required = 1)
    public abstract static class DivModNode extends BigDecimalOpNode {

        @TruffleBoundary
        private BigDecimal[] divmodBigDecimal(BigDecimal a, BigDecimal b) {
            final BigDecimal[] result = a.divideAndRemainder(b);

            if (result[1].signum() * b.signum() < 0) {
                result[0] = result[0].subtract(BigDecimal.ONE);
                result[1] = result[1].add(b);
            }

            return result;
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "!isNormalZero(a)",
                "!isNormalZero(b)"
        })
        public Object divmod(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final BigDecimal[] result = divmodBigDecimal(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b));
            final Object[] store = new Object[]{ createBigDecimal(frame, result[0]), createBigDecimal(frame, result[1]) };
            return createArray(store, store.length);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(a)",
                "!isNormalZero(b)"
        })
        public Object divmodZeroDividend(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, BigDecimal.ZERO) };
            return createArray(store, store.length);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(b)"
        })
        public Object divmodZeroDivisor(DynamicObject a, DynamicObject b) {
            throw new RaiseException(coreExceptions().zeroDivisionError(this));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        public Object divmodSpecial(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                @Cached("createMethodCall()") CallDispatchHeadNode signCall,
                @Cached("createIntegerCastNode()") IntegerCastNode signIntegerCast,
                @Cached("createBinaryProfile()") ConditionProfile nanProfile,
                @Cached("createBinaryProfile()") ConditionProfile normalNegProfile,
                @Cached("createBinaryProfile()") ConditionProfile negNormalProfile,
                @Cached("createBinaryProfile()") ConditionProfile infinityProfile) {
            final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
            final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

            if (nanProfile.profile(aType == BigDecimalType.NAN || bType == BigDecimalType.NAN)) {
                final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimalType.NAN), createBigDecimal(frame, BigDecimalType.NAN) };
                return createArray(store, store.length);
            }

            if (nanProfile.profile(bType == BigDecimalType.NEGATIVE_ZERO || (bType == BigDecimalType.NORMAL && isNormalZero(b)))) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            }

            if (normalNegProfile.profile(aType == BigDecimalType.NEGATIVE_ZERO || (aType == BigDecimalType.NORMAL && isNormalZero(a)))) {
                final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, BigDecimal.ZERO) };
                return createArray(store, store.length);
            }

            if (negNormalProfile.profile(aType == BigDecimalType.POSITIVE_INFINITY || aType == BigDecimalType.NEGATIVE_INFINITY)) {
                final int signA = aType == BigDecimalType.POSITIVE_INFINITY ? 1 : -1;
                final int signB = Integer.signum(signIntegerCast.executeCastInt(signCall.call(frame, b, "sign")));
                final int sign = signA * signB; // is between -1 and 1, 0 when nan

                final BigDecimalType type = new BigDecimalType[]{ BigDecimalType.NEGATIVE_INFINITY, BigDecimalType.NAN, BigDecimalType.POSITIVE_INFINITY }[sign + 1];

                final Object[] store = new Object[]{ createBigDecimal(frame, type), createBigDecimal(frame, BigDecimalType.NAN) };
                return createArray(store, store.length);
            }

            if (infinityProfile.profile(bType == BigDecimalType.POSITIVE_INFINITY || bType == BigDecimalType.NEGATIVE_INFINITY)) {
                final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, a) };
                return createArray(store, store.length);
            }

            throw new UnsupportedOperationException("unreachable code branch");
        }

        protected IntegerCastNode createIntegerCastNode() {
            return IntegerCastNodeGen.create(null);
        }

    }

    @CoreMethod(names = "remainder", required = 1)
    public abstract static class RemainderNode extends BigDecimalOpNode {

        @TruffleBoundary
        public static BigDecimal remainderBigDecimal(BigDecimal a, BigDecimal b) {
            return a.remainder(b);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "!isNormalZero(b)"
        })
        public Object remainder(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return createBigDecimal(frame, remainderBigDecimal(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b)));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(b)"
        })
        public Object remainderZero(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return createBigDecimal(frame, BigDecimalType.NAN);
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        public Object remainderSpecial(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                @Cached("createBinaryProfile()") ConditionProfile zeroProfile) {
            final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
            final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

            if (zeroProfile.profile(aType == BigDecimalType.NEGATIVE_ZERO && bType == BigDecimalType.NORMAL)) {
                return createBigDecimal(frame, BigDecimal.ZERO);
            } else {
                return createBigDecimal(frame, BigDecimalType.NAN);
            }
        }
    }

    @CoreMethod(names = { "modulo", "%" }, required = 1)
    public abstract static class ModuloNode extends BigDecimalOpNode {

        @TruffleBoundary
        public static BigDecimal moduloBigDecimal(BigDecimal a, BigDecimal b) {
            final BigDecimal modulo = a.remainder(b);

            if (modulo.signum() * b.signum() < 0) {
                return modulo.add(b);
            } else {
                return modulo;
            }
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "!isNormalZero(b)"
        })
        public Object modulo(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return createBigDecimal(frame, moduloBigDecimal(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b)));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(b)"
        })
        public Object moduloZero(DynamicObject a, DynamicObject b) {
            throw new RaiseException(coreExceptions().zeroDivisionError(this));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"
        })
        public Object moduloSpecial(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                @Cached("createBinaryProfile()") ConditionProfile nanProfile,
                @Cached("createBinaryProfile()") ConditionProfile normalNegProfile,
                @Cached("createBinaryProfile()") ConditionProfile negNormalProfile,
                @Cached("createBinaryProfile()") ConditionProfile posNegInfProfile,
                @Cached("createBinaryProfile()") ConditionProfile negPosInfProfile) {
            final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
            final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

            if (nanProfile.profile(aType == BigDecimalType.NAN
                    || bType == BigDecimalType.NAN)) {
                return createBigDecimal(frame, BigDecimalType.NAN);
            }

            if (normalNegProfile.profile(bType == BigDecimalType.NEGATIVE_ZERO
                    || (bType == BigDecimalType.NORMAL && isNormalZero(b)))) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            }

            if (negNormalProfile.profile(aType == BigDecimalType.NEGATIVE_ZERO
                    || (aType == BigDecimalType.NORMAL && isNormalZero(a)))) {
                return createBigDecimal(frame, BigDecimal.ZERO);
            }

            if (posNegInfProfile.profile(aType == BigDecimalType.POSITIVE_INFINITY
                    || aType == BigDecimalType.NEGATIVE_INFINITY)) {
                return createBigDecimal(frame, BigDecimalType.NAN);
            }

            if (negPosInfProfile.profile(bType == BigDecimalType.POSITIVE_INFINITY
                    || bType == BigDecimalType.NEGATIVE_INFINITY)) {
                return createBigDecimal(frame, a);
            }

            throw new UnsupportedOperationException("unreachable code branch");
        }
    }

    @CoreMethod(names = { "**", "power" }, required = 1, optional = 1, lowerFixnum = { 1, 2 })
    @NodeChildren({
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "exponent", type = RubyNode.class),
            @NodeChild(value = "precision", type = RubyNode.class),
    })
    public abstract static class PowerNode extends BigDecimalCoreMethodNode {

        public abstract Object executePower(VirtualFrame frame, Object a, Object exponent, Object precision);

        @TruffleBoundary
        private BigDecimal power(BigDecimal value, int exponent, MathContext mathContext) {
            return value.pow(exponent, mathContext);
        }

        @TruffleBoundary
        private int getDigits(BigDecimal value) {
            return value.abs().unscaledValue().toString().length();
        }

        @Specialization(guards = "isNormal(a)")
        public Object power(VirtualFrame frame, DynamicObject a, int exponent, NotProvided precision) {
            return executePower(frame, a, exponent, getLimit(frame));
        }

        @Specialization(guards = "isNormal(a)")
        public Object power(
                VirtualFrame frame,
                DynamicObject a,
                int exponent,
                int precision,
                @Cached("createBinaryProfile()") ConditionProfile positiveExponentProfile,
                @Cached("createBinaryProfile()") ConditionProfile zeroProfile,
                @Cached("createBinaryProfile()") ConditionProfile zeroExponentProfile) {
            final BigDecimal aBigDecimal = Layouts.BIG_DECIMAL.getValue(a);
            final boolean positiveExponent = positiveExponentProfile.profile(exponent >= 0);

            if (zeroProfile.profile(aBigDecimal.compareTo(BigDecimal.ZERO) == 0)) {
                final Object value;

                if (positiveExponent) {
                    if (zeroExponentProfile.profile(exponent == 0)) {
                        value = BigDecimal.ONE;
                    } else {
                        value = BigDecimal.ZERO;
                    }
                } else {
                    value = BigDecimalType.POSITIVE_INFINITY;
                }

                return createBigDecimal(frame, value);
            } else {
                final int newPrecision;

                if (positiveExponent) {
                    newPrecision = precision;
                } else {
                    newPrecision = (-exponent + 4) * (getDigits(aBigDecimal) + 4);
                }

                return createBigDecimal(frame,
                        power(Layouts.BIG_DECIMAL.getValue(a), exponent,
                                new MathContext(newPrecision, getRoundMode(frame))));
            }
        }

        @Specialization(guards = "!isNormal(a)")
        public Object power(
                VirtualFrame frame,
                DynamicObject a,
                int exponent,
                Object unusedPrecision,
                @Cached("create()") BranchProfile nanProfile,
                @Cached("create()") BranchProfile posInfinityProfile,
                @Cached("create()") BranchProfile negInfinityProfile,
                @Cached("create()") BranchProfile negZeroProfile) {
            final Object value;

            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    nanProfile.enter();
                    value = BigDecimalType.NAN;
                    break;
                case POSITIVE_INFINITY:
                    posInfinityProfile.enter();
                    value = exponent >= 0 ? BigDecimalType.POSITIVE_INFINITY : BigDecimal.ZERO;
                    break;
                case NEGATIVE_INFINITY:
                    negInfinityProfile.enter();
                    value = Integer.signum(exponent) == 1
                            ? (exponent % 2 == 0
                                ? BigDecimalType.POSITIVE_INFINITY
                                : BigDecimalType.NEGATIVE_INFINITY)
                            : BigDecimal.ZERO;
                    break;
                case NEGATIVE_ZERO:
                    negZeroProfile.enter();
                    value = Integer.signum(exponent) == 1 ? BigDecimal.ZERO : BigDecimalType.NAN;
                    break;
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
            }

            return createBigDecimal(frame, value);
        }
    }

    @CoreMethod(names = "sqrt", required = 1, lowerFixnum = 1)
    @NodeChildren({
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "precision", type = RubyNode.class),
    })
    public abstract static class SqrtNode extends BigDecimalCoreMethodNode {

        public abstract Object executeSqrt(VirtualFrame frame, DynamicObject value, int precision);

        @TruffleBoundary
        private BigDecimal sqrt(BigDecimal value, MathContext mathContext) {
            return bigSqrt(value, mathContext);
        }

        private static final BigDecimal TWO = new BigDecimal(2);
        private static final double SQRT_10 = 3.162277660168379332;

        public static BigDecimal bigSqrt(BigDecimal squarD, MathContext rootMC) {
            // General number and precision checking
            int sign = squarD.signum();
            if (sign == -1) throw new ArithmeticException("Square root of a negative number: " + squarD);
            if (sign == 0) return squarD.round(rootMC);

            int prec = rootMC.getPrecision();           // the requested precision
            if (prec == 0) throw new IllegalArgumentException("Most roots won't have infinite precision = 0");

            // Initial precision is that of double numbers 2^63/2 ~ 4E18
            int BITS = 62;                              // 63-1 an even number of number bits
            int nInit = 16;                             // precision seems 16 to 18 digits
            MathContext nMC = new MathContext(18, RoundingMode.HALF_DOWN);

            // Estimate the square root with the foremost 62 bits of squarD
            BigInteger bi = squarD.unscaledValue();     // bi and scale are a tandem
            int biLen = bi.bitLength();
            int shift = Math.max(0, biLen - BITS + (biLen%2 == 0 ? 0 : 1));   // even shift..
            bi = bi.shiftRight(shift);                  // ..floors to 62 or 63 bit BigInteger

            double root = Math.sqrt(SafeDoubleParser.doubleValue(bi));
            BigDecimal halfBack = new BigDecimal(BigInteger.ONE.shiftLeft(shift/2));

            int scale = squarD.scale();
            if (scale % 2 == 1) root *= SQRT_10; // 5 -> 2, -5 -> -3 need half a scale more..

            scale = (int) Math.ceil(scale/2.);         // ..where 100 -> 10 shifts the scale

            // Initial x - use double root - multiply by halfBack to unshift - set new scale
            BigDecimal x = new BigDecimal(root, nMC);
            x = x.multiply(halfBack, nMC);              // x0 ~ sqrt()
            if (scale != 0) x = x.movePointLeft(scale);

            if (prec < nInit) {                // for prec 15 root x0 must surely be OK
                return x.round(rootMC);        // return small prec roots without iterations
            }

            // Initial v - the reciprocal
            BigDecimal v = BigDecimal.ONE.divide(TWO.multiply(x), nMC);        // v0 = 1/(2*x)

            // Collect iteration precisions beforehand
            List<Integer> nPrecs = new ArrayList<>();

            assert nInit > 3 : "Never ending loop!";                // assume nInit = 16 <= prec

            // Let m be the exact digits precision in an earlier! loop
            for (int m = prec + 1; m > nInit; m = m/2 + (m > 100 ? 1 : 2)) {
                nPrecs.add(m);
            }

            // The loop of "Square Root by Coupled Newton Iteration"
            for (int i = nPrecs.size() - 1; i > -1; i--) {
                // Increase precision - next iteration supplies n exact digits
                nMC = new MathContext(nPrecs.get(i), i%2 == 1 ? RoundingMode.HALF_UP : RoundingMode.HALF_DOWN);

                // Next x                                        // e = d - x^2
                BigDecimal e = squarD.subtract(x.multiply(x, nMC), nMC);
                if (i != 0) {
                    x = x.add(e.multiply(v, nMC));               // x += e*v     ~ sqrt()
                } else {
                    x = x.add(e.multiply(v, rootMC), rootMC);    // root x is ready!
                    break;
                }

                // Next v                                        // g = 1 - 2*x*v
                BigDecimal g = BigDecimal.ONE.subtract(TWO.multiply(x).multiply(v, nMC));

                v = v.add(g.multiply(v, nMC));                   // v += g*v     ~ 1/2/sqrt()
            }

            return x;                      // return sqrt(squarD) with precision of rootMC
        }

        @Specialization(guards = "precision < 0")
        public Object sqrtNegativePrecision(VirtualFrame frame, DynamicObject a, int precision) {
            throw new RaiseException(coreExceptions().argumentError("precision must be positive", this));
        }

        @Specialization(guards = "precision == 0")
        public Object sqrtZeroPrecision(VirtualFrame frame, DynamicObject a, int precision) {
            return executeSqrt(frame, a, 1);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "precision > 0"
        })
        public Object sqrt(
                VirtualFrame frame,
                DynamicObject a,
                int precision,
                @Cached("createBinaryProfile()") ConditionProfile positiveValueProfile) {
            final BigDecimal valueBigDecimal = Layouts.BIG_DECIMAL.getValue(a);
            if (positiveValueProfile.profile(valueBigDecimal.signum() >= 0)) {
                return createBigDecimal(frame, sqrt(valueBigDecimal, new MathContext(precision, getRoundMode(frame))));
            } else {
                throw new RaiseException(coreExceptions().floatDomainErrorSqrtNegative(this));
            }
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "precision > 0"
        })
        public Object sqrtSpecial(
                VirtualFrame frame,
                DynamicObject a,
                int precision,
                @Cached("create()") BranchProfile nanProfile,
                @Cached("create()") BranchProfile posInfProfile,
                @Cached("create()") BranchProfile negInfProfile,
                @Cached("create()") BranchProfile negZeroProfile) {
            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    nanProfile.enter();
                    throw new RaiseException(coreExceptions().floatDomainErrorSqrtNegative(this));
                case POSITIVE_INFINITY:
                    posInfProfile.enter();
                    return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    negInfProfile.enter();
                    throw new RaiseException(coreExceptions().floatDomainErrorSqrtNegative(this));
                case NEGATIVE_ZERO:
                    negZeroProfile.enter();
                    return createBigDecimal(frame, sqrt(BigDecimal.ZERO, new MathContext(precision, getRoundMode(frame))));
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
            }
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        private int compareBigDecimal(DynamicObject a, BigDecimal b) {
            return Layouts.BIG_DECIMAL.getValue(a).compareTo(b);
        }

        @Specialization(guards = "isNormal(a)")
        public int compare(DynamicObject a, long b) {
            return compareBigDecimal(a, BigDecimal.valueOf(b));
        }

        @Specialization(guards = "isNormal(a)")
        public int compare(DynamicObject a, double b) {
            return compareBigDecimal(a, BigDecimal.valueOf(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBignum(b)"
        })
        public int compare(DynamicObject a, DynamicObject b) {
            return compareBigDecimal(a, new BigDecimal(Layouts.BIGNUM.getValue(b)));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public int compareNormal(DynamicObject a, DynamicObject b) {
            return compareBigDecimal(a, Layouts.BIG_DECIMAL.getValue(b));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(VirtualFrame frame, DynamicObject a, long b) {
            return compareSpecial(a, createBigDecimal(frame, BigDecimal.valueOf(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(VirtualFrame frame, DynamicObject a, double b) {
            return compareSpecial(a, createBigDecimal(frame, BigDecimal.valueOf(b)));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isRubyBignum(b)"
        })
        public Object compareSpecialBignum(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return compareSpecial(a, createBigDecimal(frame, new BigDecimal(Layouts.BIGNUM.getValue(b))));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNan(a)"
        })
        public Object compareSpecialNan(DynamicObject a, DynamicObject b) {
            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)",
                "isNormal(a) || !isNan(a)" })
        public Object compareSpecial(DynamicObject a, DynamicObject b) {
            final BigDecimalType aType = Layouts.BIG_DECIMAL.getType(a);
            final BigDecimalType bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == BigDecimalType.NAN || bType == BigDecimalType.NAN) {
                return nil();
            }
            if (aType == bType) {
                return 0;
            }
            if (aType == BigDecimalType.POSITIVE_INFINITY || bType == BigDecimalType.NEGATIVE_INFINITY) {
                return 1;
            }
            if (aType == BigDecimalType.NEGATIVE_INFINITY || bType == BigDecimalType.POSITIVE_INFINITY) {
                return -1;
            }

            // a and b have finite value

            final BigDecimal aCompare;
            final BigDecimal bCompare;

            if (aType == BigDecimalType.NEGATIVE_ZERO) {
                aCompare = BigDecimal.ZERO;
            } else {
                aCompare = Layouts.BIG_DECIMAL.getValue(a);
            }
            if (bType == BigDecimalType.NEGATIVE_ZERO) {
                bCompare = BigDecimal.ZERO;
            } else {
                bCompare = Layouts.BIG_DECIMAL.getValue(b);
            }

            return aCompare.compareTo(bCompare);
        }

        @Specialization(guards = "isNil(b)")
        public Object compareNil(DynamicObject a, DynamicObject b) {
            return nil();
        }

        @Specialization(guards = {
                "!isRubyBigDecimal(b)",
                "!isNil(b)"
        })
        public Object compareCoerced(
                VirtualFrame frame,
                DynamicObject a,
                DynamicObject b,
                @Cached("new()") SnippetNode snippetNode) {
            return snippetNode.execute(frame, "redo_coerced :<=>, b", "b", b);
        }

    }

    // TODO (pitr 20-May-2015): compare Ruby implementation of #== with a Java one

    @CoreMethod(names = "zero?")
    public abstract static class ZeroNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNormal(value)")
        public boolean zeroNormal(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getValue(value).compareTo(BigDecimal.ZERO) == 0;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean zeroSpecial(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getType(value) == BigDecimalType.NEGATIVE_ZERO;
        }
    }

    @CoreMethod(names = "sign")
    public abstract static class SignNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Child private GetIntegerConstantNode sign;

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"
        })
        public int signNormalZero(VirtualFrame frame, DynamicObject value) {
            return getConstant(frame, "SIGN_POSITIVE_ZERO");
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"
        })
        public int signNormal(
                VirtualFrame frame,
                DynamicObject value,
                @Cached("createBinaryProfile()") ConditionProfile positiveProfile) {
            final String name;

            if (positiveProfile.profile(Layouts.BIG_DECIMAL.getValue(value).signum() > 0)) {
                name = "SIGN_POSITIVE_FINITE";
            } else {
                name = "SIGN_NEGATIVE_FINITE";
            }

            return getConstant(frame, name);
        }

        @Specialization(guards = "!isNormal(value)")
        public int signSpecial(VirtualFrame frame, DynamicObject value) {
            final String name;

            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_INFINITY:
                    name = "SIGN_NEGATIVE_INFINITE";
                    break;
                case POSITIVE_INFINITY:
                    name = "SIGN_POSITIVE_INFINITE";
                    break;
                case NEGATIVE_ZERO:
                    name = "SIGN_NEGATIVE_ZERO";
                    break;
                case NAN:
                    name = "SIGN_NaN";
                    break;
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }

            return getConstant(frame, name);
        }

        private int getConstant(VirtualFrame frame, String name) {
            if (sign == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                sign = insert(GetIntegerConstantNodeGen.create(null, null));
            }

            return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), name);
        }

    }

    @CoreMethod(names = "nan?")
    public abstract static class NanNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNormal(value)")
        public boolean nanNormal(DynamicObject value) {
            return false;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean nanSpecial(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getType(value) == BigDecimalType.NAN;
        }

    }

    @CoreMethod(names = "exponent")
    public abstract static class ExponentNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"
        })
        public long exponent(DynamicObject value) {
            final BigDecimal val = Layouts.BIG_DECIMAL.getValue(value).abs().stripTrailingZeros();
            return val.precision() - val.scale();
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"
        })
        public int exponentZero(DynamicObject value) {
            return 0;
        }

        @Specialization(guards = "!isNormal(value)")
        public int exponentSpecial(DynamicObject value) {
            return 0;
        }

    }

    @CoreMethod(names = "abs")
    public abstract static class AbsNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        private BigDecimal abs(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getValue(value).abs();
        }

        @Specialization(guards = "isNormal(value)")
        public Object abs(VirtualFrame frame, DynamicObject value) {
            return createBigDecimal(frame, abs(value));
        }

        @Specialization(guards = "!isNormal(value)")
        public Object absSpecial(
                VirtualFrame frame,
                DynamicObject value,
                @Cached("create()") BranchProfile negInfProfile,
                @Cached("create()") BranchProfile negZeroProfile,
                @Cached("create()") BranchProfile posInfProfile) {
            final BigDecimalType type = Layouts.BIG_DECIMAL.getType(value);

            final Object result;

            switch (type) {
                case NEGATIVE_INFINITY:
                    negInfProfile.enter();
                    result = BigDecimalType.POSITIVE_INFINITY;
                    break;
                case NEGATIVE_ZERO:
                    negZeroProfile.enter();
                    result = BigDecimal.ZERO;
                    break;
                case POSITIVE_INFINITY:
                case NAN:
                    posInfProfile.enter();
                    result = type;
                    break;
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + type);
            }

            return createBigDecimal(frame, result);
        }

    }

    @CoreMethod(names = "round", optional = 2, lowerFixnum = { 1, 2 })
    public abstract static class RoundNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        private BigDecimal round(DynamicObject value, int digit, RoundingMode roundingMode) {
            final BigDecimal valueBigDecimal = Layouts.BIG_DECIMAL.getValue(value);

            if (digit <= valueBigDecimal.scale()) {
                return valueBigDecimal.
                        movePointRight(digit).
                        setScale(0, roundingMode).
                        movePointLeft(digit);
            } else {
                // Do not perform rounding when not required
                return valueBigDecimal;
            }
        }

        @Specialization(guards = "isNormal(value)")
        public Object round(
                VirtualFrame frame,
                DynamicObject value,
                NotProvided digit,
                NotProvided roundingMode,
                @Cached("new()") FixnumOrBignumNode fixnumOrBignumNode) {
            return fixnumOrBignumNode.fixnumOrBignum(round(value, 0, getRoundMode(frame)));
        }

        @Specialization(guards = "isNormal(value)")
        public Object round(VirtualFrame frame, DynamicObject value, int digit, NotProvided roundingMode) {
            return createBigDecimal(frame, round(value, digit, getRoundMode(frame)));
        }

        @Specialization(guards = "isNormal(value)")
        public Object round(VirtualFrame frame, DynamicObject value, int digit, int roundingMode) {
            return createBigDecimal(frame, round(value, digit, toRoundingMode(roundingMode)));
        }

        @Specialization(guards = "!isNormal(value)")
        public Object roundSpecial(
                DynamicObject value,
                NotProvided precision,
                Object unusedRoundingMode,
                @Cached("new()") FixnumOrBignumNode fixnumOrBignumNode,
                @Cached("create()") BranchProfile negInfinityProfile,
                @Cached("create()") BranchProfile posInfinityProfile,
                @Cached("create()") BranchProfile negZeroProfile,
                @Cached("create()") BranchProfile nanProfile) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_INFINITY:
                    negInfinityProfile.enter();
                    throw new RaiseException(coreExceptions().floatDomainErrorResultsToNegInfinity(this));
                case POSITIVE_INFINITY:
                    posInfinityProfile.enter();
                    throw new RaiseException(coreExceptions().floatDomainErrorResultsToInfinity(this));
                case NEGATIVE_ZERO:
                    negZeroProfile.enter();
                    return fixnumOrBignumNode.fixnumOrBignum(Layouts.BIG_DECIMAL.getValue(value));
                case NAN:
                    nanProfile.enter();
                    throw new RaiseException(coreExceptions().floatDomainErrorResultsToNaN(this));
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));

            }
        }

        @Specialization(guards = {
                "!isNormal(value)",
                "wasProvided(precision)"
        })
        public Object roundSpecial(DynamicObject value, Object precision, Object unusedRoundingMode) {
            return value;
        }
    }

    @CoreMethod(names = "finite?")
    public abstract static class FiniteNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNormal(value)")
        public boolean finiteNormal(DynamicObject value) {
            return true;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean finiteSpecial(DynamicObject value) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case POSITIVE_INFINITY:
                case NEGATIVE_INFINITY:
                case NAN:
                    return false;
                default:
                    return true;
            }
        }

    }

    @CoreMethod(names = "infinite?")
    public abstract static class InfiniteNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Specialization(guards = "isNormal(value)")
        public Object infiniteNormal(DynamicObject value) {
            return nil();
        }

        @Specialization(guards = "!isNormal(value)")
        public Object infiniteSpecial(DynamicObject value) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case POSITIVE_INFINITY:
                    return +1;
                case NEGATIVE_INFINITY:
                    return -1;
                default:
                    return nil();
            }
        }

    }

    @CoreMethod(names = "precs")
    public abstract static class PrecsNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public Object precsNormal(DynamicObject value) {
            final BigDecimal bigDecimalValue = Layouts.BIG_DECIMAL.getValue(value).abs();
            return createArray(new int[] {
                    bigDecimalValue.stripTrailingZeros().unscaledValue().toString().length(),
                    nearestBiggerMultipleOf4(bigDecimalValue.unscaledValue().toString().length()) }, 2);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object precsSpecial(DynamicObject value) {
            return createArray(new int[] { 1, 1 }, 2);
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public double toFNormal(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getValue(value).doubleValue();
        }

        @Specialization(guards = "!isNormal(value)")
        public double toFSpecial(
                DynamicObject value,
                @Cached("create()") BranchProfile negInfinityProfile,
                @Cached("create()") BranchProfile posInfinityProfile,
                @Cached("create()") BranchProfile negZeroProfile,
                @Cached("create()") BranchProfile nanProfile) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_INFINITY:
                    negInfinityProfile.enter();
                    return Double.NEGATIVE_INFINITY;
                case POSITIVE_INFINITY:
                    posInfinityProfile.enter();
                    return Double.POSITIVE_INFINITY;
                case NEGATIVE_ZERO:
                    negZeroProfile.enter();
                    return 0.0;
                case NAN:
                    nanProfile.enter();
                    return Double.NaN;
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }
        }

    }

    @NonStandard
    @CoreMethod(names = "unscaled", visibility = Visibility.PRIVATE)
    public abstract static class UnscaledNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public Object unscaled(DynamicObject value) {
            return createString(StringOperations.encodeRope(Layouts.BIG_DECIMAL.getValue(value).abs().stripTrailingZeros().unscaledValue().toString(), UTF8Encoding.INSTANCE));
        }

        @TruffleBoundary
        @Specialization(guards = "!isNormal(value)")
        public Object unscaledSpecial(DynamicObject value) {
            final String type = Layouts.BIG_DECIMAL.getType(value).getRepresentation();
            String string = type.startsWith("-") ? type.substring(1) : type;
            return createString(StringOperations.encodeRope(string, UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = { "to_i", "to_int" })
    public abstract static class ToINode extends BigDecimalCoreMethodArrayArgumentsNode {

        private BigInteger toBigInteger(BigDecimal bigDecimal) {
            return bigDecimal.toBigInteger();
        }

        @Specialization(guards = "isNormal(value)")
        public Object toINormal(
                DynamicObject value,
                @Cached("new()") FixnumOrBignumNode fixnumOrBignumNode) {
            return fixnumOrBignumNode.fixnumOrBignum(toBigInteger(Layouts.BIG_DECIMAL.getValue(value)));
        }

        @TruffleBoundary
        @Specialization(guards = "!isNormal(value)")
        public int toISpecial(
                DynamicObject value) {
            final BigDecimalType type = Layouts.BIG_DECIMAL.getType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    throw new RaiseException(coreExceptions().floatDomainError(type.getRepresentation(), this));
                case POSITIVE_INFINITY:
                    throw new RaiseException(coreExceptions().floatDomainError(type.getRepresentation(), this));
                case NAN:
                    throw new RaiseException(coreExceptions().floatDomainError(type.getRepresentation(), this));
                case NEGATIVE_ZERO:
                    return 0;
                default:
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Child private AllocateObjectNode allocateNode = AllocateObjectNode.create();

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return allocateNode.allocate(rubyClass, BigDecimal.ZERO, BigDecimalType.NORMAL);
        }

    }

}
