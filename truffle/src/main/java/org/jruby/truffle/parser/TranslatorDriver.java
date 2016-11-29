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

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.LoadRequiredLibrariesNode;
import org.jruby.truffle.core.SetTopLevelBindingNode;
import org.jruby.truffle.language.DataNode;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.RubyArguments;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.exceptions.TopLevelRaiseHandler;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.CatchNextNode;
import org.jruby.truffle.language.methods.CatchRetryAsErrorNode;
import org.jruby.truffle.language.methods.CatchReturnAsErrorNode;
import org.jruby.truffle.language.methods.ExceptionTranslatingNode;
import org.jruby.truffle.language.methods.InternalMethod;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.parser.ast.NilParseNode;
import org.jruby.truffle.parser.ast.RootParseNode;
import org.jruby.truffle.parser.parser.ParserConfiguration;
import org.jruby.truffle.parser.scope.DynamicScope;
import org.jruby.truffle.parser.scope.ManyVarsDynamicScope;
import org.jruby.truffle.parser.scope.StaticScope;
import org.jruby.truffle.parser.scope.StaticScopeFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranslatorDriver {

    private final ParseEnvironment parseEnvironment;

    public TranslatorDriver(RubyContext context) {
        parseEnvironment = new ParseEnvironment(context);
    }

    public RubyRootNode parse(RubyContext context, Source source, Encoding defaultEncoding, ParserContext parserContext, String[] argumentNames, FrameDescriptor frameDescriptor, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode) {
        // Set up the JRuby parser

        final org.jruby.truffle.parser.parser.Parser parser = new org.jruby.truffle.parser.parser.Parser(context);

        final StaticScopeFactory staticScopeFactory = new StaticScopeFactory();
        final StaticScope staticScope = staticScopeFactory.newLocalScope(null);

        /*
         * Note that jruby-parser will be mistaken about how deep the existing variables are,
         * but that doesn't matter as we look them up ourselves after being told they're in some
         * parent scope.
         */

        final TranslatorEnvironment parentEnvironment;

        if (frameDescriptor != null) {
            for (FrameSlot slot : frameDescriptor.getSlots()) {
                if (slot.getIdentifier() instanceof String) {
                    final String name = (String) slot.getIdentifier();
                    staticScope.addVariableThisScope(name.intern()); // StaticScope expects interned var names
                }
            }

            parentEnvironment = environmentForFrameDescriptor(context, frameDescriptor);
        } else if (parentFrame != null) {
            MaterializedFrame frame = parentFrame;

            while (frame != null) {
                for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                    if (slot.getIdentifier() instanceof String) {
                        final String name = (String) slot.getIdentifier();
                        staticScope.addVariableThisScope(name.intern()); // StaticScope expects interned var names
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame);
            }

            parentEnvironment = environmentForFrame(context, parentFrame);
        } else {
            parentEnvironment = environmentForFrame(context, null);
        }

        if (argumentNames != null) {
            for (String name : argumentNames) {
                staticScope.addVariableThisScope(name.intern()); // StaticScope expects interned var names
            }
        }

        final DynamicScope dynamicScope = new ManyVarsDynamicScope(staticScope);

        boolean isInlineSource = parserContext == ParserContext.SHELL;
        boolean isEvalParse = parserContext == ParserContext.EVAL || parserContext == ParserContext.INLINE || parserContext == ParserContext.MODULE;
        final ParserConfiguration parserConfiguration = new ParserConfiguration(context, 0, isInlineSource, !isEvalParse, false);

        if (context.getInstanceConfig().isFrozenStringLiteral()) {
            parserConfiguration.setFrozenStringLiteral(true);
        }

        parserConfiguration.setDefaultEncoding(defaultEncoding);

        // Parse to the JRuby AST

        RootParseNode node;

        try {
            node = (RootParseNode) parser.parse(source.getName(), source.getCode().getBytes(StandardCharsets.UTF_8), dynamicScope, parserConfiguration);
        } catch (org.jruby.exceptions.RaiseException e) {
            String message = e.getException().getMessage().asJavaString();

            if (message == null) {
                message = "(no message)";
            }

            throw new RaiseException(context.getCoreExceptions().syntaxError(message, currentNode));
        }

        final SourceSection sourceSection = source.createSection(0, source.getCode().length());
        final RubySourceSection rubySourceSection = new RubySourceSection(sourceSection);

        final InternalMethod parentMethod = parentFrame == null ? null : RubyArguments.getMethod(parentFrame);
        LexicalScope lexicalScope;
        if (parentMethod != null && parentMethod.getSharedMethodInfo().getLexicalScope() != null) {
            lexicalScope = parentMethod.getSharedMethodInfo().getLexicalScope();
        } else {
            lexicalScope = context.getRootLexicalScope();
        }
        if (parserContext == ParserContext.MODULE) {
            Object module = RubyArguments.getSelf(Truffle.getRuntime().getCurrentFrame().getFrame(FrameAccess.READ_ONLY, true));
            lexicalScope = new LexicalScope(lexicalScope, (DynamicObject) module);
        }
        parseEnvironment.resetLexicalScope(lexicalScope);

        // TODO (10 Feb. 2015): name should be "<top (required)> for the require-d/load-ed files.
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                sourceSection,
                parseEnvironment.getLexicalScope(),
                Arity.NO_ARGUMENTS,
                null,
                "<main>",
                null,
                null,
                false,
                false,
                false);

        final TranslatorEnvironment environment = new TranslatorEnvironment(context, parentEnvironment,
                parseEnvironment, parseEnvironment.allocateReturnID(), ownScopeForAssignments, false, sharedMethodInfo, sharedMethodInfo.getName(), 0, null);

        // Declare arguments as local variables in the top-level environment - we'll put the values there in a prelude

        if (argumentNames != null) {
            for (String name : argumentNames) {
                environment.declareVar(name);
            }
        }

        // Translate to Ruby Truffle nodes

        final BodyTranslator translator = new BodyTranslator(currentNode, context, null, environment, source, parserContext == ParserContext.TOP_LEVEL_FIRST || parserContext == ParserContext.TOP_LEVEL);

        RubyNode truffleNode;

        if (node.getBodyNode() == null || node.getBodyNode() instanceof NilParseNode) {
            translator.parentSourceSection.push(rubySourceSection);
            try {
                truffleNode = translator.nilNode(source, rubySourceSection);
            } finally {
                translator.parentSourceSection.pop();
            }
        } else {
            truffleNode = node.getBodyNode().accept(translator);
        }

        // Load arguments

        final RubyNode writeSelfNode = Translator.loadSelf(context, environment);
        truffleNode = Translator.sequence(context, source, rubySourceSection, Arrays.asList(writeSelfNode, truffleNode));

        if (argumentNames != null && argumentNames.length > 0) {
            final List<RubyNode> sequence = new ArrayList<>();

            for (int n = 0; n < argumentNames.length; n++) {
                final String name = argumentNames[n];
                final RubyNode readNode = new ProfileArgumentNode(new ReadPreArgumentNode(n, MissingArgumentBehavior.NIL));
                final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(name);
                sequence.add(WriteLocalVariableNode.createWriteLocalVariableNode(context, sourceSection, slot, readNode));
            }

            sequence.add(truffleNode);
            truffleNode = Translator.sequence(context, source, rubySourceSection, sequence);
        }

        // Load flip-flop states

        if (environment.getFlipFlopStates().size() > 0) {
            truffleNode = Translator.sequence(context, source, rubySourceSection, Arrays.asList(translator.initFlipFlopStates(rubySourceSection), truffleNode));
        }

        // Catch next

        truffleNode = new CatchNextNode(context, truffleNode.getRubySourceSection().toSourceSection(source), truffleNode);

        // Catch return

        if (parserContext != ParserContext.INLINE) {
            truffleNode = new CatchReturnAsErrorNode(context, truffleNode.getRubySourceSection().toSourceSection(source), truffleNode);
        }

        // Catch retry

        truffleNode = new CatchRetryAsErrorNode(context, truffleNode.getRubySourceSection().toSourceSection(source), truffleNode);

        if (parserContext == ParserContext.TOP_LEVEL_FIRST) {
            truffleNode = Translator.sequence(context, source, rubySourceSection, Arrays.asList(
                    new SetTopLevelBindingNode(context, sourceSection),
                    new LoadRequiredLibrariesNode(context, sourceSection),
                    truffleNode));

            if (node.hasEndPosition()) {
                truffleNode = Translator.sequence(context, source, rubySourceSection, Arrays.asList(
                        new DataNode(context, sourceSection, node.getEndPosition()),
                        truffleNode));
            }

            truffleNode = new ExceptionTranslatingNode(context, sourceSection, truffleNode, UnsupportedOperationBehavior.TYPE_ERROR);
            truffleNode = new TopLevelRaiseHandler(context, sourceSection, truffleNode);
        }

        return new RubyRootNode(context, truffleNode.getRubySourceSection().toSourceSection(source), environment.getFrameDescriptor(), sharedMethodInfo, truffleNode, environment.needsDeclarationFrame());
    }

    private TranslatorEnvironment environmentForFrameDescriptor(RubyContext context, FrameDescriptor frameDescriptor) {
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                context.getCoreLibrary().getSourceSection(),
                context.getRootLexicalScope(),
                Arity.NO_ARGUMENTS,
                null,
                null,
                "external",
                null,
                false,
                false,
                false);
            // TODO(CS): how do we know if the frame is a block or not?
            return new TranslatorEnvironment(context, null, parseEnvironment,
                    parseEnvironment.allocateReturnID(), true, true, sharedMethodInfo, sharedMethodInfo.getName(), 0, null, frameDescriptor);
    }

    private TranslatorEnvironment environmentForFrame(RubyContext context, MaterializedFrame frame) {
        if (frame == null) {
            return null;
        } else {
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    context.getCoreLibrary().getSourceSection(),
                    context.getRootLexicalScope(),
                    Arity.NO_ARGUMENTS,
                    null,
                    null,
                    "external",
                    null,
                    false,
                    false,
                    false);
            final MaterializedFrame parent = RubyArguments.getDeclarationFrame(frame);
            // TODO(CS): how do we know if the frame is a block or not?
            return new TranslatorEnvironment(context, environmentForFrame(context, parent), parseEnvironment,
                    parseEnvironment.allocateReturnID(), true, true, sharedMethodInfo, sharedMethodInfo.getName(), 0, null, frame.getFrameDescriptor());
        }
    }

}
