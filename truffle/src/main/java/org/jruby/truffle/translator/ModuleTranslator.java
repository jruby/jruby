/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.defined.DefinedWrapperNode;
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.nodes.methods.AliasNodeGen;
import org.jruby.truffle.nodes.methods.CatchReturnPlaceholderNode;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.nodes.methods.SetMethodDeclarationContext;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.runtime.RubyContext;

/**
 * Translates module and class nodes.
 * <p>
 * In Ruby, a module or class definition is somewhat like a method. It has a local scope and a value
 * for self, which is the module or class object that is being defined. Therefore for a module or
 * class definition we translate into a special method. We run that method with self set to be the
 * newly allocated module or class. We then have to treat at least method and constant definitions
 * differently.
 */
class ModuleTranslator extends BodyTranslator {

    public ModuleTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, Source source) {
        super(currentNode, context, parent, environment, source, false);
        useClassVariablesAsIfInClass = true;
    }

    public MethodDefinitionNode compileClassNode(SourceSection sourceSection, String name, org.jruby.ast.Node bodyNode) {
        RubyNode body;

        if (bodyNode != null) {
            parentSourceSection.push(sourceSection);

            try {
                body = bodyNode.accept(this);
            } finally {
                parentSourceSection.pop();
            }
        } else {
            body = new DefinedWrapperNode(context, sourceSection,
                    new LiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                    "nil");
        }

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        body = new CatchReturnPlaceholderNode(context, sourceSection, body, environment.getReturnID());

        body = new SetMethodDeclarationContext(context, sourceSection, Visibility.PUBLIC, name, body);

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        return new MethodDefinitionNode(
                context,
                sourceSection,
                environment.getSharedMethodInfo().getName(),
                environment.getSharedMethodInfo(),
                Truffle.getRuntime().createCallTarget(rootNode));
    }

    @Override
    public RubyNode visitDefnNode(org.jruby.ast.DefnNode node) {
        final SourceSection sourceSection = translate(node.getPosition(), node.getName());
        final SelfNode classNode = new SelfNode(context, sourceSection);

        // If we have a method we've defined in a node, but would like to delegate some corner cases out to the
        // Rubinius implementation for simplicity, we need a way to resolve the naming conflict.  The naive solution
        // here is to append "_internal" to the method name, which can then be called like any other method.  This is
        // a bit different than aliasing because normally if a Rubinius method name conflicts with an already defined
        // method, we simply ignore the method definition.  Here we explicitly rename the method so it's always defined.

        String methodName = node.getName();
        boolean rubiniusMethodRename = false;

        if (sourceSection.getSource().getPath().equals("core:/core/rubinius/common/array.rb")) {
            rubiniusMethodRename = methodName.equals("zip");
        } else if (sourceSection.getSource().getPath().equals("core:/core/rubinius/common/float.rb")) {
            rubiniusMethodRename = methodName.equals("round");
        } else if (sourceSection.getSource().getPath().equals("core:/core/rubinius/common/range.rb")) {
            rubiniusMethodRename = methodName.equals("each") || methodName.equals("step") || methodName.equals("to_a");
        }

        if (rubiniusMethodRename) {
            methodName = methodName + "_internal";
        }

        return translateMethodDefinition(sourceSection, classNode, methodName, node, node.getArgsNode(), node.getBodyNode());
    }

    @Override
    public RubyNode visitAliasNode(org.jruby.ast.AliasNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.LiteralNode oldName = (org.jruby.ast.LiteralNode) node.getOldName();
        final org.jruby.ast.LiteralNode newName = (org.jruby.ast.LiteralNode) node.getNewName();

        return AliasNodeGen.create(context, sourceSection, newName.getName(), oldName.getName(), new SelfNode(context, sourceSection));
    }

}
