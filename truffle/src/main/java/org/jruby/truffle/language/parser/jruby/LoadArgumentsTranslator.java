/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.types.INameNode;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.IsNilNode;
import org.jruby.truffle.core.array.ArrayLiteralNode;
import org.jruby.truffle.core.array.ArraySliceNodeGen;
import org.jruby.truffle.core.array.PrimitiveArrayNodeFactory;
import org.jruby.truffle.core.cast.SplatCastNode;
import org.jruby.truffle.core.cast.SplatCastNodeGen;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.ArrayIsAtLeastAsLargeAsNode;
import org.jruby.truffle.language.arguments.MissingArgumentBehavior;
import org.jruby.truffle.language.arguments.MissingKeywordArgumentNode;
import org.jruby.truffle.language.arguments.ProfileArgumentNode;
import org.jruby.truffle.language.arguments.ReadBlockNode;
import org.jruby.truffle.language.arguments.ReadKeywordArgumentNode;
import org.jruby.truffle.language.arguments.ReadKeywordRestArgumentNode;
import org.jruby.truffle.language.arguments.ReadOptionalArgumentNode;
import org.jruby.truffle.language.arguments.ReadPostArgumentNode;
import org.jruby.truffle.language.arguments.ReadPreArgumentNode;
import org.jruby.truffle.language.arguments.ReadRestArgumentNode;
import org.jruby.truffle.language.arguments.ReadSelfNode;
import org.jruby.truffle.language.arguments.RunBlockKWArgsHelperNode;
import org.jruby.truffle.language.control.IfElseNode;
import org.jruby.truffle.language.control.IfNode;
import org.jruby.truffle.language.literal.NilLiteralNode;
import org.jruby.truffle.language.locals.LocalVariableType;
import org.jruby.truffle.language.locals.ReadLocalVariableNode;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.language.objects.SelfNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

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

    private final boolean isProc;
    private final BodyTranslator methodBodyTranslator;
    private final Deque<ArraySlot> arraySlotStack = new ArrayDeque<>();

    private enum State {
        PRE,
        OPT,
        POST
    }

    private int required;
    private int index;
    private int indexFromEnd = 1;
    private State state;
    private boolean hasKeywordArguments;
    private List<String> excludedKeywords = new ArrayList<>();
    private boolean firstOpt = false;

    private org.jruby.ast.ArgsNode argsNode;

    public LoadArgumentsTranslator(Node currentNode, RubyContext context, Source source, boolean isProc, BodyTranslator methodBodyTranslator) {
        super(currentNode, context, source);
        this.isProc = isProc;
        this.methodBodyTranslator = methodBodyTranslator;
    }

    @Override
    public RubyNode visitArgsNode(org.jruby.ast.ArgsNode node) {
        argsNode = node;

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final List<RubyNode> sequence = new ArrayList<>();

        //if (!arraySlotStack.isEmpty()) {
            sequence.add(loadSelf());
        //}

        final org.jruby.ast.Node[] args = node.getArgs();

        final boolean useHelper = useArray() && node.hasKeyRest();

        if (useHelper) {
            sequence.add(node.getKeyRest().accept(this));

            final Object keyRestNameOrNil;

            if (node.hasKeyRest()) {
                final String name = node.getKeyRest().getName();
                methodBodyTranslator.getEnvironment().declareVar(name);
                keyRestNameOrNil = context.getSymbolTable().getSymbol(name);
            } else {
                keyRestNameOrNil = context.getCoreLibrary().getNilObject();
            }

            sequence.add(new IfNode(context, fullSourceSection,
                    new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), loadArray(sourceSection)),
                    new RunBlockKWArgsHelperNode(arraySlotStack.peek().getArraySlot(), keyRestNameOrNil)));
        }

        final int preCount = node.getPreCount();

        if (preCount > 0) {
            state = State.PRE;
            index = 0;
            for (int i = 0; i < preCount; i++) {
                sequence.add(args[i].accept(this));
                index++;
                required++;
            }
        }

        hasKeywordArguments = node.hasKwargs();

        final int optArgCount = node.getOptionalArgsCount();
        if (optArgCount > 0) {
            // (BlockNode 0, (OptArgNode:a 0, (LocalAsgnNode:a 0, (FixnumNode 0))), ...)
            state = State.OPT;
            index = argsNode.getPreCount();
            final int optArgIndex = node.getOptArgIndex();
            for (int i = 0; i < optArgCount; i++) {
                firstOpt = i == 0;
                sequence.add(args[optArgIndex + i].accept(this));
                ++index;
            }
        }

        if (node.getRestArgNode() != null) {
            methodBodyTranslator.getEnvironment().hasRestParameter = true;
            sequence.add(node.getRestArgNode().accept(this));
        }

        int postCount = node.getPostCount();

        // The load to use when the array is not nil and the length is smaller than the number of required arguments

        final List<RubyNode> notNilSmallerSequence = new ArrayList<>();

        if (postCount > 0) {
            state = State.POST;
            org.jruby.ast.Node[] children = node.getPost().children();
            index = node.getPreCount();
            for (int i = 0; i < children.length; i++) {
                notNilSmallerSequence.add(children[i].accept(this));
                index++;
            }
        }

        final RubyNode notNilSmaller = sequence(context, sourceSection, notNilSmallerSequence);

        // The load to use when the there is no rest

        final List<RubyNode> noRestSequence = new ArrayList<>();

        if (postCount > 0) {
            state = State.POST;
            org.jruby.ast.Node[] children = node.getPost().children();
            index = node.getPreCount() + node.getOptionalArgsCount();
            for (int i = 0; i < children.length; i++) {
                noRestSequence.add(children[i].accept(this));
                index++;
            }
        }

        final RubyNode noRest = sequence(context, sourceSection, noRestSequence);

        // The load to use when the array is not nil and at least as large as the number of required arguments

        final List<RubyNode> notNilAtLeastAsLargeSequence = new ArrayList<>();

        if (postCount > 0) {
            state = State.POST;
            index = -1;

            if (!useArray() && hasKeywordArguments) {
                index--;
            }

            int postIndex = node.getPostIndex();
            for (int i = postCount - 1; i >= 0; i--) {
                notNilAtLeastAsLargeSequence.add(args[postIndex + i].accept(this));
                required++;
                index--;
            }
        }

        final RubyNode notNilAtLeastAsLarge = sequence(context, sourceSection, notNilAtLeastAsLargeSequence);

        if (useArray()) {
            if (node.getPreCount() == 0 || node.hasRestArg()) {
                sequence.add(new IfElseNode(context, fullSourceSection,
                        new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), loadArray(sourceSection)),
                        notNilAtLeastAsLarge,
                        notNilSmaller));
            } else {
                sequence.add(noRest);
            }
        } else {
            // TODO CS 10-Jan-16 needn't have created notNilSmaller
            sequence.add(notNilAtLeastAsLarge);
        }

        if (hasKeywordArguments) {
            final int keywordIndex = node.getKeywordsIndex();
            final int keywordCount = node.getKeywordCount();

            for (int i = 0; i < keywordCount; i++) {
                sequence.add(args[keywordIndex + i].accept(this));
            }
        }

        if (node.getKeyRest() != null) {
            if (!useHelper) {
                sequence.add(node.getKeyRest().accept(this));
            }
        }

        if (node.getBlock() != null) {
            sequence.add(node.getBlock().accept(this));
        }

        return sequence(context, sourceSection, sequence);
    }

    private RubyNode loadSelf() {
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(SelfNode.SELF_IDENTIFIER);
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, (RubySourceSection) null, slot, new ProfileArgumentNode(new ReadSelfNode()));
    }

    @Override
    public RubyNode visitKeywordRestArgNode(org.jruby.ast.KeywordRestArgNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final RubyNode readNode = new ReadKeywordRestArgumentNode(context, fullSourceSection, required, excludedKeywords.toArray(new String[excludedKeywords.size()]));
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(node.getName());

        return WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitKeywordArgNode(org.jruby.ast.KeywordArgNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final org.jruby.ast.Node firstChild = node.childNodes().get(0);
        final org.jruby.ast.AssignableNode asgnNode;
        final String name;

        if (firstChild instanceof org.jruby.ast.LocalAsgnNode) {
            asgnNode = (org.jruby.ast.LocalAsgnNode) firstChild;
            name = ((org.jruby.ast.LocalAsgnNode) firstChild).getName();
        } else if (firstChild instanceof org.jruby.ast.DAsgnNode) {
            asgnNode = (org.jruby.ast.DAsgnNode) firstChild;
            name = ((org.jruby.ast.DAsgnNode) firstChild).getName();
        } else {
            throw new UnsupportedOperationException("unsupported keyword arg " + node);
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(name);

        final RubyNode defaultValue;
        if (asgnNode.getValueNode() instanceof RequiredKeywordArgumentValueNode) {
            /* This isn't a true default value - it's a marker to say there isn't one. This actually makes sense;
             * the semantic action of executing this node is to report an error, and we do the same thing. */
            defaultValue = new MissingKeywordArgumentNode(name);
        } else {
            defaultValue = translateNodeOrNil(sourceSection, asgnNode.getValueNode());
        }

        excludedKeywords.add(name);

        final RubyNode readNode = new ReadKeywordArgumentNode(context, fullSourceSection, required, name, defaultValue);

        return WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitArgumentNode(org.jruby.ast.ArgumentNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final RubyNode readNode = readArgument(sourceSection);
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, slot, readNode);
    }

    private RubyNode readArgument(RubySourceSection sourceSection) {
        if (useArray()) {
            return PrimitiveArrayNodeFactory.read(context, sourceSection.toSourceSection(), loadArray(sourceSection), index);
        } else {
            if (state == State.PRE) {
                return new ProfileArgumentNode(new ReadPreArgumentNode(index, isProc ? MissingArgumentBehavior.NIL : MissingArgumentBehavior.RUNTIME_ERROR));
            } else if (state == State.POST) {
                return new ReadPostArgumentNode(-index);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    @Override
    public RubyNode visitRestArgNode(org.jruby.ast.RestArgNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final RubyNode readNode;

        if (argsNode == null) {
            throw new IllegalStateException("No arguments node visited");
        }

        int from = argsNode.getPreCount() + argsNode.getOptionalArgsCount();
        int to = -argsNode.getPostCount();
        if (useArray()) {
            readNode = ArraySliceNodeGen.create(context, fullSourceSection, from, to, loadArray(sourceSection));
        } else {
            readNode = new ReadRestArgumentNode(context, fullSourceSection, from, -to, hasKeywordArguments, required);
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitBlockArgNode(org.jruby.ast.BlockArgNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final RubyNode readNode = new ReadBlockNode(context.getCoreLibrary().getNilObject());
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, slot, readNode);
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
        final RubySourceSection sourceSection = translate(sourcePosition);
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(name);

        final RubyNode readNode;

        if (indexFromEnd == 1) {
            if (valueNode instanceof org.jruby.ast.NilImplicitNode) {
                // Multiple assignment

                if (useArray()) {
                    readNode = PrimitiveArrayNodeFactory.read(context, fullSourceSection, loadArray(sourceSection), index);
                } else {
                    readNode = readArgument(sourceSection);
                }
            } else {
                // Optional argument
                final RubyNode defaultValue;

                // The JRuby parser gets local variables that shadow methods with vcalls wrong - fix up here

                if (valueNode instanceof org.jruby.ast.VCallNode) {
                    final String calledName = ((org.jruby.ast.VCallNode) valueNode).getName();

                    // Just consider the circular case for now as that's all that's speced

                    if (calledName.equals(name)) {
                        defaultValue = new ReadLocalVariableNode(context, fullSourceSection, LocalVariableType.FRAME_LOCAL, slot);
                    } else {
                        defaultValue = valueNode.accept(this);
                    }
                } else {
                    defaultValue = valueNode.accept(this);
                }

                if (argsNode == null) {
                    throw new IllegalStateException("No arguments node visited");
                }

                int minimum = index + 1 + argsNode.getPostCount();

                if (useArray()) {
                    // TODO CS 10-Jan-16 we should really hoist this check, or see if Graal does it for us
                    readNode = new IfElseNode(context, fullSourceSection,
                            new ArrayIsAtLeastAsLargeAsNode(minimum, loadArray(sourceSection)),
                            PrimitiveArrayNodeFactory.read(context, fullSourceSection, loadArray(sourceSection), index),
                            defaultValue);
                } else {
                    if (argsNode.hasKwargs()) {
                        minimum += 1;
                    }

                    final boolean considerRejectedKWArgs;
                    final ReadRestArgumentNode readRest;

                    if (firstOpt && hasKeywordArguments) {
                        considerRejectedKWArgs = true;
                        int from = argsNode.getPreCount() + argsNode.getOptionalArgsCount();
                        int to = -argsNode.getPostCount();
                        readRest = new ReadRestArgumentNode(context, fullSourceSection, from, -to, hasKeywordArguments, required);
                    } else {
                        considerRejectedKWArgs = false;
                        readRest = null;
                    }

                    readNode = new ReadOptionalArgumentNode(context, fullSourceSection, index, minimum, considerRejectedKWArgs, argsNode.hasKwargs(), required, readRest, defaultValue);
                }
            }
        } else {
            readNode = ArraySliceNodeGen.create(context, fullSourceSection, index, indexFromEnd, loadArray(sourceSection));
        }

        return WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection, slot, readNode);
    }

    @Override
    public RubyNode visitArrayNode(org.jruby.ast.ArrayNode node) {
        // (ArrayNode 0, (MultipleAsgn19Node 0, (ArrayNode 0, (LocalAsgnNode:a 0, ), (LocalAsgnNode:b 0, )), null, null)))
        if (node.size() == 1 && node.get(0) instanceof MultipleAsgnNode) {
            return node.children()[0].accept(this);
        } else {
            return defaultVisit(node);
        }
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection();

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

        // The load to use when the array is not nil and the length is smaller than the number of required arguments

        final List<RubyNode> notNilSmallerSequence = new ArrayList<>();

        if (node.getPre() != null) {
            index = 0;
            for (org.jruby.ast.Node child : node.getPre().children()) {
                notNilSmallerSequence.add(child.accept(this));
                index++;
            }
        }

        if (node.getRest() != null) {
            index = node.getPreCount();
            indexFromEnd = -node.getPostCount();
            notNilSmallerSequence.add(node.getRest().accept(this));
            indexFromEnd = 1;
        }

        if (node.getPost() != null) {
            org.jruby.ast.Node[] children = node.getPost().children();
            index = node.getPreCount();
            for (int i = 0; i < children.length; i++) {
                notNilSmallerSequence.add(children[i].accept(this));
                index++;
            }
        }

        final RubyNode notNilSmaller = sequence(context, sourceSection, notNilSmallerSequence);

        // The load to use when the array is not nil and at least as large as the number of required arguments

        final List<RubyNode> notNilAtLeastAsLargeSequence = new ArrayList<>();

        if (node.getPre() != null) {
            index = 0;
            for (org.jruby.ast.Node child : node.getPre().children()) {
                notNilAtLeastAsLargeSequence.add(child.accept(this));
                index++;
            }
        }

        if (node.getRest() != null) {
            index = node.getPreCount();
            indexFromEnd = -node.getPostCount();
            notNilAtLeastAsLargeSequence.add(node.getRest().accept(this));
            indexFromEnd = 1;
        }

        if (node.getPost() != null) {
            org.jruby.ast.Node[] children = node.getPost().children();
            index = -1;
            for (int i = children.length - 1; i >= 0; i--) {
                notNilAtLeastAsLargeSequence.add(children[i].accept(this));
                index--;
            }
        }

        final RubyNode notNilAtLeastAsLarge = sequence(context, sourceSection, notNilAtLeastAsLargeSequence);

        popArraySlot(arraySlot);

        final List<RubyNode> nilSequence = new ArrayList<>();

        final ParameterCollector parametersToClearCollector = new ParameterCollector();

        if (node.getPre() != null) {
            for (org.jruby.ast.Node child : node.getPre().children()) {
                child.accept(parametersToClearCollector);
            }
        }

        if (node.getRest() != null) {
            if (node.getRest() instanceof INameNode) {
                final String name = ((INameNode) node.getRest()).getName();

                if (node.getPreCount() == 0 && node.getPostCount() == 0) {
                    nilSequence.add(methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(name, sourceSection)
                            .makeWriteNode(ArrayLiteralNode.create(context, sourceSection, new RubyNode[] { new NilLiteralNode(context, fullSourceSection, true) })));
                } else {
                    nilSequence.add(methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(name, sourceSection)
                            .makeWriteNode(ArrayLiteralNode.create(context, sourceSection, new RubyNode[] {})));
                }
            } else if (node.getRest() instanceof StarNode) {
                // Don't think we need to do anything
            } else {
                throw new UnsupportedOperationException("unsupported rest node " + node.getRest());
            }
        }

        if (node.getPost() != null) {
            for (org.jruby.ast.Node child : node.getPost().children()) {
                child.accept(parametersToClearCollector);
            }
        }

        for (String parameterToClear : parametersToClearCollector.getParameters()) {
            nilSequence.add(methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(parameterToClear, sourceSection).makeWriteNode(nilNode(sourceSection)));
        }

        if (!childNodes.isEmpty()) {
            // We haven't pushed a new array slot, so this will read the value which we couldn't convert to an array into the first destructured argument
            index = arrayIndex;
            nilSequence.add(childNodes.get(0).accept(this));
        }

        final RubyNode nil = sequence(context, sourceSection, nilSequence);

        return sequence(context, sourceSection, Arrays.asList(WriteLocalVariableNode.createWriteLocalVariableNode(context, fullSourceSection,
                arraySlot, SplatCastNodeGen.create(context, fullSourceSection, SplatCastNode.NilBehavior.ARRAY_WITH_NIL, true,
                                readArgument(sourceSection))), new IfElseNode(context, fullSourceSection,
                        new IsNilNode(context, fullSourceSection, new ReadLocalVariableNode(context, fullSourceSection, LocalVariableType.FRAME_LOCAL, arraySlot)),
                        nil,
                        new IfElseNode(context, fullSourceSection,
                                new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), new ReadLocalVariableNode(context, fullSourceSection, LocalVariableType.FRAME_LOCAL, arraySlot)),
                                notNilAtLeastAsLarge,
                                notNilSmaller))));
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

    protected RubyNode loadArray(RubySourceSection sourceSection) {
        return new ReadLocalVariableNode(context, sourceSection.toSourceSection(), LocalVariableType.FRAME_LOCAL, arraySlotStack.peek().getArraySlot());
    }

}
