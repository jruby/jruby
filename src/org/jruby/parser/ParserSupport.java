/*
 * ParserSupport.java - description
 * Created on 23.02.2002, 13:41:01
 *
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Copyright (C) 2004 Thomas E Enebo
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Thomas E Enebo <enebo@acm.org>
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

import org.jruby.ast.AndNode;
import org.jruby.ast.ArgsCatNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.CallNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.ClassVarDeclNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.ConstNode;
import org.jruby.ast.DAsgnNode;
import org.jruby.ast.DRegexpNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DVarNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FlipNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LocalAsgnNode;
import org.jruby.ast.LocalVarNode;
import org.jruby.ast.Match2Node;
import org.jruby.ast.Match3Node;
import org.jruby.ast.MatchNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NewlineNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NthRefNode;
import org.jruby.ast.OptNNode;
import org.jruby.ast.OrNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.SValueNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.SplatNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.SuperNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.VCallNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.util.ListNodeUtil;
import org.jruby.ast.util.NodeUtil;
import org.jruby.ast.visitor.UselessStatementVisitor;
import org.jruby.common.IErrors;
import org.jruby.common.IRubyErrorHandler;
import org.jruby.lexer.yacc.SourcePosition;
import org.jruby.util.IdUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Ruby 1.8.1 compatible.
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
    private IRubyErrorHandler errorHandler;

    private RubyParserConfiguration configuration;
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
        } 

        return String.valueOf((char) operatorName);
    }
    
    public Node arg_concat(SourcePosition position, Node node1, Node node2) {
        return node2 == null ? node1 : new ArgsCatNode(position, node1, node2);
    }

    public Node arg_blk_pass(Node firstNode, BlockPassNode secondNode) {
        if (secondNode != null) {
            secondNode.setArgsNode(firstNode);
            return secondNode;
        }
        return firstNode;
    }

    public Node appendPrintToBlock(Node block) {
        return appendToBlock(block, new FCallNode(null, "print", new ArrayNode(block.getPosition()).add(new GlobalVarNode(block.getPosition(), "$_"))));
    }

    public Node appendWhileLoopToBlock(Node block, boolean chop, boolean split) {
        if (split) {
            block = appendToBlock(new GlobalAsgnNode(null, "$F", new CallNode(null, new GlobalVarNode(null, "$_"), "split", null)), block);
        }
        if (chop) {
            block = appendToBlock(new CallNode(null, new GlobalVarNode(null, "$_"), "chop!", null), block);
        }
        return new OptNNode(null, block);
    }
    
    /// TODO: We make self,nil,true,false twice....
    public Node gettable(String id, SourcePosition position) {
        if (id.equals("self")) {
            return new SelfNode(position);
        } else if (id.equals("nil")) {
        	return new NilNode(position);
        } else if (id.equals("true")) {
        	return new TrueNode(position);
        } else if (id.equals("false")) {
        	return new FalseNode(position);
        } /*else if (id == k__FILE__) {
        	return NEW_STR(rb_str_new2(ruby_sourcefile));
            }
            else if (id == k__LINE__) {
        	return NEW_LIT(INT2FIX(ruby_sourceline));
            }*/
        else if (IdUtil.isLocal(id)) {
            if (blockNames.isInBlock() && blockNames.isDefined(id)) {
                return new DVarNode(position, id);
            } else if (getLocalNames().isLocalRegistered(id)) {
                return new LocalVarNode(position, getLocalNames().getLocalIndex(id));
            }
            return new VCallNode(position, id); // Method call without arguments.
        } else if (IdUtil.isGlobal(id)) {
            return new GlobalVarNode(position, id);
        } else if (IdUtil.isInstanceVariable(id)) {
            return new InstVarNode(position, id);
        } else if (IdUtil.isConstant(id)) {
            return new ConstNode(position, id);
        } else if (IdUtil.isClassVariable(id)) {
            return new ClassVarNode(position, id);
        } else {
            errorHandler.handleError(IErrors.COMPILE_ERROR, position, "identifier " + id + " is not valid");
        }
        return null;
    }

    
    public void yyerror(String message) {
        errorHandler.handleError(IErrors.SYNTAX_ERROR, null, message, null);
    }
    
    public Node assignable(SourcePosition position, Object id, Node value) {
        checkExpression(value);
        
        if (id instanceof SelfNode) {
            yyerror("Can't change the value of self"); 
        } else if (id instanceof NilNode) {
            yyerror("Can't assign to nil");
        } else if (id instanceof TrueNode) {
    	    yyerror("Can't assign to true");
        } else if (id instanceof FalseNode) {
    	    yyerror("Can't assign to false");
        } 
        // TODO: Support FILE and LINE by making nodes of them.
        /*else if (id == k__FILE__) {
    	    yyerror("Can't assign to __FILE__");
        } else if (id == k__LINE__) {
            yyerror("Can't assign to __LINE__");
        } */else {
            String name = (String) id;
            if (IdUtil.isLocal(name)) {
                // TODO: Add curried dvar?
                /*if (rb_dvar_curr(id)) {
                    return NEW_DASGN_CURR(id, value);
                } else*/
                if (blockNames.isDefined(name)) {
                    return new DAsgnNode(position, name, value);
                } else if (getLocalNames().isLocalRegistered(name) || !blockNames.isInBlock()) {
                    return new LocalAsgnNode(position, name, getLocalNames().getLocalIndex(name), value);
                } else {
                    blockNames.add(name);
                    // TODO: Should be curried
                    return new DAsgnNode(position, name, value);
                }
            } else if (IdUtil.isGlobal(name)) {
                return new GlobalAsgnNode(position, name, value);
            } else if (IdUtil.isInstanceVariable(name)) {
                return new InstAsgnNode(position, name, value);
            } else if (IdUtil.isConstant(name)) {
                if (isInDef() || isInSingle()) {
                    yyerror("dynamic constant assignment");
                }
                return new ConstDeclNode(position, name, value);
            } else if (IdUtil.isClassVariable(name)) {
                if (isInDef() || isInSingle()) {
                    return new ClassVarAsgnNode(position, name, value);
                }
                return new ClassVarDeclNode(position, name, value);
            } else {
                errorHandler.handleError(IErrors.COMPILE_ERROR, position, "identifier " + name + " is not valid"); 
            }
        }
        
        return null;
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     *@param node
     *@return a NewlineNode or null if node is null.
     */
    public Node newline_node(Node node, SourcePosition position) {
        return node == null ? null : new NewlineNode(position, node); 
    }

    public Node appendToBlock(Node head, Node tail) {
        if (tail == null) {
            return head;
        } else if (head == null) {
            return tail;
        }

        if (!(head instanceof BlockNode)) {
            head = new BlockNode(head.getPosition()).add(head);
        }

        if (errorHandler.isVerbose() && NodeUtil.isBreakStatement(ListNodeUtil.getLast((ListNode) head))) {
            errorHandler.handleError(IErrors.WARNING, tail.getPosition(), "Statement not reached.", null);
        }

        if (tail instanceof BlockNode) {
            ListNodeUtil.addAll((ListNode) head, (ListNode) tail);
        } else {
            ((ListNode) head).add(tail);
        }

        return head;
    }

    public Node getOperatorCallNode(Node firstNode, String operator) {
        checkExpression(firstNode);

        return new CallNode(firstNode.getPosition(), firstNode, operator, null);
    }

    public Node getOperatorCallNode(Node firstNode, String operator, Node secondNode) {
        checkExpression(firstNode);
        checkExpression(secondNode);

        return new CallNode(firstNode.getPosition(), firstNode, operator, new ArrayNode(secondNode.getPosition()).add(secondNode));
    }

    public Node getMatchNode(Node firstNode, Node secondNode) {
        getLocalNames().ensureLocalRegistered("~");

        if (firstNode instanceof DRegexpNode || firstNode instanceof RegexpNode) {
            return new Match2Node(firstNode.getPosition(), firstNode, secondNode);
        } else if (secondNode instanceof DRegexpNode || secondNode instanceof RegexpNode) {
            return new Match3Node(firstNode.getPosition(), secondNode, firstNode);
        } else {
            return getOperatorCallNode(firstNode, "=~", secondNode);
        }
    }

    public Node getElementAssignmentNode(Node recv, Node idx) {
        checkExpression(recv);

        return new CallNode(recv.getPosition(), recv, "[]=", idx);
    }

    public Node getAttributeAssignmentNode(Node recv, String name) {
        checkExpression(recv);

        return new CallNode(recv.getPosition(), recv, name + "=", null);
    }

	/**
	 * @fixme need to handle positions
	 **/
    public void backrefAssignError(Node node) {
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
    public Node node_assign(Node lhs, Node rhs) {
        if (lhs == null) {
            return null;
        }
        Node result = lhs;

        checkExpression(rhs);
        if (lhs instanceof AssignableNode) {
    	    ((AssignableNode) lhs).setValueNode(rhs);
        } else if (lhs instanceof CallNode) {
			CallNode lCallLHS = (CallNode) lhs;
			Node lArgs = lCallLHS.getArgsNode();

			if (lArgs == null) {
				lArgs = new ArrayNode(lhs.getPosition());
				result = new CallNode(lCallLHS.getPosition(), lCallLHS.getReceiverNode(), lCallLHS.getName(), lArgs);
			} else if (!(lArgs instanceof ListNode)) {
				lArgs = new ArrayNode(lhs.getPosition()).add(lArgs);
				result = new CallNode(lCallLHS.getPosition(), lCallLHS.getReceiverNode(), lCallLHS.getName(), lArgs);
			}
            ((ListNode)lArgs).add(rhs);
        }
        
        return result;
    }
    
    public Node ret_args(Node node, SourcePosition position) {
        if (node != null) {
            if (node instanceof BlockPassNode) {
                errorHandler.handleError(IErrors.COMPILE_ERROR, position, "Dynamic constant assignment.");
            } else if (node instanceof ArrayNode &&
                    ((ArrayNode)node).size() == 1) {
                node = (Node) ((ArrayNode)node).iterator().next();
            } else if (node instanceof SplatNode) {
                node = new SValueNode(position, node);
            }
        }
        
        return node;
    }

    public void checkExpression(Node node) {
        if (!NodeUtil.isExpression(node)) {
            errorHandler.handleError(IErrors.SYNTAX_ERROR, node.getPosition(), "Void value expression.", null);
        }
    }

    public void checkUselessStatement(Node node) {
        if (errorHandler.isVerbose()) {
            new UselessStatementVisitor(errorHandler).acceptNode(node);
        }
    }

    public void checkUselessStatements(BlockNode blockNode) {
        if (errorHandler.isVerbose()) {
            Iterator iterator = blockNode.iterator();
            while (iterator.hasNext()) {
                checkUselessStatement((Node) iterator.next());
            }
        }
    }

	/**
	 * @fixme error handling
	 **/
    private boolean checkAssignmentInCondition(Node node) {
        if (node instanceof MultipleAsgnNode) {
            // FIXME
            errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Multiple assignment in conditional.", null);
            return true;
        } else if (node instanceof LocalAsgnNode || node instanceof DAsgnNode || node instanceof GlobalAsgnNode || node instanceof InstAsgnNode) {
            Node valueNode = ((AssignableNode) node).getValueNode();
            if (valueNode instanceof ILiteralNode || valueNode instanceof NilNode || valueNode instanceof TrueNode || valueNode instanceof FalseNode) {
                errorHandler.handleError(IErrors.WARN, null, "Found '=' in conditional, should be '=='.", null);
            }
            return true;
        } 

        return false;
    }

    private Node cond0(Node node) {
        checkAssignmentInCondition(node);

        if (node instanceof DRegexpNode) {
            getLocalNames().ensureLocalRegistered("_");
            getLocalNames().ensureLocalRegistered("~");
            return new Match2Node(node.getPosition(), node, new GlobalVarNode(node.getPosition(), "$_"));
        } else if (node instanceof DotNode) {
            return new FlipNode(
                    node.getPosition(),
                    getFlipConditionNode(((DotNode) node).getBeginNode()),
                    getFlipConditionNode(((DotNode) node).getEndNode()),
                    ((DotNode) node).isExclusive(),
					localNames.registerLocal(null));
        } else if (node instanceof RegexpNode) {
            return new MatchNode(node.getPosition(), node);
        } else if (node instanceof StrNode) {
            getLocalNames().ensureLocalRegistered("_");
            getLocalNames().ensureLocalRegistered("~");
            return new MatchNode(node.getPosition(), new RegexpNode(node.getPosition(), ((StrNode) node).getValue(), 0));
        } 

        return node;
    }

    public Node getConditionNode(Node node) {
        if (node == null) {
            return null;
        } else if (node instanceof NewlineNode) {
            return new NewlineNode(node.getPosition(), cond0(((NewlineNode) node).getNextNode()));
        } 

        return cond0(node);
    }

    private Node getFlipConditionNode(Node node) {
        node = getConditionNode(node);

        if (node instanceof NewlineNode) {
            return ((NewlineNode) node).getNextNode();
        } else if (node instanceof FixnumNode) {
            return getOperatorCallNode(node, "==", new GlobalVarNode(node.getPosition(), "$."));
        } 

        return node;
    }

    public AndNode newAndNode(Node left, Node right) {
        checkExpression(left);
        return new AndNode(left.getPosition(), getConditionNode(left), getConditionNode(right));
    }

    public OrNode newOrNode(Node left, Node right) {
        checkExpression(left);
        return new OrNode(left.getPosition(), getConditionNode(left), getConditionNode(right));
    }

	/**
	 * @fixme position
	 **/
    public Node getReturnArgsNode(Node node) {
        if (node instanceof ArrayNode && ListNodeUtil.getLength((ListNode) node) == 1) {
            return (Node) ((ListNode) node).iterator().next();
        } else if (node instanceof BlockPassNode) {
            // FIXME: position
            errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Block argument should not be given.", null);
        }
        return node;
    }

    public Node new_call(Node receiverNode, String name, Node args) {
    	/*
        Node node = ((BlockPassNode) args).getArgsNode();
        IListNode argsNode = null;
        
        if (node instanceof IListNode) {
            argsNode = (IListNode) node;
        } else if (node != null){
            argsNode = new ArrayNode(node.getPosition()).add(node);
        }
        
        ((BlockPassNode) args).setIterNode(new CallNode(receiverNode.getPosition(), receiverNode, name, argsNode));
        return args;
        */
        if (args != null && args instanceof BlockPassNode) {
            Node argsNode = ((BlockPassNode) args).getArgsNode();
            
            ((BlockPassNode) args).setIterNode(new CallNode(receiverNode.getPosition(), receiverNode, name, argsNode));
            return args;
        }

        return new CallNode(receiverNode.getPosition(), receiverNode, name, args);
    }

    public Node new_fcall(String name, Node args, SourcePosition iPosition) {
        if (args != null && args instanceof BlockPassNode) {
            ((BlockPassNode) args).setIterNode(new FCallNode(args.getPosition(), name, ((BlockPassNode) args).getArgsNode()));
            return args;
        }
        return new FCallNode(iPosition, name, args);
    }

    public Node new_super(Node args, SourcePosition iPosition) {
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
    public RubyParserConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration.
     * @param configuration The configuration to set
     */
    public void setConfiguration(RubyParserConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Sets the errorHandler.
     * @param errorHandler The errorHandler to set
     */
    public void setErrorHandler(IRubyErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }
    
    public Node literal_concat(SourcePosition position, Node head, 
            Object tail) {
        ListNode list;
        
        if (head == null) {
            list = new DStrNode(position);
        } else if (head instanceof EvStrNode) {
            list = new DStrNode(position).add(head);
        } else {
            list = (ListNode) head;
        }
        
        if (tail instanceof String) {
            tail = new StrNode(position, (String)tail);
        }
        list.add((Node)tail);
        
        return list;
    }
    
    public Node newEvStrNode(SourcePosition position, Node node) {
        Node head = node;
        while (true) {
            if (node != null) {
                if (node instanceof StrNode ||
                    node instanceof DStrNode ||
                    node instanceof EvStrNode) {
                    return node;
                }
                
                if (!(node instanceof NewlineNode)) {
                    break;
                }
                
                node = ((NewlineNode) node).getNextNode();
            }
        }
        
        return new EvStrNode(position, head);
    }
    
    public Node new_yield(SourcePosition position, Node node) {
        boolean state = true;
        
        if (node != null) {
            if (node instanceof BlockPassNode) {
                errorHandler.handleError(IErrors.SYNTAX_ERROR, null, "Block argument should not be given.", null);
            }
            
            if (node instanceof ArrayNode && ((ArrayNode)node).size() == 1) {
                node = (Node) ((ArrayNode)node).iterator().next();
                state = false;
            }
            
            if (node != null && node instanceof SplatNode) {
                state = true;
            }
        } else {
            state = false;
        }

        return new YieldNode(position, node, state);
    }
    
    public ListNode list_concat(ListNode first, Node second) {
        if (!(second instanceof ListNode)) {
            return first.add(second);
        }
        ListNode concatee = (ListNode) second;
        
        for (Iterator iterator = concatee.iterator(); iterator.hasNext();) {
            first.add((Node)iterator.next());
        }
        
        return first;
    }
}
