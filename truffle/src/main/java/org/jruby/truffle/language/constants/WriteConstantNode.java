/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.constants;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.RubyGuards;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.control.RaiseException;

/**
 * Represents writing a constant into some module.
 */
public class WriteConstantNode extends RubyNode {

    private final String name;
    @Child private RubyNode module;
    @Child private RubyNode rhs;

    public WriteConstantNode(RubyContext context, SourceSection sourceSection, String name, RubyNode module, RubyNode rhs) {
        super(context, sourceSection);
        this.name = name;
        this.module = module;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // Evaluate RHS first.
        final Object rhsValue = rhs.execute(frame);

        final Object receiverObject = module.execute(frame);

        if (!(RubyGuards.isRubyModule(receiverObject))) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(coreExceptions().typeErrorIsNotA(receiverObject.toString(), "class/module", this));
        }

        final DynamicObject module = (DynamicObject) receiverObject;

        Layouts.MODULE.getFields(module).setConstant(getContext(), this, name, rhsValue);

        return rhsValue;
    }

}
