/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchAction;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.nodes.dispatch.MissingBehavior;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ModuleOperations;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyModule;

public class ReadConstantNode extends RubyNode {

    private final String name;
    @Child private RubyNode receiver;
    @Child private DispatchHeadNode dispatch;

    public ReadConstantNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.name = name;
        this.receiver = receiver;
        dispatch = new DispatchHeadNode(context, false, false, MissingBehavior.CALL_CONST_MISSING, lexicalScope, DispatchAction.READ_CONSTANT);

    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (!(receiverObject instanceof RubyModule)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(receiverObject.toString(), "class/module", this));
        }

        return dispatch.dispatch(
                frame,
                receiverObject,
                name,
                null,
                new Object[]{});
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final RubyContext context = getContext();

        if (name.equals("Encoding")) {
            /*
             * Work-around so I don't have to load the iconv library - runners/formatters/junit.rb.
             */
            return context.makeString("constant");
        }

        Object receiverObject;

        try {
            receiverObject = receiver.execute(frame);
        } catch (RaiseException e) {
            /*
             * If we are looking up a constant in a constant that is itself undefined, we return Nil
             * rather than raising the error. Eg.. defined?(Defined::Undefined1::Undefined2)
             */

            if (e.getRubyException().getLogicalClass() == context.getCoreLibrary().getNameErrorClass()) {
                return nil();
            }

            throw e;
        }

        RubyModule module = (RubyModule) receiverObject; // TODO(cs): cast
        RubyConstant constant = ModuleOperations.lookupConstant(context, dispatch.getLexicalScope(), module, name);

        if (constant == null || !constant.isVisibleTo(context, dispatch.getLexicalScope(), module)) {
            return nil();
        } else {
            return context.makeString("constant");
        }
    }

    public String getName() {
        return name;
    }

}
