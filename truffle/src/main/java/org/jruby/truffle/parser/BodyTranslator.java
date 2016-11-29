/*
 * Copyright (c) 2013, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.parser;

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
import org.jruby.runtime.ArgumentDescriptor;
import org.jruby.runtime.Visibility;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.builtins.PrimitiveNodeConstructor;
import org.jruby.truffle.core.CoreLibrary;
import org.jruby.truffle.core.IsNilNode;
import org.jruby.truffle.core.IsRubiniusUndefinedNode;
import org.jruby.truffle.core.RaiseIfFrozenNode;
import org.jruby.truffle.core.VMPrimitiveNodesFactory;
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
import org.jruby.truffle.core.regexp.RegexpOptions;
import org.jruby.truffle.core.rope.CodeRange;
import org.jruby.truffle.core.rope.Rope;
import org.jruby.truffle.core.rope.RopeConstants;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.rubinius.RubiniusLastStringReadNode;
import org.jruby.truffle.core.rubinius.RubiniusLastStringWriteNodeGen;
import org.jruby.truffle.core.string.InterpolatedStringNode;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.LexicalScope;
import org.jruby.truffle.language.RubyNode;
import org.jruby.truffle.language.RubyRootNode;
import org.jruby.truffle.language.RubySourceSection;
import org.jruby.truffle.language.arguments.ArrayIsAtLeastAsLargeAsNode;
import org.jruby.truffle.language.arguments.SingleBlockArgNode;
import org.jruby.truffle.language.constants.ReadConstantNode;
import org.jruby.truffle.language.constants.ReadConstantWithLexicalScopeNode;
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
import org.jruby.truffle.language.dispatch.RubyCallNodeParameters;
import org.jruby.truffle.language.exceptions.DisablingBacktracesNode;
import org.jruby.truffle.language.exceptions.EnsureNode;
import org.jruby.truffle.language.exceptions.RescueAnyNode;
import org.jruby.truffle.language.exceptions.RescueClassesNode;
import org.jruby.truffle.language.exceptions.RescueNode;
import org.jruby.truffle.language.exceptions.RescueSplatNode;
import org.jruby.truffle.language.exceptions.TryNode;
import org.jruby.truffle.language.globals.AliasGlobalVarNode;
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
import org.jruby.truffle.language.threadlocal.GetFromThreadLocalNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObjectNode;
import org.jruby.truffle.language.threadlocal.ThreadLocalObjectNodeGen;
import org.jruby.truffle.language.threadlocal.WrapInThreadLocalNodeGen;
import org.jruby.truffle.language.yield.YieldExpressionNode;
import org.jruby.truffle.parser.ast.AliasParseNode;
import org.jruby.truffle.parser.ast.AndParseNode;
import org.jruby.truffle.parser.ast.ArgsCatParseNode;
import org.jruby.truffle.parser.ast.ArgsParseNode;
import org.jruby.truffle.parser.ast.ArgsPushParseNode;
import org.jruby.truffle.parser.ast.ArgumentParseNode;
import org.jruby.truffle.parser.ast.ArrayParseNode;
import org.jruby.truffle.parser.ast.AssignableParseNode;
import org.jruby.truffle.parser.ast.AttrAssignParseNode;
import org.jruby.truffle.parser.ast.BackRefParseNode;
import org.jruby.truffle.parser.ast.BeginParseNode;
import org.jruby.truffle.parser.ast.BignumParseNode;
import org.jruby.truffle.parser.ast.BlockParseNode;
import org.jruby.truffle.parser.ast.BlockPassParseNode;
import org.jruby.truffle.parser.ast.BreakParseNode;
import org.jruby.truffle.parser.ast.CallParseNode;
import org.jruby.truffle.parser.ast.CaseParseNode;
import org.jruby.truffle.parser.ast.ClassParseNode;
import org.jruby.truffle.parser.ast.ClassVarAsgnParseNode;
import org.jruby.truffle.parser.ast.ClassVarParseNode;
import org.jruby.truffle.parser.ast.Colon2ConstParseNode;
import org.jruby.truffle.parser.ast.Colon2ImplicitParseNode;
import org.jruby.truffle.parser.ast.Colon2ParseNode;
import org.jruby.truffle.parser.ast.Colon3ParseNode;
import org.jruby.truffle.parser.ast.ComplexParseNode;
import org.jruby.truffle.parser.ast.ConstDeclParseNode;
import org.jruby.truffle.parser.ast.ConstParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.DRegexpParseNode;
import org.jruby.truffle.parser.ast.DStrParseNode;
import org.jruby.truffle.parser.ast.DSymbolParseNode;
import org.jruby.truffle.parser.ast.DVarParseNode;
import org.jruby.truffle.parser.ast.DXStrParseNode;
import org.jruby.truffle.parser.ast.DefinedParseNode;
import org.jruby.truffle.parser.ast.DefnParseNode;
import org.jruby.truffle.parser.ast.DefsParseNode;
import org.jruby.truffle.parser.ast.DotParseNode;
import org.jruby.truffle.parser.ast.EncodingParseNode;
import org.jruby.truffle.parser.ast.EnsureParseNode;
import org.jruby.truffle.parser.ast.EvStrParseNode;
import org.jruby.truffle.parser.ast.FCallParseNode;
import org.jruby.truffle.parser.ast.FalseParseNode;
import org.jruby.truffle.parser.ast.FixnumParseNode;
import org.jruby.truffle.parser.ast.FlipParseNode;
import org.jruby.truffle.parser.ast.FloatParseNode;
import org.jruby.truffle.parser.ast.ForParseNode;
import org.jruby.truffle.parser.ast.GlobalAsgnParseNode;
import org.jruby.truffle.parser.ast.GlobalVarParseNode;
import org.jruby.truffle.parser.ast.HashParseNode;
import org.jruby.truffle.parser.ast.IfParseNode;
import org.jruby.truffle.parser.ast.InstAsgnParseNode;
import org.jruby.truffle.parser.ast.InstVarParseNode;
import org.jruby.truffle.parser.ast.IterParseNode;
import org.jruby.truffle.parser.ast.LambdaParseNode;
import org.jruby.truffle.parser.ast.ListParseNode;
import org.jruby.truffle.parser.ast.LiteralParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.LocalVarParseNode;
import org.jruby.truffle.parser.ast.Match2ParseNode;
import org.jruby.truffle.parser.ast.Match3ParseNode;
import org.jruby.truffle.parser.ast.MatchParseNode;
import org.jruby.truffle.parser.ast.ModuleParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.NextParseNode;
import org.jruby.truffle.parser.ast.NilParseNode;
import org.jruby.truffle.parser.ast.NthRefParseNode;
import org.jruby.truffle.parser.ast.OpAsgnAndParseNode;
import org.jruby.truffle.parser.ast.OpAsgnConstDeclParseNode;
import org.jruby.truffle.parser.ast.OpAsgnOrParseNode;
import org.jruby.truffle.parser.ast.OpAsgnParseNode;
import org.jruby.truffle.parser.ast.OpElementAsgnParseNode;
import org.jruby.truffle.parser.ast.OrParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.PostExeParseNode;
import org.jruby.truffle.parser.ast.PreExeParseNode;
import org.jruby.truffle.parser.ast.RationalParseNode;
import org.jruby.truffle.parser.ast.RedoParseNode;
import org.jruby.truffle.parser.ast.RegexpParseNode;
import org.jruby.truffle.parser.ast.RescueBodyParseNode;
import org.jruby.truffle.parser.ast.RescueParseNode;
import org.jruby.truffle.parser.ast.RetryParseNode;
import org.jruby.truffle.parser.ast.ReturnParseNode;
import org.jruby.truffle.parser.ast.SClassParseNode;
import org.jruby.truffle.parser.ast.SValueParseNode;
import org.jruby.truffle.parser.ast.SelfParseNode;
import org.jruby.truffle.parser.ast.SideEffectFree;
import org.jruby.truffle.parser.ast.SplatParseNode;
import org.jruby.truffle.parser.ast.StarParseNode;
import org.jruby.truffle.parser.ast.StrParseNode;
import org.jruby.truffle.parser.ast.SymbolParseNode;
import org.jruby.truffle.parser.ast.TrueParseNode;
import org.jruby.truffle.parser.ast.TruffleFragmentParseNode;
import org.jruby.truffle.parser.ast.UndefParseNode;
import org.jruby.truffle.parser.ast.UntilParseNode;
import org.jruby.truffle.parser.ast.VAliasParseNode;
import org.jruby.truffle.parser.ast.VCallParseNode;
import org.jruby.truffle.parser.ast.WhenParseNode;
import org.jruby.truffle.parser.ast.WhileParseNode;
import org.jruby.truffle.parser.ast.XStrParseNode;
import org.jruby.truffle.parser.ast.YieldParseNode;
import org.jruby.truffle.parser.ast.ZArrayParseNode;
import org.jruby.truffle.parser.ast.visitor.NodeVisitor;
import org.jruby.truffle.parser.lexer.ISourcePosition;
import org.jruby.truffle.parser.lexer.InvalidSourcePosition;
import org.jruby.truffle.parser.parser.ParserSupport;
import org.jruby.truffle.parser.scope.StaticScope;
import org.jruby.truffle.platform.graal.AssertConstantNodeGen;
import org.jruby.truffle.platform.graal.AssertNotCompiledNodeGen;
import org.jruby.truffle.tools.ChaosNodeGen;
import org.jruby.truffle.util.StringUtils;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

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

    public BodyTranslator(com.oracle.truffle.api.nodes.Node currentNode, RubyContext context, BodyTranslator parent, TranslatorEnvironment environment, Source source, boolean topLevel) {
        super(currentNode, context, source);
        parserSupport = new ParserSupport(context);
        this.parent = parent;
        this.environment = environment;
    }

    private DynamicObject translateNameNodeToSymbol(ParseNode node) {
        return context.getSymbolTable().getSymbol(((LiteralParseNode) node).getName());
    }

    @Override
    public RubyNode visitAliasNode(AliasParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final DynamicObject oldName = translateNameNodeToSymbol(node.getOldName());
        final DynamicObject newName = translateNameNodeToSymbol(node.getNewName());

        /*
         * ostruct in the stdlib defines an #allocate method to be the same as #new, but our #new calls
         * #allocate with full lookup, and so this forms an infinite loop. MRI doesn't do full lookup
         * for #allocate so doesn't have the same problem. MRI bug #11884 about this is to workaround
         * a problem Pysch has. We'll fix that when we see it for real. For now this is the easier fix.
         */

        if (newName.toString().equals("allocate") && source.getName().endsWith("/ostruct.rb")) {
            return nilNode(source, sourceSection);
        }

        final RubyNode ret = ModuleNodesFactory.AliasMethodNodeFactory.create(
                new RaiseIfFrozenNode(context, fullSourceSection, new GetDefaultDefineeNode(context, fullSourceSection)),
                new ObjectLiteralNode(context, fullSourceSection, newName),
                new ObjectLiteralNode(context, fullSourceSection, oldName));

        ret.unsafeSetSourceSection(sourceSection);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitVAliasNode(VAliasParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new AliasGlobalVarNode(node.getOldName(), node.getNewName());

        ret.unsafeSetSourceSection(sourceSection);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitAndNode(AndParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final RubyNode x = translateNodeOrNil(sourceSection, node.getFirstNode());
        final RubyNode y = translateNodeOrNil(sourceSection, node.getSecondNode());

        final RubyNode ret = new AndNode(x, y);
        ret.unsafeSetSourceSection(sourceSection);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitArgsCatNode(ArgsCatParseNode node) {
        final List<ParseNode> nodes = new ArrayList<>();
        collectArgsCatNodes(nodes, node);

        final List<RubyNode> translatedNodes = new ArrayList<>();

        for (ParseNode catNode : nodes) {
            translatedNodes.add(catNode.accept(this));
        }

        final RubyNode ret = new ArrayConcatNode(context, translate(node.getPosition()).toSourceSection(source), translatedNodes.toArray(new RubyNode[translatedNodes.size()]));
        return addNewlineIfNeeded(node, ret);
    }

    // ArgsCatNodes can be nested - this collects them into a flat list of children
    private void collectArgsCatNodes(List<ParseNode> nodes, ArgsCatParseNode node) {
        if (node.getFirstNode() instanceof ArgsCatParseNode) {
            collectArgsCatNodes(nodes, (ArgsCatParseNode) node.getFirstNode());
        } else {
            nodes.add(node.getFirstNode());
        }

        if (node.getSecondNode() instanceof ArgsCatParseNode) {
            collectArgsCatNodes(nodes, (ArgsCatParseNode) node.getSecondNode());
        } else {
            // ArgsCatParseNode implicitly splat its second argument. See Helpers.argsCat.
            ParseNode secondNode = new SplatParseNode(node.getSecondNode().getPosition(), node.getSecondNode());
            nodes.add(secondNode);
        }
    }

    @Override
    public RubyNode visitArgsPushNode(ArgsPushParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode args = node.getFirstNode().accept(this);
        final RubyNode value = node.getSecondNode().accept(this);
        final RubyNode ret = ArrayAppendOneNodeGen.create(context, fullSourceSection,
                KernelNodesFactory.DupNodeFactory.create(context, fullSourceSection, new RubyNode[] { args }),
                value);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitArrayNode(ArrayParseNode node) {
        final ParseNode[] values = node.children();

        final RubyNode[] translatedValues = new RubyNode[values.length];

        for (int n = 0; n < values.length; n++) {
            translatedValues[n] = values[n].accept(this);
        }

        final RubyNode ret = ArrayLiteralNode.create(context, translate(node.getPosition()), translatedValues);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitAttrAssignNode(AttrAssignParseNode node) {
        final CallParseNode callNode = new CallParseNode(
                node.getPosition(), node.getReceiverNode(), node.getName(), node.getArgsNode(), null, node.isLazy());

        copyNewline(node, callNode);
        boolean isAccessorOnSelf = (node.getReceiverNode() instanceof SelfParseNode);
        final RubyNode actualCall = translateCallNode(callNode, isAccessorOnSelf, false, true);

        return addNewlineIfNeeded(node, actualCall);
    }

    @Override
    public RubyNode visitBeginNode(BeginParseNode node) {
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBignumNode(BignumParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        // These aren't always Bignums!

        final BigInteger value = node.getValue();
        final RubyNode ret;

        if (value.bitLength() >= 64) {
            ret = new ObjectLiteralNode(context, fullSourceSection, BignumOperations.createBignum(context, node.getValue()));
        } else {
            ret = new LongFixnumLiteralNode(context, fullSourceSection, value.longValue());
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBlockNode(BlockParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> translatedChildren = new ArrayList<>();

        final int firstLine = node.getPosition().getLine() + 1;
        int lastLine = firstLine;

        for (ParseNode child : node.children()) {
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
            ret = sequence(context, source, new RubySourceSection(firstLine, lastLine), translatedChildren);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBreakNode(BreakParseNode node) {
        assert environment.isBlock() || translatingWhile : "The parser did not see an invalid break";

        final RubySourceSection sourceSection = translate(node.getPosition());

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

        final RubyNode ret = new BreakNode(environment.getBreakID(), translatingWhile, resultNode);
        ret.unsafeSetSourceSection(sourceSection);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitCallNode(CallParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final ParseNode receiver = node.getReceiverNode();
        final String methodName = node.getName();

        if (receiver instanceof StrParseNode && methodName.equals("freeze")) {
            final StrParseNode strNode = (StrParseNode) receiver;
            final ByteList byteList = strNode.getValue();
            final int codeRange = strNode.getCodeRange();

            final Rope rope = context.getRopeTable().getRope(byteList.bytes(), byteList.getEncoding(), CodeRange.fromInt(codeRange));

            final DynamicObject frozenString = context.getFrozenStrings().getFrozenString(rope);

            return addNewlineIfNeeded(node, new DefinedWrapperNode(context, fullSourceSection, context.getCoreStrings().METHOD,
                    new ObjectLiteralNode(context, null, frozenString)));
        }

        if (receiver instanceof ConstParseNode
                && ((ConstParseNode) receiver).getName().equals("Truffle")) {
            // Truffle.<method>

            if (methodName.equals("primitive")) {
                final RubyNode ret = translateRubiniusPrimitive(fullSourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("invoke_primitive")) {
                final RubyNode ret = translateRubiniusInvokePrimitive(fullSourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("privately")) {
                final RubyNode ret = translateRubiniusPrivately(fullSourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("single_block_arg")) {
                final RubyNode ret = translateSingleBlockArg(fullSourceSection, node);
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("check_frozen")) {
                final RubyNode ret = translateCheckFrozen(fullSourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof Colon2ConstParseNode // Truffle::Graal.<method>
                && ((Colon2ConstParseNode) receiver).getLeftNode() instanceof ConstParseNode
                && ((ConstParseNode) ((Colon2ConstParseNode) receiver).getLeftNode()).getName().equals("Truffle")
                && ((Colon2ConstParseNode) receiver).getName().equals("Graal")) {
            if (methodName.equals("assert_constant")) {
                final RubyNode ret = AssertConstantNodeGen.create(context, fullSourceSection, node.getArgsNode().childNodes().get(0).accept(this));
                return addNewlineIfNeeded(node, ret);
            } else if (methodName.equals("assert_not_compiled")) {
                final RubyNode ret = AssertNotCompiledNodeGen.create(context, fullSourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (receiver instanceof VCallParseNode // undefined.equal?(obj)
                && ((VCallParseNode) receiver).getName().equals("undefined")
                && getSourcePath(sourceSection).startsWith(buildCorePath(""))
                && methodName.equals("equal?")) {
            RubyNode argument = translateArgumentsAndBlock(sourceSection, null, node.getArgsNode(), methodName).getArguments()[0];
            final RubyNode ret = new IsRubiniusUndefinedNode(context, fullSourceSection, argument);
            return addNewlineIfNeeded(node, ret);
        }

        return translateCallNode(node, false, false, false);
    }

    private RubyNode translateRubiniusPrimitive(SourceSection sourceSection, CallParseNode node) {
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
         * Where the arguments are the same arguments as the method. It looks like this is only exercised with simple
         * arguments so we're not worrying too much about what happens when they're more complicated (rest,
         * keywords etc).
         */

        if (node.getArgsNode().childNodes().size() != 1 || !(node.getArgsNode().childNodes().get(0) instanceof SymbolParseNode)) {
            throw new UnsupportedOperationException("Truffle.primitive must have a single literal symbol argument");
        }

        final String primitiveName = ((SymbolParseNode) node.getArgsNode().childNodes().get(0)).getName();

        final PrimitiveNodeConstructor primitive = context.getPrimitiveManager().getPrimitive(primitiveName);
        final ReturnID returnID = environment.getReturnID();
        return primitive.createCallPrimitiveNode(context, sourceSection, returnID);
    }

    private RubyNode translateRubiniusInvokePrimitive(SourceSection sourceSection, CallParseNode node) {
        /*
         * Translates something that looks like
         *
         *   Truffle.invoke_primitive :foo, arg1, arg2, argN
         *
         * into
         *
         *   InvokePrimitiveNode(FooNode(arg1, arg2, ..., argN))
         */

        final List<ParseNode> args = node.getArgsNode().childNodes();

        if (args.size() < 1 || !(args.get(0) instanceof SymbolParseNode)) {
            throw new UnsupportedOperationException("Truffle.invoke_primitive must have at least an initial literal symbol argument");
        }

        final String primitiveName = ((SymbolParseNode) args.get(0)).getName();

        final PrimitiveNodeConstructor primitive = context.getPrimitiveManager().getPrimitive(primitiveName);

        final List<RubyNode> arguments = new ArrayList<>();

        // The first argument was the symbol so we ignore it
        for (int n = 1; n < args.size(); n++) {
            RubyNode readArgumentNode = args.get(n).accept(this);
            arguments.add(readArgumentNode);
        }

        return primitive.createInvokePrimitiveNode(context, sourceSection, arguments.toArray(new RubyNode[arguments.size()]));
    }

    private RubyNode translateRubiniusPrivately(SourceSection sourceSection, CallParseNode node) {
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

        if (!(node.getIterNode() instanceof IterParseNode)) {
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
            return (((IterParseNode) node.getIterNode()).getBodyNode()).accept(this);
        } finally {
            // Restore the previous value of the privately flag - allowing for nesting

            privately = previousPrivately;
        }
    }

    public RubyNode translateSingleBlockArg(SourceSection sourceSection, CallParseNode node) {
        return new SingleBlockArgNode(context, sourceSection);
    }

    private RubyNode translateCheckFrozen(SourceSection sourceSection) {
        return new RaiseIfFrozenNode(context, sourceSection, new SelfNode(environment.getFrameDescriptor()));
    }

    private RubyNode translateCallNode(CallParseNode node, boolean ignoreVisibility, boolean isVCall, boolean isAttrAssign) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final RubyNode receiver = node.getReceiverNode().accept(this);

        ParseNode args = node.getArgsNode();
        ParseNode block = node.getIterNode();

        if (block == null && args instanceof IterParseNode) {
            block = args;
            args = null;
        }

        final String methodName = node.getName();
        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, block, args, methodName);

        final List<RubyNode> children = new ArrayList<>();

        if (argumentsAndBlock.getBlock() != null) {
            children.add(argumentsAndBlock.getBlock());
        }

        children.addAll(Arrays.asList(argumentsAndBlock.getArguments()));

        final RubySourceSection enclosingSourceSection = enclosing(sourceSection, children.toArray(new RubyNode[children.size()]));
        final SourceSection enclosingFullSourceSection = enclosingSourceSection.toSourceSection(source);

        RubyCallNodeParameters callParameters = new RubyCallNodeParameters(context, enclosingFullSourceSection, receiver, methodName, argumentsAndBlock.getBlock(), argumentsAndBlock.getArguments(), argumentsAndBlock.isSplatted(), privately || ignoreVisibility, isVCall, node.isLazy(), isAttrAssign);
        RubyNode translated = context.getCoreMethods().createCallNode(callParameters);

        if (argumentsAndBlock.getBlock() instanceof BlockDefinitionNode) { // if we have a literal block, break breaks out of this call site
            BlockDefinitionNode blockDef = (BlockDefinitionNode) argumentsAndBlock.getBlock();
            translated = new FrameOnStackNode(translated, argumentsAndBlock.getFrameOnStackMarkerSlot());
            translated = new CatchBreakNode(context, enclosingFullSourceSection, blockDef.getBreakID(), translated);
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

    protected ArgumentsAndBlockTranslation translateArgumentsAndBlock(RubySourceSection sourceSection, ParseNode iterNode, ParseNode argsNode, String nameToSetWhenTranslatingBlock) {
        assert !(argsNode instanceof IterParseNode);

        final List<ParseNode> arguments = new ArrayList<>();
        ParseNode blockPassNode = null;

        boolean isSplatted = false;

        if (argsNode instanceof ListParseNode) {
            arguments.addAll(argsNode.childNodes());
        } else if (argsNode instanceof BlockPassParseNode) {
            final BlockPassParseNode blockPass = (BlockPassParseNode) argsNode;

            final ParseNode blockPassArgs = blockPass.getArgsNode();

            if (blockPassArgs instanceof ListParseNode) {
                arguments.addAll(blockPassArgs.childNodes());
            } else if (blockPassArgs instanceof ArgsCatParseNode) {
                arguments.add(blockPassArgs);
            } else if (blockPassArgs != null) {
                throw new UnsupportedOperationException("Don't know how to block pass " + blockPassArgs);
            }

            blockPassNode = blockPass.getBodyNode();
        } else if (argsNode instanceof SplatParseNode) {
            isSplatted = true;
            arguments.add(argsNode);
        } else if (argsNode instanceof ArgsCatParseNode) {
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

        if (iterNode instanceof BlockPassParseNode) {
            blockPassNode = ((BlockPassParseNode) iterNode).getBodyNode();
        }

        currentCallMethodName = nameToSetWhenTranslatingBlock;


        final FrameSlot frameOnStackMarkerSlot;
        RubyNode blockTranslated;

        if (blockPassNode != null) {
            blockTranslated = ToProcNodeGen.create(context, sourceSection.toSourceSection(source), blockPassNode.accept(this));
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
    public RubyNode visitCaseNode(CaseParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        RubyNode elseNode = translateNodeOrNil(sourceSection, node.getElseNode());

        /*
         * There are two sorts of case - one compares a list of expressions against a value, the
         * other just checks a list of expressions for truth.
         */

        final RubyNode ret;

        if (node.getCaseNode() != null) {
            // Evaluate the case expression and store it in a local

            final String tempName = environment.allocateLocalTemp("case");

            final ReadLocalNode readTemp = environment.findLocalVarNode(tempName, source, sourceSection);

            final RubyNode assignTemp = readTemp.makeWriteNode(node.getCaseNode().accept(this));

            /*
             * Build an if expression from the whens and else. Work backwards because the first if
             * contains all the others in its else clause.
             */

            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final WhenParseNode when = (WhenParseNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<ParseNode> expressions;

                if (when.getExpressionNodes() instanceof ListParseNode && !(when.getExpressionNodes() instanceof ArrayParseNode)) {
                    expressions = when.getExpressionNodes().childNodes();
                } else {
                    expressions = Collections.singletonList(when.getExpressionNodes());
                }

                final List<RubyNode> comparisons = new ArrayList<>();

                for (ParseNode expressionNode : expressions) {
                    final RubyNode rubyExpression = expressionNode.accept(this);

                    final RubyNode receiver;
                    final RubyNode[] arguments;
                    final String method;
                    if (expressionNode instanceof SplatParseNode
                            || expressionNode instanceof ArgsCatParseNode
                            || expressionNode instanceof ArgsPushParseNode) {
                        receiver = new ObjectLiteralNode(context, fullSourceSection, context.getCoreLibrary().getTruffleModule());
                        method = "when_splat";
                        arguments = new RubyNode[] { rubyExpression, NodeUtil.cloneNode(readTemp) };
                    } else {
                        receiver = rubyExpression;
                        method = "===";
                        arguments = new RubyNode[] { NodeUtil.cloneNode(readTemp) };
                    }
                    RubyCallNodeParameters callParameters = new RubyCallNodeParameters(context, fullSourceSection, receiver, method, null, arguments, false, true);
                    comparisons.add(new RubyCallNode(callParameters));
                }

                RubyNode conditionNode = comparisons.get(comparisons.size() - 1);

                // As with the if nodes, we work backwards to make it left associative

                for (int i = comparisons.size() - 2; i >= 0; i--) {
                    conditionNode = new OrNode(comparisons.get(i), conditionNode);
                }

                // Create the if node

                final RubyNode thenNode = translateNodeOrNil(sourceSection, when.getBodyNode());

                final IfElseNode ifNode = new IfElseNode(conditionNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            final RubyNode ifNode = elseNode;

            // A top-level block assigns the temp then runs the if

            ret = sequence(context, source, sourceSection, Arrays.asList(assignTemp, ifNode));
        } else {
            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final WhenParseNode when = (WhenParseNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<ParseNode> expressions;

                if (when.getExpressionNodes() instanceof ListParseNode) {
                    expressions = when.getExpressionNodes().childNodes();
                } else {
                    expressions = Collections.singletonList(when.getExpressionNodes());
                }

                final List<RubyNode> tests = new ArrayList<>();

                for (ParseNode expressionNode : expressions) {
                    final RubyNode rubyExpression = expressionNode.accept(this);
                    tests.add(rubyExpression);
                }

                RubyNode conditionNode = tests.get(tests.size() - 1);

                // As with the if nodes, we work backwards to make it left associative

                for (int i = tests.size() - 2; i >= 0; i--) {
                    conditionNode = new OrNode(tests.get(i), conditionNode);
                }

                // Create the if node

                final RubyNode thenNode = when.getBodyNode().accept(this);

                final IfElseNode ifNode = new IfElseNode(conditionNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            ret = elseNode;
        }

        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode openModule(RubySourceSection sourceSection, RubyNode defineOrGetNode, String name, ParseNode bodyNode, boolean sclass) {
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        LexicalScope newLexicalScope = environment.pushLexicalScope();
        try {
            final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                    fullSourceSection,
                    newLexicalScope,
                    Arity.NO_ARGUMENTS,
                    null,
                    name,
                    sclass ? "class body" : "module body",
                    null,
                    false,
                    false,
                    false);

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

            return new RunModuleDefinitionNode(context, fullSourceSection, newLexicalScope, definition, defineOrGetNode);
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
    private ModuleBodyDefinitionNode compileClassNode(RubySourceSection sourceSection, String name, ParseNode bodyNode, boolean sclass) {
        RubyNode body;

        parentSourceSection.push(sourceSection);
        try {
            body = translateNodeOrNil(sourceSection, bodyNode);
        } finally {
            parentSourceSection.pop();
        }

        if (environment.getFlipFlopStates().size() > 0) {
            body = sequence(context, source, sourceSection, Arrays.asList(initFlipFlopStates(sourceSection), body));
        }

        final RubyNode writeSelfNode = loadSelf(context, environment);
        body = sequence(context, source, sourceSection, Arrays.asList(writeSelfNode, body));

        if (context.getOptions().CHAOS) {
            body = ChaosNodeGen.create(body);
        }

        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyRootNode rootNode = new RubyRootNode(context, fullSourceSection, environment.getFrameDescriptor(), environment.getSharedMethodInfo(), body, environment.needsDeclarationFrame());

        final ModuleBodyDefinitionNode definitionNode = new ModuleBodyDefinitionNode(
                context,
                fullSourceSection,
                environment.getSharedMethodInfo().getName(),
                environment.getSharedMethodInfo(),
                Truffle.getRuntime().createCallTarget(rootNode),
                sclass);

        return definitionNode;
    }

    @Override
    public RubyNode visitClassNode(ClassParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final String name = node.getCPath().getName();

        RubyNode lexicalParent = translateCPath(sourceSection, node.getCPath());

        RubyNode superClass;
        if (node.getSuperNode() != null) {
            superClass = node.getSuperNode().accept(this);
        } else {
            superClass = new ObjectLiteralNode(context, fullSourceSection, context.getCoreLibrary().getObjectClass());
        }

        final DefineClassNode defineOrGetClass = new DefineClassNode(context, fullSourceSection, name, lexicalParent, superClass);

        final RubyNode ret = openModule(sourceSection, defineOrGetClass, name, node.getBodyNode(), false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitClassVarAsgnNode(ClassVarAsgnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode rhs = node.getValueNode().accept(this);

        final RubyNode ret = new WriteClassVariableNode(context, sourceSection.toSourceSection(source), environment.getLexicalScope(), node.getName(), rhs);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitClassVarNode(ClassVarParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new ReadClassVariableNode(context, sourceSection.toSourceSection(source), environment.getLexicalScope(), node.getName());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitColon2Node(Colon2ParseNode node) {
        // Qualified constant access, as in Mod::CONST
        if (!(node instanceof Colon2ConstParseNode)) {
            throw new UnsupportedOperationException(node.toString());
        }

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final String name = ConstantReplacer.replacementName(fullSourceSection, node.getName());

        final RubyNode lhs = node.getLeftNode().accept(this);

        final RubyNode ret = new ReadConstantNode(context, fullSourceSection, lhs, name);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitColon3Node(Colon3ParseNode node) {
        // Root namespace constant access, as in ::Foo

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final String name = ConstantReplacer.replacementName(fullSourceSection, node.getName());

        final ObjectLiteralNode root = new ObjectLiteralNode(context, fullSourceSection, context.getCoreLibrary().getObjectClass());

        final RubyNode ret = new ReadConstantNode(context, fullSourceSection, root, name);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateCPath(RubySourceSection sourceSection, Colon3ParseNode node) {
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode ret;

        if (node instanceof Colon2ImplicitParseNode) { // use current lexical scope
            ret = new LexicalScopeNode(context, fullSourceSection, environment.getLexicalScope());
        } else if (node instanceof Colon2ConstParseNode) { // A::B
            ret = node.childNodes().get(0).accept(this);
        } else { // Colon3ParseNode: on top-level (Object)
            ret = new ObjectLiteralNode(context, fullSourceSection, context.getCoreLibrary().getObjectClass());
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitComplexNode(ComplexParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode ret = translateRationalComplex(sourceSection, "Complex",
                new IntegerFixnumLiteralNode(context, fullSourceSection, 0),
                node.getNumber().accept(this));

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitConstDeclNode(ConstDeclParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        RubyNode rhs = node.getValueNode().accept(this);

        final RubyNode moduleNode;
        ParseNode constNode = node.getConstNode();
        if (constNode == null || constNode instanceof Colon2ImplicitParseNode) {
            moduleNode = new LexicalScopeNode(context, sourceSection.toSourceSection(source), environment.getLexicalScope());
        } else if (constNode instanceof Colon2ConstParseNode) {
            constNode = ((Colon2ParseNode) constNode).getLeftNode(); // Misleading doc, we only want the defined part.
            moduleNode = constNode.accept(this);
        } else if (constNode instanceof Colon3ParseNode) {
            moduleNode = new ObjectLiteralNode(context, sourceSection.toSourceSection(source), context.getCoreLibrary().getObjectClass());
        } else {
            throw new UnsupportedOperationException();
        }

        final RubyNode ret = new WriteConstantNode(node.getName(), moduleNode, rhs);
        ret.unsafeSetSourceSection(sourceSection);

        return addNewlineIfNeeded(node, ret);
    }

    private String getSourcePath(RubySourceSection sourceSection) {
        if (sourceSection == null) {
            return "(unknown)";
        }

        if (source == null) {
            return "(unknown)";
        }

        final String path = source.getName();

        if (path == null) {
            return source.getName();
        }

        return path;
    }

    private String buildCorePath(String... components) {
        final StringBuilder ret = new StringBuilder(context.getCoreLibrary().getCoreLoadPath());
        ret.append(File.separatorChar).append("core");

        for (String component : components) {
            ret.append(File.separatorChar);
            ret.append(component);
        }

        return ret.toString();
    }

    private String buildPartialPath(String... components) {
        final StringBuilder ret = new StringBuilder();

        for (final String component : components) {
            ret.append(File.separatorChar);
            ret.append(component);
        }

        return ret.toString();
    }

    @Override
    public RubyNode visitConstNode(ConstParseNode node) {
        // Unqualified constant access, as in CONST
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        /*
         * Constants of the form Rubinius::Foo in the Rubinius kernel code always seem to get resolved, even if
         * Rubinius is not defined, such as in BasicObject. We get around this by translating Rubinius to be
         * ::Rubinius. Note that this isn't quite what Rubinius does, as they say that Rubinius isn't defined, but
         * we will because we'll translate that to ::Rubinius. But it is a simpler translation.
         */

        final String name = ConstantReplacer.replacementName(fullSourceSection, node.getName());

        if (name.equals("Rubinius") && getSourcePath(sourceSection).startsWith(buildCorePath(""))) {
            final RubyNode ret = new Colon3ParseNode(node.getPosition(), name).accept(this);
            return addNewlineIfNeeded(node, ret);
        }

        // TODO (pitr 01-Dec-2015): remove when RUBY_PLATFORM is set to "truffle"
        if (name.equals("RUBY_PLATFORM") && getSourcePath(sourceSection).contains(buildPartialPath("test", "xml_mini", "jdom_engine_test.rb"))) {
            final ObjectLiteralNode ret = new ObjectLiteralNode(context, fullSourceSection, StringOperations.createString(context, StringOperations.encodeRope("truffle", UTF8Encoding.INSTANCE, CodeRange.CR_7BIT)));
            return addNewlineIfNeeded(node, ret);
        }

        final LexicalScope lexicalScope = environment.getLexicalScope();
        final RubyNode ret = new ReadConstantWithLexicalScopeNode(context, fullSourceSection, lexicalScope, name);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDAsgnNode(DAsgnParseNode node) {
        final RubyNode ret = new LocalAsgnParseNode(node.getPosition(), node.getName(), node.getDepth(), node.getValueNode()).accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDRegxNode(DRegexpParseNode node) {
        RubySourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> children = new ArrayList<>();

        for (ParseNode child : node.children()) {
            children.add(child.accept(this));
        }

        final InterpolatedRegexpNode i = new InterpolatedRegexpNode(context, sourceSection.toSourceSection(source), children.toArray(new RubyNode[children.size()]), node.getOptions());

        if (node.getOptions().isOnce()) {
            final RubyNode ret = new OnceNode(i);
            ret.unsafeSetSourceSection(sourceSection);
            return addNewlineIfNeeded(node, ret);
        }

        return addNewlineIfNeeded(node, i);
    }

    @Override
    public RubyNode visitDStrNode(DStrParseNode node) {
        final RubyNode ret = translateInterpolatedString(translate(node.getPosition()).toSourceSection(source), node.children());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDSymbolNode(DSymbolParseNode node) {
        RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode stringNode = translateInterpolatedString(fullSourceSection, node.children());

        final RubyNode ret = StringToSymbolNodeGen.create(context, fullSourceSection, stringNode);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateInterpolatedString(SourceSection sourceSection, ParseNode[] childNodes) {
        final ToSNode[] children = new ToSNode[childNodes.length];

        for (int i = 0; i < childNodes.length; i++) {
            children[i] = ToSNodeGen.create(context, sourceSection, childNodes[i].accept(this));
        }

        return new InterpolatedStringNode(context, sourceSection, children);
    }

    @Override
    public RubyNode visitDVarNode(DVarParseNode node) {
        RubyNode readNode = environment.findLocalVarNode(node.getName(), source, translate(node.getPosition()));

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
            readNode = environment.findLocalVarNode(node.getName(), source, translate(node.getPosition()));
        }

        return addNewlineIfNeeded(node, readNode);
    }

    @Override
    public RubyNode visitDXStrNode(DXStrParseNode node) {
        final DStrParseNode string = new DStrParseNode(node.getPosition(), node.getEncoding());
        string.addAll(node);
        final ParseNode argsNode = buildArrayNode(node.getPosition(), string);
        final ParseNode callNode = new FCallParseNode(node.getPosition(), "`", argsNode, null);
        copyNewline(node, callNode);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefinedNode(DefinedParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final RubyNode ret = new DefinedNode(context, sourceSection.toSourceSection(source), node.getExpressionNode().accept(this));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefnNode(DefnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final RubyNode classNode = new RaiseIfFrozenNode(context, fullSourceSection, new GetDefaultDefineeNode(context, fullSourceSection));

        String methodName = node.getName();

        final RubyNode ret = translateMethodDefinition(sourceSection, classNode, methodName, node.getArgsNode(), node.getBodyNode(), false);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitDefsNode(DefsParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode objectNode = node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(context, fullSourceSection, objectNode);

        final RubyNode ret = translateMethodDefinition(sourceSection, singletonClassNode, node.getName(), node.getArgsNode(), node.getBodyNode(), true);

        return addNewlineIfNeeded(node, ret);
    }

    protected RubyNode translateMethodDefinition(RubySourceSection sourceSection, RubyNode classNode, String methodName, ArgsParseNode argsNode, ParseNode bodyNode,
                                                 boolean isDefs) {
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final Arity arity = MethodTranslator.getArity(argsNode);
        final ArgumentDescriptor[] argumentDescriptors = Helpers.argsNodeToArgumentDescriptors(argsNode);

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                sourceSection.toSourceSection(source),
                environment.getLexicalScope(),
                arity,
                null,
                methodName,
                null,
                argumentDescriptors,
                false,
                false,
                false);

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(
                context, environment, environment.getParseEnvironment(), environment.getParseEnvironment().allocateReturnID(), true, true, sharedMethodInfo, methodName, 0, null);

        // ownScopeForAssignments is the same for the defined method as the current one.

        final MethodTranslator methodCompiler = new MethodTranslator(currentNode, context, this, newEnvironment, false, source, argsNode);

        final MethodDefinitionNode methodDefinitionNode = methodCompiler.compileMethodNode(sourceSection, methodName, bodyNode, sharedMethodInfo);

        final RubyNode visibilityNode;
        if (isDefs) {
            visibilityNode = new ObjectLiteralNode(context, fullSourceSection, Visibility.PUBLIC);
        } else {
            visibilityNode = new GetCurrentVisibilityNode(context, fullSourceSection);
        }

        return AddMethodNodeGen.create(context, fullSourceSection, isDefs, true, classNode, methodDefinitionNode, visibilityNode);
    }

    @Override
    public RubyNode visitDotNode(DotParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final RubyNode begin = node.getBeginNode().accept(this);
        final RubyNode end = node.getEndNode().accept(this);
        final RubyNode rangeClass = new ObjectLiteralNode(context, fullSourceSection, context.getCoreLibrary().getRangeClass());
        final RubyNode isExclusive = new ObjectLiteralNode(context, fullSourceSection, node.isExclusive());

        final RubyNode ret = RangeNodesFactory.NewNodeFactory.create(context, fullSourceSection, rangeClass, begin, end, isExclusive);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEncodingNode(EncodingParseNode node) {
        RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new ObjectLiteralNode(context, sourceSection.toSourceSection(source), context.getEncodingManager().getRubyEncoding(node.getEncoding()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEnsureNode(EnsureParseNode node) {
        final RubyNode tryPart = node.getBodyNode().accept(this);
        final RubyNode ensurePart = node.getEnsureNode().accept(this);
        final RubyNode ret = new EnsureNode(context, translate(node.getPosition()).toSourceSection(source), tryPart, ensurePart);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitEvStrNode(EvStrParseNode node) {
        final RubyNode ret;

        if (node.getBody() == null) {
            final RubySourceSection sourceSection = translate(node.getPosition());
            ret = new ObjectLiteralNode(context, sourceSection.toSourceSection(source), StringOperations.createString(context, RopeConstants.EMPTY_ASCII_8BIT_ROPE));
        } else {
            ret = node.getBody().accept(this);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFCallNode(FCallParseNode node) {
        final ParseNode receiver = new SelfParseNode(node.getPosition());
        final CallParseNode callNode = new CallParseNode(node.getPosition(), receiver, node.getName(), node.getArgsNode(), node.getIterNode());
        copyNewline(node, callNode);
        return translateCallNode(callNode, true, false, false);
    }

    @Override
    public RubyNode visitFalseNode(FalseParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new BooleanLiteralNode(context, sourceSection.toSourceSection(source), false);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFixnumNode(FixnumParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final long value = node.getValue();
        final RubyNode ret;

        if (CoreLibrary.fitsIntoInteger(value)) {
            ret = new IntegerFixnumLiteralNode(context, sourceSection.toSourceSection(source), (int) value);
        } else {
            ret = new LongFixnumLiteralNode(context, sourceSection.toSourceSection(source), value);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitFlipNode(FlipParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final RubyNode begin = node.getBeginNode().accept(this);
        final RubyNode end = node.getEndNode().accept(this);

        final FlipFlopStateNode stateNode = createFlipFlopState(sourceSection, 0);

        final RubyNode ret = new FlipFlopNode(context, sourceSection.toSourceSection(source), begin, end, stateNode, node.isExclusive());
        return addNewlineIfNeeded(node, ret);
    }

    protected FlipFlopStateNode createFlipFlopState(RubySourceSection sourceSection, int depth) {
        final FrameSlot frameSlot = environment.declareVar(environment.allocateLocalTemp("flipflop"));
        environment.getFlipFlopStates().add(frameSlot);

        if (depth == 0) {
            return new LocalFlipFlopStateNode(frameSlot);
        } else {
            return new DeclarationFlipFlopStateNode(depth, frameSlot);
        }
    }

    @Override
    public RubyNode visitFloatNode(FloatParseNode node) {
        final RubyNode ret = new FloatLiteralNode(context, translate(node.getPosition()).toSourceSection(source), node.getValue());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitForNode(ForParseNode node) {
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

        final ParseNode receiver = node.getIterNode();

        /*
         * The x in for x in ... is like the nodes in multiple assignment - it has a dummy RHS which
         * we need to replace with our temp. Just like in multiple assignment this is really awkward
         * with the JRuby AST.
         */

        final LocalVarParseNode readTemp = new LocalVarParseNode(node.getPosition(), 0, temp);
        final ParseNode forVar = node.getVarNode();
        final ParseNode assignTemp = setRHS(forVar, readTemp);

        final BlockParseNode bodyWithTempAssign = new BlockParseNode(node.getPosition());
        bodyWithTempAssign.add(assignTemp);
        bodyWithTempAssign.add(node.getBodyNode());

        final ArgumentParseNode blockVar = new ArgumentParseNode(node.getPosition(), temp);
        final ListParseNode blockArgsPre = new ListParseNode(node.getPosition(), blockVar);
        final ArgsParseNode blockArgs = new ArgsParseNode(node.getPosition(), blockArgsPre, null, null, null, null, null, null);
        final IterParseNode block = new IterParseNode(node.getPosition(), blockArgs, node.getScope(), bodyWithTempAssign);

        final CallParseNode callNode = new CallParseNode(node.getPosition(), receiver, "each", null, block);
        copyNewline(node, callNode);

        translatingForStatement = true;
        final RubyNode translated = callNode.accept(this);
        translatingForStatement = false;

        return addNewlineIfNeeded(node, translated);
    }

    private final ParserSupport parserSupport;

    private ParseNode setRHS(ParseNode node, ParseNode rhs) {
        if (node instanceof AssignableParseNode || node instanceof org.jruby.truffle.parser.ast.IArgumentNode) {
            return parserSupport.node_assign(node, rhs);
        } else {
            throw new UnsupportedOperationException("Don't know how to set the RHS of a " + node.getClass().getName());
        }
    }

    private RubyNode translateDummyAssignment(ParseNode dummyAssignment, final RubyNode rhs) {
        // The JRuby AST includes assignment nodes without a proper value,
        // so we need to patch them to include the proper rhs value to translate them correctly.

        if (dummyAssignment instanceof StarParseNode) {
            // Nothing to assign to, just execute the RHS
            return rhs;
        } else if (dummyAssignment instanceof AssignableParseNode || dummyAssignment instanceof org.jruby.truffle.parser.ast.IArgumentNode) {
            final ParseNode wrappedRHS = new ParseNode(dummyAssignment.getPosition(), false) {
                @SuppressWarnings("unchecked")
                @Override
                public <T> T accept(NodeVisitor<T> visitor) {
                    return (T) rhs;
                }

                @Override
                public List<ParseNode> childNodes() {
                    return Collections.emptyList();
                }

                @Override
                public org.jruby.truffle.parser.ast.NodeType getNodeType() {
                    return org.jruby.truffle.parser.ast.NodeType.FIXNUMNODE; // since we behave like a value
                }
            };

            return setRHS(dummyAssignment, wrappedRHS).accept(this);
        } else {
            throw new UnsupportedOperationException("Don't know how to translate the dummy asgn " + dummyAssignment.getClass().getName());
        }
    }

    @Override
    public RubyNode visitGlobalAsgnNode(GlobalAsgnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        RubyNode rhs = node.getValueNode().accept(this);

        String name = node.getName();

        if (GLOBAL_VARIABLE_ALIASES.containsKey(name)) {
            name = GLOBAL_VARIABLE_ALIASES.get(name);
        }

        if (name.equals("$~")) {
            rhs = new CheckMatchVariableTypeNode(context, sourceSection.toSourceSection(source), rhs);
            rhs = WrapInThreadLocalNodeGen.create(context, sourceSection.toSourceSection(source), rhs);

            environment.declareVarInMethodScope("$~");
        } else if (name.equals("$0")) {
            rhs = new CheckProgramNameVariableTypeNode(context, sourceSection.toSourceSection(source), rhs);
        } else if (name.equals("$/")) {
            rhs = new CheckRecordSeparatorVariableTypeNode(context, sourceSection.toSourceSection(source), rhs);
        } else if (name.equals("$,")) {
            rhs = new CheckOutputSeparatorVariableTypeNode(context, sourceSection.toSourceSection(source), rhs);
        } else if (name.equals("$_")) {
            if (getSourcePath(sourceSection).endsWith(buildPartialPath("truffle", "rubysl", "rubysl-stringio", "lib", "rubysl", "stringio", "stringio.rb"))) {
                rhs = RubiniusLastStringWriteNodeGen.create(context, sourceSection.toSourceSection(source), rhs);
            } else {
                rhs = WrapInThreadLocalNodeGen.create(context, sourceSection.toSourceSection(source), rhs);
            }

            environment.declareVar("$_");
        } else if (name.equals("$stdout")) {
            rhs = new CheckStdoutVariableTypeNode(context, fullSourceSection, rhs);
        } else if (name.equals("$VERBOSE")) {
            rhs = new UpdateVerbosityNode(context, fullSourceSection, rhs);
        } else if (name.equals("$@")) {
            // $@ is a special-case and doesn't write directly to an ivar field in the globals object.
            // Instead, it writes to the backtrace field of the thread-local $! value.
            return new UpdateLastBacktraceNode(context, fullSourceSection, rhs);
        }

        final boolean inCore = getSourcePath(translate(node.getValueNode().getPosition())).startsWith(buildCorePath(""));

        if (!inCore && READ_ONLY_GLOBAL_VARIABLES.contains(name)) {
            return addNewlineIfNeeded(node, new WriteReadOnlyGlobalNode(context, fullSourceSection, name, rhs));
        }

        if (THREAD_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            final ThreadLocalObjectNode threadLocalVariablesObjectNode = ThreadLocalObjectNodeGen.create(context, fullSourceSection);
            return addNewlineIfNeeded(node, new WriteInstanceVariableNode(context, fullSourceSection, name, threadLocalVariablesObjectNode, rhs));
        } else if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            if (environment.getNeverAssignInParentScope()) {
                environment.declareVar(name);
            }

            ReadLocalNode localVarNode = environment.findLocalVarNode(node.getName(), source, sourceSection);

            if (localVarNode == null) {
                if (environment.hasOwnScopeForAssignments()) {
                    environment.declareVar(node.getName());
                }

                TranslatorEnvironment environmentToDeclareIn = environment;

                while (!environmentToDeclareIn.hasOwnScopeForAssignments()) {
                    environmentToDeclareIn = environmentToDeclareIn.getParent();
                }

                environmentToDeclareIn.declareVar(node.getName());
                localVarNode = environment.findLocalVarNode(node.getName(), source, sourceSection);

                if (localVarNode == null) {
                    throw new RuntimeException("shouldn't be here");
                }
            }

            RubyNode assignment = localVarNode.makeWriteNode(rhs);

            if (name.equals("$_") || name.equals("$~")) {
                // TODO CS 4-Jan-16 I can't work out why this is a *get* node
                assignment = new GetFromThreadLocalNode(context, fullSourceSection, assignment);
            }

            return addNewlineIfNeeded(node, assignment);
        } else {
            final RubyNode writeGlobalVariableNode = WriteGlobalVariableNodeGen.create(context, fullSourceSection, name, rhs);

            final RubyNode translated;

            if (name.equals("$0")) {
                translated = VMPrimitiveNodesFactory.VMSetProcessTitleNodeFactory.create(
                        new RubyNode[]{ writeGlobalVariableNode });
            } else {
                translated = writeGlobalVariableNode;
            }

            return addNewlineIfNeeded(node, translated);
        }
    }

    @Override
    public RubyNode visitGlobalVarNode(GlobalVarParseNode node) {
        String name = node.getName();

        if (GLOBAL_VARIABLE_ALIASES.containsKey(name)) {
            name = GLOBAL_VARIABLE_ALIASES.get(name);
        }

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final RubyNode ret;

        if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            // Assignment is implicit for many of these, so we need to declare when we use

            RubyNode readNode = environment.findLocalVarNode(name, source, sourceSection);

            if (readNode == null) {
                environment.declareVarInMethodScope(name);
                readNode = environment.findLocalVarNode(name, source, sourceSection);
            }

            if (name.equals("$_")) {
                if (getSourcePath(sourceSection).equals(buildCorePath("regexp.rb"))) {
                    readNode = new RubiniusLastStringReadNode(context, fullSourceSection);
                } else {
                    readNode = new GetFromThreadLocalNode(context, fullSourceSection, readNode);
                }
            } else if (name.equals("$~")) {
                readNode = new GetFromThreadLocalNode(context, fullSourceSection, readNode);
            }

            ret = readNode;
        } else if (THREAD_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            ret = new ReadThreadLocalGlobalVariableNode(context, fullSourceSection, name, ALWAYS_DEFINED_GLOBALS.contains(name));
        } else if (name.equals("$@")) {
            // $@ is a special-case and doesn't read directly from an ivar field in the globals object.
            // Instead, it reads the backtrace field of the thread-local $! value.
            ret = new ReadLastBacktraceNode(context, fullSourceSection);
        } else {
            ret = ReadGlobalVariableNodeGen.create(context, fullSourceSection, name);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitHashNode(HashParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final List<RubyNode> hashConcats = new ArrayList<>();

        final List<RubyNode> keyValues = new ArrayList<>();

        for (KeyValuePair<ParseNode, ParseNode> pair: node.getPairs()) {
            if (pair.getKey() == null) {
                final RubyNode hashLiteralSoFar = HashLiteralNode.create(context, fullSourceSection, keyValues.toArray(new RubyNode[keyValues.size()]));
                hashConcats.add(hashLiteralSoFar);
                hashConcats.add(HashCastNodeGen.create(context, fullSourceSection, pair.getValue().accept(this)));
                keyValues.clear();
            } else {
                keyValues.add(pair.getKey().accept(this));

                if (pair.getValue() == null) {
                    keyValues.add(nilNode(source, sourceSection));
                } else {
                    keyValues.add(pair.getValue().accept(this));
                }
            }
        }

        final RubyNode hashLiteralSoFar = HashLiteralNode.create(context, fullSourceSection, keyValues.toArray(new RubyNode[keyValues.size()]));
        hashConcats.add(hashLiteralSoFar);

        if (hashConcats.size() == 1) {
            final RubyNode ret = hashConcats.get(0);
            return addNewlineIfNeeded(node, ret);
        }

        final RubyNode ret = new ConcatHashLiteralNode(context, fullSourceSection, hashConcats.toArray(new RubyNode[hashConcats.size()]));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitIfNode(IfParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode condition = translateNodeOrNil(sourceSection, node.getCondition());

        ParseNode thenBody = node.getThenBody();
        ParseNode elseBody = node.getElseBody();

        final RubyNode ret;

        if (thenBody != null && elseBody != null) {
            final RubyNode thenBodyTranslated = thenBody.accept(this);
            final RubyNode elseBodyTranslated = elseBody.accept(this);
            ret = new IfElseNode(condition, thenBodyTranslated, elseBodyTranslated);
            ret.unsafeSetSourceSection(sourceSection);
        } else if (thenBody != null) {
            final RubyNode thenBodyTranslated = thenBody.accept(this);
            ret = new IfNode(condition, thenBodyTranslated);
            ret.unsafeSetSourceSection(sourceSection);
        } else if (elseBody != null) {
            final RubyNode elseBodyTranslated = elseBody.accept(this);
            ret = new UnlessNode(condition, elseBodyTranslated);
            ret.unsafeSetSourceSection(sourceSection);
        } else {
            ret = sequence(context, source, sourceSection, Arrays.asList(condition, new NilLiteralNode(context, fullSourceSection, true)));
        }

        return ret; // no addNewlineIfNeeded(node, ret) as the condition will already have a newline
    }

    @Override
    public RubyNode visitInstAsgnNode(InstAsgnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final String name = node.getName();

        final RubyNode rhs;
        if (node.getValueNode() == null) {
            rhs = new DeadNode(context, fullSourceSection, new Exception("null RHS of instance variable assignment"));
        } else {
            rhs = node.getValueNode().accept(this);
        }

        // Every case will use a SelfParseNode, just don't it use more than once.
        // Also note the check for frozen.
        final RubyNode self = new RaiseIfFrozenNode(context, fullSourceSection, new SelfNode(environment.getFrameDescriptor()));

        final String path = getSourcePath(sourceSection);
        final String corePath = buildCorePath("");
        final RubyNode ret;
        if (path.equals(corePath + "hash.rb")) {
            if (name.equals("@default")) {
                ret = HashNodesFactory.SetDefaultValueNodeFactory.create(self, rhs);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@default_proc")) {
                ret = HashNodesFactory.SetDefaultProcNodeFactory.create(self, rhs);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "range.rb")) {
            if (name.equals("@begin")) {
                ret = RangeNodesFactory.InternalSetBeginNodeGen.create(self, rhs);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@end")) {
                ret = RangeNodesFactory.InternalSetEndNodeGen.create(self, rhs);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@excl")) {
                ret = RangeNodesFactory.InternalSetExcludeEndNodeGen.create(self, rhs);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        } else if (path.equals(corePath + "io.rb")) {
            // TODO (pitr 08-Aug-2015): values of predefined OM properties should be casted to defined types automatically
            if (name.equals("@used") || name.equals("@total") || name.equals("@lineno")) {
                // Cast int-fitting longs back to int
                ret = new WriteInstanceVariableNode(context, fullSourceSection, name, self, IntegerCastNodeGen.create(context, fullSourceSection, rhs));
                return addNewlineIfNeeded(node, ret);
            }
        }

        ret = new WriteInstanceVariableNode(context, fullSourceSection, name, self, rhs);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitInstVarNode(InstVarParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final String name = node.getName();

        // About every case will use a SelfParseNode, just don't it use more than once.
        final SelfNode self = new SelfNode(environment.getFrameDescriptor());

        final String path = getSourcePath(sourceSection);
        final String corePath = buildCorePath("");
        final RubyNode ret;
        if (path.equals(corePath + "regexp.rb")) {
            if (name.equals("@source")) {
                ret = MatchDataNodesFactory.RubiniusSourceNodeGen.create(self);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@regexp")) {
                ret = MatchDataNodesFactory.RegexpNodeFactory.create(new RubyNode[]{ self });
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            } else if (name.equals("@names")) {
                ret = RegexpNodesFactory.RubiniusNamesNodeGen.create(self);
                ret.unsafeSetSourceSection(sourceSection);
                return addNewlineIfNeeded(node, ret);
            }
        }

        ret = new ReadInstanceVariableNode(context, sourceSection.toSourceSection(source), name, self);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitIterNode(IterParseNode node) {
        return translateBlockLikeNode(node, false);
    }

    @Override
    public RubyNode visitLambdaNode(LambdaParseNode node) {
        return translateBlockLikeNode(node, true);
    }

    private RubyNode translateBlockLikeNode(IterParseNode node, boolean isLambda) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final ArgsParseNode argsNode = node.getArgsNode();

        // Unset this flag for any for any blocks within the for statement's body
        final boolean hasOwnScope = isLambda || !translatingForStatement;

        final boolean isProc = !isLambda;

        final SharedMethodInfo sharedMethodInfo = new SharedMethodInfo(
                sourceSection.toSourceSection(source),
                environment.getLexicalScope(),
                MethodTranslator.getArity(argsNode),
                null,
                null,
                isLambda ? "lambda" : getIdentifierInNewEnvironment(true, currentCallMethodName),
                Helpers.argsNodeToArgumentDescriptors(argsNode),
                false,
                false,
                false);

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
            definitionNode = methodCompiler.compileBlockNode(sourceSection, sharedMethodInfo.getName(), node.getBodyNode(), sharedMethodInfo, type, node.getScope().getVariables());
        } finally {
            if (isLambda) {
                frameOnStackMarkerSlotStack.pop();
            }
        }

        return addNewlineIfNeeded(node, definitionNode);
    }

    @Override
    public RubyNode visitLocalAsgnNode(LocalAsgnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        if (environment.getNeverAssignInParentScope()) {
            environment.declareVar(node.getName());
        }

        ReadLocalNode lhs = environment.findLocalVarNode(node.getName(), source, sourceSection);

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

            lhs = environment.findLocalVarNode(node.getName(), source, sourceSection);

            if (lhs == null) {
                throw new RuntimeException("shouldn't be here");
            }
        }

        RubyNode rhs;

        if (node.getValueNode() == null) {
            rhs = new DeadNode(context, sourceSection.toSourceSection(source), new Exception());
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
    public RubyNode visitLocalVarNode(LocalVarParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final String name = node.getName();

        RubyNode readNode = environment.findLocalVarNode(name, source, sourceSection);

        if (readNode == null) {
            /*

              This happens for code such as:

                def destructure4r((*c,d))
                    [c,d]
                end

               We're going to just assume that it should be there and add it...
             */

            environment.declareVar(node.getName());
            readNode = environment.findLocalVarNode(name, source, sourceSection);
        }

        return addNewlineIfNeeded(node, readNode);
    }

    @Override
    public RubyNode visitMatchNode(MatchParseNode node) {
        // Triggered when a Regexp literal is used as a conditional's value.

        final ParseNode argsNode = buildArrayNode(node.getPosition(), new GlobalVarParseNode(node.getPosition(), "$_"));
        final ParseNode callNode = new CallParseNode(node.getPosition(), node.getRegexpNode(), "=~", argsNode, null);
        copyNewline(node, callNode);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMatch2Node(Match2ParseNode node) {
        // Triggered when a Regexp literal is the LHS of an expression.

        if (node.getReceiverNode() instanceof RegexpParseNode) {
            final RegexpParseNode regexpNode = (RegexpParseNode) node.getReceiverNode();
            final Regex regex = new Regex(regexpNode.getValue().bytes(), 0, regexpNode.getValue().length(), regexpNode.getOptions().toOptions(), regexpNode.getEncoding(), Syntax.RUBY);

            if (regex.numberOfNames() > 0) {
                for (Iterator<NameEntry> i = regex.namedBackrefIterator(); i.hasNext(); ) {
                    final NameEntry e = i.next();
                    final String name = new String(e.name, e.nameP, e.nameEnd - e.nameP, StandardCharsets.UTF_8).intern();

                    TranslatorEnvironment environmentToDeclareIn = environment;
                    while (!environmentToDeclareIn.hasOwnScopeForAssignments()) {
                        environmentToDeclareIn = environmentToDeclareIn.getParent();
                    }
                    environmentToDeclareIn.declareVar(name);
                }
            }
        }

        final ParseNode argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final ParseNode callNode = new CallParseNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        copyNewline(node, callNode);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMatch3Node(Match3ParseNode node) {
        // Triggered when a Regexp literal is the RHS of an expression.

        final ParseNode argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final ParseNode callNode = new CallParseNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        copyNewline(node, callNode);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitModuleNode(ModuleParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final String name = node.getCPath().getName();

        RubyNode lexicalParent = translateCPath(sourceSection, node.getCPath());

        final DefineModuleNode defineModuleNode = DefineModuleNodeGen.create(context, fullSourceSection, name, lexicalParent);

        final RubyNode ret = openModule(sourceSection, defineModuleNode, name, node.getBodyNode(), false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitMultipleAsgnNode(MultipleAsgnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final ListParseNode preArray = node.getPre();
        final ListParseNode postArray = node.getPost();
        final ParseNode rhs = node.getValueNode();

        RubyNode rhsTranslated;

        if (rhs == null) {
            throw new UnsupportedOperationException("null rhs");
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
                final ReadLocalNode readTemp = environment.findLocalVarNode(tempName, source, sourceSection);
                final RubyNode assignTemp = readTemp.makeWriteNode(rhsArrayLiteral.stealNode(n));
                final RubyNode assignFinalValue = translateDummyAssignment(preArray.get(n), NodeUtil.cloneNode(readTemp));

                sequence[n] = assignTemp;
                sequence[assignedValuesCount + n] = assignFinalValue;

                tempValues[n] = NodeUtil.cloneNode(readTemp);
            }

            final RubyNode blockNode = sequence(context, source, sourceSection, Arrays.asList(sequence));

            final ArrayLiteralNode arrayNode = ArrayLiteralNode.create(context, sourceSection, tempValues);

            result = new ElidableResultNode(blockNode, arrayNode);
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
            final RubyNode writeTempRHS = environment.findLocalVarNode(tempRHSName, source, sourceSection).makeWriteNode(rhsTranslated);
            sequence.add(writeTempRHS);

            /*
             * Create a temp for the array.
             */

            final String tempName = environment.allocateLocalTemp("array");

            /*
             * Create a sequence of instructions, with the first being the literal array assigned to
             * the temp.
             */

            final RubyNode splatCastNode = SplatCastNodeGen.create(context, fullSourceSection, translatingNextExpression ? SplatCastNode.NilBehavior.EMPTY_ARRAY : SplatCastNode.NilBehavior.ARRAY_WITH_NIL, true, environment.findLocalVarNode(tempRHSName, source, sourceSection));

            final RubyNode writeTemp = environment.findLocalVarNode(tempName, source, sourceSection).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            for (int n = 0; n < preArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, fullSourceSection, environment.findLocalVarNode(tempName, source, sourceSection), n);

                sequence.add(translateDummyAssignment(preArray.get(n), assignedValue));
            }

            if (node.getRest() != null) {
                RubyNode assignedValue = ArrayGetTailNodeGen.create(context, fullSourceSection, preArray.size(), environment.findLocalVarNode(tempName, source, sourceSection));

                if (postArray != null) {
                    assignedValue = ArrayDropTailNodeGen.create(context, fullSourceSection, postArray.size(), assignedValue);
                }

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            if (postArray != null) {
                final List<RubyNode> smallerSequence = new ArrayList<>();

                for (int n = 0; n < postArray.size(); n++) {
                    final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, fullSourceSection, environment.findLocalVarNode(tempName, source, sourceSection), node.getPreCount() + n);
                    smallerSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
                }

                final RubyNode smaller = sequence(context, source, sourceSection, smallerSequence);

                final List<RubyNode> atLeastAsLargeSequence = new ArrayList<>();

                for (int n = 0; n < postArray.size(); n++) {
                    final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, fullSourceSection, environment.findLocalVarNode(tempName, source, sourceSection), -(postArray.size() - n));

                    atLeastAsLargeSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
                }

                final RubyNode atLeastAsLarge = sequence(context, source, sourceSection, atLeastAsLargeSequence);

                final RubyNode assignPost =
                        new IfElseNode(
                                new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), environment.findLocalVarNode(tempName, source, sourceSection)),
                                atLeastAsLarge,
                                smaller);

                sequence.add(assignPost);
            }

            result = new ElidableResultNode(sequence(context, source, sourceSection, sequence), environment.findLocalVarNode(tempRHSName, source, sourceSection));
        } else if (node.getPre() == null
                && node.getPost() == null
                && node.getRest() instanceof StarParseNode) {
            result = rhsTranslated;
        } else if (node.getPre() == null
                && node.getPost() == null
                && node.getRest() != null
                && rhs != null
                && !(rhs instanceof ArrayParseNode)) {
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
            final RubyNode writeTempRHS = environment.findLocalVarNode(tempRHSName, source, sourceSection).makeWriteNode(rhsTranslated);
            sequence.add(writeTempRHS);

            final SplatCastNode rhsSplatCast = SplatCastNodeGen.create(context, fullSourceSection,
                    nilBehavior,
                    true, environment.findLocalVarNode(tempRHSName, source, sourceSection));

            final String tempRHSSplattedName = environment.allocateLocalTemp("rhs");
            final RubyNode writeTempSplattedRHS = environment.findLocalVarNode(tempRHSSplattedName, source, sourceSection).makeWriteNode(rhsSplatCast);
            sequence.add(writeTempSplattedRHS);

            sequence.add(translateDummyAssignment(node.getRest(), environment.findLocalVarNode(tempRHSSplattedName, source, sourceSection)));

            final RubyNode assignmentResult;

            if (nilBehavior == SplatCastNode.NilBehavior.CONVERT) {
                assignmentResult = environment.findLocalVarNode(tempRHSSplattedName, source, sourceSection);
            } else {
                assignmentResult = environment.findLocalVarNode(tempRHSName, source, sourceSection);
            }

            result = new ElidableResultNode(sequence(context, source, sourceSection, sequence), assignmentResult);
        } else if (node.getPre() == null
                && node.getPost() == null
                && node.getRest() != null
                && rhs != null
                && rhs instanceof ArrayParseNode) {
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
            final RubyNode writeTempRHS = environment.findLocalVarNode(tempRHSName, source, sourceSection).makeWriteNode(rhsTranslated);
            sequence.add(writeTempRHS);

            /*
             * Create a temp for the array.
             */

            final String tempName = environment.allocateLocalTemp("array");

            /*
             * Create a sequence of instructions, with the first being the literal array assigned to
             * the temp.
             */


            final RubyNode splatCastNode = SplatCastNodeGen.create(context, fullSourceSection, translatingNextExpression ? SplatCastNode.NilBehavior.EMPTY_ARRAY : SplatCastNode.NilBehavior.ARRAY_WITH_NIL, false, environment.findLocalVarNode(tempRHSName, source, sourceSection));

            final RubyNode writeTemp = environment.findLocalVarNode(tempName, source, sourceSection).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            if (node.getRest() != null) {
                final ArrayDropTailNode assignedValue = ArrayDropTailNodeGen.create(context, fullSourceSection, postArray.size(), environment.findLocalVarNode(tempName, source, sourceSection));

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            final List<RubyNode> smallerSequence = new ArrayList<>();

            for (int n = 0; n < postArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, fullSourceSection, environment.findLocalVarNode(tempName, source, sourceSection), node.getPreCount() + n);
                smallerSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
            }

            final RubyNode smaller = sequence(context, source, sourceSection, smallerSequence);

            final List<RubyNode> atLeastAsLargeSequence = new ArrayList<>();

            for (int n = 0; n < postArray.size(); n++) {
                final RubyNode assignedValue = PrimitiveArrayNodeFactory.read(context, fullSourceSection, environment.findLocalVarNode(tempName, source, sourceSection), -(postArray.size() - n));

                atLeastAsLargeSequence.add(translateDummyAssignment(postArray.get(n), assignedValue));
            }

            final RubyNode atLeastAsLarge = sequence(context, source, sourceSection, atLeastAsLargeSequence);

            final RubyNode assignPost =
                    new IfElseNode(
                    new ArrayIsAtLeastAsLargeAsNode(node.getPreCount() + node.getPostCount(), environment.findLocalVarNode(tempName, source, sourceSection)),
                            atLeastAsLarge,
                            smaller);

            sequence.add(assignPost);

            result = new ElidableResultNode(sequence(context, source, sourceSection, sequence), environment.findLocalVarNode(tempRHSName, source, sourceSection));
        } else {
            throw new UnsupportedOperationException();
        }

        final RubyNode ret = new DefinedWrapperNode(context, fullSourceSection, context.getCoreStrings().ASSIGNMENT, result);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitNextNode(NextParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

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

        final RubyNode ret = new NextNode(resultNode);
        ret.unsafeSetSourceSection(sourceSection);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitNilNode(NilParseNode node) {
        if (node.getPosition() == InvalidSourcePosition.INSTANCE && parentSourceSection.peek() == null) {
            final RubyNode ret = new DeadNode(context, null, new Exception());
            return addNewlineIfNeeded(node, ret);
        }

        RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = nilNode(source, sourceSection);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitNthRefNode(NthRefParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        environment.declareVarInMethodScope("$~");

        final GetFromThreadLocalNode readMatchNode = new GetFromThreadLocalNode(context, fullSourceSection, environment.findLocalVarNode("$~", source, sourceSection));
        final RubyNode ret = new ReadMatchReferenceNode(context, fullSourceSection, readMatchNode, node.getMatchNumber());

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnAndNode(OpAsgnAndParseNode node) {
        /*
         * This doesn't translate as you might expect!
         *
         * http://www.rubyinside.com/what-rubys-double-pipe-or-equals-really-does-5488.html
         */

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final ParseNode lhs = node.getFirstNode();
        final ParseNode rhs = node.getSecondNode();

        final RubyNode andNode = new AndNode(lhs.accept(this), rhs.accept(this));
        andNode.unsafeSetSourceSection(sourceSection);

        final RubyNode ret = new DefinedWrapperNode(context, fullSourceSection, context.getCoreStrings().ASSIGNMENT, andNode);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnConstDeclNode(OpAsgnConstDeclParseNode node) {
        // TODO (eregon, 7 Nov. 2016): Is there any semantic difference?
        if ("&&".equals(node.getOperator())) {
            return visitOpAsgnAndNode(new OpAsgnAndParseNode(node.getPosition(), node.getFirstNode(), node.getSecondNode()));
        } else {
            return visitOpAsgnOrNode(new OpAsgnOrParseNode(node.getPosition(), node.getFirstNode(), node.getSecondNode()));
        }
    }

    @Override
    public RubyNode visitOpAsgnNode(OpAsgnParseNode node) {
        final ISourcePosition pos = node.getPosition();

        final boolean isOrOperator = node.getOperatorName().equals("||");
        if (isOrOperator || node.getOperatorName().equals("&&")) {
            // Why does this ||= or &&= come through as a visitOpAsgnNode and not a visitOpAsgnOrNode?

            final String temp = environment.allocateLocalTemp("opassign");
            final ParseNode writeReceiverToTemp = new LocalAsgnParseNode(pos, temp, 0, node.getReceiverNode());
            final ParseNode readReceiverFromTemp = new LocalVarParseNode(pos, 0, temp);

            final ParseNode readMethod = new CallParseNode(pos, readReceiverFromTemp, node.getVariableName(), null, null);
            final ParseNode writeMethod = new CallParseNode(pos, readReceiverFromTemp, node.getVariableName() + "=", buildArrayNode(pos,
                    node.getValueNode()), null);

            final RubySourceSection sourceSection = translate(pos);
            final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

            RubyNode lhs = readMethod.accept(this);
            RubyNode rhs = writeMethod.accept(this);

            final RubyNode controlNode = isOrOperator ? new OrNode(lhs, rhs) : new AndNode(lhs, rhs);
            final RubyNode ret = new DefinedWrapperNode(
                    context,
                    fullSourceSection,
                    context.getCoreStrings().ASSIGNMENT,
                    sequence(
                            context,
                            source,
                            sourceSection,
                            Arrays.asList(writeReceiverToTemp.accept(this), controlNode)));

            return addNewlineIfNeeded(node, ret);
        }

        /*
         * We're going to de-sugar a.foo += c into a.foo = a.foo + c. Note that we can't evaluate a
         * more than once, so we put it into a temporary, and we're doing something more like:
         *
         * temp = a; temp.foo = temp.foo + c
         */

        final String temp = environment.allocateLocalTemp("opassign");
        final ParseNode writeReceiverToTemp = new LocalAsgnParseNode(pos, temp, 0, node.getReceiverNode());

        final ParseNode readReceiverFromTemp = new LocalVarParseNode(pos, 0, temp);

        final ParseNode readMethod = new CallParseNode(pos, readReceiverFromTemp, node.getVariableName(), null, null);
        final ParseNode operation = new CallParseNode(pos, readMethod, node.getOperatorName(),
                buildArrayNode(pos, node.getValueNode()), null);
        final ParseNode writeMethod = new CallParseNode(pos, readReceiverFromTemp, node.getVariableName() + "=",
                buildArrayNode(pos, operation), null);

        final BlockParseNode block = new BlockParseNode(pos);
        block.add(writeReceiverToTemp);

        final RubyNode writeTemp = writeReceiverToTemp.accept(this);
        RubyNode body = writeMethod.accept(this);

        final RubySourceSection sourceSection = translate(pos);
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        if (node.isLazy()) {
            ReadLocalNode readLocal = environment.findLocalVarNode(temp, source, sourceSection);
            body = new IfNode(
                    new NotNode(new IsNilNode(context, fullSourceSection, readLocal)),
                    body);
            body.unsafeSetSourceSection(sourceSection);
        }
        final RubyNode ret = sequence(context, source, sourceSection, Arrays.asList(writeTemp, body));

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpAsgnOrNode(OpAsgnOrParseNode node) {
        /*
         * This doesn't translate as you might expect!
         *
         * http://www.rubyinside.com/what-rubys-double-pipe-or-equals-really-does-5488.html
         */

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        RubyNode lhs = node.getFirstNode().accept(this);
        RubyNode rhs = node.getSecondNode().accept(this);

        // I think this is only required for constants - not instance variables

        if (node.getFirstNode().needsDefinitionCheck() && !(node.getFirstNode() instanceof InstVarParseNode)) {
            RubyNode defined = new DefinedNode(context, translateSourceSection(source, lhs.getRubySourceSection()), lhs);
            lhs = new AndNode(defined, lhs);
        }

        final RubyNode ret = new DefinedWrapperNode(context, fullSourceSection, context.getCoreStrings().ASSIGNMENT,
                new OrNode(lhs, rhs));

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitOpElementAsgnNode(OpElementAsgnParseNode node) {
        /*
         * We're going to de-sugar a[b] += c into a[b] = a[b] + c. See discussion in
         * visitOpAsgnNode.
         */

        ParseNode index;

        if (node.getArgsNode() == null) {
            index = null;
        } else {
            index = node.getArgsNode().childNodes().get(0);
        }

        final ParseNode operand = node.getValueNode();

        final String temp = environment.allocateLocalTemp("opelementassign");
        final ParseNode writeArrayToTemp = new LocalAsgnParseNode(node.getPosition(), temp, 0, node.getReceiverNode());
        final ParseNode readArrayFromTemp = new LocalVarParseNode(node.getPosition(), 0, temp);

        final ParseNode arrayRead = new CallParseNode(node.getPosition(), readArrayFromTemp, "[]", buildArrayNode(node.getPosition(), index), null);

        final String op = node.getOperatorName();

        ParseNode operation = null;

        if (op.equals("||")) {
            operation = new OrParseNode(node.getPosition(), arrayRead, operand);
        } else if (op.equals("&&")) {
            operation = new AndParseNode(node.getPosition(), arrayRead, operand);
        } else {
            operation = new CallParseNode(node.getPosition(), arrayRead, node.getOperatorName(), buildArrayNode(node.getPosition(), operand), null);
        }

        copyNewline(node, operation);

        final ParseNode arrayWrite = new CallParseNode(node.getPosition(), readArrayFromTemp, "[]=", buildArrayNode(node.getPosition(), index, operation), null);

        final BlockParseNode block = new BlockParseNode(node.getPosition());
        block.add(writeArrayToTemp);
        block.add(arrayWrite);

        final RubyNode ret = block.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    private static ArrayParseNode buildArrayNode(ISourcePosition sourcePosition, ParseNode first, ParseNode... rest) {
        if (first == null) {
            return new ArrayParseNode(sourcePosition);
        }

        final ArrayParseNode array = new ArrayParseNode(sourcePosition, first);

        for (ParseNode node : rest) {
            array.add(node);
        }

        return array;
    }

    @Override
    public RubyNode visitOrNode(OrParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final RubyNode x = translateNodeOrNil(sourceSection, node.getFirstNode());
        final RubyNode y = translateNodeOrNil(sourceSection, node.getSecondNode());

        final RubyNode ret = new OrNode(x, y);
        ret.unsafeSetSourceSection(sourceSection);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitPreExeNode(PreExeParseNode node) {
        // The parser seems to visit BEGIN blocks for us first, so we just need to translate them in place
        final RubyNode ret = node.getBodyNode().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitPostExeNode(PostExeParseNode node) {
        // END blocks run after any other code - not just code in the same file

        // Turn into a call to Truffle::Kernel.at_exit

        // The scope is empty - we won't be able to access local variables
        // TODO fix this
        // https://github.com/jruby/jruby/issues/4257
        final StaticScope scope = new StaticScope(StaticScope.Type.BLOCK, null);

        return translateCallNode(
                new CallParseNode(node.getPosition(),
                        new TruffleFragmentParseNode(node.getPosition(), false, new ObjectLiteralNode(context, null, context.getCoreLibrary().getTruffleKernelModule())),
                        "at_exit",
                        new ListParseNode(node.getPosition(), new TrueParseNode(node.getPosition())),
                        new IterParseNode(node.getPosition(), node.getArgsNode(), scope, node.getBodyNode())),
                false, false, false);
    }

    @Override
    public RubyNode visitRationalNode(RationalParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        // TODO(CS): use IntFixnumLiteralNode where possible

        final RubyNode ret = translateRationalComplex(sourceSection, "Rational",
                new LongFixnumLiteralNode(context, fullSourceSection, node.getNumerator()),
                new LongFixnumLiteralNode(context, fullSourceSection, node.getDenominator()));

        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateRationalComplex(RubySourceSection sourceSection, String name, RubyNode a, RubyNode b) {
        // Translate as Truffle.privately { Rational.convert(a, b) }
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode moduleNode = new ObjectLiteralNode(context, fullSourceSection, context.getCoreLibrary().getObjectClass());
        ReadConstantNode receiver = new ReadConstantNode(context, fullSourceSection, moduleNode, name);
        RubyNode[] arguments = new RubyNode[] { a, b };
        RubyCallNodeParameters parameters = new RubyCallNodeParameters(context, fullSourceSection, receiver, "convert", null, arguments, false, true);
        return new RubyCallNode(parameters);
    }

    @Override
    public RubyNode visitRedoNode(RedoParseNode node) {
        if (!environment.isBlock() && !translatingWhile) {
            throw new RaiseException(context.getCoreExceptions().syntaxError("Invalid redo", currentNode));
        }

        final RubyNode ret = new RedoNode();
        ret.unsafeSetSourceSection(translate(node.getPosition()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRegexpNode(RegexpParseNode node) {
        final Rope rope = StringOperations.ropeFromByteList(node.getValue());
        final RegexpOptions options = node.getOptions();
        options.setLiteral(true);
        Regex regex = RegexpNodes.compile(currentNode, context, rope, options);

        // The RegexpNodes.compile operation may modify the encoding of the source rope. This modified copy is stored
        // in the Regex object as the "user object". Since ropes are immutable, we need to take this updated copy when
        // constructing the final regexp.
        final Rope updatedRope = (Rope) regex.getUserObject();
        final DynamicObject regexp = RegexpNodes.createRubyRegexp(context.getCoreLibrary().getRegexpFactory(), regex, updatedRope, options);

        final ObjectLiteralNode literalNode = new ObjectLiteralNode(context, translate(node.getPosition()).toSourceSection(source), regexp);
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
    public RubyNode visitRescueNode(RescueParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        RubyNode tryPart;

        if (node.getBodyNode() == null || node.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
            tryPart = nilNode(source, sourceSection);
        } else {
            tryPart = node.getBodyNode().accept(this);
        }

        final List<RescueNode> rescueNodes = new ArrayList<>();

        RescueBodyParseNode rescueBody = node.getRescueNode();

        if (context.getOptions().BACKTRACES_OMIT_UNUSED
                && rescueBody != null
                && rescueBody.getExceptionNodes() == null
                && rescueBody.getBodyNode() instanceof SideEffectFree
                // allow `expression rescue $!` pattern
                && (!(rescueBody.getBodyNode() instanceof GlobalVarParseNode) || !((GlobalVarParseNode) rescueBody.getBodyNode()).getName().equals("$!"))
                && rescueBody.getOptRescueNode() == null) {
            tryPart = new DisablingBacktracesNode(context, fullSourceSection, tryPart);

            RubyNode bodyNode;

            if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                bodyNode = nilNode(source, sourceSection);
            } else {
                bodyNode = rescueBody.getBodyNode().accept(this);
            }

            final RescueAnyNode rescueNode = new RescueAnyNode(context, fullSourceSection, bodyNode);
            rescueNodes.add(rescueNode);
        } else {
            while (rescueBody != null) {
                if (rescueBody.getExceptionNodes() != null) {
                    if (rescueBody.getExceptionNodes() instanceof ArrayParseNode) {
                        final ParseNode[] exceptionNodes = ((ArrayParseNode) rescueBody.getExceptionNodes()).children();

                        final RubyNode[] handlingClasses = new RubyNode[exceptionNodes.length];

                        for (int n = 0; n < handlingClasses.length; n++) {
                            handlingClasses[n] = exceptionNodes[n].accept(this);
                        }

                        RubyNode translatedBody;

                        if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                            translatedBody = nilNode(source, sourceSection);
                        } else {
                            translatedBody = rescueBody.getBodyNode().accept(this);
                        }

                        final RescueClassesNode rescueNode = new RescueClassesNode(context, fullSourceSection, handlingClasses, translatedBody);
                        rescueNodes.add(rescueNode);
                    } else if (rescueBody.getExceptionNodes() instanceof SplatParseNode) {
                        final SplatParseNode splat = (SplatParseNode) rescueBody.getExceptionNodes();

                        final RubyNode splatTranslated = translateNodeOrNil(sourceSection, splat.getValue());

                        RubyNode bodyTranslated;

                        if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                            bodyTranslated = nilNode(source, sourceSection);
                        } else {
                            bodyTranslated = rescueBody.getBodyNode().accept(this);
                        }

                        final RescueSplatNode rescueNode = new RescueSplatNode(context, fullSourceSection, splatTranslated, bodyTranslated);
                        rescueNodes.add(rescueNode);
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    RubyNode bodyNode;

                    if (rescueBody.getBodyNode() == null || rescueBody.getBodyNode().getPosition() == InvalidSourcePosition.INSTANCE) {
                        bodyNode = nilNode(source, sourceSection);
                    } else {
                        bodyNode = rescueBody.getBodyNode().accept(this);
                    }

                    final RescueAnyNode rescueNode = new RescueAnyNode(context, fullSourceSection, bodyNode);
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

        final RubyNode ret = new TryNode(context, fullSourceSection,
                new ExceptionTranslatingNode(context, fullSourceSection, tryPart, UnsupportedOperationBehavior.TYPE_ERROR),
                rescueNodes.toArray(new RescueNode[rescueNodes.size()]), elsePart);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitRetryNode(RetryParseNode node) {
        final RubyNode ret = new RetryNode();
        ret.unsafeSetSourceSection(translate(node.getPosition()));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitReturnNode(ReturnParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        RubyNode translatedChild = node.getValueNode().accept(this);

        final RubyNode ret = new ReturnNode(environment.getReturnID(), translatedChild);
        ret.unsafeSetSourceSection(sourceSection);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSClassNode(SClassParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        final RubyNode receiverNode = node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = SingletonClassNodeGen.create(context, fullSourceSection, receiverNode);

        final RubyNode ret = openModule(sourceSection, singletonClassNode, "(singleton-def)", node.getBodyNode(), true);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSValueNode(SValueParseNode node) {
        final RubyNode ret = node.getValue().accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSelfNode(SelfParseNode node) {
        final RubyNode ret = new SelfNode(environment.getFrameDescriptor());
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSplatNode(SplatParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final RubyNode value = translateNodeOrNil(sourceSection, node.getValue());
        final RubyNode ret = SplatCastNodeGen.create(context, sourceSection.toSourceSection(source), SplatCastNode.NilBehavior.CONVERT, false, value);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitStrNode(StrParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());

        final ByteList byteList = node.getValue();
        final int codeRange = node.getCodeRange();
        final Rope rope = context.getRopeTable().getRope(byteList.bytes(), byteList.getEncoding(), CodeRange.fromInt(codeRange));

        final RubyNode ret;

        if (node.isFrozen() && !getSourcePath(sourceSection).startsWith(context.getCoreLibrary().getCoreLoadPath() + "/core/")) {
            final DynamicObject frozenString = context.getFrozenStrings().getFrozenString(rope);

            ret = new DefinedWrapperNode(context, sourceSection.toSourceSection(source), context.getCoreStrings().METHOD,
                    new ObjectLiteralNode(context, null, frozenString));
        } else {
            ret = new StringLiteralNode(context, sourceSection.toSourceSection(source), rope);
        }

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitSymbolNode(SymbolParseNode node) {
        String name = node.getName();
        // The symbol is passed as a String but it's really
        // "interpret the char[] as a byte[] with the given encoding".
        byte[] bytes = new byte[name.length()];
        for (int i = 0; i < name.length(); i++) {
            char val = name.charAt(i);
            assert val >= 0 && val < 256;
            bytes[i] = (byte) (val & 0xFF);
        }
        final Rope rope = RopeOperations.create(bytes, node.getEncoding(), CodeRange.CR_UNKNOWN);
        final RubyNode ret = new ObjectLiteralNode(context, translate(node.getPosition()).toSourceSection(source), context.getSymbolTable().getSymbol(rope));
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitTrueNode(TrueParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final RubyNode ret = new BooleanLiteralNode(context, sourceSection.toSourceSection(source), true);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitUndefNode(UndefParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);
        final DynamicObject nameSymbol = translateNameNodeToSymbol(node.getName());

        final RubyNode ret = ModuleNodesFactory.UndefMethodNodeFactory.create(context, fullSourceSection, new RubyNode[]{
                new RaiseIfFrozenNode(context, fullSourceSection, new GetDefaultDefineeNode(context, fullSourceSection)),
                new ObjectLiteralNode(context, fullSourceSection, new Object[]{ nameSymbol })
        });
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitUntilNode(UntilParseNode node) {
        WhileParseNode whileNode = new WhileParseNode(node.getPosition(), node.getConditionNode(), node.getBodyNode(), node.evaluateAtStart());
        copyNewline(node, whileNode);
        final RubyNode ret = translateWhileNode(whileNode, true);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitVCallNode(VCallParseNode node) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        if (node.getName().equals("undefined") && getSourcePath(sourceSection).startsWith(buildCorePath(""))) {
            final RubyNode ret = new ObjectLiteralNode(context, sourceSection.toSourceSection(source), context.getCoreLibrary().getRubiniusUndefined());
            return addNewlineIfNeeded(node, ret);
        }

        final ParseNode receiver = new SelfParseNode(node.getPosition());
        final CallParseNode callNode = new CallParseNode(node.getPosition(), receiver, node.getName(), null, null);
        copyNewline(node, callNode);
        final RubyNode ret = translateCallNode(callNode, true, true, false);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitWhileNode(WhileParseNode node) {
        final RubyNode ret = translateWhileNode(node, false);
        return addNewlineIfNeeded(node, ret);
    }

    private RubyNode translateWhileNode(WhileParseNode node, boolean conditionInversed) {
        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        RubyNode condition = node.getConditionNode().accept(this);
        if (conditionInversed) {
            condition = new NotNode(condition);
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
            loop = WhileNode.createWhile(context, fullSourceSection, condition, body);
        } else {
            loop = WhileNode.createDoWhile(context, fullSourceSection, condition, body);
        }

        final RubyNode ret = new CatchBreakNode(context, fullSourceSection, whileBreakID, loop);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitXStrNode(XStrParseNode node) {
        final ParseNode argsNode = buildArrayNode(node.getPosition(), new StrParseNode(node.getPosition(), node.getValue()));
        final ParseNode callNode = new FCallParseNode(node.getPosition(), "`", argsNode, null);
        final RubyNode ret = callNode.accept(this);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitYieldNode(YieldParseNode node) {
        final List<ParseNode> arguments = new ArrayList<>();

        ParseNode argsNode = node.getArgsNode();

        final boolean unsplat = argsNode instanceof SplatParseNode || argsNode instanceof ArgsCatParseNode;

        if (argsNode instanceof SplatParseNode) {
            argsNode = ((SplatParseNode) argsNode).getValue();
        }

        if (argsNode != null) {
            if (argsNode instanceof ListParseNode) {
                arguments.addAll((node.getArgsNode()).childNodes());
            } else {
                arguments.add(node.getArgsNode());
            }
        }

        final List<RubyNode> argumentsTranslated = new ArrayList<>();

        for (ParseNode argument : arguments) {
            argumentsTranslated.add(argument.accept(this));
        }

        final RubyNode[] argumentsTranslatedArray = argumentsTranslated.toArray(new RubyNode[argumentsTranslated.size()]);

        final RubyNode ret = new YieldExpressionNode(context, translate(node.getPosition()).toSourceSection(source), unsplat, argumentsTranslatedArray);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitZArrayNode(ZArrayParseNode node) {
        final RubyNode[] values = new RubyNode[0];

        final RubyNode ret = ArrayLiteralNode.create(context, translate(node.getPosition()), values);
        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitBackRefNode(BackRefParseNode node) {
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

        final RubySourceSection sourceSection = translate(node.getPosition());
        final SourceSection fullSourceSection = sourceSection.toSourceSection(source);

        environment.declareVarInMethodScope("$~");

        final GetFromThreadLocalNode readMatchNode = new GetFromThreadLocalNode(context, fullSourceSection, environment.findLocalVarNode("$~", source, sourceSection));
        final RubyNode ret = new ReadMatchReferenceNode(context, fullSourceSection, readMatchNode, index);

        return addNewlineIfNeeded(node, ret);
    }

    @Override
    public RubyNode visitStarNode(StarParseNode star) {
        return nilNode(source, translate(star.getPosition()));
    }

    protected RubyNode initFlipFlopStates(RubySourceSection sourceSection) {
        final RubyNode[] initNodes = new RubyNode[environment.getFlipFlopStates().size()];

        for (int n = 0; n < initNodes.length; n++) {
            initNodes[n] = new InitFlipFlopSlotNode(context, sourceSection.toSourceSection(source), environment.getFlipFlopStates().get(n));
        }

        return sequence(context, source, sourceSection, Arrays.asList(initNodes));
    }

    @Override
    protected RubyNode defaultVisit(ParseNode node) {
        throw new UnsupportedOperationException(node.toString() + " " + node.getPosition());
    }

    public TranslatorEnvironment getEnvironment() {
        return environment;
    }

    protected String getIdentifierInNewEnvironment(boolean isBlock, String namedMethodName) {
        if (isBlock) {
            TranslatorEnvironment methodParent = environment;

            while (methodParent.isBlock()) {
                methodParent = methodParent.getParent();
            }

            if (environment.getBlockDepth() + 1 > 1) {
                return StringUtils.format("block (%d levels) in %s", environment.getBlockDepth() + 1, methodParent.getNamedMethodName());
            } else {
                return StringUtils.format("block in %s", methodParent.getNamedMethodName());
            }
        } else {
            return namedMethodName;
        }
    }

    @Override
    public RubyNode visitTruffleFragmentNode(TruffleFragmentParseNode node) {
        return addNewlineIfNeeded(node, node.getFragment());
    }

    @Override
    public RubyNode visitOther(ParseNode node) {
        if (node instanceof ReadLocalDummyParseNode) {
            final ReadLocalDummyParseNode readLocal = (ReadLocalDummyParseNode) node;
            final RubyNode ret = new ReadLocalVariableNode(context, readLocal.getSourceSection(), LocalVariableType.FRAME_LOCAL, readLocal.getFrameSlot());
            return addNewlineIfNeeded(node, ret);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private void copyNewline(ParseNode from, ParseNode to) {
        if (from.isNewline()) {
            to.setNewline();
        }
    }

    private RubyNode addNewlineIfNeeded(ParseNode jrubyNode, RubyNode node) {
        if (jrubyNode.isNewline()) {
            final RubySourceSection current = node.getEncapsulatingRubySourceSection();

            if (current == null) {
                return node;
            }

            if (context.getCoverageManager() != null) {
                context.getCoverageManager().setLineHasCode(source, current.getStartLine());
            }

            node.unsafeSetIsNewLine();
        }

        return node;
    }

}
