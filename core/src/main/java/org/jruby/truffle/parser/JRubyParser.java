/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.api.source.SourceManager;
import org.jruby.truffle.JRubyTruffleBridge;
import org.jruby.truffle.nodes.*;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.runtime.*;
import org.jruby.truffle.runtime.control.*;
import org.jruby.truffle.runtime.core.*;
import org.jruby.truffle.runtime.debug.*;
import org.jruby.truffle.runtime.methods.*;

import org.jruby.Ruby;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

import java.io.Reader;

public class JRubyParser implements RubyParser {

    private final Ruby jruby;
    private long nextReturnID = 0;

    public JRubyParser(Ruby jruby) {
        this.jruby = jruby;
    }

    @Override
    public MethodDefinitionNode parse(RubyContext context, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode) {
        // TODO(cs) should this get a new unique method identifier or not?
        final TranslatorEnvironment environment = new TranslatorEnvironment(context, environmentForFrame(context, null), this, allocateReturnID(), true, true, new UniqueMethodIdentifier());

        // All parsing contexts have a visibility slot at their top level

        environment.addMethodDeclarationSlots();

        // Translate to Ruby Truffle nodes

        final MethodTranslator translator = new MethodTranslator(context, null, environment, false, JRubyTruffleBridge.DUMMY_SOURCE);

        return translator.compileFunctionNode(JRubyTruffleBridge.DUMMY_SOURCE_SECTION, "(unknown)", argsNode, bodyNode);
    }

    @Override
    public RubyParserResult parse(RubyContext context, Source source, ParserContext parserContext, MaterializedFrame parentFrame) {
        // Set up the JRuby parser

        final org.jruby.parser.Parser parser = new org.jruby.parser.Parser(jruby);

        final org.jruby.parser.LocalStaticScope staticScope = new org.jruby.parser.LocalStaticScope(null);

        if (parentFrame != null) {
            /*
             * Note that jruby-parser will be mistaken about how deep the existing variables are,
             * but that doesn't matter as we look them up ourselves after being told they're in some
             * parent scope.
             */

            MaterializedFrame frame = parentFrame;

            final org.jruby.lexer.yacc.ISourcePosition scopeSourceSection = new org.jruby.lexer.yacc.SimpleSourcePosition("(scope)", 0);

            while (frame != null) {
                for (FrameSlot slot : frame.getFrameDescriptor().getSlots()) {
                    if (slot.getIdentifier() instanceof String) {
                        final String name = (String) slot.getIdentifier();
                        staticScope.addVariableThisScope(name);
                    }
                }

                frame = frame.getArguments(RubyArguments.class).getDeclarationFrame();
            }
        }

        final org.jruby.parser.ParserConfiguration parserConfiguration = new org.jruby.parser.ParserConfiguration(jruby, 0, false, org.jruby.CompatVersion.RUBY2_1);

        // Parse to the JRuby AST

        org.jruby.ast.RootNode node;

        try {
            node = (org.jruby.ast.RootNode) parser.parse(source.getName(), source.getCode().getBytes(), new ManyVarsDynamicScope(staticScope), parserConfiguration);
        } catch (Exception e) {
            String message = e.getMessage();

            if (message == null) {
                message = "(no message)";
            }

            throw new RaiseException(new RubyException(context.getCoreLibrary().getSyntaxErrorClass(), message));
        }

        return parse(context, source, parserContext, parentFrame, node);
    }

    public RubyParserResult parse(RubyContext context, Source source, ParserContext parserContext, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode) {
        // TODO(cs) should this get a new unique method identifier or not?
        final TranslatorEnvironment environment = new TranslatorEnvironment(context, environmentForFrame(context, parentFrame), this, allocateReturnID(), true, true, new UniqueMethodIdentifier());

        // All parsing contexts have a visibility slot at their top level

        environment.addMethodDeclarationSlots();

        // Translate to Ruby Truffle nodes

        final Translator translator;

        if (parserContext == RubyParser.ParserContext.MODULE) {
            translator = new ModuleTranslator(context, null, environment, source);
        } else {
            translator = new Translator(context, null, environment, source);
        }

        RubyNode truffleNode;

        final RubyDebugManager debugManager = context.getDebugManager();
        try {
            if (debugManager != null) {
                debugManager.notifyStartLoading(source);
            }

            if (rootNode.getBodyNode() == null) {
                truffleNode = new NilNode(context, null);
            } else {
                truffleNode = (RubyNode) rootNode.getBodyNode().accept(translator);
            }

            // Load flip-flop states

            if (environment.getFlipFlopStates().size() > 0) {
                truffleNode = new SequenceNode(context, truffleNode.getSourceSection(), translator.initFlipFlopStates(truffleNode.getSourceSection()), truffleNode);
            }

            // Catch next

            truffleNode = new CatchNextNode(context, truffleNode.getSourceSection(), truffleNode);

            // Catch return

            truffleNode = new CatchReturnAsErrorNode(context, truffleNode.getSourceSection(), truffleNode);

            // Shell result

            if (parserContext == RubyParser.ParserContext.SHELL) {
                truffleNode = new ShellResultNode(context, truffleNode.getSourceSection(), truffleNode);
            }

            // Root Node

            String indicativeName;

            switch (parserContext) {
                case TOP_LEVEL:
                    indicativeName = "(main)";
                    break;
                case SHELL:
                    indicativeName = "(shell)";
                    break;
                case MODULE:
                    indicativeName = "(module)";
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            final RootNode root = new RubyRootNode(truffleNode.getSourceSection(), indicativeName, truffleNode);

            // Return the root and the frame descriptor

            return new RubyParserResult(root, environment.getFrameDescriptor());
        } finally {
            if (debugManager != null) {
                debugManager.notifyFinishedLoading(source);
            }
        }
    }

    public long allocateReturnID() {
        if (nextReturnID == Long.MAX_VALUE) {
            throw new RuntimeException("Return IDs exhausted");
        }

        final long allocated = nextReturnID;
        nextReturnID++;
        return allocated;
    }

    private TranslatorEnvironment environmentForFrame(RubyContext context, MaterializedFrame frame) {
        if (frame == null) {
            return null;
        } else {
            final MaterializedFrame parent = frame.getArguments(RubyArguments.class).getDeclarationFrame();
            return new TranslatorEnvironment(context, environmentForFrame(context, parent), frame.getFrameDescriptor(), this, allocateReturnID(), true, true, new UniqueMethodIdentifier());
        }
    }

}
