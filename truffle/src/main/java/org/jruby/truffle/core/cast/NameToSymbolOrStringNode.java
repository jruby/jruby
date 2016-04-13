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
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.dispatch.CallDispatchHeadNode;
import org.jruby.truffle.language.dispatch.DispatchHeadNodeFactory;

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

    public abstract DynamicObject executeToSymbolOrString(VirtualFrame frame, Object name);

    @Specialization(guards = "isRubySymbol(symbol)")
    public DynamicObject coerceRubySymbol(DynamicObject symbol) {
        return symbol;
    }

    @Specialization(guards = "isRubyString(string)")
    public DynamicObject coerceRubyString(DynamicObject string) {
        return string;
    }

    @Specialization(guards = { "!isRubySymbol(object)", "!isRubyString(object)" })
    public DynamicObject coerceObject(VirtualFrame frame, Object object) {
        final Object coerced;
        try {
            coerced = toStr.call(frame, object, "to_str", null);
        } catch (RaiseException e) {
            if (Layouts.BASIC_OBJECT.getLogicalClass(e.getException()) == coreLibrary().getNoMethodErrorClass()) {
                CompilerDirectives.transferToInterpreter();
                throw new RaiseException(coreLibrary().typeErrorNoImplicitConversion(object, "String", this));
            } else {
                throw e;
            }
        }

        if (RubyGuards.isRubyString(coerced)) {
            return (DynamicObject) coerced;
        } else {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreLibrary().typeErrorBadCoercion(object, "String", "to_str", coerced, this));
        }
    }
}
