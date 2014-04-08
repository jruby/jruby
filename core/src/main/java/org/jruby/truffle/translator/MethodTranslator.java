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
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.call.*;
import org.jruby.truffle.nodes.cast.ArrayCastNodeFactory;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.methods.locals.*;
import org.jruby.truffle.nodes.respondto.RespondToNode;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.methods.*;

class MethodTranslator extends BodyTranslator {

    private boolean isBlock;

    public MethodTranslator(RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source) {
        super(context, parent, environment, source);
        this.isBlock = isBlock;
    }

    public MethodDefinitionNode compileFunctionNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node parseTree, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode, boolean ignoreLocalVisiblity) {
        environment.setMethodName(methodName);

        final ParameterCollector parameterCollector = new ParameterCollector();
        argsNode.accept(parameterCollector);

        for (String parameter : parameterCollector.getParameters()) {
            environment.declareVar(parameter);
        }

        final Arity arity = getArity(argsNode);

        RubyNode body;

        if (bodyNode != null) {
            body = bodyNode.accept(this);
        } else {
            body = new NilNode(context, sourceSection);
        }

        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(context, source, isBlock, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);

        final RubyNode prelude;

        if (isBlock) {
            boolean shouldSwitch = true;

            if (argsNode.getPreCount() + argsNode.getPostCount() == 1 && argsNode.getOptionalArgsCount() == 0 && argsNode.getRestArgNode() == null) {
                shouldSwitch = false;
            }

            if (argsNode.getPreCount() == 0 && argsNode.getRestArgNode() != null) {
                shouldSwitch = false;
            }

            RubyNode preludeBuilder;

            if (shouldSwitch) {
                final RubyNode readArrayNode = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
                final RubyNode castArrayNode = ArrayCastNodeFactory.create(context, sourceSection, readArrayNode);
                final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
                final RubyNode writeArrayNode = WriteLocalVariableNodeFactory.create(context, sourceSection, arraySlot, castArrayNode);

                final LoadArgumentsTranslator destructureArgumentsTranslator = new LoadArgumentsTranslator(context, source, isBlock, this);
                destructureArgumentsTranslator.pushArraySlot(arraySlot);
                final RubyNode newDestructureArguments = argsNode.accept(destructureArgumentsTranslator);

                final RespondToNode respondToConvertAry = new RespondToNode(context, sourceSection, readArrayNode, "to_ary");

                preludeBuilder = new IfNode(context, sourceSection,
                        BooleanCastNodeFactory.create(context, sourceSection,
                                AndNodeFactory.create(context, sourceSection,
                                    new BehaveAsBlockNode(context, sourceSection, true),
                                    new ShouldDestructureNode(context, sourceSection, arity, respondToConvertAry))),
                        SequenceNode.sequence(context, sourceSection, writeArrayNode, newDestructureArguments),
                        loadArguments);
            } else {
                preludeBuilder = loadArguments;
            }

            prelude = SequenceNode.sequence(context, sourceSection,
                    new IfNode(context, sourceSection,
                            BooleanCastNodeFactory.create(context, sourceSection,
                                    new BehaveAsBlockNode(context, sourceSection, true)),
                            new NilNode(context, sourceSection),
                            new CheckArityNode(context, sourceSection, arity)), preludeBuilder);
        } else {
            prelude = SequenceNode.sequence(context, sourceSection,
                    new CheckArityNode(context, sourceSection, arity),
                    loadArguments);
        }

        body = SequenceNode.sequence(context, sourceSection, prelude, body);

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        if (isBlock) {
            body = new RedoableNode(context, sourceSection, body);
        }

        body = new CatchReturnNode(context, sourceSection, body, environment.getReturnID(), isBlock);
        body = new CatchNextNode(context, sourceSection, body);
        body = new CatchRetryAsErrorNode(context, sourceSection, body);

        final RubyRootNode pristineRootNode = new RubyRootNode(sourceSection, environment.getFrameDescriptor(), methodName, parseTree, body);

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(NodeUtil.cloneNode(pristineRootNode));

        if (isBlock) {
            return new BlockDefinitionNode(context, sourceSection, methodName, environment.getUniqueMethodIdentifier(), environment.getFrameDescriptor(), environment.needsDeclarationFrame(),
                            pristineRootNode, callTarget);
        } else {
            return new MethodDefinitionNode(context, sourceSection, methodName, environment.getUniqueMethodIdentifier(), environment.getFrameDescriptor(), environment.needsDeclarationFrame(),
                            pristineRootNode, callTarget, ignoreLocalVisiblity);
        }
    }

    private static Arity getArity(org.jruby.ast.ArgsNode argsNode) {
        final int minimum = argsNode.getRequiredArgsCount();
        final int maximum = argsNode.getMaxArgumentsCount();
        return new Arity(minimum, maximum == -1 ? Arity.NO_MAXIMUM : maximum);
    }

    @Override
    public RubyNode visitSuperNode(org.jruby.ast.SuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), node.getArgsNode(), null);

        final String name = environment.getMethodName();

        return new GeneralSuperCallNode(context, sourceSection, name, argumentsAndBlock.getBlock(), argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
    }

    @Override
    public RubyNode visitZSuperNode(org.jruby.ast.ZSuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = environment.getMethodName();

        return new GeneralSuperReCallNode(context, sourceSection, name);
    }

    @Override
    protected FlipFlopStateNode createFlipFlopState(SourceSection sourceSection, int depth) {
        if (isBlock) {
            environment.setNeedsDeclarationFrame();
            return parent.createFlipFlopState(sourceSection, depth + 1);
        } else {
            return super.createFlipFlopState(sourceSection, depth);
        }
    }

}
