					// line 2 "parse.y"
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

import org.jruby.*;
import org.jruby.interpreter.*;
import org.jruby.interpreter.nodes.*;
import org.jruby.original.*;
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
    NODE *node;
    VALUE val;
    ID id;
    int num;
    struct RVarmap *vars;
}
*/
					// line 63 "-"
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
    if (expected != null && expected.length > 0) {
      System.err.print(message+", expecting");
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
					// line 182 "parse.y"
  {
                yyVal = ruby.getDynamicVars();
			    ph.setLexState(LexState.EXPR_BEG);
                ph.top_local_init();
			    if (ruby.getRubyClass() == ruby.getClasses().getObjectClass())
                    ph.setClassNest(0);
			    else
                    ph.setClassNest(1);
            }
  break;
case 2:
					// line 192 "parse.y"
  {
                if (((NODE)yyVals[0+yyTop]) != null && !ph.isCompileForEval()) {
                    /* last expression should not be void */
			        if (((NODE)yyVals[0+yyTop]).nd_type() != NODE.NODE_BLOCK)
                        ph.void_expr(((NODE)yyVals[0+yyTop]));
			        else {
                        NODE node = ((NODE)yyVals[0+yyTop]);
				        while (node.nd_next() != null) {
				            node = node.nd_next();
				        }
				        ph.void_expr(node.nd_head());
			        }
			    }
			    ph.setEvalTree(ph.block_append(ph.getEvalTree(), ((NODE)yyVals[0+yyTop])));
                ph.top_local_setup();
			    ph.setClassNest(0);
		        ruby.setDynamicVars(((RubyVarmap)yyVals[-1+yyTop]));
		    }
  break;
case 3:
					// line 212 "parse.y"
  {
			    ph.void_stmts(((NODE)yyVals[-1+yyTop]));
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 5:
					// line 219 "parse.y"
  {
			    yyVal = ph.newline_node(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 6:
					// line 223 "parse.y"
  {
			    yyVal = ph.block_append(((NODE)yyVals[-2+yyTop]), ph.newline_node(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 7:
					// line 227 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 8:
					// line 231 "parse.y"
  {ph.setLexState(LexState.EXPR_FNAME);}
  break;
case 9:
					// line 232 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("alias within method");
		        yyVal = nf.newAlias(((RubyId)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 10:
					// line 238 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("alias within method");
		        yyVal = nf.newVAlias(((RubyId)yyVals[-1+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 11:
					// line 244 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("alias within method");
			    String buf = "$" + (char)((NODE)yyVals[0+yyTop]).nd_nth();
		        yyVal = nf.newVAlias(((RubyId)yyVals[-1+yyTop]), ruby.intern(buf));
		    }
  break;
case 12:
					// line 251 "parse.y"
  {
		        yyerror("can't make alias for the number variables");
		        yyVal = null; /*XXX 0*/
		    }
  break;
case 13:
					// line 256 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("undef within method");
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 14:
					// line 262 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newIf(ph.cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), null);
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 15:
					// line 268 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newUnless(ph.cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), null);
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 16:
					// line 274 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    if (((NODE)yyVals[-2+yyTop]) != null && ((NODE)yyVals[-2+yyTop]).nd_type() == NODE.NODE_BEGIN) {
			        yyVal = nf.newWhile(ph.cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]).nd_body(), 0);
			    } else {
			        yyVal = nf.newWhile(ph.cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), 1);
			    }
		    }
  break;
case 17:
					// line 283 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    if (((NODE)yyVals[-2+yyTop]) != null && ((NODE)yyVals[-2+yyTop]).nd_type() == NODE.NODE_BEGIN) {
			        yyVal = nf.newUntil(ph.cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]).nd_body(), 0);
			    } else {
			        yyVal = nf.newUntil(ph.cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), 1);
			    }
		    }
  break;
case 18:
					// line 292 "parse.y"
  {
			    yyVal = nf.newRescue(((NODE)yyVals[-2+yyTop]), nf.newResBody(null,((NODE)yyVals[0+yyTop]),null), null);
		    }
  break;
case 19:
					// line 296 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle()) {
			        yyerror("BEGIN in method");
			    }
			    ph.local_push();
		    }
  break;
case 20:
					// line 303 "parse.y"
  {
			    ph.setEvalTreeBegin(ph.block_append(ph.getEvalTree(), nf.newPreExe(((NODE)yyVals[-1+yyTop]))));
		        ph.local_pop();
		        yyVal = null; /*XXX 0;*/
		    }
  break;
case 21:
					// line 309 "parse.y"
  {
			    if (ph.isCompileForEval() && (ph.isInDef() || ph.isInSingle())) {
			        yyerror("END in method; use at_exit");
			    }

			    yyVal = nf.newIter(null, nf.newPostExe(), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 22:
					// line 317 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ph.node_assign(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 23:
					// line 322 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    ((NODE)yyVals[-2+yyTop]).nd_value(((NODE)yyVals[0+yyTop]));
			    yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 24:
					// line 328 "parse.y"
  {
			    yyVal = ph.node_assign(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 26:
					// line 334 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    ((NODE)yyVals[-2+yyTop]).nd_value(((NODE)yyVals[0+yyTop]));
			    yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 27:
					// line 340 "parse.y"
  {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    yyVal = nf.newReturn(ph.ret_args(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 29:
					// line 347 "parse.y"
  {
			    yyVal = ph.logop(NODE.NODE_AND, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 30:
					// line 351 "parse.y"
  {
			    yyVal = ph.logop(NODE.NODE_OR, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 31:
					// line 355 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newNot(ph.cond(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 32:
					// line 360 "parse.y"
  {
			    yyVal = nf.newNot(ph.cond(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 37:
					// line 370 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 38:
					// line 375 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 39:
					// line 381 "parse.y"
  {
			    yyVal = ph.new_fcall(((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 40:
					// line 386 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 41:
					// line 392 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 42:
					// line 398 "parse.y"
  {
			    if (!ph.isCompileForEval() && ph.isInDef() && ph.isInSingle())
			        yyerror("super called outside of method");
			    yyVal = ph.new_super(((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 43:
					// line 405 "parse.y"
  {
			    yyVal = nf.newYield(ph.ret_args(((NODE)yyVals[0+yyTop])));
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 45:
					// line 412 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 47:
					// line 418 "parse.y"
  {
			    yyVal = nf.newMAsgn(nf.newList(((NODE)yyVals[-1+yyTop])), null);
		    }
  break;
case 48:
					// line 423 "parse.y"
  {
			    yyVal = nf.newMAsgn(((NODE)yyVals[0+yyTop]), null);
		    }
  break;
case 49:
					// line 427 "parse.y"
  {
			    yyVal = nf.newMAsgn(ph.list_append(((NODE)yyVals[-1+yyTop]),((NODE)yyVals[0+yyTop])), null);
		    }
  break;
case 50:
					// line 431 "parse.y"
  {
			    yyVal = nf.newMAsgn(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 51:
					// line 435 "parse.y"
  {
			    yyVal = nf.newMAsgn(((NODE)yyVals[-1+yyTop]), NODE.MINUS_ONE);
		    }
  break;
case 52:
					// line 439 "parse.y"
  {
			    yyVal = nf.newMAsgn(null, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 53:
					// line 443 "parse.y"
  {
			    yyVal = nf.newMAsgn(null, NODE.MINUS_ONE);
		    }
  break;
case 55:
					// line 449 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 56:
					// line 454 "parse.y"
  {
			    yyVal = nf.newList(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 57:
					// line 458 "parse.y"
  {
			    yyVal = ph.list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 58:
					// line 463 "parse.y"
  {
			    yyVal = ph.assignable(((RubyId)yyVals[0+yyTop]), null);
		    }
  break;
case 59:
					// line 467 "parse.y"
  {
			    yyVal = ph.aryset(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 60:
					// line 471 "parse.y"
  {
			    yyVal = ph.attrset(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 61:
					// line 475 "parse.y"
  {
			    yyVal = ph.attrset(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 62:
					// line 479 "parse.y"
  {
			    yyVal = ph.attrset(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 63:
					// line 483 "parse.y"
  {
		        ph.rb_backref_error(((NODE)yyVals[0+yyTop]));
			    yyVal = null; /*XXX 0;*/
		    }
  break;
case 64:
					// line 489 "parse.y"
  {
			    yyVal = ph.assignable(((RubyId)yyVals[0+yyTop]), null);
		    }
  break;
case 65:
					// line 493 "parse.y"
  {
			    yyVal = ph.aryset(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 66:
					// line 497 "parse.y"
  {
			    yyVal = ph.attrset(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 67:
					// line 501 "parse.y"
  {
			    yyVal = ph.attrset(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 68:
					// line 505 "parse.y"
  {
			    yyVal = ph.attrset(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 69:
					// line 509 "parse.y"
  {
		        ph.rb_backref_error(((NODE)yyVals[0+yyTop]));
			    yyVal = null; /*XXX 0;*/
		    }
  break;
case 70:
					// line 515 "parse.y"
  {
			    yyerror("class/module name must be CONSTANT");
		    }
  break;
case 75:
					// line 524 "parse.y"
  {
			    ph.setLexState(LexState.EXPR_END);
                yyVal = ((RubyId)yyVals[0+yyTop]);
		    }
  break;
case 76:
					// line 529 "parse.y"
  {
			    ph.setLexState(LexState.EXPR_END);
			    yyVal = yyVals[0+yyTop];
		    }
  break;
case 79:
					// line 538 "parse.y"
  {
			    yyVal = nf.newUndef(((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 80:
					// line 541 "parse.y"
  {ph.setLexState(LexState.EXPR_FNAME);}
  break;
case 81:
					// line 542 "parse.y"
  {
			    yyVal = ph.block_append(((NODE)yyVals[-3+yyTop]), nf.newUndef(((RubyId)yyVals[0+yyTop])));
		    }
  break;
case 82:
					// line 546 "parse.y"
  { yyVal = RubyId.newId(ruby, '|'); }
  break;
case 83:
					// line 547 "parse.y"
  { yyVal = RubyId.newId(ruby, '^'); }
  break;
case 84:
					// line 548 "parse.y"
  { yyVal = RubyId.newId(ruby, '&'); }
  break;
case 85:
					// line 549 "parse.y"
  { yyVal = RubyId.newId(ruby, tCMP); }
  break;
case 86:
					// line 550 "parse.y"
  { yyVal = RubyId.newId(ruby, tEQ); }
  break;
case 87:
					// line 551 "parse.y"
  { yyVal = RubyId.newId(ruby, tEQQ); }
  break;
case 88:
					// line 552 "parse.y"
  { yyVal = RubyId.newId(ruby, tMATCH); }
  break;
case 89:
					// line 553 "parse.y"
  { yyVal = RubyId.newId(ruby, '>'); }
  break;
case 90:
					// line 554 "parse.y"
  { yyVal = RubyId.newId(ruby, tGEQ); }
  break;
case 91:
					// line 555 "parse.y"
  { yyVal = RubyId.newId(ruby, '<'); }
  break;
case 92:
					// line 556 "parse.y"
  { yyVal = RubyId.newId(ruby, tLEQ); }
  break;
case 93:
					// line 557 "parse.y"
  { yyVal = RubyId.newId(ruby, tLSHFT); }
  break;
case 94:
					// line 558 "parse.y"
  { yyVal = RubyId.newId(ruby, tRSHFT); }
  break;
case 95:
					// line 559 "parse.y"
  { yyVal = RubyId.newId(ruby, '+'); }
  break;
case 96:
					// line 560 "parse.y"
  { yyVal = RubyId.newId(ruby, '-'); }
  break;
case 97:
					// line 561 "parse.y"
  { yyVal = RubyId.newId(ruby, '*'); }
  break;
case 98:
					// line 562 "parse.y"
  { yyVal = RubyId.newId(ruby, '*'); }
  break;
case 99:
					// line 563 "parse.y"
  { yyVal = RubyId.newId(ruby, '/'); }
  break;
case 100:
					// line 564 "parse.y"
  { yyVal = RubyId.newId(ruby, '%'); }
  break;
case 101:
					// line 565 "parse.y"
  { yyVal = RubyId.newId(ruby, tPOW); }
  break;
case 102:
					// line 566 "parse.y"
  { yyVal = RubyId.newId(ruby, '~'); }
  break;
case 103:
					// line 567 "parse.y"
  { yyVal = RubyId.newId(ruby, tUPLUS); }
  break;
case 104:
					// line 568 "parse.y"
  { yyVal = RubyId.newId(ruby, tUMINUS); }
  break;
case 105:
					// line 569 "parse.y"
  { yyVal = RubyId.newId(ruby, tAREF); }
  break;
case 106:
					// line 570 "parse.y"
  { yyVal = RubyId.newId(ruby, tASET); }
  break;
case 107:
					// line 571 "parse.y"
  { yyVal = RubyId.newId(ruby, '`'); }
  break;
case 149:
					// line 582 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ph.node_assign(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 150:
					// line 586 "parse.y"
  {yyVal = ph.assignable(((RubyId)yyVals[-1+yyTop]), null);}
  break;
case 151:
					// line 587 "parse.y"
  {
			    if (((RubyId)yyVals[-2+yyTop]).intValue() == tOROP) {
			        ((NODE)yyVals[-1+yyTop]).nd_value(((NODE)yyVals[0+yyTop]));
			        yyVal = nf.newOpAsgnOr(ph.gettable(((RubyId)yyVals[-3+yyTop])), ((NODE)yyVals[-1+yyTop]));
			        if (((RubyId)yyVals[-3+yyTop]).is_instance_id()) {
				        ((NODE)yyVal).nd_aid(((RubyId)yyVals[-3+yyTop]));
			        }
			    } else if (((RubyId)yyVals[-2+yyTop]).intValue() == tANDOP) {
			        ((NODE)yyVals[-1+yyTop]).nd_value(((NODE)yyVals[0+yyTop]));
			        yyVal = nf.newOpAsgnAnd(ph.gettable(((RubyId)yyVals[-3+yyTop])), ((NODE)yyVals[-1+yyTop]));
			    } else {
			        yyVal = ((NODE)yyVals[-1+yyTop]);
			        if (yyVal != null) {
				        ((NODE)yyVal).nd_value(ph.call_op(ph.gettable(((RubyId)yyVals[-3+yyTop])),((RubyId)yyVals[-2+yyTop]).intValue(),1,((NODE)yyVals[0+yyTop])));
			        }
			    }
			    ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 152:
					// line 606 "parse.y"
  {
			    NODE args = nf.newList(((NODE)yyVals[0+yyTop]));

			    ph.list_append(((NODE)yyVals[-3+yyTop]), nf.newNil());
			    ph.list_concat(args, ((NODE)yyVals[-3+yyTop]));
                if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tOROP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 0);
			    } else if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tANDOP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 1);
			    }
			    yyVal = nf.newOpAsgn1(((NODE)yyVals[-5+yyTop]), ((RubyId)yyVals[-1+yyTop]), args);
		        ph.fixpos(yyVal, ((NODE)yyVals[-5+yyTop]));
		    }
  break;
case 153:
					// line 620 "parse.y"
  {
                if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tOROP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 0);
			    } else if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tANDOP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 1);
			    }
			    yyVal = nf.newOpAsgn2(((NODE)yyVals[-4+yyTop]), ((RubyId)yyVals[-2+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 154:
					// line 630 "parse.y"
  {
                if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tOROP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 0);
			    } else if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tANDOP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 1);
			    }
			    yyVal = nf.newOpAsgn2(((NODE)yyVals[-4+yyTop]), ((RubyId)yyVals[-2+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 155:
					// line 640 "parse.y"
  {
			    if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tOROP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 0);
			    } else if (((RubyId)yyVals[-1+yyTop]).intValue() == Token.tANDOP) {
			        yyVals[-1+yyTop] = RubyId.newId(ruby, 1);
			    }
			    yyVal = nf.newOpAsgn2(((NODE)yyVals[-4+yyTop]), ((RubyId)yyVals[-2+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 156:
					// line 650 "parse.y"
  {
		        ph.rb_backref_error(((NODE)yyVals[-2+yyTop]));
			    yyVal = null; /*XXX 0*/
		    }
  break;
case 157:
					// line 655 "parse.y"
  {
			    yyVal = nf.newDot2(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 158:
					// line 659 "parse.y"
  {
			    yyVal = nf.newDot3(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 159:
					// line 663 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '+', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 160:
					// line 667 "parse.y"
  {
		        yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '-', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 161:
					// line 671 "parse.y"
  {
		        yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '*', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 162:
					// line 675 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '/', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 163:
					// line 679 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '%', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 164:
					// line 683 "parse.y"
  {
			    boolean need_negate = false;

			    if (((NODE)yyVals[-2+yyTop]).nd_type() == NODE.NODE_LIT) {
                    if (((NODE)yyVals[-2+yyTop]).nd_lit() instanceof RubyFixnum || 
                        ((NODE)yyVals[-2+yyTop]).nd_lit() instanceof RubyFloat ||
                        ((NODE)yyVals[-2+yyTop]).nd_lit() instanceof RubyBignum) {
                        if (((NODE)yyVals[-2+yyTop]).nd_lit().funcall(ruby.intern("<"), RubyFixnum.zero(ruby)).isTrue()) {
                            ((NODE)yyVals[-2+yyTop]).nd_lit(((NODE)yyVals[-2+yyTop]).nd_lit().funcall(ruby.intern("-@")));
                            need_negate = true;
                        }
                    }
			    }
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tPOW, 1, ((NODE)yyVals[0+yyTop]));
			    if (need_negate) {
			        yyVal = ph.call_op(((NODE)yyVal), tUMINUS, 0, null);
			    }
		    }
  break;
case 165:
					// line 702 "parse.y"
  {
			    if (((NODE)yyVals[0+yyTop]) != null && ((NODE)yyVals[0+yyTop]).nd_type() == NODE.NODE_LIT) {
			        yyVal = ((NODE)yyVals[0+yyTop]);
			    } else {
			        yyVal = ph.call_op(((NODE)yyVals[0+yyTop]), tUPLUS, 0, null);
			    }
		    }
  break;
case 166:
					// line 710 "parse.y"
  {
			    if (((NODE)yyVals[0+yyTop]) != null && ((NODE)yyVals[0+yyTop]).nd_type() == NODE.NODE_LIT && ((NODE)yyVals[0+yyTop]).nd_lit() instanceof RubyFixnum) {
			        long i = ((RubyFixnum)((NODE)yyVals[0+yyTop]).nd_lit()).getValue();

			        ((NODE)yyVals[0+yyTop]).nd_lit(RubyFixnum.m_newFixnum(ruby, -i));
			        yyVal = ((NODE)yyVals[0+yyTop]);
			    } else {
			        yyVal = ph.call_op(((NODE)yyVals[0+yyTop]), tUMINUS, 0, null);
			    }
		    }
  break;
case 167:
					// line 721 "parse.y"
  {
		        yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '|', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 168:
					// line 725 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '^', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 169:
					// line 729 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '&', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 170:
					// line 733 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tCMP, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 171:
					// line 737 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '>', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 172:
					// line 741 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tGEQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 173:
					// line 745 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), '<', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 174:
					// line 749 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tLEQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 175:
					// line 753 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tEQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 176:
					// line 757 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tEQQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 177:
					// line 761 "parse.y"
  {
			    yyVal = nf.newNot(ph.call_op(((NODE)yyVals[-2+yyTop]), tEQ, 1, ((NODE)yyVals[0+yyTop])));
		    }
  break;
case 178:
					// line 765 "parse.y"
  {
			    yyVal = ph.match_gen(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 179:
					// line 769 "parse.y"
  {
			    yyVal = nf.newNot(ph.match_gen(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop])));
		    }
  break;
case 180:
					// line 773 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newNot(ph.cond(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 181:
					// line 778 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[0+yyTop]), '~', 0, null);
		    }
  break;
case 182:
					// line 782 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tLSHFT, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 183:
					// line 786 "parse.y"
  {
			    yyVal = ph.call_op(((NODE)yyVals[-2+yyTop]), tRSHFT, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 184:
					// line 790 "parse.y"
  {
			    yyVal = ph.logop(NODE.NODE_AND, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 185:
					// line 794 "parse.y"
  {
			    yyVal = ph.logop(NODE.NODE_OR, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 186:
					// line 797 "parse.y"
  { ph.setInDefined(true);}
  break;
case 187:
					// line 798 "parse.y"
  {
		        ph.setInDefined(false);
			    yyVal = nf.newDefined(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 188:
					// line 803 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-4+yyTop]));
			    yyVal = nf.newIf(ph.cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 189:
					// line 809 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 191:
					// line 815 "parse.y"
  {
			yyVal = nf.newList(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 192:
					// line 819 "parse.y"
  {
			yyVal = ph.list_append(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
            }
  break;
case 193:
					// line 823 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 194:
					// line 827 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_concat(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 195:
					// line 832 "parse.y"
  {
			    yyVal = nf.newList(nf.newHash(((NODE)yyVals[-1+yyTop])));
		    }
  break;
case 196:
					// line 836 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = nf.newRestArgs(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 197:
					// line 842 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 198:
					// line 846 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 199:
					// line 850 "parse.y"
  {
			    yyVal = nf.newList(((NODE)yyVals[-2+yyTop]));
		    }
  break;
case 200:
					// line 854 "parse.y"
  {
			    yyVal = ph.list_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
		    }
  break;
case 203:
					// line 862 "parse.y"
  {
			    yyVal = nf.newList(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 204:
					// line 866 "parse.y"
  {
			yyVal = ph.list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 205:
					// line 870 "parse.y"
  {
			    yyVal = ph.arg_blk_pass(((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 206:
					// line 874 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_concat(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 207:
					// line 880 "parse.y"
  {
			    yyVal = nf.newList(nf.newHash(((NODE)yyVals[-1+yyTop])));
			    yyVal = ph.arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 208:
					// line 885 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_concat(nf.newList(nf.newHash(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 209:
					// line 891 "parse.y"
  {
			    yyVal = ph.list_append(((NODE)yyVals[-3+yyTop]), nf.newHash(((NODE)yyVals[-1+yyTop])));
			    yyVal = ph.arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 210:
					// line 896 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_concat(ph.list_append(((NODE)yyVals[-6+yyTop]), nf.newHash(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 211:
					// line 902 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = ph.arg_blk_pass(nf.newRestArgs(((NODE)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 213:
					// line 908 "parse.y"
  { rs.CMDARG_PUSH(); }
  break;
case 214:
					// line 909 "parse.y"
  {
			    rs.CMDARG_POP();
		        yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 215:
					// line 915 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newBlockPass(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 216:
					// line 921 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 218:
					// line 927 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newList(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 219:
					// line 932 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ph.list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 220:
					// line 938 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 222:
					// line 945 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ph.list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 223:
					// line 950 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ph.arg_concat(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 224:
					// line 955 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 225:
					// line 961 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
			    if (((NODE)yyVals[0+yyTop]) != null) {
			        if (((NODE)yyVals[0+yyTop]).nd_type() == NODE.NODE_ARRAY && ((NODE)yyVals[0+yyTop]).nd_next() == null) {
				        yyVal = ((NODE)yyVals[0+yyTop]).nd_head();
    			    } else if (((NODE)yyVals[0+yyTop]).nd_type() == NODE.NODE_BLOCK_PASS) {
	    			    ph.rb_compile_error("block argument should not be given");
		    	    }
			    }
		    }
  break;
case 226:
					// line 973 "parse.y"
  {
			    yyVal = nf.newLit(((VALUE)yyVals[0+yyTop]));
		    }
  break;
case 228:
					// line 978 "parse.y"
  {
			    yyVal = nf.newXStr(((VALUE)yyVals[0+yyTop]));
		    }
  break;
case 233:
					// line 986 "parse.y"
  {
			    yyVal = nf.newVCall(((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 234:
					// line 995 "parse.y"
  {
			    if (((NODE)yyVals[-3+yyTop]) == null && ((NODE)yyVals[-2+yyTop]) == null && ((NODE)yyVals[-1+yyTop]) == null)
			        yyVal = nf.newBegin(((NODE)yyVals[-4+yyTop]));
			    else {
			        if (((NODE)yyVals[-3+yyTop]) != null) yyVals[-4+yyTop] = nf.newRescue(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-2+yyTop]));
			        else if (((NODE)yyVals[-2+yyTop]) != null) {
				        ph.rb_warn("else without rescue is useless");
				        yyVals[-4+yyTop] = ph.block_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
			        }
			        if (((NODE)yyVals[-1+yyTop]) != null) yyVals[-4+yyTop] = nf.newEnsure(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
			        yyVal = ((NODE)yyVals[-4+yyTop]);
			    }
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 235:
					// line 1010 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 236:
					// line 1014 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-2+yyTop]));
			    yyVal = nf.newColon2(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 237:
					// line 1019 "parse.y"
  {
			    yyVal = nf.newColon3(((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 238:
					// line 1023 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = nf.newCall(((NODE)yyVals[-3+yyTop]), ph.newId(tAREF), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 239:
					// line 1028 "parse.y"
  {
		        if (((NODE)yyVals[-1+yyTop]) == null) {
			        yyVal = nf.newZArray(); /* zero length array*/
			    } else {
			        yyVal = ((NODE)yyVals[-1+yyTop]);
			    }
		    }
  break;
case 240:
					// line 1036 "parse.y"
  {
			    yyVal = nf.newHash(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 241:
					// line 1040 "parse.y"
  {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = nf.newReturn(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 242:
					// line 1047 "parse.y"
  {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    yyVal = nf.newReturn(null);
		    }
  break;
case 243:
					// line 1053 "parse.y"
  {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle())
			        yyerror("return appeared outside of method");
			    yyVal = nf.newReturn(null);
		    }
  break;
case 244:
					// line 1059 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-1+yyTop]));
			    yyVal = nf.newYield(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 245:
					// line 1064 "parse.y"
  {
			    yyVal = nf.newYield(null);
		    }
  break;
case 246:
					// line 1068 "parse.y"
  {
			    yyVal = nf.newYield(null);
		    }
  break;
case 247:
					// line 1071 "parse.y"
  {ph.setInDefined(true);}
  break;
case 248:
					// line 1072 "parse.y"
  {
		        ph.setInDefined(false);
			    yyVal = nf.newDefined(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 249:
					// line 1077 "parse.y"
  {
			    ((NODE)yyVals[0+yyTop]).nd_iter(nf.newFCall(((RubyId)yyVals[-1+yyTop]), null));
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 251:
					// line 1083 "parse.y"
  {
			    if (((NODE)yyVals[-1+yyTop]) != null && ((NODE)yyVals[-1+yyTop]).nd_type() == NODE.NODE_BLOCK_PASS) {
			        ph.rb_compile_error("both block arg and actual block given");
			    }
			    ((NODE)yyVals[0+yyTop]).nd_iter(((NODE)yyVals[-1+yyTop]));
			    yyVal = ((NODE)yyVals[0+yyTop]);
		        ph.fixpos(yyVal, ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 252:
					// line 1095 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-4+yyTop]));
			    yyVal = nf.newIf(ph.cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 253:
					// line 1104 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-4+yyTop]));
			    yyVal = nf.newUnless(ph.cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 254:
					// line 1109 "parse.y"
  { rs.COND_PUSH(); }
  break;
case 255:
					// line 1109 "parse.y"
  { rs.COND_POP(); }
  break;
case 256:
					// line 1112 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-4+yyTop]));
			    yyVal = nf.newWhile(ph.cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-1+yyTop]), 1);
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 257:
					// line 1117 "parse.y"
  { rs.COND_PUSH(); }
  break;
case 258:
					// line 1117 "parse.y"
  { rs.COND_POP(); }
  break;
case 259:
					// line 1120 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-4+yyTop]));
			    yyVal = nf.newUntil(ph.cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-1+yyTop]), 1);
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 260:
					// line 1128 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = nf.newCase(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 261:
					// line 1134 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 262:
					// line 1137 "parse.y"
  { rs.COND_PUSH(); }
  break;
case 263:
					// line 1137 "parse.y"
  { rs.COND_POP(); }
  break;
case 264:
					// line 1140 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-4+yyTop]));
			    yyVal = nf.newFor(((NODE)yyVals[-7+yyTop]), ((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-7+yyTop]));
		    }
  break;
case 265:
					// line 1146 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("class definition in method body");
			    ph.setClassNest(ph.getClassNest() + 1);
			    ph.local_push();
		        yyVal = new Integer(ruby.getSourceLine());
		    }
  break;
case 266:
					// line 1155 "parse.y"
  {
		        yyVal = nf.newClass(((RubyId)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[-3+yyTop]));
		        ((NODE)yyVal).nd_set_line(((Integer)yyVals[-2+yyTop]).intValue());
		        ph.local_pop();
			    ph.setClassNest(ph.getClassNest() - 1);
		    }
  break;
case 267:
					// line 1162 "parse.y"
  {
			    yyVal = new Integer(ph.getInDef());
		        ph.setInDef(0);
		    }
  break;
case 268:
					// line 1167 "parse.y"
  {
		        yyVal = new Integer(ph.getInSingle());
		        ph.setInSingle(0);
			    ph.setClassNest(ph.getClassNest() - 1);
			    ph.local_push();
		    }
  break;
case 269:
					// line 1175 "parse.y"
  {
		        yyVal = nf.newSClass(((NODE)yyVals[-5+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-5+yyTop]));
		        ph.local_pop();
			    ph.setClassNest(ph.getClassNest() - 1);
		        ph.setInDef(((Integer)yyVals[-4+yyTop]).intValue());
		        ph.setInSingle(((Integer)yyVals[-2+yyTop]).intValue());
		    }
  break;
case 270:
					// line 1184 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("module definition in method body");
			    ph.setClassNest(ph.getClassNest() + 1);
			    ph.local_push();
		        yyVal = new Integer(ruby.getSourceLine());
		    }
  break;
case 271:
					// line 1193 "parse.y"
  {
		        yyVal = nf.newModule(((RubyId)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        ((NODE)yyVal).nd_set_line(((Integer)yyVals[-2+yyTop]).intValue());
		        ph.local_pop();
			    ph.setClassNest(ph.getClassNest() - 1);
		    }
  break;
case 272:
					// line 1200 "parse.y"
  {
			    if (ph.isInDef() || ph.isInSingle())
			        yyerror("nested method definition");
			    yyVal = ph.getCurMid();
                ph.setCurMid(((RubyId)yyVals[0+yyTop]));
			    ph.setInDef(ph.getInDef() + 1);
			    ph.local_push();
		    }
  break;
case 273:
					// line 1214 "parse.y"
  {
		        if (((NODE)yyVals[-3+yyTop]) != null)
                    yyVals[-4+yyTop] = nf.newRescue(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-2+yyTop]));
			    else if (((NODE)yyVals[-2+yyTop]) != null) {
			        ph.rb_warn("else without rescue is useless");
			        yyVals[-4+yyTop] = ph.block_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
			    }
			    if (((NODE)yyVals[-1+yyTop]) != null)
                    yyVals[-4+yyTop] = nf.newEnsure(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));

		        /* NOEX_PRIVATE for toplevel */
			    yyVal = nf.newDefn(((RubyId)yyVals[-7+yyTop]), ((NODE)yyVals[-5+yyTop]), ((NODE)yyVals[-4+yyTop]), ph.getClassNest() !=0 ? NODE.NOEX_PUBLIC : NODE.NOEX_PRIVATE);
			    if (((RubyId)yyVals[-7+yyTop]).is_attrset_id())
                    ((NODE)yyVal).nd_noex(NODE.NOEX_PUBLIC);
		        ph.fixpos(yyVal, ((NODE)yyVals[-5+yyTop]));
		        ph.local_pop();
			    ph.setInDef(ph.getInDef() - 1);
			    ph.setCurMid(((RubyId)yyVals[-6+yyTop]));
		    }
  break;
case 274:
					// line 1233 "parse.y"
  {ph.setLexState(LexState.EXPR_FNAME);}
  break;
case 275:
					// line 1234 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
                ph.setInSingle(ph.getInSingle() + 1);
			    ph.local_push();
		        ph.setLexState(LexState.EXPR_END); /* force for args */
		    }
  break;
case 276:
					// line 1246 "parse.y"
  {
		        if (((NODE)yyVals[-3+yyTop]) != null)
                    yyVals[-4+yyTop] = nf.newRescue(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-2+yyTop]));
			    else if (((NODE)yyVals[-2+yyTop]) != null) {
			        ph.rb_warn("else without rescue is useless");
			        yyVals[-4+yyTop] = ph.block_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
			    }
			    if (((NODE)yyVals[-1+yyTop]) != null) yyVals[-4+yyTop] = nf.newEnsure(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));

			    yyVal = nf.newDefs(((NODE)yyVals[-10+yyTop]), ((RubyId)yyVals[-7+yyTop]), ((NODE)yyVals[-5+yyTop]), ((NODE)yyVals[-4+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-10+yyTop]));
		        ph.local_pop();
			    ph.setInSingle(ph.getInSingle() - 1);
		    }
  break;
case 277:
					// line 1261 "parse.y"
  {
			    yyVal = nf.newBreak();
		    }
  break;
case 278:
					// line 1265 "parse.y"
  {
			    yyVal = nf.newNext();
		    }
  break;
case 279:
					// line 1269 "parse.y"
  {
			    yyVal = nf.newRedo();
		    }
  break;
case 280:
					// line 1273 "parse.y"
  {
			    yyVal = nf.newRetry();
		    }
  break;
case 287:
					// line 1288 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = nf.newIf(ph.cond(((NODE)yyVals[-3+yyTop])), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 289:
					// line 1296 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 293:
					// line 1305 "parse.y"
  {
		        yyVal = new Integer(1); /*XXX (NODE*)1;*/
		    }
  break;
case 294:
					// line 1309 "parse.y"
  {
		        yyVal = new Integer(1); /*XXX (NODE*)1;*/
		    }
  break;
case 295:
					// line 1313 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 296:
					// line 1318 "parse.y"
  {
		        yyVal = ph.dyna_push();
		    }
  break;
case 297:
					// line 1324 "parse.y"
  {
			    yyVal = nf.newIter(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-2+yyTop])!=null?((NODE)yyVals[-2+yyTop]):((NODE)yyVals[-1+yyTop]));
			    ph.dyna_pop(((RubyVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 298:
					// line 1331 "parse.y"
  {
			    if (((NODE)yyVals[-1+yyTop]) != null && ((NODE)yyVals[-1+yyTop]).nd_type() == NODE.NODE_BLOCK_PASS) {
			        ph.rb_compile_error("both block arg and actual block given");
			    }
			    ((NODE)yyVals[0+yyTop]).nd_iter(((NODE)yyVals[-1+yyTop]));
			    yyVal = ((NODE)yyVals[0+yyTop]);
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 299:
					// line 1340 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 300:
					// line 1345 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 301:
					// line 1351 "parse.y"
  {
			    yyVal = ph.new_fcall(((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 302:
					// line 1356 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 303:
					// line 1362 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-3+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 304:
					// line 1368 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[-2+yyTop]));
			    yyVal = ph.new_call(((NODE)yyVals[-2+yyTop]), ((RubyId)yyVals[0+yyTop]), null);
		    }
  break;
case 305:
					// line 1373 "parse.y"
  {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle() && !ph.isInDefined())
			        yyerror("super called outside of method");
			    yyVal = ph.new_super(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 306:
					// line 1379 "parse.y"
  {
			    if (!ph.isCompileForEval() && !ph.isInDef() && !ph.isInSingle() && !ph.isInDefined())
			        yyerror("super called outside of method");
			    yyVal = nf.newZSuper();
		    }
  break;
case 307:
					// line 1386 "parse.y"
  {
		        yyVal = ph.dyna_push();
		    }
  break;
case 308:
					// line 1391 "parse.y"
  {
			    yyVal = nf.newIter(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-1+yyTop]));
			    ph.dyna_pop(((RubyVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 309:
					// line 1397 "parse.y"
  {
		        yyVal = ph.dyna_push();
		    }
  break;
case 310:
					// line 1402 "parse.y"
  {
			    yyVal = nf.newIter(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-1+yyTop]));
			    ph.dyna_pop(((RubyVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 311:
					// line 1411 "parse.y"
  {
			    yyVal = nf.newWhen(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 313:
					// line 1417 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = ph.list_append(((NODE)yyVals[-3+yyTop]), nf.newWhen(((NODE)yyVals[0+yyTop]), null, null));
		    }
  break;
case 314:
					// line 1422 "parse.y"
  {
			    ph.value_expr(((NODE)yyVals[0+yyTop]));
			    yyVal = nf.newList(nf.newWhen(((NODE)yyVals[0+yyTop]), null, null));
		    }
  break;
case 319:
					// line 1434 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 321:
					// line 1442 "parse.y"
  {
		        if (((NODE)yyVals[-3+yyTop]) != null) {
		            yyVals[-3+yyTop] = ph.node_assign(((NODE)yyVals[-3+yyTop]), nf.newGVar(ruby.intern("$!")));
			        yyVals[-1+yyTop] = ph.block_append(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
			    }
			    yyVal = nf.newResBody(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        ph.fixpos(yyVal, ((NODE)yyVals[-4+yyTop])!=null?((NODE)yyVals[-4+yyTop]):((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 324:
					// line 1454 "parse.y"
  {
			    if (((NODE)yyVals[0+yyTop]) != null)
			        yyVal = ((NODE)yyVals[0+yyTop]);
			    else
			        /* place holder */
			    yyVal = nf.newNil();
		    }
  break;
case 326:
					// line 1464 "parse.y"
  {
			    yyVal = ((RubyId)yyVals[0+yyTop]).toSymbol();
		    }
  break;
case 328:
					// line 1470 "parse.y"
  {
			    yyVal = nf.newStr(((VALUE)yyVals[0+yyTop]));
		    }
  break;
case 330:
					// line 1475 "parse.y"
  {
		        if (((NODE)yyVals[-1+yyTop]).nd_type() == NODE.NODE_DSTR) {
			        ph.list_append(((NODE)yyVals[-1+yyTop]), nf.newStr(((VALUE)yyVals[0+yyTop])));
			    } else {
			        ((RubyString)((NODE)yyVals[-1+yyTop]).nd_lit()).m_concat((RubyString)((VALUE)yyVals[0+yyTop]));
			    }
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 331:
					// line 1484 "parse.y"
  {
		        if (((NODE)yyVals[-1+yyTop]).nd_type() == NODE.NODE_STR) {
			        yyVal = nf.newDStr(((NODE)yyVals[-1+yyTop]).nd_lit());
			    } else {
			        yyVal = ((NODE)yyVals[-1+yyTop]);
			    }
			    ((NODE)yyVals[0+yyTop]).nd_head(nf.newStr(((NODE)yyVals[0+yyTop]).nd_lit()));
			    ((NODE)yyVals[0+yyTop]).nd_set_type(NODE.NODE_ARRAY);
			    ph.list_concat(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 332:
					// line 1496 "parse.y"
  {
		        ph.setLexState(LexState.EXPR_END);
			    yyVal = ((RubyId)yyVals[0+yyTop]);
		    }
  break;
case 344:
					// line 1514 "parse.y"
  {yyVal = ph.newId(kNIL);}
  break;
case 345:
					// line 1515 "parse.y"
  {yyVal = ph.newId(kSELF);}
  break;
case 346:
					// line 1516 "parse.y"
  {yyVal = ph.newId(kTRUE);}
  break;
case 347:
					// line 1517 "parse.y"
  {yyVal = ph.newId(kFALSE);}
  break;
case 348:
					// line 1518 "parse.y"
  {yyVal = ph.newId(k__FILE__);}
  break;
case 349:
					// line 1519 "parse.y"
  {yyVal = ph.newId(k__LINE__);}
  break;
case 350:
					// line 1522 "parse.y"
  {
			    yyVal = ph.gettable(((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 353:
					// line 1530 "parse.y"
  {
			    yyVal = null;
		    }
  break;
case 354:
					// line 1534 "parse.y"
  {
			    ph.setLexState(LexState.EXPR_BEG);
		    }
  break;
case 355:
					// line 1538 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 356:
					// line 1541 "parse.y"
  {yyerrok(); yyVal = null;}
  break;
case 357:
					// line 1544 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-2+yyTop]);
			    ph.setLexState(LexState.EXPR_BEG);
		    }
  break;
case 358:
					// line 1549 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 359:
					// line 1554 "parse.y"
  {
                /* +++*/
			    yyVal = ph.block_append(nf.newArgs(yyVals[-5+yyTop], ((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 360:
					// line 1559 "parse.y"
  {
                /* +++*/
			    yyVal = ph.block_append(nf.newArgs(yyVals[-3+yyTop], ((NODE)yyVals[-1+yyTop]), new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 361:
					// line 1564 "parse.y"
  {
                /* +++*/
			    yyVal = ph.block_append(nf.newArgs(yyVals[-3+yyTop], null, ((RubyId)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 362:
					// line 1569 "parse.y"
  {
                /* +++*/
			    yyVal = ph.block_append(nf.newArgs(yyVals[-1+yyTop], null, new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 363:
					// line 1574 "parse.y"
  {
			    yyVal = ph.block_append(nf.newArgs(null, ((NODE)yyVals[-3+yyTop]), ((RubyId)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 364:
					// line 1578 "parse.y"
  {
			    yyVal = ph.block_append(nf.newArgs(null, ((NODE)yyVals[-1+yyTop]), new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 365:
					// line 1582 "parse.y"
  {
			    yyVal = ph.block_append(nf.newArgs(null, null, ((RubyId)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 366:
					// line 1586 "parse.y"
  {
			    yyVal = ph.block_append(nf.newArgs(null, null, new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 367:
					// line 1590 "parse.y"
  {
			    yyVal = nf.newArgs(null, null, new Integer(-1));
		    }
  break;
case 368:
					// line 1595 "parse.y"
  {
			    yyerror("formal argument cannot be a constant");
		    }
  break;
case 369:
					// line 1599 "parse.y"
  {
                yyerror("formal argument cannot be an instance variable");
		    }
  break;
case 370:
					// line 1603 "parse.y"
  {
                yyerror("formal argument cannot be a global variable");
		    }
  break;
case 371:
					// line 1607 "parse.y"
  {
                yyerror("formal argument cannot be a class variable");
		    }
  break;
case 372:
					// line 1611 "parse.y"
  {
			    if (!((RubyId)yyVals[0+yyTop]).is_local_id())
			        yyerror("formal argument must be local variable");
			    else if (ph.local_id(((RubyId)yyVals[0+yyTop])))
			        yyerror("duplicate argument name");
			    ph.local_cnt(((RubyId)yyVals[0+yyTop]));
			    yyVal = new Integer(1);
		    }
  break;
case 374:
					// line 1622 "parse.y"
  {
			    yyVal = new Integer(((Integer)yyVal).intValue() + 1);
		    }
  break;
case 375:
					// line 1627 "parse.y"
  {
			    if (!((RubyId)yyVals[-2+yyTop]).is_local_id())
			        yyerror("formal argument must be local variable");
			    else if (ph.local_id(((RubyId)yyVals[-2+yyTop])))
			        yyerror("duplicate optional argument name");
			    yyVal = ph.assignable(((RubyId)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 376:
					// line 1636 "parse.y"
  {
			    yyVal = nf.newBlock(((NODE)yyVals[0+yyTop]));
			    ((NODE)yyVal).nd_end(((NODE)yyVal));
		    }
  break;
case 377:
					// line 1641 "parse.y"
  {
			    yyVal = ph.block_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 378:
					// line 1646 "parse.y"
  {
			    if (!((RubyId)yyVals[0+yyTop]).is_local_id())
			        yyerror("rest argument must be local variable");
			    else if (ph.local_id(((RubyId)yyVals[0+yyTop])))
			        yyerror("duplicate rest argument name");
			    yyVal = new Integer(ph.local_cnt(((RubyId)yyVals[0+yyTop])));
		    }
  break;
case 379:
					// line 1654 "parse.y"
  {
			    yyVal = new Integer(-2);
		    }
  break;
case 380:
					// line 1659 "parse.y"
  {
			    if (!((RubyId)yyVals[0+yyTop]).is_local_id())
			        yyerror("block argument must be local variable");
			    else if (ph.local_id(((RubyId)yyVals[0+yyTop])))
			        yyerror("duplicate block argument name");
			    yyVal = nf.newBlockArg(((RubyId)yyVals[0+yyTop]));
		    }
  break;
case 381:
					// line 1668 "parse.y"
  {
			    yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 383:
					// line 1674 "parse.y"
  {
			    if (((NODE)yyVals[0+yyTop]).nd_type() == NODE.NODE_SELF) {
			        yyVal = nf.newSelf();
			    } else {
			        yyVal = ((NODE)yyVals[0+yyTop]);
			    }
		    }
  break;
case 384:
					// line 1681 "parse.y"
  {ph.setLexState(LexState.EXPR_BEG);}
  break;
case 385:
					// line 1682 "parse.y"
  {
			    switch (((NODE)yyVals[-2+yyTop]).nd_type()) {
			        case NODE.NODE_STR:
			        case NODE.NODE_DSTR:
			        case NODE.NODE_XSTR:
			        case NODE.NODE_DXSTR:
			        case NODE.NODE_DREGX:
			        case NODE.NODE_LIT:
			        case NODE.NODE_ARRAY:
			        case NODE.NODE_ZARRAY:
			            yyerror("can't define single method for literals.");
			        default:
			            break;
			    }
			    yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 387:
					// line 1701 "parse.y"
  {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 388:
					// line 1705 "parse.y"
  {
			    if (((NODE)yyVals[-1+yyTop]).nd_alen()%2 != 0) {
			        yyerror("odd number list for Hash");
			    }
			    yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 390:
					// line 1714 "parse.y"
  {
			    yyVal = ph.list_concat(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 391:
					// line 1719 "parse.y"
  {
			    yyVal = ph.list_append(nf.newList(((NODE)yyVals[-2+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 411:
					// line 1749 "parse.y"
  {yyerrok();}
  break;
case 414:
					// line 1753 "parse.y"
  {yyerrok();}
  break;
case 415:
					// line 1756 "parse.y"
  {
		        yyVal = null; /*XXX 0;*/
		    }
  break;
					// line 2373 "-"
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
         62,   62,   62,   62,   62,   62,   62,   62,   62,   32,
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
          0,  345,  344,  346,  347,    0,    0,    0,   19,    0,
        349,  348,    0,    0,  341,  340,    0,  343,  337,  338,
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
        384,    0,  383,  350,  272,   75,   76,  138,  131,  141,
        125,  108,  109,   72,   73,    0,   79,   78,   77,    0,
          0,    0,    0,    0,  412,  411,    0,    0,    0,  413,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  290,
        291,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        203,    0,   27,  212,    0,  389,    0,    0,    0,   43,
          0,  305,   42,    0,   31,    0,    8,  407,    0,    0,
          0,    0,    0,    0,  237,    0,    0,    0,    0,    0,
          0,    0,    0,    0,  190,    0,    0,    0,  386,    0,
          0,   52,    0,  335,  334,  336,  332,  333,    0,   32,
          0,  330,  331,    3,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  296,  298,  309,  307,  251,    0,    0,    0,    0,
          0,    0,    0,    0,   56,  150,  301,   39,  249,    0,
          0,  354,  265,  353,    0,    0,  403,  402,  274,    0,
         80,    0,    0,  322,  282,    0,    0,    0,    0,    0,
          0,    0,    0,  414,    0,    0,    0,    0,    0,    0,
        262,    0,    0,  242,    0,  225,    0,    0,    0,    0,
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
        162,   59,   60,   61,  237,   63,   64,   65,   66,  233,
         68,   69,   70,  611,  612,  343,  708,  333,  494,  603,
        608,  242,  355,  507,  356,  563,  564,  223,  243,  362,
        214,   71,  463,  464,  323,   72,   73,  484,  485,  486,
        487,  660,  595,  247,  215,  216,  176,  217,  199,  570,
        319,  303,  182,   76,   77,   78,   79,  239,   80,   81,
        177,  218,  257,   83,  203,  514,  440,   89,  179,  446,
        489,  490,  491,    2,  188,  189,  377,  230,  167,  492,
        468,  229,  379,  391,  224,  544,  336,  191,  510,  618,
        192,  619,  519,  711,  472,  337,  469,  650,  325,  330,
        329,  475,  654,  448,  450,  449,  471,  326,
    };
  } /* End of class YyDgotoClass */

  protected static final class YySindexClass {

    public static final short yySindex [] = {            0,
          0,11890,12220, -237,  -60,14758,14486,11890,12716,12716,
      11756,    0,    0,14646,    0,    0,    0,    0,12324,12418,
         21,    0,    0,    0,    0,12716,14382,   58,    0,  -44,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,14088,14088,
        -60,11985,13304,14088,16337,14848,14182,14088,  -74,    0,
          0,    0,  388,  379, -135, 2273,   -4, -198,    0, -100,
          0,  -21,    0, -238,   71,    0,   86,16245,    0,  139,
          0, -141,    0,   14,  379,    0,    0,12716,   59,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  -14,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,  177,    0,    0,    0,  -26,
        153,  200,  259,  153,    0,    0,  116,   56,  273,    0,
      12716,12716,  300,  303,   21,   58,   62,    0,   68,    0,
          0,    0,   14,11890,14088,14088,14088,12520, 1702,   66,
          0,  306,    0,    0,  377,    0, -238, -141,12614,    0,
      12810,    0,    0,12810,    0,  233,    0,    0,  394,  318,
      11890,  136,   69,  136,    0,11985,  422,    0,  425,14088,
         58,  144,  378,  205,    0,  214,  348,  205,    0,   76,
          0,    0,    0,    0,    0,    0,    0,    0,  136,    0,
        136,    0,    0,    0,12128,12716,12716,12716,12716,12220,
      12716,12716,14088,14088,14088,14088,14088,14088,14088,14088,
      14088,14088,14088,14088,14088,14088,14088,14088,14088,14088,
      14088,14088,14088,14088,14088,14088,14088,14088,15185,15220,
      13304,    0,    0,    0,    0,    0,15255,15255,14088,13398,
      13398,11985,16337,  443,    0,    0,    0,    0,    0, -135,
        388,    0,    0,    0,11890,12716,    0,    0,    0,  255,
          0,14088,  241,    0,    0,11890,  246,14088,13500,11890,
         56,13594,  261,    0,  128,  128,  394,15527,15562,13304,
          0, 1834, 2273,    0,  478,    0,14088,15597,15632,13304,
      12912,    0,    0,13006,    0,    0,  531, -198,  534,   58,
         49,  557,    0,    0,    0,    0,14486,    0,14088,11890,
        482,15597,15632,  562,    0,    0, 1069,    0,13696,    0,
          0,    0,14088,    0,14088,    0,    0,    0,15667,15702,
      13304,  379, -135, -135, -135, -135,    0,    0,    0,  136,
       3573, 3573, 3573, 3573,  190,  190, 1475, 3145, 3573, 3573,
       2709, 2709,  691,  691, 4453,  190,  190,  208,  208,  236,
         41,   41,  136,  136,  136,  269,    0,    0,   21,    0,
          0,  271,    0,  280,   21,    0,  536,  -76,  -76,  -76,
          0,    0,   21,   21, 2273,14088, 2273,    0,  582,    0,
       2273,    0,    0,    0,  590,    0,    0,14088,  388,    0,
      12716,11890,  369,   16,15150,  578,    0,    0,    0,    0,
        330,  339,  615,11890,  388,  605,    0,    0,  608,    0,
        609,14486, 2273,  312,  612,    0,11890,  396,    0,  264,
          0, 2273,  241,  406,14088,  636,   33,    0,    0,    0,
          0,    0,    0,   21,    0,    0,   21,  600,12716,  352,
          0,    0, 2273,  269,  271,  280,  610,14088, 1702,    0,
          0,  662,14088, 1702,    0,    0,12912,  667,15255,15255,
        669,    0,    0,12716, 2273,  598,    0,    0,    0,14088,
       2273,   58,    0,    0,    0,  632,14088,14088,    0,    0,
      14088,14088,    0,    0,    0,    0,  398,    0,16153,11890,
          0,11890,11890,    0,    0,    0,    0, 2273,13790,    0,
       2273,    0,  116,  465,    0,  685,    0,14088,    0,    0,
         58,  -26,    0, -161,    0,    0,  402,    0,  615,    0,
          0,16337,   33,    0,14088,    0,11890,  486,    0,12716,
        487,    0,  488,    0, 2273,13892,11890,11890,11890,    0,
        128,  398, 1834,13108,    0, 1834, -198,   49,    0,   21,
         21,    0,   22,    0, 1069,    0,    0, 2273, 2273, 2273,
       2273,14088,    0,  629,  490,  491,  638,14088, 2273,11890,
          0,    0,    0,  255, 2273,  716,  241,  578,    0,    0,
        608,  722,  608,    0,   77,    0,    0,    0,11890,    0,
          0,  153,    0,    0,14088,  -97,  504,  505,    0,    0,
      14088,    0,  729,    0,    0, 2273,    0,    0,    0,    0,
       2273,  509,11890,    0,  396,    0, -161,    0,    0,15745,
      15780,13304,  -26,11890, 2273,    0,    0,    0,    0,    0,
      11890, 1834,    0,    0,  -26,  510,  608,    0,    0,    0,
        683,    0,  264,  515,    0,  241,    0,    0,    0,    0,
          0,  396,  516,    0,
    };
  } /* End of class YySindexClass */

  protected static final class YyRindexClass {

    public static final short yyRindex [] = {            0,
          0,  184,    0,    0,    0,    0,    0,  498,    0,    0,
        518,    0,    0,    0,    0,    0,    0,    0,11468, 6626,
       3546,    0,    0,    0,    0,    0,    0,13986,    0,    0,
          0,    0, 1794, 3015,    0,    0, 2143,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  215,  694,  668,  138,    0,    0,    0, 5839,    0,
          0,    0,  705, 1405, 5280,16018,12046, 6445,    0, 6142,
          0,15938,    0,10984,    0,    0,    0,  198,    0,    0,
          0,11069,    0,13202, 1733,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,   65,  648,  791,  870,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  880,
        904,  992,    0, 1001,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0, 5370,    0,    0,    0,  350,
          0,    0,    0,    0,    0,    0,  518,    0,  526,    0,
          0,    0, 6227, 6323, 5176,  755,    0,  151,    0,    0,
          0,  533,    0,  215,    0,    0,    0,    0,10887, 6806,
          0, 1718,    0,    0, 1718,    0, 5355, 5658,    0,    0,
        757,    0,    0,    0,    0,    0,    0,    0,14284,    0,
         89, 7108, 6711, 7193,    0,  215,    0,  109,    0,    0,
        707,  709,    0,  709,    0,  678,    0,  678,    0,    0,
        989,    0, 1289,    0,    0,    0,    0,    0, 7288,    0,
       7590,    0,    0,    0, 4719,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        694,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  215,  461,  462,    0,    0,    0,    0,    0,  430,
          0,    0,    0,    0,  159,    0,    0,    0,    0,  452,
          0,   26,  331,    0,    0,  421,11345,    0,    0,  432,
          0,    0,    0,    0,    0,    0,    0,    0,    0,  694,
          0, 1718, 5961,    0,    0,    0,    0,    0,    0,  694,
          0,    0,    0,    0,    0,    0,    0,  216,  474,  772,
        772,    0,    0,    0,    0,    0,    0,    0,    0,   89,
          0,    0,    0,    0,    0,  385,  707,    0,  721,    0,
          0,    0,   36,    0,  690,    0,    0,    0,    0,    0,
        694, 4916, 5854, 6078, 6338, 6562,    0,    0,    0, 7675,
       1283, 9664, 9817, 9893, 9235, 9421, 9969,10211,10024,10122,
      10329,10365, 8613, 8690,    0, 9512, 9588, 9068, 9144, 8992,
       8234, 8536, 7770, 8072, 8157, 4323, 3110, 3887,13202,    0,
       3451, 4418,    0, 4759, 3982,    0,    0,11553,11553,11659,
          0,    0, 4854, 4854, 1641,    0, 5476,    0,    0,    0,
      11413,    0,    0,    0,    0,    0,    0,    0,    0,    0,
          0,  159,    0,  772,    0,  497,    0,    0,    0,    0,
        622,    0,  496,  498,    0,  215,    0,    0,  215,    0,
        215,    0,   18,   34,   72,    0,  521,  555,    0,  555,
          0,10401,  555,    0,    0,   35,    0,    0,    0,    0,
          0,    0,  788,    0, 1434, 1437, 5265,    0,    0,    0,
          0,    0,11284, 2238, 2579, 2674,    0,    0,15868,    0,
          0, 1718,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,10477,    0,    0,  132,    0,    0,
         37,  707,  814,  981, 1188,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,11165,    0,    0,  159,
          0,  159,   89,    0,    0,    0,    0,16054,    0,    0,
      10513,    0,    0,    0,    0,    0,    0,    0,    0,    0,
        772,  350,    0,    0,    0,    0,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,  159,    0,    0,    0,
          0,    0,    0,    0,  171,    0,  415,  159,  159,  544,
          0, 5743, 1718,    0,    0, 1718,  506,  772,    0,   63,
         63,    0,    0,    0,  707,    0, 1541,10608,10644,10706,
      10742,    0,    0,    0,    0,    0,    0,    0,14574,  159,
          0,    0,    0,  452,  737,    0,  331,    0,    0,    0,
        215,  215,  215,    0,    0,  161,    0,  201,  498,    0,
          0,    0,    0,    0,    0,  555,    0,    0,    0,    0,
          0,    0,    0,    0,    0,10792,    0,    0,    0,    0,
      16112,    0,  498,    0,  555,    0,    0,    0,    0,    0,
          0,  694,  350,  421,  221,    0,    0,    0,    0,    0,
        159, 1718,    0,    0,  350,    0,  215,  100,  596,  601,
          0,    0,  555,    0,    0,  331,    0,    0,  230,    0,
          0,  555,    0,    0,
    };
  } /* End of class YyRindexClass */

  protected static final class YyGindexClass {

    public static final short yyGindex [] = {            0,
          0,    0,    0,    0,  663,    0,  162,  711, 1147,   -2,
        -16,  -24,    0,   93, -293, -319,    0, -551,    0,    0,
       -643, 1252,  602,    0,  524,    6, -340,  -69, -280, -213,
        103,  818,    0,  523,    0, -184,    0,  166,  342,  248,
       -548, -317,  129,    0,  -19, -273,    0,  573,  266,  196,
        778,    0,  552, 1238,  539,    0,  164, -187,  771,  -17,
        -11,  446,    0,   12,  125, -242,    0,   51,    8,    4,
       -493,  252,    0,    0,   11,  789,    0,    0,    0,    0,
          0,  -99,    0,  345,    0,    0, -179,    0, -316,    0,
          0,    0,    0,    0,    0,    9,    0,    0,    0,    0,
          0,    0,    0,    0,    0,    0,    0,    0,
    };
  } /* End of class YyGindexClass */

  protected static final class YyTableClass {

    public static final short yyTable [] = YyTables.yyTable();
  } /* End of class YyTableClass */

  protected static final class YyCheckClass {

    public static final short yyCheck [] = YyTables.yyCheck();
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


					// line 1760 "parse.y"

    // XXX +++
    // Helper Methods
    
    void yyerrok() {}
    
    // XXX ---
    
    // -----------------------------------------------------------------------
    // scanner stuff
    // -----------------------------------------------------------------------

    /*
     *  int yyerror(String msg) {
     *  char *p, *pe, *buf;
     *  int len, i;
     *  rb_compile_error("%s", msg);
     *  p = lex_p;
     *  while (lex_pbeg <= p) {
     *  if (*p == '\n') break;
     *  p--;
     *  }
     *  p++;
     *  pe = lex_p;
     *  while (pe < lex_pend) {
     *  if (*pe == '\n') break;
     *  pe++;
     *  }
     *  len = pe - p;
     *  if (len > 4) {
     *  buf = ALLOCA_N(char, len+2);
     *  MEMCPY(buf, p, char, len);
     *  buf[len] = '\0';
     *  rb_compile_error_append("%s", buf);
     *  i = lex_p - p;
     *  p = buf; pe = p + len;
     *  while (p < pe) {
     *  if (*p != '\t') *p = ' ';
     *  p++;
     *  }
     *  buf[i] = '^';
     *  buf[i+1] = '\0';
     *  rb_compile_error_append("%s", buf);
     *  }
     *  return 0;
     *  }
     */
    
    public NODE compileString(String f, RubyObject s, int line) {
        rs.setLexFileIo(false);
        rs.setLexGetsPtr(0);
        rs.setLexInput(s);
        rs.setLexP(0);
        rs.setLexPEnd(0);
        ruby.setSourceLine(line - 1);

        ph.setCompileForEval(ruby.getInEval());

        return yycompile(f, line);
    }

    public NODE compileJavaString(String f, String s, int len, int line) {
        return compileString(f, RubyString.m_newString(ruby, s, len), line);
    }

    public NODE compileFile(String f, RubyObject file, int start) {
        rs.setLexFileIo(true);
        rs.setLexInput(file);
        rs.setLexP(0);
        rs.setLexPEnd(0);
        ruby.setSourceLine(start - 1);

        return yycompile(f, start);
    }
    
    private void init_for_scanner(String s) {
        rs.setLexFileIo(false);
        rs.setLexGetsPtr(0);
        rs.setLexInput(RubyString.m_newString(ruby, s));
        rs.setLexP(0);
        rs.setLexPEnd(0);
        ruby.setSourceLine(0);
        ph.setCompileForEval(ruby.getInEval());
        ph.setRubyEndSeen(false); // is there an __end__{} statement?
        ph.setHeredocEnd(0);
        ph.setRubyInCompile(true);
    }
    
    /** This function compiles a given String into a NODE.
     *
     */
    public NODE yycompile(String f, int line) {
        RubyId sl_id = ruby.intern("SCRIPT_LINES__");
        if (!ph.isCompileForEval() && ruby.getSecurityLevel() == 0 && ruby.getClasses().getObjectClass().isConstantDefined(sl_id)) {
            RubyHash hash = (RubyHash)ruby.getClasses().getObjectClass().getConstant(sl_id);
            RubyString fName = RubyString.m_newString(ruby, f);
            
            // XXX +++
            RubyObject debugLines = ruby.getNil(); // = rb_hash_aref(hash, fName);
            // XXX ---
            
            if (debugLines.isNil()) {
                ph.setRubyDebugLines(RubyArray.m_newArray(ruby));
                hash.m_aset(fName, ph.getRubyDebugLines());
            } else {
                ph.setRubyDebugLines((RubyArray)debugLines);
            }
            
            if (line > 1) {
                RubyString str = RubyString.m_newString(ruby, null);
                while (line > 1) {
                    ph.getRubyDebugLines().m_push(str);
                    line--;
                }
            }
        }

        ph.setRubyEndSeen(false);   // is there an __end__{} statement?
        ph.setEvalTree(null);       // parser stores NODEs here
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
					// line 6725 "-"
