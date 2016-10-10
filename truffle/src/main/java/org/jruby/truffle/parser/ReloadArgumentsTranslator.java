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
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.control.SequenceNode;
import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.OptArgParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RestArgParseNode;

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
    private boolean hasRestParameter = false;

    public ReloadArgumentsTranslator(Node currentNode, RubyContext context, Source source, BodyTranslator methodBodyTranslator) {
        super(currentNode, context, source);
        this.methodBodyTranslator = methodBodyTranslator;
    }

    @Override
    public RubyNode visitArgsNode(ArgsParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

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
            hasRestParameter = true;
            sequence.add(node.getRestArgNode().accept(this));
        }

        final SourceSection sourceSectionX = sourceSection.toSourceSection(source);

        if (node.getPostCount() > 0) {
            System.err.printf("WARNING: post args in zsuper not yet implemented at %s:%d%n", sourceSectionX.getSource().getName(), sourceSectionX.getStartLine());
        }

        if (node.hasKwargs() && !source.getName().endsWith("/language/fixtures/super.rb")) {
            System.err.printf("WARNING: kwargs in zsuper not yet implemented at %s:%d%n", sourceSectionX.getSource().getName(), sourceSectionX.getStartLine());
        }

        return new SequenceNode(sourceSection.toSourceSection(source), sequence.toArray(new RubyNode[sequence.size()]));
    }

    @Override
    public RubyNode visitArgumentNode(ArgumentParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    public RubyNode visitOptArgNode(OptArgParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnParseNode node) {
        return new ProfileArgumentNode(new ReadPreArgumentNode(index, MissingArgumentBehavior.NIL));
    }

    @Override
    public RubyNode visitRestArgNode(RestArgParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), source, sourceSection);
    }

    @Override
    protected RubyNode defaultVisit(ParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return nilNode(source, sourceSection);
    }

    public boolean isSplatted() {
        return hasRestParameter;
    }

}
