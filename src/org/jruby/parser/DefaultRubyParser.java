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
case 37:
					// line 368 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 372 "DefaultRubyParser.y"
  {
	            support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 39:
					// line 377 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 40:
					// line 380 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 41:
					// line 384 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 42:
					// line 388 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && support.isInDef() 
                                               && support.isInSingle()){
                        yyerror("super called outside of method");
                    }
		    yyVal = support.new_super(((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
		}
  break;
case 43:
					// line 395 "DefaultRubyParser.y"
  {
	            yyVal = new YieldNode(getPosition(), ((INode)yyVals[0+yyTop])); /* .setPosFrom($2);*/
		}
  break;
case 45:
					// line 400 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
		}
  break;
case 47:
					// line 405 "DefaultRubyParser.y"
  {
	            yyVal = new MultipleAsgnNode(getPosition(), new ArrayNode(getPosition()).add(((MultipleAsgnNode)yyVals[-1+yyTop])), null);
                }
  break;
case 48:
					// line 409 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[0+yyTop]), null);
                }
  break;
case 49:
					// line 412 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[-1+yyTop]).add(((INode)yyVals[0+yyTop])), null);
                }
  break;
case 50:
					// line 415 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 51:
					// line 418 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), ((ArrayNode)yyVals[-1+yyTop]), new StarNode());
                }
  break;
case 52:
					// line 421 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, ((INode)yyVals[0+yyTop]));
                }
  break;
case 53:
					// line 424 "DefaultRubyParser.y"
  {
                    yyVal = new MultipleAsgnNode(getPosition(), null, new StarNode());
                }
  break;
case 55:
					// line 429 "DefaultRubyParser.y"
  {
                    yyVal = ((MultipleAsgnNode)yyVals[-1+yyTop]);
                }
  break;
case 56:
					// line 433 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 57:
					// line 436 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 58:
					// line 440 "DefaultRubyParser.y"
  {
                    yyVal = support.getAssignmentNode(((String)yyVals[0+yyTop]), null, getPosition());
                }
  break;
case 59:
					// line 443 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((INode)yyVals[-3+yyTop]), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 60:
					// line 446 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 61:
					// line 449 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 62:
					// line 452 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 63:
					// line 455 "DefaultRubyParser.y"
  {
	            support.backrefAssignError(((INode)yyVals[0+yyTop]));
                    yyVal = null;
                }
  break;
case 64:
					// line 460 "DefaultRubyParser.y"
  {
                    yyVal = support.getAssignmentNode(((String)yyVals[0+yyTop]), null, getPosition());
                }
  break;
case 65:
					// line 463 "DefaultRubyParser.y"
  {
                    yyVal = support.getElementAssignmentNode(((INode)yyVals[-3+yyTop]), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 66:
					// line 466 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 469 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 68:
					// line 472 "DefaultRubyParser.y"
  {
                    yyVal = support.getAttributeAssignmentNode(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 475 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[0+yyTop]));
                    yyVal = null;
		}
  break;
case 70:
					// line 480 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 75:
					// line 488 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 76:
					// line 492 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 79:
					// line 500 "DefaultRubyParser.y"
  {
                    yyVal = new UndefNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 80:
					// line 503 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
	        }
  break;
case 81:
					// line 505 "DefaultRubyParser.y"
  {
                    yyVal = support.appendToBlock(((INode)yyVals[-3+yyTop]), new UndefNode(getPosition(), ((String)yyVals[0+yyTop])));
                }
  break;
case 82:
					// line 509 "DefaultRubyParser.y"
  { yyVal = "|"; }
  break;
case 83:
					// line 510 "DefaultRubyParser.y"
  { yyVal = "^"; }
  break;
case 84:
					// line 511 "DefaultRubyParser.y"
  { yyVal = "&"; }
  break;
case 85:
					// line 512 "DefaultRubyParser.y"
  { yyVal = "<=>"; }
  break;
case 86:
					// line 513 "DefaultRubyParser.y"
  { yyVal = "=="; }
  break;
case 87:
					// line 514 "DefaultRubyParser.y"
  { yyVal = "==="; }
  break;
case 88:
					// line 515 "DefaultRubyParser.y"
  { yyVal = "=~"; }
  break;
case 89:
					// line 516 "DefaultRubyParser.y"
  { yyVal = ">"; }
  break;
case 90:
					// line 517 "DefaultRubyParser.y"
  { yyVal = ">="; }
  break;
case 91:
					// line 518 "DefaultRubyParser.y"
  { yyVal = "<"; }
  break;
case 92:
					// line 519 "DefaultRubyParser.y"
  { yyVal = "<="; }
  break;
case 93:
					// line 520 "DefaultRubyParser.y"
  { yyVal = "<<"; }
  break;
case 94:
					// line 521 "DefaultRubyParser.y"
  { yyVal = ">>"; }
  break;
case 95:
					// line 522 "DefaultRubyParser.y"
  { yyVal = "+"; }
  break;
case 96:
					// line 523 "DefaultRubyParser.y"
  { yyVal = "-"; }
  break;
case 97:
					// line 524 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 98:
					// line 525 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 99:
					// line 526 "DefaultRubyParser.y"
  { yyVal = "/"; }
  break;
case 100:
					// line 527 "DefaultRubyParser.y"
  { yyVal = "%"; }
  break;
case 101:
					// line 528 "DefaultRubyParser.y"
  { yyVal = "**"; }
  break;
case 102:
					// line 529 "DefaultRubyParser.y"
  { yyVal = "~"; }
  break;
case 103:
					// line 530 "DefaultRubyParser.y"
  { yyVal = "+@"; }
  break;
case 104:
					// line 531 "DefaultRubyParser.y"
  { yyVal = "-@"; }
  break;
case 105:
					// line 532 "DefaultRubyParser.y"
  { yyVal = "[]"; }
  break;
case 106:
					// line 533 "DefaultRubyParser.y"
  { yyVal = "[]="; }
  break;
case 107:
					// line 534 "DefaultRubyParser.y"
  { yyVal = "`"; }
  break;
case 149:
					// line 578 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = support.node_assign(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 150:
					// line 582 "DefaultRubyParser.y"
  {
                    yyVal = support.getAssignmentNode(((String)yyVals[-1+yyTop]), null, getPosition());
                }
  break;
case 151:
					// line 584 "DefaultRubyParser.y"
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
case 152:
					// line 603 "DefaultRubyParser.y"
  {
                    yyVal = new OpElementAsgnNode(getPosition(), ((INode)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), ((IListNode)yyVals[-3+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 153:
					// line 606 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 154:
					// line 609 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 155:
					// line 612 "DefaultRubyParser.y"
  {
                    yyVal = new OpAsgnNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((INode)yyVals[0+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]));
                }
  break;
case 156:
					// line 615 "DefaultRubyParser.y"
  {
                    support.backrefAssignError(((INode)yyVals[-2+yyTop]));
                    yyVal = null;
                }
  break;
case 157:
					// line 619 "DefaultRubyParser.y"
  {
                    yyVal = new DotNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), false);
                }
  break;
case 158:
					// line 622 "DefaultRubyParser.y"
  {
                    yyVal = new DotNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]), true);
                }
  break;
case 159:
					// line 625 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "+", ((INode)yyVals[0+yyTop]));
                }
  break;
case 160:
					// line 628 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "-", ((INode)yyVals[0+yyTop]));
                }
  break;
case 161:
					// line 631 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "*", ((INode)yyVals[0+yyTop]));
                }
  break;
case 162:
					// line 634 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "/", ((INode)yyVals[0+yyTop]));
                }
  break;
case 163:
					// line 637 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "%", ((INode)yyVals[0+yyTop]));
                }
  break;
case 164:
					// line 640 "DefaultRubyParser.y"
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
case 165:
					// line 657 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "+@");
                }
  break;
case 166:
					// line 660 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "-@");
                }
  break;
case 167:
					// line 663 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "|", ((INode)yyVals[0+yyTop]));
                }
  break;
case 168:
					// line 666 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "^", ((INode)yyVals[0+yyTop]));
                }
  break;
case 169:
					// line 669 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "&", ((INode)yyVals[0+yyTop]));
                }
  break;
case 170:
					// line 672 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<=>", ((INode)yyVals[0+yyTop]));
                }
  break;
case 171:
					// line 675 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">", ((INode)yyVals[0+yyTop]));
                }
  break;
case 172:
					// line 678 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">=", ((INode)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 681 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<", ((INode)yyVals[0+yyTop]));
                }
  break;
case 174:
					// line 684 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<=", ((INode)yyVals[0+yyTop]));
                }
  break;
case 175:
					// line 687 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "==", ((INode)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 690 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "===", ((INode)yyVals[0+yyTop]));
                }
  break;
case 177:
					// line 693 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "==", ((INode)yyVals[0+yyTop])));
                }
  break;
case 178:
					// line 696 "DefaultRubyParser.y"
  {
                    yyVal = support.getMatchNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 179:
					// line 699 "DefaultRubyParser.y"
  {
                    yyVal = new NotNode(getPosition(), support.getMatchNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop])));
                }
  break;
case 180:
					// line 702 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new NotNode(getPosition(), support.getConditionNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 181:
					// line 706 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[0+yyTop]), "~");
                }
  break;
case 182:
					// line 709 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), "<<", ((INode)yyVals[0+yyTop]));
                }
  break;
case 183:
					// line 712 "DefaultRubyParser.y"
  {
                    yyVal = support.getOperatorCallNode(((INode)yyVals[-2+yyTop]), ">>", ((INode)yyVals[0+yyTop]));
                }
  break;
case 184:
					// line 715 "DefaultRubyParser.y"
  {
                    yyVal = support.newAndNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 718 "DefaultRubyParser.y"
  {
                    yyVal = support.newOrNode(((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 721 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 187:
					// line 723 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 727 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 189:
					// line 732 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 190:
					// line 736 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 191:
					// line 739 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 192:
					// line 742 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-3+yyTop]).add(((INode)yyVals[-1+yyTop]));
                }
  break;
case 193:
					// line 745 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-1+yyTop]);
                }
  break;
case 194:
					// line 748 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop])));
                }
  break;
case 195:
					// line 752 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(new HashNode(((ArrayNode)yyVals[-1+yyTop])));
                }
  break;
case 196:
					// line 755 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));

                    yyVal = new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop])));
                }
  break;
case 197:
					// line 761 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 198:
					// line 764 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-2+yyTop]);
                }
  break;
case 199:
					// line 767 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop]));
                }
  break;
case 200:
					// line 770 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-4+yyTop]).add(((INode)yyVals[-2+yyTop]));
                }
  break;
case 203:
					// line 777 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 780 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 205:
					// line 783 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-1+yyTop]), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 786 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 207:
					// line 790 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new HashNode(((ArrayNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 208:
					// line 793 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-4+yyTop]).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 209:
					// line 797 "DefaultRubyParser.y"
  {
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-3+yyTop]).add(new HashNode(((ArrayNode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 800 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = support.arg_blk_pass(((ArrayNode)yyVals[-6+yyTop]).add(new HashNode(((ArrayNode)yyVals[-4+yyTop]))).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 804 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
		    /* FIXME*/
                    /* $$ = support.arg_blk_pass(new RestArgsNode(getPosition(), $2), $3);*/
		    yyVal = support.arg_blk_pass(new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[-1+yyTop]))), ((BlockPassNode)yyVals[0+yyTop]));
                }
  break;
case 212:
					// line 810 "DefaultRubyParser.y"
  {
	            yyVal = ((BlockPassNode)yyVals[0+yyTop]);
	        }
  break;
case 213:
					// line 814 "DefaultRubyParser.y"
  { 
		    yyVal = new Long(lexer.getCmdArgumentState().begin());
		}
  break;
case 214:
					// line 816 "DefaultRubyParser.y"
  {
                    lexer.getCmdArgumentState().reset(((Long)yyVals[-1+yyTop]).longValue());
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 215:
					// line 821 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new BlockPassNode(getPosition(), ((INode)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 826 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockPassNode)yyVals[0+yyTop]);
                }
  break;
case 217:
					// line 829 "DefaultRubyParser.y"
  {
	            yyVal = null;
	      }
  break;
case 218:
					// line 833 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 219:
					// line 837 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 220:
					// line 842 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 222:
					// line 848 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 223:
					// line 852 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-3+yyTop]).add(new ExpandArrayNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 224:
					// line 856 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 225:
					// line 861 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                    if (((INode)yyVals[0+yyTop]) instanceof ArrayNode && ListNodeUtil.getLength(((IListNode)yyVals[0+yyTop])) == 1) {
                        yyVal = ListNodeUtil.getLast(((IListNode)yyVals[0+yyTop]));
                    } else if (((INode)yyVals[0+yyTop]) instanceof BlockPassNode) {
                        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Block argument should not be given.");
                    }
                }
  break;
case 228:
					// line 872 "DefaultRubyParser.y"
  {
	            yyVal = ((ArrayNode)yyVals[0+yyTop]);
	        }
  break;
case 229:
					// line 875 "DefaultRubyParser.y"
  {
                    yyVal = new XStrNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 231:
					// line 879 "DefaultRubyParser.y"
  {
	            support.getLocalNames().getLocalIndex("~");
	            yyVal = ((INode)yyVals[0+yyTop]);
	        }
  break;
case 234:
					// line 885 "DefaultRubyParser.y"
  {
                    yyVal = new VCallNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 235:
					// line 888 "DefaultRubyParser.y"
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
case 236:
					// line 905 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 237:
					// line 908 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-2+yyTop]));
                    yyVal = new Colon2Node(getPosition(), ((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 912 "DefaultRubyParser.y"
  {
                    yyVal = new Colon3Node(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 239:
					// line 915 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = new CallNode(getPosition(), ((INode)yyVals[-3+yyTop]), "[]", ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 240:
					// line 919 "DefaultRubyParser.y"
  {
                    if (((IListNode)yyVals[-1+yyTop]) == null) {
                        yyVal = new ArrayNode(getPosition()); /* zero length array*/
                    } else {
                        yyVal = ((IListNode)yyVals[-1+yyTop]);
                    }
                }
  break;
case 241:
					// line 926 "DefaultRubyParser.y"
  {
                    yyVal = new HashNode(getPosition(), ((IListNode)yyVals[-1+yyTop]));
                }
  break;
case 242:
					// line 929 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef() 
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = new ReturnNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 243:
					// line 937 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = new ReturnNode(getPosition(), null);
                }
  break;
case 244:
					// line 944 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                               && !support.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = new ReturnNode(getPosition(), null);
                }
  break;
case 245:
					// line 951 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-1+yyTop]));
                    yyVal = new YieldNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 246:
					// line 955 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null);
                }
  break;
case 247:
					// line 958 "DefaultRubyParser.y"
  {
                    yyVal = new YieldNode(getPosition(), null);
                }
  break;
case 248:
					// line 961 "DefaultRubyParser.y"
  {
	            support.setInDefined(true);
		}
  break;
case 249:
					// line 963 "DefaultRubyParser.y"
  {
                    support.setInDefined(false);
                    yyVal = new DefinedNode(getPosition(), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 250:
					// line 967 "DefaultRubyParser.y"
  {
                    ((IterNode)yyVals[0+yyTop]).setIterNode(new FCallNode(getPosition(), ((String)yyVals[-1+yyTop]), null));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                }
  break;
case 252:
					// line 972 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-1+yyTop]) instanceof BlockPassNode) {
                       errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                    /* $<Node>$.setPosFrom($1);*/
                }
  break;
case 253:
					// line 980 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 254:
					// line 984 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[-2+yyTop]));
                }
  break;
case 255:
					// line 988 "DefaultRubyParser.y"
  { 
	            lexer.getConditionState().begin();
		}
  break;
case 256:
					// line 990 "DefaultRubyParser.y"
  {
		    lexer.getConditionState().end();
		}
  break;
case 257:
					// line 992 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new WhileNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 258:
					// line 996 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 259:
					// line 998 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 260:
					// line 1000 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new UntilNode(getPosition(), support.getConditionNode(((INode)yyVals[-4+yyTop])), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 261:
					// line 1004 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new CaseNode(getPosition(), ((INode)yyVals[-4+yyTop]), ((IListNode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop])); /* XXX*/
                }
  break;
case 262:
					// line 1008 "DefaultRubyParser.y"
  {
                    yyVal = new CaseNode(getPosition(), null, ((IListNode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]));
                }
  break;
case 263:
					// line 1011 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().begin();
                }
  break;
case 264:
					// line 1013 "DefaultRubyParser.y"
  {
                    lexer.getConditionState().end();
                }
  break;
case 265:
					// line 1015 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-4+yyTop]));
                    yyVal = new ForNode(getPosition(), ((INode)yyVals[-7+yyTop]), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[-4+yyTop]));
                }
  break;
case 266:
					// line 1019 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 267:
					// line 1026 "DefaultRubyParser.y"
  {
                    yyVal = new ClassNode(getPosition(), ((String)yyVals[-4+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])), ((INode)yyVals[-3+yyTop]));
                    /* $<INode>$.setLine($<Integer>4.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 268:
					// line 1032 "DefaultRubyParser.y"
  {
                    yyVal = new Boolean(support.isInDef());
                    support.setInDef(false);
                }
  break;
case 269:
					// line 1035 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(support.getInSingle());
                    support.setInSingle(0);
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                }
  break;
case 270:
					// line 1040 "DefaultRubyParser.y"
  {
                    yyVal = new SClassNode(getPosition(), ((INode)yyVals[-5+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                    support.setInDef(((Boolean)yyVals[-4+yyTop]).booleanValue());
                    support.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 271:
					// line 1047 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    support.setClassNest(support.getClassNest() + 1);
                    support.getLocalNames().push();
                    /* $$ = new Integer(ruby.getSourceLine());*/
                }
  break;
case 272:
					// line 1054 "DefaultRubyParser.y"
  {
                    yyVal = new ModuleNode(getPosition(), ((String)yyVals[-3+yyTop]), new ScopeNode(support.getLocalNames().getNames(), ((INode)yyVals[-1+yyTop])));
                    /* $<Node>$.setLine($<Integer>3.intValue());*/
                    support.getLocalNames().pop();
                    support.setClassNest(support.getClassNest() - 1);
                }
  break;
case 273:
					// line 1060 "DefaultRubyParser.y"
  {
                    if (support.isInDef() || support.isInSingle()) {
                        yyerror("nested method definition");
                    }
                    support.setInDef(true);
                    support.getLocalNames().push();
                }
  break;
case 274:
					// line 1066 "DefaultRubyParser.y"
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
case 275:
					// line 1085 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_FNAME);
                }
  break;
case 276:
					// line 1087 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    support.setInSingle(support.getInSingle() + 1);
                    support.getLocalNames().push();
                    lexer.setState(LexState.EXPR_END); /* force for args */
                }
  break;
case 277:
					// line 1092 "DefaultRubyParser.y"
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
case 278:
					// line 1107 "DefaultRubyParser.y"
  {
                    yyVal = new BreakNode(getPosition());
                }
  break;
case 279:
					// line 1110 "DefaultRubyParser.y"
  {
                    yyVal = new NextNode(getPosition());
                }
  break;
case 280:
					// line 1113 "DefaultRubyParser.y"
  {
                    yyVal = new RedoNode(getPosition());
                }
  break;
case 281:
					// line 1116 "DefaultRubyParser.y"
  {
                    yyVal = new RetryNode(getPosition());
                }
  break;
case 288:
					// line 1128 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = new IfNode(getPosition(), support.getConditionNode(((INode)yyVals[-3+yyTop])), ((INode)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 290:
					// line 1134 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 292:
					// line 1139 "DefaultRubyParser.y"
  {
	            yyVal = ((MultipleAsgnNode)yyVals[0+yyTop]);
	      }
  break;
case 294:
					// line 1144 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
                }
  break;
case 295:
					// line 1147 "DefaultRubyParser.y"
  {
                    yyVal = new ZeroArgNode();
		}
  break;
case 296:
					// line 1150 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 297:
					// line 1154 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
                }
  break;
case 298:
					// line 1156 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 299:
					// line 1161 "DefaultRubyParser.y"
  {
                    if (((INode)yyVals[-1+yyTop]) instanceof BlockPassNode) {
		        errorHandler.handleError(IErrors.COMPILE_ERROR, null, "Both block arg and actual block given.");
                    }
                    ((IterNode)yyVals[0+yyTop]).setIterNode(((INode)yyVals[-1+yyTop]));
                    yyVal = ((IterNode)yyVals[0+yyTop]);
                    /* $$$2);*/
                }
  break;
case 300:
					// line 1169 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 301:
					// line 1173 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 302:
					// line 1178 "DefaultRubyParser.y"
  {
                    yyVal = support.new_fcall(((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop]), getPosition()); /* .setPosFrom($2);*/
                }
  break;
case 303:
					// line 1181 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 304:
					// line 1185 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-3+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((INode)yyVals[0+yyTop])); /*.setPosFrom($1);*/
                }
  break;
case 305:
					// line 1189 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[-2+yyTop]));
                    yyVal = support.new_call(((INode)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), null);
                }
  break;
case 306:
					// line 1193 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                    && !support.isInSingle() && !support.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    yyVal = support.new_super(((INode)yyVals[0+yyTop]), getPosition());
                }
  break;
case 307:
					// line 1200 "DefaultRubyParser.y"
  {
                    if (!support.isCompileForEval() && !support.isInDef()
                                    && !support.isInSingle() && !support.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    yyVal = new ZSuperNode(getPosition());
                }
  break;
case 308:
					// line 1208 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
                }
  break;
case 309:
					// line 1210 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 310:
					// line 1214 "DefaultRubyParser.y"
  {
                    support.getBlockNames().push();
                }
  break;
case 311:
					// line 1216 "DefaultRubyParser.y"
  {
                    yyVal = new IterNode(getPosition(), ((INode)yyVals[-2+yyTop]), ((INode)yyVals[-1+yyTop]), null);
                    support.getBlockNames().pop();
                }
  break;
case 312:
					// line 1221 "DefaultRubyParser.y"
  {
                    yyVal = new WhenNode(getPosition(), ((ArrayNode)yyVals[-2+yyTop]), ((INode)yyVals[0+yyTop]));
                }
  break;
case 314:
					// line 1226 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = ((ArrayNode)yyVals[-3+yyTop]).add(new ExpandArrayNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 315:
					// line 1230 "DefaultRubyParser.y"
  {
                    support.checkExpression(((INode)yyVals[0+yyTop]));
                    yyVal = new ArrayNode(getPosition()).add(new ExpandArrayNode(((INode)yyVals[0+yyTop])));
                }
  break;
case 316:
					// line 1235 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-1+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 317:
					// line 1238 "DefaultRubyParser.y"
  {
	            yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
	        }
  break;
case 318:
					// line 1242 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 320:
					// line 1247 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 322:
					// line 1252 "DefaultRubyParser.y"
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
case 323:
					// line 1262 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 325:
					// line 1267 "DefaultRubyParser.y"
  {
                    /*if ($2 != null) {*/
                        yyVal = ((INode)yyVals[0+yyTop]);
                    /*} else {*/
                    /*    $$ = new NilNode(null);*/
                    /*}*/
                }
  break;
case 327:
					// line 1276 "DefaultRubyParser.y"
  {
                    yyVal = new SymbolNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 328:
					// line 1279 "DefaultRubyParser.y"
  {
	            support.getLocalNames().getLocalIndex("~");  
	            yyVal = ((RegexpNode)yyVals[0+yyTop]);
	        }
  break;
case 329:
					// line 1284 "DefaultRubyParser.y"
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
case 330:
					// line 1293 "DefaultRubyParser.y"
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
case 331:
					// line 1303 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(), ((String)yyVals[0+yyTop]));
                }
  break;
case 332:
					// line 1306 "DefaultRubyParser.y"
  {
	            yyVal = ((DStrNode)yyVals[0+yyTop]);
	        }
  break;
case 333:
					// line 1310 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 338:
					// line 1320 "DefaultRubyParser.y"
  {
                    if (((Number)yyVals[0+yyTop]) instanceof Long) {
                        yyVal = new FixnumNode(getPosition(), ((Long)yyVals[0+yyTop]).longValue());
                    } else {
                        yyVal = new BignumNode(getPosition(), ((BigInteger)yyVals[0+yyTop]));
                    }
                }
  break;
case 339:
					// line 1327 "DefaultRubyParser.y"
  {
	                yyVal = new FloatNode(getPosition(), ((Double)yyVals[0+yyTop]).doubleValue());
	            }
  break;
case 345:
					// line 1337 "DefaultRubyParser.y"
  {
                    if (((String)yyVals[0+yyTop]).equals("$_") || ((String)yyVals[0+yyTop]).equals("$~")) {
		        support.getLocalNames().getLocalIndex("~");
		    }
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 346:
					// line 1344 "DefaultRubyParser.y"
  {
                    yyVal = support.getAccessNode(((String)yyVals[0+yyTop]), getPosition());
                }
  break;
case 347:
					// line 1347 "DefaultRubyParser.y"
  { 
                    yyVal = new NilNode(getPosition());
                }
  break;
case 348:
					// line 1350 "DefaultRubyParser.y"
  {
                    yyVal = new SelfNode(getPosition());
                }
  break;
case 349:
					// line 1353 "DefaultRubyParser.y"
  { 
                    yyVal = new TrueNode(getPosition());
                }
  break;
case 350:
					// line 1356 "DefaultRubyParser.y"
  {
                    yyVal = new FalseNode(getPosition());
                }
  break;
case 351:
					// line 1359 "DefaultRubyParser.y"
  {
                    yyVal = new StrNode(getPosition(), getPosition().getFile());
                }
  break;
case 352:
					// line 1362 "DefaultRubyParser.y"
  {
                    yyVal = new FixnumNode(getPosition(), getPosition().getLine());
                }
  break;
case 353:
					// line 1366 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[0+yyTop]);
                }
  break;
case 354:
					// line 1369 "DefaultRubyParser.y"
  {
	            yyVal = ((INode)yyVals[0+yyTop]);
	        }
  break;
case 355:
					// line 1373 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 356:
					// line 1376 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 357:
					// line 1378 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 358:
					// line 1381 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 359:
					// line 1386 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-2+yyTop]);
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 360:
					// line 1390 "DefaultRubyParser.y"
  {
                    yyVal = ((INode)yyVals[-1+yyTop]);
                }
  break;
case 361:
					// line 1394 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-5+yyTop]).intValue(), ((IListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 362:
					// line 1397 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), ((IListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 363:
					// line 1400 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-3+yyTop]).intValue(), null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 364:
					// line 1403 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), ((Integer)yyVals[-1+yyTop]).intValue(), null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 365:
					// line 1406 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((IListNode)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 366:
					// line 1409 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, ((IListNode)yyVals[-1+yyTop]), -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 367:
					// line 1412 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, ((Integer)yyVals[-1+yyTop]).intValue(), ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 368:
					// line 1415 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, ((BlockArgNode)yyVals[0+yyTop]));
                }
  break;
case 369:
					// line 1418 "DefaultRubyParser.y"
  {
                    yyVal = new ArgsNode(getPosition(), 0, null, -1, null);
                }
  break;
case 370:
					// line 1422 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 371:
					// line 1425 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 372:
					// line 1428 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a global variable");
                }
  break;
case 373:
					// line 1431 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 374:
					// line 1434 "DefaultRubyParser.y"
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
case 376:
					// line 1445 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(((Integer)yyVal).intValue() + 1);
                }
  break;
case 377:
					// line 1449 "DefaultRubyParser.y"
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
case 378:
					// line 1459 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 379:
					// line 1462 "DefaultRubyParser.y"
  {
                    yyVal = ((IListNode)yyVals[-2+yyTop]).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 380:
					// line 1466 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("rest argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate rest argument name");
                    }
                    yyVal = new Integer(support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 381:
					// line 1474 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(-2);
                }
  break;
case 382:
					// line 1478 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("block argument must be local variable");
                    } else if (support.getLocalNames().isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate block argument name");
                    }
                    yyVal = new BlockArgNode(getPosition(), support.getLocalNames().getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 383:
					// line 1487 "DefaultRubyParser.y"
  {
                    yyVal = ((BlockArgNode)yyVals[0+yyTop]);
                }
  break;
case 384:
					// line 1490 "DefaultRubyParser.y"
  {
	            yyVal = null;
	        }
  break;
case 385:
					// line 1494 "DefaultRubyParser.y"
  {
                    /*if ($1 instanceof SelfNode()) {
                        $$ = new SelfNode(null);
                    } else {*/
                        yyVal = ((INode)yyVals[0+yyTop]);
                    /*}*/
                }
  break;
case 386:
					// line 1501 "DefaultRubyParser.y"
  {
                    lexer.setState(LexState.EXPR_BEG);
                }
  break;
case 387:
					// line 1503 "DefaultRubyParser.y"
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
case 389:
					// line 1519 "DefaultRubyParser.y"
  {
                    yyVal = ((ArrayNode)yyVals[-1+yyTop]);
                }
  break;
case 390:
					// line 1522 "DefaultRubyParser.y"
  {
                    if (ListNodeUtil.getLength(((ArrayNode)yyVals[-1+yyTop])) % 2 != 0) {
                        yyerror("Odd number list for Hash.");
                    }
                    yyVal = ((ArrayNode)yyVals[-1+yyTop]);
                }
  break;
case 392:
					// line 1530 "DefaultRubyParser.y"
  {
                    yyVal = ListNodeUtil.addAll(((ArrayNode)yyVals[-2+yyTop]), ((ArrayNode)yyVals[0+yyTop]));
                }
  break;
case 393:
					// line 1534 "DefaultRubyParser.y"
  {
                    yyVal = new ArrayNode(getPosition()).add(((INode)yyVals[-2+yyTop])).add(((INode)yyVals[0+yyTop]));
                }
  break;
case 413:
					// line 1564 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 416:
					// line 1570 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 417:
					// line 1574 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
					// line 2477 "-"
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
          8,    8,    8,   12,   12,   29,   29,   29,   11,   11,
         11,   11,   11,   46,   46,   48,   48,   47,   47,   47,
         47,   47,   47,   44,   44,   49,   49,   45,   45,   45,
         45,   45,   45,   40,   40,   40,   40,   40,   40,   68,
         68,   69,   69,   69,   69,   69,   61,   61,   36,   81,
         36,   70,   70,   70,   70,   70,   70,   70,   70,   70,
         70,   70,   70,   70,   70,   70,   70,   70,   70,   70,
         70,   70,   70,   70,   70,   70,   70,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
         80,   80,   80,   80,   80,   80,   80,   80,    9,   82,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,   84,    9,    9,    9,   56,
         56,   56,   56,   56,   56,   56,   21,   21,   21,   21,
         22,   22,   20,   20,   20,   20,   20,   20,   20,   20,
         20,   20,   86,   23,   59,   60,   60,   50,   50,   25,
         25,   26,   26,   26,   19,   10,   10,   10,   10,   10,
         10,   10,   10,   10,   10,   10,   10,   10,   10,   10,
         10,   10,   10,   10,   10,   10,   10,   87,   10,   10,
         10,   10,   10,   10,   89,   91,   10,   92,   93,   10,
         10,   10,   94,   95,   10,   96,   10,   98,   99,   10,
        100,   10,  101,   10,  103,  104,   10,   10,   10,   10,
         10,   88,   88,   88,   90,   90,   14,   14,   15,   15,
         38,   38,   39,   39,   39,   39,  105,   43,   28,   28,
         28,   13,   13,   13,   13,   13,   13,  106,   42,  107,
         42,   16,   51,   51,   51,   58,   58,   52,   52,   17,
         17,   57,   57,   18,   18,    3,    3,    3,    2,    2,
          2,    2,   64,   63,   63,   63,   63,    4,    4,   62,
         62,   62,   62,   62,   71,   24,   24,   24,   24,   24,
         24,   24,   37,   37,   27,  108,   27,   27,   30,   30,
         31,   31,   31,   31,   31,   31,   31,   31,   31,   73,
         73,   73,   73,   73,   74,   74,   32,   55,   55,   72,
         72,   33,   34,   34,    1,  109,    1,   35,   35,   35,
         54,   54,   53,   65,   65,   65,   66,   66,   66,   66,
         67,   67,   67,  102,  102,   76,   76,   83,   83,   85,
         85,   85,   97,   97,   77,   77,   41,
    };
  } /* End of class YyLhsClass */

  protected static final class YyLenClass {

    public static final short yyLen [] = {           2,
          0,    2,    2,    1,    1,    3,    2,    0,    4,    3,
          3,    3,    2,    3,    3,    3,    3,    3,    0,    5,
          4,    3,    3,    3,    1,    3,    2,    1,    3,    3,
          2,    2,    1,    1,    1,    1,    4,    4,    2,    4,
          4,    2,    2,    1,    3,    1,    3,    1,    2,    3,
          2,    2,    1,    1,    3,    2,    3,    1,    4,    3,
          3,    3,    1,    1,    4,    3,    3,    3,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    0,
          4,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    3,    0,
          4,    6,    5,    5,    5,    3,    3,    3,    3,    3,
          3,    3,    3,    3,    2,    2,    3,    3,    3,    3,
          3,    3,    3,    3,    3,    3,    3,    3,    3,    2,
          2,    3,    3,    3,    3,    0,    4,    5,    1,    0,
          2,    4,    2,    5,    2,    3,    3,    4,    4,    6,
          1,    1,    1,    3,    2,    5,    2,    5,    4,    7,
          3,    1,    0,    2,    2,    2,    0,    1,    3,    1,
          1,    3,    4,    2,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    6,    3,    3,    2,    4,    3,
          3,    4,    3,    1,    4,    3,    1,    0,    6,    2,
          1,    2,    6,    6,    0,    0,    7,    0,    0,    7,
          6,    5,    0,    0,    9,    0,    6,    0,    0,    8,
          0,    5,    0,    9,    0,    0,   12,    1,    1,    1,
          1,    1,    1,    2,    1,    1,    1,    5,    1,    2,
          1,    1,    1,    2,    1,    3,    0,    5,    2,    4,
          4,    2,    4,    4,    3,    2,    1,    0,    5,    0,
          5,    4,    1,    4,    2,    2,    1,    0,    1,    2,
          1,    6,    0,    1,    2,    1,    1,    1,    2,    2,
          1,    1,    2,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    0,    4,    2,    4,    2,
          6,    4,    4,    2,    4,    2,    2,    1,    0,    1,
          1,    1,    1,    1,    1,    3,    3,    1,    3,    2,
          1,    2,    2,    0,    1,    0,    5,    1,    2,    2,
          1,    3,    3,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    0,    1,    0,    1,    0,
          1,    1,    1,    1,    1,    2,    0,
    };
  } /* End class YyLenClass */

  protected static final class YyDefRedClass {

    public static final short yyDefRed [] = {            1,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  255,  258,    0,  278,  279,  280,  281,    0,    0,
          0,  348,  347,  349,  350,    0,    0,    0,   19,    0,
        352,  351,    0,    0,  345,  341,    0,  344,  338,  339,
        331,  229,  328,  230,  231,  354,  353,  332,  228,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        226,  326,    2,    0,    0,    0,    0,    0,    0,   28,
          0,  232,    0,   35,    0,    0,    4,    0,   54,    0,
         44,    0,    0,  327,    0,  342,    0,   70,   71,    0,
          0,  271,  117,  129,  118,  142,  114,  135,  124,  123,
        140,  122,  121,  116,  145,  126,  115,  130,  134,  136,
        128,  120,  137,  147,  139,    0,    0,    0,    0,  113,
        133,  132,  127,  143,  146,  144,  148,  112,  119,  110,
        111,    0,    0,    0,   74,    0,  103,  104,  101,   85,
         86,   87,   90,   92,   88,  105,  106,   93,   94,   98,
         89,   91,   82,   83,   84,   95,   96,   97,   99,  100,
        102,  107,  386,    0,  385,  346,  273,   75,   76,  138,
        131,  141,  125,  108,  109,   72,   73,    0,   79,   78,
         77,  323,    0,    0,    0,    0,  414,  413,    0,    0,
          0,  415,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  291,  292,    0,    0,    0,    0,    0,    0,    0,
          0,    0,  203,   27,  225,    0,    0,  391,    0,  212,
          0,    0,   43,    0,  306,   42,    0,   31,    8,    0,
        409,    0,    0,    0,    0,    0,    0,  238,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  388,    0,
          0,    0,    0,   52,    0,  335,  337,  333,  334,  336,
          0,   32,    0,  329,  330,    3,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  297,  299,  310,  308,  252,    0,    0,
          0,    0,   56,    0,    0,    0,    0,  150,  302,   39,
        250,    0,    0,  356,  266,  355,    0,    0,  405,  404,
        275,    0,   80,    0,  283,    0,    0,    0,    0,    0,
          0,    0,  317,    0,  416,    0,    0,    0,    0,    0,
          0,  263,    0,    0,  243,    0,    0,    0,    0,    0,
          0,  205,    0,  207,  246,    0,    0,    0,    0,    0,
          0,  214,    0,   11,   12,   10,  248,    0,    0,    0,
          0,    0,    0,  236,    0,    0,  191,    0,  411,  193,
          0,  195,  240,  241,    0,  390,  389,    0,    0,    0,
          0,    0,    0,    0,    0,   18,   29,   30,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  305,    0,
          0,  399,    0,    0,  400,    0,    0,    0,    0,  397,
        398,    0,    0,    0,    0,    0,   22,   24,    0,    0,
         23,   26,  221,    0,   50,   57,    0,    0,  358,    0,
          0,    0,    0,    0,    0,  371,  370,  373,    0,    0,
          0,    0,    0,  378,  368,    0,  372,    0,  375,    0,
          0,    0,    0,    0,  289,    0,  284,    0,    0,    0,
          0,    0,    0,    0,    0,  316,  286,  256,  285,  259,
          0,    0,    0,    0,    0,    0,    0,    0,  211,  242,
          0,    0,    0,    0,    0,    0,    0,  204,    0,  216,
          0,    0,  392,  245,    0,    0,    0,    0,  197,    0,
          9,    0,    0,    0,   21,    0,  196,    0,    0,    0,
          0,    0,    0,    0,    0,    0,  304,   41,    0,    0,
        202,  303,   40,  201,    0,  295,    0,    0,  293,    0,
          0,  301,   38,  300,   37,    0,    0,   55,    0,  269,
          0,    0,  272,    0,  276,    0,  380,  382,    0,  323,
        360,    0,  366,    0,  367,    0,  364,   81,    0,    0,
        290,    0,    0,  324,    0,    0,  287,    0,    0,    0,
          0,    0,  262,    0,    0,    0,    0,    0,    0,    0,
        209,    0,  198,    0,    0,  199,    0,    0,    0,   20,
          0,  192,    0,    0,    0,    0,    0,    0,  294,    0,
          0,    0,    0,    0,    0,    0,  357,  267,  387,    0,
          0,    0,    0,    0,  379,  383,    0,    0,    0,  376,
          0,    0,    0,  321,  325,  235,    0,  253,  254,  261,
          0,  312,    0,    0,  264,  206,    0,  208,    0,  249,
        194,    0,  296,  298,  311,  309,    0,    0,    0,  359,
          0,  365,    0,  362,  363,    0,    0,  320,    0,    0,
          0,    0,  257,  260,    0,    0,  200,  270,  323,    0,
          0,    0,    0,    0,  322,    0,    0,  210,    0,  274,
        361,    0,    0,    0,    0,  288,  265,    0,    0,    0,
        277,
    };
  } /* End of class YyDefRedClass */

  protected static final class YyDgotoClass {

    public static final short yyDgoto [] = {             1,
        164,   60,   61,   62,  240,   64,   65,   66,   67,  236,
         69,   70,   71,  606,  607,  343,  663,  603,  214,  215,
        561,  562,  226,   72,  462,  463,  325,   73,   74,  482,
        483,  484,  656,  593,  248,  178,  216,  201,  568,  184,
         77,  321,  305,   78,   79,   80,   81,  242,   82,  245,
        504,  600,  218,  219,  486,  247,  334,  344,  220,  362,
        179,  221,  258,   84,  205,  512,  439,   91,  181,  445,
         86,  488,  489,  490,    2,  190,  191,  373,  233,  169,
        491,  467,  232,  378,  390,  227,  542,  336,  193,  508,
        614,  194,  615,  517,  705,  471,  337,  468,  646,  327,
        332,  331,  474,  650,  447,  449,  448,  470,  328,
    };
  } /* End of class YyDgotoClass */

  protected static final class YySindexClass {

    public static final short yySindex [] = {            0,
          0,11517,11919,  226,   62,14458,14365,11517,12408,12408,
      11347,    0,    0,16112,    0,    0,    0,    0,12013,12116,
          6,    0,    0,    0,    0,12408,14092,   47,    0,  -26,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,13787,
      13787,   62,11722,12999,13787,16298,14731,13890,13787, -214,
          0,    0,    0,  496,  480,  258, 2640,   76, -172,    0,
        -80,    0,  -21,    0, -221,  104,    0,   94,    0,  142,
          0,16205, -169,    0,  132,    0,  480,    0,    0,12408,
        233,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  -10,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,  220,    0,    0,
          0,    0,  228,  215,  267,  228,    0,    0,  214,   60,
        285,    0,12408,12408,  321,  327,    6,   47,   84,    0,
        110,    0,    0,    0,  132,11517,13787,13787,13787,12210,
       1761,   91,    0,    0,    0, -221,  355,    0,  381,    0,
       -169,12314,    0,12511,    0,    0,12511,    0,    0, -206,
          0,  394,  339,11517,  117,   99,  117,    0,11722,  403,
          0,  432,13787,   47,  247,  278,  458,  395,    0,  377,
        278,  108,    0,    0,    0,    0,    0,    0,    0,    0,
        117,    0,  117,    0,    0,    0,11816,12408,12408,12408,
      12408,11919,12408,12408,13787,13787,13787,13787,13787,13787,
      13787,13787,13787,13787,13787,13787,13787,13787,13787,13787,
      13787,13787,13787,13787,13787,13787,13787,13787,13787,13787,
      14859,15162,12999,    0,    0,    0,    0,    0,15199,15199,
      13787,13102,    0,13102,11722,16298,  510,    0,    0,    0,
          0,  258,  496,    0,    0,    0,11517,12408,    0,    0,
          0,  271,    0, -149,    0,11517,  289,13787,13196,11517,
         60,13299,    0,  260,    0,  164,  164,  394,15236,15273,
      12999,    0, 2203, 2640,    0,  520,13787,15310,15347,12999,
      12605,    0,12708,    0,    0,  522, -172,   47,   77,  529,
        530,    0,14365,    0,    0,    0,    0,13787,11517,  460,
      15310,15347,  551,    0,    0, 7900,    0,13393,    0,    0,
      13787,    0,    0,    0,13787,    0,    0,15384,15687,12999,
        480,  258,  258,  258,  258,    0,    0,    0,  117, 4722,
       4722, 4722, 4722,  773,  773, 4386, 3514, 4722, 4722, 3077,
       3077,  184,  184, 3951,  773,  773,  119,  119,  632,   59,
         59,  117,  117,  117,  257,    0,    0,    6,    0,    0,
        275,    0,  297,    6,    0,  527,  -97,  -97,  -97,    0,
          0,    6,    6, 2640,13787, 2640,    0,    0,  583, 2640,
          0,    0,    0,  604,    0,    0,13787,  496,    0,12408,
      11517,  385,    9,14822,  593,    0,    0,    0,  351,  353,
        761,11517,  496,    0,    0,  614,    0,  615,    0,  617,
      14365,13787,11517,  399,    0,  -75,    0, 2640,  398,  260,
      13787, 2640,  626,   48,  408,    0,    0,    0,    0,    0,
          0,    6,    0,    0,    6,  585,12408,  334,    0,    0,
       2640,  257,  275,  297,  597,13787, 1761,    0,  648,    0,
      13787, 1761,    0,    0,  655,15199,15199,  656,    0,12605,
          0,12408, 2640,  575,    0,    0,    0,13787, 2640,   47,
          0,    0,    0,  621,13787,13787,    0,    0,13787,13787,
          0,    0,    0,    0,  365,    0,16019,11517,    0,11517,
      11517,    0,    0,    0,    0, 2640,13496,    0, 2640,    0,
        214,  444,    0,  681,    0,13787,    0,    0,   47,    0,
          0, -245,    0,  375,    0,  761,    0,    0,  680,  386,
          0,11517,  466,    0,12408,  467,    0,  470,  472, 2640,
      13590,11517,    0,11517,11517,    0,  164,  365, 2203,12802,
          0, 2203,    0,    6,    6,    0, -172,   77,  112,    0,
       7900,    0,    0, 2640, 2640, 2640, 2640,13787,    0,  616,
        479,  481,  625,13787, 2640,11517,    0,    0,    0,  271,
       2640,  705, -149,  593,    0,    0,  615,  708,  615,    0,
      13787,16298,   48,    0,    0,    0,  228,    0,    0,    0,
      13787,    0,  487,  490,    0,    0,13787,    0,  717,    0,
          0, 2640,    0,    0,    0,    0, 2640,  495,11517,    0,
        399,    0, -245,    0,    0,  125,    0,    0,    0,11517,
      11517, 2640,    0,    0,11517, 2203,    0,    0,    0,  501,
        615,15724,15761,12999,    0,  -75,  502,    0, -149,    0,
          0,    0,    0,    0,  675,    0,    0,  399,    0,  506,
          0,
    };
  } /* End of class YySindexClass */

  protected static final class YyRindexClass {

    public static final short yyRindex [] = {            0,
          0,  572,    0,    0,    0,    0,    0,  493,    0,    0,
        507,    0,    0,    0,    0,    0,    0,    0,11016, 6238,
       3384,    0,    0,    0,    0,    0,    0,13693,    0,    0,
          0,    0, 1625, 2605,    0,    0, 1723,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,  573,  686,  657,  157,    0,    0,    0, 5657,
          0,    0,    0,  894, 1472, 1352,11588,11638, 8378,    0,
       5753,    0,15603,    0,10533,    0,    0,    0,    0,    0,
          0,  512,10623,    0,12905,    0, 5708,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  486,  643,  845,  923,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  978,  990,  999,    0, 1073,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0, 4800,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  507,    0,
        509,    0,    0,    0, 6057, 6142, 4785,  743,    0,  106,
          0,    0,    0,  580,    0,  573,    0,    0,    0,    0,
      10834, 6627,    0,    0,    0, 5178, 5876,    0, 5876,    0,
       5268,    0,    0,  744,    0,    0,    0,    0,    0,    0,
          0,13984,    0,  517, 6722, 6542, 7025,    0,  573,    0,
        425,    0,    0,  691,  695,  695,    0,    0,    0,  671,
        671,    0,  589,    0,  781,    0,    0,    0,    0,    0,
       7110,    0, 7205,    0,    0,    0, 5029,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,  686,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,  573,  560,  561,    0,    0,    0,
          0,  536,    0,    0,    0,    0,  317,    0,    0,    0,
          0,  552,    0,  344,    0,  244,10901,    0,    0,   40,
          0,    0,    0,  535,    0,    0,    0,    0,    0,    0,
        686,    0, 5876, 6361,    0,    0,    0,    0,    0,  686,
          0,    0,    0,    0,    0,    0,  236,  767,  767,    0,
        248,    0,    0,    0,    0,    0,    0,    0,  517,    0,
          0,    0,    0,    0,  409,  691,    0,  720,    0,    0,
         55,    0,    0,    0,  689,    0,    0,    0,    0,  686,
       6193, 4934, 5140, 5194, 5768,    0,    0,    0, 7508, 9087,
       9173, 9270, 9352, 8714, 8809, 9443, 9691, 9499, 9596, 9747,
       9787, 8251, 8414,    0, 8895, 8992, 7809, 8595, 8499, 8069,
       8154, 7593, 7688, 7991, 3916, 2947, 3479,12905,    0, 3042,
       4258,    0, 4353, 3821,    0,    0,11109,11109,11227,    0,
          0, 4695, 4695, 1290,    0, 1567,    0,    0,    0, 4896,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        317,    0,  767,    0,  200,    0,    0,    0,  201,    0,
        264,  493,    0,    0,    0,  587,    0,  587,    0,  587,
          0,   12,  442,  535,    0,  535,    0, 9825,  535,  535,
          0,   30,   81,    0,    0,    0,    0,    0,    0,    0,
       1220,    0, 1452, 1478, 5089,    0,    0,    0,    0,    0,
      14553, 2073, 2168, 2510,    0,    0,15842,    0, 5876,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0, 9907,    0,    0,  543,    0,    0,   11,  691,
       1204, 1475, 4746,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,10712,    0,    0,  317,    0,  317,
        517,    0,    0,    0,    0,15914,    0,    0,10091,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  767,    0,
          0,    0,    0,    0,    0,    0,    0,    0,   31,  176,
          0,  317,    0,    0,    0,    0,    0,    0,    0,  204,
          0,  478,    0,  317,  317,  620,    0, 5572, 5876,    0,
          0, 5876,    0,   83,   83,    0,  468,  767,    0,    0,
        691,    0, 1000,10136,10173,10213,10255,    0,    0,    0,
          0,    0,    0,    0,15878,  317,    0,    0,    0,  552,
        547,    0,  344,    0,    0,    0,  587,  587,  587,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,10320,    0,    0,    0,    0,15976,    0,  493,    0,
        535,    0,    0,    0,    0,    0,   68,    0,   69,  493,
        244,  223,    0,    0,  317, 5876,    0,    0,    0,    0,
        587,    0,    0,  686,    0,  535,    0,    0,  344,    0,
          0,  401,  550,  843,    0,    0,    0,  535,  137,    0,
          0,
    };
  } /* End of class YyRindexClass */

  protected static final class YyGindexClass {

    public static final short yyGindex [] = {            0,
          0,    0,    0,    0,  316,    0,   13,  159, 1151,   -2,
        -15,  -28,    0,  103, -252, -250,    0, -652,  197, -108,
        -18,   23,  -38,  815,    0,  519,    0, -201,    0,  182,
        354, -547, -266,  448,    0,    0,  707,  269,  189,  514,
        427,  772,    0,  756,  -14,  988,   22, -176,    0,   29,
          0,    0, -326,   18,  249, -265, -546,  503, -278, -209,
          2,  533,    0,    8,  123, -241,    0,  231,    5,    7,
        -25, -441,  250,    0,    0,  -11,  783,    0,    0,    0,
          0,    0, -167,    0,  -62,    0,    0, -153,    0, -327,
          0,    0,    0,    0,    0,    0,   17,    0,    0,    0,
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


					// line 1578 "DefaultRubyParser.y"

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
					// line 6713 "-"
