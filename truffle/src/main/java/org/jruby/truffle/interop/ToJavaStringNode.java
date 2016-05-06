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
import org.jruby.truffle.Layouts;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChild(value = "value", type = RubyNode.class)
public abstract class ToJavaStringNode extends RubyNode {

    public ToJavaStringNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract String executeToJavaString(VirtualFrame frame, Object value);

    @Specialization(
            guards = {
                    "isRubyString(value)",
                    "ropesEqual(value, cachedRope)"
            },
            limit = "getLimit()")
    public String stringUncached(
            DynamicObject value,
            @Cached("privatizeRope(value)") Rope cachedRope,
            @Cached("value.toString()") String convertedString) {
        return convertedString;
    }

    protected String objectToString(DynamicObject object) {
        return object.toString();
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyString(value)", contains = "stringUncached")
    public String stringCached(DynamicObject value) {
        return value.toString();
    }

    @Specialization(guards = "isRubySymbol(value)")
    public String symbol(DynamicObject value) {
        return Layouts.SYMBOL.getString(value);
    }

    @Specialization
    public String javaString(String value) {
        return value;
    }

    protected int getLimit() {
        return getContext().getOptions().INTEROP_CONVERT_CACHE;
    }

}
