/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CreateCast;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.RubyBignum;
import org.jruby.RubyFixnum;
import org.jruby.RubyRational;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreClass;
import org.jruby.truffle.core.CoreMethod;
import org.jruby.truffle.core.CoreMethodArrayArgumentsNode;
import org.jruby.truffle.core.CoreMethodNode;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.RubiniusOnly;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.core.cast.IntegerCastNode;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.core.coerce.ToIntNode;
import org.jruby.truffle.core.coerce.ToIntNodeGen;
import org.jruby.truffle.core.numeric.FixnumOrBignumNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.constants.ReadConstantNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.stdlib.BigDecimalNodesFactory.BigDecimalCastNodeGen;
import org.jruby.truffle.stdlib.BigDecimalNodesFactory.BigDecimalCoerceNodeGen;
import org.jruby.truffle.stdlib.BigDecimalNodesFactory.CreateBigDecimalNodeFactory;
import org.jruby.truffle.stdlib.BigDecimalNodesFactory.GetIntegerConstantNodeGen;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    // TODO (pitr 2015-jun-16): lazy setup when required, see https://github.com/jruby/jruby/pull/3048#discussion_r32413656

    public static BigDecimal getBigDecimalValue(long v) {
        return BigDecimal.valueOf(v);
    }

    public static BigDecimal getBigDecimalValue(double v) {
        return BigDecimal.valueOf(v);
    }

    public static BigDecimal getBignumBigDecimalValue(DynamicObject v) {
        return new BigDecimal(Layouts.BIGNUM.getValue(v));
    }

    public static RoundingMode toRoundingMode(int constValue) {
        switch (constValue) {
            case 1:
                return RoundingMode.UP;
            case 2:
                return RoundingMode.DOWN;
            case 3:
                return RoundingMode.HALF_UP;
            case 4:
                return RoundingMode.HALF_DOWN;
            case 5:
                return RoundingMode.CEILING;
            case 6:
                return RoundingMode.FLOOR;
            case 7:
                return RoundingMode.HALF_EVEN;
            default:
                throw new UnsupportedOperationException("unknown value: " + constValue);
        }
    }

    private static int nearestBiggerMultipleOf4(int value) {
        return ((value / 4) + 1) * 4;
    }

    public static int defaultDivisionPrecision(int precisionA, int precisionB, int limit) {
        final int combination = nearestBiggerMultipleOf4(precisionA + precisionB) * 4;
        return (limit > 0 && limit < combination) ? limit : combination;
    }

    public static int defaultDivisionPrecision(BigDecimal a, BigDecimal b, int limit) {
        return defaultDivisionPrecision(a.precision(), b.precision(), limit);
    }

    public enum Type {
        NEGATIVE_INFINITY("-Infinity"),
        POSITIVE_INFINITY("Infinity"),
        NAN("NaN"),
        NEGATIVE_ZERO("-0"),
        NORMAL(null);

        private final String representation;

        Type(String representation) {
            this.representation = representation;
        }

        public String getRepresentation() {
            assert representation != null;
            return representation;
        }
    }

    public abstract static class BigDecimalCoreMethodNode extends CoreMethodNode {

        @Child private CreateBigDecimalNode createBigDecimal;
        @Child private CallDispatchHeadNode limitCall;
        @Child private IntegerCastNode limitIntegerCast;
        @Child private CallDispatchHeadNode roundModeCall;
        @Child private IntegerCastNode roundModeIntegerCast;

        public BigDecimalCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static boolean isNormal(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getType(value) == Type.NORMAL;
        }

        public static boolean isNormalRubyBigDecimal(DynamicObject value) {
            return RubyGuards.isRubyBigDecimal(value) && Layouts.BIG_DECIMAL.getType(value) == Type.NORMAL;
        }

        public static boolean isSpecialRubyBigDecimal(DynamicObject value) {
            return RubyGuards.isRubyBigDecimal(value) && Layouts.BIG_DECIMAL.getType(value) != Type.NORMAL;
        }

        public static boolean isNormalZero(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getValue(value).compareTo(BigDecimal.ZERO) == 0;
        }

        public static boolean isNan(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getType(value) == Type.NAN;
        }

        private void setupCreateBigDecimal() {
            if (createBigDecimal == null) {
                CompilerDirectives.transferToInterpreter();
                createBigDecimal = insert(CreateBigDecimalNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
        }

        protected DynamicObject createBigDecimal(VirtualFrame frame, Object value) {
            setupCreateBigDecimal();
            return createBigDecimal.executeCreate(frame, value);
        }

        protected DynamicObject initializeBigDecimal(VirtualFrame frame, Object value, DynamicObject self, Object digits) {
            setupCreateBigDecimal();
            return createBigDecimal.executeInitialize(frame, value, self, digits);
        }

        private void setupLimitCall() {
            if (limitCall == null) {
                CompilerDirectives.transferToInterpreter();
                limitCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }
        }

        private void setupLimitIntegerCast() {
            if (limitIntegerCast == null) {
                CompilerDirectives.transferToInterpreter();
                limitIntegerCast = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
            }
        }

        protected int getLimit(VirtualFrame frame) {
            setupLimitCall();
            setupLimitIntegerCast();

            return limitIntegerCast.executeCastInt(limitCall.call(frame, getBigDecimalClass(), "limit", null));
        }

        private void setupRoundModeCall() {
            if (roundModeCall == null) {
                CompilerDirectives.transferToInterpreter();
                roundModeCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }
        }

        private void setupRoundModeIntegerCast() {
            if (roundModeIntegerCast == null) {
                CompilerDirectives.transferToInterpreter();
                roundModeIntegerCast = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
            }
        }

        protected RoundingMode getRoundMode(VirtualFrame frame) {
            setupRoundModeCall();
            setupRoundModeIntegerCast();

            return toRoundingMode(roundModeIntegerCast.executeCastInt(
                    // TODO (pitr 21-Jun-2015): read the actual constant
                    roundModeCall.call(frame, getBigDecimalClass(), "mode", null, 256)));
        }

        protected DynamicObject getBigDecimalClass() {
            return coreLibrary().getBigDecimalClass();
        }
    }

    @NodeChild(value = "arguments", type = RubyNode[].class)
    public abstract static class BigDecimalCoreMethodArrayArgumentsNode extends BigDecimalCoreMethodNode {

        public BigDecimalCoreMethodArrayArgumentsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }
    }

    @NodeChildren({
            @NodeChild(value = "value", type = RubyNode.class),
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "digits", type = RubyNode.class)
    })
    @ImportStatic(org.jruby.truffle.stdlib.BigDecimalNodes.Type.class)
    public abstract static class CreateBigDecimalNode extends BigDecimalCoreMethodNode {

        private final static Pattern NUMBER_PATTERN;
        private final static Pattern ZERO_PATTERN;

        static {
            final String exponent = "([eE][+-]?)?(\\d*)";
            NUMBER_PATTERN = Pattern.compile("^([+-]?\\d*\\.?\\d*" + exponent + ").*");
            ZERO_PATTERN = Pattern.compile("^[+-]?0*\\.?0*" + exponent);
        }

        @Child private BigDecimalCastNode bigDecimalCast;
        @Child private CallDispatchHeadNode modeCall;
        @Child private GetIntegerConstantNode getIntegerConstant;
        @Child private BooleanCastNode booleanCast;
        @Child private CallDispatchHeadNode allocateNode;

        public CreateBigDecimalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            bigDecimalCast = BigDecimalCastNodeGen.create(context, sourceSection, null, null);
        }

        private void setBigDecimalValue(DynamicObject bigdecimal, BigDecimal value) {
            Layouts.BIG_DECIMAL.setValue(bigdecimal, value);
        }

        private void setBigDecimalValue(DynamicObject bigdecimal, Type type) {
            Layouts.BIG_DECIMAL.setType(bigdecimal, type);
        }

        public abstract DynamicObject executeInitialize(VirtualFrame frame, Object value, DynamicObject alreadyAllocatedSelf, Object digits);

        public final DynamicObject executeCreate(VirtualFrame frame, Object value) {
            if (allocateNode == null) {
                CompilerDirectives.transferToInterpreter();
                allocateNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }

            DynamicObject rubyClass = (getBigDecimalClass());
            return executeInitialize(frame, value, (DynamicObject) allocateNode.call(frame, rubyClass, "allocate", null), NotProvided.INSTANCE);
        }

        @Specialization
        public DynamicObject create(VirtualFrame frame, long value, DynamicObject self, NotProvided digits) {
            return create(frame, value, self, 0);
        }

        @Specialization
        public DynamicObject create(VirtualFrame frame, long value, DynamicObject self, int digits) {
            setBigDecimalValue(self,
                    bigDecimalCast.executeBigDecimal(frame, value, getRoundMode(frame)).round(new MathContext(digits, getRoundMode(frame))));
            return self;
        }

        @Specialization
        public DynamicObject create(VirtualFrame frame, double value, DynamicObject self, NotProvided digits) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().argumentError("can't omit precision for a Float.", this));
        }

        @Specialization
        public DynamicObject create(VirtualFrame frame, double value, DynamicObject self, int digits) {
            setBigDecimalValue(self,
                    bigDecimalCast.executeBigDecimal(frame, value, getRoundMode(frame)).round(new MathContext(digits, getRoundMode(frame))));
            return self;
        }

        @Specialization(guards = "value == NEGATIVE_INFINITY || value == POSITIVE_INFINITY")
        public DynamicObject createInfinity(VirtualFrame frame, Type value, DynamicObject self, Object digits) {
            return createWithMode(frame, value, self, "EXCEPTION_INFINITY", "Computation results to 'Infinity'");
        }

        @Specialization(guards = "value == NAN")
        public DynamicObject createNaN(VirtualFrame frame, Type value, DynamicObject self, Object digits) {
            return createWithMode(frame, value, self, "EXCEPTION_NaN", "Computation results to 'NaN'(Not a Number)");
        }

        @Specialization(guards = "value == NEGATIVE_ZERO")
        public DynamicObject createNegativeZero(VirtualFrame frame, Type value, DynamicObject self, Object digits) {
            setBigDecimalValue(self, value);
            return self;
        }

        @Specialization
        public DynamicObject create(VirtualFrame frame, BigDecimal value, DynamicObject self, NotProvided digits) {
            return create(frame, value, self, 0);
        }
        @Specialization
        public DynamicObject create(VirtualFrame frame, BigDecimal value, DynamicObject self, int digits) {
            setBigDecimalValue(self, value.round(new MathContext(digits, getRoundMode(frame))));
            return self;
        }

        @Specialization(guards = "isRubyBignum(value)")
        public DynamicObject createBignum(VirtualFrame frame, DynamicObject value, DynamicObject self, NotProvided digits) {
            return createBignum(frame, value, self, 0);
        }

        @Specialization(guards = "isRubyBignum(value)")
        public DynamicObject createBignum(VirtualFrame frame, DynamicObject value, DynamicObject self, int digits) {
            setBigDecimalValue(self,
                    getBignumBigDecimalValue(value).round(new MathContext(digits, getRoundMode(frame))));
            return self;
        }

        @Specialization(guards = "isRubyBigDecimal(value)")
        public DynamicObject createBigDecimal(VirtualFrame frame, DynamicObject value, DynamicObject self, NotProvided digits) {
            return createBigDecimal(frame, value, self, 0);
        }

        @Specialization(guards = "isRubyBigDecimal(value)")
        public DynamicObject createBigDecimal(VirtualFrame frame, DynamicObject value, DynamicObject self, int digits) {
            setBigDecimalValue(self,
                    Layouts.BIG_DECIMAL.getValue(value).round(new MathContext(digits, getRoundMode(frame))));
            return self;
        }

        @Specialization(guards = "isRubyString(value)")
        public DynamicObject createString(VirtualFrame frame, DynamicObject value, DynamicObject self, NotProvided digits) {
            return createString(frame, value, self, 0);
        }

        @Specialization(guards = "isRubyString(value)")
        public DynamicObject createString(VirtualFrame frame, DynamicObject value, DynamicObject self, int digits) {
            return executeInitialize(frame, getValueFromString(value.toString(), digits), self, digits);
        }

        @Specialization(guards = { "!isRubyBignum(value)", "!isRubyBigDecimal(value)", "!isRubyString(value)" })
        public DynamicObject create(VirtualFrame frame, DynamicObject value, DynamicObject self, int digits) {
            final Object castedValue = bigDecimalCast.executeObject(frame, value, getRoundMode(frame));
            if (castedValue == nil()) {
                throw new RaiseException(coreLibrary().typeError("could not be casted to BigDecimal", this));
            }

            setBigDecimalValue(
                    self,
                    ((BigDecimal) castedValue).round(new MathContext(digits, getRoundMode(frame))));

            return self;
        }

        // TODO (pitr 21-Jun-2015): raise on underflow

        private DynamicObject createWithMode(VirtualFrame frame, Type value, DynamicObject self,
                                             String constantName, String errorMessage) {
            setupModeCall();
            setupGetIntegerConstant();
            setupBooleanCast();

            final int exceptionConstant = getIntegerConstant.executeGetIntegerConstant(frame, getBigDecimalClass(), constantName);
            final boolean raise = booleanCast.executeBoolean(frame,
                    modeCall.call(frame, getBigDecimalClass(), "boolean_mode", null, exceptionConstant));
            if (raise) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError(errorMessage, this));
            }

            setBigDecimalValue(self, value);
            return self;
        }

        private void setupBooleanCast() {
            if (booleanCast == null) {
                CompilerDirectives.transferToInterpreter();
                booleanCast = insert(BooleanCastNodeGen.create(getContext(), getSourceSection(), null));
            }
        }

        private void setupGetIntegerConstant() {
            if (getIntegerConstant == null) {
                CompilerDirectives.transferToInterpreter();
                getIntegerConstant = insert(GetIntegerConstantNodeGen.create(getContext(), getSourceSection(), null, null));
            }
        }

        private void setupModeCall() {
            if (modeCall == null) {
                CompilerDirectives.transferToInterpreter();
                modeCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext(), true));
            }
        }

        @TruffleBoundary
        private Object getValueFromString(String string, int digits) {
            String strValue = string.trim();

            // TODO (pitr 26-May-2015): create specialization without trims and other cleanups, use rewriteOn

            switch (strValue) {
                case "NaN":
                    return Type.NAN;
                case "Infinity":
                case "+Infinity":
                    return Type.POSITIVE_INFINITY;
                case "-Infinity":
                    return Type.NEGATIVE_INFINITY;
                case "-0":
                    return Type.NEGATIVE_ZERO;
            }

            // Convert String to Java understandable format (for BigDecimal).
            strValue = strValue.replaceFirst("[dD]", "E");                  // 1. MRI allows d and D as exponent separators
            strValue = strValue.replaceAll("_", "");                        // 2. MRI allows underscores anywhere

            final MatchResult result;
            {
                final Matcher matcher = NUMBER_PATTERN.matcher(strValue);
                strValue = matcher.replaceFirst("$1"); // 3. MRI ignores the trailing junk
                result = matcher.toMatchResult();
            }

            try {
                final BigDecimal value = new BigDecimal(strValue, new MathContext(digits));
                if (value.compareTo(BigDecimal.ZERO) == 0 && strValue.startsWith("-")) {
                    return Type.NEGATIVE_ZERO;
                } else {
                    return value;
                }

            } catch (NumberFormatException e) {
                if (ZERO_PATTERN.matcher(strValue).matches()) {
                    return BigDecimal.ZERO;
                }

                final BigInteger exponent = new BigInteger(result.group(3));
                if (exponent.signum() == 1) {
                    return Type.POSITIVE_INFINITY;
                }
                // TODO (pitr 21-Jun-2015): raise on underflow
                if (exponent.signum() == -1) {
                    return BigDecimal.ZERO;
                }

                throw e;
            }
        }
    }

    // TODO (pitr 21-Jun-2015): Check for missing coerce on OpNodes

    @CoreMethod(unsafeNeedsAudit = true, names = "initialize", required = 1, optional = 1)
    public abstract static class InitializeNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object initialize(VirtualFrame frame, DynamicObject self, Object value, NotProvided digits) {
            return initializeBigDecimal(frame, value, self, digits);
        }

        @Specialization
        public Object initialize(VirtualFrame frame, DynamicObject self, Object value, int digits) {
            return initializeBigDecimal(frame, value, self, digits);
        }
    }

    @NodeChildren({
            @NodeChild(value = "a", type = RubyNode.class),
            @NodeChild(value = "b", type = RubyNode.class),
    })
    public abstract static class OpNode extends BigDecimalCoreMethodNode {

        public OpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CreateCast("b")
        protected RubyNode castB(RubyNode b) {
            return BigDecimalCoerceNodeGen.create(getContext(), getSourceSection(), b);
        }

    }

    public abstract static class AbstractAddNode extends OpNode {

        public AbstractAddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal addBigDecimal(DynamicObject a, DynamicObject b, MathContext mathContext) {
            return Layouts.BIG_DECIMAL.getValue(a).add(Layouts.BIG_DECIMAL.getValue(b), mathContext);
        }

        protected Object add(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return createBigDecimal(frame, addBigDecimal(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object addSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN ||
                    (aType == Type.POSITIVE_INFINITY && bType == Type.NEGATIVE_INFINITY) ||
                    (aType == Type.NEGATIVE_INFINITY && bType == Type.POSITIVE_INFINITY)) {
                return createBigDecimal(frame, Type.NAN);
            }

            if (aType == Type.POSITIVE_INFINITY || bType == Type.POSITIVE_INFINITY) {
                return createBigDecimal(frame, Type.POSITIVE_INFINITY);
            }

            if (aType == Type.NEGATIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) {
                return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
            }

            // one is NEGATIVE_ZERO and second is NORMAL
            if (isNormal(a)) {
                return a;
            } else {
                return b;
            }
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "+", required = 1)
    public abstract static class AddOpNode extends AbstractAddNode {

        public AddOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object add(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return add(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object addSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return addSpecial(frame, a, b, 0);
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "add", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class AddNode extends AbstractAddNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        protected Object add(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.add(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        protected Object addSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.addSpecial(frame, a, b, precision);
        }
    }

    public abstract static class AbstractSubNode extends OpNode {

        public AbstractSubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal subBigDecimal(DynamicObject a, DynamicObject b, MathContext mathContext) {
            return Layouts.BIG_DECIMAL.getValue(a).subtract(Layouts.BIG_DECIMAL.getValue(b), mathContext);
        }

        protected Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return createBigDecimal(frame, subBigDecimal(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object subSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN ||
                    (aType == Type.POSITIVE_INFINITY && bType == Type.POSITIVE_INFINITY) ||
                    (aType == Type.NEGATIVE_INFINITY && bType == Type.NEGATIVE_INFINITY)) {
                return createBigDecimal(frame, Type.NAN);
            }

            if (aType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) {
                return createBigDecimal(frame, Type.POSITIVE_INFINITY);
            }

            if (aType == Type.NEGATIVE_INFINITY || bType == Type.POSITIVE_INFINITY) {
                return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
            }

            // one is NEGATIVE_ZERO and second is NORMAL
            if (isNormal(a)) {
                return a;
            } else {
                return createBigDecimal(frame, Layouts.BIG_DECIMAL.getValue(b).negate());
            }
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "-", required = 1)
    public abstract static class SubOpNode extends AbstractSubNode {

        public SubOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return subNormal(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object subSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return subSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "sub", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class SubNode extends AbstractSubNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object subNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.subNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object subSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.subSpecial(frame, a, b, precision);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "-@")
    public abstract static class NegNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)" })
        public Object negNormal(VirtualFrame frame, DynamicObject value) {
            return createBigDecimal(frame, Layouts.BIG_DECIMAL.getValue(value).negate());
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)" })
        public Object negNormalZero(VirtualFrame frame, DynamicObject value) {
            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object negSpecial(VirtualFrame frame, DynamicObject value) {
            switch (Layouts.BIG_DECIMAL.getType(value)) {
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, BigDecimal.ZERO);
                case NAN:
                    return value;
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + value);
            }
        }

    }

    public abstract static class AbstractMultNode extends OpNode {

        private final ConditionProfile zeroNormal = ConditionProfile.createBinaryProfile();

        public AbstractMultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private Object multBigDecimalWithProfile(DynamicObject a, DynamicObject b, MathContext mathContext) {
            final BigDecimal bBigDecimal = Layouts.BIG_DECIMAL.getValue(b);

            if (zeroNormal.profile(isNormalZero(a) && bBigDecimal.signum() == -1)) {
                return Type.NEGATIVE_ZERO;
            }

            return multBigDecimal(Layouts.BIG_DECIMAL.getValue(a), bBigDecimal, mathContext);
        }

        @TruffleBoundary
        private Object multBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
            return a.multiply(b, mathContext);
        }

        protected Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return createBigDecimal(frame, multBigDecimalWithProfile(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return multSpecialNormal(frame, b, a, precision);
        }

        protected Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    return createBigDecimal(frame, Type.NAN);
                case NEGATIVE_ZERO:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                        case -1:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                case POSITIVE_INFINITY:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                    }
                case NEGATIVE_INFINITY:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                    }
                default:
                    throw new UnsupportedOperationException("unreachable code branch");
            }
        }

        protected Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN) {
                return createBigDecimal(frame, Type.NAN);
            }
            if (aType == Type.NEGATIVE_ZERO && bType == Type.NEGATIVE_ZERO) {
                return createBigDecimal(frame, BigDecimal.ZERO);
            }
            if (aType == Type.NEGATIVE_ZERO || bType == Type.NEGATIVE_ZERO) {
                return createBigDecimal(frame, Type.NAN);
            }

            // a and b are only +-Infinity

            if (aType == Type.POSITIVE_INFINITY) {
                return bType == Type.POSITIVE_INFINITY ? a : createBigDecimal(frame, Type.NEGATIVE_INFINITY);
            }
            if (aType == Type.NEGATIVE_INFINITY) {
                return bType == Type.POSITIVE_INFINITY ? a : createBigDecimal(frame, (Type.POSITIVE_INFINITY));
            }

            throw new UnsupportedOperationException("unreachable code branch");
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "*", required = 1)
    public abstract static class MultOpNode extends AbstractMultNode {

        public MultOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return mult(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return multSpecialNormal(frame, b, a, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return multSpecialNormal(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return multSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "mult", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class MultNode extends AbstractMultNode {

        public MultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object mult(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.mult(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multNormalSpecial(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object multSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multSpecialNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.multSpecial(frame, a, b, precision);
        }
    }

    public abstract static class AbstractDivNode extends OpNode {

        private final ConditionProfile normalZero = ConditionProfile.createBinaryProfile();

        public AbstractDivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private Object divBigDecimalWithProfile(DynamicObject a, DynamicObject b, MathContext mathContext) {
            final BigDecimal aBigDecimal = Layouts.BIG_DECIMAL.getValue(a);
            final BigDecimal bBigDecimal = Layouts.BIG_DECIMAL.getValue(b);
            if (normalZero.profile(bBigDecimal.signum() == 0)) {
                switch (aBigDecimal.signum()) {
                    case 1:
                        return Type.POSITIVE_INFINITY;
                    case 0:
                        return Type.NAN;
                    case -1:
                        return Type.NEGATIVE_INFINITY;
                    default:
                        throw new UnsupportedOperationException("unreachable code branch for value: " + aBigDecimal.signum());
                }
            } else {
                return divBigDecimal(aBigDecimal, bBigDecimal, mathContext);
            }
        }

        @TruffleBoundary
        private BigDecimal divBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
            return a.divide(b, mathContext);
        }

        protected Object div(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return createBigDecimal(frame, divBigDecimalWithProfile(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            switch (Layouts.BIG_DECIMAL.getType(b)) {
                case NAN:
                    return createBigDecimal(frame, Type.NAN);
                case NEGATIVE_ZERO:
                    switch (Layouts.BIG_DECIMAL.getValue(a).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                    }
                case POSITIVE_INFINITY:
                    switch (Layouts.BIG_DECIMAL.getValue(a).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                        case -1:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                    }
                case NEGATIVE_INFINITY:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                        case 0:
                        case -1:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(b));
            }
        }

        protected Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    return createBigDecimal(frame, Type.NAN);
                case NEGATIVE_ZERO:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                case POSITIVE_INFINITY:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                        case -1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                    }
                case NEGATIVE_INFINITY:
                    switch (Layouts.BIG_DECIMAL.getValue(b).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                        case -1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                    }
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
            }
        }

        protected Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN ||
                    (aType == Type.NEGATIVE_ZERO && bType == Type.NEGATIVE_ZERO)) {
                return createBigDecimal(frame, Type.NAN);
            }

            if (aType == Type.NEGATIVE_ZERO) {
                if (bType == Type.POSITIVE_INFINITY) {
                    return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                } else {
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                }
            }

            if (bType == Type.NEGATIVE_ZERO) {
                if (aType == Type.POSITIVE_INFINITY) {
                    return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                } else {
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                }
            }

            // a and b are only +-Infinity
            return createBigDecimal(frame, Type.NAN);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = { "/", "quo" }, required = 1)
    public abstract static class DivOpNode extends AbstractDivNode {

        public DivOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object div(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final int precision = defaultDivisionPrecision(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b), getLimit(frame));
            return div(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return divNormalSpecial(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return divSpecialNormal(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return divSpecialSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "div", required = 1, optional = 1)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class DivNode extends AbstractDivNode {

        private final ConditionProfile zeroPrecisionProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile bZeroProfile = ConditionProfile.createBinaryProfile();
        @Child private CallDispatchHeadNode floorCall;

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private void setupFloorCall() {
            if (floorCall == null) {
                CompilerDirectives.transferToInterpreter();
                floorCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object div(VirtualFrame frame, DynamicObject a, DynamicObject b, NotProvided precision) {
            setupFloorCall();
            if (bZeroProfile.profile(isNormalZero(b))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            } else {
                final Object result = div(frame, a, b, 0);
                return floorCall.call(frame, result, "floor", null);
            }
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object div(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
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
                "isSpecialRubyBigDecimal(b)" })
        public Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, NotProvided precision) {
            if (Layouts.BIG_DECIMAL.getType(b) == Type.NEGATIVE_ZERO) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            } else if (Layouts.BIG_DECIMAL.getType(b) == Type.NAN) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("Computation results to 'NaN'(Not a Number)", this));
            } else {
                return divNormalSpecial(frame, a, b, 0);
            }
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divNormalSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.divNormalSpecial(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, NotProvided precision) {
            if (isNormalZero(b)) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            } else if (Layouts.BIG_DECIMAL.getType(a) == Type.NAN) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("Computation results to 'NaN'(Not a Number)", this));
            } else if (Layouts.BIG_DECIMAL.getType(a) == Type.POSITIVE_INFINITY || Layouts.BIG_DECIMAL.getType(a) == Type.NEGATIVE_INFINITY) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("Computation results to 'Infinity'", this));
            } else {
                return divSpecialNormal(frame, a, b, 0);
            }
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object divSpecialNormal(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.divSpecialNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, NotProvided precision) {
            if (Layouts.BIG_DECIMAL.getType(b) == Type.NEGATIVE_ZERO) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            } else if (Layouts.BIG_DECIMAL.getType(a) == Type.NAN || Layouts.BIG_DECIMAL.getType(b) == Type.NAN) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().floatDomainError("Computation results to 'NaN'(Not a Number)", this));
            } else {
                return divSpecialSpecial(frame, a, b, 0);
            }
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divSpecialSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b, int precision) {
            return super.divSpecialSpecial(frame, a, b, precision);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "divmod", required = 1)
    public abstract static class DivModNode extends OpNode {

        @Child private CallDispatchHeadNode signCall;
        @Child private IntegerCastNode signIntegerCast;

        public DivModNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal[] divmodBigDecimal(BigDecimal a, BigDecimal b) {
            final BigDecimal[] result = a.divideAndRemainder(b);

            if (result[1].signum() * b.signum() < 0) {
                result[0] = result[0].subtract(BigDecimal.ONE);
                result[1] = result[1].add(b);
            }

            return result;
        }

        private void setupSignCall() {
            if (signCall == null) {
                CompilerDirectives.transferToInterpreter();
                signCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }
        }

        private void setupLimitIntegerCast() {
            if (signIntegerCast == null) {
                CompilerDirectives.transferToInterpreter();
                signIntegerCast = insert(IntegerCastNodeGen.create(getContext(), getSourceSection(), null));
            }
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "!isNormalZero(a)",
                "!isNormalZero(b)" })
        public Object divmod(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final BigDecimal[] result = divmodBigDecimal(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b));
            Object[] store = new Object[]{ createBigDecimal(frame, result[0]), createBigDecimal(frame, result[1]) };
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(a)",
                "!isNormalZero(b)" })
        public Object divmodZeroDividend(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, BigDecimal.ZERO) };
            return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(b)" })
        public Object divmodZeroDivisor(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().zeroDivisionError(this));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object divmodSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN) {
                Object[] store = new Object[]{ createBigDecimal(frame, Type.NAN), createBigDecimal(frame, Type.NAN) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            if (bType == Type.NEGATIVE_ZERO || (bType == Type.NORMAL && isNormalZero(b))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            }

            if (aType == Type.NEGATIVE_ZERO || (aType == Type.NORMAL && isNormalZero(a))) {
                Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, BigDecimal.ZERO) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            if (aType == Type.POSITIVE_INFINITY || aType == Type.NEGATIVE_INFINITY) {
                setupSignCall();
                setupLimitIntegerCast();

                final int signA = aType == Type.POSITIVE_INFINITY ? 1 : -1;
                final int signB = Integer.signum(signIntegerCast.executeCastInt(signCall.call(frame, b, "sign", null)));
                final int sign = signA * signB; // is between -1 and 1, 0 when nan

                final Type type = new Type[]{ Type.NEGATIVE_INFINITY, Type.NAN, Type.POSITIVE_INFINITY }[sign + 1];

                Object[] store = new Object[]{ createBigDecimal(frame, type), createBigDecimal(frame, Type.NAN) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            if (bType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) {
                Object[] store = new Object[]{ createBigDecimal(frame, BigDecimal.ZERO), createBigDecimal(frame, a) };
                return Layouts.ARRAY.createArray(coreLibrary().getArrayFactory(), store, store.length);
            }

            throw new UnsupportedOperationException("unreachable code branch");
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "remainder", required = 1)
    public abstract static class RemainderNode extends OpNode {

        public RemainderNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        public static BigDecimal remainderBigDecimal(BigDecimal a, BigDecimal b) {
            return a.remainder(b);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "!isNormalZero(b)" })
        public Object remainder(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return createBigDecimal(frame, remainderBigDecimal(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b)));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(b)" })
        public Object remainderZero(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return createBigDecimal(frame, Type.NAN);
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object remainderSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NEGATIVE_ZERO && bType == Type.NORMAL) {
                return createBigDecimal(frame, BigDecimal.ZERO);
            }

            return createBigDecimal(frame, Type.NAN);
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = { "modulo", "%" }, required = 1)
    public abstract static class ModuloNode extends OpNode {

        public ModuloNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
                "!isNormalZero(b)" })
        public Object modulo(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return createBigDecimal(frame, moduloBigDecimal(Layouts.BIG_DECIMAL.getValue(a), Layouts.BIG_DECIMAL.getValue(b)));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)",
                "isNormalZero(b)" })
        public Object moduloZero(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().zeroDivisionError(this));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object moduloSpecial(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN) {
                return createBigDecimal(frame, Type.NAN);
            }

            if (bType == Type.NEGATIVE_ZERO || (bType == Type.NORMAL && isNormalZero(b))) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().zeroDivisionError(this));
            }

            if (aType == Type.NEGATIVE_ZERO || (aType == Type.NORMAL && isNormalZero(a))) {
                return createBigDecimal(frame, BigDecimal.ZERO);
            }

            if (aType == Type.POSITIVE_INFINITY || aType == Type.NEGATIVE_INFINITY) {
                return createBigDecimal(frame, Type.NAN);
            }

            if (bType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) {
                return createBigDecimal(frame, a);
            }

            throw new UnsupportedOperationException("unreachable code branch");
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = { "**", "power" }, required = 1, optional = 1)
    @NodeChildren({
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "exponent", type = RubyNode.class),
            @NodeChild(value = "precision", type = RubyNode.class),
    })
    public abstract static class PowerNode extends BigDecimalCoreMethodNode {

        private final ConditionProfile positiveExponentProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile zeroExponentProfile = ConditionProfile.createBinaryProfile();
        private final ConditionProfile zeroProfile = ConditionProfile.createBinaryProfile();

        public PowerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal power(BigDecimal value, int exponent, MathContext mathContext) {
            return value.pow(exponent, mathContext);
        }

        @Specialization(guards = "isNormal(a)")
        public Object power(VirtualFrame frame, DynamicObject a, int exponent, NotProvided precision) {
            return power(frame, a, exponent, getLimit(frame));
        }

        @Specialization(guards = { "isNormal(a)" })
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
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
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
                    return createBigDecimal(frame, Type.NAN);
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, exponent >= 0 ? Type.POSITIVE_INFINITY : BigDecimal.ZERO);
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame,
                            Integer.signum(exponent) == 1 ? (exponent % 2 == 0 ? Type.POSITIVE_INFINITY : Type.NEGATIVE_INFINITY) : BigDecimal.ZERO);
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, Integer.signum(exponent) == 1 ? BigDecimal.ZERO : Type.NAN);
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
            }
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "sqrt", required = 1)
    @NodeChildren({
            @NodeChild(value = "self", type = RubyNode.class),
            @NodeChild(value = "precision", type = RubyNode.class),
    })
    public abstract static class SqrtNode extends BigDecimalCoreMethodNode {

        private final ConditionProfile positiveValueProfile = ConditionProfile.createBinaryProfile();

        public SqrtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeSqrt(VirtualFrame frame, DynamicObject value, int precision);

        @TruffleBoundary
        private BigDecimal sqrt(BigDecimal value, MathContext mathContext) {
            return RubyBigDecimal.bigSqrt(value, mathContext);
        }

        @Specialization(guards = { "precision < 0" })
        public Object sqrtNegativePrecision(VirtualFrame frame, DynamicObject a, int precision) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().argumentError("precision must be positive", this));
        }

        @Specialization(guards = { "precision == 0" })
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
                throw new RaiseException(coreLibrary().floatDomainError("(VpSqrt) SQRT(negative value)", this));
            }
        }

        @Specialization(guards = { "!isNormal(a)", "precision > 0" })
        public Object sqrtSpecial(VirtualFrame frame, DynamicObject a, int precision) {
            switch (Layouts.BIG_DECIMAL.getType(a)) {
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().floatDomainError("(VpSqrt) SQRT(NaN value)", this));
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().floatDomainError("(VpSqrt) SQRT(negative value)", this));
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, sqrt(BigDecimal.ZERO, new MathContext(precision, getRoundMode(frame))));
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(a));
            }
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "<=>", required = 1)
    public abstract static class CompareNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private int compareBigDecimal(DynamicObject a, BigDecimal b) {
            return Layouts.BIG_DECIMAL.getValue(a).compareTo(b);
        }

        @Specialization(guards = "isNormal(a)")
        public int compare(DynamicObject a, long b) {
            return compareBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public int compare(DynamicObject a, double b) {
            return compareBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = { "isNormal(a)", "isRubyBignum(b)" })
        public int compare(DynamicObject a, DynamicObject b) {
            return compareBigDecimal(a, getBignumBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public int compareNormal(DynamicObject a, DynamicObject b) {
            return compareBigDecimal(a, Layouts.BIG_DECIMAL.getValue(b));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(VirtualFrame frame, DynamicObject a, long b) {
            return compareSpecial(a, createBigDecimal(frame, getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(VirtualFrame frame, DynamicObject a, double b) {
            return compareSpecial(a, createBigDecimal(frame, getBigDecimalValue(b)));
        }

        @Specialization(guards = { "!isNormal(a)", "isRubyBignum(b)" })
        public Object compareSpecialBignum(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return compareSpecial(a, createBigDecimal(frame, getBignumBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNan(a)" })
        public Object compareSpecialNan(DynamicObject a, DynamicObject b) {
            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)",
                "isNormal(a) || !isNan(a)" })
        public Object compareSpecial(DynamicObject a, DynamicObject b) {
            final Type aType = Layouts.BIG_DECIMAL.getType(a);
            final Type bType = Layouts.BIG_DECIMAL.getType(b);

            if (aType == Type.NAN || bType == Type.NAN) {
                return nil();
            }
            if (aType == bType) {
                return 0;
            }
            if (aType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) {
                return 1;
            }
            if (aType == Type.NEGATIVE_INFINITY || bType == Type.POSITIVE_INFINITY) {
                return -1;
            }

            // a and b have finite value

            final BigDecimal aCompare;
            final BigDecimal bCompare;

            if (aType == Type.NEGATIVE_ZERO) {
                aCompare = BigDecimal.ZERO;
            } else {
                aCompare = Layouts.BIG_DECIMAL.getValue(a);
            }
            if (bType == Type.NEGATIVE_ZERO) {
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
                "!isNil(b)" })
        public Object compareCoerced(VirtualFrame frame, DynamicObject a, DynamicObject b) {
            return ruby("redo_coerced :<=>, b", "b", b);
        }

    }

    // TODO (pitr 20-May-2015): compare Ruby implementation of #== with a Java one

    @CoreMethod(unsafeNeedsAudit = true, names = "zero?")
    public abstract static class ZeroNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @NodeChildren({ @NodeChild("module"), @NodeChild("name") })
    public abstract static class GetIntegerConstantNode extends RubyNode {

        @Child ReadConstantNode readConstantNode;
        @Child ToIntNode toIntNode;
        @Child IntegerCastNode integerCastNode;

        public GetIntegerConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            readConstantNode = new ReadConstantNode(context, sourceSection, false, false, null, null);
            toIntNode = ToIntNodeGen.create(context, sourceSection, null);
            integerCastNode = IntegerCastNodeGen.create(context, sourceSection, null);
        }

        public abstract int executeGetIntegerConstant(VirtualFrame frame, DynamicObject module, String name);

        @Specialization(guards = "isRubyModule(module)")
        public int doInteger(VirtualFrame frame, DynamicObject module, String name) {
            final Object value = readConstantNode.readConstant(frame, module, name);
            return integerCastNode.executeCastInt(toIntNode.executeIntOrLong(frame, value));
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "sign")
    public abstract static class SignNode extends BigDecimalCoreMethodArrayArgumentsNode {

        private final ConditionProfile positive = ConditionProfile.createBinaryProfile();
        @Child private GetIntegerConstantNode sign;

        public SignNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sign = GetIntegerConstantNodeGen.create(context, sourceSection, null, null);
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)" })
        public int signNormalZero(VirtualFrame frame, DynamicObject value) {
            return sign.executeGetIntegerConstant(frame, getBigDecimalClass(), "SIGN_POSITIVE_ZERO");
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)" })
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

    @CoreMethod(unsafeNeedsAudit = true, names = "nan?")
    public abstract static class NanNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public NanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormal(value)")
        public boolean nanNormal(DynamicObject value) {
            return false;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean nanSpecial(DynamicObject value) {
            return Layouts.BIG_DECIMAL.getType(value) == Type.NAN;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "exponent")
    public abstract static class ExponentNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ExponentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)" })
        public long exponent(DynamicObject value) {
            final BigDecimal val = Layouts.BIG_DECIMAL.getValue(value).abs().stripTrailingZeros();
            return val.precision() - val.scale();
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)" })
        public int exponentZero(DynamicObject value) {
            return 0;
        }

        @Specialization(guards = "!isNormal(value)")
        public int exponentSpecial(DynamicObject value) {
            return 0;
        }

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "abs")
    public abstract static class AbsNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
            final Type type = Layouts.BIG_DECIMAL.getType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
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

    @CoreMethod(unsafeNeedsAudit = true, names = "round", optional = 2)
    public abstract static class RoundNode extends BigDecimalCoreMethodArrayArgumentsNode {

        @Child private FixnumOrBignumNode fixnumOrBignumNode;

        public RoundNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
                    throw new RaiseException(coreLibrary().
                            floatDomainError("Computation results to '-Infinity'", this));
                case POSITIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().
                            floatDomainError("Computation results to 'Infinity'", this));
                case NEGATIVE_ZERO:
                    return fixnumOrBignumNode.fixnumOrBignum(Layouts.BIG_DECIMAL.getValue(value));
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().
                            floatDomainError("Computation results to 'NaN'(Not a Number)", this));
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));

            }
        }

        @Specialization(guards = { "!isNormal(value)", "wasProvided(precision)" })
        public Object roundSpecial(VirtualFrame frame, DynamicObject value, Object precision, Object unusedRoundingMode) {
            return value;
        }
    }

    @CoreMethod(unsafeNeedsAudit = true, names = "finite?")
    public abstract static class FiniteNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public FiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @CoreMethod(unsafeNeedsAudit = true, names = "infinite?")
    public abstract static class InfiniteNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public InfiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @CoreMethod(unsafeNeedsAudit = true, names = "precs")
    public abstract static class PrecsNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public PrecsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @CoreMethod(unsafeNeedsAudit = true, names = "to_f")
    public abstract static class ToFNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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
    @CoreMethod(unsafeNeedsAudit = true, names = "unscaled", visibility = Visibility.PRIVATE)
    public abstract static class UnscaledNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public UnscaledNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

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

    @CoreMethod(unsafeNeedsAudit = true, names = { "to_i", "to_int" })
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
            final Type type = Layouts.BIG_DECIMAL.getType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case POSITIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(coreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case NEGATIVE_ZERO:
                    return 0;
                default:
                    throw new UnsupportedOperationException("unreachable code branch for value: " + Layouts.BIG_DECIMAL.getType(value));
            }
        }
    }

    /**
     * Casts a value into a BigDecimal.
     */
    @NodeChildren({
            @NodeChild(value = "value", type = RubyNode.class),
            @NodeChild(value = "roundingMode", type = RubyNode.class)
    })
    @ImportStatic(BigDecimalCoreMethodNode.class)
    public abstract static class BigDecimalCastNode extends RubyNode {
        public BigDecimalCastNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract BigDecimal executeBigDecimal(VirtualFrame frame, Object value, RoundingMode roundingMode);

        public abstract Object executeObject(VirtualFrame frame, Object value, RoundingMode roundingMode);

        @Specialization
        public BigDecimal doInt(long value, Object roundingMode) {
            return BigDecimal.valueOf(value);
        }

        @Specialization
        public BigDecimal doDouble(double value, Object roundingMode) {
            return BigDecimal.valueOf(value);
        }

        @Specialization(guards = "isRubyBignum(value)")
        public BigDecimal doBignum(DynamicObject value, Object roundingMode) {
            return new BigDecimal(Layouts.BIGNUM.getValue(value));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(value)")
        public BigDecimal doBigDecimal(DynamicObject value, Object roundingMode) {
            return Layouts.BIG_DECIMAL.getValue(value);
        }

        @Specialization(guards = { "!isRubyBignum(value)", "!isRubyBigDecimal(value)" })
        public Object doOther(VirtualFrame frame, DynamicObject value, Object roundingMode) {
            if (roundingMode instanceof RoundingMode && (boolean) ruby("value.is_a?(Rational)", "value", value)) {

                final Object numerator = ruby("value.numerator", "value", value);

                final IRubyObject numeratorValue;

                if (numerator instanceof Integer) {
                    numeratorValue = RubyFixnum.newFixnum(getContext().getJRubyRuntime(), (int) numerator);
                } else if (numerator instanceof Long) {
                    numeratorValue = RubyFixnum.newFixnum(getContext().getJRubyRuntime(), (long) numerator);
                } else if (RubyGuards.isRubyBignum(numerator)) {
                    numeratorValue = RubyBignum.newBignum(getContext().getJRubyRuntime(), Layouts.BIGNUM.getValue((DynamicObject) numerator));
                } else {
                    throw new UnsupportedOperationException(numerator.toString());
                }

                final Object denominator = ruby("value.denominator", "value", value);

                final IRubyObject denominatorValue;

                if (denominator instanceof Integer) {
                    denominatorValue = RubyFixnum.newFixnum(getContext().getJRubyRuntime(), (int) denominator);
                } else if (denominator instanceof Long) {
                    denominatorValue = RubyFixnum.newFixnum(getContext().getJRubyRuntime(), (long) denominator);
                } else if (RubyGuards.isRubyBignum(denominator)) {
                    denominatorValue = RubyBignum.newBignum(getContext().getJRubyRuntime(), Layouts.BIGNUM.getValue((DynamicObject) denominator));
                } else {
                    throw new UnsupportedOperationException(denominator.toString());
                }

                final RubyRational rubyRationalValue = RubyRational.newRationalRaw(getContext().getJRubyRuntime(), numeratorValue, denominatorValue);

                final RubyBigDecimal rubyBigDecimalValue;

                try {
                    rubyBigDecimalValue = RubyBigDecimal.getVpRubyObjectWithPrec19Inner(getContext().getJRubyRuntime().getCurrentContext(), rubyRationalValue, (RoundingMode) roundingMode);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }

                return rubyBigDecimalValue.getBigDecimalValue();
            } else {
                final Object result = ruby("value.to_f", "value", value);
                if (result != nil()) {
                    return new BigDecimal((double) result);
                } else {
                    return result;
                }
            }
        }

        @Fallback
        public Object doBigDecimalFallback(Object value, Object roundingMode) {
            return nil();
        }
        // TODO (pitr 22-Jun-2015): How to better communicate failure without throwing
    }

    /**
     * Coerces a value into a BigDecimal.
     */
    @NodeChildren({
            @NodeChild(value = "value", type = RubyNode.class),
            @NodeChild(value = "roundingMode", type = RoundModeNode.class),
            @NodeChild(value = "cast", type = BigDecimalCastNode.class, executeWith = {"value", "roundingMode"})

    })
    public abstract static class BigDecimalCoerceNode extends RubyNode {
        @Child private CreateBigDecimalNode createBigDecimal;

        public BigDecimalCoerceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static BigDecimalCoerceNode create(RubyContext context, SourceSection sourceSection, RubyNode value) {
            return BigDecimalCoerceNodeGen.create(context, sourceSection, value,
                    BigDecimalNodesFactory.RoundModeNodeFactory.create(context, sourceSection),
                    BigDecimalCastNodeGen.create(context, sourceSection, null, null));
        }

        private void setupCreateBigDecimal() {
            if (createBigDecimal == null) {
                CompilerDirectives.transferToInterpreter();
                createBigDecimal = insert(CreateBigDecimalNodeFactory.create(getContext(), getSourceSection(), null, null, null));
            }
        }

        protected DynamicObject createBigDecimal(VirtualFrame frame, Object value) {
            setupCreateBigDecimal();
            return createBigDecimal.executeCreate(frame, value);
        }

        public abstract DynamicObject executeBigDecimal(VirtualFrame frame, RoundingMode roundingMode, Object value);

        @Specialization
        public DynamicObject doBigDecimal(VirtualFrame frame, Object value, RoundingMode roundingMode, BigDecimal cast) {
            return createBigDecimal(frame, cast);
        }

        @Specialization(guards = { "isRubyBigDecimal(value)", "isNil(cast)" })
        public Object doBigDecimal(DynamicObject value, RoundingMode roundingMode, DynamicObject cast) {
            return value;
        }

        // TODO (pitr 22-Jun-2015): deal with not-coerce-able values

    }

    @CoreMethod(unsafeNeedsAudit = true, names = "allocate", constructor = true)
    public abstract static class AllocateNode extends CoreMethodArrayArgumentsNode {

        public AllocateNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public DynamicObject allocate(DynamicObject rubyClass) {
            return Layouts.BIG_DECIMAL.createBigDecimal(Layouts.CLASS.getInstanceFactory(rubyClass), BigDecimal.ZERO, Type.NORMAL);
        }

    }

    public abstract static class RoundModeNode extends BigDecimalCoreMethodNode {

        public RoundModeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RoundingMode doGetRoundMode(VirtualFrame frame) {
            return getRoundMode(frame);
        }

    }

}
