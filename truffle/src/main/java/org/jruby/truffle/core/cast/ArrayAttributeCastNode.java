/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.cast;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.NotProvided;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

/**
 * Array indices and lengths must be in 32-bit ranges. This class handle various data types and lowers to a 32-bit int
 * if possible or raises an exception if the value is out of the acceptable range.
 */
@ImportStatic(Integer.class)
@NodeChild(value = "value", type = RubyNode.class)
public abstract class ArrayAttributeCastNode extends RubyNode {

    final String indexName;

    public ArrayAttributeCastNode(RubyContext context, SourceSection sourceSection, String indexName) {
        super(context, sourceSection);
        this.indexName = indexName;
    }

    @Specialization
    public int doIntegerFixnum(int value) {
        return value;
    }

    @Specialization(guards = "inBounds(value)")
    public int doLongFixnum(long value) {
        return (int) value;
    }

    @Specialization(guards = "!inBounds(value)")
    public int doLongFixnumOutOfBounds(long value) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(coreLibrary().argumentError(String.format("%s out of int range", indexName), this));
    }

    @Specialization(guards = "inBounds(value)")
    public int doDouble(double value) {
        return (int) value;
    }

    @Specialization(guards = "!inBounds(value)")
    public int doDoubleOutOfBounds(double value) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(coreLibrary().argumentError(String.format("%s out of int range", indexName), this));
    }

    @Specialization(guards = "isRubyBignum(value)")
    public DynamicObject doBignum(DynamicObject value) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(coreLibrary().argumentError(String.format("%s out of int range", indexName), this));
    }


    @Specialization(guards = "isRubyRange(range)")
    public DynamicObject passThroughRange(DynamicObject range) {
        return range;
    }

    @Specialization
    public Object passThroughNotProvided(NotProvided value) {
        return value;
    }

    @Specialization(guards = { "!isInteger(value)", "!isLong(value)", "!isDouble(value)", "!isRubyBignum(value)", "!isRubyRange(value)" })
    public Object coerce(VirtualFrame frame, DynamicObject value, @Cached("create(getContext(), getSourceSection())") ToIntNode coerceToIntNode) {
        return coerceToIntNode.executeIntOrLong(frame, value);
    }

    @Fallback
    public int doBasicObject(Object object) {
        CompilerDirectives.transferToInterpreter();
        throw new RaiseException(coreLibrary().
                typeErrorIsNotA(object.toString(), "Fixnum (fitting in int)", this));
    }

    protected static boolean inBounds(long value) {
        return (value >= Integer.MIN_VALUE) && (value <= Integer.MAX_VALUE);
    }

    protected static boolean inBounds(double value) {
        return (value >= Integer.MIN_VALUE) && (value <= Integer.MAX_VALUE);
    }
}
