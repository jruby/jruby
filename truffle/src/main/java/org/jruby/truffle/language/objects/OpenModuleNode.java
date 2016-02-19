/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.objects;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.Layouts;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.ModuleBodyDefinitionNode;

/**
 * Open a module and execute a method in it - probably to define new methods.
 */
public class OpenModuleNode extends RubyNode {

    @Child private RubyNode definingModule;
    final protected LexicalScope lexicalScope;
    @Child private IndirectCallNode callModuleDefinitionNode;

    final private ModuleBodyDefinitionNode definitionMethod;

    public OpenModuleNode(RubyContext context, SourceSection sourceSection, RubyNode definingModule, ModuleBodyDefinitionNode definition, LexicalScope lexicalScope) {
        super(context, sourceSection);
        this.definingModule = definingModule;
        this.definitionMethod = definition;
        this.lexicalScope = lexicalScope;
        callModuleDefinitionNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();

        // TODO(CS): cast
        final DynamicObject module = (DynamicObject) definingModule.execute(frame);

        lexicalScope.unsafeSetLiveModule(module);
        Layouts.MODULE.getFields(lexicalScope.getParent().getLiveModule()).addLexicalDependent(module);

        final InternalMethod definition = definitionMethod.executeMethod(frame).withDeclaringModule(module);
        return callModuleDefinitionNode.call(frame, definition.getCallTarget(),
                RubyArguments.pack(null, null, definition, DeclarationContext.MODULE, null, module, null, new Object[]{}));
    }

}
