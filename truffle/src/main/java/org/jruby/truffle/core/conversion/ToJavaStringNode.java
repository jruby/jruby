/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.conversion;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyNode;

@NodeChild(value="child", type=RubyNode.class)
public abstract class ToJavaStringNode extends RubyNode {

    public ToJavaStringNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract String executeJavaString(VirtualFrame frame, Object object);

    @Specialization(guards = "isRubySymbol(symbol)")
    protected String toJavaStringSymbol(DynamicObject symbol) {
        return Layouts.SYMBOL.getString(symbol);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyString(string)")
    protected String toJavaStringString(DynamicObject string) {
        return string.toString();
    }

    @Specialization
    protected String toJavaString(String string) {
        return string;
    }

}
