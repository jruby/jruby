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
package org.jruby.truffle.parser.parser;

import org.jcodings.Encoding;
import org.jruby.RubyBignum;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.truffle.RubyContext;
import org.jruby.truffle.core.regexp.ClassicRegexp;
import org.jruby.truffle.core.regexp.RegexpOptions;
import org.jruby.truffle.language.control.RaiseException;
import org.jruby.truffle.parser.Signature;
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
import org.jruby.truffle.parser.ast.BinaryOperatorParseNode;
import org.jruby.truffle.parser.ast.BlockArgParseNode;
import org.jruby.truffle.parser.ast.BlockParseNode;
import org.jruby.truffle.parser.ast.BlockPassParseNode;
import org.jruby.truffle.parser.ast.CallParseNode;
import org.jruby.truffle.parser.ast.CaseParseNode;
import org.jruby.truffle.parser.ast.ClassVarParseNode;
import org.jruby.truffle.parser.ast.Colon2ConstParseNode;
import org.jruby.truffle.parser.ast.Colon2ImplicitParseNode;
import org.jruby.truffle.parser.ast.Colon2ParseNode;
import org.jruby.truffle.parser.ast.Colon3ParseNode;
import org.jruby.truffle.parser.ast.ComplexParseNode;
import org.jruby.truffle.parser.ast.ConstParseNode;
import org.jruby.truffle.parser.ast.DAsgnParseNode;
import org.jruby.truffle.parser.ast.DRegexpParseNode;
import org.jruby.truffle.parser.ast.DStrParseNode;
import org.jruby.truffle.parser.ast.DSymbolParseNode;
import org.jruby.truffle.parser.ast.DefinedParseNode;
import org.jruby.truffle.parser.ast.DotParseNode;
import org.jruby.truffle.parser.ast.EvStrParseNode;
import org.jruby.truffle.parser.ast.FCallParseNode;
import org.jruby.truffle.parser.ast.FalseParseNode;
import org.jruby.truffle.parser.ast.FixnumParseNode;
import org.jruby.truffle.parser.ast.FlipParseNode;
import org.jruby.truffle.parser.ast.FloatParseNode;
import org.jruby.truffle.parser.ast.GlobalAsgnParseNode;
import org.jruby.truffle.parser.ast.GlobalVarParseNode;
import org.jruby.truffle.parser.ast.HashParseNode;
import org.jruby.truffle.parser.ast.IArgumentNode;
import org.jruby.truffle.parser.ast.IfParseNode;
import org.jruby.truffle.parser.ast.InstAsgnParseNode;
import org.jruby.truffle.parser.ast.InstVarParseNode;
import org.jruby.truffle.parser.ast.KeywordArgParseNode;
import org.jruby.truffle.parser.ast.KeywordRestArgParseNode;
import org.jruby.truffle.parser.ast.ListParseNode;
import org.jruby.truffle.parser.ast.LocalAsgnParseNode;
import org.jruby.truffle.parser.ast.Match2CaptureParseNode;
import org.jruby.truffle.parser.ast.Match2ParseNode;
import org.jruby.truffle.parser.ast.Match3ParseNode;
import org.jruby.truffle.parser.ast.MatchParseNode;
import org.jruby.truffle.parser.ast.MultipleAsgnParseNode;
import org.jruby.truffle.parser.ast.NilImplicitParseNode;
import org.jruby.truffle.parser.ast.NilParseNode;
import org.jruby.truffle.parser.ast.NthRefParseNode;
import org.jruby.truffle.parser.ast.NumericParseNode;
import org.jruby.truffle.parser.ast.OpAsgnConstDeclParseNode;
import org.jruby.truffle.parser.ast.OpAsgnParseNode;
import org.jruby.truffle.parser.ast.OpElementAsgnParseNode;
import org.jruby.truffle.parser.ast.OrParseNode;
import org.jruby.truffle.parser.ast.ParseNode;
import org.jruby.truffle.parser.ast.RationalParseNode;
import org.jruby.truffle.parser.ast.RegexpParseNode;
import org.jruby.truffle.parser.ast.RescueBodyParseNode;
import org.jruby.truffle.parser.ast.RescueModParseNode;
import org.jruby.truffle.parser.ast.RestArgParseNode;
import org.jruby.truffle.parser.ast.RootParseNode;
import org.jruby.truffle.parser.ast.SValueParseNode;
import org.jruby.truffle.parser.ast.SplatParseNode;
import org.jruby.truffle.parser.ast.StrParseNode;
import org.jruby.truffle.parser.ast.SuperParseNode;
import org.jruby.truffle.parser.ast.SymbolParseNode;
import org.jruby.truffle.parser.ast.TrueParseNode;
import org.jruby.truffle.parser.ast.UndefParseNode;
import org.jruby.truffle.parser.ast.WhenOneArgParseNode;
import org.jruby.truffle.parser.ast.WhenParseNode;
import org.jruby.truffle.parser.ast.YieldParseNode;
import org.jruby.truffle.parser.ast.types.ILiteralNode;
import org.jruby.truffle.parser.ast.types.INameNode;
import org.jruby.truffle.parser.lexer.ISourcePosition;
import org.jruby.truffle.parser.lexer.ISourcePositionHolder;
import org.jruby.truffle.parser.lexer.RubyLexer;
import org.jruby.truffle.parser.lexer.SyntaxException.PID;
import org.jruby.truffle.parser.scope.DynamicScope;
import org.jruby.truffle.parser.scope.StaticScope;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import org.jruby.util.cli.Options;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.jruby.truffle.parser.lexer.LexingCommon.ASCII8BIT_ENCODING;
import static org.jruby.truffle.parser.lexer.LexingCommon.USASCII_ENCODING;
import static org.jruby.truffle.parser.lexer.LexingCommon.UTF8_ENCODING;

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

    private final RubyContext context;

    public ParserSupport(RubyContext context) {
        this.context = context;
    }

    public RubyContext getContext() {
        return context;
    }

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
        if (!currentScope.isBlockScope()) {
            lexer.getCmdArgumentState().reset(currentScope.getCommandArgumentStack());
        }
        currentScope = currentScope.getEnclosingScope();

    }

    public void pushBlockScope() {
        currentScope = configuration.getStaticScopeFactory().newBlockScope(currentScope, lexer.getFile());
    }

    public void pushLocalScope() {
        currentScope = configuration.getStaticScopeFactory().newLocalScope(currentScope, lexer.getFile());
        currentScope.setCommandArgumentStack(lexer.getCmdArgumentState().getStack());
        lexer.getCmdArgumentState().reset(0);
    }

    public ParseNode arg_concat(ISourcePosition position, ParseNode node1, ParseNode node2) {
        return node2 == null ? node1 : new ArgsCatParseNode(position, node1, node2);
    }

    public ParseNode arg_blk_pass(ParseNode firstNode, BlockPassParseNode secondNode) {
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
    public ParseNode gettable2(ParseNode node) {
        switch (node.getNodeType()) {
        case DASGNNODE: // LOCALVAR
        case LOCALASGNNODE:
            String name = ((INameNode) node).getName();
            if (name.equals(lexer.getCurrentArg())) {
                warn(ID.AMBIGUOUS_ARGUMENT, node.getPosition(), "circular argument reference - " + name);
            }
            return currentScope.declare(node.getPosition(), name);
        case CONSTDECLNODE: // CONSTANT
            return new ConstParseNode(node.getPosition(), ((INameNode) node).getName());
        case INSTASGNNODE: // INSTANCE VARIABLE
            return new InstVarParseNode(node.getPosition(), ((INameNode) node).getName());
        case CLASSVARDECLNODE:
        case CLASSVARASGNNODE:
            return new ClassVarParseNode(node.getPosition(), ((INameNode) node).getName());
        case GLOBALASGNNODE:
            return new GlobalVarParseNode(node.getPosition(), ((INameNode) node).getName());
        }

        getterIdentifierError(node.getPosition(), ((INameNode) node).getName());
        return null;
    }

    public ParseNode declareIdentifier(String name) {
        if (name.equals(lexer.getCurrentArg())) {
            warn(ID.AMBIGUOUS_ARGUMENT, lexer.getPosition(), "circular argument reference - " + name);
        }
        return currentScope.declare(lexer.tokline, name);
    }

    // We know it has to be tLABEL or tIDENTIFIER so none of the other assignable logic is needed
    public AssignableParseNode assignableLabelOrIdentifier(String name, ParseNode value) {
        return currentScope.assign(lexer.getPosition(), name.intern(), makeNullNil(value));
    }

    // Only calls via f_kw so we know it has to be tLABEL
    public AssignableParseNode assignableLabel(String name, ParseNode value) {
        return currentScope.assign(lexer.getPosition(), name, makeNullNil(value));
    }

    protected void getterIdentifierError(ISourcePosition position, String identifier) {
        lexer.compile_error(PID.BAD_IDENTIFIER, "identifier " + identifier + " is not valid to get");
    }

    /**
     *  Wraps node with NEWLINE node.
     *
     *@param node
     */
    public ParseNode newline_node(ParseNode node, ISourcePosition position) {
        if (node == null) return null;
        node.setNewline();
        return node;
    }

    // This is the last node made in the AST unintuitively so so post-processing can occur here.
    public ParseNode addRootNode(ParseNode topOfAST) {
        final int endPosition;

        if (lexer.isEndSeen()) {
            endPosition = lexer.getLineOffset();
        } else {
            endPosition = -1;
        }

        ISourcePosition position;
        if (result.getBeginNodes().isEmpty()) {
            if (topOfAST == null) {
                topOfAST = NilImplicitParseNode.NIL;
                position = lexer.getPosition();
            } else {
                position = topOfAST.getPosition();
            }
        } else {
            position = topOfAST != null ? topOfAST.getPosition() : result.getBeginNodes().get(0).getPosition();
            BlockParseNode newTopOfAST = new BlockParseNode(position);
            for (ParseNode beginNode : result.getBeginNodes()) {
                appendToBlock(newTopOfAST, beginNode);
            }

            // Add real top to new top (unless this top is empty [only begin/end nodes or truly empty])
            if (topOfAST != null) newTopOfAST.add(topOfAST);
            topOfAST = newTopOfAST;
        }

        return new RootParseNode(position, result.getScope(), topOfAST, lexer.getFile(), endPosition, false);
    }

    /* MRI: block_append */
    public ParseNode appendToBlock(ParseNode head, ParseNode tail) {
        if (tail == null) return head;
        if (head == null) return tail;

        if (!(head instanceof BlockParseNode)) {
            head = new BlockParseNode(head.getPosition()).add(head);
        }

        if (warnings.isVerbose() && isBreakStatement(((ListParseNode) head).getLast()) && Options.PARSER_WARN_NOT_REACHED.load()) {
            warnings.warning(ID.STATEMENT_NOT_REACHED, tail.getPosition().getFile(), tail.getPosition().getLine(), "statement not reached");
        }

        // Assumption: tail is never a list node
        ((ListParseNode) head).addAll(tail);
        return head;
    }

    // We know it has to be tLABEL or tIDENTIFIER so none of the other assignable logic is needed
    public AssignableParseNode assignableInCurr(String name, ParseNode value) {
        currentScope.addVariableThisScope(name);
        return currentScope.assign(lexer.getPosition(), name, makeNullNil(value));
    }

    public ParseNode getOperatorCallNode(ParseNode firstNode, String operator) {
        checkExpression(firstNode);

        return new CallParseNode(firstNode.getPosition(), firstNode, operator, null, null);
    }

    public ParseNode getOperatorCallNode(ParseNode firstNode, String operator, ParseNode secondNode) {
        return getOperatorCallNode(firstNode, operator, secondNode, null);
    }

    public ParseNode getOperatorCallNode(ParseNode firstNode, String operator, ParseNode secondNode, ISourcePosition defaultPosition) {
        if (defaultPosition != null) {
            firstNode = checkForNilNode(firstNode, defaultPosition);
            secondNode = checkForNilNode(secondNode, defaultPosition);
        }

        checkExpression(firstNode);
        checkExpression(secondNode);

        return new CallParseNode(firstNode.getPosition(), firstNode, operator, new ArrayParseNode(secondNode.getPosition(), secondNode), null);
    }

    public ParseNode getMatchNode(ParseNode firstNode, ParseNode secondNode) {
        if (firstNode instanceof DRegexpParseNode) {
            return new Match2ParseNode(firstNode.getPosition(), firstNode, secondNode);
        } else if (firstNode instanceof RegexpParseNode) {
            List<Integer> locals = allocateNamedLocals((RegexpParseNode) firstNode);

            if (locals.size() > 0) {
                int[] primitiveLocals = new int[locals.size()];
                for (int i = 0; i < primitiveLocals.length; i++) {
                    primitiveLocals[i] = locals.get(i);
                }
                return new Match2CaptureParseNode(firstNode.getPosition(), firstNode, secondNode, primitiveLocals);
            } else {
                return new Match2ParseNode(firstNode.getPosition(), firstNode, secondNode);
            }
        } else if (secondNode instanceof DRegexpParseNode || secondNode instanceof RegexpParseNode) {
            return new Match3ParseNode(firstNode.getPosition(), firstNode, secondNode);
        }

        return getOperatorCallNode(firstNode, "=~", secondNode);
    }

    /**
     * Define an array set condition so we can return lhs
     *
     * @param receiver array being set
     * @param index node which should evalute to index of array set
     * @return an AttrAssignParseNode
     */
    public ParseNode aryset(ParseNode receiver, ParseNode index) {
        checkExpression(receiver);

        return new_attrassign(receiver.getPosition(), receiver, "[]=", index, false);
    }

    /**
     * Define an attribute set condition so we can return lhs
     *
     * @param receiver object which contains attribute
     * @param name of the attribute being set
     * @return an AttrAssignParseNode
     */
    public ParseNode attrset(ParseNode receiver, String name) {
        return attrset(receiver, ".", name);
    }

    public ParseNode attrset(ParseNode receiver, String callType, String name) {
        checkExpression(receiver);

        return new_attrassign(receiver.getPosition(), receiver, name + "=", null, isLazy(callType));
    }

    public void backrefAssignError(ParseNode node) {
        if (node instanceof NthRefParseNode) {
            String varName = "$" + ((NthRefParseNode) node).getMatchNumber();
            lexer.compile_error(PID.INVALID_ASSIGNMENT, "Can't set variable " + varName + '.');
        } else if (node instanceof BackRefParseNode) {
            String varName = "$" + ((BackRefParseNode) node).getType();
            lexer.compile_error(PID.INVALID_ASSIGNMENT, "Can't set variable " + varName + '.');
        }
    }

    public ParseNode arg_add(ISourcePosition position, ParseNode node1, ParseNode node2) {
        if (node1 == null) {
            if (node2 == null) {
                return new ArrayParseNode(position, NilImplicitParseNode.NIL);
            } else {
                return new ArrayParseNode(node2.getPosition(), node2);
            }
        }
        if (node1 instanceof ArrayParseNode) return ((ArrayParseNode) node1).add(node2);

        return new ArgsPushParseNode(position, node1, node2);
    }

	/**
	 * @fixme position
	 **/
    public ParseNode node_assign(ParseNode lhs, ParseNode rhs) {
        if (lhs == null) return null;

        ParseNode newNode = lhs;

        checkExpression(rhs);
        if (lhs instanceof AssignableParseNode) {
    	    ((AssignableParseNode) lhs).setValueNode(rhs);
        } else if (lhs instanceof IArgumentNode) {
            IArgumentNode invokableNode = (IArgumentNode) lhs;

            return invokableNode.setArgsNode(arg_add(lhs.getPosition(), invokableNode.getArgsNode(), rhs));
        }

        return newNode;
    }

    public ParseNode ret_args(ParseNode node, ISourcePosition position) {
        if (node != null) {
            if (node instanceof BlockPassParseNode) {
                lexer.compile_error(PID.BLOCK_ARG_UNEXPECTED, "block argument should not be given");
            } else if (node instanceof ArrayParseNode && ((ArrayParseNode)node).size() == 1) {
                node = ((ArrayParseNode)node).get(0);
            } else if (node instanceof SplatParseNode) {
                node = newSValueNode(position, node);
            }
        }

        if (node == null) node = NilImplicitParseNode.NIL;

        return node;
    }

    /**
     * Is the supplied node a break/control statement?
     *
     * @param node to be checked
     * @return true if a control node, false otherwise
     */
    public boolean isBreakStatement(ParseNode node) {
        breakLoop: do {
            if (node == null) return false;

            switch (node.getNodeType()) {
            case BREAKNODE: case NEXTNODE: case REDONODE:
            case RETRYNODE: case RETURNNODE:
                return true;
            default:
                return false;
            }
        } while (true);
    }

    public void warnUnlessEOption(ID id, ParseNode node, String message) {
        if (!configuration.isInlineSource()) {
            warnings.warn(id, node.getPosition().getFile(), node.getPosition().getLine(), message);
        }
    }

    public void warningUnlessEOption(ID id, ParseNode node, String message) {
        if (warnings.isVerbose() && !configuration.isInlineSource()) {
            warnings.warning(id, node.getPosition().getFile(), node.getPosition().getLine(), message);
        }
    }

    // logical equivalent to value_expr in MRI
    public boolean checkExpression(ParseNode node) {
        boolean conditional = false;

        while (node != null) {
            switch (node.getNodeType()) {
            case RETURNNODE: case BREAKNODE: case NEXTNODE: case REDONODE:
            case RETRYNODE:
                if (!conditional) lexer.compile_error(PID.VOID_VALUE_EXPRESSION, "void value expression");

                return false;
            case BLOCKNODE:
                node = ((BlockParseNode) node).getLast();
                break;
            case BEGINNODE:
                node = ((BeginParseNode) node).getBodyNode();
                break;
            case IFNODE:
                if (!checkExpression(((IfParseNode) node).getThenBody())) return false;
                node = ((IfParseNode) node).getElseBody();
                break;
            case ANDNODE: case ORNODE:
                conditional = true;
                node = ((BinaryOperatorParseNode) node).getSecondNode();
                break;
            default: // ParseNode
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
    public boolean isLiteral(ParseNode node) {
        return node != null && (node instanceof FixnumParseNode || node instanceof BignumParseNode ||
                node instanceof FloatParseNode || node instanceof SymbolParseNode ||
                (node instanceof RegexpParseNode && ((RegexpParseNode) node).getOptions().toJoniOptions() == 0));
    }

    private void handleUselessWarn(ParseNode node, String useless) {
        if (Options.PARSER_WARN_USELESSS_USE_OF.load()) {
            warnings.warn(ID.USELESS_EXPRESSION, node.getPosition().getFile(), node.getPosition().getLine(), "Useless use of " + useless + " in void context.");
        }
    }

    /**
     * Check to see if current node is an useless statement.  If useless a warning if printed.
     *
     * @param node to be checked.
     */
    public void checkUselessStatement(ParseNode node) {
        if (!warnings.isVerbose() || (!configuration.isInlineSource() && configuration.isEvalParse())) return;

        uselessLoop: do {
            if (node == null) return;

            switch (node.getNodeType()) {
            case CALLNODE: {
                String name = ((CallParseNode) node).getName();

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
                handleUselessWarn(node, ((DotParseNode) node).isExclusive() ? "..." : ".."); return;
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
     * Check all nodes but the last one in a BlockParseNode for useless (void context) statements.
     *
     * @param blockNode to be checked.
     */
    public void checkUselessStatements(BlockParseNode blockNode) {
        if (warnings.isVerbose()) {
            ParseNode lastNode = blockNode.getLast();

            for (int i = 0; i < blockNode.size(); i++) {
                ParseNode currentNode = blockNode.get(i);

                if (lastNode != currentNode ) {
                    checkUselessStatement(currentNode);
                }
            }
        }
    }

	/**
     * assign_in_cond
	 **/
    private boolean checkAssignmentInCondition(ParseNode node) {
        if (node instanceof MultipleAsgnParseNode) {
            lexer.compile_error(PID.MULTIPLE_ASSIGNMENT_IN_CONDITIONAL, "multiple assignment in conditional");
        } else if (node instanceof LocalAsgnParseNode || node instanceof DAsgnParseNode || node instanceof GlobalAsgnParseNode || node instanceof InstAsgnParseNode) {
            ParseNode valueNode = ((AssignableParseNode) node).getValueNode();
            if (isStaticContent(valueNode)) {
                warnings.warn(ID.ASSIGNMENT_IN_CONDITIONAL, node.getPosition().getFile(), node.getPosition().getLine(), "found = in conditional, should be ==");
            }
            return true;
        }

        return false;
    }

    // Only literals or does it contain something more dynamic like variables?
    private boolean isStaticContent(ParseNode node) {
        if (node instanceof HashParseNode) {
            HashParseNode hash = (HashParseNode) node;
            for (KeyValuePair<ParseNode, ParseNode> pair : hash.getPairs()) {
                if (!isStaticContent(pair.getKey()) || !isStaticContent(pair.getValue())) return false;
            }
            return true;
        } else if (node instanceof ArrayParseNode) {
            ArrayParseNode array = (ArrayParseNode) node;
            int size = array.size();

            for (int i = 0; i < size; i++) {
                if (!isStaticContent(array.get(i))) return false;
            }
            return true;
        } else if (node instanceof ILiteralNode || node instanceof NilParseNode || node instanceof TrueParseNode || node instanceof FalseParseNode) {
            return true;
        }

        return false;
    }

    protected ParseNode makeNullNil(ParseNode node) {
        return node == null ? NilImplicitParseNode.NIL : node;
    }

    private ParseNode cond0(ParseNode node) {
        checkAssignmentInCondition(node);

        if (node == null) return new NilParseNode(lexer.getPosition());

        ParseNode leftNode;
        ParseNode rightNode;

        // FIXME: DSTR,EVSTR,STR: warning "string literal in condition"
        switch(node.getNodeType()) {
        case DREGEXPNODE: {
            ISourcePosition position = node.getPosition();

            return new Match2ParseNode(position, node, new GlobalVarParseNode(position, "$_"));
        }
        case ANDNODE:
            leftNode = cond0(((AndParseNode) node).getFirstNode());
            rightNode = cond0(((AndParseNode) node).getSecondNode());

            return new AndParseNode(node.getPosition(), makeNullNil(leftNode), makeNullNil(rightNode));
        case ORNODE:
            leftNode = cond0(((OrParseNode) node).getFirstNode());
            rightNode = cond0(((OrParseNode) node).getSecondNode());

            return new OrParseNode(node.getPosition(), makeNullNil(leftNode), makeNullNil(rightNode));
        case DOTNODE: {
            DotParseNode dotNode = (DotParseNode) node;
            if (dotNode.isLiteral()) return node;

            String label = String.valueOf("FLIP" + node.hashCode());
            currentScope.getLocalScope().addVariable(label);
            int slot = currentScope.isDefined(label);

            return new FlipParseNode(node.getPosition(),
                    getFlipConditionNode(((DotParseNode) node).getBeginNode()),
                    getFlipConditionNode(((DotParseNode) node).getEndNode()),
                    dotNode.isExclusive(), slot);
        }
        case REGEXPNODE:
            if (Options.PARSER_WARN_REGEX_CONDITION.load()) {
                warningUnlessEOption(ID.REGEXP_LITERAL_IN_CONDITION, node, "regex literal in condition");
            }

            return new MatchParseNode(node.getPosition(), node);
        }

        return node;
    }

    public ParseNode getConditionNode(ParseNode node) {
        ParseNode cond = cond0(node);

        cond.setNewline();

        return cond;
    }

    /* MRI: range_op */
    private ParseNode getFlipConditionNode(ParseNode node) {
        if (!configuration.isInlineSource()) return node;

        node = getConditionNode(node);

        if (node instanceof FixnumParseNode) {
            warnUnlessEOption(ID.LITERAL_IN_CONDITIONAL_RANGE, node, "integer literal in conditional range");
            return getOperatorCallNode(node, "==", new GlobalVarParseNode(node.getPosition(), "$."));
        }

        return node;
    }

    public SValueParseNode newSValueNode(ISourcePosition position, ParseNode node) {
        return new SValueParseNode(position, node);
    }

    public SplatParseNode newSplatNode(ISourcePosition position, ParseNode node) {
        return new SplatParseNode(position, makeNullNil(node));
    }

    public ArrayParseNode newArrayNode(ISourcePosition position, ParseNode firstNode) {
        return new ArrayParseNode(position, makeNullNil(firstNode));
    }

    public ISourcePosition position(ISourcePositionHolder one, ISourcePositionHolder two) {
        return one == null ? two.getPosition() : one.getPosition();
    }

    public AndParseNode newAndNode(ISourcePosition position, ParseNode left, ParseNode right) {
        checkExpression(left);

        if (left == null && right == null) return new AndParseNode(position, makeNullNil(left), makeNullNil(right));

        return new AndParseNode(position(left, right), makeNullNil(left), makeNullNil(right));
    }

    public OrParseNode newOrNode(ISourcePosition position, ParseNode left, ParseNode right) {
        checkExpression(left);

        if (left == null && right == null) return new OrParseNode(position, makeNullNil(left), makeNullNil(right));

        return new OrParseNode(position(left, right), makeNullNil(left), makeNullNil(right));
    }

    /**
     * Ok I admit that this is somewhat ugly.  We post-process a chain of when nodes and analyze
     * them to re-insert them back into our new CaseParseNode the way we want.  The grammar is being
     * difficult and until I go back into the depths of that this is where things are.
     *
     * @param expression of the case node (e.g. case foo)
     * @param firstWhenNode first when (which could also be the else)
     * @return a new case node
     */
    public CaseParseNode newCaseNode(ISourcePosition position, ParseNode expression, ParseNode firstWhenNode) {
        ArrayParseNode cases = new ArrayParseNode(firstWhenNode != null ? firstWhenNode.getPosition() : position);
        CaseParseNode caseNode = new CaseParseNode(position, expression, cases);

        for (ParseNode current = firstWhenNode; current != null; current = ((WhenParseNode) current).getNextCase()) {
            if (current instanceof WhenOneArgParseNode) {
                cases.add(current);
            } else if (current instanceof WhenParseNode) {
                simplifyMultipleArgumentWhenNodes((WhenParseNode) current, cases);
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
    private void simplifyMultipleArgumentWhenNodes(WhenParseNode sourceWhen, ArrayParseNode cases) {
        ParseNode expressionNodes = sourceWhen.getExpressionNodes();

        if (expressionNodes instanceof SplatParseNode || expressionNodes instanceof ArgsCatParseNode) {
            cases.add(sourceWhen);
            return;
        }

        if (expressionNodes instanceof ListParseNode) {
            ListParseNode list = (ListParseNode) expressionNodes;
            ISourcePosition position = sourceWhen.getPosition();
            ParseNode bodyNode = sourceWhen.getBodyNode();

            for (int i = 0; i < list.size(); i++) {
                ParseNode expression = list.get(i);

                if (expression instanceof SplatParseNode || expression instanceof ArgsCatParseNode) {
                    cases.add(new WhenParseNode(position, expression, bodyNode, null));
                } else {
                    cases.add(new WhenOneArgParseNode(position, expression, bodyNode, null));
                }
            }
        } else {
            cases.add(sourceWhen);
        }
    }

    public WhenParseNode newWhenNode(ISourcePosition position, ParseNode expressionNodes, ParseNode bodyNode, ParseNode nextCase) {
        if (bodyNode == null) bodyNode = NilImplicitParseNode.NIL;

        if (expressionNodes instanceof SplatParseNode || expressionNodes instanceof ArgsCatParseNode || expressionNodes instanceof ArgsPushParseNode) {
            return new WhenParseNode(position, expressionNodes, bodyNode, nextCase);
        }

        ListParseNode list = (ListParseNode) expressionNodes;

        if (list.size() == 1) {
            ParseNode element = list.get(0);

            if (!(element instanceof SplatParseNode)) {
                return new WhenOneArgParseNode(position, element, bodyNode, nextCase);
            }
        }

        return new WhenParseNode(position, expressionNodes, bodyNode, nextCase);
    }

    // FIXME: Currently this is passing in position of receiver
    public ParseNode new_opElementAsgnNode(ParseNode receiverNode, String operatorName, ParseNode argsNode, ParseNode valueNode) {
        ISourcePosition position = lexer.tokline;  // FIXME: ruby_sourceline in new lexer.

        ParseNode newNode = new OpElementAsgnParseNode(position, receiverNode, operatorName, argsNode, valueNode);

        fixpos(newNode, receiverNode);

        return newNode;
    }

    public ParseNode newOpAsgn(ISourcePosition position, ParseNode receiverNode, String callType, ParseNode valueNode, String variableName, String operatorName) {
        return new OpAsgnParseNode(position, receiverNode, valueNode, variableName, operatorName, isLazy(callType));
    }

    public ParseNode newOpConstAsgn(ISourcePosition position, ParseNode lhs, String operatorName, ParseNode rhs) {
        // FIXME: Maybe need to fixup position?
        if (lhs != null) {
            return new OpAsgnConstDeclParseNode(position, lhs, operatorName, rhs);
        } else {
            return new BeginParseNode(position, NilImplicitParseNode.NIL);
        }
    }

    public boolean isLazy(String callType) {
        return "&.".equals(callType);
    }

    public ParseNode new_attrassign(ISourcePosition position, ParseNode receiver, String name, ParseNode args, boolean isLazy) {
        return new AttrAssignParseNode(position, receiver, name, args, isLazy);
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

    public ParseNode new_call(ParseNode receiver, String callType, String name, ParseNode argsNode, ParseNode iter) {
        if (argsNode instanceof BlockPassParseNode) {
            if (iter != null) lexer.compile_error(PID.BLOCK_ARG_AND_BLOCK_GIVEN, "Both block arg and actual block given.");

            BlockPassParseNode blockPass = (BlockPassParseNode) argsNode;
            return new CallParseNode(position(receiver, argsNode), receiver, name, blockPass.getArgsNode(), blockPass, isLazy(callType));
        }

        return new CallParseNode(position(receiver, argsNode), receiver, name, argsNode, iter, isLazy(callType));

    }

    public ParseNode new_call(ParseNode receiver, String name, ParseNode argsNode, ParseNode iter) {
        return new_call(receiver, ".", name, argsNode, iter);
    }

    public Colon2ParseNode new_colon2(ISourcePosition position, ParseNode leftNode, String name) {
        if (leftNode == null) return new Colon2ImplicitParseNode(position, name);

        return new Colon2ConstParseNode(position, leftNode, name);
    }

    public Colon3ParseNode new_colon3(ISourcePosition position, String name) {
        return new Colon3ParseNode(position, name);
    }

    public void frobnicate_fcall_args(FCallParseNode fcall, ParseNode args, ParseNode iter) {
        if (args instanceof BlockPassParseNode) {
            if (iter != null) lexer.compile_error(PID.BLOCK_ARG_AND_BLOCK_GIVEN, "Both block arg and actual block given.");

            BlockPassParseNode blockPass = (BlockPassParseNode) args;
            args = blockPass.getArgsNode();
            iter = blockPass;
        }

        fcall.setArgsNode(args);
        fcall.setIterNode(iter);
    }

    public void fixpos(ParseNode node, ParseNode orig) {
        if (node == null || orig == null) return;

        node.setPosition(orig.getPosition());
    }

    public ParseNode new_fcall(String operation) {
        return new FCallParseNode(lexer.tokline, operation);
    }

    public ParseNode new_super(ISourcePosition position, ParseNode args) {
        if (args != null && args instanceof BlockPassParseNode) {
            return new SuperParseNode(position, ((BlockPassParseNode) args).getArgsNode(), args);
        }
        return new SuperParseNode(position, args);
    }

    /**
    *  Description of the RubyMethod
    */
    public void initTopLocalVariables() {
        DynamicScope scope = configuration.getScope(lexer.getFile());
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

    public DStrParseNode createDStrNode(ISourcePosition position) {
        DStrParseNode dstr = new DStrParseNode(position, lexer.getEncoding());
        if (getConfiguration().isFrozenStringLiteral()) dstr.setFrozen(true);
        return dstr;
    }

    public KeyValuePair<ParseNode, ParseNode> createKeyValue(ParseNode key, ParseNode value) {
        if (key != null && key instanceof StrParseNode) ((StrParseNode) key).setFrozen(true);

        return new KeyValuePair<>(key, value);
    }

    public ParseNode asSymbol(ISourcePosition position, String value) {
        return new SymbolParseNode(position, value, lexer.getEncoding(), lexer.getTokenCR());
    }

    public ParseNode asSymbol(ISourcePosition position, ParseNode value) {
        return value instanceof StrParseNode ? new SymbolParseNode(position, ((StrParseNode) value).getValue()) :
                new DSymbolParseNode(position, (DStrParseNode) value);
    }

    public ParseNode literal_concat(ISourcePosition position, ParseNode head, ParseNode tail) {
        if (head == null) return tail;
        if (tail == null) return head;

        if (head instanceof EvStrParseNode) {
            head = createDStrNode(head.getPosition()).add(head);
        }

        if (lexer.getHeredocIndent() > 0) {
            if (head instanceof StrParseNode) {
                head = createDStrNode(head.getPosition()).add(head);
                return list_append(head, tail);
            } else if (head instanceof DStrParseNode) {
                return list_append(head, tail);
            }
        }

        if (tail instanceof StrParseNode) {
            if (head instanceof StrParseNode) {
                StrParseNode front = (StrParseNode) head;
                // string_contents always makes an empty strnode...which is sometimes valid but
                // never if it ever is in literal_concat.
                if (front.getValue().getRealSize() > 0) {
                    return new StrParseNode(head.getPosition(), front, (StrParseNode) tail);
                } else {
                    return tail;
                }
            }
            head.setPosition(head.getPosition());
            return ((ListParseNode) head).add(tail);

        } else if (tail instanceof DStrParseNode) {
            if (head instanceof StrParseNode) { // Str + oDStr -> Dstr(Str, oDStr.contents)
                DStrParseNode newDStr = new DStrParseNode(head.getPosition(), ((DStrParseNode) tail).getEncoding());
                newDStr.add(head);
                newDStr.addAll(tail);
                if (getConfiguration().isFrozenStringLiteral()) newDStr.setFrozen(true);
                return newDStr;
            }

            return ((ListParseNode) head).addAll(tail);
        }

        // tail must be EvStrParseNode at this point
        if (head instanceof StrParseNode) {

            //Do not add an empty string node
            if(((StrParseNode) head).getValue().length() == 0) {
                head = createDStrNode(head.getPosition());
            } else {
                head = createDStrNode(head.getPosition()).add(head);
            }
        }
        return ((DStrParseNode) head).add(tail);
    }

    public ParseNode newRescueModNode(ParseNode body, ParseNode rescueBody) {
        if (rescueBody == null) rescueBody = NilImplicitParseNode.NIL; // foo rescue () can make null.
        ISourcePosition pos = getPosition(body);

        return new RescueModParseNode(pos, body, new RescueBodyParseNode(pos, null, rescueBody, null));
    }

    public ParseNode newEvStrNode(ISourcePosition position, ParseNode node) {
        if (node instanceof StrParseNode || node instanceof DStrParseNode || node instanceof EvStrParseNode) return node;

        return new EvStrParseNode(position, node);
    }

    public ParseNode new_yield(ISourcePosition position, ParseNode node) {
        if (node != null && node instanceof BlockPassParseNode) {
            lexer.compile_error(PID.BLOCK_ARG_UNEXPECTED, "Block argument should not be given.");
        }

        return new YieldParseNode(position, node);
    }

    public NumericParseNode negateInteger(NumericParseNode integerNode) {
        if (integerNode instanceof FixnumParseNode) {
            FixnumParseNode fixnumNode = (FixnumParseNode) integerNode;

            fixnumNode.setValue(-fixnumNode.getValue());
            return fixnumNode;
        } else if (integerNode instanceof BignumParseNode) {
            BignumParseNode bignumNode = (BignumParseNode) integerNode;

            BigInteger value = bignumNode.getValue().negate();

            // Negating a bignum will make the last negative value of our bignum
            if (value.compareTo(RubyBignum.LONG_MIN) >= 0) {
                return new FixnumParseNode(bignumNode.getPosition(), value.longValue());
            }

            bignumNode.setValue(value);
        }

        return integerNode;
    }

    public FloatParseNode negateFloat(FloatParseNode floatNode) {
        floatNode.setValue(-floatNode.getValue());

        return floatNode;
    }

    public ComplexParseNode negateComplexNode(ComplexParseNode complexNode) {
        complexNode.setNumber(negateNumeric(complexNode.getNumber()));

        return complexNode;
    }

    public RationalParseNode negateRational(RationalParseNode rationalNode) {
        return new RationalParseNode(rationalNode.getPosition(),
                                -rationalNode.getNumerator(),
                                rationalNode.getDenominator());
    }

    private ParseNode checkForNilNode(ParseNode node, ISourcePosition defaultPosition) {
        return (node == null) ? new NilParseNode(defaultPosition) : node;
    }

    public ParseNode new_args(ISourcePosition position, ListParseNode pre, ListParseNode optional, RestArgParseNode rest,
                              ListParseNode post, ArgsTailHolder tail) {
        ArgsParseNode argsNode;
        if (tail == null) {
            argsNode = new ArgsParseNode(position, pre, optional, rest, post, null);
        } else {
            argsNode = new ArgsParseNode(position, pre, optional, rest, post,
                    tail.getKeywordArgs(), tail.getKeywordRestArgNode(), tail.getBlockArg());
        }

        getCurrentScope().setSignature(Signature.from(argsNode));

        return argsNode;
    }

    public ArgsTailHolder new_args_tail(ISourcePosition position, ListParseNode keywordArg,
                                        String keywordRestArgName, BlockArgParseNode blockArg) {
        if (keywordRestArgName == null) return new ArgsTailHolder(position, keywordArg, null, blockArg);

        String restKwargsName = keywordRestArgName;

        int slot = currentScope.exists(restKwargsName);
        if (slot == -1) slot = currentScope.addVariable(restKwargsName);

        KeywordRestArgParseNode keywordRestArg = new KeywordRestArgParseNode(position, restKwargsName, slot);

        return new ArgsTailHolder(position, keywordArg, keywordRestArg, blockArg);
    }

    public ParseNode remove_duplicate_keys(HashParseNode hash) {
        List<ParseNode> encounteredKeys = new ArrayList<>();

        for (KeyValuePair<ParseNode,ParseNode> pair: hash.getPairs()) {
            ParseNode key = pair.getKey();
            if (key == null) continue;
            int index = encounteredKeys.indexOf(key);
            if (index >= 0) {
                warn(ID.AMBIGUOUS_ARGUMENT, hash.getPosition(), "key " + key +
                        " is duplicated and overwritten on line " + (encounteredKeys.get(index).getLine() + 1));
            } else {
                encounteredKeys.add(key);
            }
        }

        return hash;
    }

    public ParseNode newAlias(ISourcePosition position, ParseNode newNode, ParseNode oldNode) {
        return new AliasParseNode(position, newNode, oldNode);
    }

    public ParseNode newUndef(ISourcePosition position, ParseNode nameNode) {
        return new UndefParseNode(position, nameNode);
    }

    /**
     * generate parsing error
     */
    public void yyerror(String message) {
        lexer.compile_error(PID.GRAMMAR_ERROR, message);
    }

    /**
     * generate parsing error
     * @param message text to be displayed.
     * @param expected list of acceptable tokens, if available.
     */
    public void yyerror(String message, String[] expected, String found) {
        lexer.compile_error(PID.GRAMMAR_ERROR, message + ", unexpected " + found + "\n");
    }

    public ISourcePosition getPosition(ISourcePositionHolder start) {
        return start != null ? lexer.getPosition(start.getPosition()) : lexer.getPosition();
    }

    public void warn(ID id, ISourcePosition position, String message, Object... data) {
        warnings.warn(id, position.getFile(), position.getLine(), message);
    }

    public void warning(ID id, ISourcePosition position, String message, Object... data) {
        if (warnings.isVerbose()) warnings.warning(id, position.getFile(), position.getLine(), message);
    }

    // ENEBO: Totally weird naming (in MRI is not allocated and is a local var name) [1.9]
    public boolean is_local_id(String name) {
        return lexer.isIdentifierChar(name.charAt(0));
    }

    // 1.9
    public ListParseNode list_append(ParseNode list, ParseNode item) {
        if (list == null) return new ArrayParseNode(item.getPosition(), item);
        if (!(list instanceof ListParseNode)) return new ArrayParseNode(list.getPosition(), list).add(item);

        return ((ListParseNode) list).add(item);
    }

    // 1.9
    public ParseNode new_bv(String identifier) {
        if (!is_local_id(identifier)) {
            getterIdentifierError(lexer.getPosition(), identifier);
        }
        shadowing_lvar(identifier);

        return arg_var(identifier);
    }

    // 1.9
    public ArgumentParseNode arg_var(String name) {
        StaticScope current = getCurrentScope();

        // Multiple _ arguments are allowed.  To not screw with tons of arity
        // issues in our runtime we will allocate unnamed bogus vars so things
        // still work. MRI does not use name as intern'd value so they don't
        // have this issue.
        if (name == "_") {
            int count = 0;
            while (current.exists(name) >= 0) {
                name = ("_$" + count++).intern();
            }
        }

        return new ArgumentParseNode(lexer.getPosition(), name, current.addVariableThisScope(name));
    }

    public String formal_argument(String identifier) {
        lexer.validateFormalIdentifier(identifier);

        return shadowing_lvar(identifier);
    }

    // 1.9
    public String shadowing_lvar(String name) {
        if (name == "_") return name;

        StaticScope current = getCurrentScope();
        if (current.isBlockScope()) {
            if (current.exists(name) >= 0) yyerror("duplicated argument name");

            if (warnings.isVerbose() && current.isDefined(name) >= 0 &&
                    Options.PARSER_WARN_LOCAL_SHADOWING.load() &&
                    !ParserSupport.skipTruffleRubiniusWarnings(lexer)) {

                warnings.warning(ID.STATEMENT_NOT_REACHED, lexer.getPosition().getFile(), lexer.getPosition().getLine(),
                        "shadowing outer local variable - " + name);
            }
        } else if (current.exists(name) >= 0) {
            yyerror("duplicated argument name");
        }

        return name;
    }

    // 1.9
    public ListParseNode list_concat(ParseNode first, ParseNode second) {
        if (first instanceof ListParseNode) {
            if (second instanceof ListParseNode) {
                return ((ListParseNode) first).addAll((ListParseNode) second);
            } else {
                return ((ListParseNode) first).addAll(second);
            }
        }

        return new ArrayParseNode(first.getPosition(), first).add(second);
    }

    // 1.9
    /**
     * If node is a splat and it is splatting a literal array then return the literal array.
     * Otherwise return null.  This allows grammar to not splat into a Ruby Array if splatting
     * a literal array.
     */
    public ParseNode splat_array(ParseNode node) {
        if (node instanceof SplatParseNode) node = ((SplatParseNode) node).getValue();
        if (node instanceof ArrayParseNode) return node;
        return null;
    }

    // 1.9
    public ParseNode arg_append(ParseNode node1, ParseNode node2) {
        if (node1 == null) return new ArrayParseNode(node2.getPosition(), node2);
        if (node1 instanceof ListParseNode) return ((ListParseNode) node1).add(node2);
        if (node1 instanceof BlockPassParseNode) return arg_append(((BlockPassParseNode) node1).getBodyNode(), node2);
        if (node1 instanceof ArgsPushParseNode) {
            ArgsPushParseNode pushNode = (ArgsPushParseNode) node1;
            ParseNode body = pushNode.getSecondNode();

            return new ArgsCatParseNode(pushNode.getPosition(), pushNode.getFirstNode(),
                    new ArrayParseNode(body.getPosition(), body).add(node2));
        }

        return new ArgsPushParseNode(position(node1, node2), node1, node2);
    }

    // MRI: reg_fragment_check
    public void regexpFragmentCheck(RegexpParseNode end, ByteList value) {
        setRegexpEncoding(end, value);
        try {
            ClassicRegexp.preprocessCheck(configuration.getContext(), value);
        } catch (RaiseException re) {
            compile_error(re.getMessage());
        }
    }        // 1.9 mode overrides to do extra checking...

    private List<Integer> allocateNamedLocals(RegexpParseNode regexpNode) {
        ClassicRegexp pattern = ClassicRegexp.newRegexp(configuration.getContext(), regexpNode.getValue(), regexpNode.getOptions());
        pattern.setLiteral();
        String[] names = pattern.getNames();
        int length = names.length;
        List<Integer> locals = new ArrayList<Integer>();
        StaticScope scope = getCurrentScope();

        for (int i = 0; i < length; i++) {
            // TODO: Pass by non-local-varnamed things but make sure consistent with list we get from regexp
            if (RubyLexer.getKeyword(names[i]) == null && !Character.isUpperCase(names[i].charAt(0))) {
                int slot = scope.isDefined(names[i]);
                if (slot >= 0) {
                    // If verbose and the variable is not just another named capture, warn
                    if (warnings.isVerbose() && !scope.isNamedCapture(slot)) {
                        warn(ID.AMBIGUOUS_ARGUMENT, getPosition(regexpNode), "named capture conflicts a local variable - " + names[i]);
                    }
                    locals.add(slot);
                } else {
                    locals.add(getCurrentScope().addNamedCaptureVariable(names[i]));
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
        if (optionEncoding == USASCII_ENCODING) return 'n';
        if (optionEncoding == org.jcodings.specific.EUCJPEncoding.INSTANCE) return 'e';
        if (optionEncoding == org.jcodings.specific.SJISEncoding.INSTANCE) return 's';
        if (optionEncoding == UTF8_ENCODING) return 'u';

        return ' ';
    }

    public void compile_error(String message) { // mri: rb_compile_error_with_enc
        String line = lexer.getCurrentLine();
        ISourcePosition position = lexer.getPosition();
        String errorMessage = lexer.getFile() + ":" + (position.getLine() + 1) + ": ";

        if (line != null && line.length() > 5) {
            boolean addNewline = message != null && ! message.endsWith("\n");

            message += (addNewline ? "\n" : "") + line;
        }

        throw new RaiseException(getConfiguration().getContext().getCoreExceptions().syntaxError(errorMessage + message, null));
    }

    protected void compileError(Encoding optionEncoding, Encoding encoding) {
        lexer.compile_error(PID.REGEXP_ENCODING_MISMATCH, "regexp encoding option '" + optionsEncodingChar(optionEncoding) +
                "' differs from source encoding '" + encoding + "'");
    }
    
    // MRI: reg_fragment_setenc_gen
    public void setRegexpEncoding(RegexpParseNode end, ByteList value) {
        RegexpOptions options = end.getOptions();
        Encoding optionsEncoding = options.setup(configuration.getContext()) ;

        // Change encoding to one specified by regexp options as long as the string is compatible.
        if (optionsEncoding != null) {
            if (optionsEncoding != value.getEncoding() && !is7BitASCII(value)) {
                compileError(optionsEncoding, value.getEncoding());
            }

            value.setEncoding(optionsEncoding);
        } else if (options.isEncodingNone()) {
            if (value.getEncoding() == ASCII8BIT_ENCODING && !is7BitASCII(value)) {
                compileError(optionsEncoding, value.getEncoding());
            }
            value.setEncoding(ASCII8BIT_ENCODING);
        } else if (lexer.getEncoding() == USASCII_ENCODING) {
            if (!is7BitASCII(value)) {
                value.setEncoding(USASCII_ENCODING); // This will raise later
            } else {
                value.setEncoding(ASCII8BIT_ENCODING);
            }
        }
    }    

    protected void checkRegexpSyntax(ByteList value, RegexpOptions options) {
        final String stringValue = value.toString();
        // Joni doesn't support these modifiers - but we can fix up in some cases - let the error delay until we try that
        if (stringValue.startsWith("(?u)") || stringValue.startsWith("(?a)") || stringValue.startsWith("(?d)"))
            return;

        try {
            // This is only for syntax checking but this will as a side-effect create an entry in the regexp cache.
            ClassicRegexp.newRegexpParser(getConfiguration().getContext(), value, (RegexpOptions)options.clone());
        } catch (RaiseException re) {
            compile_error(re.getMessage());
        }
    }

    public ParseNode newRegexpNode(ISourcePosition position, ParseNode contents, RegexpParseNode end) {
        RegexpOptions options = end.getOptions();
        Encoding encoding = lexer.getEncoding();

        if (contents == null) {
            ByteList newValue = ByteList.create("");
            if (encoding != null) {
                newValue.setEncoding(encoding);
            }

            regexpFragmentCheck(end, newValue);
            return new RegexpParseNode(position, newValue, options.withoutOnce());
        } else if (contents instanceof StrParseNode) {
            ByteList meat = (ByteList) ((StrParseNode) contents).getValue().clone();
            regexpFragmentCheck(end, meat);
            checkRegexpSyntax(meat, options.withoutOnce());
            return new RegexpParseNode(contents.getPosition(), meat, options.withoutOnce());
        } else if (contents instanceof DStrParseNode) {
            DStrParseNode dStrNode = (DStrParseNode) contents;
            
            for (int i = 0; i < dStrNode.size(); i++) {
                ParseNode fragment = dStrNode.get(i);
                if (fragment instanceof StrParseNode) {
                    ByteList frag = ((StrParseNode) fragment).getValue();
                    regexpFragmentCheck(end, frag);
//                    if (!lexer.isOneEight()) encoding = frag.getEncoding();
                }
            }
            
            DRegexpParseNode dRegexpNode = new DRegexpParseNode(position, options, encoding);
            dRegexpNode.add(new StrParseNode(contents.getPosition(), createMaster(options)));
            dRegexpNode.addAll(dStrNode);
            return dRegexpNode;
        }

        // EvStrParseNode: #{val}: no fragment check, but at least set encoding
        ByteList master = createMaster(options);
        regexpFragmentCheck(end, master);
        encoding = master.getEncoding();
        DRegexpParseNode node = new DRegexpParseNode(position, options, encoding);
        node.add(new StrParseNode(contents.getPosition(), master));
        node.add(contents);
        return node;
    }
    
    // Create the magical empty 'master' string which will be encoded with
    // regexp options encoding so dregexps can end up starting with the
    // right encoding.
    private ByteList createMaster(RegexpOptions options) {
        Encoding encoding = options.setup(configuration.getContext());

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
    
    public KeywordArgParseNode keyword_arg(ISourcePosition position, AssignableParseNode assignable) {
        return new KeywordArgParseNode(position, assignable);
    }
    
    public NumericParseNode negateNumeric(NumericParseNode node) {
        switch (node.getNodeType()) {
            case FIXNUMNODE:
            case BIGNUMNODE:
                return negateInteger(node);
            case COMPLEXNODE:
                return negateComplexNode((ComplexParseNode) node);
            case FLOATNODE:
                return negateFloat((FloatParseNode) node);
            case RATIONALNODE:
                return negateRational((RationalParseNode) node);
        }
        
        yyerror("Invalid or unimplemented numeric to negate: " + node.toString());
        return null;
    }
    
    public ParseNode new_defined(ISourcePosition position, ParseNode something) {
        return new DefinedParseNode(position, something);
    }

    public String internalId() {
        return "";
    }

    public static boolean skipTruffleRubiniusWarnings(RubyLexer lexer) {
        return lexer.getFile().startsWith(Options.TRUFFLE_CORE_LOAD_PATH.load());
    }
}
