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

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyModule;
import org.jruby.truffle.runtime.methods.InternalMethod;

/**
 * Open a module and execute a method in it - probably to define new methods.
 */
public class OpenModuleNode extends RubyNode {

    @Child private RubyNode definingModule;
    @Child private MethodDefinitionNode definitionMethod;
    final protected LexicalScope lexicalScope;
    @Child private IndirectCallNode callModuleDefinitionNode;

    public OpenModuleNode(RubyContext context, SourceSection sourceSection, RubyNode definingModule, MethodDefinitionNode definitionMethod, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.definingModule = definingModule;
        this.definitionMethod = definitionMethod;
        this.lexicalScope = lexicalScope;
        callModuleDefinitionNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // TODO(CS): cast
        final RubyModule module = (RubyModule) definingModule.execute(frame);

        lexicalScope.setLiveModule(module);
        lexicalScope.getParent().getLiveModule().addLexicalDependent(module);

        final InternalMethod definition = definitionMethod.executeMethod(frame).withDeclaringModule(module);
        return callModuleDefinitionNode.call(frame, definition.getCallTarget(), RubyArguments.pack(definition, definition.getDeclarationFrame(), module, null, new Object[]{}));
    }

}
