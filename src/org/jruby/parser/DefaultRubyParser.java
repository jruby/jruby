					// line 2 "DefaultRubyParser.y"
/*
 * DefaultRubyParser.java - JRuby - Parser constructed from parse.y
 * Created on 07. Oktober 2001, 01:28
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
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

import java.math.*;

import org.jruby.common.*;
import org.jruby.lexer.yacc.*;
import org.jruby.ast.*;
import org.jruby.ast.types.*;

import org.jruby.ast.util.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

public class DefaultRubyParser {
    private ParserSupport support;
    private RubyYaccLexer lexer;

    private IRubyErrorHandler errorHandler;

    public DefaultRubyParser() {
        this.support = new ParserSupport();
        this.lexer = new RubyYaccLexer();
	// lame
	this.lexer.setParserSupport(support);
    }

    public void setErrorHandler(IRubyErrorHandler errorHandler) {
        this.errorHandler = errorHandler;

	support.setErrorHandler(errorHandler);
	lexer.setErrorHandler(errorHandler);
    }

/*
%union {
    Node *node;
    VALUE val;
    ID id;
    int num;
    struct RVarmap *vars;
}
*/
					// line 72 "-"
// %token constants

  public static final int kCLASS = 257;
  public static final int kMODULE = 258;
  public static final int kDEF = 259;
  public static final int kUNDEF = 260;
  public static final int kBEGIN = 261;
  public static final int kRESCUE = 262;
  public static final int kENSURE = 263;
  public static final int kEND = 264;
  public static final int kIF = 265;
  public static final int kUNLESS = 266;
  public static final int kTHEN = 267;
  public static final int kELSIF = 268;
  public static final int kELSE = 269;
  public static final int kCASE = 270;
  public static final int kWHEN = 271;
  public static final int kWHILE = 272;
  public static final int kUNTIL = 273;
  public static final int kFOR = 274;
  public static final int kBREAK = 275;
  public static final int kNEXT = 276;
  public static final int kREDO = 277;
  public static final int kRETRY = 278;
  public static final int kIN = 279;
  public static final int kDO = 280;
  public static final int kDO_COND = 281;
  public static final int kDO_BLOCK = 282;
  public static final int kRETURN = 283;
  public static final int kYIELD = 284;
  public static final int kSUPER = 285;
  public static final int kSELF = 286;
  public static final int kNIL = 287;
  public static final int kTRUE = 288;
  public static final int kFALSE = 289;
  public static final int kAND = 290;
  public static final int kOR = 291;
  public static final int kNOT = 292;
  public static final int kIF_MOD = 293;
  public static final int kUNLESS_MOD = 294;
  public static final int kWHILE_MOD = 295;
  public static final int kUNTIL_MOD = 296;
  public static final int kRESCUE_MOD = 297;
  public static final int kALIAS = 298;
  public static final int kDEFINED = 299;
  public static final int klBEGIN = 300;
  public static final int klEND = 301;
  public static final int k__LINE__ = 302;
  public static final int k__FILE__ = 303;
  public static final int tIDENTIFIER = 304;
  public static final int tFID = 305;
  public static final int tGVAR = 306;
  public static final int tIVAR = 307;
  public static final int tCONSTANT = 308;
  public static final int tCVAR = 309;
  public static final int tINTEGER = 310;
  public static final int tFLOAT = 311;
  public static final int tSTRING_CONTENT = 312;
  public static final int tNTH_REF = 313;
  public static final int tBACK_REF = 314;
  public static final int tREGEXP_END = 315;
  public static final int tUPLUS = 316;
  public static final int tUMINUS = 317;
  public static final int tPOW = 318;
  public static final int tCMP = 319;
  public static final int tEQ = 320;
  public static final int tEQQ = 321;
  public static final int tNEQ = 322;
  public static final int tGEQ = 323;
  public static final int tLEQ = 324;
  public static final int tANDOP = 325;
  public static final int tOROP = 326;
  public static final int tMATCH = 327;
  public static final int tNMATCH = 328;
  public static final int tDOT2 = 329;
  public static final int tDOT3 = 330;
  public static final int tAREF = 331;
  public static final int tASET = 332;
  public static final int tLSHFT = 333;
  public static final int tRSHFT = 334;
  public static final int tCOLON2 = 335;
  public static final int tCOLON3 = 336;
  public static final int tOP_ASGN = 337;
  public static final int tASSOC = 338;
  public static final int tLPAREN = 339;
  public static final int tLPAREN_ARG = 340;
  public static final int tLBRACK = 341;
  public static final int tLBRACE = 342;
  public static final int tLBRACE_ARG = 343;
  public static final int tSTAR = 344;
  public static final int tAMPER = 345;
  public static final int tSYMBEG = 346;
  public static final int tSTRING_BEG = 347;
  public static final int tXSTRING_BEG = 348;
  public static final int tREGEXP_BEG = 349;
  public static final int tWORDS_BEG = 350;
  public static final int tQWORDS_BEG = 351;
  public static final int tSTRING_DBEG = 352;
  public static final int tSTRING_DVAR = 353;
  public static final int tSTRING_END = 354;
  public static final int tLOWEST = 355;
  public static final int tUMINUS_NUM = 356;
  public static final int tLAST_TOKEN = 357;
  public static final int yyErrorCode = 256;

  /** thrown for irrecoverable syntax errors and stack overflow.
    */
  public static class yyException extends java.lang.Exception {
    public yyException (String message) {
      super(message);
    }
  }

  /** simplified error message.
      @see <a href="#yyerror(java.lang.String, java.lang.String[])">yyerror</a>
    */
  public void yyerror (String message) {
     yyerror(message, null);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected vector of acceptable tokens, if available.
    */
  public void yyerror (String message, Object expected) {
     // FIXME: in skeleton.jruby: postition
     errorHandler.handleError(IErrors.SYNTAX_ERROR, getPosition(), message, expected);
  }

  /** debugging support, requires the package jay.yydebug.
      Set to null to suppress debugging messages.
    */

  protected static final int yyFinal = 1;

  /** index-checked interface to yyName[].
      @param token single character or %token value.
      @return token name or [illegal] or [unknown].
    */

  /** computes list of expected tokens on error by tracing the tables.
      @param state for which to compute the list.
      @return list of token names.
    */
  protected String[] yyExpecting (int state) {
    int token, n, len = 0;
    boolean[] ok = new boolean[YyNameClass.yyName.length];

    if ((n = YySindexClass.yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < YyNameClass.yyName.length && n+token < YyTableClass.yyTable.length; ++ token)
        if (YyCheckClass.yyCheck[n+token] == token && !ok[token] && YyNameClass.yyName[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = YyRindexClass.yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < YyNameClass.yyName.length && n+token < YyTableClass.yyTable.length; ++ token)
        if (YyCheckClass.yyCheck[n+token] == token && !ok[token] && YyNameClass.yyName[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = YyNameClass.yyName[token];
    return result;
  }

  /** the generated parser, with debugging messages.
      Maintains a state and a value stack, currently with fixed maximum size.
      @param yyLex The lexer.
      @param yydebug debug message writer implementing yyDebug, or null.
      @return result of the last reduction, if any.
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (RubyYaccLexer yyLex, Object yydebug)
				throws java.io.IOException, yyException {
    return yyparse(yyLex);
  }

  /** initial size and increment of the state/value stack [default 256].
      This is not final so that it can be overwritten outside of invocations
      of yyparse().
    */
  protected int yyMax;

  /** executed at the beginning of a reduce action.
      Used as $$ = yyDefault($1), prior to the user-specified action, if any.
      Can be overwritten to provide deep copy, etc.
      @param first value for $1, or null.
      @return first.
    */
  protected Object yyDefault (Object first) {
    return first;
  }

  /** the generated parser.
      Maintains a state and a value stack, currently with fixed maximum size.
      @param yyLex The lexer.
      @return result of the last reduction, if any.
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (RubyYaccLexer yyLex)
				throws java.io.IOException, yyException {
    if (yyMax <= 0) yyMax = 256;			// initial size
    int yyState = 0, yyStates[] = new int[yyMax];	// state stack
    Object yyVal = null, yyVals[] = new Object[yyMax];	// value stack
    int yyToken = -1;					// current input
    int yyErrorFlag = 0;				// #tks to shift

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

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = YyDefRedClass.yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if ((yyN = YySindexClass.yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < YyTableClass.yyTable.length && YyCheckClass.yyCheck[yyN] == yyToken) {
            yyState = YyTableClass.yyTable[yyN];		// shift to yyN
            yyVal = yyLex.value();
            yyToken = -1;
            if (yyErrorFlag > 0) -- yyErrorFlag;
            continue yyLoop;
          }
          if ((yyN = YyRindexClass.yyRindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < YyTableClass.yyTable.length && YyCheckClass.yyCheck[yyN] == yyToken)
            yyN = YyTableClass.yyTable[yyN];			// reduce (yyN)
          else
            switch (yyErrorFlag) {
  
            case 0:
              yyerror("syntax error", new SyntaxErrorState(yyExpecting(yyState), YyNameClass.yyName[yyToken]));
  
            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = YySindexClass.yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < YyTableClass.yyTable.length
                    && YyCheckClass.yyCheck[yyN] == yyErrorCode) {
                  yyState = YyTableClass.yyTable[yyN];
                  yyVal = yyLex.value();
                  continue yyLoop;
                }
              } while (-- yyTop >= 0);
              throw new yyException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                throw new yyException("irrecoverable syntax error at end-of-file");
              }
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-YyLenClass.yyLen[yyN];
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
case 1:
					// line 214 "DefaultRubyParser.y"
  {
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();

		  /* Fix: Move to ruby runtime....?*/
                  /*if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {*/
                  /*    support.setClassNest(0);*/
                  /*} else {*/
                  /*    support.setClassNest(1);*/
                  /*}*/
              }
  break;
case 2:
					// line 224 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null && !support.isCompileForEval()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(ListNodeUtil.getLast(((BlockNode)yyVals[0+yyTop])));
                      } else {
                          support.checkUselessStatement(((Node)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), ((Node)yyVals[0+yyTop])));
                  support.updateTopLocalVariables();
                  support.setClassNest(0);
              }
  break;
case 3:
					// line 241 "DefaultRubyParser.y"
  {
                 Node node = ((Node)yyVals[-3+yyTop]);

		 if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
		    node = new RescueNode(getPosition(), ((Node)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		 } else if (((Node)yyVals[-1+yyTop]) != null) {
		    errorHandler.handleError(IErrors.WARN, null, "else without rescue is useless");
                    node = support.appendToBlock(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
		 }
		 if (((Node)yyVals[0+yyTop]) != null) {
		    node = new EnsureNode(getPosition(), node, ((Node)yyVals[0+yyTop]));
		 }

		 yyVal = node;
             }
  break;
case 4:
					// line 257 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 265 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((Node)yyVals[0+yyTop]), getPosition());
                }
  break;
case 7:
					// line 268 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-2+yyTop]), support.newline_node(((Node)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 8:
					// line 271 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 275 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 277 "DefaultRubyParser.y"
  {
                    yyVal = new AliasNode(getPosition(), ((String)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 11:
					// line 280 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 12:
					// line 283 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(), ((String)yyVals[-1+yyTop]), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
                }
  break;
case 13:
					// line 286 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 14:
					// line 290 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 15:
					// line 293 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null);
                }
  break;
case 16:
					// line 296 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])), null, ((Node)yyVals[-2+yyTop]));
                }
  break;
case 17:
					// line 299 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), true);
                    }
                }
  break;
case 18:
					// line 306 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        yyVal = new UntilNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]));
                    }
                }
  break;
case 19:
					// line 314 "DefaultRubyParser.y"
  {
		  yyVal = new RescueNode(getPosition(), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(), null,((Node)yyVals[0+yyTop]), null), null);
                }
  break;
case 20:
					// line 318 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push();
                }
  break;
case 21:
					// line 323 "DefaultRubyParser.y"
  {
                    support.getResult().setBeginNodes(support.appendToBlock(support.getResult().getBeginNodes(), new ScopeNode(support.getLocalNames().getNames(), ((Node)yyVals[-1+yyTop]))));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 22:
					// line 328 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    yyVal = new IterNode(getPosition(), null, new PostExeNode(getPosition()), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 23:
					// line 334 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 24:
					// line 338 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(), ((Node)yyVals[0+yyTop])));
		    } else {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop])));
		    }
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                }
  break;
case 25:
					// line 347 "DefaultRubyParser.y"
  {
 		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
		        if (((String)yyVals[-1+yyTop]).equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(), support.gettable(name, getPosition()), ((Node)yyVals[-2+yyTop]));
			    /* XXX
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (((String)yyVals[-1+yyTop]).equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(), support.gettable(name, getPosition()), ((Node)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
                                ((AssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, getPosition()), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null;
		    }
		}
  break;
case 26:
					// line 372 "DefaultRubyParser.y"
  {
                    /* Much smaller than ruby block */
                    yyVal = new OpElementAsgnNode(getPosition(), ((Node)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));

                }
  break;
case 27:
					// line 377 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 28:
					// line 380 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 29:
					// line 383 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 30:
					// line 386 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 31:
					// line 390 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), new SValueNode(getPosition(), ((Node)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 393 "DefaultRubyParser.y"
  {
                    if (((MultipleAsgnNode)yyVals[-2+yyTop]).getHeadNode() != null) {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ToAryNode(getPosition(), ((Node)yyVals[0+yyTop])));
		    } else {
		        ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop])));
		    }
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
		}
  break;
case 33:
					// line 401 "DefaultRubyParser.y"
  {
                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
		    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
		}
  break;
case 36:
					// line 408 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 37:
					// line 411 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 414 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 39:
					// line 417 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 41:
					// line 422 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]); /*Do we really need this set? $1 is $$?*/
		}
  break;
case 44:
					// line 429 "DefaultRubyParser.y"
  {
                    yyVal = new ReturnNode(getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 45:
					// line 432 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 46:
					// line 435 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(), support.ret_args(((Node)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 48:
					// line 440 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 49:
					// line 443 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 50:
					// line 447 "DefaultRubyParser.y"
  {
                      support.getBlockNames().push();
		  }
  break;
case 51:
					// line 449 "DefaultRubyParser.y"
  {
                      yyVal = new IterNode(getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                      support.getBlockNames().pop();
		  }
  break;
case 52:
					// line 454 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 53:
					// line 457 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), getPosition()); 
	            if (((IterNode)yyVals[0+yyTop]) != null) {
                        if (yyVal instanceof BlockPassNode) {
                            errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
                        yyVal = ((Node)yyVals[-1+yyTop]);
		   }
                }
  break;
case 54:
					// line 467 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 55:
					// line 470 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])); 
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
		 }
  break;
case 56:
					// line 480 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 57:
					// line 483 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])); 
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
	        }
  break;
case 58:
					// line 493 "DefaultRubyParser.y"
  {
		    yyVal = support.new_super(((Node)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
		}
  break;
case 59:
					// line 496 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(), ((Node)yyVals[0+yyTop]));
		}
  break;
case 61:
					// line 501 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
		}
  break;
case 63:
					// line 506 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(), new ArrayNode(getPosition()).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 64:
					// line 510 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ListNode)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 513 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ListNode)yyVals[-1+yyTop]).add(((Node)yyVals[0+yyTop])), null);
                }
  break;
case 66:
					// line 516 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ListNode)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 519 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ListNode)yyVals[-1+yyTop]), new StarNode());
                }
  break;
case 68:
					// line 522 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, ((Node)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 525 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, new StarNode());
                }
  break;
case 71:
					// line 530 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
                }
  break;
case 72:
					// line 534 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 73:
					// line 537 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 74:
					// line 541 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(), yyVals[0+yyTop], null);
                }
  break;
case 75:
					// line 544 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 76:
					// line 547 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 77:
					// line 550 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 78:
					// line 553 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 79:
					// line 556 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
		}
  break;
case 80:
					// line 563 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    yyVal = new Colon3Node(getPosition(), ((String)yyVals[0+yyTop]));
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
		    }
  break;
case 81:
					// line 573 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 82:
					// line 578 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(), yyVals[0+yyTop], null);
                }
  break;
case 83:
					// line 581 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 84:
					// line 584 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 85:
					// line 587 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
 	        }
  break;
case 86:
					// line 590 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 87:
					// line 593 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
	        }
  break;
case 88:
					// line 600 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }

		    /* ERROR:  VEry likely a big error. */
                    yyVal = new Colon3Node(getPosition(), ((String)yyVals[0+yyTop]));
		    /* ruby $$ = NEW_CDECL(0, 0, NEW_COLON3($2)); */
	        }
  break;
case 89:
					// line 609 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 90:
					// line 614 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 92:
					// line 619 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(), null, ((String)yyVals[0+yyTop]));
		}
  break;
case 93:
					// line 622 "DefaultRubyParser.y"
  {
                    /* $1 was $$ in ruby?*/
                    yyVal = new Colon2Node(getPosition(), null, ((String)yyVals[0+yyTop]));
 	        }
  break;
case 94:
					// line 626 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(), ((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
		}
  break;
case 98:
					// line 633 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 99:
					// line 637 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 102:
					// line 645 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 103:
					// line 648 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 104:
					// line 650 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((Node)yyVals[-3+yyTop]), new UndefNode(getPosition(), ((String)yyVals[0+yyTop])));
                }
  break;
case 105:
					// line 654 "DefaultRubyParser.y"
  { yyVal = "|"; }
  break;
case 106:
					// line 655 "DefaultRubyParser.y"
  { yyVal = "^"; }
  break;
case 107:
					// line 656 "DefaultRubyParser.y"
  { yyVal = "&"; }
  break;
case 108:
					// line 657 "DefaultRubyParser.y"
  { yyVal = "<=>"; }
  break;
case 109:
					// line 658 "DefaultRubyParser.y"
  { yyVal = "=="; }
  break;
case 110:
					// line 659 "DefaultRubyParser.y"
  { yyVal = "==="; }
  break;
case 111:
					// line 660 "DefaultRubyParser.y"
  { yyVal = "=~"; }
  break;
case 112:
					// line 661 "DefaultRubyParser.y"
  { yyVal = ">"; }
  break;
case 113:
					// line 662 "DefaultRubyParser.y"
  { yyVal = ">="; }
  break;
case 114:
					// line 663 "DefaultRubyParser.y"
  { yyVal = "<"; }
  break;
case 115:
					// line 664 "DefaultRubyParser.y"
  { yyVal = "<="; }
  break;
case 116:
					// line 665 "DefaultRubyParser.y"
  { yyVal = "<<"; }
  break;
case 117:
					// line 666 "DefaultRubyParser.y"
  { yyVal = ">>"; }
  break;
case 118:
					// line 667 "DefaultRubyParser.y"
  { yyVal = "+"; }
  break;
case 119:
					// line 668 "DefaultRubyParser.y"
  { yyVal = "-"; }
  break;
case 120:
					// line 669 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 121:
					// line 670 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 122:
					// line 671 "DefaultRubyParser.y"
  { yyVal = "/"; }
  break;
case 123:
					// line 672 "DefaultRubyParser.y"
  { yyVal = "%"; }
  break;
case 124:
					// line 673 "DefaultRubyParser.y"
  { yyVal = "**"; }
  break;
case 125:
					// line 674 "DefaultRubyParser.y"
  { yyVal = "~"; }
  break;
case 126:
					// line 675 "DefaultRubyParser.y"
  { yyVal = "+@"; }
  break;
case 127:
					// line 676 "DefaultRubyParser.y"
  { yyVal = "-@"; }
  break;
case 128:
					// line 677 "DefaultRubyParser.y"
  { yyVal = "[]"; }
  break;
case 129:
					// line 678 "DefaultRubyParser.y"
  { yyVal = "[]="; }
  break;
case 130:
					// line 679 "DefaultRubyParser.y"
  { yyVal = "`"; }
  break;
case 172:
					// line 690 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 693 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((Node)yyVals[-4+yyTop]), new RescueNode(getPosition(), ((Node)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(), null,((Node)yyVals[0+yyTop]), null), null));
		}
  break;
case 174:
					// line 696 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();

		        if (((String)yyVals[-1+yyTop]).equals("||")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(), support.gettable(name, getPosition()), ((Node)yyVals[-2+yyTop]));
			    /* FIXME
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (((String)yyVals[-1+yyTop]).equals("&&")) {
	                    ((AssignableNode)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(), support.gettable(name, getPosition()), ((Node)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
                                ((AssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, getPosition()), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null; /* XXX 0; */
		    }
                }
  break;
case 175:
					// line 722 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(), ((Node)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 725 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 177:
					// line 728 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 178:
					// line 731 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 179:
					// line 734 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 180:
					// line 738 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 181:
					// line 742 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((Node)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 182:
					// line 746 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), false);
                }
  break;
case 183:
					// line 751 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
		    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]), true);
                }
  break;
case 184:
					// line 756 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "+", ((Node)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 759 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "-", ((Node)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 762 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "*", ((Node)yyVals[0+yyTop]));
                }
  break;
case 187:
					// line 765 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "/", ((Node)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 768 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "%", ((Node)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 771 "DefaultRubyParser.y"
  {
		      yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "**", ((Node)yyVals[0+yyTop]));
                    /* Covert '- number ** number' to '- (number ** number)' 
                    boolean needNegate = false;
                    if (($1 instanceof FixnumNode && $<FixnumNode>1.getValue() < 0) ||
                        ($1 instanceof BignumNode && $<BignumNode>1.getValue().compareTo(BigInteger.ZERO) < 0) ||
                        ($1 instanceof FloatNode && $<FloatNode>1.getValue() < 0.0)) {

                        $<>1 = support.getOperatorCallNode($1, "-@");
                        needNegate = true;
                    }

                    $$ = support.getOperatorCallNode($1, "**", $3);

                    if (needNegate) {
                        $$ = support.getOperatorCallNode($<Node>$, "-@");
                    }
		    */
                }
  break;
case 190:
					// line 790 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode((((Number)yyVals[-2+yyTop]) instanceof Long ? (Node) new FixnumNode(getPosition(), ((Long)yyVals[-2+yyTop]).longValue()) : (Node)new BignumNode(getPosition(), ((BigInteger)yyVals[-2+yyTop]))), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 191:
					// line 793 "DefaultRubyParser.y"
  {
	            yyVal = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition(), ((Double)yyVals[-3+yyTop]).doubleValue()), "**", ((Node)yyVals[0+yyTop])), "-@");
                }
  break;
case 192:
					// line 796 "DefaultRubyParser.y"
  {
 	            if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof ILiteralNode) {
		        yyVal = ((Node)yyVals[0+yyTop]);
		    } else {
                        yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "+@");
		    }
                }
  break;
case 193:
					// line 803 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "-@");
		}
  break;
case 194:
					// line 806 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "|", ((Node)yyVals[0+yyTop]));
                }
  break;
case 195:
					// line 809 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "^", ((Node)yyVals[0+yyTop]));
                }
  break;
case 196:
					// line 812 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "&", ((Node)yyVals[0+yyTop]));
                }
  break;
case 197:
					// line 815 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 198:
					// line 818 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">", ((Node)yyVals[0+yyTop]));
                }
  break;
case 199:
					// line 821 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 200:
					// line 824 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 201:
					// line 827 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<=", ((Node)yyVals[0+yyTop]));
                }
  break;
case 202:
					// line 830 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop]));
                }
  break;
case 203:
					// line 833 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "===", ((Node)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 836 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "==", ((Node)yyVals[0+yyTop])));
                }
  break;
case 205:
					// line 839 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 842 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getMatchNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 207:
					// line 845 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((Node)yyVals[0+yyTop])));
                }
  break;
case 208:
					// line 848 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[0+yyTop]), "~");
                }
  break;
case 209:
					// line 851 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), "<<", ((Node)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 854 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((Node)yyVals[-2+yyTop]), ">>", ((Node)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 857 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 860 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 863 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 214:
					// line 865 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 215:
					// line 869 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 872 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 876 "DefaultRubyParser.y"
  {
		    support.checkExpression(((Node)yyVals[0+yyTop]));
	            yyVal = ((Node)yyVals[0+yyTop]);   
		}
  break;
case 219:
					// line 882 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[-1+yyTop]));
                }
  break;
case 220:
					// line 886 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 221:
					// line 889 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_concat(getPosition(), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 222:
					// line 893 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new HashNode(((ListNode)yyVals[-1+yyTop])));
                }
  break;
case 223:
					// line 896 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = new NewlineNode(getPosition(), new SplatNode(getPosition(), ((Node)yyVals[-1+yyTop])));
                }
  break;
case 224:
					// line 901 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 225:
					// line 904 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 226:
					// line 907 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 227:
					// line 911 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = ((ListNode)yyVals[-4+yyTop]).add(((Node)yyVals[-2+yyTop]));
                }
  break;
case 230:
					// line 919 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 923 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 232:
					// line 926 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(), ((ListNode)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 930 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new HashNode(((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 934 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(), new ArrayNode(getPosition()).add(new HashNode(((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 938 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new HashNode(((ListNode)yyVals[-1+yyTop])));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 236:
					// line 942 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[-1+yyTop]));
		    yyVal = support.arg_concat(getPosition(), ((ListNode)yyVals[-6+yyTop]).add(new HashNode(((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 947 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new SplatNode(getPosition(), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 950 "DefaultRubyParser.y"
  {
	        }
  break;
case 239:
					// line 953 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(support.list_concat(new ArrayNode(getPosition()).add(((Node)yyVals[-3+yyTop])), ((ListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 240:
					// line 956 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((Node)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                  }
  break;
case 241:
					// line 959 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(), new ArrayNode(getPosition()).add(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 242:
					// line 963 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(), support.list_concat(new ArrayNode(getPosition()).add(((Node)yyVals[-6+yyTop])), new HashNode(((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 243:
					// line 967 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition()).add(new HashNode(((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 244:
					// line 971 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(), new ArrayNode(getPosition()).add(new HashNode(((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 245:
					// line 975 "DefaultRubyParser.y"
  {
                      yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[-3+yyTop])).add(new HashNode(((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 246:
					// line 979 "DefaultRubyParser.y"
  {
                      yyVal = support.list_concat(new ArrayNode(getPosition()).add(((Node)yyVals[-5+yyTop])), ((ListNode)yyVals[-3+yyTop])).add(new HashNode(((ListNode)yyVals[-1+yyTop])));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 247:
					// line 983 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(), new ArrayNode(getPosition()).add(((Node)yyVals[-6+yyTop])).add(new HashNode(((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 248:
					// line 987 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_concat(getPosition(), support.list_concat(new ArrayNode(getPosition()).add(((Node)yyVals[-8+yyTop])), ((ListNode)yyVals[-6+yyTop])).add(new HashNode(((ListNode)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                      yyVal = support.arg_blk_pass((Node)yyVal, ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 249:
					// line 991 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new SplatNode(getPosition(), ((Node)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 250:
					// line 994 "DefaultRubyParser.y"
  {}
  break;
case 251:
					// line 996 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 252:
					// line 998 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 254:
					// line 1004 "DefaultRubyParser.y"
  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 255:
					// line 1006 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "don't put space before argument parentheses");
		    yyVal = null;
		  }
  break;
case 256:
					// line 1010 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 257:
					// line 1012 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "don't put space before argument parentheses");
		    yyVal = ((Node)yyVals[-2+yyTop]);
		  }
  break;
case 258:
					// line 1017 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(), ((Node)yyVals[0+yyTop]));
                }
  break;
case 259:
					// line 1022 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 261:
					// line 1027 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 262:
					// line 1030 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 263:
					// line 1034 "DefaultRubyParser.y"
  {
		    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1037 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_concat(getPosition(), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 265:
					// line 1040 "DefaultRubyParser.y"
  {  
                    yyVal = new SplatNode(getPosition(), ((Node)yyVals[0+yyTop]));
		}
  break;
case 274:
					// line 1052 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(getPosition(), ((String)yyVals[0+yyTop]));
		}
  break;
case 275:
					// line 1056 "DefaultRubyParser.y"
  {
                    yyVal = new BeginNode(getPosition(), ((Node)yyVals[-1+yyTop]));
		}
  break;
case 276:
					// line 1059 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		    errorHandler.handleError(IErrors.WARN, null, "(...) interpreted as grouped expression");
                    yyVal = ((Node)yyVals[-2+yyTop]);
		}
  break;
case 277:
					// line 1064 "DefaultRubyParser.y"
  {
	            yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 278:
					// line 1067 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(), ((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 279:
					// line 1070 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 280:
					// line 1073 "DefaultRubyParser.y"
  {
                    yyVal = new CallNode(getPosition(), ((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                }
  break;
case 281:
					// line 1076 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = new ZArrayNode(getPosition()); /* zero length array*/
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                }
  break;
case 282:
					// line 1083 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(), ((ListNode)yyVals[-1+yyTop]));
                }
  break;
case 283:
					// line 1086 "DefaultRubyParser.y"
  {
		    yyVal = new ReturnNode(getPosition(), null);
                }
  break;
case 284:
					// line 1089 "DefaultRubyParser.y"
  {
                    yyVal = support.new_yield(getPosition(), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 285:
					// line 1092 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null, false);
                }
  break;
case 286:
					// line 1095 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null, false);
                }
  break;
case 287:
					// line 1098 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 288:
					// line 1100 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 289:
					// line 1104 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(), ((String)yyVals[-1+yyTop]), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 291:
					// line 1109 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) != null && ((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
		    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 292:
					// line 1116 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 293:
					// line 1125 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-2+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 294:
					// line 1134 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 295:
					// line 1136 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 296:
					// line 1138 "DefaultRubyParser.y"
  {
                    yyVal = new WhileNode(getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
  break;
case 297:
					// line 1145 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 298:
					// line 1147 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 299:
					// line 1149 "DefaultRubyParser.y"
  {
                    yyVal = new UntilNode(getPosition(), support.getConditionNode(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
  break;
case 300:
					// line 1158 "DefaultRubyParser.y"
  {
		    yyVal = new CaseNode(getPosition(), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 301:
					// line 1161 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(), null, ((Node)yyVals[-1+yyTop]));
                }
  break;
case 302:
					// line 1164 "DefaultRubyParser.y"
  {
		    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 303:
					// line 1167 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 304:
					// line 1169 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 305:
					// line 1172 "DefaultRubyParser.y"
  {
                    yyVal = new ForNode(getPosition(), ((Node)yyVals[-7+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-4+yyTop]));
                }
  break;
case 306:
					// line 1175 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 307:
					// line 1183 "DefaultRubyParser.y"
  {
  yyVal = new ClassNode(getPosition(), ((Colon2Node)yyVals[-4+yyTop]).getName(), new ScopeNode(support.getLocalNames().getNames(), ((Node)yyVals[-1+yyTop])), ((Node)yyVals[-3+yyTop]));
                    /* $<Node>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 308:
					// line 1189 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 309:
					// line 1192 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                }
  break;
case 310:
					// line 1198 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(getPosition(), ((Node)yyVals[-5+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((Node)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 311:
					// line 1205 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 312:
					// line 1213 "DefaultRubyParser.y"
  {
  yyVal = new ModuleNode(getPosition(), ((Colon2Node)yyVals[-3+yyTop]).getName(), new ScopeNode(support.getLocalNames().getNames(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 313:
					// line 1219 "DefaultRubyParser.y"
  {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push();
                }
  break;
case 314:
					// line 1227 "DefaultRubyParser.y"
  {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    yyVal = new DefnNode(getPosition(), ((String)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]),
		                      new ScopeNode(support.getLocalNames().getNames(), ((Node)yyVals[-1+yyTop])), Visibility.PRIVATE);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
  break;
case 315:
					// line 1237 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 316:
					// line 1239 "DefaultRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 317:
					// line 1245 "DefaultRubyParser.y"
  {
                    yyVal = new DefsNode(getPosition(), ((Node)yyVals[-7+yyTop]), ((String)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((Node)yyVals[-1+yyTop])));
                    /* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 318:
					// line 1251 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition());
                }
  break;
case 319:
					// line 1254 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition());
                }
  break;
case 320:
					// line 1257 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition());
                }
  break;
case 321:
					// line 1260 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition());
                }
  break;
case 322:
					// line 1264 "DefaultRubyParser.y"
  {
                    support.checkExpression(((Node)yyVals[0+yyTop]));
		    yyVal = ((Node)yyVals[0+yyTop]);
		}
  break;
case 331:
					// line 1281 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 333:
					// line 1286 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 335:
					// line 1291 "DefaultRubyParser.y"
  {}
  break;
case 337:
					// line 1294 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
                }
  break;
case 338:
					// line 1297 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
		}
  break;
case 339:
					// line 1300 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 340:
					// line 1304 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
		}
  break;
case 341:
					// line 1307 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 342:
					// line 1312 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) instanceof BlockPassNode) {
		        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 343:
					// line 1319 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 344:
					// line 1322 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1326 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 346:
					// line 1329 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 347:
					// line 1332 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 348:
					// line 1335 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), null);
                }
  break;
case 349:
					// line 1338 "DefaultRubyParser.y"
  {
                    yyVal = support.new_super(((Node)yyVals[0+yyTop]), getPosition());
                }
  break;
case 350:
					// line 1341 "DefaultRubyParser.y"
  {
                    yyVal = new ZSuperNode(getPosition());
                }
  break;
case 351:
					// line 1345 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
		}
  break;
case 352:
					// line 1347 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 353:
					// line 1351 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
		}
  break;
case 354:
					// line 1353 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 355:
					// line 1360 "DefaultRubyParser.y"
  {
		    yyVal = new WhenNode(getPosition(), ((ListNode)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 357:
					// line 1365 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(), new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop])), null, null));
                }
  break;
case 358:
					// line 1368 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new WhenNode(getPosition(), new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop])), null, null));
                }
  break;
case 361:
					// line 1378 "DefaultRubyParser.y"
  {
                    Node node;
		    if (((Node)yyVals[-3+yyTop]) != null) {
                       node = support.appendToBlock(support.node_assign(((Node)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(), "$!")), ((Node)yyVals[-1+yyTop]));
		    } else {
		       node = ((Node)yyVals[-1+yyTop]);
                    }
                    yyVal = new RescueBodyNode(getPosition(), ((Node)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
		}
  break;
case 362:
					// line 1387 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 363:
					// line 1389 "DefaultRubyParser.y"
  {
	            yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop]));
		}
  break;
case 366:
					// line 1395 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 368:
					// line 1400 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = new NilNode(null);
                    }
                }
  break;
case 371:
					// line 1410 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 373:
					// line 1415 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[0+yyTop]) == null) {
		        yyVal = new StrNode(getPosition(), "");
		    } else {
		        if (((Node)yyVals[0+yyTop]) instanceof EvStrNode) {
			    yyVal = new DStrNode(getPosition()).add(((Node)yyVals[0+yyTop]));
			} else {
		            yyVal = ((Node)yyVals[0+yyTop]);
			}
		    }
		}
  break;
case 375:
					// line 1428 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		}
  break;
case 376:
					// line 1432 "DefaultRubyParser.y"
  {
		     yyVal = ((Node)yyVals[-1+yyTop]);
		}
  break;
case 377:
					// line 1436 "DefaultRubyParser.y"
  {
		    if (((Node)yyVals[-1+yyTop]) == null) {
			  yyVal = new XStrNode(getPosition(), null);
		    } else {
		      if (((Node)yyVals[-1+yyTop]) instanceof StrNode) {
			  yyVal = new XStrNode(getPosition(), ((StrNode)yyVals[-1+yyTop]).getValue());
		      } else if (((Node)yyVals[-1+yyTop]) instanceof DStrNode) {
			  yyVal = new DXStrNode(getPosition()).add(((Node)yyVals[-1+yyTop]));
		      } else {
			yyVal = new DXStrNode(getPosition()).add(new ArrayNode(getPosition()).add(((Node)yyVals[-1+yyTop])));
		      }
		    }
                }
  break;
case 378:
					// line 1450 "DefaultRubyParser.y"
  {
		    int options = ((RegexpNode)yyVals[0+yyTop]).getOptions();
		    Node node = ((Node)yyVals[-1+yyTop]);

		    if (node == null) {
		        yyVal = new RegexpNode(getPosition(), "", options & ~ReOptions.RE_OPTION_ONCE);
		    } else if (node instanceof StrNode) {
		      yyVal = new RegexpNode(getPosition(), ((StrNode) node).getValue(), options & ~ReOptions.RE_OPTION_ONCE);
		    } else {
		        if (node instanceof DStrNode == false) {
			    node = new DStrNode(getPosition()).add(new ArrayNode(getPosition()).add(node));
		        } 

			yyVal = new DRegexpNode(getPosition(), options, (options & ReOptions.RE_OPTION_ONCE) != 0).add(node);
		    }
		 }
  break;
case 379:
					// line 1467 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition());
		 }
  break;
case 380:
					// line 1470 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 381:
					// line 1474 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 382:
					// line 1477 "DefaultRubyParser.y"
  {
                     Node node = ((Node)yyVals[-1+yyTop]);

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition()).add(node);
		     }

		     yyVal = ((ListNode)yyVals[-2+yyTop]).add(node);
		 }
  break;
case 384:
					// line 1488 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
	         }
  break;
case 385:
					// line 1492 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition());
		 }
  break;
case 386:
					// line 1495 "DefaultRubyParser.y"
  {
		     yyVal = ((ListNode)yyVals[-1+yyTop]);
		 }
  break;
case 387:
					// line 1499 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 388:
					// line 1502 "DefaultRubyParser.y"
  {
                     if (((ListNode)yyVals[-2+yyTop]) == null) {
		         yyVal = new ArrayNode(getPosition()).add(new StrNode(getPosition(), ((String)yyVals[-1+yyTop])));
		     } else {
                         yyVal = ((ListNode)yyVals[-2+yyTop]).add(new StrNode(getPosition(), ((String)yyVals[-1+yyTop])));
		     }
		 }
  break;
case 389:
					// line 1510 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 390:
					// line 1513 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 391:
					// line 1517 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 392:
					// line 1520 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
		 }
  break;
case 393:
					// line 1525 "DefaultRubyParser.y"
  {
                     yyVal = new StrNode(getPosition(), ((String)yyVal));
                  }
  break;
case 394:
					// line 1528 "DefaultRubyParser.y"
  {
                      yyVal = lexer.strTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 395:
					// line 1532 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((Node)yyVals[-1+yyTop]));
		      yyVal = new EvStrNode(getPosition(), ((Node)yyVals[0+yyTop]));
		  }
  break;
case 396:
					// line 1536 "DefaultRubyParser.y"
  {
		      yyVal = lexer.strTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 397:
					// line 1540 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((Node)yyVals[-2+yyTop]));
		      Node node = ((Node)yyVals[-1+yyTop]);

		      if (node instanceof NewlineNode) {
		        node = ((NewlineNode)node).getNextNode();
		      }

		      yyVal = support.newEvStrNode(getPosition(), node);
		  }
  break;
case 398:
					// line 1551 "DefaultRubyParser.y"
  {
		      yyVal = new GlobalVarNode(getPosition(), ((String)yyVals[0+yyTop]));
                 }
  break;
case 399:
					// line 1554 "DefaultRubyParser.y"
  {
		      yyVal = new InstVarNode(getPosition(), ((String)yyVals[0+yyTop]));
                 }
  break;
case 400:
					// line 1557 "DefaultRubyParser.y"
  {
		      yyVal = new ClassVarNode(getPosition(), ((String)yyVals[0+yyTop]));
                 }
  break;
case 402:
					// line 1563 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 407:
					// line 1573 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);

		    /* In ruby, it seems to be possible to get a*/
		    /* StrNode (NODE_STR) among other node type.  This */
		    /* is not possible for us.  We will always have a */
		    /* DStrNode (NODE_DSTR).*/
		    yyVal = new DSymbolNode(getPosition(), ((DStrNode)yyVals[-1+yyTop]));
		}
  break;
case 408:
					// line 1583 "DefaultRubyParser.y"
  {
                    if (((Number)yyVals[0+yyTop]) instanceof Long) {
                        yyVal = new FixnumNode(getPosition(), ((Long)yyVals[0+yyTop]).longValue());
                    } else {
                        yyVal = new BignumNode(getPosition(), ((BigInteger)yyVals[0+yyTop]));
                    }
                }
  break;
case 409:
					// line 1590 "DefaultRubyParser.y"
  {
	            yyVal = new FloatNode(getPosition(), ((Double)yyVals[0+yyTop]).doubleValue());
	        }
  break;
case 410:
					// line 1593 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode((((Number)yyVals[0+yyTop]) instanceof Long ? (Node) new FixnumNode(getPosition(), ((Long)yyVals[0+yyTop]).longValue()) : (Node) new BignumNode(getPosition(), ((BigInteger)yyVals[0+yyTop]))), "-@");
		}
  break;
case 411:
					// line 1596 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(new FloatNode(getPosition(), ((Double)yyVals[0+yyTop]).doubleValue()), "-@");
		}
  break;
case 412:
					// line 1605 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 413:
					// line 1608 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 414:
					// line 1611 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 415:
					// line 1614 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 416:
					// line 1617 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 417:
					// line 1620 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(getPosition());
                }
  break;
case 418:
					// line 1623 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(getPosition());
                }
  break;
case 419:
					// line 1626 "DefaultRubyParser.y"
  { 
                    yyVal = new TrueNode(getPosition());
                }
  break;
case 420:
					// line 1629 "DefaultRubyParser.y"
  {
                    yyVal = new FalseNode(getPosition());
                }
  break;
case 421:
					// line 1632 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(), getPosition().getFile());
                }
  break;
case 422:
					// line 1635 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(), getPosition().getLine());
                }
  break;
case 423:
					// line 1639 "DefaultRubyParser.y"
  {
                    /* Work around __LINE__ and __FILE__ */
                    if (yyVals[0+yyTop] instanceof INameNode) {
		        String name = ((INameNode)yyVals[0+yyTop]).getName();
                        yyVal = support.gettable(name, getPosition());
		    } else if (yyVals[0+yyTop] instanceof String) {
                        yyVal = support.gettable(((String)yyVals[0+yyTop]), getPosition());
		    } else {
		        yyVal = yyVals[0+yyTop];
		    }
                }
  break;
case 424:
					// line 1652 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(), yyVals[0+yyTop], null);
                }
  break;
case 427:
					// line 1659 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 428:
					// line 1662 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 429:
					// line 1664 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 430:
					// line 1667 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 431:
					// line 1672 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 432:
					// line 1676 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 433:
					// line 1680 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-5+yyTop]).intValue(), ((ListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 434:
					// line 1683 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 435:
					// line 1686 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 436:
					// line 1689 "DefaultRubyParser.y"
  {
	int h = ((Integer)yyVals[-1+yyTop]).intValue();
                    yyVal = new ArgsNode(getPosition(), h, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 437:
					// line 1692 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((ListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 438:
					// line 1695 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((ListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 439:
					// line 1698 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 440:
					// line 1701 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 441:
					// line 1704 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, null);
                }
  break;
case 442:
					// line 1708 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 443:
					// line 1711 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 444:
					// line 1714 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 445:
					// line 1717 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate argument name");
                    }
                    support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop]));
                    yyVal = new Integer(1);
                }
  break;
case 447:
					// line 1728 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(((Integer)yyVal).intValue() + 1);
                }
  break;
case 448:
					// line 1732 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[-2+yyTop]))) {
                        yyerror("duplicate optional argument name");
                    }
		    support.getLocalNames().getLocalIndex(((String)yyVals[-2+yyTop]));
                    yyVal = support.assignable(getPosition(), ((String)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 449:
					// line 1742 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 450:
					// line 1745 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-2+yyTop]).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 453:
					// line 1752 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("rest argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate rest argument name");
                    }
                    yyVal = new Integer(support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 454:
					// line 1760 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(-2);
                }
  break;
case 457:
					// line 1767 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("block argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate block argument name");
                    }
                    yyVal = new BlockArgNode(getPosition(), support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 458:
					// line 1776 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 459:
					// line 1779 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 460:
					// line 1783 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) instanceof SelfNode) {
                        yyVal = new SelfNode(null);
                    } else {
			support.checkExpression(((Node)yyVals[0+yyTop]));
			yyVal = ((Node)yyVals[0+yyTop]);
		    }
                }
  break;
case 461:
					// line 1791 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 462:
					// line 1793 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-2+yyTop]) instanceof ILiteralNode) {
                        /*case Constants.NODE_STR:
                        case Constants.NODE_DSTR:
                        case Constants.NODE_XSTR:
                        case Constants.NODE_DXSTR:
                        case Constants.NODE_DREGX:
                        case Constants.NODE_LIT:
                        case Constants.NODE_ARRAY:
                        case Constants.NODE_ZARRAY:*/
                        yyerror("Can't define single method for literals.");
                    }
		    support.checkExpression(((Node)yyVals[-2+yyTop]));
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 464:
					// line 1810 "DefaultRubyParser.y"
  {
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 465:
					// line 1813 "DefaultRubyParser.y"
  {
                    if (ListNodeUtil.getLength(((ListNode)yyVals[-1+yyTop])) % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((ListNode)yyVals[-1+yyTop]);
                }
  break;
case 467:
					// line 1821 "DefaultRubyParser.y"
  {
                    yyVal = ListNodeUtil.addAll(((ListNode)yyVals[-2+yyTop]), ((ListNode)yyVals[0+yyTop]));
                }
  break;
case 468:
					// line 1825 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((Node)yyVals[-2+yyTop])).add(((Node)yyVals[0+yyTop]));
                }
  break;
case 488:
					// line 1855 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 491:
					// line 1861 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 492:
					// line 1865 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 493:
					// line 1869 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
case 494:
					// line 1872 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
					// line 2921 "-"
        }
        yyTop -= YyLenClass.yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = YyLhsClass.yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          yyState = yyFinal;
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if (yyToken == 0) {
            return yyVal;
          }
          continue yyLoop;
        }
        if ((yyN = YyGindexClass.yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < YyTableClass.yyTable.length && YyCheckClass.yyCheck[yyN] == yyState)
          yyState = YyTableClass.yyTable[yyN];
        else
          yyState = YyDgotoClass.yyDgoto[yyM];
	 continue yyLoop;
      }
    }
  }

  protected static final class YyLhsClass {

    public static final short yyLhs [] = {              -1,
         97,    0,   18,   17,   19,   19,   19,   19,  100,   20,
         20,   20,   20,   20,   20,   20,   20,   20,   20,  101,
         20,   20,   20,   20,   20,   20,   20,   20,   20,   20,
         20,   20,   20,   20,   21,   21,   21,   21,   21,   21,
         29,   25,   25,   25,   25,   25,   48,   48,   48,  102,
         63,   24,   24,   24,   24,   24,   24,   24,   24,   69,
         69,   71,   71,   70,   70,   70,   70,   70,   70,   65,
         65,   74,   74,   66,   66,   66,   66,   66,   66,   66,
         66,   59,   59,   59,   59,   59,   59,   59,   59,   91,
         91,   16,   16,   16,   92,   92,   92,   92,   92,   85,
         85,   54,  104,   54,   93,   93,   93,   93,   93,   93,
         93,   93,   93,   93,   93,   93,   93,   93,   93,   93,
         93,   93,   93,   93,   93,   93,   93,   93,   93,   93,
        103,  103,  103,  103,  103,  103,  103,  103,  103,  103,
        103,  103,  103,  103,  103,  103,  103,  103,  103,  103,
        103,  103,  103,  103,  103,  103,  103,  103,  103,  103,
        103,  103,  103,  103,  103,  103,  103,  103,  103,  103,
        103,   22,   22,   22,   22,   22,   22,   22,   22,   22,
         22,   22,   22,   22,   22,   22,   22,   22,   22,   22,
         22,   22,   22,   22,   22,   22,   22,   22,   22,   22,
         22,   22,   22,   22,   22,   22,   22,   22,   22,   22,
         22,   22,  106,   22,   22,   22,   67,   78,   78,   78,
         78,   78,   78,   36,   36,   36,   36,   37,   37,   38,
         38,   38,   38,   38,   38,   38,   38,   38,   39,   39,
         39,   39,   39,   39,   39,   39,   39,   39,   39,   39,
        108,   41,   40,  109,   40,  110,   40,   44,   43,   43,
         72,   72,   64,   64,   64,   23,   23,   23,   23,   23,
         23,   23,   23,   23,   23,   23,   23,   23,   23,   23,
         23,   23,   23,   23,   23,   23,  111,   23,   23,   23,
         23,   23,   23,  113,  115,   23,  116,  117,   23,   23,
         23,   23,  118,  119,   23,  120,   23,  122,  123,   23,
        124,   23,  125,   23,  127,  128,   23,   23,   23,   23,
         23,   30,  112,  112,  112,  112,  114,  114,  114,   33,
         33,   31,   31,   57,   57,   58,   58,   58,   58,  129,
         62,   47,   47,   47,   26,   26,   26,   26,   26,   26,
        130,   61,  131,   61,   68,   73,   73,   73,   32,   32,
         79,   79,   77,   77,   77,   34,   34,   35,   35,   13,
         13,   13,    2,    3,    3,    4,    5,    6,   10,   10,
         28,   28,   12,   12,   11,   11,   27,   27,    7,    7,
          8,    8,    9,  132,    9,  133,    9,   56,   56,   56,
         56,   87,   86,   86,   86,   86,   15,   14,   14,   14,
         14,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   42,   81,   55,   55,   46,  134,   46,   46,
         51,   51,   52,   52,   52,   52,   52,   52,   52,   52,
         52,   94,   94,   94,   94,   95,   95,   53,   84,   84,
        135,  135,   96,   96,  136,  136,   50,   49,   49,    1,
        137,    1,   83,   83,   83,   75,   75,   76,   88,   88,
         88,   89,   89,   89,   89,   90,   90,   90,  126,  126,
         98,   98,  105,  105,  107,  107,  107,  121,  121,   99,
         99,   60,   82,   45,
    };
  } /* End of class YyLhsClass */

  protected static final class YyLenClass {

    public static final short yyLen [] = {           2,
          0,    2,    4,    2,    1,    1,    3,    2,    0,    4,
          3,    3,    3,    2,    3,    3,    3,    3,    3,    0,
          5,    4,    3,    3,    3,    6,    5,    5,    5,    3,
          3,    3,    3,    1,    1,    3,    3,    2,    2,    1,
          1,    1,    1,    2,    2,    2,    1,    4,    4,    0,
          5,    2,    3,    4,    5,    4,    5,    2,    2,    1,
          3,    1,    3,    1,    2,    3,    2,    2,    1,    1,
          3,    2,    3,    1,    4,    3,    3,    3,    3,    2,
          1,    1,    4,    3,    3,    3,    3,    2,    1,    1,
          1,    2,    1,    3,    1,    1,    1,    1,    1,    1,
          1,    1,    0,    4,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    3,    5,    3,    6,    5,    5,    5,    5,    4,
          3,    3,    3,    3,    3,    3,    3,    3,    3,    4,
          4,    2,    2,    3,    3,    3,    3,    3,    3,    3,
          3,    3,    3,    3,    3,    3,    2,    2,    3,    3,
          3,    3,    0,    4,    5,    1,    1,    1,    2,    2,
          5,    2,    3,    3,    4,    4,    6,    1,    1,    1,
          2,    5,    2,    5,    4,    7,    3,    1,    4,    3,
          5,    7,    2,    5,    4,    6,    7,    9,    3,    1,
          0,    2,    1,    0,    3,    0,    4,    2,    2,    1,
          1,    3,    3,    4,    2,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    3,    4,    3,    3,    2,    4,
          3,    3,    1,    4,    3,    1,    0,    6,    2,    1,
          2,    6,    6,    0,    0,    7,    0,    0,    7,    5,
          4,    5,    0,    0,    9,    0,    6,    0,    0,    8,
          0,    5,    0,    6,    0,    0,    9,    1,    1,    1,
          1,    1,    1,    1,    1,    2,    1,    1,    1,    1,
          5,    1,    2,    1,    1,    1,    2,    1,    3,    0,
          5,    2,    4,    4,    2,    4,    4,    3,    2,    1,
          0,    5,    0,    5,    5,    1,    4,    2,    1,    1,
          6,    0,    1,    1,    1,    2,    1,    2,    1,    1,
          1,    1,    1,    1,    2,    3,    3,    3,    3,    3,
          0,    3,    1,    2,    3,    3,    0,    3,    0,    2,
          0,    2,    1,    0,    3,    0,    4,    1,    1,    1,
          1,    2,    1,    1,    1,    1,    3,    1,    1,    2,
          2,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    0,    4,    2,
          4,    2,    6,    4,    4,    2,    4,    2,    2,    1,
          0,    1,    1,    1,    1,    1,    3,    3,    1,    3,
          1,    1,    2,    1,    1,    1,    2,    2,    0,    1,
          0,    5,    1,    2,    2,    1,    3,    3,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          0,    1,    0,    1,    0,    1,    1,    1,    1,    1,
          2,    0,    0,    0,
    };
  } /* End class YyLenClass */

  protected static final class YyDefRedClass {

    public static final short yyDefRed [] = {            1,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  294,  297,    0,    0,    0,  320,  321,    0,    0,
          0,  418,  417,  419,  420,    0,    0,    0,   20,    0,
        422,  421,    0,    0,  414,  413,    0,  416,  408,  409,
        425,  426,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  389,  391,  391,    0,    0,    0,    0,    0,  267,
          0,  374,  268,  269,  270,  271,  266,  370,  372,    2,
          0,    0,    0,    0,    0,    0,   35,    0,    0,  272,
          0,   43,    0,    0,    5,    0,   70,    0,   60,    0,
          0,    0,  371,    0,    0,  318,  319,  283,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  322,    0,
        273,  423,    0,   93,  311,  140,  151,  141,  164,  137,
        157,  147,  146,  162,  145,  144,  139,  165,  149,  138,
        152,  156,  158,  150,  143,  159,  166,  161,    0,    0,
          0,    0,  136,  155,  154,  167,  168,  169,  170,  171,
        135,  142,  133,  134,    0,    0,    0,   97,    0,  126,
        127,  124,  108,  109,  110,  113,  115,  111,  128,  129,
        116,  117,  121,  112,  114,  105,  106,  107,  118,  119,
        120,  122,  123,  125,  130,  461,    0,  460,  313,   98,
         99,  160,  153,  163,  148,  131,  132,   95,   96,    0,
          0,  102,  101,  100,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  489,  488,    0,    0,    0,
        490,    0,    0,    0,    0,    0,    0,  334,  335,    0,
          0,    0,    0,    0,  230,   45,  238,    0,    0,    0,
        466,   46,   44,    0,   59,    0,    0,  349,   58,   38,
          0,    9,  484,    0,    0,    0,  192,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,  218,    0,    0,
          0,    0,    0,  463,    0,    0,    0,    0,   68,    0,
        405,  404,  406,    0,  402,  403,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  207,   39,  208,  375,    4,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,  340,  342,  353,  351,  291,    0,
          0,    0,    0,    0,    0,    0,   72,    0,    0,    0,
          0,    0,  345,    0,  289,    0,    0,   90,    0,   92,
        410,  411,    0,  428,  306,  427,    0,    0,    0,    0,
          0,  480,  479,  315,    0,  103,    0,    0,  275,    0,
        325,  324,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  491,    0,    0,    0,    0,
          0,    0,  303,    0,  258,    0,    0,  231,  260,    0,
        233,  285,    0,    0,  253,  252,    0,    0,    0,    0,
          0,   11,   13,   12,    0,  287,    0,    0,    0,    0,
          0,    0,    0,  277,    0,    0,    0,  219,    0,  486,
        220,    0,  222,  281,    0,  465,  464,  282,    0,    0,
          0,    0,  393,  396,  394,  407,  392,  376,  390,  377,
        378,  379,  380,  383,    0,  385,    0,  386,    0,    0,
          0,   15,   16,   17,   18,   19,   36,   37,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  348,    0,    0,    0,  474,    0,    0,  475,  472,
        473,    0,    0,    0,   30,    0,    0,   23,   31,  261,
          0,   24,   33,    0,    0,   66,   73,    0,   25,   50,
         53,    0,  430,    0,    0,    0,    0,    0,   94,    0,
          0,    0,    0,    0,    0,  443,  442,  444,  452,  456,
        455,  451,    0,  440,    0,    0,  449,    0,  446,    0,
          0,    0,    0,    0,  365,  364,    0,    0,    0,    0,
        332,    0,  326,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  301,  329,  328,  295,  327,
        298,    0,    0,    0,    0,    0,    0,    0,  237,  468,
          0,  259,    0,    0,    0,    0,  467,  284,    0,    0,
        256,  250,    0,    0,    0,    0,    0,    0,    0,    0,
        224,   10,    0,    0,    0,   22,    0,    0,  276,  223,
          0,  262,    0,    0,    0,    0,    0,    0,    0,  382,
        384,  388,    0,    0,    0,  338,    0,    0,  336,    0,
          0,    0,    0,  347,    0,    0,    0,    0,  229,  346,
          0,  228,  344,   49,  343,   48,  265,    0,    0,   71,
          0,  309,    0,    0,  280,  312,    0,  316,    0,    0,
          0,  432,    0,  438,    0,  436,    0,  439,  453,  457,
        104,    0,    0,  367,  333,    0,    3,  369,    0,  330,
          0,    0,    0,    0,    0,    0,  300,  302,  358,    0,
          0,    0,    0,    0,    0,    0,    0,  235,    0,    0,
          0,    0,    0,  243,  255,  225,    0,    0,  226,    0,
          0,    0,   21,    0,    0,    0,  398,  399,  400,  401,
        395,    0,  337,    0,    0,    0,    0,    0,   29,    0,
         57,    0,    0,   27,    0,   28,   55,    0,    0,    0,
          0,    0,  429,  307,  462,    0,  448,    0,  314,    0,
        458,  450,    0,    0,  447,    0,    0,    0,    0,  366,
          0,    0,  368,    0,  292,    0,  293,    0,    0,    0,
          0,  304,  232,    0,  234,  249,  257,    0,  240,    0,
          0,    0,    0,  288,  221,  397,  339,  341,  354,  352,
          0,   26,  264,    0,    0,    0,  431,  437,    0,  434,
        435,    0,    0,    0,    0,    0,    0,  357,  359,  355,
        360,  296,  299,    0,    0,    0,    0,  239,    0,  245,
          0,  227,   51,  310,    0,    0,    0,    0,    0,    0,
          0,  361,    0,    0,  236,  241,    0,    0,    0,  244,
        317,  433,    0,  331,  305,    0,    0,  246,    0,  242,
          0,  247,    0,  248,
    };
  } /* End of class YyDefRedClass */

  protected static final class YyDgotoClass {

    public static final short yyDgoto [] = {             1,
        187,   60,   61,   62,   63,   64,  287,  284,  457,   65,
         66,  465,   67,   68,   69,  108,  205,  206,   71,   72,
         73,   74,   75,   76,   77,   78,  293,  291,  209,  258,
        710,  840,  711,  703,  707,  669,  670,  236,  621,  416,
        245,   80,  408,  612,  409,  365,   81,   82,  694,  781,
        565,  566,  567,  201,  211,  751,  227,  658,  212,   85,
        355,  336,  541,  529,   86,   87,  238,  395,   88,   89,
        264,  269,  595,   90,  270,  241,  578,  271,  378,  213,
        214,  274,  275,  568,  202,  285,   93,  113,  548,  512,
        114,  204,  519,  569,  570,  571,    2,  219,  220,  425,
        255,  681,  191,  574,  254,  427,  441,  246,  625,  731,
        633,  383,  222,  599,  722,  223,  723,  607,  844,  545,
        384,  542,  772,  370,  375,  374,  554,  776,  505,  507,
        506,  649,  648,  544,  572,  573,  371,
    };
  } /* End of class YyDgotoClass */

  protected static final class YySindexClass {

    public static final short yySindex [] = {            0,
          0,14064,14483,18684,18972,17554,17256,14064,15827,15827,
       6926,    0,    0,18780,14675,14675,    0,    0,14675,   14,
         65,    0,    0,    0,    0,15827,17166,  100,    0,   27,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,16691,16691, -147,14283,15827,15923,16691,19068,
      17632,    0,    0,    0,  161,  171,  141,16787,16691,    0,
       -117,    0,    0,    0,    0,    0,    0,    0,    0,    0,
         86,  723,  206,10550,    0,  -35,    0,  -47,   94,    0,
          7,    0,  -80,  204,    0,  233,    0,  275,    0,18876,
          0,  -44,    0,   44,  723,    0,    0,    0,   14,   65,
        100,    0,    0,15827,  187,14064,  217,  202,    0,  103,
          0,    0,   44,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,   29,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,17632,
        300,    0,    0,    0,   93,   96,   98,  206,   78,  106,
         73,  367,    0,  124,   78,    0,    0,   86, -156,  426,
          0,15827,15827,  196,  131,    0,  242,    0,    0,    0,
      16691,16691,16691,10550,    0,    0,    0,  176,  490,  501,
          0,    0,    0,14579,    0,14771,14675,    0,    0,    0,
         52,    0,    0,  477,  430,14064,    0,  137,  243,  237,
      14283,  545,    0,  551,  154,16691,  100,    0,  121,  156,
        512,  235,  156,    0,  498,  321,  153,    0,    0,    0,
          0,    0,    0,  339,    0,    0,  403,  505,  250,  280,
        613,  283, -263,  324,  332,    0,    0,    0,    0,    0,
      14383,15827,15827,15827,15827,14483,15827,15827,16691,16691,
      16691,16691,16691,16691,16691,16691,16691,16691,16691,16691,
      16691,16691,16691,16691,16691,16691,16691,16691,16691,16691,
      16691,16691,16691,16691,    0,    0,    0,    0,    0, 9090,
      15923,11110,17677,17677,16787,16019,    0,16019,14283,19068,
        611,16787,    0,  315,    0,  477,  206,    0,    0,    0,
          0,    0,   86,    0,    0,    0,17975,15923,17677,14064,
      15827,    0,    0,    0,  185,    0,16115,  391,    0,  237,
          0,    0,14064,  411,18006,15923,18043,16691,16691,16691,
      14064,  424,14064,16211,  432,    0,   80,   80,    0,18341,
      15923,18372,    0,  658,    0,16691,14867,    0,    0,14963,
          0,    0,  662, 5768,    0,    0,  -35,  100,   87,  664,
        669,    0,    0,    0,17256,    0,16691,14064,  586,18006,
      18043,16691,  678,    0,    0,  679, 4721,    0,16307,    0,
          0,16691,    0,    0,16691,    0,    0,    0,    0,18409,
      15923,18440,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,   16,    0,  691,    0,16691,16691,
        723,    0,    0,    0,    0,    0,    0,    0,  243, 1576,
       1576, 1576, 1576,  530,  530,10688,13379, 1576, 1576,12918,
      12918,  238,  238, 5160,  530,  530,  688,  688,  588,   75,
         75,  243,  243,  243,  -52,  -52,  -52,  392,    0,  401,
         65,    0,    0,  655,  415,    0,  442,   65,    0,    0,
          0,   65,   65,10550,    0,16691, 5621,    0,    0,    0,
        747,    0,    0,    0,  739,    0,    0,10550,    0,    0,
          0,   86,    0,15827,14064,    0,    0,   65,    0,  701,
         65,  532,  154,17930,  737,    0,    0,    0,    0,    0,
          0,    0,  240,    0,14064,   86,    0,  756,    0,  758,
        763,  507,  511,17256,    0,    0,    0,  480,14064,  558,
          0,  404,    0,  486,  401,  732,  489,  492, 5621,  391,
        575,  580,16691,  789,   78,    0,    0,    0,    0,    0,
          0,    0,    0,  748,    0,    0,15827,  502,    0,    0,
      16691,    0,  176,  805,16691,  176,    0,    0,16691,10550,
          0,    0,   19,  806,  828,  829,17677,17677,  836,15059,
          0,    0,15827,10550,  753,    0,10550,    0,    0,    0,
      16691,    0,    0,    0,  790,    0,    0,14064,  284,    0,
          0,    0,  243,  243,16691,    0,18588,14064,    0,14064,
      14064,16787,16691,    0,  315,  547,16787,16787,    0,    0,
        315,    0,    0,    0,    0,    0,    0,16691,16403,    0,
        -52,    0,   86,  618,    0,    0,  845,    0,16691,  100,
        623,    0,  133,    0,  240,    0,  -24,    0,    0,    0,
          0,19164,   78,    0,    0,14064,    0,    0,15827,    0,
        628,16691,  552,16691,16691,  636,    0,    0,    0,16499,
      14064,14064,14064,    0,   80,  658,15155,    0,  658,  658,
        854,15251,15347,    0,    0,    0,   65,   65,    0,  -35,
         87,   -4,    0, 4721,    0,  776,    0,    0,    0,    0,
          0,10550,    0,  781,  645,  651,  792,10550,    0,10550,
          0,16787,10550,    0,10550,    0,    0,10550,16691,    0,
      14064,14064,    0,    0,    0,  185,    0,  879,    0,  737,
          0,    0,  763,  883,    0,  763,  620,  165,    0,    0,
          0,14064,    0,   78,    0,16691,    0,16691,  286,  666,
        667,    0,    0,16691,    0,    0,    0,16691,    0,  895,
        896,16691,  901,    0,    0,    0,    0,    0,    0,    0,
      10550,    0,    0,  819,  683,14064,    0,    0,  133,    0,
          0,    0,18477,15923,18508,   93,14064,    0,    0,    0,
          0,    0,    0,14064, 9494,  658,15443,    0,15539,    0,
        658,    0,    0,    0,  684,  763,    0,    0,  859,    0,
          0,    0,  404,  689,    0,    0,16691,  911,16691,    0,
          0,    0,    0,    0,    0,  658,15635,    0,  658,    0,
      16691,    0,  658,    0,
    };
  } /* End of class YySindexClass */

  protected static final class YyRindexClass {

    public static final short yyRindex [] = {            0,
          0,  210,    0,    0,    0,    0,    0,  726,    0,    0,
        343,    0,    0,    0,13267,13352,    0,    0,13455, 4233,
       3689,    0,    0,    0,    0,    0,    0,16595,    0,    0,
          0,    0, 1897, 2889,    0,    0, 1993,    0,    0,    0,
          0,    0,    0,    0,    0,   82,    0,  863,  834,  419,
        621,    0,    0,    0,  625, -210,    0,    0,    0,    0,
       8212,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       1209, 5519, 6088,13915, 8297,14005,    0, 8393,    0,    0,
      17121,    0,13830,    0,    0,    0,    0,    0,    0,  464,
      13754,    0,    0,15731, 6345,    0,    0,    0, 8696, 7246,
        920, 5564, 5876,    0,    0,   82,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  736,  870,
        903, 1114,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0, 1456, 1582, 1669,    0, 1765,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       9428,    0,    0,    0,  406,    0,    0, 7055,    0,    0,
       7728,    0, 7335,    0,    0,    0,    0,  690,    0,  348,
          0,    0,    0,    0,    0,   55,    0,    0,    0,  525,
          0,    0,    0, 1799,    0,    0,    0,12925, 8027, 8027,
          0,    0,    0,    0,    0,    0,  922,    0,    0,    0,
          0,    0,    0,16883,    0,   49,    0,    0, 9180,13904,
         82,    0,  102,    0,  927,    0,  877,    0,  878,  878,
          0,  851,  851,    0,    0,    0,    0, 1016,    0, 1617,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0, 8781, 8877,    0,    0,    0,    0,    0,
       1867,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        863,    0,    0,    0,    0,    0,    0,    0,   82,  577,
        600,    0,    0, 6735,    0,    0,  119,    0, 6390,    0,
          0,    0,    0,    0,    0,    0,    0,  863,    0,  726,
          0,    0,    0,    0,  132,    0,   21,  417,    0, 7813,
          0,    0,  473, 6497,    0,  863,    0,    0,    0,    0,
        463,    0,   68,    0,    0,    0,    0,    0,  794,    0,
        863,    0,    0, 8027,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  941,    0,    0,  115,  927,  927,  374,
          0,    0,    0,    0,    0,    0,    0,   49,    0,    0,
          0,    0,    0,    0,  465,    0,  877,    0,  894,    0,
          0,   89,    0,    0,  860,    0,    0,    0, 1860,    0,
        863,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       6446,    0,    0,    0,    0,    0,    0,    0, 9253, 8116,
       8600,11529,11708,11070,11285,11794, 9857,11871,11922,11973,
      12011,10497,10570,    0,11362,11435,10920,10993,10647,10122,
      10199, 9342, 9584, 9673, 6624, 6624, 6817, 4585, 3241, 5120,
      15731,    0, 3337,    0, 4681,    0, 5024, 3785,    0,    0,
          0, 5463, 5463,12271,    0,    0,17363,    0,    0,    0,
          0,    0,    0, 7546,    0,    0,    0,12309,    0,    0,
          0,    0,    0,    0,  726, 5977, 6289,    0,    0,    0,
       7425,    0,  927,    0,  569,    0,    0,    0,    0,    0,
          0,    0,  377,    0,  726,    0,    0,  209,    0,  209,
        209,  913,    0,    0,    0,    0,   60,  232,  550,  727,
          0,  727,    0, 2345, 4137,    0, 2441, 2793,13007,  727,
          0,    0,    0,  493,    0,    0,    0,    0,    0,    0,
          0,  744,  152,    0, 1507, 1736,    0,    0,    0,    0,
          0,    0,13167, 8027,    0,    0,    0,    0,    0,  110,
          0,    0,    0,  952,    0,    0,    0,    0,    0,    0,
          0,    0,    0,12348,    0,    0,12387,  472,    0,    0,
          0,    0,  940,  731,    0, 1252, 1482,   49,    0,    0,
          0,    0, 9746,10049,    0,    0,    0,   68,    0,   68,
         49,    0,    0,    0, 8511,14203,    0,    0,    0,    0,
      13847,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       6817,    0,    0,    0,    0,    0,    0,    0,    0,  927,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,   68,    0,    0,    0,    0,
          0,    0, 7909,    0,    0,    0,    0,    0,    0,    0,
        337,   68,   68, 1111,    0, 8027,    0,    0, 8027,  952,
          0,    0,    0,    0,    0,    0,   48,   48,    0,    0,
        927,    0,    0,  877, 2310,    0,    0,    0,    0,    0,
          0,12425,    0,    0,    0,    0,    0,12464,    0,12729,
          0,    0,12765,    0,12816,    0,    0,12852,    0,11832,
         49,  726,    0,    0,    0,  132,    0,    0,    0,    0,
          0,    0,  209,  209,    0,  209,    0,    0,  111,    0,
        114,  726,    0,    0,    0,    0,    0,    0,  727,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  952,
        952,    0,    0,    0,    0,    0,    0,    0,    0,    0,
      12889,    0,    0,    0,    0,  726,    0,    0,    0,    0,
          0,  149,    0,  863,    0,  406,  473,    0,    0,    0,
          0,    0,    0,   68, 8027,  952,    0,    0,    0,    0,
        952,    0,    0,    0,    0,  209,  536,  122,    0,  835,
        892,    0,  727,    0,    0,    0,    0,  952,    0,    0,
          0,    0,  180,    0,    0,  952,    0,    0,  952,    0,
          0,    0,  952,    0,
    };
  } /* End of class YyRindexClass */

  protected static final class YyGindexClass {

    public static final short yyGindex [] = {            0,
          0,    0,    0,  935,    0,    0,    0,  631, -205,    0,
          0,    0,    0,    0,    0,  992,   28, -359,    0,   66,
       1042,  -15,   57,    2,  -23,    0,    0,    0,   36,  460,
       -366,    0,  135,    0,    0,  -13, -256,  857,    0,    0,
          1,  997, -189,   26,    0,    0, -207,    0,  216, -362,
        231,  445, -608,    0,  663,    0,  352, -318, 1092,  986,
        933,    0, -430, -190,  924,  -30,  985, -367,  -11,   31,
       -235,    8,    0,    0,  -10, -307,    0, -303,  199, 1024,
       1306,  795,    0,  341,  -20,    0,   25,  888, -276,    0,
        -32,    4,    9,  342,    0, -565,    0,   35,  970,    0,
          0,    0,    0,    0,   83,    0,  266,    0,    0,    0,
          0, -213,    0, -379,    0,    0,    0,    0,    0,    0,
         45,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,
    };
  } /* End of class YyGindexClass */

  protected static final class YyTableClass {

    public static final short[] yyTable = YyTables.yyTable();
  } /* End of class YyTableClass */

  protected static final class YyCheckClass {

    public static final short[] yyCheck = YyTables.yyCheck();
  } /* End of class YyCheckClass */




  protected static final class YyNameClass {

    public static final String yyName [] = {    
    "end-of-file",null,null,null,null,null,null,null,null,null,"'\\n'",
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"' '","'!'",null,null,null,"'%'",
    "'&'",null,"'('","')'","'*'","'+'","','","'-'","'.'","'/'",null,null,
    null,null,null,null,null,null,null,null,"':'","';'","'<'","'='","'>'",
    "'?'",null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,"'['",null,"']'","'^'",null,"'`'",null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,"'{'","'|'","'}'","'~'",null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,"kCLASS","kMODULE","kDEF","kUNDEF","kBEGIN","kRESCUE","kENSURE",
    "kEND","kIF","kUNLESS","kTHEN","kELSIF","kELSE","kCASE","kWHEN",
    "kWHILE","kUNTIL","kFOR","kBREAK","kNEXT","kREDO","kRETRY","kIN",
    "kDO","kDO_COND","kDO_BLOCK","kRETURN","kYIELD","kSUPER","kSELF",
    "kNIL","kTRUE","kFALSE","kAND","kOR","kNOT","kIF_MOD","kUNLESS_MOD",
    "kWHILE_MOD","kUNTIL_MOD","kRESCUE_MOD","kALIAS","kDEFINED","klBEGIN",
    "klEND","k__LINE__","k__FILE__","tIDENTIFIER","tFID","tGVAR","tIVAR",
    "tCONSTANT","tCVAR","tINTEGER","tFLOAT","tSTRING_CONTENT","tNTH_REF",
    "tBACK_REF","tREGEXP_END","tUPLUS","tUMINUS","tPOW","tCMP","tEQ",
    "tEQQ","tNEQ","tGEQ","tLEQ","tANDOP","tOROP","tMATCH","tNMATCH",
    "tDOT2","tDOT3","tAREF","tASET","tLSHFT","tRSHFT","tCOLON2","tCOLON3",
    "tOP_ASGN","tASSOC","tLPAREN","tLPAREN_ARG","tLBRACK","tLBRACE",
    "tLBRACE_ARG","tSTAR","tAMPER","tSYMBEG","tSTRING_BEG","tXSTRING_BEG",
    "tREGEXP_BEG","tWORDS_BEG","tQWORDS_BEG","tSTRING_DBEG",
    "tSTRING_DVAR","tSTRING_END","tLOWEST","tUMINUS_NUM","tLAST_TOKEN",
    };
  } /* End of class YyNameClass */


					// line 1876 "DefaultRubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public RubyParserResult parse(LexerSource source) {
        support.reset();
        support.setResult(new RubyParserResult());

	lexer.setSource(source);
	try {
            yyparse(lexer, null);
	} catch (Exception excptn) {
            excptn.printStackTrace();
	}

        return support.getResult();
    }

    public void init(RubyParserConfiguration configuration) {
        support.setConfiguration(configuration);
    }

    // +++
    // Helper Methods
    
    void yyerrok() {}

    private SourcePosition getPosition() {
        return lexer.getPosition();
    }
}
					// line 7877 "-"
