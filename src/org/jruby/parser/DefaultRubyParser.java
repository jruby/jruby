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

import org.ablaf.ast.*;
import org.ablaf.common.*;
import org.ablaf.lexer.*;
import org.ablaf.parser.*;

import org.jruby.common.*;
import org.jruby.lexer.yacc.*;
import org.jruby.ast.*;
import org.jruby.ast.types.*;

import org.jruby.ast.util.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

public class DefaultRubyParser implements IParser {
    private ParserSupport support;
    private RubyYaccLexer lexer;

    private IErrorHandler errorHandler;

    public DefaultRubyParser() {
        this.support = new ParserSupport();
        this.lexer = new RubyYaccLexer();
	// lame
	this.lexer.setParserSupport(support);
    }

    public void setErrorHandler(IErrorHandler errorHandler) {
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
					// line 77 "-"
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
  public Object yyparse (IYaccLexer yyLex, Object yydebug)
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
  public Object yyparse (IYaccLexer yyLex)
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
					// line 219 "DefaultRubyParser.y"
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
					// line 229 "DefaultRubyParser.y"
  {
                  if (((INode)yyVals[0+yyTop]) != null && !support.isCompileForEval()) {
                      /* last expression should not be void */
                      if (((INode)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(ListNodeUtil.getLast(((BlockNode)yyVals[0+yyTop])));
                      } else {
                          support.checkUselessStatement(((INode)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), ((INode)yyVals[0+yyTop])));
                  support.updateTopLocalVariables();
                  support.setClassNest(0);
              }
  break;
case 3:
					// line 246 "DefaultRubyParser.y"
  {
                 INode node = ((INode)yyVals[-3+yyTop]);

		 if (((RescueBodyNode)yyVals[-2+yyTop]) != null) {
		    node = new RescueNode(getPosition(), ((INode)yyVals[-3+yyTop]), ((RescueBodyNode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]));
		 } else if (((INode)yyVals[-1+yyTop]) != null) {
		    errorHandler.handleError(IErrors.WARN, null, "else without rescue is useless");
                    node = support.appendToBlock(((INode)yyVals[-3+yyTop]), ((INode)yyVals[-1+yyTop]));
		 }
		 if (((INode)yyVals[0+yyTop]) != null) {
		    node = new EnsureNode(getPosition(), node, ((INode)yyVals[0+yyTop]));
		 }

		 yyVal = node;
             }
  break;
case 4:
					// line 262 "DefaultRubyParser.y"
  {
                  if (((INode)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		  }
                  yyVal = ((INode)yyVals[-1+yyTop]);
              }
  break;
case 6:
					// line 270 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((INode)yyVals[0+yyTop]), getPosition());
                }
  break;
case 7:
					// line 273 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((INode)yyVals[-2+yyTop]), support.newline_node(((INode)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 8:
					// line 276 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 9:
					// line 280 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 10:
					// line 282 "DefaultRubyParser.y"
  {
                    yyVal = new AliasNode(getPosition(), ((String)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 11:
					// line 285 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 12:
					// line 288 "DefaultRubyParser.y"
  {
                    yyVal = new VAliasNode(getPosition(), ((String)yyVals[-1+yyTop]), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
                }
  break;
case 13:
					// line 291 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 14:
					// line 295 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 15:
					// line 298 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((INode)yyVals[-2+yyTop]), null);
                }
  break;
case 16:
					// line 301 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), null, ((INode)yyVals[-2+yyTop]));
                }
  break;
case 17:
					// line 304 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-2+yyTop]) != null && ((INode)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((INode)yyVals[-2+yyTop]), false);
                    }
                }
  break;
case 18:
					// line 311 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-2+yyTop]) != null && ((INode)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((INode)yyVals[-2+yyTop]));
                    }
                }
  break;
case 19:
					// line 319 "DefaultRubyParser.y"
  {
		  yyVal = new RescueNode(getPosition(), ((INode)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(), null,((INode)yyVals[0+yyTop]), null), null);
                }
  break;
case 20:
					// line 323 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push();
                }
  break;
case 21:
					// line 328 "DefaultRubyParser.y"
  {
                    support.getResult().setBeginNodes(support.appendToBlock(support.getResult().getBeginNodes(), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop]))));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 22:
					// line 333 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("END in method; use at_exit");
                    }
                    yyVal = new IterNode(getPosition(), null, new PostExeNode(getPosition()), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 23:
					// line 339 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 24:
					// line 343 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
		    if (((INode)yyVals[-2+yyTop]) instanceof MultipleAsgnNode && 
			((MultipleAsgnNode) ((INode)yyVals[-2+yyTop])).getHeadNode() == null) {
		        ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop])));
		    } else {
                        ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
		    }
		    yyVal = ((INode)yyVals[-2+yyTop]);
                }
  break;
case 25:
					// line 353 "DefaultRubyParser.y"
  {
 		    support.checkExpression(((INode)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();
		        if (((String)yyVals[-1+yyTop]).equals("||")) {
	                    ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(), support.gettable(name, getPosition()), ((INode)yyVals[-2+yyTop]));
			    /* XXX
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (((String)yyVals[-1+yyTop]).equals("&&")) {
	                    ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(), support.gettable(name, getPosition()), ((INode)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
                                ((IAssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, getPosition()), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null;
		    }
		}
  break;
case 26:
					// line 378 "DefaultRubyParser.y"
  {
                    /* Much smaller than ruby block */
                    yyVal = new OpElementAsgnNode(getPosition(), ((INode)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[0+yyTop]));

                }
  break;
case 27:
					// line 383 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 28:
					// line 386 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 29:
					// line 389 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 30:
					// line 392 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 31:
					// line 396 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 32:
					// line 399 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-2+yyTop]) instanceof MultipleAsgnNode && 
			((MultipleAsgnNode) ((INode)yyVals[-2+yyTop])).getHeadNode() == null) {
		        ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop])));
		    } else {
                        ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
		    }
		    yyVal = ((INode)yyVals[-2+yyTop]);
		}
  break;
case 33:
					// line 408 "DefaultRubyParser.y"
  {
                    ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
		    yyVal = ((INode)yyVals[-2+yyTop]);
		}
  break;
case 36:
					// line 415 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 37:
					// line 418 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 421 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 39:
					// line 424 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 41:
					// line 429 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
		    yyVal = ((INode)yyVals[0+yyTop]); /*Do we really need this set? $1 is $$?*/
		}
  break;
case 44:
					// line 436 "DefaultRubyParser.y"
  {
                    yyVal = new ReturnNode(getPosition(), support.ret_args(((INode)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 45:
					// line 439 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition(), support.ret_args(((INode)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 46:
					// line 442 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition(), support.ret_args(((INode)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 48:
					// line 447 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 49:
					// line 450 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 50:
					// line 454 "DefaultRubyParser.y"
  {
                      support.getBlockNames().push();
		  }
  break;
case 51:
					// line 456 "DefaultRubyParser.y"
  {
                      yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                      support.getBlockNames().pop();
		  }
  break;
case 52:
					// line 461 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 53:
					// line 464 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), getPosition()); 
	            if (((IterNode)yyVals[0+yyTop]) != null) {
                        if (yyVal instanceof BlockPassNode) {
                            errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVal));
                        yyVal = ((INode)yyVals[-1+yyTop]);
		   }
                }
  break;
case 54:
					// line 474 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 55:
					// line 477 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop])); 
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
		 }
  break;
case 56:
					// line 487 "DefaultRubyParser.y"
  {
		      /* not in ruby support.checkExpression($1); */
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 57:
					// line 491 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop])); 
		    if (((IterNode)yyVals[0+yyTop]) != null) {
		        if (yyVal instanceof BlockPassNode) {
                            errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                        }
                        ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVal));
			yyVal = ((IterNode)yyVals[0+yyTop]);
		    }
	        }
  break;
case 58:
					// line 501 "DefaultRubyParser.y"
  {
		    yyVal = support.new_super(((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
		}
  break;
case 59:
					// line 504 "DefaultRubyParser.y"
  {
	            yyVal = new YieldNode(getPosition(), ((INode)yyVals[0+yyTop])); /* .setPosFrom($2);*/
		}
  break;
case 61:
					// line 509 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
		}
  break;
case 63:
					// line 514 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(), new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop])), null);
                }
  break;
case 64:
					// line 518 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((IListNode)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 521 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((IListNode)yyVals[-1+yyTop]).add(((INode)yyVals[0+yyTop])), null);
                }
  break;
case 66:
					// line 524 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((IListNode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 527 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((IListNode)yyVals[-1+yyTop]), new StarNode());
                }
  break;
case 68:
					// line 530 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, ((INode)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 533 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, new StarNode());
                }
  break;
case 71:
					// line 538 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 72:
					// line 542 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 73:
					// line 545 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-2+yyTop]).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 74:
					// line 549 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(), yyVals[0+yyTop], null);
                }
  break;
case 75:
					// line 552 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((INode)yyVals[-3+yyTop]), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 76:
					// line 555 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 77:
					// line 558 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 78:
					// line 561 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 79:
					// line 564 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
		}
  break;
case 80:
					// line 571 "DefaultRubyParser.y"
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
					// line 581 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((INode)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 82:
					// line 586 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(), yyVals[0+yyTop], null);
                }
  break;
case 83:
					// line 589 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((INode)yyVals[-3+yyTop]), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 84:
					// line 592 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 85:
					// line 595 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
 	        }
  break;
case 86:
					// line 598 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 87:
					// line 601 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
			    yyerror("dynamic constant assignment");
		    }
			
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
	        }
  break;
case 88:
					// line 608 "DefaultRubyParser.y"
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
					// line 617 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 90:
					// line 622 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 92:
					// line 627 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(), null, ((String)yyVals[0+yyTop]));
		}
  break;
case 93:
					// line 630 "DefaultRubyParser.y"
  {
                    /* $1 was $$ in ruby?*/
                    yyVal = new Colon2Node(getPosition(), null, ((String)yyVals[0+yyTop]));
 	        }
  break;
case 94:
					// line 634 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(), ((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
		}
  break;
case 98:
					// line 641 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 99:
					// line 645 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 102:
					// line 653 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 103:
					// line 656 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 104:
					// line 658 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((INode)yyVals[-3+yyTop]), new UndefNode(getPosition(), ((String)yyVals[0+yyTop])));
                }
  break;
case 105:
					// line 662 "DefaultRubyParser.y"
  { yyVal = "|"; }
  break;
case 106:
					// line 663 "DefaultRubyParser.y"
  { yyVal = "^"; }
  break;
case 107:
					// line 664 "DefaultRubyParser.y"
  { yyVal = "&"; }
  break;
case 108:
					// line 665 "DefaultRubyParser.y"
  { yyVal = "<=>"; }
  break;
case 109:
					// line 666 "DefaultRubyParser.y"
  { yyVal = "=="; }
  break;
case 110:
					// line 667 "DefaultRubyParser.y"
  { yyVal = "==="; }
  break;
case 111:
					// line 668 "DefaultRubyParser.y"
  { yyVal = "=~"; }
  break;
case 112:
					// line 669 "DefaultRubyParser.y"
  { yyVal = ">"; }
  break;
case 113:
					// line 670 "DefaultRubyParser.y"
  { yyVal = ">="; }
  break;
case 114:
					// line 671 "DefaultRubyParser.y"
  { yyVal = "<"; }
  break;
case 115:
					// line 672 "DefaultRubyParser.y"
  { yyVal = "<="; }
  break;
case 116:
					// line 673 "DefaultRubyParser.y"
  { yyVal = "<<"; }
  break;
case 117:
					// line 674 "DefaultRubyParser.y"
  { yyVal = ">>"; }
  break;
case 118:
					// line 675 "DefaultRubyParser.y"
  { yyVal = "+"; }
  break;
case 119:
					// line 676 "DefaultRubyParser.y"
  { yyVal = "-"; }
  break;
case 120:
					// line 677 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 121:
					// line 678 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 122:
					// line 679 "DefaultRubyParser.y"
  { yyVal = "/"; }
  break;
case 123:
					// line 680 "DefaultRubyParser.y"
  { yyVal = "%"; }
  break;
case 124:
					// line 681 "DefaultRubyParser.y"
  { yyVal = "**"; }
  break;
case 125:
					// line 682 "DefaultRubyParser.y"
  { yyVal = "~"; }
  break;
case 126:
					// line 683 "DefaultRubyParser.y"
  { yyVal = "+@"; }
  break;
case 127:
					// line 684 "DefaultRubyParser.y"
  { yyVal = "-@"; }
  break;
case 128:
					// line 685 "DefaultRubyParser.y"
  { yyVal = "[]"; }
  break;
case 129:
					// line 686 "DefaultRubyParser.y"
  { yyVal = "[]="; }
  break;
case 130:
					// line 687 "DefaultRubyParser.y"
  { yyVal = "`"; }
  break;
case 172:
					// line 698 "DefaultRubyParser.y"
  {
		      /* not in ruby support.checkExpression($3); */
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 702 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((INode)yyVals[-4+yyTop]), new RescueNode(getPosition(), ((INode)yyVals[-2+yyTop]), new RescueBodyNode(getPosition(), null,((INode)yyVals[0+yyTop]), null), null));
		}
  break;
case 174:
					// line 705 "DefaultRubyParser.y"
  {
		    support.checkExpression(((INode)yyVals[0+yyTop]));
		    if (yyVals[-2+yyTop] != null) {
		        String name = ((INameNode)yyVals[-2+yyTop]).getName();

		        if (((String)yyVals[-1+yyTop]).equals("||")) {
	                    ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
	                    yyVal = new OpAsgnOrNode(getPosition(), support.gettable(name, getPosition()), ((INode)yyVals[-2+yyTop]));
			    /* FIXME
			    if (is_asgn_or_id(vid)) {
				$$->nd_aid = vid;
			    }
			    */
			} else if (((String)yyVals[-1+yyTop]).equals("&&")) {
	                    ((IAssignableNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
                            yyVal = new OpAsgnAndNode(getPosition(), support.gettable(name, getPosition()), ((INode)yyVals[-2+yyTop]));
			} else {
			    yyVal = yyVals[-2+yyTop];
                            if (yyVal != null) {
                                ((IAssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.gettable(name, getPosition()), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])));
                            }
			}
		    } else {
 		        yyVal = null; /* XXX 0; */
		    }
                }
  break;
case 175:
					// line 731 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(), ((INode)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 734 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 177:
					// line 737 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 178:
					// line 740 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 179:
					// line 743 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 180:
					// line 747 "DefaultRubyParser.y"
  {
		    yyerror("constant re-assignment");
		    yyVal = null;
	        }
  break;
case 181:
					// line 751 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 182:
					// line 755 "DefaultRubyParser.y"
  {
		    support.checkExpression(((INode)yyVals[-2+yyTop]));
		    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), false);
                }
  break;
case 183:
					// line 760 "DefaultRubyParser.y"
  {
		    support.checkExpression(((INode)yyVals[-2+yyTop]));
		    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new DotNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), true);
                }
  break;
case 184:
					// line 765 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "+", ((INode)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 768 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "-", ((INode)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 771 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "*", ((INode)yyVals[0+yyTop]));
                }
  break;
case 187:
					// line 774 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "/", ((INode)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 777 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "%", ((INode)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 780 "DefaultRubyParser.y"
  {
		      yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "**", ((INode)yyVals[0+yyTop]));
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
                        $$ = support.getOperatorCallNode($<INode>$, "-@");
                    }
		    */
                }
  break;
case 190:
					// line 799 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(support.getOperatorCallNode((((Number)yyVals[-2+yyTop]) instanceof Long ? (INode) new FixnumNode(getPosition(), ((Long)yyVals[-2+yyTop]).longValue()) : (INode)new BignumNode(getPosition(), ((BigInteger)yyVals[-2+yyTop]))), "**", ((INode)yyVals[0+yyTop])), "-@");
                }
  break;
case 191:
					// line 802 "DefaultRubyParser.y"
  {
	            yyVal = support.getOperatorCallNode(support.getOperatorCallNode(new FloatNode(getPosition(), ((Double)yyVals[-3+yyTop]).doubleValue()), "**", ((INode)yyVals[0+yyTop])), "-@");
                }
  break;
case 192:
					// line 805 "DefaultRubyParser.y"
  {
 	            if (((INode)yyVals[0+yyTop]) != null && ((INode)yyVals[0+yyTop]) instanceof ILiteralNode) {
		        yyVal = ((INode)yyVals[0+yyTop]);
		    } else {
                        yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "+@");
		    }
                }
  break;
case 193:
					// line 812 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "-@");
		}
  break;
case 194:
					// line 815 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "|", ((INode)yyVals[0+yyTop]));
                }
  break;
case 195:
					// line 818 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "^", ((INode)yyVals[0+yyTop]));
                }
  break;
case 196:
					// line 821 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "&", ((INode)yyVals[0+yyTop]));
                }
  break;
case 197:
					// line 824 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<=>", ((INode)yyVals[0+yyTop]));
                }
  break;
case 198:
					// line 827 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">", ((INode)yyVals[0+yyTop]));
                }
  break;
case 199:
					// line 830 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">=", ((INode)yyVals[0+yyTop]));
                }
  break;
case 200:
					// line 833 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<", ((INode)yyVals[0+yyTop]));
                }
  break;
case 201:
					// line 836 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<=", ((INode)yyVals[0+yyTop]));
                }
  break;
case 202:
					// line 839 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "==", ((INode)yyVals[0+yyTop]));
                }
  break;
case 203:
					// line 842 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "===", ((INode)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 845 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "==", ((INode)yyVals[0+yyTop])));
                }
  break;
case 205:
					// line 848 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 851 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getMatchNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop])));
                }
  break;
case 207:
					// line 854 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 208:
					// line 857 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "~");
                }
  break;
case 209:
					// line 860 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<<", ((INode)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 863 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">>", ((INode)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 866 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 869 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 872 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 214:
					// line 874 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 215:
					// line 878 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 881 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 885 "DefaultRubyParser.y"
  {
		    support.checkExpression(((INode)yyVals[0+yyTop]));
	            yyVal = ((INode)yyVals[0+yyTop]);   
		}
  break;
case 219:
					// line 891 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 220:
					// line 895 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-1+yyTop]);
                }
  break;
case 221:
					// line 898 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IListNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop])));
                }
  break;
case 222:
					// line 902 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new HashNode(((IListNode)yyVals[-1+yyTop])));
                }
  break;
case 223:
					// line 905 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));

                    yyVal = new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop])));
                }
  break;
case 224:
					// line 911 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 225:
					// line 914 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-2+yyTop]);
                }
  break;
case 226:
					// line 917 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop]));
                }
  break;
case 227:
					// line 921 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = ((IListNode)yyVals[-4+yyTop]).add(((INode)yyVals[-2+yyTop]));
                }
  break;
case 230:
					// line 929 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "parenthesize argument(s) for future version");
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 933 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((IListNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 232:
					// line 936 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((IListNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 939 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new HashNode(((IListNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 942 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((IListNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 945 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((IListNode)yyVals[-3+yyTop]).add(new HashNode(((IListNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 236:
					// line 948 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((IListNode)yyVals[-6+yyTop]).add(new HashNode(((IListNode)yyVals[-4+yyTop]))).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 952 "DefaultRubyParser.y"
  {
		    /* FIXME*/
                    /* $$ = support.arg_blk_pass(new RestArgsNode(getPosition(), $2), $3);*/
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 957 "DefaultRubyParser.y"
  {
	        }
  break;
case 239:
					// line 960 "DefaultRubyParser.y"
  {
		      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-3+yyTop])).add(((IListNode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 240:
					// line 963 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
                  }
  break;
case 241:
					// line 966 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-4+yyTop])).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 242:
					// line 969 "DefaultRubyParser.y"
  {
                      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-6+yyTop])).add(new HashNode(((IListNode)yyVals[-4+yyTop]))).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 243:
					// line 972 "DefaultRubyParser.y"
  {
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new HashNode(((IListNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 244:
					// line 975 "DefaultRubyParser.y"
  {
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new ExpandArrayNode(((IListNode)yyVals[-4+yyTop]))).add(((INode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 245:
					// line 978 "DefaultRubyParser.y"
  {
		    /* list_append and I just added the hash*/
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-3+yyTop])).add(new ExpandArrayNode(((IListNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 246:
					// line 982 "DefaultRubyParser.y"
  {
		    /* list_append and I just added the hash*/
	            yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-5+yyTop])).add(((IListNode)yyVals[-3+yyTop])).add(new ExpandArrayNode(((IListNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 247:
					// line 986 "DefaultRubyParser.y"
  {
		    /* list_append and I just added the hash*/
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-6+yyTop])).add(new ExpandArrayNode(((IListNode)yyVals[-4+yyTop]))).add(((INode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 248:
					// line 990 "DefaultRubyParser.y"
  {
		    /* list_append and I just added the hash*/
		      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(((INode)yyVals[-8+yyTop])).add(((IListNode)yyVals[-6+yyTop])).add(new ExpandArrayNode(((IListNode)yyVals[-4+yyTop]))).add(((INode)yyVals[-1+yyTop])), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 249:
					// line 994 "DefaultRubyParser.y"
  {
		      /* This may be a fixme (see tStar in last production)*/
		      yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
		  }
  break;
case 250:
					// line 998 "DefaultRubyParser.y"
  {}
  break;
case 251:
					// line 1000 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 252:
					// line 1002 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 254:
					// line 1008 "DefaultRubyParser.y"
  {                    
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 255:
					// line 1010 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "don't put space before argument parentheses");
		    yyVal = null;
		  }
  break;
case 256:
					// line 1014 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		  }
  break;
case 257:
					// line 1016 "DefaultRubyParser.y"
  {
		    errorHandler.handleError(IErrors.WARN, null, "don't put space before argument parentheses");
		    yyVal = ((INode)yyVals[-2+yyTop]);
		  }
  break;
case 258:
					// line 1021 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 259:
					// line 1026 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 261:
					// line 1031 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 262:
					// line 1034 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 263:
					// line 1042 "DefaultRubyParser.y"
  {
		    yyVal = ((IListNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 264:
					// line 1045 "DefaultRubyParser.y"
  {
	            /* Append?*/
		      yyVal = ((IListNode)yyVals[-3+yyTop]).add(((INode)yyVals[0+yyTop]));
		}
  break;
case 265:
					// line 1049 "DefaultRubyParser.y"
  {  
                    yyVal = new ExpandArrayNode(((INode)yyVals[0+yyTop]));
		}
  break;
case 274:
					// line 1061 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(getPosition(), ((String)yyVals[0+yyTop]));
		}
  break;
case 275:
					// line 1065 "DefaultRubyParser.y"
  {
                    yyVal = new BeginNode(getPosition(), ((INode)yyVals[-1+yyTop]));
		}
  break;
case 276:
					// line 1068 "DefaultRubyParser.y"
  {
		    lexer.setState(LexState.EXPR_ENDARG);
		    errorHandler.handleError(IErrors.WARN, null, "(...) interpreted as grouped expression");
                    yyVal = ((INode)yyVals[-2+yyTop]);
		}
  break;
case 277:
					// line 1073 "DefaultRubyParser.y"
  {
	            yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 278:
					// line 1076 "DefaultRubyParser.y"
  {
                    yyVal = new Colon2Node(getPosition(), ((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 279:
					// line 1079 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 280:
					// line 1082 "DefaultRubyParser.y"
  {
                    yyVal = new CallNode(getPosition(), ((INode)yyVals[-3+yyTop]), "[]", ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 281:
					// line 1085 "DefaultRubyParser.y"
  {
                    if (((IListNode)yyVals[-1+yyTop]) == null) {
                        yyVal = new ArrayNode(getPosition()); /* zero length array*/
                    } else {
                        yyVal = ((IListNode)yyVals[-1+yyTop]);
                    }
                }
  break;
case 282:
					// line 1092 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 283:
					// line 1095 "DefaultRubyParser.y"
  {
		    yyVal = new ReturnNode(getPosition(), null);
                }
  break;
case 284:
					// line 1098 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 285:
					// line 1101 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null);
                }
  break;
case 286:
					// line 1104 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null);
                }
  break;
case 287:
					// line 1107 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 288:
					// line 1109 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 289:
					// line 1113 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(), ((String)yyVals[-1+yyTop]), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 291:
					// line 1118 "DefaultRubyParser.y"
  {
		    if (((INode)yyVals[-1+yyTop]) != null && ((INode)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
		    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 292:
					// line 1125 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 293:
					// line 1134 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[-2+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
		            NODE *tmp = $$->nd_body;
		            $$->nd_body = $$->nd_else;
		            $$->nd_else = tmp;
			    } */
                }
  break;
case 294:
					// line 1143 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 295:
					// line 1145 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 296:
					// line 1147 "DefaultRubyParser.y"
  {
                    yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_UNTIL);
			    } */
                }
  break;
case 297:
					// line 1154 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 298:
					// line 1156 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 299:
					// line 1158 "DefaultRubyParser.y"
  {
                    yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]));
		    /* missing from ruby
			if (cond_negative(&$$->nd_cond)) {
			    nd_set_type($$, NODE_WHILE);
			    } */
                }
  break;
case 300:
					// line 1167 "DefaultRubyParser.y"
  {
		    yyVal = new CaseNode(getPosition(), ((INode)yyVals[-3+yyTop]), ((INode)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 301:
					// line 1170 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(), null, ((INode)yyVals[-1+yyTop]));
                }
  break;
case 302:
					// line 1173 "DefaultRubyParser.y"
  {
		    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 303:
					// line 1176 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 304:
					// line 1178 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 305:
					// line 1181 "DefaultRubyParser.y"
  {
                    yyVal = new ForNode(getPosition(), ((INode)yyVals[-7+yyTop]), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[-4+yyTop]));
                }
  break;
case 306:
					// line 1184 "DefaultRubyParser.y"
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
					// line 1192 "DefaultRubyParser.y"
  {
  yyVal = new ClassNode(getPosition(), ((Colon2Node)yyVals[-4+yyTop]).getName(), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])), ((INode)yyVals[-3+yyTop]));
                    /* $<INode>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 308:
					// line 1198 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 309:
					// line 1201 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                }
  break;
case 310:
					// line 1207 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(getPosition(), ((INode)yyVals[-5+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 311:
					// line 1214 "DefaultRubyParser.y"
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
					// line 1222 "DefaultRubyParser.y"
  {
  yyVal = new ModuleNode(getPosition(), ((Colon2Node)yyVals[-3+yyTop]).getName(), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 313:
					// line 1228 "DefaultRubyParser.y"
  {
		      /* missing
			$<id>$ = cur_mid;
			cur_mid = $2; */
                    support.setInDef(true);
                    support.getLocalNames().push();
                }
  break;
case 314:
					// line 1236 "DefaultRubyParser.y"
  {
		      /* was in old jruby grammar support.getClassNest() !=0 || IdUtil.isAttrSet($2) ? Visibility.PUBLIC : Visibility.PRIVATE); */
                    /* NOEX_PRIVATE for toplevel */
                    yyVal = new DefnNode(getPosition(), ((String)yyVals[-4+yyTop]), ((INode)yyVals[-2+yyTop]),
		                      new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])), Visibility.PRIVATE);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
		    /* missing cur_mid = $<id>3; */
                }
  break;
case 315:
					// line 1246 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 316:
					// line 1248 "DefaultRubyParser.y"
  {
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 317:
					// line 1254 "DefaultRubyParser.y"
  {
                    yyVal = new DefsNode(getPosition(), ((INode)yyVals[-7+yyTop]), ((String)yyVals[-4+yyTop]), ((INode)yyVals[-2+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    /* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 318:
					// line 1260 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition());
                }
  break;
case 319:
					// line 1263 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition());
                }
  break;
case 320:
					// line 1266 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition());
                }
  break;
case 321:
					// line 1269 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition());
                }
  break;
case 322:
					// line 1273 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
		    yyVal = ((INode)yyVals[0+yyTop]);
		}
  break;
case 331:
					// line 1290 "DefaultRubyParser.y"
  {
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-3+yyTop])), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 333:
					// line 1295 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 337:
					// line 1303 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
                }
  break;
case 338:
					// line 1306 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
		}
  break;
case 339:
					// line 1309 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 340:
					// line 1313 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
		}
  break;
case 341:
					// line 1316 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 342:
					// line 1321 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-1+yyTop]) instanceof BlockPassNode) {
		        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 343:
					// line 1328 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 344:
					// line 1331 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1335 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 346:
					// line 1338 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 347:
					// line 1341 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 348:
					// line 1344 "DefaultRubyParser.y"
  {
                    yyVal = support.new_call(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), null);
                }
  break;
case 349:
					// line 1347 "DefaultRubyParser.y"
  {
                    yyVal = support.new_super(((INode)yyVals[0+yyTop]), getPosition());
                }
  break;
case 350:
					// line 1350 "DefaultRubyParser.y"
  {
                    yyVal = new ZSuperNode(getPosition());
                }
  break;
case 351:
					// line 1354 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
		}
  break;
case 352:
					// line 1356 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 353:
					// line 1360 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
		}
  break;
case 354:
					// line 1362 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 355:
					// line 1369 "DefaultRubyParser.y"
  {
		    yyVal = new WhenNode(getPosition(), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 357:
					// line 1374 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-3+yyTop]).add(new WhenNode(getPosition(), new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop])), null, null));
                }
  break;
case 358:
					// line 1377 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new WhenNode(getPosition(), new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop])), null, null));
                }
  break;
case 361:
					// line 1387 "DefaultRubyParser.y"
  {
                    INode node;
		    if (((INode)yyVals[-3+yyTop]) != null) {
                       node = support.appendToBlock(support.node_assign(((INode)yyVals[-3+yyTop]), new GlobalVarNode(getPosition(), "$!")), ((INode)yyVals[-1+yyTop]));
		    } else {
		       node = ((INode)yyVals[-1+yyTop]);
                    }
                    yyVal = new RescueBodyNode(getPosition(), ((INode)yyVals[-4+yyTop]), node, ((RescueBodyNode)yyVals[0+yyTop]));
		}
  break;
case 362:
					// line 1396 "DefaultRubyParser.y"
  {yyVal = null;}
  break;
case 363:
					// line 1398 "DefaultRubyParser.y"
  {
	            yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
		}
  break;
case 366:
					// line 1404 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 368:
					// line 1409 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[0+yyTop]) != null) {
                        yyVal = ((INode)yyVals[0+yyTop]);
                    } else {
                        yyVal = new NilNode(null);
                    }
                }
  break;
case 371:
					// line 1419 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 373:
					// line 1424 "DefaultRubyParser.y"
  {
		    if (((INode)yyVals[0+yyTop]) == null) {
		        yyVal = new StrNode(getPosition(), "");
		    } else {
		        if (((INode)yyVals[0+yyTop]) instanceof EvStrNode) {
			    yyVal = new DStrNode(getPosition()).add(((INode)yyVals[0+yyTop]));
			} else {
		            yyVal = ((INode)yyVals[0+yyTop]);
			}
		    }
		}
  break;
case 375:
					// line 1437 "DefaultRubyParser.y"
  {
                    yyVal = support.literal_concat(getPosition(), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
		}
  break;
case 376:
					// line 1441 "DefaultRubyParser.y"
  {
		     yyVal = ((INode)yyVals[-1+yyTop]);
		}
  break;
case 377:
					// line 1445 "DefaultRubyParser.y"
  {
		    if (((INode)yyVals[-1+yyTop]) == null) {
			  yyVal = new XStrNode(getPosition(), null);
		    } else {
		      if (((INode)yyVals[-1+yyTop]) instanceof StrNode) {
			  yyVal = new XStrNode(getPosition(), ((StrNode)yyVals[-1+yyTop]).getValue());
		      } else if (((INode)yyVals[-1+yyTop]) instanceof DStrNode) {
			  yyVal = new DXStrNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
		      } else {
			yyVal = new DXStrNode(getPosition()).add(new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop])));
		      }
		    }
                }
  break;
case 378:
					// line 1459 "DefaultRubyParser.y"
  {
		    int options = ((RegexpNode)yyVals[0+yyTop]).getOptions();
		    INode node = ((INode)yyVals[-1+yyTop]);

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
					// line 1476 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition());
		 }
  break;
case 380:
					// line 1479 "DefaultRubyParser.y"
  {
		     yyVal = ((IListNode)yyVals[-1+yyTop]);
		 }
  break;
case 381:
					// line 1483 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 382:
					// line 1486 "DefaultRubyParser.y"
  {
                     INode node = ((INode)yyVals[-1+yyTop]);

                     if (node instanceof EvStrNode) {
		       node = new DStrNode(getPosition()).add(node);
		     }

		     yyVal = ((IListNode)yyVals[-2+yyTop]).add(node);
		 }
  break;
case 384:
					// line 1497 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
	         }
  break;
case 385:
					// line 1501 "DefaultRubyParser.y"
  {
		     yyVal = new ZArrayNode(getPosition());
		 }
  break;
case 386:
					// line 1504 "DefaultRubyParser.y"
  {
		     yyVal = ((IListNode)yyVals[-1+yyTop]);
		 }
  break;
case 387:
					// line 1508 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 388:
					// line 1511 "DefaultRubyParser.y"
  {
                     if (((IListNode)yyVals[-2+yyTop]) == null) {
		         yyVal = new ArrayNode(getPosition()).add(new StrNode(getPosition(), ((String)yyVals[-1+yyTop])));
		     } else {
                         yyVal = ((IListNode)yyVals[-2+yyTop]).add(new StrNode(getPosition(), ((String)yyVals[-1+yyTop])));
		     }
		 }
  break;
case 389:
					// line 1519 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 390:
					// line 1522 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
		 }
  break;
case 391:
					// line 1526 "DefaultRubyParser.y"
  {
		     yyVal = null;
		 }
  break;
case 392:
					// line 1529 "DefaultRubyParser.y"
  {
                     yyVal = support.literal_concat(getPosition(), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
		 }
  break;
case 393:
					// line 1534 "DefaultRubyParser.y"
  {
                     yyVal = new StrNode(getPosition(), ((String)yyVal));
                  }
  break;
case 394:
					// line 1537 "DefaultRubyParser.y"
  {
                      yyVal = lexer.strTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 395:
					// line 1541 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((INode)yyVals[-1+yyTop]));
		      yyVal = new EvStrNode(getPosition(), ((INode)yyVals[0+yyTop]));
		  }
  break;
case 396:
					// line 1545 "DefaultRubyParser.y"
  {
		      yyVal = lexer.strTerm();
		      lexer.setStrTerm(null);
		      lexer.setState(LexState.EXPR_BEG);
		  }
  break;
case 397:
					// line 1549 "DefaultRubyParser.y"
  {
		      lexer.setStrTerm(((INode)yyVals[-2+yyTop]));
		      INode node = ((INode)yyVals[-1+yyTop]);

		      if (node instanceof NewlineNode) {
		        node = ((NewlineNode)node).getNextNode();
		      }

		      yyVal = support.newEvStrNode(getPosition(), node);
		  }
  break;
case 398:
					// line 1560 "DefaultRubyParser.y"
  {
		      yyVal = new GlobalVarNode(getPosition(), ((String)yyVals[0+yyTop]));
                 }
  break;
case 399:
					// line 1563 "DefaultRubyParser.y"
  {
		      yyVal = new InstVarNode(getPosition(), ((String)yyVals[0+yyTop]));
                 }
  break;
case 400:
					// line 1566 "DefaultRubyParser.y"
  {
		      yyVal = new ClassVarNode(getPosition(), ((String)yyVals[0+yyTop]));
                 }
  break;
case 402:
					// line 1572 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 407:
					// line 1582 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
		    INode node = ((INode)yyVals[-1+yyTop]);
		    /*
			if (!($$ = $2)) {
			    yyerror("empty symbol literal");
			}
		    */

		    /* In ruby the only place DSYM is used is same place*/
		    /* as DSTR itself, therefore I will a DSTR*/
		    if (node instanceof DStrNode == false) {
		      /* in ruby
			      case NODE_STR:
				if (strlen(RSTRING($$->nd_lit)->ptr) == RSTRING($$->nd_lit)->len) {
				    $$->nd_lit = ID2SYM(rb_intern(RSTRING($$->nd_lit)->ptr));
				    nd_set_type($$, NODE_LIT);
				    break;
				}
		      */
		      yyVal = new DStrNode(getPosition()).add(node);

		    } else {
		      yyVal = node;
		    }
		}
  break;
case 408:
					// line 1609 "DefaultRubyParser.y"
  {
                    if (((Number)yyVals[0+yyTop]) instanceof Long) {
                        yyVal = new FixnumNode(getPosition(), ((Long)yyVals[0+yyTop]).longValue());
                    } else {
                        yyVal = new BignumNode(getPosition(), ((BigInteger)yyVals[0+yyTop]));
                    }
                }
  break;
case 409:
					// line 1616 "DefaultRubyParser.y"
  {
	            yyVal = new FloatNode(getPosition(), ((Double)yyVals[0+yyTop]).doubleValue());
	        }
  break;
case 410:
					// line 1619 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode((((Number)yyVals[0+yyTop]) instanceof Long ? (INode) new FixnumNode(getPosition(), ((Long)yyVals[0+yyTop]).longValue()) : (INode) new BignumNode(getPosition(), ((BigInteger)yyVals[0+yyTop]))), "-@");
		}
  break;
case 411:
					// line 1622 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(new FloatNode(getPosition(), ((Double)yyVals[0+yyTop]).doubleValue()), "-@");
		}
  break;
case 412:
					// line 1631 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 413:
					// line 1634 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 414:
					// line 1637 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 415:
					// line 1640 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 416:
					// line 1643 "DefaultRubyParser.y"
  {
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 417:
					// line 1646 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(getPosition());
                }
  break;
case 418:
					// line 1649 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(getPosition());
                }
  break;
case 419:
					// line 1652 "DefaultRubyParser.y"
  { 
                    yyVal = new TrueNode(getPosition());
                }
  break;
case 420:
					// line 1655 "DefaultRubyParser.y"
  {
                    yyVal = new FalseNode(getPosition());
                }
  break;
case 421:
					// line 1658 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(), getPosition().getFile());
                }
  break;
case 422:
					// line 1661 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(), getPosition().getLine());
                }
  break;
case 423:
					// line 1665 "DefaultRubyParser.y"
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
					// line 1678 "DefaultRubyParser.y"
  {
                    yyVal = support.assignable(getPosition(), yyVals[0+yyTop], null);
                }
  break;
case 427:
					// line 1685 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 428:
					// line 1688 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 429:
					// line 1690 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 430:
					// line 1693 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 431:
					// line 1698 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 432:
					// line 1702 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 433:
					// line 1706 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-5+yyTop]).intValue(), ((IListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 434:
					// line 1709 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), ((IListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 435:
					// line 1712 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 436:
					// line 1715 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-1+yyTop]).intValue(), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 437:
					// line 1718 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((IListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 438:
					// line 1721 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((IListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 439:
					// line 1724 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 440:
					// line 1727 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 441:
					// line 1730 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, null);
                }
  break;
case 442:
					// line 1734 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 443:
					// line 1737 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 444:
					// line 1740 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 445:
					// line 1743 "DefaultRubyParser.y"
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
					// line 1754 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(((Integer)yyVal).intValue() + 1);
                }
  break;
case 448:
					// line 1758 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[-2+yyTop]))) {
                        yyerror("duplicate optional argument name");
                    }
		    support.getLocalNames().getLocalIndex(((String)yyVals[-2+yyTop]));
                    yyVal = support.assignable(getPosition(), ((String)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 449:
					// line 1768 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 450:
					// line 1771 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 453:
					// line 1778 "DefaultRubyParser.y"
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
					// line 1786 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(-2);
                }
  break;
case 457:
					// line 1793 "DefaultRubyParser.y"
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
					// line 1802 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 459:
					// line 1805 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 460:
					// line 1809 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[0+yyTop]) instanceof SelfNode) {
                        yyVal = new SelfNode(null);
                    } else {
			support.checkExpression(((INode)yyVals[0+yyTop]));
			yyVal = ((INode)yyVals[0+yyTop]);
		    }
                }
  break;
case 461:
					// line 1817 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 462:
					// line 1819 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-2+yyTop]) instanceof ILiteralNode) {
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
		    support.checkExpression(((INode)yyVals[-2+yyTop]));
                    yyVal = ((INode)yyVals[-2+yyTop]);
                }
  break;
case 464:
					// line 1836 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-1+yyTop]);
                }
  break;
case 465:
					// line 1839 "DefaultRubyParser.y"
  {
                    if (ListNodeUtil.getLength(((IListNode)yyVals[-1+yyTop])) % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((IListNode)yyVals[-1+yyTop]);
                }
  break;
case 467:
					// line 1847 "DefaultRubyParser.y"
  {
                    yyVal = ListNodeUtil.addAll(((IListNode)yyVals[-2+yyTop]), ((IListNode)yyVals[0+yyTop]));
                }
  break;
case 468:
					// line 1851 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop])).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 488:
					// line 1881 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 491:
					// line 1887 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 492:
					// line 1891 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 493:
					// line 1895 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
case 494:
					// line 1898 "DefaultRubyParser.y"
  {  yyVal = null;
		  }
  break;
					// line 2939 "-"
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
         63,   24,   24,   24,   24,   24,   24,   24,   24,   67,
         67,   69,   69,   68,   68,   68,   68,   68,   68,   64,
         64,   74,   74,   65,   65,   65,   65,   65,   65,   65,
         65,   59,   59,   59,   59,   59,   59,   59,   59,   91,
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
         22,   22,  106,   22,   22,   22,   70,   82,   82,   82,
         82,   82,   82,   39,   39,   39,   39,   40,   40,   36,
         36,   36,   36,   36,   36,   36,   36,   36,   37,   37,
         37,   37,   37,   37,   37,   37,   37,   37,   37,   37,
        108,   41,   38,  109,   38,  110,   38,   44,   43,   43,
         72,   72,   66,   66,   66,   23,   23,   23,   23,   23,
         23,   23,   23,   23,   23,   23,   23,   23,   23,   23,
         23,   23,   23,   23,   23,   23,  111,   23,   23,   23,
         23,   23,   23,  113,  115,   23,  116,  117,   23,   23,
         23,   23,  118,  119,   23,  120,   23,  122,  123,   23,
        124,   23,  125,   23,  127,  128,   23,   23,   23,   23,
         23,   30,  112,  112,  112,  112,  114,  114,  114,   33,
         33,   31,   31,   57,   57,   58,   58,   58,   58,  129,
         62,   47,   47,   47,   26,   26,   26,   26,   26,   26,
        130,   61,  131,   61,   71,   73,   73,   73,   32,   32,
         78,   78,   77,   77,   77,   34,   34,   35,   35,   13,
         13,   13,    2,    3,    3,    4,    5,    6,   10,   10,
         28,   28,   12,   12,   11,   11,   27,   27,    7,    7,
          8,    8,    9,  132,    9,  133,    9,   56,   56,   56,
         56,   87,   86,   86,   86,   86,   15,   14,   14,   14,
         14,   79,   79,   79,   79,   79,   79,   79,   79,   79,
         79,   79,   42,   80,   55,   55,   46,  134,   46,   46,
         51,   51,   52,   52,   52,   52,   52,   52,   52,   52,
         52,   94,   94,   94,   94,   95,   95,   53,   84,   84,
        135,  135,   96,   96,  136,  136,   50,   49,   49,    1,
        137,    1,   83,   83,   83,   75,   75,   76,   88,   88,
         88,   89,   89,   89,   89,   90,   90,   90,  126,  126,
         98,   98,  105,  105,  107,  107,  107,  121,  121,   99,
         99,   60,   81,   45,
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
          0,    0,    0,    0,    0,    0,    0,    0,    0,  218,
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
        256,  250,    0,    0,    0,    0,    0,    0,    0,  224,
          0,   10,    0,    0,    0,   22,    0,    0,  276,  223,
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
        710,  840,  711,  703,  707,  236,  621,  416,  669,  670,
        245,   80,  408,  612,  409,  365,   81,   82,  694,  781,
        565,  566,  567,  201,  211,  751,  227,  658,  212,   85,
        355,  336,  541,   86,   87,  529,   88,   89,  264,  238,
        395,  268,  595,   90,  269,  241,  578,  378,  213,  214,
        270,  271,  275,  568,  202,  285,   93,  113,  548,  512,
        114,  204,  519,  569,  570,  571,    2,  219,  220,  425,
        255,  681,  191,  574,  254,  427,  441,  246,  625,  731,
        633,  383,  222,  599,  722,  223,  723,  607,  844,  545,
        384,  542,  772,  370,  375,  374,  554,  776,  505,  507,
        506,  649,  648,  544,  572,  573,  371,
    };
  } /* End of class YyDgotoClass */

  protected static final class YySindexClass {

    public static final short yySindex [] = {            0,
          0,14342,14646,18756,14124,17722,17424,14342,15995,15995,
       6993,    0,    0,18852,14843,14843,    0,    0,14843,  110,
        127,    0,    0,    0,    0,15995,17334,  160,    0,   65,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,16859,16859, -190,14443,15995,16091,16859,19044,
      17800,    0,    0,    0,   95,  188,  278,16955,16859,    0,
       -150,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        106,  756,  308,10617,    0,  -70,    0,  -27,   23,    0,
        111,    0, -112,  179,    0,  206,    0,  196,    0,18948,
          0,  -77,    0,   66,  756,    0,    0,    0,  110,  127,
        160,    0,    0,15995,  -21,14342,  321,  591,    0,   37,
          0,    0,   66,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,  117,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,17800,
        221,    0,    0,    0,   12,   21,   19,  308,  425,   49,
          5,  364,    0,  144,  425,    0,    0,  106,  -36,  418,
          0,15995,15995,  181,   55,    0,  234,    0,    0,    0,
      16859,16859,16859,10617,    0,    0,    0,  178,  488,  492,
          0,    0,    0,14747,    0,14939,14843,    0,    0,    0,
        211,    0,    0,  512,  433,14342,    0,   87,  242,  228,
      14443,  534,    0,  549,   70,16859,  160,  120,  125,    0,
        484,  183,  125,    0,  468,  311,  108,    0,    0,    0,
          0,    0,    0,  218,    0,    0,  606,  660,  746,  252,
        663,  269, -263,  307,  310,    0,    0,    0,    0,    0,
      14545,15995,15995,15995,15995,14646,15995,15995,16859,16859,
      16859,16859,16859,16859,16859,16859,16859,16859,16859,16859,
      16859,16859,16859,16859,16859,16859,16859,16859,16859,16859,
      16859,16859,16859,16859,    0,    0,    0,    0,    0, 9157,
      16091,11177,17845,17845,16955,16187,    0,16187,14443,19044,
        590,16955,    0,  299,    0,  512,  308,    0,    0,    0,
          0,    0,  106,    0,    0,    0,18143,16091,17845,14342,
      15995,    0,    0,    0,  240,    0,16283,  386,    0,  228,
          0,    0,14342,  390,18174,16091,18211,16859,16859,16859,
      14342,  397,14342,16379,  406,    0,  180,  180,    0,18509,
      16091,18540,    0,  627,    0,16859,15035,    0,    0,15131,
          0,    0,  637, 5835,    0,    0,  -70,  160,   48,  639,
        641,    0,    0,    0,17424,    0,16859,14342,  556,18174,
      18211,16859,  653,    0,    0,  654, 4788,    0,16475,    0,
          0,16859,    0,    0,16859,    0,    0,    0,    0,18577,
      16091,18608,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,   16,    0,  677,    0,16859,16859,
        756,    0,    0,    0,    0,    0,    0,    0,  242, 1793,
       1793, 1793, 1793,  838,  838,10755,13767, 1793, 1793,13263,
      13263,  140,  140, 5227,  838,  838,  575,  575,  224,  149,
        149,  242,  242,  242,  -98,  -98,  -98,  359,    0,  361,
        127,    0,    0,  620,  382,    0,  383,  127,    0,    0,
          0,  127,  127,10617,    0,16859, 5688,    0,    0,    0,
        679,    0,    0,    0,  684,    0,    0,10617,    0,    0,
          0,  106,    0,15995,14342,    0,    0,  127,    0,  635,
        127,  466,   70,18098,  670,    0,    0,    0,    0,    0,
          0,    0,  358,    0,14342,  106,    0,  692,    0,  693,
        704,  449,  455,17424,    0,    0,    0,  441,14342,  517,
          0,  340,    0,  453,  361,  698,  457,  459, 5688,  386,
        522,  523,16859,  754,  425,    0,    0,    0,    0,    0,
          0,    0,    0,  709,    0,    0,15995,  470,    0,    0,
      16859,    0,  178,  763,16859,  178,    0,    0,16859,10617,
          0,    0,   17,  773,  780,  782,17845,17845,  784,    0,
      15227,    0,15995,10617,  708,    0,10617,    0,    0,    0,
      16859,    0,    0,    0,  742,    0,    0,14342,  728,    0,
          0,    0,  242,  242,16859,    0, 6254,14342,    0,14342,
      14342,16955,16859,    0,  299,  502,16955,16955,    0,    0,
        299,    0,    0,    0,    0,    0,    0,16859,16571,    0,
        -98,    0,  106,  565,    0,    0,  799,    0,16859,  160,
        577,    0,   61,    0,  358,    0,  -25,    0,    0,    0,
          0,19140,  425,    0,    0,14342,    0,    0,15995,    0,
        580,16859,  513,16859,16859,  592,    0,    0,    0,16667,
      14342,14342,14342,    0,  180,  627,15323,    0,  627,  627,
        816,15419,15515,    0,    0,    0,  127,  127,    0,  -70,
         48,  138,    0, 4788,    0,  733,    0,    0,    0,    0,
          0,10617,    0,  745,  596,  614,  757,10617,    0,10617,
          0,16955,10617,    0,10617,    0,    0,10617,16859,    0,
      14342,14342,    0,    0,    0,  240,    0,  829,    0,  670,
          0,    0,  704,  840,    0,  704,  578,  112,    0,    0,
          0,14342,    0,  425,    0,16859,    0,16859,  226,  628,
        636,    0,    0,16859,    0,    0,    0,16859,    0,  851,
        858,16859,  864,    0,    0,    0,    0,    0,    0,    0,
      10617,    0,    0,  781,  646,14342,    0,    0,   61,    0,
          0,    0,18645,16091,18676,   12,14342,    0,    0,    0,
          0,    0,    0,14342, 9561,  627,15611,    0,15707,    0,
        627,    0,    0,    0,  651,  704,    0,    0,  823,    0,
          0,    0,  340,  659,    0,    0,16859,  873,16859,    0,
          0,    0,    0,    0,    0,  627,15803,    0,  627,    0,
      16859,    0,  627,    0,
    };
  } /* End of class YySindexClass */

  protected static final class YyRindexClass {

    public static final short yyRindex [] = {            0,
          0,  151,    0,    0,    0,    0,    0,  276,    0,    0,
        262,    0,    0,    0,13339,13429,    0,    0,13728, 4300,
       3756,    0,    0,    0,    0,    0,    0,16763,    0,    0,
          0,    0, 1955, 2956,    0,    0, 2060,    0,    0,    0,
          0,    0,    0,    0,    0,  133,    0,  835,  808,   84,
        666,    0,    0,    0,  694, -235,    0,    0,    0,    0,
       8279,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        826, 1262, 1657,13906, 8364,14064,    0, 8460,    0,    0,
      17289,    0,13889,    0,    0,    0,    0,    0,    0,  473,
      13813,    0,    0,15899, 1766,    0,    0,    0, 8763, 7313,
        897, 5631, 5943,    0,    0,  133,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  736, 1309,
       1357, 1448,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0, 1780, 1836, 1865,    0, 2156,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       6155,    0,    0,    0,  298,    0,    0, 7122,    0,    0,
       7795,    0, 7402,    0,    0,    0,    0,  667,    0,  282,
          0,    0,    0,    0,    0,  233,    0,    0,    0,  450,
          0,    0,    0,12338,    0,    0,    0,13074, 8094, 8094,
          0,    0,    0,    0,    0,    0,  899,    0,    0,    0,
          0,    0,    0,17051,    0,   82,    0,    0, 9247,14188,
        133,    0,   88,    0,  903,    0,  852,  854,  854,    0,
          0,  825,  825,    0,    0,    0,    0,  731,    0,  930,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0, 8848, 8944,    0,    0,    0,    0,    0,
       1202,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        835,    0,    0,    0,    0,    0,    0,    0,  133,  608,
        777,    0,    0, 6802,    0,    0,  114,    0, 6457,    0,
          0,    0,    0,    0,    0,    0,    0,  835,    0,  276,
          0,    0,    0,    0,  185,    0,   80,  374,    0, 7880,
          0,    0,  595, 6564,    0,  835,    0,    0,    0,    0,
        625,    0,   72,    0,    0,    0,    0,    0,  664,    0,
        835,    0,    0, 8094,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  907,    0,    0,   63,  903,  903,    0,
         69,    0,    0,    0,    0,    0,    0,   82,    0,    0,
          0,    0,    0,    0,  220,    0,  852,    0,  859,    0,
          0,  -28,    0,    0,  836,    0,    0,    0, 1285,    0,
        835,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       1804,    0,    0,    0,    0,    0,    0,    0, 9320, 8183,
       8667,11596,11775,11137,11352,11861, 9924,11938,11989,12040,
      12078,10564,10637,    0,11429,11502,10987,11060,10714,10189,
      10266, 9409, 9651, 9740, 6691, 6691, 6884, 4652, 3308, 5187,
      15899,    0, 3404,    0, 4748,    0, 5091, 3852,    0,    0,
          0, 5530, 5530,12376,    0,    0,13418,    0,    0,    0,
          0,    0,    0, 7613,    0,    0,    0,12415,    0,    0,
          0,    0,    0,    0,  276, 6044, 6356,    0,    0,    0,
       7492,    0,  903,    0,  103,    0,    0,    0,    0,    0,
          0,    0,  235,    0,  276,    0,    0,  462,    0,  462,
        462,  631,    0,    0,    0,    0,   78,  434,  725,  690,
          0,  690,    0, 2412, 4204,    0, 2508, 2860,13234,  690,
          0,    0,    0,  544,    0,    0,    0,    0,    0,    0,
          0,  633,  909,    0,  647,  833,    0,    0,    0,    0,
          0,    0,13270, 8094,    0,    0,    0,    0,    0,   76,
          0,    0,    0,  925,    0,    0,    0,    0,    0,    0,
          0,    0,    0,12454,    0,    0,12492,  444,    0,    0,
          0,    0,  940, 1106,    0, 1352, 1462,   82,    0,    0,
          0,    0, 9813,10116,    0,    0,    0,   72,    0,   72,
         82,    0,    0,    0, 8578,14262,    0,    0,    0,    0,
      13974,    0,    0,    0,    0,    0,    0,    0,    0,    0,
       6884,    0,    0,    0,    0,    0,    0,    0,    0,  903,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,   72,    0,    0,    0,    0,
          0,    0, 7976,    0,    0,    0,    0,    0,    0,    0,
        209,   72,   72,  933,    0, 8094,    0,    0, 8094,  925,
          0,    0,    0,    0,    0,    0,   43,   43,    0,    0,
        903,    0,    0,  852, 1377,    0,    0,    0,    0,    0,
          0,12531,    0,    0,    0,    0,    0,12796,    0,12832,
          0,    0,12883,    0,12919,    0,    0,12956,    0,11899,
         82,  276,    0,    0,    0,  185,    0,    0,    0,    0,
          0,    0,  462,  462,    0,  462,    0,    0,  122,    0,
        156,  276,    0,    0,    0,    0,    0,    0,  690,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  925,
        925,    0,    0,    0,    0,    0,    0,    0,    0,    0,
      12992,    0,    0,    0,    0,  276,    0,    0,    0,    0,
          0,  184,    0,  835,    0,  298,  595,    0,    0,    0,
          0,    0,    0,   72, 8094,  925,    0,    0,    0,    0,
        925,    0,    0,    0,    0,  462,  165,  113,    0,  533,
        536,    0,  690,    0,    0,    0,    0,  925,    0,    0,
          0,    0,  391,    0,    0,  925,    0,    0,  925,    0,
          0,    0,  925,    0,
    };
  } /* End of class YyRindexClass */

  protected static final class YyGindexClass {

    public static final short yyGindex [] = {            0,
          0,    0,    0,  908,    0,    0,    0,  607, -131,    0,
          0,    0,    0,    0,    0,  968,   28, -339,    0,  109,
       1237,  -15,   58,    2,  -23,    0,    0,    0,   36,  460,
       -368,    0,  130,    0,    0,  695,    0,    0,  -13, -447,
          4,  977,  119,   26,    0,    0, -233,    0,  -42, -363,
        214,  422, -608,    0,  917,    0,  339,  123, 1320,  830,
        919,    0, -205,  913,  -18,  -85,  -11,   32, -207, 1330,
       -373,    8,    0,    0,  -10, -340,    0,  172, 1024, 1057,
        960, -330,    0,  316,   -5,    0,   13,  888, -276,    0,
        -46,    1,    9,  327,    0, -588,    0,  -20,  965,    0,
          0,    0,    0,    0,   83,    0,  342,    0,    0,    0,
          0, -213,    0, -361,    0,    0,    0,    0,    0,    0,
         44,    0,    0,    0,    0,    0,    0,    0,    0,    0,
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


					// line 1902 "DefaultRubyParser.y"

    /** The parse method use an lexer stream and parse it to an AST node 
     * structure
     */
    public IParserResult parse(ILexerSource source) {
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

    public void init(IConfiguration configuration) {
        support.setConfiguration((IRubyParserConfiguration)configuration);
    }

    // +++
    // Helper Methods
    
    void yyerrok() {}

    private ISourcePosition getPosition() {
        return lexer.getPosition();
    }
}
					// line 7891 "-"
