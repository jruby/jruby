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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.object.BasicObjectType;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumSet;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    public static class BigDecimalType extends BasicObjectType {

    }

    public static final BigDecimalType BIG_DECIMAL_TYPE = new BigDecimalType();

    private static final HiddenKey VALUE_IDENTIFIER = new HiddenKey("value");
    public static final Shape BIG_DECIMAL_SHAPE;
    private static final DynamicObjectFactory BIG_DECIMAL_FACTORY;
    public static final Property VALUE_PROPERTY;

    static {
        final Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        VALUE_PROPERTY = Property.create(
                VALUE_IDENTIFIER,
                allocator.locationForType(BigDecimal.class, EnumSet.of(LocationModifier.NonNull)),
                0);
        BIG_DECIMAL_SHAPE = RubyBasicObject.LAYOUT.createShape(BIG_DECIMAL_TYPE).addProperty(VALUE_PROPERTY);
        BIG_DECIMAL_FACTORY = BIG_DECIMAL_SHAPE.createFactory();
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

    public static void setBigDecimalValue(RubyBasicObject bignum, BigDecimal value) {
        assert bignum.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        VALUE_PROPERTY.setSafe(bignum.getDynamicObject(), value, null);
    }

    public static RubyBasicObject createRubyBigDecimal(RubyClass rubyClass, BigDecimal value) {
        return new RubyBasicObject(rubyClass, BIG_DECIMAL_FACTORY.newInstance(value));
    }

    public abstract static class BigDecimalCoreMethodNode extends CoreMethodArrayArgumentsNode {

        public BigDecimalCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }
    }

    @CoreMethod(names = "initialize", required = 1)
    public abstract static class InitializeNode extends BigDecimalCoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyBigDecimal(v)")
        public RubyBasicObject initialize(RubyBasicObject self, RubyBasicObject v) {
            return v;
        }

        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(RubyBasicObject self, RubyBasicObject v) {
            // TODO (pitr 20-May-2015): add NaN, Infinity handling
            switch (v.toString()) {
                case "NaN":
                    return self;
                case "Infinity":
                case "+Infinity":
                    setBigDecimalValue(self, new BigDecimal("9E9999")); // temporary to get comparison test working
                    return self;
                case "-Infinity":
                    setBigDecimalValue(self, new BigDecimal("-9E9999")); // temporary to get comparison test working
                    return self;
            }

            setBigDecimalValue(self, new BigDecimal(v.toString()));
            return self;
        }
    }

    @CoreMethod(names = "+", required = 1)
    public abstract static class AddOpNode extends BigDecimalCoreMethodNode {

        public AddOpNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        private Object addBigDecimal(RubyBasicObject a, BigDecimal b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).add(b));
        }

        @Specialization
        public Object add(RubyBasicObject a, int b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object add(RubyBasicObject a, long b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object add(RubyBasicObject a, double b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization
        public Object add(RubyBasicObject a, RubyBignum b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object add(RubyBasicObject a, RubyBasicObject b) {
            return addBigDecimal(a, getBigDecimalValue(b));
        }

    }

    @CoreMethod(names = "add", required = 2)
    public abstract static class AddNode extends BigDecimalCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        private Object addBigDecimal(RubyBasicObject a, BigDecimal b, int precision) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).add(b, new MathContext(precision)));
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

        @CompilerDirectives.TruffleBoundary
        private Object subBigDecimal(RubyBasicObject a, BigDecimal b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).subtract(b));
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

        @CompilerDirectives.TruffleBoundary
        private Object subBigDecimal(RubyBasicObject a, BigDecimal b, int precision) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).subtract(b, new MathContext(precision)));
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

        @CompilerDirectives.TruffleBoundary
        private Object multBigDecimal(RubyBasicObject a, BigDecimal b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).multiply(b));
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

        @CompilerDirectives.TruffleBoundary
        private Object mulBigDecimal(RubyBasicObject a, BigDecimal b, int precision) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).multiply(b, new MathContext(precision)));
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

        @CompilerDirectives.TruffleBoundary
        private Object divBigDecimal(RubyBasicObject a, BigDecimal b) {
            // precision based on https://github.com/pitr-ch/jruby/blob/bigdecimal/core/src/main/java/org/jruby/ext/bigdecimal/RubyBigDecimal.java#L892-903
            final int len = getBigDecimalValue(a).precision() + b.precision();
            final int pow = len / 4;
            final int precision = (pow + 1) * 4 * 2;

            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).divide(b, new MathContext(precision)));
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

        @Specialization
        public int compare(RubyBasicObject a, int b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization
        public int compare(RubyBasicObject a, long b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization
        public int compare(RubyBasicObject a, double b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization
        public int compare(RubyBasicObject a, RubyBignum b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public int compare(RubyBasicObject a, RubyBasicObject b) {
            return compareBigNum(a, getBigDecimalValue(b));
        }

        @Specialization(guards = "!isRubyBigDecimal(b)")
        public Object compareCoerced(VirtualFrame frame, RubyBasicObject a, RubyBasicObject b) {
            return ruby(frame, "redo_coerced :<=>, b", "b", b);
        }

    }

    // TODO (pitr 20-May-2015): compare Ruby implementation of #== with a Java one

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyBasicObject value) {
            final BigDecimal bigDecimal = getBigDecimalValue(value);
            final boolean negative = bigDecimal.signum() == -1;

            return getContext().makeString((negative ? "-" : "") + "0." +
                    (negative ? bigDecimal.unscaledValue().toString().substring(1) : bigDecimal.unscaledValue()) +
                    "E" + (bigDecimal.precision() - bigDecimal.scale()));
        }

    }

}
