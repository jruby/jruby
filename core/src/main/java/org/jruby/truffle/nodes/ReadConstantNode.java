/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.cast.BoxingNode;
import org.jruby.truffle.nodes.dispatch.Dispatch;
import org.jruby.truffle.nodes.dispatch.DispatchHeadNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.core.RubyModule;

public class ReadConstantNode extends RubyNode {

    private final boolean isLiteral;
    private final String name;
    @Child protected RubyNode receiver;
    @Child protected DispatchHeadNode dispatch;

    public ReadConstantNode(RubyContext context, SourceSection sourceSection, boolean isLiteral, String name, RubyNode receiver) {
        super(context, sourceSection);
        this.isLiteral = isLiteral;
        this.name = name;
        this.receiver = receiver;
        dispatch = new DispatchHeadNode(context, Dispatch.MissingBehavior.CALL_CONST_MISSING);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final Object receiverObject = receiver.execute(frame);

        if (isLiteral && !(receiverObject instanceof RubyModule)) {
            CompilerDirectives.transferToInterpreter();
            throw new RaiseException(getContext().getCoreLibrary().typeErrorIsNotA(receiverObject.toString(), "class/module", this));
        }

        return dispatch.dispatch(
                frame,
                NilPlaceholder.INSTANCE,
                RubyArguments.getSelf(frame.getArguments()),
                receiverObject,
                name,
                null,
                new Object[]{},
                Dispatch.DispatchAction.READ_CONSTANT);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        notDesignedForCompilation();

        final RubyContext context = getContext();

        if (name.equals("Encoding")) {
            /*
             * Work-around so I don't have to load the iconv library - runners/formatters/junit.rb.
             */
            return context.makeString("constant");
        }

        Object value;

        try {
            value = ModuleOperations.lookupConstant(context.getCoreLibrary().box(receiver.execute(frame)).getMetaClass(), name);
        } catch (RaiseException e) {
            /*
             * If we are looking up a constant in a constant that is itself undefined, we return Nil
             * rather than raising the error. Eg.. defined?(Defined::Undefined1::Undefined2)
             */

            if (e.getRubyException().getLogicalClass() == context.getCoreLibrary().getNameErrorClass()) {
                return NilPlaceholder.INSTANCE;
            }

            throw e;
        }

        if (value == null) {
            return NilPlaceholder.INSTANCE;
        } else {
            return context.makeString("constant");
        }
    }

    public String getName() {
        return name;
    }

}
