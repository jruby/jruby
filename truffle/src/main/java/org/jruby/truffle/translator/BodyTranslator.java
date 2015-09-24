/*
 * Copyright (c) 2013, 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.translator;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.joni.NameEntry;
import org.joni.Regex;
import org.joni.Syntax;
import org.jruby.ast.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.ThreadLocalObjectNode;
import org.jruby.truffle.nodes.arguments.IsRubiniusUndefinedNode;
import org.jruby.truffle.nodes.cast.*;
import org.jruby.truffle.nodes.constants.ReadConstantWithLexicalScopeNode;
import org.jruby.truffle.nodes.constants.ReadLiteralConstantNode;
import org.jruby.truffle.nodes.constants.WriteConstantNode;
import org.jruby.truffle.nodes.control.AndNode;
import org.jruby.truffle.nodes.control.BreakNode;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.NextNode;
import org.jruby.truffle.nodes.control.OrNode;
import org.jruby.truffle.nodes.control.RedoNode;
import org.jruby.truffle.nodes.control.RetryNode;
import org.jruby.truffle.nodes.control.ReturnNode;
import org.jruby.truffle.nodes.control.WhileNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.core.ProcNodes.Type;
import org.jruby.truffle.nodes.core.array.*;
import org.jruby.truffle.nodes.core.fixnum.FixnumLiteralNode;
import org.jruby.truffle.nodes.core.hash.ConcatHashLiteralNode;
import org.jruby.truffle.nodes.core.hash.HashLiteralNode;
import org.jruby.truffle.nodes.core.hash.HashNodesFactory;
import org.jruby.truffle.nodes.debug.AssertConstantNodeGen;
import org.jruby.truffle.nodes.debug.AssertNotCompiledNodeGen;
import org.jruby.truffle.nodes.defined.DefinedNode;
import org.jruby.truffle.nodes.defined.DefinedWrapperNode;
import org.jruby.truffle.nodes.dispatch.RubyCallNode;
import org.jruby.truffle.nodes.exceptions.EnsureNode;
import org.jruby.truffle.nodes.exceptions.*;
import org.jruby.truffle.nodes.exceptions.RescueNode;
import org.jruby.truffle.nodes.globals.*;
import org.jruby.truffle.nodes.literal.BooleanLiteralNode;
import org.jruby.truffle.nodes.literal.LiteralNode;
import org.jruby.truffle.nodes.literal.RangeLiteralNodeGen;
import org.jruby.truffle.nodes.literal.StringLiteralNode;
import org.jruby.truffle.nodes.locals.*;
import org.jruby.truffle.nodes.methods.*;
import org.jruby.truffle.nodes.methods.UndefNode;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.nodes.rubinius.RubiniusLastStringReadNode;
import org.jruby.truffle.nodes.rubinius.RubiniusPrimitiveConstructor;
import org.jruby.truffle.nodes.rubinius.RubiniusSingleBlockArgNode;
import org.jruby.truffle.nodes.yield.YieldNode;
import org.jruby.truffle.runtime.ConstantReplacer;
import org.jruby.truffle.runtime.LexicalScope;
import org.jruby.truffle.runtime.ReturnID;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.control.RaiseException;
import org.jruby.truffle.runtime.core.CoreLibrary;
import org.jruby.truffle.runtime.layouts.Layouts;
import org.jruby.truffle.runtime.methods.Arity;
import org.jruby.truffle.runtime.methods.SharedMethodInfo;
import org.jruby.truffle.translator.TranslatorEnvironment.BreakID;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * A JRuby parser node visitor which translates JRuby AST nodes into truffle Nodes. Therefore there is some namespace
 * contention here! We make all references to JRuby explicit.
 */
public class BodyTranslator extends Translator {

    protected final BodyTranslator parent;
    protected final TranslatorEnvironment environment;

    public boolean translatingForStatement = false;
    public boolean useClassVariablesAsIfInClass = false;
    private boolean translatingNextExpression = false;
    private boolean translatingWhile = false;
    protected String currentCallMethodName = null;

    private boolean privately = false;

    protected boolean usesRubiniusPrimitive = false;

    private static final Set<String> debugIgnoredCalls = new HashSet<>();

    static {
        debugIgnoredCalls.add("downto");
        debugIgnoredCalls.add("each");
        debugIgnoredCalls.add("times");
        debugIgnoredCalls.add("upto");
    }

    public static final Set<String> THREAD_LOCAL_GLOBAL_VARIABLES = new HashSet<>(Arrays.asList("$~", "$1", "$2", "$3", "$4", "$5", "$6", "$7", "$8", "$9", "$!", "$?")); // "$_"

    public BodyTranslator(com.oracle.truffle.api.nodes.Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, Source source, boolean topLevel) {
        super(currentNode, context, source);
        this.parent = parent;
        this.environment = environment;
        initGlobalVariableAliases();
        initReadOnlyGlobalVariables();
    }

    @Override
    public RubyNode visitAliasNode(org.jruby.ast.AliasNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.LiteralNode oldName = (org.jruby.ast.LiteralNode) node.getOldName();
        final org.jruby.ast.LiteralNode newName = (org.jruby.ast.LiteralNode) node.getNewName();

        final RubyNode ret = AliasNodeGen.create(context, sourceSection, newName.getName(), oldName.getName(), new SelfNode(context, sourceSection));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitAndNode(org.jruby.ast.AndNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode x = translateNodeOrNil(sourceSection, node.getFirstNode());
        final RubyNode y = translateNodeOrNil(sourceSection, node.getSecondNode());

        final RubyNode ret = new AndNode(context, sourceSection, x, y);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitArgsCatNode(org.jruby.ast.ArgsCatNode node) {
        final List<org.jruby.ast.Node> nodes = new ArrayList<>();
        collectArgsCatNodes(nodes, node);

        final List<RubyNode> translatedNodes = new ArrayList<>();

        for (org.jruby.ast.Node catNode : nodes) {
            translatedNodes.add(catNode.accept(this));
        }

        final RubyNode ret = new ArrayConcatNode(context, translate(node.getPosition()), translatedNodes.toArray(new RubyNode[translatedNodes.size()]));
        return addNewlineIfNeeded(node, ret);
    }

    // ArgsCatNodes can be nested - this collects them into a flat list of children
    private void collectArgsCatNodes(List<org.jruby.ast.Node> nodes, org.jruby.ast.ArgsCatNode node) {
        if (node.getFirstNode() instanceof org.jruby.ast.ArgsCatNode) {
            collectArgsCatNodes(nodes, (org.jruby.ast.ArgsCatNode) node.getFirstNode());
        } else {
            nodes.add(node.getFirstNode());
        }

        if (node.getSecondNode() instanceof org.jruby.ast.ArgsCatNode) {
            collectArgsCatNodes(nodes, (org.jruby.ast.ArgsCatNode) node.getSecondNode());
        } else {
            // ArgsCatNode implicitly splat its second argument. See Helpers.argsCat.
            Node secondNode = new SplatNode(node.getSecondNode().getPosition(), node.getSecondNode());
            nodes.add(secondNode);
        }
    }

    @Override
    public RubyNode visitArgsPushNode(org.jruby.ast.ArgsPushNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode ret = ArrayNodesFactory.PushOneNodeFactory.create(context, sourceSection, new RubyNode[]{
                KernelNodesFactory.DupNodeFactory.create(context, sourceSection, new RubyNode[]{
                        node.getFirstNode().accept(this)
                }),
                node.getSecondNode().accept(this)
        });

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitArrayNode(org.jruby.ast.ArrayNode node) {
        final org.jruby.ast.Node[] values = node.children();

        final RubyNode[] translatedValues = new RubyNode[values.length];

        for (int n = 0; n < values.length; n++) {
            translatedValues[n] = values[n].accept(this);
        }

        final RubyNode ret = new ArrayLiteralNode.UninitialisedArrayLiteralNode(context, translate(node.getPosition()), translatedValues);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitAttrAssignNode(org.jruby.ast.AttrAssignNode node) {
        final RubyNode ret = visitAttrAssignNodeExtraArgument(node, null);
        return addNewlineIfNeeded(node, ret);
    }

    /**
     * See translateDummyAssignment to understand what this is for.
     */
    public RubyNode visitAttrAssignNodeExtraArgument(org.jruby.ast.AttrAssignNode node, RubyNode extraArgument) {
        final SourceSection sourceSection = translate(node.getPosition());

        // The last argument is the value we assign, and we need to return that as the whole result of this node

        final FrameSlot frameSlot = environment.declareVar(environment.allocateLocalTemp("attrasgn"));
        final WriteLocalVariableNode writeValue;

        final org.jruby.ast.ArrayNode newArgsNode;

        if (extraArgument == null) {
            // Get that last argument out
            final List<org.jruby.ast.Node> argChildNodes = new ArrayList<>(node.getArgsNode().childNodes());
            final org.jruby.ast.Node valueNode = argChildNodes.get(argChildNodes.size() - 1);
            argChildNodes.remove(argChildNodes.size() - 1);

            // Evaluate the value and store it in a local variable
            writeValue = new WriteLocalVariableNode(context, sourceSection, valueNode.accept(this), frameSlot);

            // Recreate the arguments array, reading that local instead of including the RHS for the last argument
            argChildNodes.add(new ReadLocalDummyNode(node.getPosition(), sourceSection, frameSlot));
            newArgsNode = new org.jruby.ast.ArrayNode(node.getPosition(), argChildNodes.get(0));
            argChildNodes.remove(0);
            for (org.jruby.ast.Node child : argChildNodes) {
                newArgsNode.add(child);
            }
        } else {
            final RubyNode valueNode = extraArgument;

            // Evaluate the value and store it in a local variable
            writeValue = new WriteLocalVariableNode(context, sourceSection, valueNode, frameSlot);

            // Recreate the arguments array, reading that local instead of including the RHS for the last argument
            final List<org.jruby.ast.Node> argChildNodes = new ArrayList<>();
            if (node.getArgsNode() != null) {
                argChildNodes.addAll(node.getArgsNode().childNodes());
            }
            argChildNodes.add(new ReadLocalDummyNode(node.getPosition(), sourceSection, frameSlot));
            newArgsNode = new org.jruby.ast.ArrayNode(node.getPosition(), argChildNodes.get(0));
            argChildNodes.remove(0);
            for (org.jruby.ast.Node child : argChildNodes) {
                newArgsNode.add(child);
            }
        }

        /*
         * If the original call was of the form:
         *
            (AttrAssignNode:[]= 10
                (LocalVarNode:f 9)
                (ArgsPushNode 9
                    (SplatNode 9
                        (LocalVarNode:x 9)
                    )
                    (FixnumNode 9)
                )
            )
         *
         * Then we will have lost that args push and we will have ended up with (Array (Splat (Local x) (Fixnum))
         *
         * Restory the args push.
         */

        final org.jruby.ast.Node fixedArgsNode;

        if (node.getArgsNode() instanceof org.jruby.ast.ArgsPushNode) {
            if (newArgsNode.size() != 2) {
                throw new UnsupportedOperationException();
            }

            fixedArgsNode = new org.jruby.ast.ArgsPushNode(newArgsNode.getPosition(), newArgsNode.children()[0], newArgsNode.children()[1]);
        } else {
            fixedArgsNode = newArgsNode;
        }

        final CallNode callNode = new CallNode(node.getPosition(), node.getReceiverNode(), node.getName(), fixedArgsNode, null);
        boolean isAccessorOnSelf = (node.getReceiverNode() instanceof org.jruby.ast.SelfNode);
        final RubyNode actualCall = visitCallNodeExtraArgument(callNode, null, isAccessorOnSelf, false);

        final RubyNode ret = SequenceNode.sequence(context, sourceSection,
                writeValue,
                actualCall,
                new ReadLocalVariableNode(context, sourceSection, frameSlot));

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBeginNode(org.jruby.ast.BeginNode node) {
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBignumNode(org.jruby.ast.BignumNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        // These aren't always Bignums!

        final BigInteger value = node.getValue();
        final RubyNode ret;

        if (value.bitLength() >= 64) {
            ret = new LiteralNode(context, sourceSection, Layouts.BIGNUM.createBignum(context.getCoreLibrary().getBignumFactory(), node.getValue()));
        } else {
            ret = new FixnumLiteralNode.LongFixnumLiteralNode(context, sourceSection, value.longValue());
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBlockNode(org.jruby.ast.BlockNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> translatedChildren = new ArrayList<>();

        for (org.jruby.ast.Node child : node.children()) {
            if (child.getPosition() == InvalidSourcePosition.INSTANCE) {
                parentSourceSection.push(sourceSection);
            }

            final RubyNode translatedChild;

            try {
                translatedChild = child.accept(this);
            } finally {
                if (child.getPosition() == InvalidSourcePosition.INSTANCE) {
                    parentSourceSection.pop();
                }
            }

            if (!(translatedChild instanceof DeadNode)) {
                translatedChildren.add(translatedChild);
            }
        }

        final RubyNode ret;

        if (translatedChildren.size() == 1) {
            ret = translatedChildren.get(0);
        } else {
            ret = SequenceNode.sequence(context, sourceSection, translatedChildren.toArray(new RubyNode[translatedChildren.size()]));
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBreakNode(org.jruby.ast.BreakNode node) {
        assert environment.isBlock() || translatingWhile : "The parser did not see an invalid break";

        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode resultNode;

        if (node.getValueNode().getPosition() == InvalidSourcePosition.INSTANCE) {
            parentSourceSection.push(sourceSection);

            try {
                resultNode = node.getValueNode().accept(this);
            } finally {
                parentSourceSection.pop();
            }
        } else {
            resultNode = node.getValueNode().accept(this);
        }

        final RubyNode ret = new BreakNode(context, sourceSection, environment.getBreakID(), resultNode);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitCallNode(CallNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final Node receiver = node.getReceiverNode();
        final String methodName = node.getName();

        // Rubinius.<method>
        if (receiver instanceof org.jruby.ast.ConstNode
                && ((ConstNode) receiver).getName().equals("Rubinius")) {
            if (methodName.equals("primitive")) {
                final RubyNode ret = translateRubiniusPrimitive(sourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("invoke_primitive")) {
                final RubyNode ret = translateRubiniusInvokePrimitive(sourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("privately")) {
                final RubyNode ret = translateRubiniusPrivately(sourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("single_block_arg")) {
                final RubyNode ret = translateRubiniusSingleBlockArg(sourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("check_frozen")) {
                final RubyNode ret = translateRubiniusCheckFrozen(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof org.jruby.ast.Colon2ConstNode // Truffle::Primitive.<method>
                && ((org.jruby.ast.Colon2ConstNode) receiver).getLeftNode() instanceof org.jruby.ast.ConstNode
                && ((org.jruby.ast.ConstNode) ((org.jruby.ast.Colon2ConstNode) receiver).getLeftNode()).getName().equals("Truffle")
                && ((org.jruby.ast.Colon2ConstNode) receiver).getName().equals("Primitive")) {
            if (methodName.equals("assert_constant")) {
                final RubyNode ret = AssertConstantNodeGen.create(context, sourceSection, node.getArgsNode().childNodes().get(0).accept(this));
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("assert_not_compiled")) {
                final RubyNode ret = AssertNotCompiledNodeGen.create(context, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof org.jruby.ast.ConstNode // Truffle.omit
                && ((ConstNode) receiver).getName().equals("Truffle")) {
            if (methodName.equals("omit")) {
                // We're never going to run the omitted code and it's never used as the RHS for anything, so just
                // replace the call with nil.
                final RubyNode ret = nilNode(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof VCallNode // undefined.equal?(obj)
                && ((VCallNode) receiver).getName().equals("undefined")
                && sourceSection.getSource().getPath().startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/")
                && methodName.equals("equal?")) {
            RubyNode argument = translateArgumentsAndBlock(sourceSection, null, node.getArgsNode(), null, methodName).getArguments()[0];
            final RubyNode ret = new IsRubiniusUndefinedNode(context, sourceSection, argument);
            return addNewlineIfNeeded(node, ret);
        }

        final RubyNode ret = visitCallNodeExtraArgument(node, null, false, false);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateRubiniusPrimitive(SourceSection sourceSection, CallNode node) {
        usesRubiniusPrimitive = true;

        /*
         * Translates something that looks like
         *
         *   Rubinius.primitive :foo
         *
         * into
         *
         *   CallRubiniusPrimitiveNode(FooNode(arg1, arg2, ..., argN))
         *
         * or
         *
         *   (<#Method ModuleDefinedIn#foo>).call(arg1, arg2, ..., argN)
         *
         * Where the arguments are the same arguments as the method. It looks like this is only exercised with simple
         * arguments so we're not worrying too much about what happens when they're more complicated (rest,
         * keywords etc).
         */

        if (node.getArgsNode().childNodes().size() != 1 || !(node.getArgsNode().childNodes().get(0) instanceof org.jruby.ast.SymbolNode)) {
            throw new UnsupportedOperationException("Rubinius.primitive must have a single literal symbol argument");
        }

        final String primitiveName = ((org.jruby.ast.SymbolNode) node.getArgsNode().childNodes().get(0)).getName();

        final RubiniusPrimitiveConstructor primitive = context.getRubiniusPrimitiveManager().getPrimitive(primitiveName);
        final ReturnID returnID = environment.getReturnID();
        return primitive.createCallPrimitiveNode(context, sourceSection, returnID);
    }

    private RubyNode translateRubiniusInvokePrimitive(SourceSection sourceSection, CallNode node) {
        /*
         * Translates something that looks like
         *
         *   Rubinius.invoke_primitive :foo, arg1, arg2, argN
         *
         * into
         *
         *   InvokeRubiniusPrimitiveNode(FooNode(arg1, arg2, ..., argN))
         *
         * or
         *
         *   (<#Method ModuleDefinedIn#foo>).call(arg1, arg2, ..., argN)
         */

        if (node.getArgsNode().childNodes().size() < 1 || !(node.getArgsNode().childNodes().get(0) instanceof org.jruby.ast.SymbolNode)) {
            throw new UnsupportedOperationException("Rubinius.invoke_primitive must have at least an initial literal symbol argument");
        }

        final String primitiveName = ((org.jruby.ast.SymbolNode) node.getArgsNode().childNodes().get(0)).getName();

        final RubiniusPrimitiveConstructor primitive = context.getRubiniusPrimitiveManager().getPrimitive(primitiveName);

        final List<RubyNode> arguments = new ArrayList<>();

        // The first argument was the symbol so we ignore it
        for (int n = 1; n < node.getArgsNode().childNodes().size(); n++) {
            RubyNode readArgumentNode = node.getArgsNode().childNodes().get(n).accept(this);
            arguments.add(readArgumentNode);
        }

        return primitive.createInvokePrimitiveNode(context, sourceSection, arguments.toArray(new RubyNode[arguments.size()]));
    }

    private RubyNode translateRubiniusPrivately(SourceSection sourceSection, CallNode node) {
        /*
         * Translates something that looks like
         *
         *   Rubinius.privately { foo }
         *
         * into just
         *
         *   foo
         *
         * While we translate foo we'll mark all call sites as ignoring visbility.
         */

        if (!(node.getIterNode() instanceof org.jruby.ast.IterNode)) {
            throw new UnsupportedOperationException("Rubinius.privately needs a literal block");
        }

        if (node.getArgsNode() != null && node.getArgsNode().childNodes().size() > 0) {
            throw new UnsupportedOperationException("Rubinius.privately should not have any arguments");
        }

        /*
         * Normally when you visit an 'iter' (block) node it will set the method name for you, so that we can name the
         * block something like 'times-block'. Here we bypass the iter node and translate its child. So we set the
         * name here.
         */

        currentCallMethodName = "privately";

        /*
         * While we translate the body of the iter we want to create all call nodes with the ignore-visbility flag.
         * This flag is checked in visitCallNodeExtraArgument.
         */

        final boolean previousPrivately = privately;
        privately = true;

        try {
            return (((org.jruby.ast.IterNode) node.getIterNode()).getBodyNode()).accept(this);
        } finally {
            // Restore the previous value of the privately flag - allowing for nesting

            privately = previousPrivately;
        }
    }

    public RubyNode translateRubiniusSingleBlockArg(SourceSection sourceSection, CallNode node) {
        return new RubiniusSingleBlockArgNode(context, sourceSection);
    }

    private RubyNode translateRubiniusCheckFrozen(SourceSection sourceSection) {
        /*
         * Translate
         *
         *   Rubinius.check_frozen
         *
         * into
         *
         *   raise RuntimeError.new("can't modify frozen ClassName") if frozen?
         *
         * TODO(CS, 30-Jan-15) usual questions about monkey patching of the methods we're using
         */

        final RubyNode frozen = new RubyCallNode(context, sourceSection, "frozen?", new SelfNode(context, sourceSection), null, false);

        final RubyNode constructException = new RubyCallNode(context, sourceSection, "new",
                new LiteralNode(context, sourceSection, context.getCoreLibrary().getRuntimeErrorClass()),
                null, false,
                new StringLiteralNode(context, sourceSection, ByteList.create("FrozenError: can't modify frozen TODO"), StringSupport.CR_UNKNOWN));

        final RubyNode raise = new RubyCallNode(context, sourceSection, "raise", new SelfNode(context, sourceSection), null, false, true, constructException);

        return new IfNode(context, sourceSection,
                frozen,
                raise,
                nilNode(sourceSection));
    }

    /**
     * See translateDummyAssignment to understand what this is for.
     */
    public RubyNode visitCallNodeExtraArgument(CallNode node, RubyNode extraArgument, boolean ignoreVisibility, boolean isVCall) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode receiverTranslated = node.getReceiverNode().accept(this);

        org.jruby.ast.Node args = node.getArgsNode();
        org.jruby.ast.Node block = node.getIterNode();

        if (block == null && args instanceof org.jruby.ast.IterNode) {
            block = args;
            args = null;
        }

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, block, args, extraArgument, node.getName());

        RubyNode translated = new RubyCallNode(context, sourceSection,
                node.getName(), receiverTranslated, argumentsAndBlock.getBlock(), argumentsAndBlock.isSplatted(),
                privately || ignoreVisibility, isVCall, argumentsAndBlock.getArguments());

        if (argumentsAndBlock.getBlock() instanceof BlockDefinitionNode) { // if we have a literal block, break breaks out of this call site
            BlockDefinitionNode blockDef = (BlockDefinitionNode) argumentsAndBlock.getBlock();
            translated = new CatchBreakNode(context, sourceSection, translated, blockDef.getBreakID());
        }

        return addNewlineIfNeeded(node, translated);
    }

    protected static class ArgumentsAndBlockTranslation {

        private final RubyNode block;
        private final RubyNode[] arguments;
        private final boolean isSplatted;

        public ArgumentsAndBlockTranslation(RubyNode block, RubyNode[] arguments, boolean isSplatted) {
            super();
            this.block = block;
            this.arguments = arguments;
            this.isSplatted = isSplatted;
        }

        public RubyNode getBlock() {
            return block;
        }

        public RubyNode[] getArguments() {
            return Arrays.copyOf(arguments, arguments.length);
        }

        public boolean isSplatted() {
            return isSplatted;
        }

    }

    protected ArgumentsAndBlockTranslation translateArgumentsAndBlock(SourceSection sourceSection, org.jruby.ast.Node iterNode, org.jruby.ast.Node argsNode, RubyNode extraArgument, String nameToSetWhenTranslatingBlock) {
        assert !(argsNode instanceof org.jruby.ast.IterNode);

        final List<org.jruby.ast.Node> arguments = new ArrayList<>();
        org.jruby.ast.Node blockPassNode = null;

        boolean isSplatted = false;

        if (argsNode instanceof org.jruby.ast.ListNode) {
            arguments.addAll(argsNode.childNodes());
        } else if (argsNode instanceof org.jruby.ast.BlockPassNode) {
            final org.jruby.ast.BlockPassNode blockPass = (org.jruby.ast.BlockPassNode) argsNode;

            final org.jruby.ast.Node blockPassArgs = blockPass.getArgsNode();

            if (blockPassArgs instanceof org.jruby.ast.ListNode) {
                arguments.addAll(blockPassArgs.childNodes());
            } else if (blockPassArgs instanceof org.jruby.ast.ArgsCatNode) {
                arguments.add(blockPassArgs);
            } else if (blockPassArgs != null) {
                throw new UnsupportedOperationException("Don't know how to block pass " + blockPassArgs);
            }

            blockPassNode = blockPass.getBodyNode();
        } else if (argsNode instanceof org.jruby.ast.SplatNode) {
            isSplatted = true;
            arguments.add(argsNode);
        } else if (argsNode instanceof org.jruby.ast.ArgsCatNode) {
            isSplatted = true;
            arguments.add(argsNode);
        } else if (argsNode != null) {
            isSplatted = true;
            arguments.add(argsNode);
        }

        final List<RubyNode> argumentsTranslated = new ArrayList<>();

        for (org.jruby.ast.Node argument : arguments) {
            argumentsTranslated.add(argument.accept(this));
        }

        if (extraArgument != null) {
            argumentsTranslated.add(extraArgument);
        }

        final RubyNode[] argumentsTranslatedArray = argumentsTranslated.toArray(new RubyNode[argumentsTranslated.size()]);

        if (iterNode instanceof org.jruby.ast.BlockPassNode) {
            blockPassNode = ((org.jruby.ast.BlockPassNode) iterNode).getBodyNode();
        }

        currentCallMethodName = nameToSetWhenTranslatingBlock;

        RubyNode blockTranslated;

        if (blockPassNode != null) {
            blockTranslated = ProcCastNodeGen.create(context, sourceSection, blockPassNode.accept(this));
        } else if (iterNode != null) {
            blockTranslated = iterNode.accept(this);

            if (blockTranslated instanceof LiteralNode && ((LiteralNode) blockTranslated).getObject() == context.getCoreLibrary().getNilObject()) {
                blockTranslated = null;
            }
        } else {
            blockTranslated = null;
        }

        return new ArgumentsAndBlockTranslation(blockTranslated, argumentsTranslatedArray, isSplatted);
    }

    @Override
    public RubyNode visitCaseNode(org.jruby.ast.CaseNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode elseNode = translateNodeOrNil(sourceSection, node.getElseNode());

        /*
         * There are two sorts of case - one compares a list of expressions against a value, the
         * other just checks a list of expressions for truth.
         */

        final RubyNode ret;

        if (node.getCaseNode() != null) {
            // Evaluate the case expression and store it in a local

            final String tempName = environment.allocateLocalTemp("case");

            final RubyNode readTemp = environment.findLocalVarNode(tempName, sourceSection);

            final RubyNode assignTemp = ((ReadNode) readTemp).makeWriteNode(node.getCaseNode().accept(this));

            /*
             * Build an if expression from the whens and else. Work backwards because the first if
             * contains all the others in its else clause.
             */

            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final org.jruby.ast.WhenNode when = (org.jruby.ast.WhenNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<org.jruby.ast.Node> expressions;

                if (when.getExpressionNodes() instanceof org.jruby.ast.ListNode && !(when.getExpressionNodes() instanceof org.jruby.ast.ArrayNode)) {
                    expressions = when.getExpressionNodes().childNodes();
                } else {
                    expressions = Arrays.asList(when.getExpressionNodes());
                }

                final List<RubyNode> comparisons = new ArrayList<>();

                for (org.jruby.ast.Node expressionNode : expressions) {
                    final RubyNode rubyExpression = expressionNode.accept(this);

                    if (expressionNode instanceof org.jruby.ast.SplatNode) {
                        final SplatCastNode splatCastNode = (SplatCastNode) rubyExpression;
                        comparisons.add(new WhenSplatNode(context, sourceSection, NodeUtil.cloneNode(readTemp), splatCastNode));
                    } else if (expressionNode instanceof org.jruby.ast.ArgsCatNode) {
                        final ArrayConcatNode arrayConcatNode = (ArrayConcatNode) rubyExpression;
                        comparisons.add(new WhenSplatNode(context, sourceSection, NodeUtil.cloneNode(readTemp), arrayConcatNode));
                    } else {
                        comparisons.add(new RubyCallNode(context, sourceSection, "===", rubyExpression, null, false, true, NodeUtil.cloneNode(readTemp)));
                    }
                }

                RubyNode conditionNode = comparisons.get(comparisons.size() - 1);

                // As with the if nodes, we work backwards to make it left associative

                for (int i = comparisons.size() - 2; i >= 0; i--) {
                    conditionNode = new OrNode(context, sourceSection, comparisons.get(i), conditionNode);
                }

                // Create the if node

                final RubyNode thenNode = translateNodeOrNil(sourceSection, when.getBodyNode());

                final IfNode ifNode = new IfNode(context, sourceSection, conditionNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            final RubyNode ifNode = elseNode;

            // A top-level block assigns the temp then runs the if

            ret = SequenceNode.sequence(context, sourceSection, assignTemp, ifNode);
        } else {
            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final org.jruby.ast.WhenNode when = (org.jruby.ast.WhenNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<org.jruby.ast.Node> expressions;

                if (when.getExpressionNodes() instanceof org.jruby.ast.ListNode) {
                    expressions = when.getExpressionNodes().childNodes();
                } else {
                    expressions = Arrays.asList(when.getExpressionNodes());
                }

                final List<RubyNode> tests = new ArrayList<>();

                for (org.jruby.ast.Node expressionNode : expressions) {
                    final RubyNode rubyExpression = expressionNode.accept(this);
                    tests.add(rubyExpression);
                }

                RubyNode conditionNode = tests.get(tests.size() - 1);

                // As with the if nodes, we work backwards to make it left associative

                for (int i = tests.size() - 2; i >= 0; i--) {
                    conditionNode = new OrNode(context, sourceSection, tests.get(i), conditionNode);
                }

                // Create the if node

                final RubyNode thenNode = when.getBodyNode().accept(this);

                final IfNode ifNode = new IfNode(context, sourceSection, conditionNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            ret = elseNode;
        }

        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode openModule(SourceSection sourceSection, RubyNode defineOrGetNode, String name, Node bodyNode) {
        LexicalScope newLexicalScope = environment.pushLexicalScope();
        try {
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, newLexicalScope, Arity.NO_ARGUMENTS, name, false, null, false);

            final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParseEnvironment(),
                    environment.getParseEnvironment().allocateReturnID(), true, true, sharedMethodInfo, name, false, null);

            final ModuleTranslator classTranslator = new ModuleTranslator(currentNode, context, this, newEnvironment, source);

            final MethodDefinitionNode definitionMethod = classTranslator.compileClassNode(sourceSection, name, bodyNode);

            return new OpenModuleNode(context, sourceSection, defineOrGetNode, definitionMethod, newLexicalScope);
        } finally {
            environment.popLexicalScope();
        }
    }

    @Override
    public RubyNode visitClassNode(org.jruby.ast.ClassNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getCPath().getName();

        RubyNode lexicalParent = translateCPath(sourceSection, node.getCPath());

        RubyNode superClass;
        if (node.getSuperNode() != null) {
            superClass = node.getSuperNode().accept(this);
        } else {
            superClass = new LiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        }

        final DefineOrGetClassNode defineOrGetClass = new DefineOrGetClassNode(context, sourceSection, name, lexicalParent, superClass);

        final RubyNode ret = openModule(sourceSection, defineOrGetClass, name, node.getBodyNode());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitClassVarAsgnNode(org.jruby.ast.ClassVarAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode rhs = node.getValueNode().accept(this);
        final RubyNode ret = new WriteClassVariableNode(context, sourceSection, node.getName(), environment.getLexicalScope(), rhs);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitClassVarNode(org.jruby.ast.ClassVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new ReadClassVariableNode(context, sourceSection, node.getName(), environment.getLexicalScope());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitColon2Node(org.jruby.ast.Colon2Node node) {
        // Qualified constant access, as in Mod::CONST
        if (!(node instanceof Colon2ConstNode)) {
            throw new UnsupportedOperationException(node.toString());
        }

        final SourceSection sourceSection = translate(node.getPosition());
        final String name = ConstantReplacer.replacementName(sourceSection, node.getName());

        final RubyNode lhs = node.getLeftNode().accept(this);

        final RubyNode ret = new ReadLiteralConstantNode(context, sourceSection, lhs, name);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitColon3Node(org.jruby.ast.Colon3Node node) {
        // Root namespace constant access, as in ::Foo

        final SourceSection sourceSection = translate(node.getPosition());
        final String name = ConstantReplacer.replacementName(sourceSection, node.getName());

        final LiteralNode root = new LiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());

        final RubyNode ret = new ReadLiteralConstantNode(context, sourceSection, root, name);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateCPath(SourceSection sourceSection, org.jruby.ast.Colon3Node node) {
        final RubyNode ret;

        if (node instanceof Colon2ImplicitNode) { // use current lexical scope
            ret = new LexicalScopeNode(context, sourceSection, environment.getLexicalScope());
        } else if (node instanceof Colon2ConstNode) { // A::B
            ret = node.childNodes().get(0).accept(this);
        } else { // Colon3Node: on top-level (Object)
            ret = new LiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitComplexNode(ComplexNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode ret = translateRationalComplex(sourceSection, "Complex",
                new FixnumLiteralNode.IntegerFixnumLiteralNode(context, sourceSection, 0),
                node.getNumber().accept(this));

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitConstDeclNode(org.jruby.ast.ConstDeclNode node) {
        final RubyNode ret = visitConstDeclNode(node, node.getValueNode().accept(this));
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode visitConstDeclNode(org.jruby.ast.ConstDeclNode node, RubyNode rhs) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode moduleNode;
        Node constNode = node.getConstNode();
        if (constNode == null || constNode instanceof Colon2ImplicitNode) {
            moduleNode = new LexicalScopeNode(context, sourceSection, environment.getLexicalScope());
        } else if (constNode instanceof Colon2ConstNode) {
            constNode = ((Colon2Node) constNode).getLeftNode(); // Misleading doc, we only want the defined part.
            moduleNode = constNode.accept(this);
        } else if (constNode instanceof Colon3Node) {
            moduleNode = new LiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        } else {
            throw new UnsupportedOperationException();
        }

        final RubyNode ret = new WriteConstantNode(context, sourceSection, node.getName(), moduleNode, rhs);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitConstNode(org.jruby.ast.ConstNode node) {
        // Unqualified constant access, as in CONST
        final SourceSection sourceSection = translate(node.getPosition());

        /*
         * Constants of the form Rubinius::Foo in the Rubinius kernel code always seem to get resolved, even if
         * Rubinius is not defined, such as in BasicObject. We get around this by translating Rubinius to be
         * ::Rubinius. Note that this isn't quite what Rubinius does, as they say that Rubinius isn't defined, but
         * we will because we'll translate that to ::Rubinius. But it is a simpler translation.
         */

        final String name = ConstantReplacer.replacementName(sourceSection, node.getName());

        if (name.equals("Rubinius") && sourceSection.getSource().getPath().startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius")) {
            final RubyNode ret = new org.jruby.ast.Colon3Node(node.getPosition(), name).accept(this);
            return addNewlineIfNeeded(node, ret);
        }

        final LexicalScope lexicalScope = environment.getLexicalScope();
        final RubyNode ret = new ReadConstantWithLexicalScopeNode(context, sourceSection, lexicalScope, name);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDAsgnNode(org.jruby.ast.DAsgnNode node) {
        final RubyNode ret = new org.jruby.ast.LocalAsgnNode(node.getPosition(), node.getName(), node.getDepth(), node.getValueNode()).accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDRegxNode(org.jruby.ast.DRegexpNode node) {
        SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> children = new ArrayList<>();

        for (org.jruby.ast.Node child : node.children()) {
            children.add(child.accept(this));
        }

        final InterpolatedRegexpNode i = new InterpolatedRegexpNode(context, sourceSection, children.toArray(new RubyNode[children.size()]), node.getOptions());

        if (node.getOptions().isOnce()) {
            final RubyNode ret = new OnceNode(context, i.getEncapsulatingSourceSection(), i);
            return addNewlineIfNeeded(node, ret);
        }

        return addNewlineIfNeeded(node, i);
    }

    @Override
    public RubyNode visitDStrNode(org.jruby.ast.DStrNode node) {
        final RubyNode ret = translateInterpolatedString(translate(node.getPosition()), node.children());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDSymbolNode(org.jruby.ast.DSymbolNode node) {
        SourceSection sourceSection = translate(node.getPosition());

        final RubyNode stringNode = translateInterpolatedString(sourceSection, node.children());

        final RubyNode ret = StringToSymbolNodeGen.create(context, sourceSection, stringNode);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateInterpolatedString(SourceSection sourceSection, org.jruby.ast.Node[] childNodes) {
        final ToSNode[] children = new ToSNode[childNodes.length];

        for (int i = 0; i < childNodes.length; i++) {
            children[i] = ToSNodeGen.create(context, sourceSection, childNodes[i].accept(this));
        }

        return new InterpolatedStringNode(context, sourceSection, children);
    }

    @Override
    public RubyNode visitDVarNode(org.jruby.ast.DVarNode node) {
        RubyNode readNode = environment.findLocalVarNode(node.getName(), translate(node.getPosition()));

        if (readNode == null) {
            // If we haven't seen this dvar before it's possible that it's a block local variable

            final int depth = node.getDepth();

            TranslatorEnvironment e = environment;

            for (int n = 0; n < depth; n++) {
                e = e.getParent();
            }

            e.declareVar(node.getName());

            // Searching for a local variable must start at the base environment, even though we may have determined
            // the variable should be declared in a parent frame descriptor.  This is so the search can determine
            // whether to return a ReadLocalVariableNode or a ReadDeclarationVariableNode and potentially record the
            // fact that a declaration frame is needed.
            readNode = environment.findLocalVarNode(node.getName(), translate(node.getPosition()));
        }

        return addNewlineIfNeeded(node, readNode);
    }

    @Override
    public RubyNode visitDXStrNode(org.jruby.ast.DXStrNode node) {
        final org.jruby.ast.DStrNode string = new org.jruby.ast.DStrNode(node.getPosition(), node.getEncoding());
        string.addAll(node);
        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), string);
        final org.jruby.ast.Node callNode = new FCallNode(node.getPosition(), "`", argsNode, null);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefinedNode(org.jruby.ast.DefinedNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode ret = new DefinedNode(context, sourceSection, node.getExpressionNode().accept(this));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefnNode(org.jruby.ast.DefnNode node) {
        final SourceSection sourceSection = translate(node.getPosition(), node.getName());
        final RubyNode classNode;

        if (parent == null) {
            /*
             * In the top-level, methods are defined in the class of the main object. This is
             * counter-intuitive - I would have expected them to be defined in the singleton class.
             * Apparently this is a design decision to make top-level methods sort of global.
             *
             * http://stackoverflow.com/questions/1761148/where-are-methods-defined-at-the-ruby-top-level
             */

            // TODO: different for Kernel#load(..., true)
            classNode = new LiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        } else {
            classNode = new SelfNode(context, sourceSection);
        }

        final RubyNode ret = translateMethodDefinition(sourceSection, classNode, node.getName(), node, node.getArgsNode(), node.getBodyNode());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefsNode(org.jruby.ast.DefsNode node) {
        final SourceSection sourceSection = translate(node.getPosition(), node.getName());

        final RubyNode objectNode = node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, objectNode);

        final RubyNode ret = new SetMethodDeclarationContext(context, sourceSection, Visibility.PUBLIC,
                "defs", translateMethodDefinition(sourceSection, singletonClassNode, node.getName(), node, node.getArgsNode(), node.getBodyNode()));

        return addNewlineIfNeeded(node, ret);
    }

    protected RubyNode translateMethodDefinition(SourceSection sourceSection, RubyNode classNode, String methodName, org.jruby.ast.Node parseTree, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode) {
        //

        if (methodName.startsWith("visit_Psych_Nodes_")) {
            methodName = "visit_Truffle_Psych_Nodes_" + methodName.substring("visit_Psych_Nodes_".length());
        }

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, environment.getLexicalScope(), MethodTranslator.getArity(argsNode), methodName, false, Helpers.argsNodeToArgumentDescriptors(parseTree.findFirstChild(ArgsNode.class)), false);

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, environment.getParseEnvironment(), environment.getParseEnvironment().allocateReturnID(), true, true, sharedMethodInfo, methodName, false, null);

        // ownScopeForAssignments is the same for the defined method as the current one.

        final MethodTranslator methodCompiler = new MethodTranslator(currentNode, context, this, newEnvironment, false, source, argsNode);

        final MethodDefinitionNode functionExprNode = methodCompiler.compileMethodNode(sourceSection, methodName, bodyNode, sharedMethodInfo);

        return new AddMethodNode(context, sourceSection, classNode, functionExprNode);
    }

    @Override
    public RubyNode visitDotNode(org.jruby.ast.DotNode node) {
        final RubyNode begin = node.getBeginNode().accept(this);
        final RubyNode end = node.getEndNode().accept(this);
        SourceSection sourceSection = translate(node.getPosition());

        // See RangeNode for why there is a node specifically for creating this one type
        final RubyNode ret = RangeLiteralNodeGen.create(context, sourceSection, node.isExclusive(), begin, end);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEncodingNode(org.jruby.ast.EncodingNode node) {
        SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new LiteralNode(context, sourceSection, EncodingNodes.getEncoding(node.getEncoding()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEnsureNode(org.jruby.ast.EnsureNode node) {
        final RubyNode tryPart = node.getBodyNode().accept(this);
        final RubyNode ensurePart = node.getEnsureNode().accept(this);
        final RubyNode ret = new EnsureNode(context, translate(node.getPosition()), tryPart, ensurePart);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEvStrNode(org.jruby.ast.EvStrNode node) {
        final RubyNode ret;

        if (node.getBody() == null) {
            final SourceSection sourceSection = translate(node.getPosition());
            ret = new LiteralNode(context, sourceSection, Layouts.STRING.createString(context.getCoreLibrary().getStringFactory(), new ByteList(), StringSupport.CR_UNKNOWN, null));
        } else {
            ret = node.getBody().accept(this);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFCallNode(org.jruby.ast.FCallNode node) {
        final org.jruby.ast.Node receiver = new org.jruby.ast.SelfNode(node.getPosition());
        final CallNode callNode = new CallNode(node.getPosition(), receiver, node.getName(), node.getArgsNode(), node.getIterNode());

        final RubyNode ret = visitCallNodeExtraArgument(callNode, null, true, false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFalseNode(org.jruby.ast.FalseNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new BooleanLiteralNode(context, sourceSection, false);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFixnumNode(org.jruby.ast.FixnumNode node) {
        final long value = node.getValue();
        final RubyNode ret;

        if (CoreLibrary.fitsIntoInteger(value)) {
            ret = new FixnumLiteralNode.IntegerFixnumLiteralNode(context, translate(node.getPosition()), (int) value);
        } else {
            ret = new FixnumLiteralNode.LongFixnumLiteralNode(context, translate(node.getPosition()), value);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFlipNode(org.jruby.ast.FlipNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode begin = node.getBeginNode().accept(this);
        final RubyNode end = node.getEndNode().accept(this);

        final FlipFlopStateNode stateNode = createFlipFlopState(sourceSection, 0);

        final RubyNode ret = new FlipFlopNode(context, sourceSection, begin, end, stateNode, node.isExclusive());
        return addNewlineIfNeeded(node, ret);
    }

    protected FlipFlopStateNode createFlipFlopState(SourceSection sourceSection, int depth) {
        final FrameSlot frameSlot = environment.declareVar(environment.allocateLocalTemp("flipflop"));
        environment.getFlipFlopStates().add(frameSlot);

        if (depth == 0) {
            return new LocalFlipFlopStateNode(sourceSection, frameSlot);
        } else {
            return new DeclarationFlipFlopStateNode(sourceSection, depth, frameSlot);
        }
    }

    @Override
    public RubyNode visitFloatNode(org.jruby.ast.FloatNode node) {
        final RubyNode ret = new LiteralNode(context, translate(node.getPosition()), node.getValue());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitForNode(org.jruby.ast.ForNode node) {
        /**
         * A Ruby for-loop, such as:
         *
         * <pre>
         * for x in y
         *     z = x
         *     puts z
         * end
         * </pre>
         *
         * naively desugars to:
         *
         * <pre>
         * y.each do |x|
         *     z = x
         *     puts z
         * end
         * </pre>
         *
         * The main difference is that z is always going to be local to the scope outside the block,
         * so it's a bit more like:
         *
         * <pre>
         * z = nil unless z is already defined
         * y.each do |x|
         *    z = x
         *    puts x
         * end
         * </pre>
         *
         * Which forces z to be defined in the correct scope. The parser already correctly calls z a
         * local, but then that causes us a problem as if we're going to translate to a block we
         * need a formal parameter - not a local variable. My solution to this is to add a
         * temporary:
         *
         * <pre>
         * z = nil unless z is already defined
         * y.each do |temp|
         *    x = temp
         *    z = x
         *    puts x
         * end
         * </pre>
         *
         * We also need that temp because the expression assigned in the for could be index
         * assignment, multiple assignment, or whatever:
         *
         * <pre>
         * for x[0] in y
         *     z = x[0]
         *     puts z
         * end
         * </pre>
         *
         * http://blog.grayproductions.net/articles/the_evils_of_the_for_loop
         * http://stackoverflow.com/questions/3294509/for-vs-each-in-ruby
         *
         * The other complication is that normal locals should be defined in the enclosing scope,
         * unlike a normal block. We do that by setting a flag on this translator object when we
         * visit the new iter, translatingForStatement, which we recognise when visiting an iter
         * node.
         *
         * Finally, note that JRuby's terminology is strange here. Normally 'iter' is a different
         * term for a block. Here, JRuby calls the object being iterated over the 'iter'.
         */

        final String temp = environment.allocateLocalTemp("for");

        final org.jruby.ast.Node receiver = node.getIterNode();

        /*
         * The x in for x in ... is like the nodes in multiple assignment - it has a dummy RHS which
         * we need to replace with our temp. Just like in multiple assignment this is really awkward
         * with the JRuby AST.
         */

        final org.jruby.ast.LocalVarNode readTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);
        final org.jruby.ast.Node forVar = node.getVarNode();
        final org.jruby.ast.Node assignTemp = setRHS(forVar, readTemp);

        final org.jruby.ast.BlockNode bodyWithTempAssign = new org.jruby.ast.BlockNode(node.getPosition());
        bodyWithTempAssign.add(assignTemp);
        bodyWithTempAssign.add(node.getBodyNode());

        final org.jruby.ast.ArgumentNode blockVar = new org.jruby.ast.ArgumentNode(node.getPosition(), temp);
        final org.jruby.ast.ListNode blockArgsPre = new org.jruby.ast.ListNode(node.getPosition(), blockVar);
        final org.jruby.ast.ArgsNode blockArgs = new org.jruby.ast.ArgsNode(node.getPosition(), blockArgsPre, null, null, null, null, null, null);
        final org.jruby.ast.IterNode block = new org.jruby.ast.IterNode(node.getPosition(), blockArgs, node.getScope(), bodyWithTempAssign);

        final CallNode callNode = new CallNode(node.getPosition(), receiver, "each", null, block);

        translatingForStatement = true;
        final RubyNode translated = callNode.accept(this);
        translatingForStatement = false;

        return addNewlineIfNeeded(node, translated);
    }

    private static org.jruby.ast.Node setRHS(org.jruby.ast.Node node, org.jruby.ast.Node rhs) {
        if (node instanceof org.jruby.ast.LocalAsgnNode) {
            final org.jruby.ast.LocalAsgnNode localAsgnNode = (org.jruby.ast.LocalAsgnNode) node;
            return new org.jruby.ast.LocalAsgnNode(node.getPosition(), localAsgnNode.getName(), 0, rhs);
        } else if (node instanceof org.jruby.ast.DAsgnNode) {
            final org.jruby.ast.DAsgnNode dAsgnNode = (org.jruby.ast.DAsgnNode) node;
            return new org.jruby.ast.DAsgnNode(node.getPosition(), dAsgnNode.getName(), 0, rhs);
        } else if (node instanceof MultipleAsgnNode) {
            final MultipleAsgnNode multAsgnNode = (MultipleAsgnNode) node;
            final MultipleAsgnNode newNode = new MultipleAsgnNode(node.getPosition(), multAsgnNode.getPre(), multAsgnNode.getRest(), multAsgnNode.getPost());
            newNode.setValueNode(rhs);
            return newNode;
        } else if (node instanceof org.jruby.ast.InstAsgnNode) {
            final org.jruby.ast.InstAsgnNode instAsgnNode = (org.jruby.ast.InstAsgnNode) node;
            return new org.jruby.ast.InstAsgnNode(node.getPosition(), instAsgnNode.getName(), rhs);
        } else if (node instanceof org.jruby.ast.ClassVarAsgnNode) {
            final org.jruby.ast.ClassVarAsgnNode instAsgnNode = (org.jruby.ast.ClassVarAsgnNode) node;
            return new org.jruby.ast.ClassVarAsgnNode(node.getPosition(), instAsgnNode.getName(), rhs);
        } else if (node instanceof org.jruby.ast.ConstDeclNode) {
            final org.jruby.ast.ConstDeclNode constDeclNode = (org.jruby.ast.ConstDeclNode) node;
            return new org.jruby.ast.ConstDeclNode(node.getPosition(), constDeclNode.getName(), (org.jruby.ast.types.INameNode) constDeclNode.getConstNode(), rhs);
        } else {
            throw new UnsupportedOperationException("Don't know how to set the RHS of a " + node.getClass().getName());
        }
    }

    private final Set<String> readOnlyGlobalVariables = new HashSet<String>();
    private final Map<String, String> globalVariableAliases = new HashMap<String, String>();

    private void initReadOnlyGlobalVariables() {
        Set<String> s = readOnlyGlobalVariables;
        s.add("$:");
        s.add("$LOAD_PATH");
        s.add("$-I");
        s.add("$\"");
        s.add("$LOADED_FEATURES");
        s.add("$<");
        s.add("$FILENAME");
        s.add("$?");
        s.add("$-a");
        s.add("$-l");
        s.add("$-p");
        s.add("$!");
    }

    private void initGlobalVariableAliases() {
        Map<String, String> m = globalVariableAliases;
        m.put("$-I", "$LOAD_PATH");
        m.put("$:", "$LOAD_PATH");
        m.put("$-d", "$DEBUG");
        m.put("$-v", "$VERBOSE");
        m.put("$-w", "$VERBOSE");
        m.put("$-0", "$/");
        m.put("$RS", "$/");
        m.put("$INPUT_RECORD_SEPARATOR", "$/");
        m.put("$>", "$stdout");
        m.put("$PROGRAM_NAME", "$0");
    }

    @Override
    public RubyNode visitGlobalAsgnNode(org.jruby.ast.GlobalAsgnNode node) {
        final RubyNode ret = translateGlobalAsgnNode(node, node.getValueNode().accept(this));
        return addNewlineIfNeeded(node, ret);
    }

    public RubyNode translateGlobalAsgnNode(org.jruby.ast.GlobalAsgnNode node, RubyNode rhs) {
        final SourceSection sourceSection = translate(node.getPosition());

        String name = node.getName();

        if (globalVariableAliases.containsKey(name)) {
            name = globalVariableAliases.get(name);
        }

        if (name.equals("$~")) {
            rhs = new CheckMatchVariableTypeNode(context, sourceSection, rhs);
        } else if (name.equals("$0")) {
            rhs = new CheckProgramNameVariableTypeNode(context, sourceSection, rhs);
        } else if (name.equals("$/")) {
            rhs = new CheckRecordSeparatorVariableTypeNode(context, sourceSection, rhs);
        } else if (name.equals("$,")) {
            rhs = new CheckOutputSeparatorVariableTypeNode(context, sourceSection, rhs);
        } else if (name.equals("$_")) {
            rhs = WrapInThreadLocalNodeGen.create(context, sourceSection, rhs);
        } else if (name.equals("$stdout")) {
            rhs = new CheckStdoutVariableTypeNode(context, sourceSection, rhs);
        } else if (name.equals("$VERBOSE")) {
            rhs = new UpdateVerbosityNode(context, sourceSection, rhs);
        } else if (name.equals("$@")) {
            // $@ is a special-case and doesn't write directly to an ivar field in the globals object.
            // Instead, it writes to the backtrace field of the thread-local $! value.
            return new UpdateLastBacktraceNode(context, sourceSection, rhs);
        }

        final boolean inCore = rhs.getSourceSection().getSource().getPath().startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/");
        if (!inCore && readOnlyGlobalVariables.contains(name)) {
            return new WriteReadOnlyGlobalNode(context, sourceSection, name, rhs);
        }

        if (THREAD_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            final ThreadLocalObjectNode threadLocalVariablesObjectNode = new ThreadLocalObjectNode(context, sourceSection);
            return new WriteInstanceVariableNode(context, sourceSection, name, threadLocalVariablesObjectNode, rhs, true);
        } else if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            if (environment.getNeverAssignInParentScope()) {
                environment.declareVar(name);
            }

            RubyNode localVarNode = environment.findLocalVarNode(node.getName(), sourceSection);

            if (localVarNode == null) {
                if (environment.hasOwnScopeForAssignments()) {
                    environment.declareVar(node.getName());
                }

                TranslatorEnvironment environmentToDeclareIn = environment;

                while (!environmentToDeclareIn.hasOwnScopeForAssignments()) {
                    environmentToDeclareIn = environmentToDeclareIn.getParent();
                }

                environmentToDeclareIn.declareVar(node.getName());
                localVarNode = environment.findLocalVarNode(node.getName(), sourceSection);

                if (localVarNode == null) {
                    throw new RuntimeException("shouldn't be here");
                }
            }

            RubyNode assignment = ((ReadNode) localVarNode).makeWriteNode(rhs);

            if (name.equals("$_")) {
                assignment = GetFromThreadLocalNodeGen.create(context, sourceSection, assignment);
            }

            return assignment;
        } else {
            final LiteralNode globalVariablesObjectNode = new LiteralNode(context, sourceSection, context.getCoreLibrary().getGlobalVariablesObject());
            return new WriteInstanceVariableNode(context, sourceSection, name, globalVariablesObjectNode, rhs, true);

        }
    }

    @Override
    public RubyNode visitGlobalVarNode(org.jruby.ast.GlobalVarNode node) {
        String name = node.getName();

        if (globalVariableAliases.containsKey(name)) {
            name = globalVariableAliases.get(name);
        }

        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret;

        if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            // Assignment is implicit for many of these, so we need to declare when we use

            environment.declareVarWhereAllowed(name);

            RubyNode readNode = environment.findLocalVarNode(name, sourceSection);

            if (name.equals("$_")) {
                if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/regexp.rb")) {
                    readNode = new RubiniusLastStringReadNode(context, sourceSection);
                } else {
                    readNode = GetFromThreadLocalNodeGen.create(context, sourceSection, readNode);
                }
            }

            ret = readNode;
        } else if (THREAD_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            final ThreadLocalObjectNode threadLocalVariablesObjectNode = new ThreadLocalObjectNode(context, sourceSection);
            ret = new ReadInstanceVariableNode(context, sourceSection, name, threadLocalVariablesObjectNode, true);
        } else if (name.equals("$@")) {
            // $@ is a special-case and doesn't read directly from an ivar field in the globals object.
            // Instead, it reads the backtrace field of the thread-local $! value.
            ret = new ReadLastBacktraceNode(context, sourceSection);
        } else {
            final LiteralNode globalVariablesObjectNode = new LiteralNode(context, sourceSection, context.getCoreLibrary().getGlobalVariablesObject());
            ret = new ReadInstanceVariableNode(context, sourceSection, name, globalVariablesObjectNode, true);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitHashNode(org.jruby.ast.HashNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> hashConcats = new ArrayList<>();

        final List<RubyNode> keyValues = new ArrayList<>();

        for (KeyValuePair<Node, Node> pair: node.getPairs()) {
            if (pair.getKey() == null) {
                final RubyNode hashLiteralSoFar = HashLiteralNode.create(context, translate(node.getPosition()), keyValues.toArray(new RubyNode[keyValues.size()]));
                hashConcats.add(hashLiteralSoFar);
                hashConcats.add(HashCastNodeGen.create(context, sourceSection, pair.getValue().accept(this)));
                keyValues.clear();
            } else {
                keyValues.add(pair.getKey().accept(this));

                if (pair.getValue() == null) {
                    keyValues.add(nilNode(sourceSection));
                } else {
                    keyValues.add(pair.getValue().accept(this));
                }
            }
        }

        final RubyNode hashLiteralSoFar = HashLiteralNode.create(context, translate(node.getPosition()), keyValues.toArray(new RubyNode[keyValues.size()]));
        hashConcats.add(hashLiteralSoFar);

        if (hashConcats.size() == 1) {
            final RubyNode ret = hashConcats.get(0);
            return addNewlineIfNeeded(node, ret);
        }

        final RubyNode ret = new ConcatHashLiteralNode(context, sourceSection, hashConcats.toArray(new RubyNode[hashConcats.size()]));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitIfNode(org.jruby.ast.IfNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        org.jruby.ast.Node thenBody = node.getThenBody();

        if (thenBody == null || thenBody.isNil()) {
            thenBody = new org.jruby.ast.NilNode(node.getPosition());
        }

        org.jruby.ast.Node elseBody = node.getElseBody();

        if (elseBody == null || elseBody.isNil()) {
            elseBody = new org.jruby.ast.NilNode(node.getPosition());
        }

        final RubyNode condition = translateNodeOrNil(sourceSection, node.getCondition());

        final RubyNode thenBodyTranslated = thenBody.accept(this);
        final RubyNode elseBodyTranslated = elseBody.accept(this);

        final RubyNode ret = new IfNode(context, sourceSection, condition, thenBodyTranslated, elseBodyTranslated);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitInstAsgnNode(org.jruby.ast.InstAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final String name = node.getName();

        final RubyNode rhs;
        if (node.getValueNode() == null) {
            rhs = new DeadNode(context, sourceSection, new Exception("null RHS of instance variable assignment"));
        } else {
            rhs = node.getValueNode().accept(this);
        }

        // Every case will use a SelfNode, just don't it use more than once.
        // Also note the check for frozen.
        final RubyNode self = new RaiseIfFrozenNode(new SelfNode(context, sourceSection));

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/time.rb")) {
            if (name.equals("@is_gmt")) {
                final RubyNode ret = TimeNodesFactory.InternalSetGMTNodeFactory.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@offset")) {
                final RubyNode ret = TimeNodesFactory.InternalSetOffsetNodeFactory.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/hash.rb")) {
            if (name.equals("@default")) {
                final RubyNode ret = HashNodesFactory.SetDefaultValueNodeFactory.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@default_proc")) {
                final RubyNode ret = HashNodesFactory.SetDefaultProcNodeFactory.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/bootstrap/string.rb") ||
                sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/string.rb")) {

            if (name.equals("@hash")) {
                final RubyNode ret = StringNodesFactory.ModifyBangNodeFactory.create(context, sourceSection, new RubyNode[]{});
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/range.rb")) {
            if (name.equals("@begin")) {
                final RubyNode ret = RangeNodesFactory.InternalSetBeginNodeGen.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@end")) {
                final RubyNode ret = RangeNodesFactory.InternalSetEndNodeGen.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@excl")) {
                final RubyNode ret = RangeNodesFactory.InternalSetExcludeEndNodeGen.create(context, sourceSection, self, rhs);
                return addNewlineIfNeeded(node, ret);
            }
        }

        // TODO (pitr 08-Aug-2015): values of predefined OM properties should be casted to defined types automatically
        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/io.rb")) {
            if (name.equals("@start") || name.equals("@used") || name.equals("@total") || name.equals("@lineno")) {
                // Cast int-fitting longs back to int
                return addNewlineIfNeeded(
                        node,
                        new WriteInstanceVariableNode(context, sourceSection, name, self,
                                IntegerCastNodeGen.create(context, sourceSection, rhs),
                                false));
            }
        }

        final RubyNode ret = new WriteInstanceVariableNode(context, sourceSection, name, self, rhs, false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitInstVarNode(org.jruby.ast.InstVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final String name = node.getName();

        // About every case will use a SelfNode, just don't it use more than once.
        final SelfNode self = new SelfNode(context, sourceSection);

        /*
         * Rubinius uses the instance variable @total to store the size of an array. In order to use code that
         * expects that we'll replace it statically with a call to Array#size. We also replace @tuple with
         * self, and @start to be 0.
         */

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/array.rb") ||
                sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/api/shims/array.rb")) {

            if (name.equals("@total")) {
                final RubyNode ret = new RubyCallNode(context, sourceSection, "size", self, null, false);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@tuple")) {
                final RubyNode ret = self;
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@start")) {
                final RubyNode ret = new FixnumLiteralNode.IntegerFixnumLiteralNode(context, sourceSection, 0);
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/regexp.rb")) {
            if (name.equals("@source")) {
                final RubyNode ret = MatchDataNodesFactory.RubiniusSourceNodeGen.create(context, sourceSection, self);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@full")) {
                // Delegate to MatchDatat#full, in shims.
                final RubyNode ret = new RubyCallNode(context, sourceSection, "full", self, null, false);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@regexp")) {
                final RubyNode ret = MatchDataNodesFactory.RegexpNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@names")) {
                final RubyNode ret = RegexpNodesFactory.RubiniusNamesNodeGen.create(context, sourceSection, self);
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/bootstrap/string.rb") ||
                sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/string.rb")) {

            if (name.equals("@num_bytes")) {
                final RubyNode ret = StringNodesFactory.ByteSizeNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@data")) {
                final RubyNode bytes = StringNodesFactory.BytesNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                // Wrap in a StringData instance, see shims.
                LiteralNode stringDataClass = new LiteralNode(context, sourceSection, context.getCoreLibrary().getStringDataClass());
                final RubyNode ret = new RubyCallNode(context, sourceSection, "new", stringDataClass, null, false, bytes);
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/time.rb")) {
            if (name.equals("@is_gmt")) {
                final RubyNode ret = TimeNodesFactory.InternalGMTNodeFactory.create(context, sourceSection, self);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@offset")) {
                final RubyNode ret = TimeNodesFactory.InternalOffsetNodeFactory.create(context, sourceSection, self);
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/hash.rb")) {
            if (name.equals("@default")) {
                final RubyNode ret = HashNodesFactory.DefaultValueNodeFactory.create(context, sourceSection, self);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@default_proc")) {
                final RubyNode ret = HashNodesFactory.DefaultProcNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@size")) {
                final RubyNode ret = HashNodesFactory.SizeNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            }
        }

        if (sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/common/range.rb") ||
                sourceSection.getSource().getPath().equals(context.getCoreLibrary().getCoreLoadPath() + "/core/rubinius/api/shims/range.rb")) {

            if (name.equals("@begin")) {
                final RubyNode ret = RangeNodesFactory.BeginNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@end")) {
                final RubyNode ret = RangeNodesFactory.EndNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@excl")) {
                final RubyNode ret = RangeNodesFactory.ExcludeEndNodeFactory.create(context, sourceSection, new RubyNode[] { self });
                return addNewlineIfNeeded(node, ret);
            }
        }

        final RubyNode ret = new ReadInstanceVariableNode(context, sourceSection, name, self, false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitIterNode(org.jruby.ast.IterNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        /*
         * In a block we do NOT allocate a new return ID - returns will return from the method, not
         * the block (in the general case, see Proc and the difference between Proc and Lambda for
         * specifics).
         */

        final boolean hasOwnScope = !translatingForStatement;

        org.jruby.ast.ArgsNode argsNode;

        if (node.getVarNode() instanceof org.jruby.ast.ArgsNode) {
            argsNode = (org.jruby.ast.ArgsNode) node.getVarNode();
        } else if (node.getVarNode() instanceof org.jruby.ast.DAsgnNode) {
            final org.jruby.ast.ArgumentNode arg = new org.jruby.ast.ArgumentNode(node.getPosition(), ((org.jruby.ast.DAsgnNode) node.getVarNode()).getName());
            final org.jruby.ast.ListNode preArgs = new org.jruby.ast.ArrayNode(node.getPosition(), arg);
            argsNode = new org.jruby.ast.ArgsNode(node.getPosition(), preArgs, null, null, null, null, null, null);
        } else if (node.getVarNode() == null) {
            argsNode = null;
        } else {
            throw new UnsupportedOperationException();
        }

        // Unset this flag for any for any blocks within the for statement's body
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, environment.getLexicalScope(), MethodTranslator.getArity(argsNode), currentCallMethodName, true, Helpers.argsNodeToArgumentDescriptors(node.findFirstChild(ArgsNode.class)), false);

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, environment.getParseEnvironment(), environment.getReturnID(), hasOwnScope, false,
                sharedMethodInfo, environment.getNamedMethodName(), true, environment.getParseEnvironment().allocateBreakID());
        final MethodTranslator methodCompiler = new MethodTranslator(currentNode, context, this, newEnvironment, true, source, argsNode);
        methodCompiler.translatingForStatement = translatingForStatement;

        if (translatingForStatement && useClassVariablesAsIfInClass) {
            methodCompiler.useClassVariablesAsIfInClass = true;
        }

        final RubyNode ret = methodCompiler.compileBlockNode(translate(node.getPosition()), sharedMethodInfo.getName(), node.getBodyNode(), sharedMethodInfo, Type.PROC);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitLocalAsgnNode(org.jruby.ast.LocalAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        if (environment.getNeverAssignInParentScope()) {
            environment.declareVar(node.getName());
        }

        RubyNode lhs = environment.findLocalVarNode(node.getName(), sourceSection);

        if (lhs == null) {
            if (environment.hasOwnScopeForAssignments()) {
                environment.declareVar(node.getName());
            } else {
                TranslatorEnvironment environmentToDeclareIn = environment;

                while (!environmentToDeclareIn.hasOwnScopeForAssignments()) {
                    environmentToDeclareIn = environmentToDeclareIn.getParent();
                }

                environmentToDeclareIn.declareVar(node.getName());
            }

            lhs = environment.findLocalVarNode(node.getName(), sourceSection);

            if (lhs == null) {
                throw new RuntimeException("shouldn't be here");
            }
        }

        RubyNode rhs;

        if (node.getValueNode() == null) {
            rhs = new DeadNode(context, sourceSection, new Exception());
        } else {
            if (node.getValueNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                parentSourceSection.push(sourceSection);
            }

            try {
                rhs = node.getValueNode().accept(this);
            } finally {
                if (node.getValueNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                    parentSourceSection.pop();
                }
            }
        }

        final RubyNode ret = ((ReadNode) lhs).makeWriteNode(rhs);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitLocalVarNode(org.jruby.ast.LocalVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getName();

        RubyNode readNode = environment.findLocalVarNode(name, sourceSection);

        if (readNode == null) {
            /*

              This happens for code such as:

                def destructure4r((*c,d))
                    [c,d]
                end

               We're going to just assume that it should be there and add it...
             */

            environment.declareVar(node.getName());
            readNode = environment.findLocalVarNode(name, sourceSection);
        }

        return addNewlineIfNeeded(node, readNode);
    }

    @Override
    public RubyNode visitMatchNode(org.jruby.ast.MatchNode node) {
        // Triggered when a Regexp literal is used as a conditional's value.

        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), new org.jruby.ast.GlobalVarNode(node.getPosition(), "$_"));
        final org.jruby.ast.Node callNode = new CallNode(node.getPosition(), node.getRegexpNode(), "=~", argsNode, null);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMatch2Node(org.jruby.ast.Match2Node node) {
        // Triggered when a Regexp literal is the LHS of an expression.

        if (node.getReceiverNode() instanceof org.jruby.ast.RegexpNode) {
            final org.jruby.ast.RegexpNode regexpNode = (org.jruby.ast.RegexpNode) node.getReceiverNode();
            final Regex regex = new Regex(regexpNode.getValue().bytes(), 0, regexpNode.getValue().length(), regexpNode.getOptions().toOptions(), regexpNode.getEncoding(), Syntax.RUBY);

            if (regex.numberOfNames() > 0) {
                for (Iterator<NameEntry> i = regex.namedBackrefIterator(); i.hasNext(); ) {
                    final NameEntry e = i.next();
                    final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();

                    if (environment.hasOwnScopeForAssignments()) {
                        environment.declareVar(name);
                    } else {
                        TranslatorEnvironment environmentToDeclareIn = environment;

                        while (!environmentToDeclareIn.hasOwnScopeForAssignments()) {
                            environmentToDeclareIn = environmentToDeclareIn.getParent();
                        }

                        environmentToDeclareIn.declareVar(name);
                    }
                }
            }
        }

        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final org.jruby.ast.Node callNode = new CallNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMatch3Node(org.jruby.ast.Match3Node node) {
        // Triggered when a Regexp literal is the RHS of an expression.

        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final org.jruby.ast.Node callNode = new CallNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitModuleNode(org.jruby.ast.ModuleNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getCPath().getName();

        RubyNode lexicalParent = translateCPath(sourceSection, node.getCPath());

        final DefineOrGetModuleNode defineModuleNode = new DefineOrGetModuleNode(context, sourceSection, name, lexicalParent);

        final RubyNode ret = openModule(sourceSection, defineModuleNode, name, node.getBodyNode());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.ListNode preArray = node.getPre();
        final org.jruby.ast.ListNode postArray = node.getPost();
        final org.jruby.ast.Node rhs = node.getValueNode();

        RubyNode rhsTranslated;

        if (rhs == null) {
            context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, source.getName(), node.getPosition().getLine(), "no RHS for multiple assignment - using nil");
            rhsTranslated = nilNode(sourceSection);
        } else {
            rhsTranslated = rhs.accept(this);
        }

        final RubyNode result;

        if (preArray != null
                && node.getPost() == null
                && node.getRest() == null
                && rhsTranslated instanceof ArrayLiteralNode.UninitialisedArrayLiteralNode
                && ((ArrayLiteralNode.UninitialisedArrayLiteralNode) rhsTranslated).getValues().length == preArray.size()) {
            /*
             * We can deal with this common case be rewriting as
             *
             * temp1 = c; temp2 = d; a = temp1; b = temp2
             *
             * We can't just do
             *
             * a = c; b = d
             *
             * As we don't know if d depends on the original value of a.
             *
             * We also need to return an array [c, d], but we make that result elidable so it isn't
             * executed if it isn't actually demanded.
             */

            final RubyNode[] rhsValues = ((ArrayLiteralNode.UninitialisedArrayLiteralNode) rhsTranslated).getValues();
            final int assignedValuesCount = preArray.size();

            final RubyNode[] sequence = new RubyNode[assignedValuesCount * 2];

            final RubyNode[] tempValues = new RubyNode[assignedValuesCount];

            for (int n = 0; n < assignedValuesCount; n++) {
                final String tempName = environment.allocateLocalTemp("multi");
                final RubyNode readTemp = environment.findLocalVarNode(tempName, sourceSection);
                final RubyNode assignTemp = ((ReadNode) NodeUtil.cloneNode(readTemp)).makeWriteNode(rhsValues[n]);
                final RubyNode assignFinalValue = translateDummyAssignment(preArray.get(n), NodeUtil.cloneNode(readTemp));

                sequence[n] = assignTemp;
                sequence[assignedValuesCount + n] = assignFinalValue;

                tempValues[n] = NodeUtil.cloneNode(readTemp);
            }

            final RubyNode blockNode = SequenceNode.sequence(context, sourceSection, sequence);

            final ArrayLiteralNode.UninitialisedArrayLiteralNode arrayNode = new ArrayLiteralNode.UninitialisedArrayLiteralNode(context, sourceSection, tempValues);

            final ElidableResultNode elidableResult = new ElidableResultNode(context, sourceSection, blockNode, arrayNode);

            result = elidableResult;
        } else if (preArray != null) {
            /*
             * The other simple case is
             *
             * a, b, c = x
             *
             * If x is an array, then it's
             *
             * a[0] = x[0] etc
             *
             * If x isn't an array then it's
             *
             * a, b, c = [x, nil, nil]
             *
             * Which I believe is the same effect as
             *
             * a, b, c, = *x
             *
             * So we insert the splat cast node, even though it isn't there.
             *
             * In either case, we return the RHS
             */

            final List<RubyNode> sequence = new ArrayList<>();

            /*
             * Store the RHS in a temp.
             */

            final String tempRHSName = environment.allocateLocalTemp("rhs");
            final RubyNode writeTempRHS = ((ReadNode) environment.findLocalVarNode(tempRHSName, sourceSection)).makeWriteNode(rhsTranslated);
            sequence.add(writeTempRHS);

            /*
             * Create a temp for the array.
             */

            final String tempName = environment.allocateLocalTemp("array");

            /*
             * Create a sequence of instructions, with the first being the literal array assigned to
             * the temp.
             */

            final RubyNode splatCastNode = SplatCastNodeGen.create(context, sourceSection, translatingNextExpression ? SplatCastNode.NilBehavior.EMPTY_ARRAY : SplatCastNode.NilBehavior.ARRAY_WITH_NIL, false, environment.findLocalVarNode(tempRHSName, sourceSection));

            final RubyNode writeTemp = ((ReadNode) environment.findLocalVarNode(tempName, sourceSection)).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            for (int n = 0; n < preArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), n);

                sequence.add(translateDummyAssignment(preArray.get(n), assignedValue));
            }

            if (node.getRest() != null) {
                final ArrayGetTailNode assignedValue = ArrayGetTailNodeGen.create(context, sourceSection, preArray.size(), environment.findLocalVarNode(tempName, sourceSection));

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            result = new ElidableResultNode(context, sourceSection, SequenceNode.sequence(context, sourceSection, sequence), environment.findLocalVarNode(tempRHSName, sourceSection));
        } else if (node.getPre() == null
                && node.getPost() == null
                && node.getRest() instanceof org.jruby.ast.StarNode) {
            result = rhsTranslated;
        } else if (node.getPre() == null
                && node.getPost() == null
                && node.getRest() != null
                && rhs != null
                && !(rhs instanceof org.jruby.ast.ArrayNode)) {
            /*
             * *a = b
             *
             * >= 1.8, this seems to be the same as:
             *
             * a = *b
             */

            final RubyNode restTranslated = (node.getRest().accept(this)).getNonProxyNode();

            /*
             * Sometimes rest is a corrupt write with no RHS, like in other multiple assignments,
             * and sometimes it is already a read.
             */

            ReadNode restRead;

            if (restTranslated instanceof ReadNode) {
                restRead = (ReadNode) restTranslated;
            } else if (restTranslated instanceof WriteNode) {
                restRead = (ReadNode) ((WriteNode) restTranslated).makeReadNode();
            } else {
                throw new RuntimeException("Unknown form of multiple assignment " + node + " at " + node.getPosition());
            }

            final SplatCastNode rhsSplatCast = SplatCastNodeGen.create(context, sourceSection, translatingNextExpression ? SplatCastNode.NilBehavior.EMPTY_ARRAY : SplatCastNode.NilBehavior.ARRAY_WITH_NIL, false, rhsTranslated);

            result = restRead.makeWriteNode(rhsSplatCast);
        } else if (node.getPre() == null
                && node.getPost() == null
                && node.getRest() != null
                && rhs != null
                && rhs instanceof org.jruby.ast.ArrayNode) {
            /*
             * *a = [b, c]
             *
             * This seems to be the same as:
             *
             * a = [b, c]
             */

            final RubyNode restTranslated = (node.getRest().accept(this)).getNonProxyNode();

            /*
             * Sometimes rest is a corrupt write with no RHS, like in other multiple assignments,
             * and sometimes it is already a read.
             */

            ReadNode restRead;

            if (restTranslated instanceof ReadNode) {
                restRead = (ReadNode) restTranslated;
            } else if (restTranslated instanceof WriteNode) {
                restRead = (ReadNode) ((WriteNode) restTranslated).makeReadNode();
            } else {
                throw new RuntimeException("Unknown form of multiple assignment " + node + " at " + node.getPosition());
            }

            result = restRead.makeWriteNode(rhsTranslated);
        } else if (node.getPre() == null && node.getRest() != null && node.getPost() != null) {
            /*
             * Something like
             *
             *     *a,b = [1, 2, 3, 4]
             */

            // This is very similar to the case with pre and rest, so unify with that

            /*
             * Create a temp for the array.
             */

            final String tempName = environment.allocateLocalTemp("array");

            /*
             * Create a sequence of instructions, with the first being the literal array assigned to
             * the temp.
             */

            final List<RubyNode> sequence = new ArrayList<>();

            final RubyNode splatCastNode = SplatCastNodeGen.create(context, sourceSection, translatingNextExpression ? SplatCastNode.NilBehavior.EMPTY_ARRAY : SplatCastNode.NilBehavior.ARRAY_WITH_NIL, false, rhsTranslated);

            final RubyNode writeTemp = ((ReadNode) environment.findLocalVarNode(tempName, sourceSection)).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            if (node.getRest() != null) {
                final ArrayDropTailNode assignedValue = ArrayDropTailNodeGen.create(context, sourceSection, postArray.size(), environment.findLocalVarNode(tempName, sourceSection));

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            for (int n = 0; n < postArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), -(postArray.size() - n));

                sequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
            }

            result = SequenceNode.sequence(context, sourceSection, sequence);
        } else {
            context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, source.getName(), node.getPosition().getLine(), node + " unknown form of multiple assignment");
            result = nilNode(sourceSection);
        }

        final RubyNode ret = new DefinedWrapperNode(context, sourceSection, result, "assignment");
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateDummyAssignment(org.jruby.ast.Node dummyAssignment, RubyNode rhs) {
        final SourceSection sourceSection = translate(dummyAssignment.getPosition());

        /*
         * This is tricky. To represent the RHS of a multiple assignment they use corrupt assignment
         * values, in some cases with no value to be assigned, and in other cases with a dummy
         * value. We can't visit them normally, as they're corrupt. We can't just modify them to
         * have our RHS, as that's a node in our AST, not theirs. We can't use a dummy value in
         * their AST because I can't add new visitors to this interface.
         */

        RubyNode translated;

        if (dummyAssignment instanceof org.jruby.ast.LocalAsgnNode) {
            /*
             * They have a dummy NilImplicitNode as the RHS. Translate, convert to read, convert to
             * write which allows us to set the RHS.
             */

            final WriteNode dummyTranslated = (WriteNode) (dummyAssignment.accept(this)).getNonProxyNode();
            translated = ((ReadNode) dummyTranslated.makeReadNode()).makeWriteNode(rhs);
        } else if (dummyAssignment instanceof org.jruby.ast.InstAsgnNode) {
            /*
             * Same as before, just a different type of assignment.
             */

            final WriteInstanceVariableNode dummyTranslated = (WriteInstanceVariableNode) dummyAssignment.accept(this);
            translated = ((ReadNode) dummyTranslated.makeReadNode()).makeWriteNode(rhs);
        } else if (dummyAssignment instanceof org.jruby.ast.AttrAssignNode) {
            /*
             * They've given us an AttrAssignNode with the final argument, the assigned value,
             * missing. If we translate that we'll get foo.[]=(index), so missing the value. To
             * solve we have a special version of the visitCallNode that allows us to pass another
             * already translated argument, visitCallNodeExtraArgument. However, we initially have
             * an AttrAssignNode, so we also need a special version of that.
             */

            final org.jruby.ast.AttrAssignNode dummyAttrAssignment = (org.jruby.ast.AttrAssignNode) dummyAssignment;
            translated = visitAttrAssignNodeExtraArgument(dummyAttrAssignment, rhs);
        } else if (dummyAssignment instanceof org.jruby.ast.DAsgnNode) {
            final RubyNode dummyTranslated = dummyAssignment.accept(this);

            if (dummyTranslated.getNonProxyNode() instanceof WriteDeclarationVariableNode) {
                translated = ((ReadNode) ((WriteDeclarationVariableNode) dummyTranslated.getNonProxyNode()).makeReadNode()).makeWriteNode(rhs);
            } else {
                translated = ((ReadNode) ((WriteLocalVariableNode) dummyTranslated.getNonProxyNode()).makeReadNode()).makeWriteNode(rhs);
            }
        } else if (dummyAssignment instanceof org.jruby.ast.GlobalAsgnNode) {
            return translateGlobalAsgnNode((org.jruby.ast.GlobalAsgnNode) dummyAssignment, rhs);
        } else if (dummyAssignment instanceof org.jruby.ast.ConstDeclNode) {
            return visitConstDeclNode((org.jruby.ast.ConstDeclNode) dummyAssignment, rhs);
        } else {
            translated = ((ReadNode) environment.findLocalVarNode(environment.allocateLocalTemp("dummy"), sourceSection)).makeWriteNode(rhs);
        }

        return translated;
    }

    @Override
    public RubyNode visitNextNode(org.jruby.ast.NextNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        if (!environment.isBlock() && !translatingWhile) {
            throw new RaiseException(context.getCoreLibrary().syntaxError("Invalid next", currentNode));
        }

        final RubyNode resultNode;

        final boolean t = translatingNextExpression;
        translatingNextExpression = true;

        if (node.getValueNode().getPosition() == InvalidSourcePosition.INSTANCE) {
            parentSourceSection.push(sourceSection);
        }

        try {
            resultNode = node.getValueNode().accept(this);
        } finally {
            if (node.getValueNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                parentSourceSection.pop();
            }

            translatingNextExpression = t;
        }

        final RubyNode ret = new NextNode(context, sourceSection, resultNode);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitNilNode(org.jruby.ast.NilNode node) {
        if (node.getPosition() == InvalidSourcePosition.INSTANCE && parentSourceSection.peek() == null) {
            final RubyNode ret = new DeadNode(context, null, new Exception());
            return addNewlineIfNeeded(node, ret);
        }

        SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = nilNode(sourceSection);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitNthRefNode(org.jruby.ast.NthRefNode node) {
        final RubyNode ret = new ReadMatchReferenceNode(context, translate(node.getPosition()), node.getMatchNumber());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnAndNode(org.jruby.ast.OpAsgnAndNode node) {
        /*
         * This doesn't translate as you might expect!
         *
         * http://www.rubyinside.com/what-rubys-double-pipe-or-equals-really-does-5488.html
         */

        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.Node lhs = node.getFirstNode();
        final org.jruby.ast.Node rhs = node.getSecondNode();

        final RubyNode ret = new DefinedWrapperNode(context, sourceSection, new AndNode(context, sourceSection, lhs.accept(this), rhs.accept(this)), "assignment");
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnNode(org.jruby.ast.OpAsgnNode node) {
        if (node.getOperatorName().equals("||")) {
            // Why does this ||= come through as a visitOpAsgnNode and not a visitOpAsgnOrNode?

            final String temp = environment.allocateLocalTemp("opassign");
            final org.jruby.ast.Node writeReceiverToTemp = new org.jruby.ast.LocalAsgnNode(node.getPosition(), temp, 0, node.getReceiverNode());
            final org.jruby.ast.Node readReceiverFromTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);

            final org.jruby.ast.Node readMethod = new CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName(), null, null);
            final org.jruby.ast.Node writeMethod = new CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName() + "=", buildArrayNode(node.getPosition(),
                    node.getValueNode()), null);

            final SourceSection sourceSection = translate(node.getPosition());

            RubyNode lhs = readMethod.accept(this);
            RubyNode rhs = writeMethod.accept(this);

            final RubyNode ret = new DefinedWrapperNode(context, sourceSection,
                    SequenceNode.sequence(context, sourceSection, writeReceiverToTemp.accept(this), new OrNode(context, sourceSection, lhs, rhs)),
                    "assignment");

            return addNewlineIfNeeded(node, ret);
        }

        /*
         * We're going to de-sugar a.foo += c into a.foo = a.foo + c. Note that we can't evaluate a
         * more than once, so we put it into a temporary, and we're doing something more like:
         *
         * temp = a; temp.foo = temp.foo + c
         */

        final String temp = environment.allocateLocalTemp("opassign");
        final org.jruby.ast.Node writeReceiverToTemp = new org.jruby.ast.LocalAsgnNode(node.getPosition(), temp, 0, node.getReceiverNode());
        final org.jruby.ast.Node readReceiverFromTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);

        final org.jruby.ast.Node readMethod = new CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName(), null, null);
        final org.jruby.ast.Node operation = new CallNode(node.getPosition(), readMethod, node.getOperatorName(), buildArrayNode(node.getPosition(), node.getValueNode()), null);
        final org.jruby.ast.Node writeMethod = new CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName() + "=", buildArrayNode(node.getPosition(),
                        operation), null);

        final org.jruby.ast.BlockNode block = new org.jruby.ast.BlockNode(node.getPosition());
        block.add(writeReceiverToTemp);
        block.add(writeMethod);

        final RubyNode ret = block.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnOrNode(org.jruby.ast.OpAsgnOrNode node) {
        /*
         * This doesn't translate as you might expect!
         *
         * http://www.rubyinside.com/what-rubys-double-pipe-or-equals-really-does-5488.html
         */

        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode lhs = node.getFirstNode().accept(this);
        RubyNode rhs = node.getSecondNode().accept(this);

        // I think this is only required for constants - not instance variables

        if (node.getFirstNode().needsDefinitionCheck() && !(node.getFirstNode() instanceof org.jruby.ast.InstVarNode)) {
            RubyNode defined = new DefinedNode(context, lhs.getSourceSection(), lhs);
            lhs = new AndNode(context, lhs.getSourceSection(), defined, lhs);
        }

        final RubyNode ret = new DefinedWrapperNode(context, sourceSection,
                new OrNode(context, sourceSection, lhs, rhs),
                "assignment");

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpElementAsgnNode(org.jruby.ast.OpElementAsgnNode node) {
        /*
         * We're going to de-sugar a[b] += c into a[b] = a[b] + c. See discussion in
         * visitOpAsgnNode.
         */

        org.jruby.ast.Node index;

        if (node.getArgsNode() == null) {
            index = null;
        } else {
            index = node.getArgsNode().childNodes().get(0);
        }

        final org.jruby.ast.Node operand = node.getValueNode();

        final String temp = environment.allocateLocalTemp("opelementassign");
        final org.jruby.ast.Node writeArrayToTemp = new org.jruby.ast.LocalAsgnNode(node.getPosition(), temp, 0, node.getReceiverNode());
        final org.jruby.ast.Node readArrayFromTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);

        final org.jruby.ast.Node arrayRead = new CallNode(node.getPosition(), readArrayFromTemp, "[]", buildArrayNode(node.getPosition(), index), null);

        final String op = node.getOperatorName();

        org.jruby.ast.Node operation = null;

        if (op.equals("||")) {
            operation = new org.jruby.ast.OrNode(node.getPosition(), arrayRead, operand);
        } else if (op.equals("&&")) {
            operation = new org.jruby.ast.AndNode(node.getPosition(), arrayRead, operand);
        } else {
            operation = new CallNode(node.getPosition(), arrayRead, node.getOperatorName(), buildArrayNode(node.getPosition(), operand), null);
        }

        final org.jruby.ast.Node arrayWrite = new CallNode(node.getPosition(), readArrayFromTemp, "[]=", buildArrayNode(node.getPosition(), index, operation), null);

        final org.jruby.ast.BlockNode block = new org.jruby.ast.BlockNode(node.getPosition());
        block.add(writeArrayToTemp);
        block.add(arrayWrite);

        final RubyNode ret = block.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    private static org.jruby.ast.ArrayNode buildArrayNode(org.jruby.lexer.yacc.ISourcePosition sourcePosition, org.jruby.ast.Node first, org.jruby.ast.Node... rest) {
        if (first == null) {
            return new org.jruby.ast.ArrayNode(sourcePosition);
        }

        final org.jruby.ast.ArrayNode array = new org.jruby.ast.ArrayNode(sourcePosition, first);

        for (org.jruby.ast.Node node : rest) {
            array.add(node);
        }

        return array;
    }

    @Override
    public RubyNode visitOrNode(org.jruby.ast.OrNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode x = translateNodeOrNil(sourceSection, node.getFirstNode());
        final RubyNode y = translateNodeOrNil(sourceSection, node.getSecondNode());

        final RubyNode ret = new OrNode(context, sourceSection, x, y);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitPreExeNode(PreExeNode node) {
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitPostExeNode(PostExeNode node) {
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRationalNode(RationalNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        // TODO(CS): use IntFixnumLiteralNode where possible

        final RubyNode ret = translateRationalComplex(sourceSection, "Rational",
                new FixnumLiteralNode.LongFixnumLiteralNode(context, sourceSection, node.getNumerator()),
                new FixnumLiteralNode.LongFixnumLiteralNode(context, sourceSection, node.getDenominator()));

        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateRationalComplex(SourceSection sourceSection, String name, RubyNode a, RubyNode b) {
        // Translate as Rubinius.privately { Rational.convert(a, b) }

        final RubyNode moduleNode = new LiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        return new RubyCallNode(
                context, sourceSection, "convert",
                new ReadLiteralConstantNode(context, sourceSection, moduleNode, name),
                null, false, true, new RubyNode[]{a, b});
    }

    @Override
    public RubyNode visitRedoNode(org.jruby.ast.RedoNode node) {
        if (!environment.isBlock() && !translatingWhile) {
            throw new RaiseException(context.getCoreLibrary().syntaxError("Invalid redo", currentNode));
        }

        final RubyNode ret = new RedoNode(context, translate(node.getPosition()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRegexpNode(org.jruby.ast.RegexpNode node) {
        Regex regex = RegexpNodes.compile(currentNode, context, node.getValue(), node.getOptions());

        final DynamicObject regexp = RegexpNodes.createRubyRegexp(context.getCoreLibrary().getRegexpClass(), regex, node.getValue(), node.getOptions());
        Layouts.REGEXP.getOptions(regexp).setLiteral(true);

        final LiteralNode literalNode = new LiteralNode(context, translate(node.getPosition()), regexp);

        if (node.getOptions().isOnce()) {
            final RubyNode ret = new OnceNode(context, literalNode.getEncapsulatingSourceSection(), literalNode);
            return addNewlineIfNeeded(node, ret);
        }

        return addNewlineIfNeeded(node, literalNode);
    }

    public static boolean all7Bit(byte[] bytes) {
        for (int n = 0; n < bytes.length; n++) {
            if (bytes[n] < 0) {
                return false;
            }

            if (bytes[n] == '\\' && n + 1 < bytes.length && bytes[n + 1] == 'x') {
                final String num;
                final boolean isSecondHex = n + 3 < bytes.length && Character.digit(bytes[n + 3], 16) != -1;
                if (isSecondHex) {
                    num = new String(Arrays.copyOfRange(bytes, n + 2, n + 4), StandardCharsets.UTF_8);
                } else {
                    num = new String(Arrays.copyOfRange(bytes, n + 2, n + 3), StandardCharsets.UTF_8);
                }

                int b = Integer.parseInt(num, 16);

                if (b > 0x7F) {
                    return false;
                }

                if (isSecondHex) {
                    n += 3;
                } else {
                    n += 2;
                }

            }
        }

        return true;
    }

    @Override
    public RubyNode visitRescueNode(org.jruby.ast.RescueNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode tryPart;

        if (node.getBodyNode() == null || node.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
            tryPart = nilNode(sourceSection);
        } else {
            tryPart = node.getBodyNode().accept(this);
        }

        final List<RescueNode> rescueNodes = new ArrayList<>();

        org.jruby.ast.RescueBodyNode rescueBody = node.getRescueNode();

        while (rescueBody != null) {
            if (rescueBody.getExceptionNodes() != null) {
                if (rescueBody.getExceptionNodes() instanceof org.jruby.ast.ArrayNode) {
                    final org.jruby.ast.Node[] exceptionNodes = ((org.jruby.ast.ArrayNode) rescueBody.getExceptionNodes()).children();

                    final RubyNode[] handlingClasses = new RubyNode[exceptionNodes.length];

                    for (int n = 0; n < handlingClasses.length; n++) {
                        handlingClasses[n] = exceptionNodes[n].accept(this);
                    }

                    RubyNode translatedBody;

                    if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                        translatedBody = nilNode(sourceSection);
                    } else {
                        translatedBody = rescueBody.getBodyNode().accept(this);
                    }

                    final RescueClassesNode rescueNode = new RescueClassesNode(context, sourceSection, handlingClasses, translatedBody);
                    rescueNodes.add(rescueNode);
                } else if (rescueBody.getExceptionNodes() instanceof org.jruby.ast.SplatNode) {
                    final org.jruby.ast.SplatNode splat = (org.jruby.ast.SplatNode) rescueBody.getExceptionNodes();

                    final RubyNode splatTranslated = translateNodeOrNil(sourceSection, splat.getValue());

                    RubyNode bodyTranslated;

                    if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                        bodyTranslated = nilNode(sourceSection);
                    } else {
                        bodyTranslated = rescueBody.getBodyNode().accept(this);
                    }

                    final RescueSplatNode rescueNode = new RescueSplatNode(context, sourceSection, splatTranslated, bodyTranslated);
                    rescueNodes.add(rescueNode);
                } else {
                    unimplemented(node);
                }
            } else {
                RubyNode bodyNode;

                if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                    bodyNode = nilNode(sourceSection);
                } else {
                    bodyNode = rescueBody.getBodyNode().accept(this);
                }

                final RescueAnyNode rescueNode = new RescueAnyNode(context, sourceSection, bodyNode);
                rescueNodes.add(rescueNode);
            }

            rescueBody = rescueBody.getOptRescueNode();
        }

        RubyNode elsePart;

        if (node.getElseNode() == null || node.getElseNode().getPosition() == InvalidSourcePosition.INSTANCE) {
            elsePart = nilNode(sourceSection);
        } else {
            elsePart = node.getElseNode().accept(this);
        }

        final RubyNode ret = new TryNode(context, sourceSection,
                new ExceptionTranslatingNode(context, sourceSection, tryPart),
                rescueNodes.toArray(new RescueNode[rescueNodes.size()]), elsePart);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRetryNode(org.jruby.ast.RetryNode node) {
        final RubyNode ret = new RetryNode(context, translate(node.getPosition()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitReturnNode(org.jruby.ast.ReturnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode translatedChild = node.getValueNode().accept(this);

        final RubyNode ret = new ReturnNode(context, sourceSection, environment.getReturnID(), translatedChild);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSClassNode(org.jruby.ast.SClassNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode receiverNode = node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, receiverNode);

        final RubyNode ret = openModule(sourceSection, singletonClassNode, "(singleton-def)", node.getBodyNode());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSValueNode(org.jruby.ast.SValueNode node) {
        final RubyNode ret = node.getValue().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSelfNode(org.jruby.ast.SelfNode node) {
        final RubyNode ret = new SelfNode(context, translate(node.getPosition()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSplatNode(org.jruby.ast.SplatNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode value = translateNodeOrNil(sourceSection, node.getValue());
        final RubyNode ret = SplatCastNodeGen.create(context, sourceSection, SplatCastNode.NilBehavior.EMPTY_ARRAY, false, value);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitStrNode(org.jruby.ast.StrNode node) {
        final RubyNode ret = new StringLiteralNode(context, translate(node.getPosition()), node.getValue(), node.getCodeRange());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSymbolNode(org.jruby.ast.SymbolNode node) {
        final ByteList byteList = ByteList.create(node.getName());
        byteList.setEncoding(node.getEncoding());
        final RubyNode ret = new LiteralNode(context, translate(node.getPosition()), context.getSymbol(byteList));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitTrueNode(org.jruby.ast.TrueNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new BooleanLiteralNode(context, sourceSection, true);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitUndefNode(org.jruby.ast.UndefNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final SelfNode classNode = new SelfNode(context, sourceSection);
        final RubyNode ret = new UndefNode(context, sourceSection, classNode, ((org.jruby.ast.LiteralNode) node.getName()).getName());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitUntilNode(org.jruby.ast.UntilNode node) {
        org.jruby.ast.WhileNode whileNode = new org.jruby.ast.WhileNode(node.getPosition(), node.getConditionNode(), node.getBodyNode(), node.evaluateAtStart());
        final RubyNode ret = visitWhileNode(whileNode, true);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitVCallNode(org.jruby.ast.VCallNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        if (node.getName().equals("undefined") && sourceSection.getSource().getPath().startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/")) {
            final RubyNode ret = new LiteralNode(context, sourceSection, context.getCoreLibrary().getRubiniusUndefined());
            return addNewlineIfNeeded(node, ret);
        }

        final org.jruby.ast.Node receiver = new org.jruby.ast.SelfNode(node.getPosition());
        final CallNode callNode = new CallNode(node.getPosition(), receiver, node.getName(), null, null);
        final RubyNode ret = visitCallNodeExtraArgument(callNode, null, true, true);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitWhileNode(org.jruby.ast.WhileNode node) {
        final RubyNode ret = visitWhileNode(node, false);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode visitWhileNode(org.jruby.ast.WhileNode node, boolean conditionInversed) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode condition = node.getConditionNode().accept(this);
        if (conditionInversed) {
            condition = new NotNode(context, sourceSection, condition);
        }

        RubyNode body;
        final BreakID whileBreakID = environment.getParseEnvironment().allocateBreakID();

        final boolean oldTranslatingWhile = translatingWhile;
        translatingWhile = true;
        BreakID oldBreakID = environment.getBreakID();
        environment.setBreakIDForWhile(whileBreakID);
        try {
            body = translateNodeOrNil(sourceSection, node.getBodyNode());
        } finally {
            environment.setBreakIDForWhile(oldBreakID);
            translatingWhile = oldTranslatingWhile;
        }

        final RubyNode loop;

        if (node.evaluateAtStart()) {
            loop = WhileNode.createWhile(context, sourceSection, condition, body);
        } else {
            loop = WhileNode.createDoWhile(context, sourceSection, condition, body);
        }

        final RubyNode ret = new CatchBreakNode(context, sourceSection, loop, whileBreakID);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitXStrNode(org.jruby.ast.XStrNode node) {
        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), new org.jruby.ast.StrNode(node.getPosition(), node.getValue()));
        final org.jruby.ast.Node callNode = new FCallNode(node.getPosition(), "`", argsNode, null);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitYieldNode(org.jruby.ast.YieldNode node) {
        final List<org.jruby.ast.Node> arguments = new ArrayList<>();

        org.jruby.ast.Node argsNode = node.getArgsNode();

        final boolean unsplat = argsNode instanceof org.jruby.ast.SplatNode || argsNode instanceof org.jruby.ast.ArgsCatNode;

        if (argsNode instanceof org.jruby.ast.SplatNode) {
            argsNode = ((org.jruby.ast.SplatNode) argsNode).getValue();
        }

        if (argsNode != null) {
            if (argsNode instanceof org.jruby.ast.ListNode) {
                arguments.addAll((node.getArgsNode()).childNodes());
            } else {
                arguments.add(node.getArgsNode());
            }
        }

        final List<RubyNode> argumentsTranslated = new ArrayList<>();

        for (org.jruby.ast.Node argument : arguments) {
            argumentsTranslated.add(argument.accept(this));
        }

        final RubyNode[] argumentsTranslatedArray = argumentsTranslated.toArray(new RubyNode[argumentsTranslated.size()]);

        final RubyNode ret = new YieldNode(context, translate(node.getPosition()), argumentsTranslatedArray, unsplat);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitZArrayNode(org.jruby.ast.ZArrayNode node) {
        final RubyNode[] values = new RubyNode[0];

        final RubyNode ret = new ArrayLiteralNode.UninitialisedArrayLiteralNode(context, translate(node.getPosition()), values);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBackRefNode(org.jruby.ast.BackRefNode node) {
        int index = 0;

        switch (node.getType()) {
            case '`':
                index = ReadMatchReferenceNode.PRE;
                break;
            case '\'':
                index = ReadMatchReferenceNode.POST;
                break;
            case '&':
                index = ReadMatchReferenceNode.GLOBAL;
                break;
            case '+':
                index = ReadMatchReferenceNode.HIGHEST;
                break;
            default:
                throw new UnsupportedOperationException(Character.toString(node.getType()));
        }

        final RubyNode ret = new ReadMatchReferenceNode(context, translate(node.getPosition()), index);
        return addNewlineIfNeeded(node, ret);
    }

    public RubyNode visitLambdaNode(org.jruby.ast.LambdaNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        org.jruby.ast.ArgsNode argsNode;

        if (node.getVarNode() instanceof org.jruby.ast.ArgsNode) {
            argsNode = (org.jruby.ast.ArgsNode) node.getVarNode();
        } else if (node.getVarNode() instanceof org.jruby.ast.DAsgnNode) {
            final org.jruby.ast.ArgumentNode arg = new org.jruby.ast.ArgumentNode(node.getPosition(), ((org.jruby.ast.DAsgnNode) node.getVarNode()).getName());
            final org.jruby.ast.ListNode preArgs = new org.jruby.ast.ArrayNode(node.getPosition(), arg);
            argsNode = new org.jruby.ast.ArgsNode(node.getPosition(), preArgs, null, null, null, null, null, null);
        } else if (node.getVarNode() == null) {
            argsNode = null;
        } else {
            throw new UnsupportedOperationException();
        }

        // TODO(cs): code copied and modified from visitIterNode - extract common
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, environment.getLexicalScope(), MethodTranslator.getArity(argsNode), "(lambda)", true, Helpers.argsNodeToArgumentDescriptors(node.findFirstChild(ArgsNode.class)), false);

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, environment.getParseEnvironment(), environment.getReturnID(), false, false,
                sharedMethodInfo, sharedMethodInfo.getName(), true, environment.getParseEnvironment().allocateBreakID());
        final MethodTranslator methodCompiler = new MethodTranslator(currentNode, context, this, newEnvironment, false, source, argsNode);

        final RubyNode definitionNode = methodCompiler.compileBlockNode(translate(node.getPosition()), sharedMethodInfo.getName(), node.getBodyNode(), sharedMethodInfo, Type.LAMBDA);

        return addNewlineIfNeeded(node, definitionNode);
    }

    protected RubyNode initFlipFlopStates(SourceSection sourceSection) {
        final RubyNode[] initNodes = new RubyNode[environment.getFlipFlopStates().size()];

        for (int n = 0; n < initNodes.length; n++) {
            initNodes[n] = new InitFlipFlopSlotNode(context, sourceSection, environment.getFlipFlopStates().get(n));
        }

        return SequenceNode.sequence(context, sourceSection, initNodes);
    }

    @Override
    protected RubyNode defaultVisit(Node node) {
        final RubyNode ret = unimplemented(node);
        return addNewlineIfNeeded(node, ret);
    }

    protected RubyNode unimplemented(Node node) {
        context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, source.getName(), node.getPosition().getLine(), node + " does nothing - translating as nil");
        SourceSection sourceSection = translate(node.getPosition());
        return nilNode(sourceSection);
    }

    public TranslatorEnvironment getEnvironment() {
        return environment;
    }

    @Override
    protected String getIdentifier() {
        if (environment.isBlock()) {
            TranslatorEnvironment methodParent = environment.getParent();

            while (methodParent.isBlock()) {
                methodParent = methodParent.getParent();
            }

            return "block in " + methodParent.getNamedMethodName();
        } else {
            return environment.getNamedMethodName();
        }
    }

    @Override
    public RubyNode visitOther(Node node) {
        if (node instanceof ReadLocalDummyNode) {
            final ReadLocalDummyNode readLocal = (ReadLocalDummyNode) node;
            final RubyNode ret = new ReadLocalVariableNode(context, readLocal.getSourceSection(), readLocal.getFrameSlot());
            return addNewlineIfNeeded(node, ret);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private RubyNode addNewlineIfNeeded(org.jruby.ast.Node jrubyNode, RubyNode node) {
        if (jrubyNode.isNewline()) {
            node.setAtNewline();
        }

        return node;
    }

}
