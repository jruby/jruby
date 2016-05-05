/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.RubiniusOnly;
import org.jruby.truffle.core.cast.IntegerCastNode;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SnippetNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    // TODO (pitr 2015-jun-16): lazy setup when required, see https://github.com/jruby/jruby/pull/3048#discussion_r32413656

    // TODO (pitr 21-Jun-2015): Check for missing coerce on OpNodes

    @CoreMethod(names = "initialize", required = 1, optional = 1)
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

    @CoreMethod(names = "add", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class AddNode extends AbstractAddNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        protected Object add(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.add(frame, a, b, precision);
        }

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

    @CoreMethod(names = "sub", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class SubNode extends AbstractSubNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.subNormal(frame, a, b, precision);
        }

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

    @CoreMethod(names = "mult", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class MultNode extends AbstractMultNode {

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.mult(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)"
        })
        public Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multNormalSpecial(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)"
        })
        public Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multSpecialNormal(frame, a, b, precision);
        }

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

    @CoreMethod(names = "div", required = 1, optional = 1)
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
                return floorNode.call(frame, result, "floor", null);
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
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(a)",
                "!isNormalZero(b)"
        })
        public Object divmodZeroDividend(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, BigDecimal.ZERO) };
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
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
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            if (nanProfile.profile(bType == BigDecimalType.NEGATIVE_ZERO || (bType == BigDecimalType.NORMAL && isNormalZero(b)))) {
                throw new RaiseException(coreExceptions().zeroDivisionError(this));
            }

            if (normalNegProfile.profile(aType == BigDecimalType.NEGATIVE_ZERO || (aType == BigDecimalType.NORMAL && isNormalZero(a)))) {
                final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, BigDecimal.ZERO) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            if (negNormalProfile.profile(aType == BigDecimalType.POSITIVE_INFINITY || aType == BigDecimalType.NEGATIVE_INFINITY)) {
                final int signA = aType == BigDecimalType.POSITIVE_INFINITY ? 1 : -1;
                final int signB = Integer.signum(signIntegerCast.executeCastInt(signCall.call(frame, b, "sign", null)));
                final int sign = signA * signB; // is between -1 and 1, 0 when nan

                final BigDecimalType type = new BigDecimalType[]{ BigDecimalType.NEGATIVE_INFINITY, BigDecimalType.NAN, BigDecimalType.POSITIVE_INFINITY }[sign + 1];

                final Object[] store = new Object[]{ createBigDecimal(frame, type), createBigDecimal(frame, BigDecimalType.NAN) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            if (infinityProfile.profile(bType == BigDecimalType.POSITIVE_INFINITY || bType == BigDecimalType.NEGATIVE_INFINITY)) {
                final Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, a) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            throw new UnsupportedOperationException("unreachable code branch");
        }

        protected IntegerCastNode createIntegerCastNode() {
            return IntegerCastNodeGen.create(null, null, null);
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
                CompilerDirectives.transferToInterpreter();
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

    @CoreMethod(names = { "**", "power" }, required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "exponent", type = RubyNode.class),
            @NodeChild(value = "precision", type = RubyNode.class),
    })
    public abstract static class PowerNode extends BigDecimalCoreMethodNode {

        private final ConditionProfile positiveExponentProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile zeroExponentProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile zeroProfile = ConditionProfile.createBinaryProfile();

        @TruffleBoundary
        private BigDecimal power(BigDecimal value, int exponent, MathContext mathContext) {
            return value.pow(exponent, mathContext);
        }

        @Specialization(guards = "isNormal(a)")
        public Object power(VirtualFrame frame, DynamicObject a, int exponent, NotProvided precision) {
            return power(frame, a, exponent, getLimit(frame));
        }

        @Specialization(guards = "isNormal(a)")
        public Object power(VirtualFrame frame, DynamicObject a, int exponent, int precision) {
            final BigDecimal aBigDecimal = Layouts.BIG_DECIMAL.getValue(a);
            final boolean positiveExponent = positiveExponentProfile.profile(exponent >= 0);

            if (zeroProfile.profile(aBigDecimal.compareTo(BigDecimal.ZERO) == 0)) {
                if (positiveExponent) {
                    if (zeroExponentProfile.profile(exponent == 0)) {
                        return createBigDecimal(frame, BigDecimal.ONE);
                    } else {
                        return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                } else {
                    return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                }
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

        @TruffleBoundary
        private int getDigits(BigDecimal value) {
            return value.abs().unscaledValue().toString().length();
        }

        @Specialization(guards = "!isNormal(a)")
        public Object power(VirtualFrame frame, DynamicObject a, int exponent, Object unusedPrecision) {
            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    return createBigDecimal(frame, BigDecimalType.NAN);
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, exponent >= 0 ? BigDecimalType.POSITIVE_INFINITY : BigDecimal.ZERO);
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame,
                            Integer.signum(exponent) == 1 ? (exponent % 2 == 0 ? BigDecimalType.POSITIVE_INFINITY : BigDecimalType.NEGATIVE_INFINITY) : BigDecimal.ZERO);
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, Integer.signum(exponent) == 1 ? BigDecimal.ZERO : BigDecimalType.NAN);
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
            }
        }
    }

    @CoreMethod(names = "sqrt", required = 1)
    @NodeChildren({
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "precision", type = RubyNode.class),
    })
    public abstract static class SqrtNode extends BigDecimalCoreMethodNode {

        private final ConditionProfile positiveValueProfile = ConditionProfile.createBinaryProfile();

        public abstract Object executeSqrt(VirtualFrame frame, DynamicObject value, int precision);

        @TruffleBoundary
        private BigDecimal sqrt(BigDecimal value, MathContext mathContext) {
            return RubyBigDecimal.bigSqrt(value, mathContext);
        }

        @Specialization(guards = "precision < 0")
        public Object sqrtNegativePrecision(VirtualFrame frame, DynamicObject a, int precision) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreExceptions().argumentError("precision must be positive", this));
        }

        @Specialization(guards = "precision == 0")
        public Object sqrtZeroPrecision(VirtualFrame frame, DynamicObject a, int precision) {
            return executeSqrt(frame, a, 1);
        }

        @Specialization(guards = { "isNormal(a)", "precision > 0" })
        public Object sqrt(VirtualFrame frame, DynamicObject a, int precision) {
            final BigDecimal valueBigDecimal = Layouts.BIG_DECIMAL.getValue(a);
            if (positiveValueProfile.profile(valueBigDecimal.signum() >= 0)) {
                return createBigDecimal(frame, sqrt(valueBigDecimal, new MathContext(precision, getRoundMode(frame))));
            } else {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreExceptions().floatDomainError("(VpSqrt) SQRT(negative value)", this));
            }
        }

        @Specialization(guards = { "!isNormal(a)", "precision > 0" })
        public Object sqrtSpecial(VirtualFrame frame, DynamicObject a, int precision) {
            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().floatDomainError("(VpSqrt) SQRT(NaN value)", this));
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().floatDomainError("(VpSqrt) SQRT(negative value)", this));
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, sqrt(BigDecimal.ZERO, new MathContext(precision, getRoundMode(frame))));
                default:
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
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_ZERO:
                    return true;
                default:
                    return false;
            }
        }
    }

    @CoreMethod(names = "sign")
    public abstract static class SignNode extends BigDecimalCoreMethodArrayArgumentsNode {

        private final ConditionProfile positive = ConditionProfile.createBinaryProfile();
        @Child private GetIntegerConstantNode sign;

        public SignNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sign = GetIntegerConstantNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"
        })
        public int signNormalZero(VirtualFrame frame, DynamicObject value) {
            return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_POSITIVE_ZERO");
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"
        })
        public int signNormal(VirtualFrame frame, DynamicObject value) {
            if (positive.profile(Layouts.BIG_DECIMAL.getValue(value).signum() > 0)) {
                return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_POSITIVE_FINITE");
            } else {
                return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_NEGATIVE_FINITE");
            }
        }

        @Specialization(guards = "!isNormal(value)")
        public int signSpecial(VirtualFrame frame, DynamicObject value) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_INFINITY:
                    return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_NEGATIVE_INFINITE");
                case POSITIVE_INFINITY:
                    return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_POSITIVE_INFINITE");
                case NEGATIVE_ZERO:
                    return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_NEGATIVE_ZERO");
                case NAN:
                    return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_NaN");
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }
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
        public Object absSpecial(VirtualFrame frame, DynamicObject value) {
            final BigDecimalType type = Layouts.BIG_DECIMAL.getType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame, BigDecimalType.POSITIVE_INFINITY);
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, BigDecimal.ZERO);
                case POSITIVE_INFINITY:
                case NAN:
                    return createBigDecimal(frame, type);
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + type);
            }
        }

    }

    @CoreMethod(names = "round", optional = 2)
    public abstract static class RoundNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignumNode;

        @TruffleBoundary
        private BigDecimal round(DynamicObject value, int digit, RoundingMode roundingMode) {
            final BigDecimal valueBigDecimal = Layouts.BIG_DECIMAL.getValue(value);

            if (digit <= valueBigDecimal.scale()) {
                return valueBigDecimal.
                        movePointRight(digit).
                        setScale(0, roundingMode).
                        movePointLeft(digit);
            } else {
                // do not perform rounding when not required;
                return valueBigDecimal;
            }
        }

        @Specialization(guards = "isNormal(value)")
        public Object round(VirtualFrame frame, DynamicObject value, NotProvided digit, NotProvided roundingMode) {
            if (fixnumOrBignumNode == null) {
                CompilerDirectives.transferToInterpreter();
                fixnumOrBignumNode = insert(FixnumOrBignumNode.create(getContext(), getSourceSection()));
            }

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
        public Object roundSpecial(VirtualFrame frame, DynamicObject value, NotProvided precision, Object unusedRoundingMode) {
            if (fixnumOrBignumNode == null) {
                CompilerDirectives.transferToInterpreter();
                fixnumOrBignumNode = insert(FixnumOrBignumNode.create(getContext(), getSourceSection()));
            }

            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().
                            floatDomainError("Computation results to '-Infinity'", this));
                case POSITIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().
                            floatDomainError("Computation results to 'Infinity'", this));
                case NEGATIVE_ZERO:
                    return fixnumOrBignumNode.fixnumOrBignum(Layouts.BIG_DECIMAL.getValue(value));
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().
                            floatDomainError("Computation results to 'NaN'(Not a Number)", this));
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));

            }
        }

        @Specialization(guards = {
                "!isNormal(value)",
                "wasProvided(precision)"
        })
        public Object roundSpecial(VirtualFrame frame, DynamicObject value, Object precision, Object unusedRoundingMode) {
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
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new int[]{
                    bigDecimalValue.stripTrailingZeros().unscaledValue().toString().length(),
                    nearestBiggerMultipleOf4(bigDecimalValue.unscaledValue().toString().length()) }, 2);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object precsSpecial(DynamicObject value) {
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), new int[]{ 1, 1 }, 2);
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
        public double toFSpecial(DynamicObject value) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case NEGATIVE_INFINITY:
                    return Double.NEGATIVE_INFINITY;
                case POSITIVE_INFINITY:
                    return Double.POSITIVE_INFINITY;
                case NEGATIVE_ZERO:
                    return 0.0D;
                case NAN:
                    return Double.NaN;
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "unscaled", visibility = Visibility.PRIVATE)
    public abstract static class UnscaledNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public Object unscaled(DynamicObject value) {
            return createString(StringOperations.encodeRope(Layouts.BIG_DECIMAL.getValue(value).abs().stripTrailingZeros().unscaledValue().toString(), UTF8Encoding.INSTANCE));
        }

        @Specialization(guards = "!isNormal(value)")
        public Object unscaledSpecial(DynamicObject value) {
            final String type = Layouts.BIG_DECIMAL.getType(value).getRepresentation();
            String string = type.startsWith("-") ? type.substring(1) : type;
            return createString(StringOperations.encodeRope(string, UTF8Encoding.INSTANCE));
        }

    }

    @CoreMethod(names = { "to_i", "to_int" })
    public abstract static class ToINode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignum;

        public ToINode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            fixnumOrBignum = new FixnumOrBignumNode(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public Object toINormal(DynamicObject value) {
            return fixnumOrBignum.fixnumOrBignum(Layouts.BIG_DECIMAL.getValue(value).toBigInteger());
        }

        @Specialization(guards = "!isNormal(value)")
        public int toISpecial(DynamicObject value) {
            final BigDecimalType type = Layouts.BIG_DECIMAL.getType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().
                            floatDomainError(type.getRepresentation(), this));
                case POSITIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().
                            floatDomainError(type.getRepresentation(), this));
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreExceptions().
                            floatDomainError(type.getRepresentation(), this));
                case NEGATIVE_ZERO:
                    return 0;
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }
        }
    }

    @CoreMethod(names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return Layouts.BIG_DECIMAL.createBigDecimal(Layouts.CLASS.getInstanceFactory(rubyClass), BigDecimal.ZERO, BigDecimalType.NORMAL);
        }

    }

}
