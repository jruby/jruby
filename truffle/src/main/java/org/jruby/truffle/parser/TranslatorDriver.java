/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
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
import org.jruby.truffle.language.DataNode;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.SourceIndexLength;
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
import org.jruby.truffle.parser.ast.RootParseNode;
import org.jruby.truffle.parser.lexer.LexerSource;
import org.jruby.truffle.parser.lexer.SyntaxException;
import org.jruby.truffle.parser.parser.ParserConfiguration;
import org.jruby.truffle.parser.parser.RubyParser;
import org.jruby.truffle.parser.parser.RubyParserResult;
import org.jruby.truffle.parser.scope.DynamicScope;
import org.jruby.truffle.parser.scope.StaticScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TranslatorDriver {

    private final RubyContext context;
    private final ParseEnvironment parseEnvironment;

    public TranslatorDriver(RubyContext context) {
        this.context = context;
        parseEnvironment = new ParseEnvironment(context);
    }

    public RubyRootNode parse(RubyContext context, Source source, Encoding defaultEncoding, ParserContext parserContext, String[] argumentNames, FrameDescriptor frameDescriptor, MaterializedFrame parentFrame, boolean ownScopeForAssignments, Node currentNode) {
        final StaticScope staticScope = new StaticScope(StaticScope.Type.LOCAL, null);

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

        final DynamicScope dynamicScope = new DynamicScope(staticScope);

        boolean isInlineSource = parserContext == ParserContext.SHELL;
        boolean isEvalParse = parserContext == ParserContext.EVAL || parserContext == ParserContext.INLINE || parserContext == ParserContext.MODULE;
        final ParserConfiguration parserConfiguration = new ParserConfiguration(context, 0, isInlineSource, !isEvalParse, false);

        if (context.getOptions().FROZEN_STRING_LITERALS) {
            parserConfiguration.setFrozenStringLiteral(true);
        }

        parserConfiguration.setDefaultEncoding(defaultEncoding);

        // Parse to the JRuby AST

        RootParseNode node = parse(source, dynamicScope, parserConfiguration);

        final SourceSection sourceSection = source.createSection(0, source.getCode().length());
        final SourceIndexLength sourceIndexLength = new SourceIndexLength(sourceSection.getCharIndex(), sourceSection.getCharLength());

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

        final boolean topLevel = parserContext == ParserContext.TOP_LEVEL_FIRST || parserContext == ParserContext.TOP_LEVEL;
        final boolean isModuleBody = topLevel;
        final TranslatorEnvironment environment = new TranslatorEnvironment(context, parentEnvironment,
                        parseEnvironment, parseEnvironment.allocateReturnID(), ownScopeForAssignments, false, isModuleBody, sharedMethodInfo, sharedMethodInfo.getName(), 0, null);

        // Declare arguments as local variables in the top-level environment - we'll put the values there in a prelude

        if (argumentNames != null) {
            for (String name : argumentNames) {
                environment.declareVar(name);
            }
        }

        // Translate to Ruby Truffle nodes

        context.getCoverageManager().loadingSource(source);

        final BodyTranslator translator = new BodyTranslator(currentNode, context, null, environment, source, parserContext, topLevel);

        RubyNode truffleNode = translator.translateNodeOrNil(sourceIndexLength, node.getBodyNode());

        // Load arguments

        final RubyNode writeSelfNode = Translator.loadSelf(context, environment);
        truffleNode = Translator.sequence(sourceIndexLength, Arrays.asList(writeSelfNode, truffleNode));

        if (argumentNames != null && argumentNames.length > 0) {
            final List<RubyNode> sequence = new ArrayList<>();

            for (int n = 0; n < argumentNames.length; n++) {
                final String name = argumentNames[n];
                final RubyNode readNode = new ProfileArgumentNode(new ReadPreArgumentNode(n, MissingArgumentBehavior.NIL));
                final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(name);
                sequence.add(WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode));
            }

            sequence.add(truffleNode);
            truffleNode = Translator.sequence(sourceIndexLength, sequence);
        }

        // Load flip-flop states

        if (environment.getFlipFlopStates().size() > 0) {
            truffleNode = Translator.sequence(sourceIndexLength, Arrays.asList(translator.initFlipFlopStates(sourceIndexLength), truffleNode));
        }

        // Catch next

        truffleNode = new CatchNextNode(truffleNode);

        // Catch return

        if (parserContext != ParserContext.INLINE) {
            truffleNode = new CatchReturnAsErrorNode(truffleNode);
        }

        // Catch retry

        truffleNode = new CatchRetryAsErrorNode(truffleNode);

        if (parserContext == ParserContext.TOP_LEVEL_FIRST) {
            truffleNode = Translator.sequence(sourceIndexLength, Arrays.asList(
                    new LoadRequiredLibrariesNode(),
                    truffleNode));

            if (node.hasEndPosition()) {
                truffleNode = Translator.sequence(sourceIndexLength, Arrays.asList(
                        new DataNode(node.getEndPosition()),
                        truffleNode));
            }

            truffleNode = new ExceptionTranslatingNode(truffleNode, UnsupportedOperationBehavior.TYPE_ERROR);
            truffleNode = new TopLevelRaiseHandler(truffleNode);
        }

        return new RubyRootNode(context, sourceIndexLength.toSourceSection(source), environment.getFrameDescriptor(), sharedMethodInfo, truffleNode, environment.needsDeclarationFrame());
    }

    public RootParseNode parse(Source source, DynamicScope blockScope,
                           ParserConfiguration configuration) {
        LexerSource ByteListLexerSource = new LexerSource(source, configuration.getLineNumber(), configuration.getDefaultEncoding());
        // We only need to pass in current scope if we are evaluating as a block (which
        // is only done for evals).  We need to pass this in so that we can appropriately scope
        // down to captured scopes when we are parsing.
        if (blockScope != null) {
            configuration.parseAsBlock(blockScope);
        }

        RubyParser parser = new RubyParser(context, ByteListLexerSource, new RubyWarnings(configuration.getContext()));
        RubyParserResult result;
        try {
            result = parser.parse(configuration);
        } catch (IOException e) {
            // Enebo: We may want to change this error to be more specific,
            // but I am not sure which conditions leads to this...so lame message.
            throw new RaiseException(context.getCoreExceptions().syntaxError("Problem reading source: " + e, null));
        } catch (SyntaxException e) {
            switch (e.getPid()) {
                case UNKNOWN_ENCODING:
                case NOT_ASCII_COMPATIBLE:
                    throw new RaiseException(context.getCoreExceptions().argumentError(e.getMessage(), null));
                default:
                    StringBuilder buffer = new StringBuilder(100);
                    buffer.append(e.getFile()).append(':');
                    buffer.append(e.getLine() + 1).append(": ");
                    buffer.append(e.getMessage());

                    throw new RaiseException(context.getCoreExceptions().syntaxError(buffer.toString(), null));
            }
        }

        // If variables were added then we may need to grow the dynamic scope to match the static
        // one.
        // FIXME: Make this so we only need to check this for blockScope != null.  We cannot
        // currently since we create the DynamicScope for a LocalStaticScope before parse begins.
        // Refactoring should make this fixable.
        if (result.getScope() != null) {
            result.getScope().growIfNeeded();
        }

        return (RootParseNode) result.getAST();
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
                        parseEnvironment.allocateReturnID(), true, true, false, sharedMethodInfo, sharedMethodInfo.getName(), 0, null, frameDescriptor);
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
                            parseEnvironment.allocateReturnID(), true, true, false, sharedMethodInfo, sharedMethodInfo.getName(), 0, null, frame.getFrameDescriptor());
        }
    }

}
