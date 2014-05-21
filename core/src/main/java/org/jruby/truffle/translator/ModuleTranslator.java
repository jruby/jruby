/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.ast.*;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.constants.*;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.literal.NilNode;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.methods.AliasNode;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.methods.*;

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

    public ModuleTranslator(RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, Source source) {
        super(context, parent, environment, source);
        useClassVariablesAsIfInClass = true;
    }

    public MethodDefinitionNode compileClassNode(ISourcePosition sourcePosition, String name, org.jruby.ast.Node bodyNode) {
        final SourceSection sourceSection = translate(sourcePosition);

        environment.addMethodDeclarationSlots();

        RubyNode body;

        if (bodyNode != null) {
            body = bodyNode.accept(this);
        } else {
            body = new NilNode(context, sourceSection);
        }

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        body = new CatchReturnPlaceholderNode(context, sourceSection, body, environment.getReturnID());

        final RubyRootNode rootNode = new RubyRootNode(sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body);

        return new MethodDefinitionNode(context, sourceSection, environment.getSharedMethodInfo().getName(), environment.getSharedMethodInfo(), environment.needsDeclarationFrame(), rootNode, false);
    }

    @Override
    public RubyNode visitConstDeclNode(org.jruby.ast.ConstDeclNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final SelfNode selfNode = new SelfNode(context, sourceSection);

        return new WriteConstantNode(context, sourceSection, node.getName(), selfNode, (RubyNode) node.getValueNode().accept(this));
    }

    @Override
    public RubyNode visitConstNode(org.jruby.ast.ConstNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final SelfNode selfNode = new SelfNode(context, sourceSection);

        return new ReadConstantNode(context, sourceSection, node.getName(), selfNode);
    }

    @Override
    public RubyNode visitDefnNode(org.jruby.ast.DefnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        /*
         * The top-level translator puts methods into Object. We put ours into the self, which is
         * the class being defined.
         */

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, node.getName(), false, node.getBodyNode());

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, environment.getParser(), environment.getParser().allocateReturnID(), true, true, sharedMethodInfo, sharedMethodInfo.getName());
        final MethodTranslator methodCompiler = new MethodTranslator(context, this, newEnvironment, false, false, source);
        final MethodDefinitionNode functionExprNode = methodCompiler.compileFunctionNode(translate(node.getPosition()), node.getName(), node.getArgsNode(), node.getBodyNode(), false);

        return new AddMethodNode(context, sourceSection, new SelfNode(context, sourceSection), functionExprNode);
    }

    @Override
    public RubyNode visitClassVarAsgnNode(org.jruby.ast.ClassVarAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode rhs = node.getValueNode().accept(this);
        return new WriteClassVariableNode(context, sourceSection, node.getName(), new SelfNode(context, sourceSection), rhs);
    }

    @Override
    public RubyNode visitClassVarNode(org.jruby.ast.ClassVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        return new ReadClassVariableNode(context, sourceSection, node.getName(), new SelfNode(context, sourceSection));
    }

    @Override
    public RubyNode visitAliasNode(org.jruby.ast.AliasNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.LiteralNode oldName = (org.jruby.ast.LiteralNode) node.getOldName();
        final org.jruby.ast.LiteralNode newName = (org.jruby.ast.LiteralNode) node.getNewName();

        return new AliasNode(context, sourceSection, new SelfNode(context, sourceSection), newName.getName(), oldName.getName());
    }

    @Override
    protected RubyNode getModuleToDefineModulesIn(SourceSection sourceSection) {
        return new SelfNode(context, sourceSection);
    }

}
