/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

package org.jruby.truffle.nodes.coerce;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyGuards;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNodeFactory;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

/**
 * Take a Symbol or some object accepting #to_str
 * and convert it to a RubySymbol or a RubyString.
 */
@NodeChild(value = "child", type = RubyNode.class)
public abstract class NameToSymbolOrStringNode extends RubyNode {

    @Child private CallDispatchHeadNode toStr;

    public NameToSymbolOrStringNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
        toStr = DispatchHeadNodeFactory.createMethodCall(context);
    }

    public abstract RubyBasicObject executeToSymbolOrString(VirtualFrame frame, Object name);

    @Specialization(guards = "isRubySymbol(symbol)")
    public RubyBasicObject coerceRubySymbol(RubyBasicObject symbol) {
        return symbol;
    }

    @Specialization(guards = "isRubyString(string)")
    public RubyBasicObject coerceRubyString(RubyBasicObject string) {
        return string;
    }

    @Specialization(guards = { "!isRubySymbol(object)", "!isRubyString(object)" })
    public RubyBasicObject coerceObject(VirtualFrame frame, Object object) {
        final Object coerced;

        try {
            coerced = toStr.call(frame, object, "to_str", null);
        } catch (RaiseException e) {
            if (((RubyBasicObject) e.getRubyException()).getLogicalClass() == getContext().getCoreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(getContext().getCoreLibrary().typeErrorNoImplicitConversion(object, "String", this));
            } else {
                throw e;
            }
        }

        if (RubyGuards.isRubyString(coerced)) {
            return (RubyBasicObject) coerced;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorBadCoercion(object, "String", "to_str", coerced, this));
        }
    }
}
