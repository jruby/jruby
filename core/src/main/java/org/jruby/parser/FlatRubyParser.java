// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "FlatRubyParser.y"
/***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2008-2017 Thomas E Enebo <enebo@acm.org>
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

import org.jruby.ast.ArgsNode;
import org.jruby.ast.ArgumentNode;
import org.jruby.ast.ArrayNode;
import org.jruby.ast.AssignableNode;
import org.jruby.ast.BackRefNode;
import org.jruby.ast.BeginNode;
import org.jruby.ast.BlockAcceptingNode;
import org.jruby.ast.BlockArgNode;
import org.jruby.ast.BlockNode;
import org.jruby.ast.BlockPassNode;
import org.jruby.ast.BreakNode;
import org.jruby.ast.ClassNode;
import org.jruby.ast.ClassVarNode;
import org.jruby.ast.ClassVarAsgnNode;
import org.jruby.ast.Colon3Node;
import org.jruby.ast.ConstNode;
import org.jruby.ast.ConstDeclNode;
import org.jruby.ast.DStrNode;
import org.jruby.ast.DSymbolNode;
import org.jruby.ast.DXStrNode;
import org.jruby.ast.DefnNode;
import org.jruby.ast.DefsNode;
import org.jruby.ast.DotNode;
import org.jruby.ast.EncodingNode;
import org.jruby.ast.EnsureNode;
import org.jruby.ast.EvStrNode;
import org.jruby.ast.FalseNode;
import org.jruby.ast.FileNode;
import org.jruby.ast.FCallNode;
import org.jruby.ast.FixnumNode;
import org.jruby.ast.FloatNode;
import org.jruby.ast.ForNode;
import org.jruby.ast.GlobalAsgnNode;
import org.jruby.ast.GlobalVarNode;
import org.jruby.ast.HashNode;
import org.jruby.ast.IfNode;
import org.jruby.ast.InstAsgnNode;
import org.jruby.ast.InstVarNode;
import org.jruby.ast.IterNode;
import org.jruby.ast.LambdaNode;
import org.jruby.ast.ListNode;
import org.jruby.ast.LiteralNode;
import org.jruby.ast.ModuleNode;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.NextNode;
import org.jruby.ast.NilImplicitNode;
import org.jruby.ast.NilNode;
import org.jruby.ast.Node;
import org.jruby.ast.NonLocalControlFlowNode;
import org.jruby.ast.NumericNode;
import org.jruby.ast.OpAsgnAndNode;
import org.jruby.ast.OpAsgnOrNode;
import org.jruby.ast.OptArgNode;
import org.jruby.ast.PostExeNode;
import org.jruby.ast.PreExe19Node;
import org.jruby.ast.RationalNode;
import org.jruby.ast.RedoNode;
import org.jruby.ast.RegexpNode;
import org.jruby.ast.RequiredKeywordArgumentValueNode;
import org.jruby.ast.RescueBodyNode;
import org.jruby.ast.RescueNode;
import org.jruby.ast.RestArgNode;
import org.jruby.ast.RetryNode;
import org.jruby.ast.ReturnNode;
import org.jruby.ast.SClassNode;
import org.jruby.ast.SelfNode;
import org.jruby.ast.StarNode;
import org.jruby.ast.StrNode;
import org.jruby.ast.TrueNode;
import org.jruby.ast.UnnamedRestArgNode;
import org.jruby.ast.UntilNode;
import org.jruby.ast.VAliasNode;
import org.jruby.ast.WhileNode;
import org.jruby.ast.XStrNode;
import org.jruby.ast.YieldNode;
import org.jruby.ast.ZArrayNode;
import org.jruby.ast.ZSuperNode;
import org.jruby.ast.types.ILiteralNode;
import org.jruby.common.IRubyWarnings;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.ISourcePositionHolder;
import org.jruby.lexer.LexerSource;
import org.jruby.lexer.yacc.RubyLexer;
import org.jruby.lexer.yacc.StrTerm;
import org.jruby.lexer.yacc.SyntaxException.PID;
import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;
import org.jruby.util.StringSupport;
import static org.jruby.lexer.LexingCommon.EXPR_BEG;
import static org.jruby.lexer.LexingCommon.EXPR_FITEM;
import static org.jruby.lexer.LexingCommon.EXPR_FNAME;
import static org.jruby.lexer.LexingCommon.EXPR_ENDFN;
import static org.jruby.lexer.LexingCommon.EXPR_ENDARG;
import static org.jruby.lexer.LexingCommon.EXPR_END;
import static org.jruby.lexer.LexingCommon.EXPR_LABEL;
 
public class FlatRubyParser extends RubyParser {
    public FlatRubyParser(LexerSource source, IRubyWarnings warnings) {
        super(source, warnings);
    }

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
    */
  public Object yyparse (RubyLexer yyLex) throws java.io.IOException {
    if (yyMax <= 0) yyMax = 256;			// initial size
    int yyState = 0, yyStates[] = new int[yyMax];	// state stack
    Object yyVal = null, yyVals[] = new Object[yyMax];	// value stack
    int yyToken = -1;					// current input
    int yyErrorFlag = 0;				// #tokens to shift

    yyLoop: for (int yyTop = 0;; ++ yyTop) {
      if (yyTop >= yyStates.length) {			// dynamically increase
        int[] i = new int[yyStates.length+yyMax];
        System.arraycopy(yyStates, 0, i, 0, yyStates.length);
        yyStates = i;
        Object[] o = new Object[yyVals.length+yyMax];
        System.arraycopy(yyVals, 0, o, 0, yyVals.length);
        yyVals = o;
      }
      yyStates[yyTop] = yyState;
      yyVals[yyTop] = yyVal;
      if (yydebug != null) yydebug.push(yyState, yyVal);

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
            yyToken = yyLex.nextToken();
            if (yydebug != null)
              yydebug.lex(yyState, yyToken, yyName(yyToken), yyLex.value());
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
            if (yydebug != null)
              yydebug.shift(yyState, yyTable[yyN], yyErrorFlag-1);
            yyState = yyTable[yyN];		// shift to yyN
            yyVal = yyLex.value();
            yyToken = -1;
            if (yyErrorFlag > 0) -- yyErrorFlag;
            continue yyLoop;
          }
          if ((yyN = yyRindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken)
            yyN = yyTable[yyN];			// reduce (yyN)
          else
            switch (yyErrorFlag) {
  
            case 0:
              support.yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
              if (yydebug != null) yydebug.error("syntax error");
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  if (yydebug != null)
                    yydebug.shift(yyStates[yyTop], yyTable[yyN], 3);
                  yyState = yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
                if (yydebug != null) yydebug.pop(yyStates[yyTop]);
              } while (-- yyTop >= 0);
              if (yydebug != null) yydebug.reject();
              support.yyerror("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                if (yydebug != null) yydebug.reject();
                support.yyerror("irrecoverable syntax error at end-of-file");
              }
              if (yydebug != null)
                yydebug.discard(yyState, yyToken, yyName(yyToken),
  							yyLex.value());
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        if (yydebug != null)
          yydebug.reduce(yyState, yyStates[yyV-1], yyN, yyRule[yyN], yyLen[yyN]);
//        ParserState state = states[yyN];
//        if (state == null) {
//            yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
//        } else {
//            yyVal = state.execute(support, lexer, yyVal, yyVals, yyTop);
//        }
        switch (yyN) {
// ACTIONS_BEGIN
case 1:
					// line 321 "FlatRubyParser.y"
  {
                  lexer.setState(EXPR_BEG);
                  support.initTopLocalVariables();
              }
  break;
case 2:
					// line 324 "FlatRubyParser.y"
  {
  /* ENEBO: Removed !compile_for_eval which probably is to reduce warnings*/
                  if (((Node)yyVals[0+yyTop]) != null) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(((BlockNode)yyVals[0+yyTop]).getLast());
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.addRootNode(((Node)yyVals[0+yyTop])));
              }
  break;
case 3:
					// line 337 "FlatRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                      support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 5:
					// line 345 "FlatRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop])));
              }
  break;
case 6:
					// line 348 "FlatRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop]))));
              }
  break;
case 7:
					// line 351 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
              }
  break;
case 9:
					// line 356 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("BEGIN in method");
                    }
              }
  break;
case 10:
					// line 360 "FlatRubyParser.y"
  {
                    support.getResult().addBeginNode(new PreExe19Node(((ISourcePosition)yyVals[-4+yyTop]), support.getCurrentScope(), ((Node)yyVals[-1+yyTop])));
                    yyVal = null;
              }
  break;
case 11:
					// line 365 "FlatRubyParser.y"
  {
                  Node node = ((Node)yyVals[-3+yyTop]);

                  if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
                      node = new RescueNode(support.getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                  } else if (((Node)yyVals[-1+yyTop]) != null) {
                      support.warn(ID.ELSE_WITHOUT_RESCUE, support.getPosition(((Node)yyVals[-3+yyTop])), "else without rescue is useless");
                      node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                  }
                  if (((Node)yyVals[0+yyTop]) != null) {
                      node = new EnsureNode(support.getPosition(((Node)yyVals[-3+yyTop])), support.makeNullNil(node), ((Node)yyVals[0+yyTop]));
                  }

                  support.fixpos(node, ((Node)yyVals[-3+yyTop]));
                  yyVal = node;
                }
  break;
case 12:
					// line 382 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                        support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 14:
					// line 390 "FlatRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop])));
                }
  break;
case 15:
					// line 393 "FlatRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), support.getPosition(((Node)yyVals[0+yyTop]))));
                }
  break;
case 16:
					// line 396 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 17:
					// line 400 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 18:
					// line 404 "FlatRubyParser.y"
  {
                   support.yyerror("BEGIN is permitted only at toplevel");
                }
  break;
case 19:
					// line 406 "FlatRubyParser.y"
  {
                    yyVal = new BeginNode(((ISourcePosition)yyVals[-4+yyTop]), support.makeNullNil(((Node)yyVals[-3+yyTop])));
                }
  break;
case 20:
					// line 410 "FlatRubyParser.y"
  {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
                }
  break;
case 21:
					// line 412 "FlatRubyParser.y"
  {
                    yyVal = support.newAlias(((ISourcePosition)yyVals[-3+yyTop]), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 22:
					// line 415 "FlatRubyParser.y"
  {
                    yyVal = new VAliasNode(((ISourcePosition)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 23:
					// line 418 "FlatRubyParser.y"
  {
                    yyVal = new VAliasNode(((ISourcePosition)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), "$" + ((BackRefNode)yyVals[0+yyTop]).getType());
                }
  break;
case 24:
					// line 421 "FlatRubyParser.y"
  {
                    support.yyerror("can't make alias for the number variables");
                }
  break;
case 25:
					// line 424 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 26:
					// line 427 "FlatRubyParser.y"
  {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 27:
					// line 431 "FlatRubyParser.y"
  {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                    support.fixpos(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 28:
					// line 435 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 29:
					// line 442 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new UntilNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 30:
					// line 449 "FlatRubyParser.y"
  {
                    yyVal = support.newRescueModNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 31:
					// line 452 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.warn(ID.END_IN_METHOD, ((ISourcePosition)yyVals[-3+yyTop]), "END in method; use at_exit");
                    }
                    yyVal = new PostExeNode(((ISourcePosition)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 33:
					// line 459 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                }
  break;
case 34:
					// line 464 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    ISourcePosition pos = ((AssignableNode)yyVals[-2+yyTop]).getPosition();
                    String asgnOp = ((String)yyVals[-1+yyTop]);
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
                }
  break;
case 35:
					// line 481 "FlatRubyParser.y"
  {
  /* FIXME: arg_concat logic missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(((Node)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 36:
					// line 485 "FlatRubyParser.y"
  {
                    yyVal = support.newOpAsgn(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 37:
					// line 488 "FlatRubyParser.y"
  {
                    yyVal = support.newOpAsgn(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 38:
					// line 491 "FlatRubyParser.y"
  {
                    ISourcePosition pos = ((Node)yyVals[-4+yyTop]).getPosition();
                    yyVal = support.newOpConstAsgn(pos, support.new_colon2(pos, ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop])), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 39:
					// line 496 "FlatRubyParser.y"
  {
                    yyVal = support.newOpAsgn(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 40:
					// line 499 "FlatRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                }
  break;
case 41:
					// line 502 "FlatRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 42:
					// line 505 "FlatRubyParser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                    ((MultipleAsgnNode)yyVals[-2+yyTop]).setPosition(support.getPosition(((MultipleAsgnNode)yyVals[-2+yyTop])));
                }
  break;
case 44:
					// line 512 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 45:
					// line 516 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 47:
					// line 523 "FlatRubyParser.y"
  {
                    yyVal = support.newAndNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 48:
					// line 526 "FlatRubyParser.y"
  {
                    yyVal = support.newOrNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 49:
					// line 529 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
                }
  break;
case 50:
					// line 532 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
                }
  break;
case 52:
					// line 537 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                }
  break;
case 56:
					// line 547 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 57:
					// line 552 "FlatRubyParser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 58:
					// line 554 "FlatRubyParser.y"
  {
                    yyVal = new IterNode(((ISourcePosition)yyVals[-4+yyTop]), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 59:
					// line 559 "FlatRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[0+yyTop]));
                }
  break;
case 60:
					// line 564 "FlatRubyParser.y"
  {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop]);
                }
  break;
case 61:
					// line 568 "FlatRubyParser.y"
  {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
                    yyVal = ((FCallNode)yyVals[-2+yyTop]);
                }
  break;
case 62:
					// line 572 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 63:
					// line 575 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop])); 
                }
  break;
case 64:
					// line 578 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 581 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
                }
  break;
case 66:
					// line 584 "FlatRubyParser.y"
  {
                    yyVal = support.new_super(((ISourcePosition)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 587 "FlatRubyParser.y"
  {
                    yyVal = support.new_yield(((ISourcePosition)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 68:
					// line 590 "FlatRubyParser.y"
  {
                    yyVal = new ReturnNode(((ISourcePosition)yyVals[-1+yyTop]), support.ret_args(((Node)yyVals[0+yyTop]), ((ISourcePosition)yyVals[-1+yyTop])));
                }
  break;
case 69:
					// line 593 "FlatRubyParser.y"
  {
                    yyVal = new BreakNode(((ISourcePosition)yyVals[-1+yyTop]), support.ret_args(((Node)yyVals[0+yyTop]), ((ISourcePosition)yyVals[-1+yyTop])));
                }
  break;
case 70:
					// line 596 "FlatRubyParser.y"
  {
                    yyVal = new NextNode(((ISourcePosition)yyVals[-1+yyTop]), support.ret_args(((Node)yyVals[0+yyTop]), ((ISourcePosition)yyVals[-1+yyTop])));
                }
  break;
case 72:
					// line 602 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 73:
					// line 607 "FlatRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[0+yyTop]);
                }
  break;
case 74:
					// line 610 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ISourcePosition)yyVals[-2+yyTop]), support.newArrayNode(((ISourcePosition)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])), null, null);
                }
  break;
case 75:
					// line 615 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop]).getPosition(), ((ListNode)yyVals[0+yyTop]), null, null);
                }
  break;
case 76:
					// line 618 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null, null);
                }
  break;
case 77:
					// line 621 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), (ListNode) null);
                }
  break;
case 78:
					// line 624 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 79:
					// line 627 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), new StarNode(lexer.getPosition()), null);
                }
  break;
case 80:
					// line 630 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 81:
					// line 633 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[0+yyTop]).getPosition(), null, ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 82:
					// line 636 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((Node)yyVals[-2+yyTop]).getPosition(), null, ((Node)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 83:
					// line 639 "FlatRubyParser.y"
  {
                      yyVal = new MultipleAsgnNode(lexer.getPosition(), null, new StarNode(lexer.getPosition()), null);
                }
  break;
case 84:
					// line 642 "FlatRubyParser.y"
  {
                      yyVal = new MultipleAsgnNode(lexer.getPosition(), null, new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 86:
					// line 647 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 87:
					// line 652 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 88:
					// line 655 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 89:
					// line 660 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 90:
					// line 663 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 91:
					// line 667 "FlatRubyParser.y"
  {
                    yyVal = support.assignableLabelOrIdentifier(((String)yyVals[0+yyTop]), null);
                }
  break;
case 92:
					// line 670 "FlatRubyParser.y"
  {
                   yyVal = new InstAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 93:
					// line 673 "FlatRubyParser.y"
  {
                   yyVal = new GlobalAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 94:
					// line 676 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), null, NilImplicitNode.NIL);
                }
  break;
case 95:
					// line 681 "FlatRubyParser.y"
  {
                    yyVal = new ClassVarAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 96:
					// line 684 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
                }
  break;
case 97:
					// line 688 "FlatRubyParser.y"
  {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
                }
  break;
case 98:
					// line 692 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
                }
  break;
case 99:
					// line 696 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
                }
  break;
case 100:
					// line 700 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                }
  break;
case 101:
					// line 704 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                }
  break;
case 102:
					// line 708 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                }
  break;
case 103:
					// line 712 "FlatRubyParser.y"
  {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 104:
					// line 715 "FlatRubyParser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 105:
					// line 718 "FlatRubyParser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 106:
					// line 721 "FlatRubyParser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 107:
					// line 724 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop])), NilImplicitNode.NIL);
                }
  break;
case 108:
					// line 733 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = lexer.getPosition();

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, ((String)yyVals[0+yyTop])), NilImplicitNode.NIL);
                }
  break;
case 109:
					// line 742 "FlatRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                }
  break;
case 110:
					// line 746 "FlatRubyParser.y"
  {
                    yyVal = support.assignableLabelOrIdentifier(((String)yyVals[0+yyTop]), null);
                }
  break;
case 111:
					// line 749 "FlatRubyParser.y"
  {
                   yyVal = new InstAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 112:
					// line 752 "FlatRubyParser.y"
  {
                   yyVal = new GlobalAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 113:
					// line 755 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), null, NilImplicitNode.NIL);
                }
  break;
case 114:
					// line 760 "FlatRubyParser.y"
  {
                    yyVal = new ClassVarAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 115:
					// line 763 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
                }
  break;
case 116:
					// line 767 "FlatRubyParser.y"
  {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
                }
  break;
case 117:
					// line 771 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
                }
  break;
case 118:
					// line 775 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
                }
  break;
case 119:
					// line 779 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                }
  break;
case 120:
					// line 783 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                }
  break;
case 121:
					// line 787 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                }
  break;
case 122:
					// line 791 "FlatRubyParser.y"
  {
                    yyVal = support.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 123:
					// line 794 "FlatRubyParser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 124:
					// line 797 "FlatRubyParser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 125:
					// line 800 "FlatRubyParser.y"
  {
                    yyVal = support.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 126:
					// line 803 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = support.getPosition(((Node)yyVals[-2+yyTop]));

                    yyVal = new ConstDeclNode(position, null, support.new_colon2(position, ((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop])), NilImplicitNode.NIL);
                }
  break;
case 127:
					// line 812 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("dynamic constant assignment");
                    }

                    ISourcePosition position = lexer.getPosition();

                    yyVal = new ConstDeclNode(position, null, support.new_colon3(position, ((String)yyVals[0+yyTop])), NilImplicitNode.NIL);
                }
  break;
case 128:
					// line 821 "FlatRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                }
  break;
case 129:
					// line 825 "FlatRubyParser.y"
  {
                    support.yyerror("class/module name must be CONSTANT");
                }
  break;
case 131:
					// line 830 "FlatRubyParser.y"
  {
                    yyVal = support.new_colon3(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 132:
					// line 833 "FlatRubyParser.y"
  {
                    yyVal = support.new_colon2(lexer.getPosition(), null, ((String)yyVals[0+yyTop]));
                }
  break;
case 133:
					// line 836 "FlatRubyParser.y"
  {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 137:
					// line 842 "FlatRubyParser.y"
  {
                   lexer.setState(EXPR_ENDFN);
                   yyVal = ((String)yyVals[0+yyTop]);
               }
  break;
case 138:
					// line 846 "FlatRubyParser.y"
  {
                   lexer.setState(EXPR_ENDFN);
                   yyVal = ((String)yyVals[0+yyTop]);
               }
  break;
case 139:
					// line 852 "FlatRubyParser.y"
  {
                    yyVal = new LiteralNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 140:
					// line 855 "FlatRubyParser.y"
  {
                    yyVal = new LiteralNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 141:
					// line 860 "FlatRubyParser.y"
  {
                    yyVal = ((LiteralNode)yyVals[0+yyTop]);
                }
  break;
case 142:
					// line 863 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 143:
					// line 867 "FlatRubyParser.y"
  {
                    yyVal = support.newUndef(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 144:
					// line 870 "FlatRubyParser.y"
  {
                    lexer.setState(EXPR_FNAME|EXPR_FITEM);
                }
  break;
case 145:
					// line 872 "FlatRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), support.newUndef(((Node)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[0+yyTop])));
                }
  break;
case 176:
					// line 884 "FlatRubyParser.y"
  {
                    yyVal = "__LINE__";
                }
  break;
case 177:
					// line 887 "FlatRubyParser.y"
  {
                    yyVal = "__FILE__";
                }
  break;
case 178:
					// line 890 "FlatRubyParser.y"
  {
                    yyVal = "__ENCODING__";
                }
  break;
case 179:
					// line 893 "FlatRubyParser.y"
  {
                    yyVal = "BEGIN";
                }
  break;
case 180:
					// line 896 "FlatRubyParser.y"
  {
                    yyVal = "END";
                }
  break;
case 181:
					// line 899 "FlatRubyParser.y"
  {
                    yyVal = "alias";
                }
  break;
case 182:
					// line 902 "FlatRubyParser.y"
  {
                    yyVal = "and";
                }
  break;
case 183:
					// line 905 "FlatRubyParser.y"
  {
                    yyVal = "begin";
                }
  break;
case 184:
					// line 908 "FlatRubyParser.y"
  {
                    yyVal = "break";
                }
  break;
case 185:
					// line 911 "FlatRubyParser.y"
  {
                    yyVal = "case";
                }
  break;
case 186:
					// line 914 "FlatRubyParser.y"
  {
                    yyVal = "class";
                }
  break;
case 187:
					// line 917 "FlatRubyParser.y"
  {
                    yyVal = "def";
                }
  break;
case 188:
					// line 920 "FlatRubyParser.y"
  {
                    yyVal = "defined?";
                }
  break;
case 189:
					// line 923 "FlatRubyParser.y"
  {
                    yyVal = "do";
                }
  break;
case 190:
					// line 926 "FlatRubyParser.y"
  {
                    yyVal = "else";
                }
  break;
case 191:
					// line 929 "FlatRubyParser.y"
  {
                    yyVal = "elsif";
                }
  break;
case 192:
					// line 932 "FlatRubyParser.y"
  {
                    yyVal = "end";
                }
  break;
case 193:
					// line 935 "FlatRubyParser.y"
  {
                    yyVal = "ensure";
                }
  break;
case 194:
					// line 938 "FlatRubyParser.y"
  {
                    yyVal = "false";
                }
  break;
case 195:
					// line 941 "FlatRubyParser.y"
  {
                    yyVal = "for";
                }
  break;
case 196:
					// line 944 "FlatRubyParser.y"
  {
                    yyVal = "in";
                }
  break;
case 197:
					// line 947 "FlatRubyParser.y"
  {
                    yyVal = "module";
                }
  break;
case 198:
					// line 950 "FlatRubyParser.y"
  {
                    yyVal = "next";
                }
  break;
case 199:
					// line 953 "FlatRubyParser.y"
  {
                    yyVal = "nil";
                }
  break;
case 200:
					// line 956 "FlatRubyParser.y"
  {
                    yyVal = "not";
                }
  break;
case 201:
					// line 959 "FlatRubyParser.y"
  {
                    yyVal = "or";
                }
  break;
case 202:
					// line 962 "FlatRubyParser.y"
  {
                    yyVal = "redo";
                }
  break;
case 203:
					// line 965 "FlatRubyParser.y"
  {
                    yyVal = "rescue";
                }
  break;
case 204:
					// line 968 "FlatRubyParser.y"
  {
                    yyVal = "retry";
                }
  break;
case 205:
					// line 971 "FlatRubyParser.y"
  {
                    yyVal = "return";
                }
  break;
case 206:
					// line 974 "FlatRubyParser.y"
  {
                    yyVal = "self";
                }
  break;
case 207:
					// line 977 "FlatRubyParser.y"
  {
                    yyVal = "super";
                }
  break;
case 208:
					// line 980 "FlatRubyParser.y"
  {
                    yyVal = "then";
                }
  break;
case 209:
					// line 983 "FlatRubyParser.y"
  {
                    yyVal = "true";
                }
  break;
case 210:
					// line 986 "FlatRubyParser.y"
  {
                    yyVal = "undef";
                }
  break;
case 211:
					// line 989 "FlatRubyParser.y"
  {
                    yyVal = "when";
                }
  break;
case 212:
					// line 992 "FlatRubyParser.y"
  {
                    yyVal = "yield";
                }
  break;
case 213:
					// line 995 "FlatRubyParser.y"
  {
                    yyVal = "if";
                }
  break;
case 214:
					// line 998 "FlatRubyParser.y"
  {
                    yyVal = "unless";
                }
  break;
case 215:
					// line 1001 "FlatRubyParser.y"
  {
                    yyVal = "while";
                }
  break;
case 216:
					// line 1004 "FlatRubyParser.y"
  {
                    yyVal = "until";
                }
  break;
case 217:
					// line 1007 "FlatRubyParser.y"
  {
                    yyVal = "rescue";
                }
  break;
case 218:
					// line 1011 "FlatRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    /* FIXME: Consider fixing node_assign itself rather than single case*/
                    ((Node)yyVal).setPosition(support.getPosition(((Node)yyVals[-2+yyTop])));
                }
  break;
case 219:
					// line 1016 "FlatRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), support.newRescueModNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 220:
					// line 1019 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    ISourcePosition pos = ((AssignableNode)yyVals[-2+yyTop]).getPosition();
                    String asgnOp = ((String)yyVals[-1+yyTop]);
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-2+yyTop])), ((AssignableNode)yyVals[-2+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-2+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-2+yyTop])), asgnOp, ((Node)yyVals[0+yyTop])));
                        ((AssignableNode)yyVals[-2+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-2+yyTop]);
                    }
                }
  break;
case 221:
					// line 1036 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    Node rescue = support.newRescueModNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));

                    ISourcePosition pos = ((AssignableNode)yyVals[-4+yyTop]).getPosition();
                    String asgnOp = ((String)yyVals[-3+yyTop]);
                    if (asgnOp.equals("||")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(rescue);
                        yyVal = new OpAsgnOrNode(pos, support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else if (asgnOp.equals("&&")) {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(rescue);
                        yyVal = new OpAsgnAndNode(pos, support.gettable2(((AssignableNode)yyVals[-4+yyTop])), ((AssignableNode)yyVals[-4+yyTop]));
                    } else {
                        ((AssignableNode)yyVals[-4+yyTop]).setValueNode(support.getOperatorCallNode(support.gettable2(((AssignableNode)yyVals[-4+yyTop])), asgnOp, rescue));
                        ((AssignableNode)yyVals[-4+yyTop]).setPosition(pos);
                        yyVal = ((AssignableNode)yyVals[-4+yyTop]);
                    }
                }
  break;
case 222:
					// line 1054 "FlatRubyParser.y"
  {
  /* FIXME: arg_concat missing for opt_call_args*/
                    yyVal = support.new_opElementAsgnNode(((Node)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 223:
					// line 1058 "FlatRubyParser.y"
  {
                    yyVal = support.newOpAsgn(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 224:
					// line 1061 "FlatRubyParser.y"
  {
                    yyVal = support.newOpAsgn(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 225:
					// line 1064 "FlatRubyParser.y"
  {
                    yyVal = support.newOpAsgn(support.getPosition(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 226:
					// line 1067 "FlatRubyParser.y"
  {
                    ISourcePosition pos = support.getPosition(((Node)yyVals[-4+yyTop]));
                    yyVal = support.newOpConstAsgn(pos, support.new_colon2(pos, ((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop])), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 227:
					// line 1071 "FlatRubyParser.y"
  {
                    ISourcePosition pos = lexer.getPosition();
                    yyVal = support.newOpConstAsgn(pos, new Colon3Node(pos, ((String)yyVals[-3+yyTop])), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 228:
					// line 1075 "FlatRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                }
  break;
case 229:
					// line 1078 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));
    
                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.makeNullNil(((Node)yyVals[-2+yyTop])), support.makeNullNil(((Node)yyVals[0+yyTop])), false, isLiteral);
                }
  break;
case 230:
					// line 1085 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    support.checkExpression(((Node)yyVals[0+yyTop]));

                    boolean isLiteral = ((Node)yyVals[-2+yyTop]) instanceof FixnumNode && ((Node)yyVals[0+yyTop]) instanceof FixnumNode;
                    yyVal = new DotNode(support.getPosition(((Node)yyVals[-2+yyTop])), support.makeNullNil(((Node)yyVals[-2+yyTop])), support.makeNullNil(((Node)yyVals[0+yyTop])), true, isLiteral);
                }
  break;
case 231:
					// line 1092 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 232:
					// line 1095 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 233:
					// line 1098 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 234:
					// line 1101 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 235:
					// line 1104 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 236:
					// line 1107 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 237:
					// line 1110 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode(((NumericNode)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]), lexer.getPosition()), "-@");
                }
  break;
case 238:
					// line 1113 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
                }
  break;
case 239:
					// line 1116 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
                }
  break;
case 240:
					// line 1119 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 241:
					// line 1122 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 242:
					// line 1125 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 243:
					// line 1128 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 244:
					// line 1131 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 245:
					// line 1134 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 246:
					// line 1137 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 247:
					// line 1140 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 248:
					// line 1143 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 249:
					// line 1146 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 250:
					// line 1149 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!=", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 251:
					// line 1152 "FlatRubyParser.y"
  {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                  /* ENEBO
                        $$ = match_op($1, $3);
                        if (nd_type($1) == NODE_LIT && TYPE($1->nd_lit) == T_REGEXP) {
                            $$ = reg_named_capture_assign($1->nd_lit, $$);
                        }
                  */
                }
  break;
case 252:
					// line 1161 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "!~", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 253:
					// line 1164 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[0+yyTop])), "!");
                }
  break;
case 254:
					// line 1167 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                }
  break;
case 255:
					// line 1170 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 256:
					// line 1173 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]), lexer.getPosition());
                }
  break;
case 257:
					// line 1176 "FlatRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 258:
					// line 1179 "FlatRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 259:
					// line 1182 "FlatRubyParser.y"
  {
                    yyVal = support.new_defined(((ISourcePosition)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 260:
					// line 1185 "FlatRubyParser.y"
  {
                    yyVal = new IfNode(support.getPosition(((Node)yyVals[-5+yyTop])), support.getConditionNode(((Node)yyVals[-5+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 261:
					// line 1188 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 262:
					// line 1192 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.makeNullNil(((Node)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1198 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 265:
					// line 1201 "FlatRubyParser.y"
  {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop])));
                }
  break;
case 266:
					// line 1204 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop]).getPosition(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop])));
                }
  break;
case 267:
					// line 1208 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    if (yyVal != null) ((Node)yyVal).setPosition(((ISourcePosition)yyVals[-2+yyTop]));
                }
  break;
case 272:
					// line 1217 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 273:
					// line 1220 "FlatRubyParser.y"
  {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop])));
                }
  break;
case 274:
					// line 1223 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop]).getPosition(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop])));
                }
  break;
case 275:
					// line 1229 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 276:
					// line 1232 "FlatRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((Node)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 277:
					// line 1235 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((HashNode)yyVals[-1+yyTop]).getPosition(), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 278:
					// line 1239 "FlatRubyParser.y"
  {
                    yyVal = support.arg_append(((Node)yyVals[-3+yyTop]), support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 279:
					// line 1243 "FlatRubyParser.y"
  {
                }
  break;
case 280:
					// line 1246 "FlatRubyParser.y"
  {
                    yyVal = Long.valueOf(lexer.getCmdArgumentState().getStack());
                    lexer.getCmdArgumentState().begin();
                }
  break;
case 281:
					// line 1249 "FlatRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 282:
					// line 1254 "FlatRubyParser.y"
  {
                    yyVal = new BlockPassNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 283:
					// line 1258 "FlatRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 285:
					// line 1264 "FlatRubyParser.y"
  { /* ArrayNode*/
                    ISourcePosition pos = ((Node)yyVals[0+yyTop]) == null ? lexer.getPosition() : ((Node)yyVals[0+yyTop]).getPosition();
                    yyVal = support.newArrayNode(pos, ((Node)yyVals[0+yyTop]));
                }
  break;
case 286:
					// line 1268 "FlatRubyParser.y"
  { /* SplatNode*/
                    yyVal = support.newSplatNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 287:
					// line 1271 "FlatRubyParser.y"
  { /* ArgsCatNode, SplatNode, ArrayNode*/
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 288:
					// line 1280 "FlatRubyParser.y"
  { /* ArgsCatNode, SplatNode, ArrayNode*/
                    Node node = null;

                    /* FIXME: lose syntactical elements here (and others like this)*/
                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(support.getPosition(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 289:
					// line 1292 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 290:
					// line 1295 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 291:
					// line 1300 "FlatRubyParser.y"
  {
                    Node node = support.splat_array(((Node)yyVals[-2+yyTop]));

                    if (node != null) {
                        yyVal = support.list_append(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 292:
					// line 1309 "FlatRubyParser.y"
  {
                    Node node = null;

                    if (((Node)yyVals[0+yyTop]) instanceof ArrayNode &&
                        (node = support.splat_array(((Node)yyVals[-3+yyTop]))) != null) {
                        yyVal = support.list_concat(node, ((Node)yyVals[0+yyTop]));
                    } else {
                        yyVal = support.arg_concat(((Node)yyVals[-3+yyTop]).getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                    }
                }
  break;
case 293:
					// line 1319 "FlatRubyParser.y"
  {
                     yyVal = support.newSplatNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 300:
					// line 1329 "FlatRubyParser.y"
  { 
                     yyVal = ((ListNode)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
                }
  break;
case 301:
					// line 1332 "FlatRubyParser.y"
  {
                     yyVal = ((ListNode)yyVals[0+yyTop]); /* FIXME: Why complaining without $$ = $1;*/
                }
  break;
case 304:
					// line 1337 "FlatRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[0+yyTop]));
                }
  break;
case 305:
					// line 1340 "FlatRubyParser.y"
  {
                    yyVal = lexer.getCmdArgumentState().getStack();
                    lexer.getCmdArgumentState().reset();
                }
  break;
case 306:
					// line 1343 "FlatRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-2+yyTop]).longValue());
                    yyVal = new BeginNode(((ISourcePosition)yyVals[-3+yyTop]), support.makeNullNil(((Node)yyVals[-1+yyTop])));
                }
  break;
case 307:
					// line 1347 "FlatRubyParser.y"
  {
                    lexer.setState(EXPR_ENDARG);
                }
  break;
case 308:
					// line 1349 "FlatRubyParser.y"
  {
                    yyVal = null; /*FIXME: Should be implicit nil?*/
                }
  break;
case 309:
					// line 1352 "FlatRubyParser.y"
  {
                    yyVal = lexer.getCmdArgumentState().getStack();
                    lexer.getCmdArgumentState().reset();
                }
  break;
case 310:
					// line 1355 "FlatRubyParser.y"
  {
                    lexer.setState(EXPR_ENDARG); 
                }
  break;
case 311:
					// line 1357 "FlatRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-3+yyTop]).longValue());
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 312:
					// line 1361 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        /* compstmt position includes both parens around it*/
                        ((ISourcePositionHolder) ((Node)yyVals[-1+yyTop])).setPosition(((ISourcePosition)yyVals[-2+yyTop]));
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    } else {
                        yyVal = new NilNode(((ISourcePosition)yyVals[-2+yyTop]));
                    }
                }
  break;
case 313:
					// line 1370 "FlatRubyParser.y"
  {
                    yyVal = support.new_colon2(support.getPosition(((Node)yyVals[-2+yyTop])), ((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 314:
					// line 1373 "FlatRubyParser.y"
  {
                    yyVal = support.new_colon3(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 315:
					// line 1376 "FlatRubyParser.y"
  {
                    ISourcePosition position = support.getPosition(((Node)yyVals[-1+yyTop]));
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(position); /* zero length array */
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                }
  break;
case 316:
					// line 1384 "FlatRubyParser.y"
  {
                    yyVal = ((HashNode)yyVals[-1+yyTop]);
                }
  break;
case 317:
					// line 1387 "FlatRubyParser.y"
  {
                    yyVal = new ReturnNode(((ISourcePosition)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 318:
					// line 1390 "FlatRubyParser.y"
  {
                    yyVal = support.new_yield(((ISourcePosition)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 319:
					// line 1393 "FlatRubyParser.y"
  {
                    yyVal = new YieldNode(((ISourcePosition)yyVals[-2+yyTop]), null);
                }
  break;
case 320:
					// line 1396 "FlatRubyParser.y"
  {
                    yyVal = new YieldNode(((ISourcePosition)yyVals[0+yyTop]), null);
                }
  break;
case 321:
					// line 1399 "FlatRubyParser.y"
  {
                    yyVal = support.new_defined(((ISourcePosition)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 322:
					// line 1402 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getConditionNode(((Node)yyVals[-1+yyTop])), "!");
                }
  break;
case 323:
					// line 1405 "FlatRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(NilImplicitNode.NIL, "!");
                }
  break;
case 324:
					// line 1408 "FlatRubyParser.y"
  {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop]), null, ((IterNode)yyVals[0+yyTop]));
                    yyVal = ((FCallNode)yyVals[-1+yyTop]);                    
                }
  break;
case 326:
					// line 1413 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) != null && 
                          ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                          lexer.compile_error(PID.BLOCK_ARG_AND_BLOCK_GIVEN, "Both block arg and actual block given.");
                    }
                    yyVal = ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    ((Node)yyVal).setPosition(((Node)yyVals[-1+yyTop]).getPosition());
                }
  break;
case 327:
					// line 1421 "FlatRubyParser.y"
  {
                    yyVal = ((LambdaNode)yyVals[0+yyTop]);
                }
  break;
case 328:
					// line 1424 "FlatRubyParser.y"
  {
                    yyVal = new IfNode(((ISourcePosition)yyVals[-5+yyTop]), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 329:
					// line 1427 "FlatRubyParser.y"
  {
                    yyVal = new IfNode(((ISourcePosition)yyVals[-5+yyTop]), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
                }
  break;
case 330:
					// line 1430 "FlatRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 331:
					// line 1432 "FlatRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 332:
					// line 1434 "FlatRubyParser.y"
  {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop]));
                    yyVal = new WhileNode(((ISourcePosition)yyVals[-6+yyTop]), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
                }
  break;
case 333:
					// line 1438 "FlatRubyParser.y"
  {
                  lexer.getConditionState().begin();
                }
  break;
case 334:
					// line 1440 "FlatRubyParser.y"
  {
                  lexer.getConditionState().end();
                }
  break;
case 335:
					// line 1442 "FlatRubyParser.y"
  {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop]));
                    yyVal = new UntilNode(((ISourcePosition)yyVals[-6+yyTop]), support.getConditionNode(((Node)yyVals[-4+yyTop])), body);
                }
  break;
case 336:
					// line 1446 "FlatRubyParser.y"
  {
                    yyVal = support.newCaseNode(((ISourcePosition)yyVals[-4+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 337:
					// line 1449 "FlatRubyParser.y"
  {
                    yyVal = support.newCaseNode(((ISourcePosition)yyVals[-3+yyTop]), null, ((Node)yyVals[-1+yyTop]));
                }
  break;
case 338:
					// line 1452 "FlatRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 339:
					// line 1454 "FlatRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 340:
					// line 1456 "FlatRubyParser.y"
  {
                      /* ENEBO: Lots of optz in 1.9 parser here*/
                    yyVal = new ForNode(((ISourcePosition)yyVals[-8+yyTop]), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]), support.getCurrentScope());
                }
  break;
case 341:
					// line 1460 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        support.yyerror("class definition in method body");
                    }
                    support.pushLocalScope();
                }
  break;
case 342:
					// line 1465 "FlatRubyParser.y"
  {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop]));

                    yyVal = new ClassNode(((ISourcePosition)yyVals[-5+yyTop]), ((Colon3Node)yyVals[-4+yyTop]), support.getCurrentScope(), body, ((Node)yyVals[-3+yyTop]));
                    support.popCurrentScope();
                }
  break;
case 343:
					// line 1471 "FlatRubyParser.y"
  {
                    yyVal = Boolean.valueOf(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 344:
					// line 1474 "FlatRubyParser.y"
  {
                    yyVal = Integer.valueOf(support.getInSingle());
                    support.setInSingle(0);
                    support.pushLocalScope();
                }
  break;
case 345:
					// line 1478 "FlatRubyParser.y"
  {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop]));

                    yyVal = new SClassNode(((ISourcePosition)yyVals[-7+yyTop]), ((Node)yyVals[-5+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 346:
					// line 1486 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        support.yyerror("module definition in method body");
                    }
                    support.pushLocalScope();
                }
  break;
case 347:
					// line 1491 "FlatRubyParser.y"
  {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop]));

                    yyVal = new ModuleNode(((ISourcePosition)yyVals[-4+yyTop]), ((Colon3Node)yyVals[-3+yyTop]), support.getCurrentScope(), body);
                    support.popCurrentScope();
                }
  break;
case 348:
					// line 1497 "FlatRubyParser.y"
  {
                    support.setInDef(true);
                    support.pushLocalScope();
                    yyVal = lexer.getCurrentArg();
                    lexer.setCurrentArg(null);
                }
  break;
case 349:
					// line 1502 "FlatRubyParser.y"
  {
                    Node body = support.makeNullNil(((Node)yyVals[-1+yyTop]));

                    yyVal = new DefnNode(((ISourcePosition)yyVals[-5+yyTop]), ((String)yyVals[-4+yyTop]), (ArgsNode) yyVals[-2+yyTop], support.getCurrentScope(), body, ((ISourcePosition)yyVals[0+yyTop]).getLine());
                    support.popCurrentScope();
                    support.setInDef(false);
                    lexer.setCurrentArg(((String)yyVals[-3+yyTop]));
                }
  break;
case 350:
					// line 1510 "FlatRubyParser.y"
  {
                    lexer.setState(EXPR_FNAME);
                }
  break;
case 351:
					// line 1512 "FlatRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.pushLocalScope();
                    lexer.setState(EXPR_ENDFN|EXPR_LABEL); /* force for args */
                    yyVal = lexer.getCurrentArg();
                    lexer.setCurrentArg(null);
                }
  break;
case 352:
					// line 1518 "FlatRubyParser.y"
  {
                    Node body = ((Node)yyVals[-1+yyTop]);
                    if (body == null) body = NilImplicitNode.NIL;

                    yyVal = new DefsNode(((ISourcePosition)yyVals[-8+yyTop]), ((Node)yyVals[-7+yyTop]), ((String)yyVals[-4+yyTop]), (ArgsNode) yyVals[-2+yyTop], support.getCurrentScope(), body, ((ISourcePosition)yyVals[0+yyTop]).getLine());
                    support.popCurrentScope();
                    support.setInSingle(support.getInSingle() - 1);
                    lexer.setCurrentArg(((String)yyVals[-3+yyTop]));
                }
  break;
case 353:
					// line 1527 "FlatRubyParser.y"
  {
                    yyVal = new BreakNode(((ISourcePosition)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 354:
					// line 1530 "FlatRubyParser.y"
  {
                    yyVal = new NextNode(((ISourcePosition)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 355:
					// line 1533 "FlatRubyParser.y"
  {
                    yyVal = new RedoNode(((ISourcePosition)yyVals[0+yyTop]));
                }
  break;
case 356:
					// line 1536 "FlatRubyParser.y"
  {
                    yyVal = new RetryNode(((ISourcePosition)yyVals[0+yyTop]));
                }
  break;
case 357:
					// line 1540 "FlatRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    if (yyVal == null) yyVal = NilImplicitNode.NIL;
                }
  break;
case 364:
					// line 1554 "FlatRubyParser.y"
  {
                    yyVal = new IfNode(((ISourcePosition)yyVals[-4+yyTop]), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 366:
					// line 1559 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 368:
					// line 1564 "FlatRubyParser.y"
  {
                }
  break;
case 369:
					// line 1567 "FlatRubyParser.y"
  {
                     yyVal = support.assignableInCurr(((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 370:
					// line 1570 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 371:
					// line 1575 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 372:
					// line 1578 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 373:
					// line 1582 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[0+yyTop]).getPosition(), ((ListNode)yyVals[0+yyTop]), null, null);
                }
  break;
case 374:
					// line 1585 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), support.assignableInCurr(((String)yyVals[0+yyTop]), null), null);
                }
  break;
case 375:
					// line 1588 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), support.assignableInCurr(((String)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 376:
					// line 1591 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-2+yyTop]).getPosition(), ((ListNode)yyVals[-2+yyTop]), new StarNode(lexer.getPosition()), null);
                }
  break;
case 377:
					// line 1594 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(((ListNode)yyVals[-4+yyTop]).getPosition(), ((ListNode)yyVals[-4+yyTop]), new StarNode(lexer.getPosition()), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 378:
					// line 1597 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(lexer.getPosition(), null, support.assignableInCurr(((String)yyVals[0+yyTop]), null), null);
                }
  break;
case 379:
					// line 1600 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(lexer.getPosition(), null, support.assignableInCurr(((String)yyVals[-2+yyTop]), null), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 380:
					// line 1603 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(lexer.getPosition(), null, new StarNode(lexer.getPosition()), null);
                }
  break;
case 381:
					// line 1606 "FlatRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(support.getPosition(((ListNode)yyVals[0+yyTop])), null, null, ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 382:
					// line 1610 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 383:
					// line 1613 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 384:
					// line 1616 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(lexer.getPosition(), null, ((String)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 385:
					// line 1619 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 386:
					// line 1623 "FlatRubyParser.y"
  {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
                }
  break;
case 387:
					// line 1626 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(lexer.getPosition(), null, null, null);
                }
  break;
case 388:
					// line 1631 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 389:
					// line 1634 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop]).getPosition(), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 390:
					// line 1637 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 391:
					// line 1640 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 392:
					// line 1643 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 393:
					// line 1646 "FlatRubyParser.y"
  {
                    RestArgNode rest = new UnnamedRestArgNode(((ListNode)yyVals[-1+yyTop]).getPosition(), null, support.getCurrentScope().addVariable("*"));
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, rest, null, (ArgsTailHolder) null);
                }
  break;
case 394:
					// line 1650 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 395:
					// line 1653 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 396:
					// line 1656 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-3+yyTop])), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 397:
					// line 1659 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-5+yyTop])), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 398:
					// line 1662 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(support.getPosition(((ListNode)yyVals[-1+yyTop])), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 399:
					// line 1665 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 400:
					// line 1668 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 401:
					// line 1671 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 402:
					// line 1674 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 403:
					// line 1678 "FlatRubyParser.y"
  {
    /* was $$ = null;*/
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
  break;
case 404:
					// line 1682 "FlatRubyParser.y"
  {
                    lexer.commandStart = true;
                    yyVal = ((ArgsNode)yyVals[0+yyTop]);
                }
  break;
case 405:
					// line 1687 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
  break;
case 406:
					// line 1691 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
  break;
case 407:
					// line 1694 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                }
  break;
case 408:
					// line 1700 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 409:
					// line 1703 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 410:
					// line 1708 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 411:
					// line 1711 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 412:
					// line 1715 "FlatRubyParser.y"
  {
                    support.new_bv(((String)yyVals[0+yyTop]));
                }
  break;
case 413:
					// line 1718 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 414:
					// line 1722 "FlatRubyParser.y"
  {
                    support.pushBlockScope();
                    yyVal = lexer.getLeftParenBegin();
                    lexer.setLeftParenBegin(lexer.incrementParenNest());
                }
  break;
case 415:
					// line 1726 "FlatRubyParser.y"
  {
                    yyVal = new LambdaNode(((ArgsNode)yyVals[-1+yyTop]).getPosition(), ((ArgsNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                    lexer.setLeftParenBegin(((Integer)yyVals[-2+yyTop]));
                }
  break;
case 416:
					// line 1732 "FlatRubyParser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-2+yyTop]);
                }
  break;
case 417:
					// line 1735 "FlatRubyParser.y"
  {
                    yyVal = ((ArgsNode)yyVals[0+yyTop]);
                }
  break;
case 418:
					// line 1739 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 419:
					// line 1742 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 420:
					// line 1746 "FlatRubyParser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 421:
					// line 1748 "FlatRubyParser.y"
  {
                    yyVal = new IterNode(((ISourcePosition)yyVals[-4+yyTop]), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 422:
					// line 1757 "FlatRubyParser.y"
  {
                    /* Workaround for JRUBY-2326 (MRI does not enter this production for some reason)*/
                    if (((Node)yyVals[-1+yyTop]) instanceof YieldNode) {
                        lexer.compile_error(PID.BLOCK_GIVEN_TO_YIELD, "block given to yield");
                    }
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockAcceptingNode && ((BlockAcceptingNode)yyVals[-1+yyTop]).getIterNode() instanceof BlockPassNode) {
                        lexer.compile_error(PID.BLOCK_ARG_AND_BLOCK_GIVEN, "Both block arg and actual block given.");
                    }
                    if (((Node)yyVals[-1+yyTop]) instanceof NonLocalControlFlowNode) {
                        ((BlockAcceptingNode) ((NonLocalControlFlowNode)yyVals[-1+yyTop]).getValueNode()).setIterNode(((IterNode)yyVals[0+yyTop]));
                    } else {
                        ((BlockAcceptingNode)yyVals[-1+yyTop]).setIterNode(((IterNode)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    ((Node)yyVal).setPosition(((Node)yyVals[-1+yyTop]).getPosition());
                }
  break;
case 423:
					// line 1773 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 424:
					// line 1776 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
                }
  break;
case 425:
					// line 1779 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((String)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), ((IterNode)yyVals[0+yyTop]));
                }
  break;
case 426:
					// line 1784 "FlatRubyParser.y"
  {
                    support.frobnicate_fcall_args(((FCallNode)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                    yyVal = ((FCallNode)yyVals[-1+yyTop]);
                }
  break;
case 427:
					// line 1788 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 428:
					// line 1791 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 429:
					// line 1794 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), null, null);
                }
  break;
case 430:
					// line 1797 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), "call", ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 431:
					// line 1800 "FlatRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), "call", ((Node)yyVals[0+yyTop]), null);
                }
  break;
case 432:
					// line 1803 "FlatRubyParser.y"
  {
                    yyVal = support.new_super(((ISourcePosition)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 433:
					// line 1806 "FlatRubyParser.y"
  {
                    yyVal = new ZSuperNode(((ISourcePosition)yyVals[0+yyTop]));
                }
  break;
case 434:
					// line 1809 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-3+yyTop]) instanceof SelfNode) {
                        yyVal = support.new_fcall("[]");
                        support.frobnicate_fcall_args(((FCallNode)yyVal), ((Node)yyVals[-1+yyTop]), null);
                    } else {
                        yyVal = support.new_call(((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]), null);
                    }
                }
  break;
case 435:
					// line 1818 "FlatRubyParser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 436:
					// line 1820 "FlatRubyParser.y"
  {
                    yyVal = new IterNode(((ISourcePosition)yyVals[-4+yyTop]), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 437:
					// line 1824 "FlatRubyParser.y"
  {
                    support.pushBlockScope();
                }
  break;
case 438:
					// line 1826 "FlatRubyParser.y"
  {
                    yyVal = new IterNode(((ISourcePosition)yyVals[-4+yyTop]), ((ArgsNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), support.getCurrentScope());
                    support.popCurrentScope();
                }
  break;
case 439:
					// line 1831 "FlatRubyParser.y"
  {
                    yyVal = support.newWhenNode(((ISourcePosition)yyVals[-4+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 442:
					// line 1837 "FlatRubyParser.y"
  {
                    Node node;
                    if (((Node)yyVals[-3+yyTop]) != null) {
                        node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(((ISourcePosition)yyVals[-5+yyTop]), "$!")), ((Node)yyVals[-1+yyTop]));
                        if (((Node)yyVals[-1+yyTop]) != null) {
                            node.setPosition(((ISourcePosition)yyVals[-5+yyTop]));
                        }
                    } else {
                        node = ((Node)yyVals[-1+yyTop]);
                    }
                    Node body = support.makeNullNil(node);
                    yyVal = new RescueBodyNode(((ISourcePosition)yyVals[-5+yyTop]), ((Node)yyVals[-4+yyTop]), body, ((RescueBodyNode)yyVals[0+yyTop]));
                }
  break;
case 443:
					// line 1850 "FlatRubyParser.y"
  { 
                    yyVal = null; 
                }
  break;
case 444:
					// line 1854 "FlatRubyParser.y"
  {
                    yyVal = support.newArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 445:
					// line 1857 "FlatRubyParser.y"
  {
                    yyVal = support.splat_array(((Node)yyVals[0+yyTop]));
                    if (yyVal == null) yyVal = ((Node)yyVals[0+yyTop]); /* ArgsCat or ArgsPush*/
                }
  break;
case 447:
					// line 1863 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 449:
					// line 1868 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 451:
					// line 1873 "FlatRubyParser.y"
  {
                    yyVal = ((NumericNode)yyVals[0+yyTop]);
                }
  break;
case 452:
					// line 1876 "FlatRubyParser.y"
  {
                    yyVal = support.asSymbol(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 454:
					// line 1881 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]) instanceof EvStrNode ? new DStrNode(((Node)yyVals[0+yyTop]).getPosition(), lexer.getEncoding()).add(((Node)yyVals[0+yyTop])) : ((Node)yyVals[0+yyTop]);
                    /*
                    NODE *node = $1;
                    if (!node) {
                        node = NEW_STR(STR_NEW0());
                    } else {
                        node = evstr2dstr(node);
                    }
                    $$ = node;
                    */
                }
  break;
case 455:
					// line 1895 "FlatRubyParser.y"
  {
                    yyVal = ((StrNode)yyVals[0+yyTop]);
                }
  break;
case 456:
					// line 1898 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 457:
					// line 1901 "FlatRubyParser.y"
  {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 458:
					// line 1905 "FlatRubyParser.y"
  {
                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop]));
		    lexer.setHeredocIndent(0);
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 459:
					// line 1911 "FlatRubyParser.y"
  {
                    ISourcePosition position = support.getPosition(((Node)yyVals[-1+yyTop]));

                    lexer.heredoc_dedent(((Node)yyVals[-1+yyTop]));
		    lexer.setHeredocIndent(0);

                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new XStrNode(position, null, StringSupport.CR_7BIT);
                    } else if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
                        yyVal = new XStrNode(position, (ByteList) ((StrNode)yyVals[-1+yyTop]).getValue().clone(), ((StrNode)yyVals[-1+yyTop]).getCodeRange());
                    } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                        yyVal = new DXStrNode(position, ((DStrNode)yyVals[-1+yyTop]));

                        ((Node)yyVal).setPosition(position);
                    } else {
                        yyVal = new DXStrNode(position).add(((Node)yyVals[-1+yyTop]));
                    }
                }
  break;
case 460:
					// line 1930 "FlatRubyParser.y"
  {
                    yyVal = support.newRegexpNode(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), (RegexpNode) ((RegexpNode)yyVals[0+yyTop]));
                }
  break;
case 461:
					// line 1934 "FlatRubyParser.y"
  {
                    yyVal = new ZArrayNode(lexer.getPosition());
                }
  break;
case 462:
					// line 1937 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 463:
					// line 1941 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(lexer.getPosition());
                }
  break;
case 464:
					// line 1944 "FlatRubyParser.y"
  {
                     yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DStrNode(((ListNode)yyVals[-2+yyTop]).getPosition(), lexer.getEncoding()).add(((Node)yyVals[-1+yyTop])) : ((Node)yyVals[-1+yyTop]));
                }
  break;
case 465:
					// line 1948 "FlatRubyParser.y"
  {
                     yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 466:
					// line 1951 "FlatRubyParser.y"
  {
                     yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 467:
					// line 1955 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(lexer.getPosition());
                }
  break;
case 468:
					// line 1958 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 469:
					// line 1962 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(lexer.getPosition());
                }
  break;
case 470:
					// line 1965 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]) instanceof EvStrNode ? new DSymbolNode(((ListNode)yyVals[-2+yyTop]).getPosition()).add(((Node)yyVals[-1+yyTop])) : support.asSymbol(((ListNode)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 471:
					// line 1969 "FlatRubyParser.y"
  {
                     yyVal = new ZArrayNode(lexer.getPosition());
                }
  break;
case 472:
					// line 1972 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 473:
					// line 1976 "FlatRubyParser.y"
  {
                    yyVal = new ZArrayNode(lexer.getPosition());
                }
  break;
case 474:
					// line 1979 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 475:
					// line 1984 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(lexer.getPosition());
                }
  break;
case 476:
					// line 1987 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 477:
					// line 1991 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(lexer.getPosition());
                }
  break;
case 478:
					// line 1994 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(support.asSymbol(((ListNode)yyVals[-2+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 479:
					// line 1998 "FlatRubyParser.y"
  {
                    ByteList aChar = ByteList.create("");
                    aChar.setEncoding(lexer.getEncoding());
                    yyVal = lexer.createStr(aChar, 0);
                }
  break;
case 480:
					// line 2003 "FlatRubyParser.y"
  {
                    yyVal = support.literal_concat(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 481:
					// line 2007 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 482:
					// line 2010 "FlatRubyParser.y"
  {
                    yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 483:
					// line 2014 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 484:
					// line 2017 "FlatRubyParser.y"
  {
    /* FIXME: mri is different here.*/
                    yyVal = support.literal_concat(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 485:
					// line 2022 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 486:
					// line 2025 "FlatRubyParser.y"
  {
                    yyVal = lexer.getStrTerm();
                    lexer.setStrTerm(null);
                    lexer.setState(EXPR_BEG);
                }
  break;
case 487:
					// line 2029 "FlatRubyParser.y"
  {
                    lexer.setStrTerm(((StrTerm)yyVals[-1+yyTop]));
                    yyVal = new EvStrNode(support.getPosition(((Node)yyVals[0+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 488:
					// line 2033 "FlatRubyParser.y"
  {
                   yyVal = lexer.getStrTerm();
                   lexer.setStrTerm(null);
                   lexer.getConditionState().stop();
                }
  break;
case 489:
					// line 2037 "FlatRubyParser.y"
  {
                   yyVal = lexer.getCmdArgumentState().getStack();
                   lexer.getCmdArgumentState().reset();
                }
  break;
case 490:
					// line 2040 "FlatRubyParser.y"
  {
                   yyVal = lexer.getState();
                   lexer.setState(EXPR_BEG);
                }
  break;
case 491:
					// line 2043 "FlatRubyParser.y"
  {
                   yyVal = lexer.getBraceNest();
                   lexer.setBraceNest(0);
                }
  break;
case 492:
					// line 2046 "FlatRubyParser.y"
  {
                   yyVal = lexer.getHeredocIndent();
                   lexer.setHeredocIndent(0);
                }
  break;
case 493:
					// line 2049 "FlatRubyParser.y"
  {
                   lexer.getConditionState().restart();
                   lexer.setStrTerm(((StrTerm)yyVals[-6+yyTop]));
                   lexer.getCmdArgumentState().reset(((Long)yyVals[-5+yyTop]).longValue());
                   lexer.setState(((Integer)yyVals[-4+yyTop]));
                   lexer.setBraceNest(((Integer)yyVals[-3+yyTop]));
                   lexer.setHeredocIndent(((Integer)yyVals[-2+yyTop]));
                   lexer.setHeredocLineIndent(-1);

                   yyVal = support.newEvStrNode(support.getPosition(((Node)yyVals[-1+yyTop])), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 494:
					// line 2061 "FlatRubyParser.y"
  {
                     yyVal = new GlobalVarNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 495:
					// line 2064 "FlatRubyParser.y"
  {
                     yyVal = new InstVarNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 496:
					// line 2067 "FlatRubyParser.y"
  {
                     yyVal = new ClassVarNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 498:
					// line 2073 "FlatRubyParser.y"
  {
                     lexer.setState(EXPR_END);
                     yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 503:
					// line 2081 "FlatRubyParser.y"
  {
                     lexer.setState(EXPR_END);

                     /* DStrNode: :"some text #{some expression}"*/
                     /* StrNode: :"some text"*/
                     /* EvStrNode :"#{some expression}"*/
                     /* Ruby 1.9 allows empty strings as symbols*/
                     if (((Node)yyVals[-1+yyTop]) == null) {
                         yyVal = support.asSymbol(lexer.getPosition(), "");
                     } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
                         yyVal = new DSymbolNode(((Node)yyVals[-1+yyTop]).getPosition(), ((DStrNode)yyVals[-1+yyTop]));
                     } else if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
                         yyVal = support.asSymbol(((Node)yyVals[-1+yyTop]).getPosition(), ((Node)yyVals[-1+yyTop]));
                     } else {
                         yyVal = new DSymbolNode(((Node)yyVals[-1+yyTop]).getPosition());
                         ((DSymbolNode)yyVal).add(((Node)yyVals[-1+yyTop]));
                     }
                }
  break;
case 504:
					// line 2100 "FlatRubyParser.y"
  {
                    yyVal = ((NumericNode)yyVals[0+yyTop]);  
                }
  break;
case 505:
					// line 2103 "FlatRubyParser.y"
  {
                     yyVal = support.negateNumeric(((NumericNode)yyVals[0+yyTop]));
                }
  break;
case 506:
					// line 2107 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 507:
					// line 2110 "FlatRubyParser.y"
  {
                     yyVal = ((FloatNode)yyVals[0+yyTop]);
                }
  break;
case 508:
					// line 2113 "FlatRubyParser.y"
  {
                     yyVal = ((RationalNode)yyVals[0+yyTop]);
                }
  break;
case 509:
					// line 2116 "FlatRubyParser.y"
  {
                     yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 510:
					// line 2121 "FlatRubyParser.y"
  {
                    yyVal = support.declareIdentifier(((String)yyVals[0+yyTop]));
                }
  break;
case 511:
					// line 2124 "FlatRubyParser.y"
  {
                    yyVal = new InstVarNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 512:
					// line 2127 "FlatRubyParser.y"
  {
                    yyVal = new GlobalVarNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 513:
					// line 2130 "FlatRubyParser.y"
  {
                    yyVal = new ConstNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 514:
					// line 2133 "FlatRubyParser.y"
  {
                    yyVal = new ClassVarNode(lexer.getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 515:
					// line 2136 "FlatRubyParser.y"
  { 
                    yyVal = new NilNode(lexer.getPosition());
                }
  break;
case 516:
					// line 2139 "FlatRubyParser.y"
  {
                    yyVal = new SelfNode(lexer.getPosition());
                }
  break;
case 517:
					// line 2142 "FlatRubyParser.y"
  { 
                    yyVal = new TrueNode(lexer.getPosition());
                }
  break;
case 518:
					// line 2145 "FlatRubyParser.y"
  {
                    yyVal = new FalseNode(lexer.getPosition());
                }
  break;
case 519:
					// line 2148 "FlatRubyParser.y"
  {
                    yyVal = new FileNode(lexer.getPosition(), new ByteList(lexer.getFile().getBytes(),
                    support.getConfiguration().getRuntime().getEncodingService().getLocaleEncoding()));
                }
  break;
case 520:
					// line 2152 "FlatRubyParser.y"
  {
                    yyVal = new FixnumNode(lexer.getPosition(), lexer.tokline.getLine()+1);
                }
  break;
case 521:
					// line 2155 "FlatRubyParser.y"
  {
                    yyVal = new EncodingNode(lexer.getPosition(), lexer.getEncoding());
                }
  break;
case 522:
					// line 2160 "FlatRubyParser.y"
  {
                    yyVal = support.assignableLabelOrIdentifier(((String)yyVals[0+yyTop]), null);
                }
  break;
case 523:
					// line 2163 "FlatRubyParser.y"
  {
                   yyVal = new InstAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 524:
					// line 2166 "FlatRubyParser.y"
  {
                   yyVal = new GlobalAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 525:
					// line 2169 "FlatRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) support.compile_error("dynamic constant assignment");

                    yyVal = new ConstDeclNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), null, NilImplicitNode.NIL);
                }
  break;
case 526:
					// line 2174 "FlatRubyParser.y"
  {
                    yyVal = new ClassVarAsgnNode(lexer.getPosition(), ((String)yyVals[0+yyTop]), NilImplicitNode.NIL);
                }
  break;
case 527:
					// line 2177 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to nil");
                    yyVal = null;
                }
  break;
case 528:
					// line 2181 "FlatRubyParser.y"
  {
                    support.compile_error("Can't change the value of self");
                    yyVal = null;
                }
  break;
case 529:
					// line 2185 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to true");
                    yyVal = null;
                }
  break;
case 530:
					// line 2189 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to false");
                    yyVal = null;
                }
  break;
case 531:
					// line 2193 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __FILE__");
                    yyVal = null;
                }
  break;
case 532:
					// line 2197 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __LINE__");
                    yyVal = null;
                }
  break;
case 533:
					// line 2201 "FlatRubyParser.y"
  {
                    support.compile_error("Can't assign to __ENCODING__");
                    yyVal = null;
                }
  break;
case 534:
					// line 2207 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 535:
					// line 2210 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 536:
					// line 2214 "FlatRubyParser.y"
  {
                   lexer.setState(EXPR_BEG);
                   lexer.commandStart = true;
                }
  break;
case 537:
					// line 2217 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 538:
					// line 2220 "FlatRubyParser.y"
  {
                   yyVal = null;
                }
  break;
case 539:
					// line 2225 "FlatRubyParser.y"
  {
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
                }
  break;
case 540:
					// line 2230 "FlatRubyParser.y"
  {
                   yyVal = lexer.inKwarg;
                   lexer.inKwarg = true;
                   lexer.setState(lexer.getState() | EXPR_LABEL);
                }
  break;
case 541:
					// line 2234 "FlatRubyParser.y"
  {
                   lexer.inKwarg = ((Boolean)yyVals[-2+yyTop]);
                    yyVal = ((ArgsNode)yyVals[-1+yyTop]);
                    lexer.setState(EXPR_BEG);
                    lexer.commandStart = true;
                }
  break;
case 542:
					// line 2242 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 543:
					// line 2245 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 544:
					// line 2248 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(lexer.getPosition(), null, ((String)yyVals[-1+yyTop]), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 545:
					// line 2251 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(((BlockArgNode)yyVals[0+yyTop]).getPosition(), null, null, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 546:
					// line 2255 "FlatRubyParser.y"
  {
                    yyVal = ((ArgsTailHolder)yyVals[0+yyTop]);
                }
  break;
case 547:
					// line 2258 "FlatRubyParser.y"
  {
                    yyVal = support.new_args_tail(lexer.getPosition(), null, null, null);
                }
  break;
case 548:
					// line 2263 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 549:
					// line 2266 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-7+yyTop]).getPosition(), ((ListNode)yyVals[-7+yyTop]), ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 550:
					// line 2269 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 551:
					// line 2272 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 552:
					// line 2275 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), ((ListNode)yyVals[-3+yyTop]), null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 553:
					// line 2278 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), ((ListNode)yyVals[-5+yyTop]), null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 554:
					// line 2281 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), ((ListNode)yyVals[-1+yyTop]), null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 555:
					// line 2284 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 556:
					// line 2287 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-5+yyTop]).getPosition(), null, ((ListNode)yyVals[-5+yyTop]), ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 557:
					// line 2290 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-1+yyTop]).getPosition(), null, ((ListNode)yyVals[-1+yyTop]), null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 558:
					// line 2293 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ListNode)yyVals[-3+yyTop]).getPosition(), null, ((ListNode)yyVals[-3+yyTop]), null, ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 559:
					// line 2296 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((RestArgNode)yyVals[-1+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-1+yyTop]), null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 560:
					// line 2299 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((RestArgNode)yyVals[-3+yyTop]).getPosition(), null, null, ((RestArgNode)yyVals[-3+yyTop]), ((ListNode)yyVals[-1+yyTop]), ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 561:
					// line 2302 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(((ArgsTailHolder)yyVals[0+yyTop]).getPosition(), null, null, null, null, ((ArgsTailHolder)yyVals[0+yyTop]));
                }
  break;
case 562:
					// line 2305 "FlatRubyParser.y"
  {
                    yyVal = support.new_args(lexer.getPosition(), null, null, null, null, (ArgsTailHolder) null);
                }
  break;
case 563:
					// line 2309 "FlatRubyParser.y"
  {
                    support.yyerror("formal argument cannot be a constant");
                }
  break;
case 564:
					// line 2312 "FlatRubyParser.y"
  {
                    support.yyerror("formal argument cannot be an instance variable");
                }
  break;
case 565:
					// line 2315 "FlatRubyParser.y"
  {
                    support.yyerror("formal argument cannot be a global variable");
                }
  break;
case 566:
					// line 2318 "FlatRubyParser.y"
  {
                    support.yyerror("formal argument cannot be a class variable");
                }
  break;
case 568:
					// line 2324 "FlatRubyParser.y"
  {
                    yyVal = support.formal_argument(((String)yyVals[0+yyTop]));
                }
  break;
case 569:
					// line 2328 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(((String)yyVals[0+yyTop]));
                    yyVal = support.arg_var(((String)yyVals[0+yyTop]));
                }
  break;
case 570:
					// line 2333 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = ((ArgumentNode)yyVals[0+yyTop]);
                }
  break;
case 571:
					// line 2337 "FlatRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                    /*            {
            ID tid = internal_id();
            arg_var(tid);
            if (dyna_in_block()) {
                $2->nd_value = NEW_DVAR(tid);
            }
            else {
                $2->nd_value = NEW_LVAR(tid);
            }
            $$ = NEW_ARGS_AUX(tid, 1);
            $$->nd_next = $2;*/
                }
  break;
case 572:
					// line 2353 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(lexer.getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 573:
					// line 2356 "FlatRubyParser.y"
  {
                    ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                    yyVal = ((ListNode)yyVals[-2+yyTop]);
                }
  break;
case 574:
					// line 2361 "FlatRubyParser.y"
  {
                    support.arg_var(support.formal_argument(((String)yyVals[0+yyTop])));
                    lexer.setCurrentArg(((String)yyVals[0+yyTop]));
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 575:
					// line 2367 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = support.keyword_arg(((Node)yyVals[0+yyTop]).getPosition(), support.assignableKeyword(((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 576:
					// line 2371 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = support.keyword_arg(lexer.getPosition(), support.assignableKeyword(((String)yyVals[0+yyTop]), new RequiredKeywordArgumentValueNode()));
                }
  break;
case 577:
					// line 2376 "FlatRubyParser.y"
  {
                    yyVal = support.keyword_arg(support.getPosition(((Node)yyVals[0+yyTop])), support.assignableKeyword(((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 578:
					// line 2379 "FlatRubyParser.y"
  {
                    yyVal = support.keyword_arg(lexer.getPosition(), support.assignableKeyword(((String)yyVals[0+yyTop]), new RequiredKeywordArgumentValueNode()));
                }
  break;
case 579:
					// line 2384 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 580:
					// line 2387 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 581:
					// line 2391 "FlatRubyParser.y"
  {
                    yyVal = new ArrayNode(((Node)yyVals[0+yyTop]).getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 582:
					// line 2394 "FlatRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 583:
					// line 2398 "FlatRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 584:
					// line 2401 "FlatRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 585:
					// line 2405 "FlatRubyParser.y"
  {
                    support.shadowing_lvar(((String)yyVals[0+yyTop]));
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 586:
					// line 2409 "FlatRubyParser.y"
  {
                    yyVal = support.internalId();
                }
  break;
case 587:
					// line 2413 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop])), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop]).getName(), ((Node)yyVals[0+yyTop])));
                }
  break;
case 588:
					// line 2418 "FlatRubyParser.y"
  {
                    lexer.setCurrentArg(null);
                    yyVal = new OptArgNode(support.getPosition(((Node)yyVals[0+yyTop])), support.assignableLabelOrIdentifier(((ArgumentNode)yyVals[-2+yyTop]).getName(), ((Node)yyVals[0+yyTop])));
                }
  break;
case 589:
					// line 2423 "FlatRubyParser.y"
  {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 590:
					// line 2426 "FlatRubyParser.y"
  {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 591:
					// line 2430 "FlatRubyParser.y"
  {
                    yyVal = new BlockNode(((Node)yyVals[0+yyTop]).getPosition()).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 592:
					// line 2433 "FlatRubyParser.y"
  {
                    yyVal = support.appendToBlock(((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 595:
					// line 2440 "FlatRubyParser.y"
  {
                    if (!support.is_local_id(((String)yyVals[0+yyTop]))) {
                        support.yyerror("rest argument must be local variable");
                    }
                    
                    yyVal = new RestArgNode(support.arg_var(support.shadowing_lvar(((String)yyVals[0+yyTop]))));
                }
  break;
case 596:
					// line 2447 "FlatRubyParser.y"
  {
                    yyVal = new UnnamedRestArgNode(lexer.getPosition(), "", support.getCurrentScope().addVariable("*"));
                }
  break;
case 599:
					// line 2455 "FlatRubyParser.y"
  {
                    if (!support.is_local_id(((String)yyVals[0+yyTop]))) {
                        support.yyerror("block argument must be local variable");
                    }
                    
                    yyVal = new BlockArgNode(support.arg_var(support.shadowing_lvar(((String)yyVals[0+yyTop]))));
                }
  break;
case 600:
					// line 2463 "FlatRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 601:
					// line 2466 "FlatRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 602:
					// line 2470 "FlatRubyParser.y"
  {
                    if (!(((Node)yyVals[0+yyTop]) instanceof SelfNode)) {
                        support.checkExpression(((Node)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 603:
					// line 2476 "FlatRubyParser.y"
  {
                    lexer.setState(EXPR_BEG);
                }
  break;
case 604:
					// line 2478 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        support.yyerror("can't define single method for ().");
                    } else if (((Node)yyVals[-1+yyTop]) instanceof ILiteralNode) {
                        support.yyerror("can't define single method for literals.");
                    }
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 605:
					// line 2489 "FlatRubyParser.y"
  {
                    yyVal = new HashNode(lexer.getPosition());
                }
  break;
case 606:
					// line 2492 "FlatRubyParser.y"
  {
                    yyVal = support.remove_duplicate_keys(((HashNode)yyVals[-1+yyTop]));
                }
  break;
case 607:
					// line 2497 "FlatRubyParser.y"
  {
                    yyVal = new HashNode(lexer.getPosition(), ((KeyValuePair)yyVals[0+yyTop]));
                }
  break;
case 608:
					// line 2500 "FlatRubyParser.y"
  {
                    yyVal = ((HashNode)yyVals[-2+yyTop]).add(((KeyValuePair)yyVals[0+yyTop]));
                }
  break;
case 609:
					// line 2505 "FlatRubyParser.y"
  {
                    yyVal = support.createKeyValue(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 610:
					// line 2508 "FlatRubyParser.y"
  {
                    Node label = support.asSymbol(support.getPosition(((Node)yyVals[0+yyTop])), ((String)yyVals[-1+yyTop]));
                    yyVal = support.createKeyValue(label, ((Node)yyVals[0+yyTop]));
                }
  break;
case 611:
					// line 2512 "FlatRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) instanceof StrNode) {
                        DStrNode dnode = new DStrNode(support.getPosition(((Node)yyVals[-2+yyTop])), lexer.getEncoding());
                        dnode.add(((Node)yyVals[-2+yyTop]));
                        yyVal = support.createKeyValue(new DSymbolNode(support.getPosition(((Node)yyVals[-2+yyTop])), dnode), ((Node)yyVals[0+yyTop]));
                    } else if (((Node)yyVals[-2+yyTop]) instanceof DStrNode) {
                        yyVal = support.createKeyValue(new DSymbolNode(support.getPosition(((Node)yyVals[-2+yyTop])), ((DStrNode)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
                    } else {
                        support.compile_error("Uknown type for assoc in strings: " + ((Node)yyVals[-2+yyTop]));
                    }

                }
  break;
case 612:
					// line 2524 "FlatRubyParser.y"
  {
                    yyVal = support.createKeyValue(null, ((Node)yyVals[0+yyTop]));
                }
  break;
case 625:
					// line 2533 "FlatRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 626:
					// line 2536 "FlatRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 628:
					// line 2541 "FlatRubyParser.y"
  {
                    yyVal = "::";
                }
  break;
case 633:
					// line 2547 "FlatRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 634:
					// line 2550 "FlatRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 642:
					// line 2561 "FlatRubyParser.y"
  {
                      yyVal = null;
                }
  break;
case 643:
					// line 2565 "FlatRubyParser.y"
  {  
                  yyVal = null;
                }
  break;
					// line 10078 "-"
// ACTIONS_END
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          if (yydebug != null) yydebug.shift(0, yyFinal);
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.nextToken();
//            yyToken = yyLex.advance() ? yyLex.token() : 0;
            if (yydebug != null)
               yydebug.lex(yyState, yyToken,yyName(yyToken), yyLex.value());
          }
          if (yyToken == 0) {
            if (yydebug != null) yydebug.accept(yyVal);
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
        if (yydebug != null) yydebug.shift(yyStates[yyTop], yyState);
        continue yyLoop;
      }
    }
  }
}
					// line 10126 "-"
