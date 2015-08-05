/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.objects;

import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.core.FiberNodes;
import org.jruby.truffle.nodes.core.ModuleNodes;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBasicObject;
import org.jruby.truffle.runtime.methods.InternalMethod;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;

/**
 * Open a module and execute a method in it - probably to define new methods.
 */
public class OpenModuleNode extends RubyNode {

    @Child private RubyNode definingModule;
    @Child private MethodDefinitionNode definitionMethod;
    @Child private IndirectCallNode callModuleDefinitionNode;

    public OpenModuleNode(RubyContext context, SourceSection sourceSection, RubyNode definingModule, MethodDefinitionNode definitionMethod) {
        super(context, sourceSection);
        this.definingModule = definingModule;
        this.definitionMethod = definitionMethod;
        callModuleDefinitionNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // TODO(CS): cast
        final RubyBasicObject module = (RubyBasicObject) definingModule.execute(frame);

        final LexicalScope oldLexicalScope = FiberNodes.getLexicalScopeStack(getContext());
        final LexicalScope newLexicalScope = new LexicalScope(oldLexicalScope, module);

        ModuleNodes.getModel(oldLexicalScope.getModule()).addLexicalDependent(module);
        FiberNodes.setLexicalScopeStack(getContext(), newLexicalScope);
        try {
            final InternalMethod definition = definitionMethod.executeMethod(frame).withDeclaringModule(module);
            final Object[] frameArguments = RubyArguments.pack(definition, definition.getDeclarationFrame(), module, null, new Object[] {});
            return callModuleDefinitionNode.call(frame, definition.getCallTarget(), frameArguments);
        } finally {
            FiberNodes.setLexicalScopeStack(getContext(), oldLexicalScope);
        }
    }

}
