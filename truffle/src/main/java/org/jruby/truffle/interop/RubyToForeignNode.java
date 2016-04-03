/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChild(value = "value", type = RubyNode.class)
public abstract class RubyToForeignNode extends RubyNode {

    public RubyToForeignNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeConvert(VirtualFrame frame, Object value);

    @Specialization(
            guards = {
                    "isRubyString(value)",
                    "ropesEqual(value, cachedRope)"
            },
            limit = "getLimit()")
    public String convertUncached(
            DynamicObject value,
            @Cached("privatizeRope(value)") Rope cachedRope,
            @Cached("objectToString(value)") String convertedString) {
        return convertedString;
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyString(value)")
    public String convertStringUncached(DynamicObject value) {
        return value.toString();
    }

    protected int getLimit() {
        return getContext().getOptions().INTEROP_CONVERT_CACHE;
    }

    protected String objectToString(DynamicObject object) {
        return object.toString();
    }

    @Specialization(guards = "isRubySymbol(value)")
    public String convertSymbol(DynamicObject value) {
        return Layouts.SYMBOL.getString(value);
    }

    @Specialization(guards = {"!isRubyString(value)", "!isRubySymbol(value)"})
    public Object convert(Object value) {
        return value;
    }

}
