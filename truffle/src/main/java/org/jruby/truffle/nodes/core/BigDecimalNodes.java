/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.core;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;

import com.oracle.truffle.api.utilities.ConditionProfile;
import org.jruby.truffle.nodes.cast.IntegerCastNode;
import org.jruby.truffle.nodes.cast.IntegerCastNodeGen;
import org.jruby.truffle.nodes.coerce.ToIntNode;
import org.jruby.truffle.nodes.coerce.ToIntNodeGen;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.NotProvided;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumSet;
import java.util.regex.Pattern;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    public static final BigDecimalType BIG_DECIMAL_TYPE = new BigDecimalType();
    public static final Property VALUE_PROPERTY;
    public static final Property TYPE_PROPERTY;
    // TODO (pitr 22-May-2015): would it make sense to have two shapes, special(type) and normal(type, value)?
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

    public static BigDecimal getBigDecimalValue(RubyBignum v) {
        return new BigDecimal(BignumNodes.getBigIntegerValue(v));
    }

    public static BigDecimal getBigDecimalValue(RubyBasicObject bignum) {
        assert bignum.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        return (BigDecimal) VALUE_PROPERTY.get(bignum.getDynamicObject(), true);
    }

    public static Type getBigDecimalType(RubyBasicObject bignum) {
        assert bignum.getDynamicObject().getShape().hasProperty(TYPE_IDENTIFIER);
        return (Type) TYPE_PROPERTY.get(bignum.getDynamicObject(), true);
    }

    private static void setBigDecimalValue(RubyBasicObject bignum, BigDecimal value) {
        assert bignum.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        VALUE_PROPERTY.setSafe(bignum.getDynamicObject(), value, null);
        TYPE_PROPERTY.setSafe(bignum.getDynamicObject(), Type.NORMAL, null);
    }

    private static void setBigDecimalValue(RubyBasicObject bignum, Type type) {
        assert bignum.getDynamicObject().getShape().hasProperty(TYPE_IDENTIFIER);
        VALUE_PROPERTY.setSafe(bignum.getDynamicObject(), BigDecimal.ZERO, null);
        TYPE_PROPERTY.setSafe(bignum.getDynamicObject(), type, null);
    }

    public static RubyBasicObject createRubyBigDecimal(RubyClass rubyClass, Type type) {
        assert type != Type.NORMAL;
        return new RubyBasicObject(rubyClass, BIG_DECIMAL_FACTORY.newInstance(type, BigDecimal.ZERO));
    }

    public static RubyBasicObject createRubyBigDecimal(RubyClass rubyClass, BigDecimal value) {
        return new RubyBasicObject(rubyClass, BIG_DECIMAL_FACTORY.newInstance(Type.NORMAL, value));
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

    public abstract static class BigDecimalCoreMethodNode extends CoreMethodArrayArgumentsNode {

        public BigDecimalCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static boolean isNormal(RubyBasicObject value) {
            return getBigDecimalType(value) == Type.NORMAL;
        }

        public static boolean isNormalZero(RubyBasicObject value) {
            return getBigDecimalValue(value).compareTo(BigDecimal.ZERO) == 0;
        }

        public static boolean isNan(RubyBasicObject value) {
            return getBigDecimalType(value) == Type.NAN;
        }

        protected RubyBasicObject createRubyBigDecimal(Type type) {
            return BigDecimalNodes.createRubyBigDecimal(getContext().getCoreLibrary().getBigDecimalClass(), type);
        }

        protected RubyBasicObject createRubyBigDecimal(BigDecimal value) {
            return BigDecimalNodes.createRubyBigDecimal(getContext().getCoreLibrary().getBigDecimalClass(), value);
        }

    }

    // TODO (pitr 30-may-2015): handle digits argument also for other types than just String
    @CoreMethod(names = "initialize", required = 1, optional = 1)
    public abstract static class InitializeNode extends BigDecimalCoreMethodNode {

        private final static Pattern NUMBER_PATTERN;
        private final static Pattern ZERO_PATTERN;

        static {
            final String exponent = "([eE][+-]?)?\\d*";
            NUMBER_PATTERN = Pattern.compile("^([+-]?\\d*\\.?\\d*" + exponent + ").*");
            ZERO_PATTERN = Pattern.compile("^[+-]?0*\\.?0*" + exponent);
        }

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public RubyBasicObject initialize(RubyBasicObject self, long value, NotProvided digits) {
            setBigDecimalValue(self, getBigDecimalValue(value));
            return self;
        }

        @Specialization
        public RubyBasicObject initialize(RubyBasicObject self, double value, NotProvided digits) {
            setBigDecimalValue(self, getBigDecimalValue(value));
            return self;
        }

        @Specialization()
        public RubyBasicObject initialize(RubyBasicObject self, RubyBignum value, NotProvided digits) {
            setBigDecimalValue(self, getBigDecimalValue(value));
            return self;
        }

        @Specialization(guards = "isRubyBigDecimal(value)")
        public RubyBasicObject initialize(RubyBasicObject self, RubyBasicObject value, NotProvided digits) {
            setBigDecimalValue(self, getBigDecimalValue(value));
            return self;
        }

        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(RubyBasicObject self, RubyBasicObject v, NotProvided digits) {
            return initializeFromString(self, v, 0);
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(RubyBasicObject self, RubyBasicObject v, int digits) {
            String strValue = v.toString().trim();

            // TODO (pitr 26-May-2015): create specialization without trims and other cleanups, use rewriteOn,
            // string value specializations (try @Cache)

            switch (strValue) {
                case "NaN":
                    setBigDecimalValue(self, Type.NAN);
                    return self;
                case "Infinity":
                case "+Infinity":
                    setBigDecimalValue(self, Type.POSITIVE_INFINITY);
                    return self;
                case "-Infinity":
                    setBigDecimalValue(self, Type.NEGATIVE_INFINITY);
                    return self;
                case "-0":
                    setBigDecimalValue(self, Type.NEGATIVE_ZERO);
                    return self;
            }

            // Convert String to Java understandable format (for BigDecimal).
            strValue = strValue.replaceFirst("[dD]", "E");                  // 1. MRI allows d and D as exponent separators
            strValue = strValue.replaceAll("_", "");                        // 2. MRI allows underscores anywhere
            strValue = NUMBER_PATTERN.matcher(strValue).replaceFirst("$1"); // 3. MRI ignores the trailing junk

            try {
                final BigDecimal value = new BigDecimal(strValue, new MathContext(digits));
                setBigDecimalValue(self, value);
                if (value.compareTo(BigDecimal.ZERO) == 0 && strValue.startsWith("-"))
                    setBigDecimalValue(self, Type.NEGATIVE_ZERO);

            } catch (NumberFormatException e) {
                if (ZERO_PATTERN.matcher(strValue).matches()) {
                    setBigDecimalValue(self, BigDecimal.ZERO);
                } else {
                    throw e;
                }
            }
            return self;
        }
    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddOpNode extends BigDecimalCoreMethodNode {

        public AddOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object addBigDecimal(RubyBasicObject a, BigDecimal b) {
            return createRubyBigDecimal(getBigDecimalValue(a).add(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object add(RubyBasicObject a, long b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object add(RubyBasicObject a, double b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object add(RubyBasicObject a, RubyBignum b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object add(RubyBasicObject a, RubyBasicObject b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object addSpecial(RubyBasicObject a, long b) {
            return addSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object addSpecial(RubyBasicObject a, double b) {
            return addSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object addSpecial(RubyBasicObject a, RubyBignum b) {
            return addSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"})
        public Object addSpecial(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

            if (aType == Type.NAN || bType == Type.NAN ||
                    (aType == Type.POSITIVE_INFINITY && bType == Type.NEGATIVE_INFINITY) ||
                    (aType == Type.NEGATIVE_INFINITY && bType == Type.POSITIVE_INFINITY))
                return createRubyBigDecimal(Type.NAN);

            if (aType == Type.POSITIVE_INFINITY || bType == Type.POSITIVE_INFINITY)
                return createRubyBigDecimal(Type.POSITIVE_INFINITY);

            if (aType == Type.NEGATIVE_INFINITY || bType == Type.NEGATIVE_INFINITY)
                return createRubyBigDecimal(Type.NEGATIVE_INFINITY);

            // one is NEGATIVE_ZERO and second is NORMAL
            if (isNormal(a))
                return a;
            else
                return b;
        }

        // TODO (pitr 28-may-2015): should it support coerce?
    }

    @CoreMethod(names = "add", required = 2)
    public abstract static class AddNode extends BigDecimalCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object addBigDecimal(RubyBasicObject a, BigDecimal b, int precision) {
            return createRubyBigDecimal(getBigDecimalValue(a).add(b, new MathContext(precision)));
        }

        @Specialization(guards = "isNormal(a)")
        public Object add(RubyBasicObject a, long b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isNormal(a)")
        public Object add(RubyBasicObject a, double b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isNormal(a)")
        public Object add(RubyBasicObject a, RubyBignum b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object add(RubyBasicObject a, RubyBasicObject b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubOpNode extends BigDecimalCoreMethodNode {

        public SubOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object subBigDecimal(RubyBasicObject a, BigDecimal b) {
            return createRubyBigDecimal(getBigDecimalValue(a).subtract(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object sub(RubyBasicObject a, long b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object sub(RubyBasicObject a, double b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object sub(RubyBasicObject a, RubyBignum b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object subNormal(RubyBasicObject a, RubyBasicObject b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object subSpecial(RubyBasicObject a, long b) {
            return subSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object subSpecial(RubyBasicObject a, double b) {
            return subSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object subSpecial(RubyBasicObject a, RubyBignum b) {
            return subSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)"})
        public Object subSpecial(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

            if (aType == Type.NAN || bType == Type.NAN ||
                    (aType == Type.POSITIVE_INFINITY && bType == Type.POSITIVE_INFINITY) ||
                    (aType == Type.NEGATIVE_INFINITY && bType == Type.NEGATIVE_INFINITY))
                return createRubyBigDecimal(Type.NAN);

            if (aType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY)
                return createRubyBigDecimal(Type.POSITIVE_INFINITY);

            if (aType == Type.NEGATIVE_INFINITY || bType == Type.POSITIVE_INFINITY)
                return createRubyBigDecimal(Type.NEGATIVE_INFINITY);

            // one is NEGATIVE_ZERO and second is NORMAL
            if (isNormal(a))
                return a;
            else
                return createRubyBigDecimal(getBigDecimalValue(b).negate());
        }

    }

    @CoreMethod(names = "-@")
    public abstract static class NegNode extends BigDecimalCoreMethodNode {

        public NegNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"})
        public Object negNormal(RubyBasicObject value) {
            return createRubyBigDecimal(getBigDecimalValue(value).negate());
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"})
        public Object negNormalZero(RubyBasicObject value) {
            return createRubyBigDecimal(Type.NEGATIVE_ZERO);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object negSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case POSITIVE_INFINITY:
                    return createRubyBigDecimal(Type.NEGATIVE_INFINITY);
                case NEGATIVE_INFINITY:
                    return createRubyBigDecimal(Type.POSITIVE_INFINITY);
                case NEGATIVE_ZERO:
                    return createRubyBigDecimal(BigDecimal.ZERO);
                case NAN:
                    return value;
                default:
                    throw new RuntimeException(); // never reached

            }
        }

    }

    @CoreMethod(names = "sub", required = 2)
    public abstract static class SubNode extends BigDecimalCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object subBigDecimal(RubyBasicObject a, BigDecimal b, int precision) {
            return createRubyBigDecimal(getBigDecimalValue(a).subtract(b, new MathContext(precision)));
        }

        @Specialization(guards = "isNormal(a)")
        public Object sub(RubyBasicObject a, long b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isNormal(a)")
        public Object sub(RubyBasicObject a, double b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isNormal(a)")
        public Object sub(RubyBasicObject a, RubyBignum b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object sub(RubyBasicObject a, RubyBasicObject b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }
    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MultOpNode extends BigDecimalCoreMethodNode {

        private final ConditionProfile zeroNormal = ConditionProfile.createBinaryProfile();

        public MultOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object multBigDecimal(RubyBasicObject a, BigDecimal b) {
            if (zeroNormal.profile(isNormalZero(a) && b.signum() == -1))
                return createRubyBigDecimal(Type.NEGATIVE_ZERO);
            return createRubyBigDecimal(getBigDecimalValue(a).multiply(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object mult(RubyBasicObject a, long b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object mult(RubyBasicObject a, double b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object mult(RubyBasicObject a, RubyBignum b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object mult(RubyBasicObject a, RubyBasicObject b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "!isNormal(b)"})
        public Object multNormalSpecial(RubyBasicObject a, RubyBasicObject b) {
            return multSpecialNormal(b, a);
        }

        @Specialization(guards = "!isNormal(a)")
        public Object multSpecialNormal(RubyBasicObject a, long b) {
            return multSpecialNormal(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object multSpecialNormal(RubyBasicObject a, double b) {
            return multSpecialNormal(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object multSpecialNormal(RubyBasicObject a, RubyBignum b) {
            return multSpecialNormal(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object multSpecialNormal(RubyBasicObject a, RubyBasicObject b) {
            switch (getBigDecimalType(a)) {
                case NAN:
                    return a;
                case NEGATIVE_ZERO:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                        case 0:
                            return a;
                        case -1:
                            return createRubyBigDecimal(BigDecimal.ZERO);
                    }
                case POSITIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return a;
                        case 0:
                            return createRubyBigDecimal(Type.NAN);
                        case -1:
                            return createRubyBigDecimal(Type.NEGATIVE_INFINITY);
                    }
                case NEGATIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return a;
                        case 0:
                            return createRubyBigDecimal(Type.NAN);
                        case -1:
                            return createRubyBigDecimal(Type.POSITIVE_INFINITY);
                    }
            }
            throw new RuntimeException();
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isRubyBigDecimal(b)",
                "!isNormal(b)"})
        public Object multSpecial(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

            if (aType == Type.NAN || bType == Type.NAN)
                return createRubyBigDecimal(Type.NAN);
            if (aType == Type.NEGATIVE_ZERO && bType == Type.NEGATIVE_ZERO)
                return createRubyBigDecimal(BigDecimal.ZERO);
            if (aType == Type.NEGATIVE_ZERO || bType == Type.NEGATIVE_ZERO)
                return createRubyBigDecimal(Type.NAN);

            // a and b are only +-Infinity

            if (aType == Type.POSITIVE_INFINITY)
                return bType == Type.POSITIVE_INFINITY ? a : createRubyBigDecimal(Type.NEGATIVE_INFINITY);
            if (aType == Type.NEGATIVE_INFINITY)
                return bType == Type.POSITIVE_INFINITY ? a : createRubyBigDecimal(Type.POSITIVE_INFINITY);

            throw new RuntimeException();
        }

    }

    @CoreMethod(names = "mult", required = 2)
    public abstract static class MultNode extends BigDecimalCoreMethodNode {

        public MultNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object mulBigDecimal(RubyBasicObject a, BigDecimal b, int precision) {
            return createRubyBigDecimal(getBigDecimalValue(a).multiply(b, new MathContext(precision)));
        }

        @Specialization(guards = "isNormal(a)")
        public Object mult(RubyBasicObject a, long b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isNormal(a)")
        public Object mult(RubyBasicObject a, double b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isNormal(a)")
        public Object mult(RubyBasicObject a, RubyBignum b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"
        })
        public Object mult(RubyBasicObject a, RubyBasicObject b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

    }

    @CoreMethod(names = {"/", "quo"}, required = 1)
    public abstract static class DivOpNode extends BigDecimalCoreMethodNode {

        final ConditionProfile normalZero = ConditionProfile.createBinaryProfile();

        public DivOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object div(RubyBasicObject a, BigDecimal b) {
            if (normalZero.profile(b.signum() == 0)) {
                switch (getBigDecimalValue(a).signum()) {
                    case 1:
                        return createRubyBigDecimal(Type.POSITIVE_INFINITY);
                    case 0:
                        return createRubyBigDecimal(Type.NAN);
                    case -1:
                        return createRubyBigDecimal(Type.NEGATIVE_INFINITY);
                    default:
                        throw new RuntimeException();
                }
            } else {
                final int sumOfPrecisions = getBigDecimalValue(a).precision() + b.precision();
                final int precision = nearestBiggerMultipleOf4(sumOfPrecisions) * 2;
                return createRubyBigDecimal(getBigDecimalValue(a).divide(b, new MathContext(precision)));
            }
        }

        @Specialization(guards = "isNormal(a)")
        public Object div(RubyBasicObject a, long b) {
            return div(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object div(RubyBasicObject a, double b) {
            return div(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormal(a)")
        public Object div(RubyBasicObject a, RubyBignum b) {
            return div(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object div(RubyBasicObject a, RubyBasicObject b) {
            return div(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "!isNormal(b)"})
        public Object divNormalSpecial(RubyBasicObject a, RubyBasicObject b) {
            switch (getBigDecimalType(b)) {
                case NAN:
                    return b;
                case NEGATIVE_ZERO:
                    switch (getBigDecimalValue(a).signum()) {
                        case 1:
                            return createRubyBigDecimal(Type.NEGATIVE_INFINITY);
                        case 0:
                            return createRubyBigDecimal(Type.NAN);
                        case -1:
                            return createRubyBigDecimal(Type.POSITIVE_INFINITY);
                    }
                case POSITIVE_INFINITY:
                    switch (getBigDecimalValue(a).signum()) {
                        case 1:
                        case 0:
                            return createRubyBigDecimal(BigDecimal.ZERO);
                        case -1:
                            return createRubyBigDecimal(Type.NEGATIVE_ZERO);
                    }
                case NEGATIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return createRubyBigDecimal(Type.NEGATIVE_ZERO);
                        case 0:
                        case -1:
                            return createRubyBigDecimal(BigDecimal.ZERO);
                    }
            }
            throw new RuntimeException();
        }


        @Specialization(guards = "!isNormal(a)")
        public Object divSpecialNormal(RubyBasicObject a, long b) {
            return divSpecialNormal(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object divSpecialNormal(RubyBasicObject a, double b) {
            return divSpecialNormal(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "!isNormal(a)")
        public Object divSpecialNormal(RubyBasicObject a, RubyBignum b) {
            return divSpecialNormal(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
        public Object divSpecialNormal(RubyBasicObject a, RubyBasicObject b) {
            switch (getBigDecimalType(a)) {
                case NAN:
                    return a;
                case NEGATIVE_ZERO:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return a;
                        case 0:
                            return createRubyBigDecimal(Type.NAN);
                        case -1:
                            return createRubyBigDecimal(BigDecimal.ZERO);
                    }
                case POSITIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return a;
                        case 0:
                            return a;
                        case -1:
                            return createRubyBigDecimal(Type.NEGATIVE_INFINITY);
                    }
                case NEGATIVE_INFINITY:
                    switch (getBigDecimalValue(b).signum()) {
                        case 1:
                            return a;
                        case 0:
                            return a;
                        case -1:
                            return createRubyBigDecimal(Type.POSITIVE_INFINITY);
                    }
            }
            throw new RuntimeException();
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isRubyBigDecimal(b)",
                "!isNormal(b)"})
        public Object divSpecialSpecia(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

            if (aType == Type.NAN || bType == Type.NAN ||
                    (aType == Type.NEGATIVE_ZERO && bType == Type.NEGATIVE_ZERO))
                return createRubyBigDecimal(Type.NAN);

            if (aType == Type.NEGATIVE_ZERO)
                if (bType == Type.POSITIVE_INFINITY) return a;
                else return createRubyBigDecimal(Type.POSITIVE_INFINITY);

            if (bType == Type.NEGATIVE_ZERO)
                if (aType == Type.POSITIVE_INFINITY) return createRubyBigDecimal(Type.NEGATIVE_INFINITY);
                else return createRubyBigDecimal(Type.POSITIVE_INFINITY);

            // a and b are only +-Infinity
            return createRubyBigDecimal(Type.NAN);
        }
    }

    @CoreMethod(names = {"<=>"}, required = 1)
    public abstract static class CompareNode extends BigDecimalCoreMethodNode {

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

        @Specialization(guards = "isNormal(a)")
        public int compare(RubyBasicObject a, RubyBignum b) {
            return compareBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {
                "isNormal(a)",
                "isRubyBigDecimal(b)",
                "isNormal(b)"})
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

        @Specialization(guards = "!isNormal(a)")
        public Object compareSpecial(RubyBasicObject a, RubyBignum b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = {
                "!isNormal(a)",
                "isNan(a)"})
        public Object compareSpecialNan(RubyBasicObject a, RubyBasicObject b) {
            return nil();
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isRubyBigDecimal(b)",
                "!isNormal(a) || !isNormal(b)",
                "isNormal(a) || !isNan(a)"})
        public Object compareSpecial(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

            if (aType == Type.NAN || bType == Type.NAN) return nil();
            if (aType == bType) return 0;
            if (aType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) return 1;
            if (aType == Type.NEGATIVE_INFINITY || bType == Type.POSITIVE_INFINITY) return -1;

            // a and b have finite value

            final BigDecimal aCompare;
            final BigDecimal bCompare;

            if (aType == Type.NEGATIVE_ZERO) aCompare = BigDecimal.ZERO;
            else aCompare = getBigDecimalValue(a);
            if (bType == Type.NEGATIVE_ZERO) bCompare = BigDecimal.ZERO;
            else bCompare = getBigDecimalValue(b);

            return aCompare.compareTo(bCompare);
        }

        @Specialization(guards = "isNil(b)")
        public Object compareNil(RubyBasicObject a, RubyBasicObject b) {
            return nil();
        }

        @Specialization(guards = {
                "!isRubyBigDecimal(b)",
                "!isNil(b)"})
        public Object compareCoerced(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :<=>, b", "b", b);
        }

    }

    // TODO (pitr 20-May-2015): compare Ruby implementation of #== with a Java one

    @CoreMethod(names = "zero?")
    public abstract static class ZeroNode extends BigDecimalCoreMethodNode {

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

    @CoreMethod(names = "sign")
    public abstract static class SignNode extends BigDecimalCoreMethodNode {

        public SignNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"})
        public int signNormalZero(RubyBasicObject value) {
            return 1;
        }

        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"})
        public int signNormal(RubyBasicObject value) {
            return getBigDecimalValue(value).signum() * 2;
        }

        @Specialization(guards = "!isNormal(value)")
        public int signSpecial(RubyBasicObject value) {
            // TODO (pitr 26-May-2015): should be using const look up nodes to use values of BigDecimal::SIGN_* constants
            switch (getBigDecimalType(value)) {
                case NEGATIVE_INFINITY:
                    return -3;
                case POSITIVE_INFINITY:
                    return 3;
                case NEGATIVE_ZERO:
                    return -1;
                case NAN:
                    return 0;
            }
            // TODO (pitr 28-may-2015): is it ok to throw exception in unreachable branch?
            throw new RuntimeException();
        }

    }

    @CoreMethod(names = "nan?")
    public abstract static class NanNode extends BigDecimalCoreMethodNode {

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
    public abstract static class ExponentNode extends BigDecimalCoreMethodNode {

        public ExponentNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = {
                "isNormal(value)",
                "!isNormalZero(value)"})
        public long exponent(RubyBasicObject value) {
            final BigDecimal val = getBigDecimalValue(value).abs().stripTrailingZeros();
            return val.precision() - val.scale();
        }

        @Specialization(guards = {
                "isNormal(value)",
                "isNormalZero(value)"})
        public int exponentZero(RubyBasicObject value) {
            return 0;
        }

        @Specialization(guards = "!isNormal(value)")
        public int exponentSpecial(RubyBasicObject value) {
            return 0;
        }

    }

    @CoreMethod(names = "abs")
    public abstract static class AbsNode extends BigDecimalCoreMethodNode {

        public AbsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject abs(RubyBasicObject value) {
            return createRubyBigDecimal(getBigDecimalValue(value).abs());
        }

        @Specialization(guards = "!isNormal(value)")
        public RubyBasicObject absSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case NEGATIVE_INFINITY:
                    return createRubyBigDecimal(Type.POSITIVE_INFINITY);
                case NEGATIVE_ZERO:
                    return createRubyBigDecimal(BigDecimal.ZERO);
                case POSITIVE_INFINITY:
                case NAN:
                    return value;
                default:
                    throw new RuntimeException();
            }
        }

    }

    @CoreMethod(names = "finite?")
    public abstract static class FiniteNode extends BigDecimalCoreMethodNode {

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
    public abstract static class InfiniteNode extends BigDecimalCoreMethodNode {

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
    public abstract static class PrecsNode extends BigDecimalCoreMethodNode {

        public PrecsNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormal(value)")
        public RubyBasicObject precsNormal(RubyBasicObject value) {
            final BigDecimal bigDecimalValue = getBigDecimalValue(value).abs();
            // TODO (pitr 26-May-2015): replace with ArrayBuilder node
            return createArray(
                    new int[]{
                            bigDecimalValue.stripTrailingZeros().unscaledValue().toString().length(),
                            nearestBiggerMultipleOf4(bigDecimalValue.unscaledValue().toString().length())},
                    2);
        }

        @Specialization(guards = "!isNormal(value)")
        public Object precsSpecial(RubyBasicObject value) {
            // TODO (pitr 26-May-2015): replace with ArrayBuilder node
            return createArray(new int[]{1, 1}, 2);
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends BigDecimalCoreMethodNode {

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
    public abstract static class ToFNode extends BigDecimalCoreMethodNode {

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
                    throw new RuntimeException();
            }
        }

    }

    @CoreMethod(names = "split")
    public abstract static class SplitNode extends BigDecimalCoreMethodNode {

        final private ConditionProfile wasNormal;
        @Child private CallDispatchHeadNode signCall;
        @Child private CallDispatchHeadNode exponentCall;
        @Child private ToIntNode signToInt;
        @Child private ToIntNode exponentToInt;
        @Child private IntegerCastNode signIntegerCast;

        public SplitNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
            wasNormal = ConditionProfile.createBinaryProfile();
            signCall = DispatchHeadNodeFactory.createMethodCall(context);
            exponentCall = DispatchHeadNodeFactory.createMethodCall(context);
            signToInt = ToIntNodeGen.create(context, sourceSection, null);
            exponentToInt = ToIntNodeGen.create(context, sourceSection, null);
            signIntegerCast = IntegerCastNodeGen.create(context, sourceSection, null);
        }

        @Specialization
        public RubyBasicObject split(VirtualFrame frame, RubyBasicObject value) {
            final String digits;

            if (wasNormal.profile(isNormal(value))) {
                digits = getNormalDigits(value);
            } else {
                final String type = getBigDecimalType(value).getRepresentation();
                digits = type.startsWith("-") ? type.substring(1) : type;
            }

            final int sign = Integer.signum(signIntegerCast.executeInteger(frame,
                    signToInt.executeIntOrLong(frame, signCall.call(frame, value, "sign", null))));
            final Object exponent =
                    exponentToInt.executeIntOrLong(frame, this.exponentCall.call(frame, value, "exponent", null));

            return createArray(new Object[]{sign, createString(digits), 10, exponent}, 4);
        }

        @TruffleBoundary
        private String getNormalDigits(RubyBasicObject value) {
            return getBigDecimalValue(value).abs().stripTrailingZeros().unscaledValue().toString();
        }
    }

    @CoreMethod(names = {"to_i", "to_int"})
    public abstract static class ToINode extends BigDecimalCoreMethodNode {

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
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case POSITIVE_INFINITY:
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case NAN:
                    throw new RaiseException(getContext().getCoreLibrary().
                            floatDomainError(type.getRepresentation(), this));
                case NEGATIVE_ZERO:
                    return 0;
                default:
                    throw new RuntimeException();
            }

        }

    }
}
