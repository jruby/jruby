/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.Log;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.hash.ConcatHashLiteralNode;
import org.jruby.truffle.core.hash.HashLiteralNode;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SourceIndexLength;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.language.literal.ObjectLiteralNode;
import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.AssignableParseNode;
import org.jruby.truffle.parser.ast.KeywordArgParseNode;
import org.jruby.truffle.parser.ast.KeywordRestArgParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.OptArgParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RestArgParseNode;
import org.jruby.truffle.parser.ast.types.INameNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces code to reload arguments from local variables back into the
 * arguments array. Only works for simple cases. Used for zsuper calls which
 * pass the same arguments, but will pick up modifications made to them in the
 * method so far.
 */
public class ReloadArgumentsTranslator extends Translator {

    private final BodyTranslator methodBodyTranslator;

    private int index = 0;
    private int restParameterIndex = -1;

    public ReloadArgumentsTranslator(Node currentNode, RubyContext context, Source source, ParserContext parserContext, BodyTranslator methodBodyTranslator) {
        super(currentNode, context, source, parserContext);
        this.methodBodyTranslator = methodBodyTranslator;
    }

    public int getRestParameterIndex() {
        return restParameterIndex;
    }

    @Override
    public RubyNode visitArgsNode(ArgsParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();

        final List<RubyNode> sequence = new ArrayList<>();
        final ParseNode[] args = node.getArgs();
        final int preCount = node.getPreCount();

        if (preCount > 0) {
            for (int i = 0; i < preCount; i++) {
                sequence.add(args[i].accept(this));
                index++;
            }
        }

        final int optArgsCount = node.getOptionalArgsCount();
        if (optArgsCount > 0) {
            final int optArgsIndex = node.getOptArgIndex();
            for (int i = 0; i < optArgsCount; i++) {
                sequence.add(args[optArgsIndex + i].accept(this));
                index++;
            }
        }

        if (node.hasRestArg()) {
            restParameterIndex = node.getPostIndex();
            sequence.add(node.getRestArgNode().accept(this));
        }

        final SourceSection sourceSectionX = sourceSection.toSourceSection(source);

        if (node.getPostCount() > 0) {
            Log.LOGGER.warning(String.format("post args in zsuper not yet implemented at %s:%d%n", sourceSectionX.getSource().getName(), sourceSectionX.getStartLine()));
        }

        RubyNode kwArgsNode = null;

        if (node.hasKwargs()) {
            final int keywordIndex = node.getKeywordsIndex();
            final int keywordCount = node.getKeywordCount();
            RubyNode[] keyValues = new RubyNode[keywordCount * 2];

            for (int i = 0; i < keywordCount; i++) {
                final KeywordArgParseNode kwArg = (KeywordArgParseNode) args[keywordIndex + i];
                final RubyNode value = kwArg.accept(this);
                final String name = ((INameNode) kwArg.getAssignable()).getName();
                RubyNode key = new ObjectLiteralNode(context.getSymbolTable().getSymbol(name));
                keyValues[2 * i] = key;
                keyValues[2 * i + 1] = value;
            }
            kwArgsNode = HashLiteralNode.create(context, keyValues);
        }

        if (node.hasKeyRest()) {
            final RubyNode keyRest = node.getKeyRest().accept(this);
            if (kwArgsNode == null) {
                kwArgsNode = keyRest;
            } else {
                kwArgsNode = new ConcatHashLiteralNode(new RubyNode[]{kwArgsNode, keyRest});
            }

        }

        if (kwArgsNode != null) {
            sequence.add(kwArgsNode);
        }

        return new SequenceNode(sequence.toArray(new RubyNode[sequence.size()]));
    }

    @Override
    public RubyNode visitArgumentNode(ArgumentParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    public RubyNode visitOptArgNode(OptArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnParseNode node) {
        return new ProfileArgumentNode(new ReadPreArgumentNode(index, MissingArgumentBehavior.NIL));
    }

    @Override
    public RubyNode visitRestArgNode(RestArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    public RubyNode visitKeywordArgNode(KeywordArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        final AssignableParseNode asgnNode = node.getAssignable();
        final String name = ((INameNode) asgnNode).getName();
        return methodBodyTranslator.getEnvironment().findLocalVarNode(name, source, sourceSection);
    }

    @Override
    public RubyNode visitKeywordRestArgNode(KeywordRestArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    protected RubyNode defaultVisit(ParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        return nilNode(source, sourceSection);
    }

}
