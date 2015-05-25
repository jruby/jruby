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

import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyBignum;
import org.jruby.truffle.runtime.core.RubyClass;
import org.jruby.truffle.runtime.core.RubyString;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumSet;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    public static class BigDecimalType extends BasicObjectType {
        private BigDecimalType() {
            super();
        }
    }

    public static final BigDecimalType BIG_DECIMAL_TYPE = new BigDecimalType();

    // TODO (pitr 22-May-2015): would it make sense to have two shapes, special(type) and normal(type, value)?
    private static final HiddenKey VALUE_IDENTIFIER = new HiddenKey("value");
    private static final HiddenKey TYPE_IDENTIFIER = new HiddenKey("type");
    private static final DynamicObjectFactory BIG_DECIMAL_FACTORY;

    public static final Property VALUE_PROPERTY;
    public static final Property TYPE_PROPERTY;

    public enum Type {NEGATIVE_INFINITY, POSITIVE_INFINITY, POSITIVE_ZERO, NEGATIVE_ZERO, NAN, NORMAL}

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

    public static class RubyBigDecimalAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createRubyBigDecimal(rubyClass, BigDecimal.ZERO);
        }

    }

    public static BigDecimal getBigDecimalValue(int v) {
        return BigDecimal.valueOf(v);
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

    public abstract static class BigDecimalCoreMethodNode extends CoreMethodArrayArgumentsNode {

        public BigDecimalCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static boolean isRubyBigDecimal(RubyBasicObject value, Type type) {
            return RubyGuards.isRubyBigDecimal(value) && getBigDecimalType(value) == type;
        }

        public static boolean isNormalRubyBigDecimal(RubyBasicObject value) {
            return isRubyBigDecimal(value, Type.NORMAL);
        }

        public static boolean isSpecialRubyBigDecimal(RubyBasicObject value) {
            return RubyGuards.isRubyBigDecimal(value) && getBigDecimalType(value) != Type.NORMAL;
        }

        public static boolean isSpecialNanRubyBigDecimal(RubyBasicObject value) {
            return isRubyBigDecimal(value, Type.NAN);
        }

        protected RubyBasicObject createRubyBigDecimal(Type type) {
            return BigDecimalNodes.createRubyBigDecimal(getContext().getCoreLibrary().getBigDecimalClass(), type);
        }

        protected RubyBasicObject createRubyBigDecimal(BigDecimal value) {
            return BigDecimalNodes.createRubyBigDecimal(getContext().getCoreLibrary().getBigDecimalClass(), value);
        }

    }

    // TODO (pitr 21-May-2015) cache special BigDecimal instances

    @CoreMethod(names = "initialize", required = 1)
    public abstract static class InitializeNode extends BigDecimalCoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyBigDecimal(v)")
        public RubyBasicObject initialize(RubyBasicObject self, RubyBasicObject v) {
            return v;
        }

        @TruffleBoundary
        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(RubyBasicObject self, RubyBasicObject v) {
            // TODO (pitr 21-May-2015): add profile?
            // TODO (pitr 25-May-2015): construction of positive and negative zero
            switch (v.toString()) {
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
                default:
                    setBigDecimalValue(self, new BigDecimal(v.toString()));
                    return self;
            }
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

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public Object add(RubyBasicObject a, int b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public Object add(RubyBasicObject a, long b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public Object add(RubyBasicObject a, double b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public Object add(RubyBasicObject a, RubyBignum b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {"isNormalRubyBigDecimal(a)", "isNormalRubyBigDecimal(b)"})
        public Object addNormal(RubyBasicObject a, RubyBasicObject b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(a) || isSpecialRubyBigDecimal(b)")
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

            // a,b are either normal or +-zero

            BigDecimal sum = BigDecimal.ZERO;
            if (isNormalRubyBigDecimal(a)) sum = sum.add(getBigDecimalValue(a));
            if (isNormalRubyBigDecimal(b)) sum = sum.add(getBigDecimalValue(b));
            return createRubyBigDecimal(sum);
        }

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

        @Specialization
        public Object add(RubyBasicObject a, int b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object add(RubyBasicObject a, long b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object add(RubyBasicObject a, double b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object add(RubyBasicObject a, RubyBignum b, int precision) {
            return addBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
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

        @Specialization
        public Object sub(RubyBasicObject a, int b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object sub(RubyBasicObject a, long b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object sub(RubyBasicObject a, double b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object sub(RubyBasicObject a, RubyBignum b) {
            return subBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object subtract(RubyBasicObject a, RubyBasicObject b) {
            return subBigDecimal(a, getBigDecimalValue(b));
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

        @Specialization
        public Object sub(RubyBasicObject a, int b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object sub(RubyBasicObject a, long b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object sub(RubyBasicObject a, double b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object sub(RubyBasicObject a, RubyBignum b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object sub(RubyBasicObject a, RubyBasicObject b, int precision) {
            return subBigDecimal(a, getBigDecimalValue(b), precision);
        }
    }

    @CoreMethod(names = "*", required = 1)
    public abstract static class MultOpNode extends BigDecimalCoreMethodNode {

        public MultOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object multBigDecimal(RubyBasicObject a, BigDecimal b) {
            return createRubyBigDecimal(getBigDecimalValue(a).multiply(b));
        }

        @Specialization
        public Object mult(RubyBasicObject a, int b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object mult(RubyBasicObject a, long b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object mult(RubyBasicObject a, double b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object mult(RubyBasicObject a, RubyBignum b) {
            return multBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object mult(RubyBasicObject a, RubyBasicObject b) {
            return multBigDecimal(a, getBigDecimalValue(b));
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

        @Specialization
        public Object mult(RubyBasicObject a, int b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object mult(RubyBasicObject a, long b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object mult(RubyBasicObject a, double b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization
        public Object mult(RubyBasicObject a, RubyBignum b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object mult(RubyBasicObject a, RubyBasicObject b, int precision) {
            return mulBigDecimal(a, getBigDecimalValue(b), precision);
        }

    }

    @CoreMethod(names = "/", required = 1)
    public abstract static class DivOpNode extends BigDecimalCoreMethodNode {

        public DivOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        private Object divBigDecimal(RubyBasicObject a, BigDecimal b) {
            // precision based on https://github.com/pitr-ch/jruby/blob/bigdecimal/core/src/main/java/org/jruby/ext/bigdecimal/RubyBigDecimal.java#L892-903
            final int len = getBigDecimalValue(a).precision() + b.precision();
            final int pow = len / 4;
            final int precision = (pow + 1) * 4 * 2;

            return createRubyBigDecimal(getBigDecimalValue(a).divide(b, new MathContext(precision)));
        }

        @Specialization
        public Object div(RubyBasicObject a, int b) {
            return divBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object div(RubyBasicObject a, long b) {
            return divBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object div(RubyBasicObject a, double b) {
            return divBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object div(RubyBasicObject a, RubyBignum b) {
            return divBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object div(RubyBasicObject a, RubyBasicObject b) {
            return divBigDecimal(a, getBigDecimalValue(b));
        }

    }

    @CoreMethod(names = {"<=>"}, required = 1)
    public abstract static class CompareNode extends BigDecimalCoreMethodNode {

        public CompareNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        private int compareBigNum(RubyBasicObject a, BigDecimal b) {
            return getBigDecimalValue(a).compareTo(b);
        }

        // TODO (pitr 25-May-2015): how to reduce the number of specializations? or is this usual?

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public int compare(RubyBasicObject a, int b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public int compare(RubyBasicObject a, long b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public int compare(RubyBasicObject a, double b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isNormalRubyBigDecimal(a)")
        public int compare(RubyBasicObject a, RubyBignum b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = {"isNormalRubyBigDecimal(a)", "isNormalRubyBigDecimal(b)"})
        public int compareNormal(RubyBasicObject a, RubyBasicObject b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(a)")
        public Object compareSpecial(RubyBasicObject a, int b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(a)")
        public Object compareSpecial(RubyBasicObject a, long b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(a)")
        public Object compareSpecial(RubyBasicObject a, double b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(a)")
        public Object compareSpecial(RubyBasicObject a, RubyBignum b) {
            return compareSpecial(a, createRubyBigDecimal(getBigDecimalValue(b)));
        }

        @Specialization(guards = "isSpecialNanRubyBigDecimal(a)")
        public Object compareSpecialNan(RubyBasicObject a, RubyBasicObject b) {
            return nil();
        }

        @Specialization(guards = {
                "!isSpecialNanRubyBigDecimal(a)",
                "isSpecialRubyBigDecimal(a) || isSpecialRubyBigDecimal(b)",
                "isRubyBigDecimal(b)"})
        public Object compareSpecial(RubyBasicObject a, RubyBasicObject b) {
            final Type aType = getBigDecimalType(a);
            final Type bType = getBigDecimalType(b);

            if (aType == Type.NAN || bType == Type.NAN) return nil();
            if (aType == bType) return 0;
            if (aType == Type.POSITIVE_INFINITY || bType == Type.NEGATIVE_INFINITY) return 1;
            if (aType == Type.NEGATIVE_INFINITY || bType == Type.POSITIVE_INFINITY) return -1;

            // a and b are +-ZERO or normal

            final BigDecimal aCompare;
            final BigDecimal bCompare;

            if (aType == Type.POSITIVE_ZERO || aType == Type.NEGATIVE_ZERO) aCompare = BigDecimal.ZERO;
            else aCompare = getBigDecimalValue(a);
            if (bType == Type.POSITIVE_ZERO || bType == Type.NEGATIVE_ZERO) bCompare = BigDecimal.ZERO;
            else bCompare = getBigDecimalValue(b);

            return aCompare.compareTo(bCompare);
        }

        @Specialization(guards = {"isRubyBigDecimal(a)", "isNil(b)"})
        public Object compareNil(RubyBasicObject a, RubyBasicObject b) {
            return nil();
        }

        @Specialization(guards = {"isRubyBigDecimal(a)", "!isRubyBigDecimal(b)", "!isNil(b)"})
        public Object compareCoerced(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :<=>, b", "b", b);
        }

    }

    // TODO (pitr 20-May-2015): compare Ruby implementation of #== with a Java one

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends BigDecimalCoreMethodNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @TruffleBoundary
        @Specialization(guards = "isNormalRubyBigDecimal(value)")
        public RubyBasicObject toS(RubyBasicObject value) {
            final BigDecimal bigDecimal = getBigDecimalValue(value);
            final boolean negative = bigDecimal.signum() == -1;

            String string = (negative ? "-" : "") + "0." +
                    (negative ? bigDecimal.unscaledValue().toString().substring(1) : bigDecimal.unscaledValue()) +
                    "E" + (bigDecimal.precision() - bigDecimal.scale());
            return createString(string);
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(value)")
        public RubyBasicObject toSSpecial(RubyBasicObject value) {
            return createString(getBigDecimalType(value).toString());
        }

    }

    @CoreMethod(names = "zero?")
    public abstract static class ZeroNode extends BigDecimalCoreMethodNode {

        public ZeroNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormalRubyBigDecimal(value)")
        public boolean zeroNormal(RubyBasicObject value) {
            return getBigDecimalValue(value).compareTo(BigDecimal.ZERO) == 0;
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(value)")
        public boolean zeroSpecial(RubyBasicObject value) {
            switch (getBigDecimalType(value)) {
                case POSITIVE_ZERO:
                case NEGATIVE_ZERO:
                    return true;
                default:
                    return false;
            }
        }

    }

    @CoreMethod(names = "nan?")
    public abstract static class NanNode extends BigDecimalCoreMethodNode {

        public NanNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormalRubyBigDecimal(value)")
        public boolean nanNormal(RubyBasicObject value) {
            return false;
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(value)")
        public boolean nanSpecial(RubyBasicObject value) {
            return getBigDecimalType(value) == Type.NAN;
        }

    }

    @CoreMethod(names = "finite?")
    public abstract static class FiniteNode extends BigDecimalCoreMethodNode {

        public FiniteNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isNormalRubyBigDecimal(value)")
        public boolean finiteNormal(RubyBasicObject value) {
            return true;
        }

        @Specialization(guards = "isSpecialRubyBigDecimal(value)")
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


}
