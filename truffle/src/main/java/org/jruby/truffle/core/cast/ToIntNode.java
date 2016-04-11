/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.cast.ToIntNodeGen;
import org.jruby.truffle.core.numeric.FloatNodes;
import org.jruby.truffle.core.numeric.FloatNodesFactory;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

@NodeChild(value = "child", type = RubyNode.class)
public abstract class ToIntNode extends RubyNode {

    @Child private CallDispatchHeadNode toIntNode;
    @Child private FloatNodes.ToINode floatToIntNode;

    private final ConditionProfile wasInteger = ConditionProfile.createBinaryProfile();
    private final ConditionProfile wasLong = ConditionProfile.createBinaryProfile();
    private final ConditionProfile wasLongInRange = ConditionProfile.createBinaryProfile();

    public static ToIntNode create(RubyContext context, SourceSection sourceSection) {
        return ToIntNodeGen.create(context, sourceSection, null);
    }

    public ToIntNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public int doInt(VirtualFrame frame, Object object) {
        // TODO CS 14-Nov-15 this code is crazy - should have separate nodes for ToRubyInteger and ToJavaInt

        final Object integerObject = executeIntOrLong(frame, object);

        if (wasInteger.profile(integerObject instanceof Integer)) {
            return (int) integerObject;
        }

        if (wasLong.profile(integerObject instanceof Long)) {
            final long longValue = (long) integerObject;

            if (wasLongInRange.profile(CoreLibrary.fitsIntoInteger(longValue))) {
                return (int) longValue;
            }
        }

        CompilerDirectives.transferToInterpreter();
        if (RubyGuards.isRubyBignum(object)) {
            throw new RaiseException(coreLibrary().rangeError("bignum too big to convert into `long'", this));
        } else {
            throw new UnsupportedOperationException(object.getClass().toString());
        }
    }

    public abstract Object executeIntOrLong(VirtualFrame frame, Object object);

    @Specialization
    public int coerceInt(int value) {
        return value;
    }

    @Specialization
    public long coerceLong(long value) {
        return value;
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject coerceRubyBignum(DynamicObject value) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(coreLibrary().rangeError("bignum too big to convert into `long'", this));
    }

    @Specialization
    public Object coerceDouble(VirtualFrame frame, double value) {
        if (floatToIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            floatToIntNode = insert(FloatNodesFactory.ToINodeFactory.create(getContext(), getSourceSection(), new RubyNode[] { null }));
        }
        return floatToIntNode.executeToI(frame, value);
    }

    @Specialization
    public Object coerceBoolean(VirtualFrame frame, boolean value) {
        return coerceObject(frame, value);
    }

    @Specialization(guards = "!isRubyBignum(object)")
    public Object coerceBasicObject(VirtualFrame frame, DynamicObject object) {
        return coerceObject(frame, object);
    }

    private Object coerceObject(VirtualFrame frame, Object object) {
        if (toIntNode == null) {
            CompilerDirectives.transferToInterpreter();
            toIntNode = insert(DispatchHeadNodeFactory.createMethodCall(getContext()));
        }

        final Object coerced;
        try {
            coerced = toIntNode.call(frame, object, "to_int", null);
        } catch (RaiseException e) {
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().typeErrorNoImplicitConversion(object, "Integer", this));
            } else {
                throw e;
            }
        }

        if (coreLibrary().getLogicalClass(coerced) == coreLibrary().getFixnumClass()) {
            return coerced;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().typeErrorBadCoercion(object, "Integer", "to_int", coerced, this));
        }
    }

}
