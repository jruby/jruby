/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.ArgumentNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.arguments.WritePreArgumentNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.locals.ReadLocalVariableNode;
import org.jruby.truffle.runtime.RubyContext;

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

    private int index;

    public ReloadArgumentsTranslator(Node currentNode, RubyContext context, Source source, BodyTranslator methodBodyTranslator) {
        super(currentNode, context, source);
        this.methodBodyTranslator = methodBodyTranslator;
    }

    @Override
    public RubyNode visitArgsNode(org.jruby.ast.ArgsNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> sequence = new ArrayList<>();

        if (node.getPre() != null) {
            for (org.jruby.ast.Node arg : node.getPre().childNodes()) {
                sequence.add(arg.accept(this));
                index++;
            }
        }

        return SequenceNode.sequence(context, sourceSection, sequence);
    }

    @Override
    public RubyNode visitArgumentNode(ArgumentNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        final RubyNode read = new ReadLocalVariableNode(context, sourceSection, slot);
        return new WritePreArgumentNode(context, sourceSection, index, read);
    }

    @Override
    protected RubyNode defaultVisit(org.jruby.ast.Node node) {
        final SourceSection sourceSection = translate(node.getPosition());
        return new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject());
    }

    @Override
    protected String getIdentifier() {
        return methodBodyTranslator.getIdentifier();
    }

}
