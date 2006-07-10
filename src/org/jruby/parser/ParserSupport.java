/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Mirko Stocker <me@misto.ch>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

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
import org.jruby.ast.visitor.BreakStatementVisitor;
import org.jruby.ast.visitor.ExpressionVisitor;
import org.jruby.ast.visitor.UselessStatementVisitor;
import org.jruby.common.IRubyWarnings;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionFactory;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.Token;
import org.jruby.util.IdUtil;

/** 
 *
 */
public class ParserSupport {
    // Parser states:
    private Stack localNamesStack;
    private BlockNamesStack blockNamesStack;
    private ISourcePositionFactory positionFactory;

    private int inSingle;
    private boolean inDef;
    private boolean inDefined;

    private IRubyWarnings warnings;

    private RubyParserConfiguration configuration;
    private RubyParserResult result;

    public void reset() {
        localNamesStack = new Stack();
        blockNamesStack = new BlockNamesStack(localNamesStack);

        inSingle = 0;
        inDef = false;
        inDefined = false;
    }
    
    public void setPositionFactory(ISourcePositionFactory factory) {
        this.positionFactory = factory;
    }


    public String getOperatorName(int operatorName) {
        if (operatorName >= Tokens.tUPLUS && operatorName <= Tokens.tCOLON2) {
            return Tokens.operators[operatorName - Tokens.tUPLUS];
        } 

        return String.valueOf((char) operatorName);
    }
    
    public Node arg_concat(ISourcePosition position, Node node1, Node node2) {
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
    	ISourcePosition position = block.getPosition();
        return appendToBlock(block, new FCallNode(position, "print", 
            new ArrayNode(position).add(new GlobalVarNode(position, "$_"))));
    }

    public Node appendWhileLoopToBlock(Node block, boolean chop, boolean split) {
    	ISourcePosition position = block.getPosition();
        if (split) {
            block = appendToBlock(new GlobalAsgnNode(position, "$F", 
                new CallNode(position, new GlobalVarNode(position, "$_"), "split", null)), block);
        }
        if (chop) {
            block = appendToBlock(new CallNode(position, new GlobalVarNode(position, "$_"), "chop!", null), block);
        }
        return new OptNNode(position, block);
    }
    
    /// TODO: We make self,nil,true,false twice....
    public Node gettable(String id, ISourcePosition position) {
        if (id.equals("self")) {
            return new SelfNode(position);
        } else if (id.equals("nil")) {
        	return new NilNode(position);
        } else if (id.equals("true")) {
        	return new TrueNode(position);
        } else if (id.equals("false")) {
        	return new FalseNode(position);
        } /* TODO: add __FILE__ and __LINE__ support?
        else if (id == k__FILE__) {
        	return NEW_STR(rb_str_new2(ruby_sourcefile));
            }
            else if (id == k__LINE__) {
        	return NEW_LIT(INT2FIX(ruby_sourceline));
            }*/
        else if (IdUtil.isLocal(id)) {
            BlockNamesElement blockNames = (BlockNamesElement) blockNamesStack.peek();
			LocalNamesElement localNames = (LocalNamesElement) localNamesStack.peek();
			
            if (localNames.isInBlock() && blockNames.isDefined(id)) {
                return new DVarNode(position, id);
            } else if (localNames.isLocalRegistered(id)) {
                return new LocalVarNode(position, localNames.getLocalIndex(id));
            }
            return new VCallNode(position, id); // RubyMethod call without arguments.
        } else if (IdUtil.isGlobal(id)) {
            return new GlobalVarNode(position, id);
        } else if (IdUtil.isInstanceVariable(id)) {
            return new InstVarNode(position, id);
        } else if (IdUtil.isConstant(id)) {
            return new ConstNode(position, id);
        } else if (IdUtil.isClassVariable(id)) {
            return new ClassVarNode(position, id);
        }
        // not reached
        assert false;
        return null;
    }

    
    public Node assignable(ISourcePosition position, Object id, Node value) {
        checkExpression(value);
        
        if (id instanceof SelfNode) {
            throw new SyntaxException(value.getPosition(), "Can't change the value of self");
        } else if (id instanceof NilNode) {
            throw new SyntaxException(value.getPosition(), "Can't assign to nil");
        } else if (id instanceof TrueNode) {
            throw new SyntaxException(value.getPosition(), "Can't assign to true");
        } else if (id instanceof FalseNode) {
            throw new SyntaxException(value.getPosition(), "Can't assign to false");
        }
        // TODO: Support FILE and LINE by making nodes of them.
        /*
         * else if (id == k__FILE__) { yyerror("Can't assign to __FILE__"); } else if (id ==
         * k__LINE__) { yyerror("Can't assign to __LINE__"); }
         */else {
	        String name = null;
	        if (id instanceof Token) {
	            name = (String) ((Token) id).getValue(); 
	        } else if (id instanceof String) {
	            name = (String) id;
	        }
            if (IdUtil.isLocal(name)) {
                // TODO: Add curried dvar?
                /*
                 * if (rb_dvar_curr(id)) { return NEW_DASGN_CURR(id, value); } else
                 */
				BlockNamesElement blockNames = (BlockNamesElement) blockNamesStack.peek();
				LocalNamesElement localNames = (LocalNamesElement) localNamesStack.peek();
                if (blockNames != null && blockNames.isDefined(name)) {
                    return new DAsgnNode(position, name, value);
                } else if (localNames.isLocalRegistered(name) || !localNames.isInBlock()) {
                    return new LocalAsgnNode(position, name, localNames.getLocalIndex(name),
                            value);
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
                    throw new SyntaxException(position, "dynamic constant assignment");
                }
                return new ConstDeclNode(position, null, name, value);
            } else if (IdUtil.isClassVariable(name)) {
                if (isInDef() || isInSingle()) {
                    return new ClassVarAsgnNode(position, name, value);
                }
                return new ClassVarDeclNode(position, name, value);
            }
        }
        // not reached
        assert false;
        return null;
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     *@param node
     *@return a NewlineNode or null if node is null.
     */
    public Node newline_node(Node node, ISourcePosition position) {
        if (node == null) {
            return null;
        }
        
        return node instanceof NewlineNode ? node : new NewlineNode(position, node); 
    }
    
    public ISourcePosition union(Node first, Node second) {
        while (first instanceof NewlineNode) {
            first = ((NewlineNode) first).getNextNode();
        }

        while (second instanceof NewlineNode) {
            second = ((NewlineNode) second).getNextNode();
        }
        
        return positionFactory.getUnion(first.getPosition(), second.getPosition());
    }
    
    public ISourcePosition union(Node first, Token second) {
        while (first instanceof NewlineNode) {
            first = ((NewlineNode) first).getNextNode();
        }

        return positionFactory.getUnion(first.getPosition(), second.getPosition());
    }

    public ISourcePosition union(Token first, Node second) {
        while (second instanceof NewlineNode) {
            second = ((NewlineNode) second).getNextNode();
        }
        
        return positionFactory.getUnion(first.getPosition(), second.getPosition());
    }
    
    public ISourcePosition union(Token first, Token second) {
        return positionFactory.getUnion(first.getPosition(), second.getPosition());
    }

    public Node appendToBlock(Node head, Node tail) {
        if (tail == null) {
            return head;
        } else if (head == null) {
            return tail;
        }
        
        while (head instanceof NewlineNode) {
            head = ((NewlineNode) head).getNextNode();
        }

        if (!(head instanceof BlockNode)) {
            head = new BlockNode(union(head, tail)).add(head);
        }

        if (warnings.isVerbose() && new BreakStatementVisitor().isBreakStatement(((ListNode) head).getLast())) {
            warnings.warning(tail.getPosition(), "Statement not reached.");
        }

        if (tail instanceof BlockNode) {
            ((ListNode) head).addAll((ListNode) tail);
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
        ((LocalNamesElement) localNamesStack.peek()).ensureLocalRegistered("~");

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

    public void backrefAssignError(Node node) {
        if (node instanceof NthRefNode) {
            throw new SyntaxException(node.getPosition(), "Can't set variable $" + ((NthRefNode) node).getMatchNumber() + '.');
        } else if (node instanceof BackRefNode) {
            throw new SyntaxException(node.getPosition(), "Can't set variable $" + ((BackRefNode) node).getType() + '.');
        }
    }

	/**
	 * @fixme position
	 **/
    public Node node_assign(Node lhs, Node rhs) {
        if (lhs == null) {
            return null;
        }
        Node newNode = lhs;

        checkExpression(rhs);
        if (lhs instanceof AssignableNode) {
    	    ((AssignableNode) lhs).setValueNode(rhs);
        } else if (lhs instanceof CallNode) {
			CallNode lCallLHS = (CallNode) lhs;
			Node lArgs = lCallLHS.getArgsNode();

			if (lArgs == null) {
				lArgs = new ArrayNode(lhs.getPosition());
				newNode = new CallNode(lCallLHS.getPosition(), lCallLHS.getReceiverNode(), lCallLHS.getName(), lArgs);
			} else if (!(lArgs instanceof ListNode)) {
				lArgs = new ArrayNode(lhs.getPosition()).add(lArgs);
				newNode = new CallNode(lCallLHS.getPosition(), lCallLHS.getReceiverNode(), lCallLHS.getName(), lArgs);
			}
            ((ListNode)lArgs).add(rhs);
        }
        
        return newNode;
    }
    
    public Node ret_args(Node node, ISourcePosition position) {
        if (node != null) {
            if (node instanceof BlockPassNode) {
                throw new SyntaxException(position, "Dynamic constant assignment.");
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
        if (!new ExpressionVisitor().isExpression(node)) {
            warnings.warning(node.getPosition(), "void value expression");
        }
    }

    public void checkUselessStatement(Node node) {
        if (warnings.isVerbose()) {
            new UselessStatementVisitor(warnings).acceptNode(node);
        }
    }

    /**
     * Check all nodes but the last one in a BlockNode for useless (void context) statements.
     * 
     * @param blockNode to be checked.
     */
    public void checkUselessStatements(BlockNode blockNode) {
        if (warnings.isVerbose()) {
            Node lastNode = blockNode.getLast();

            for (Iterator iterator = blockNode.iterator(); iterator.hasNext(); ) {
                Node currentNode = (Node) iterator.next();
        		
                if (lastNode != currentNode ) {
                    checkUselessStatement(currentNode);
                }
            }
        }
    }

	/**
	 * @fixme error handling
	 **/
    private boolean checkAssignmentInCondition(Node node) {
        if (node instanceof MultipleAsgnNode) {
            throw new SyntaxException(node.getPosition(), "Multiple assignment in conditional.");
        } else if (node instanceof LocalAsgnNode || node instanceof DAsgnNode || node instanceof GlobalAsgnNode || node instanceof InstAsgnNode) {
            Node valueNode = ((AssignableNode) node).getValueNode();
            if (valueNode instanceof ILiteralNode || valueNode instanceof NilNode || valueNode instanceof TrueNode || valueNode instanceof FalseNode) {
                warnings.warn(node.getPosition(), "Found '=' in conditional, should be '=='.");
            }
            return true;
        } 

        return false;
    }

    private Node cond0(Node node) {
        checkAssignmentInCondition(node);

        if (node instanceof DRegexpNode) {
            ISourcePosition position = node.getPosition();
			LocalNamesElement localNames = (LocalNamesElement) localNamesStack.peek();
            localNames.ensureLocalRegistered("_");
            localNames.ensureLocalRegistered("~");
            return new Match2Node(position, node, new GlobalVarNode(position, "$_"));
        } else if (node instanceof DotNode) {
            return new FlipNode(
                    node.getPosition(),
                    getFlipConditionNode(((DotNode) node).getBeginNode()),
                    getFlipConditionNode(((DotNode) node).getEndNode()),
                    ((DotNode) node).isExclusive(),
					((LocalNamesElement) localNamesStack.peek()).registerLocal(null));
        } else if (node instanceof RegexpNode) {
            return new MatchNode(node.getPosition(), node);
        } else if (node instanceof StrNode) {
            ISourcePosition position = node.getPosition();
			LocalNamesElement localNames = (LocalNamesElement) localNamesStack.peek();
            localNames.ensureLocalRegistered("_");
            localNames.ensureLocalRegistered("~");
            return new MatchNode(position, new RegexpNode(position, ((StrNode) node).getValue(), 0));
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
        return new AndNode(left.getPosition(), left, right);
    }

    public OrNode newOrNode(Node left, Node right) {
        checkExpression(left);
        return new OrNode(left.getPosition(), left, right);
    }

    public Node getReturnArgsNode(Node node) {
        if (node instanceof ArrayNode && ((ArrayNode) node).size() == 1) { 
            return (Node) ((ListNode) node).iterator().next();
        } else if (node instanceof BlockPassNode) {
            throw new SyntaxException(node.getPosition(), "Block argument should not be given.");
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

    public Node new_fcall(String name, Node args, Token operation) {
        if (args != null && args instanceof BlockPassNode) {
            ((BlockPassNode) args).setIterNode(new FCallNode(union(operation, args), name, ((BlockPassNode) args).getArgsNode()));
            return args;
        }
        return new FCallNode(operation.getPosition(), name, args);
    }

    public Node new_super(Node args, Token operation) {
        if (args != null && args instanceof BlockPassNode) {
            ((BlockPassNode) args).setIterNode(new SuperNode(union(operation, args), ((BlockPassNode) args).getArgsNode()));
            return args;
        }
        return new SuperNode(operation.getPosition(), args);
    }

    /**
    *  Description of the RubyMethod
    */
    public void initTopLocalVariables() {
        LocalNamesElement localNames = new LocalNamesElement();
        localNamesStack.push(localNames);

        String[] names = configuration.getLocalVariables();
        if (names != null && names.length > 0) {
			List namesList = new ArrayList(names.length);
            for (int i = 0; i < names.length; i++) namesList.add(names[i]);
            localNames.setNames(namesList);
        }
        
        if (configuration.getDynamicVariables() != null) {
            localNames.changeBlockLevel(1);
            getBlockNames().push(new BlockNamesElement(configuration.getDynamicVariables()));
        }
    }

    /**
     *  Description of the RubyMethod
     */
    public void updateTopLocalVariables() {
		LocalNamesElement localNames = (LocalNamesElement) localNamesStack.peek();
        result.setLocalVariables(localNames.getNames().size() > 0 ? localNames.getNames() : null);
        result.setBlockVariables(localNames.isInBlock() ? 
            ((BlockNamesElement) blockNamesStack.peek()).getNames() : null);

        localNamesStack.pop();
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

    /**
     * Gets the blockNames.
     * @return Returns a BlockNamesStack
     */
    public BlockNamesStack getBlockNames() {
        return blockNamesStack;
    }

    /**
     * Gets the localNames.
     * @return Returns a LocalNamesStack
     */
    public Stack getLocalNames() {
        return localNamesStack;
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
     * Sets the configuration.
     * @param configuration The configuration to set
     */
    public void setConfiguration(RubyParserConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;
    }
    
    public Node literal_concat(ISourcePosition position, Node head, Node tail) { 

        if (head == null) {
        	assert tail == null || tail instanceof Node;
        	return (Node) tail;
        }
        
        if (tail == null) {
        	return head;
        }
        
        if (head instanceof EvStrNode) {
            head = new DStrNode(position).add(head);
        } 

        if (tail instanceof StrNode) {
        	if (head instanceof StrNode) {
        	    head = new StrNode(union(head, (Node) tail), 
                       ((StrNode) head).getValue() + ((StrNode) tail).getValue());
        	} else {
        		((ListNode) head).add((Node) tail);
        	}
        } else if (tail instanceof DStrNode) {
            if(head instanceof StrNode){
                ((DStrNode)tail).childNodes().add(0, head);
                return tail;
            } else {
            	return list_concat((ListNode) head, tail);
            }
        } else if (tail instanceof EvStrNode) {
        	if (head instanceof StrNode) {
        		head = new DStrNode(head.getPosition()).add(head);
        		
        	}
        	((DStrNode) head).add(tail);
        }
        
        return head;
    }
    
    public Node newEvStrNode(ISourcePosition position, Node node) {
        Node head = node;
        while (true) {
            if (node == null) {
                break;
            }
            
            if (node instanceof StrNode|| node instanceof DStrNode || node instanceof EvStrNode) {
                return node;
            }
                
            if (!(node instanceof NewlineNode)) {
                break;
            }
                
            node = ((NewlineNode) node).getNextNode();
        }
        
        return new EvStrNode(position, head);
    }
    
    public Node new_yield(ISourcePosition position, Node node) {
        boolean state = true;
        
        if (node != null) {
            if (node instanceof BlockPassNode) {
                throw new SyntaxException(node.getPosition(), "Block argument should not be given.");
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
