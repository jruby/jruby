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
					// line 75 "-"
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
  public static final int tSTRING = 312;
  public static final int tXSTRING = 313;
  public static final int tREGEXP = 314;
  public static final int tDXSTRING = 315;
  public static final int tDREGEXP = 316;
  public static final int tBACK_REF = 317;
  public static final int tNTH_REF = 318;
  public static final int tDSTRING = 319;
  public static final int tARRAY = 320;
  public static final int tUPLUS = 321;
  public static final int tUMINUS = 322;
  public static final int tPOW = 323;
  public static final int tCMP = 324;
  public static final int tEQ = 325;
  public static final int tEQQ = 326;
  public static final int tNEQ = 327;
  public static final int tGEQ = 328;
  public static final int tLEQ = 329;
  public static final int tANDOP = 330;
  public static final int tOROP = 331;
  public static final int tMATCH = 332;
  public static final int tNMATCH = 333;
  public static final int tDOT2 = 334;
  public static final int tDOT3 = 335;
  public static final int tAREF = 336;
  public static final int tASET = 337;
  public static final int tLSHFT = 338;
  public static final int tRSHFT = 339;
  public static final int tCOLON2 = 340;
  public static final int tCOLON3 = 341;
  public static final int tOP_ASGN = 342;
  public static final int tASSOC = 343;
  public static final int tLPAREN = 344;
  public static final int tLBRACK = 345;
  public static final int tLBRACE = 346;
  public static final int tSTAR = 347;
  public static final int tAMPER = 348;
  public static final int tSYMBEG = 349;
  public static final int LAST_TOKEN = 350;
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
					// line 203 "DefaultRubyParser.y"
  {
                  /* $<Object>$ = ruby.getDynamicVars();*/
                  lexer.setState(LexState.EXPR_BEG);
                  support.initTopLocalVariables();
		  /* FIXME move to ruby runtime*/
                  /*if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {*/
                  /*    support.setClassNest(0);*/
                  /*} else {*/
                  /*    support.setClassNest(1);*/
                  /*}*/
              }
  break;
case 2:
					// line 213 "DefaultRubyParser.y"
  {
                  if (((INode)yyVals[0+yyTop]) != null && !support.isCompileForEval()) {
                      /* last expression should not be void */
                      if (((INode)yyVals[0+yyTop]) instanceof BlockNode) {
                          support.checkUselessStatement(ListNodeUtil.getLast(((IListNode)yyVals[0+yyTop])));
                      } else {
                          support.checkUselessStatement(((INode)yyVals[0+yyTop]));
                      }
                  }
                  support.getResult().setAST(support.appendToBlock(support.getResult().getAST(), ((INode)yyVals[0+yyTop])));
                  support.updateTopLocalVariables();
                  support.setClassNest(0);
                  /* ruby.setDynamicVars($<RubyVarmap>1);*/
              }
  break;
case 3:
					// line 228 "DefaultRubyParser.y"
  {
                  if (((INode)yyVals[-1+yyTop]) instanceof BlockNode) {
                     support.checkUselessStatements(((BlockNode)yyVals[-1+yyTop]));
		      }
                  yyVal = ((INode)yyVals[-1+yyTop]);
              }
  break;
case 5:
					// line 236 "DefaultRubyParser.y"
  {
                    yyVal = support.newline_node(((INode)yyVals[0+yyTop]), getPosition());
                }
  break;
case 6:
					// line 239 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((INode)yyVals[-2+yyTop]), support.newline_node(((INode)yyVals[0+yyTop]), getPosition()));
                }
  break;
case 7:
					// line 242 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 8:
					// line 246 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 9:
					// line 248 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("alias within method");
                    }
                    yyVal = new AliasNode(getPosition(), ((String)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 10:
					// line 254 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("alias within method");
                    }
                    yyVal = new VAliasNode(getPosition(), ((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 11:
					// line 260 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("alias within method");
                    }
                    yyVal = new VAliasNode(getPosition(), ((String)yyVals[-1+yyTop]), "$" + ((BackRefNode)yyVals[0+yyTop]).getType()); /* XXX*/
                }
  break;
case 12:
					// line 266 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 13:
					// line 270 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("undef within method");
                    }
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 14:
					// line 276 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((INode)yyVals[-2+yyTop]), null);
                }
  break;
case 15:
					// line 280 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), null, ((INode)yyVals[-2+yyTop]));
                }
  break;
case 16:
					// line 284 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    if (((INode)yyVals[-2+yyTop]) != null && ((INode)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode(), false);
                    } else {
                        yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((INode)yyVals[-2+yyTop]), false);
                    }
                }
  break;
case 17:
					// line 292 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    if (((INode)yyVals[-2+yyTop]) != null && ((INode)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((BeginNode)yyVals[-2+yyTop]).getBodyNode());
                    } else {
                        yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])), ((INode)yyVals[-2+yyTop]));
                    }
                }
  break;
case 18:
					// line 301 "DefaultRubyParser.y"
  {
                    yyVal = new RescueNode(getPosition(), ((INode)yyVals[-2+yyTop]), new ArrayNode(getPosition()).add(new RescueBodyNode(getPosition(), null,((INode)yyVals[0+yyTop]))), null);
                }
  break;
case 19:
					// line 305 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    support.getLocalNames().push();
                }
  break;
case 20:
					// line 310 "DefaultRubyParser.y"
  {
                    support.getResult().setBeginNodes(support.appendToBlock(support.getResult().getBeginNodes(), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop]))));
                    support.getLocalNames().pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 21:
					// line 315 "DefaultRubyParser.y"
  {
                    if (support.isCompileForEval() && (support.isInDef() 
                                              || support.isInSingle())) {
                        yyerror("END in method; use at_exit");
                    }
                    yyVal = new IterNode(getPosition(), null, new PostExeNode(getPosition()), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 22:
					// line 322 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 23:
					// line 326 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                }
  break;
case 24:
					// line 331 "DefaultRubyParser.y"
  {
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 26:
					// line 336 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    ((MultipleAsgnNode)yyVals[-2+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
                    yyVal = ((MultipleAsgnNode)yyVals[-2+yyTop]);
                }
  break;
case 27:
					// line 341 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = new ReturnNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 29:
					// line 349 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 30:
					// line 352 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 31:
					// line 355 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 359 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 36:
					// line 366 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    if (((INode)yyVals[0+yyTop]) instanceof ArrayNode && 
                        ListNodeUtil.getLength(((IListNode)yyVals[0+yyTop])) == 1) {
                        yyVal = new ReturnNode(getPosition(), 
                            ListNodeUtil.getLast(((IListNode)yyVals[0+yyTop])));
                    } else {
                        yyVal = new ReturnNode(getPosition(), ((INode)yyVals[0+yyTop]));
                    }
                }
  break;
case 37:
					// line 376 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    if (((INode)yyVals[0+yyTop]) instanceof ArrayNode && 
                        ListNodeUtil.getLength(((IListNode)yyVals[0+yyTop])) == 1) {
                        yyVal = new BreakNode(getPosition(), 
                            ListNodeUtil.getLast(((IListNode)yyVals[0+yyTop])));
                    } else {
                        yyVal = new BreakNode(getPosition(), ((INode)yyVals[0+yyTop]));
                    }
                }
  break;
case 38:
					// line 386 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    if (((INode)yyVals[0+yyTop]) instanceof ArrayNode && 
                        ListNodeUtil.getLength(((IListNode)yyVals[0+yyTop])) == 1) {
                        yyVal = new NextNode(getPosition(), 
                            ListNodeUtil.getLast(((IListNode)yyVals[0+yyTop])));
                    } else {
                        yyVal = new NextNode(getPosition(), ((INode)yyVals[0+yyTop]));
                    }
                }
  break;
case 40:
					// line 398 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 41:
					// line 402 "DefaultRubyParser.y"
  {
	            support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 42:
					// line 407 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 43:
					// line 410 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 44:
					// line 414 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 45:
					// line 418 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && support.isInDef() 
                                               && support.isInSingle()){
                        yyerror("super called outside of method");
                    }
		    yyVal = support.new_super(((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
		}
  break;
case 46:
					// line 425 "DefaultRubyParser.y"
  {
	            yyVal = new YieldNode(getPosition(), ((INode)yyVals[0+yyTop])); /* .setPosFrom($2);*/
		}
  break;
case 48:
					// line 430 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
		}
  break;
case 50:
					// line 435 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(), new ArrayNode(getPosition()).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 51:
					// line 439 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[0+yyTop]), null);
                }
  break;
case 52:
					// line 442 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[-1+yyTop]).add(((INode)yyVals[0+yyTop])), null);
                }
  break;
case 53:
					// line 445 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 54:
					// line 448 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[-1+yyTop]), new StarNode());
                }
  break;
case 55:
					// line 451 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, ((INode)yyVals[0+yyTop]));
                }
  break;
case 56:
					// line 454 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, new StarNode());
                }
  break;
case 58:
					// line 459 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
                }
  break;
case 59:
					// line 463 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 60:
					// line 466 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 61:
					// line 470 "DefaultRubyParser.y"
  {
                    yyVal = support.getAssignmentNode(((String)yyVals[0+yyTop]), null, getPosition());
                }
  break;
case 62:
					// line 473 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((INode)yyVals[-3+yyTop]), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 63:
					// line 476 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 64:
					// line 479 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 65:
					// line 482 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 66:
					// line 485 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((INode)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 67:
					// line 490 "DefaultRubyParser.y"
  {
                    yyVal = support.getAssignmentNode(((String)yyVals[0+yyTop]), null, getPosition());
                }
  break;
case 68:
					// line 493 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((INode)yyVals[-3+yyTop]), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 69:
					// line 496 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 70:
					// line 499 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 71:
					// line 502 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 72:
					// line 505 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 73:
					// line 510 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 78:
					// line 518 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 79:
					// line 522 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 82:
					// line 530 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 83:
					// line 533 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 84:
					// line 535 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((INode)yyVals[-3+yyTop]), new UndefNode(getPosition(), ((String)yyVals[0+yyTop])));
                }
  break;
case 85:
					// line 539 "DefaultRubyParser.y"
  { yyVal = "|"; }
  break;
case 86:
					// line 540 "DefaultRubyParser.y"
  { yyVal = "^"; }
  break;
case 87:
					// line 541 "DefaultRubyParser.y"
  { yyVal = "&"; }
  break;
case 88:
					// line 542 "DefaultRubyParser.y"
  { yyVal = "<=>"; }
  break;
case 89:
					// line 543 "DefaultRubyParser.y"
  { yyVal = "=="; }
  break;
case 90:
					// line 544 "DefaultRubyParser.y"
  { yyVal = "==="; }
  break;
case 91:
					// line 545 "DefaultRubyParser.y"
  { yyVal = "=~"; }
  break;
case 92:
					// line 546 "DefaultRubyParser.y"
  { yyVal = ">"; }
  break;
case 93:
					// line 547 "DefaultRubyParser.y"
  { yyVal = ">="; }
  break;
case 94:
					// line 548 "DefaultRubyParser.y"
  { yyVal = "<"; }
  break;
case 95:
					// line 549 "DefaultRubyParser.y"
  { yyVal = "<="; }
  break;
case 96:
					// line 550 "DefaultRubyParser.y"
  { yyVal = "<<"; }
  break;
case 97:
					// line 551 "DefaultRubyParser.y"
  { yyVal = ">>"; }
  break;
case 98:
					// line 552 "DefaultRubyParser.y"
  { yyVal = "+"; }
  break;
case 99:
					// line 553 "DefaultRubyParser.y"
  { yyVal = "-"; }
  break;
case 100:
					// line 554 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 101:
					// line 555 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 102:
					// line 556 "DefaultRubyParser.y"
  { yyVal = "/"; }
  break;
case 103:
					// line 557 "DefaultRubyParser.y"
  { yyVal = "%"; }
  break;
case 104:
					// line 558 "DefaultRubyParser.y"
  { yyVal = "**"; }
  break;
case 105:
					// line 559 "DefaultRubyParser.y"
  { yyVal = "~"; }
  break;
case 106:
					// line 560 "DefaultRubyParser.y"
  { yyVal = "+@"; }
  break;
case 107:
					// line 561 "DefaultRubyParser.y"
  { yyVal = "-@"; }
  break;
case 108:
					// line 562 "DefaultRubyParser.y"
  { yyVal = "[]"; }
  break;
case 109:
					// line 563 "DefaultRubyParser.y"
  { yyVal = "[]="; }
  break;
case 110:
					// line 564 "DefaultRubyParser.y"
  { yyVal = "`"; }
  break;
case 152:
					// line 608 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 153:
					// line 612 "DefaultRubyParser.y"
  {
                    yyVal = support.getAssignmentNode(((String)yyVals[-1+yyTop]), null, getPosition());
                }
  break;
case 154:
					// line 614 "DefaultRubyParser.y"
  {
                    if (((String)yyVals[-2+yyTop]).equals("||")) {
	                ((IAssignableNode)yyVals[-1+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
	                yyVal = new OpAsgnOrNode(getPosition(), support.getAccessNode(((String)yyVals[-3+yyTop]), getPosition()), ((INode)yyVals[-1+yyTop]));
                        /* FIXME*/
			/* if (IdUtil.isInstanceVariable($1)) {*/
                        /*    $<Node>$.setAId($1);*/
                        /* }*/
                    } else if (((String)yyVals[-2+yyTop]).equals("&&")) {
                        ((IAssignableNode)yyVals[-1+yyTop]).setValueNode(((INode)yyVals[0+yyTop]));
                        yyVal = new OpAsgnAndNode(getPosition(), support.getAccessNode(((String)yyVals[-3+yyTop]), getPosition()), ((INode)yyVals[-1+yyTop]));
                    } else {
                        yyVal = ((INode)yyVals[-1+yyTop]);
                        if (yyVal != null) {
                            ((IAssignableNode)yyVal).setValueNode(support.getOperatorCallNode(support.getAccessNode(((String)yyVals[-3+yyTop]), getPosition()), ((String)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop])));
                        }
                    }
                    /* $<Node>$.setPosFrom($4);*/
                }
  break;
case 155:
					// line 633 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(), ((INode)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 156:
					// line 636 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 157:
					// line 639 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 158:
					// line 642 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 159:
					// line 645 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 160:
					// line 649 "DefaultRubyParser.y"
  {
                    yyVal = new DotNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), false);
                }
  break;
case 161:
					// line 652 "DefaultRubyParser.y"
  {
                    yyVal = new DotNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), true);
                }
  break;
case 162:
					// line 655 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "+", ((INode)yyVals[0+yyTop]));
                }
  break;
case 163:
					// line 658 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "-", ((INode)yyVals[0+yyTop]));
                }
  break;
case 164:
					// line 661 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "*", ((INode)yyVals[0+yyTop]));
                }
  break;
case 165:
					// line 664 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "/", ((INode)yyVals[0+yyTop]));
                }
  break;
case 166:
					// line 667 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "%", ((INode)yyVals[0+yyTop]));
                }
  break;
case 167:
					// line 670 "DefaultRubyParser.y"
  {
                    /* Covert '- number ** number' to '- (number ** number)' */
                    boolean needNegate = false;
                    if ((((INode)yyVals[-2+yyTop]) instanceof FixnumNode && ((FixnumNode)yyVals[-2+yyTop]).getValue() < 0) ||
                        (((INode)yyVals[-2+yyTop]) instanceof BignumNode && ((BignumNode)yyVals[-2+yyTop]).getValue().compareTo(BigInteger.ZERO) < 0) ||
                        (((INode)yyVals[-2+yyTop]) instanceof FloatNode && ((FloatNode)yyVals[-2+yyTop]).getValue() < 0.0)) {

                        yyVals[-2+yyTop] = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "-@");
                        needNegate = true;
                    }

                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "**", ((INode)yyVals[0+yyTop]));

                    if (needNegate) {
                        yyVal = support.getOperatorCallNode(((INode)yyVal), "-@");
                    }
                }
  break;
case 168:
					// line 687 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "+@");
                }
  break;
case 169:
					// line 690 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "-@");
                }
  break;
case 170:
					// line 693 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "|", ((INode)yyVals[0+yyTop]));
                }
  break;
case 171:
					// line 696 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "^", ((INode)yyVals[0+yyTop]));
                }
  break;
case 172:
					// line 699 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "&", ((INode)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 702 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<=>", ((INode)yyVals[0+yyTop]));
                }
  break;
case 174:
					// line 705 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">", ((INode)yyVals[0+yyTop]));
                }
  break;
case 175:
					// line 708 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">=", ((INode)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 711 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<", ((INode)yyVals[0+yyTop]));
                }
  break;
case 177:
					// line 714 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<=", ((INode)yyVals[0+yyTop]));
                }
  break;
case 178:
					// line 717 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "==", ((INode)yyVals[0+yyTop]));
                }
  break;
case 179:
					// line 720 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "===", ((INode)yyVals[0+yyTop]));
                }
  break;
case 180:
					// line 723 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "==", ((INode)yyVals[0+yyTop])));
                }
  break;
case 181:
					// line 726 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 182:
					// line 729 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getMatchNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop])));
                }
  break;
case 183:
					// line 732 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 184:
					// line 736 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "~");
                }
  break;
case 185:
					// line 739 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<<", ((INode)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 742 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">>", ((INode)yyVals[0+yyTop]));
                }
  break;
case 187:
					// line 745 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 748 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 751 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 190:
					// line 753 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 191:
					// line 757 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 192:
					// line 762 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 193:
					// line 766 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 194:
					// line 769 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 195:
					// line 772 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-3+yyTop]).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 196:
					// line 775 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-1+yyTop]);
                }
  break;
case 197:
					// line 778 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop])));
                }
  break;
case 198:
					// line 782 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new HashNode(((ArrayNode)yyVals[-1+yyTop])));
                }
  break;
case 199:
					// line 785 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));

                    yyVal = new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop])));
                }
  break;
case 200:
					// line 791 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 201:
					// line 794 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-2+yyTop]);
                }
  break;
case 202:
					// line 797 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop]));
                }
  break;
case 203:
					// line 800 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-4+yyTop]).add(((INode)yyVals[-2+yyTop]));
                }
  break;
case 206:
					// line 807 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 207:
					// line 810 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 208:
					// line 813 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 209:
					// line 816 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 820 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new HashNode(((ArrayNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 823 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 827 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-3+yyTop]).add(new HashNode(((ArrayNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 830 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-6+yyTop]).add(new HashNode(((ArrayNode)yyVals[-4+yyTop]))).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 214:
					// line 834 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
		    /* FIXME*/
                    /* $$ = support.arg_blk_pass(new RestArgsNode(getPosition(), $2), $3);*/
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 215:
					// line 840 "DefaultRubyParser.y"
  {
	            yyVal = ((BlockPassNode)yyVals[0+yyTop]);
	        }
  break;
case 216:
					// line 844 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 217:
					// line 846 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 218:
					// line 851 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 219:
					// line 856 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 220:
					// line 859 "DefaultRubyParser.y"
  {
	            yyVal = null;
	      }
  break;
case 221:
					// line 863 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 222:
					// line 867 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 223:
					// line 872 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 225:
					// line 878 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 226:
					// line 882 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-3+yyTop]).add(new ExpandArrayNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 227:
					// line 886 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 228:
					// line 891 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                    if (((INode)yyVals[0+yyTop]) instanceof ArrayNode && ListNodeUtil.getLength(((IListNode)yyVals[0+yyTop])) == 1) {
                        yyVal = ListNodeUtil.getLast(((IListNode)yyVals[0+yyTop]));
                    } else if (((INode)yyVals[0+yyTop]) instanceof BlockPassNode) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Block argument should not be given.");
                    }
                }
  break;
case 231:
					// line 902 "DefaultRubyParser.y"
  {
	            yyVal = ((ArrayNode)yyVals[0+yyTop]);
	        }
  break;
case 232:
					// line 905 "DefaultRubyParser.y"
  {
                    yyVal = new XStrNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 909 "DefaultRubyParser.y"
  {
	            support.getLocalNames().getLocalIndex("~");
	            yyVal = ((INode)yyVals[0+yyTop]);
	        }
  break;
case 237:
					// line 915 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 918 "DefaultRubyParser.y"
  {
                    if (((IListNode)yyVals[-3+yyTop]) == null && ((INode)yyVals[-2+yyTop]) == null && ((INode)yyVals[-1+yyTop]) == null) {
                        yyVal = new BeginNode(getPosition(), ((INode)yyVals[-4+yyTop]));
                    } else {
                        if (((IListNode)yyVals[-3+yyTop]) != null) {
                            yyVals[-4+yyTop] = new RescueNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[-2+yyTop]));
                        } else if (((INode)yyVals[-2+yyTop]) != null) {
			    errorHandler.handleError(IErrors.WARN, null, "else without rescue is useless");
                            yyVals[-4+yyTop] = support.appendToBlock(((INode)yyVals[-4+yyTop]), ((INode)yyVals[-2+yyTop]));
                        }
                        if (((INode)yyVals[-1+yyTop]) != null) {
                            yyVals[-4+yyTop] = new EnsureNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[-1+yyTop]));
                        }
                        yyVal = ((INode)yyVals[-4+yyTop]);
                    }
                    /* $<Node>$.setPosFrom($2);*/
                }
  break;
case 239:
					// line 935 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 240:
					// line 938 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-2+yyTop]));
                    yyVal = new Colon2Node(getPosition(), ((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 241:
					// line 942 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 242:
					// line 945 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = new CallNode(getPosition(), ((INode)yyVals[-3+yyTop]), "[]", ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 243:
					// line 949 "DefaultRubyParser.y"
  {
                    if (((IListNode)yyVals[-1+yyTop]) == null) {
                        yyVal = new ArrayNode(getPosition()); /* zero length array*/
                    } else {
                        yyVal = ((IListNode)yyVals[-1+yyTop]);
                    }
                }
  break;
case 244:
					// line 956 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 245:
					// line 959 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef() 
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = new ReturnNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 246:
					// line 967 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = new ReturnNode(getPosition(), null);
                }
  break;
case 247:
					// line 974 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = new ReturnNode(getPosition(), null);
                }
  break;
case 248:
					// line 981 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = new YieldNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 249:
					// line 985 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null);
                }
  break;
case 250:
					// line 988 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null);
                }
  break;
case 251:
					// line 991 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 252:
					// line 993 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 253:
					// line 997 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(), ((String)yyVals[-1+yyTop]), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 255:
					// line 1002 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                       errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                    /* $<Node>$.setPosFrom($1);*/
                }
  break;
case 256:
					// line 1010 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 257:
					// line 1014 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[-2+yyTop]));
                }
  break;
case 258:
					// line 1018 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 259:
					// line 1020 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 260:
					// line 1022 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 261:
					// line 1026 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 262:
					// line 1028 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 263:
					// line 1030 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 264:
					// line 1034 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new CaseNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((IListNode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 265:
					// line 1038 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(), null, ((IListNode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 266:
					// line 1041 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 267:
					// line 1043 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 268:
					// line 1045 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new ForNode(getPosition(), ((INode)yyVals[-7+yyTop]), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[-4+yyTop]));
                }
  break;
case 269:
					// line 1049 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 270:
					// line 1056 "DefaultRubyParser.y"
  {
                    yyVal = new ClassNode(getPosition(), ((String)yyVals[-4+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])), ((INode)yyVals[-3+yyTop]));
                    /* $<INode>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 271:
					// line 1062 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 272:
					// line 1065 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                }
  break;
case 273:
					// line 1070 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(getPosition(), ((INode)yyVals[-5+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 274:
					// line 1077 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 275:
					// line 1084 "DefaultRubyParser.y"
  {
                    yyVal = new ModuleNode(getPosition(), ((String)yyVals[-3+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 276:
					// line 1090 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("nested method definition");
                    }
                    support.setInDef(true);
                    support.getLocalNames().push();
                }
  break;
case 277:
					// line 1096 "DefaultRubyParser.y"
  {
                    if (((IListNode)yyVals[-3+yyTop]) != null) {
                        yyVals[-4+yyTop] = new RescueNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[-2+yyTop]));
                    } else if (((INode)yyVals[-2+yyTop]) != null) {
		        errorHandler.handleError(IErrors.WARN, null, "Else without rescue is useless.");
                        yyVals[-4+yyTop] = support.appendToBlock(((INode)yyVals[-4+yyTop]), ((INode)yyVals[-2+yyTop]));
                    }
                    if (((INode)yyVals[-1+yyTop]) != null) {
                        yyVals[-4+yyTop] = new EnsureNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[-1+yyTop]));
                    }

                    /* NOEX_PRIVATE for toplevel */
                    yyVal = new DefnNode(getPosition(), ((String)yyVals[-7+yyTop]), ((INode)yyVals[-5+yyTop]),
		                      new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-4+yyTop])),
		                      support.getClassNest() !=0 || IdUtil.isAttrSet(((String)yyVals[-7+yyTop])) ? Visibility.PUBLIC : Visibility.PRIVATE);
                    /* $<Node>$.setPosFrom($4);*/
                    support.getLocalNames().pop();
                    support.setInDef(false);
                }
  break;
case 278:
					// line 1115 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 279:
					// line 1117 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 280:
					// line 1122 "DefaultRubyParser.y"
  {
                    if (((IListNode)yyVals[-3+yyTop]) != null) {
                        yyVals[-4+yyTop] = new RescueNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[-2+yyTop]));
                    } else if (((INode)yyVals[-2+yyTop]) != null) {
		        errorHandler.handleError(IErrors.WARN, null, "Else without rescue is useless.");
                        yyVals[-4+yyTop] = support.appendToBlock(((INode)yyVals[-4+yyTop]), ((INode)yyVals[-2+yyTop]));
                    }
                    if (((INode)yyVals[-1+yyTop]) != null) {
                        yyVals[-4+yyTop] = new EnsureNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[-1+yyTop]));
                    }
                    yyVal = new DefsNode(getPosition(), ((INode)yyVals[-10+yyTop]), ((String)yyVals[-7+yyTop]), ((INode)yyVals[-5+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-4+yyTop])));
                    /* $<Node>$.setPosFrom($2);*/
                    support.getLocalNames().pop();
                    support.setInSingle(support.getInSingle() - 1);
                }
  break;
case 281:
					// line 1137 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition());
                }
  break;
case 282:
					// line 1140 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition());
                }
  break;
case 283:
					// line 1143 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition());
                }
  break;
case 284:
					// line 1146 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition());
                }
  break;
case 291:
					// line 1158 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-3+yyTop])), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 293:
					// line 1164 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 295:
					// line 1169 "DefaultRubyParser.y"
  {
	            yyVal = ((MultipleAsgnNode)yyVals[0+yyTop]);
	      }
  break;
case 297:
					// line 1174 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
                }
  break;
case 298:
					// line 1177 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
		}
  break;
case 299:
					// line 1180 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 300:
					// line 1184 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
                }
  break;
case 301:
					// line 1186 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 302:
					// line 1191 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-1+yyTop]) instanceof BlockPassNode) {
		        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                    /* $$$2);*/
                }
  break;
case 303:
					// line 1199 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 304:
					// line 1203 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 305:
					// line 1208 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 306:
					// line 1211 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 307:
					// line 1215 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 308:
					// line 1219 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-2+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), null);
                }
  break;
case 309:
					// line 1223 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                    && !support.isInSingle() && !support.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    yyVal = support.new_super(((INode)yyVals[0+yyTop]), getPosition());
                }
  break;
case 310:
					// line 1230 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                    && !support.isInSingle() && !support.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    yyVal = new ZSuperNode(getPosition());
                }
  break;
case 311:
					// line 1238 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
                }
  break;
case 312:
					// line 1240 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 313:
					// line 1244 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
                }
  break;
case 314:
					// line 1246 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 315:
					// line 1251 "DefaultRubyParser.y"
  {
                    yyVal = new WhenNode(getPosition(), ((ArrayNode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 317:
					// line 1256 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-3+yyTop]).add(new ExpandArrayNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 318:
					// line 1260 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 319:
					// line 1265 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-1+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 320:
					// line 1268 "DefaultRubyParser.y"
  {
	            yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
	        }
  break;
case 321:
					// line 1272 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 323:
					// line 1277 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 325:
					// line 1282 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-2+yyTop]) != null) {
                        yyVals[-2+yyTop] = support.node_assign(((INode)yyVals[-2+yyTop]), new GlobalVarNode(getPosition(), "$!"));
                        yyVals[0+yyTop] = support.appendToBlock(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                    }
		    if (((IListNode)yyVals[-5+yyTop]) == null) {
		    	yyVals[-5+yyTop] = new ArrayNode(getPosition());
		    }
                    yyVal = ((IListNode)yyVals[-5+yyTop]).add(new RescueBodyNode(getPosition(), ((ArrayNode)yyVals[-3+yyTop]), ((INode)yyVals[0+yyTop])));
                }
  break;
case 326:
					// line 1292 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 328:
					// line 1297 "DefaultRubyParser.y"
  {
                    /*if ($2 != null) {*/
                        yyVal = ((INode)yyVals[0+yyTop]);
                    /*} else {*/
                    /*    $$ = new NilNode(null);*/
                    /*}*/
                }
  break;
case 330:
					// line 1306 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 331:
					// line 1309 "DefaultRubyParser.y"
  {
	            support.getLocalNames().getLocalIndex("~");  
	            yyVal = ((RegexpNode)yyVals[0+yyTop]);
	        }
  break;
case 332:
					// line 1314 "DefaultRubyParser.y"
  {
	            /* FIXME */
                    if (((INode)yyVals[-1+yyTop]) instanceof DStrNode) {
                        ((DStrNode)yyVals[-1+yyTop]).add(new StrNode(getPosition(), ((String)yyVals[0+yyTop])));
                    } else {
                        ((StrNode)yyVals[-1+yyTop]).setValue(((StrNode)yyVals[-1+yyTop]).getValue() + ((String)yyVals[0+yyTop]));
                    }
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 333:
					// line 1323 "DefaultRubyParser.y"
  {
	            /* FIXME */
                    if (((INode)yyVals[-1+yyTop]) instanceof StrNode) {
                        yyVal = new DStrNode(getPosition());
			((DStrNode)yyVal).add(((INode)yyVals[-1+yyTop]));
                    } else {
                        yyVal = ((INode)yyVals[-1+yyTop]);
                    }
		    yyVal = ListNodeUtil.addAll(((DStrNode)yyVal), ((DStrNode)yyVals[0+yyTop]));
                }
  break;
case 334:
					// line 1333 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 335:
					// line 1336 "DefaultRubyParser.y"
  {
	            yyVal = ((DStrNode)yyVals[0+yyTop]);
	        }
  break;
case 336:
					// line 1340 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 341:
					// line 1350 "DefaultRubyParser.y"
  {
                    if (((Number)yyVals[0+yyTop]) instanceof Long) {
                        yyVal = new FixnumNode(getPosition(), ((Long)yyVals[0+yyTop]).longValue());
                    } else {
                        yyVal = new BignumNode(getPosition(), ((BigInteger)yyVals[0+yyTop]));
                    }
                }
  break;
case 342:
					// line 1357 "DefaultRubyParser.y"
  {
	                yyVal = new FloatNode(getPosition(), ((Double)yyVals[0+yyTop]).doubleValue());
	            }
  break;
case 348:
					// line 1367 "DefaultRubyParser.y"
  {
                    if (((String)yyVals[0+yyTop]).equals("$_") || ((String)yyVals[0+yyTop]).equals("$~")) {
		        support.getLocalNames().getLocalIndex("~");
		    }
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 349:
					// line 1374 "DefaultRubyParser.y"
  {
                    yyVal = support.getAccessNode(((String)yyVals[0+yyTop]), getPosition());
                }
  break;
case 350:
					// line 1377 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(getPosition());
                }
  break;
case 351:
					// line 1380 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(getPosition());
                }
  break;
case 352:
					// line 1383 "DefaultRubyParser.y"
  { 
                    yyVal = new TrueNode(getPosition());
                }
  break;
case 353:
					// line 1386 "DefaultRubyParser.y"
  {
                    yyVal = new FalseNode(getPosition());
                }
  break;
case 354:
					// line 1389 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(), getPosition().getFile());
                }
  break;
case 355:
					// line 1392 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(), getPosition().getLine());
                }
  break;
case 356:
					// line 1396 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 357:
					// line 1399 "DefaultRubyParser.y"
  {
	            yyVal = ((INode)yyVals[0+yyTop]);
	        }
  break;
case 358:
					// line 1403 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 359:
					// line 1406 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 360:
					// line 1408 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 361:
					// line 1411 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 362:
					// line 1416 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 363:
					// line 1420 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 364:
					// line 1424 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-5+yyTop]).intValue(), ((IListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 365:
					// line 1427 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), ((IListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 366:
					// line 1430 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 367:
					// line 1433 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-1+yyTop]).intValue(), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 368:
					// line 1436 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((IListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 369:
					// line 1439 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((IListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 370:
					// line 1442 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 371:
					// line 1445 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 372:
					// line 1448 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, null);
                }
  break;
case 373:
					// line 1452 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 374:
					// line 1455 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 375:
					// line 1458 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a global variable");
                }
  break;
case 376:
					// line 1461 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 377:
					// line 1464 "DefaultRubyParser.y"
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
case 379:
					// line 1475 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(((Integer)yyVal).intValue() + 1);
                }
  break;
case 380:
					// line 1479 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[-2+yyTop]))) {
                        yyerror("duplicate optional argument name");
                    }
		    support.getLocalNames().getLocalIndex(((String)yyVals[-2+yyTop]));
                    yyVal = support.getAssignmentNode(((String)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), getPosition());
                }
  break;
case 381:
					// line 1489 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 382:
					// line 1492 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 383:
					// line 1496 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("rest argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate rest argument name");
                    }
                    yyVal = new Integer(support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 384:
					// line 1504 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(-2);
                }
  break;
case 385:
					// line 1508 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("block argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate block argument name");
                    }
                    yyVal = new BlockArgNode(getPosition(), support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 386:
					// line 1517 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 387:
					// line 1520 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 388:
					// line 1524 "DefaultRubyParser.y"
  {
                    /*if ($1 instanceof SelfNode()) {
                        $$ = new SelfNode(null);
                    } else {*/
                        yyVal = ((INode)yyVals[0+yyTop]);
                    /*}*/
                }
  break;
case 389:
					// line 1531 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 390:
					// line 1533 "DefaultRubyParser.y"
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
                    yyVal = ((INode)yyVals[-2+yyTop]);
                }
  break;
case 392:
					// line 1549 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-1+yyTop]);
                }
  break;
case 393:
					// line 1552 "DefaultRubyParser.y"
  {
                    if (ListNodeUtil.getLength(((ArrayNode)yyVals[-1+yyTop])) % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((ArrayNode)yyVals[-1+yyTop]);
                }
  break;
case 395:
					// line 1560 "DefaultRubyParser.y"
  {
                    yyVal = ListNodeUtil.addAll(((ArrayNode)yyVals[-2+yyTop]), ((ArrayNode)yyVals[0+yyTop]));
                }
  break;
case 396:
					// line 1564 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop])).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 416:
					// line 1594 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 419:
					// line 1600 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 420:
					// line 1604 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
					// line 2516 "-"
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
         75,    0,    5,    6,    6,    6,    6,   78,    7,    7,
          7,    7,    7,    7,    7,    7,    7,    7,   79,    7,
          7,    7,    7,    7,    7,    8,    8,    8,    8,    8,
          8,    8,    8,   12,   12,   12,   12,   12,   29,   29,
         29,   11,   11,   11,   11,   11,   46,   46,   48,   48,
         47,   47,   47,   47,   47,   47,   44,   44,   49,   49,
         45,   45,   45,   45,   45,   45,   40,   40,   40,   40,
         40,   40,   68,   68,   69,   69,   69,   69,   69,   61,
         61,   36,   81,   36,   70,   70,   70,   70,   70,   70,
         70,   70,   70,   70,   70,   70,   70,   70,   70,   70,
         70,   70,   70,   70,   70,   70,   70,   70,   70,   70,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,    9,   82,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,   84,    9,
          9,    9,   56,   56,   56,   56,   56,   56,   56,   21,
         21,   21,   21,   22,   22,   20,   20,   20,   20,   20,
         20,   20,   20,   20,   20,   86,   23,   59,   60,   60,
         50,   50,   25,   25,   26,   26,   26,   19,   10,   10,
         10,   10,   10,   10,   10,   10,   10,   10,   10,   10,
         10,   10,   10,   10,   10,   10,   10,   10,   10,   10,
         87,   10,   10,   10,   10,   10,   10,   89,   91,   10,
         92,   93,   10,   10,   10,   94,   95,   10,   96,   10,
         98,   99,   10,  100,   10,  101,   10,  103,  104,   10,
         10,   10,   10,   10,   88,   88,   88,   90,   90,   14,
         14,   15,   15,   38,   38,   39,   39,   39,   39,  105,
         43,   28,   28,   28,   13,   13,   13,   13,   13,   13,
        106,   42,  107,   42,   16,   51,   51,   51,   58,   58,
         52,   52,   17,   17,   57,   57,   18,   18,    3,    3,
          3,    2,    2,    2,    2,   64,   63,   63,   63,   63,
          4,    4,   62,   62,   62,   62,   62,   71,   24,   24,
         24,   24,   24,   24,   24,   37,   37,   27,  108,   27,
         27,   30,   30,   31,   31,   31,   31,   31,   31,   31,
         31,   31,   73,   73,   73,   73,   73,   74,   74,   32,
         55,   55,   72,   72,   33,   34,   34,    1,  109,    1,
         35,   35,   35,   54,   54,   53,   65,   65,   65,   66,
         66,   66,   66,   67,   67,   67,  102,  102,   76,   76,
         83,   83,   85,   85,   85,   97,   97,   77,   77,   41,
    };
  } /* End of class YyLhsClass */

  protected static final class YyLenClass {

    public static final short yyLen [] = {           2,
          0,    2,    2,    1,    1,    3,    2,    0,    4,    3,
          3,    3,    2,    3,    3,    3,    3,    3,    0,    5,
          4,    3,    3,    3,    1,    3,    2,    1,    3,    3,
          2,    2,    1,    1,    1,    2,    2,    2,    1,    4,
          4,    2,    4,    4,    2,    2,    1,    3,    1,    3,
          1,    2,    3,    2,    2,    1,    1,    3,    2,    3,
          1,    4,    3,    3,    3,    1,    1,    4,    3,    3,
          3,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    0,    4,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    3,    0,    4,    6,    5,    5,    5,    3,    3,
          3,    3,    3,    3,    3,    3,    3,    2,    2,    3,
          3,    3,    3,    3,    3,    3,    3,    3,    3,    3,
          3,    3,    2,    2,    3,    3,    3,    3,    0,    4,
          5,    1,    0,    2,    4,    2,    5,    2,    3,    3,
          4,    4,    6,    1,    1,    1,    3,    2,    5,    2,
          5,    4,    7,    3,    1,    0,    2,    2,    2,    0,
          1,    3,    1,    1,    3,    4,    2,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    6,    3,    3,
          2,    4,    3,    3,    4,    3,    1,    4,    3,    1,
          0,    6,    2,    1,    2,    6,    6,    0,    0,    7,
          0,    0,    7,    6,    5,    0,    0,    9,    0,    6,
          0,    0,    8,    0,    5,    0,    9,    0,    0,   12,
          1,    1,    1,    1,    1,    1,    2,    1,    1,    1,
          5,    1,    2,    1,    1,    1,    2,    1,    3,    0,
          5,    2,    4,    4,    2,    4,    4,    3,    2,    1,
          0,    5,    0,    5,    4,    1,    4,    2,    2,    1,
          0,    1,    2,    1,    6,    0,    1,    2,    1,    1,
          1,    2,    2,    1,    1,    2,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    0,    4,
          2,    4,    2,    6,    4,    4,    2,    4,    2,    2,
          1,    0,    1,    1,    1,    1,    1,    1,    3,    3,
          1,    3,    2,    1,    2,    2,    0,    1,    0,    5,
          1,    2,    2,    1,    3,    3,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    0,    1,
          0,    1,    0,    1,    1,    1,    1,    1,    2,    0,
    };
  } /* End class YyLenClass */

  protected static final class YyDefRedClass {

    public static final short yyDefRed [] = {            1,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  258,  261,    0,    0,    0,  283,  284,    0,    0,
          0,  351,  350,  352,  353,    0,    0,    0,   19,    0,
        355,  354,    0,    0,  348,  344,    0,  347,  341,  342,
        334,  232,  331,  233,  234,  357,  356,  335,  231,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        229,  329,    2,    0,    0,    0,    0,    0,    0,   28,
          0,  235,    0,   35,    0,    0,    4,    0,   57,    0,
         47,    0,    0,  330,    0,  345,    0,   73,   74,    0,
          0,  274,  120,  132,  121,  145,  117,  138,  127,  126,
        143,  125,  124,  119,  148,  129,  118,  133,  137,  139,
        131,  123,  140,  150,  142,    0,    0,    0,    0,  116,
        136,  135,  130,  146,  149,  147,  151,  115,  122,  113,
        114,    0,    0,    0,   77,    0,  106,  107,  104,   88,
         89,   90,   93,   95,   91,  108,  109,   96,   97,  101,
         92,   94,   85,   86,   87,   98,   99,  100,  102,  103,
        105,  110,  389,    0,  388,  349,  276,   78,   79,  141,
        134,  144,  128,  111,  112,   75,   76,    0,   82,   81,
         80,  326,    0,    0,    0,    0,  417,  416,    0,    0,
          0,  418,    0,    0,  281,  282,    0,    0,    0,    0,
          0,    0,    0,  294,  295,    0,    0,    0,    0,    0,
          0,    0,    0,  206,   37,    0,    0,  394,    0,  215,
          0,   38,    0,   27,   36,    0,   46,  228,    0,  309,
         45,    0,   31,    8,    0,  412,    0,    0,    0,    0,
          0,    0,  241,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,  391,    0,    0,    0,    0,   55,
          0,  338,  340,  336,  337,  339,    0,   32,    0,  332,
        333,    3,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  300,
        302,  313,  311,  255,    0,    0,    0,    0,   59,    0,
          0,    0,    0,  153,  305,   42,  253,    0,    0,  359,
        269,  358,    0,    0,  408,  407,  278,    0,   83,    0,
        286,    0,    0,    0,    0,    0,    0,    0,  320,    0,
        419,    0,    0,    0,    0,    0,    0,  266,    0,    0,
          0,    0,    0,    0,    0,  208,    0,  210,  246,    0,
        249,    0,    0,    0,    0,    0,    0,  217,    0,   11,
         12,   10,  251,    0,    0,    0,    0,    0,    0,  239,
          0,   36,    0,  194,    0,  414,  196,    0,  198,  243,
        244,    0,  393,  392,    0,    0,    0,    0,    0,    0,
          0,    0,   18,   29,   30,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  308,    0,    0,  402,    0,
          0,  403,    0,    0,    0,    0,  400,  401,    0,    0,
          0,    0,    0,   22,   24,    0,    0,   23,   26,  224,
          0,   53,   60,    0,    0,  361,    0,    0,    0,    0,
          0,    0,  374,  373,  376,    0,    0,    0,    0,    0,
        381,  371,    0,  375,    0,  378,    0,    0,    0,    0,
          0,  292,    0,  287,    0,    0,    0,    0,    0,    0,
          0,    0,  319,  289,  259,  288,  262,    0,    0,    0,
          0,    0,    0,    0,    0,  214,    0,    0,    0,    0,
          0,    0,    0,  207,    0,  219,    0,    0,  395,  245,
        248,    0,    0,    0,    0,  200,    0,    9,    0,    0,
          0,   21,    0,  199,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  307,   44,    0,    0,  205,  306,   43,
        204,    0,  298,    0,    0,  296,    0,    0,  304,   41,
        303,   40,    0,    0,   58,    0,  272,    0,    0,  275,
          0,  279,    0,  383,  385,    0,  326,  363,    0,  369,
          0,  370,    0,  367,   84,    0,    0,  293,    0,    0,
        327,    0,    0,  290,    0,    0,    0,    0,    0,  265,
          0,    0,    0,    0,    0,    0,    0,  212,    0,  201,
          0,    0,  202,    0,    0,    0,   20,    0,  195,    0,
          0,    0,    0,    0,    0,  297,    0,    0,    0,    0,
          0,    0,    0,  360,  270,  390,    0,    0,    0,    0,
          0,  382,  386,    0,    0,    0,  379,    0,    0,    0,
        324,  328,  238,    0,  256,  257,  264,    0,  315,    0,
          0,  267,  209,    0,  211,    0,  252,  197,    0,  299,
        301,  314,  312,    0,    0,    0,  362,    0,  368,    0,
        365,  366,    0,    0,  323,    0,    0,    0,    0,  260,
        263,    0,    0,  203,  273,  326,    0,    0,    0,    0,
          0,  325,    0,    0,  213,    0,  277,  364,    0,    0,
          0,    0,  291,  268,    0,    0,    0,  280,
    };
  } /* End of class YyDefRedClass */

  protected static final class YyDgotoClass {

    public static final short yyDgoto [] = {             1,
        164,   60,   61,   62,  245,   64,   65,   66,   67,  241,
         69,   70,   71,  613,  614,  349,  670,  610,  224,  228,
        568,  569,  231,   72,  469,  470,  331,   73,   74,  489,
        490,  491,  663,  600,  254,  178,  216,  203,  575,  184,
         77,  327,  311,   78,   79,   80,   81,  247,   82,  217,
        511,  607,  218,  219,  493,  253,  340,  350,  220,  366,
        179,  221,  264,   84,  207,  519,  446,   91,  181,  452,
         86,  495,  496,  497,    2,  190,  191,  379,  238,  169,
        498,  474,  237,  384,  397,  232,  549,  342,  193,  515,
        621,  194,  622,  524,  712,  478,  343,  475,  653,  333,
        338,  337,  481,  657,  454,  456,  455,  477,  334,
    };
  } /* End of class YyDgotoClass */

  protected static final class YySindexClass {

    public static final short yySindex [] = {            0,
          0,12071,12473,  -68, -115,15012,14919,12071,12962,12962,
      11901,    0,    0,16566,13065,13065,    0,    0,12567,12670,
         73,    0,    0,    0,    0,12962,14646,   62,    0,   11,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,14341,
      14341, -115,12276,13553,14341,16752,15285,14444,14341,  -97,
          0,    0,    0,  110,  407,  294, 3146,   -5, -159,    0,
       -103,    0,  -43,    0, -205,  126,    0,  172,    0,  195,
          0,16659,  -82,    0,  144,    0,  407,    0,    0,12962,
        173,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  -42,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,  237,    0,    0,
          0,    0,   37,  225,  241,   37,    0,    0,  123,   38,
        291,    0,12962,12962,    0,    0,  314,  336,   73,   62,
         13,    0,  131,    0,    0,    0,  144,12071,14341,14341,
      14341, 2272,   39,    0,    0, -205,  388,    0,  410,    0,
        -82,    0,12764,    0,    0,12868,    0,    0,13065,    0,
          0,13065,    0,    0, -219,    0,  380,  344,12071,  136,
         56,  136,    0,12276,  440,    0,  442,12567,14341,   62,
        166,  204,  393,  369,    0,  209,  204,   75,    0,    0,
          0,    0,    0,    0,    0,    0,  136,    0,  136,    0,
          0,    0,12370,12962,12962,12962,12962,12473,12962,12962,
      14341,14341,14341,14341,14341,14341,14341,14341,14341,14341,
      14341,14341,14341,14341,14341,14341,14341,14341,14341,14341,
      14341,14341,14341,14341,14341,14341,15413,15716,13553,    0,
          0,    0,    0,    0,15753,15753,14341,13656,    0,13656,
      12276,16752,  452,    0,    0,    0,    0,  294,  110,    0,
          0,    0,12071,12962,    0,    0,    0,  325,    0,   32,
          0,12071,  235,14341,13750,12071,   38,13853,    0, -143,
          0,  378,  378,  380,15790,15827,13553,    0, 2709, 3146,
      14341,15864,15901,13553,13159,    0,13262,    0,    0,  499,
          0,  501, -159,   62,   47,  522,  493,    0,14919,    0,
          0,    0,    0,14341,12071,  444,15864,15901,  537,    0,
          0,    0, 8454,    0,13947,    0,    0,14341,    0,    0,
          0,14341,    0,    0,15938,16241,13553,  407,  294,  294,
        294,  294,    0,    0,    0,  136, 1130, 1130, 1130, 1130,
        760,  760, 4455, 1603, 1130, 1130, 3583, 3583,  496,  496,
       4020,  760,  760,  212,  212,  111,  120,  120,  136,  136,
        136,  223,    0,    0,   73,    0,    0,  248,    0,  251,
         73,    0,  489,  -80,  -80,  -80,    0,    0,   73,   73,
       3146,14341, 3146,    0,    0,  555, 3146,    0,    0,    0,
        567,    0,    0,14341,  110,    0,12962,12071,  342,  162,
      15376,  554,    0,    0,    0,  317,  319,  911,12071,  110,
          0,    0,  581,    0,  592,    0,  598,14919,14341,12071,
        383,    0,  327,    0, 3146,  370, -143,14341, 3146,  605,
         58,  391,    0,    0,    0,    0,    0,    0,   73,    0,
          0,   73,  568,12962,  326,    0, 3146,  223,  248,  251,
        583,14341, 2272,    0,  634,    0,14341, 2272,    0,    0,
          0,  638,15753,15753,  649,    0,13159,    0,12962, 3146,
        556,    0,    0,    0,14341, 3146,   62,    0,    0,    0,
        599,14341,14341,    0,    0,14341,14341,    0,    0,    0,
          0,  349,    0,16473,12071,    0,12071,12071,    0,    0,
          0,    0, 3146,14050,    0, 3146,    0,  123,  433,    0,
        666,    0,14341,    0,    0,   62,    0,    0,  201,    0,
        363,    0,  911,    0,    0,  669,  374,    0,12071,  458,
          0,12962,  462,    0,  473,  476, 3146,14144,12071,    0,
      12071,12071,    0,  378,  349, 2709,13356,    0, 2709,    0,
         73,   73,    0, -159,   47,  188,    0, 8454,    0,    0,
       3146, 3146, 3146, 3146,14341,    0,  597,  479,  485,  628,
      14341, 3146,12071,    0,    0,    0,  325, 3146,  714,   32,
        554,    0,    0,  592,  712,  592,    0,14341,16752,   58,
          0,    0,    0,   37,    0,    0,    0,14341,    0,  502,
        503,    0,    0,14341,    0,  722,    0,    0, 3146,    0,
          0,    0,    0, 3146,  509,12071,    0,  383,    0,  201,
          0,    0,  104,    0,    0,    0,12071,12071, 3146,    0,
          0,12071, 2709,    0,    0,    0,  510,  592,16278,16315,
      13553,    0,  327,  513,    0,   32,    0,    0,    0,    0,
          0,  672,    0,    0,  383,    0,  514,    0,
    };
  } /* End of class YySindexClass */

  protected static final class YyRindexClass {

    public static final short yyRindex [] = {            0,
          0,  145,    0,    0,    0,    0,    0,  521,    0,    0,
        508,    0,    0,    0, 6611, 6696,    0,    0,11570, 6307,
       3453,    0,    0,    0,    0,    0,    0,14247,    0,    0,
          0,    0, 1693, 2674,    0,    0, 1792,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,  184,  688,  657,  533,    0,    0,    0, 5726,
          0,    0,    0,  846, 1632, 1341, 8932,12192, 6430,    0,
       5822,    0, 6707,    0,11087,    0,    0,    0,    0,    0,
          0,  536,11177,    0,13459,    0, 4825,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  100,  376,  524,  618,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0, 1013, 1079, 1143,    0, 1242,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0, 5837,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  508,    0,
        515,    0,    0,    0,    0,    0, 6126, 6211, 4854,  755,
          0,   51,    0,    0,    0,  348,    0,  184,    0,    0,
          0,11388, 7181,    0,    0, 5247, 5005,    0, 5005,    0,
       5337,    0,    0,    0,    0,    0,    0,    0,  758,    0,
          0,    0,    0,    0,    0,    0,14538,    0,  101, 7276,
       7096, 7579,    0,  184,    0,  505,    0, 6792,    0,  708,
        713,  713,    0,    0,    0,  683,  683,    0,  776,    0,
        956,    0,    0,    0,    0,    0, 7664,    0, 7759,    0,
          0,    0, 1415,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  688,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        184,  710,  711,    0,    0,    0,    0,  112,    0,    0,
          0,    0,   53,    0,    0,    0,    0,  119,    0,  340,
          0,  402,11455,    0,    0,  640,    0,    0,    0,  546,
          0,    0,    0,    0,    0,    0,  688,    0, 5005, 5945,
          0,    0,    0,  688,    0,    0,    0,    0,    0,    0,
          0,    0,  133,  770,  770,    0,  472,    0,    0,    0,
          0,    0,    0,    0,  101,    0,    0,    0,    0,    0,
        457,    0,  708,    0,  723,    0,    0,  186,    0,    0,
          0,  696,    0,    0,    0,    0,  688, 5777, 6322, 6747,
       6807, 7197,    0,    0,    0, 8062, 9641, 9727, 9824, 9906,
       9268, 9363, 9997,10245,10053,10150, 1362,10301, 8805, 8968,
          0, 9449, 9546, 8363, 9149, 9053, 8623, 8708, 8147, 8242,
       8545, 3985, 3016, 3548,13459,    0, 3111, 4327,    0, 4422,
       3890,    0,    0,11663,11663,11781,    0,    0, 4764, 4764,
      10341,    0, 5097,    0,    0,    0, 4965,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,   53,    0,  770,
          0,  176,    0,    0,    0,  467,    0,  482,  521,    0,
          0,    0,  520,    0,  520,    0,  520,    0,   59,  221,
        546,    0,  546,    0,10379,  546,  546,    0,   17,  169,
          0,    0,    0,    0,    0,    0,    0,  572,    0, 1057,
       1088, 5158,    0,    0,    0,    0,15107, 2142, 2237, 2579,
          0,    0,16396,    0, 5005,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,10461,
          0,    0,  527,    0,    0,   80,  708, 1750, 2103, 4812,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,11266,    0,    0,   53,    0,   53,  101,    0,    0,
          0,    0,12142,    0,    0,10645,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  770,    0,    0,    0,    0,
          0,    0,    0,    0,    0,   82,  175,    0,   53,    0,
          0,    0,    0,    0,    0,    0,  199,    0,  256,    0,
         53,   53,  565,    0, 5641, 5005,    0,    0, 5005,    0,
         60,   60,    0,  511,  770,    0,    0,  708,    0, 1349,
      10690,10727,10767,10809,    0,    0,    0,    0,    0,    0,
          0,16432,   53,    0,    0,    0,  119,  750,    0,  340,
          0,    0,    0,  520,  520,  520,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,10874,    0,
          0,    0,    0,16157,    0,  521,    0,  546,    0,    0,
          0,    0,    0,   90,    0,  105,  521,  402,  203,    0,
          0,   53, 5005,    0,    0,    0,    0,  520,    0,    0,
        688,    0,  546,    0,    0,  340,    0,    0,   79,  595,
        702,    0,    0,    0,  546,  106,    0,    0,
    };
  } /* End of class YyRindexClass */

  protected static final class YyGindexClass {

    public static final short yyGindex [] = {            0,
          0,    0,    0,    0,  622,    0,  160, 1015, 1213,   -2,
         15,    4,    0,  113, -308, -295,    0, -627,  232,  116,
        -19,  354,  -70,  817,    0,  506,    0, -191,    0,  181,
        352, -492, -288,  273,    0,    0,  736,  267,  197,  706,
        414,  771,    0,  761,  -37,  867,   61, -153,    0,   -6,
          0,    0, -334,   20,  242, -304, -515,  500, -286, -198,
         18,  454,    0,   19,  280, -279,    0,  240,   10,   16,
        -17, -392,  245,    0,    0,    1,  785,    0,    0,    0,
          0,    0, -106,    0,  335,    0,    0, -175,    0, -314,
          0,    0,    0,    0,    0,    0,   14,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,
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
    null,null,null,null,null,null,null,null,"'!'",null,null,null,"'%'",
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
    "tCONSTANT","tCVAR","tINTEGER","tFLOAT","tSTRING","tXSTRING",
    "tREGEXP","tDXSTRING","tDREGEXP","tBACK_REF","tNTH_REF","tDSTRING",
    "tARRAY","tUPLUS","tUMINUS","tPOW","tCMP","tEQ","tEQQ","tNEQ","tGEQ",
    "tLEQ","tANDOP","tOROP","tMATCH","tNMATCH","tDOT2","tDOT3","tAREF",
    "tASET","tLSHFT","tRSHFT","tCOLON2","tCOLON3","tOP_ASGN","tASSOC",
    "tLPAREN","tLBRACK","tLBRACE","tSTAR","tAMPER","tSYMBEG","LAST_TOKEN",
    };
  } /* End of class YyNameClass */


					// line 1608 "DefaultRubyParser.y"

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
					// line 6847 "-"
