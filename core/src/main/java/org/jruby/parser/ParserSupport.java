/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.*;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.ast.types.INameNode;
import org.jruby.ast.visitor.OperatorCallNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ext.coverage.CoverageData;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Signature;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.CommonByteLists;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import static org.jruby.lexer.LexingCommon.*;
import static org.jruby.util.RubyStringBuilder.*;

/** 
 *
 */
public class ParserSupport {
    // Parser states:
    protected StaticScope currentScope;
    protected ScopedParserState scopedParserState;

    protected RubyLexer lexer;
    
    // Is the parser currently within a method definition
    private boolean inDefinition;

    // Is the parser currently within a class body.
    private boolean inClass;

    // Should we warn if a variable is declared not actually used?
    private boolean warnOnUnusedVariables;

    protected IRubyWarnings warnings;

    protected ParserConfiguration configuration;
    private RubyParserResult result;

    public boolean isNextBreak = false;

    public void reset() {
        inDefinition = false;
    }

    public StaticScope getCurrentScope() {
        return currentScope;
    }
    
    public ParserConfiguration getConfiguration() {
        return configuration;
    }
    
    public void popCurrentScope() {
        if (!currentScope.isBlockScope()) { // blocks are soft scopes. All others are roots of lvars we are leaving.
            lexer.getCmdArgumentState().pop();
            lexer.getConditionState().pop();
        }

        if (warnOnUnusedVariables) {
            scopedParserState.warnUnusedVariables(configuration.getRuntime(), warnings, currentScope.getFile());
        }

        currentScope = currentScope.getEnclosingScope();
        scopedParserState = scopedParserState.getEnclosingScope();
    }
    
    public void pushBlockScope() {
        warnOnUnusedVariables = warnings.isVerbose();
        currentScope = configuration.getRuntime().getStaticScopeFactory().newBlockScope(currentScope, lexer.getFile());
        scopedParserState = new ScopedParserState(scopedParserState);
    }
    
    public void pushLocalScope() {
        warnOnUnusedVariables = warnings.isVerbose();
        currentScope = configuration.getRuntime().getStaticScopeFactory().newLocalScope(currentScope, lexer.getFile());
        scopedParserState = new ScopedParserState(scopedParserState, lexer.getCmdArgumentState().getStack(), lexer.getConditionState().getStack());
        lexer.getCmdArgumentState().push0();
        lexer.getConditionState().push0();
    }
    
    public static Node arg_concat(Node node1, Node node2) {
        return node2 == null ? node1 : new ArgsCatNode(node1.getLine(), node1, node2);
    }

    // firstNode is ArgsCatNode, SplatNode, ArrayNode, HashNode
    // secondNode is null or not
    public static Node arg_blk_pass(Node firstNode, BlockPassNode secondNode) {
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
            RubySymbol name = ((INameNode) node).getName();
            if (name.getBytes().equals(lexer.getCurrentArg())) {
                warnings.warn(ID.AMBIGUOUS_ARGUMENT, lexer.getFile(), node.getLine(), "circular argument reference - " + name);
            }
            Node newNode = currentScope.declare(node.getLine(), name);

            if (warnOnUnusedVariables && newNode instanceof IScopedNode) {
                scopedParserState.markUsedVariable(name, ((IScopedNode) node).getDepth());
            }
            return newNode;
        case CONSTDECLNODE: // CONSTANT
            return new ConstNode(node.getLine(), ((INameNode) node).getName());
        case INSTASGNNODE: // INSTANCE VARIABLE
            return new InstVarNode(node.getLine(), ((INameNode) node).getName());
        case CLASSVARDECLNODE:
        case CLASSVARASGNNODE:
            return new ClassVarNode(node.getLine(), ((INameNode) node).getName());
        case GLOBALASGNNODE:
            return new GlobalVarNode(node.getLine(), ((INameNode) node).getName());
        }

        getterIdentifierError(((INameNode) node).getName());
        return null;
    }

    public Node declareIdentifier(ByteList byteName) {
        RubySymbol name = symbolID(byteName);
        if (byteName.equals(lexer.getCurrentArg())) {
            warnings.warn(ID.AMBIGUOUS_ARGUMENT, lexer.getFile(), lexer.getRubySourceline(),
                    str(getConfiguration().getRuntime(), "circular argument reference - ", name));
        }

        int slot = currentScope.isDefined(name.idString());
        Node node = currentScope.declare(lexer.tokline, name);

        if (warnOnUnusedVariables && node instanceof IScopedNode) addOrMarkVariable(name, slot);

        return node;
    }

    // We know it has to be tLABEL or tIDENTIFIER so none of the other assignable logic is needed
    public AssignableNode assignableLabelOrIdentifier(ByteList byteName, Node value) {
        RubySymbol name = symbolID(byteName);

        if (warnOnUnusedVariables) addOrMarkVariable(name, currentScope.isDefined(name.idString()));

        return currentScope.assign(lexer.getRubySourceline(), name, makeNullNil(value));
    }

    private void addOrMarkVariable(RubySymbol name, int slot) {
        if (slot == -1) {
            scopedParserState.addDefinedVariable(name, lexer.getRubySourceline());
        } else {
            scopedParserState.markUsedVariable(name, slot >> 16);
        }
    }

    // We know it has to be tLABEL or tIDENTIFIER so none of the other assignable logic is needed
    public AssignableNode assignableKeyword(ByteList name, Node value) {
        return currentScope.assignKeyword(lexer.getRubySourceline(), symbolID(name), makeNullNil(value));
    }
    
    protected void getterIdentifierError(RubySymbol identifier) {
        lexer.compile_error("identifier " + identifier + " is not valid to get");
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     */
    public Node newline_node(Node node, int line) {
        if (node == null) return null;

        configuration.coverLine(line);
        node.setNewline();

        return node;
    }

    // This is the last node made in the AST unintuitively so so post-processing can occur here.
    public Node addRootNode(Node topOfAST) {
        int line;
        CoverageData coverageData = configuration.finishCoverage(lexer.getFile(), lexer.lineno());
        if (result.getBeginNodes().isEmpty()) {
            if (topOfAST == null) {
                topOfAST = NilImplicitNode.NIL;
                line = lexer.getRubySourceline();
            } else {
                line = topOfAST.getLine();
            }
        } else {
            line = topOfAST != null ? topOfAST.getLine() : result.getBeginNodes().get(0).getLine();
            BlockNode newTopOfAST = new BlockNode(line);
            for (Node beginNode : result.getBeginNodes()) {
                appendToBlock(newTopOfAST, beginNode);
            }

            // Add real top to new top (unless this top is empty [only begin/end nodes or truly empty])
            if (topOfAST != null) newTopOfAST.add(topOfAST);
            topOfAST = newTopOfAST;
        }

        int coverageMode = coverageData == null ?
                CoverageData.NONE :
                coverageData.getMode();

        return new RootNode(line, result.getScope(), topOfAST, lexer.getFile(), coverageMode);
    }
    
    /* MRI: block_append */
    public Node appendToBlock(Node head, Node tail) {
        if (tail == null) return head;
        if (head == null) return tail;

        switch (head.getNodeType()) {
            case BIGNUMNODE: case FIXNUMNODE: case FLOATNODE: // NODE_LIT
            case STRNODE: case SELFNODE: case TRUENODE: case FALSENODE: case NILNODE:
                warnings.warning(ID.MISCELLANEOUS, lexer.getFile(), tail.getLine(), "unused literal ignored");
                return tail;
        }

        if (!(head instanceof BlockNode)) {
            head = new BlockNode(head.getLine()).add(head);
        }

        if (warnings.isVerbose() && isBreakStatement(((ListNode) head).getLast()) && Options.PARSER_WARN_NOT_REACHED.load()) {
            warnings.warning(ID.STATEMENT_NOT_REACHED, lexer.getFile(), tail.getLine(), "statement not reached");
        }

        // Assumption: tail is never a list node
        ((ListNode) head).addAll(tail);
        return head;
    }

    // We know it has to be tLABEL or tIDENTIFIER so none of the other assignable logic is needed
    public AssignableNode assignableInCurr(ByteList nameBytes, Node value) {
        RubySymbol name = symbolID(nameBytes);
        currentScope.addVariableThisScope(name.idString());
        if (warnOnUnusedVariables) scopedParserState.addDefinedVariable(name, lexer.getRubySourceline());
        return currentScope.assign(lexer.getRubySourceline(), name, makeNullNil(value));
    }

    public Node getOperatorCallNode(Node firstNode, ByteList operator) {
        value_expr(lexer, firstNode);

        return new OperatorCallNode(firstNode.getLine(), firstNode, symbolID(operator), null, null, false);
    }
    
    public Node getOperatorCallNode(Node firstNode, ByteList operator, Node secondNode) {
        return getOperatorCallNodeInner(firstNode, operator, secondNode);
    }

    public Node getOperatorCallNode(Node firstNode, ByteList operator, Node secondNode, int defaultPosition) {
        firstNode = checkForNilNode(firstNode, defaultPosition);
        secondNode = checkForNilNode(secondNode, defaultPosition);

        return getOperatorCallNodeInner(firstNode, operator, secondNode);
    }

    private Node getOperatorCallNodeInner(Node firstNode, ByteList operator, Node secondNode) {
        value_expr(lexer, firstNode);
        value_expr(lexer, secondNode);

        return new OperatorCallNode(firstNode.getLine(), firstNode, symbolID(operator), new ArrayNode(secondNode.getLine(), secondNode), null, false);
    }

    public Node getMatchNode(Node firstNode, Node secondNode) {
        if (firstNode instanceof DRegexpNode) {
            return new Match2Node(firstNode.getLine(), firstNode, secondNode);
        } else if (firstNode instanceof RegexpNode) {
            List<Integer> locals = allocateNamedLocals((RegexpNode) firstNode);

            if (locals.size() > 0) {
                int[] primitiveLocals = new int[locals.size()];
                for (int i = 0; i < primitiveLocals.length; i++) {
                    primitiveLocals[i] = locals.get(i);
                }
                return new Match2CaptureNode(firstNode.getLine(), firstNode, secondNode, primitiveLocals);
            } else {
                return new Match2Node(firstNode.getLine(), firstNode, secondNode);
            }
        } else if (secondNode instanceof DRegexpNode || secondNode instanceof RegexpNode) {
            return new Match3Node(firstNode.getLine(), firstNode, secondNode);
        }

        return getOperatorCallNode(firstNode, CommonByteLists.EQUAL_TILDE, secondNode);
    }

    /**
     * Define an array set condition so we can return lhs
     * 
     * @param receiver array being set
     * @param index node which should evalute to index of array set
     * @return an AttrAssignNode
     */
    public Node aryset(Node receiver, Node index) {
        value_expr(lexer, receiver);

        return new_attrassign(receiver.getLine(), receiver, CommonByteLists.ASET_METHOD, index, false);
    }

    /**
     * Define an attribute set condition so we can return lhs
     * 
     * @param receiver object which contains attribute
     * @param name of the attribute being set
     * @return an AttrAssignNode
     */
    public Node attrset(Node receiver, ByteList name) {
        return attrset(receiver, DOT, name);
    }

    public Node attrset(Node receiver, ByteList callType, ByteList name) {
        return new_attrassign(receiver.getLine(), receiver, name.append('='), null, isLazy(callType));
    }

    public void backrefAssignError(Node node) {
        if (node instanceof NthRefNode) {
            String varName = "$" + ((NthRefNode) node).getMatchNumber();
            lexer.compile_error("Can't set variable " + varName + '.');
        } else if (node instanceof BackRefNode) {
            String varName = "$" + ((BackRefNode) node).getType();
            lexer.compile_error("Can't set variable " + varName + '.');
        }
    }

    private static Node arg_add(int line, Node node1, Node node2) {
        if (node1 == null) {
            if (node2 == null) {
                return new ArrayNode(line, NilImplicitNode.NIL);
            } else {
                return new ArrayNode(node2.getLine(), node2);
            }
        }
        if (node1 instanceof ArrayNode) return ((ArrayNode) node1).add(node2);
        
        return new ArgsPushNode(line, node1, node2);
    }
    
	/**
	 **/
    public static Node node_assign(Node lhs, Node rhs) {
        if (lhs == null) return null;

        // MRI sets position to one passed in its version of node_assign but it is always pos of lhs????
        if (lhs instanceof AssignableNode) {
    	    ((AssignableNode) lhs).setValueNode(rhs);
        } else if (lhs instanceof IArgumentNode) {
            IArgumentNode invokableNode = (IArgumentNode) lhs;
            
            return invokableNode.setArgsNode(arg_add(lhs.getLine(), invokableNode.getArgsNode(), rhs));
        }
        
        return lhs;
    }
    
    public Node ret_args(Node node, int line) {
        if (node != null) {
            if (node instanceof BlockPassNode) {
                lexer.compile_error("block argument should not be given");
            } else if (node instanceof ArrayNode && ((ArrayNode)node).size() == 1) {
                node = ((ArrayNode)node).get(0);
            } else if (node instanceof SplatNode) {
                node = newSValueNode(line, node);
            }
        }

        if (node == null) node = NilImplicitNode.NIL;
        
        return node;
    }

    private static boolean isBreakStatement(Node node) {
        if (node == null) return false;

        switch (node.getNodeType()) {
            case BREAKNODE: case NEXTNODE: case REDONODE:
            case RETRYNODE: case RETURNNODE:
                return true;
            default:
                return false;
        }
    }
    
    public void warnUnlessEOption(ID id, Node node, String message) {
        if (!configuration.isInlineSource()) {
            warnings.warn(id, lexer.getFile(), node.getLine(), message);
        }
    }

    public boolean value_expr(RubyLexer lexer, Node node) {
        boolean conditional = false;

        while (node != null) {
            switch (node.getNodeType()) {
                case RETURNNODE: case BREAKNODE: case NEXTNODE: case REDONODE:
                case RETRYNODE:
                    if (!conditional) lexer.compile_error("void value expression");

                    return false;
                case BLOCKNODE:
                    node = ((BlockNode) node).getLast();
                    break;
                case BEGINNODE:
                    node = ((BeginNode) node).getBodyNode();
                    break;
                case IFNODE:
                    if (!value_expr(lexer, ((IfNode) node).getThenBody())) return false;
                    node = ((IfNode) node).getElseBody();
                    break;
                case ANDNODE: case ORNODE:
                    conditional = true;
                    node = ((BinaryOperatorNode) node).getSecondNode();
                    break;
                case LOCALASGNNODE: case DASGNNODE:
                    if (warnOnUnusedVariables) {
                        scopedParserState.markUsedVariable((((INameNode) node).getName()), (((IScopedNode) node).getDepth()));
                    }
                    return true;
                default: // Node
                    return true;
            }
        }

        return true;
    }

    private void handleUselessWarn(Node node, String useless) {
        if (Options.PARSER_WARN_USELESSS_USE_OF.load()) {
            warnings.warn(ID.USELESS_EXPRESSION, lexer.getFile(), node.getLine(), "possibly useless use of " + useless + " in void context");
        }
    }

    /**
     * Check to see if current node is an useless statement.  If useless a warning if printed.
     * 
     * @param node to be checked.
     */
    public void void_expr(Node node) {
        if (!warnings.isVerbose()) return;
        
        if (node == null) return;
            
        switch (node.getNodeType()) {
            case CALLNODE: {
                if (!(node instanceof OperatorCallNode)) return;
                ByteList name = ((CallNode) node).getName().getBytes();
                int length = name.realSize();

                if (length > 3) {
                    return;
                } else if (length == 3) {
                    if (name.charAt(0) == '<' || name.charAt(1) == '=' || name.charAt(2) == '>') {
                        handleUselessWarn(node, name.toString());
                    }
                    return;
                }

                boolean isUseless = false;
                switch (name.charAt(0)) {
                    case '+': case '-':
                        if (length == 1 || name.charAt(1) == '@') isUseless = true;
                        break;
                    case '*':
                        if (length == 1 || name.charAt(1) == '*') isUseless = true;
                        break;
                    case '/': case '%': case '|': case '^': case '&':
                        if (length == 1) isUseless = true;
                        break;
                    case '<': case '>': case '=':
                        if (length == 1 || name.charAt(1) == '=') isUseless = true;
                        break;

                    case '!':
                        if (length > 1 && name.charAt(1) == '=') isUseless = true;
                        break;
                }

                if (isUseless) handleUselessWarn(node, name.toString());

                return;
            }
            case BACKREFNODE: case DVARNODE: case GLOBALVARNODE:
            case LOCALVARNODE: case NTHREFNODE: case CLASSVARNODE:
            case INSTVARNODE:
                handleUselessWarn(node, "a variable"); return;
            case CONSTNODE:
                handleUselessWarn(node, "a constant"); return;
            case BIGNUMNODE: case DREGEXPNODE: case DSTRNODE: case DSYMBOLNODE:
            case FIXNUMNODE: case FLOATNODE: case REGEXPNODE:
            case STRNODE: case SYMBOLNODE:
                handleUselessWarn(node, "a literal"); return;
            case COLON2NODE: case COLON3NODE:
                handleUselessWarn(node, "::"); return;
            case DOTNODE:
                handleUselessWarn(node, ((DotNode) node).isExclusive() ? "..." : ".."); return;
            case SELFNODE:
                handleUselessWarn(node, "self"); return;
            case NILNODE:
                handleUselessWarn(node, "nil"); return;
            case FALSENODE:
                handleUselessWarn(node, "false"); return;
            case TRUENODE:
                handleUselessWarn(node, "true"); return;
            case DEFINEDNODE:
                handleUselessWarn(node, "defined?"); return;
        }
    }

    /**
     * Check all nodes but the last one in a BlockNode for useless (void context) statements.
     * 
     * @param node to be checked.
     */
    public Node void_stmts(Node node) {
        if (!warnings.isVerbose() || !(node instanceof BlockNode)) return node;

        BlockNode blockNode = (BlockNode) node;
        int size = blockNode.size();

        for (int i = 0; i <= size - 2; i++) {
            void_expr(blockNode.get(i));
        }

        return node;
    }

	/**
     * assign_in_cond
	 **/
    private boolean checkAssignmentInCondition(Node node) {
        if (node instanceof MultipleAsgnNode || node instanceof LocalAsgnNode || node instanceof DAsgnNode || node instanceof GlobalAsgnNode || node instanceof InstAsgnNode) {
            Node valueNode = ((AssignableNode) node).getValueNode();
            if (isStaticContent(valueNode)) {
                warnings.warn(ID.ASSIGNMENT_IN_CONDITIONAL, lexer.getFile(), valueNode.getLine(), "found = in conditional, should be ==");
            }
            return true;
        } 

        return false;
    }

    // Only literals or does it contain something more dynamic like variables?
    private static boolean isStaticContent(Node node) {
        if (node instanceof HashNode) {
            HashNode hash = (HashNode) node;
            for (KeyValuePair<Node, Node> pair : hash.getPairs()) {
                if (!isStaticContent(pair.getKey()) || !isStaticContent(pair.getValue())) return false;
            }
            return true;
        } else if (node instanceof ArrayNode) {
            ArrayNode array = (ArrayNode) node;
            int size = array.size();

            for (int i = 0; i < size; i++) {
                if (!isStaticContent(array.get(i))) return false;
            }
            return true;
        }

        return node instanceof ILiteralNode || node instanceof NilNode || node instanceof TrueNode || node instanceof FalseNode;
    }
    
    protected Node makeNullNil(Node node) {
        return node == null ? NilImplicitNode.NIL : node;
    }

    private Node cond0(Node node, boolean method) {
        checkAssignmentInCondition(node);

        if (node == null) return new NilNode(lexer.getRubySourceline());
        
        Node leftNode;
        Node rightNode;

        switch(node.getNodeType()) {
            case DSTRNODE:
            case EVSTRNODE:
            case STRNODE:
                if (!method) warn(ID.VOID_VALUE_EXPRESSION, node.getLine(), "string literal in condition");
                break;
            case DREGEXPNODE: {
                int line = node.getLine();

                return new Match2Node(line, node, new GlobalVarNode(line, symbolID(DOLLAR_UNDERSCORE)));
            }
            case ANDNODE:
                leftNode = cond0(((AndNode) node).getFirstNode(), false);
                rightNode = cond0(((AndNode) node).getSecondNode(), false);
            
                return new AndNode(node.getLine(), makeNullNil(leftNode), makeNullNil(rightNode));
            case ORNODE:
                leftNode = cond0(((OrNode) node).getFirstNode(), false);
                rightNode = cond0(((OrNode) node).getSecondNode(), false);
            
                return new OrNode(node.getLine(), makeNullNil(leftNode), makeNullNil(rightNode));
            case DOTNODE: {
                DotNode dotNode = (DotNode) node;
                if (dotNode.isLiteral()) return node;
            
                ByteList label = new ByteList(new byte[] {'F', 'L', 'I', 'P'}, USASCII_ENCODING);
                label.append(Long.toString(node.hashCode()).getBytes());
                RubySymbol symbolID = symbolID(label);

                if (!method && !configuration.isInlineSource()) {
                    if ((dotNode.getBeginNode() instanceof TrueNode && dotNode.getEndNode() instanceof FalseNode) ||
                            (dotNode.getBeginNode() instanceof FalseNode && dotNode.getEndNode() instanceof TrueNode)) {
                        warn(ID.VOID_VALUE_EXPRESSION, node.getLine(), "range literal in condition");
                    }

                }

                return new FlipNode(node.getLine(),
                        getFlipConditionNode(((DotNode) node).getBeginNode()),
                        getFlipConditionNode(((DotNode) node).getEndNode()),
                        dotNode.isExclusive(), currentScope.getLocalScope().addVariable(symbolID.idString()));
            }
            case SYMBOLNODE:
            case DSYMBOLNODE:
            case FIXNUMNODE:
                if (!method) warn(ID.VOID_VALUE_EXPRESSION, node.getLine(), "literal in condition");
                break;
            case REGEXPNODE:
                if (Options.PARSER_WARN_REGEX_CONDITION.load()) {
                    if (!method) warnUnlessEOption(ID.REGEXP_LITERAL_IN_CONDITION, node, "regex literal in condition");
                }
            
                return new MatchNode(node.getLine(), node);
        }

        return node;
    }

    public Node cond(Node node) {
        return cond0(node, false);
    }

    public Node method_cond(Node node) {
        return cond0(node, true);
    }

    // we just reverse then/else for unless
    public Node new_if(int line, Node condition, Node thenNode, Node elseNode) {
        if (condition == null) return elseNode;

        condition = cond0(condition, false);

        return new IfNode(line, condition, thenNode, elseNode);
    }

    /* MRI: range_op */
    private Node getFlipConditionNode(Node node) {
        if (!configuration.isInlineSource()) return node;
        
        node = cond0(node, false);

        if (node instanceof FixnumNode) {
            warnUnlessEOption(ID.LITERAL_IN_CONDITIONAL_RANGE, node, "integer literal in conditional range");
            return getOperatorCallNode(node, EQ_EQ, new GlobalVarNode(node.getLine(), symbolID(DOLLAR_DOT)));
        } 

        return node;
    }

    public SValueNode newSValueNode(int line, Node node) {
        return new SValueNode(line, node);
    }

    // note: node is from arg_value and will be implicit nil or a real node.
    public SplatNode newSplatNode(Node node) {
        int line = node instanceof NilImplicitNode ? lexer.getRubySourceline() : node.getLine();
        return new SplatNode(line, node);
    }

    // FIXME: audit all callers and see if we can remove makeNullNil here (we deplicate both line and makeNullNil on occasions.
    public ArrayNode newArrayNode(int line, Node node) {
        node = makeNullNil(node);
        line = node instanceof NilImplicitNode ? lexer.getRubySourceline() : node.getLine();

        return new ArrayNode(line, node);
    }

    public int position(Node one, Node two) {
        return one == null ? two.getLine() : one.getLine();
    }

    public AndNode newAndNode(Node left, Node right) {
        value_expr(lexer, left);

        if (left == null && right == null) return new AndNode(lexer.getRubySourceline(), makeNullNil(left), makeNullNil(right));
        
        return new AndNode(position(left, right), makeNullNil(left), makeNullNil(right));
    }

    public OrNode newOrNode(Node left, Node right) {
        value_expr(lexer, left);

        if (left == null && right == null) return new OrNode(lexer.getRubySourceline(), makeNullNil(left), makeNullNil(right));
        
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
    public static CaseNode newCaseNode(int line, Node expression, Node firstWhenNode) {
        ArrayNode cases = new ArrayNode(firstWhenNode != null ? firstWhenNode.getLine() : line);
        CaseNode caseNode = new CaseNode(line, expression, cases);

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
    private static void simplifyMultipleArgumentWhenNodes(WhenNode sourceWhen, ArrayNode cases) {
        Node expressionNodes = sourceWhen.getExpressionNodes();

        if (expressionNodes instanceof SplatNode || expressionNodes instanceof ArgsCatNode) {
            cases.add(sourceWhen);
            return;
        }

        if (expressionNodes instanceof ListNode) {
            ListNode list = (ListNode) expressionNodes;
            int line = sourceWhen.getLine();
            Node bodyNode = sourceWhen.getBodyNode();

            for (int i = 0; i < list.size(); i++) {
                Node expression = list.get(i);

                if (expression instanceof SplatNode || expression instanceof ArgsCatNode) {
                    cases.add(new WhenNode(line, expression, bodyNode, null));
                } else {
                    cases.add(new WhenOneArgNode(line, expression, bodyNode, null));
                }
            }
        } else {
            cases.add(sourceWhen);
        }
    }
    
    public WhenNode newWhenNode(int line, Node expressionNodes, Node bodyNode, Node nextCase) {
        if (bodyNode == null) bodyNode = NilImplicitNode.NIL;

        if (expressionNodes instanceof SplatNode || expressionNodes instanceof ArgsCatNode || expressionNodes instanceof ArgsPushNode) {
            return new WhenNode(line, expressionNodes, bodyNode, nextCase);
        }

        ListNode list = (ListNode) expressionNodes;

        if (list.size() == 1) {
            Node element = list.get(0);
            
            if (!(element instanceof SplatNode)) {
                return new WhenOneArgNode(line, element, bodyNode, nextCase);
            }
        }

        return new WhenNode(line, expressionNodes, bodyNode, nextCase);
    }

    public Node new_op_assign(AssignableNode receiverNode, ByteList operatorName, Node valueNode) {
        int line = receiverNode.getLine();

        if (operatorName == OR_OR) {
            receiverNode.setValueNode(valueNode);
            return new OpAsgnOrNode(line, gettable2(receiverNode), receiverNode);
        } else if (operatorName == AMPERSAND_AMPERSAND) {
            receiverNode.setValueNode(valueNode);
            return new OpAsgnAndNode(line, gettable2(receiverNode), receiverNode);
        } else {
            receiverNode.setValueNode(getOperatorCallNode(gettable2(receiverNode), operatorName, valueNode));
            receiverNode.setLine(line);
            return receiverNode;
        }
    }

    public Node new_ary_op_assign(Node receiverNode, ByteList operatorName, Node argsNode, Node valueNode) {
        int line = lexer.tokline;

        // We extract BlockPass from tree and insert it as a block node value (MRI wraps it around the args)
        Node blockNode = null;
        if (argsNode instanceof BlockPassNode) {
            blockNode = argsNode; // It is weird to leave this as-is but we need to know it vs iternode vs weird ast bug.
            argsNode = ((BlockPassNode) argsNode).getArgsNode();
        }

        Node newNode = new OpElementAsgnNode(line, receiverNode, symbolID(operatorName), argsNode, valueNode, blockNode);
        fixpos(newNode, receiverNode);

        return newNode;
    }

    public Node new_attr_op_assign(Node receiverNode, ByteList callType, Node valueNode, ByteList variableName, ByteList operatorName) {
        return new OpAsgnNode(receiverNode.getLine(), receiverNode, valueNode, symbolID(variableName), symbolID(operatorName), isLazy(callType));
    }

    public Node new_const_op_assign(int line, Node lhs, ByteList operatorName, Node rhs) {
        // FIXME: Maybe need to fixup position?
        if (lhs != null) {
            return new OpAsgnConstDeclNode(line, lhs, symbolID(operatorName), rhs);
        } else {
            return new BeginNode(line, NilImplicitNode.NIL);
        }
    }

    public Node new_bodystmt(Node head, RescueBodyNode rescue, Node rescueElse, Node ensure) {
        Node node = head;

        if (rescue != null) {
            node = new RescueNode(getPosition(head), head, rescue, rescueElse);
        } else if (rescueElse != null) {
            // FIXME: MRI removed this...
            warn(ID.ELSE_WITHOUT_RESCUE, lexer.tokline, "else without rescue is useless");
            node = appendToBlock(head, rescue);
        }
        if (ensure != null) {
            if (node != null) {
                node = new EnsureNode(getPosition(head), makeNullNil(node), ensure);
            } else {
                node = appendToBlock(ensure, NilImplicitNode.NIL);
            }
        }

        fixpos(node, head);
        return node;
    }

    public RubySymbol symbolID(ByteList identifierValue) {
        // FIXME: We walk this during identifier construction so we should calculate CR without having to walk twice.
        if (RubyString.scanForCodeRange(identifierValue) == StringSupport.CR_BROKEN) {
            Ruby runtime = getConfiguration().getRuntime();
            throw runtime.newEncodingError(str(runtime, "invalid symbol in encoding " + lexer.getEncoding() + " :\"", inspectIdentifierByteList(runtime, identifierValue), "\""));
        }

        return RubySymbol.newIDSymbol(getConfiguration().getRuntime(), identifierValue);
    }

    public boolean isLazy(String callType) {
        return "&.".equals(callType);
    }

    public boolean isLazy(ByteList callType) {
        return callType == AMPERSAND_DOT;
    }
    
    public Node new_attrassign(int line, Node receiver, ByteList name, Node argsNode, boolean isLazy) {
        // We extract BlockPass from tree and insert it as a block node value (MRI wraps it around the args)
        Node blockNode = null;
        if (argsNode instanceof BlockPassNode) {
            blockNode = argsNode; // It is weird to leave this as-is but we need to know it vs iternode vs weird ast bug.
            argsNode = ((BlockPassNode) argsNode).getArgsNode();
        }

        return new AttrAssignNode(line, receiver, symbolID(name), argsNode, blockNode, isLazy);
    }

    public Node new_call(Node receiver, ByteList callType, ByteList name, Node argsNode, Node iter) {
        return new_call(receiver, callType, name, argsNode, iter, position(receiver, argsNode));
    }

    public Node new_call(Node receiver, ByteList callType, ByteList name, Node argsNode, Node iter, int line) {
        if (argsNode instanceof BlockPassNode) {
            if (iter != null) lexer.compile_error("Both block arg and actual block given.");

            BlockPassNode blockPass = (BlockPassNode) argsNode;
            return new CallNode(line, receiver, symbolID(name), blockPass.getArgsNode(), blockPass, isLazy(callType));
        }

        return new CallNode(line, receiver, symbolID(name), argsNode, iter, isLazy(callType));
    }

    public Node new_call(Node receiver, ByteList name, Node argsNode, Node iter) {
        return new_call(receiver, DOT, name, argsNode, iter);
    }

    public Colon2Node new_colon2(int line, Node leftNode, ByteList name) {
        if (leftNode == null) return new Colon2ImplicitNode(line, symbolID(name));

        return new Colon2ConstNode(line, leftNode, symbolID(name));
    }

    public Colon3Node new_colon3(int line, ByteList name) {
        return new Colon3Node(line, symbolID(name));
    }

    public void frobnicate_fcall_args(FCallNode fcall, Node args, Node iter) {
        if (args instanceof BlockPassNode) {
            if (iter != null) lexer.compile_error("Both block arg and actual block given.");

            BlockPassNode blockPass = (BlockPassNode) args;
            args = blockPass.getArgsNode();
            iter = blockPass;
        }

        fcall.setArgsNode(args);
        fcall.setIterNode(iter);
    }

    public void fixpos(Node node, Node orig) {
        if (node == null || orig == null) return;

        node.setLine(orig.getLine());
    }

    public Node new_fcall(ByteList operation) {
        return new FCallNode(lexer.tokline, symbolID(operation));
    }

    public Node new_super(int line, Node args) {
        if (args instanceof BlockPassNode) {
            return new SuperNode(line, ((BlockPassNode) args).getArgsNode(), args);
        }
        return new SuperNode(line, args);
    }

    /**
    *  Description of the RubyMethod
    */
    public void initTopLocalVariables() {
        DynamicScope scope = configuration.getScope(lexer.getFile());
        currentScope = scope.getStaticScope();
        scopedParserState = new ScopedParserState(null);
        warnOnUnusedVariables = warnings.isVerbose() && !configuration.isEvalParse() && !configuration.isInlineSource();
        
        result.setScope(scope);
    }

    public boolean isInDef() {
        return inDefinition;
    }

    public void setInDef(boolean inDef) {
        this.inDefinition = inDef;
    }

    public boolean isInClass() {
        return inClass;
    }

    public void setIsInClass(boolean inClass) {
        this.inClass = inClass;
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

    public DStrNode createDStrNode(int line) {
        DStrNode dstr = new DStrNode(line, lexer.getEncoding());
        if (getConfiguration().isFrozenStringLiteral()) dstr.setFrozen(true);
        return dstr;
    }

    public KeyValuePair<Node, Node> createKeyValue(Node key, Node value) {
        if (key instanceof StrNode) ((StrNode) key).setFrozen(true);

        return new KeyValuePair<>(key, value);
    }

    public Node asSymbol(int line, ByteList value) {
        return new SymbolNode(line, symbolID(value));
    }

        
    public Node asSymbol(int line, Node value) {
        return value instanceof StrNode ? new SymbolNode(line, symbolID(((StrNode) value).getValue())) :
                new DSymbolNode(line, (DStrNode) value);
    }
    
    public Node literal_concat(Node head, Node tail) {
        if (head == null) return tail;
        if (tail == null) return head;
        
        if (head instanceof EvStrNode) {
            head = createDStrNode(head.getLine()).add(head);
        }

        if (lexer.getHeredocIndent() > 0) {
            if (head instanceof StrNode) {
                head = createDStrNode(head.getLine()).add(head);
                return list_append(head, tail);
            } else if (head instanceof DStrNode) {
                return list_append(head, tail);
            }
        }

        if (tail instanceof StrNode) {
            if (head instanceof StrNode) {
                StrNode front = (StrNode) head;
                // string_contents always makes an empty strnode...which is sometimes valid but
                // never if it ever is in literal_concat.
                if (front.getValue().getRealSize() > 0) {
                    return new StrNode(head.getLine(), front, (StrNode) tail);
                } else {
                    return tail;
                }
            } 
            head.setLine(head.getLine());
            return ((ListNode) head).add(tail);
        	
        } else if (tail instanceof DStrNode) {
            if (head instanceof StrNode) { // Str + oDStr -> Dstr(Str, oDStr.contents)
                DStrNode newDStr = new DStrNode(head.getLine(), ((DStrNode) tail).getEncoding());
                newDStr.add(head);
                newDStr.addAll(tail);
                if (getConfiguration().isFrozenStringLiteral()) newDStr.setFrozen(true);
                return newDStr;
            } 

            return ((ListNode) head).addAll(tail);
        } 

        // tail must be EvStrNode at this point 
        if (head instanceof StrNode) {
        	
            //Do not add an empty string node
            if(((StrNode) head).getValue().length() == 0) {
                head = createDStrNode(head.getLine());
            } else {
                head = createDStrNode(head.getLine()).add(head);
            }
        }
        return ((DStrNode) head).add(tail);
    }

    public Node newRescueModNode(Node body, Node rescueBody) {
        if (rescueBody == null) rescueBody = NilImplicitNode.NIL; // foo rescue () can make null.
        int line = getPosition(body);

        if (body instanceof OpElementAsgnNode) {
            OpElementAsgnNode original = (OpElementAsgnNode) body;
            return new OpElementAsgnNode(line, original.getReceiverNode(), original.getOperatorSymbolName(),
                    original.getArgsNode(), new RescueModNode(line, original.getValueNode(), new RescueBodyNode(line, null, rescueBody, null)), original.getBlockNode());
        }

        return new RescueModNode(line, body, new RescueBodyNode(line, null, rescueBody, null));
    }
    
    public Node newEvStrNode(int line, Node node) {
        if (node instanceof StrNode || node instanceof DStrNode || node instanceof EvStrNode) return node;

        return new EvStrNode(line, node);
    }
    
    public Node new_yield(int line, Node node) {
        if (node instanceof BlockPassNode) lexer.compile_error("Block argument should not be given.");

        return new YieldNode(line, node);
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
                return new FixnumNode(bignumNode.getLine(), value.longValue());
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
        return (RationalNode) rationalNode.negate();
    }
    
    private Node checkForNilNode(Node node, int defaultPosition) {
        return (node == null) ? new NilNode(defaultPosition) : node; 
    }

    public Node new_args(int line, ListNode pre, ListNode optional, RestArgNode rest,
            ListNode post, ArgsTailHolder tail) {
        ArgsNode argsNode;
        if (tail == null) {
            argsNode = new ArgsNode(line, pre, optional, rest, post, null);
        } else {
            argsNode = new ArgsNode(line, pre, optional, rest, post,
                    tail.getKeywordArgs(), tail.getKeywordRestArgNode(), tail.getBlockArg());
        }

        getCurrentScope().setSignature(Signature.from(argsNode));

        return argsNode;
    }

    public ArgsTailHolder new_args_tail(int line, ListNode keywordArg,
                                        ByteList keywordRestArgName, BlockArgNode blockArg) {
        if (keywordRestArgName == null) return new ArgsTailHolder(line, keywordArg, null, blockArg);

        RubySymbol restKwargsName = symbolID(keywordRestArgName);
        String id = restKwargsName.idString();

        int slot = currentScope.exists(id);
        if (slot == -1) slot = currentScope.addVariable(id);

        KeywordRestArgNode keywordRestArg = new KeywordRestArgNode(line, restKwargsName, slot);

        return new ArgsTailHolder(line, keywordArg, keywordRestArg, blockArg);
    }

    public Node remove_duplicate_keys(HashNode hash) {
        List<Node> encounteredKeys = new ArrayList<>();

        for (KeyValuePair<Node,Node> pair: hash.getPairs()) {
            Node key = pair.getKey();
            if (key == null || !(key instanceof LiteralValue)) continue;
            int index = encounteredKeys.indexOf(key);
            if (index >= 0) {
                Ruby runtime = getConfiguration().getRuntime();
                IRubyObject value = ((LiteralValue) key).literalValue(runtime);
                warnings.warn(ID.AMBIGUOUS_ARGUMENT, lexer.getFile(), hash.getLine(), str(runtime, "key ", value.inspect(),
                        " is duplicated and overwritten on line " + (encounteredKeys.get(index).getLine() + 1)));
            } else {
                encounteredKeys.add(key);
            }
        }

        for (Node key: encounteredKeys) {
            hash.getPairs().remove(key);
        }

        return hash;
    }

    public static Node newAlias(int line, Node newNode, Node oldNode) {
        return new AliasNode(line, newNode, oldNode);
    }

    public static Node newUndef(int line, Node nameNode) {
        return new UndefNode(line, nameNode);
    }

    /**
     * generate parsing error
     */
    public void yyerror(String message) {
        lexer.compile_error(message);
    }

    public void yyerror(String message, ProductionState state) {
        lexer.compile_error(message, state.start, state.end);
    }

    /**
     * generate parsing error
     * @param message text to be displayed.
     * @param expected list of acceptable tokens, if available.
     */
    public void yyerror(String message, String[] expected, String found) {
        lexer.compile_error(message + ", unexpected " + found + "\n");
    }

    public int getPosition(Node start) {
        return start != null ? start.getLine() : lexer.getRubySourceline();
    }

    public void warn(ID id, int line, String message, Object... data) {
        warnings.warn(id, lexer.getFile(), line, message);
    }

    // FIXME: Replace this with file/line version and stop using ISourcePosition
    public void warning(ID id, int line, String message, Object... data) {
        if (warnings.isVerbose()) warnings.warning(id, lexer.getFile(), line, message);
    }

    // ENEBO: Totally weird naming (in MRI is not allocated and is a local var name) [1.9]
    public static boolean is_local_id(ByteList name) {
        return RubyLexer.isIdentifierChar(name.charAt(0));
    }

    @Deprecated
    public boolean is_local_id(String name) {
        return RubyLexer.isIdentifierChar(name.charAt(0));
    }

    // 1.9
    public ListNode list_append(Node list, Node item) {
        if (list == null) return new ArrayNode(item.getLine(), item);
        if (!(list instanceof ListNode)) return new ArrayNode(list.getLine(), list).add(item);

        return ((ListNode) list).add(item);
    }

    public Node new_bv(ByteList identifier) {
        if (!is_local_id(identifier)) getterIdentifierError(symbolID(identifier));

        shadowing_lvar(identifier);
        
        return arg_var(identifier);
    }

    public ArgumentNode arg_var(ByteList byteName) {
        RubySymbol name = symbolID(byteName);

        if (warnOnUnusedVariables) {
            scopedParserState.addDefinedVariable(name, lexer.getRubySourceline());
            scopedParserState.markUsedVariable(name, 0);
        }
        return new ArgumentNode(lexer.getRubySourceline(), name, getCurrentScope().addVariableThisScope(name.idString()));
    }

    public ByteList formal_argument(ByteList identifier) {
        lexer.validateFormalIdentifier(identifier);

        return shadowing_lvar(identifier);
    }

    public static boolean is_private_local_id(ByteList name) {
        if (name.realSize() == 1 && name.charAt(0) == '_') return true;
        if (!is_local_id(name)) return false;

        return name.charAt(0) == '_';
    }

    // 1.9
    public ByteList shadowing_lvar(ByteList nameBytes) {
        if (is_private_local_id(nameBytes)) return nameBytes;

        RubySymbol name = symbolID(nameBytes);
        String id = name.idString();

        StaticScope current = getCurrentScope();
        if (current.exists(id) >= 0) yyerror("duplicated argument name");

        if (warnOnUnusedVariables) {
            int slot = current.isDefined(id);
            if (slot != -1) {
                scopedParserState.addDefinedVariable(name, lexer.getRubySourceline());
                scopedParserState.markUsedVariable(name, slot >> 16);
            }
        }

        return nameBytes;
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

        return new ArrayNode(first.getLine(), first).add(second);
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

    public Node arg_append(Node node1, Node node2) {
        if (node1 == null) return new ArrayNode(node2.getLine(), node2);
        if (node1 instanceof ListNode) return ((ListNode) node1).add(node2);
        if (node1 instanceof BlockPassNode) return arg_append(((BlockPassNode) node1).getBodyNode(), node2);
        if (node1 instanceof ArgsPushNode) {
            ArgsPushNode pushNode = (ArgsPushNode) node1;
            Node body = pushNode.getSecondNode();

            return new ArgsCatNode(pushNode.getLine(), pushNode.getFirstNode(),
                    new ArrayNode(body.getLine(), body).add(node2));
        }

        return new ArgsPushNode(position(node1, node2), node1, node2);
    }

    private List<Integer> allocateNamedLocals(RegexpNode regexpNode) {
        RubyRegexp pattern = RubyRegexp.newRegexp(configuration.getRuntime(), regexpNode.getValue(), regexpNode.getOptions());
        pattern.setLiteral();
        String[] names = pattern.getNames();
        List<Integer> locals = new ArrayList<>();
        StaticScope scope = getCurrentScope();

        Ruby runtime = getConfiguration().getRuntime();
        for (String name : names) {
            if (RubyLexer.getKeyword(name) == null && !Character.isUpperCase(name.charAt(0))) {
                String id = runtime.newSymbol(name).idString();
                int slot = scope.isDefined(id);
                if (slot >= 0) {
                    locals.add(slot);
                } else {
                    int index = getCurrentScope().addVariableThisScope(id);
                    locals.add(index);
                    scopedParserState.growNamedCaptures(index);
                }
            }
        }

        return locals;
    }

    public void compile_error(String message) { // mri: rb_compile_error_with_enc
        String line = lexer.getCurrentLine();
        int pos = lexer.getRubySourceline();
        String errorMessage = lexer.getFile() + ":" + (pos + 1) + ": ";

        if (line != null && line.length() > 5) {
            boolean addNewline = message != null && ! message.endsWith("\n");

            message += (addNewline ? "\n" : "") + line;
        }

        throw getConfiguration().getRuntime().newSyntaxError(errorMessage + message);
    }

    public Node newRegexpNode(int line, Node contents, RegexpNode end) {
        Ruby runtime = configuration.getRuntime();
        RegexpOptions options = end.getOptions();
        Encoding encoding = lexer.getEncoding();

        if (contents == null) {
            ByteList newValue = ByteList.create("");
            if (encoding != null) {
                newValue.setEncoding(encoding);
            }

            lexer.checkRegexpFragment(runtime, newValue, options);
            return new RegexpNode(line, newValue, options.withoutOnce());
        } else if (contents instanceof StrNode) {
            ByteList meat = (ByteList) ((StrNode) contents).getValue().clone();
            lexer.checkRegexpFragment(runtime, meat, options);
            lexer.checkRegexpSyntax(runtime, meat, options.withoutOnce());
            return new RegexpNode(contents.getLine(), meat, options.withoutOnce());
        } else if (contents instanceof DStrNode) {
            DStrNode dStrNode = (DStrNode) contents;
            
            for (int i = 0; i < dStrNode.size(); i++) {
                Node fragment = dStrNode.get(i);
                if (fragment instanceof StrNode) {
                    ByteList frag = ((StrNode) fragment).getValue();
                    lexer.checkRegexpFragment(runtime, frag, options);
//                    if (!lexer.isOneEight()) encoding = frag.getEncoding();
                }
            }
            
            DRegexpNode dRegexpNode = new DRegexpNode(line, options, encoding);
            dRegexpNode.add(new StrNode(contents.getLine(), createMaster(options)));
            dRegexpNode.addAll(dStrNode);
            return dRegexpNode;
        }

        // EvStrNode: #{val}: no fragment check, but at least set encoding
        ByteList master = createMaster(options);
        lexer.checkRegexpFragment(runtime, master, options);
        encoding = master.getEncoding();
        DRegexpNode node = new DRegexpNode(line, options, encoding);
        node.add(new StrNode(contents.getLine(), master));
        node.add(contents);
        return node;
    }
    
    // Create the magical empty 'master' string which will be encoded with
    // regexp options encoding so dregexps can end up starting with the
    // right encoding.
    private ByteList createMaster(RegexpOptions options) {
        Encoding encoding = options.setup(configuration.getRuntime());

        return new ByteList(ByteList.NULL_ARRAY, encoding);
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

    public static final ByteList INTERNAL_ID = new ByteList(new byte[] {}, USASCIIEncoding.INSTANCE);

    @Deprecated
    public String internalId() {
        return INTERNAL_ID.toString();
    }

}
