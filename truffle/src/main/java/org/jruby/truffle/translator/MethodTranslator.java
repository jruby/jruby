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
import org.jruby.ast.AssignableNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.arguments.ShouldDestructureNode;
import org.jruby.truffle.nodes.cast.ArrayCastNodeGen;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.defined.DefinedWrapperNode;
import org.jruby.truffle.nodes.dispatch.RespondToNode;
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.nodes.locals.FlipFlopStateNode;
import org.jruby.truffle.nodes.locals.WriteLocalVariableNode;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.supercall.GeneralSuperCallNode;
import org.jruby.truffle.nodes.supercall.GeneralSuperReCallNode;
import org.jruby.truffle.nodes.supercall.ZSuperOutsideMethodNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class MethodTranslator extends BodyTranslator {

    private final org.jruby.ast.ArgsNode argsNode;
    private boolean isBlock;

    public MethodTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source, org.jruby.ast.ArgsNode argsNode) {
        super(currentNode, context, parent, environment, source, false);
        this.isBlock = isBlock;
        this.argsNode = argsNode;
    }

    public RubyNode compileFunctionNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo) {
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

        if (isBlock && argsNode.getRestArgNode() instanceof org.jruby.ast.UnnamedRestArgNode && !((UnnamedRestArgNode) argsNode.getRestArgNode()).isStar()) {
            arityForCheck = new Arity(arity.getPreRequired(), 0, false);
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
            body = nilNode(sourceSection);
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
                final RubyNode castArrayNode = ArrayCastNodeGen.create(context, sourceSection, readArrayNode);
                final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
                final RubyNode writeArrayNode = new WriteLocalVariableNode(context, sourceSection, castArrayNode, arraySlot);

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
                            nilNode(sourceSection),
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
            body = new CatchNextNode(context, sourceSection, body);
            body = new CatchReturnPlaceholderNode(context, sourceSection, body, environment.getReturnID());

            body = new BehaveAsProcNode(context, sourceSection,
                    new CatchBreakAsProcErrorNode(context, sourceSection, body),
                    NodeUtil.cloneNode(body));
        } else {
            body = new CatchReturnNode(context, sourceSection, body, environment.getReturnID());
        }

        body = new CatchRetryAsErrorNode(context, sourceSection, body);

        if (!isBlock) {
            // TODO(CS, 10-Jan-15) why do we only translate exceptions in methods and not blocks?
            body = new ExceptionTranslatingNode(context, sourceSection, body);

            final RubyRootNode rootNode = new RubyRootNode(
                    context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

            if (PRINT_AST_METHOD_NAMES.contains(methodName)) {
                System.err.println(sourceSection + " " + methodName);
                NodeUtil.printCompactTree(System.err, rootNode);
            }

            if (PRINT_FULL_AST_METHOD_NAMES.contains(methodName)) {
                System.err.println(sourceSection + " " + methodName);
                NodeUtil.printTree(System.err, rootNode);
            }

            final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
            return new MethodDefinitionNode(context, sourceSection, methodName, environment.getSharedMethodInfo(), callTarget);
        }

        // Blocks
        final RubyNode newNodeForBlocks = NodeUtil.cloneNode(body);

        for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(newNodeForBlocks, BehaveAsBlockNode.class)) {
            behaveAsBlockNode.replace(behaveAsBlockNode.getAsBlock());
        }

        for (BehaveAsProcNode behaveAsProcNode : NodeUtil.findAllNodeInstances(newNodeForBlocks, BehaveAsProcNode.class)) {
            behaveAsProcNode.replace(behaveAsProcNode.getNotAsProc());
        }

        final RubyRootNode newRootNodeForBlocks = new RubyRootNode(context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                newNodeForBlocks, environment.needsDeclarationFrame());

        // Procs
        final RubyNode newNodeForProcs = NodeUtil.cloneNode(body);

        for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(newNodeForProcs, BehaveAsBlockNode.class)) {
            behaveAsBlockNode.replace(behaveAsBlockNode.getAsBlock());
        }

        for (BehaveAsProcNode behaveAsProcNode : NodeUtil.findAllNodeInstances(newNodeForProcs, BehaveAsProcNode.class)) {
            behaveAsProcNode.replace(behaveAsProcNode.getAsProc());
        }

        final RubyRootNode newRootNodeForProcs = new RubyRootNode(context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                newNodeForProcs, environment.needsDeclarationFrame());

        // Lambdas
        final RubyNode newNodeForLambdas = NodeUtil.cloneNode(body);

        for (BehaveAsBlockNode behaveAsBlockNode : NodeUtil.findAllNodeInstances(newNodeForLambdas, BehaveAsBlockNode.class)) {
            behaveAsBlockNode.replace(behaveAsBlockNode.getNotAsBlock());
        }

        for (BehaveAsProcNode behaveAsProcNode : NodeUtil.findAllNodeInstances(newNodeForLambdas, BehaveAsProcNode.class)) {
            behaveAsProcNode.replace(behaveAsProcNode.getNotAsProc());
        }

        final RubyRootNode newRootNodeForLambdas = new RubyRootNode(
                context, sourceSection,
                environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                new CatchBreakAsReturnNode(context, sourceSection,
                        new CatchReturnNode(context, sourceSection,
                                newNodeForLambdas, environment.getReturnID())),
                environment.needsDeclarationFrame());


        final CallTarget callTargetAsBlock = Truffle.getRuntime().createCallTarget(newRootNodeForBlocks);
        final CallTarget callTargetAsProc = Truffle.getRuntime().createCallTarget(newRootNodeForProcs);
        final CallTarget callTargetAsLambda = Truffle.getRuntime().createCallTarget(newRootNodeForLambdas);

        return new BlockDefinitionNode(context, sourceSection, environment.getSharedMethodInfo(),
                callTargetAsBlock, callTargetAsProc, callTargetAsLambda, environment.getBreakID());
    }

    public static Arity getArity(org.jruby.ast.ArgsNode argsNode) {
        final String[] keywordArguments;

        if (argsNode.hasKwargs() && argsNode.getKeywords() != null) {
            final org.jruby.ast.Node[] keywordNodes = argsNode.getKeywords().children();
            final int keywordsCount = keywordNodes.length;

            keywordArguments = new String[keywordsCount];
            for (int i = 0; i < keywordsCount; i++) {
                final KeywordArgNode kwarg = (KeywordArgNode) keywordNodes[i];
                final AssignableNode assignableNode = kwarg.getAssignable();

                if (assignableNode instanceof LocalAsgnNode) {
                    keywordArguments[i] = ((LocalAsgnNode) assignableNode).getName();
                } else if (assignableNode instanceof DAsgnNode) {
                    keywordArguments[i] = ((DAsgnNode) assignableNode).getName();
                } else {
                    throw new UnsupportedOperationException(
                            "unsupported keyword arg " + kwarg);
                }
            }
        } else {
            keywordArguments = Arity.NO_KEYWORDS;
        }

        return new Arity(
                argsNode.getPreCount(),
                argsNode.getOptionalArgsCount(),
                argsNode.hasRestArg(),
                argsNode.getPostCount(),
                keywordArguments,
                argsNode.hasKeyRest());
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

        currentCallMethodName = environment.getNamedMethodName();

        final RubyNode blockNode;
        if (node.getIterNode() != null) {
            blockNode = node.getIterNode().accept(this);
        } else {
            blockNode = null;
        }

        boolean insideDefineMethod = false;
        MethodTranslator methodArgumentsTranslator = this;
        while (methodArgumentsTranslator.isBlock) {
            if (!(methodArgumentsTranslator.parent instanceof MethodTranslator)) {
                return new ZSuperOutsideMethodNode(context, sourceSection, insideDefineMethod);
            } else if (methodArgumentsTranslator.currentCallMethodName.equals("define_method")) {
                insideDefineMethod = true;
            }
            methodArgumentsTranslator = (MethodTranslator) methodArgumentsTranslator.parent;
        }

        final ReloadArgumentsTranslator reloadTranslator = new ReloadArgumentsTranslator(
                currentNode, context, source, this);

        final ArgsNode argsNode = methodArgumentsTranslator.argsNode;
        final SequenceNode reloadSequence = (SequenceNode) reloadTranslator.visitArgsNode(argsNode);

        return new GeneralSuperReCallNode(context, sourceSection,
                reloadTranslator.isSplatted(),
                reloadSequence.getSequence(),
                blockNode);
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
