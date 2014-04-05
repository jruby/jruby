/*
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.Source;
import com.oracle.truffle.api.SourceSection;
import com.oracle.truffle.api.frame.FrameSlot;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.ArrayIndexNodeFactory;
import org.jruby.truffle.nodes.methods.arguments.MissingArgumentBehaviour;
import org.jruby.truffle.nodes.methods.arguments.ReadPreArgumentNode;
import org.jruby.truffle.nodes.methods.arguments.ReadRestArgumentNode;
import org.jruby.truffle.nodes.methods.locals.ReadLocalVariableNodeFactory;
import org.jruby.truffle.nodes.methods.locals.WriteLocalVariableNodeFactory;
import org.jruby.truffle.runtime.RubyContext;

import java.util.ArrayList;
import java.util.List;

public class LoadArgumentsTranslator extends Translator {

    private final TranslatorEnvironment environment;
    private final List<FrameSlot> arraySlotStack = new ArrayList<>();

    public LoadArgumentsTranslator(RubyContext context, Source source, TranslatorEnvironment environment) {
        super(context, source);
        this.environment = environment;
    }

    @Override
    public RubyNode visitArgsNode(org.jruby.ast.ArgsNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> sequence = new ArrayList<>();

        if (node.getPre() != null) {
            for (org.jruby.ast.Node arg : node.getPre().childNodes()) {
                sequence.add(arg.accept(this));
            }
        }

        if (node.getRestArgNode() != null) {
            environment.hasRestParameter = true;
            sequence.add(node.getRestArgNode().accept(this));
        }

        return SequenceNode.sequence(context, sourceSection, sequence);
    }

    @Override
    public RubyNode visitArgumentNode(org.jruby.ast.ArgumentNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode;

        if (useArray()) {
            readNode = ArrayIndexNodeFactory.create(context, sourceSection, node.getIndex(), loadArray(sourceSection));
        } else {
            readNode = new ReadPreArgumentNode(context, sourceSection, node.getIndex(), MissingArgumentBehaviour.RUNTIME_ERROR);
        }

        final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNodeFactory.create(context, sourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitRestArgNode(org.jruby.ast.RestArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode = new ReadRestArgumentNode(context, sourceSection, node.getIndex());
        final FrameSlot slot = environment.getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNodeFactory.create(context, sourceSection, slot, readNode);
    }

    @Override
    protected RubyNode defaultVisit(org.jruby.ast.Node node) {
        throw new UnsupportedOperationException(node.toString());
    }

    public void pushArraySlot(FrameSlot slot) {
        arraySlotStack.add(slot);
    }

    protected boolean useArray() {
        return !arraySlotStack.isEmpty();
    }

    protected RubyNode loadArray(SourceSection sourceSection) {
        return ReadLocalVariableNodeFactory.create(context, sourceSection, arraySlotStack.get(0));
    }

}
