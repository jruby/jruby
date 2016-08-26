/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.Ruby;
import org.jruby.ast.RestArgNode;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.control.SequenceNode;

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
    public RubyNode visitArgsNode(org.jruby.ast.ArgsNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> sequence = new ArrayList<>();
        final org.jruby.ast.Node[] args = node.getArgs();
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

        if (node.getPostCount() > 0) {
            System.err.println("WARNING: post args in zsuper not yet implemented at " + sourceSection.toSourceSection().getShortDescription());
        }

        if (node.hasKwargs() && !sourceSection.getSource().getName().endsWith("/language/fixtures/super.rb")) {
            System.err.println("WARNING: kwargs in zsuper not yet implemented at " + sourceSection.toSourceSection().getShortDescription());
        }

        return new SequenceNode(context, sourceSection.toSourceSection(), sequence.toArray(new RubyNode[sequence.size()]));
    }

    @Override
    public RubyNode visitArgumentNode(org.jruby.ast.ArgumentNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), sourceSection);
    }

    @Override
    public RubyNode visitOptArgNode(org.jruby.ast.OptArgNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), sourceSection);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(org.jruby.ast.MultipleAsgnNode node) {
        return new ProfileArgumentNode(new ReadPreArgumentNode(index, MissingArgumentBehavior.NIL));
    }

    @Override
    public RubyNode visitRestArgNode(RestArgNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return methodBodyTranslator.getEnvironment().findLocalVarNode(node.getName(), sourceSection);
    }

    @Override
    protected RubyNode defaultVisit(org.jruby.ast.Node node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        return nilNode(sourceSection);
    }

    public boolean isSplatted() {
        return hasRestParameter;
    }

}
