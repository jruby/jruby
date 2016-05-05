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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.methods.DeclarationContext;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.ModuleBodyDefinitionNode;

public class RunModuleDefinitionNode extends RubyNode {

    final protected LexicalScope lexicalScope;

    @Child private RubyNode definingModule;
    @Child private ModuleBodyDefinitionNode definitionMethod;
    @Child private IndirectCallNode callModuleDefinitionNode;

    public RunModuleDefinitionNode(RubyContext context, SourceSection sourceSection, LexicalScope lexicalScope,
                                   ModuleBodyDefinitionNode definition, RubyNode definingModule) {
        super(context, sourceSection);
        this.definingModule = definingModule;
        this.definitionMethod = definition;
        this.lexicalScope = lexicalScope;
        callModuleDefinitionNode = Truffle.getRuntime().createIndirectCallNode();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        final DynamicObject module = (DynamicObject) definingModule.execute(frame);
        final InternalMethod definition = prepareLexicalScope(module, definitionMethod.executeMethod(frame));

        return callModuleDefinitionNode.call(frame, definition.getCallTarget(), RubyArguments.pack(
                null, null, definition, DeclarationContext.MODULE, null, module, null, new Object[]{}));
    }

    @TruffleBoundary
    private InternalMethod prepareLexicalScope(DynamicObject module, InternalMethod definition) {
        lexicalScope.unsafeSetLiveModule(module);
        Layouts.MODULE.getFields(lexicalScope.getParent().getLiveModule()).addLexicalDependent(module);
        return definition.withDeclaringModule(module);
    }

}
