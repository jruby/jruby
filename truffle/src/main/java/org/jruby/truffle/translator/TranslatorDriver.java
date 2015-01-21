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

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.NullSourceSection;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.Encoding;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.RubyRootNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.methods.CatchNextNode;
import org.jruby.truffle.nodes.methods.CatchRetryAsErrorNode;
import org.jruby.truffle.nodes.methods.CatchReturnAsErrorNode;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.RubyException;
import org.jruby.truffle.runtime.core.RubyFile;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TranslatorDriver {

    public static enum ParserContext {
        TOP_LEVEL, SHELL, MODULE
    }

    private long nextReturnID = 0;

    public RubyNode parse(RubyContext context, org.jruby.ast.Node parseTree, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode, RubyNode currentNode) {
        final LexicalScope lexicalScope = context.getRootLexicalScope(); // TODO(eregon): figure out how to get the lexical scope from JRuby
        final SharedMethodInfo sharedMethod = new SharedMethodInfo(null, lexicalScope, "(unknown)", false, parseTree, false);

        final TranslatorEnvironment environment = new TranslatorEnvironment(
                context, environmentForFrame(context, null), this, allocateReturnID(), true, true, sharedMethod, sharedMethod.getName(), false);

        // Translate to Ruby Truffle nodes

        final MethodTranslator translator;

        try {
            translator = new MethodTranslator(currentNode, context, null, environment, false, Source.fromFileName(bodyNode.getPosition().getFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return translator.compileFunctionNode(null, "(unknown)", argsNode, bodyNode, sharedMethod);
    }

    public RubyRootNode parse(RubyContext context, Source source, Encoding defaultEncoding, ParserContext parserContext, MaterializedFrame parentFrame, RubyNode currentNode, NodeWrapper wrapper) {
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
                        staticScope.addVariableThisScope(name);
                    }
                }

                frame = RubyArguments.getDeclarationFrame(frame.getArguments());
            }
        }

        final org.jruby.parser.ParserConfiguration parserConfiguration = new org.jruby.parser.ParserConfiguration(context.getRuntime(), 0, false, parserContext == ParserContext.TOP_LEVEL, true);
        parserConfiguration.setDefaultEncoding(defaultEncoding);

        // Parse to the JRuby AST

        org.jruby.ast.RootNode node;

        try {
            node = (org.jruby.ast.RootNode) parser.parse(source.getName(), source.getCode().getBytes(StandardCharsets.UTF_8), new ManyVarsDynamicScope(staticScope), parserConfiguration);
        } catch (Exception e) {
            String message = e.getMessage();

            if (message == null) {
                message = "(no message)";
            }

            throw new RaiseException(new RubyException(context.getCoreLibrary().getSyntaxErrorClass(), context.makeString(message), RubyCallStack.getBacktrace(currentNode)));
        }

        return parse(currentNode, context, source, parserContext, parentFrame, node, wrapper);
    }

    public RubyRootNode parse(RubyNode currentNode, RubyContext context, Source source, ParserContext parserContext, MaterializedFrame parentFrame, org.jruby.ast.RootNode rootNode, NodeWrapper wrapper) {
        final SourceSection sourceSection = source.createSection("<main>", 0, source.getCode().length());
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, context.getRootLexicalScope(), "<main>", false, rootNode, false);

        final TranslatorEnvironment environment = new TranslatorEnvironment(context, environmentForFrame(context, parentFrame), this, allocateReturnID(), true, false, sharedMethodInfo, sharedMethodInfo.getName(), false);

        // Get the DATA constant

        final Object data = getData(context);

        if (data != null) {
            context.getCoreLibrary().getObjectClass().setConstant(currentNode, "DATA", data);
        }

        // Translate to Ruby Truffle nodes

        final BodyTranslator translator;

        if (parserContext == TranslatorDriver.ParserContext.MODULE) {
            translator = new ModuleTranslator(currentNode, context, null, environment, source);
        } else {
            translator = new BodyTranslator(currentNode, context, null, environment, source, parserContext == ParserContext.TOP_LEVEL);
        }

        RubyNode truffleNode;

        if (rootNode.getBodyNode() == null || rootNode.getBodyNode() instanceof org.jruby.ast.NilNode) {
            translator.parentSourceSection = sharedMethodInfo.getSourceSection();
            
            try {
                truffleNode = new ObjectLiteralNode(context, null, context.getCoreLibrary().getNilObject());
            } finally {
                translator.parentSourceSection = null;
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

        truffleNode = new CatchReturnAsErrorNode(context, truffleNode.getSourceSection(), truffleNode);

        // Catch retry

        truffleNode = new CatchRetryAsErrorNode(context, truffleNode.getSourceSection(), truffleNode);

        // Custom node wrapper

        truffleNode = wrapper.wrap(truffleNode);

        // Shell result

        return new RubyRootNode(context, truffleNode.getSourceSection(), environment.getFrameDescriptor(), sharedMethodInfo, truffleNode);
    }

    private Object getData(RubyContext context) {
        // TODO(CS) how do we know this has been populated already?

        // TODO(CS) rough translation of File object just to get up and running

        final IRubyObject jrubyData = context.getRuntime().getObject().getConstantNoConstMissing("DATA", false, false);

        if (jrubyData == null) {
            return null;
        }

        final org.jruby.RubyFile jrubyFile = (org.jruby.RubyFile) jrubyData;
        final RubyFile truffleFile = new RubyFile(context.getCoreLibrary().getFileClass(), new InputStreamReader(jrubyFile.getInStream(), StandardCharsets.UTF_8), null);

        return truffleFile;
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
            SourceSection sourceSection = new NullSourceSection("Unknown source section", "(unknown)");
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, context.getRootLexicalScope(), "(unknown)", false, null, false);
            final MaterializedFrame parent = RubyArguments.getDeclarationFrame(frame.getArguments());
            // TODO(CS): how do we know if the frame is a block or not?
            return new TranslatorEnvironment(context, environmentForFrame(context, parent), frame.getFrameDescriptor(), this, allocateReturnID(), true, true, sharedMethodInfo, sharedMethodInfo.getName(), false);
        }
    }

}
