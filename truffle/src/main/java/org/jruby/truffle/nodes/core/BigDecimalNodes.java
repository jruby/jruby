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
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.*;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.truffle.nodes.objects.Allocator;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.UndefinedPlaceholder;
import org.jruby.truffle.runtime.core.*;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.EnumSet;

@CoreClass(name = "Truffle::BigDecimal")
public abstract class BigDecimalNodes {

    private static final HiddenKey VALUE_IDENTIFIER = new HiddenKey("value");
    private static final DynamicObjectFactory BIG_DECIMAL_FACTORY;
    public static final Property VALUE_PROPERTY;

    static {
        Shape.Allocator allocator = RubyBasicObject.LAYOUT.createAllocator();
        VALUE_PROPERTY = Property.create(
                VALUE_IDENTIFIER,
                allocator.locationForType(BigDecimal.class, EnumSet.of(LocationModifier.NonNull)),
                0);
        Shape shape = RubyBasicObject.EMPTY_SHAPE.addProperty(VALUE_PROPERTY);
        BIG_DECIMAL_FACTORY = shape.createFactory();
    }

    // TODO (pitr 15-May-2015) figure out where to put RubyBigDecimal, or remove completely
    public static class RubyBigDecimal extends RubyBasicObject {
        public RubyBigDecimal(RubyClass rubyClass, DynamicObject dynamicObject) {
            super(rubyClass, dynamicObject);
        }
    }

    public static class RubyBigDecimalAllocator implements Allocator {

        @Override
        public RubyBasicObject allocate(RubyContext context, RubyClass rubyClass, Node currentNode) {
            return createRubyBigDecimal(rubyClass, BigDecimal.ZERO);
        }

    }

    public static BigDecimal getBigDecimalValue(RubyBasicObject bignum) {
        assert bignum.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        return (BigDecimal) VALUE_PROPERTY.get(bignum.getDynamicObject(), true);
    }

    public static void setBigDecimalValue(RubyBasicObject bignum, BigDecimal value) {
        assert bignum.getDynamicObject().getShape().hasProperty(VALUE_IDENTIFIER);
        VALUE_PROPERTY.setSafe(bignum.getDynamicObject(), value, null);
    }

    public static RubyBigDecimal createRubyBigDecimal(RubyClass rubyClass, BigDecimal value) {
        return new RubyBigDecimal(rubyClass, BIG_DECIMAL_FACTORY.newInstance(value));
    }

    public abstract static class BigDecimalCoreMethodNode extends CoreMethodArrayArgumentsNode {

        public BigDecimalCoreMethodNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        public static boolean isRubyBigDecimal(Object value) {
            return value instanceof RubyBigDecimal;
        }
    }

    @CoreMethod(names = "initialize", required = 1)
    public abstract static class InitializeNode extends BigDecimalCoreMethodNode {

        public InitializeNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyBigDecimal(v)")
        public RubyBasicObject initialize(RubyBigDecimal self, RubyBasicObject v) {
            return v;
        }

        @Specialization(guards = "isRubyString(v)")
        public RubyBasicObject initializeFromString(RubyBigDecimal self, RubyBasicObject v) {
            switch (v.toString()) {
                case "NaN": // FIXME
                case "Infinity": // FIXME
                case "+Infinity": // FIXME
                case "-Infinity": // FIXME
                    return self;
            }

            setBigDecimalValue(self, new BigDecimal(v.toString()));
            return self;
        }
    }

    // TODO: double specializations

    @CoreMethod(names = "+", required = 1)
    public abstract static class AdditionNode extends BigDecimalCoreMethodNode {

        public AdditionNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object add(RubyBasicObject a, int b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).add(BigDecimal.valueOf(b)));
        }

        @Specialization
        public Object add(RubyBasicObject a, long b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).add(BigDecimal.valueOf(b)));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object add(RubyBasicObject a, RubyBasicObject b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).add(getBigDecimalValue(b)));
        }

    }

    @CoreMethod(names = "add", required = 2)
    public abstract static class AddNode extends BigDecimalCoreMethodNode {

        public AddNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object add(RubyBasicObject a, RubyBasicObject b, int precision) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).add(getBigDecimalValue(b), new MathContext(precision)));
        }

    }

    @CoreMethod(names = "-", required = 1)
    public abstract static class SubtractNode extends BigDecimalCoreMethodNode {

        public SubtractNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public Object subtract(RubyBasicObject a, int b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).subtract(BigDecimal.valueOf(b)));
        }

        @Specialization
        public Object subtract(RubyBasicObject a, long b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).subtract(BigDecimal.valueOf(b)));
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object subtract(RubyBasicObject a, RubyBasicObject b) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).subtract(getBigDecimalValue(b)));
        }

    }

    @CoreMethod(names = "sub", required = 2)
    public abstract static class SubNode extends BigDecimalCoreMethodNode {

        public SubNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public Object sub(RubyBasicObject a, RubyBasicObject b, int precision) {
            return createRubyBigDecimal(
                    getContext().getCoreLibrary().getBigDecimalClass(),
                    getBigDecimalValue(a).subtract(getBigDecimalValue(b), new MathContext(precision)));
        }
    }

    @CoreMethod(names = {"==", "eql?"}, required = 1)
    public abstract static class EqualNode extends BigDecimalCoreMethodNode {

        public EqualNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @Specialization
        public boolean equal(RubyBasicObject a, int b) {
            return getBigDecimalValue(a).compareTo(BigDecimal.valueOf(b)) == 0;
        }

        @Specialization
        public boolean equal(RubyBasicObject a, long b) {
            return getBigDecimalValue(a).compareTo(BigDecimal.valueOf(b)) == 0;
        }

        @Specialization(guards = "isRubyBigDecimal(b)")
        public boolean equal(RubyBasicObject a, RubyBasicObject b) {
            return getBigDecimalValue(a).compareTo(getBigDecimalValue(b)) == 0;
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends CoreMethodArrayArgumentsNode {

        public ToSNode(RubyContext context, SourceSection sourceSection) {
            super(context, sourceSection);
        }

        @CompilerDirectives.TruffleBoundary
        @Specialization
        public RubyString toS(RubyBasicObject value) {
            return getContext().makeString(getBigDecimalValue(value).toString());
        }

    }

}
