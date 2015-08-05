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
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.runtime.ConstantReplacer;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

public class ReadConstantNode extends RubyNode {

    private final String name;
    @Child private GetConstantNode getConstantNode;

    public ReadConstantNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver) {
        this(context, sourceSection, name, receiver, false);
    }

    public ReadConstantNode(RubyContext context, SourceSection sourceSection, String name, RubyNode receiver, boolean considerLexicalScope) {
        super(context, sourceSection);

        this.name = ConstantReplacer.replacementName(sourceSection, name);
        final AbstractLookupConstantNode lookupConstantNode;
        if (considerLexicalScope) {
            lookupConstantNode = LookupConstantWithLexicalScopeNodeGen.create(context, sourceSection, null, null);
        } else {
            lookupConstantNode = LookupConstantNodeGen.create(context, sourceSection, null, null);
        }
        this.getConstantNode = GetConstantNodeGen.create(context, sourceSection, receiver, new LiteralNode(context, sourceSection, this.name), lookupConstantNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return getConstantNode.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        RubyNode receiver = getConstantNode.getModule();
        final RubyContext context = getContext();

        if (name.equals("Encoding")) {
            // Work-around so I don't have to load the iconv library - runners/formatters/junit.rb.
            return createString("constant");
        }

        final Object receiverObject;
        try {
            receiverObject = receiver.execute(frame);
        } catch (RaiseException e) {
            /* If we are looking up a constant in a constant that is itself undefined, we return Nil
             * rather than raising the error. Eg.. defined?(Defined::Undefined1::Undefined2).
             *
             * We should maybe try to see if receiver.isDefined() but we also need its value if it is,
             * and we do not want to execute receiver twice. */
            if (((RubyBasicObject) e.getRubyException()).getLogicalClass() == context.getCoreLibrary().getNameErrorClass()) {
                return nil();
            }
            throw e;
        }

        final RubyConstant constant;
        try {
            constant = getConstantNode.getLookupConstantNode().executeLookupConstant(frame, receiverObject, name);
        } catch (RaiseException e) {
            if (((RubyBasicObject) e.getRubyException()).getLogicalClass() == context.getCoreLibrary().getTypeErrorClass()) {
                // module is not a class/module
                return nil();
            } else if (((RubyBasicObject) e.getRubyException()).getLogicalClass() == context.getCoreLibrary().getNameErrorClass()) {
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

    public String getName() {
        return name;
    }

}
