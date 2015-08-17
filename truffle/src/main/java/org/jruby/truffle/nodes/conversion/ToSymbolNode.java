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

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.StringNodes;
import org.jruby.truffle.runtime.RubyContext;

@NodeChild(value="child", type=RubyNode.class)
public abstract class ToSymbolNode extends RubyNode {

    public ToSymbolNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract DynamicObject executeRubySymbol(VirtualFrame frame, Object object);

    // TODO(CS): cache the conversion to a symbol? Or should the user do that themselves?

    @Specialization(guards = "isRubySymbol(symbol)")
    protected DynamicObject toSymbolSymbol(DynamicObject symbol) {
        return symbol;
    }

    @Specialization(guards = "isRubyString(string)")
    protected DynamicObject toSymbolString(DynamicObject string) {
        return getSymbol(StringNodes.getByteList(string));
    }

    @Specialization
    protected DynamicObject toSymbol(String string) {
        return getSymbol(string);
    }

}
