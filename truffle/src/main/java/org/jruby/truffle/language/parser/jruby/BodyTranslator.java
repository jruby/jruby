/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.language.parser.jruby;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.NodeUtil;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.joni.NameEntry;
import org.joni.Regex;
import org.joni.Syntax;
import org.jruby.ast.SideEffectFree;
import org.jruby.ast.visitor.NodeVisitor;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.InvalidSourcePosition;
import org.jruby.parser.ParserSupport;
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.PrimitiveConstructor;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.IsRubiniusUndefinedNode;
import org.jruby.truffle.core.RaiseIfFrozenNode;
import org.jruby.truffle.core.array.ArrayAppendOneNodeGen;
import org.jruby.truffle.core.array.ArrayConcatNode;
import org.jruby.truffle.core.array.ArrayDropTailNode;
import org.jruby.truffle.core.array.ArrayDropTailNodeGen;
import org.jruby.truffle.core.array.ArrayGetTailNodeGen;
import org.jruby.truffle.core.array.ArrayLiteralNode;
import org.jruby.truffle.core.array.PrimitiveArrayNodeFactory;
import org.jruby.truffle.core.cast.HashCastNodeGen;
import org.jruby.truffle.core.cast.IntegerCastNodeGen;
import org.jruby.truffle.core.cast.SplatCastNode;
import org.jruby.truffle.core.cast.SplatCastNodeGen;
import org.jruby.truffle.core.cast.StringToSymbolNodeGen;
import org.jruby.truffle.core.cast.ToProcNodeGen;
import org.jruby.truffle.core.cast.ToSNode;
import org.jruby.truffle.core.cast.ToSNodeGen;
import org.jruby.truffle.core.encoding.EncodingNodes;
import org.jruby.truffle.core.hash.ConcatHashLiteralNode;
import org.jruby.truffle.core.hash.HashLiteralNode;
import org.jruby.truffle.core.hash.HashNodesFactory;
import org.jruby.truffle.core.kernel.KernelNodesFactory;
import org.jruby.truffle.core.module.ModuleNodesFactory;
import org.jruby.truffle.core.numeric.BignumOperations;
import org.jruby.truffle.core.proc.ProcType;
import org.jruby.truffle.core.range.RangeNodesFactory;
import org.jruby.truffle.core.regexp.InterpolatedRegexpNode;
import org.jruby.truffle.core.regexp.MatchDataNodesFactory;
import org.jruby.truffle.core.regexp.RegexpNodes;
import org.jruby.truffle.core.regexp.RegexpNodesFactory;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rubinius.RubiniusLastStringReadNode;
import org.jruby.truffle.core.rubinius.RubiniusLastStringWriteNodeGen;
import org.jruby.truffle.language.arguments.SingleBlockArgNode;
import org.jruby.truffle.core.string.InterpolatedStringNode;
import org.jruby.truffle.core.string.StringNodesFactory;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.core.time.TimeNodesFactory;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.arguments.ArrayIsAtLeastAsLargeAsNode;
import org.jruby.truffle.language.constants.ReadConstantWithLexicalScopeNode;
import org.jruby.truffle.language.constants.ReadLiteralConstantNode;
import org.jruby.truffle.language.constants.WriteConstantNode;
import org.jruby.truffle.language.control.AndNode;
import org.jruby.truffle.language.control.BreakID;
import org.jruby.truffle.language.control.BreakNode;
import org.jruby.truffle.language.control.ElidableResultNode;
import org.jruby.truffle.language.control.FrameOnStackNode;
import org.jruby.truffle.language.control.IfElseNode;
import org.jruby.truffle.language.control.IfNode;
import org.jruby.truffle.language.control.NextNode;
import org.jruby.truffle.language.control.NotNode;
import org.jruby.truffle.language.control.OnceNode;
import org.jruby.truffle.language.control.OrNode;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.language.control.RedoNode;
import org.jruby.truffle.language.control.RetryNode;
import org.jruby.truffle.language.control.ReturnID;
import org.jruby.truffle.language.control.ReturnNode;
import org.jruby.truffle.language.control.UnlessNode;
import org.jruby.truffle.language.control.WhileNode;
import org.jruby.truffle.language.defined.DefinedNode;
import org.jruby.truffle.language.defined.DefinedWrapperNode;
import org.jruby.truffle.language.dispatch.RubyCallNode;
import org.jruby.truffle.language.exceptions.DisablingBacktracesNode;
import org.jruby.truffle.language.exceptions.EnsureNode;
import org.jruby.truffle.language.exceptions.RescueAnyNode;
import org.jruby.truffle.language.exceptions.RescueClassesNode;
import org.jruby.truffle.language.exceptions.RescueNode;
import org.jruby.truffle.language.exceptions.RescueSplatNode;
import org.jruby.truffle.language.exceptions.TryNode;
import org.jruby.truffle.language.globals.CheckMatchVariableTypeNode;
import org.jruby.truffle.language.globals.CheckOutputSeparatorVariableTypeNode;
import org.jruby.truffle.language.globals.CheckProgramNameVariableTypeNode;
import org.jruby.truffle.language.globals.CheckRecordSeparatorVariableTypeNode;
import org.jruby.truffle.language.globals.CheckStdoutVariableTypeNode;
import org.jruby.truffle.language.globals.ReadGlobalVariableNodeGen;
import org.jruby.truffle.language.globals.ReadLastBacktraceNode;
import org.jruby.truffle.language.globals.ReadMatchReferenceNode;
import org.jruby.truffle.language.globals.ReadThreadLocalGlobalVariableNode;
import org.jruby.truffle.language.globals.UpdateLastBacktraceNode;
import org.jruby.truffle.language.globals.UpdateVerbosityNode;
import org.jruby.truffle.language.globals.WriteGlobalVariableNodeGen;
import org.jruby.truffle.language.globals.WriteProgramNameNodeGen;
import org.jruby.truffle.language.globals.WriteReadOnlyGlobalNode;
import org.jruby.truffle.language.literal.BooleanLiteralNode;
import org.jruby.truffle.language.literal.FloatLiteralNode;
import org.jruby.truffle.language.literal.IntegerFixnumLiteralNode;
import org.jruby.truffle.language.literal.LongFixnumLiteralNode;
import org.jruby.truffle.language.literal.NilLiteralNode;
import org.jruby.truffle.language.literal.ObjectLiteralNode;
import org.jruby.truffle.language.literal.StringLiteralNode;
import org.jruby.truffle.language.locals.DeclarationFlipFlopStateNode;
import org.jruby.truffle.language.locals.FlipFlopNode;
import org.jruby.truffle.language.locals.FlipFlopStateNode;
import org.jruby.truffle.language.locals.InitFlipFlopSlotNode;
import org.jruby.truffle.language.locals.LocalFlipFlopStateNode;
import org.jruby.truffle.language.locals.LocalVariableType;
import org.jruby.truffle.language.locals.ReadLocalVariableNode;
import org.jruby.truffle.language.locals.WriteLocalVariableNode;
import org.jruby.truffle.language.methods.AddMethodNodeGen;
import org.jruby.truffle.language.methods.Arity;
import org.jruby.truffle.language.methods.BlockDefinitionNode;
import org.jruby.truffle.language.methods.CatchBreakNode;
import org.jruby.truffle.language.methods.ExceptionTranslatingNode;
import org.jruby.truffle.language.methods.GetCurrentVisibilityNode;
import org.jruby.truffle.language.methods.GetDefaultDefineeNode;
import org.jruby.truffle.language.methods.MethodDefinitionNode;
import org.jruby.truffle.language.methods.ModuleBodyDefinitionNode;
import org.jruby.truffle.language.methods.SharedMethodInfo;
import org.jruby.truffle.language.methods.UnsupportedOperationBehavior;
import org.jruby.truffle.language.objects.DefineClassNode;
import org.jruby.truffle.language.objects.DefineModuleNode;
import org.jruby.truffle.language.objects.DefineModuleNodeGen;
import org.jruby.truffle.language.objects.LexicalScopeNode;
import org.jruby.truffle.language.objects.ReadClassVariableNode;
import org.jruby.truffle.language.objects.ReadInstanceVariableNode;
import org.jruby.truffle.language.objects.RunModuleDefinitionNode;
import org.jruby.truffle.language.objects.SelfNode;
import org.jruby.truffle.language.objects.SingletonClassNode;
import org.jruby.truffle.language.objects.SingletonClassNodeGen;
import org.jruby.truffle.language.objects.WriteClassVariableNode;
import org.jruby.truffle.language.objects.WriteInstanceVariableNode;
import org.jruby.truffle.language.threadlocal.GetFromThreadLocalNodeGen;
import org.jruby.truffle.language.threadlocal.ThreadLocalObjectNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObjectNodeGen;
import org.jruby.truffle.language.threadlocal.WrapInThreadLocalNodeGen;
import org.jruby.truffle.language.yield.YieldExpressionNode;
import org.jruby.truffle.platform.graal.AssertConstantNodeGen;
import org.jruby.truffle.platform.graal.AssertNotCompiledNodeGen;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A JRuby parser node visitor which translates JRuby AST nodes into truffle Nodes. Therefore there is some namespace
 * contention here! We make all references to JRuby explicit.
 */
public class BodyTranslator extends Translator {

    protected final BodyTranslator parent;
    protected final TranslatorEnvironment environment;

    public boolean translatingForStatement = false;
    private boolean translatingNextExpression = false;
    private boolean translatingWhile = false;
    protected String currentCallMethodName = null;

    private boolean privately = false;

    protected boolean usesRubiniusPrimitive = false;

    private static final Set<String> THREAD_LOCAL_GLOBAL_VARIABLES = new HashSet<>(
            Arrays.asList("$~", "$1", "$2", "$3", "$4", "$5", "$6", "$7", "$8", "$9", "$!", "$?")); // "$_"

    private static final Set<String> READ_ONLY_GLOBAL_VARIABLES = new HashSet<String>(
            Arrays.asList("$:", "$LOAD_PATH", "$-I", "$\"", "$LOADED_FEATURES", "$<", "$FILENAME", "$?", "$-a", "$-l", "$-p", "$!"));

    private static final Map<String, String> GLOBAL_VARIABLE_ALIASES = new HashMap<String, String>();

    static {
        Map<String, String> m = GLOBAL_VARIABLE_ALIASES;
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

    public BodyTranslator(com.oracle.truffle.api.nodes.Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, Source source, boolean topLevel) {
        super(currentNode, context, source);
        this.parent = parent;
        this.environment = environment;
    }

    private DynamicObject translateNameNodeToSymbol(org.jruby.ast.Node node) {
        return context.getSymbolTable().getSymbol(((org.jruby.ast.LiteralNode) node).getName());
    }

    @Override
    public RubyNode visitAliasNode(org.jruby.ast.AliasNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final DynamicObject oldName = translateNameNodeToSymbol(node.getOldName());
        final DynamicObject newName = translateNameNodeToSymbol(node.getNewName());

        final RubyNode ret = ModuleNodesFactory.AliasMethodNodeFactory.create(
                new RaiseIfFrozenNode(new GetDefaultDefineeNode(context, sourceSection)),
                new ObjectLiteralNode(context, sourceSection, newName),
                new ObjectLiteralNode(context, sourceSection, oldName));

        setSourceSection(ret, sourceSection);

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
            org.jruby.ast.Node secondNode = new org.jruby.ast.SplatNode(node.getSecondNode().getPosition(), node.getSecondNode());
            nodes.add(secondNode);
        }
    }

    @Override
    public RubyNode visitArgsPushNode(org.jruby.ast.ArgsPushNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode args = node.getFirstNode().accept(this);
        final RubyNode value = node.getSecondNode().accept(this);
        final RubyNode ret = ArrayAppendOneNodeGen.create(context, sourceSection,
                KernelNodesFactory.DupNodeFactory.create(context, sourceSection, new RubyNode[] { args }),
                value);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitArrayNode(org.jruby.ast.ArrayNode node) {
        final org.jruby.ast.Node[] values = node.children();

        final RubyNode[] translatedValues = new RubyNode[values.length];

        for (int n = 0; n < values.length; n++) {
            translatedValues[n] = values[n].accept(this);
        }

        final RubyNode ret = ArrayLiteralNode.create(context, translate(node.getPosition()), translatedValues);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitAttrAssignNode(org.jruby.ast.AttrAssignNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        // The last argument is the value we assign, and we need to return that as the whole result of this node

        final FrameSlot frameSlot = environment.declareVar(environment.allocateLocalTemp("attrasgn"));
        final WriteLocalVariableNode writeValue;

        final org.jruby.ast.ArrayNode newArgsNode;

        // Get that last argument out
        final List<org.jruby.ast.Node> argChildNodes = new ArrayList<>(node.getArgsNode().childNodes());
        final org.jruby.ast.Node valueNode = argChildNodes.remove(argChildNodes.size() - 1);

        // Evaluate the value and store it in a local variable
        writeValue = new WriteLocalVariableNode(context, sourceSection, frameSlot, valueNode.accept(this));

        // Recreate the arguments array, reading that local instead of including the RHS for the last argument
        argChildNodes.add(new ReadLocalDummyNode(node.getPosition(), sourceSection, frameSlot));
        newArgsNode = new org.jruby.ast.ArrayNode(node.getPosition(), argChildNodes.get(0));
        argChildNodes.remove(0);
        for (org.jruby.ast.Node child : argChildNodes) {
            newArgsNode.add(child);
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

        final org.jruby.ast.CallNode callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getReceiverNode(), node.getName(), fixedArgsNode, null);
        copyNewline(node, callNode);
        boolean isAccessorOnSelf = (node.getReceiverNode() instanceof org.jruby.ast.SelfNode);
        final RubyNode actualCall = translateCallNode(callNode, isAccessorOnSelf, false);

        final RubyNode ret = sequence(context, sourceSection, Arrays.asList(writeValue, actualCall, new ReadLocalVariableNode(context, sourceSection, LocalVariableType.FRAME_LOCAL, frameSlot)));

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
            ret = new ObjectLiteralNode(context, sourceSection, BignumOperations.createBignum(context, node.getValue()));
        } else {
            ret = new LongFixnumLiteralNode(context, sourceSection, value.longValue());
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBlockNode(org.jruby.ast.BlockNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> translatedChildren = new ArrayList<>();

        final int firstLine = node.getPosition().getLine() + 1;
        int lastLine = firstLine;

        for (org.jruby.ast.Node child : node.children()) {
            if (child.getPosition() == InvalidSourcePosition.INSTANCE) {
                parentSourceSection.push(sourceSection);
            } else {
                lastLine = Math.max(lastLine, child.getPosition().getLine() + 1);
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
            final int startIndex = sourceSection.getSource().getLineStartOffset(node.getPosition().getLine() + 1);

            int length = 0;

            for (int n = firstLine; n <= lastLine; n++) {
                length += sourceSection.getSource().getLineLength(n);
            }

            length = Math.min(length + startIndex, sourceSection.getSource().getLength()) - startIndex;

            ret = sequence(context, sourceSection.getSource().createSection(sourceSection.getIdentifier(), startIndex, length), translatedChildren);
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

        final RubyNode ret = new BreakNode(context, sourceSection, environment.getBreakID(), translatingWhile, resultNode);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitCallNode(org.jruby.ast.CallNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final org.jruby.ast.Node receiver = node.getReceiverNode();
        final String methodName = node.getName();

        if (receiver instanceof org.jruby.ast.StrNode && methodName.equals("freeze")) {
            final org.jruby.ast.StrNode strNode = (org.jruby.ast.StrNode) receiver;
            final ByteList byteList = strNode.getValue();
            final int codeRange = strNode.getCodeRange();

            final Rope rope = context.getRopeTable().getRope(byteList.bytes(), byteList.getEncoding(), CodeRange.fromInt(codeRange));

            final DynamicObject frozenString = context.getFrozenStrings().getFrozenString(rope);

            return addNewlineIfNeeded(node, new DefinedWrapperNode(context, sourceSection, context.getCoreStrings().METHOD,
                    new ObjectLiteralNode(context, null, frozenString)));
        }

        if (receiver instanceof org.jruby.ast.ConstNode
                && ((org.jruby.ast.ConstNode) receiver).getName().equals("Truffle")) {
            // Truffle.<method>

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
                final RubyNode ret = translateSingleBlockArg(sourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("check_frozen")) {
                final RubyNode ret = translateCheckFrozen(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof org.jruby.ast.Colon2ConstNode // Truffle.<method>
                && ((org.jruby.ast.Colon2ConstNode) receiver).getLeftNode() instanceof org.jruby.ast.ConstNode
                && ((org.jruby.ast.ConstNode) ((org.jruby.ast.Colon2ConstNode) receiver).getLeftNode()).getName().equals("Truffle")
                && ((org.jruby.ast.Colon2ConstNode) receiver).getName().equals("Graal")) {
            if (methodName.equals("assert_constant")) {
                final RubyNode ret = AssertConstantNodeGen.create(context, sourceSection, node.getArgsNode().childNodes().get(0).accept(this));
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("assert_not_compiled")) {
                final RubyNode ret = AssertNotCompiledNodeGen.create(context, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof org.jruby.ast.ConstNode // Truffle.omit
                && ((org.jruby.ast.ConstNode) receiver).getName().equals("Truffle")) {
            if (methodName.equals("omit")) {
                // We're never going to run the omitted code and it's never used as the RHS for anything, so just
                // replace the call with nil.
                final RubyNode ret = nilNode(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof org.jruby.ast.VCallNode // undefined.equal?(obj)
                && ((org.jruby.ast.VCallNode) receiver).getName().equals("undefined")
                && getSourcePath(sourceSection).startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/")
                && methodName.equals("equal?")) {
            RubyNode argument = translateArgumentsAndBlock(sourceSection, null, node.getArgsNode(), methodName).getArguments()[0];
            final RubyNode ret = new IsRubiniusUndefinedNode(context, sourceSection, argument);
            return addNewlineIfNeeded(node, ret);
        }

        return translateCallNode(node, false, false);
    }

    private RubyNode translateRubiniusPrimitive(SourceSection sourceSection, org.jruby.ast.CallNode node) {
        usesRubiniusPrimitive = true;

        /*
         * Translates something that looks like
         *
         *   Truffle.primitive :foo
         *
         * into
         *
         *   CallPrimitiveNode(FooNode(arg1, arg2, ..., argN))
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
            throw new UnsupportedOperationException("Truffle.primitive must have a single literal symbol argument");
        }

        final String primitiveName = ((org.jruby.ast.SymbolNode) node.getArgsNode().childNodes().get(0)).getName();

        final PrimitiveConstructor primitive = context.getPrimitiveManager().getPrimitive(primitiveName);
        final ReturnID returnID = environment.getReturnID();
        return primitive.createCallPrimitiveNode(context, sourceSection, returnID);
    }

    private RubyNode translateRubiniusInvokePrimitive(SourceSection sourceSection, org.jruby.ast.CallNode node) {
        /*
         * Translates something that looks like
         *
         *   Truffle.invoke_primitive :foo, arg1, arg2, argN
         *
         * into
         *
         *   InvokePrimitiveNode(FooNode(arg1, arg2, ..., argN))
         *
         * or
         *
         *   (<#Method ModuleDefinedIn#foo>).call(arg1, arg2, ..., argN)
         */

        if (node.getArgsNode().childNodes().size() < 1 || !(node.getArgsNode().childNodes().get(0) instanceof org.jruby.ast.SymbolNode)) {
            throw new UnsupportedOperationException("Truffle.invoke_primitive must have at least an initial literal symbol argument");
        }

        final String primitiveName = ((org.jruby.ast.SymbolNode) node.getArgsNode().childNodes().get(0)).getName();

        final PrimitiveConstructor primitive = context.getPrimitiveManager().getPrimitive(primitiveName);

        final List<RubyNode> arguments = new ArrayList<>();

        // The first argument was the symbol so we ignore it
        for (int n = 1; n < node.getArgsNode().childNodes().size(); n++) {
            RubyNode readArgumentNode = node.getArgsNode().childNodes().get(n).accept(this);
            arguments.add(readArgumentNode);
        }

        return primitive.createInvokePrimitiveNode(context, sourceSection, arguments.toArray(new RubyNode[arguments.size()]));
    }

    private RubyNode translateRubiniusPrivately(SourceSection sourceSection, org.jruby.ast.CallNode node) {
        /*
         * Translates something that looks like
         *
         *   Truffle.privately { foo }
         *
         * into just
         *
         *   foo
         *
         * While we translate foo we'll mark all call sites as ignoring visbility.
         */

        if (!(node.getIterNode() instanceof org.jruby.ast.IterNode)) {
            throw new UnsupportedOperationException("Truffle.privately needs a literal block");
        }

        if (node.getArgsNode() != null && node.getArgsNode().childNodes().size() > 0) {
            throw new UnsupportedOperationException("Truffle.privately should not have any arguments");
        }

        /*
         * Normally when you visit an 'iter' (block) node it will set the method name for you, so that we can name the
         * block something like 'times-block'. Here we bypass the iter node and translate its child. So we set the
         * name here.
         */

        currentCallMethodName = "privately";

        /*
         * While we translate the body of the iter we want to create all call nodes with the ignore-visbility flag.
         * This flag is checked in visitCallNode.
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

    public RubyNode translateSingleBlockArg(SourceSection sourceSection, org.jruby.ast.CallNode node) {
        return new SingleBlockArgNode(context, sourceSection);
    }

    private RubyNode translateCheckFrozen(SourceSection sourceSection) {
        return new RaiseIfFrozenNode(new SelfNode(context, sourceSection));
    }

    private RubyNode translateCallNode(org.jruby.ast.CallNode node, boolean ignoreVisibility, boolean isVCall) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode receiverTranslated = node.getReceiverNode().accept(this);

        org.jruby.ast.Node args = node.getArgsNode();
        org.jruby.ast.Node block = node.getIterNode();

        if (block == null && args instanceof org.jruby.ast.IterNode) {
            block = args;
            args = null;
        }

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, block, args, node.getName());

        final List<RubyNode> children = new ArrayList<>();

        if (argumentsAndBlock.getBlock() != null) {
            children.add(argumentsAndBlock.getBlock());
        }

        children.addAll(Arrays.asList(argumentsAndBlock.getArguments()));

        RubyNode translated = new RubyCallNode(context, enclosing(sourceSection, children.toArray(new RubyNode[children.size()])),
                node.getName(), receiverTranslated, argumentsAndBlock.getBlock(), argumentsAndBlock.isSplatted(),
                privately || ignoreVisibility, isVCall, argumentsAndBlock.getArguments());

        if (argumentsAndBlock.getBlock() instanceof BlockDefinitionNode) { // if we have a literal block, break breaks out of this call site
            BlockDefinitionNode blockDef = (BlockDefinitionNode) argumentsAndBlock.getBlock();
            translated = new FrameOnStackNode(context, translated.getSourceSection(), translated, argumentsAndBlock.getFrameOnStackMarkerSlot());
            translated = new CatchBreakNode(context, translated.getSourceSection(), blockDef.getBreakID(), translated);
        }

        return addNewlineIfNeeded(node, translated);
    }

    protected static class ArgumentsAndBlockTranslation {

        private final RubyNode block;
        private final RubyNode[] arguments;
        private final boolean isSplatted;
        private final FrameSlot frameOnStackMarkerSlot;

        public ArgumentsAndBlockTranslation(RubyNode block, RubyNode[] arguments, boolean isSplatted, FrameSlot frameOnStackMarkerSlot) {
            super();
            this.block = block;
            this.arguments = arguments;
            this.isSplatted = isSplatted;
            this.frameOnStackMarkerSlot = frameOnStackMarkerSlot;
        }

        public RubyNode getBlock() {
            return block;
        }

        public RubyNode[] getArguments() {
            return arguments;
        }

        public boolean isSplatted() {
            return isSplatted;
        }

        public FrameSlot getFrameOnStackMarkerSlot() {
            return frameOnStackMarkerSlot;
        }
    }

    public static final Object BAD_FRAME_SLOT = new Object();

    public Deque<Object> frameOnStackMarkerSlotStack = new ArrayDeque<>();

    protected ArgumentsAndBlockTranslation translateArgumentsAndBlock(SourceSection sourceSection, org.jruby.ast.Node iterNode, org.jruby.ast.Node argsNode, String nameToSetWhenTranslatingBlock) {
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

        final RubyNode[] argumentsTranslated = new RubyNode[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            argumentsTranslated[i] = arguments.get(i).accept(this);
        }

        if (iterNode instanceof org.jruby.ast.BlockPassNode) {
            blockPassNode = ((org.jruby.ast.BlockPassNode) iterNode).getBodyNode();
        }

        currentCallMethodName = nameToSetWhenTranslatingBlock;


        final FrameSlot frameOnStackMarkerSlot;
        RubyNode blockTranslated;

        if (blockPassNode != null) {
            blockTranslated = ToProcNodeGen.create(context, sourceSection, blockPassNode.accept(this));
            frameOnStackMarkerSlot = null;
        } else if (iterNode != null) {
            frameOnStackMarkerSlot = environment.declareVar(environment.allocateLocalTemp("frame_on_stack_marker"));
            frameOnStackMarkerSlotStack.push(frameOnStackMarkerSlot);

            try {
                blockTranslated = iterNode.accept(this);
            } finally {
                frameOnStackMarkerSlotStack.pop();
            }

            if (blockTranslated instanceof ObjectLiteralNode && ((ObjectLiteralNode) blockTranslated).getObject() == context.getCoreLibrary().getNilObject()) {
                blockTranslated = null;
            }
        } else {
            blockTranslated = null;
            frameOnStackMarkerSlot = null;
        }

        return new ArgumentsAndBlockTranslation(blockTranslated, argumentsTranslated, isSplatted, frameOnStackMarkerSlot);
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

            final ReadLocalNode readTemp = environment.findLocalVarNode(tempName, sourceSection);

            final RubyNode assignTemp = readTemp.makeWriteNode(node.getCaseNode().accept(this));

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
                    expressions = Collections.singletonList(when.getExpressionNodes());
                }

                final List<RubyNode> comparisons = new ArrayList<>();

                for (org.jruby.ast.Node expressionNode : expressions) {
                    final RubyNode rubyExpression = expressionNode.accept(this);

                    if (expressionNode instanceof org.jruby.ast.SplatNode
                            || expressionNode instanceof org.jruby.ast.ArgsCatNode
                            || expressionNode instanceof org.jruby.ast.ArgsPushNode) {
                        comparisons.add(new RubyCallNode(context, sourceSection, "when_splat", new SelfNode(context, sourceSection), null, false, true, rubyExpression, NodeUtil.cloneNode(readTemp)));
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

                final IfElseNode ifNode = new IfElseNode(context, sourceSection, conditionNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            final RubyNode ifNode = elseNode;

            // A top-level block assigns the temp then runs the if

            ret = sequence(context, sourceSection, Arrays.asList(assignTemp, ifNode));
        } else {
            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final org.jruby.ast.WhenNode when = (org.jruby.ast.WhenNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<org.jruby.ast.Node> expressions;

                if (when.getExpressionNodes() instanceof org.jruby.ast.ListNode) {
                    expressions = when.getExpressionNodes().childNodes();
                } else {
                    expressions = Collections.singletonList(when.getExpressionNodes());
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

                final IfElseNode ifNode = new IfElseNode(context, sourceSection, conditionNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            ret = elseNode;
        }

        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode openModule(SourceSection sourceSection, RubyNode defineOrGetNode, String name, org.jruby.ast.Node bodyNode, boolean sclass) {
        LexicalScope newLexicalScope = environment.pushLexicalScope();
        try {
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, newLexicalScope, Arity.NO_ARGUMENTS, name, false, null, false, false, false);

            final ReturnID returnId;

            if (sclass) {
                returnId = environment.getReturnID();
            } else {
                returnId = environment.getParseEnvironment().allocateReturnID();
            }

            final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParseEnvironment(),
                    returnId, true, true, sharedMethodInfo, name, 0, null);

            final BodyTranslator moduleTranslator = new BodyTranslator(currentNode, context, this, newEnvironment, source, false);

            final ModuleBodyDefinitionNode definition = moduleTranslator.compileClassNode(sourceSection, name, bodyNode, sclass);

            return new RunModuleDefinitionNode(context, sourceSection, newLexicalScope, definition, defineOrGetNode);
        } finally {
            environment.popLexicalScope();
        }
    }

    /**
     * Translates module and class nodes.
     * <p>
     * In Ruby, a module or class definition is somewhat like a method. It has a local scope and a value
     * for self, which is the module or class object that is being defined. Therefore for a module or
     * class definition we translate into a special method. We run that method with self set to be the
     * newly allocated module or class.
     * </p>
     */
    private ModuleBodyDefinitionNode compileClassNode(SourceSection sourceSection, String name, org.jruby.ast.Node bodyNode, boolean sclass) {
        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        if (environment.getFlipFlopStates().size() > 0) {
            body = sequence(context, sourceSection, Arrays.asList(initFlipFlopStates(sourceSection), body));
        }

        final RubyRootNode rootNode = new RubyRootNode(context, sourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        final ModuleBodyDefinitionNode definitionNode = new ModuleBodyDefinitionNode(
                context,
                sourceSection,
                environment.getSharedMethodInfo().getName(),
                environment.getSharedMethodInfo(),
                Truffle.getRuntime().createCallTarget(rootNode),
                sclass);

        return definitionNode;
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
            superClass = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        }

        final DefineClassNode defineOrGetClass = new DefineClassNode(context, sourceSection, name, lexicalParent, superClass);

        final RubyNode ret = openModule(sourceSection, defineOrGetClass, name, node.getBodyNode(), false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitClassVarAsgnNode(org.jruby.ast.ClassVarAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode rhs = node.getValueNode().accept(this);

        final RubyNode ret = new WriteClassVariableNode(context, sourceSection, environment.getLexicalScope(), node.getName(), rhs);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitClassVarNode(org.jruby.ast.ClassVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new ReadClassVariableNode(context, sourceSection, environment.getLexicalScope(), node.getName());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitColon2Node(org.jruby.ast.Colon2Node node) {
        // Qualified constant access, as in Mod::CONST
        if (!(node instanceof org.jruby.ast.Colon2ConstNode)) {
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

        final ObjectLiteralNode root = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());

        final RubyNode ret = new ReadLiteralConstantNode(context, sourceSection, root, name);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateCPath(SourceSection sourceSection, org.jruby.ast.Colon3Node node) {
        final RubyNode ret;

        if (node instanceof org.jruby.ast.Colon2ImplicitNode) { // use current lexical scope
            ret = new LexicalScopeNode(context, sourceSection, environment.getLexicalScope());
        } else if (node instanceof org.jruby.ast.Colon2ConstNode) { // A::B
            ret = node.childNodes().get(0).accept(this);
        } else { // Colon3Node: on top-level (Object)
            ret = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitComplexNode(org.jruby.ast.ComplexNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode ret = translateRationalComplex(sourceSection, "Complex",
                new IntegerFixnumLiteralNode(context, sourceSection, 0),
                node.getNumber().accept(this));

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitConstDeclNode(org.jruby.ast.ConstDeclNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        RubyNode rhs = node.getValueNode().accept(this);

        final RubyNode moduleNode;
        org.jruby.ast.Node constNode = node.getConstNode();
        if (constNode == null || constNode instanceof org.jruby.ast.Colon2ImplicitNode) {
            moduleNode = new LexicalScopeNode(context, sourceSection, environment.getLexicalScope());
        } else if (constNode instanceof org.jruby.ast.Colon2ConstNode) {
            constNode = ((org.jruby.ast.Colon2Node) constNode).getLeftNode(); // Misleading doc, we only want the defined part.
            moduleNode = constNode.accept(this);
        } else if (constNode instanceof org.jruby.ast.Colon3Node) {
            moduleNode = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        } else {
            throw new UnsupportedOperationException();
        }

        final RubyNode ret = new WriteConstantNode(node.getName(), moduleNode, rhs);
        ret.unsafeSetSourceSection(sourceSection);

        return addNewlineIfNeeded(node, ret);
    }

    private String getSourcePath(SourceSection sourceSection) {
        final Source source = sourceSection.getSource();

        if (source == null) {
            return "(unknown)";
        }

        final String path = source.getPath();

        if (path == null) {
            return source.getShortName();
        }

        return path;
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

        if (name.equals("Rubinius") && getSourcePath(sourceSection).startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core")) {
            final RubyNode ret = new org.jruby.ast.Colon3Node(node.getPosition(), name).accept(this);
            return addNewlineIfNeeded(node, ret);
        }

        // TODO (pitr 01-Dec-2015): remove when RUBY_PLATFORM is set to "truffle"
        if (name.equals("RUBY_PLATFORM") && getSourcePath(sourceSection).contains("test/xml_mini/jdom_engine_test.rb")) {
            final ObjectLiteralNode ret = new ObjectLiteralNode(context, sourceSection, StringOperations.createString(context, StringOperations.encodeRope("truffle", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT)));
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
        final org.jruby.ast.Node callNode = new org.jruby.ast.FCallNode(node.getPosition(), "`", argsNode, null);
        copyNewline(node, callNode);
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
        final RubyNode classNode = new RaiseIfFrozenNode(new GetDefaultDefineeNode(context, sourceSection));

        String methodName = node.getName();

        // If we have a method we've defined in a node, but would like to delegate some corner cases out to the
        // Rubinius implementation for simplicity, we need a way to resolve the naming conflict.  The naive solution
        // here is to append "_internal" to the method name, which can then be called like any other method.  This is
        // a bit different than aliasing because normally if a Rubinius method name conflicts with an already defined
        // method, we simply ignore the method definition.  Here we explicitly rename the method so it's always defined.

        final String path = getSourcePath(sourceSection);
        final String coreRubiniusPath = context.getCoreLibrary().getCoreLoadPath() + "/core/";
        if (path.startsWith(coreRubiniusPath)) {
            boolean rename = false;

            if (path.equals(coreRubiniusPath + "array.rb")) {
                rename = methodName.equals("fill") || methodName.equals("zip");
            } else if (path.equals(coreRubiniusPath + "float.rb")) {
                rename = methodName.equals("round");
            } else if (path.equals(coreRubiniusPath + "range.rb")) {
                rename = methodName.equals("each") || methodName.equals("step") || methodName.equals("to_a");
            } else if (path.equals(coreRubiniusPath + "integer.rb")) {
                rename = methodName.equals("downto") || methodName.equals("upto");
            } else if (path.equals(coreRubiniusPath + "string.rb")) {
                rename = methodName.equals("<<");
            }

            if (rename) {
                // <<_internal is an invalid method name, so we need to rename to its alias for String#{<<,concat}.
                if (methodName.equals("<<")) {
                    methodName = "concat_internal";
                } else {
                    methodName = methodName + "_internal";
                }
            }
        }

        final RubyNode ret = translateMethodDefinition(sourceSection, classNode, methodName, node.getArgsNode(), node.getBodyNode(), false);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefsNode(org.jruby.ast.DefsNode node) {
        final SourceSection sourceSection = translate(node.getPosition(), node.getName());

        final RubyNode objectNode = node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(context, sourceSection, objectNode);

        final RubyNode ret = translateMethodDefinition(sourceSection, singletonClassNode, node.getName(), node.getArgsNode(), node.getBodyNode(), true);

        return addNewlineIfNeeded(node, ret);
    }

    protected RubyNode translateMethodDefinition(SourceSection sourceSection, RubyNode classNode, String methodName, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode,
                                                 boolean isDefs) {
        final Arity arity = MethodTranslator.getArity(argsNode);
        final ArgumentDescriptor[] argumentDescriptors = Helpers.argsNodeToArgumentDescriptors(argsNode);
        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, environment.getLexicalScope(), arity, methodName, false, argumentDescriptors, false, false, false);

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, environment.getParseEnvironment(), environment.getParseEnvironment().allocateReturnID(), true, true, sharedMethodInfo, methodName, 0, null);

        // ownScopeForAssignments is the same for the defined method as the current one.

        final MethodTranslator methodCompiler = new MethodTranslator(currentNode, context, this, newEnvironment, false, source, argsNode);

        final MethodDefinitionNode methodDefinitionNode = methodCompiler.compileMethodNode(sourceSection, methodName, bodyNode, sharedMethodInfo);

        final RubyNode visibilityNode;
        if (isDefs) {
            visibilityNode = new ObjectLiteralNode(context, sourceSection, Visibility.PUBLIC);
        } else {
            visibilityNode = new GetCurrentVisibilityNode(context, sourceSection);
        }

        return AddMethodNodeGen.create(context, sourceSection, isDefs, true, classNode, methodDefinitionNode, visibilityNode);
    }

    @Override
    public RubyNode visitDotNode(org.jruby.ast.DotNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode begin = node.getBeginNode().accept(this);
        final RubyNode end = node.getEndNode().accept(this);
        final RubyNode rangeClass = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getRangeClass());
        final RubyNode isExclusive = new ObjectLiteralNode(context, sourceSection, node.isExclusive());

        final RubyNode ret = RangeNodesFactory.NewNodeFactory.create(context, sourceSection, rangeClass, begin, end, isExclusive);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEncodingNode(org.jruby.ast.EncodingNode node) {
        SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new ObjectLiteralNode(context, sourceSection, EncodingNodes.getEncoding(node.getEncoding()));
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
            ret = new ObjectLiteralNode(context, sourceSection, StringOperations.createString(context, RopeConstants.EMPTY_ASCII_8BIT_ROPE));
        } else {
            ret = node.getBody().accept(this);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFCallNode(org.jruby.ast.FCallNode node) {
        final org.jruby.ast.Node receiver = new org.jruby.ast.SelfNode(node.getPosition());
        final org.jruby.ast.CallNode callNode = new org.jruby.ast.CallNode(node.getPosition(), receiver, node.getName(), node.getArgsNode(), node.getIterNode());
        copyNewline(node, callNode);
        return translateCallNode(callNode, true, false);
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
            ret = new IntegerFixnumLiteralNode(context, translate(node.getPosition()), (int) value);
        } else {
            ret = new LongFixnumLiteralNode(context, translate(node.getPosition()), value);
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
            return new LocalFlipFlopStateNode(frameSlot);
        } else {
            return new DeclarationFlipFlopStateNode(depth, frameSlot);
        }
    }

    @Override
    public RubyNode visitFloatNode(org.jruby.ast.FloatNode node) {
        final RubyNode ret = new FloatLiteralNode(context, translate(node.getPosition()), node.getValue());
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

        final org.jruby.ast.CallNode callNode = new org.jruby.ast.CallNode(node.getPosition(), receiver, "each", null, block);
        copyNewline(node, callNode);

        translatingForStatement = true;
        final RubyNode translated = callNode.accept(this);
        translatingForStatement = false;

        return addNewlineIfNeeded(node, translated);
    }

    private static final ParserSupport PARSER_SUPPORT = new ParserSupport();

    private static org.jruby.ast.Node setRHS(org.jruby.ast.Node node, org.jruby.ast.Node rhs) {
        if (node instanceof org.jruby.ast.AssignableNode || node instanceof org.jruby.ast.IArgumentNode) {
            return PARSER_SUPPORT.node_assign(node, rhs);
        } else {
            throw new UnsupportedOperationException("Don't know how to set the RHS of a " + node.getClass().getName());
        }
    }

    private RubyNode translateDummyAssignment(org.jruby.ast.Node dummyAssignment, final RubyNode rhs) {
        // The JRuby AST includes assignment nodes without a proper value,
        // so we need to patch them to include the proper rhs value to translate them correctly.

        if (dummyAssignment instanceof org.jruby.ast.StarNode) {
            // Nothing to assign to, just execute the RHS
            return rhs;
        } else if (dummyAssignment instanceof org.jruby.ast.AssignableNode || dummyAssignment instanceof org.jruby.ast.IArgumentNode) {
            final org.jruby.ast.Node wrappedRHS = new org.jruby.ast.Node(dummyAssignment.getPosition(), false) {
                @SuppressWarnings("unchecked")
                @Override
                public <T> T accept(NodeVisitor<T> visitor) {
                    return (T) rhs;
                }

                @Override
                public List<org.jruby.ast.Node> childNodes() {
                    return Collections.emptyList();
                }

                @Override
                public org.jruby.ast.NodeType getNodeType() {
                    return org.jruby.ast.NodeType.FIXNUMNODE; // since we behave like a value
                }
            };

            return setRHS(dummyAssignment, wrappedRHS).accept(this);
        } else {
            throw new UnsupportedOperationException("Don't know how to translate the dummy asgn " + dummyAssignment.getClass().getName());
        }
    }

    @Override
    public RubyNode visitGlobalAsgnNode(org.jruby.ast.GlobalAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        RubyNode rhs = node.getValueNode().accept(this);

        String name = node.getName();

        if (GLOBAL_VARIABLE_ALIASES.containsKey(name)) {
            name = GLOBAL_VARIABLE_ALIASES.get(name);
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
            if (getSourcePath(sourceSection).endsWith("truffle/rubysl/rubysl-stringio/lib/rubysl/stringio/stringio.rb")) {
                rhs = RubiniusLastStringWriteNodeGen.create(context, sourceSection, rhs);
            } else {
                rhs = WrapInThreadLocalNodeGen.create(context, sourceSection, rhs);
            }

            environment.declareVar("$_");
        } else if (name.equals("$stdout")) {
            rhs = new CheckStdoutVariableTypeNode(context, sourceSection, rhs);
        } else if (name.equals("$VERBOSE")) {
            rhs = new UpdateVerbosityNode(context, sourceSection, rhs);
        } else if (name.equals("$@")) {
            // $@ is a special-case and doesn't write directly to an ivar field in the globals object.
            // Instead, it writes to the backtrace field of the thread-local $! value.
            return new UpdateLastBacktraceNode(context, sourceSection, rhs);
        }

        final boolean inCore = getSourcePath(rhs.getSourceSection()).startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/");

        if (!inCore && READ_ONLY_GLOBAL_VARIABLES.contains(name)) {
            return addNewlineIfNeeded(node, new WriteReadOnlyGlobalNode(context, sourceSection, name, rhs));
        }

        if (THREAD_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            final ThreadLocalObjectNode threadLocalVariablesObjectNode = ThreadLocalObjectNodeGen.create(context, sourceSection);
            return addNewlineIfNeeded(node, new WriteInstanceVariableNode(context, sourceSection, name, threadLocalVariablesObjectNode, rhs));
        } else if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            if (environment.getNeverAssignInParentScope()) {
                environment.declareVar(name);
            }

            ReadLocalNode localVarNode = environment.findLocalVarNode(node.getName(), sourceSection);

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

            RubyNode assignment = localVarNode.makeWriteNode(rhs);

            if (name.equals("$_")) {
                // TODO CS 4-Jan-16 I can't work out why this is a *get* node
                assignment = GetFromThreadLocalNodeGen.create(context, sourceSection, assignment);
            }

            return addNewlineIfNeeded(node, assignment);
        } else {
            final RubyNode writeGlobalVariableNode = WriteGlobalVariableNodeGen.create(context, sourceSection, name, rhs);

            final RubyNode translated;

            if (name.equals("$0")) {
                translated = WriteProgramNameNodeGen.create(context, sourceSection, writeGlobalVariableNode);
            } else {
                translated = writeGlobalVariableNode;
            }

            return addNewlineIfNeeded(node, translated);
        }
    }

    @Override
    public RubyNode visitGlobalVarNode(org.jruby.ast.GlobalVarNode node) {
        String name = node.getName();

        if (GLOBAL_VARIABLE_ALIASES.containsKey(name)) {
            name = GLOBAL_VARIABLE_ALIASES.get(name);
        }

        final SourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret;

        if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            // Assignment is implicit for many of these, so we need to declare when we use

            environment.declareVarWhereAllowed(name);

            RubyNode readNode = environment.findLocalVarNode(name, sourceSection);

            if (name.equals("$_")) {
                if (getSourcePath(sourceSection).equals(context.getCoreLibrary().getCoreLoadPath() + "/core/regexp.rb")) {
                    readNode = new RubiniusLastStringReadNode(context, sourceSection);
                } else {
                    readNode = GetFromThreadLocalNodeGen.create(context, sourceSection, readNode);
                }
            }

            ret = readNode;
        } else if (THREAD_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            ret = new ReadThreadLocalGlobalVariableNode(context, sourceSection, name, ALWAYS_DEFINED_GLOBALS.contains(name));
        } else if (name.equals("$@")) {
            // $@ is a special-case and doesn't read directly from an ivar field in the globals object.
            // Instead, it reads the backtrace field of the thread-local $! value.
            ret = new ReadLastBacktraceNode(context, sourceSection);
        } else {
            ret = ReadGlobalVariableNodeGen.create(context, sourceSection, name);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitHashNode(org.jruby.ast.HashNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> hashConcats = new ArrayList<>();

        final List<RubyNode> keyValues = new ArrayList<>();

        for (KeyValuePair<org.jruby.ast.Node, org.jruby.ast.Node> pair: node.getPairs()) {
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

        final RubyNode condition = translateNodeOrNil(sourceSection, node.getCondition());

        org.jruby.ast.Node thenBody = node.getThenBody();
        org.jruby.ast.Node elseBody = node.getElseBody();

        final RubyNode ret;

        if (thenBody != null && elseBody != null) {
            final RubyNode thenBodyTranslated = thenBody.accept(this);
            final RubyNode elseBodyTranslated = elseBody.accept(this);
            ret = new IfElseNode(context, sourceSection, condition, thenBodyTranslated, elseBodyTranslated);
        } else if (thenBody != null) {
            final RubyNode thenBodyTranslated = thenBody.accept(this);
            ret = new IfNode(context, sourceSection, condition, thenBodyTranslated);
        } else if (elseBody != null) {
            final RubyNode elseBodyTranslated = elseBody.accept(this);
            ret = new UnlessNode(context, sourceSection, condition, elseBodyTranslated);
        } else {
            ret = sequence(context, sourceSection, Arrays.asList(condition, new NilLiteralNode(context, sourceSection, true)));
        }

        return ret; // no addNewlineIfNeeded(node, ret) as the condition will already have a newline
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

        final String path = getSourcePath(sourceSection);
        final String corePath = context.getCoreLibrary().getCoreLoadPath() + "/core/";
        final RubyNode ret;
        if (path.equals(corePath + "hash.rb")) {
            if (name.equals("@default")) {
                ret = HashNodesFactory.SetDefaultValueNodeFactory.create(self, rhs);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@default_proc")) {
                ret = HashNodesFactory.SetDefaultProcNodeFactory.create(self, rhs);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "string.rb")) {
            if (name.equals("@hash")) {
                ret = StringNodesFactory.ModifyBangNodeFactory.create(new RubyNode[]{});
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "range.rb")) {
            if (name.equals("@begin")) {
                ret = RangeNodesFactory.InternalSetBeginNodeGen.create(self, rhs);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@end")) {
                ret = RangeNodesFactory.InternalSetEndNodeGen.create(self, rhs);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@excl")) {
                ret = RangeNodesFactory.InternalSetExcludeEndNodeGen.create(self, rhs);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "io.rb")) {
            // TODO (pitr 08-Aug-2015): values of predefined OM properties should be casted to defined types automatically
            if (name.equals("@used") || name.equals("@total") || name.equals("@lineno")) {
                // Cast int-fitting longs back to int
                ret = new WriteInstanceVariableNode(context, sourceSection, name, self, IntegerCastNodeGen.create(context, sourceSection, rhs));
                return addNewlineIfNeeded(node, ret);
            }
        }

        ret = new WriteInstanceVariableNode(context, sourceSection, name, self, rhs);
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
        final String path = getSourcePath(sourceSection);
        final String corePath = context.getCoreLibrary().getCoreLoadPath() + "/core/";
        final RubyNode ret;
        if (path.equals(corePath + "array.rb")) {
            if (name.equals("@total")) {
                ret = new RubyCallNode(context, sourceSection, "size", self, null, false);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@tuple")) {
                ret = self;
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@start")) {
                ret = new IntegerFixnumLiteralNode(context, sourceSection, 0);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "regexp.rb")) {
            if (name.equals("@source")) {
                ret = MatchDataNodesFactory.RubiniusSourceNodeGen.create(self);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@full")) {
                // Delegate to MatchData#full.
                ret = new RubyCallNode(context, sourceSection, "full", self, null, false);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@regexp")) {
                ret = MatchDataNodesFactory.RegexpNodeFactory.create(new RubyNode[]{ self });
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@names")) {
                ret = RegexpNodesFactory.RubiniusNamesNodeGen.create(self);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "time.rb")) {
            if (name.equals("@is_gmt")) {
                ret = TimeNodesFactory.InternalGMTNodeFactory.create(self);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@offset")) {
                ret = TimeNodesFactory.InternalOffsetNodeFactory.create(self);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "hash.rb")) {
            if (name.equals("@default")) {
                ret = HashNodesFactory.DefaultValueNodeFactory.create(self);
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@default_proc")) {
                ret = HashNodesFactory.DefaultProcNodeFactory.create(new RubyNode[]{ self });
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@size")) {
                ret = HashNodesFactory.SizeNodeFactory.create(new RubyNode[]{ self });
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "range.rb")) {
            if (name.equals("@begin")) {
                ret = RangeNodesFactory.BeginNodeFactory.create(new RubyNode[]{ self });
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@end")) {
                ret = RangeNodesFactory.EndNodeFactory.create(new RubyNode[]{ self });
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@excl")) {
                ret = RangeNodesFactory.ExcludeEndNodeFactory.create(new RubyNode[]{ self });
                setSourceSection(ret, sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        }

        ret = new ReadInstanceVariableNode(context, sourceSection, name, self);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitIterNode(org.jruby.ast.IterNode node) {
        return translateBlockLikeNode(node, false);
    }

    @Override
    public RubyNode visitLambdaNode(org.jruby.ast.LambdaNode node) {
        return translateBlockLikeNode(node, true);
    }

    private RubyNode translateBlockLikeNode(org.jruby.ast.IterNode node, boolean isLambda) {
        final SourceSection sourceSection = translate(node.getPosition());
        final org.jruby.ast.ArgsNode argsNode = node.getArgsNode();

        // Unset this flag for any for any blocks within the for statement's body
        final boolean hasOwnScope = isLambda || !translatingForStatement;

        final String name = isLambda ? "(lambda)" : currentCallMethodName;
        final boolean isProc = !isLambda;

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(sourceSection, environment.getLexicalScope(), MethodTranslator.getArity(argsNode), name, true, Helpers.argsNodeToArgumentDescriptors(argsNode), false, false, false);

        final String namedMethodName = isLambda ? sharedMethodInfo.getName(): environment.getNamedMethodName();

        final ParseEnvironment parseEnvironment = environment.getParseEnvironment();
        final ReturnID returnID = isLambda ? parseEnvironment.allocateReturnID() : environment.getReturnID();

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, parseEnvironment, returnID, hasOwnScope, false,
                sharedMethodInfo, namedMethodName, environment.getBlockDepth() + 1, parseEnvironment.allocateBreakID());
        final MethodTranslator methodCompiler = new MethodTranslator(currentNode, context, this, newEnvironment, true, source, argsNode);

        if (isProc) {
            methodCompiler.translatingForStatement = translatingForStatement;
        }

        methodCompiler.frameOnStackMarkerSlotStack = frameOnStackMarkerSlotStack;

        final ProcType type = isLambda ? ProcType.LAMBDA : ProcType.PROC;

        if (isLambda) {
            frameOnStackMarkerSlotStack.push(BAD_FRAME_SLOT);
        }

        final RubyNode definitionNode;

        try {
            definitionNode = methodCompiler.compileBlockNode(sourceSection, sharedMethodInfo.getName(), node.getBodyNode(), sharedMethodInfo, type);
        } finally {
            if (isLambda) {
                frameOnStackMarkerSlotStack.pop();
            }
        }

        return addNewlineIfNeeded(node, definitionNode);
    }

    @Override
    public RubyNode visitLocalAsgnNode(org.jruby.ast.LocalAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        if (environment.getNeverAssignInParentScope()) {
            environment.declareVar(node.getName());
        }

        ReadLocalNode lhs = environment.findLocalVarNode(node.getName(), sourceSection);

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

        final RubyNode ret = lhs.makeWriteNode(rhs);
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
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getRegexpNode(), "=~", argsNode, null);
        copyNewline(node, callNode);
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
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        copyNewline(node, callNode);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMatch3Node(org.jruby.ast.Match3Node node) {
        // Triggered when a Regexp literal is the RHS of an expression.

        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        copyNewline(node, callNode);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitModuleNode(org.jruby.ast.ModuleNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getCPath().getName();

        RubyNode lexicalParent = translateCPath(sourceSection, node.getCPath());

        final DefineModuleNode defineModuleNode = DefineModuleNodeGen.create(context, sourceSection, name, lexicalParent);

        final RubyNode ret = openModule(sourceSection, defineModuleNode, name, node.getBodyNode(), false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(org.jruby.ast.MultipleAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.ListNode preArray = node.getPre();
        final org.jruby.ast.ListNode postArray = node.getPost();
        final org.jruby.ast.Node rhs = node.getValueNode();

        RubyNode rhsTranslated;

        if (rhs == null) {
            context.getJRubyRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, source.getName(), node.getPosition().getLine(), "no RHS for multiple assignment - using nil");
            rhsTranslated = nilNode(sourceSection);
        } else {
            rhsTranslated = rhs.accept(this);
        }

        final RubyNode result;

        // TODO CS 5-Jan-15 we shouldn't be doing this kind of low level optimisation or pattern matching - EA should do it for us

        if (preArray != null
                && node.getPost() == null
                && node.getRest() == null
                && rhsTranslated instanceof ArrayLiteralNode
                && ((ArrayLiteralNode) rhsTranslated).getSize() == preArray.size()) {
            /*
             * We can deal with this common case be rewriting
             *
             * a, b = c, d
             *
             * as
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

            final ArrayLiteralNode rhsArrayLiteral = (ArrayLiteralNode) rhsTranslated;
            final int assignedValuesCount = preArray.size();

            final RubyNode[] sequence = new RubyNode[assignedValuesCount * 2];

            final RubyNode[] tempValues = new RubyNode[assignedValuesCount];

            for (int n = 0; n < assignedValuesCount; n++) {
                final String tempName = environment.allocateLocalTemp("multi");
                final ReadLocalNode readTemp = environment.findLocalVarNode(tempName, sourceSection);
                final RubyNode assignTemp = readTemp.makeWriteNode(rhsArrayLiteral.stealNode(n));
                final RubyNode assignFinalValue = translateDummyAssignment(preArray.get(n), NodeUtil.cloneNode(readTemp));

                sequence[n] = assignTemp;
                sequence[assignedValuesCount + n] = assignFinalValue;

                tempValues[n] = NodeUtil.cloneNode(readTemp);
            }

            final RubyNode blockNode = sequence(context, sourceSection, Arrays.asList(sequence));

            final ArrayLiteralNode arrayNode = ArrayLiteralNode.create(context, sourceSection, tempValues);

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
             * a = x[0] etc
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
            final RubyNode writeTempRHS = environment.findLocalVarNode(tempRHSName, sourceSection).makeWriteNode(rhsTranslated);
            sequence.add(writeTempRHS);

            /*
             * Create a temp for the array.
             */

            final String tempName = environment.allocateLocalTemp("array");

            /*
             * Create a sequence of instructions, with the first being the literal array assigned to
             * the temp.
             */

            final RubyNode splatCastNode = SplatCastNodeGen.create(context, sourceSection, translatingNextExpression ? SplatCastNode.NilBehavior.EMPTY_ARRAY : SplatCastNode.NilBehavior.ARRAY_WITH_NIL, true, environment.findLocalVarNode(tempRHSName, sourceSection));

            final RubyNode writeTemp = environment.findLocalVarNode(tempName, sourceSection).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            for (int n = 0; n < preArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), n);

                sequence.add(translateDummyAssignment(preArray.get(n), assignedValue));
            }

            if (node.getRest() != null) {
                RubyNode assignedValue = ArrayGetTailNodeGen.create(context, sourceSection, preArray.size(), environment.findLocalVarNode(tempName, sourceSection));

                if (postArray != null) {
                    assignedValue = ArrayDropTailNodeGen.create(context, sourceSection, postArray.size(), assignedValue);
                }

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            if (postArray != null) {
                final List<RubyNode> smallerSequence = new ArrayList<>();

                for (int n = 0; n < postArray.size(); n++) {
                    final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), node.getPreCount() + n);
                    smallerSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
                }

                final RubyNode smaller = sequence(context, sourceSection, smallerSequence);

                final List<RubyNode> atLeastAsLargeSequence = new ArrayList<>();

                for (int n = 0; n < postArray.size(); n++) {
                    final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), -(postArray.size() - n));

                    atLeastAsLargeSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
                }

                final RubyNode atLeastAsLarge = sequence(context, sourceSection, atLeastAsLargeSequence);

                final RubyNode assignPost =
                        new IfElseNode(context, sourceSection,
                                new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), environment.findLocalVarNode(tempName, sourceSection)),
                                atLeastAsLarge,
                                smaller);

                sequence.add(assignPost);
            }

            result = new ElidableResultNode(context, sourceSection, sequence(context, sourceSection, sequence), environment.findLocalVarNode(tempRHSName, sourceSection));
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

            final List<RubyNode> sequence = new ArrayList<>();

            SplatCastNode.NilBehavior nilBehavior;

            if (translatingNextExpression) {
                nilBehavior = SplatCastNode.NilBehavior.EMPTY_ARRAY;
            } else {
                if (rhsTranslated instanceof SplatCastNode && ((SplatCastNodeGen) rhsTranslated).getChild() instanceof NilLiteralNode) {
                    rhsTranslated = ((SplatCastNodeGen) rhsTranslated).getChild();
                    nilBehavior = SplatCastNode.NilBehavior.CONVERT;
                } else {
                    nilBehavior = SplatCastNode.NilBehavior.ARRAY_WITH_NIL;
                }
            }

            final String tempRHSName = environment.allocateLocalTemp("rhs");
            final RubyNode writeTempRHS = environment.findLocalVarNode(tempRHSName, sourceSection).makeWriteNode(rhsTranslated);
            sequence.add(writeTempRHS);

            final SplatCastNode rhsSplatCast = SplatCastNodeGen.create(context, sourceSection,
                    nilBehavior,
                    true, environment.findLocalVarNode(tempRHSName, sourceSection));

            final String tempRHSSplattedName = environment.allocateLocalTemp("rhs");
            final RubyNode writeTempSplattedRHS = environment.findLocalVarNode(tempRHSSplattedName, sourceSection).makeWriteNode(rhsSplatCast);
            sequence.add(writeTempSplattedRHS);

            sequence.add(translateDummyAssignment(node.getRest(), environment.findLocalVarNode(tempRHSSplattedName, sourceSection)));

            final RubyNode assignmentResult;

            if (nilBehavior == SplatCastNode.NilBehavior.CONVERT) {
                assignmentResult = environment.findLocalVarNode(tempRHSSplattedName, sourceSection);
            } else {
                assignmentResult = environment.findLocalVarNode(tempRHSName, sourceSection);
            }

            result = new ElidableResultNode(context, sourceSection, sequence(context, sourceSection, sequence), assignmentResult);
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
            result = translateDummyAssignment(node.getRest(), rhsTranslated);
        } else if (node.getPre() == null && node.getRest() != null && node.getPost() != null) {
            /*
             * Something like
             *
             *     *a,b = [1, 2, 3, 4]
             */

            // This is very similar to the case with pre and rest, so unify with that

            final List<RubyNode> sequence = new ArrayList<>();

            final String tempRHSName = environment.allocateLocalTemp("rhs");
            final RubyNode writeTempRHS = environment.findLocalVarNode(tempRHSName, sourceSection).makeWriteNode(rhsTranslated);
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

            final RubyNode writeTemp = environment.findLocalVarNode(tempName, sourceSection).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            if (node.getRest() != null) {
                final ArrayDropTailNode assignedValue = ArrayDropTailNodeGen.create(context, sourceSection, postArray.size(), environment.findLocalVarNode(tempName, sourceSection));

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            final List<RubyNode> smallerSequence = new ArrayList<>();

            for (int n = 0; n < postArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), node.getPreCount() + n);
                smallerSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
            }

            final RubyNode smaller = sequence(context, sourceSection, smallerSequence);

            final List<RubyNode> atLeastAsLargeSequence = new ArrayList<>();

            for (int n = 0; n < postArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, sourceSection, environment.findLocalVarNode(tempName, sourceSection), -(postArray.size() - n));

                atLeastAsLargeSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
            }

            final RubyNode atLeastAsLarge = sequence(context, sourceSection, atLeastAsLargeSequence);

            final RubyNode assignPost =
                    new IfElseNode(context, sourceSection,
                    new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), environment.findLocalVarNode(tempName, sourceSection)),
                            atLeastAsLarge,
                            smaller);

            sequence.add(assignPost);

            result = new ElidableResultNode(context, sourceSection, sequence(context, sourceSection, sequence), environment.findLocalVarNode(tempRHSName, sourceSection));
        } else {
            context.getJRubyRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, source.getName(), node.getPosition().getLine(), node + " unknown form of multiple assignment");
            result = nilNode(sourceSection);
        }

        final RubyNode ret = new DefinedWrapperNode(context, sourceSection, context.getCoreStrings().ASSIGNMENT, result);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitNextNode(org.jruby.ast.NextNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        if (!environment.isBlock() && !translatingWhile) {
            throw new RaiseException(context.getCoreExceptions().syntaxError("Invalid next", currentNode));
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

        final RubyNode ret = new DefinedWrapperNode(context, sourceSection, context.getCoreStrings().ASSIGNMENT, new AndNode(context, sourceSection, lhs.accept(this), rhs.accept(this)));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnNode(org.jruby.ast.OpAsgnNode node) {
        if (node.getOperatorName().equals("||")) {
            // Why does this ||= come through as a visitOpAsgnNode and not a visitOpAsgnOrNode?

            final String temp = environment.allocateLocalTemp("opassign");
            final org.jruby.ast.Node writeReceiverToTemp = new org.jruby.ast.LocalAsgnNode(node.getPosition(), temp, 0, node.getReceiverNode());
            final org.jruby.ast.Node readReceiverFromTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);

            final org.jruby.ast.Node readMethod = new org.jruby.ast.CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName(), null, null);
            final org.jruby.ast.Node writeMethod = new org.jruby.ast.CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName() + "=", buildArrayNode(node.getPosition(),
                    node.getValueNode()), null);

            final SourceSection sourceSection = translate(node.getPosition());

            RubyNode lhs = readMethod.accept(this);
            RubyNode rhs = writeMethod.accept(this);

            final RubyNode ret = new DefinedWrapperNode(context, sourceSection, context.getCoreStrings().ASSIGNMENT,
                    sequence(context, sourceSection, Arrays.asList(writeReceiverToTemp.accept(this), new OrNode(context, sourceSection, lhs, rhs))));

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

        final org.jruby.ast.Node readMethod = new org.jruby.ast.CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName(), null, null);
        final org.jruby.ast.Node operation = new org.jruby.ast.CallNode(node.getPosition(), readMethod, node.getOperatorName(), buildArrayNode(node.getPosition(), node.getValueNode()), null);
        final org.jruby.ast.Node writeMethod = new org.jruby.ast.CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName() + "=", buildArrayNode(node.getPosition(),
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

        final RubyNode ret = new DefinedWrapperNode(context, sourceSection, context.getCoreStrings().ASSIGNMENT,
                new OrNode(context, sourceSection, lhs, rhs));

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

        final org.jruby.ast.Node arrayRead = new org.jruby.ast.CallNode(node.getPosition(), readArrayFromTemp, "[]", buildArrayNode(node.getPosition(), index), null);

        final String op = node.getOperatorName();

        org.jruby.ast.Node operation = null;

        if (op.equals("||")) {
            operation = new org.jruby.ast.OrNode(node.getPosition(), arrayRead, operand);
        } else if (op.equals("&&")) {
            operation = new org.jruby.ast.AndNode(node.getPosition(), arrayRead, operand);
        } else {
            operation = new org.jruby.ast.CallNode(node.getPosition(), arrayRead, node.getOperatorName(), buildArrayNode(node.getPosition(), operand), null);
        }

        copyNewline(node, operation);

        final org.jruby.ast.Node arrayWrite = new org.jruby.ast.CallNode(node.getPosition(), readArrayFromTemp, "[]=", buildArrayNode(node.getPosition(), index, operation), null);

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
    public RubyNode visitPreExeNode(org.jruby.ast.PreExeNode node) {
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitPostExeNode(org.jruby.ast.PostExeNode node) {
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRationalNode(org.jruby.ast.RationalNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        // TODO(CS): use IntFixnumLiteralNode where possible

        final RubyNode ret = translateRationalComplex(sourceSection, "Rational",
                new LongFixnumLiteralNode(context, sourceSection, node.getNumerator()),
                new LongFixnumLiteralNode(context, sourceSection, node.getDenominator()));

        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateRationalComplex(SourceSection sourceSection, String name, RubyNode a, RubyNode b) {
        // Translate as Truffle.privately { Rational.convert(a, b) }

        final RubyNode moduleNode = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        return new RubyCallNode(
                context, sourceSection, "convert",
                new ReadLiteralConstantNode(context, sourceSection, moduleNode, name),
                null, false, true, new RubyNode[]{ a, b });
    }

    @Override
    public RubyNode visitRedoNode(org.jruby.ast.RedoNode node) {
        if (!environment.isBlock() && !translatingWhile) {
            throw new RaiseException(context.getCoreExceptions().syntaxError("Invalid redo", currentNode));
        }

        final RubyNode ret = new RedoNode(context, translate(node.getPosition()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRegexpNode(org.jruby.ast.RegexpNode node) {
        final Rope rope = StringOperations.ropeFromByteList(node.getValue());
        Regex regex = RegexpNodes.compile(currentNode, context, rope, node.getOptions());

        // The RegexpNodes.compile operation may modify the encoding of the source rope. This modified copy is stored
        // in the Regex object as the "user object". Since ropes are immutable, we need to take this updated copy when
        // constructing the final regexp.
        final DynamicObject regexp = RegexpNodes.createRubyRegexp(context.getCoreLibrary().getRegexpClass(), regex, (Rope) regex.getUserObject(), node.getOptions());
        Layouts.REGEXP.getOptions(regexp).setLiteral(true);

        final ObjectLiteralNode literalNode = new ObjectLiteralNode(context, translate(node.getPosition()), regexp);

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

        if (context.getOptions().BACKTRACES_OMIT_UNUSED
                && rescueBody != null
                && rescueBody.getExceptionNodes() == null
                && rescueBody.getBodyNode() instanceof SideEffectFree
                // allow `expression rescue $!` pattern
                && (!(rescueBody.getBodyNode() instanceof org.jruby.ast.GlobalVarNode) || !((org.jruby.ast.GlobalVarNode) rescueBody.getBodyNode()).getName().equals("$!"))
                && rescueBody.getOptRescueNode() == null) {
            tryPart = new DisablingBacktracesNode(context, sourceSection, tryPart);

            RubyNode bodyNode;

            if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                bodyNode = nilNode(sourceSection);
            } else {
                bodyNode = rescueBody.getBodyNode().accept(this);
            }

            final RescueAnyNode rescueNode = new RescueAnyNode(context, sourceSection, bodyNode);
            rescueNodes.add(rescueNode);
        } else {
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
        }

        RubyNode elsePart;

        if (node.getElseNode() == null || node.getElseNode().getPosition() == InvalidSourcePosition.INSTANCE) {
            elsePart = null; //nilNode(sourceSection);
        } else {
            elsePart = node.getElseNode().accept(this);
        }

        final RubyNode ret = new TryNode(context, sourceSection,
                new ExceptionTranslatingNode(context, sourceSection, tryPart, UnsupportedOperationBehavior.TYPE_ERROR),
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

        final RubyNode ret = openModule(sourceSection, singletonClassNode, "(singleton-def)", node.getBodyNode(), true);
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
        final RubyNode ret = SplatCastNodeGen.create(context, sourceSection, SplatCastNode.NilBehavior.CONVERT, false, value);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitStrNode(org.jruby.ast.StrNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ByteList byteList = node.getValue();
        final int codeRange = node.getCodeRange();
        final Rope rope = context.getRopeTable().getRope(byteList.bytes(), byteList.getEncoding(), CodeRange.fromInt(codeRange));

        final RubyNode ret;

        if (node.isFrozen() && !getSourcePath(sourceSection).startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/")) {
            final DynamicObject frozenString = context.getFrozenStrings().getFrozenString(rope);

            ret = new DefinedWrapperNode(context, sourceSection, context.getCoreStrings().METHOD,
                    new ObjectLiteralNode(context, null, frozenString));
        } else {
            ret = new StringLiteralNode(context, sourceSection, rope);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSymbolNode(org.jruby.ast.SymbolNode node) {
        final Rope rope = StringOperations.createRope(node.getName(), node.getEncoding());
        final RubyNode ret = new ObjectLiteralNode(context, translate(node.getPosition()), context.getSymbolTable().getSymbol(rope));
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
        final DynamicObject nameSymbol = translateNameNodeToSymbol(node.getName());

        final RubyNode ret = ModuleNodesFactory.UndefMethodNodeFactory.create(context, sourceSection, new RubyNode[]{
                new RaiseIfFrozenNode(new GetDefaultDefineeNode(context, sourceSection)),
                new ObjectLiteralNode(context, sourceSection, new Object[]{ nameSymbol })
        });
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitUntilNode(org.jruby.ast.UntilNode node) {
        org.jruby.ast.WhileNode whileNode = new org.jruby.ast.WhileNode(node.getPosition(), node.getConditionNode(), node.getBodyNode(), node.evaluateAtStart());
        copyNewline(node, whileNode);
        final RubyNode ret = translateWhileNode(whileNode, true);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitVCallNode(org.jruby.ast.VCallNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        if (node.getName().equals("undefined") && getSourcePath(sourceSection).startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/")) {
            final RubyNode ret = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getRubiniusUndefined());
            return addNewlineIfNeeded(node, ret);
        }

        final org.jruby.ast.Node receiver = new org.jruby.ast.SelfNode(node.getPosition());
        final org.jruby.ast.CallNode callNode = new org.jruby.ast.CallNode(node.getPosition(), receiver, node.getName(), null, null);
        copyNewline(node, callNode);
        final RubyNode ret = translateCallNode(callNode, true, true);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitWhileNode(org.jruby.ast.WhileNode node) {
        final RubyNode ret = translateWhileNode(node, false);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateWhileNode(org.jruby.ast.WhileNode node, boolean conditionInversed) {
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
        frameOnStackMarkerSlotStack.push(BAD_FRAME_SLOT);
        try {
            body = translateNodeOrNil(sourceSection, node.getBodyNode());
        } finally {
            frameOnStackMarkerSlotStack.pop();
            environment.setBreakIDForWhile(oldBreakID);
            translatingWhile = oldTranslatingWhile;
        }

        final RubyNode loop;

        if (node.evaluateAtStart()) {
            loop = WhileNode.createWhile(context, sourceSection, condition, body);
        } else {
            loop = WhileNode.createDoWhile(context, sourceSection, condition, body);
        }

        final RubyNode ret = new CatchBreakNode(context, sourceSection, whileBreakID, loop);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitXStrNode(org.jruby.ast.XStrNode node) {
        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), new org.jruby.ast.StrNode(node.getPosition(), node.getValue()));
        final org.jruby.ast.Node callNode = new org.jruby.ast.FCallNode(node.getPosition(), "`", argsNode, null);
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

        final RubyNode ret = new YieldExpressionNode(context, translate(node.getPosition()), unsplat, argumentsTranslatedArray);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitZArrayNode(org.jruby.ast.ZArrayNode node) {
        final RubyNode[] values = new RubyNode[0];

        final RubyNode ret = ArrayLiteralNode.create(context, translate(node.getPosition()), values);
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

    @Override
    public RubyNode visitStarNode(org.jruby.ast.StarNode star) {
        return nilNode(translate(star.getPosition()));
    }

    protected RubyNode initFlipFlopStates(SourceSection sourceSection) {
        final RubyNode[] initNodes = new RubyNode[environment.getFlipFlopStates().size()];

        for (int n = 0; n < initNodes.length; n++) {
            initNodes[n] = new InitFlipFlopSlotNode(context, sourceSection, environment.getFlipFlopStates().get(n));
        }

        return sequence(context, sourceSection, Arrays.asList(initNodes));
    }

    @Override
    protected RubyNode defaultVisit(org.jruby.ast.Node node) {
        final RubyNode ret = unimplemented(node);
        return addNewlineIfNeeded(node, ret);
    }

    protected RubyNode unimplemented(org.jruby.ast.Node node) {
        context.getJRubyRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, source.getName(), node.getPosition().getLine(), node + " does nothing - translating as nil");
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

            if (environment.getBlockDepth() > 1) {
                return String.format("block (%d levels) in %s", environment.getBlockDepth(), methodParent.getNamedMethodName());
            } else {
                return String.format("block in %s", methodParent.getNamedMethodName());
            }
        } else {
            return environment.getNamedMethodName();
        }
    }

    @Override
    public RubyNode visitOther(org.jruby.ast.Node node) {
        if (node instanceof ReadLocalDummyNode) {
            final ReadLocalDummyNode readLocal = (ReadLocalDummyNode) node;
            final RubyNode ret = new ReadLocalVariableNode(context, readLocal.getSourceSection(), LocalVariableType.FRAME_LOCAL, readLocal.getFrameSlot());
            return addNewlineIfNeeded(node, ret);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void copyNewline(org.jruby.ast.Node from, org.jruby.ast.Node to) {
        if (from.isNewline()) {
            to.setNewline();
        }
    }

    private RubyNode addNewlineIfNeeded(org.jruby.ast.Node jrubyNode, RubyNode node) {
        if (jrubyNode.isNewline()) {
            final SourceSection current = node.getEncapsulatingSourceSection();

            if (current == null) {
                return node;
            }

            if (context.getCoverageManager() != null) {
                context.getCoverageManager().setLineHasCode(current.getLineLocation());
            }
            node.unsafeSetIsNewLine();
        }

        return node;
    }

}
