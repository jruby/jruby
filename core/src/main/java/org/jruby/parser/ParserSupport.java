/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
 * Copyright (C) 2006-2007 Mirko Stocker <me@misto.ch>
 * Copyright (C) 2006 Thomas Corbat <tcorbat@hsr.ch>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.parser;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.jcodings.Encoding;
import org.jruby.RubyBignum;
import org.jruby.RubyRegexp;
import org.jruby.ast.*;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.exceptions.RaiseException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.SyntaxException;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.runtime.DynamicScope;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

/** 
 *
 */
public class ParserSupport {
    // Parser states:
    protected StaticScope currentScope;

    protected RubyLexer lexer;
    
    // Is the parser current within a singleton (value is number of nested singletons)
    private int inSingleton;
    
    // Is the parser currently within a method definition
    private boolean inDefinition;

    protected IRubyWarnings warnings;

    protected ParserConfiguration configuration;
    private RubyParserResult result;

    public void reset() {
        inSingleton = 0;
        inDefinition = false;
    }
    
    public StaticScope getCurrentScope() {
        return currentScope;
    }
    
    public ParserConfiguration getConfiguration() {
        return configuration;
    }
    
    public void popCurrentScope() {
        currentScope = currentScope.getEnclosingScope();
    }
    
    public void pushBlockScope() {
        currentScope = configuration.getRuntime().getStaticScopeFactory().newBlockScope(currentScope);
    }
    
    public void pushLocalScope() {
        currentScope = configuration.getRuntime().getStaticScopeFactory().newLocalScope(currentScope);
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

    /**
     * We know for callers of this that it cannot be any of the specials checked in gettable.
     * 
     * @param node to check its variable type
     * @return an AST node representing this new variable
     */
    public Node gettable2(Node node) {
        switch (node.getNodeType()) {
        case DASGNNODE: // LOCALVAR
        case LOCALASGNNODE:
            return currentScope.declare(node.getPosition(), ((INameNode) node).getName());
        case CONSTDECLNODE: // CONSTANT
            return new ConstNode(node.getPosition(), ((INameNode) node).getName());
        case INSTASGNNODE: // INSTANCE VARIABLE
            return new InstVarNode(node.getPosition(), ((INameNode) node).getName());
        case CLASSVARDECLNODE:
        case CLASSVARASGNNODE:
            return new ClassVarNode(node.getPosition(), ((INameNode) node).getName());
        case GLOBALASGNNODE:
            return new GlobalVarNode(node.getPosition(), ((INameNode) node).getName());
        }

        getterIdentifierError(node.getPosition(), ((INameNode) node).getName());
        return null;
    }

    public Node declareIdentifier(String name) {
        return currentScope.declare(lexer.tokline, name);
    }

    // We know it has to be tLABEL or tIDENTIFIER so none of the other assignable logic is needed
    public AssignableNode assignableLabelOrIdentifier(String name, Node value) {
        return currentScope.assign(lexer.getPosition(), name, makeNullNil(value));
    }

    // Only calls via f_kw so we know it has to be tLABEL
    public AssignableNode assignableLabel(String name, Node value) {
        return currentScope.assign(lexer.getPosition(), name, makeNullNil(value));
    }
    
    protected void getterIdentifierError(ISourcePosition position, String identifier) {
        throw new SyntaxException(PID.BAD_IDENTIFIER, position, "identifier " +
                identifier + " is not valid to get", identifier);
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     *@param node
     *@return a NewlineNode or null if node is null.
     */
    public Node newline_node(Node node, ISourcePosition position) {
        if (node == null) return null;

        configuration.coverLine(position.getLine());
        
        return node instanceof NewlineNode ? node : new NewlineNode(position, node); 
    }
    
    public Node addRootNode(Node topOfAST) {
        if (result.getBeginNodes().isEmpty()) {
            ISourcePosition position;
            if (topOfAST == null) {
                topOfAST = NilImplicitNode.NIL;
                position = lexer.getPosition();
            } else {
                position = topOfAST.getPosition();
            }
            
            return new RootNode(position, result.getScope(), topOfAST);
        }

        ISourcePosition position = topOfAST != null ? topOfAST.getPosition() : result.getBeginNodes().get(0).getPosition();
        BlockNode newTopOfAST = new BlockNode(position);
        for (Node beginNode: result.getBeginNodes()) {
            appendToBlock(newTopOfAST, beginNode);
        }
        
        // Add real top to new top (unless this top is empty [only begin/end nodes or truly empty])
        if (topOfAST != null) newTopOfAST.add(topOfAST);
        
        return new RootNode(position, result.getScope(), newTopOfAST);
    }
    
    /* MRI: block_append */
    public Node appendToBlock(Node head, Node tail) {
        if (tail == null) return head;
        if (head == null) return tail;

        // Reduces overhead in interp by not set position every single line we encounter.
        head = compactNewlines(head);

        if (!(head instanceof BlockNode)) {
            head = new BlockNode(head.getPosition()).add(head);
        }

        if (warnings.isVerbose() && isBreakStatement(((ListNode) head).getLast()) && Options.PARSER_WARN_NOT_REACHED.load()) {
            warnings.warning(ID.STATEMENT_NOT_REACHED, tail.getPosition(), "Statement not reached.");
        }

        // Assumption: tail is never a list node
        ((ListNode) head).addAll(tail);
        return head;
    }

    public Node getOperatorCallNode(Node firstNode, String operator) {
        checkExpression(firstNode);

        return new CallNode(firstNode.getPosition(), firstNode, operator, null, null);
    }
    
    public Node getOperatorCallNode(Node firstNode, String operator, Node secondNode) {
        return getOperatorCallNode(firstNode, operator, secondNode, null);
    }

    public Node getOperatorCallNode(Node firstNode, String operator, Node secondNode, ISourcePosition defaultPosition) {
        if (defaultPosition != null) {
            firstNode = checkForNilNode(firstNode, defaultPosition);
            secondNode = checkForNilNode(secondNode, defaultPosition);
        }
        
        checkExpression(firstNode);
        checkExpression(secondNode);

        return new CallNode(firstNode.getPosition(), firstNode, operator, new ArrayNode(secondNode.getPosition(), secondNode), null);
    }

    public Node getMatchNode(Node firstNode, Node secondNode) {
        if (firstNode instanceof DRegexpNode) {
            return new Match2Node(firstNode.getPosition(), firstNode, secondNode);
        } else if (firstNode instanceof RegexpNode) {
            List<Integer> locals = allocateNamedLocals((RegexpNode) firstNode);

            if (locals.size() > 0) {
                int[] primitiveLocals = new int[locals.size()];
                for (int i = 0; i < primitiveLocals.length; i++) {
                    primitiveLocals[i] = locals.get(i);
                }
                return new Match2CaptureNode(firstNode.getPosition(), firstNode, secondNode, primitiveLocals);
            } else {
                return new Match2Node(firstNode.getPosition(), firstNode, secondNode);
            }
        } else if (secondNode instanceof DRegexpNode || secondNode instanceof RegexpNode) {
            return new Match3Node(firstNode.getPosition(), secondNode, firstNode);
        }

        return getOperatorCallNode(firstNode, "=~", secondNode);
    }

    /**
     * Define an array set condition so we can return lhs
     * 
     * @param receiver array being set
     * @param index node which should evalute to index of array set
     * @return an AttrAssignNode
     */
    public Node aryset(Node receiver, Node index) {
        checkExpression(receiver);

        return new_attrassign(receiver.getPosition(), receiver, "[]=", index);
    }

    /**
     * Define an attribute set condition so we can return lhs
     * 
     * @param receiver object which contains attribute
     * @param name of the attribute being set
     * @return an AttrAssignNode
     */
    public Node attrset(Node receiver, String name) {
        checkExpression(receiver);

        return new_attrassign(receiver.getPosition(), receiver, name + "=", null);
    }

    public void backrefAssignError(Node node) {
        if (node instanceof NthRefNode) {
            String varName = "$" + ((NthRefNode) node).getMatchNumber();
            throw new SyntaxException(PID.INVALID_ASSIGNMENT, node.getPosition(), 
                    "Can't set variable " + varName + '.', varName);
        } else if (node instanceof BackRefNode) {
            String varName = "$" + ((BackRefNode) node).getType();
            throw new SyntaxException(PID.INVALID_ASSIGNMENT, node.getPosition(), "Can't set variable " + varName + '.', varName);
        }
    }

    public Node arg_add(ISourcePosition position, Node node1, Node node2) {
        if (node1 == null) {
            if (node2 == null) {
                return new ArrayNode(position, NilImplicitNode.NIL);
            } else {
                return new ArrayNode(node2.getPosition(), node2);
            }
        }
        if (node1 instanceof ArrayNode) return ((ArrayNode) node1).add(node2);
        
        return new ArgsPushNode(position, node1, node2);
    }
    
	/**
	 * @fixme position
	 **/
    public Node node_assign(Node lhs, Node rhs) {
        if (lhs == null) return null;

        Node newNode = lhs;

        checkExpression(rhs);
        if (lhs instanceof AssignableNode) {
    	    ((AssignableNode) lhs).setValueNode(rhs);
        } else if (lhs instanceof IArgumentNode) {
            IArgumentNode invokableNode = (IArgumentNode) lhs;
            
            return invokableNode.setArgsNode(arg_add(lhs.getPosition(), invokableNode.getArgsNode(), rhs));
        }
        
        return newNode;
    }
    
    public Node ret_args(Node node, ISourcePosition position) {
        if (node != null) {
            if (node instanceof BlockPassNode) {
                throw new SyntaxException(PID.BLOCK_ARG_UNEXPECTED, position,
                        lexer.getCurrentLine(), "block argument should not be given");
            } else if (node instanceof ArrayNode && ((ArrayNode)node).size() == 1) {
                node = ((ArrayNode)node).get(0);
            } else if (node instanceof SplatNode) {
                node = newSValueNode(position, node);
            }
        }

        if (node == null) node = NilImplicitNode.NIL;
        
        return node;
    }

    /**
     * Is the supplied node a break/control statement?
     * 
     * @param node to be checked
     * @return true if a control node, false otherwise
     */
    public boolean isBreakStatement(Node node) {
        breakLoop: do {
            if (node == null) return false;

            switch (node.getNodeType()) {
            case NEWLINENODE:
                node = ((NewlineNode) node).getNextNode();
                continue breakLoop;
            case BREAKNODE: case NEXTNODE: case REDONODE:
            case RETRYNODE: case RETURNNODE:
                return true;
            default:
                return false;
            }
        } while (true);                    
    }
    
    public void warnUnlessEOption(ID id, Node node, String message) {
        if (!configuration.isInlineSource()) {
            warnings.warn(id, node.getPosition(), message);
        }
    }

    public void warningUnlessEOption(ID id, Node node, String message) {
        if (warnings.isVerbose() && !configuration.isInlineSource()) {
            warnings.warning(id, node.getPosition(), message);
        }
    }

    private Node compactNewlines(Node head) {
        while (head instanceof NewlineNode) {
            Node nextNode = ((NewlineNode) head).getNextNode();

            if (!(nextNode instanceof NewlineNode)) {
                break;
            }
            head = nextNode;
        }
        return head;
    }

    // logical equivalent to value_expr in MRI
    public boolean checkExpression(Node node) {
        boolean conditional = false;

        while (node != null) {
            switch (node.getNodeType()) {
            case RETURNNODE: case BREAKNODE: case NEXTNODE: case REDONODE:
            case RETRYNODE:
                if (!conditional) {
                    throw new SyntaxException(PID.VOID_VALUE_EXPRESSION,
                            node.getPosition(), lexer.getCurrentLine(),
                            "void value expression");
                }
                return false;
            case BLOCKNODE:
                node = ((BlockNode) node).getLast();
                break;
            case BEGINNODE:
                node = ((BeginNode) node).getBodyNode();
                break;
            case IFNODE:
                if (!checkExpression(((IfNode) node).getThenBody())) return false;
                node = ((IfNode) node).getElseBody();
                break;
            case ANDNODE: case ORNODE:
                conditional = true;
                node = ((BinaryOperatorNode) node).getSecondNode();
                break;
            case NEWLINENODE:
                node = ((NewlineNode) node).getNextNode();
                break;
            default: // Node
                return true;
            }
        }

        return true;
    }
    
    /**
     * Is this a literal in the sense that MRI has a NODE_LIT for.  This is different than
     * ILiteralNode.  We should pick a different name since ILiteralNode is something we created
     * which is similiar but used for a slightly different condition (can I do singleton things).
     * 
     * @param node to be tested
     * @return true if it is a literal
     */
    public boolean isLiteral(Node node) {
        return node != null && (node instanceof FixnumNode || node instanceof BignumNode || 
                node instanceof FloatNode || node instanceof SymbolNode || 
                (node instanceof RegexpNode && ((RegexpNode) node).getOptions().toJoniOptions() == 0));
    }

    private void handleUselessWarn(Node node, String useless) {
        if (Options.PARSER_WARN_USELESSS_USE_OF.load()) {
            warnings.warn(ID.USELESS_EXPRESSION, node.getPosition(), "Useless use of " + useless + " in void context.");
        }
    }

    /**
     * Check to see if current node is an useless statement.  If useless a warning if printed.
     * 
     * @param node to be checked.
     */
    public void checkUselessStatement(Node node) {
        if (!warnings.isVerbose() || (!configuration.isInlineSource() && configuration.isEvalParse())) return;
        
        uselessLoop: do {
            if (node == null) return;
            
            switch (node.getNodeType()) {
            case NEWLINENODE:
                node = ((NewlineNode) node).getNextNode();
                continue uselessLoop;
            case CALLNODE: {
                String name = ((CallNode) node).getName();
                
                if (name == "+" || name == "-" || name == "*" || name == "/" || name == "%" || 
                    name == "**" || name == "+@" || name == "-@" || name == "|" || name == "^" || 
                    name == "&" || name == "<=>" || name == ">" || name == ">=" || name == "<" || 
                    name == "<=" || name == "==" || name == "!=") {
                    handleUselessWarn(node, name);
                }
                return;
            }
            case BACKREFNODE: case DVARNODE: case GLOBALVARNODE:
            case LOCALVARNODE: case NTHREFNODE: case CLASSVARNODE:
            case INSTVARNODE:
                handleUselessWarn(node, "a variable"); return;
            // FIXME: Temporarily disabling because this fires way too much running Rails tests. JRUBY-518
            /*case CONSTNODE:
                handleUselessWarn(node, "a constant"); return;*/
            case BIGNUMNODE: case DREGEXPNODE: case DSTRNODE: case DSYMBOLNODE:
            case FIXNUMNODE: case FLOATNODE: case REGEXPNODE:
            case STRNODE: case SYMBOLNODE:
                handleUselessWarn(node, "a literal"); return;
            // FIXME: Temporarily disabling because this fires way too much running Rails tests. JRUBY-518
            /*case CLASSNODE: case COLON2NODE:
                handleUselessWarn(node, "::"); return;*/
            case DOTNODE:
                handleUselessWarn(node, ((DotNode) node).isExclusive() ? "..." : ".."); return;
            case DEFINEDNODE:
                handleUselessWarn(node, "defined?"); return;
            case FALSENODE:
                handleUselessWarn(node, "false"); return;
            case NILNODE: 
                handleUselessWarn(node, "nil"); return;
            // FIXME: Temporarily disabling because this fires way too much running Rails tests. JRUBY-518
            /*case SELFNODE:
                handleUselessWarn(node, "self"); return;*/
            case TRUENODE:
                handleUselessWarn(node, "true"); return;
            default: return;
            }
        } while (true);
    }

    /**
     * Check all nodes but the last one in a BlockNode for useless (void context) statements.
     * 
     * @param blockNode to be checked.
     */
    public void checkUselessStatements(BlockNode blockNode) {
        if (warnings.isVerbose()) {
            Node lastNode = blockNode.getLast();

            for (int i = 0; i < blockNode.size(); i++) {
                Node currentNode = blockNode.get(i);
        		
                if (lastNode != currentNode ) {
                    checkUselessStatement(currentNode);
                }
            }
        }
    }

	/**
     * assign_in_cond
	 **/
    private boolean checkAssignmentInCondition(Node node) {
        if (node instanceof MultipleAsgnNode) {
            throw new SyntaxException(PID.MULTIPLE_ASSIGNMENT_IN_CONDITIONAL, node.getPosition(),
                    lexer.getCurrentLine(), "Multiple assignment in conditional.");
        } else if (node instanceof LocalAsgnNode || node instanceof DAsgnNode || node instanceof GlobalAsgnNode || node instanceof InstAsgnNode) {
            Node valueNode = ((AssignableNode) node).getValueNode();
            if (valueNode instanceof ILiteralNode || valueNode instanceof NilNode || valueNode instanceof TrueNode || valueNode instanceof FalseNode) {
                warnings.warn(ID.ASSIGNMENT_IN_CONDITIONAL, node.getPosition(), "Found '=' in conditional, should be '=='.");
            }
            return true;
        } 

        return false;
    }
    
    protected Node makeNullNil(Node node) {
        return node == null ? NilImplicitNode.NIL : node;
    }

    private Node cond0(Node node) {
        checkAssignmentInCondition(node);
        
        Node leftNode;
        Node rightNode;

        // FIXME: DSTR,EVSTR,STR: warning "string literal in condition"
        switch(node.getNodeType()) {
        case DREGEXPNODE: {
            ISourcePosition position = node.getPosition();

            return new Match2Node(position, node, new GlobalVarNode(position, "$_"));
        }
        case ANDNODE:
            leftNode = cond0(((AndNode) node).getFirstNode());
            rightNode = cond0(((AndNode) node).getSecondNode());
            
            return new AndNode(node.getPosition(), makeNullNil(leftNode), makeNullNil(rightNode));
        case ORNODE:
            leftNode = cond0(((OrNode) node).getFirstNode());
            rightNode = cond0(((OrNode) node).getSecondNode());
            
            return new OrNode(node.getPosition(), makeNullNil(leftNode), makeNullNil(rightNode));
        case DOTNODE: {
            DotNode dotNode = (DotNode) node;
            if (dotNode.isLiteral()) return node; 
            
            String label = String.valueOf("FLIP" + node.hashCode());
            currentScope.getLocalScope().addVariable(label);
            int slot = currentScope.isDefined(label);
            
            return new FlipNode(node.getPosition(),
                    getFlipConditionNode(((DotNode) node).getBeginNode()),
                    getFlipConditionNode(((DotNode) node).getEndNode()),
                    dotNode.isExclusive(), slot);
        }
        case REGEXPNODE:
            if (Options.PARSER_WARN_REGEX_CONDITION.load()) {
                warningUnlessEOption(ID.REGEXP_LITERAL_IN_CONDITION, node, "regex literal in condition");
            }
            
            return new MatchNode(node.getPosition(), node);
        }

        return node;
    }

    public Node getConditionNode(Node node) {
        if (node == null) return NilImplicitNode.NIL;

        if (node instanceof NewlineNode) {
            return new NewlineNode(node.getPosition(), cond0(((NewlineNode) node).getNextNode()));
        } 

        return cond0(node);
    }

    /* MRI: range_op */
    private Node getFlipConditionNode(Node node) {
        if (!configuration.isInlineSource()) return node;
        
        node = getConditionNode(node);

        if (node instanceof NewlineNode) return ((NewlineNode) node).getNextNode();
        
        if (node instanceof FixnumNode) {
            warnUnlessEOption(ID.LITERAL_IN_CONDITIONAL_RANGE, node, "integer literal in conditional range");
            return getOperatorCallNode(node, "==", new GlobalVarNode(node.getPosition(), "$."));
        } 

        return node;
    }

    public SValueNode newSValueNode(ISourcePosition position, Node node) {
        return new SValueNode(position, node);
    }
    
    public SplatNode newSplatNode(ISourcePosition position, Node node) {
        return new SplatNode(position, makeNullNil(node));
    }
    
    public ArrayNode newArrayNode(ISourcePosition position, Node firstNode) {
        return new ArrayNode(position, makeNullNil(firstNode));
    }

    public ISourcePosition position(ISourcePositionHolder one, ISourcePositionHolder two) {
        return one == null ? two.getPosition() : one.getPosition();
    }

    public AndNode newAndNode(ISourcePosition position, Node left, Node right) {
        checkExpression(left);
        
        if (left == null && right == null) return new AndNode(position, makeNullNil(left), makeNullNil(right));
        
        return new AndNode(position(left, right), makeNullNil(left), makeNullNil(right));
    }

    public OrNode newOrNode(ISourcePosition position, Node left, Node right) {
        checkExpression(left);

        if (left == null && right == null) return new OrNode(position, makeNullNil(left), makeNullNil(right));
        
        return new OrNode(position(left, right), makeNullNil(left), makeNullNil(right));
    }

    /**
     * Ok I admit that this is somewhat ugly.  We post-process a chain of when nodes and analyze
     * them to re-insert them back into our new CaseNode the way we want.  The grammar is being
     * difficult and until I go back into the depths of that this is where things are.
     *
     * @param expression of the case node (e.g. case foo)
     * @param firstWhenNode first when (which could also be the else)
     * @return a new case node
     */
    public CaseNode newCaseNode(ISourcePosition position, Node expression, Node firstWhenNode) {
        ArrayNode cases = new ArrayNode(firstWhenNode != null ? firstWhenNode.getPosition() : position);
        CaseNode caseNode = new CaseNode(position, expression, cases);

        for (Node current = firstWhenNode; current != null; current = ((WhenNode) current).getNextCase()) {
            if (current instanceof WhenOneArgNode) {
                cases.add(current);
            } else if (current instanceof WhenNode) {
                simplifyMultipleArgumentWhenNodes((WhenNode) current, cases);
            } else {
                caseNode.setElseNode(current);
                break;
            }
        }

        return caseNode;
    }

    /*
     * This method exists for us to break up multiple expression when nodes (e.g. when 1,2,3:)
     * into individual whenNodes.  The primary reason for this is to ensure lazy evaluation of
     * the arguments (when foo,bar,gar:) to prevent side-effects.  In the old code this was done
     * using nested when statements, which was awful for interpreter and compilation.
     *
     * Notes: This has semantic equivalence but will not be lexically equivalent.  Compiler
     * needs to detect same bodies to simplify bytecode generated.
     */
    private void simplifyMultipleArgumentWhenNodes(WhenNode sourceWhen, ArrayNode cases) {
        Node expressionNodes = sourceWhen.getExpressionNodes();

        if (expressionNodes instanceof SplatNode || expressionNodes instanceof ArgsCatNode) {
            cases.add(sourceWhen);
            return;
        }

        if (expressionNodes instanceof ListNode) {
            ListNode list = (ListNode) expressionNodes;
            ISourcePosition position = sourceWhen.getPosition();
            Node bodyNode = sourceWhen.getBodyNode();

            for (int i = 0; i < list.size(); i++) {
                Node expression = list.get(i);

                if (expression instanceof SplatNode || expression instanceof ArgsCatNode) {
                    cases.add(new WhenNode(position, expression, bodyNode, null));
                } else {
                    cases.add(new WhenOneArgNode(position, expression, bodyNode, null));
                }
            }
        } else {
            cases.add(sourceWhen);
        }
    }
    
    public WhenNode newWhenNode(ISourcePosition position, Node expressionNodes, Node bodyNode, Node nextCase) {
        if (bodyNode == null) bodyNode = NilImplicitNode.NIL;

        if (expressionNodes instanceof SplatNode || expressionNodes instanceof ArgsCatNode || expressionNodes instanceof ArgsPushNode) {
            return new WhenNode(position, expressionNodes, bodyNode, nextCase);
        }

        ListNode list = (ListNode) expressionNodes;

        if (list.size() == 1) {
            Node element = list.get(0);
            
            if (!(element instanceof SplatNode)) {
                return new WhenOneArgNode(position, element, bodyNode, nextCase);
            }
        }

        return new WhenNode(position, expressionNodes, bodyNode, nextCase);
    }

    // FIXME: Currently this is passing in position of receiver
    public Node new_opElementAsgnNode(Node receiverNode, String operatorName, Node argsNode, Node valueNode) {
        ISourcePosition position = lexer.tokline;  // FIXME: ruby_sourceline in new lexer.
        Node newNode = null;

        if (argsNode instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) argsNode;

            if (array.size() == 1) {
                if (operatorName.equals("||")) {
                    newNode = new OpElementOneArgOrAsgnNode(position, receiverNode, operatorName, array, valueNode);
                } else if (operatorName.equals("&&")) {

                    newNode = new OpElementOneArgAndAsgnNode(position, receiverNode, operatorName, array, valueNode);
                } else {
                    newNode = new OpElementOneArgAsgnNode(position, receiverNode, operatorName, array, valueNode);
                }
            }
        }

        if (newNode == null) {
            newNode = new OpElementAsgnNode(position, receiverNode, operatorName, argsNode, valueNode);
        }

        fixpos(newNode, receiverNode);

        return newNode;
    }
    
    public Node new_attrassign(ISourcePosition position, Node receiver, String name, Node args) {
        return new AttrAssignNode(position, receiver, name, args);
    }
    
    private boolean isNumericOperator(String name) {
        if (name.length() == 1) {
            switch (name.charAt(0)) {
                case '+': case '-': case '*': case '/': case '<': case '>':
                    return true;
            }
        } else if (name.length() == 2) {
            switch (name.charAt(0)) {
            case '<': case '>': case '=':
                switch (name.charAt(1)) {
                case '=': case '<':
                    return true;
                }
            }
        }
        
        return false;
    }

    public Node new_call(Node receiver, String name, Node argsNode, Node iter) {
        if (argsNode instanceof BlockPassNode) {
            if (iter != null) {
                throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, iter.getPosition(),
                        lexer.getCurrentLine(), "Both block arg and actual block given.");
            }

            BlockPassNode blockPass = (BlockPassNode) argsNode;
            return new CallNode(position(receiver, argsNode), receiver, name, blockPass.getArgsNode(), blockPass);
        }

        return new CallNode(position(receiver, argsNode), receiver, name, argsNode, iter);
    }

    public Colon2Node new_colon2(ISourcePosition position, Node leftNode, String name) {
        if (leftNode == null) return new Colon2ImplicitNode(position, name);

        return new Colon2ConstNode(position, leftNode, name);
    }

    public Colon3Node new_colon3(ISourcePosition position, String name) {
        return new Colon3Node(position, name);
    }

    public void frobnicate_fcall_args(FCallNode fcall, Node args, Node iter) {
        if (args instanceof BlockPassNode) {
            if (iter != null) {
                throw new SyntaxException(PID.BLOCK_ARG_AND_BLOCK_GIVEN, iter.getPosition(),
                        lexer.getCurrentLine(), "Both block arg and actual block given.");
            }

            BlockPassNode blockPass = (BlockPassNode) args;
            args = blockPass.getArgsNode();
            iter = blockPass;
        }

        fcall.setArgsNode(args);
        fcall.setIterNode(iter);
    }

    public void fixpos(Node node, Node orig) {
        if (node == null || orig == null) return;

        node.setPosition(orig.getPosition());
    }

    public Node new_fcall(String operation) {
        return new FCallNode(lexer.tokline, operation);
    }

    public Node new_super(ISourcePosition position, Node args) {
        if (args != null && args instanceof BlockPassNode) {
            return new SuperNode(position, ((BlockPassNode) args).getArgsNode(), args);
        }
        return new SuperNode(position, args);
    }

    /**
    *  Description of the RubyMethod
    */
    public void initTopLocalVariables() {
        DynamicScope scope = configuration.getScope(); 
        currentScope = scope.getStaticScope(); 
        
        result.setScope(scope);
    }

    /** Getter for property inSingle.
     * @return Value of property inSingle.
     */
    public boolean isInSingle() {
        return inSingleton != 0;
    }

    /** Setter for property inSingle.
     * @param inSingle New value of property inSingle.
     */
    public void setInSingle(int inSingle) {
        this.inSingleton = inSingle;
    }

    public boolean isInDef() {
        return inDefinition;
    }

    public void setInDef(boolean inDef) {
        this.inDefinition = inDef;
    }

    /** Getter for property inSingle.
     * @return Value of property inSingle.
     */
    public int getInSingle() {
        return inSingleton;
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
    public void setConfiguration(ParserConfiguration configuration) {
        this.configuration = configuration;
    }

    public void setWarnings(IRubyWarnings warnings) {
        this.warnings = warnings;
    }

    public void setLexer(RubyLexer lexer) {
        this.lexer = lexer;
    }

    public DStrNode createDStrNode(ISourcePosition position) {
        return new DStrNode(position, lexer.getEncoding());
    }
        
    public Node asSymbol(ISourcePosition position, Node value) {
        return value instanceof StrNode ? new SymbolNode(position, ((StrNode) value).getValue()) :
                new DSymbolNode(position, (DStrNode) value);
    }
    
    public Node literal_concat(ISourcePosition position, Node head, Node tail) { 
        if (head == null) return tail;
        if (tail == null) return head;
        
        if (head instanceof EvStrNode) {
            head = createDStrNode(head.getPosition()).add(head);
        } 

        if (tail instanceof StrNode) {
            if (head instanceof StrNode) {
                StrNode front = (StrNode) head;
                // string_contents always makes an empty strnode...which is sometimes valid but
                // never if it ever is in literal_concat.
                if (front.getValue().getRealSize() > 0) {
                    return new StrNode(head.getPosition(), front, (StrNode) tail);
                } else {
                    return tail;
                }
            } 
            head.setPosition(head.getPosition());
            return ((ListNode) head).add(tail);
        	
        } else if (tail instanceof DStrNode) {
            if (head instanceof StrNode){
                ((DStrNode)tail).prepend(head);
                return tail;
            } 

            return ((ListNode) head).addAll(tail);
        } 

        // tail must be EvStrNode at this point 
        if (head instanceof StrNode) {
        	
            //Do not add an empty string node
            if(((StrNode) head).getValue().length() == 0) {
                head = createDStrNode(head.getPosition());
            } else {
                head = createDStrNode(head.getPosition()).add(head);
            }
        }
        return ((DStrNode) head).add(tail);
    }
    
    public Node newEvStrNode(ISourcePosition position, Node node) {
        Node head = node;
        while (true) {
            if (node == null) break;
            
            if (node instanceof StrNode || node instanceof DStrNode || node instanceof EvStrNode) {
                return node;
            }
                
            if (!(node instanceof NewlineNode)) break;
                
            node = ((NewlineNode) node).getNextNode();
        }
        
        return new EvStrNode(position, head);
    }
    
    public Node new_yield(ISourcePosition position, Node node) {
        if (node != null && node instanceof BlockPassNode) {
            throw new SyntaxException(PID.BLOCK_ARG_UNEXPECTED, node.getPosition(),
                    lexer.getCurrentLine(), "Block argument should not be given.");
        }

        return new Yield19Node(position, node); 
    }
    
    public NumericNode negateInteger(NumericNode integerNode) {
        if (integerNode instanceof FixnumNode) {
            FixnumNode fixnumNode = (FixnumNode) integerNode;
            
            fixnumNode.setValue(-fixnumNode.getValue());
            return fixnumNode;
        } else if (integerNode instanceof BignumNode) {
            BignumNode bignumNode = (BignumNode) integerNode;

            BigInteger value = bignumNode.getValue().negate();

            // Negating a bignum will make the last negative value of our bignum
            if (value.compareTo(RubyBignum.LONG_MIN) >= 0) {
                return new FixnumNode(bignumNode.getPosition(), value.longValue());
            }
            
            bignumNode.setValue(value);
        }
        
        return integerNode;
    }
    
    public FloatNode negateFloat(FloatNode floatNode) {
        floatNode.setValue(-floatNode.getValue());
        
        return floatNode;
    }

    public ComplexNode negateComplexNode(ComplexNode complexNode) {
        complexNode.setNumber(negateNumeric(complexNode.getNumber()));

        return complexNode;
    }

    public RationalNode negateRational(RationalNode rationalNode) {
        return new RationalNode(rationalNode.getPosition(),
                                -rationalNode.getNumerator(),
                                rationalNode.getDenominator());
    }
    
    public Node unwrapNewlineNode(Node node) {
    	if(node instanceof NewlineNode) {
    		return ((NewlineNode) node).getNextNode();
    	}
    	return node;
    }
    
    private Node checkForNilNode(Node node, ISourcePosition defaultPosition) {
        return (node == null) ? new NilNode(defaultPosition) : node; 
    }

    public Node new_args(ISourcePosition position, ListNode pre, ListNode optional, RestArgNode rest,
            ListNode post, ArgsTailHolder tail) {
        if (tail == null) return new ArgsNode(position, pre, optional, rest, post, (BlockArgNode) null);

        return new ArgsNode(position, pre, optional, rest, post, 
                tail.getKeywordArgs(), tail.getKeywordRestArgNode(), tail.getBlockArg());
    }
    
    public ArgsTailHolder new_args_tail(ISourcePosition position, ListNode keywordArg, 
            String keywordRestArgName, BlockArgNode blockArg) {
        if (keywordRestArgName == null) return new ArgsTailHolder(position, keywordArg, null, blockArg);
        
        String restKwargsName = keywordRestArgName;

        int slot = currentScope.exists(restKwargsName);
        if (slot == -1) slot = currentScope.addVariable(restKwargsName);

        KeywordRestArgNode keywordRestArg = new KeywordRestArgNode(position, restKwargsName, slot);
        
        return new ArgsTailHolder(position, keywordArg, keywordRestArg, blockArg);
    }

    public Node newAlias(ISourcePosition position, Node newNode, Node oldNode) {
        return new AliasNode(position, newNode, oldNode);
    }

    public Node newUndef(ISourcePosition position, Node nameNode) {
        return new UndefNode(position, nameNode);
    }

    /**
     * generate parsing error
     */
    public void yyerror(String message) {
        throw new SyntaxException(PID.GRAMMAR_ERROR, lexer.getPosition(), lexer.getCurrentLine(), message);
    }

    /**
     * generate parsing error
     * @param message text to be displayed.
     * @param expected list of acceptable tokens, if available.
     */
    public void yyerror(String message, String[] expected, String found) {
        String text = message + ", unexpected " + found + "\n";
        throw new SyntaxException(PID.GRAMMAR_ERROR, lexer.getPosition(), lexer.getCurrentLine(), text, found);
    }

    public ISourcePosition getPosition(ISourcePositionHolder start) {
        return start != null ? lexer.getPosition(start.getPosition()) : lexer.getPosition();
    }

    public void warn(ID id, ISourcePosition position, String message, Object... data) {
        warnings.warn(id, position, message);
    }

    public void warning(ID id, ISourcePosition position, String message, Object... data) {
        if (warnings.isVerbose()) warnings.warning(id, position, message);
    }

    // ENEBO: Totally weird naming (in MRI is not allocated and is a local var name) [1.9]
    public boolean is_local_id(String name) {
        return lexer.isIdentifierChar(name.charAt(0));
    }

    // 1.9
    public ListNode list_append(Node list, Node item) {
        if (list == null) return new ArrayNode(item.getPosition(), item);
        if (!(list instanceof ListNode)) return new ArrayNode(list.getPosition(), list).add(item);

        return ((ListNode) list).add(item);
    }

    // 1.9
    public Node new_bv(String identifier) {
        if (!is_local_id(identifier)) {
            getterIdentifierError(lexer.getPosition(), identifier);
        }
        shadowing_lvar(identifier);
        
        return arg_var(identifier);
    }

    // 1.9
    public ArgumentNode arg_var(String name) {
        StaticScope current = getCurrentScope();

        // Multiple _ arguments are allowed.  To not screw with tons of arity
        // issues in our runtime we will allocate unnamed bogus vars so things
        // still work. MRI does not use name as intern'd value so they don't
        // have this issue.
        if (name == "_") {
            int count = 0;
            while (current.exists(name) >= 0) {
                name = "_$" + count++;
            }
        }
        
        return new ArgumentNode(lexer.getPosition(), name, current.addVariableThisScope(name));
    }

    public String formal_argument(String identifier) {
        if (!is_local_id(identifier)) yyerror("formal argument must be local variable");

        return shadowing_lvar(identifier);
    }

    // 1.9
    public String shadowing_lvar(String name) {
        if (name == "_") return name;

        StaticScope current = getCurrentScope();
        if (current.isBlockScope()) {
            if (current.exists(name) >= 0) yyerror("duplicated argument name");

            if (warnings.isVerbose() && current.isDefined(name) >= 0 && Options.PARSER_WARN_LOCAL_SHADOWING.load()) {
                warnings.warning(ID.STATEMENT_NOT_REACHED, lexer.getPosition(),
                        "shadowing outer local variable - " + name);
            }
        } else if (current.exists(name) >= 0) {
            yyerror("duplicated argument name");
        }

        return name;
    }

    // 1.9
    public ListNode list_concat(Node first, Node second) {
        if (first instanceof ListNode) {
            if (second instanceof ListNode) {
                return ((ListNode) first).addAll((ListNode) second);
            } else {
                return ((ListNode) first).addAll(second);
            }
        }

        return new ArrayNode(first.getPosition(), first).add(second);
    }

    // 1.9
    /**
     * If node is a splat and it is splatting a literal array then return the literal array.
     * Otherwise return null.  This allows grammar to not splat into a Ruby Array if splatting
     * a literal array.
     */
    public Node splat_array(Node node) {
        if (node instanceof SplatNode) node = ((SplatNode) node).getValue();
        if (node instanceof ArrayNode) return node;
        return null;
    }

    // 1.9
    public Node arg_append(Node node1, Node node2) {
        if (node1 == null) return new ArrayNode(node2.getPosition(), node2);
        if (node1 instanceof ListNode) return ((ListNode) node1).add(node2);
        if (node1 instanceof BlockPassNode) return arg_append(((BlockPassNode) node1).getBodyNode(), node2);
        if (node1 instanceof ArgsPushNode) {
            ArgsPushNode pushNode = (ArgsPushNode) node1;
            Node body = pushNode.getSecondNode();

            return new ArgsCatNode(pushNode.getPosition(), pushNode.getFirstNode(),
                    new ArrayNode(body.getPosition(), body).add(node2));
        }

        return new ArgsPushNode(position(node1, node2), node1, node2);
    }

    // MRI: reg_fragment_check
    public void regexpFragmentCheck(RegexpNode end, ByteList value) {
        setRegexpEncoding(end, value);
        try {
            RubyRegexp.preprocessCheck(configuration.getRuntime(), value);
        } catch (RaiseException re) {
            compile_error(re.getMessage());
        }
    }        // 1.9 mode overrides to do extra checking...

    private List<Integer> allocateNamedLocals(RegexpNode regexpNode) {
        RubyRegexp pattern = RubyRegexp.newRegexp(configuration.getRuntime(), regexpNode.getValue(), regexpNode.getOptions());
        pattern.setLiteral();
        String[] names = pattern.getNames();
        int length = names.length;
        List<Integer> locals = new ArrayList<Integer>();
        StaticScope scope = getCurrentScope();

        for (int i = 0; i < length; i++) {
            // TODO: Pass by non-local-varnamed things but make sure consistent with list we get from regexp
            
            if (RubyLexer.getKeyword(names[i]) == null) {
                int slot = scope.isDefined(names[i]);
                if (slot >= 0) {
                    locals.add(slot);
                } else {
                    locals.add(getCurrentScope().addVariableThisScope(names[i]));
                }
            }
        }

        return locals;
    }

    private boolean is7BitASCII(ByteList value) {
        return StringSupport.codeRangeScan(value.getEncoding(), value) == StringSupport.CR_7BIT;
    }

    // TODO: Put somewhere more consolidated (similiar
    private char optionsEncodingChar(Encoding optionEncoding) {
        if (optionEncoding == RubyLexer.USASCII_ENCODING) return 'n';
        if (optionEncoding == org.jcodings.specific.EUCJPEncoding.INSTANCE) return 'e';
        if (optionEncoding == org.jcodings.specific.SJISEncoding.INSTANCE) return 's';
        if (optionEncoding == RubyLexer.UTF8_ENCODING) return 'u';

        return ' ';
    }

    public void compile_error(String message) { // mri: rb_compile_error_with_enc
        String line = lexer.getCurrentLine();
        ISourcePosition position = lexer.getPosition();
        String errorMessage = position.getFile() + ":" + position.getLine() + ": ";

        if (line != null && line.length() > 5) {
            boolean addNewline = message != null && ! message.endsWith("\n");

            message += (addNewline ? "\n" : "") + line;
        }

        throw getConfiguration().getRuntime().newSyntaxError(errorMessage + message);
    }

    protected void compileError(Encoding optionEncoding, Encoding encoding) {
        throw new SyntaxException(PID.REGEXP_ENCODING_MISMATCH, lexer.getPosition(), lexer.getCurrentLine(),
                "regexp encoding option '" + optionsEncodingChar(optionEncoding) +
                "' differs from source encoding '" + encoding + "'");
    }
    
    // MRI: reg_fragment_setenc_gen
    public void setRegexpEncoding(RegexpNode end, ByteList value) {
        RegexpOptions options = end.getOptions();
        Encoding optionsEncoding = options.setup(configuration.getRuntime()) ;

        // Change encoding to one specified by regexp options as long as the string is compatible.
        if (optionsEncoding != null) {
            if (optionsEncoding != value.getEncoding() && !is7BitASCII(value)) {
                compileError(optionsEncoding, value.getEncoding());
            }

            value.setEncoding(optionsEncoding);
        } else if (options.isEncodingNone()) {
            if (value.getEncoding() == RubyLexer.ASCII8BIT_ENCODING && !is7BitASCII(value)) {
                compileError(optionsEncoding, value.getEncoding());
            }
            value.setEncoding(RubyLexer.ASCII8BIT_ENCODING);
        } else if (lexer.getEncoding() == RubyLexer.USASCII_ENCODING) {
            if (!is7BitASCII(value)) {
                value.setEncoding(RubyLexer.USASCII_ENCODING); // This will raise later
            } else {
                value.setEncoding(RubyLexer.ASCII8BIT_ENCODING);
            }
        }
    }    

    protected void checkRegexpSyntax(ByteList value, RegexpOptions options) {
        RubyRegexp.newRegexp(getConfiguration().getRuntime(), value, options);
    }

    public Node newRegexpNode(ISourcePosition position, Node contents, RegexpNode end) {
        RegexpOptions options = end.getOptions();
        Encoding encoding = lexer.getEncoding();

        if (contents == null) {
            ByteList newValue = ByteList.create("");
            if (encoding != null) {
                newValue.setEncoding(encoding);
            }

            regexpFragmentCheck(end, newValue);
            return new RegexpNode(position, newValue, options.withoutOnce());
        } else if (contents instanceof StrNode) {
            ByteList meat = (ByteList) ((StrNode) contents).getValue().clone();
            regexpFragmentCheck(end, meat);
            checkRegexpSyntax(meat, options.withoutOnce());
            return new RegexpNode(contents.getPosition(), meat, options.withoutOnce());
        } else if (contents instanceof DStrNode) {
            DStrNode dStrNode = (DStrNode) contents;
            
            for (int i = 0; i < dStrNode.size(); i++) {
                Node fragment = dStrNode.get(i);
                if (fragment instanceof StrNode) {
                    ByteList frag = ((StrNode) fragment).getValue();
                    regexpFragmentCheck(end, frag);
//                    if (!lexer.isOneEight()) encoding = frag.getEncoding();
                }
            }
            
            dStrNode.prepend(new StrNode(contents.getPosition(), createMaster(options)));

            return new DRegexpNode(position, options, encoding).addAll(dStrNode);
        }

        // EvStrNode: #{val}: no fragment check, but at least set encoding
        ByteList master = createMaster(options);
        regexpFragmentCheck(end, master);
        encoding = master.getEncoding();
        DRegexpNode node = new DRegexpNode(position, options, encoding);
        node.add(new StrNode(contents.getPosition(), master));
        node.add(contents);
        return node;
    }
    
    // Create the magical empty 'master' string which will be encoded with
    // regexp options encoding so dregexps can end up starting with the
    // right encoding.
    private ByteList createMaster(RegexpOptions options) {
        Encoding encoding = options.setup(configuration.getRuntime());

        return new ByteList(new byte[] {}, encoding);
    }
    
    // FIXME:  This logic is used by many methods in MRI, but we are only using it in lexer
    // currently.  Consolidate this when we tackle a big encoding refactoring
    public static int associateEncoding(ByteList buffer, Encoding newEncoding, int codeRange) {
        Encoding bufferEncoding = buffer.getEncoding();
                
        if (newEncoding == bufferEncoding) return codeRange;
        
        // TODO: Special const error
        
        buffer.setEncoding(newEncoding);
        
        if (codeRange != StringSupport.CR_7BIT || !newEncoding.isAsciiCompatible()) {
            return StringSupport.CR_UNKNOWN;
        }
        
        return codeRange;
    }
    
    public KeywordArgNode keyword_arg(ISourcePosition position, AssignableNode assignable) {
        return new KeywordArgNode(position, assignable);
    }
    
    public NumericNode negateNumeric(NumericNode node) {
        switch (node.getNodeType()) {
            case FIXNUMNODE:
            case BIGNUMNODE:
                return negateInteger(node);
            case COMPLEXNODE:
                return negateComplexNode((ComplexNode) node);
            case FLOATNODE:
                return negateFloat((FloatNode) node);
            case RATIONALNODE:
                return negateRational((RationalNode) node);
        }
        
        yyerror("Invalid or unimplemented numeric to negate: " + node.toString());
        return null;
    }
    
    public Node new_defined(ISourcePosition position, Node something) {
        return new DefinedNode(position, something);
    }
    
    public String internalId() {
        return "";
    }
}
