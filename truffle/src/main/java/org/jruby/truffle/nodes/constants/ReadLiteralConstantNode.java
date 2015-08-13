/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.constants;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.BasicObjectNodes;
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import com.oracle.truffle.api.object.DynamicObject;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class ReadLiteralConstantNode extends RubyNode {

    @Child private ReadConstantNode readConstantNode;

    public ReadLiteralConstantNode(RubyContext context, SourceSection sourceSection, RubyNode moduleNode, String name) {
        super(context, sourceSection);
        RubyNode nameNode = new LiteralNode(context, sourceSection, name);
        this.readConstantNode = new ReadConstantNode(context, sourceSection, false,  moduleNode, nameNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return readConstantNode.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        final RubyContext context = getContext();
        final String name = (String) readConstantNode.nameNode.execute(frame);

        final Object module;
        try {
            module = readConstantNode.moduleNode.execute(frame);
        } catch (RaiseException e) {
            /* If we are looking up a constant in a constant that is itself undefined, we return Nil
             * rather than raising the error. Eg.. defined?(Defined::Undefined1::Undefined2).
             *
             * We should maybe try to see if receiver.isDefined() but we also need its value if it is,
             * and we do not want to execute receiver twice. */
            if (BasicObjectNodes.getLogicalClass(((DynamicObject) e.getRubyException())) == context.getCoreLibrary().getNameErrorClass()) {
                return nil();
            }
            throw e;
        }

        final RubyConstant constant;
        try {
            constant = readConstantNode.lookupConstantNode.executeLookupConstant(frame, module, name);
        } catch (RaiseException e) {
            if (BasicObjectNodes.getLogicalClass(((DynamicObject) e.getRubyException())) == context.getCoreLibrary().getTypeErrorClass()) {
                // module is not a class/module
                return nil();
            } else if (BasicObjectNodes.getLogicalClass(((DynamicObject) e.getRubyException())) == context.getCoreLibrary().getNameErrorClass()) {
                // private constant
                return nil();
            }
            throw e;
        }

        if (constant == null) {
            return nil();
        } else {
            return createString("constant");
        }
    }

}
