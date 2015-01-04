/*
 * Copyright (c) 2013, 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyModule;

/**
 * Represents writing a constant into some module.
 */
public class WriteConstantNode extends RubyNode {

    private final String name;
    private final LexicalScope lexicalScope;
    @Child protected RubyNode module;
    @Child protected RubyNode rhs;

    public WriteConstantNode(RubyContext context, SourceSection sourceSection, String name, LexicalScope lexicalScope, RubyNode module, RubyNode rhs) {
        super(context, sourceSection);
        this.name = name;
        this.lexicalScope = lexicalScope;
        this.module = module;
        this.rhs = rhs;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        notDesignedForCompilation();

        // Evaluate RHS first.
        final Object rhsValue = rhs.execute(frame);

        if (rhsValue instanceof RubyModule) {
            final RubyModule setModule = (RubyModule) rhsValue;
            if (setModule.getName() == null) {
                setModule.setLexicalScope(lexicalScope);
                setModule.setName(name);
            }
        }

        final Object receiverObject = module.execute(frame);

        if (!(receiverObject instanceof RubyModule)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(receiverObject.toString(), "class/module", this));
        }

        final RubyModule module = (RubyModule) receiverObject;

        module.setConstant(this, name, rhsValue);

        return rhsValue;
    }

}
