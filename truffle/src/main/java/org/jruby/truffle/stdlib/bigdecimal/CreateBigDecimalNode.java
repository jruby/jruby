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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.cast.BooleanCastNode;
import org.jruby.truffle.core.cast.BooleanCastNodeGen;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NodeChildren({
        @NodeChild(value = "value", type = RubyNode.class),
        @NodeChild(value = "self", type = RubyNode.class),
        @NodeChild(value = "digits", type = RubyNode.class)
})
@ImportStatic(BigDecimalType.class)
public abstract class CreateBigDecimalNode extends BigDecimalCoreMethodNode {

    private static final String exponent = "([eE][+-]?)?(\\d*)";
    private final static Pattern NUMBER_PATTERN = Pattern.compile("^([+-]?\\d*\\.?\\d*" + exponent + ").*");
    private final static Pattern ZERO_PATTERN = Pattern.compile("^[+-]?0*\\.?0*" + exponent);

    @Child private CallDispatchHeadNode allocateNode;

    public abstract DynamicObject executeInitialize(
            VirtualFrame frame,
            Object value,
            DynamicObject alreadyAllocatedSelf,
            Object digits);

    public final DynamicObject executeCreate(VirtualFrame frame, Object value) {
        if (allocateNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            allocateNode = insert(DispatchHeadNodeFactory.createMethodCall(true));
        }

        return executeInitialize(
                frame,
                value,
                (DynamicObject) allocateNode.call(frame, getBigDecimalClass(), "allocate"),
                NotProvided.INSTANCE);
    }

    @Specialization
    public DynamicObject create(VirtualFrame frame, long value, DynamicObject self, NotProvided digits) {
        return executeInitialize(frame, value, self, 0);
    }

    @Specialization
    public DynamicObject create(
            VirtualFrame frame,
            long value,
            DynamicObject self,
            int digits,
            @Cached("createBigDecimalCastNode()") BigDecimalCastNode bigDecimalCastNode) {
        Layouts.BIG_DECIMAL.setValue(self,
                bigDecimalCastNode.executeBigDecimal(frame, value, getRoundMode(frame))
                        .round(new MathContext(digits, getRoundMode(frame))));
        return self;
    }

    @Specialization
    public DynamicObject create(double value, DynamicObject self, NotProvided digits) {
        throw new RaiseException(coreExceptions().argumentErrorCantOmitPrecision(this));
    }

    @Specialization
    public DynamicObject create(
            VirtualFrame frame,
            double value,
            DynamicObject self,
            int digits,
            @Cached("createBigDecimalCastNode()") BigDecimalCastNode bigDecimalCastNode) {
        Layouts.BIG_DECIMAL.setValue(self, bigDecimalCastNode
                .executeBigDecimal(frame, value, getRoundMode(frame))
                .round(new MathContext(digits, getRoundMode(frame))));
        return self;
    }

    @Specialization(guards = "value == NEGATIVE_INFINITY || value == POSITIVE_INFINITY")
    public DynamicObject createInfinity(
            VirtualFrame frame,
            BigDecimalType value,
            DynamicObject self,
            Object digits,
            @Cached("createBooleanCastNode()") BooleanCastNode booleanCastNode,
            @Cached("createGetIntegerConstantNode()") GetIntegerConstantNode getIntegerConstantNode,
            @Cached("createMethodCallIgnoreVisibility()") CallDispatchHeadNode modeCallNode,
            @Cached("createBinaryProfile()") ConditionProfile raiseProfile) {
        // TODO (pitr 21-Jun-2015): raise on underflow

        final int exceptionConstant = getIntegerConstantNode
                .executeGetIntegerConstant(frame, getBigDecimalClass(), "EXCEPTION_INFINITY");

        final boolean raise = booleanCastNode.executeToBoolean(
                modeCallNode.call(frame, getBigDecimalClass(), "boolean_mode", exceptionConstant));

        if (raiseProfile.profile(raise)) {
            throw new RaiseException(coreExceptions().floatDomainErrorResultsToInfinity(this));
        }

        Layouts.BIG_DECIMAL.setType(self, value);
        return self;
    }

    @Specialization(guards = "value == NAN")
    public DynamicObject createNaN(
            VirtualFrame frame,
            BigDecimalType value,
            DynamicObject self,
            Object digits,
            @Cached("createBooleanCastNode()") BooleanCastNode booleanCastNode,
            @Cached("createGetIntegerConstantNode()") GetIntegerConstantNode getIntegerConstantNode,
            @Cached("createMethodCallIgnoreVisibility()") CallDispatchHeadNode modeCallNode,
            @Cached("createBinaryProfile()") ConditionProfile raiseProfile) {
        // TODO (pitr 21-Jun-2015): raise on underflow

        final int exceptionConstant = getIntegerConstantNode.executeGetIntegerConstant
                (frame, getBigDecimalClass(), "EXCEPTION_NaN");

        final boolean raise = booleanCastNode.executeToBoolean(
                modeCallNode.call(frame, getBigDecimalClass(), "boolean_mode", exceptionConstant));

        if (raiseProfile.profile(raise)) {
            throw new RaiseException(coreExceptions().floatDomainErrorResultsToNaN(this));
        }

        Layouts.BIG_DECIMAL.setType(self, value);
        return self;
    }

    @Specialization(guards = "value == NEGATIVE_ZERO")
    public DynamicObject createNegativeZero(VirtualFrame frame, BigDecimalType value, DynamicObject self, Object digits) {
        Layouts.BIG_DECIMAL.setType(self, value);
        return self;
    }

    @Specialization
    public DynamicObject create(VirtualFrame frame, BigDecimal value, DynamicObject self, NotProvided digits) {
        return create(frame, value, self, 0);
    }

    @Specialization
    public DynamicObject create(VirtualFrame frame, BigDecimal value, DynamicObject self, int digits) {
        Layouts.BIG_DECIMAL.setValue(self, value.round(new MathContext(digits, getRoundMode(frame))));
        return self;
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject createBignum(VirtualFrame frame, DynamicObject value, DynamicObject self, NotProvided digits) {
        return createBignum(frame, value, self, 0);
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject createBignum(VirtualFrame frame, DynamicObject value, DynamicObject self, int digits) {
        Layouts.BIG_DECIMAL.setValue(self,
                new BigDecimal(Layouts.BIGNUM.getValue(value))
                        .round(new MathContext(digits, getRoundMode(frame))));
        return self;
    }

    @Specialization(guards = "isRubyBigDecimal(value)")
    public DynamicObject createBigDecimal(VirtualFrame frame, DynamicObject value, DynamicObject self, NotProvided digits) {
        return createBigDecimal(frame, value, self, 0);
    }

    @Specialization(guards = "isRubyBigDecimal(value)")
    public DynamicObject createBigDecimal(VirtualFrame frame, DynamicObject value, DynamicObject self, int digits) {
        Layouts.BIG_DECIMAL.setValue(self,
                Layouts.BIG_DECIMAL.getValue(value)
                        .round(new MathContext(digits, getRoundMode(frame))));
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

    @Specialization(guards = {
            "!isRubyBignum(value)",
            "!isRubyBigDecimal(value)",
            "!isRubyString(value)"
    })
    public DynamicObject create(
            VirtualFrame frame,
            DynamicObject value,
            DynamicObject self,
            int digits,
            @Cached("createBigDecimalCastNode()") BigDecimalCastNode bigDecimalCastNode,
            @Cached("createBinaryProfile()") ConditionProfile nilProfile) {
        final Object castedValue = bigDecimalCastNode.executeObject(frame, value, getRoundMode(frame));

        if (nilProfile.profile(castedValue == nil())) {
            throw new RaiseException(coreExceptions().typeErrorCantBeCastedToBigDecimal(this));
        }

        Layouts.BIG_DECIMAL.setValue(self, ((BigDecimal) castedValue).round(new MathContext(digits, getRoundMode(frame))));

        return self;
    }

    @TruffleBoundary
    private Object getValueFromString(String string, int digits) {
        String strValue = string.trim();

        // TODO (pitr 26-May-2015): create specialization without trims and other cleanups, use rewriteOn

        switch (strValue) {
            case "NaN":
                return BigDecimalType.NAN;
            case "Infinity":
            case "+Infinity":
                return BigDecimalType.POSITIVE_INFINITY;
            case "-Infinity":
                return BigDecimalType.NEGATIVE_INFINITY;
            case "-0":
                return BigDecimalType.NEGATIVE_ZERO;
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
                return BigDecimalType.NEGATIVE_ZERO;
            } else {
                return value;
            }

        } catch (NumberFormatException e) {
            if (ZERO_PATTERN.matcher(strValue).matches()) {
                return BigDecimal.ZERO;
            }

            final BigInteger exponent = new BigInteger(result.group(3));

            if (exponent.signum() == 1) {
                return BigDecimalType.POSITIVE_INFINITY;
            }

            // TODO (pitr 21-Jun-2015): raise on underflow
            if (exponent.signum() == -1) {
                return BigDecimal.ZERO;
            }

            throw e;
        }
    }

    protected BigDecimalCastNode createBigDecimalCastNode() {
        return BigDecimalCastNodeGen.create(null, null);
    }

    protected BooleanCastNode createBooleanCastNode() {
        return BooleanCastNodeGen.create(null);
    }

    protected GetIntegerConstantNode createGetIntegerConstantNode() {
        return GetIntegerConstantNodeGen.create(null, null);
    }

}
