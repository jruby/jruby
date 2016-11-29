/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.IsNilNode;
import org.jruby.truffle.core.cast.ArrayCastNodeGen;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
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
import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.AssignableParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.KeywordArgParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.SuperParseNode;
import org.jruby.truffle.parser.ast.UnnamedRestArgParseNode;
import org.jruby.truffle.parser.ast.ZSuperParseNode;
import org.jruby.truffle.tools.ChaosNodeGen;

import java.util.Arrays;

public class MethodTranslator extends BodyTranslator {

    private final ArgsParseNode argsNode;
    private boolean isBlock;

    public MethodTranslator(Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, boolean isBlock, Source source, ArgsParseNode argsNode) {
        super(currentNode, context, parent, environment, source, false);
        this.isBlock = isBlock;
        this.argsNode = argsNode;
    }

    public BlockDefinitionNode compileBlockNode(RubySourceSection sourceSection, String methodName, ParseNode bodyNode, SharedMethodInfo sharedMethodInfo, ProcType type, String[] variables) {
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        declareArguments();
        final Arity arity = getArity(argsNode);
        final Arity arityForCheck;

        /*
         * If you have a block with parameters |a,| Ruby checks the arity as if was minimum 1, maximum 1. That's
         * counter-intuitive - as you'd expect the anonymous rest argument to cause it to have no maximum. Indeed,
         * that's how JRuby reports it, and by the look of their failing spec they consider this to be correct. We'll
         * follow the specs for now until we see a reason to do something else.
         */

        if (argsNode.getRestArgNode() instanceof UnnamedRestArgParseNode && !((UnnamedRestArgParseNode) argsNode.getRestArgNode()).isStar()) {
            arityForCheck = arity.withRest(false);
        } else {
            arityForCheck = arity;
        }

        final boolean isProc = type == ProcType.PROC;
        final LoadArgumentsTranslator loadArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isProc, this);
        final RubyNode loadArguments = argsNode.accept(loadArgumentsTranslator);

        final RubyNode preludeProc;
        if (shouldConsiderDestructuringArrayArg(arity)) {
            final RubyNode readArrayNode = new ProfileArgumentNode(new ReadPreArgumentNode(0, MissingArgumentBehavior.RUNTIME_ERROR));
            final RubyNode castArrayNode = ArrayCastNodeGen.create(context, fullSourceSection, readArrayNode);

            final FrameSlot arraySlot = environment.declareVar(environment.allocateLocalTemp("destructure"));
            final RubyNode writeArrayNode = WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, arraySlot, castArrayNode);

            final LoadArgumentsTranslator destructureArgumentsTranslator = new LoadArgumentsTranslator(currentNode, context, source, isProc, this);
            destructureArgumentsTranslator.pushArraySlot(arraySlot);
            final RubyNode newDestructureArguments = argsNode.accept(destructureArgumentsTranslator);

            final RubyNode shouldDestructure = new ShouldDestructureNode(readArrayNode);

            final RubyNode arrayWasNotNil = sequence(context, source, sourceSection,
                    Arrays.asList(writeArrayNode, new NotNode(new IsNilNode(context, fullSourceSection, new ReadLocalVariableNode(context, fullSourceSection, LocalVariableType.FRAME_LOCAL, arraySlot)))));

            final RubyNode shouldDestructureAndArrayWasNotNil = new AndNode(
                    shouldDestructure,
                    arrayWasNotNil);

            preludeProc = new IfElseNode(
                    shouldDestructureAndArrayWasNotNil,
                    newDestructureArguments,
                    loadArguments);
        } else {
            preludeProc = loadArguments;
        }

        final RubyNode checkArity = createCheckArityNode(context, source, sourceSection, arityForCheck);

        final RubyNode preludeLambda = sequence(context, source, sourceSection, Arrays.asList(checkArity, NodeUtil.cloneNode(loadArguments)));

        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            if (!translatingForStatement) {
                // Make sure to declare block-local variables
                for (String var : variables) {
                    environment.declareVar(var);
                }
            }

            body = translateNodeOrNil(sourceSection, bodyNode);

            if (context.getOptions().CHAOS) {
                body = ChaosNodeGen.create(body);
            }
        } finally {
            parentSourceSection.pop();
        }

        // Procs
        final RubyNode bodyProc = new CatchForProcNode(context, translateSourceSection(source, enclosing(sourceSection, body)), composeBody(sourceSection, preludeProc, NodeUtil.cloneNode(body)));

        final RubyRootNode newRootNodeForProcs = new RubyRootNode(context, translateSourceSection(source, considerExtendingMethodToCoverEnd(sourceSection)), environment.getFrameDescriptor(), environment.getSharedMethodInfo(),
                bodyProc, environment.needsDeclarationFrame());

        // Lambdas
        final RubyNode composed = composeBody(sourceSection, preludeLambda, body /* no copy, last usage */);
        final RubyNode bodyLambda = new CatchForLambdaNode(context, sourceSection.toSourceSection(source), environment.getReturnID(), composed);

        final RubyRootNode newRootNodeForLambdas = new RubyRootNode(
                context, translateSourceSection(source, considerExtendingMethodToCoverEnd(sourceSection)),
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

        return new BlockDefinitionNode(context, newRootNodeForProcs.getSourceSection(), type, environment.getSharedMethodInfo(),
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

    private RubyNode composeBody(RubySourceSection preludeSourceSection, RubyNode prelude, RubyNode body) {
        final RubySourceSection sourceSection = enclosing(preludeSourceSection, body);

        body = sequence(context, source, sourceSection, Arrays.asList(prelude, body));

        if (environment.getFlipFlopStates().size() > 0) {
            body = sequence(context, source, sourceSection, Arrays.asList(initFlipFlopStates(sourceSection), body));
        }

        return body;
    }

    /*
     * This method exists solely to be substituted to support lazy
     * method parsing. The substitution returns a node which performs
     * the parsing lazily and then calls doCompileMethodBody.
     */
    public RubyNode compileMethodBody(RubySourceSection sourceSection, String methodName, ParseNode bodyNode, SharedMethodInfo sharedMethodInfo) {
        return doCompileMethodBody(sourceSection, methodName, bodyNode, sharedMethodInfo);
    }

    public RubyNode doCompileMethodBody(RubySourceSection sourceSection, String methodName, ParseNode bodyNode, SharedMethodInfo sharedMethodInfo) {
        declareArguments();
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
            // Use Truffle.primitive seems to turn off arity checking. See Time.from_array for example.
            prelude = loadArguments;
        } else {
            final RubyNode checkArity = createCheckArityNode(context, source, sourceSection, arity);

            prelude = sequence(context, source, sourceSection, Arrays.asList(checkArity, loadArguments));
        }

        body = sequence(context, source, body.getRubySourceSection(), Arrays.asList(prelude, body));

        if (environment.getFlipFlopStates().size() > 0) {
            body = sequence(context, source, body.getRubySourceSection(), Arrays.asList(initFlipFlopStates(sourceSection), body));
        }

        body = new CatchForMethodNode(context, translateSourceSection(source, body.getRubySourceSection()), environment.getReturnID(), body);

        // TODO(CS, 10-Jan-15) why do we only translate exceptions in methods and not blocks?
        body = new ExceptionTranslatingNode(context, translateSourceSection(source, body.getRubySourceSection()), body, UnsupportedOperationBehavior.TYPE_ERROR);

        if (context.getOptions().CHAOS) {
            body = ChaosNodeGen.create(body);
        }

        return body;
    }

    public MethodDefinitionNode compileMethodNode(RubySourceSection sourceSection, String methodName, ParseNode bodyNode, SharedMethodInfo sharedMethodInfo) {
        final RubyNode body = compileMethodBody(sourceSection, methodName, bodyNode, sharedMethodInfo);

        final SourceSection extendedBodySourceSection;

        if (body.getRubySourceSection() == null) {
            extendedBodySourceSection = sourceSection.toSourceSection(source);
        } else {
            extendedBodySourceSection = translateSourceSection(source, considerExtendingMethodToCoverEnd(body.getRubySourceSection()));
        }

        final RubyRootNode rootNode = new RubyRootNode(
                context, extendedBodySourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        final CallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        return new MethodDefinitionNode(context, translateSourceSection(source, body.getRubySourceSection()), methodName, environment.getSharedMethodInfo(), callTarget);
    }

    private void declareArguments() {
        final ParameterCollector parameterCollector = new ParameterCollector();
        argsNode.accept(parameterCollector);

        for (String parameter : parameterCollector.getParameters()) {
            environment.declareVar(parameter);
        }
    }

    public static Arity getArity(ArgsParseNode argsNode) {
        final String[] keywordArguments;

        if (argsNode.hasKwargs() && argsNode.getKeywordCount() > 0) {
            final ParseNode[] keywordNodes = argsNode.getKeywords().children();
            final int keywordsCount = keywordNodes.length;

            keywordArguments = new String[keywordsCount];
            for (int i = 0; i < keywordsCount; i++) {
                final KeywordArgParseNode kwarg = (KeywordArgParseNode) keywordNodes[i];
                final AssignableParseNode assignableNode = kwarg.getAssignable();

                if (assignableNode instanceof LocalAsgnParseNode) {
                    keywordArguments[i] = ((LocalAsgnParseNode) assignableNode).getName();
                } else if (assignableNode instanceof DAsgnParseNode) {
                    keywordArguments[i] = ((DAsgnParseNode) assignableNode).getName();
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
    public RubyNode visitSuperNode(SuperParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, node.getIterNode(), node.getArgsNode(), environment.getNamedMethodName());

        final RubyNode arguments = new ReadSuperArgumentsNode(context, fullSourceSection, argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted());
        final RubyNode block = executeOrInheritBlock(argumentsAndBlock.getBlock());
        return new SuperCallNode(context, fullSourceSection, arguments, block);
    }

    @Override
    public RubyNode visitZSuperNode(ZSuperParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

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
                return new ZSuperOutsideMethodNode(context, fullSourceSection, insideDefineMethod);
            } else if (methodArgumentsTranslator.currentCallMethodName != null && methodArgumentsTranslator.currentCallMethodName.equals("define_method")) {
                insideDefineMethod = true;
            }
            methodArgumentsTranslator = (MethodTranslator) methodArgumentsTranslator.parent;
        }

        final ReloadArgumentsTranslator reloadTranslator = new ReloadArgumentsTranslator(currentNode, context, source, this);

        final ArgsParseNode argsNode = methodArgumentsTranslator.argsNode;
        final SequenceNode reloadSequence = (SequenceNode) reloadTranslator.visitArgsNode(argsNode);

        final RubyNode arguments = new ReadZSuperArgumentsNode(context, fullSourceSection,
                reloadTranslator.isSplatted(),
                reloadSequence.getSequence());
        final RubyNode block = executeOrInheritBlock(blockNode);
        return new SuperCallNode(context, fullSourceSection, arguments, block);
    }

    private RubyNode executeOrInheritBlock(RubyNode blockNode) {
        if (blockNode != null) {
            return blockNode;
        } else {
            return new ReadBlockNode(context.getCoreLibrary().getNilObject());
        }
    }

    @Override
    protected FlipFlopStateNode createFlipFlopState(RubySourceSection sourceSection, int depth) {
        if (isBlock) {
            environment.setNeedsDeclarationFrame();
            return parent.createFlipFlopState(sourceSection, depth + 1);
        } else {
            return super.createFlipFlopState(sourceSection, depth);
        }
    }

    private RubySourceSection considerExtendingMethodToCoverEnd(RubySourceSection sourceSection) {
        if (sourceSection == null) {
            return sourceSection;
        }

        if (sourceSection.getEndLine() + 1 >= source.getLineCount()) {
            return sourceSection;
        }

        final String indentationOnFirstLine = indentation(source.getCode(sourceSection.getStartLine()));

        int lineAfter = sourceSection.getEndLine() + 1;
        for (;;) {
            final String lineAfterString = source.getCode(lineAfter).replaceAll("\\s+$","");
            if (lineAfterString.equals(indentationOnFirstLine + "end") || lineAfterString.equals(indentationOnFirstLine + "}")) {
                return new RubySourceSection(sourceSection.getStartLine(), sourceSection.getEndLine() + 1);
            }
            if (++lineAfter >= source.getLineCount()) {
                return sourceSection;
            }
        }
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
