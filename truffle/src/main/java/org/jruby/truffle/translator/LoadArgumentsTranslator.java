/*
 * Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
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
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.types.INameNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.arguments.*;
import org.jruby.truffle.nodes.cast.ArrayCastNodeGen;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.SequenceNode;
import org.jruby.truffle.nodes.core.array.ArrayLiteralNode;
import org.jruby.truffle.nodes.core.array.ArraySliceNodeGen;
import org.jruby.truffle.nodes.core.array.PrimitiveArrayNodeFactory;
import org.jruby.truffle.nodes.defined.DefinedWrapperNode;
import org.jruby.truffle.nodes.literal.ObjectLiteralNode;
import org.jruby.truffle.nodes.locals.ReadLocalVariableNode;
import org.jruby.truffle.nodes.locals.WriteLocalVariableNode;
import org.jruby.truffle.runtime.RubyContext;

import java.util.*;

public class LoadArgumentsTranslator extends Translator {

    private static class ArraySlot {

        private FrameSlot arraySlot;
        private int previousIndex;

        public ArraySlot(FrameSlot arraySlot, int previousIndex) {
            this.arraySlot = arraySlot;
            this.previousIndex = previousIndex;
        }

        public FrameSlot getArraySlot() {
            return arraySlot;
        }

        public int getPreviousIndex() {
            return previousIndex;
        }
    }

    private final boolean isBlock;
    private final BodyTranslator methodBodyTranslator;
    private final Deque<ArraySlot> arraySlotStack = new ArrayDeque<>();

    private enum State {
        PRE,
        OPT,
        POST
    }

    private int required;
    private int index;
    private int kwIndex;
    private int countKwArgs;
    private int indexFromEnd = 1;
    private State state;
    private boolean hasKeywordArguments;
    private List<String> excludedKeywords = new ArrayList<>();

    private org.jruby.ast.ArgsNode argsNode;

    public LoadArgumentsTranslator(Node currentNode, RubyContext context, Source source, boolean isBlock, BodyTranslator methodBodyTranslator) {
        super(currentNode, context, source);
        this.isBlock = isBlock;
        this.methodBodyTranslator = methodBodyTranslator;
    }

    @Override
    public RubyNode visitArgsNode(org.jruby.ast.ArgsNode node) {
        argsNode = node;

        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> sequence = new ArrayList<>();

        if (node.getPre() != null) {
            state = State.PRE;
            index = 0;
            for (org.jruby.ast.Node arg : node.getPre().childNodes()) {
                sequence.add(arg.accept(this));
                ++index;
                required++;
            }
        }

        if (node.getOptArgs() != null) {
            // (BlockNode 0, (OptArgNode:a 0, (LocalAsgnNode:a 0, (FixnumNode 0))), ...)
            state = State.OPT;
            index = argsNode.getPreCount();
            for (org.jruby.ast.Node arg : node.getOptArgs().childNodes()) {
                sequence.add(arg.accept(this));
                ++index;
            }
        }

        hasKeywordArguments = node.hasKwargs() && node.getKeywords() != null;

        if (node.getRestArgNode() != null) {
            methodBodyTranslator.getEnvironment().hasRestParameter = true;
            sequence.add(node.getRestArgNode().accept(this));
        }

        if (node.getPost() != null) {
            state = State.POST;
            index = -1;
            final List<org.jruby.ast.Node> children = new ArrayList<>(node.getPost().childNodes());
            Collections.reverse(children);
            for (org.jruby.ast.Node arg : children) {
                sequence.add(arg.accept(this));
                index--;
                required++;
            }
        }

        if (hasKeywordArguments) {
            kwIndex = 0;
            countKwArgs = 0;
            
            for (org.jruby.ast.Node arg : node.getKeywords().childNodes()) {
                sequence.add(arg.accept(this));
                kwIndex++;
                countKwArgs++;
            }
        }

        if (node.getKeyRest() != null) {
            sequence.add(node.getKeyRest().accept(this));
        }

        if (node.getBlock() != null) {
            sequence.add(node.getBlock().accept(this));
        }

        return SequenceNode.sequence(context, sourceSection, sequence);
    }

    @Override
    public RubyNode visitKeywordRestArgNode(org.jruby.ast.KeywordRestArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode = new ReadKeywordRestArgumentNode(context, sourceSection, required, excludedKeywords.toArray(new String[excludedKeywords.size()]), -countKwArgs - 1);
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(node.getName());

        return new WriteLocalVariableNode(context, sourceSection, readNode, slot);
    }

    @Override
    public RubyNode visitKeywordArgNode(org.jruby.ast.KeywordArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name;
        final RubyNode defaultValue;

        final org.jruby.ast.Node firstChild = node.childNodes().get(0);

        if (firstChild instanceof org.jruby.ast.LocalAsgnNode) {
            final org.jruby.ast.LocalAsgnNode localAsgnNode = (org.jruby.ast.LocalAsgnNode) firstChild;
            name = localAsgnNode.getName();

            if (localAsgnNode.getValueNode() == null) {
                defaultValue = new DefinedWrapperNode(context, sourceSection,
                        new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                        "nil");
            } else if (localAsgnNode.getValueNode() instanceof RequiredKeywordArgumentValueNode) {
                /*
                 * This isn't a true default value - it's a marker to say there isn't one. This actually makes sense;
                 * the semantic action of executing this node is to report an error, and we do the same thing.
                 */
                defaultValue = new MissingKeywordArgumentNode(context, sourceSection, name);
            } else {
                defaultValue = localAsgnNode.getValueNode().accept(this);
            }
        } else if (firstChild instanceof org.jruby.ast.DAsgnNode) {
            final org.jruby.ast.DAsgnNode dAsgnNode = (org.jruby.ast.DAsgnNode) firstChild;
            name = dAsgnNode.getName();

            if (dAsgnNode.getValueNode() == null) {
                defaultValue = new DefinedWrapperNode(context, sourceSection,
                        new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                        "nil");
            } else if (dAsgnNode.getValueNode() instanceof RequiredKeywordArgumentValueNode) {
                /*
                 * This isn't a true default value - it's a marker to say there isn't one. This actually makes sense;
                 * the semantic action of executing this node is to report an error, and we do the same thing.
                 */
                defaultValue = new MissingKeywordArgumentNode(context, sourceSection, name);
            }else {
                defaultValue = dAsgnNode.getValueNode().accept(this);
            }
        } else {
            throw new UnsupportedOperationException("unsupported keyword arg " + node);
        }

        excludedKeywords.add(name);

        final RubyNode readNode = new ReadKeywordArgumentNode(context, sourceSection, required, name, defaultValue, kwIndex - countKwArgs);
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(name);

        return new WriteLocalVariableNode(context, sourceSection, readNode, slot);
    }

    @Override
    public RubyNode visitArgumentNode(org.jruby.ast.ArgumentNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode = readArgument(sourceSection);
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return new WriteLocalVariableNode(context, sourceSection, readNode, slot);
    }

    private RubyNode readArgument(SourceSection sourceSection) {
        if (useArray()) {
            return PrimitiveArrayNodeFactory.read(context, sourceSection, loadArray(sourceSection), index);
        } else {
            if (state == State.PRE) {
                return new ReadPreArgumentNode(context, sourceSection, index, isBlock ? MissingArgumentBehaviour.NIL : MissingArgumentBehaviour.RUNTIME_ERROR);
            } else if (state == State.POST) {
                return new ReadPostArgumentNode(context, sourceSection, index);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public RubyNode visitRestArgNode(org.jruby.ast.RestArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode;

        if (argsNode == null) {
            throw new IllegalStateException("No arguments node visited");
        }

        int from = argsNode.getPreCount() + argsNode.getOptionalArgsCount();
        int to = -argsNode.getPostCount();
        if (useArray()) {
            readNode = ArraySliceNodeGen.create(context, sourceSection, from, to, loadArray(sourceSection));
        } else {
            readNode = new ReadRestArgumentNode(context, sourceSection, from, to, hasKeywordArguments);
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return new WriteLocalVariableNode(context, sourceSection, readNode, slot);
    }

    @Override
    public RubyNode visitBlockArgNode(org.jruby.ast.BlockArgNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode readNode = new ReadBlockNode(context, sourceSection, context.getCoreLibrary().getNilObject());
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return new WriteLocalVariableNode(context, sourceSection, readNode, slot);
    }

    @Override
    public RubyNode visitOptArgNode(org.jruby.ast.OptArgNode node) {
        // (OptArgNode:a 0, (LocalAsgnNode:a 0, (FixnumNode 0)))
        return node.getValue().accept(this);
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

        if (indexFromEnd == 1) {
            if (valueNode instanceof org.jruby.ast.NilImplicitNode) {
                // Multiple assignment

                if (useArray()) {
                    readNode = PrimitiveArrayNodeFactory.read(context, sourceSection, loadArray(sourceSection), index);
                } else {
                    readNode = readArgument(sourceSection);
                }
            } else {
                // Optional argument
                final RubyNode defaultValue = valueNode.accept(this);

                if (argsNode == null) {
                    throw new IllegalStateException("No arguments node visited");
                }

                int minimum = index + 1 + argsNode.getPostCount();

                if (argsNode.hasKwargs()) {
                    minimum += 1;
                }

                readNode = new ReadOptionalArgumentNode(context, sourceSection, index, minimum, defaultValue);
            }
        } else {
            readNode = ArraySliceNodeGen.create(context, sourceSection, index, indexFromEnd, loadArray(sourceSection));
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(name);
        return new WriteLocalVariableNode(context, sourceSection, readNode, slot);
    }

    @Override
    public RubyNode visitArrayNode(org.jruby.ast.ArrayNode node) {
        // (ArrayNode 0, (MultipleAsgn19Node 0, (ArrayNode 0, (LocalAsgnNode:a 0, ), (LocalAsgnNode:b 0, )), null, null)))
        if (node.size() == 1 && node.get(0) instanceof MultipleAsgnNode) {
            return node.childNodes().get(0).accept(this);
        } else {
            return defaultVisit(node);
        }
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnNode node) {
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

        if (node.getPre() != null) {
            index = 0;
            for (org.jruby.ast.Node child : node.getPre().childNodes()) {
                notNilSequence.add(child.accept(this));
                index++;
            }
        }

        if (node.getRest() != null) {
            index = node.getPreCount();
            indexFromEnd = -node.getPostCount();
            notNilSequence.add(node.getRest().accept(this));
            indexFromEnd = 1;
        }

        if (node.getPost() != null) {
            index = -1;
            final List<org.jruby.ast.Node> children = new ArrayList<>(node.getPost().childNodes());
            Collections.reverse(children);
            for (org.jruby.ast.Node child : children) {
                notNilSequence.add(child.accept(this));
                index--;
            }
        }

        final RubyNode notNil = SequenceNode.sequence(context, sourceSection, notNilSequence);

        popArraySlot(arraySlot);

        final List<RubyNode> nilSequence = new ArrayList<>();

        final ParameterCollector parametersToClearCollector = new ParameterCollector();

        if (node.getPre() != null) {
            for (org.jruby.ast.Node child : node.getPre().childNodes()) {
                child.accept(parametersToClearCollector);
            }
        }

        if (node.getRest() != null) {
            if (node.getRest() instanceof INameNode) {
                final String name = ((INameNode) node.getRest()).getName();
                nilSequence.add(((ReadNode) methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(name, sourceSection)).makeWriteNode(new ArrayLiteralNode.UninitialisedArrayLiteralNode(context, sourceSection, new RubyNode[]{})));
            } else if (node.getRest() instanceof StarNode) {
                // Don't think we need to do anything
            } else {
                throw new UnsupportedOperationException("unsupported rest node " + node.getRest());
            }
        }

        if (node.getPost() != null) {
            for (org.jruby.ast.Node child : node.getPost().childNodes()) {
                child.accept(parametersToClearCollector);
            }
        }

        for (String parameterToClear : parametersToClearCollector.getParameters()) {
            nilSequence.add(((ReadNode) methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(parameterToClear, sourceSection)).makeWriteNode(new DefinedWrapperNode(context, sourceSection,
                    new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                    "nil")));
        }

        if (!childNodes.isEmpty()) {
            // We haven't pushed a new array slot, so this will read the value which we couldn't convert to an array into the first destructured argument
            index = arrayIndex;
            nilSequence.add(childNodes.get(0).accept(this));
        }

        final RubyNode nil = SequenceNode.sequence(context, sourceSection, nilSequence);

        return SequenceNode.sequence(context, sourceSection,
                new WriteLocalVariableNode(context, sourceSection,
                        ArrayCastNodeGen.create(context, sourceSection,
                                readArgument(sourceSection)), arraySlot),
                new IfNode(context, sourceSection,
                        new IsNilNode(context, sourceSection, new ReadLocalVariableNode(context, sourceSection, arraySlot)),
                        nil,
                        notNil == null ? new DefinedWrapperNode(context, sourceSection,
                                new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getNilObject()),
                                "nil") : notNil));
    }

    @Override
    protected RubyNode defaultVisit(org.jruby.ast.Node node) {
        // For normal expressions in the default value for optional arguments, use the normal body translator
        return node.accept(methodBodyTranslator);
    }

    public void pushArraySlot(FrameSlot slot) {
        arraySlotStack.push(new ArraySlot(slot, index));
    }

    public void popArraySlot(FrameSlot slot) {
        index = arraySlotStack.pop().getPreviousIndex();
    }

    protected boolean useArray() {
        return !arraySlotStack.isEmpty();
    }

    protected RubyNode loadArray(SourceSection sourceSection) {
        return new ReadLocalVariableNode(context, sourceSection, arraySlotStack.peek().getArraySlot());
    }

    @Override
    protected String getIdentifier() {
        return methodBodyTranslator.getIdentifier();
    }

}
