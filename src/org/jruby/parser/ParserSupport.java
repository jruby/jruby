/*
 * ParserSupport.java - description
 * Created on 23.02.2002, 13:41:01
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jruby.parser;

import org.jruby.ast.types.IListNode;
import org.jruby.ast.types.IAssignableNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.*;
import org.jruby.ast.visitor.UselessStatementVisitor;
import org.jruby.ast.util.NodeUtil;
import org.jruby.ast.util.ListNodeUtil;
import org.jruby.common.IErrors;
import org.jruby.util.IdUtil;
import org.jruby.util.Asserts;
import org.ablaf.common.IErrorHandler;
import org.ablaf.common.ISourcePosition;
import org.ablaf.ast.INode;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/** Ruby 1.6.7 compatible.
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class ParserSupport {
    // Parser states:
    private LocalNamesStack localNames;
    private BlockNamesStack blockNames;

    private int inSingle;
    private boolean inDef;
    private boolean inDefined;

    private int classNest;

    // Abstract Language Framework
    private IErrorHandler errorHandler;

    private IRubyParserConfiguration configuration;
    private RubyParserResult result;

    public void reset() {
        localNames = new LocalNamesStack();
        blockNames = new BlockNamesStack(localNames);

        inSingle = 0;
        inDef = false;
        inDefined = false;

        classNest = configuration.isClassNest() ? 1 : 0;
    }


    public String getOperatorName(int operatorName) {
        if (operatorName >= Token.tUPLUS && operatorName <= Token.tCOLON2) {
            return Token.operators[operatorName - Token.tUPLUS];
        } else {
            return String.valueOf((char) operatorName);
        }
    }

    public INode arg_blk_pass(IListNode firstNode, BlockPassNode secondNode) {
        if (secondNode != null) {
            secondNode.setArgsNode(firstNode);
            return secondNode;
        }
        return firstNode;
    }

    public INode appendPrintToBlock(INode block) {
        return appendToBlock(block, new FCallNode(null, "print", new ArrayNode(block.getPosition()).add(new GlobalVarNode(block.getPosition(), "$_"))));
    }

    public INode appendWhileLoopToBlock(INode block, boolean chop, boolean split) {
        if (split) {
            block = appendToBlock(new GlobalAsgnNode(null, "$F", new CallNode(null, new GlobalVarNode(null, "$_"), "split", null)), block);
        }
        if (chop) {
            block = appendToBlock(new CallNode(null, new GlobalVarNode(null, "$_"), "chop!", null), block);
        }
        return new OptNNode(null, block);
    }

    /**
     * Returns a Node representing the access of the
     * variable or constant named id.
     *
     * see gettable() in MRI
     *@param id The name of the variable or constant.
     *@return   A node representing the access.
     */
    public INode getAccessNode(String id, ISourcePosition iPosition) {
        if (IdUtil.isLocal(id)) {
            if (blockNames.isInBlock() && blockNames.isDefined(id)) {
                return new DVarNode(iPosition, id);
            } else if (getLocalNames().isLocalRegistered(id)) {
                return new LocalVarNode(iPosition, getLocalNames().getLocalIndex(id));
            }
            return new VCallNode(iPosition, id); // Method call without arguments.
        } else if (IdUtil.isGlobal(id)) {
            return new GlobalVarNode(iPosition, id);
        } else if (IdUtil.isInstanceVariable(id)) {
            return new InstVarNode(iPosition, id);
        } else if (IdUtil.isConstant(id)) {
            return new ConstNode(iPosition, id);
        } else if (IdUtil.isClassVariable(id)) {
            /* [REMOVED 1.6.7] if (isInSingle()) {
                return new CVar2Node(iPosition, id);
            }*/
            return new ClassVarNode(iPosition, id);
        }
        Asserts.notReached();
        return null;
    }

    /**
     * Returns a Node representing the assignment of value to
     * the variable or constant named id.
     *
     * cf assignable in MRI
     *
     *@param name The name of the variable or constant.
     *@param valueNode A Node representing the value which should be assigned.
     *@return A Node representing the assignment.
	 * @fixme need to handle positions
     */
    public INode getAssignmentNode(String name, INode valueNode, ISourcePosition position) {
        checkExpression(valueNode);

        if (IdUtil.isLocal(name)) {
            if (blockNames.isDefined(name)) {
                return new DAsgnNode(position, name, valueNode);
            } else if (getLocalNames().isLocalRegistered(name) || !blockNames.isInBlock()) {
                return new LocalAsgnNode(position, getLocalNames().getLocalIndex(name), valueNode);
            } else {
                blockNames.add(name);
                return new DAsgnNode(position, name, valueNode);
            }
        } else if (IdUtil.isGlobal(name)) {
            return new GlobalAsgnNode(position, name, valueNode);
        } else if (IdUtil.isInstanceVariable(name)) {
            return new InstAsgnNode(position, name, valueNode);
        } else if (IdUtil.isConstant(name)) {
            if (isInDef() || isInSingle()) {
                errorHandler.handleError(IErrors.SYNTAX_ERROR, position, "Dynamic constant assignment.");
            }
            return new ConstDeclNode(position, name, valueNode);
        } else if (IdUtil.isClassVariable(name)) {
            if (isInDef() || isInSingle()) {
                return new ClassVarAsgnNode(position, name, valueNode);
            }
            return new ClassVarDeclNode(position, name, valueNode);
        } else {
            Asserts.notReached("Id '" + name + "' not allowed for variable.");
            return null;
        }
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     *@param node
     *@return a NewlineNode or null if node is null.
     */
    public INode newline_node(INode node, ISourcePosition iPosition) {
        if (node != null) {
			return new NewlineNode(iPosition, node);
        } else {
            return null;
        }
    }

    public INode appendToBlock(INode head, INode tail) {
        if (tail == null) {
            return head;
        } else if (head == null) {
            return tail;
        }

        if (!(head instanceof BlockNode)) {
            head = new BlockNode(head.getPosition()).add(head);
        }

        if (errorHandler.isHandled(IErrors.VERBOSE) && NodeUtil.isBreakStatement(ListNodeUtil.getLast((IListNode) head))) {
            errorHandler.handleError(IErrors.WARNING, tail.getPosition(), "Statement not reached.", null);
        }

        if (tail instanceof BlockNode) {
            ListNodeUtil.addAll((IListNode) head, (IListNode) tail);
        } else {
            ((IListNode) head).add(tail);
        }

        return head;
    }

    public INode getOperatorCallNode(INode firstNode, String operator) {
        checkExpression(firstNode);

        return new CallNode(firstNode.getPosition(), firstNode, operator, null);
    }

    public INode getOperatorCallNode(INode firstNode, String operator, INode secondNode) {
        checkExpression(firstNode);
        checkExpression(secondNode);

        return new CallNode(firstNode.getPosition(), firstNode, operator, new ArrayNode(secondNode.getPosition()).add(secondNode));
    }

    public INode getMatchNode(INode firstNode, INode secondNode) {
        getLocalNames().getLocalIndex("~");

        if (firstNode instanceof DRegexpNode || firstNode instanceof RegexpNode) {
            return new Match2Node(firstNode.getPosition(), firstNode, secondNode);
        } else if (secondNode instanceof DRegexpNode || secondNode instanceof RegexpNode) {
            return new Match3Node(firstNode.getPosition(), secondNode, firstNode);
        } else {
            return getOperatorCallNode(firstNode, "=~", secondNode);
        }
    }

    public INode getElementAssignmentNode(INode recv, IListNode idx) {
        checkExpression(recv);

        return new CallNode(recv.getPosition(), recv, "[]=", idx);
    }

    public INode getAttributeAssignmentNode(INode recv, String name) {
        checkExpression(recv);

        return new CallNode(recv.getPosition(), recv, name + "=", null);
    }

	/**
	 * @fixme need to handle positions
	 **/
    public void backrefAssignError(INode node) {
        if (node instanceof NthRefNode) {
            // FIXME: position
            errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Can't set variable $" + ((NthRefNode) node).getMatchNumber() + '.', null);
        } else if (node instanceof BackRefNode) {
            // FIXME: position
            errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Can't set variable $" + ((BackRefNode) node).getType() + '.', null);
        }
    }

	/**
	 * @fixme position
	 **/
    public INode node_assign(INode lhs, INode rhs) {
        if (lhs == null) {
            return null;
        }
        INode result = lhs;

        checkExpression(rhs);

        if (lhs instanceof IAssignableNode) {
            ((IAssignableNode) lhs).setValueNode(rhs);
        } else if (lhs instanceof CallNode) {
			CallNode lCallLHS = (CallNode) lhs;
			IListNode lArgs = lCallLHS.getArgsNode();
			if (lArgs == null) {
				lArgs = new ArrayNode(lhs.getPosition());
				result = new CallNode(lCallLHS.getPosition(), lCallLHS.getReceiverNode(), lCallLHS.getName(), lArgs);
			}
            lArgs.add(rhs);
        }
        return result;
    }

    public void checkExpression(INode node) {
        if (!NodeUtil.isExpression(node)) {
            errorHandler.handleError(IErrors.SYNTAX_ERROR, node.getPosition(), "Void value expression.", null);
        }
    }

    public void checkUselessStatement(INode node) {
        if (errorHandler.isHandled(IErrors.VERBOSE)) {
            new UselessStatementVisitor(errorHandler).acceptNode(node);
        }
    }

    public void checkUselessStatements(BlockNode blockNode) {
        if (errorHandler.isHandled(IErrors.VERBOSE)) {
            Iterator iterator = blockNode.iterator();
            while (iterator.hasNext()) {
                checkUselessStatement((INode) iterator.next());
            }
        }
    }

	/**
	 * @fixme error handling
	 **/
    private boolean checkAssignmentInCondition(INode node) {
        if (node instanceof MultipleAsgnNode) {
            // FIXME
            errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Multiple assignment in conditional.", null);
            return true;
        } else if (node instanceof LocalAsgnNode || node instanceof DAsgnNode || node instanceof GlobalAsgnNode || node instanceof InstAsgnNode) {
            INode valueNode = ((IAssignableNode) node).getValueNode();
            if (valueNode instanceof ILiteralNode || valueNode instanceof NilNode || valueNode instanceof TrueNode || valueNode instanceof FalseNode) {
                errorHandler.handleError(IErrors.WARN, null, "Found '=' in conditional, should be '=='.", null);
            }
            return true;
        } else {
            return false;
        }
    }

    private INode cond0(INode node) {
        checkAssignmentInCondition(node);

        if (node instanceof DRegexpNode) {
            getLocalNames().getLocalIndex("_");
            getLocalNames().getLocalIndex("~");
            return new Match2Node(node.getPosition(), node, new GlobalVarNode(node.getPosition(), "$_"));
        } else if (node instanceof DotNode) {
            FlipNode flipNode =
                new FlipNode(
                    node.getPosition(),
                    getFlipConditionNode(((DotNode) node).getBeginNode()),
                    getFlipConditionNode(((DotNode) node).getEndNode()),
                    ((DotNode) node).isExclusive());

            flipNode.setCount(localNames.registerLocal(null));
            return flipNode;
        } else if (node instanceof RegexpNode) {
            return new MatchNode(node.getPosition(), node);
        } else if (node instanceof StrNode) {
            getLocalNames().getLocalIndex("_");
            getLocalNames().getLocalIndex("~");
            return new MatchNode(node.getPosition(), new RegexpNode(node.getPosition(), ((StrNode) node).getValue(), 0));
        } else {
            return node;
        }
    }

    public INode getConditionNode(INode node) {
        if (node == null) {
            return null;
        } else if (node instanceof NewlineNode) {
            return new NewlineNode(node.getPosition(), cond0(((NewlineNode) node).getNextNode()));
        } else {
            return cond0(node);
        }
    }

    private INode getFlipConditionNode(INode node) {
        node = getConditionNode(node);

        if (node instanceof NewlineNode) {
            return ((NewlineNode) node).getNextNode();
        } else if (node instanceof FixnumNode) {
            return getOperatorCallNode(node, "==", new GlobalVarNode(node.getPosition(), "$."));
        } else {
            return node;
        }
    }

    public AndNode newAndNode(INode left, INode right) {
        checkExpression(left);
        return new AndNode(left.getPosition(), getConditionNode(left), getConditionNode(right));
    }

    public OrNode newOrNode(INode left, INode right) {
        checkExpression(left);
        return new OrNode(left.getPosition(), getConditionNode(left), getConditionNode(right));
    }

	/**
	 * @fixme position
	 **/
    public INode getReturnArgsNode(INode node) {
        if (node instanceof ArrayNode && ListNodeUtil.getLength((IListNode) node) == 1) {
            return (INode) ((IListNode) node).iterator().next();
        } else if (node instanceof BlockPassNode) {
            // FIXME: position
            errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Block argument should not be given.", null);
        }
        return node;
    }

    public INode new_call(INode receiverNode, String name, INode args) {
        if (args != null && args instanceof BlockPassNode) {
            ((BlockPassNode) args).setIterNode(new CallNode(receiverNode.getPosition(), receiverNode, name, ((BlockPassNode) args).getArgsNode()));
            return args;
        }
        return new CallNode(receiverNode.getPosition(), receiverNode, name, (IListNode) args);
    }

    public INode new_fcall(String name, INode args, ISourcePosition iPosition) {
        if (args != null && args instanceof BlockPassNode) {
            ((BlockPassNode) args).setIterNode(new FCallNode(args.getPosition(), name, ((BlockPassNode) args).getArgsNode()));
            return args;
        }
        return new FCallNode(iPosition, name, (IListNode) args);
    }

    public INode new_super(INode args, ISourcePosition iPosition) {
        if (args != null && args instanceof BlockPassNode) {
            ((BlockPassNode) args).setIterNode(new SuperNode(args.getPosition(), ((BlockPassNode) args).getArgsNode()));
            return args;
        }
        return new SuperNode(iPosition, args);
    }

    /**
    *  Description of the Method
    */
    public void initTopLocalVariables() {
        localNames.push();

        List names = configuration.getLocalVariables();
        if (names != null && names.size() > 0) {
            localNames.setNames(new ArrayList(names));
        }

        if (configuration.getBlockVariables() != null) {
            blockNames.push(configuration.getBlockVariables());
        }
    }

    /**
     *  Description of the Method
     */
    public void updateTopLocalVariables() {
        result.setLocalVariables(localNames.getNames().size() > 0 ? localNames.getNames() : null);
        result.setBlockVariables(blockNames.isInBlock() ? blockNames.getNames() : null);

        localNames.pop();
    }

    /** Getter for property inSingle.
     * @return Value of property inSingle.
     */
    public boolean isInSingle() {
        return inSingle != 0;
    }

    /** Setter for property inSingle.
     * @param inSingle New value of property inSingle.
     */
    public void setInSingle(int inSingle) {
        this.inSingle = inSingle;
    }

    public boolean isInDef() {
        return inDef;
    }

    public void setInDef(boolean inDef) {
        this.inDef = inDef;
    }

    /** Getter for property inSingle.
     * @return Value of property inSingle.
     */
    public int getInSingle() {
        return inSingle;
    }

    /** Getter for property inDefined.
     * @return Value of property inDefined.
     */
    public boolean isInDefined() {
        return inDefined;
    }

    /** Setter for property inDefined.
     * @param inDefined New value of property inDefined.
     */
    public void setInDefined(boolean inDefined) {
        this.inDefined = inDefined;
    }

    public boolean isCompileForEval() {
        return configuration.isCompileForEval();
    }

    /** Getter for property classNest.
     * @return Value of property classNest.
     */
    public int getClassNest() {
        return classNest;
    }

    /** Setter for property classNest.
     * @param classNest New value of property classNest.
     */
    public void setClassNest(int classNest) {
        this.classNest = classNest;
    }

    /**
     * Gets the blockNames.
     * @return Returns a BlockNamesStack
     */
    public BlockNamesStack getBlockNames() {
        return blockNames;
    }

    /**
     * Gets the localNames.
     * @return Returns a LocalNamesStack
     */
    public LocalNamesStack getLocalNames() {
        return localNames;
    }

    /**
     * Gets the result.
     * @return Returns a RubyParserResult
     */
    public RubyParserResult getResult() {
        return result;
    }

    /**
     * Sets the result.
     * @param result The result to set
     */
    public void setResult(RubyParserResult result) {
        this.result = result;
    }

    /**
     * Gets the configuration.
     * @return Returns a IRubyParserConfiguration
     */
    public IRubyParserConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration.
     * @param configuration The configuration to set
     */
    public void setConfiguration(IRubyParserConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Sets the errorHandler.
     * @param errorHandler The errorHandler to set
     */
    public void setErrorHandler(IErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
}
