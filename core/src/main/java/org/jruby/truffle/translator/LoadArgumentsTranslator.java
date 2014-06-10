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
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.call.RubyCallNode;
import org.jruby.truffle.nodes.cast.ArrayCastNodeFactory;
import org.jruby.truffle.nodes.cast.BooleanCastNodeFactory;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.ArrayGetTailNodeFactory;
import org.jruby.truffle.nodes.core.ArrayIndexNodeFactory;
import org.jruby.truffle.nodes.literal.NilNode;
import org.jruby.truffle.nodes.methods.arguments.*;
import org.jruby.truffle.nodes.methods.locals.ReadLocalVariableNode;
import org.jruby.truffle.nodes.methods.locals.ReadLocalVariableNodeFactory;
import org.jruby.truffle.nodes.methods.locals.WriteLocalVariableNode;
import org.jruby.truffle.nodes.methods.locals.WriteLocalVariableNodeFactory;
import org.jruby.truffle.nodes.respondto.RespondToNode;
import org.jruby.truffle.runtime.NilPlaceholder;
import org.jruby.truffle.runtime.RubyContext;

import java.util.*;

public class LoadArgumentsTranslator extends Translator {

    private final boolean isBlock;
    private final BodyTranslator methodBodyTranslator;
    private final Deque<FrameSlot> arraySlotStack = new ArrayDeque<>();

    private enum State {
        PRE,
        OPT,
        POST
    }

    private int index;
    private State state;

    private org.jruby.ast.ArgsNode argsNode;

    public LoadArgumentsTranslator(RubyContext context, Source source, boolean isBlock, BodyTranslator methodBodyTranslator) {
        super(context, source);
        this.isBlock = isBlock;
        this.methodBodyTranslator = methodBodyTranslator;
    }

    @Override
    public RubyNode visitArgsNode(org.jruby.ast.ArgsNode node) {
        argsNode = node;

        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> sequence = new ArrayList<>();

        int localIndex = 0;

        if (node.getPre() != null) {
            state = State.PRE;
            for (org.jruby.ast.Node arg : node.getPre().childNodes()) {
                sequence.add(arg.accept(this));
                index = ++localIndex;
            }
        }

        if (node.getOptArgs() != null) {
            // (BlockNode 0, (OptArgNode:a 0, (LocalAsgnNode:a 0, (FixnumNode 0))), ...)
            state = State.OPT;
            for (org.jruby.ast.Node arg : node.getOptArgs().childNodes()) {
                sequence.add(arg.accept(this));
                index = ++localIndex;
            }
        }

        if (node.getPost() != null) {
            state = State.POST;
            for (org.jruby.ast.Node arg : node.getPost().childNodes()) {
                sequence.add(arg.accept(this));
                index = ++localIndex;
            }
        }

        if (node.getRestArgNode() != null) {
            methodBodyTranslator.getEnvironment().hasRestParameter = true;
            sequence.add(node.getRestArgNode().accept(this));
        }

        if (node.getBlock() != null) {
            sequence.add(node.getBlock().accept(this));
        }

        return SequenceNode.sequence(context, sourceSection, sequence);
    }

    @Override
    public RubyNode visitArgumentNode(org.jruby.ast.ArgumentNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode;

        if (useArray()) {
            readNode = readArgument(sourceSection, index);
        } else {
            if (state == State.PRE) {
                readNode = readArgument(sourceSection, index);
            } else if (state == State.POST) {
                readNode = readArgument(sourceSection, (argsNode.getPreCount() + argsNode.getOptionalArgsCount() + argsNode.getPostCount()) - index - 1);
            } else {
                throw new IllegalStateException();
            }
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNodeFactory.create(context, sourceSection, slot, readNode);
    }

    private RubyNode readArgument(SourceSection sourceSection, int index) {
        if (useArray()) {
            return ArrayIndexNodeFactory.create(context, sourceSection, index, loadArray(sourceSection));
        } else {
            if (state == State.PRE) {
                return new ReadPreArgumentNode(context, sourceSection, index, isBlock ? MissingArgumentBehaviour.NIL : MissingArgumentBehaviour.RUNTIME_ERROR);
            } else if (state == State.POST) {
                return new ReadPostArgumentNode(context, sourceSection, (argsNode.getPreCount() + argsNode.getOptionalArgsCount() + argsNode.getPostCount()) - index - 1);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public RubyNode visitRestArgNode(org.jruby.ast.RestArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode;

        if (useArray()) {
            readNode = ArrayGetTailNodeFactory.create(context, sourceSection, argsNode.getPreCount(), loadArray(sourceSection));
        } else {
            readNode = new ReadRestArgumentNode(context, sourceSection, argsNode.getPreCount());
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNodeFactory.create(context, sourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitBlockArgNode(org.jruby.ast.BlockArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode = new ReadBlockNode(context, sourceSection, NilPlaceholder.INSTANCE);
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNodeFactory.create(context, sourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitOptArgNode(org.jruby.ast.OptArgNode node) {
        // (OptArgNode:a 0, (LocalAsgnNode:a 0, (FixnumNode 0)))
        return node.childNodes().get(0).accept(this);
    }

    @Override
    public RubyNode visitLocalAsgnNode(org.jruby.ast.LocalAsgnNode node) {
        return translateLocalAssignment(node.getPosition(), node.getName(), node.getValueNode());
    }

    @Override
    public RubyNode visitDAsgnNode(org.jruby.ast.DAsgnNode node) {
        return translateLocalAssignment(node.getPosition(), node.getName(), node.getValueNode());
    }

    private RubyNode translateLocalAssignment(ISourcePosition sourcePosition, String name, org.jruby.ast.Node valueNode) {
        final SourceSection sourceSection = translate(sourcePosition);

        final RubyNode readNode;

        if (valueNode instanceof org.jruby.ast.NilImplicitNode) {
            // Multiple assignment

            if (useArray()) {
                readNode = ArrayIndexNodeFactory.create(context, sourceSection, index, loadArray(sourceSection));
            } else {
                readNode = readArgument(sourceSection, index);
            }
        } else {
            // Optional argument
            final RubyNode defaultValue = valueNode.accept(this);
            readNode = new ReadOptionalArgumentNode(context, sourceSection, index, index + argsNode.getPostCount() + 1, defaultValue);
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(name);
        return WriteLocalVariableNodeFactory.create(context, sourceSection, slot, readNode);

    }

    @Override
    public RubyNode visitArrayNode(org.jruby.ast.ArrayNode node) {
        // (ArrayNode 0, (MultipleAsgn19Node 0, (ArrayNode 0, (LocalAsgnNode:a 0, ), (LocalAsgnNode:b 0, )), null, null)))
        return node.childNodes().get(0).accept(this);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(org.jruby.ast.MultipleAsgn19Node node) {
        final SourceSection sourceSection = translate(node.getPosition());

        // (MultipleAsgn19Node 0, (ArrayNode 0, (LocalAsgnNode:a 0, ), (LocalAsgnNode:b 0, )), null, null))

        final int arrayIndex = index;

        final String arrayName = methodBodyTranslator.getEnvironment().allocateLocalTemp("destructure");
        final FrameSlot arraySlot = methodBodyTranslator.getEnvironment().declareVar(arrayName);

        pushArraySlot(arraySlot);

        final List<org.jruby.ast.Node> childNodes;

        if (node.childNodes() == null || node.childNodes().get(0) == null) {
            childNodes = Collections.emptyList();
        } else {
            childNodes = node.childNodes().get(0).childNodes();
        }

        final List<RubyNode> notNilSequence = new ArrayList<>();

        for (int n = 0; n < childNodes.size(); n++) {
            index = n;
            notNilSequence.add(childNodes.get(n).accept(this));
        }

        final RubyNode notNil = SequenceNode.sequence(context, sourceSection, notNilSequence);

        popArraySlot(arraySlot);

        final List<RubyNode> nilSequence = new ArrayList<>();

        if (!childNodes.isEmpty()) {
            // We haven't pushed a new array slot, so this will read the value which we couldn't convert to an array into the first destructured argument
            index = arrayIndex;
            nilSequence.add(childNodes.get(0).accept(this));
        }

        final RubyNode nil = SequenceNode.sequence(context, sourceSection, nilSequence);

        return SequenceNode.sequence(context, sourceSection,
                WriteLocalVariableNodeFactory.create(context, sourceSection, arraySlot,
                        ArrayCastNodeFactory.create(context, sourceSection,
                                readArgument(sourceSection, arrayIndex))),
                new IfNode(context, sourceSection,
                        BooleanCastNodeFactory.create(context, sourceSection,
                                new IsNilNode(context, sourceSection, ReadLocalVariableNodeFactory.create(context, sourceSection, arraySlot))),
                        nil,
                        notNil));
    }

    @Override
    protected RubyNode defaultVisit(org.jruby.ast.Node node) {
        // For normal expressions in the default value for optional arguments, use the normal body translator
        return node.accept(methodBodyTranslator);
    }

    public void pushArraySlot(FrameSlot slot) {
        arraySlotStack.push(slot);
    }

    public void popArraySlot(FrameSlot slot) {
        arraySlotStack.pop();
    }

    protected boolean useArray() {
        return !arraySlotStack.isEmpty();
    }

    protected RubyNode loadArray(SourceSection sourceSection) {
        return ReadLocalVariableNodeFactory.create(context, sourceSection, arraySlotStack.peek());
    }

}
