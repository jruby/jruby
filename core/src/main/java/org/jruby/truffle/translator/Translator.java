/*
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved. This
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
import com.oracle.truffle.api.impl.DefaultSourceSection;
import org.joni.Regex;
import org.jruby.ast.*;
import org.jruby.common.IRubyWarnings;
import org.jruby.truffle.nodes.DefinedNode;
import org.jruby.truffle.nodes.ReadNode;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.WriteNode;
import org.jruby.truffle.nodes.call.RubyCallNode;
import org.jruby.truffle.nodes.cast.*;
import org.jruby.truffle.nodes.cast.LambdaNode;
import org.jruby.truffle.nodes.constants.EncodingPseudoVariableNode;
import org.jruby.truffle.nodes.constants.UninitializedReadConstantNode;
import org.jruby.truffle.nodes.constants.WriteConstantNode;
import org.jruby.truffle.nodes.control.*;
import org.jruby.truffle.nodes.control.BreakNode;
import org.jruby.truffle.nodes.control.EnsureNode;
import org.jruby.truffle.nodes.control.IfNode;
import org.jruby.truffle.nodes.control.NextNode;
import org.jruby.truffle.nodes.control.RedoNode;
import org.jruby.truffle.nodes.control.RescueNode;
import org.jruby.truffle.nodes.control.RetryNode;
import org.jruby.truffle.nodes.control.ReturnNode;
import org.jruby.truffle.nodes.control.WhileNode;
import org.jruby.truffle.nodes.core.*;
import org.jruby.truffle.nodes.literal.*;
import org.jruby.truffle.nodes.literal.NilNode;
import org.jruby.truffle.nodes.literal.array.UninitialisedArrayLiteralNode;
import org.jruby.truffle.nodes.methods.AddMethodNode;
import org.jruby.truffle.nodes.methods.AliasNode;
import org.jruby.truffle.nodes.methods.MethodDefinitionNode;
import org.jruby.truffle.nodes.methods.locals.*;
import org.jruby.truffle.nodes.objects.*;
import org.jruby.truffle.nodes.objects.ClassNode;
import org.jruby.truffle.nodes.objects.SelfNode;
import org.jruby.truffle.nodes.objects.WriteInstanceVariableNode;
import org.jruby.truffle.nodes.yield.YieldNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyFixnum;
import org.jruby.truffle.runtime.core.RubyRegexp;
import org.jruby.truffle.runtime.core.range.FixnumRange;
import org.jruby.truffle.runtime.methods.UniqueMethodIdentifier;

import java.math.BigInteger;
import java.util.*;

/**
 * A JRuby parser node visitor which translates JRuby AST nodes into our Ruby nodes, implementing a
 * Ruby parser. Therefore there is some namespace contention here! We make all references to JRuby
 * explicit. This is the only place though - it doesn't leak out elsewhere.
 */
public class Translator implements org.jruby.ast.visitor.NodeVisitor {

    protected final Translator parent;

    protected final RubyContext context;
    protected final TranslatorEnvironment environment;
    protected final Source source;
    protected final RubyNodeInstrumenter instrumenter;

    private boolean translatingForStatement = false;

    private static final Map<Class, String> nodeDefinedNames = new HashMap<>();

    static {
        nodeDefinedNames.put(org.jruby.ast.SelfNode.class, "self");
        nodeDefinedNames.put(org.jruby.ast.NilNode.class, "nil");
        nodeDefinedNames.put(org.jruby.ast.TrueNode.class, "true");
        nodeDefinedNames.put(org.jruby.ast.FalseNode.class, "false");
        nodeDefinedNames.put(org.jruby.ast.LocalAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.DAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.GlobalAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.InstAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.ClassVarAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.OpAsgnAndNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.OpAsgnOrNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.OpAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.OpElementAsgnNode.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.MultipleAsgn19Node.class, "assignment");
        nodeDefinedNames.put(org.jruby.ast.GlobalVarNode.class, "global-variable");
        nodeDefinedNames.put(org.jruby.ast.StrNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.DStrNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.FixnumNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.BignumNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.FloatNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.RegexpNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.DRegexpNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.ArrayNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.HashNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.SymbolNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.DotNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.AndNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.OrNode.class, "expression");
        nodeDefinedNames.put(org.jruby.ast.LocalVarNode.class, "local-variable");
        nodeDefinedNames.put(org.jruby.ast.DVarNode.class, "local-variable");
    }

    private static final Set<String> debugIgnoredCalls = new HashSet<>();

    static {
        debugIgnoredCalls.add("downto");
        debugIgnoredCalls.add("each");
        debugIgnoredCalls.add("times");
        debugIgnoredCalls.add("upto");
    }

    /**
     * Global variables which in common usage have frame local semantics.
     */
    public static final Set<String> FRAME_LOCAL_GLOBAL_VARIABLES = new HashSet<>(Arrays.asList("$_"));

    public Translator(RubyContext context, Translator parent, TranslatorEnvironment environment, Source source) {
        this.context = context;
        this.parent = parent;
        this.environment = environment;
        this.source = source;
        this.instrumenter = environment.getNodeInstrumenter();
    }

    @Override
    public Object visitAliasNode(org.jruby.ast.AliasNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.LiteralNode oldName = (org.jruby.ast.LiteralNode) node.getOldName();
        final org.jruby.ast.LiteralNode newName = (org.jruby.ast.LiteralNode) node.getNewName();

        final ClassNode classNode = new ClassNode(context, sourceSection, new SelfNode(context, sourceSection));

        return new AliasNode(context, sourceSection, classNode, newName.getName(), oldName.getName());
    }

    @Override
    public Object visitAndNode(org.jruby.ast.AndNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode x;

        if (node.getFirstNode() == null) {
            x = new NilNode(context, sourceSection);
        } else {
            x = (RubyNode) node.getFirstNode().accept(this);
        }

        RubyNode y;

        if (node.getSecondNode() == null) {
            y = new NilNode(context, sourceSection);
        } else {
            y = (RubyNode) node.getSecondNode().accept(this);
        }

        return AndNodeFactory.create(context, sourceSection, x, y);
    }

    @Override
    public Object visitArgsCatNode(org.jruby.ast.ArgsCatNode node) {
        final List<org.jruby.ast.Node> nodes = new ArrayList<>();
        collectArgsCatNodes(nodes, node);

        final List<RubyNode> translatedNodes = new ArrayList<>();

        for (org.jruby.ast.Node catNode : nodes) {
            translatedNodes.add((RubyNode) catNode.accept(this));
        }

        return new ArrayConcatNode(context, translate(node.getPosition()), translatedNodes.toArray(new RubyNode[translatedNodes.size()]));
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
            nodes.add(node.getSecondNode());
        }
    }

    @Override
    public Object visitArgsNode(org.jruby.ast.ArgsNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitArgsPushNode(org.jruby.ast.ArgsPushNode node) {
        return new ArrayPushNode(context, translate(node.getPosition()), (RubyNode) node.getFirstNode().accept(this), (RubyNode) node.getSecondNode().accept(this));
    }

    @Override
    public Object visitArrayNode(org.jruby.ast.ArrayNode node) {
        final List<org.jruby.ast.Node> values = node.childNodes();

        final RubyNode[] translatedValues = new RubyNode[values.size()];

        for (int n = 0; n < values.size(); n++) {
            translatedValues[n] = (RubyNode) values.get(n).accept(this);
        }

        return new UninitialisedArrayLiteralNode(context, translate(node.getPosition()), translatedValues);
    }

    @Override
    public Object visitAttrAssignNode(org.jruby.ast.AttrAssignNode node) {
        return visitAttrAssignNodeExtraArgument(node, null);
    }

    /**
     * See translateDummyAssignment to understand what this is for.
     */
    public RubyNode visitAttrAssignNodeExtraArgument(org.jruby.ast.AttrAssignNode node, RubyNode extraArgument) {
        final org.jruby.ast.CallNode callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getReceiverNode(), node.getName(), node.getArgsNode());
        return visitCallNodeExtraArgument(callNode, extraArgument);
    }

    @Override
    public Object visitBackRefNode(org.jruby.ast.BackRefNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitBeginNode(org.jruby.ast.BeginNode node) {
        return node.getBodyNode().accept(this);
    }

    @Override
    public Object visitBignumNode(org.jruby.ast.BignumNode node) {
        return new BignumLiteralNode(context, translate(node.getPosition()), node.getValue());
    }

    @Override
    public Object visitBlockArg18Node(org.jruby.ast.BlockArg18Node node) {
        return unimplemented(node);
    }

    @Override
    public Object visitBlockArgNode(org.jruby.ast.BlockArgNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitBlockNode(org.jruby.ast.BlockNode node) {
        final List<org.jruby.ast.Node> children = node.childNodes();

        final List<RubyNode> translatedChildren = new ArrayList<>();

        for (int n = 0; n < children.size(); n++) {
            final RubyNode translatedChild = (RubyNode) children.get(n).accept(this);

            if (!(translatedChild instanceof DeadNode)) {
                translatedChildren.add(translatedChild);
            }
        }

        if (translatedChildren.size() == 1) {
            return translatedChildren.get(0);
        } else {
            return new SequenceNode(context, translate(node.getPosition()), translatedChildren.toArray(new RubyNode[translatedChildren.size()]));
        }
    }

    @Override
    public Object visitBlockPassNode(org.jruby.ast.BlockPassNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitBreakNode(org.jruby.ast.BreakNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode resultNode;

        if (node.getValueNode() == null) {
            resultNode = new NilNode(context, sourceSection);
        } else {
            resultNode = (RubyNode) node.getValueNode().accept(this);
        }

        return new BreakNode(context, sourceSection, resultNode);
    }

    @Override
    public Object visitCallNode(org.jruby.ast.CallNode node) {
        return visitCallNodeExtraArgument(node, null);
    }

    /**
     * See translateDummyAssignment to understand what this is for.
     */
    public RubyNode visitCallNodeExtraArgument(org.jruby.ast.CallNode node, RubyNode extraArgument) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode receiverTranslated = (RubyNode) node.getReceiverNode().accept(this);

        org.jruby.ast.Node args = node.getArgsNode();
        org.jruby.ast.Node block = node.getIterNode();

        if (block == null && args instanceof org.jruby.ast.IterNode) {
            final org.jruby.ast.Node temp = args;
            args = block;
            block = temp;
        }

        final ArgumentsAndBlockTranslation argumentsAndBlock = translateArgumentsAndBlock(sourceSection, block, args, extraArgument);

        RubyNode translated = new RubyCallNode(context, sourceSection, node.getName(), receiverTranslated, argumentsAndBlock.getBlock(), argumentsAndBlock.isSplatted(), argumentsAndBlock.getArguments());

        return instrumenter.instrumentAsCall(translated, node.getName());
    }

    protected class ArgumentsAndBlockTranslation {

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
            return arguments;
        }

        public boolean isSplatted() {
            return isSplatted;
        }

    }

    protected ArgumentsAndBlockTranslation translateArgumentsAndBlock(SourceSection sourceSection, org.jruby.ast.Node iterNode, org.jruby.ast.Node argsNode, RubyNode extraArgument) {
        assert !(argsNode instanceof org.jruby.ast.IterNode);

        final List<org.jruby.ast.Node> arguments = new ArrayList<>();
        org.jruby.ast.Node blockPassNode = null;

        boolean isSplatted = false;

        if (argsNode instanceof org.jruby.ast.ListNode) {
            arguments.addAll(((org.jruby.ast.ListNode) argsNode).childNodes());
        } else if (argsNode instanceof org.jruby.ast.BlockPassNode) {
            final org.jruby.ast.BlockPassNode blockPass = (org.jruby.ast.BlockPassNode) argsNode;

            final org.jruby.ast.Node blockPassArgs = blockPass.getArgsNode();

            if (blockPassArgs instanceof org.jruby.ast.ListNode) {
                arguments.addAll(((org.jruby.ast.ListNode) blockPassArgs).childNodes());
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

        if (iterNode instanceof org.jruby.ast.BlockPassNode) {
            blockPassNode = ((org.jruby.ast.BlockPassNode) iterNode).getBodyNode();
        }

        RubyNode blockTranslated;

        if (blockPassNode != null) {
            blockTranslated = ProcCastNodeFactory.create(context, sourceSection, (RubyNode) blockPassNode.accept(this));
        } else if (iterNode != null) {
            blockTranslated = (RubyNode) iterNode.accept(this);

            if (blockTranslated instanceof NilNode) {
                blockTranslated = null;
            }
        } else {
            blockTranslated = null;
        }

        final List<RubyNode> argumentsTranslated = new ArrayList<>();

        for (org.jruby.ast.Node argument : arguments) {
            argumentsTranslated.add((RubyNode) argument.accept(this));
        }

        if (extraArgument != null) {
            argumentsTranslated.add(extraArgument);
        }

        final RubyNode[] argumentsTranslatedArray = argumentsTranslated.toArray(new RubyNode[argumentsTranslated.size()]);

        return new ArgumentsAndBlockTranslation(blockTranslated, argumentsTranslatedArray, isSplatted);
    }

    @Override
    public Object visitCaseNode(org.jruby.ast.CaseNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode elseNode;

        if (node.getElseNode() != null) {
            elseNode = (RubyNode) node.getElseNode().accept(this);
        } else {
            elseNode = new NilNode(context, sourceSection);
        }

        /*
         * There are two sorts of case - one compares a list of expressions against a value, the
         * other just checks a list of expressions for truth.
         */

        if (node.getCaseNode() != null) {
            // Evaluate the case expression and store it in a local

            final String tempName = environment.allocateLocalTemp();

            final RubyNode readTemp = environment.findLocalVarNode(tempName, sourceSection);

            final RubyNode assignTemp = ((ReadNode) readTemp).makeWriteNode((RubyNode) node.getCaseNode().accept(this));

            /*
             * Build an if expression from the whens and else. Work backwards because the first if
             * contains all the others in its else clause.
             */

            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final org.jruby.ast.WhenNode when = (org.jruby.ast.WhenNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<org.jruby.ast.Node> expressions;

                if (when.getExpressionNodes() instanceof org.jruby.ast.ListNode) {
                    expressions = ((org.jruby.ast.ListNode) when.getExpressionNodes()).childNodes();
                } else {
                    expressions = Arrays.asList(when.getExpressionNodes());
                }

                final List<RubyNode> comparisons = new ArrayList<>();

                for (org.jruby.ast.Node expressionNode : expressions) {
                    final RubyNode rubyExpression = (RubyNode) expressionNode.accept(this);

                    final RubyCallNode comparison = new RubyCallNode(context, sourceSection, "===", rubyExpression, null, false, new RubyNode[]{environment.findLocalVarNode(tempName, sourceSection)});

                    comparisons.add(comparison);
                }

                RubyNode conditionNode = comparisons.get(comparisons.size() - 1);

                // As with the if nodes, we work backwards to make it left associative

                for (int i = comparisons.size() - 2; i >= 0; i--) {
                    conditionNode = OrNodeFactory.create(context, sourceSection, comparisons.get(i), conditionNode);
                }

                // Create the if node

                final BooleanCastNode conditionCastNode = BooleanCastNodeFactory.create(context, sourceSection, conditionNode);

                RubyNode thenNode;

                if (when.getBodyNode() == null) {
                    thenNode = new NilNode(context, sourceSection);
                } else {
                    thenNode = (RubyNode) when.getBodyNode().accept(this);
                }

                final IfNode ifNode = new IfNode(context, sourceSection, conditionCastNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            final RubyNode ifNode = elseNode;

            // A top-level block assigns the temp then runs the if

            return new SequenceNode(context, sourceSection, assignTemp, ifNode);
        } else {
            for (int n = node.getCases().size() - 1; n >= 0; n--) {
                final org.jruby.ast.WhenNode when = (org.jruby.ast.WhenNode) node.getCases().get(n);

                // Make a condition from the one or more expressions combined in an or expression

                final List<org.jruby.ast.Node> expressions;

                if (when.getExpressionNodes() instanceof org.jruby.ast.ListNode) {
                    expressions = ((org.jruby.ast.ListNode) when.getExpressionNodes()).childNodes();
                } else {
                    expressions = Arrays.asList(when.getExpressionNodes());
                }

                final List<RubyNode> tests = new ArrayList<>();

                for (org.jruby.ast.Node expressionNode : expressions) {
                    final RubyNode rubyExpression = (RubyNode) expressionNode.accept(this);
                    tests.add(rubyExpression);
                }

                RubyNode conditionNode = tests.get(tests.size() - 1);

                // As with the if nodes, we work backwards to make it left associative

                for (int i = tests.size() - 2; i >= 0; i--) {
                    conditionNode = OrNodeFactory.create(context, sourceSection, tests.get(i), conditionNode);
                }

                // Create the if node

                final BooleanCastNode conditionCastNode = BooleanCastNodeFactory.create(context, sourceSection, conditionNode);

                final RubyNode thenNode = (RubyNode) when.getBodyNode().accept(this);

                final IfNode ifNode = new IfNode(context, sourceSection, conditionCastNode, thenNode, elseNode);

                // This if becomes the else for the next if

                elseNode = ifNode;
            }

            return elseNode;
        }
    }

    @Override
    public Object visitClassNode(org.jruby.ast.ClassNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getCPath().getName();

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParser(), environment.getParser().allocateReturnID(), true, true,
                        new UniqueMethodIdentifier());
        final ModuleTranslator classTranslator = new ModuleTranslator(context, this, newEnvironment, source);

        final MethodDefinitionNode definitionMethod = classTranslator.compileClassNode(node.getPosition(), node.getCPath().getName(), node.getBodyNode());

        /*
         * See my note in visitDefnNode about where the class gets defined - the same applies here.
         */

        RubyNode superClass;

        ArrayList<RubyNode> nodes = new ArrayList<RubyNode>();

        if (node != null && node.getCPath() != null) {

            for(org.jruby.ast.Node n : node.getCPath().childNodes()){
                if (n instanceof org.jruby.ast.NilNode){
                    NilNode nilNode = (NilNode) visitNilNode((org.jruby.ast.NilNode) n);
                    nodes.add(nilNode);
                } else if(n instanceof Colon2ConstNode) {
                    Colon2ConstNode parentNode = ((Colon2ConstNode) n);
                    final RubyNode lhs = (RubyNode) parentNode.getLeftNode().accept(this);
                    nodes.add(new UninitializedReadConstantNode(context, translate(node.getPosition()), parentNode.getName(), lhs, false));
                } else if(n instanceof ConstNode){
                    ConstNode parentNode = ((ConstNode) n);
                    final SourceSection s = translate(node.getPosition());
                    nodes.add(new UninitializedReadConstantNode(context, s, parentNode.getName(), new SelfNode(context, s), false));
                }
            }
        }

        if (node.getSuperNode() != null) {
            superClass = (RubyNode) node.getSuperNode().accept(this);
        } else {
            superClass = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getObjectClass());
        }
        final DefineOrGetClassNode defineOrGetClass = new DefineOrGetClassNode(context, sourceSection, name, getModuleToDefineModulesIn(sourceSection), superClass);

        return new OpenModuleNode(context, sourceSection, defineOrGetClass, definitionMethod, nodes);
    }

    protected RubyNode getModuleToDefineModulesIn(SourceSection sourceSection) {
        return new ClassNode(context, sourceSection, new SelfNode(context, sourceSection));
    }

    @Override
    public Object visitClassVarAsgnNode(org.jruby.ast.ClassVarAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode receiver = new ClassNode(context, sourceSection, new SelfNode(context, sourceSection));

        final RubyNode rhs = (RubyNode) node.getValueNode().accept(this);

        return new WriteClassVariableNode(context, sourceSection, node.getName(), receiver, rhs);
    }

    @Override
    public Object visitClassVarDeclNode(org.jruby.ast.ClassVarDeclNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitClassVarNode(org.jruby.ast.ClassVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        return new ReadClassVariableNode(context, sourceSection, node.getName(), new SelfNode(context, sourceSection));
    }

    @Override
    public Object visitColon2Node(org.jruby.ast.Colon2Node node) {
        final RubyNode lhs = (RubyNode) node.getLeftNode().accept(this);

        return new UninitializedReadConstantNode(context, translate(node.getPosition()), node.getName(), lhs);
    }

    @Override
    public Object visitColon3Node(org.jruby.ast.Colon3Node node) {
        // Colon3 means the root namespace, as in ::Foo

        final SourceSection sourceSection = translate(node.getPosition());

        final ObjectLiteralNode root = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getMainObject());

        return new UninitializedReadConstantNode(context, sourceSection, node.getName(), root);
    }
    
    @Override
    public Object visitComplexNode(org.jruby.ast.ComplexNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitConstDeclNode(org.jruby.ast.ConstDeclNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final ClassNode classNode = new ClassNode(context, sourceSection, new SelfNode(context, sourceSection));

        return new WriteConstantNode(context, sourceSection, node.getName(), classNode, (RubyNode) node.getValueNode().accept(this));
    }

    @Override
    public Object visitConstNode(org.jruby.ast.ConstNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        return new UninitializedReadConstantNode(context, sourceSection, node.getName(), new SelfNode(context, sourceSection));
    }

    @Override
    public Object visitDAsgnNode(org.jruby.ast.DAsgnNode node) {
        return new org.jruby.ast.LocalAsgnNode(node.getPosition(), node.getName(), node.getDepth(), node.getValueNode()).accept(this);
    }

    @Override
    public Object visitDRegxNode(org.jruby.ast.DRegexpNode node) {
        SourceSection sourceSection = translate(node.getPosition());

        final RubyNode stringNode = translateInterpolatedString(sourceSection, node.childNodes());

        return StringToRegexpNodeFactory.create(context, sourceSection, stringNode);
    }

    @Override
    public Object visitDStrNode(org.jruby.ast.DStrNode node) {
        return translateInterpolatedString(translate(node.getPosition()), node.childNodes());
    }

    @Override
    public Object visitDSymbolNode(org.jruby.ast.DSymbolNode node) {
        SourceSection sourceSection = translate(node.getPosition());

        final RubyNode stringNode = translateInterpolatedString(sourceSection, node.childNodes());

        return StringToSymbolNodeFactory.create(context, sourceSection, stringNode);
    }

    private RubyNode translateInterpolatedString(SourceSection sourceSection, List<org.jruby.ast.Node> childNodes) {
        final List<RubyNode> children = new ArrayList<>();

        for (org.jruby.ast.Node child : childNodes) {
            children.add((RubyNode) child.accept(this));
        }

        return new InterpolatedStringNode(context, sourceSection, children.toArray(new RubyNode[children.size()]));
    }

    @Override
    public Object visitDVarNode(org.jruby.ast.DVarNode node) {
        RubyNode readNode = environment.findLocalVarNode(node.getName(), translate(node.getPosition()));

        if (readNode == null) {
            context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, node.getPosition().getFile(), node.getPosition().getStartLine(), "can't find variable " + node.getName() + ", translating as nil");
            readNode = new NilNode(context, translate(node.getPosition()));
        }

        return readNode;
    }

    @Override
    public Object visitDXStrNode(org.jruby.ast.DXStrNode node) {
        SourceSection sourceSection = translate(node.getPosition());

        final RubyNode string = translateInterpolatedString(sourceSection, node.childNodes());

        return new SystemNode(context, sourceSection, string);
    }

    @Override
    public Object visitDefinedNode(org.jruby.ast.DefinedNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        org.jruby.ast.Node expressionNode = node.getExpressionNode();

        while (expressionNode instanceof org.jruby.ast.NewlineNode) {
            expressionNode = ((org.jruby.ast.NewlineNode) expressionNode).getNextNode();
        }

        final String name = nodeDefinedNames.get(expressionNode.getClass());

        if (name != null) {
            final StringLiteralNode literal = new StringLiteralNode(context, sourceSection, name);
            return literal;
        }

        return new DefinedNode(context, sourceSection, (RubyNode) node.getExpressionNode().accept(this));
    }

    @Override
    public Object visitDefnNode(org.jruby.ast.DefnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final ClassNode classNode = new ClassNode(context, sourceSection, new SelfNode(context, sourceSection));
        return translateMethodDefinition(sourceSection, classNode, node.getName(), node.getArgsNode(), node.getBodyNode());
    }

    @Override
    public Object visitDefsNode(org.jruby.ast.DefsNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode objectNode = (RubyNode) node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = new SingletonClassNode(context, sourceSection, objectNode);

        return translateMethodDefinition(sourceSection, singletonClassNode, node.getName(), node.getArgsNode(), node.getBodyNode());
    }

    private RubyNode translateMethodDefinition(SourceSection sourceSection, RubyNode classNode, String methodName, org.jruby.ast.ArgsNode argsNode, org.jruby.ast.Node bodyNode) {
        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParser(), environment.getParser().allocateReturnID(), true, true,
                        new UniqueMethodIdentifier());

        // ownScopeForAssignments is the same for the defined method as the current one.

        final MethodTranslator methodCompiler = new MethodTranslator(context, this, newEnvironment, false, source);

        final MethodDefinitionNode functionExprNode = methodCompiler.compileFunctionNode(sourceSection, methodName, argsNode, bodyNode);

        /*
         * In the top-level, methods are defined in the class of the main object. This is
         * counter-intuitive - I would have expected them to be defined in the singleton class.
         * Apparently this is a design decision to make top-level methods sort of global.
         * 
         * http://stackoverflow.com/questions/1761148/where-are-methods-defined-at-the-ruby-top-level
         */

        return new AddMethodNode(context, sourceSection, classNode, functionExprNode);
    }

    @Override
    public Object visitDotNode(org.jruby.ast.DotNode node) {
        final RubyNode begin = (RubyNode) node.getBeginNode().accept(this);
        final RubyNode end = (RubyNode) node.getEndNode().accept(this);
        SourceSection sourceSection = translate(node.getPosition());

        if (begin instanceof FixnumLiteralNode && end instanceof FixnumLiteralNode) {
            final int beginValue = ((FixnumLiteralNode) begin).getValue();
            final int endValue = ((FixnumLiteralNode) end).getValue();

            return new ObjectLiteralNode(context, sourceSection, new FixnumRange(context.getCoreLibrary().getRangeClass(), beginValue, endValue, node.isExclusive()));
        }
        // See RangeNode for why there is a node specifically for creating this one type
        return RangeLiteralNodeFactory.create(context, sourceSection, node.isExclusive(), begin, end);
    }

    @Override
    public Object visitEncodingNode(org.jruby.ast.EncodingNode node) {
        SourceSection sourceSection = translate(node.getPosition());
        return new EncodingPseudoVariableNode(context, sourceSection);
    }

    @Override
    public Object visitEnsureNode(org.jruby.ast.EnsureNode node) {
        final RubyNode tryPart = (RubyNode) node.getBodyNode().accept(this);
        final RubyNode ensurePart = (RubyNode) node.getEnsureNode().accept(this);
        return new EnsureNode(context, translate(node.getPosition()), tryPart, ensurePart);
    }

    @Override
    public Object visitEvStrNode(org.jruby.ast.EvStrNode node) {
        return node.getBody().accept(this);
    }

    @Override
    public Object visitFCallNode(org.jruby.ast.FCallNode node) {
        final org.jruby.ast.Node receiver = new org.jruby.ast.SelfNode(node.getPosition());
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), receiver, node.getName(), node.getArgsNode(), node.getIterNode());

        return callNode.accept(this);
    }

    @Override
    public Object visitFalseNode(org.jruby.ast.FalseNode node) {
        return new BooleanLiteralNode(context, translate(node.getPosition()), false);
    }

    @Override
    public Object visitFixnumNode(org.jruby.ast.FixnumNode node) {
        final long value = node.getValue();

        if (value >= RubyFixnum.MIN_VALUE && value <= RubyFixnum.MAX_VALUE) {
            return new FixnumLiteralNode(context, translate(node.getPosition()), (int) value);
        }
        return new BignumLiteralNode(context, translate(node.getPosition()), BigInteger.valueOf(value));
    }

    @Override
    public Object visitFlipNode(org.jruby.ast.FlipNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final RubyNode begin = (RubyNode) node.getBeginNode().accept(this);
        final RubyNode end = (RubyNode) node.getEndNode().accept(this);

        final BooleanCastNode beginCast = BooleanCastNodeFactory.create(context, sourceSection, begin);
        final BooleanCastNode endCast = BooleanCastNodeFactory.create(context, sourceSection, end);
        final FlipFlopStateNode stateNode = createFlipFlopState(sourceSection, 0);

        return new FlipFlopNode(context, sourceSection, beginCast, endCast, stateNode, node.isExclusive());
    }

    protected FlipFlopStateNode createFlipFlopState(SourceSection sourceSection, int depth) {
        final FrameSlot frameSlot = environment.declareVar(environment.allocateLocalTemp());
        environment.getFlipFlopStates().add(frameSlot);

        if (depth == 0) {
            return new LocalFlipFlopStateNode(sourceSection, frameSlot);
        } else {
            return new LevelFlipFlopStateNode(sourceSection, depth, frameSlot);
        }
    }

    @Override
    public Object visitFloatNode(org.jruby.ast.FloatNode node) {
        return new FloatLiteralNode(context, translate(node.getPosition()), node.getValue());
    }

    @Override
    public Object visitForNode(org.jruby.ast.ForNode node) {
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

        final String temp = environment.allocateLocalTemp();

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

        translatingForStatement = true;
        final RubyNode translated = (RubyNode) callNode.accept(this);
        translatingForStatement = false;

        return translated;
    }

    private static org.jruby.ast.Node setRHS(org.jruby.ast.Node node, org.jruby.ast.Node rhs) {
        if (node instanceof org.jruby.ast.LocalAsgnNode) {
            final org.jruby.ast.LocalAsgnNode localAsgnNode = (org.jruby.ast.LocalAsgnNode) node;
            return new org.jruby.ast.LocalAsgnNode(node.getPosition(), localAsgnNode.getName(), 0, rhs);
        } else if (node instanceof org.jruby.ast.DAsgnNode) {
            final org.jruby.ast.DAsgnNode dAsgnNode = (org.jruby.ast.DAsgnNode) node;
            return new org.jruby.ast.DAsgnNode(node.getPosition(), dAsgnNode.getName(), 0, rhs);
        } else if (node instanceof org.jruby.ast.MultipleAsgn19Node) {
            final org.jruby.ast.MultipleAsgn19Node multAsgnNode = (org.jruby.ast.MultipleAsgn19Node) node;
            return new org.jruby.ast.MultipleAsgn19Node(node.getPosition(), multAsgnNode.getPre(), multAsgnNode.getRest(), multAsgnNode.getPost());
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

    @Override
    public Object visitGlobalAsgnNode(org.jruby.ast.GlobalAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getName();
        final RubyNode rhs = (RubyNode) node.getValueNode().accept(this);

        if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, node.getPosition().getFile(), node.getPosition().getStartLine(), "assigning to frame local global variables not implemented");
            return rhs;
        } else {
            final ObjectLiteralNode globalVariablesObjectNode = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getGlobalVariablesObject());

            return new WriteInstanceVariableNode(context, sourceSection, name, globalVariablesObjectNode, rhs);
        }
    }

    @Override
    public Object visitGlobalVarNode(org.jruby.ast.GlobalVarNode node) {
        final String name = node.getName();
        final SourceSection sourceSection = translate(node.getPosition());

        if (FRAME_LOCAL_GLOBAL_VARIABLES.contains(name)) {
            // Assignment is implicit for many of these, so we need to declare when we use

            environment.declareVar(name);

            final RubyNode readNode = environment.findLocalVarNode(name, sourceSection);

            return readNode;
        } else {
            final ObjectLiteralNode globalVariablesObjectNode = new ObjectLiteralNode(context, sourceSection, context.getCoreLibrary().getGlobalVariablesObject());

            return new ReadInstanceVariableNode(context, sourceSection, name, globalVariablesObjectNode);
        }
    }

    @Override
    public Object visitHashNode(org.jruby.ast.HashNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final List<RubyNode> keys = new ArrayList<>();
        final List<RubyNode> values = new ArrayList<>();

        final org.jruby.ast.ListNode entries = node.getListNode();

        assert entries.size() % 2 == 0;

        for (int n = 0; n < entries.size(); n += 2) {
            if (entries.get(n) == null) {
                final NilNode nilNode = new NilNode(context, sourceSection);
                keys.add(nilNode);
            } else {
                keys.add((RubyNode) entries.get(n).accept(this));
            }

            if (entries.get(n + 1) == null) {
                final NilNode nilNode = new NilNode(context, sourceSection);
                values.add(nilNode);
            } else {
                values.add((RubyNode) entries.get(n + 1).accept(this));
            }
        }

        return new HashLiteralNode(translate(node.getPosition()), keys.toArray(new RubyNode[keys.size()]), values.toArray(new RubyNode[values.size()]), context);
    }

    @Override
    public Object visitIfNode(org.jruby.ast.IfNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        org.jruby.ast.Node thenBody = node.getThenBody();

        if (thenBody == null) {
            thenBody = new org.jruby.ast.NilNode(node.getPosition());
        }

        org.jruby.ast.Node elseBody = node.getElseBody();

        if (elseBody == null) {
            elseBody = new org.jruby.ast.NilNode(node.getPosition());
        }

        RubyNode condition;

        if (node.getCondition() == null) {
            condition = new NilNode(context, sourceSection);
        } else {
            condition = (RubyNode) node.getCondition().accept(this);
        }

        final BooleanCastNode conditionCast = BooleanCastNodeFactory.create(context, sourceSection, condition);

        final RubyNode thenBodyTranslated = (RubyNode) thenBody.accept(this);
        final RubyNode elseBodyTranslated = (RubyNode) elseBody.accept(this);

        return new IfNode(context, sourceSection, conditionCast, thenBodyTranslated, elseBodyTranslated);
    }

    @Override
    public Object visitInstAsgnNode(org.jruby.ast.InstAsgnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final String nameWithoutSigil = node.getName();

        final RubyNode receiver = new SelfNode(context, sourceSection);

        RubyNode rhs;

        if (node.getValueNode() == null) {
            rhs = new DeadNode(context, sourceSection);
        } else {
            rhs = (RubyNode) node.getValueNode().accept(this);
        }

        return new WriteInstanceVariableNode(context, sourceSection, nameWithoutSigil, receiver, rhs);
    }

    @Override
    public Object visitInstVarNode(org.jruby.ast.InstVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());
        final String nameWithoutSigil = node.getName();

        final RubyNode receiver = new SelfNode(context, sourceSection);

        return new ReadInstanceVariableNode(context, sourceSection, nameWithoutSigil, receiver);
    }

    @Override
    public Object visitIterNode(org.jruby.ast.IterNode node) {
        /*
         * In a block we do NOT allocate a new return ID - returns will return from the method, not
         * the block (in the general case, see Proc and the difference between Proc and Lambda for
         * specifics).
         */

        final boolean hasOwnScope = !translatingForStatement;

        // Unset this flag for any for any blocks within the for statement's body

        translatingForStatement = false;

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParser(), environment.getReturnID(), hasOwnScope, false,
                        new UniqueMethodIdentifier());
        final MethodTranslator methodCompiler = new MethodTranslator(context, this, newEnvironment, true, source);

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

        return methodCompiler.compileFunctionNode(translate(node.getPosition()), "(block)", argsNode, node.getBodyNode());
    }

    @Override
    public Object visitLiteralNode(org.jruby.ast.LiteralNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitLocalAsgnNode(org.jruby.ast.LocalAsgnNode node) {

        final SourceSection sourceSection = translate(node.getPosition());

        if (environment.getNeverAssignInParentScope()) {
            environment.declareVar(node.getName());
        }

        RubyNode lhs = environment.findLocalVarNode(node.getName(), sourceSection);

        if (lhs == null) {
            if (environment.hasOwnScopeForAssignments()) {
                environment.declareVar(node.getName());
            }

            TranslatorEnvironment environmentToDeclareIn = environment;

            while (!environmentToDeclareIn.hasOwnScopeForAssignments()) {
                environmentToDeclareIn = environmentToDeclareIn.getParent();
            }

            environmentToDeclareIn.declareVar(node.getName());
            lhs = environment.findLocalVarNode(node.getName(), sourceSection);

            if (lhs == null) {
                throw new RuntimeException("shoudln't be here");
            }
        }

        RubyNode rhs;

        if (node.getValueNode() == null) {
            rhs = new DeadNode(context, sourceSection);
        } else {
            rhs = (RubyNode) node.getValueNode().accept(this);
        }

        RubyNode translated = ((ReadNode) lhs).makeWriteNode(rhs);

        final UniqueMethodIdentifier methodIdentifier = environment.findMethodForLocalVar(node.getName());

        return instrumenter.instrumentAsLocalAssignment(translated, methodIdentifier, node.getName());
    }

    @Override
    public Object visitLocalVarNode(org.jruby.ast.LocalVarNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getName();

        RubyNode readNode = environment.findLocalVarNode(name, sourceSection);

        if (readNode == null) {
            context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, node.getPosition().getFile(), node.getPosition().getStartLine(), "local variable " + name + " found by parser but not by translator");
            readNode = environment.findLocalVarNode(environment.allocateLocalTemp(), sourceSection);
        }

        return readNode;
    }

    @Override
    public Object visitMatch2Node(org.jruby.ast.Match2Node node) {
        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        return callNode.accept(this);
    }

    @Override
    public Object visitMatch3Node(org.jruby.ast.Match3Node node) {
        final org.jruby.ast.Node argsNode = buildArrayNode(node.getPosition(), node.getValueNode());
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), node.getReceiverNode(), "=~", argsNode, null);
        return callNode.accept(this);
    }

    @Override
    public Object visitMatchNode(org.jruby.ast.MatchNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitModuleNode(org.jruby.ast.ModuleNode node) {
        // See visitClassNode

        final SourceSection sourceSection = translate(node.getPosition());

        final String name = node.getCPath().getName();

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParser(), environment.getParser().allocateReturnID(), true, true,
                        new UniqueMethodIdentifier());
        final ModuleTranslator classTranslator = new ModuleTranslator(context, this, newEnvironment, source);

        final MethodDefinitionNode definitionMethod = classTranslator.compileClassNode(node.getPosition(), node.getCPath().getName(), node.getBodyNode());

        final DefineOrGetModuleNode defineModuleNode = new DefineOrGetModuleNode(context, sourceSection, name, getModuleToDefineModulesIn(sourceSection));

        return new OpenModuleNode(context, sourceSection, defineModuleNode, definitionMethod);
    }

    @Override
    public Object visitMultipleAsgnNode(org.jruby.ast.MultipleAsgnNode node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object visitMultipleAsgnNode(MultipleAsgn19Node node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final org.jruby.ast.ArrayNode preArray = (org.jruby.ast.ArrayNode) node.getPre();
        final org.jruby.ast.Node rhs = node.getValueNode();

        RubyNode rhsTranslated;

        if (rhs == null) {
            context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, node.getPosition().getFile(), node.getPosition().getStartLine(), "no RHS for multiple assignment - using nil");
            rhsTranslated = new NilNode(context, sourceSection);
        } else {
            rhsTranslated = (RubyNode) rhs.accept(this);
        }

        /*
         * One very common case is to do
         *
         * a, b = c, d
         */

        if (preArray != null && node.getPost() == null && node.getRest() == null && rhsTranslated instanceof UninitialisedArrayLiteralNode &&
                ((UninitialisedArrayLiteralNode) rhsTranslated).getValues().length == preArray.size()) {
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

            final RubyNode[] rhsValues = ((UninitialisedArrayLiteralNode) rhsTranslated).getValues();
            final int assignedValuesCount = preArray.size();

            final RubyNode[] sequence = new RubyNode[assignedValuesCount * 2];

            final RubyNode[] tempValues = new RubyNode[assignedValuesCount];

            for (int n = 0; n < assignedValuesCount; n++) {
                final String tempName = environment.allocateLocalTemp();
                final RubyNode readTemp = environment.findLocalVarNode(tempName, sourceSection);
                final RubyNode assignTemp = ((ReadNode) readTemp).makeWriteNode(rhsValues[n]);
                final RubyNode assignFinalValue = translateDummyAssignment(preArray.get(n), readTemp);

                sequence[n] = assignTemp;
                sequence[assignedValuesCount + n] = assignFinalValue;

                tempValues[n] = readTemp;
            }

            final RubyNode blockNode = new SequenceNode(context, sourceSection, sequence);

            final UninitialisedArrayLiteralNode arrayNode = new UninitialisedArrayLiteralNode(context, sourceSection, tempValues);

            final ElidableResultNode elidableResult = new ElidableResultNode(context, sourceSection, blockNode, arrayNode);

            return elidableResult;
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
             */

            /*
             * Create a temp for the array.
             */

            final String tempName = environment.allocateLocalTemp();

            /*
             * Create a sequence of instructions, with the first being the literal array assigned to
             * the temp.
             */

            final List<RubyNode> sequence = new ArrayList<>();

            final RubyNode splatCastNode = SplatCastNodeFactory.create(context, sourceSection, rhsTranslated);

            final RubyNode writeTemp = ((ReadNode) environment.findLocalVarNode(tempName, sourceSection)).makeWriteNode(splatCastNode);

            sequence.add(writeTemp);

            /*
             * Then index the temp array for each assignment on the LHS.
             */

            for (int n = 0; n < preArray.size(); n++) {
                final ArrayIndexNode assignedValue = ArrayIndexNodeFactory.create(context, sourceSection, n, environment.findLocalVarNode(tempName, sourceSection));

                sequence.add(translateDummyAssignment(preArray.get(n), assignedValue));
            }

            if (node.getRest() != null) {
                final ArrayRestNode assignedValue = new ArrayRestNode(context, sourceSection, preArray.size(), environment.findLocalVarNode(tempName, sourceSection));

                sequence.add(translateDummyAssignment(node.getRest(), assignedValue));
            }

            return new SequenceNode(context, sourceSection, sequence.toArray(new RubyNode[sequence.size()]));
        } else if (node.getPre() == null && node.getPost() == null && node.getRest() instanceof org.jruby.ast.StarNode) {
            return rhsTranslated;
        } else if (node.getPre() == null && node.getPost() == null && node.getRest() != null && rhs != null && !(rhs instanceof org.jruby.ast.ArrayNode)) {
            /*
             * *a = b
             *
             * >= 1.8, this seems to be the same as:
             *
             * a = *b
             */

            final RubyNode restTranslated = ((RubyNode) node.getRest().accept(this)).getNonProxyNode();

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

            final SplatCastNode rhsSplatCast = SplatCastNodeFactory.create(context, sourceSection, rhsTranslated);

            return restRead.makeWriteNode(rhsSplatCast);
        } else if (node.getPre() == null && node.getPost() == null && node.getRest() != null && rhs != null && rhs instanceof org.jruby.ast.ArrayNode) {
            /*
             * *a = [b, c]
             *
             * This seems to be the same as:
             *
             * a = [b, c]
             */

            final RubyNode restTranslated = ((RubyNode) node.getRest().accept(this)).getNonProxyNode();

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

            return restRead.makeWriteNode(rhsTranslated);
        } else {
            throw new RuntimeException("Unknown form of multiple assignment " + node + " at " + node.getPosition());
        }
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

            final WriteNode dummyTranslated = (WriteNode) ((RubyNode) dummyAssignment.accept(this)).getNonProxyNode();
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
            final RubyNode dummyTranslated = (RubyNode) dummyAssignment.accept(this);

            if (dummyTranslated.getNonProxyNode() instanceof WriteLevelVariableNode) {
                translated = ((ReadNode) ((WriteLevelVariableNode) dummyTranslated.getNonProxyNode()).makeReadNode()).makeWriteNode(rhs);
            } else {
                translated = ((ReadNode) ((WriteLocalVariableNode) dummyTranslated.getNonProxyNode()).makeReadNode()).makeWriteNode(rhs);
            }
        } else {
            translated = ((ReadNode) environment.findLocalVarNode(environment.allocateLocalTemp(), sourceSection)).makeWriteNode(rhs);
        }

        return translated;
    }

    @Override
    public Object visitNewlineNode(org.jruby.ast.NewlineNode node) {
        final RubyNode translated = (RubyNode) node.getNextNode().accept(this);
        return instrumenter.instrumentAsStatement(translated);
    }

    @Override
    public Object visitNextNode(org.jruby.ast.NextNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode resultNode;

        if (node.getValueNode() == null) {
            resultNode = new NilNode(context, sourceSection);
        } else {
            resultNode = (RubyNode) node.getValueNode().accept(this);
        }

        return new NextNode(context, sourceSection, resultNode);
    }

    @Override
    public Object visitNilNode(org.jruby.ast.NilNode node) {
        return new NilNode(context, translate(node.getPosition()));
    }

    @Override
    public Object visitNthRefNode(org.jruby.ast.NthRefNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final String name = "$" + node.getMatchNumber();

        RubyNode readLocal = environment.findLocalVarNode(name, sourceSection);

        if (readLocal == null) {
            environment.declareVar(name);
            readLocal = environment.findLocalVarNode(name, sourceSection);
        }

        return readLocal;
    }

    @Override
    public Object visitOpAsgnAndNode(org.jruby.ast.OpAsgnAndNode node) {
        final org.jruby.ast.Node lhs = node.getFirstNode();
        final org.jruby.ast.Node rhs = node.getSecondNode();

        return AndNodeFactory.create(context, translate(node.getPosition()), (RubyNode) lhs.accept(this), (RubyNode) rhs.accept(this));
    }

    @Override
    public Object visitOpAsgnNode(org.jruby.ast.OpAsgnNode node) {
        /*
         * We're going to de-sugar a.foo += c into a.foo = a.foo + c. Note that we can't evaluate a
         * more than once, so we put it into a temporary, and we're doing something more like:
         * 
         * temp = a; temp.foo = temp.foo + c
         */

        final String temp = environment.allocateLocalTemp();
        final org.jruby.ast.Node writeReceiverToTemp = new org.jruby.ast.LocalAsgnNode(node.getPosition(), temp, 0, node.getReceiverNode());
        final org.jruby.ast.Node readReceiverFromTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);

        final org.jruby.ast.Node readMethod = new org.jruby.ast.CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName(), null);
        final org.jruby.ast.Node operation = new org.jruby.ast.CallNode(node.getPosition(), readMethod, node.getOperatorName(), buildArrayNode(node.getPosition(), node.getValueNode()));
        final org.jruby.ast.Node writeMethod = new org.jruby.ast.CallNode(node.getPosition(), readReceiverFromTemp, node.getVariableName() + "=", buildArrayNode(node.getPosition(),
                        operation));

        final org.jruby.ast.BlockNode block = new org.jruby.ast.BlockNode(node.getPosition());
        block.add(writeReceiverToTemp);
        block.add(writeMethod);

        return block.accept(this);
    }

    @Override
    public Object visitOpAsgnOrNode(org.jruby.ast.OpAsgnOrNode node) {
        /*
         * De-sugar x ||= y into x || x = y. No repeated evaluations there so it's easy. It's also
         * basically how jruby-parser represents it already. We'll do it directly, rather than via
         * another JRuby AST node.
         */

        final org.jruby.ast.Node lhs = node.getFirstNode();
        final org.jruby.ast.Node rhs = node.getSecondNode();

        return OrNodeFactory.create(context, translate(node.getPosition()), (RubyNode) lhs.accept(this), (RubyNode) rhs.accept(this));
    }

    @Override
    public Object visitOpElementAsgnNode(org.jruby.ast.OpElementAsgnNode node) {
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

        final String temp = environment.allocateLocalTemp();
        final org.jruby.ast.Node writeArrayToTemp = new org.jruby.ast.LocalAsgnNode(node.getPosition(), temp, 0, node.getReceiverNode());
        final org.jruby.ast.Node readArrayFromTemp = new org.jruby.ast.LocalVarNode(node.getPosition(), 0, temp);

        final org.jruby.ast.Node arrayRead = new org.jruby.ast.CallNode(node.getPosition(), readArrayFromTemp, "[]", buildArrayNode(node.getPosition(), index));

        final String op = node.getOperatorName();

        org.jruby.ast.Node operation = null;

        if (op.equals("||")) {
            operation = new org.jruby.ast.OrNode(node.getPosition(), arrayRead, operand);
        } else if (op.equals("&&")) {
            operation = new org.jruby.ast.AndNode(node.getPosition(), arrayRead, operand);
        } else {
            operation = new org.jruby.ast.CallNode(node.getPosition(), arrayRead, node.getOperatorName(), buildArrayNode(node.getPosition(), operand));
        }

        final org.jruby.ast.Node arrayWrite = new org.jruby.ast.CallNode(node.getPosition(), readArrayFromTemp, "[]=", buildArrayNode(node.getPosition(), index, operation));

        final org.jruby.ast.BlockNode block = new org.jruby.ast.BlockNode(node.getPosition());
        block.add(writeArrayToTemp);
        block.add(arrayWrite);

        return block.accept(this);
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
    public Object visitOrNode(org.jruby.ast.OrNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode x;

        if (node.getFirstNode() == null) {
            x = new NilNode(context, sourceSection);
        } else {
            x = (RubyNode) node.getFirstNode().accept(this);
        }

        RubyNode y;

        if (node.getSecondNode() == null) {
            y = new NilNode(context, sourceSection);
        } else {
            y = (RubyNode) node.getSecondNode().accept(this);
        }

        return OrNodeFactory.create(context, sourceSection, x, y);
    }

    @Override
    public Object visitPostExeNode(org.jruby.ast.PostExeNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitPreExeNode(org.jruby.ast.PreExeNode node) {
        return unimplemented(node);
    }
    
    @Override
    public Object visitRationalNode(org.jruby.ast.RationalNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitRedoNode(org.jruby.ast.RedoNode node) {
        return new RedoNode(context, translate(node.getPosition()));
    }

    @Override
    public Object visitRegexpNode(org.jruby.ast.RegexpNode node) {
        Regex regex;

        if (node.getPattern() != null) {
            regex = node.getPattern().getPattern();
        } else {
            regex = RubyRegexp.compile(context, node.getValue().bytes(), node.getEncoding(), node.getOptions().toOptions());
        }

        final RubyRegexp regexp = new RubyRegexp(context.getCoreLibrary().getRegexpClass(), regex, node.getValue().toString());
        final ObjectLiteralNode literalNode = new ObjectLiteralNode(context, translate(node.getPosition()), regexp);
        return literalNode;
    }

    @Override
    public Object visitRescueBodyNode(org.jruby.ast.RescueBodyNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitRescueNode(org.jruby.ast.RescueNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode tryPart;

        if (node.getBodyNode() != null) {
            tryPart = (RubyNode) node.getBodyNode().accept(this);
        } else {
            tryPart = new NilNode(context, sourceSection);
        }

        final List<RescueNode> rescueNodes = new ArrayList<>();

        org.jruby.ast.RescueBodyNode rescueBody = node.getRescueNode();

        while (rescueBody != null) {
            if (rescueBody.getExceptionNodes() != null) {
                if (rescueBody.getExceptionNodes() instanceof org.jruby.ast.ArrayNode) {
                    final List<org.jruby.ast.Node> exceptionNodes = ((org.jruby.ast.ArrayNode) rescueBody.getExceptionNodes()).childNodes();

                    final RubyNode[] handlingClasses = new RubyNode[exceptionNodes.size()];

                    for (int n = 0; n < handlingClasses.length; n++) {
                        handlingClasses[n] = (RubyNode) exceptionNodes.get(n).accept(this);
                    }

                    RubyNode translatedBody;

                    if (rescueBody.getBodyNode() == null) {
                        translatedBody = new NilNode(context, sourceSection);
                    } else {
                        translatedBody = (RubyNode) rescueBody.getBodyNode().accept(this);
                    }

                    final RescueClassesNode rescueNode = new RescueClassesNode(context, sourceSection, handlingClasses, translatedBody);
                    rescueNodes.add(rescueNode);
                } else if (rescueBody.getExceptionNodes() instanceof org.jruby.ast.SplatNode) {
                    final org.jruby.ast.SplatNode splat = (org.jruby.ast.SplatNode) rescueBody.getExceptionNodes();

                    RubyNode splatTranslated;

                    if (splat.getValue() == null) {
                        splatTranslated = new NilNode(context, sourceSection);
                    } else {
                        splatTranslated = (RubyNode) splat.getValue().accept(this);
                    }

                    RubyNode bodyTranslated;

                    if (rescueBody.getBodyNode() == null) {
                        bodyTranslated = new NilNode(context, sourceSection);
                    } else {
                        bodyTranslated = (RubyNode) rescueBody.getBodyNode().accept(this);
                    }

                    final RescueSplatNode rescueNode = new RescueSplatNode(context, sourceSection, splatTranslated, bodyTranslated);
                    rescueNodes.add(rescueNode);
                } else {
                    unimplemented(node);
                }
            } else {
                RubyNode bodyNode;

                if (rescueBody.getBodyNode() == null) {
                    bodyNode = new NilNode(context, sourceSection);
                } else {
                    bodyNode = (RubyNode) rescueBody.getBodyNode().accept(this);
                }

                final RescueAnyNode rescueNode = new RescueAnyNode(context, sourceSection, bodyNode);
                rescueNodes.add(rescueNode);
            }

            rescueBody = rescueBody.getOptRescueNode();
        }

        RubyNode elsePart;

        if (node.getElseNode() != null) {
            elsePart = (RubyNode) node.getElseNode().accept(this);
        } else {
            elsePart = new NilNode(context, sourceSection);
        }

        return new TryNode(context, sourceSection, tryPart, rescueNodes.toArray(new RescueNode[rescueNodes.size()]), elsePart);
    }

    @Override
    public Object visitRestArgNode(org.jruby.ast.RestArgNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitRetryNode(org.jruby.ast.RetryNode node) {
        return new RetryNode(context, translate(node.getPosition()));
    }

    @Override
    public Object visitReturnNode(org.jruby.ast.ReturnNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode translatedChild;

        if (node.getValueNode() == null) {
            translatedChild = new NilNode(context, sourceSection);
        } else {
            translatedChild = (RubyNode) node.getValueNode().accept(this);
        }

        return new ReturnNode(context, sourceSection, environment.getReturnID(), translatedChild);
    }

    @Override
    public Object visitRootNode(org.jruby.ast.RootNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitSClassNode(org.jruby.ast.SClassNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParser(), environment.getParser().allocateReturnID(), true, true,
                        new UniqueMethodIdentifier());
        final ModuleTranslator classTranslator = new ModuleTranslator(context, this, newEnvironment, source);

        final MethodDefinitionNode definitionMethod = classTranslator.compileClassNode(node.getPosition(), "singleton", node.getBodyNode());

        final RubyNode receiverNode = (RubyNode) node.getReceiverNode().accept(this);

        final SingletonClassNode singletonClassNode = new SingletonClassNode(context, sourceSection, receiverNode);

        return new OpenModuleNode(context, sourceSection, singletonClassNode, definitionMethod);
    }

    @Override
    public Object visitSValueNode(org.jruby.ast.SValueNode node) {
        return node.getValue().accept(this);
    }

    @Override
    public Object visitSelfNode(org.jruby.ast.SelfNode node) {
        return new SelfNode(context, translate(node.getPosition()));
    }

    @Override
    public Object visitSplatNode(org.jruby.ast.SplatNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode value;

        if (node.getValue() == null) {
            value = new NilNode(context, sourceSection);
        } else {
            value = (RubyNode) node.getValue().accept(this);
        }

        return SplatCastNodeFactory.create(context, sourceSection, value);
    }

    @Override
    public Object visitStrNode(org.jruby.ast.StrNode node) {
        return new StringLiteralNode(context, translate(node.getPosition()), node.getValue().toString());
    }

    @Override
    public Object visitSuperNode(org.jruby.ast.SuperNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitSymbolNode(org.jruby.ast.SymbolNode node) {
        return new ObjectLiteralNode(context, translate(node.getPosition()), context.newSymbol(node.getName()));
    }

    @Override
    public Object visitToAryNode(org.jruby.ast.ToAryNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitTrueNode(org.jruby.ast.TrueNode node) {
        return new BooleanLiteralNode(context, translate(node.getPosition()), true);
    }

    @Override
    public Object visitUndefNode(org.jruby.ast.UndefNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitUntilNode(org.jruby.ast.UntilNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode condition;

        if (node.getConditionNode() == null) {
            condition = new NilNode(context, sourceSection);
        } else {
            condition = (RubyNode) node.getConditionNode().accept(this);
        }

        final BooleanCastNode conditionCast = BooleanCastNodeFactory.create(context, sourceSection, condition);
        final NotNode conditionCastNot = new NotNode(context, sourceSection, conditionCast);
        final BooleanCastNode conditionCastNotCast = BooleanCastNodeFactory.create(context, sourceSection, conditionCastNot);

        final RubyNode body = (RubyNode) node.getBodyNode().accept(this);

        return new WhileNode(context, sourceSection, conditionCastNotCast, body);
    }

    @Override
    public Object visitVAliasNode(org.jruby.ast.VAliasNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitVCallNode(org.jruby.ast.VCallNode node) {
        final org.jruby.ast.Node receiver = new org.jruby.ast.SelfNode(node.getPosition());
        final org.jruby.ast.Node args = null;
        final org.jruby.ast.Node callNode = new org.jruby.ast.CallNode(node.getPosition(), receiver, node.getName(), args);

        return callNode.accept(this);
    }

    @Override
    public Object visitWhenNode(org.jruby.ast.WhenNode node) {
        return unimplemented(node);
    }

    @Override
    public Object visitWhileNode(org.jruby.ast.WhileNode node) {
        final SourceSection sourceSection = translate(node.getPosition());

        RubyNode condition;

        if (node.getConditionNode() == null) {
            condition = new NilNode(context, sourceSection);
        } else {
            condition = (RubyNode) node.getConditionNode().accept(this);
        }

        final BooleanCastNode conditionCast = BooleanCastNodeFactory.create(context, sourceSection, condition);

        final RubyNode body = (RubyNode) node.getBodyNode().accept(this);

        return new WhileNode(context, sourceSection, conditionCast, body);
    }

    @Override
    public Object visitXStrNode(org.jruby.ast.XStrNode node) {
        SourceSection sourceSection = translate(node.getPosition());

        final StringLiteralNode literal = new StringLiteralNode(context, sourceSection, node.getValue().toString());

        return new SystemNode(context, sourceSection, literal);
    }

    @Override
    public Object visitYieldNode(org.jruby.ast.YieldNode node) {
        final List<org.jruby.ast.Node> arguments = new ArrayList<>();

        org.jruby.ast.Node argsNode = node.getArgsNode();

        final boolean unsplat = argsNode instanceof org.jruby.ast.SplatNode;

        if (unsplat) {
            argsNode = ((org.jruby.ast.SplatNode) argsNode).getValue();
        }

        if (argsNode != null) {
            if (argsNode instanceof org.jruby.ast.ListNode) {
                arguments.addAll(((org.jruby.ast.ListNode) node.getArgsNode()).childNodes());
            } else {
                arguments.add(node.getArgsNode());
            }
        }

        final List<RubyNode> argumentsTranslated = new ArrayList<>();

        for (org.jruby.ast.Node argument : arguments) {
            argumentsTranslated.add((RubyNode) argument.accept(this));
        }

        final RubyNode[] argumentsTranslatedArray = argumentsTranslated.toArray(new RubyNode[argumentsTranslated.size()]);

        return new YieldNode(context, translate(node.getPosition()), argumentsTranslatedArray, unsplat);
    }

    @Override
    public Object visitZArrayNode(org.jruby.ast.ZArrayNode node) {
        final RubyNode[] values = new RubyNode[0];

        return new UninitialisedArrayLiteralNode(context, translate(node.getPosition()), values);
    }

    @Override
    public Object visitZSuperNode(org.jruby.ast.ZSuperNode node) {
        return unimplemented(node);
    }

    public Object visitArgumentNode(org.jruby.ast.ArgumentNode node) {
        return unimplemented(node);
    }

    public Object visitKeywordArgNode(org.jruby.ast.KeywordArgNode node) {
        return unimplemented(node);
    }

    public Object visitRequiredKeywordArgumentValueNode(RequiredKeywordArgumentValueNode node) {
        return unimplemented(node);
    }

    public Object visitKeywordRestArgNode(org.jruby.ast.KeywordRestArgNode node) {
        return unimplemented(node);
    }

    public Object visitListNode(org.jruby.ast.ListNode node) {
        return unimplemented(node);
    }

    public Object visitOptArgNode(org.jruby.ast.OptArgNode node) {
        return unimplemented(node);
    }

    public Object visitLambdaNode(org.jruby.ast.LambdaNode node) {
        // TODO(cs): code copied and modified from visitIterNode - extract common

        final TranslatorEnvironment newEnvironment = new TranslatorEnvironment(context, environment, environment.getParser(), environment.getReturnID(), false, false, new UniqueMethodIdentifier());
        final MethodTranslator methodCompiler = new MethodTranslator(context, this, newEnvironment, false, source);

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

        final MethodDefinitionNode definitionNode = methodCompiler.compileFunctionNode(translate(node.getPosition()), "(lambda)", argsNode, node.getBodyNode());

        return new LambdaNode(context, translate(node.getPosition()), definitionNode);
    }

    protected Object unimplemented(org.jruby.ast.Node node) {
        context.getRuntime().getWarnings().warn(IRubyWarnings.ID.TRUFFLE, node.getPosition().getFile(), node.getPosition().getStartLine(), node + " does nothing - translating as nil");
        return new NilNode(context, translate(node.getPosition()));
    }

    protected SourceSection translate(final org.jruby.lexer.yacc.ISourcePosition sourcePosition) {
        try {
            // TODO(cs): get an identifier
            final String identifier = "(identifier)";

            // TODO(cs): work out the start column
            final int startColumn = -1;

            final int charLength = -1;

            return new DefaultSourceSection(source, identifier, sourcePosition.getStartLine() + 1, startColumn, -1, charLength);
        } catch (UnsupportedOperationException e) {
            // In some circumstances JRuby can't tell you what the position is
            return translate(new org.jruby.lexer.yacc.SimpleSourcePosition("(unknown)", 0));
        }
    }

    protected SequenceNode initFlipFlopStates(SourceSection sourceSection) {
        final RubyNode[] initNodes = new RubyNode[environment.getFlipFlopStates().size()];

        for (int n = 0; n < initNodes.length; n++) {
            initNodes[n] = new InitFlipFlopSlotNode(context, sourceSection, environment.getFlipFlopStates().get(n));
        }

        return new SequenceNode(context, sourceSection, initNodes);
    }

}
