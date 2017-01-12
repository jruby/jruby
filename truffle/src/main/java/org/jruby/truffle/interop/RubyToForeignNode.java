/*
 * Copyright (c) 2013, 2017 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.core.cast.NameToJavaStringNode;
import org.jruby.truffle.core.string.StringCachingGuards;
import org.jruby.truffle.language.RubyNode;

@ImportStatic(StringCachingGuards.class)
@NodeChild(value = "value", type = RubyNode.class)
public abstract class RubyToForeignNode extends RubyNode {

    public abstract Object executeConvert(VirtualFrame frame, Object value);

    @Specialization(guards = "isRubySymbol(value) || isRubyString(value)")
    public String convert(
            VirtualFrame frame,
            DynamicObject value,
            @Cached("create()") NameToJavaStringNode toJavaStringNode) {
        return toJavaStringNode.executeToJavaString(frame, value);
    }

    @Specialization(guards = {"!isRubyString(value)", "!isRubySymbol(value)"})
    public Object convert(Object value) {
        return value;
    }

}
