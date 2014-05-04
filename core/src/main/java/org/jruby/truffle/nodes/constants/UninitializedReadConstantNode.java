/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;

public class UninitializedReadConstantNode extends ReadConstantChainNode {

    private final String name;

    public UninitializedReadConstantNode(String name) {
        this.name = name;
    }

    @Override
    public boolean executeBoolean(RubyBasicObject receiver) throws UnexpectedResultException {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnexpectedResultException(execute(receiver));
    }

    @Override
    public int executeIntegerFixnum(RubyBasicObject receiver) throws UnexpectedResultException {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnexpectedResultException(execute(receiver));
    }

    @Override
    public long executeLongFixnum(RubyBasicObject receiver) throws UnexpectedResultException {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnexpectedResultException(execute(receiver));
    }

    @Override
    public double executeFloat(RubyBasicObject receiver) throws UnexpectedResultException {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        throw new UnexpectedResultException(execute(receiver));
    }

    /**
     * This execute method allows us to pass in the already executed receiver object, so that during
     * uninitialization it is not executed once by the specialized node and again by this node.
     */
    public Object execute(RubyBasicObject receiver) {
        CompilerDirectives.transferToInterpreterAndInvalidate();

        final RubyContext context = receiver.getRubyClass().getContext();

        RubyModule.RubyConstant constant;

        constant = receiver.getLookupNode().lookupConstant(name);

        if (constant == null && receiver instanceof RubyModule) {
            /*
             * FIXME(CS): I'm obviously doing something wrong with constant lookup in nested modules
             * here, but explicitly looking in the Module itself, not its lookup node, seems to fix
             * it for now.
             */

            constant = ((RubyModule) receiver).lookupConstant(name);
        }

        if (constant == null) {
            throw new RaiseException(context.getCoreLibrary().nameErrorUninitializedConstant(name));
        }

        replace(new CachedReadConstantNode(receiver.getRubyClass(), constant.value, this));

        assert RubyContext.shouldObjectBeVisible(constant.value);

        return constant.value;
    }

}
