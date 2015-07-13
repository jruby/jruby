/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.conversion;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.SymbolNodes;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;

@NodeChild(value="child", type=RubyNode.class)
public abstract class ToJavaStringNode extends RubyNode {

    public ToJavaStringNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract String executeJavaString(VirtualFrame frame, Object object);

    // TODO(CS): cache the conversion to a Java String? Or should the user do that themselves?

    @TruffleBoundary
    @Specialization(guards = "isRubySymbol(symbol)")
    protected String toJavaStringSymbol(RubyBasicObject symbol) {
        return SymbolNodes.getString(symbol);
    }

    @TruffleBoundary
    @Specialization(guards = "isRubyString(string)")
    protected String toJavaStringString(RubyBasicObject string) {
        return string.toString();
    }

    @Specialization
    protected String toJavaString(String string) {
        return string;
    }

}
