/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.ext;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.ext.bigdecimal.RubyBigDecimal;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.cast.BooleanCastNode;
import org.jruby.truffle.nodes.cast.BooleanCastNodeGen;
import org.jruby.truffle.nodes.cast.IntegerCastNode;
import org.jruby.truffle.nodes.cast.IntegerCastNodeGen;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.constants.GetConstantNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.constants.GetConstantNodeGen;
import org.jruby.truffle.nodes.constants.LookupConstantNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.ext.BigDecimalNodesFactory.BigDecimalCastNodeGen;
import org.jruby.truffle.nodes.ext.BigDecimalNodesFactory.BigDecimalCoerceNodeGen;
import org.jruby.truffle.nodes.ext.BigDecimalNodesFactory.CreateBigDecimalNodeFactory;
import org.jruby.truffle.nodes.ext.BigDecimalNodesFactory.GetIntegerConstantNodeGen;
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    // TODO (pitr 2015-jun-16): lazy setup when required, see https://github.com/jruby/jruby/pull/3048#discussion_r32413656

    public static final BigDecimalType BIG_DECIMAL_TYPE = new BigDecimalType();
    public static final Property VALUE_PROPERTY;
    public static final Property TYPE_PROPERTY;
    private static final HiddenKey VALUE_IDENTIFIER = new HiddenKey("value");
    private static final HiddenKey TYPE_IDENTIFIER = new HiddenKey("type");
    private static final DynamicObjectFactory BIG_DECIMAL_FACTORY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        VALUE_PROPERTY = Property.create(
                VALUE_IDENTIFIER,
                allocator.locationForType(BigDecimal.class, EnumSet.of(LocationModifier.NonNull)),
                0);
        TYPE_PROPERTY = Property.create(
                TYPE_IDENTIFIER,
                allocator.locationForType(Type.class, EnumSet.of(LocationModifier.NonNull)),
                0);
        BIG_DECIMAL_FACTORY = RubyBasicObject.LAYOUT.
                createShape(BIG_DECIMAL_TYPE).
                addProperty(TYPE_PROPERTY).
                addProperty(VALUE_PROPERTY).
                createFactory();
    }

    public static BigDecimal getBigDecimalValue(long v) {
        return BigDecimal.valueOf(v);
    }

    public static BigDecimal getBigDecimalValue(double v) {
        return BigDecimal.valueOf(v);
    }

    public static BigDecimal getBignumBigDecimalValue(RubyBasicObject v) {
        return new BigDecimal(BignumNodes.getBigIntegerValue(v));
    }

    public static BigDecimal getBigDecimalValue(RubyBasicObject bigdecimal) {
        assert RubyGuards.isRubyBigDecimal(bigdecimal);
        assert bigdecimal.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        return (BigDecimal) VALUE_PROPERTY.get(bigdecimal.getDynamicObject(), true);
    }

    public static Type getBigDecimalType(RubyBasicObject bigdecimal) {
        assert RubyGuards.isRubyBigDecimal(bigdecimal);
        assert bigdecimal.getDynamicObject().getShape().hasProperty(TYPE_IDENTIFIER);
        return (Type) TYPE_PROPERTY.get(bigdecimal.getDynamicObject(), true);
    }

    private static void setBigDecimalValue(RubyBasicObject bigdecimal, BigDecimal value) {
        assert RubyGuards.isRubyBigDecimal(bigdecimal);
        assert bigdecimal.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        VALUE_PROPERTY.setSafe(bigdecimal.getDynamicObject(), value, null);
        TYPE_PROPERTY.setSafe(bigdecimal.getDynamicObject(), Type.NORMAL, null);
    }

    private static void setBigDecimalValue(RubyBasicObject bigdecimal, Type type) {
        assert RubyGuards.isRubyBigDecimal(bigdecimal);
        assert bigdecimal.getDynamicObject().getShape().hasProperty(TYPE_IDENTIFIER);
        VALUE_PROPERTY.setSafe(bigdecimal.getDynamicObject(), BigDecimal.ZERO, null);
        TYPE_PROPERTY.setSafe(bigdecimal.getDynamicObject(), type, null);
    }

    public static RubyBasicObject createRubyBigDecimal(RubyClass rubyClass, Type type) {
        assert type != Type.NORMAL;
        return new RubyBasicObject(rubyClass, BIG_DECIMAL_FACTORY.newInstance(type, BigDecimal.ZERO));
    }

    public static RubyBasicObject createRubyBigDecimal(RubyClass rubyClass, BigDecimal value) {
        return new RubyBasicObject(rubyClass, BIG_DECIMAL_FACTORY.newInstance(Type.NORMAL, value));
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
                throw new UnsupportedOperationException();
        }
    }

    private static int nearestBiggerMultipleOf4(int value) {
        return ((value / 4) + 1) * 4;
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

    public static class BigDecimalType extends BasicObjectType {
        private BigDecimalType() {
            super();
        }
    }

    public static class RubyBigDecimalAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createRubyBigDecimal(rubyClass, BigDecimal.ZERO);
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

        public static boolean isNormal(RubyBasicObject value) {
            return getBigDecimalType(value) == Type.NORMAL;
        }

        public static boolean isNormalRubyBigDecimal(RubyBasicObject value) {
            return RubyGuards.isRubyBigDecimal(value) && getBigDecimalType(value) == Type.NORMAL;
        }

        public static boolean isSpecialRubyBigDecimal(RubyBasicObject value) {
            return RubyGuards.isRubyBigDecimal(value) && getBigDecimalType(value) != Type.NORMAL;
        }

        public static boolean isNormalZero(RubyBasicObject value) {
            return getBigDecimalValue(value).compareTo(BigDecimal.ZERO) == 0;
        }

        public static boolean isNan(RubyBasicObject value) {
            return getBigDecimalType(value) == Type.NAN;
        }

        private void setupCreateBigDecimal() {
            if (createBigDecimal == null) {
                CompilerDirectives.transferToInterpreter();
                createBigDecimal = insert(CreateBigDecimalNodeFactory.create(getContext(), getSourceSection(), null, null));
            }
        }

        protected RubyBasicObject createBigDecimal(VirtualFrame frame, Object value) {
            setupCreateBigDecimal();
            return createBigDecimal.executeCreate(frame, value);
        }

        protected RubyBasicObject createBigDecimal(VirtualFrame frame, Object value, RubyBasicObject self) {
            setupCreateBigDecimal();
            return createBigDecimal.executeCreate(frame, value, self);
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

            return limitIntegerCast.executeInteger(frame, limitCall.call(frame, getBigDecimalClass(), "limit", null));
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

            return toRoundingMode(roundModeIntegerCast.executeInteger(frame,
                    // TODO (pitr 21-Jun-2015): read the actual constant
                    roundModeCall.call(frame, getBigDecimalClass(), "mode", null, 256)));
        }

        @Deprecated
        protected RubyBasicObject createRubyBigDecimal(Type type) {
            return BigDecimalNodes.createRubyBigDecimal(getBigDecimalClass(), type);
        }

        @Deprecated
        protected RubyBasicObject createRubyBigDecimal(BigDecimal value) {
            return BigDecimalNodes.createRubyBigDecimal(getBigDecimalClass(), value);
        }

        protected RubyClass getBigDecimalClass() {
            return getContext().getCoreLibrary().getBigDecimalClass();
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
            @NodeChild(value = "self", type = RubyNode.class)
    })
    @ImportStatic(org.jruby.truffle.nodes.ext.BigDecimalNodes.Type.class)
    public abstract static class CreateBigDecimalNode extends BigDecimalCoreMethodNode {

        @Child private BigDecimalCastNode bigDecimalCast;
        @Child private CallDispatchHeadNode modeCall;
        @Child private GetIntegerConstantNode getIntegerConstant;
        @Child private BooleanCastNode booleanCast;

        public CreateBigDecimalNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            bigDecimalCast = BigDecimalCastNodeGen.create(context, sourceSection, null);
        }

        public abstract RubyBasicObject executeCreate(VirtualFrame frame, Object value, RubyBasicObject alreadyAllocatedSelf);

        public final RubyBasicObject executeCreate(VirtualFrame frame, Object value) {
            return executeCreate(frame, value, getBigDecimalClass().allocate(this));
        }

        @Specialization
        public RubyBasicObject create(VirtualFrame frame, long value, RubyBasicObject self) {
            setBigDecimalValue(self, bigDecimalCast.executeBigDecimal(frame, value));
            return self;
        }

        @Specialization
        public RubyBasicObject create(VirtualFrame frame, double value, RubyBasicObject self) {
            setBigDecimalValue(self, bigDecimalCast.executeBigDecimal(frame, value));
            return self;
        }

        @Specialization(guards = "value == NEGATIVE_INFINITY || value == POSITIVE_INFINITY")
        public RubyBasicObject createInfinity(VirtualFrame frame, Type value, RubyBasicObject self) {
            return createWithMode(frame, value, self, "EXCEPTION_INFINITY", "Computation results to 'Infinity'");
        }

        @Specialization(guards = "value == NAN")
        public RubyBasicObject createNaN(VirtualFrame frame, Type value, RubyBasicObject self) {
            return createWithMode(frame, value, self, "EXCEPTION_NaN", "Computation results to 'NaN'(Not a Number)");
        }

        @Specialization(guards = "value == NEGATIVE_ZERO")
        public RubyBasicObject createNegativeZero(VirtualFrame frame, Type value, RubyBasicObject self) {
            setBigDecimalValue(self, value);
            return self;
        }

        // FIXME (pitr 21-Jun-2015): raise on underflow

        private RubyBasicObject createWithMode(VirtualFrame frame, Type value, RubyBasicObject self,
                                               String constantName, String errorMessage) {
            setupModeCall();
            setupGetIntegerConstant();
            setupBooleanCast();

            final int exceptionConstant = getIntegerConstant.executeGetIntegerConstant(frame, constantName);
            final boolean raise = booleanCast.executeBoolean(frame,
                    modeCall.call(frame, getBigDecimalClass(), "mode", null, exceptionConstant));
            if (raise) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError(errorMessage, this));
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
                getIntegerConstant = insert(GetIntegerConstantNodeGen.create(getContext(), getSourceSection(),
                        new LiteralNode(getContext(), getSourceSection(), getBigDecimalClass())));
            }
        }

        private void setupModeCall() {
            if (modeCall == null) {
                CompilerDirectives.transferToInterpreter();
                modeCall = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
            }
        }

        @Specialization
        public RubyBasicObject create(VirtualFrame frame, BigDecimal value, RubyBasicObject self) {
            setBigDecimalValue(self, value);
            return self;
        }

        @Specialization(guards = "isRubyBignum(value)")
        public RubyBasicObject createBignum(VirtualFrame frame, RubyBasicObject value, RubyBasicObject self) {
            setBigDecimalValue(self, getBignumBigDecimalValue(value));
            return self;
        }

        @Specialization(guards = "isRubyBigDecimal(value)")
        public RubyBasicObject createBigDecimal(VirtualFrame frame, RubyBasicObject value, RubyBasicObject self) {
            setBigDecimalValue(self, getBigDecimalValue(value));
            return self;
        }
    }


    // TODO (pitr 30-may-2015): handle digits argument also for other types than just String
    @CoreMethod(names = "initialize", required = 1, optional = 1)
    public abstract static class InitializeNode extends BigDecimalCoreMethodArrayArgumentsNode {

        private final static Pattern NUMBER_PATTERN;
        private final static Pattern ZERO_PATTERN;

        static {
            final String exponent = "([eE][+-]?)?(\\d*)";
            NUMBER_PATTERN = Pattern.compile("^([+-]?\\d*\\.?\\d*" + exponent + ").*");
            ZERO_PATTERN = Pattern.compile("^[+-]?0*\\.?0*" + exponent);
        }

        @Child private CreateBigDecimalNode createBigDecimal;

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            createBigDecimal = CreateBigDecimalNodeFactory.create(context, sourceSection, null, null);
        }

        @Specialization
        public RubyBasicObject initialize(VirtualFrame frame, RubyBasicObject self, long value, NotProvided digits) {
            return createBigDecimal(frame, value, self);
        }

        @Specialization
        public RubyBasicObject initialize(VirtualFrame frame, RubyBasicObject self, double value, NotProvided digits) {
            return createBigDecimal(frame, value, self);
        }

        @Specialization(guards = "isRubyBignum(value)")
        public RubyBasicObject initializeBignum(VirtualFrame frame, RubyBasicObject self, RubyBasicObject value, NotProvided digits) {
            return createBigDecimal(frame, value, self);
        }

        @Specialization(guards = "isRubyBigDecimal(value)")
        public RubyBasicObject initializeBigDecimal(VirtualFrame frame, RubyBasicObject self, RubyBasicObject value, NotProvided digits) {
            return createBigDecimal(frame, value, self);
        }

        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(VirtualFrame frame, RubyBasicObject self, RubyBasicObject v, NotProvided digits) {
            return initializeFromString(frame, self, v, 0);
        }

        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(VirtualFrame frame, RubyBasicObject self, RubyBasicObject v, int digits) {
            return createBigDecimal(
                    frame,
                    getValueFromString(v.toString(), digits),
                    self);
        }

        @TruffleBoundary
        private Object getValueFromString(String string, int digits) {
            String strValue = string.trim();

            // TODO (pitr 26-May-2015): create specialization without trims and other cleanups, use rewriteOn,
            // string value specializations (try @Cache)

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
                // FIXME (pitr 21-Jun-2015): raise on underflow
                if (exponent.signum() == -1) {
                    return BigDecimal.ZERO;
                }

                throw e;
            }
        }
    }

    // TODO (pitr 21-Jun-2015): Check for missing coerce on OpNodess

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
        private BigDecimal addBigDecimal(RubyBasicObject a, RubyBasicObject b, MathContext mathContext) {
            return getBigDecimalValue(a).add(getBigDecimalValue(b), mathContext);
        }

        protected Object add(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return createBigDecimal(frame, addBigDecimal(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object addSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

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

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddOpNode extends AbstractAddNode {

        public AddOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object add(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return add(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object addSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return addSpecial(frame, a, b, 0);
        }

    }

    @CoreMethod(names = "add", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class AddNode extends AbstractAddNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        protected Object add(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.add(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        protected Object addSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.addSpecial(frame, a, b, precision);
        }
    }

    public abstract static class AbstractSubNode extends OpNode {

        public AbstractSubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal subBigDecimal(RubyBasicObject a, RubyBasicObject b, MathContext mathContext) {
            return getBigDecimalValue(a).subtract(getBigDecimalValue(b), mathContext);
        }

        protected Object subNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return createBigDecimal(frame, subBigDecimal(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object subSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

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
                return createBigDecimal(frame, getBigDecimalValue(b).negate());
            }
        }
    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubOpNode extends AbstractSubNode {

        public SubOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object subNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return subNormal(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object subSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return subSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(names = "sub", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class SubNode extends AbstractSubNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object subNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.subNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)" })
        public Object subSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.subSpecial(frame, a, b, precision);
        }
    }


    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)" })
        public Object negNormal(VirtualFrame frame, RubyBasicObject value) {
            return createBigDecimal(frame, getBigDecimalValue(value).negate());
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)" })
        public Object negNormalZero(VirtualFrame frame, RubyBasicObject value) {
            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object negSpecial(VirtualFrame frame, RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, BigDecimal.ZERO);
                case NAN:
                    return value;
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

    public abstract static class AbstractMultNode extends OpNode {

        private final ConditionProfile zeroNormal = ConditionProfile.createBinaryProfile();

        public AbstractMultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private Object multBigDecimalWithProfile(RubyBasicObject a, RubyBasicObject b, MathContext mathContext) {
            final BigDecimal bBigDecimal = getBigDecimalValue(b);

            if (zeroNormal.profile(isNormalZero(a) && bBigDecimal.signum() == -1)) {
                return Type.NEGATIVE_ZERO;
            }

            return multBigDecimal(getBigDecimalValue(a), bBigDecimal, mathContext);
        }

        @TruffleBoundary
        private Object multBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
            return a.multiply(b, mathContext);
        }

        protected Object mult(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return createBigDecimal(frame, multBigDecimalWithProfile(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object multNormalSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return multSpecialNormal(frame, b, a, precision);
        }

        protected Object multSpecialNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            switch (getBigDecimalType(a)) {
                case NAN:
                    return createBigDecimal(frame, Type.NAN);
                case NEGATIVE_ZERO:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                        case -1:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                case POSITIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                    }
                case NEGATIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                    }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        protected Object multSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

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

            throw new UnsupportedOperationException();
        }
    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MultOpNode extends AbstractMultNode {

        public MultOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object mult(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return mult(frame, a, b, getLimit(frame));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multNormalSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return multSpecialNormal(frame, b, a, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object multSpecialNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return multSpecialNormal(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return multSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(names = "mult", required = 2)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class MultNode extends AbstractMultNode {

        public MultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object mult(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.mult(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multNormalSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.multNormalSpecial(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object multSpecialNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.multSpecialNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object multSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.multSpecial(frame, a, b, precision);
        }
    }

    public abstract static class AbstractDivNode extends OpNode {

        private final ConditionProfile normalZero = ConditionProfile.createBinaryProfile();

        public AbstractDivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private Object divBigDecimalWithProfile(RubyBasicObject a, RubyBasicObject b, MathContext mathContext) {
            final BigDecimal aBigDecimal = getBigDecimalValue(a);
            final BigDecimal bBigDecimal = getBigDecimalValue(b);
            if (normalZero.profile(bBigDecimal.signum() == 0)) {
                switch (aBigDecimal.signum()) {
                    case 1:
                        return Type.POSITIVE_INFINITY;
                    case 0:
                        return Type.NAN;
                    case -1:
                        return Type.NEGATIVE_INFINITY;
                    default:
                        throw new UnsupportedOperationException();
                }
            } else {
                return divBigDecimal(aBigDecimal, bBigDecimal, mathContext);
            }
        }

        @TruffleBoundary
        private BigDecimal divBigDecimal(BigDecimal a, BigDecimal b, MathContext mathContext) {
            return a.divide(b, mathContext);
        }

        protected Object div(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return createBigDecimal(frame, divBigDecimalWithProfile(a, b, new MathContext(precision, getRoundMode(frame))));
        }

        protected Object divNormalSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            switch (getBigDecimalType(b)) {
                case NAN:
                    return createBigDecimal(frame, Type.NAN);
                case NEGATIVE_ZERO:
                    switch (getBigDecimalValue(a).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                    }
                case POSITIVE_INFINITY:
                    switch (getBigDecimalValue(a).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                        case -1:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                    }
                case NEGATIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                        case 0:
                        case -1:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        protected Object divSpecialNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            switch (getBigDecimalType(a)) {
                case NAN:
                    return createBigDecimal(frame, Type.NAN);
                case NEGATIVE_ZERO:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return createBigDecimal(frame, Type.NEGATIVE_ZERO);
                        case 0:
                            return createBigDecimal(frame, Type.NAN);
                        case -1:
                            return createBigDecimal(frame, BigDecimal.ZERO);
                    }
                case POSITIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                        case -1:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                    }
                case NEGATIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                        case 0:
                            return createBigDecimal(frame, Type.NEGATIVE_INFINITY);
                        case -1:
                            return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                    }
                default:
                    throw new UnsupportedOperationException();
            }
        }

        protected Object divSpecialSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

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

    @CoreMethod(names = { "/", "quo" }, required = 1)
    public abstract static class DivOpNode extends AbstractDivNode {

        public DivOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object div(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            final int sumOfPrecisions = getBigDecimalValue(a).precision() + getBigDecimalValue(b).precision();
            final int defaultPrecision = nearestBiggerMultipleOf4(sumOfPrecisions) * 2;
            final int limit = getLimit(frame);
            return div(frame, a, b, (limit > 0 && limit < defaultPrecision) ? limit : defaultPrecision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divNormalSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return divNormalSpecial(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object divSpecialNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return divSpecialNormal(frame, a, b, 0);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divSpecialSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return divSpecialSpecial(frame, a, b, 0);
        }
    }

    @CoreMethod(names = "div", required = 1, optional = 1)
    @NodeChild(value = "precision", type = RubyNode.class)
    public abstract static class DivNode extends AbstractDivNode {

        public DivNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object div(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.div(frame, a, b, precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divNormalSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.divNormalSpecial(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public Object divSpecialNormal(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.divSpecialNormal(frame, a, b, precision);
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isSpecialRubyBigDecimal(b)" })
        public Object divSpecialSpecial(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b, int precision) {
            return super.divSpecialSpecial(frame, a, b, precision);
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

        public PowerNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal power(BigDecimal value, int exponent, MathContext mathContext) {
            return value.pow(exponent, mathContext);
        }

        @Specialization(guards = "isNormal(a)")
        public Object power(VirtualFrame frame, RubyBasicObject a, int exponent, NotProvided precision) {
            return power(frame, a, exponent, getLimit(frame));
        }

        @Specialization(guards = { "isNormal(a)" })
        public Object power(VirtualFrame frame, RubyBasicObject a, int exponent, int precision) {
            final BigDecimal aBigDecimal = getBigDecimalValue(a);
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
                        power(getBigDecimalValue(a), exponent,
                                new MathContext(newPrecision, getRoundMode(frame))));
            }
        }

        @TruffleBoundary
        private int getDigits(BigDecimal value) {
            return value.abs().unscaledValue().toString().length();
        }

        @Specialization(guards = "!isNormal(a)")
        public Object power(VirtualFrame frame, RubyBasicObject a, int exponent, Object precision) {
            switch (getBigDecimalType(a)) {
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
                    throw new UnsupportedOperationException();
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

        public SqrtNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract Object executeSqrt(VirtualFrame frame, RubyBasicObject value, int precision);

        @TruffleBoundary
        private BigDecimal sqrt(BigDecimal value, MathContext mathContext) {
            return RubyBigDecimal.bigSqrt(value, mathContext);
        }

        @Specialization(guards = { "precision < 0" })
        public Object sqrtNegativePrecision(VirtualFrame frame, RubyBasicObject a, int precision) {
            throw new RaiseException(getContext().getCoreLibrary().argumentError("precision must be positive", this));
        }

        @Specialization(guards = { "precision == 0" })
        public Object sqrtZeroPrecision(VirtualFrame frame, RubyBasicObject a, int precision) {
            return executeSqrt(frame, a, 1);
        }

        @Specialization(guards = { "isNormal(a)", "precision > 0" })
        public Object sqrt(VirtualFrame frame, RubyBasicObject a, int precision) {
            final BigDecimal valueBigDecimal = getBigDecimalValue(a);
            if (positiveValueProfile.profile(valueBigDecimal.signum() >= 0)) {
                return createBigDecimal(frame, sqrt(valueBigDecimal, new MathContext(precision, getRoundMode(frame))));
            } else {
                throw new RaiseException(getContext().getCoreLibrary().floatDomainError("(VpSqrt) SQRT(negative value)", this));
            }
        }

        @Specialization(guards = { "!isNormal(a)", "precision > 0" })
        public Object sqrtSpecial(VirtualFrame frame, RubyBasicObject a, int precision) {
            switch (getBigDecimalType(a)) {
                case NAN:
                    throw new RaiseException(getContext().getCoreLibrary().floatDomainError("(VpSqrt) SQRT(NaN value)", this));
                case POSITIVE_INFINITY:
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    throw new RaiseException(getContext().getCoreLibrary().floatDomainError("(VpSqrt) SQRT(negative value)", this));
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, sqrt(BigDecimal.ZERO, new MathContext(precision, getRoundMode(frame))));
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    @CoreMethod(names = "<=>", required = 1)
    public abstract static class CompareNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private int compareBigDecimal(RubyBasicObject a, BigDecimal b) {
            return getBigDecimalValue(a).compareTo(b);
        }

        @Specialization(guards = "isNormal(a)")
        public int compare(RubyBasicObject a, long b) {
            return compareBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public int compare(RubyBasicObject a, double b) {
            return compareBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = { "isNormal(a)", "isRubyBignum(b)" })
        public int compare(RubyBasicObject a, RubyBasicObject b) {
            return compareBigDecimal(a, getBignumBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isNormalRubyBigDecimal(b)" })
        public int compareNormal(RubyBasicObject a, RubyBasicObject b) {
            return compareBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(RubyBasicObject a, long b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(RubyBasicObject a, double b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = { "!isNormal(a)", "isRubyBignum(b)" })
        public Object compareSpecialBignum(RubyBasicObject a, RubyBasicObject b) {
            return compareSpecial(a, createRubyBigDecimal(getBignumBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNan(a)" })
        public Object compareSpecialNan(RubyBasicObject a, RubyBasicObject b) {
            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)",
                "isNormal(a) || !isNan(a)" })
        public Object compareSpecial(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

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
                aCompare = getBigDecimalValue(a);
            }
            if (bType == Type.NEGATIVE_ZERO) {
                bCompare = BigDecimal.ZERO;
            } else {
                bCompare = getBigDecimalValue(b);
            }

            return aCompare.compareTo(bCompare);
        }

        @Specialization(guards = "isNil(b)")
        public Object compareNil(RubyBasicObject a, RubyBasicObject b) {
            return nil();
        }

        @Specialization(guards = {
                "!isRubyBigDecimal(b)",
                "!isNil(b)" })
        public Object compareCoerced(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :<=>, b", "b", b);
        }

    }

    // TODO (pitr 20-May-2015): compare Ruby implementation of #== with a Java one

    @CoreMethod(names = "zero?")
    public abstract static class ZeroNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormal(value)")
        public boolean zeroNormal(RubyBasicObject value) {
            return getBigDecimalValue(value).compareTo(BigDecimal.ZERO) == 0;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean zeroSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case NEGATIVE_ZERO:
                    return true;
                default:
                    return false;
            }
        }
    }

    @NodeChildren({
            @NodeChild(value = "name", type = RubyNode.class),
            @NodeChild(value = "module", type = RubyNode.class),
            @NodeChild(value = "getConst", type = GetConstantNode.class, executeWith = { "module", "name" }),
            @NodeChild(value = "coerce", type = ToIntNode.class, executeWith = "getConst"),
            @NodeChild(value = "cast", type = IntegerCastNode.class, executeWith = "coerce")
    })
    public abstract static class GetIntegerConstantNode extends RubyNode {

        public GetIntegerConstantNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static GetIntegerConstantNode create(RubyContext context, SourceSection sourceSection) {
            return create(context, sourceSection, null);
        }

        public static GetIntegerConstantNode create(RubyContext context, SourceSection sourceSection, RubyNode module) {
            return GetIntegerConstantNodeGen.create(
                    context, sourceSection, null, module,
                    GetConstantNodeGen.create(context, sourceSection, null, null,
                            LookupConstantNodeGen.create(context, sourceSection, LexicalScope.NONE, null, null)),
                    ToIntNodeGen.create(context, sourceSection, null),
                    IntegerCastNodeGen.create(context, sourceSection, null));
        }

        public abstract IntegerCastNode getCast();

        public abstract int executeGetIntegerConstant(VirtualFrame frame, String name, RubyModule module);

        public abstract int executeGetIntegerConstant(VirtualFrame frame, String name);

        @Specialization
        public int doInteger(String name,
                             RubyModule module,
                             Object constValue,
                             Object coercedConstValue,
                             int castedValue) {
            return castedValue;
        }
    }

    @CoreMethod(names = "sign")
    public abstract static class SignNode extends BigDecimalCoreMethodArrayArgumentsNode {

        final private ConditionProfile positive = ConditionProfile.createBinaryProfile();
        @Child private GetIntegerConstantNode sign;

        public SignNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            sign = GetIntegerConstantNodeGen.create(context, sourceSection,
                    new LiteralNode(context, sourceSection, getBigDecimalClass()));
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)" })
        public int signNormalZero(VirtualFrame frame, RubyBasicObject value) {
            return sign.executeGetIntegerConstant(frame, "SIGN_POSITIVE_ZERO");
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)" })
        public int signNormal(VirtualFrame frame, RubyBasicObject value) {
            if (positive.profile(getBigDecimalValue(value).signum() > 0)) {
                return sign.executeGetIntegerConstant(frame, "SIGN_POSITIVE_FINITE");
            } else {
                return sign.executeGetIntegerConstant(frame, "SIGN_NEGATIVE_FINITE");
            }
        }

        @Specialization(guards = "!isNormal(value)")
        public int signSpecial(VirtualFrame frame, RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case NEGATIVE_INFINITY:
                    return sign.executeGetIntegerConstant(frame, "SIGN_NEGATIVE_INFINITE");
                case POSITIVE_INFINITY:
                    return sign.executeGetIntegerConstant(frame, "SIGN_POSITIVE_INFINITE");
                case NEGATIVE_ZERO:
                    return sign.executeGetIntegerConstant(frame, "SIGN_NEGATIVE_ZERO");
                case NAN:
                    return sign.executeGetIntegerConstant(frame, "SIGN_NaN");
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

    @CoreMethod(names = "nan?")
    public abstract static class NanNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public NanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormal(value)")
        public boolean nanNormal(RubyBasicObject value) {
            return false;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean nanSpecial(RubyBasicObject value) {
            return getBigDecimalType(value) == Type.NAN;
        }

    }

    @CoreMethod(names = "exponent")
    public abstract static class ExponentNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ExponentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)" })
        public long exponent(RubyBasicObject value) {
            final BigDecimal val = getBigDecimalValue(value).abs().stripTrailingZeros();
            return val.precision() - val.scale();
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)" })
        public int exponentZero(RubyBasicObject value) {
            return 0;
        }

        @Specialization(guards = "!isNormal(value)")
        public int exponentSpecial(RubyBasicObject value) {
            return 0;
        }

    }

    @CoreMethod(names = "abs")
    public abstract static class AbsNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal abs(RubyBasicObject value) {
            return getBigDecimalValue(value).abs();
        }

        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject abs(VirtualFrame frame, RubyBasicObject value) {
            return createBigDecimal(frame, abs(value));
        }

        @Specialization(guards = "!isNormal(value)")
        public RubyBasicObject absSpecial(VirtualFrame frame, RubyBasicObject value) {
            final Type type = getBigDecimalType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    return createBigDecimal(frame, Type.POSITIVE_INFINITY);
                case NEGATIVE_ZERO:
                    return createBigDecimal(frame, BigDecimal.ZERO);
                case POSITIVE_INFINITY:
                case NAN:
                    return createBigDecimal(frame, type);
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

    @CoreMethod(names = "round", optional = 2)
    public abstract static class RoundNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public RoundNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private BigDecimal round(RubyBasicObject value, int digit, RoundingMode roundingMode) {
            final BigDecimal valueBigDecimal = getBigDecimalValue(value);

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
        public RubyBasicObject round(VirtualFrame frame, RubyBasicObject value, NotProvided digit, NotProvided roundingMode) {
            return createBigDecimal(frame, round(value, 0, getRoundMode(frame)));
        }

        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject round(VirtualFrame frame, RubyBasicObject value, int digit, NotProvided roundingMode) {
            return createBigDecimal(frame, round(value, digit, getRoundMode(frame)));
        }

        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject round(VirtualFrame frame, RubyBasicObject value, int digit, int roundingMode) {
            return createBigDecimal(frame, round(value, digit, toRoundingMode(roundingMode)));
        }

        @Specialization(guards = "!isNormal(value)")
        public RubyBasicObject roundSpecial(VirtualFrame frame, RubyBasicObject value, Object precision, Object roundingMode) {
            switch (getBigDecimalType(value)) {
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError("Computation results to '-Infinity'", this));
                case POSITIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError("Computation results to 'Infinity'", this));
                case NEGATIVE_ZERO:
                    return value;
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError("Computation results to 'NaN'(Not a Number)", this));
                default:
                    throw new UnsupportedOperationException();

            }
        }
    }

    @CoreMethod(names = "finite?")
    public abstract static class FiniteNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public FiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormal(value)")
        public boolean finiteNormal(RubyBasicObject value) {
            return true;
        }

        @Specialization(guards = "!isNormal(value)")
        public boolean finiteSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
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

        public InfiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject infiniteNormal(RubyBasicObject value) {
            return nil();
        }

        @Specialization(guards = "!isNormal(value)")
        public Object infiniteSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
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

        public PrecsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject precsNormal(RubyBasicObject value) {
            final BigDecimal bigDecimalValue = getBigDecimalValue(value).abs();
            return createArray(
                    new int[]{
                            bigDecimalValue.stripTrailingZeros().unscaledValue().toString().length(),
                            nearestBiggerMultipleOf4(bigDecimalValue.unscaledValue().toString().length()) },
                    2);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object precsSpecial(RubyBasicObject value) {
            return createArray(new int[]{ 1, 1 }, 2);
        }

    }

    @CoreMethod(names = { "to_s", "inspect" })
    public abstract static class ToSNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject toSNormal(RubyBasicObject value) {
            final BigDecimal bigDecimal = getBigDecimalValue(value);
            final boolean negative = bigDecimal.signum() == -1;

            return createString((negative ? "-" : "") + "0." +
                    (negative ? bigDecimal.unscaledValue().toString().substring(1) : bigDecimal.unscaledValue()) +
                    "E" + (bigDecimal.precision() - bigDecimal.scale()));
        }

        @TruffleBoundary
        @Specialization(guards = "!isNormal(value)")
        public RubyBasicObject toSSpecial(RubyBasicObject value) {
            return createString(getBigDecimalType(value).getRepresentation());
        }

    }

    @CoreMethod(names = "to_f")
    public abstract static class ToFNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public ToFNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public double toFNormal(RubyBasicObject value) {
            return getBigDecimalValue(value).doubleValue();
        }

        @Specialization(guards = "!isNormal(value)")
        public double toFSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case NEGATIVE_INFINITY:
                    return Double.NEGATIVE_INFINITY;
                case POSITIVE_INFINITY:
                    return Double.POSITIVE_INFINITY;
                case NEGATIVE_ZERO:
                    return 0.0D;
                case NAN:
                    return Double.NaN;
                default:
                    throw new UnsupportedOperationException();
            }
        }

    }

    @RubiniusOnly
    @CoreMethod(names = "unscaled", visibility = Visibility.PRIVATE)
    public abstract static class UnscaledNode extends BigDecimalCoreMethodArrayArgumentsNode {

        public UnscaledNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject unscaled(RubyBasicObject value) {
            return createString(getBigDecimalValue(value).abs().stripTrailingZeros().unscaledValue().toString());
        }

        @Specialization(guards = "!isNormal(value)")
        public RubyBasicObject unscaledSpecial(RubyBasicObject value) {
            final String type = getBigDecimalType(value).getRepresentation();
            return createString(type.startsWith("-") ? type.substring(1) : type);
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
        public Object toINormal(RubyBasicObject value) {
            return fixnumOrBignum.fixnumOrBignum(getBigDecimalValue(value).toBigInteger());
        }

        @Specialization(guards = "!isNormal(value)")
        public int toISpecial(RubyBasicObject value) {
            final Type type = getBigDecimalType(value);
            switch (type) {
                case NEGATIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case POSITIVE_INFINITY:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case NAN:
                    CompilerDirectives.transferToInterpreter();
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case NEGATIVE_ZERO:
                    return 0;
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * Casts a value into a BigDecimal.
     */
    @NodeChild(value = "value", type = RubyNode.class)
    @ImportStatic(BigDecimalCoreMethodNode.class)
    public abstract static class BigDecimalCastNode extends RubyNode {
        public BigDecimalCastNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public abstract BigDecimal executeBigDecimal(VirtualFrame frame, Object value);

        public abstract Object executeObject(VirtualFrame frame, Object value);

        @Specialization
        public BigDecimal doInt(long value) {
            return BigDecimal.valueOf(value);
        }

        @Specialization
        public BigDecimal doDouble(double value) {
            return BigDecimal.valueOf(value);
        }

        @Specialization(guards = "isRubyBignum(value)")
        public BigDecimal doBignum(RubyBasicObject value) {
            return new BigDecimal(BignumNodes.getBigIntegerValue(value));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(value)")
        public BigDecimal doBigDecimal(RubyBasicObject value) {
            return getBigDecimalValue(value);
        }

        @Fallback
        public RubyBasicObject doBigDecimalFallback(Object value) {
            return nil();
        }
        // TODO (pitr 22-Jun-2015): How to better communicate failure without throwing
    }

    /**
     * Coerces a value into a BigDecimal.
     */
    @NodeChildren({
            @NodeChild(value = "value", type = RubyNode.class),
            @NodeChild(value = "cast", type = BigDecimalCastNode.class, executeWith = "value")

    })
    public abstract static class BigDecimalCoerceNode extends RubyNode {
        @Child private CreateBigDecimalNode createBigDecimal;

        public BigDecimalCoerceNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static BigDecimalCoerceNode create(RubyContext context, SourceSection sourceSection, RubyNode value) {
            return BigDecimalCoerceNodeGen.create(context, sourceSection, value,
                    BigDecimalCastNodeGen.create(context, sourceSection, null));
        }

        private void setupCreateBigDecimal() {
            if (createBigDecimal == null) {
                CompilerDirectives.transferToInterpreter();
                createBigDecimal = insert(CreateBigDecimalNodeFactory.create(getContext(), getSourceSection(), null, null));
            }
        }

        protected RubyBasicObject createBigDecimal(VirtualFrame frame, Object value) {
            setupCreateBigDecimal();
            return createBigDecimal.executeCreate(frame, value);
        }

        public abstract RubyBasicObject executeBigDecimal(VirtualFrame frame, Object value);

        @Specialization
        public RubyBasicObject doBigDecimal(VirtualFrame frame, Object value, BigDecimal cast) {
            return createBigDecimal(frame, cast);
        }

        @Specialization(guards = { "isRubyBigDecimal(value)", "isNil(cast)" })
        public RubyBasicObject doBigDecimal(RubyBasicObject value, RubyBasicObject cast) {
            return value;
        }

        // TODO (pitr 22-Jun-2015): deal with not-coerce-able values

    }

}
