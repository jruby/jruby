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
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.nodes.objects.LexicalScopeNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class ReadConstantWithLexicalScopeNode extends RubyNode {

    @Child private ReadConstantNode readConstantNode;

    public ReadConstantWithLexicalScopeNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope, String name) {
        super(context, sourceSection);
        RubyNode moduleNode = new LexicalScopeNode(context, sourceSection, lexicalScope);
        RubyNode nameNode = new LiteralNode(context, sourceSection, name);
        this.readConstantNode = ReadConstantNodeGen.create(context, sourceSection, lexicalScope, moduleNode, nameNode);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return readConstantNode.execute(frame);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        final RubyContext context = getContext();
        final String name = (String) readConstantNode.getName().execute(frame);

        if (name.equals("Encoding")) {
            // Work-around so I don't have to load the iconv library - runners/formatters/junit.rb.
            return createString("constant");
        }

        final Object moduleObject = readConstantNode.getModule().execute(frame);

        final RubyConstant constant;
        try {
            constant = readConstantNode.lookupConstantNode.executeLookupConstant(frame, moduleObject, name);
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

}
