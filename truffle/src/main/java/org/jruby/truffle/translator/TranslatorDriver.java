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
import com.oracle.truffle.api.frame.FrameInstance.FrameAccess;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import org.jcodings.Encoding;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.methods.CatchNextNode;
import org.jruby.truffle.nodes.methods.CatchRetryAsErrorNode;
import org.jruby.truffle.nodes.methods.CatchReturnAsErrorNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.InternalMethod;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.nio.charset.StandardCharsets;

public class TranslatorDriver {

    public static enum ParserContext {
        TOP_LEVEL, SHELL, MODULE, EVAL, INLINE
    }

    private final ParseEnvironment parseEnvironment;

    public TranslatorDriver(RubyContext context) {
        parseEnvironment = new ParseEnvironment(context);
    }

    public RubyRootNode parse(RubyContext context, Source source, Encoding defaultEncoding, ParserContext parserContext, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode) {
        // Set up the JRuby parser

        final org.jruby.parser.Parser parser = new org.jruby.parser.Parser(context.getRuntime());

        final StaticScope staticScope = context.getRuntime().getStaticScopeFactory().newLocalScope(null);
        if (parentFrame != null) {
            /*
             * Note that jruby-parser will be mistaken about how deep the existing variables are,
             * but that doesn't matter as we look them up ourselves after being told they're in some
             * parent scope.
             */

            MaterializedFrame frame = parentFrame;

            while (frame != null) {
                for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                    if (slot.getIdentifier() instanceof String) {
                        final String name = (String) slot.getIdentifier();
                        staticScope.addVariableThisScope(name.intern()); // StaticScope expects interned var names
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }
        }

        final DynamicScope dynamicScope = new ManyVarsDynamicScope(staticScope);

        boolean isInlineSource = parserContext == ParserContext.SHELL;
        boolean isEvalParse = parserContext == ParserContext.EVAL || parserContext == ParserContext.INLINE || parserContext == ParserContext.MODULE;
        final org.jruby.parser.ParserConfiguration parserConfiguration = new org.jruby.parser.ParserConfiguration(context.getRuntime(), 0, isInlineSource, !isEvalParse, true);
        parserConfiguration.setDefaultEncoding(defaultEncoding);

        // Parse to the JRuby AST

        org.jruby.ast.RootNode node;

        try {
            node = (org.jruby.ast.RootNode) parser.parse(source.getName(), source.getCode().getBytes(StandardCharsets.UTF_8), dynamicScope, parserConfiguration);
        } catch (org.jruby.exceptions.RaiseException e) {
            String message = e.getException().getMessage().asJavaString();

            if (message == null) {
                message = "(no message)";
            }

            throw new RaiseException(context.getCoreLibrary().syntaxError(message, currentNode));
        }

        return parse(currentNode, context, source, parserContext, parentFrame, ownScopeForAssignments, node);
    }

    private RubyRootNode parse(Node currentNode, RubyContext context, Source source, ParserContext parserContext, MaterializedFrame parentFrame, boolean ownScopeForAssignments, org.jruby.ast.RootNode rootNode) {
        final SourceSection sourceSection = source.createSection("<main>", 0, source.getCode().length());

        final InternalMethod parentMethod = parentFrame == null ? null : RubyArguments.getMethod(parentFrame.getArguments());
        LexicalScope lexicalScope;
        if (parentMethod != null && parentMethod.getSharedMethodInfo().getLexicalScope() != null) {
            lexicalScope = parentMethod.getSharedMethodInfo().getLexicalScope();
        } else {
            lexicalScope = context.getRootLexicalScope();
        }
        if (parserContext == ParserContext.MODULE) {
            Object module = RubyArguments.getSelf(Truffle.getRuntime().getCurrentFrame().getFrame(FrameAccess.READ_ONLY, true).getArguments());
            lexicalScope = new LexicalScope(lexicalScope, (DynamicObject) module);
        }
        parseEnvironment.resetLexicalScope(lexicalScope);

        // TODO (10 Feb. 2015): name should be "<top (required)> for the require-d/load-ed files.
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, parseEnvironment.getLexicalScope(), Arity.NO_ARGUMENTS, "<main>", false, null, false, false, false);

        final TranslatorEnvironment environment = new TranslatorEnvironment(context, environmentForFrame(context, parentFrame),
                parseEnvironment, parseEnvironment.allocateReturnID(), ownScopeForAssignments, false, sharedMethodInfo, sharedMethodInfo.getName(), false, null);

        // Get the DATA constant

        final Object data = getData(context);

        if (data != null) {
            Layouts.MODULE.getFields(context.getCoreLibrary().getObjectClass()).setConstant(context, currentNode, "DATA", data);
        }

        // Translate to Ruby Truffle nodes

        final BodyTranslator translator = new BodyTranslator(currentNode, context, null, environment, source, parserContext == ParserContext.TOP_LEVEL);

        RubyNode truffleNode;

        if (rootNode.getBodyNode() == null || rootNode.getBodyNode() instanceof org.jruby.ast.NilNode) {
            translator.parentSourceSection.push(sourceSection);
            try {
                truffleNode = translator.nilNode(sourceSection);
            } finally {
                translator.parentSourceSection.pop();
            }
        } else {
            truffleNode = rootNode.getBodyNode().accept(translator);
        }

        // Load flip-flop states

        if (environment.getFlipFlopStates().size() > 0) {
            truffleNode = SequenceNode.sequence(context, truffleNode.getSourceSection(), translator.initFlipFlopStates(truffleNode.getSourceSection()), truffleNode);
        }

        // Catch next

        truffleNode = new CatchNextNode(context, truffleNode.getSourceSection(), truffleNode);

        // Catch return

        if (parserContext != ParserContext.INLINE) {
            truffleNode = new CatchReturnAsErrorNode(context, truffleNode.getSourceSection(), truffleNode);
        }

        // Catch retry

        truffleNode = new CatchRetryAsErrorNode(context, truffleNode.getSourceSection(), truffleNode);

        // Shell result

        return new RubyRootNode(context, truffleNode.getSourceSection(), environment.getFrameDescriptor(), sharedMethodInfo, truffleNode, environment.needsDeclarationFrame());
    }

    private Object getData(RubyContext context) {
        // TODO CS 18-Apr-15 restore the DATA functionality
        return null;
    }

    private TranslatorEnvironment environmentForFrame(RubyContext context, MaterializedFrame frame) {
        if (frame == null) {
            return null;
        } else {
            SourceSection sourceSection = SourceSection.createUnavailable("Unknown source section", "(unknown)");
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, context.getRootLexicalScope(), Arity.NO_ARGUMENTS, "(unknown)", false, null, false, false, false);
            final MaterializedFrame parent = RubyArguments.getDeclarationFrame(frame.getArguments());
            // TODO(CS): how do we know if the frame is a block or not?
            return new TranslatorEnvironment(context, environmentForFrame(context, parent), parseEnvironment,
                    parseEnvironment.allocateReturnID(), true, true, sharedMethodInfo, sharedMethodInfo.getName(), false, null, frame.getFrameDescriptor());
        }
    }

}
