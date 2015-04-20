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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.jruby.ast.ArgsNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.cast.ArrayCastNodeFactory;
import org.jruby.truffle.nodes.control.BreakNode;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.methods.locals.FlipFlopStateNode;
import org.jruby.truffle.nodes.methods.locals.WriteLocalVariableNodeFactory;
import org.jruby.truffle.nodes.respondto.RespondToNode;
import org.jruby.truffle.nodes.supercall.GeneralSuperCallNode;
import org.jruby.truffle.nodes.supercall.GeneralSuperReCallNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

class MethodTranslator extends BodyTranslator {

    private boolean isBlock;

    public MethodTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source) {
        super(currentNode, context, parent, environment, source, false);
        this.isBlock = isBlock;
    }

    public RubyNode compileFunctionNode(SourceSection sourceSection, String methodName, ArgsNode argsNode, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo) {
        if (PRINT_PARSE_TREE_METHOD_NAMES.contains(methodName)) {
            System.err.println(sourceSection + " " + methodName);
            System.err.println(sharedMethodInfo.getParseTree().toString(true, 0));
        }

        final ParameterCollector parameterCollector = new ParameterCollector();
        argsNode.accept(parameterCollector);

        for (String parameter : parameterCollector.getParameters()) {
            environment.declareVar(parameter);
        }

        final Arity arity = getArity(argsNode);

        final Arity arityForCheck;

        /*
         * If you have a block with parameters |a,| Ruby checks the arity as if was minimum 1, maximum 1. That's
         * counter-intuitive - as you'd expect the anonymous rest argument to cause it to have no maximum. Indeed,
         * that's how JRuby reports it, and by the look of their failing spec they consider this to be correct. We'll
         * follow the specs for now until we see a reason to do something else.
         */

        if (isBlock && argsNode.childNodes().size() == 2 && argsNode.getRestArgNode() instanceof org.jruby.ast.UnnamedRestArgNode) {
            arityForCheck = new Arity(arity.getRequired(), 0, false, false, false, 0);
        } else {
            arityForCheck = arity;
        }

        RubyNode body;

        if (bodyNode != null) {
            parentSourceSection.push(sourceSection);

            try {
                body = bodyNode.accept(this);
            } finally {
                parentSourceSection.pop();
            }
        } else {
            body = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject());
        }

        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isBlock, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);

        final RubyNode prelude;

        if (isBlock) {
            boolean shouldConsiderDestructuringArrayArg = true;

            if (argsNode.getPreCount() == 0 && argsNode.getOptionalArgsCount() == 0 && argsNode.getPostCount() == 0 && argsNode.getRestArgNode() == null) {
                shouldConsiderDestructuringArrayArg = false;
            }

            if (argsNode.getPreCount() + argsNode.getPostCount() == 1 && argsNode.getOptionalArgsCount() == 0 && argsNode.getRestArgNode() == null) {
                shouldConsiderDestructuringArrayArg = false;
            }

            if (argsNode.getPreCount() == 0 && argsNode.getRestArgNode() != null) {
                shouldConsiderDestructuringArrayArg = false;
            }

            RubyNode preludeBuilder;

            if (shouldConsiderDestructuringArrayArg) {
                final RubyNode readArrayNode = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
                final RubyNode castArrayNode = ArrayCastNodeFactory.create(context, sourceSection, readArrayNode);
                final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
                final RubyNode writeArrayNode = WriteLocalVariableNodeFactory.create(context, sourceSection, arraySlot, castArrayNode);

                final LoadArgumentsTranslator destructureArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isBlock, this);
                destructureArgumentsTranslator.pushArraySlot(arraySlot);
                final RubyNode newDestructureArguments = argsNode.accept(destructureArgumentsTranslator);

                preludeBuilder =
                        new BehaveAsBlockNode(context, sourceSection,
                                new IfNode(context, sourceSection,
                                        new ShouldDestructureNode(context, sourceSection, arity,
                                                new RespondToNode(context, sourceSection, readArrayNode, "to_ary")),
                                        SequenceNode.sequence(context, sourceSection, writeArrayNode, newDestructureArguments),
                                        NodeUtil.cloneNode(loadArguments)),
                                NodeUtil.cloneNode(loadArguments));
            } else {
                preludeBuilder = loadArguments;
            }

            prelude = SequenceNode.sequence(context, sourceSection,
                    new BehaveAsBlockNode(context, sourceSection,
                            new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                            new CheckArityNode(context, sourceSection, arityForCheck, parameterCollector.getKeywords(), argsNode.getKeyRest() != null)), preludeBuilder);
        } else {
            if (usesRubiniusPrimitive) {
                // Use Rubinius.primitive seems to turn off arity checking. See Time.from_array for example.
                prelude = loadArguments;
            } else {
                prelude = SequenceNode.sequence(context, sourceSection,
                        new CheckArityNode(context, sourceSection, arityForCheck, parameterCollector.getKeywords(), argsNode.getKeyRest() != null),
                        loadArguments);
            }
        }

        body = SequenceNode.sequence(context, sourceSection, prelude, body);

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        if (isBlock) {
            body = new RedoableNode(context, sourceSection, body);
            body = new CatchReturnPlaceholderNode(context, sourceSection, body, environment.getReturnID());

            body = new BehaveAsProcNode(context, sourceSection,
                    new CatchBreakAsProcErrorNode(context, sourceSection, body),
                    NodeUtil.cloneNode(body));
        } else {
            body = new CatchReturnNode(context, sourceSection, body, environment.getReturnID());
        }

        body = new CatchNextNode(context, sourceSection, body);
        body = new CatchRetryAsErrorNode(context, sourceSection, body);

        if (!isBlock) {
            // TODO(CS, 10-Jan-15) why do we only translate exceptions in methods and not blocks?
            body = new ExceptionTranslatingNode(context, sourceSection, body);
        }

        final RubyRootNode rootNode = new RubyRootNode(
                context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body);

        if (PRINT_AST_METHOD_NAMES.contains(methodName)) {
            System.err.println(sourceSection + " " + methodName);
            NodeUtil.printCompactTree(System.err, rootNode);
        }

        if (PRINT_FULL_AST_METHOD_NAMES.contains(methodName)) {
            System.err.println(sourceSection + " " + methodName);
            NodeUtil.printTree(System.err, rootNode);
        }

        if (isBlock) {
            // Blocks
            final RubyRootNode newRootNodeForBlocks = rootNode.cloneRubyRootNode();

            for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(newRootNodeForBlocks, BehaveAsBlockNode.class)) {
                behaveAsBlockNode.replace(behaveAsBlockNode.getAsBlock());
            }

            for (BehaveAsProcNode behaveAsProcNode : NodeUtil.findAllNodeInstances(newRootNodeForBlocks, BehaveAsProcNode.class)) {
                behaveAsProcNode.replace(behaveAsProcNode.getNotAsProc());
            }

            final CallTarget callTargetAsBlock = Truffle.getRuntime().createCallTarget(newRootNodeForBlocks);

            // Procs
            final RubyRootNode newRootNodeForProcs = rootNode.cloneRubyRootNode();

            for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(newRootNodeForProcs, BehaveAsBlockNode.class)) {
                behaveAsBlockNode.replace(behaveAsBlockNode.getAsBlock());
            }

            for (BehaveAsProcNode behaveAsProcNode : NodeUtil.findAllNodeInstances(newRootNodeForProcs, BehaveAsProcNode.class)) {
                behaveAsProcNode.replace(behaveAsProcNode.getAsProc());
            }

            final CallTarget callTargetAsProc = Truffle.getRuntime().createCallTarget(newRootNodeForProcs);

            // Methods
            final RubyRootNode newRootNodeForMethods = rootNode.cloneRubyRootNode();

            for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(newRootNodeForMethods, BehaveAsBlockNode.class)) {
                behaveAsBlockNode.replace(behaveAsBlockNode.getNotAsBlock());
            }

            for (BehaveAsProcNode behaveAsProcNode : NodeUtil.findAllNodeInstances(newRootNodeForMethods, BehaveAsProcNode.class)) {
                behaveAsProcNode.replace(behaveAsProcNode.getNotAsProc());
            }

            final RubyRootNode newRootNodeWithCatchReturn = new RubyRootNode(
                    context,
                    newRootNodeForMethods.getSourceSection(),
                    newRootNodeForMethods.getFrameDescriptor(), newRootNodeForMethods.getSharedMethodInfo(),
                    new CatchBreakAsReturnNode(context, sourceSection,
                        new CatchReturnNode(context, newRootNodeForMethods.getSourceSection(),
                            newRootNodeForMethods.getBody(), getEnvironment().getReturnID())));

            final CallTarget callTargetAsMethod = Truffle.getRuntime().createCallTarget(newRootNodeWithCatchReturn);

            return new BlockDefinitionNode(context, sourceSection, environment.getSharedMethodInfo(),
                    environment.needsDeclarationFrame(), callTargetAsBlock, callTargetAsProc, callTargetAsMethod, environment.getBlockID());
        } else {
            return new MethodDefinitionNode(context, sourceSection, methodName, environment.getSharedMethodInfo(),
                    environment.needsDeclarationFrame(), Truffle.getRuntime().createCallTarget(rootNode));
        }
    }

    public static Arity getArity(org.jruby.ast.ArgsNode argsNode) {
        final int minimum = argsNode.getRequiredArgsCount();
        final int maximum = argsNode.getMaxArgumentsCount();
        // TODO CS 19-Mar-15 collect up the keyword argument names here
        return new Arity(minimum, argsNode.getOptionalArgsCount(), maximum == -1, argsNode.hasKwargs(), argsNode.hasKeyRest(), argsNode.countKeywords(), argsNode);
    }

    @Override
    public RubyNode visitSuperNode(org.jruby.ast.SuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), node.getArgsNode(), null, environment.getNamedMethodName());

        return new GeneralSuperCallNode(context, sourceSection, argumentsAndBlock.getBlock(), argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
    }

    @Override
    public RubyNode visitZSuperNode(org.jruby.ast.ZSuperNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        if (environment.isBlock()) {
            // We need the declaration frame to get the arguments to use
            environment.setNeedsDeclarationFrame();
        }

        return new GeneralSuperReCallNode(context, sourceSection, environment.isBlock());
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
