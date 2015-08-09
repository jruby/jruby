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
import org.jruby.truffle.nodes.core.KernelNodes.RequireNode;
import org.jruby.truffle.nodes.core.KernelNodesFactory;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyConstant;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyBasicObject;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public class ReadConstantWithLexicalScopeNode extends RubyNode {

    private final LexicalScope lexicalScope;
    private final String name;
    @Child protected LookupConstantWithLexicalScopeNode lookupConstantNode;
    @Child private GetConstantNode getConstantNode;

    @Child private RequireNode requireNode;

    public ReadConstantWithLexicalScopeNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope, String name) {
        super(context, sourceSection);
        this.lexicalScope = lexicalScope;
        this.name = name;
        this.lookupConstantNode = LookupConstantWithLexicalScopeNodeGen.create(context, sourceSection, lexicalScope, name);
        this.getConstantNode = GetConstantNodeGen.create(context, sourceSection, null, null, null);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final RubyConstant constant = lookupConstantNode.executeLookupConstant(frame);

        if (constant != null && constant.isAutoload()) {
            CompilerDirectives.transferToInterpreter();
            return autoload(frame, constant);
        }

        return getConstantNode.executeGetConstant(frame, lexicalScope.getLiveModule(), name, constant);
    }

    protected Object autoload(VirtualFrame frame, RubyConstant constant) {
        final RubyBasicObject path = (RubyBasicObject) constant.getValue();

        // The autoload constant must only be removed if everything succeeds.
        // We remove it first to allow lookup to ignore it and add it back if there was a failure.
        ModuleNodes.getModel(constant.getDeclaringModule()).removeConstant(this, name);
        try {
            require(path);
            return execute(frame); // retry
        } catch (RaiseException e) {
            ModuleNodes.getModel(constant.getDeclaringModule()).setAutoloadConstant(this, name, path);
            throw e;
        }
    }

    private boolean require(RubyBasicObject feature) {
        if (requireNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            requireNode = insert(KernelNodesFactory.RequireNodeFactory.create(getContext(), getSourceSection(), null));
        }
        return requireNode.require(feature);
    }

    @Override
    public Object isDefined(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        if (name.equals("Encoding")) {
            // Work-around so I don't have to load the iconv library - runners/formatters/junit.rb.
            return createString("constant");
        }

        final RubyConstant constant;
        try {
            constant = lookupConstantNode.executeLookupConstant(frame);
        } catch (RaiseException e) {
            if (((RubyBasicObject) e.getRubyException()).getLogicalClass() == getContext().getCoreLibrary().getNameErrorClass()) {
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
