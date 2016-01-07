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

import org.jruby.ast.*;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.arguments.CheckArityNode;
import org.jruby.truffle.nodes.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.arguments.ReadBlockNode;
import org.jruby.truffle.nodes.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.arguments.ShouldDestructureNode;
import org.jruby.truffle.nodes.cast.ArrayCastNodeGen;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.ProcNodes.Type;
import org.jruby.truffle.nodes.dispatch.RespondToNode;
import org.jruby.truffle.nodes.locals.FlipFlopStateNode;
import org.jruby.truffle.nodes.locals.WriteLocalVariableNode;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.supercall.ReadSuperArgumentsNode;
import org.jruby.truffle.nodes.supercall.ReadZSuperArgumentsNode;
import org.jruby.truffle.nodes.supercall.SuperCallNode;
import org.jruby.truffle.nodes.supercall.ZSuperOutsideMethodNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.util.ArrayDeque;
import java.util.Deque;

public class MethodTranslator extends BodyTranslator {

    private final org.jruby.ast.ArgsNode argsNode;
    private boolean isBlock;

    public MethodTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source, org.jruby.ast.ArgsNode argsNode) {
        super(currentNode, context, parent, environment, source, false);
        this.isBlock = isBlock;
        this.argsNode = argsNode;
    }

    public BlockDefinitionNode compileBlockNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo, Type type) {
        declareArguments(sourceSection, methodName, sharedMethodInfo);
        final Arity arity = getArity(argsNode);
        final Arity arityForCheck;

        /*
         * If you have a block with parameters |a,| Ruby checks the arity as if was minimum 1, maximum 1. That's
         * counter-intuitive - as you'd expect the anonymous rest argument to cause it to have no maximum. Indeed,
         * that's how JRuby reports it, and by the look of their failing spec they consider this to be correct. We'll
         * follow the specs for now until we see a reason to do something else.
         */

        if (argsNode.getRestArgNode() instanceof org.jruby.ast.UnnamedRestArgNode && !((UnnamedRestArgNode) argsNode.getRestArgNode()).isStar()) {
            arityForCheck = arity.withRest(false);
        } else {
            arityForCheck = arity;
        }

        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        final boolean isProc = type == Type.PROC;
        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isProc, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);

        final RubyNode preludeProc;
        if (shouldConsiderDestructuringArrayArg(arity)) {
            final RubyNode readArrayNode = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehaviour.RUNTIME_ERROR);
            final RubyNode castArrayNode = ArrayCastNodeGen.create(context, sourceSection, readArrayNode);
            final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
            final RubyNode writeArrayNode = new WriteLocalVariableNode(context, sourceSection, castArrayNode, arraySlot);

            final LoadArgumentsTranslator destructureArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isProc, this);
            destructureArgumentsTranslator.pushArraySlot(arraySlot);
            final RubyNode newDestructureArguments = argsNode.accept(destructureArgumentsTranslator);

            preludeProc = new IfNode(context, sourceSection,
                                    new ShouldDestructureNode(context, sourceSection, new RespondToNode(context, sourceSection, readArrayNode, "to_ary")),
                    SequenceNode.sequence(context, sourceSection, writeArrayNode, newDestructureArguments), loadArguments);
        } else {
            preludeProc = loadArguments;
        }

        final RubyNode preludeLambda = SequenceNode.sequence(context, sourceSection,
                CheckArityNode.create(context, sourceSection, arityForCheck),
                NodeUtil.cloneNode(loadArguments));

        // Procs
        final RubyNode bodyProc = new CatchForProcNode(context, sourceSection, composeBody(preludeProc, NodeUtil.cloneNode(body)));

        final RubyRootNode newRootNodeForProcs = new RubyRootNode(context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                bodyProc, environment.needsDeclarationFrame());

        // Lambdas
        final RubyNode bodyLambda = new CatchForLambdaNode(context, sourceSection, composeBody(preludeLambda, body /* no copy, last usage */), environment.getReturnID());

        final RubyRootNode newRootNodeForLambdas = new RubyRootNode(
                context, sourceSection,
                environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                bodyLambda,
                environment.needsDeclarationFrame());

        // TODO CS 23-Nov-15 only the second one will get instrumented properly!
        final CallTarget callTargetAsLambda = Truffle.getRuntime().createCallTarget(newRootNodeForLambdas);
        final CallTarget callTargetAsProc = Truffle.getRuntime().createCallTarget(newRootNodeForProcs);

        return new BlockDefinitionNode(context, sourceSection, type, environment.getSharedMethodInfo(),
                callTargetAsProc, callTargetAsLambda, environment.getBreakID());
    }

    private boolean shouldConsiderDestructuringArrayArg(Arity arity) {
        // If we do not accept any arguments or only one required, there's never any need to destructure
        if (!arity.hasRest() && arity.getOptional() == 0 && arity.getRequired() <= 1) {
            return false;
        // If there are only a rest argument and optional arguments, there is no need to destructure.
        // Because the first optional argument (or the rest if no optional) will take the whole array.
        } else if (arity.hasRest() && arity.getRequired() == 0) {
            return false;
        } else {
            return true;
        }
    }

    private RubyNode composeBody(RubyNode prelude, RubyNode body) {
        final SourceSection sourceSection = body.getSourceSection();

        body = SequenceNode.sequence(context, sourceSection, prelude, body);

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, sourceSection, initFlipFlopStates(sourceSection), body);
        }

        return body;
    }

    /*
     * This method exists solely to be substituted to support lazy
     * method parsing. The substitution returns a node which performs
     * the parsing lazily and then calls doCompileMethodBody.
     */
    public RubyNode compileMethodBody(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo) {
        return doCompileMethodBody(sourceSection, methodName, bodyNode, sharedMethodInfo);
    }

    public RubyNode doCompileMethodBody(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo) {
        declareArguments(sourceSection, methodName, sharedMethodInfo);
        final Arity arity = getArity(argsNode);

        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, false, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);

        final RubyNode prelude;

        if (usesRubiniusPrimitive) {
            // Use Rubinius.primitive seems to turn off arity checking. See Time.from_array for example.
            prelude = loadArguments;
        } else {
            prelude = SequenceNode.sequence(context, sourceSection,
                    CheckArityNode.create(context, sourceSection, arity),
                    loadArguments);
        }

        body = SequenceNode.sequence(context, body.getSourceSection(), prelude, body);

        if (environment.getFlipFlopStates().size() > 0) {
            body = SequenceNode.sequence(context, body.getSourceSection(), initFlipFlopStates(sourceSection), body);
        }

        body = new CatchForMethodNode(context, body.getSourceSection(), body, environment.getReturnID());

        // TODO(CS, 10-Jan-15) why do we only translate exceptions in methods and not blocks?
        body = new ExceptionTranslatingNode(context, body.getSourceSection(), body);
        return body;
    }

    public MethodDefinitionNode compileMethodNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo) {
        final RubyNode body = compileMethodBody(sourceSection,  methodName, bodyNode, sharedMethodInfo);
        final RubyRootNode rootNode = new RubyRootNode(
                context, body.getSourceSection(), environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        return new MethodDefinitionNode(context, sourceSection, methodName, environment.getSharedMethodInfo(), callTarget, false, true);
    }

    private void declareArguments(SourceSection sourceSection, String methodName, SharedMethodInfo sharedMethodInfo) {
        final ParameterCollector parameterCollector = new ParameterCollector();
        argsNode.accept(parameterCollector);

        for (String parameter : parameterCollector.getParameters()) {
            environment.declareVar(parameter);
        }
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

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), node.getArgsNode(), environment.getNamedMethodName());

        final RubyNode arguments = new ReadSuperArgumentsNode(context, sourceSection, argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
        final RubyNode block = executeOrInheritBlock(sourceSection, argumentsAndBlock.getBlock());
        return new SuperCallNode(context, sourceSection, arguments, block);
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

        final ReloadArgumentsTranslator reloadTranslator = new ReloadArgumentsTranslator(currentNode, context, source, this);

        final ArgsNode argsNode = methodArgumentsTranslator.argsNode;
        final SequenceNode reloadSequence = (SequenceNode) reloadTranslator.visitArgsNode(argsNode);

        final RubyNode arguments = new ReadZSuperArgumentsNode(context, sourceSection,
                reloadTranslator.isSplatted(),
                reloadSequence.getSequence());
        final RubyNode block = executeOrInheritBlock(sourceSection, blockNode);
        return new SuperCallNode(context, sourceSection, arguments, block);
    }

    private RubyNode executeOrInheritBlock(SourceSection sourceSection, RubyNode blockNode) {
        if (blockNode != null) {
            return blockNode;
        } else {
            return new ReadBlockNode(context, sourceSection, context.getCoreLibrary().getNilObject());
        }
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

    /*
     * The following methods allow us to save and restore enough of
     * the current state of the Translator to allow lazy parsing. When
     * the lazy parsing is actually performed, the state is restored
     * to what it would have been if the method had been parsed
     * eagerly.
     */
    public TranslatorState getCurrentState() {
        return new TranslatorState(getEnvironment().getLexicalScope(), new ArrayDeque<SourceSection>(parentSourceSection));
    }

    public void restoreState(TranslatorState state) {
        this.getEnvironment().getParseEnvironment().resetLexicalScope(state.scope);
        this.parentSourceSection = state.parentSourceSection;
    }

    public static class TranslatorState {
        private final LexicalScope scope;
        private final Deque<SourceSection> parentSourceSection;

        private TranslatorState(LexicalScope scope, Deque<SourceSection> parentSourceSection) {
            this.scope = scope;
            this.parentSourceSection = parentSourceSection;
        }
    }
}
