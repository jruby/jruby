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

import java.util.*;
import java.io.*;

import org.jruby.scanner.*;
import org.jruby.*;
import org.jruby.nodes.*;
import org.jruby.runtime.*;
import org.jruby.util.*;

public class DefaultRubyParser implements RubyParser {
    private Ruby ruby;
    private ParserHelper ph;
    private NodeFactory nf;
    private DefaultRubyScanner rs;
    
    public DefaultRubyParser(Ruby ruby) {
        this.ruby = ruby;
        this.ph = ruby.getParserHelper();
        this.nf = new NodeFactory(ruby);
        this.rs = new DefaultRubyScanner(ruby);
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
					// line 62 "-"
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
  public static final int tDSTRING = 315;
  public static final int tDXSTRING = 316;
  public static final int tDREGEXP = 317;
  public static final int tNTH_REF = 318;
  public static final int tBACK_REF = 319;
  public static final int tUPLUS = 320;
  public static final int tUMINUS = 321;
  public static final int tPOW = 322;
  public static final int tCMP = 323;
  public static final int tEQ = 324;
  public static final int tEQQ = 325;
  public static final int tNEQ = 326;
  public static final int tGEQ = 327;
  public static final int tLEQ = 328;
  public static final int tANDOP = 329;
  public static final int tOROP = 330;
  public static final int tMATCH = 331;
  public static final int tNMATCH = 332;
  public static final int tDOT2 = 333;
  public static final int tDOT3 = 334;
  public static final int tAREF = 335;
  public static final int tASET = 336;
  public static final int tLSHFT = 337;
  public static final int tRSHFT = 338;
  public static final int tCOLON2 = 339;
  public static final int tCOLON3 = 340;
  public static final int tOP_ASGN = 341;
  public static final int tASSOC = 342;
  public static final int tLPAREN = 343;
  public static final int tLBRACK = 344;
  public static final int tLBRACE = 345;
  public static final int tSTAR = 346;
  public static final int tAMPER = 347;
  public static final int tSYMBEG = 348;
  public static final int LAST_TOKEN = 349;
  public static final int yyErrorCode = 256;

  /** thrown for irrecoverable syntax errors and stack overflow.
    */
  public static class yyException extends java.lang.Exception {
    public yyException (String message) {
      super(message);
    }
  }

  /** must be implemented by a scanner object to supply input to the parser.
    */
  public interface yyInput {
    /** move on to next token.
        @return false if positioned beyond tokens.
        @throws IOException on input error.
      */
    boolean advance () throws java.io.IOException;
    /** classifies current token.
        Should not be called if advance() returned false.
        @return current %token or single character.
      */
    int token ();
    /** associated with current token.
        Should not be called if advance() returned false.
        @return value for token().
      */
    Object value ();
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
  public void yyerror (String message, String[] expected) {
    System.err.print(rs.getSource().getSource() + " [" + rs.getSource().getLine() + ',' + rs.getSource().getColumn() + "] :");
    if (expected != null && expected.length > 0) {
      System.err.print(message + ", expecting");
      for (int n = 0; n < expected.length; ++ n)
        System.err.print(" "+expected[n]);
      System.err.println();
    } else
      System.err.println(message);
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
      @param yyLex scanner.
      @param yydebug debug message writer implementing yyDebug, or null.
      @return result of the last reduction, if any.
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (yyInput yyLex, Object yydebug)
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
      @param yyLex scanner.
      @return result of the last reduction, if any.
      @throws yyException on irrecoverable parse error.
    */
  public Object yyparse (yyInput yyLex)
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
              yyerror("syntax error", yyExpecting(yyState));
  
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
					// line 180 "DefaultRubyParser.y"
  {
                  yyVal = ruby.getDynamicVars();
                  ph.setLexState(LexState.EXPR_BEG);
                  ph.top_local_init();
                  if (ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {
                      ph.setClassNest(0);
                  } else {
                      ph.setClassNest(1);
                  }
              }
  break;
case 2:
					// line 198 "DefaultRubyParser.y"
  {
                  if (((Node)yyVals[0+yyTop]) != null && !ph.isCompileForEval()) {
                      /* last expression should not be void */
                      if (((Node)yyVals[0+yyTop]).getType() != Constants.NODE_BLOCK) {
                          ph.void_expr(((Node)yyVals[0+yyTop]));
                      } else {
                          Node node = ((Node)yyVals[0+yyTop]);
                          while (node.getNextNode() != null) {
                              node = node.getNextNode();
                          }
                          ph.void_expr(node.getHeadNode());
                      }
                  }
                  ph.setEvalTree(ph.block_append(ph.getEvalTree(), ((Node)yyVals[0+yyTop])));
                  ph.top_local_setup();
                  ph.setClassNest(0);
                  ruby.setDynamicVars(((RubyVarmap)yyVals[-1+yyTop]));
              }
  break;
case 3:
					// line 217 "DefaultRubyParser.y"
  {
                  ph.void_stmts(((Node)yyVals[-1+yyTop]));
                  yyVal = ((Node)yyVals[-1+yyTop]);
              }
  break;
case 5:
					// line 223 "DefaultRubyParser.y"
  {
                    yyVal = ph.newline_node(((Node)yyVals[0+yyTop]));
                }
  break;
case 6:
					// line 226 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(((Node)yyVals[-2+yyTop]), ph.newline_node(((Node)yyVals[0+yyTop])));
                }
  break;
case 7:
					// line 229 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 8:
					// line 233 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_FNAME);
                }
  break;
case 9:
					// line 235 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("alias within method");
                    }
                    yyVal = nf.newAlias(((String)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 10:
					// line 241 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("alias within method");
                    }
                    yyVal = nf.newVAlias(((String)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 11:
					// line 247 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("alias within method");
                    }
                    yyVal = nf.newVAlias(((String)yyVals[-1+yyTop]), "$" + (char)((Node)yyVals[0+yyTop]).getNth());
                }
  break;
case 12:
					// line 253 "DefaultRubyParser.y"
  {
                    yyerror("can't make alias for the number variables");
                    yyVal = null; /*XXX 0*/
                }
  break;
case 13:
					// line 257 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("undef within method");
                    }
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 14:
					// line 263 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newIf(ph.cond(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null).setPosFrom(((Node)yyVals[0+yyTop]));
                }
  break;
case 15:
					// line 267 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newUnless(ph.cond(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]), null).setPosFrom(((Node)yyVals[0+yyTop]));
                }
  break;
case 16:
					// line 271 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = nf.newWhile(ph.cond(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]).getBodyNode()); /* , 0*/
                    } else {
                        yyVal = nf.newWhile(ph.cond(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop])); /* , 1*/
                    }
                }
  break;
case 17:
					// line 279 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    if (((Node)yyVals[-2+yyTop]) != null && ((Node)yyVals[-2+yyTop]) instanceof BeginNode) {
                        yyVal = nf.newUntil(ph.cond(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop]).getBodyNode()); /* , 0*/
                    } else {
                        yyVal = nf.newUntil(ph.cond(((Node)yyVals[0+yyTop])), ((Node)yyVals[-2+yyTop])); /* , 1*/
                    }
                }
  break;
case 18:
					// line 288 "DefaultRubyParser.y"
  {
                    yyVal = nf.newRescue(((Node)yyVals[-2+yyTop]), nf.newResBody(null,((Node)yyVals[0+yyTop]),null), null);
                }
  break;
case 19:
					// line 292 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("BEGIN in method");
                    }
                    ph.local_push();
                }
  break;
case 20:
					// line 297 "DefaultRubyParser.y"
  {
                    ph.setEvalTreeBegin(ph.block_append(ph.getEvalTree(), nf.newPreExe(((Node)yyVals[-1+yyTop]))));
                    ph.local_pop();
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 21:
					// line 302 "DefaultRubyParser.y"
  {
                    if (ph.isCompileForEval() && (ph.isInDef() 
                                              || ph.isInSingle())) {
                        yyerror("END in method; use at_exit");
                    }
                    yyVal = nf.newIter(null, nf.newPostExe(), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 22:
					// line 309 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ph.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 23:
					// line 313 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    ((Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 24:
					// line 318 "DefaultRubyParser.y"
  {
                    yyVal = ph.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 26:
					// line 323 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    ((Node)yyVals[-2+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 27:
					// line 328 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && !ph.isInDef()
                                               && !ph.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = nf.newReturn(((Node)yyVals[0+yyTop]));
                }
  break;
case 29:
					// line 336 "DefaultRubyParser.y"
  {
                    yyVal = ph.logop(Constants.NODE_AND, ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 30:
					// line 339 "DefaultRubyParser.y"
  {
                    yyVal = ph.logop(Constants.NODE_OR, ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 31:
					// line 342 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newNot(ph.cond(((Node)yyVals[0+yyTop])));
                }
  break;
case 32:
					// line 346 "DefaultRubyParser.y"
  {
                    yyVal = nf.newNot(ph.cond(((Node)yyVals[0+yyTop])));
                }
  break;
case 37:
					// line 355 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 38:
					// line 359 "DefaultRubyParser.y"
  {
	            ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 39:
					// line 364 "DefaultRubyParser.y"
  {
                    yyVal = ph.new_fcall(((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[0+yyTop]));
                }
  break;
case 40:
					// line 367 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-3+yyTop]));
                }
  break;
case 41:
					// line 371 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-3+yyTop]));
                }
  break;
case 42:
					// line 375 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && ph.isInDef() 
                                               && ph.isInSingle()){
                        yyerror("super called outside of method");
                    }
		    yyVal = ph.new_super(((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[0+yyTop]));
		}
  break;
case 43:
					// line 382 "DefaultRubyParser.y"
  {
	            yyVal = nf.newYield(((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[0+yyTop]));
		}
  break;
case 45:
					// line 387 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
		}
  break;
case 47:
					// line 392 "DefaultRubyParser.y"
  {
	            yyVal = nf.newMAsgn(nf.newList(((Node)yyVals[-1+yyTop])), null);
                }
  break;
case 48:
					// line 396 "DefaultRubyParser.y"
  {
                    yyVal = nf.newMAsgn(((Node)yyVals[0+yyTop]), null);
                }
  break;
case 49:
					// line 399 "DefaultRubyParser.y"
  {
                    yyVal = nf.newMAsgn(ph.list_append(((Node)yyVals[-1+yyTop]),((Node)yyVals[0+yyTop])), null);
                }
  break;
case 50:
					// line 402 "DefaultRubyParser.y"
  {
                    yyVal = nf.newMAsgn(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 51:
					// line 405 "DefaultRubyParser.y"
  {
                    yyVal = nf.newMAsgn(((Node)yyVals[-1+yyTop]), Node.MINUS_ONE);
                }
  break;
case 52:
					// line 408 "DefaultRubyParser.y"
  {
                    yyVal = nf.newMAsgn(null, ((Node)yyVals[0+yyTop]));
                }
  break;
case 53:
					// line 411 "DefaultRubyParser.y"
  {
                    yyVal = nf.newMAsgn(null, Node.MINUS_ONE);
                }
  break;
case 55:
					// line 416 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 56:
					// line 420 "DefaultRubyParser.y"
  {
                    yyVal = nf.newList(((Node)yyVals[-1+yyTop]));
                }
  break;
case 57:
					// line 423 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 58:
					// line 427 "DefaultRubyParser.y"
  {
                    yyVal = ph.getAssignmentNode(((String)yyVals[0+yyTop]), null);
                }
  break;
case 59:
					// line 430 "DefaultRubyParser.y"
  {
                    yyVal = ph.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 60:
					// line 433 "DefaultRubyParser.y"
  {
                    yyVal = ph.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 61:
					// line 436 "DefaultRubyParser.y"
  {
                    yyVal = ph.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 62:
					// line 439 "DefaultRubyParser.y"
  {
                    yyVal = ph.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 63:
					// line 442 "DefaultRubyParser.y"
  {
                    ph.rb_backref_error(((Node)yyVals[0+yyTop]));
                    yyVal = null; /*XXX 0;*/
                }
  break;
case 64:
					// line 447 "DefaultRubyParser.y"
  {
                    yyVal = ph.getAssignmentNode(((String)yyVals[0+yyTop]), null);
                }
  break;
case 65:
					// line 450 "DefaultRubyParser.y"
  {
                    yyVal = ph.aryset(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 66:
					// line 453 "DefaultRubyParser.y"
  {
                    yyVal = ph.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 67:
					// line 456 "DefaultRubyParser.y"
  {
                    yyVal = ph.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 68:
					// line 459 "DefaultRubyParser.y"
  {
                    yyVal = ph.attrset(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 69:
					// line 462 "DefaultRubyParser.y"
  {
                    ph.rb_backref_error(((Node)yyVals[0+yyTop]));
                    yyVal = null; /*XXX 0;*/
		}
  break;
case 70:
					// line 467 "DefaultRubyParser.y"
  {
                    yyerror("class/module name must be CONSTANT");
                }
  break;
case 75:
					// line 475 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 76:
					// line 479 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_END);
                    yyVal = yyVals[0+yyTop];
                }
  break;
case 79:
					// line 487 "DefaultRubyParser.y"
  {
                    yyVal = nf.newUndef(((String)yyVals[0+yyTop]));
                }
  break;
case 80:
					// line 490 "DefaultRubyParser.y"
  {
	            ph.setLexState(LexState.EXPR_FNAME);
		}
  break;
case 81:
					// line 492 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(((Node)yyVals[-3+yyTop]), nf.newUndef(((String)yyVals[0+yyTop])));
                }
  break;
case 82:
					// line 496 "DefaultRubyParser.y"
  { yyVal = "|"; }
  break;
case 83:
					// line 497 "DefaultRubyParser.y"
  { yyVal = "^"; }
  break;
case 84:
					// line 498 "DefaultRubyParser.y"
  { yyVal = "&"; }
  break;
case 85:
					// line 499 "DefaultRubyParser.y"
  { yyVal = "<=>"; }
  break;
case 86:
					// line 500 "DefaultRubyParser.y"
  { yyVal = "=="; }
  break;
case 87:
					// line 501 "DefaultRubyParser.y"
  { yyVal = "==="; }
  break;
case 88:
					// line 502 "DefaultRubyParser.y"
  { yyVal = "=~"; }
  break;
case 89:
					// line 503 "DefaultRubyParser.y"
  { yyVal = ">"; }
  break;
case 90:
					// line 504 "DefaultRubyParser.y"
  { yyVal = ">="; }
  break;
case 91:
					// line 505 "DefaultRubyParser.y"
  { yyVal = "<"; }
  break;
case 92:
					// line 506 "DefaultRubyParser.y"
  { yyVal = "<="; }
  break;
case 93:
					// line 507 "DefaultRubyParser.y"
  { yyVal = "<<"; }
  break;
case 94:
					// line 508 "DefaultRubyParser.y"
  { yyVal = ">>"; }
  break;
case 95:
					// line 509 "DefaultRubyParser.y"
  { yyVal = "+"; }
  break;
case 96:
					// line 510 "DefaultRubyParser.y"
  { yyVal = "-"; }
  break;
case 97:
					// line 511 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 98:
					// line 512 "DefaultRubyParser.y"
  { yyVal = "*"; }
  break;
case 99:
					// line 513 "DefaultRubyParser.y"
  { yyVal = "/"; }
  break;
case 100:
					// line 514 "DefaultRubyParser.y"
  { yyVal = "%"; }
  break;
case 101:
					// line 515 "DefaultRubyParser.y"
  { yyVal = "**"; }
  break;
case 102:
					// line 516 "DefaultRubyParser.y"
  { yyVal = "~"; }
  break;
case 103:
					// line 517 "DefaultRubyParser.y"
  { yyVal = "+@"; }
  break;
case 104:
					// line 518 "DefaultRubyParser.y"
  { yyVal = "-@"; }
  break;
case 105:
					// line 519 "DefaultRubyParser.y"
  { yyVal = "[]"; }
  break;
case 106:
					// line 520 "DefaultRubyParser.y"
  { yyVal = "[]="; }
  break;
case 107:
					// line 521 "DefaultRubyParser.y"
  { yyVal = "`"; }
  break;
case 149:
					// line 565 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ph.node_assign(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 150:
					// line 569 "DefaultRubyParser.y"
  {
                    yyVal = ph.getAssignmentNode(((String)yyVals[-1+yyTop]), null);
                }
  break;
case 151:
					// line 571 "DefaultRubyParser.y"
  {
                    if (((String)yyVals[-2+yyTop]).equals("||")) {
		        ((Node)yyVals[-1+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
			yyVal = nf.newOpAsgnOr(ph.getAccessNode(((String)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
                        if (IdUtil.isInstanceVariable(((String)yyVals[-3+yyTop]))) {
                            ((Node)yyVal).setAId(((String)yyVals[-3+yyTop]));
                        }
                    } else if (((String)yyVals[-2+yyTop]).equals("&&")) {
                        ((Node)yyVals[-1+yyTop]).setValueNode(((Node)yyVals[0+yyTop]));
                        yyVal = nf.newOpAsgnAnd(ph.getAccessNode(((String)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]));
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                        if (yyVal != null) {
                            ((Node)yyVal).setValueNode(ph.call_op(ph.getAccessNode(((String)yyVals[-3+yyTop])),((String)yyVals[-2+yyTop]),1,((Node)yyVals[0+yyTop])));
                        }
                    }
                    ((Node)yyVal).setPosFrom(((Node)yyVals[0+yyTop]));
                }
  break;
case 152:
					// line 589 "DefaultRubyParser.y"
  {
                    ArrayNode args = nf.newList(((Node)yyVals[0+yyTop]));

                    ph.list_append(((Node)yyVals[-3+yyTop]), nf.newNil());
                    ph.list_concat(args, ((Node)yyVals[-3+yyTop]));
                    yyVal = nf.newOpAsgn1(((Node)yyVals[-5+yyTop]), ((String)yyVals[-1+yyTop]), args).setPosFrom(((Node)yyVals[-5+yyTop]));
                }
  break;
case 153:
					// line 596 "DefaultRubyParser.y"
  {
                    yyVal = nf.newOpAsgn2(((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 154:
					// line 599 "DefaultRubyParser.y"
  {
                    yyVal = nf.newOpAsgn2(((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 155:
					// line 602 "DefaultRubyParser.y"
  {
                    yyVal = nf.newOpAsgn2(((Node)yyVals[-4+yyTop]), ((String)yyVals[-2+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 156:
					// line 605 "DefaultRubyParser.y"
  {
                    ph.rb_backref_error(((Node)yyVals[-2+yyTop]));
                    yyVal = null; /*XXX 0*/
                }
  break;
case 157:
					// line 609 "DefaultRubyParser.y"
  {
                    yyVal = nf.newDot2(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 158:
					// line 612 "DefaultRubyParser.y"
  {
                    yyVal = nf.newDot3(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 159:
					// line 615 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '+', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 160:
					// line 618 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '-', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 161:
					// line 621 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '*', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 162:
					// line 624 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '/', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 163:
					// line 627 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '%', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 164:
					// line 630 "DefaultRubyParser.y"
  {
                    boolean need_negate = false;

                    if (((Node)yyVals[-2+yyTop]) instanceof LitNode) {
                        if (((Node)yyVals[-2+yyTop]).getLiteral() instanceof RubyFixnum
                                  || ((Node)yyVals[-2+yyTop]).getLiteral() instanceof RubyFloat
                                  || ((Node)yyVals[-2+yyTop]).getLiteral() instanceof RubyBignum) {
                            if (((Node)yyVals[-2+yyTop]).getLiteral().funcall("<", RubyFixnum.zero(ruby)).isTrue()) {
                                ((Node)yyVals[-2+yyTop]).setLiteral(((Node)yyVals[-2+yyTop]).getLiteral().funcall("-@"));
                                need_negate = true;
                            }
                        }
                    }
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tPOW, 1, ((Node)yyVals[0+yyTop]));
                    if (need_negate) {
                        yyVal = ph.call_op(((Node)yyVal), tUMINUS, 0, null);
                    }
                }
  break;
case 165:
					// line 648 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof LitNode) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = ph.call_op(((Node)yyVals[0+yyTop]), tUPLUS, 0, null);
                    }
                }
  break;
case 166:
					// line 655 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null && ((Node)yyVals[0+yyTop]) instanceof LitNode 
		                   && ((Node)yyVals[0+yyTop]).getLiteral() instanceof RubyFixnum) {
                        long i = RubyNumeric.num2long(((Node)yyVals[0+yyTop]).getLiteral());
                        ((Node)yyVals[0+yyTop]).setLiteral(RubyFixnum.newFixnum(ruby, -i));
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = ph.call_op(((Node)yyVals[0+yyTop]), tUMINUS, 0, null);
                    }
                }
  break;
case 167:
					// line 665 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '|', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 168:
					// line 668 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '^', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 169:
					// line 671 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '&', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 170:
					// line 674 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tCMP, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 171:
					// line 677 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '>', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 172:
					// line 680 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tGEQ, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 173:
					// line 683 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), '<', 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 174:
					// line 686 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tLEQ, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 175:
					// line 689 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tEQ, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 176:
					// line 692 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tEQQ, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 177:
					// line 695 "DefaultRubyParser.y"
  {
                    yyVal = nf.newNot(ph.call_op(((Node)yyVals[-2+yyTop]), tEQ, 1, ((Node)yyVals[0+yyTop])));
                }
  break;
case 178:
					// line 698 "DefaultRubyParser.y"
  {
                    yyVal = ph.match_gen(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 179:
					// line 701 "DefaultRubyParser.y"
  {
                    yyVal = nf.newNot(ph.match_gen(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])));
                }
  break;
case 180:
					// line 704 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newNot(ph.cond(((Node)yyVals[0+yyTop])));
                }
  break;
case 181:
					// line 708 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[0+yyTop]), '~', 0, null);
                }
  break;
case 182:
					// line 711 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tLSHFT, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 183:
					// line 714 "DefaultRubyParser.y"
  {
                    yyVal = ph.call_op(((Node)yyVals[-2+yyTop]), tRSHFT, 1, ((Node)yyVals[0+yyTop]));
                }
  break;
case 184:
					// line 717 "DefaultRubyParser.y"
  {
                    yyVal = ph.logop(Constants.NODE_AND, ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 185:
					// line 720 "DefaultRubyParser.y"
  {
                    yyVal = ph.logop(Constants.NODE_OR, ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 186:
					// line 723 "DefaultRubyParser.y"
  {
	            ph.setInDefined(true);
		}
  break;
case 187:
					// line 725 "DefaultRubyParser.y"
  {
                    ph.setInDefined(false);
                    yyVal = nf.newDefined(((Node)yyVals[0+yyTop]));
                }
  break;
case 188:
					// line 729 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-4+yyTop]));
                    yyVal = nf.newIf(ph.cond(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 189:
					// line 734 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 191:
					// line 739 "DefaultRubyParser.y"
  {
                    yyVal = nf.newList(((Node)yyVals[-1+yyTop]));
                }
  break;
case 192:
					// line 742 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_append(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 193:
					// line 745 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 194:
					// line 748 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_concat(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                }
  break;
case 195:
					// line 752 "DefaultRubyParser.y"
  {
                    yyVal = nf.newList(nf.newHash(((Node)yyVals[-1+yyTop])));
                }
  break;
case 196:
					// line 755 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = nf.newRestArgs(((Node)yyVals[-1+yyTop]));
                }
  break;
case 197:
					// line 760 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 198:
					// line 763 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 199:
					// line 766 "DefaultRubyParser.y"
  {
                    yyVal = nf.newList(((Node)yyVals[-2+yyTop]));
                }
  break;
case 200:
					// line 769 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_append(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]));
                }
  break;
case 203:
					// line 776 "DefaultRubyParser.y"
  {
                    yyVal = nf.newList(((Node)yyVals[0+yyTop]));
                }
  break;
case 204:
					// line 779 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 205:
					// line 782 "DefaultRubyParser.y"
  {
                    yyVal = ph.arg_blk_pass(((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 206:
					// line 785 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_concat(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_blk_pass(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 207:
					// line 790 "DefaultRubyParser.y"
  {
                    yyVal = nf.newList(nf.newHash(((Node)yyVals[-1+yyTop])));
                    yyVal = ph.arg_blk_pass(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 208:
					// line 794 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_concat(nf.newList(nf.newHash(((Node)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_blk_pass(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 209:
					// line 799 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_append(((Node)yyVals[-3+yyTop]), nf.newHash(((Node)yyVals[-1+yyTop])));
                    yyVal = ph.arg_blk_pass(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 210:
					// line 803 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_concat(ph.list_append(((Node)yyVals[-6+yyTop]), nf.newHash(((Node)yyVals[-4+yyTop]))), ((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_blk_pass(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 211:
					// line 808 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = ph.arg_blk_pass(nf.newRestArgs(((Node)yyVals[-1+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 213:
					// line 814 "DefaultRubyParser.y"
  { 
                    rs.CMDARG_PUSH();
		}
  break;
case 214:
					// line 816 "DefaultRubyParser.y"
  {
                    rs.CMDARG_POP();
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 215:
					// line 821 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newBlockPass(((Node)yyVals[0+yyTop]));
                }
  break;
case 216:
					// line 826 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 218:
					// line 831 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newList(((Node)yyVals[0+yyTop]));
                }
  break;
case 219:
					// line 835 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ph.list_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 220:
					// line 840 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 222:
					// line 846 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ph.list_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 223:
					// line 850 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ph.arg_concat(((Node)yyVals[-3+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 224:
					// line 854 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 225:
					// line 859 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                    if (((Node)yyVals[0+yyTop]) != null) {
                        if (((Node)yyVals[0+yyTop]).getType() == Constants.NODE_ARRAY
			                  && ((Node)yyVals[0+yyTop]).getNextNode() == null) {
                            yyVal = ((Node)yyVals[0+yyTop]).getHeadNode();
                        } else if (((Node)yyVals[0+yyTop]).getType() == Constants.NODE_BLOCK_PASS) {
                            ph.rb_compile_error("block argument should not be given");
                        }
                    }
                }
  break;
case 226:
					// line 871 "DefaultRubyParser.y"
  {
                    yyVal = nf.newLit(((RubyObject)yyVals[0+yyTop]));
                }
  break;
case 228:
					// line 875 "DefaultRubyParser.y"
  {
                    yyVal = nf.newXStr(((RubyObject)yyVals[0+yyTop]));
                }
  break;
case 233:
					// line 882 "DefaultRubyParser.y"
  {
                    yyVal = nf.newVCall(((String)yyVals[0+yyTop]));
                }
  break;
case 234:
					// line 885 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-3+yyTop]) == null && ((Node)yyVals[-2+yyTop]) == null && ((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = nf.newBegin(((Node)yyVals[-4+yyTop]));
                    } else {
                        if (((Node)yyVals[-3+yyTop]) != null) {
                            yyVals[-4+yyTop] = nf.newRescue(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-2+yyTop]));
                        } else if (((Node)yyVals[-2+yyTop]) != null) {
                            ph.rb_warn("else without rescue is useless");
                            yyVals[-4+yyTop] = ph.block_append(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]));
                        }
                        if (((Node)yyVals[-1+yyTop]) != null) {
                            yyVals[-4+yyTop] = nf.newEnsure(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                        }
                        yyVal = ((Node)yyVals[-4+yyTop]);
                    }
                    ((Node)yyVal).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 235:
					// line 902 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 236:
					// line 905 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-2+yyTop]));
                    yyVal = nf.newColon2(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]));
                }
  break;
case 237:
					// line 909 "DefaultRubyParser.y"
  {
                    yyVal = nf.newColon3(((String)yyVals[0+yyTop]));
                }
  break;
case 238:
					// line 912 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = nf.newCall(((Node)yyVals[-3+yyTop]), "[]", ((Node)yyVals[-1+yyTop]));
                }
  break;
case 239:
					// line 916 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) == null) {
                        yyVal = nf.newZArray(); /* zero length array*/
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                }
  break;
case 240:
					// line 923 "DefaultRubyParser.y"
  {
                    yyVal = nf.newHash(((Node)yyVals[-1+yyTop]));
                }
  break;
case 241:
					// line 926 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && !ph.isInDef() 
                                               && !ph.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = nf.newReturn(((Node)yyVals[-1+yyTop]));
                }
  break;
case 242:
					// line 934 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && !ph.isInDef()
                                               && !ph.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = nf.newReturn(null);
                }
  break;
case 243:
					// line 941 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && !ph.isInDef()
                                               && !ph.isInSingle()) {
                        yyerror("return appeared outside of method");
                    }
                    yyVal = nf.newReturn(null);
                }
  break;
case 244:
					// line 948 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-1+yyTop]));
                    yyVal = nf.newYield(((Node)yyVals[-1+yyTop]));
                }
  break;
case 245:
					// line 952 "DefaultRubyParser.y"
  {
                    yyVal = nf.newYield(null);
                }
  break;
case 246:
					// line 955 "DefaultRubyParser.y"
  {
                    yyVal = nf.newYield(null);
                }
  break;
case 247:
					// line 958 "DefaultRubyParser.y"
  {
	            ph.setInDefined(true);
		}
  break;
case 248:
					// line 960 "DefaultRubyParser.y"
  {
                    ph.setInDefined(false);
                    yyVal = nf.newDefined(((Node)yyVals[-1+yyTop]));
                }
  break;
case 249:
					// line 964 "DefaultRubyParser.y"
  {
                    ((Node)yyVals[0+yyTop]).setIterNode(nf.newFCall(((String)yyVals[-1+yyTop]), null));
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 251:
					// line 969 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) != null && 
		                    ((Node)yyVals[-1+yyTop]).getType() == Constants.NODE_BLOCK_PASS) {
                       ph.rb_compile_error("both block arg and actual block given");
                    }
                    ((Node)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    ((Node)yyVal).setPosFrom(((Node)yyVals[-1+yyTop]));
                }
  break;
case 252:
					// line 978 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-4+yyTop]));
                    yyVal = nf.newIf(ph.cond(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 253:
					// line 982 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-4+yyTop]));
                    yyVal = nf.newUnless(ph.cond(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-2+yyTop]), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 254:
					// line 986 "DefaultRubyParser.y"
  { 
	            rs.COND_PUSH();
		}
  break;
case 255:
					// line 988 "DefaultRubyParser.y"
  {
		    rs.COND_POP();
		}
  break;
case 256:
					// line 990 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-4+yyTop]));
                    yyVal = nf.newWhile(ph.cond(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 257:
					// line 994 "DefaultRubyParser.y"
  {
                    rs.COND_PUSH();
                }
  break;
case 258:
					// line 996 "DefaultRubyParser.y"
  {
                    rs.COND_POP();
                }
  break;
case 259:
					// line 998 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-4+yyTop]));
                    yyVal = nf.newUntil(ph.cond(((Node)yyVals[-4+yyTop])), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]));
                }
  break;
case 260:
					// line 1002 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = nf.newCase(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-3+yyTop]));
                }
  break;
case 261:
					// line 1006 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 262:
					// line 1009 "DefaultRubyParser.y"
  {
                    rs.COND_PUSH();
                }
  break;
case 263:
					// line 1011 "DefaultRubyParser.y"
  {
                    rs.COND_POP();
                }
  break;
case 264:
					// line 1013 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-4+yyTop]));
                    yyVal = nf.newFor(((Node)yyVals[-7+yyTop]), ((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-7+yyTop]));
                }
  break;
case 265:
					// line 1017 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("class definition in method body");
                    }
                    ph.setClassNest(ph.getClassNest() + 1);
                    ph.local_push();
                    yyVal = new Integer(ruby.getSourceLine());
                }
  break;
case 266:
					// line 1024 "DefaultRubyParser.y"
  {
                    yyVal = nf.newClass(((String)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[-3+yyTop]));
                    ((Node)yyVal).setLine(((Integer)yyVals[-2+yyTop]).intValue());
                    ph.local_pop();
                    ph.setClassNest(ph.getClassNest() - 1);
                }
  break;
case 267:
					// line 1030 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(ph.getInDef());
                    ph.setInDef(0);
                }
  break;
case 268:
					// line 1033 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(ph.getInSingle());
                    ph.setInSingle(0);
                    ph.setClassNest(ph.getClassNest() - 1);
                    ph.local_push();
                }
  break;
case 269:
					// line 1038 "DefaultRubyParser.y"
  {
                    yyVal = nf.newSClass(((Node)yyVals[-5+yyTop]), ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-5+yyTop]));
                    ph.local_pop();
                    ph.setClassNest(ph.getClassNest() - 1);
                    ph.setInDef(((Integer)yyVals[-4+yyTop]).intValue());
                    ph.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
                }
  break;
case 270:
					// line 1045 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) { 
                        yyerror("module definition in method body");
                    }
                    ph.setClassNest(ph.getClassNest() + 1);
                    ph.local_push();
                    yyVal = new Integer(ruby.getSourceLine());
                }
  break;
case 271:
					// line 1052 "DefaultRubyParser.y"
  {
                    yyVal = nf.newModule(((String)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                    ((Node)yyVal).setLine(((Integer)yyVals[-2+yyTop]).intValue());
                    ph.local_pop();
                    ph.setClassNest(ph.getClassNest() - 1);
                }
  break;
case 272:
					// line 1058 "DefaultRubyParser.y"
  {
                    if (ph.isInDef() || ph.isInSingle()) {
                        yyerror("nested method definition");
                    }
                    /* $$ = ph.getCurMid(); useless*/
                    ph.setCurMid(((String)yyVals[0+yyTop]));
                    ph.setInDef(ph.getInDef() + 1);
                    ph.local_push();
                }
  break;
case 273:
					// line 1066 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-3+yyTop]) != null) {
                        yyVals[-4+yyTop] = nf.newRescue(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-2+yyTop]));
                    } else if (((Node)yyVals[-2+yyTop]) != null) {
                        ph.rb_warn("else without rescue is useless");
                        yyVals[-4+yyTop] = ph.block_append(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]));
                    }
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        yyVals[-4+yyTop] = nf.newEnsure(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    }

                    /* NOEX_PRIVATE for toplevel */
                    yyVal = nf.newDefn(((String)yyVals[-7+yyTop]), ((Node)yyVals[-5+yyTop]), ((Node)yyVals[-4+yyTop]), ph.getClassNest() !=0 ? 
                                Constants.NOEX_PUBLIC : Constants.NOEX_PRIVATE);
                    if (IdUtil.isAttrSet(((String)yyVals[-7+yyTop]))) {
                        ((Node)yyVal).setNoex(Constants.NOEX_PUBLIC);
                    }
                    ((Node)yyVal).setPosFrom(((Node)yyVals[-5+yyTop]));
                    ph.local_pop();
                    ph.setInDef(ph.getInDef() - 1);
                    /*+++ ph.setCurMid($3);*/
                    ph.setCurMid(((String)yyVals[-7+yyTop]));
                }
  break;
case 274:
					// line 1089 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_FNAME);
                }
  break;
case 275:
					// line 1091 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    ph.setInSingle(ph.getInSingle() + 1);
                    ph.local_push();
                    ph.setLexState(LexState.EXPR_END); /* force for args */
                }
  break;
case 276:
					// line 1096 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-3+yyTop]) != null) {
                        yyVals[-4+yyTop] = nf.newRescue(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-3+yyTop]), ((Node)yyVals[-2+yyTop]));
                    } else if (((Node)yyVals[-2+yyTop]) != null) {
                        ph.rb_warn("else without rescue is useless");
                        yyVals[-4+yyTop] = ph.block_append(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-2+yyTop]));
                    }
                    if (((Node)yyVals[-1+yyTop]) != null) {
                        yyVals[-4+yyTop] = nf.newEnsure(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]));
                    }
                    yyVal = nf.newDefs(((Node)yyVals[-10+yyTop]), ((String)yyVals[-7+yyTop]), ((Node)yyVals[-5+yyTop]), ((Node)yyVals[-4+yyTop]));
                    ((Node)yyVal).setPosFrom(((Node)yyVals[-10+yyTop]));
                    ph.local_pop();
                    ph.setInSingle(ph.getInSingle() - 1);
                }
  break;
case 277:
					// line 1111 "DefaultRubyParser.y"
  {
                    yyVal = nf.newBreak();
                }
  break;
case 278:
					// line 1114 "DefaultRubyParser.y"
  {
                    yyVal = nf.newNext();
                }
  break;
case 279:
					// line 1117 "DefaultRubyParser.y"
  {
                    yyVal = nf.newRedo();
                }
  break;
case 280:
					// line 1120 "DefaultRubyParser.y"
  {
                    yyVal = nf.newRetry();
                }
  break;
case 287:
					// line 1132 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = nf.newIf(ph.cond(((Node)yyVals[-3+yyTop])), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-3+yyTop]));
                }
  break;
case 289:
					// line 1138 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 293:
					// line 1146 "DefaultRubyParser.y"
  {
                    yyVal = Node.ONE; /* new Integer(1); //XXX (Node*)1;*/
                }
  break;
case 294:
					// line 1149 "DefaultRubyParser.y"
  {
                    yyVal = Node.ONE; /* new Integer(1); //XXX (Node*)1;*/
                }
  break;
case 295:
					// line 1152 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 296:
					// line 1156 "DefaultRubyParser.y"
  {
                    yyVal = ph.dyna_push();
                }
  break;
case 297:
					// line 1158 "DefaultRubyParser.y"
  {
                    yyVal = nf.newIter(((Node)yyVals[-2+yyTop]), null, ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-2+yyTop]) != null ? ((Node)yyVals[-2+yyTop]) : ((Node)yyVals[-1+yyTop]));
                    ph.dyna_pop(((RubyVarmap)yyVals[-3+yyTop]));
                }
  break;
case 298:
					// line 1163 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]) != null &&
		                   ((Node)yyVals[-1+yyTop]).getType() == Constants.NODE_BLOCK_PASS) {
                        ph.rb_compile_error("both block arg and actual block given");
                    }
                    ((Node)yyVals[0+yyTop]).setIterNode(((Node)yyVals[-1+yyTop]));
                    yyVal = ((Node)yyVals[0+yyTop]);
                    /* $$$2);*/
                }
  break;
case 299:
					// line 1172 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 300:
					// line 1176 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 301:
					// line 1181 "DefaultRubyParser.y"
  {
                    yyVal = ph.new_fcall(((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[0+yyTop]));
                }
  break;
case 302:
					// line 1184 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-3+yyTop]));
                }
  break;
case 303:
					// line 1188 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-3+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-3+yyTop]), ((String)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-3+yyTop]));
                }
  break;
case 304:
					// line 1192 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[-2+yyTop]));
                    yyVal = ph.new_call(((Node)yyVals[-2+yyTop]), ((String)yyVals[0+yyTop]), null);
                }
  break;
case 305:
					// line 1196 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && !ph.isInDef()
                                    && !ph.isInSingle() && !ph.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    yyVal = ph.new_super(((Node)yyVals[0+yyTop]));
                }
  break;
case 306:
					// line 1203 "DefaultRubyParser.y"
  {
                    if (!ph.isCompileForEval() && !ph.isInDef()
                                    && !ph.isInSingle() && !ph.isInDefined()) {
                        yyerror("super called outside of method");
                    }
                    yyVal = nf.newZSuper();
                }
  break;
case 307:
					// line 1211 "DefaultRubyParser.y"
  {
                    yyVal = ph.dyna_push();
                }
  break;
case 308:
					// line 1213 "DefaultRubyParser.y"
  {
                    yyVal = nf.newIter(((Node)yyVals[-2+yyTop]), null, ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-1+yyTop]));
                    ph.dyna_pop(((RubyVarmap)yyVals[-3+yyTop]));
                }
  break;
case 309:
					// line 1217 "DefaultRubyParser.y"
  {
                    yyVal = ph.dyna_push();
                }
  break;
case 310:
					// line 1219 "DefaultRubyParser.y"
  {
                    yyVal = nf.newIter(((Node)yyVals[-2+yyTop]), null, ((Node)yyVals[-1+yyTop])).setPosFrom(((Node)yyVals[-1+yyTop]));
                    ph.dyna_pop(((RubyVarmap)yyVals[-3+yyTop]));
                }
  break;
case 311:
					// line 1224 "DefaultRubyParser.y"
  {
                    yyVal = nf.newWhen(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 313:
					// line 1229 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = ph.list_append(((Node)yyVals[-3+yyTop]), nf.newWhen(((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 314:
					// line 1233 "DefaultRubyParser.y"
  {
                    ph.value_expr(((Node)yyVals[0+yyTop]));
                    yyVal = nf.newList(nf.newWhen(((Node)yyVals[0+yyTop]), null, null));
                }
  break;
case 319:
					// line 1244 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 321:
					// line 1249 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-3+yyTop]) != null) {
                        yyVals[-3+yyTop] = ph.node_assign(((Node)yyVals[-3+yyTop]), nf.newGVar("$!"));
                        yyVals[-1+yyTop] = ph.block_append(((Node)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]));
                    }
                    yyVal = nf.newResBody(((Node)yyVals[-4+yyTop]), ((Node)yyVals[-1+yyTop]), ((Node)yyVals[0+yyTop])).setPosFrom(((Node)yyVals[-4+yyTop]) != null ? ((Node)yyVals[-4+yyTop]) : ((Node)yyVals[-1+yyTop]));
                }
  break;
case 324:
					// line 1259 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]) != null) {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    } else {
                        yyVal = nf.newNil();
                    }
                }
  break;
case 326:
					// line 1268 "DefaultRubyParser.y"
  {
                    yyVal = RubySymbol.newSymbol(ruby, ((String)yyVals[0+yyTop]));
                }
  break;
case 328:
					// line 1273 "DefaultRubyParser.y"
  {
                    yyVal = nf.newStr(((RubyObject)yyVals[0+yyTop]));
                }
  break;
case 330:
					// line 1277 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]).getType() == Constants.NODE_DSTR) {
                        ph.list_append(((Node)yyVals[-1+yyTop]), nf.newStr(((RubyObject)yyVals[0+yyTop])));
                    } else {
                        ((RubyString)((Node)yyVals[-1+yyTop]).getLiteral()).concat((RubyString)((RubyObject)yyVals[0+yyTop]));
                    }
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 331:
					// line 1285 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[-1+yyTop]).getType() == Constants.NODE_STR) {
                        yyVal = nf.newDStr(((Node)yyVals[-1+yyTop]).getLiteral());
                    } else {
                        yyVal = ((Node)yyVals[-1+yyTop]);
                    }
                    /* $2.setHeadNode(nf.newStr($2.getLiteral()));*/
                    /* $2.nd_set_type(Constants.NODE_ARRAY);*/
                    /*+++ */
                    yyVals[0+yyTop] = nf.newArray(nf.newStr(((Node)yyVals[0+yyTop]).getLiteral()));
                    /*---*/
                    ph.list_concat(((Node)yyVal), ((Node)yyVals[0+yyTop]));
                }
  break;
case 332:
					// line 1299 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_END);
                    yyVal = ((String)yyVals[0+yyTop]);
                }
  break;
case 344:
					// line 1318 "DefaultRubyParser.y"
  {
                    yyVal = ph.getAccessNode(((String)yyVals[0+yyTop]));
                }
  break;
case 345:
					// line 1321 "DefaultRubyParser.y"
  { 
                    yyVal = nf.newNil();
                }
  break;
case 346:
					// line 1324 "DefaultRubyParser.y"
  {
                    yyVal = nf.newSelf();
                }
  break;
case 347:
					// line 1327 "DefaultRubyParser.y"
  { 
                    yyVal = nf.newTrue();
                }
  break;
case 348:
					// line 1330 "DefaultRubyParser.y"
  {
                    yyVal = nf.newFalse();
                }
  break;
case 349:
					// line 1333 "DefaultRubyParser.y"
  {
                    yyVal = nf.newStr(RubyString.newString(ruby, ruby.getSourceFile())); 
                }
  break;
case 350:
					// line 1336 "DefaultRubyParser.y"
  {
                    yyVal = nf.newLit(RubyFixnum.newFixnum(ruby, ruby.getSourceLine()));
                }
  break;
case 353:
					// line 1343 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
case 354:
					// line 1346 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_BEG);
                }
  break;
case 355:
					// line 1348 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 356:
					// line 1351 "DefaultRubyParser.y"
  {
                    yyerrok();
                    yyVal = null;
                }
  break;
case 357:
					// line 1356 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-2+yyTop]);
                    ph.setLexState(LexState.EXPR_BEG);
                }
  break;
case 358:
					// line 1360 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 359:
					// line 1364 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(((Integer)yyVals[-5+yyTop]), ((Node)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue()), ((Node)yyVals[0+yyTop]));
                }
  break;
case 360:
					// line 1367 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(((Integer)yyVals[-3+yyTop]), ((Node)yyVals[-1+yyTop]), -1), ((Node)yyVals[0+yyTop]));
                }
  break;
case 361:
					// line 1370 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(((Integer)yyVals[-3+yyTop]), null, ((Integer)yyVals[-1+yyTop]).intValue()), ((Node)yyVals[0+yyTop]));
                }
  break;
case 362:
					// line 1373 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(((Integer)yyVals[-1+yyTop]), null, -1), ((Node)yyVals[0+yyTop]));
                }
  break;
case 363:
					// line 1376 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(null, ((Node)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop]).intValue()), ((Node)yyVals[0+yyTop]));
                }
  break;
case 364:
					// line 1379 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(null, ((Node)yyVals[-1+yyTop]), -1), ((Node)yyVals[0+yyTop]));
                }
  break;
case 365:
					// line 1382 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(null, null, ((Integer)yyVals[-1+yyTop]).intValue()), ((Node)yyVals[0+yyTop]));
                }
  break;
case 366:
					// line 1385 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(nf.newArgs(null, null, -1), ((Node)yyVals[0+yyTop]));
                }
  break;
case 367:
					// line 1388 "DefaultRubyParser.y"
  {
                    yyVal = nf.newArgs(null, null, -1);
                }
  break;
case 368:
					// line 1392 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a constant");
                }
  break;
case 369:
					// line 1395 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be an instance variable");
                }
  break;
case 370:
					// line 1398 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a global variable");
                }
  break;
case 371:
					// line 1401 "DefaultRubyParser.y"
  {
                    yyerror("formal argument cannot be a class variable");
                }
  break;
case 372:
					// line 1404 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    } else if (ph.isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate argument name");
                    }
                    ph.getLocalIndex(((String)yyVals[0+yyTop]));
                    yyVal = new Integer(1);
                }
  break;
case 374:
					// line 1415 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(((Integer)yyVal).intValue() + 1);
                }
  break;
case 375:
					// line 1419 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[-2+yyTop]))) {
                        yyerror("formal argument must be local variable");
                    } else if (ph.isLocalRegistered(((String)yyVals[-2+yyTop]))) {
                        yyerror("duplicate optional argument name");
                    }
		    ph.getLocalIndex(((String)yyVals[-2+yyTop]));
                    yyVal = ph.getAssignmentNode(((String)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 376:
					// line 1429 "DefaultRubyParser.y"
  {
                    yyVal = nf.newBlock(((Node)yyVals[0+yyTop]));
                    /* $<Node>$.setEndNode($<Node>$); not needed anymore Benoit.*/
                }
  break;
case 377:
					// line 1433 "DefaultRubyParser.y"
  {
                    yyVal = ph.block_append(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 378:
					// line 1437 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("rest argument must be local variable");
                    } else if (ph.isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate rest argument name");
                    }
                    yyVal = new Integer(ph.getLocalIndex(((String)yyVals[0+yyTop])));
                }
  break;
case 379:
					// line 1445 "DefaultRubyParser.y"
  {
                    yyVal = new Integer(-2);
                }
  break;
case 380:
					// line 1449 "DefaultRubyParser.y"
  {
                    if (!IdUtil.isLocal(((String)yyVals[0+yyTop]))) {
                        yyerror("block argument must be local variable");
                    } else if (ph.isLocalRegistered(((String)yyVals[0+yyTop]))) {
                        yyerror("duplicate block argument name");
                    }
                    yyVal = nf.newBlockArg(((String)yyVals[0+yyTop]));
                }
  break;
case 381:
					// line 1458 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[0+yyTop]);
                }
  break;
case 383:
					// line 1463 "DefaultRubyParser.y"
  {
                    if (((Node)yyVals[0+yyTop]).getType() == Constants.NODE_SELF) {
                        yyVal = nf.newSelf();
                    } else {
                        yyVal = ((Node)yyVals[0+yyTop]);
                    }
                }
  break;
case 384:
					// line 1470 "DefaultRubyParser.y"
  {
                    ph.setLexState(LexState.EXPR_BEG);
                }
  break;
case 385:
					// line 1472 "DefaultRubyParser.y"
  {
                    switch (((Node)yyVals[-2+yyTop]).getType()) {
                        case Constants.NODE_STR:
                        case Constants.NODE_DSTR:
                        case Constants.NODE_XSTR:
                        case Constants.NODE_DXSTR:
                        case Constants.NODE_DREGX:
                        case Constants.NODE_LIT:
                        case Constants.NODE_ARRAY:
                        case Constants.NODE_ZARRAY:
                            yyerror("can't define single method for literals.");
                        default:
                            break;
                    }
                    yyVal = ((Node)yyVals[-2+yyTop]);
                }
  break;
case 387:
					// line 1490 "DefaultRubyParser.y"
  {
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 388:
					// line 1493 "DefaultRubyParser.y"
  {
                    /* if ($1.getLength() % 2 != 0) {
                        yyerror("odd number list for Hash");
                    }*/
                    yyVal = ((Node)yyVals[-1+yyTop]);
                }
  break;
case 390:
					// line 1501 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_concat(((Node)yyVals[-2+yyTop]), ((Node)yyVals[0+yyTop]));
                }
  break;
case 391:
					// line 1505 "DefaultRubyParser.y"
  {
                    yyVal = ph.list_append(nf.newList(((Node)yyVals[-2+yyTop])), ((Node)yyVals[0+yyTop]));
                }
  break;
case 411:
					// line 1535 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 414:
					// line 1541 "DefaultRubyParser.y"
  {
                    yyerrok();
                }
  break;
case 415:
					// line 1545 "DefaultRubyParser.y"
  {
                    yyVal = null;
                }
  break;
					// line 2411 "-"
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
         74,    0,    5,    6,    6,    6,    6,   77,    7,    7,
          7,    7,    7,    7,    7,    7,    7,    7,   78,    7,
          7,    7,    7,    7,    7,    8,    8,    8,    8,    8,
          8,    8,    8,   12,   12,   37,   37,   37,   11,   11,
         11,   11,   11,   55,   55,   58,   58,   57,   57,   57,
         57,   57,   57,   59,   59,   56,   56,   60,   60,   60,
         60,   60,   60,   53,   53,   53,   53,   53,   53,   68,
         68,   69,   69,   69,   69,   69,   61,   61,   47,   80,
         47,   70,   70,   70,   70,   70,   70,   70,   70,   70,
         70,   70,   70,   70,   70,   70,   70,   70,   70,   70,
         70,   70,   70,   70,   70,   70,   70,   79,   79,   79,
         79,   79,   79,   79,   79,   79,   79,   79,   79,   79,
         79,   79,   79,   79,   79,   79,   79,   79,   79,   79,
         79,   79,   79,   79,   79,   79,   79,   79,   79,   79,
         79,   79,   79,   79,   79,   79,   79,   79,    9,   81,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
          9,    9,    9,    9,    9,   83,    9,    9,    9,   29,
         29,   29,   29,   29,   29,   29,   26,   26,   26,   26,
         27,   27,   25,   25,   25,   25,   25,   25,   25,   25,
         25,   25,   85,   28,   31,   30,   30,   22,   22,   33,
         33,   34,   34,   34,   23,   10,   10,   10,   10,   10,
         10,   10,   10,   10,   10,   10,   10,   10,   10,   10,
         10,   10,   10,   10,   10,   10,   86,   10,   10,   10,
         10,   10,   10,   88,   90,   10,   91,   92,   10,   10,
         10,   93,   94,   10,   95,   10,   97,   98,   10,   99,
         10,  100,   10,  102,  103,   10,   10,   10,   10,   10,
         87,   87,   87,   89,   89,   14,   14,   15,   15,   49,
         49,   50,   50,   50,   50,  104,   52,   36,   36,   36,
         13,   13,   13,   13,   13,   13,  105,   51,  106,   51,
         16,   24,   24,   24,   17,   17,   19,   19,   20,   20,
         18,   18,   21,   21,    3,    3,    3,    2,    2,    2,
          2,   64,   63,   63,   63,   63,    4,    4,   62,   62,
         62,   62,   62,   32,   32,   32,   32,   32,   32,   32,
         48,   48,   35,  107,   35,   35,   38,   38,   39,   39,
         39,   39,   39,   39,   39,   39,   39,   72,   72,   72,
         72,   72,   73,   73,   41,   40,   40,   71,   71,   42,
         43,   43,    1,  108,    1,   44,   44,   44,   45,   45,
         46,   65,   65,   65,   66,   66,   66,   66,   67,   67,
         67,  101,  101,   75,   75,   82,   82,   84,   84,   84,
         96,   96,   76,   76,   54,
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
          2,    3,    3,    3,    3,    0,    4,    5,    1,    1,
          2,    4,    2,    5,    2,    3,    3,    4,    4,    6,
          1,    1,    1,    3,    2,    5,    2,    5,    4,    7,
          3,    1,    0,    2,    2,    2,    1,    1,    3,    1,
          1,    3,    4,    2,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    6,    3,    3,    2,    4,    3,    3,
          4,    3,    1,    4,    3,    1,    0,    6,    2,    1,
          2,    6,    6,    0,    0,    7,    0,    0,    7,    5,
          4,    0,    0,    9,    0,    6,    0,    0,    8,    0,
          5,    0,    9,    0,    0,   12,    1,    1,    1,    1,
          1,    1,    2,    1,    1,    1,    5,    1,    2,    1,
          1,    1,    2,    1,    3,    0,    5,    2,    4,    4,
          2,    4,    4,    3,    2,    1,    0,    5,    0,    5,
          5,    1,    4,    2,    1,    1,    1,    1,    2,    1,
          6,    1,    1,    2,    1,    1,    1,    1,    1,    2,
          2,    2,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    0,    4,    2,    4,    2,    6,    4,
          4,    2,    4,    2,    2,    1,    0,    1,    1,    1,
          1,    1,    1,    3,    3,    1,    3,    2,    1,    2,
          2,    1,    1,    0,    5,    1,    2,    2,    1,    3,
          3,    1,    1,    1,    1,    1,    1,    1,    1,    1,
          1,    1,    1,    0,    1,    0,    1,    0,    1,    1,
          1,    1,    1,    2,    0,
    };
  } /* End class YyLenClass */

  protected static final class YyDefRedClass {

    public static final short yyDefRed [] = {            1,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  254,  257,    0,  277,  278,  279,  280,    0,    0,
          0,  346,  345,  347,  348,    0,    0,    0,   19,    0,
        350,  349,    0,    0,  341,  340,    0,  343,  337,  338,
        328,  228,  327,  329,  229,  230,  351,  352,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  226,
        325,    2,    0,    0,    0,    0,    0,    0,   28,    0,
        231,    0,   35,    0,    0,    4,    0,    0,   44,    0,
         54,    0,  326,    0,    0,   70,   71,    0,    0,  270,
        117,  129,  118,  142,  114,  135,  124,  123,  140,  122,
        121,  116,  145,  126,  115,  130,  134,  136,  128,  120,
        137,  147,  139,    0,    0,    0,    0,  113,  133,  132,
        127,  143,  146,  144,  148,  112,  119,  110,  111,    0,
          0,    0,   74,    0,  103,  104,  101,   85,   86,   87,
         90,   92,   88,  105,  106,   93,   94,   98,   89,   91,
         82,   83,   84,   95,   96,   97,   99,  100,  102,  107,
        384,    0,  383,  344,  272,   75,   76,  138,  131,  141,
        125,  108,  109,   72,   73,    0,   79,   78,   77,    0,
          0,    0,    0,    0,  412,  411,    0,    0,    0,  413,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  290,
        291,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        203,    0,   27,  225,  212,    0,  389,    0,    0,    0,
         43,    0,  305,   42,    0,   31,    0,    8,  407,    0,
          0,    0,    0,    0,    0,  237,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  190,    0,    0,    0,  386,
          0,    0,   52,    0,  335,  334,  336,  332,  333,    0,
         32,    0,  330,  331,    3,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,  296,  298,  309,  307,  251,    0,    0,    0,
          0,    0,    0,    0,    0,   56,  150,  301,   39,  249,
          0,    0,  354,  265,  353,    0,    0,  403,  402,  274,
          0,   80,    0,    0,  322,  282,    0,    0,    0,    0,
          0,    0,    0,    0,  414,    0,    0,    0,    0,    0,
          0,  262,    0,    0,  242,    0,    0,    0,    0,    0,
          0,  205,  217,    0,  207,  245,    0,    0,    0,    0,
          0,    0,  214,   10,   12,   11,    0,  247,    0,    0,
          0,    0,    0,    0,  235,    0,    0,  191,    0,  409,
        193,  239,    0,  195,    0,  388,  240,  387,    0,    0,
          0,    0,    0,    0,    0,    0,   18,   29,   30,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  304,
          0,    0,  397,    0,    0,  398,    0,    0,    0,    0,
        395,  396,    0,    0,    0,    0,    0,   22,    0,   24,
          0,   23,   26,  221,    0,   50,   57,    0,    0,  356,
          0,    0,    0,    0,    0,    0,  370,  369,  368,  371,
          0,    0,    0,    0,    0,    0,  376,  366,    0,  373,
          0,    0,    0,    0,    0,  317,    0,    0,  288,    0,
        283,    0,    0,    0,    0,    0,    0,  261,  285,  255,
        284,  258,    0,    0,    0,    0,    0,    0,    0,    0,
        211,  241,    0,    0,    0,    0,    0,    0,    0,  204,
        216,    0,    0,    0,  390,  244,    0,    0,    0,    0,
          0,  197,    9,    0,    0,    0,   21,    0,  196,    0,
          0,    0,    0,    0,    0,    0,    0,    0,  303,   41,
          0,    0,  202,  302,   40,  201,    0,  294,    0,    0,
        292,    0,    0,  300,   38,  299,   37,    0,    0,   55,
          0,  268,    0,    0,  271,    0,  275,    0,  378,  380,
          0,    0,  358,    0,  364,  382,    0,  365,    0,  362,
         81,    0,    0,  320,    0,  289,    0,    0,  323,    0,
          0,  286,    0,  260,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,  209,    0,    0,    0,  198,    0,
          0,  199,    0,   20,    0,  192,    0,    0,    0,    0,
          0,    0,  293,    0,    0,    0,    0,    0,    0,    0,
        355,  266,  385,    0,    0,    0,    0,    0,  377,  381,
          0,    0,    0,  374,    0,    0,  319,    0,    0,  324,
        234,    0,  252,  253,    0,    0,    0,    0,  263,  206,
          0,  208,    0,  248,  194,    0,  295,  297,  310,  308,
          0,    0,    0,  357,    0,  363,    0,  360,  361,    0,
          0,    0,    0,    0,    0,  315,  316,  311,  256,  259,
          0,    0,  200,  269,    0,    0,    0,    0,    0,    0,
          0,  321,    0,    0,  210,    0,  273,  359,    0,  287,
        264,    0,    0,  276,
    };
  } /* End of class YyDefRedClass */

  protected static final class YyDgotoClass {

    public static final short yyDgoto [] = {             1,
        162,   59,   60,   61,  238,   63,   64,   65,   66,  234,
         68,   69,   70,  611,  612,  344,  708,  334,  494,  603,
        608,  243,  213,  507,  214,  563,  564,  224,  244,  362,
        215,   71,  463,  464,  324,   72,   73,  484,  485,  486,
        487,  660,  595,  248,  216,  217,  176,  218,  199,  570,
        320,  304,  182,   76,   77,   78,   79,  240,   80,   81,
        177,  219,  258,   83,  203,  514,  440,   89,  179,  446,
        489,  490,  491,    2,  188,  189,  377,  231,  167,  492,
        468,  230,  379,  391,  225,  544,  337,  191,  510,  618,
        192,  619,  519,  711,  472,  338,  469,  650,  326,  331,
        330,  475,  654,  448,  450,  449,  471,  327,
    };
  } /* End of class YyDgotoClass */

  protected static final class YySindexClass {

    public static final short yySindex [] = {            0,
          0,11753,12083,  176, -186,14621,14349,11753,12579,12579,
      11619,    0,    0,14509,    0,    0,    0,    0,12187,12281,
         55,    0,    0,    0,    0,12579,14245,   91,    0,   16,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,13951,13951,
       -186,11848,13167,13951,16129,14711,14045,13951,  -96,    0,
          0,    0,   57,  550,   -8, 3312,  -42, -151,    0,  -66,
          0,  -11,    0, -198,  122,    0,  133,16037,    0,  189,
          0, -103,    0,  161,  550,    0,    0,12579,  162,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,   38,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  228,    0,    0,    0,   45,
        415,  240,  304,  415,    0,    0,  216,  137,  355,    0,
      12579,12579,  377,  388,   55,   91,  -31,    0,  174,    0,
          0,    0,  161,11753,13951,13951,13951,12383, 2440,    2,
          0,  402,    0,    0,    0,  420,    0, -198, -103,12477,
          0,12673,    0,    0,12673,    0,  337,    0,    0,  432,
        352,11753,  159,   41,  159,    0,11848,  448,    0,  459,
      13951,   91,   34,  422,   79,    0,  165,  407,   79,    0,
         63,    0,    0,    0,    0,    0,    0,    0,    0,  159,
          0,  159,    0,    0,    0,11991,12579,12579,12579,12579,
      12083,12579,12579,13951,13951,13951,13951,13951,13951,13951,
      13951,13951,13951,13951,13951,13951,13951,13951,13951,13951,
      13951,13951,13951,13951,13951,13951,13951,13951,13951,15048,
      15083,13167,    0,    0,    0,    0,    0,15118,15118,13951,
      13261,13261,11848,16129,  483,    0,    0,    0,    0,    0,
         -8,   57,    0,    0,    0,11753,12579,    0,    0,    0,
        141,    0,13951,  278,    0,    0,11753,  283,13951,13363,
      11753,  137,13457,  290,    0,  153,  153,  432,15390,15425,
      13167,    0, 2876, 3312,    0,  521,13951,15460,15495,13167,
      12775,    0,    0,12869,    0,    0,  532, -151,  551,   91,
         12,  566,    0,    0,    0,    0,14349,    0,13951,11753,
        496,15460,15495,  573,    0,    0, 1650,    0,13559,    0,
          0,    0,13951,    0,13951,    0,    0,    0,15530,15565,
      13167,  550,   -8,   -8,   -8,   -8,    0,    0,    0,  159,
       5058, 5058, 5058, 5058,  208,  208, 7095, 4618, 5058, 5058,
       3748, 3748,  494,  494, 4184,  208,  208,  556,  556,  765,
        120,  120,  159,  159,  159,  287,    0,    0,   55,    0,
          0,  292,    0,  316,   55,    0,  571,  -98,  -98,  -98,
          0,    0,   55,   55, 3312,13951, 3312,    0,  624,    0,
       3312,    0,    0,    0,  633,    0,    0,13951,   57,    0,
      12579,11753,  411,   13,15013,  615,    0,    0,    0,    0,
        373,  379,  806,11753,   57,  642,    0,    0,  645,    0,
        646,14349, 3312,  351,  659,    0,11753,  447,    0,  318,
          0, 3312,  278,  455,13951,  667,   46,    0,    0,    0,
          0,    0,    0,   55,    0,    0,   55,  630,12579,  382,
          0,    0, 3312,  287,  292,  316,  631,13951, 2440,    0,
          0,  682,13951, 2440,    0,    0,12775,  692,15118,15118,
        704,    0,    0,12579, 3312,  621,    0,    0,    0,13951,
       3312,   91,    0,    0,    0,  654,13951,13951,    0,    0,
      13951,13951,    0,    0,    0,    0,  410,    0,15853,11753,
          0,11753,11753,    0,    0,    0,    0, 3312,13653,    0,
       3312,    0,  216,  488,    0,  713,    0,13951,    0,    0,
         91,   45,    0,  230,    0,    0,  413,    0,  806,    0,
          0,16129,   46,    0,13951,    0,11753,  498,    0,12579,
        500,    0,  501,    0, 3312,13755,11753,11753,11753,    0,
        153,  410, 2876,12971,    0, 2876, -151,   12,    0,   55,
         55,    0,  202,    0, 1650,    0,    0, 3312, 3312, 3312,
       3312,13951,    0,  639,  505,  506,  656,13951, 3312,11753,
          0,    0,    0,  141, 3312,  732,  278,  615,    0,    0,
        645,  742,  645,    0,   80,    0,    0,    0,11753,    0,
          0,  415,    0,    0,13951,  -27,  524,  525,    0,    0,
      13951,    0,  750,    0,    0, 3312,    0,    0,    0,    0,
       3312,  528,11753,    0,  447,    0,  230,    0,    0,15608,
      15643,13167,   45,11753, 3312,    0,    0,    0,    0,    0,
      11753, 2876,    0,    0,   45,  531,  645,    0,    0,    0,
        707,    0,  318,  533,    0,  278,    0,    0,    0,    0,
          0,  447,  539,    0,
    };
  } /* End of class YySindexClass */

  protected static final class YyRindexClass {

    public static final short yyRindex [] = {            0,
          0,   94,    0,    0,    0,    0,    0,  559,    0,    0,
        535,    0,    0,    0,    0,    0,    0,    0,11331, 6466,
       3618,    0,    0,    0,    0,    0,    0,13849,    0,    0,
          0,    0, 1866, 2841,    0,    0, 1961,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  132,  711,  699,  487,    0,    0,    0, 5886,    0,
          0,    0,  383,  801, 5619,14437,11909, 5167,    0, 5982,
          0,15914,    0,10847,    0,    0,    0,  675,    0,    0,
          0,10932,    0,13065, 1527,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,  214,  564,  694,  728,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  739,
        923, 1122,    0, 1300,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0, 5997,    0,    0,    0,  396,
          0,    0,    0,    0,    0,    0,  535,    0,  548,    0,
          0,    0, 6285, 6370, 5016,  793,    0,  596,    0,    0,
          0,  617,    0,  132,    0,    0,    0,    0,11147, 6854,
          0, 6104,    0,    0,    0, 6104,    0, 5408, 5498,    0,
          0,  794,    0,    0,    0,    0,    0,    0,    0,14147,
          0,   33, 6949, 6769, 7251,    0,  132,    0,  167,    0,
          0,  745,  748,    0,  748,    0,  724,    0,  724,    0,
          0,  124,    0,  511,    0,    0,    0,    0,    0, 7336,
          0, 7431,    0,    0,    0, 1569,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  711,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,  132,  716,  752,    0,    0,    0,    0,    0,
        100,    0,    0,    0,    0,  197,    0,    0,    0,    0,
        107,    0,   32,  349,    0,    0,  640,11208,    0,    0,
        102,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        711,    0, 6104, 6588,    0,    0,    0,    0,    0,  711,
          0,    0,    0,    0,    0,    0,    0,  515,  565,  811,
        811,    0,    0,    0,    0,    0,    0,    0,    0,   33,
          0,    0,    0,    0,    0,  715,  745,    0,  764,    0,
          0,    0,  112,    0,  754,    0,    0,    0,    0,    0,
        711, 4986, 6221, 6481, 6705, 7069,    0,    0,    0, 7733,
        854, 8032, 9621, 9697, 9177, 9253, 9773,10033, 9881, 9957,
       1334,10088, 1767, 8671,    0, 9344, 9530, 8824, 9101, 8748,
       8292, 8369, 7818, 7913, 8215, 4149, 3182, 3713,13065,    0,
       3277, 4490,    0, 4585, 4054,    0,    0,11416,11416,11522,
          0,    0, 4926, 4926, 1120,    0, 5258,    0,    0,    0,
      10222,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  197,    0,  811,    0,  105,    0,    0,    0,    0,
        499,    0,  578,  559,    0,  132,    0,    0,  132,    0,
        132,    0,   70,   96,   93,    0,  247,  595,    0,  595,
          0,10124,  595,    0,    0,  115,    0,    0,    0,    0,
          0,    0, 1014,    0, 1460, 1605, 5319,    0,    0,    0,
          0,    0,15731, 2310, 2405, 2746,    0,    0,15801,    0,
          0, 6104,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,10186,    0,    0,   58,    0,    0,
         31,  745, 1238, 1256, 1592,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,11028,    0,    0,  197,
          0,  197,   33,    0,    0,    0,    0,15950,    0,    0,
      10275,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        811,  396,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,  197,    0,    0,    0,
          0,    0,    0,    0,  166,    0,  295,  197,  197,  734,
          0, 5801, 6104,    0,    0, 6104,  579,  811,    0,   67,
         67,    0,    0,    0,  745,    0,  947,10401,10465,10504,
      10541,    0,    0,    0,    0,    0,    0,    0,11276,  197,
          0,    0,    0,  107,  807,    0,  349,    0,    0,    0,
        132,  132,  132,    0,    0,  101,    0,  138,  559,    0,
          0,    0,    0,    0,    0,  595,    0,    0,    0,    0,
          0,    0,    0,    0,    0,10581,    0,    0,    0,    0,
      15996,    0,  559,    0,  595,    0,    0,    0,    0,    0,
          0,  711,  396,  640,  193,    0,    0,    0,    0,    0,
        197, 6104,    0,    0,  396,    0,  132,  140,  648,  691,
          0,    0,  595,    0,    0,  349,    0,    0,  190,    0,
          0,  595,    0,    0,
    };
  } /* End of class YyRindexClass */

  protected static final class YyGindexClass {

    public static final short yyGindex [] = {            0,
          0,    0,    0,    0,  489,    0,   82,   19, 1199,   -2,
         54,  -17,    0,  154, -315, -332,    0, -191,    0,    0,
       -595,  612,   39,    0,   36,  -16, -391,  -23, -281, -213,
        104,  874,    0,  572,    0, -197,    0,  232,  404,  289,
       -567, -279,  364,    0,   28, -166,    0,  670,  320,  221,
        820,    0,  753, 1220,  445,    0,  -20, -172,  813,  -18,
        -25,  312,    0,    4,  125, -262,    0,  229,    8,   27,
       -528,  293,    0,    0,    6,  834,    0,    0,    0,    0,
          0,   -1,    0,  618,    0,    0, -171,    0, -331,    0,
          0,    0,    0,    0,    0,    9,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,
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
    "tREGEXP","tDSTRING","tDXSTRING","tDREGEXP","tNTH_REF","tBACK_REF",
    "tUPLUS","tUMINUS","tPOW","tCMP","tEQ","tEQQ","tNEQ","tGEQ","tLEQ",
    "tANDOP","tOROP","tMATCH","tNMATCH","tDOT2","tDOT3","tAREF","tASET",
    "tLSHFT","tRSHFT","tCOLON2","tCOLON3","tOP_ASGN","tASSOC","tLPAREN",
    "tLBRACK","tLBRACE","tSTAR","tAMPER","tSYMBEG","LAST_TOKEN",
    };
  } /* End of class YyNameClass */


					// line 1549 "DefaultRubyParser.y"

    // +++
    // Helper Methods
    
    void yyerrok() {}
    
    public Node compileString(String f, RubyObject s, int line) {
        /*rs.setLexFileIo(false);
        rs.setLexGetsPtr(0);
        rs.setLexInput(s);
        rs.setLexP(0);
        rs.setLexPEnd(0);*/
        ruby.setSourceLine(line - 1);

		rs.setSource(new StringScannerSource(f, s.toString()));
        ph.setCompileForEval(ruby.getInEval());

        return yycompile(f, line);
    }

    public Node compileJavaString(String f, String s, int len, int line) {
        return compileString(f, RubyString.newString(ruby, s, len), line);
    }

    public Node compileFile(String f, RubyObject file, int start) {
        /*rs.setLexFileIo(true);
        rs.setLexInput(file);
        rs.setLexP(0);
        rs.setLexPEnd(0);*/
        ruby.setSourceLine(start - 1);

        return yycompile(f, start);
    }
    
    /** This function compiles a given String into a Node.
     *
     */
    public Node yycompile(String f, int line) {
        if (!ph.isCompileForEval() && ruby.getSafeLevel() == 0 && ruby.getClasses().getObjectClass().isConstantDefined("SCRIPT_LINES__")) {
            RubyHash hash = (RubyHash)ruby.getClasses().getObjectClass().getConstant("SCRIPT_LINES__");
            RubyString fName = RubyString.newString(ruby, f);
            
            // XXX +++
            RubyObject debugLines = ruby.getNil(); // = rb_hash_aref(hash, fName);
            // XXX ---
            
            if (debugLines.isNil()) {
                ph.setRubyDebugLines(RubyArray.newArray(ruby));
                hash.aset(fName, ph.getRubyDebugLines());
            } else {
                ph.setRubyDebugLines((RubyArray)debugLines);
            }
            
            if (line > 1) {
                RubyString str = RubyString.newString(ruby, null);
                while (line > 1) {
                    ph.getRubyDebugLines().push(str);
                    line--;
                }
            }
        }

        ph.setRubyEndSeen(false);   // is there an __end__{} statement?
        ph.setEvalTree(null);       // parser stores Nodes here
        ph.setHeredocEnd(0);
        ruby.setSourceFile(f);      // source file name
        ph.setRubyInCompile(true);

        try {
            yyparse(rs, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ph.setRubyDebugLines(null); // remove debug info
        ph.setCompileForEval(0);
        ph.setRubyInCompile(false);
        rs.resetStacks();
        ph.setClassNest(0);
        ph.setInSingle(0);
        ph.setInDef(0);
        ph.setCurMid(null);

        return ph.getEvalTree();
    }
}
					// line 6660 "-"
