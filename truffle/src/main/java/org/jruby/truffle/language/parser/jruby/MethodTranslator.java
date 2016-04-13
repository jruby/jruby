/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.KeywordArgNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.types.INameNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.IsNilNode;
import org.jruby.truffle.core.cast.ArrayCastNodeGen;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ReadBlockNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ShouldDestructureNode;
import org.jruby.truffle.language.control.AndNode;
import org.jruby.truffle.language.control.IfElseNode;
import org.jruby.truffle.language.control.NotNode;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.locals.FlipFlopStateNode;
import org.jruby.truffle.language.locals.LocalVariableType;
import org.jruby.truffle.language.locals.ReadLocalVariableNode;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.BlockDefinitionNode;
import org.jruby.truffle.language.methods.CatchForLambdaNode;
import org.jruby.truffle.language.methods.CatchForMethodNode;
import org.jruby.truffle.language.methods.CatchForProcNode;
import org.jruby.truffle.language.methods.ExceptionTranslatingNode;
import org.jruby.truffle.language.methods.MethodDefinitionNode;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.language.supercall.ReadSuperArgumentsNode;
import org.jruby.truffle.language.supercall.ReadZSuperArgumentsNode;
import org.jruby.truffle.language.supercall.SuperCallNode;
import org.jruby.truffle.language.supercall.ZSuperOutsideMethodNode;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class MethodTranslator extends BodyTranslator {

    private final org.jruby.ast.ArgsNode argsNode;
    private boolean isBlock;

    public MethodTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source, org.jruby.ast.ArgsNode argsNode) {
        super(currentNode, context, parent, environment, source, false);
        this.isBlock = isBlock;
        this.argsNode = argsNode;
    }

    public BlockDefinitionNode compileBlockNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo, ProcType type) {
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

        final boolean isProc = type == ProcType.PROC;
        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isProc, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);

        final RubyNode preludeProc;
        if (shouldConsiderDestructuringArrayArg(arity)) {
            final RubyNode readArrayNode = new ReadPreArgumentNode(context, sourceSection, 0, MissingArgumentBehavior.RUNTIME_ERROR);
            final RubyNode castArrayNode = ArrayCastNodeGen.create(context, sourceSection, readArrayNode);

            final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
            final RubyNode writeArrayNode = new WriteLocalVariableNode(context, sourceSection, arraySlot, castArrayNode);

            final LoadArgumentsTranslator destructureArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isProc, this);
            destructureArgumentsTranslator.pushArraySlot(arraySlot);
            final RubyNode newDestructureArguments = argsNode.accept(destructureArgumentsTranslator);

            final RubyNode shouldDestructure = new ShouldDestructureNode(context, sourceSection, readArrayNode);

            final RubyNode arrayWasNotNil = sequence(context, sourceSection, Arrays.asList(writeArrayNode, new NotNode(context, sourceSection, new IsNilNode(context, sourceSection, new ReadLocalVariableNode(context, sourceSection, LocalVariableType.FRAME_LOCAL, arraySlot)))));

            final RubyNode shouldDestructureAndArrayWasNotNil = new AndNode(context, sourceSection,
                    shouldDestructure,
                    arrayWasNotNil);

            preludeProc = new IfElseNode(context, sourceSection,
                    shouldDestructureAndArrayWasNotNil,
                    newDestructureArguments,
                    loadArguments);
        } else {
            preludeProc = loadArguments;
        }

        final RubyNode checkArity = Translator.createCheckArityNode(context, sourceSection, arityForCheck);

        final RubyNode preludeLambda = sequence(context, sourceSection, Arrays.asList(checkArity, NodeUtil.cloneNode(loadArguments)));

        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            if (argsNode.getBlockLocalVariables() != null && !argsNode.getBlockLocalVariables().isEmpty()) {
                for (org.jruby.ast.Node var : argsNode.getBlockLocalVariables().children()) {
                    environment.declareVar(((INameNode) var).getName());
                }
            }

            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        // Procs
        final RubyNode bodyProc = new CatchForProcNode(context, enclosing(sourceSection, body.getEncapsulatingSourceSection()), composeBody(preludeProc, NodeUtil.cloneNode(body)));

        final RubyRootNode newRootNodeForProcs = new RubyRootNode(context, considerExtendingMethodToCoverEnd(bodyProc.getEncapsulatingSourceSection()), environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                bodyProc, environment.needsDeclarationFrame());

        // Lambdas
        final RubyNode composed = composeBody(preludeLambda, body /* no copy, last usage */);
        final RubyNode bodyLambda = new CatchForLambdaNode(context, composed.getEncapsulatingSourceSection(), environment.getReturnID(), composed);

        final RubyRootNode newRootNodeForLambdas = new RubyRootNode(
                context, considerExtendingMethodToCoverEnd(bodyLambda.getEncapsulatingSourceSection()),
                environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                bodyLambda,
                environment.needsDeclarationFrame());

        // TODO CS 23-Nov-15 only the second one will get instrumented properly!
        final CallTarget callTargetAsLambda = Truffle.getRuntime().createCallTarget(newRootNodeForLambdas);
        final CallTarget callTargetAsProc = Truffle.getRuntime().createCallTarget(newRootNodeForProcs);


        Object frameOnStackMarkerSlot;

        if (frameOnStackMarkerSlotStack.isEmpty()) {
            frameOnStackMarkerSlot = null;
        } else {
            frameOnStackMarkerSlot = frameOnStackMarkerSlotStack.peek();

            if (frameOnStackMarkerSlot == BAD_FRAME_SLOT) {
                frameOnStackMarkerSlot = null;
            }
        }

        return new BlockDefinitionNode(context, newRootNodeForProcs.getEncapsulatingSourceSection(), type, environment.getSharedMethodInfo(),
                callTargetAsProc, callTargetAsLambda, environment.getBreakID(), (FrameSlot) frameOnStackMarkerSlot);
    }

    private boolean shouldConsiderDestructuringArrayArg(Arity arity) {
        if (arity.hasKeywordsRest())
            return true;
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
        final SourceSection sourceSection = enclosing(prelude.getSourceSection(), body.getSourceSection());

        body = sequence(context, sourceSection, Arrays.asList(prelude, body));

        if (environment.getFlipFlopStates().size() > 0) {
            body = sequence(context, sourceSection, Arrays.asList(initFlipFlopStates(sourceSection), body));
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

        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, false, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);
        
        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        final RubyNode prelude;

        if (usesRubiniusPrimitive) {
            // Use Rubinius.primitive seems to turn off arity checking. See Time.from_array for example.
            prelude = loadArguments;
        } else {
            final RubyNode checkArity = Translator.createCheckArityNode(context, sourceSection, arity);

            prelude = sequence(context, sourceSection, Arrays.asList(checkArity, loadArguments));
        }

        body = sequence(context, body.getSourceSection(), Arrays.asList(prelude, body));

        if (environment.getFlipFlopStates().size() > 0) {
            body = sequence(context, body.getSourceSection(), Arrays.asList(initFlipFlopStates(sourceSection), body));
        }

        body = new CatchForMethodNode(context, body.getSourceSection(), environment.getReturnID(), body);

        // TODO(CS, 10-Jan-15) why do we only translate exceptions in methods and not blocks?
        body = new ExceptionTranslatingNode(context, body.getSourceSection(), body, UnsupportedOperationBehavior.TYPE_ERROR);
        return body;
    }

    public MethodDefinitionNode compileMethodNode(SourceSection sourceSection, String methodName, org.jruby.ast.Node bodyNode, SharedMethodInfo sharedMethodInfo) {
        final RubyNode body = compileMethodBody(sourceSection,  methodName, bodyNode, sharedMethodInfo);
        final RubyRootNode rootNode = new RubyRootNode(
                context, considerExtendingMethodToCoverEnd(body.getSourceSection()), environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        return new MethodDefinitionNode(context, sourceSection, methodName, environment.getSharedMethodInfo(), callTarget);
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

    private static SourceSection considerExtendingMethodToCoverEnd(SourceSection sourceSection) {
        final Source source = sourceSection.getSource();

        if (sourceSection.getEndLine() + 1 >= source.getLineCount()) {
            return sourceSection;
        }

        final String indentationOnFirstLine = indentation(source.getCode(sourceSection.getStartLine()));

        final int lineAfter = sourceSection.getEndLine() + 1;
        final String lineAfterString = source.getCode(lineAfter).replaceAll("\\s+$","");

        if (lineAfterString.equals(indentationOnFirstLine + "end") || lineAfterString.equals(indentationOnFirstLine + "}")) {
            return source.createSection(sourceSection.getIdentifier(), sourceSection.getCharIndex(), sourceSection.getCharLength() + 1 + source.getLineLength(lineAfter));
        }

        return sourceSection;
    }

    private static String indentation(String line) {
        for (int n = 0; n < line.length(); n++) {
            if (!Character.isWhitespace(line.charAt(n))) {
                return line.substring(0, n);
            }
        }

        return "";
    }

}
