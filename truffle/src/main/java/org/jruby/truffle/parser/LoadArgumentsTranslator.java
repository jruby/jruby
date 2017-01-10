/*
 * Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.IsNilNode;
import org.jruby.truffle.core.array.ArrayLiteralNode;
import org.jruby.truffle.core.array.ArraySliceNodeGen;
import org.jruby.truffle.core.array.PrimitiveArrayNodeFactory;
import org.jruby.truffle.core.cast.SplatCastNode;
import org.jruby.truffle.core.cast.SplatCastNodeGen;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.SourceIndexLength;
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
import org.jruby.truffle.language.arguments.RunBlockKWArgsHelperNode;
import org.jruby.truffle.language.control.IfElseNode;
import org.jruby.truffle.language.control.IfNode;
import org.jruby.truffle.language.literal.NilLiteralNode;
import org.jruby.truffle.language.locals.LocalVariableType;
import org.jruby.truffle.language.locals.ReadLocalVariableNode;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.ArrayParseNode;
import org.jruby.truffle.parser.ast.AssignableParseNode;
import org.jruby.truffle.parser.ast.BlockArgParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.KeywordArgParseNode;
import org.jruby.truffle.parser.ast.KeywordRestArgParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.NilImplicitParseNode;
import org.jruby.truffle.parser.ast.OptArgParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RequiredKeywordArgumentValueParseNode;
import org.jruby.truffle.parser.ast.RestArgParseNode;
import org.jruby.truffle.parser.ast.StarParseNode;
import org.jruby.truffle.parser.ast.VCallParseNode;
import org.jruby.truffle.parser.ast.types.INameNode;

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

    private ArgsParseNode argsNode;

    public LoadArgumentsTranslator(Node currentNode, RubyContext context, Source source, ParserContext parserContext, boolean isProc, BodyTranslator methodBodyTranslator) {
        super(currentNode, context, source, parserContext);
        this.isProc = isProc;
        this.methodBodyTranslator = methodBodyTranslator;
    }

    @Override
    public RubyNode visitArgsNode(ArgsParseNode node) {
        argsNode = node;

        final SourceIndexLength sourceSection = node.getPosition();

        final List<RubyNode> sequence = new ArrayList<>();

        //if (!arraySlotStack.isEmpty()) {
        sequence.add(loadSelf(context, methodBodyTranslator.getEnvironment()));
        //}

        final ParseNode[] args = node.getArgs();

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

            sequence.add(new IfNode(
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
            // (BlockParseNode 0, (OptArgParseNode:a 0, (LocalAsgnParseNode:a 0, (FixnumParseNode 0))), ...)
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
            ParseNode[] children = node.getPost().children();
            index = node.getPreCount();
            for (int i = 0; i < children.length; i++) {
                notNilSmallerSequence.add(children[i].accept(this));
                index++;
            }
        }

        final RubyNode notNilSmaller = sequence(sourceSection, notNilSmallerSequence);

        // The load to use when the there is no rest

        final List<RubyNode> noRestSequence = new ArrayList<>();

        if (postCount > 0) {
            state = State.POST;
            ParseNode[] children = node.getPost().children();
            index = node.getPreCount() + node.getOptionalArgsCount();
            for (int i = 0; i < children.length; i++) {
                noRestSequence.add(children[i].accept(this));
                index++;
            }
        }

        final RubyNode noRest = sequence(sourceSection, noRestSequence);

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

        final RubyNode notNilAtLeastAsLarge = sequence(sourceSection, notNilAtLeastAsLargeSequence);

        if (useArray()) {
            if (node.getPreCount() == 0 || node.hasRestArg()) {
                sequence.add(new IfElseNode(
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

        return sequence(sourceSection, sequence);
    }

    @Override
    public RubyNode visitKeywordRestArgNode(KeywordRestArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();

        final RubyNode readNode = new ReadKeywordRestArgumentNode(required, excludedKeywords.toArray(new String[excludedKeywords.size()]));
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(node.getName());

        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode);
    }

    @Override
    public RubyNode visitKeywordArgNode(KeywordArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final AssignableParseNode asgnNode = node.getAssignable();
        final String name = ((INameNode) asgnNode).getName();

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(name);

        final RubyNode defaultValue;
        if (asgnNode.getValueNode() instanceof RequiredKeywordArgumentValueParseNode) {
            /* This isn't a true default value - it's a marker to say there isn't one. This actually makes sense;
             * the semantic action of executing this node is to report an error, and we do the same thing. */
            defaultValue = new MissingKeywordArgumentNode(name);
        } else {
            defaultValue = translateNodeOrNil(sourceSection, asgnNode.getValueNode());
        }

        excludedKeywords.add(name);

        final RubyNode readNode = new ReadKeywordArgumentNode(required, name, defaultValue);

        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode);
    }

    @Override
    public RubyNode visitArgumentNode(ArgumentParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode readNode = readArgument(sourceSection);
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode);
    }

    private RubyNode readArgument(SourceIndexLength sourceSection) {
        if (useArray()) {
            return PrimitiveArrayNodeFactory.read(loadArray(sourceSection), index);
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
    public RubyNode visitRestArgNode(RestArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();

        final RubyNode readNode;

        if (argsNode == null) {
            throw new IllegalStateException("No arguments node visited");
        }

        int from = argsNode.getPreCount() + argsNode.getOptionalArgsCount();
        int to = -argsNode.getPostCount();
        if (useArray()) {
            readNode = ArraySliceNodeGen.create(from, to, loadArray(sourceSection));
        } else {
            readNode = new ReadRestArgumentNode(from, -to, hasKeywordArguments, required);
        }

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode);
    }

    @Override
    public RubyNode visitBlockArgNode(BlockArgParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();

        final RubyNode readNode = new ReadBlockNode(context.getCoreLibrary().getNilObject());
        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findFrameSlot(node.getName());
        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode);
    }

    @Override
    public RubyNode visitOptArgNode(OptArgParseNode node) {
        // (OptArgParseNode:a 0, (LocalAsgnParseNode:a 0, (FixnumParseNode 0)))
        return node.getValue().accept(this);
    }

    @Override
    public RubyNode visitLocalAsgnNode(LocalAsgnParseNode node) {
        return translateLocalAssignment(node.getPosition(), node.getName(), node.getValueNode());
    }

    @Override
    public RubyNode visitDAsgnNode(DAsgnParseNode node) {
        return translateLocalAssignment(node.getPosition(), node.getName(), node.getValueNode());
    }

    private RubyNode translateLocalAssignment(SourceIndexLength sourcePosition, String name, ParseNode valueNode) {
        final SourceIndexLength sourceSection = sourcePosition;

        final FrameSlot slot = methodBodyTranslator.getEnvironment().getFrameDescriptor().findOrAddFrameSlot(name);

        final RubyNode readNode;

        if (indexFromEnd == 1) {
            if (valueNode instanceof NilImplicitParseNode) {
                // Multiple assignment

                if (useArray()) {
                    readNode = PrimitiveArrayNodeFactory.read(loadArray(sourceSection), index);
                } else {
                    readNode = readArgument(sourceSection);
                }
            } else {
                // Optional argument
                final RubyNode defaultValue;

                // The JRuby parser gets local variables that shadow methods with vcalls wrong - fix up here

                if (valueNode instanceof VCallParseNode) {
                    final String calledName = ((VCallParseNode) valueNode).getName();

                    // Just consider the circular case for now as that's all that's speced

                    if (calledName.equals(name)) {
                        defaultValue = new ReadLocalVariableNode(LocalVariableType.FRAME_LOCAL, slot);
                        defaultValue.unsafeSetSourceSection(sourceSection);
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
                    readNode = new IfElseNode(
                            new ArrayIsAtLeastAsLargeAsNode(minimum, loadArray(sourceSection)),
                            PrimitiveArrayNodeFactory.read(loadArray(sourceSection), index),
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
                        readRest = new ReadRestArgumentNode(from, -to, hasKeywordArguments, required);
                    } else {
                        considerRejectedKWArgs = false;
                        readRest = null;
                    }

                    readNode = new ReadOptionalArgumentNode(index, minimum, considerRejectedKWArgs, argsNode.hasKwargs(), required, readRest, defaultValue);
                }
            }
        } else {
            readNode = ArraySliceNodeGen.create(index, indexFromEnd, loadArray(sourceSection));
        }

        return WriteLocalVariableNode.createWriteLocalVariableNode(context, slot, readNode);
    }

    @Override
    public RubyNode visitArrayNode(ArrayParseNode node) {
        // (ArrayParseNode 0, (MultipleAsgn19Node 0, (ArrayParseNode 0, (LocalAsgnParseNode:a 0, ), (LocalAsgnParseNode:b 0, )), null, null)))
        if (node.size() == 1 && node.get(0) instanceof MultipleAsgnParseNode) {
            return node.children()[0].accept(this);
        } else {
            return defaultVisit(node);
        }
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnParseNode node) {
        final SourceIndexLength sourceSection = node.getPosition();
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        // (MultipleAsgn19Node 0, (ArrayParseNode 0, (LocalAsgnParseNode:a 0, ), (LocalAsgnParseNode:b 0, )), null, null))

        final int arrayIndex = index;

        final String arrayName = methodBodyTranslator.getEnvironment().allocateLocalTemp("destructure");
        final FrameSlot arraySlot = methodBodyTranslator.getEnvironment().declareVar(arrayName);

        pushArraySlot(arraySlot);

        final List<ParseNode> childNodes;

        if (node.childNodes().get(0) == null) {
            childNodes = Collections.emptyList();
        } else {
            childNodes = node.childNodes().get(0).childNodes();
        }

        // The load to use when the array is not nil and the length is smaller than the number of required arguments

        final List<RubyNode> notNilSmallerSequence = new ArrayList<>();

        if (node.getPre() != null) {
            index = 0;
            for (ParseNode child : node.getPre().children()) {
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
            ParseNode[] children = node.getPost().children();
            index = node.getPreCount();
            for (int i = 0; i < children.length; i++) {
                notNilSmallerSequence.add(children[i].accept(this));
                index++;
            }
        }

        final RubyNode notNilSmaller = sequence(sourceSection, notNilSmallerSequence);

        // The load to use when the array is not nil and at least as large as the number of required arguments

        final List<RubyNode> notNilAtLeastAsLargeSequence = new ArrayList<>();

        if (node.getPre() != null) {
            index = 0;
            for (ParseNode child : node.getPre().children()) {
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
            ParseNode[] children = node.getPost().children();
            index = -1;
            for (int i = children.length - 1; i >= 0; i--) {
                notNilAtLeastAsLargeSequence.add(children[i].accept(this));
                index--;
            }
        }

        final RubyNode notNilAtLeastAsLarge = sequence(sourceSection, notNilAtLeastAsLargeSequence);

        popArraySlot(arraySlot);

        final List<RubyNode> nilSequence = new ArrayList<>();

        final ParameterCollector parametersToClearCollector = new ParameterCollector();

        if (node.getPre() != null) {
            for (ParseNode child : node.getPre().children()) {
                child.accept(parametersToClearCollector);
            }
        }

        if (node.getRest() != null) {
            if (node.getRest() instanceof INameNode) {
                final String name = ((INameNode) node.getRest()).getName();

                if (node.getPreCount() == 0 && node.getPostCount() == 0) {
                    nilSequence.add(methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(name, source, sourceSection)
                            .makeWriteNode(ArrayLiteralNode.create(new RubyNode[] { new NilLiteralNode(true) })));
                } else {
                    nilSequence.add(methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(name, source, sourceSection)
                            .makeWriteNode(ArrayLiteralNode.create(new RubyNode[] {})));
                }
            } else if (node.getRest() instanceof StarParseNode) {
                // Don't think we need to do anything
            } else {
                throw new UnsupportedOperationException("unsupported rest node " + node.getRest());
            }
        }

        if (node.getPost() != null) {
            for (ParseNode child : node.getPost().children()) {
                child.accept(parametersToClearCollector);
            }
        }

        for (String parameterToClear : parametersToClearCollector.getParameters()) {
            nilSequence.add(methodBodyTranslator.getEnvironment().findOrAddLocalVarNodeDangerous(parameterToClear, source, sourceSection).makeWriteNode(nilNode(source, sourceSection)));
        }

        if (!childNodes.isEmpty()) {
            // We haven't pushed a new array slot, so this will read the value which we couldn't convert to an array into the first destructured argument
            index = arrayIndex;
            nilSequence.add(childNodes.get(0).accept(this));
        }

        final RubyNode nil = sequence(sourceSection, nilSequence);

        return sequence(sourceSection, Arrays.asList(WriteLocalVariableNode.createWriteLocalVariableNode(context,
                arraySlot, SplatCastNodeGen.create(SplatCastNode.NilBehavior.ARRAY_WITH_NIL, true,
                                readArgument(sourceSection))), new IfElseNode(
                        new IsNilNode(new ReadLocalVariableNode(LocalVariableType.FRAME_LOCAL, arraySlot)),
                        nil,
                        new IfElseNode(
                                new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), new ReadLocalVariableNode(LocalVariableType.FRAME_LOCAL, arraySlot)),
                                notNilAtLeastAsLarge,
                                notNilSmaller))));
    }

    @Override
    protected RubyNode defaultVisit(ParseNode node) {
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

    protected RubyNode loadArray(SourceIndexLength sourceSection) {
        final RubyNode node = new ReadLocalVariableNode(LocalVariableType.FRAME_LOCAL, arraySlotStack.peek().getArraySlot());
        node.unsafeSetSourceSection(sourceSection);
        return node;
    }

}
