/*
 * parse.java - No description
 * Created on 10. September 2001, 17:51
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 */
package org.jruby.parser;

// created by jay 0.7 (c) 1998 Axel.Schreiner@informatik.uni-osnabrueck.de

					// line 5 "parse.y"
import java.util.*;
import java.io.*;

import org.jruby.*;
import org.jruby.original.*;
import org.jruby.util.*;

public class parse /*extends Ruby*/ implements lex_state, node_type, re_options {
    private NODE ruby_eval_tree_begin;
    private NODE ruby_eval_tree;

    private String ruby_sourcefile;		// current source file
    private int    ruby_sourceline;		// current line no.

    private /*lex_state*/int lex_state;

    private int class_nest;
    private int in_single;
    private int in_def;
    private boolean compile_for_eval;
    private ID cur_mid;

    private boolean in_defined;

    //XXX global variables
    private RVarmap ruby_dyna_vars;
    private VALUE ruby_class;
    private Hashtable rb_global_tbl = new Hashtable();

    // jruby
    private Ruby ruby; // Runtime
    private RubyOriginalMethods rom; // C rb_* methods

    private VALUE rb_cObject;

    public parse(Ruby ruby) {
    	this.ruby = ruby;
	this.rom = ruby.getOriginalMethods();

	rb_cObject = ruby.getObjectClass();
    }

    private long cond_stack;

    private void COND_PUSH(int i) {
        cond_stack <<= 1;
        cond_stack |= i & 1;
    }

    private void COND_POP() {
        cond_stack >>>= 1;
    }

    private void COND_LEXPOP() {
        boolean last = COND_P();
        cond_stack >>= 1;
        if (last) cond_stack |= 1;
    }

    private boolean COND_P() {
        return (cond_stack & 1) == 1;
    }

    private long cmdarg_stack;

    void CMDARG_PUSH(int i) {
        cmdarg_stack <<= 1;
        cmdarg_stack |= i & 1;
    }

    private void CMDARG_POP() {
        cmdarg_stack >>= 1;
    }

    private void CMDARG_LEXPOP() {
        boolean last = CMDARG_P();
        cmdarg_stack >>= 1;
        if (last) cmdarg_stack |= 1;
    }

    private boolean CMDARG_P() {
        return (cmdarg_stack & 1) == 1;
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
					// line 124 "-"
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
  public static final int tLPAREN_ARG = 344;
  public static final int tRPAREN = 345;
  public static final int tLBRACK = 346;
  public static final int tLBRACE = 347;
  public static final int tLBRACE_ARG = 348;
  public static final int tSTAR = 349;
  public static final int tAMPER = 350;
  public static final int tSYMBEG = 351;
  public static final int LAST_TOKEN = 352;
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
  protected static final String yyName [] = {
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
    "tLPAREN_ARG","tRPAREN","tLBRACK","tLBRACE","tLBRACE_ARG","tSTAR",
    "tAMPER","tSYMBEG","LAST_TOKEN",
  };

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
    boolean[] ok = new boolean[yyName.length];

    if ((n = yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyName.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyName[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyName.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyName[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = yyName[token];
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
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            yyToken = yyLex.advance() ? yyLex.token() : 0;
          }
          if ((yyN = yySindex[yyState]) != 0 && (yyN += yyToken) >= 0
              && yyN < yyTable.length && yyCheck[yyN] == yyToken) {
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
              yyerror("syntax error", yyExpecting(yyState));

            case 1: case 2:
              yyErrorFlag = 3;
              do {
                if ((yyN = yySindex[yyStates[yyTop]]) != 0
                    && (yyN += yyErrorCode) >= 0 && yyN < yyTable.length
                    && yyCheck[yyN] == yyErrorCode) {
                  yyState = yyTable[yyN];
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
        int yyV = yyTop + 1-yyLen[yyN];
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
case 1:
					// line 246 "parse.y"
  {
		        yyVal = ruby_dyna_vars;
			lex_state = EXPR_BEG;
                        top_local_init();
			if ((VALUE)ruby_class == rb_cObject) class_nest = 0;
			else class_nest = 1;
		    }
  break;
case 2:
					// line 254 "parse.y"
  {
			if (((NODE)yyVals[0+yyTop]) != null && !compile_for_eval) {
                            /* last expression should not be void */
			    if (nd_type(((NODE)yyVals[0+yyTop])) != NODE.NODE_BLOCK) void_expr(((NODE)yyVals[0+yyTop]));
			    else {
				NODE node = ((NODE)yyVals[0+yyTop]);
				while (node.nd_next() != null) {
				    node = node.nd_next();
				}
				void_expr(node.nd_head());
			    }
			}
			ruby_eval_tree = block_append(ruby_eval_tree, ((NODE)yyVals[0+yyTop]));
                        top_local_setup();
			class_nest = 0;
		        ruby_dyna_vars = ((RVarmap)yyVals[-1+yyTop]);
		    }
  break;
case 3:
					// line 273 "parse.y"
  {
			void_stmts(((NODE)yyVals[-1+yyTop]));
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 5:
					// line 280 "parse.y"
  {
			yyVal = newline_node(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 6:
					// line 284 "parse.y"
  {
			yyVal = block_append(((NODE)yyVals[-2+yyTop]), newline_node(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 7:
					// line 288 "parse.y"
  {
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 8:
					// line 292 "parse.y"
  {lex_state = EXPR_FNAME;}
  break;
case 9:
					// line 293 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("alias within method");
		        yyVal = NEW_ALIAS(((ID)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 10:
					// line 299 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("alias within method");
		        yyVal = NEW_VALIAS(((ID)yyVals[-1+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 11:
					// line 305 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("alias within method");
			String buf = "$" + (char)((NODE)yyVals[0+yyTop]).nd_nth();
		        yyVal = NEW_VALIAS(((ID)yyVals[-1+yyTop]), ID.rb_intern(buf, ruby));
		    }
  break;
case 12:
					// line 312 "parse.y"
  {
		        yyerror("can't make alias for the number variables");
		        yyVal = null; /*XXX 0*/
		    }
  break;
case 13:
					// line 317 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("undef within method");
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 14:
					// line 323 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_IF(cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), null);
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 15:
					// line 329 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_UNLESS(cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), null);
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 16:
					// line 335 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			if (((NODE)yyVals[-2+yyTop]) != null && nd_type(((NODE)yyVals[-2+yyTop])) == NODE.NODE_BEGIN) {
			    yyVal = NEW_WHILE(cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]).nd_body(), 0);
			}
			else {
			    yyVal = NEW_WHILE(cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), 1);
			}
		    }
  break;
case 17:
					// line 345 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			if (((NODE)yyVals[-2+yyTop]) != null && nd_type(((NODE)yyVals[-2+yyTop])) == NODE.NODE_BEGIN) {
			    yyVal = NEW_UNTIL(cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]).nd_body(), 0);
			}
			else {
			    yyVal = NEW_UNTIL(cond(((NODE)yyVals[0+yyTop])), ((NODE)yyVals[-2+yyTop]), 1);
			}
		    }
  break;
case 18:
					// line 355 "parse.y"
  {
			yyVal = NEW_RESCUE(((NODE)yyVals[-2+yyTop]), NEW_RESBODY(null,((NODE)yyVals[0+yyTop]),null), null);
		    }
  break;
case 19:
					// line 359 "parse.y"
  {
			if (in_def!=0 || in_single!=0) {
			    yyerror("BEGIN in method");
			}
			local_push();
		    }
  break;
case 20:
					// line 366 "parse.y"
  {
			ruby_eval_tree_begin = block_append(ruby_eval_tree_begin,
						            NEW_PREEXE(((NODE)yyVals[-1+yyTop])));
		        local_pop();
		        yyVal = null; /*XXX 0;*/
		    }
  break;
case 21:
					// line 373 "parse.y"
  {
			if (compile_for_eval && (in_def!=0 || in_single!=0)) {
			    yyerror("END in method; use at_exit");
			}

			yyVal = NEW_ITER(null, NEW_POSTEXE(), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 22:
					// line 381 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = node_assign(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 23:
					// line 386 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			((NODE)yyVals[-2+yyTop]).nd_value((NODE)yyVals[0+yyTop]);
			yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 24:
					// line 392 "parse.y"
  {
			yyVal = node_assign(((NODE)yyVals[-2+yyTop]), NEW_REXPAND(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 25:
					// line 396 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			((NODE)yyVals[-2+yyTop]).nd_value((NODE)yyVals[0+yyTop]);
			yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 27:
					// line 404 "parse.y"
  {
			if (!compile_for_eval && in_def==0 && in_single==0)
			    yyerror("return appeared outside of method");
			yyVal = NEW_RETURN(ret_args(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 28:
					// line 410 "parse.y"
  {
			yyVal = NEW_BREAK(ret_args(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 29:
					// line 414 "parse.y"
  {
			yyVal = NEW_NEXT(ret_args(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 31:
					// line 419 "parse.y"
  {
			yyVal = logop(NODE.NODE_AND, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 32:
					// line 423 "parse.y"
  {
			yyVal = logop(NODE.NODE_OR, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 33:
					// line 427 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_NOT(cond(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 34:
					// line 432 "parse.y"
  {
			yyVal = NEW_NOT(cond(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 39:
					// line 442 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 40:
					// line 447 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 41:
					// line 453 "parse.y"
  {
			yyVal = new_fcall(((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		   }
  break;
case 42:
					// line 458 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 43:
					// line 464 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 44:
					// line 470 "parse.y"
  {
			if (!compile_for_eval && in_def==0 && in_single==0)
			    yyerror("super called outside of method");
			yyVal = new_super(((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 45:
					// line 477 "parse.y"
  {
			yyVal = NEW_YIELD(ret_args(((NODE)yyVals[0+yyTop])));
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 47:
					// line 484 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 49:
					// line 490 "parse.y"
  {
			yyVal = NEW_MASGN(NEW_LIST(((NODE)yyVals[-1+yyTop])), null);
		    }
  break;
case 50:
					// line 495 "parse.y"
  {
			yyVal = NEW_MASGN(((NODE)yyVals[0+yyTop]), null);
		    }
  break;
case 51:
					// line 499 "parse.y"
  {
			yyVal = NEW_MASGN(list_append(((NODE)yyVals[-1+yyTop]),((NODE)yyVals[0+yyTop])), null);
		    }
  break;
case 52:
					// line 503 "parse.y"
  {
			yyVal = NEW_MASGN(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 53:
					// line 507 "parse.y"
  {
			yyVal = NEW_MASGN(((NODE)yyVals[-1+yyTop]), NODE.MINUS_ONE);
		    }
  break;
case 54:
					// line 511 "parse.y"
  {
			yyVal = NEW_MASGN(null, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 55:
					// line 515 "parse.y"
  {
			yyVal = NEW_MASGN(null, NODE.MINUS_ONE);
		    }
  break;
case 57:
					// line 521 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 58:
					// line 526 "parse.y"
  {
			yyVal = NEW_LIST(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 59:
					// line 530 "parse.y"
  {
			yyVal = list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 60:
					// line 535 "parse.y"
  {
			yyVal = assignable(((ID)yyVals[0+yyTop]), null);
		    }
  break;
case 61:
					// line 539 "parse.y"
  {
			yyVal = aryset(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 62:
					// line 543 "parse.y"
  {
			yyVal = attrset(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 63:
					// line 547 "parse.y"
  {
			yyVal = attrset(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 64:
					// line 551 "parse.y"
  {
			yyVal = attrset(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 65:
					// line 555 "parse.y"
  {
		        rb_backref_error(((NODE)yyVals[0+yyTop]));
			yyVal = null; /*XXX 0;*/
		    }
  break;
case 66:
					// line 561 "parse.y"
  {
			yyVal = assignable(((ID)yyVals[0+yyTop]), null);
		    }
  break;
case 67:
					// line 565 "parse.y"
  {
			yyVal = aryset(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 68:
					// line 569 "parse.y"
  {
			yyVal = attrset(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 69:
					// line 573 "parse.y"
  {
			yyVal = attrset(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 70:
					// line 577 "parse.y"
  {
			yyVal = attrset(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 71:
					// line 581 "parse.y"
  {
		        rb_backref_error(((NODE)yyVals[0+yyTop]));
			yyVal = null; /*XXX 0;*/
		    }
  break;
case 72:
					// line 587 "parse.y"
  {
			yyerror("class/module name must be CONSTANT");
		    }
  break;
case 77:
					// line 596 "parse.y"
  {
			lex_state = EXPR_END;
                        //XXX Integer->ID
			yyVal = new RubyId(ruby, ((Integer)yyVals[0+yyTop]).intValue());
		    }
  break;
case 78:
					// line 601 "parse.y"
  {
			lex_state = EXPR_END;
			yyVal = ((ID)yyVals[0+yyTop]);
		    }
  break;
case 81:
					// line 610 "parse.y"
  {
			yyVal = NEW_UNDEF(((ID)yyVals[0+yyTop]));
		    }
  break;
case 82:
					// line 613 "parse.y"
  {lex_state = EXPR_FNAME;}
  break;
case 83:
					// line 614 "parse.y"
  {
			yyVal = block_append(((NODE)yyVals[-3+yyTop]), NEW_UNDEF(((ID)yyVals[0+yyTop])));
		    }
  break;
case 84:
					// line 618 "parse.y"
  { yyVal = new Integer('|'); }
  break;
case 85:
					// line 619 "parse.y"
  { yyVal = new Integer('^'); }
  break;
case 86:
					// line 620 "parse.y"
  { yyVal = new Integer('&'); }
  break;
case 87:
					// line 621 "parse.y"
  { yyVal = new Integer(tCMP); }
  break;
case 88:
					// line 622 "parse.y"
  { yyVal = new Integer(tEQ); }
  break;
case 89:
					// line 623 "parse.y"
  { yyVal = new Integer(tEQQ); }
  break;
case 90:
					// line 624 "parse.y"
  { yyVal = new Integer(tMATCH); }
  break;
case 91:
					// line 625 "parse.y"
  { yyVal = new Integer('>'); }
  break;
case 92:
					// line 626 "parse.y"
  { yyVal = new Integer(tGEQ); }
  break;
case 93:
					// line 627 "parse.y"
  { yyVal = new Integer('<'); }
  break;
case 94:
					// line 628 "parse.y"
  { yyVal = new Integer(tLEQ); }
  break;
case 95:
					// line 629 "parse.y"
  { yyVal = new Integer(tLSHFT); }
  break;
case 96:
					// line 630 "parse.y"
  { yyVal = new Integer(tRSHFT); }
  break;
case 97:
					// line 631 "parse.y"
  { yyVal = new Integer('+'); }
  break;
case 98:
					// line 632 "parse.y"
  { yyVal = new Integer('-'); }
  break;
case 99:
					// line 633 "parse.y"
  { yyVal = new Integer('*'); }
  break;
case 100:
					// line 634 "parse.y"
  { yyVal = new Integer('*'); }
  break;
case 101:
					// line 635 "parse.y"
  { yyVal = new Integer('/'); }
  break;
case 102:
					// line 636 "parse.y"
  { yyVal = new Integer('%'); }
  break;
case 103:
					// line 637 "parse.y"
  { yyVal = new Integer(tPOW); }
  break;
case 104:
					// line 638 "parse.y"
  { yyVal = new Integer('~'); }
  break;
case 105:
					// line 639 "parse.y"
  { yyVal = new Integer(tUPLUS); }
  break;
case 106:
					// line 640 "parse.y"
  { yyVal = new Integer(tUMINUS); }
  break;
case 107:
					// line 641 "parse.y"
  { yyVal = new Integer(tAREF); }
  break;
case 108:
					// line 642 "parse.y"
  { yyVal = new Integer(tASET); }
  break;
case 109:
					// line 643 "parse.y"
  { yyVal = new Integer('`'); }
  break;
case 151:
					// line 654 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = node_assign(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 152:
					// line 658 "parse.y"
  {yyVal = assignable(((ID)yyVals[-1+yyTop]), null);}
  break;
case 153:
					// line 659 "parse.y"
  {
			if (((ID)yyVals[-2+yyTop]).intValue() == tOROP) {
			    ((NODE)yyVals[-1+yyTop]).nd_value((NODE)yyVals[0+yyTop]);
			    yyVal = NEW_OP_ASGN_OR(gettable(((ID)yyVals[-3+yyTop])), ((NODE)yyVals[-1+yyTop]));
			    if (((ID)yyVals[-3+yyTop]).is_instance_id()) {
				((NODE)yyVal).nd_aid((ID)yyVals[-3+yyTop]);
			    }
			}
			else if (((ID)yyVals[-2+yyTop]).intValue() == tANDOP) {
			    ((NODE)yyVals[-1+yyTop]).nd_value((NODE)yyVals[0+yyTop]);
			    yyVal = NEW_OP_ASGN_AND(gettable(((ID)yyVals[-3+yyTop])), ((NODE)yyVals[-1+yyTop]));
			}
			else {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
			    if (yyVal != null) {
				((NODE)yyVal).nd_value(call_op(gettable(((ID)yyVals[-3+yyTop])),((ID)yyVals[-2+yyTop]).intValue(),1,((NODE)yyVals[0+yyTop])));
			    }
			}
			fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 154:
					// line 680 "parse.y"
  {
			NODE args = NEW_LIST(((NODE)yyVals[0+yyTop]));

			list_append(((NODE)yyVals[-3+yyTop]), NEW_NIL());
			list_concat(args, ((NODE)yyVals[-3+yyTop]));
			yyVal = NEW_OP_ASGN1(((NODE)yyVals[-5+yyTop]), fixop(((ID)yyVals[-1+yyTop])), args);
		        fixpos(yyVal, ((NODE)yyVals[-5+yyTop]));
		    }
  break;
case 155:
					// line 689 "parse.y"
  {
			yyVal = NEW_OP_ASGN2(((NODE)yyVals[-4+yyTop]), ((ID)yyVals[-2+yyTop]), fixop(((ID)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 156:
					// line 694 "parse.y"
  {
			yyVal = NEW_OP_ASGN2(((NODE)yyVals[-4+yyTop]), ((ID)yyVals[-2+yyTop]), fixop(((ID)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 157:
					// line 699 "parse.y"
  {
			yyVal = NEW_OP_ASGN2(((NODE)yyVals[-4+yyTop]), ((ID)yyVals[-2+yyTop]), fixop(((ID)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 158:
					// line 704 "parse.y"
  {
		        rb_backref_error(((NODE)yyVals[-2+yyTop]));
			yyVal = null; /*XXX 0*/
		    }
  break;
case 159:
					// line 709 "parse.y"
  {
			yyVal = NEW_DOT2(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 160:
					// line 713 "parse.y"
  {
			yyVal = NEW_DOT3(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 161:
					// line 717 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '+', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 162:
					// line 721 "parse.y"
  {
		        yyVal = call_op(((NODE)yyVals[-2+yyTop]), '-', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 163:
					// line 725 "parse.y"
  {
		        yyVal = call_op(((NODE)yyVals[-2+yyTop]), '*', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 164:
					// line 729 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '/', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 165:
					// line 733 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '%', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 166:
					// line 737 "parse.y"
  {
			boolean need_negate = false;

			if (nd_type(((NODE)yyVals[-2+yyTop])) == NODE.NODE_LIT) {

			    switch (TYPE(((NODE)yyVals[-2+yyTop]).nd_lit())) {
			      case T_FIXNUM:
			      case T_FLOAT:
			      case T_BIGNUM:
				if (RTEST(rb_funcall(((NODE)yyVals[-2+yyTop]).nd_lit(),'<',1,INT2FIX(0)))) {
				    ((NODE)yyVals[-2+yyTop]).nd_lit(rb_funcall(((NODE)yyVals[-2+yyTop]).nd_lit(),ID.rb_intern("-@", ruby),0,null));
				    need_negate = true;
				}
			      default:
				break;
			    }
			}
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tPOW, 1, ((NODE)yyVals[0+yyTop]));
			if (need_negate) {
			    yyVal = call_op(((NODE)yyVal), tUMINUS, 0, null);
			}
		    }
  break;
case 167:
					// line 760 "parse.y"
  {
			if (((NODE)yyVals[0+yyTop]) != null && nd_type(((NODE)yyVals[0+yyTop])) == NODE.NODE_LIT) {
			    yyVal = ((NODE)yyVals[0+yyTop]);
			}
			else {
			    yyVal = call_op(((NODE)yyVals[0+yyTop]), tUPLUS, 0, null);
			}
		    }
  break;
case 168:
					// line 769 "parse.y"
  {
			if (((NODE)yyVals[0+yyTop]) != null && nd_type(((NODE)yyVals[0+yyTop])) == NODE.NODE_LIT && FIXNUM_P(((NODE)yyVals[0+yyTop]).nd_lit())) {
			    long i = FIX2LONG(((NODE)yyVals[0+yyTop]).nd_lit());

			    ((NODE)yyVals[0+yyTop]).nd_lit(INT2FIX(-i));
			    yyVal = ((NODE)yyVals[0+yyTop]);
			}
			else {
			    yyVal = call_op(((NODE)yyVals[0+yyTop]), tUMINUS, 0, null);
			}
		    }
  break;
case 169:
					// line 781 "parse.y"
  {
		        yyVal = call_op(((NODE)yyVals[-2+yyTop]), '|', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 170:
					// line 785 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '^', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 171:
					// line 789 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '&', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 172:
					// line 793 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tCMP, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 173:
					// line 797 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '>', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 174:
					// line 801 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tGEQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 175:
					// line 805 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), '<', 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 176:
					// line 809 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tLEQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 177:
					// line 813 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tEQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 178:
					// line 817 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tEQQ, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 179:
					// line 821 "parse.y"
  {
			yyVal = NEW_NOT(call_op(((NODE)yyVals[-2+yyTop]), tEQ, 1, ((NODE)yyVals[0+yyTop])));
		    }
  break;
case 180:
					// line 825 "parse.y"
  {
			yyVal = match_gen(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 181:
					// line 829 "parse.y"
  {
			yyVal = NEW_NOT(match_gen(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop])));
		    }
  break;
case 182:
					// line 833 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_NOT(cond(((NODE)yyVals[0+yyTop])));
		    }
  break;
case 183:
					// line 838 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[0+yyTop]), '~', 0, null);
		    }
  break;
case 184:
					// line 842 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tLSHFT, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 185:
					// line 846 "parse.y"
  {
			yyVal = call_op(((NODE)yyVals[-2+yyTop]), tRSHFT, 1, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 186:
					// line 850 "parse.y"
  {
			yyVal = logop(NODE.NODE_AND, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 187:
					// line 854 "parse.y"
  {
			yyVal = logop(NODE.NODE_OR, ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 188:
					// line 857 "parse.y"
  {in_defined = true;}
  break;
case 189:
					// line 858 "parse.y"
  {
		        in_defined = false;
			yyVal = NEW_DEFINED(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 190:
					// line 863 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			yyVal = NEW_IF(cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 191:
					// line 869 "parse.y"
  {
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 193:
					// line 875 "parse.y"
  {
			yyVal = NEW_LIST(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 194:
					// line 879 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 195:
					// line 883 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 196:
					// line 888 "parse.y"
  {
			yyVal = NEW_LIST(NEW_HASH(((NODE)yyVals[-1+yyTop])));
		    }
  break;
case 197:
					// line 892 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = NEW_RESTARY(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 198:
					// line 898 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 199:
					// line 902 "parse.y"
  {
			yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 200:
					// line 906 "parse.y"
  {
			yyVal = NEW_LIST(((NODE)yyVals[-2+yyTop]));
		    }
  break;
case 201:
					// line 910 "parse.y"
  {
			yyVal = list_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
		    }
  break;
case 204:
					// line 918 "parse.y"
  {
			yyVal = NEW_LIST(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 205:
					// line 922 "parse.y"
  {
			yyVal = arg_blk_pass(((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 206:
					// line 926 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 207:
					// line 932 "parse.y"
  {
			yyVal = NEW_LIST(NEW_HASH(((NODE)yyVals[-1+yyTop])));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 208:
					// line 937 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(NEW_LIST(NEW_HASH(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 209:
					// line 943 "parse.y"
  {
			yyVal = list_append(((NODE)yyVals[-3+yyTop]), NEW_HASH(((NODE)yyVals[-1+yyTop])));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 210:
					// line 948 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(list_append(((NODE)yyVals[-6+yyTop]), NEW_HASH(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 211:
					// line 954 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(NEW_RESTARGS(((NODE)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 213:
					// line 961 "parse.y"
  {
			yyVal = arg_blk_pass(list_concat(NEW_LIST(((NODE)yyVals[-3+yyTop])),((NODE)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 214:
					// line 965 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(NEW_LIST(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 215:
					// line 972 "parse.y"
  {
			value_expr(((NODE)yyVals[-6+yyTop]));
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(list_concat(((NODE)yyVals[-6+yyTop]),((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 216:
					// line 979 "parse.y"
  {
			yyVal = NEW_LIST(NEW_HASH(((NODE)yyVals[-1+yyTop])));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 217:
					// line 984 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(NEW_LIST(NEW_HASH(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 218:
					// line 990 "parse.y"
  {
			yyVal = list_append(NEW_LIST(((NODE)yyVals[-3+yyTop])), NEW_HASH(((NODE)yyVals[-1+yyTop])));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 219:
					// line 995 "parse.y"
  {
			value_expr(((NODE)yyVals[-5+yyTop]));
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = list_append(list_concat(((NODE)yyVals[-5+yyTop]),((NODE)yyVals[-3+yyTop])), NEW_HASH(((NODE)yyVals[-1+yyTop])));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 220:
					// line 1002 "parse.y"
  {
			value_expr(((NODE)yyVals[-6+yyTop]));
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(list_append(NEW_LIST(((NODE)yyVals[-6+yyTop])), NEW_HASH(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 221:
					// line 1009 "parse.y"
  {
			value_expr(((NODE)yyVals[-8+yyTop]));
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_concat(list_append(list_concat(NEW_LIST(((NODE)yyVals[-8+yyTop])), ((NODE)yyVals[-6+yyTop])), NEW_HASH(((NODE)yyVals[-4+yyTop]))), ((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 222:
					// line 1016 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = arg_blk_pass(NEW_RESTARGS(((NODE)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 224:
					// line 1022 "parse.y"
  {
			yyVal = new Long(cmdarg_stack);
			CMDARG_PUSH(1);
		    }
  break;
case 225:
					// line 1027 "parse.y"
  {
			/* CMDARG_POP() */
		        cmdarg_stack = ((Long)yyVals[-1+yyTop]).longValue();
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 227:
					// line 1034 "parse.y"
  {lex_state = EXPR_ENDARG;}
  break;
case 228:
					// line 1035 "parse.y"
  {
		        rb_warning("%s (...) interpreted as method call",
		                   ID.rb_id2name_last_id(ruby));
			yyVal = null;
		    }
  break;
case 229:
					// line 1040 "parse.y"
  {lex_state = EXPR_ENDARG;}
  break;
case 230:
					// line 1041 "parse.y"
  {
		        rb_warning("%s (...) interpreted as method call",
		                   ID.rb_id2name_last_id(ruby));
			yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 231:
					// line 1048 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_BLOCK_PASS(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 232:
					// line 1054 "parse.y"
  {
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 234:
					// line 1060 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_LIST(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 235:
					// line 1065 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 236:
					// line 1071 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 237:
					// line 1076 "parse.y"
  {
			yyVal = NEW_REXPAND(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 238:
					// line 1081 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = list_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 239:
					// line 1086 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = arg_concat(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 240:
					// line 1091 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 241:
					// line 1097 "parse.y"
  {
			yyVal = NEW_LIT(((VALUE)yyVals[0+yyTop]));
		    }
  break;
case 243:
					// line 1102 "parse.y"
  {
			yyVal = NEW_XSTR(((VALUE)yyVals[0+yyTop]));
		    }
  break;
case 248:
					// line 1110 "parse.y"
  {
			yyVal = NEW_VCALL(((ID)yyVals[0+yyTop]));
		    }
  break;
case 249:
					// line 1119 "parse.y"
  {
			if (((NODE)yyVals[-3+yyTop]) == null && ((NODE)yyVals[-2+yyTop]) == null && ((NODE)yyVals[-1+yyTop]) == null)
			    yyVal = NEW_BEGIN(((NODE)yyVals[-4+yyTop]));
			else {
			    if (((NODE)yyVals[-3+yyTop]) != null) yyVals[-4+yyTop] = NEW_RESCUE(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-2+yyTop]));
			    else if (((NODE)yyVals[-2+yyTop]) != null) {
				rb_warn("else without rescue is useless");
				yyVals[-4+yyTop] = block_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
			    }
			    if (((NODE)yyVals[-1+yyTop]) != null) yyVals[-4+yyTop] = NEW_ENSURE(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
			    yyVal = ((NODE)yyVals[-4+yyTop]);
			}
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 250:
					// line 1133 "parse.y"
  {lex_state = EXPR_ENDARG;}
  break;
case 251:
					// line 1134 "parse.y"
  {
		        rb_warning("%s (...) interpreted as command call", ID.rb_id2name_last_id(ruby));
			yyVal = ((NODE)yyVals[-2+yyTop]);
		    }
  break;
case 252:
					// line 1139 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 253:
					// line 1143 "parse.y"
  {
			value_expr(((NODE)yyVals[-2+yyTop]));
			yyVal = NEW_COLON2(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]));
		    }
  break;
case 254:
					// line 1148 "parse.y"
  {
			yyVal = NEW_COLON3(((ID)yyVals[0+yyTop]));
		    }
  break;
case 255:
					// line 1152 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = NEW_CALL(((NODE)yyVals[-3+yyTop]), new RubyId(ruby, tAREF), ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 256:
					// line 1157 "parse.y"
  {
		        if (((NODE)yyVals[-1+yyTop]) == null) {
			    yyVal = NEW_ZARRAY(); /* zero length array*/
			}
			else {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
			}
		    }
  break;
case 257:
					// line 1166 "parse.y"
  {
			yyVal = NEW_HASH(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 258:
					// line 1170 "parse.y"
  {
			if (!compile_for_eval && in_def==0 && in_single==0)
			    yyerror("return appeared outside of method");
			yyVal = NEW_RETURN(null);
		    }
  break;
case 259:
					// line 1176 "parse.y"
  {
			value_expr(((NODE)yyVals[-1+yyTop]));
			yyVal = NEW_YIELD(ret_args(((NODE)yyVals[-1+yyTop])));
		    }
  break;
case 260:
					// line 1181 "parse.y"
  {
			yyVal = NEW_YIELD(null);
		    }
  break;
case 261:
					// line 1185 "parse.y"
  {
			yyVal = NEW_YIELD(null);
		    }
  break;
case 262:
					// line 1188 "parse.y"
  {in_defined = true;}
  break;
case 263:
					// line 1189 "parse.y"
  {
		        in_defined = false;
			yyVal = NEW_DEFINED(((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 264:
					// line 1194 "parse.y"
  {
			((NODE)yyVals[0+yyTop]).nd_iter(NEW_FCALL(((ID)yyVals[-1+yyTop]), null));
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 266:
					// line 1200 "parse.y"
  {
			if (((NODE)yyVals[-1+yyTop]) != null && nd_type(((NODE)yyVals[-1+yyTop])) == NODE.NODE_BLOCK_PASS) {
			    rb_compile_error("both block arg and actual block given");
			}
			((NODE)yyVals[0+yyTop]).nd_iter(((NODE)yyVals[-1+yyTop]));
			yyVal = ((NODE)yyVals[0+yyTop]);
		        fixpos(yyVal, ((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 267:
					// line 1212 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			yyVal = NEW_IF(cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 268:
					// line 1221 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			yyVal = NEW_UNLESS(cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 269:
					// line 1226 "parse.y"
  {COND_PUSH(1);}
  break;
case 270:
					// line 1226 "parse.y"
  {COND_POP();}
  break;
case 271:
					// line 1229 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			yyVal = NEW_WHILE(cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-1+yyTop]), 1);
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 272:
					// line 1234 "parse.y"
  {COND_PUSH(1);}
  break;
case 273:
					// line 1234 "parse.y"
  {COND_POP();}
  break;
case 274:
					// line 1237 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			yyVal = NEW_UNTIL(cond(((NODE)yyVals[-4+yyTop])), ((NODE)yyVals[-1+yyTop]), 1);
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop]));
		    }
  break;
case 275:
					// line 1245 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = NEW_CASE(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 276:
					// line 1251 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 277:
					// line 1254 "parse.y"
  {COND_PUSH(1);}
  break;
case 278:
					// line 1254 "parse.y"
  {COND_POP();}
  break;
case 279:
					// line 1257 "parse.y"
  {
			value_expr(((NODE)yyVals[-4+yyTop]));
			yyVal = NEW_FOR(((NODE)yyVals[-7+yyTop]), ((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-7+yyTop]));
		    }
  break;
case 280:
					// line 1263 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("class definition in method body");
			class_nest++;
			local_push();
		        yyVal = new Integer(ruby_sourceline);
		    }
  break;
case 281:
					// line 1272 "parse.y"
  {
		        yyVal = NEW_CLASS(((ID)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[-3+yyTop]));
		        nd_set_line(((NODE)yyVal), ((Integer)yyVals[-2+yyTop]).intValue());
		        local_pop();
			class_nest--;
		    }
  break;
case 282:
					// line 1279 "parse.y"
  {
			yyVal = new Integer(in_def);
		        in_def = 0;
		    }
  break;
case 283:
					// line 1284 "parse.y"
  {
		        yyVal = new Integer(in_single);
		        in_single = 0;
			class_nest++;
			local_push();
		    }
  break;
case 284:
					// line 1292 "parse.y"
  {
		        yyVal = NEW_SCLASS(((NODE)yyVals[-5+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-5+yyTop]));
		        local_pop();
			class_nest--;
		        in_def = ((Integer)yyVals[-4+yyTop]).intValue();
		        in_single = ((Integer)yyVals[-2+yyTop]).intValue();
		    }
  break;
case 285:
					// line 1301 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("module definition in method body");
			class_nest++;
			local_push();
		        yyVal = new Integer(ruby_sourceline);
		    }
  break;
case 286:
					// line 1310 "parse.y"
  {
		        yyVal = NEW_MODULE(((ID)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
		        nd_set_line(((NODE)yyVal), ((Integer)yyVals[-2+yyTop]).intValue());
		        local_pop();
			class_nest--;
		    }
  break;
case 287:
					// line 1317 "parse.y"
  {
			if (in_def!=0 || in_single!=0)
			    yyerror("nested method definition");
			yyVal = cur_mid;
			cur_mid = ((ID)yyVals[0+yyTop]);
			in_def++;
			local_push();
		    }
  break;
case 288:
					// line 1331 "parse.y"
  {
		        if (((NODE)yyVals[-3+yyTop]) != null) yyVals[-4+yyTop] = NEW_RESCUE(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-2+yyTop]));
			else if (((NODE)yyVals[-2+yyTop]) != null) {
			    rb_warn("else without rescue is useless");
			    yyVals[-4+yyTop] = block_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
			}
			if (((NODE)yyVals[-1+yyTop]) != null) yyVals[-4+yyTop] = NEW_ENSURE(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));

		        /* NOEX_PRIVATE for toplevel */
			yyVal = NEW_DEFN(((ID)yyVals[-7+yyTop]), ((NODE)yyVals[-5+yyTop]), ((NODE)yyVals[-4+yyTop]), class_nest!=0?NODE.NOEX_PUBLIC:NODE.NOEX_PRIVATE);
			if (((ID)yyVals[-7+yyTop]).is_attrset_id()) ((NODE)yyVal).nd_noex(NODE.NOEX_PUBLIC);
		        fixpos(yyVal, ((NODE)yyVals[-5+yyTop]));
		        local_pop();
			in_def--;
			cur_mid = ((ID)yyVals[-6+yyTop]);
		    }
  break;
case 289:
					// line 1347 "parse.y"
  {lex_state = EXPR_FNAME;}
  break;
case 290:
					// line 1348 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			in_single++;
			local_push();
		        lex_state = EXPR_END; /* force for args */
		    }
  break;
case 291:
					// line 1360 "parse.y"
  {
		        if (((NODE)yyVals[-3+yyTop]) != null) yyVals[-4+yyTop] = NEW_RESCUE(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-2+yyTop]));
			else if (((NODE)yyVals[-2+yyTop]) != null) {
			    rb_warn("else without rescue is useless");
			    yyVals[-4+yyTop] = block_append(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-2+yyTop]));
			}
			if (((NODE)yyVals[-1+yyTop]) != null) yyVals[-4+yyTop] = NEW_ENSURE(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]));

			yyVal = NEW_DEFS(((NODE)yyVals[-10+yyTop]), ((ID)yyVals[-7+yyTop]), ((NODE)yyVals[-5+yyTop]), ((NODE)yyVals[-4+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-10+yyTop]));
		        local_pop();
			in_single--;
		    }
  break;
case 292:
					// line 1374 "parse.y"
  {
			yyVal = NEW_BREAK(null);
		    }
  break;
case 293:
					// line 1378 "parse.y"
  {
			yyVal = NEW_NEXT(null);
		    }
  break;
case 294:
					// line 1382 "parse.y"
  {
			yyVal = NEW_REDO();
		    }
  break;
case 295:
					// line 1386 "parse.y"
  {
			yyVal = NEW_RETRY();
		    }
  break;
case 302:
					// line 1401 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = NEW_IF(cond(((NODE)yyVals[-3+yyTop])), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 304:
					// line 1409 "parse.y"
  {
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 308:
					// line 1418 "parse.y"
  {
		        yyVal = new Integer(1); /*XXX (NODE*)1;*/
		    }
  break;
case 309:
					// line 1422 "parse.y"
  {
		        yyVal = new Integer(1); /*XXX (NODE*)1;*/
		    }
  break;
case 310:
					// line 1426 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 311:
					// line 1431 "parse.y"
  {
		        yyVal = dyna_push();
		    }
  break;
case 312:
					// line 1437 "parse.y"
  {
			yyVal = NEW_ITER(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-2+yyTop])!=null?((NODE)yyVals[-2+yyTop]):((NODE)yyVals[-1+yyTop]));
			dyna_pop(((RVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 313:
					// line 1442 "parse.y"
  {yyVal = dyna_push();}
  break;
case 314:
					// line 1446 "parse.y"
  {
			yyVal = NEW_ITER(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-2+yyTop])!=null?((NODE)yyVals[-2+yyTop]):((NODE)yyVals[-1+yyTop]));
			dyna_pop(((RVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 315:
					// line 1454 "parse.y"
  {
			if (((NODE)yyVals[-1+yyTop]) != null && nd_type(((NODE)yyVals[-1+yyTop])) == NODE.NODE_BLOCK_PASS) {
			    rb_compile_error("both block arg and actual block given");
			}
			((NODE)yyVals[0+yyTop]).nd_iter((NODE)yyVals[-1+yyTop]);
			yyVal = ((NODE)yyVals[0+yyTop]);
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 316:
					// line 1463 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 317:
					// line 1468 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 318:
					// line 1474 "parse.y"
  {
			yyVal = new_fcall(((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 319:
					// line 1479 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 320:
					// line 1485 "parse.y"
  {
			value_expr(((NODE)yyVals[-3+yyTop]));
			yyVal = new_call(((NODE)yyVals[-3+yyTop]), ((ID)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-3+yyTop]));
		    }
  break;
case 321:
					// line 1491 "parse.y"
  {
			value_expr(((NODE)yyVals[-2+yyTop]));
			yyVal = new_call(((NODE)yyVals[-2+yyTop]), ((ID)yyVals[0+yyTop]), null);
		    }
  break;
case 322:
					// line 1496 "parse.y"
  {
			if (!compile_for_eval && in_def==0 &&
		            in_single==0 && !in_defined)
			    yyerror("super called outside of method");
			yyVal = new_super(((NODE)yyVals[0+yyTop]));
		    }
  break;
case 323:
					// line 1503 "parse.y"
  {
			if (!compile_for_eval && in_def==0 &&
		            in_single==0 && !in_defined)
			    yyerror("super called outside of method");
			yyVal = NEW_ZSUPER();
		    }
  break;
case 324:
					// line 1511 "parse.y"
  {
		        yyVal = dyna_push();
		    }
  break;
case 325:
					// line 1516 "parse.y"
  {
			yyVal = NEW_ITER(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-1+yyTop]));
			dyna_pop(((RVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 326:
					// line 1522 "parse.y"
  {
		        yyVal = dyna_push();
		    }
  break;
case 327:
					// line 1527 "parse.y"
  {
			yyVal = NEW_ITER(((NODE)yyVals[-2+yyTop]), null, ((NODE)yyVals[-1+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-1+yyTop]));
			dyna_pop(((RVarmap)yyVals[-3+yyTop]));
		    }
  break;
case 328:
					// line 1536 "parse.y"
  {
			yyVal = NEW_WHEN(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 330:
					// line 1542 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = list_append(((NODE)yyVals[-3+yyTop]), NEW_WHEN(((NODE)yyVals[0+yyTop]), null, null));
		    }
  break;
case 331:
					// line 1547 "parse.y"
  {
			value_expr(((NODE)yyVals[0+yyTop]));
			yyVal = NEW_LIST(NEW_WHEN(((NODE)yyVals[0+yyTop]), null, null));
		    }
  break;
case 336:
					// line 1559 "parse.y"
  {
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 338:
					// line 1567 "parse.y"
  {
		        if (((NODE)yyVals[-3+yyTop]) != null) {
		            yyVals[-3+yyTop] = node_assign(((NODE)yyVals[-3+yyTop]), NEW_GVAR(ID.rb_intern("$!", ruby)));
			    yyVals[-1+yyTop] = block_append(((NODE)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]));
			}
			yyVal = NEW_RESBODY(((NODE)yyVals[-4+yyTop]), ((NODE)yyVals[-1+yyTop]), ((NODE)yyVals[0+yyTop]));
		        fixpos(yyVal, ((NODE)yyVals[-4+yyTop])!=null?((NODE)yyVals[-4+yyTop]):((NODE)yyVals[-1+yyTop]));
		    }
  break;
case 341:
					// line 1579 "parse.y"
  {
			if (((NODE)yyVals[0+yyTop]) != null)
			    yyVal = ((NODE)yyVals[0+yyTop]);
			else
			    /* place holder */
			    yyVal = NEW_NIL();
		    }
  break;
case 343:
					// line 1589 "parse.y"
  {
			yyVal = ID2SYM(((ID)yyVals[0+yyTop]));
		    }
  break;
case 345:
					// line 1595 "parse.y"
  {
			yyVal = NEW_STR(((VALUE)yyVals[0+yyTop]));
		    }
  break;
case 347:
					// line 1600 "parse.y"
  {
		        if (nd_type(((NODE)yyVals[-1+yyTop])) == NODE.NODE_DSTR) {
			    list_append(((NODE)yyVals[-1+yyTop]), NEW_STR(((VALUE)yyVals[0+yyTop])));
			}
			else {
			    rb_str_concat(((NODE)yyVals[-1+yyTop]).nd_lit(), ((VALUE)yyVals[0+yyTop]));
			}
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 348:
					// line 1610 "parse.y"
  {
		        if (nd_type(((NODE)yyVals[-1+yyTop])) == NODE.NODE_STR) {
			    yyVal = NEW_DSTR(((NODE)yyVals[-1+yyTop]).nd_lit());
			}
			else {
			    yyVal = ((NODE)yyVals[-1+yyTop]);
			}
			((NODE)yyVals[0+yyTop]).nd_head(NEW_STR(((NODE)yyVals[0+yyTop]).nd_lit()));
			nd_set_type(((NODE)yyVals[0+yyTop]), NODE.NODE_ARRAY);
			list_concat(((NODE)yyVal), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 349:
					// line 1623 "parse.y"
  {
		        lex_state = EXPR_END;
			yyVal = ((ID)yyVals[0+yyTop]);
		    }
  break;
case 361:
					// line 1641 "parse.y"
  {yyVal = new RubyId(ruby, kNIL);}
  break;
case 362:
					// line 1642 "parse.y"
  {yyVal = new RubyId(ruby, kSELF);}
  break;
case 363:
					// line 1643 "parse.y"
  {yyVal = new RubyId(ruby, kTRUE);}
  break;
case 364:
					// line 1644 "parse.y"
  {yyVal = new RubyId(ruby, kFALSE);}
  break;
case 365:
					// line 1645 "parse.y"
  {yyVal = new RubyId(ruby, k__FILE__);}
  break;
case 366:
					// line 1646 "parse.y"
  {yyVal = new RubyId(ruby, k__LINE__);}
  break;
case 367:
					// line 1649 "parse.y"
  {
			yyVal = gettable(((ID)yyVals[0+yyTop]));
		    }
  break;
case 370:
					// line 1657 "parse.y"
  {
			yyVal = null;
		    }
  break;
case 371:
					// line 1661 "parse.y"
  {
			lex_state = EXPR_BEG;
		    }
  break;
case 372:
					// line 1665 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 373:
					// line 1668 "parse.y"
  {yyerrok(); yyVal = null;}
  break;
case 374:
					// line 1671 "parse.y"
  {
			yyVal = ((NODE)yyVals[-2+yyTop]);
			lex_state = EXPR_BEG;
		    }
  break;
case 375:
					// line 1676 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 376:
					// line 1681 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(((Integer)yyVals[-5+yyTop]), ((NODE)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 377:
					// line 1685 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(((Integer)yyVals[-3+yyTop]), ((NODE)yyVals[-1+yyTop]), new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 378:
					// line 1689 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(((Integer)yyVals[-3+yyTop]), null, ((Integer)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 379:
					// line 1693 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(((Integer)yyVals[-1+yyTop]), null, new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 380:
					// line 1697 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(null, ((NODE)yyVals[-3+yyTop]), ((Integer)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 381:
					// line 1701 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(null, ((NODE)yyVals[-1+yyTop]), new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 382:
					// line 1705 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(null, null, ((Integer)yyVals[-1+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 383:
					// line 1709 "parse.y"
  {
			yyVal = block_append(NEW_ARGS(null, null, new Integer(-1)), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 384:
					// line 1713 "parse.y"
  {
			yyVal = NEW_ARGS(null, null, new Integer(-1));
		    }
  break;
case 385:
					// line 1718 "parse.y"
  {
			yyerror("formal argument cannot be a constant");
		    }
  break;
case 386:
					// line 1722 "parse.y"
  {
                        yyerror("formal argument cannot be an instance variable");
		    }
  break;
case 387:
					// line 1726 "parse.y"
  {
                        yyerror("formal argument cannot be a global variable");
		    }
  break;
case 388:
					// line 1730 "parse.y"
  {
                        yyerror("formal argument cannot be a class variable");
		    }
  break;
case 389:
					// line 1734 "parse.y"
  {
			if (!((ID)yyVals[0+yyTop]).is_local_id())
			    yyerror("formal argument must be local variable");
			else if (local_id(((ID)yyVals[0+yyTop])))
			    yyerror("duplicate argument name");
			local_cnt(((ID)yyVals[0+yyTop]));
			yyVal = new Integer(1);
		    }
  break;
case 391:
					// line 1745 "parse.y"
  {
			yyVal = new Integer(((Integer)yyVal).intValue() + 1);
		    }
  break;
case 392:
					// line 1750 "parse.y"
  {
			if (!((ID)yyVals[-2+yyTop]).is_local_id())
			    yyerror("formal argument must be local variable");
			else if (local_id(((ID)yyVals[-2+yyTop])))
			    yyerror("duplicate optional argument name");
			yyVal = assignable(((ID)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 393:
					// line 1759 "parse.y"
  {
			yyVal = NEW_BLOCK(((NODE)yyVals[0+yyTop]));
			((NODE)yyVal).nd_end(((NODE)yyVal));
		    }
  break;
case 394:
					// line 1764 "parse.y"
  {
			yyVal = block_append(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 395:
					// line 1769 "parse.y"
  {
			if (!((ID)yyVals[0+yyTop]).is_local_id())
			    yyerror("rest argument must be local variable");
			else if (local_id(((ID)yyVals[0+yyTop])))
			    yyerror("duplicate rest argument name");
			yyVal = new Integer(local_cnt(((ID)yyVals[0+yyTop])));
		    }
  break;
case 396:
					// line 1777 "parse.y"
  {
			yyVal = new Integer(-2);
		    }
  break;
case 397:
					// line 1782 "parse.y"
  {
			if (!((ID)yyVals[0+yyTop]).is_local_id())
			    yyerror("block argument must be local variable");
			else if (local_id(((ID)yyVals[0+yyTop])))
			    yyerror("duplicate block argument name");
			yyVal = NEW_BLOCK_ARG(((ID)yyVals[0+yyTop]));
		    }
  break;
case 398:
					// line 1791 "parse.y"
  {
			yyVal = ((NODE)yyVals[0+yyTop]);
		    }
  break;
case 400:
					// line 1797 "parse.y"
  {
			if (nd_type(((NODE)yyVals[0+yyTop])) == NODE.NODE_SELF) {
			    yyVal = NEW_SELF();
			}
			else {
			    yyVal = ((NODE)yyVals[0+yyTop]);
			}
		    }
  break;
case 401:
					// line 1805 "parse.y"
  {lex_state = EXPR_BEG;}
  break;
case 402:
					// line 1806 "parse.y"
  {
			switch ((int)nd_type(((NODE)yyVals[-2+yyTop]))) {
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
case 404:
					// line 1825 "parse.y"
  {
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 405:
					// line 1829 "parse.y"
  {
			if (((NODE)yyVals[-1+yyTop]).nd_alen()%2 != 0) {
			    yyerror("odd number list for Hash");
			}
			yyVal = ((NODE)yyVals[-1+yyTop]);
		    }
  break;
case 407:
					// line 1838 "parse.y"
  {
			yyVal = list_concat(((NODE)yyVals[-2+yyTop]), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 408:
					// line 1843 "parse.y"
  {
			yyVal = list_append(NEW_LIST(((NODE)yyVals[-2+yyTop])), ((NODE)yyVals[0+yyTop]));
		    }
  break;
case 428:
					// line 1873 "parse.y"
  {yyerrok();}
  break;
case 431:
					// line 1877 "parse.y"
  {yyerrok();}
  break;
case 432:
					// line 1880 "parse.y"
  {
		        yyVal = null; /*XXX 0;*/
		    }
  break;
					// line 3000 "-"
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
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
        if ((yyN = yyGindex[yyM]) != 0 && (yyN += yyState) >= 0
            && yyN < yyTable.length && yyCheck[yyN] == yyState)
          yyState = yyTable[yyN];
        else
          yyState = yyDgoto[yyM];
	 continue yyLoop;
      }
    }
  }

  protected static final short yyLhs [] = {              -1,
   75,    0,    5,    6,    6,    6,    6,   78,    7,    7,
    7,    7,    7,    7,    7,    7,    7,    7,   79,    7,
    7,    7,    7,    7,    7,    7,    8,    8,    8,    8,
    8,    8,    8,    8,    8,   12,   12,   38,   38,   38,
   11,   11,   11,   11,   11,   56,   56,   59,   59,   58,
   58,   58,   58,   58,   58,   60,   60,   57,   57,   61,
   61,   61,   61,   61,   61,   54,   54,   54,   54,   54,
   54,   69,   69,   70,   70,   70,   70,   70,   62,   62,
   48,   81,   48,   71,   71,   71,   71,   71,   71,   71,
   71,   71,   71,   71,   71,   71,   71,   71,   71,   71,
   71,   71,   71,   71,   71,   71,   71,   71,   71,   80,
   80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
   80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
   80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
   80,   80,   80,   80,   80,   80,   80,   80,   80,   80,
    9,   82,    9,    9,    9,    9,    9,    9,    9,    9,
    9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
    9,    9,    9,    9,    9,    9,    9,    9,    9,    9,
    9,    9,    9,    9,    9,    9,    9,   84,    9,    9,
    9,   30,   30,   30,   30,   30,   30,   27,   27,   27,
   27,   28,   28,   24,   24,   24,   24,   24,   24,   24,
   24,   24,   25,   25,   25,   25,   25,   25,   25,   25,
   25,   25,   25,   86,   29,   26,   87,   26,   88,   26,
   32,   31,   31,   22,   22,   34,   34,   35,   35,   35,
   10,   10,   10,   10,   10,   10,   10,   10,   10,   89,
   10,   10,   10,   10,   10,   10,   10,   10,   10,   10,
   10,   90,   10,   10,   10,   10,   10,   10,   92,   94,
   10,   95,   96,   10,   10,   10,   97,   98,   10,   99,
   10,  101,  102,   10,  103,   10,  104,   10,  106,  107,
   10,   10,   10,   10,   10,   91,   91,   91,   93,   93,
   14,   14,   15,   15,   50,   50,   51,   51,   51,   51,
  108,   53,  109,   53,   37,   37,   37,   13,   13,   13,
   13,   13,   13,  110,   52,  111,   52,   16,   23,   23,
   23,   17,   17,   19,   19,   20,   20,   18,   18,   21,
   21,    3,    3,    3,    2,    2,    2,    2,   65,   64,
   64,   64,   64,    4,    4,   63,   63,   63,   63,   63,
   63,   63,   63,   63,   63,   63,   33,   49,   49,   36,
  112,   36,   36,   39,   39,   40,   40,   40,   40,   40,
   40,   40,   40,   40,   73,   73,   73,   73,   73,   74,
   74,   42,   41,   41,   72,   72,   43,   44,   44,    1,
  113,    1,   45,   45,   45,   46,   46,   47,   66,   66,
   66,   67,   67,   67,   67,   68,   68,   68,  105,  105,
   76,   76,   83,   83,   85,   85,   85,  100,  100,   77,
   77,   55,
  };
  protected static final short yyLen [] = {           2,
    0,    2,    2,    1,    1,    3,    2,    0,    4,    3,
    3,    3,    2,    3,    3,    3,    3,    3,    0,    5,
    4,    3,    3,    3,    3,    1,    2,    2,    2,    1,
    3,    3,    2,    2,    1,    1,    1,    1,    4,    4,
    2,    4,    4,    2,    2,    1,    3,    1,    3,    1,
    2,    3,    2,    2,    1,    1,    3,    2,    3,    1,
    4,    3,    3,    3,    1,    1,    4,    3,    3,    3,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    0,    4,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    3,    0,    4,    6,    5,    5,    5,    3,    3,    3,
    3,    3,    3,    3,    3,    3,    2,    2,    3,    3,
    3,    3,    3,    3,    3,    3,    3,    3,    3,    3,
    3,    2,    2,    3,    3,    3,    3,    0,    4,    5,
    1,    1,    2,    2,    5,    2,    3,    3,    4,    4,
    6,    1,    1,    1,    2,    5,    2,    5,    4,    7,
    3,    1,    4,    5,    7,    2,    5,    4,    6,    7,
    9,    3,    1,    0,    2,    1,    0,    3,    0,    4,
    2,    2,    1,    1,    3,    1,    1,    3,    4,    2,
    1,    1,    1,    1,    1,    1,    1,    1,    6,    0,
    4,    3,    3,    2,    4,    3,    3,    1,    4,    3,
    1,    0,    6,    2,    1,    2,    6,    6,    0,    0,
    7,    0,    0,    7,    5,    4,    0,    0,    9,    0,
    6,    0,    0,    8,    0,    5,    0,    9,    0,    0,
   12,    1,    1,    1,    1,    1,    1,    2,    1,    1,
    1,    5,    1,    2,    1,    1,    1,    2,    1,    3,
    0,    5,    0,    5,    2,    4,    4,    2,    4,    4,
    3,    2,    1,    0,    5,    0,    5,    5,    1,    4,
    2,    1,    1,    1,    1,    2,    1,    6,    1,    1,
    2,    1,    1,    1,    1,    1,    2,    2,    2,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    0,    4,    2,    4,    2,    6,    4,    4,    2,    4,
    2,    2,    1,    0,    1,    1,    1,    1,    1,    1,
    3,    3,    1,    3,    2,    1,    2,    2,    1,    1,
    0,    5,    1,    2,    2,    1,    3,    3,    1,    1,
    1,    1,    1,    1,    1,    1,    1,    1,    1,    1,
    0,    1,    0,    1,    0,    1,    1,    1,    1,    1,
    2,    0,
  };
  protected static final short yyDefRed [] = {            1,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  269,  272,    0,    0,    0,  294,  295,    0,    0,
    0,  362,  361,  363,  364,    0,    0,    0,   19,    0,
  366,  365,    0,    0,  358,  357,    0,  360,  354,  355,
  345,  243,  344,  346,  244,  245,  368,  369,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  241,  342,    2,    0,    0,    0,    0,    0,    0,   30,
    0,  246,    0,   37,    0,    0,    4,    0,    0,   46,
    0,   56,    0,  343,    0,    0,   72,   73,    0,    0,
  285,  119,  131,  120,  144,  116,  137,  126,  125,  142,
  124,  123,  118,  147,  128,  117,  132,  136,  138,  130,
  122,  139,  149,  141,    0,    0,    0,    0,  115,  135,
  134,  129,  145,  148,  146,  150,  114,  121,  112,  113,
    0,    0,    0,   76,    0,  105,  106,  103,   87,   88,
   89,   92,   94,   90,  107,  108,   95,   96,  100,   91,
   93,   84,   85,   86,   97,   98,   99,  101,  102,  104,
  109,  401,    0,  400,  367,  287,   77,   78,  140,  133,
  143,  127,  110,  111,   74,   75,    0,   81,   80,   79,
    0,    0,    0,    0,    0,    0,    0,    0,  429,  428,
    0,    0,    0,  430,    0,    0,  292,  293,  258,    0,
    0,    0,    0,    0,    0,  305,  306,    0,    0,    0,
    0,    0,    0,  204,    0,   28,  212,    0,  406,   29,
   27,    0,   45,    0,  322,   44,    0,   33,    0,    8,
  424,    0,    0,    0,    0,    0,    0,  254,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,  192,    0,
    0,    0,  403,    0,    0,   54,    0,  352,  351,  353,
  349,  350,    0,   34,    0,  347,  348,    3,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,  311,  313,  315,  326,  324,
  266,    0,    0,    0,    0,    0,    0,    0,    0,   58,
  152,  318,   41,  264,    0,    0,  371,  280,  370,    0,
    0,  420,  419,  289,    0,   82,    0,    0,  339,  297,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  431,    0,    0,    0,    0,    0,    0,  277,    0,    0,
    0,    0,  205,  233,    0,  207,  260,    0,    0,    0,
    0,    0,    0,    0,  226,  225,   10,   12,   11,    0,
  262,    0,    0,    0,    0,    0,    0,  252,    0,    0,
    0,  193,    0,  426,  194,  256,    0,  196,    0,  405,
  257,  404,    0,    0,    0,    0,    0,    0,    0,    0,
   18,   31,   32,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,  321,    0,    0,  414,    0,    0,  415,
    0,    0,    0,    0,    0,  412,  413,    0,    0,    0,
    0,    0,   22,    0,   24,    0,   23,   25,  237,    0,
   52,   59,    0,    0,  373,    0,    0,    0,    0,    0,
    0,  387,  386,  385,  388,    0,    0,    0,    0,    0,
    0,  393,  383,    0,  390,    0,    0,    0,    0,    0,
  334,    0,    0,  303,    0,  298,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  276,  300,  270,  299,
  273,    0,    0,    0,    0,    0,    0,    0,    0,  211,
    0,    0,    0,  232,    0,    0,    0,  407,  259,    0,
    0,    0,    0,    0,  198,    0,    0,  229,  223,    0,
    0,    9,    0,    0,    0,   21,    0,  251,  197,    0,
    0,    0,    0,    0,    0,    0,    0,  320,   43,    0,
    0,  203,  319,   42,  202,    0,  309,    0,    0,  307,
    0,    0,    0,  317,   40,  316,   39,    0,    0,   57,
    0,  283,    0,    0,  286,    0,  290,    0,  395,  397,
    0,    0,  375,    0,  381,  399,    0,  382,    0,  379,
   83,    0,    0,  337,    0,  304,    0,    0,  340,    0,
    0,  301,    0,    0,  275,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  209,    0,    0,    0,  199,    0,
    0,  200,    0,    0,    0,    0,  216,  228,    0,   20,
    0,    0,    0,    0,    0,    0,    0,  308,    0,    0,
    0,    0,    0,    0,    0,    0,  372,  281,  402,    0,
    0,    0,    0,    0,  394,  398,    0,    0,    0,  391,
    0,    0,  336,    0,    0,  341,  249,    0,  267,  268,
    0,    0,    0,    0,  278,  206,    0,  208,    0,  222,
    0,    0,    0,  230,    0,  263,  195,    0,  310,  312,
  314,  327,  325,    0,    0,    0,  374,    0,  380,    0,
  377,  378,    0,    0,    0,    0,    0,    0,  332,  333,
  328,  271,  274,    0,    0,  201,    0,    0,  213,    0,
  218,    0,  284,    0,    0,    0,    0,    0,    0,    0,
  338,    0,    0,  210,  214,    0,    0,    0,  217,    0,
  288,  376,    0,  302,  279,    0,    0,  219,    0,    0,
  215,    0,  220,    0,    0,  291,  221,
  };
  protected static final short yyDgoto [] = {             1,
  163,   60,   61,   62,  240,   64,   65,   66,   67,  236,
   69,   70,   71,  621,  622,  350,  731,  338,  499,  613,
  618,  215,  516,  216,  548,  376,  572,  573,  226,  247,
  363,  534,   72,  468,  465,  328,   73,   74,  489,  490,
  491,  492,  676,  605,  251,  218,  219,  177,  185,  205,
  579,  324,  308,  186,   77,   78,   79,   80,  242,   81,
   82,  178,  187,  261,   84,  209,  523,  444,   90,  180,
  450,  494,  495,  496,    2,  192,  193,  380,  233,  168,
  497,  473,  232,  382,  395,  227,  551,  645,  390,  553,
  341,  195,  519,  629,  196,  630,  528,  734,  477,  342,
  474,  666,  330,  335,  334,  480,  670,  452,  453,  455,
  454,  476,  331,
  };
  protected static final short yySindex [] = {            0,
    0, 4393,12265,  212, -155,15494,15222, 4393,13761,13761,
11356,    0,    0,15020,12683,12683,    0,    0,12683,12389,
   34,    0,    0,    0,    0,13761,15127,   41,    0,  -92,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,14643,14643,
 -155,12005,13761,13859,14643,16962,15587,14741,14643, -177,
    0,    0,    0,   68,  331,  214,16880,  -28, -235,    0,
  -82,    0,  -27,    0, -280,   89,    0,  128,15385,    0,
  130,    0, -123,    0,   17,  331,    0,    0,13761,   83,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,  -16,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  187,    0,    0,    0,
  -40, 4393,  180,   -4, -280,  165, -123,  180,    0,    0,
   66,  -35,  185,    0,13761,13761,    0,    0,    0,  209,
   34,   41,   31,    0,  -18,    0,    0,    0,   17,14643,
14643,14643,16825,    0,  244,    0,    0,  272,    0,    0,
    0,12487,    0,12683,    0,    0,12781,    0,  173,    0,
    0,  280,  202, 4393,   16,   37,   16,    0,12005,  303,
    0,  371,  214,14643,   41,   72,  279,   82,    0,  207,
  301,   82,    0,   56,    0,    0,    0,    0,    0,    0,
    0,    0,   16,    0,   16,    0,    0,    0,12100,13761,
13761,13761,13761,12265,13761,13761,14643,14643,14643,14643,
14643,14643,14643,14643,14643,14643,14643,14643,14643,14643,
14643,14643,14643,14643,14643,14643,14643,14643,14643,14643,
14643,14643,15925,15960,13859,    0,    0,    0,    0,    0,
    0,15995,15995,14643,13957,13957,12005,16962,  400,    0,
    0,    0,    0,    0,  214,   68,    0,    0,    0, 4393,
13761,    0,    0,    0,  111,    0,14643,  219,    0,    0,
 4393,  211,16298,16333,13859,14643, 4393,  -35,14055,  188,
    0,  178,  178,  280,16368,16403,13859,    0, 4199,16880,
14643,12879,    0,    0,12977,    0,    0,  435, -235,  450,
   41,    4,  461,12585,    0,    0,    0,    0,    0,15222,
    0,14643, 4393,  382,16298,16333,  474,    0,    0,  478,
 6222,    0,14153,    0,    0,    0,14643,    0,14643,    0,
    0,    0,16438,16473,13859,  331,  214,  214,  214,  214,
    0,    0,    0,   16, 1315, 1315, 1315, 1315, 1414, 1414,
 7190, 6709, 1315, 1315,11920,11920,   94,   94,16853, 1414,
 1414,  205,  205,   61,   87,   87,   16,   16,   16,  191,
    0,    0,   34,    0,    0,  203,    0,  222,   34,    0,
  430, -102, -102, -102, -102,    0,    0,   34,   34,16880,
14643,16880,    0,  521,    0,16880,    0,    0,    0,  527,
    0,    0,14643,   68,    0,13761, 4393,  312,  134,15890,
  519,    0,    0,    0,    0,  288,  291,  530, 4393,   68,
  543,    0,    0,  554,    0,  563,15222,16880,  266,  569,
    0, 4393,  351,    0,  261,    0,  191,  203,  222,  525,
16880,  219,  357,14643,  588,   74,    0,    0,    0,    0,
    0,    0,   34,    0,    0,   34,  546,13761,  296,    0,
16880,14643,16825,    0,  613,14643,16825,    0,    0,13075,
  611,15995,15995,  621,    0,14643,10601,    0,    0,  619,
  625,    0,13761,16880,  542,    0,    0,    0,    0,14643,
16880,    0,    0,    0,  581,14643,14643,    0,    0,14643,
14643,    0,    0,    0,    0,  336,    0,16710, 4393,    0,
 4393, 4393, 4393,    0,    0,    0,    0,16880,14251,    0,
16880,    0,   66,  415,    0,  639,    0,14643,    0,    0,
   41,  -40,    0,  161,    0,    0,  333,    0,  530,    0,
    0,16962,   74,    0,14643,    0, 4393,  425,    0,13761,
  428,    0,  336,  429,    0,16880,14349, 4393, 4393, 4393,
    0,  178, 4199,13173,    0, 4199, -235,    4,    0,   34,
   34,    0, 4199,14447,  655,13271,    0,    0,  138,    0,
 6222,    0,16880,16880,16880,16880,14643,    0,  574,  436,
  576,  441,  579,14643,16880, 4393,    0,    0,    0,  111,
16880,  658,  219,  519,    0,    0,  554,  662,  554,    0,
   64,    0,    0,    0, 4393,    0,    0,  180,    0,    0,
14643, -112,  448,  451,    0,    0,14643,    0,  667,    0,
14643,  673,  683,    0,14643,    0,    0,16880,    0,    0,
    0,    0,    0,16880,  464, 4393,    0,  351,    0,  161,
    0,    0,16516,16788,13859,  -40, 4393,16880,    0,    0,
    0,    0,    0, 4393, 4199,    0, 4199,13369,    0,13467,
    0, 4199,    0,  -40,  466,  554,    0,    0,    0,  636,
    0,  261,  467,    0,    0,14643,  691,14643,    0,  219,
    0,    0,    0,    0,    0, 4199,13565,    0, 4199,  351,
    0,14643,    0,  475, 4199,    0,    0,
  };
  protected static final short yyRindex [] = {            0,
    0,  200,    0,    0,    0,    0,    0,  561,    0,    0,
  469,    0,    0,    0,10761,10846,    0,    0,10942, 6071,
 3306,    0,    0,    0,    0,    0,    0,14545,    0,    0,
    0,    0, 1586, 2781,    0,    0, 1921,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  250,    0,  654,  638,  465,    0,    0,    0, 5584,
    0,    0,    0,  997, 1341, 5022, 1707,12162, 5312,    0,
 5892,    0, 5799,    0,11678,    0,    0,    0,  477,    0,
    0,    0,11763,    0,13663, 1428,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  364,  692,  782,  808,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  868,  999, 1084,    0, 1272,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0, 5113,    0,    0,    0,
  306,  250,    0, 6468, 5097,    0, 5405,    0,    0,    0,
  469,    0,  503,    0,    0,    0,    0,    0,    0, 5981,
 4918,  735,    0,  114,    0,    0,    0,  443,    0,    0,
    0,    0, 6679,    0,10453,    0,    0,10453,    0,    0,
    0,    0,    0,  739,    0,    0,    0,    0,    0,    0,
    0,14839,    0,   46, 6558, 6379, 6860,    0,  250,    0,
  440,    0,  740,    0,  693,  695,    0,  695,    0,  664,
    0,  664,    0,    0,  830,    0,  891,    0,    0,    0,
    0,    0, 6949,    0, 7039,    0,    0,    0, 1678,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,  654,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  250,  499,  541,    0,
    0,    0,    0,    0,  107,    0,    0,    0,    0,   97,
    0,    0,    0,    0,  182,    0,   26,  283,    0,    0,
  390,11037,    0,    0,  654,    0,  198,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  654,    0,10453,10495,
    0,    0,    0,    0,    0,    0,    0,    0,   80,  120,
  752,  752,    0,  753,    0,    0,    0,    0,    0,    0,
    0,    0,   46,    0,    0,    0,    0,    0,  493,    0,
  693,    0,  706,    0,    0,    0,   21,    0,  677,    0,
    0,    0,    0,    0,  654, 1530, 5510, 5600, 6087, 7681,
    0,    0,    0, 7341, 9072, 9164, 9251, 9345, 8518, 8795,
 9437, 9710, 9524, 9618, 1491, 9797, 8216, 8304,    0, 8887,
 8974, 8609, 8697, 8395, 7910, 8001, 7430, 7520, 7822, 4071,
 2876, 3641,13663,    0, 3211, 4166,    0, 4501, 3736,    0,
    0,11134,11245,11134,11245,    0,    0, 4596, 4596, 9833,
    0, 5218,    0,    0,    0, 5705,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,   97,    0,  752,    0,
  589,    0,    0,    0,    0,  596,    0,  204,  561,    0,
  250,    0,    0,  250,    0,  250,    0,   22,  113,   50,
    0,  421,  544,    0,  544,    0, 2016, 2351, 2446,    0,
 9895,  544,    0,    0,  144,    0,    0,    0,    0,    0,
    0,  865,    0, 2407, 2837, 5007,    0,    0,    0,    0,
 7160,    0, 8121,    0,10453,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,  183,    0,    0,  739,
    0,    0,    0, 9983,    0,    0,  492,    0,    0,    0,
  108, 1203, 1386, 1977,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,11859,    0,    0,   97,    0,
   46,   97,   46,    0,    0,    0,    0, 7925,    0,    0,
10026,    0,    0,    0,    0,    0,    0,    0,    0,    0,
  752,  306,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,    0,    0,    0,   97,    0,    0,    0,
    0,    0, 5494,    0,    0,  154,    0,  322,   97,   97,
  937,    0,10453,    0,    0,10453,    0,  752,    0,   27,
   27,    0,  739,    0,    0,    0,    0,    0,    0,    0,
  693,  916,10138,10174,10271,10334,    0,    0,    0,    0,
    0,    0,    0,    0, 6192,   97,    0,    0,    0,  182,
  732,    0,  283,    0,    0,    0,  250,  250,  250,    0,
    0,  314,    0,  444,  561,    0,    0,    0,    0,    0,
    0,  544,    0,    0,    0,    0,    0,    0,    0,    0,
    0,  739,  739,    0,    0,    0,    0,10403,    0,    0,
    0,    0,    0, 9147,    0,  561,    0,  544,    0,    0,
    0,    0,    0,    0,  654,  306,  390,  196,    0,    0,
    0,    0,    0,   97,10453,    0,  739,    0,    0,    0,
    0,  739,    0,  306,    0,  250,  518,  635,  678,    0,
    0,  544,    0,    0,    0,    0,  739,    0,    0,  283,
    0,    0,  709,    0,    0,  739,    0,    0,  739,  544,
    0,    0,    0,    0,  739,    0,    0,
  };
  protected static final short yyGindex [] = {            0,
    0,    0,    0,    0,  232,    0,   52,  878,  -15,  167,
    8,   36,    0,   54, -285, -338,    0, -581,    0,    0,
 -659,   -9,    0,  631,    0,    0,  -10, -387,  -72, -293,
  277,   81,  801,    0,  494,    0, -207,    0,  147,  338,
  210, -509, -315,  609,    0,  -47, -332,    0,  608,  251,
  150,  760,    0,  867, 1065,  -12,    0,  -23, -169,  756,
   11,  -11,  582,    0,   -1,  751, -224,    0,  397,   -3,
   42, -518,  223,    0,    0,  -26,  779,    0,    0,    0,
    0,    0, -187,    0,  309,    0,    0,    0,    0,    0,
 -179,    0, -328,    0,    0,    0,    0,    0,    0,  104,
    0,    0,    0,    0,    0,    0,    0,    0,    0,    0,
    0,    0,    0,
  };
    protected static final short yyTable [] = tab.yyTable();
    protected static final short yyCheck [] = tab.yyCheck();

					// line 1884 "parse.y"
    //XXX helpers
    void yyerrok() {}

    private int fixop(ID op) {
        if (op.intValue() == tOROP)
	    return 0;
	if (op.intValue() == tANDOP)
	    return 1;
	return op.intValue();
    }

// ---------------------------------------------------------------------------
// here the parser methods start....
// ---------------------------------------------------------------------------

    /** Returns the node's type (added to reduce the need for casts). */
    private int nd_type(Object node) {
        return ((NODE)node).nd_type();
    }

    /** Sets the node's type. */
    private void nd_set_type(NODE node, int type) {
        node.nd_set_type(type);
    }

    /* Returns the node's type (same as nd_type()) */
    private /*enum node_type*/int nodetype(NODE node) {
        return nd_type(node);
    }

    /** Returns the node's source code line number (same as nd_line()) */
    private int nodeline(NODE node) {
        return nd_line(node);
    }

    /** Returns the node's source code line number. */
    private int nd_line(NODE node) {
        return node.nd_line();
    }

    /** Sets the node's source code line number. */
    private void nd_set_line(NODE node, int line) {
        node.nd_set_line(line);
    }

    /** Copies position info (added to reduce the need for casts). */
    private void fixpos(Object node, NODE orig) {
        fixpos((NODE)node, orig);
    }

    /** Copies filename and line number from "orig" to "node". */
    private void fixpos(NODE node, NODE orig) {
        if (node == null) return;
        if (orig == null) return;
        node.nd_file = orig.nd_file;
        nd_set_line(node, nd_line(orig));
    }

    /** Wraps node with NEWLINE node. */
    private NODE newline_node(NODE node) {
        NODE nl = null;
        if (node != null) {
            nl = NEW_NEWLINE(node);
            fixpos(nl, node);
            nl.nd_nth(nd_line(node));
        }
        return nl;
    }

    private NODE block_append(NODE head, NODE tail) {
        if (tail == null) return head;
        if (head == null) return tail;

        NODE end;
        if (nd_type(head) != NODE.NODE_BLOCK) {
            end = NEW_BLOCK(head);
            end.nd_end(end);
	    fixpos(end, head);
	    head = end;
        }
        else {
	    end = head.nd_end();
        }

        if (RTEST(ruby_verbose)) {
	    NODE nd = end.nd_head();
          newline:for(;;) {
            switch (nd_type(nd)) {
              case NODE.NODE_RETURN:
              case NODE.NODE_BREAK:
              case NODE.NODE_NEXT:
              case NODE.NODE_REDO:
              case NODE.NODE_RETRY:
                rb_warning("statement not reached");
                break;

            case NODE.NODE_NEWLINE:
                nd = nd.nd_next();
                continue newline;

              default:
                break;
            }
            break;
          }
        }

        if (nd_type(tail) != NODE.NODE_BLOCK) {
            tail = NEW_BLOCK(tail);
	    tail.nd_end(tail);
        }
        end.nd_next(tail);
        head.nd_end(tail.nd_end());
        return head;
    }

    private NODE list_append(NODE head, NODE tail) {
        if (head == null) return NEW_LIST(tail);

        NODE last = head;
        while (last.nd_next() != null) {
            last = last.nd_next();
        }

        last.nd_next(NEW_LIST(tail));
        head.nd_alen(head.nd_alen() + 1);
        return head;
    }

    private NODE list_concat(NODE head, NODE tail) {
        NODE last = head;

        while (last.nd_next() != null) {
            last = last.nd_next();
        }

        last.nd_next(tail);
        head.nd_alen(head.nd_alen() + tail.nd_alen());

        return head;
    }

    /** Creates a new CALL node (added to reduce the need for casts). */
    NODE call_op(NODE recv, int op, int narg, NODE arg1) {
        return call_op(recv, new RubyId(ruby, op), narg, arg1);
    }

    /** Creates a new CALL node. */
    private NODE call_op(NODE recv, ID id, int narg, NODE arg1) {
        value_expr(recv);
        if (narg == 1) {
            value_expr(arg1);
        }
        return NEW_CALL(recv, id, narg == 1 ? NEW_LIST(arg1) : null);
    }

    private NODE match_gen(NODE node1, NODE node2) {
        local_cnt('~');

        switch (nd_type(node1)) {
          case NODE.NODE_DREGX:
          case NODE.NODE_DREGX_ONCE:
            return NEW_MATCH2(node1, node2);

          case NODE.NODE_LIT:
            if (TYPE(node1.nd_lit()) == T_REGEXP) {
                return NEW_MATCH2(node1, node2);
            }
        }

        switch (nd_type(node2)) {
          case NODE.NODE_DREGX:
          case NODE.NODE_DREGX_ONCE:
            return NEW_MATCH3(node2, node1);

          case NODE.NODE_LIT:
            if (TYPE(node2.nd_lit()) == T_REGEXP) {
                return NEW_MATCH3(node2, node1);
            }
        }

        return NEW_CALL(node1, new RubyId(ruby, tMATCH), NEW_LIST(node2));
    }

    private NODE gettable(ID id) {
        if (id.intValue() == kSELF) {
            return NEW_SELF();
        }
        else if (id.intValue() == kNIL) {
            return NEW_NIL();
        }
        else if (id.intValue() == kTRUE) {
            return NEW_TRUE();
        }
        else if (id.intValue() == kFALSE) {
            return NEW_FALSE();
        }
        else if (id.intValue() == k__FILE__) {
            return NEW_STR(rb_str_new2(ruby_sourcefile));
        }
        else if (id.intValue() == k__LINE__) {
            return NEW_LIT(INT2FIX(ruby_sourceline));
        }
        else if (id.is_local_id()) {
            if (dyna_in_block() && rb_dvar_defined(id)) return NEW_DVAR(id);
            if (local_id(id)) return NEW_LVAR(id);
            /* method call without arguments */
            return NEW_VCALL(id);
        }
        else if (id.is_global_id()) {
            return NEW_GVAR(id);
        }
        else if (id.is_instance_id()) {
            return NEW_IVAR(id);
        }
        else if (id.is_const_id()) {
            return NEW_CONST(id);
        }
        else if (id.is_class_id()) {
            if (in_single!=0)
                return NEW_CVAR2(id);
            return NEW_CVAR(id);
        }
        rb_bug("invalid id for gettable");
        return null;
    }

    private NODE assignable(ID id, NODE val) {
        value_expr(val);
        if (id.intValue() == kSELF) {
            yyerror("Can't change the value of self");
        }
        else if (id.intValue() == kNIL) {
            yyerror("Can't assign to nil");
        }
        else if (id.intValue() == kTRUE) {
            yyerror("Can't assign to true");
        }
        else if (id.intValue() == kFALSE) {
            yyerror("Can't assign to false");
        }
        else if (id.intValue() == k__FILE__) {
            yyerror("Can't assign to __FILE__");
        }
        else if (id.intValue() == k__LINE__) {
            yyerror("Can't assign to __LINE__");
        }
        else if (id.is_local_id()) {
            if (rb_dvar_curr(id) != false) {
                return NEW_DASGN_CURR(id, val);
            }
            else if (rb_dvar_defined(id)) {
                return NEW_DASGN(id, val);
            }
            else if (local_id(id) || !dyna_in_block()) {
                return NEW_LASGN(id, val);
            }
            else{
                rb_dvar_push(id, Qnil);
                return NEW_DASGN_CURR(id, val);
            }
        }
        else if (id.is_global_id()) {
            return NEW_GASGN(id, val);
        }
        else if (id.is_instance_id()) {
            return NEW_IASGN(id, val);
        }
        else if (id.is_const_id()) {
            if (in_def!=0 || in_single!=0)
                yyerror("dynamic constant assignment");
            return NEW_CDECL(id, val);
        }
        else if (id.is_class_id()) {
            if (in_single!=0) return NEW_CVASGN(id, val);
            return NEW_CVDECL(id, val);
        }
        else {
            rb_bug("bad id for variable");
        }
        return null;
    }

    private NODE aryset(NODE recv, NODE idx) {
        value_expr(recv);
        return NEW_CALL(recv, new RubyId(ruby, tASET), idx);
    }

    private NODE attrset(NODE recv, ID id) {
        value_expr(recv);

        return NEW_CALL(recv, id.rb_id_attrset(ruby), null);
    }

    private void rb_backref_error(NODE node) {
        switch (nd_type(node)) {
          case NODE.NODE_NTH_REF:
            rb_compile_error("Can't set variable $%d", new Long(node.nd_nth()));
            break;
          case NODE.NODE_BACK_REF:
            rb_compile_error("Can't set variable $%c", new Long(node.nd_nth()));
            break;
        }
    }

    private NODE arg_concat(NODE node1, NODE node2)
    {
        if (node2 == null) return node1;
        return NEW_ARGSCAT(node1, node2);
    }

    private NODE  arg_add(NODE node1, NODE node2)
    {
        if (node1 == null) return NEW_LIST(node2);
        if (nd_type(node1) == NODE.NODE_ARRAY) {
            return list_append(node1, node2);
        }
        else {
            return NEW_ARGSPUSH(node1, node2);
        }
    }

    private NODE node_assign(NODE lhs, NODE rhs)
    {
        if (lhs == null) return null;

        value_expr(rhs);
        switch (nd_type(lhs)) {
          case NODE.NODE_GASGN:
          case NODE.NODE_IASGN:
          case NODE.NODE_LASGN:
          case NODE.NODE_DASGN:
          case NODE.NODE_DASGN_CURR:
          case NODE.NODE_MASGN:
          case NODE.NODE_CDECL:
          case NODE.NODE_CVDECL:
          case NODE.NODE_CVASGN:
            lhs.nd_value(rhs);
            break;

          case NODE.NODE_CALL:
            lhs.nd_args(arg_add(lhs.nd_args(), rhs));
            break;

          default:
            /* should not happen */
            break;
        }

        if (rhs != null) fixpos(lhs, rhs);
        return lhs;
    }

    private boolean value_expr(NODE node)
    {
        if (node == null) return true;

        switch (nd_type(node)) {
          case NODE.NODE_RETURN:
          case NODE.NODE_BREAK:
          case NODE.NODE_NEXT:
          case NODE.NODE_REDO:
          case NODE.NODE_RETRY:
          case NODE.NODE_WHILE:
          case NODE.NODE_UNTIL:
          case NODE.NODE_CLASS:
          case NODE.NODE_MODULE:
          case NODE.NODE_DEFN:
          case NODE.NODE_DEFS:
            yyerror("void value expression");
            return false;

          case NODE.NODE_BLOCK:
            while (node.nd_next() != null) {
                node = node.nd_next();
            }
            return value_expr(node.nd_head());

          case NODE.NODE_BEGIN:
            return value_expr(node.nd_body());

          case NODE.NODE_IF:
            return value_expr(node.nd_body()) && value_expr(node.nd_else());

          case NODE.NODE_NEWLINE:
            return value_expr(node.nd_next());

          default:
            return true;
        }
    }

    private void void_expr(NODE node)
    {
        String useless = null;

        if (!RTEST(ruby_verbose)) return;
        if (node == null) return;

      again:for(;;) {
        switch (nd_type(node)) {
          case NODE.NODE_NEWLINE:
            node = node.nd_next();
            continue again;

          case NODE.NODE_CALL:
            switch (node.nd_mid().intValue()) {
              case '+':
              case '-':
              case '*':
              case '/':
              case '%':
              case tPOW:
              case tUPLUS:
              case tUMINUS:
              case '|':
              case '^':
              case '&':
              case tCMP:
              case '>':
              case tGEQ:
              case '<':
              case tLEQ:
              case tEQ:
              case tNEQ:
              case tAREF:
              case tRSHFT:
              case tCOLON2:
              case tCOLON3:
                useless = ID.rb_id2name(ruby, node.nd_mid());
                break;
            }
            break;

          case NODE.NODE_LVAR:
          case NODE.NODE_DVAR:
          case NODE.NODE_GVAR:
          case NODE.NODE_IVAR:
          case NODE.NODE_CVAR:
          case NODE.NODE_NTH_REF:
          case NODE.NODE_BACK_REF:
            useless = "a variable";
            break;
          case NODE.NODE_CONST:
          case NODE.NODE_CREF:
            useless = "a constant";
            break;
          case NODE.NODE_LIT:
          case NODE.NODE_STR:
          case NODE.NODE_DSTR:
          case NODE.NODE_DREGX:
          case NODE.NODE_DREGX_ONCE:
            useless = "a literal";
            break;
          case NODE.NODE_COLON2:
          case NODE.NODE_COLON3:
            useless = "::";
            break;
          case NODE.NODE_DOT2:
            useless = "..";
            break;
          case NODE.NODE_DOT3:
            useless = "...";
            break;
          case NODE.NODE_SELF:
            useless = "self";
            break;
          case NODE.NODE_NIL:
            useless = "nil";
            break;
          case NODE.NODE_TRUE:
            useless = "true";
            break;
          case NODE.NODE_FALSE:
            useless = "false";
            break;
          case NODE.NODE_DEFINED:
            useless = "defined?";
            break;
        }
        break again;
        }

        if (useless != null) {
            int line = ruby_sourceline;

            ruby_sourceline = nd_line(node);
            rb_warn("useless use of %s in void context", useless);
            ruby_sourceline = line;
        }
    }

    private void void_stmts(NODE node) {
        if (!RTEST(ruby_verbose)) return;
        if (node == null) return;
        if (nd_type(node) != NODE.NODE_BLOCK) return;

        for (;;) {
            if (node.nd_next() == null) return;
            void_expr(node.nd_head());
            node = node.nd_next();
        }
    }

    private boolean assign_in_cond(NODE node) {
        switch (nd_type(node)) {
          case NODE.NODE_MASGN:
            yyerror("multiple assignment in conditional");
            return true;

          case NODE.NODE_LASGN:
          case NODE.NODE_DASGN:
          case NODE.NODE_GASGN:
          case NODE.NODE_IASGN:
            break;

          case NODE.NODE_NEWLINE:
          default:
            return false;
        }

        switch (nd_type(node.nd_value())) {
          case NODE.NODE_LIT:
          case NODE.NODE_STR:
          case NODE.NODE_NIL:
          case NODE.NODE_TRUE:
          case NODE.NODE_FALSE:
            /* reports always */
            rb_warn("found = in conditional, should be ==");
            return true;

          case NODE.NODE_DSTR:
          case NODE.NODE_XSTR:
          case NODE.NODE_DXSTR:
          case NODE.NODE_EVSTR:
          case NODE.NODE_DREGX:
          default:
            break;
        }
        return true;
    }

    private boolean e_option_supplied() {
        return ruby_sourcefile.equals("-e");
    }

    private void warn_unless_e_option(String str) {
        if (!e_option_supplied()) rb_warn(str);
    }

    private void warning_unless_e_option(String str) {
        if (!e_option_supplied()) rb_warning(str);
    }

    private NODE range_op(NODE node, int logop) {
        /*enum node_type*/int type;

        if (logop!=0) return node;
        if (!e_option_supplied()) return node;

        warn_unless_e_option("integer literal in condition");
        node = cond0(node, 0); //XXX second argument was missing
        type = nd_type(node);
        if (type == NODE.NODE_NEWLINE) node = node.nd_next();
        if (type == NODE.NODE_LIT && FIXNUM_P(node.nd_lit())) {
            return call_op(node,tEQ,1,NEW_GVAR(ID.rb_intern("$.", ruby)));
        }
        return node;
    }

    private NODE cond0(NODE node, int logop) {
        /*enum node_type*/int type = nd_type(node);

        assign_in_cond(node);
        switch (type) {
          case NODE.NODE_DSTR:
          case NODE.NODE_STR:
            if (logop!=0) break;
            rb_warn("string literal in condition");
            break;

          case NODE.NODE_DREGX:
          case NODE.NODE_DREGX_ONCE:
            warning_unless_e_option("regex literal in condition");
            local_cnt('_');
            local_cnt('~');
            return NEW_MATCH2(node, NEW_GVAR(ID.rb_intern("$_", ruby)));

          case NODE.NODE_DOT2:
          case NODE.NODE_DOT3:
            node.nd_beg(range_op(node.nd_beg(), logop));
            node.nd_end(range_op(node.nd_end(), logop));
            if (type == NODE.NODE_DOT2) nd_set_type(node,NODE.NODE_FLIP2);
            else if (type == NODE.NODE_DOT3) nd_set_type(node, NODE.NODE_FLIP3);
            node.nd_cnt(local_append(new RubyId(ruby, 0)));
            warning_unless_e_option("range literal in condition");
            break;

          case NODE.NODE_LIT:
            if (TYPE(node.nd_lit()) == T_REGEXP) {
                warning_unless_e_option("regex literal in condition");
                nd_set_type(node, NODE.NODE_MATCH);
                local_cnt('_');
                local_cnt('~');
            }
        }
        return node;
    }

    private NODE cond1(NODE node, int logop) {
        if (node == null) return null;
        if (nd_type(node) == NODE.NODE_NEWLINE){
            node.nd_next(cond0(node.nd_next(), logop));
            return node;
        }
        return cond0(node, logop);
    }

    private NODE cond(NODE node) {
        return cond1(node, 0);
    }

    private NODE logop(/*node_type*/int type, NODE left, NODE right) {
	value_expr(left);
	return rb_node_newnode(type, cond1(left, 1), cond1(right, 1), null);
    }

    private NODE ret_args(NODE node) {
	if (node != null) {
	    if (nd_type(node) == NODE.NODE_BLOCK_PASS) {
		rb_compile_error("block argument should not be given");
	    }
	}
	return node;
    }

    /** (added to reduce the need to cast) */
    NODE arg_blk_pass(Object n1, NODE n2) {
        return arg_blk_pass((NODE)n1, n2);
    }

    NODE arg_blk_pass(NODE node1, NODE node2) {
	if (node2 != null) {
	    node2.nd_head(node1);
	    return node2;
	}
	return node1;
    }

    /*
    private NODE arg_prepend(NODE node1, NODE node2) {
	switch (nodetype(node2)) {
	case NODE.NODE_ARRAY:
	    return list_concat(NEW_LIST(node1), node2);

	case NODE.NODE_RESTARGS:
	    return arg_concat(node1, node2.nd_head());

	case NODE.NODE_BLOCK_PASS:
	    node2.nd_body = arg_prepend(node1, node2.nd_body);
	    return node2;

	default:
	    rb_bug("unknown nodetype(%d) for arg_prepend");
	}
	return null;			// not reached
    }
    */

    private NODE new_call(NODE r, ID m, NODE a) {
	if (a != null && nd_type(a) == NODE.NODE_BLOCK_PASS) {
	    a.nd_iter(NEW_CALL(r,m,a.nd_head()));
	    return a;
	}
	return NEW_CALL(r,m,a);
    }

    private NODE new_fcall(ID m, NODE a) {
	if (a != null && nd_type(a) == NODE.NODE_BLOCK_PASS) {
	    a.nd_iter(NEW_FCALL(m,a.nd_head()));
	    return a;
	}
	return NEW_FCALL(m,a);
    }

    private NODE new_super(NODE a) {
	if (a != null && nd_type(a) == NODE.NODE_BLOCK_PASS) {
	    a.nd_iter(NEW_SUPER(a.nd_head()));
	    return a;
	}
	return NEW_SUPER(a);
    }

    private class local_vars {
        ID[] tbl;
        int nofree;
        int cnt;
        int dlev;
        local_vars prev;
    };

    private local_vars lvtbl = new local_vars();

    private void local_push() {
        local_vars local = new local_vars();
        local.prev = lvtbl;
        local.nofree = 0;
        local.cnt = 0;
        local.tbl = null;
        local.dlev = 0;
        lvtbl = local;
    }

    private void local_pop() {
        local_vars local = lvtbl.prev;

        if (lvtbl.tbl != null) {
            if (lvtbl.nofree == 0) /*free(lvtbl.tbl)*/;
            else lvtbl.tbl[0] = new RubyId(ruby, lvtbl.cnt);
        }
        /*free(lvtbl);*/
        lvtbl = local;
    }

    private ID[] local_tbl() {
        lvtbl.nofree = 1;
        return lvtbl.tbl;
    }

    private int local_append(ID id) {
        if (lvtbl.tbl == null) {
            lvtbl.tbl = new ID[4];
            lvtbl.tbl[0] = new RubyId(ruby, 0);
            lvtbl.tbl[1] = new RubyId(ruby, '_');
            lvtbl.tbl[2] = new RubyId(ruby, '~');
            lvtbl.cnt = 2;
            if (id.intValue() == '_') return 0;
            if (id.intValue() == '~') return 1;
        }
        else {
            ID[] ntbl = new ID[lvtbl.cnt + 2];
            System.arraycopy(lvtbl.tbl, 0, ntbl, 0, lvtbl.tbl.length);
            lvtbl.tbl = ntbl;
        }

        lvtbl.tbl[lvtbl.cnt + 1] = id;
        return lvtbl.cnt++;
    }

    private int local_cnt(int id) {
        return local_cnt(new RubyId(ruby, id));
    }
    private int local_cnt(ID id) {
        if (id == null) return lvtbl.cnt;

        for (int cnt = 1, max = lvtbl.cnt + 1; cnt < max; cnt++) {
            if (lvtbl.tbl[cnt] == id) return cnt - 1;
        }
        return local_append(id);
    }

    private boolean local_id(ID id)
    {
        int i, max;

        if (lvtbl == null) return false;
        for (i=3, max=lvtbl.cnt+1; i<max; i++) {
            if (lvtbl.tbl[i] == id) return true;
        }
        return false;
    }

    private void
    top_local_init()
    {
        local_push();
        lvtbl.cnt = ruby.rubyScope.getLocalTbl() != null ? ruby.rubyScope.getLocalTbl(0).intValue() : 0;
        if (lvtbl.cnt > 0) {
            lvtbl.tbl = new ID[lvtbl.cnt+3];
	    System.arraycopy(lvtbl.tbl, 0, ruby.rubyScope.getLocalTbl(), 0, lvtbl.cnt+1);
        }
        else {
            lvtbl.tbl = null;
        }
        if (ruby_dyna_vars != null)
            lvtbl.dlev = 1;
        else
            lvtbl.dlev = 0;
    }

    private void
    top_local_setup()
    {
        int len = lvtbl.cnt;
        int i;

        if (len > 0) {
            i = ruby.rubyScope.getLocalTbl() != null ? ruby.rubyScope.getLocalTbl(0).intValue() : 0;

            if (i < len) {
                if (i == 0 || (ruby.rubyScope.getFlags() & 1 /*SCOPE_MALLOC*/) == 0) {
                    // SHIFTABLE +++
		    /*VALUE[] vars = new VALUE[len + 1];
                    int vi = 0;
                    if (ruby.rubyScope.getLocalVars() != null) {
                        vars[vi++] = ruby.rubyScope.getLocalVars(-1);

                        System.arraycopy(vars, 0, ruby.rubyScope.getLocalVars(), 0, i);
                        // rb_mem_clear(vars+i, len-i);
                    } else {
                        vars[vi++] = null;
                        // rb_mem_clear(vars, len);
                    }*/
		    // SHIFTABLE ---
		    List tmp = Collections.nCopies(len + 1, ruby.getNil()); // do rb_mem_clear
		    ShiftableList vars = new ShiftableList(new ArrayList(tmp));
		    if (ruby.rubyScope.getLocalVars() != null) {
			vars.set(0, ruby.rubyScope.getLocalVars(-1));
	                vars.shift(1);
			for (int j = 0; j < i; j++) {
			    vars.set(j, ruby.rubyScope.getLocalVars(j));
			}
		    } else {
			vars.set(0, null);
			vars.shift(1);
		    }
			    
                    ruby.rubyScope.setLocalVars(vars);
                    ruby.rubyScope.setFlags(ruby.rubyScope.getFlags() | 1); // SCOPE_MALLOC;
                } else {
                    // VALUE[] vars = ruby.ruby_scope.local_vars-1;
                    // REALLOC_N(vars, VALUE, len+1);
                    // ruby_scope.local_vars = vars+1;
                    // rb_mem_clear(ruby_scope.local_vars+i, len-i);
                }
                // if (ruby.ruby_scope.local_tbl != null && ruby_scope.local_vars[-1] == 0) {
                //     free(ruby_scope.local_tbl);
                // }
                // ruby_scope.local_vars[-1] = 0;
                ruby.rubyScope.setLocalTbl(local_tbl());
            }
        }
        local_pop();
        // */
    }

    private RVarmap dyna_push() {
	RVarmap vars = ruby_dyna_vars;

	rb_dvar_push(null, null);
	lvtbl.dlev++;
	return vars;
    }

    private void dyna_pop(RVarmap vars) {
	lvtbl.dlev--;
	ruby_dyna_vars = vars;
    }

    private boolean dyna_in_block() {
	return lvtbl.dlev > 0;
    }

    void rb_parser_append_print() {
	ruby_eval_tree =
	    block_append(ruby_eval_tree,
			 NEW_FCALL(ID.rb_intern("print", ruby),
				   NEW_ARRAY(NEW_GVAR(ID.rb_intern("$_", ruby)))));
    }

    void rb_parser_while_loop(int chop, int split) {
	if (split != 0) {
	    ruby_eval_tree =
		block_append(NEW_GASGN(ID.rb_intern("$F", ruby),
				       NEW_CALL(NEW_GVAR(ID.rb_intern("$_", ruby)),
						ID.rb_intern("split", ruby), null)),
			     ruby_eval_tree);
	}
	if (chop != 0) {
	    ruby_eval_tree =
		block_append(NEW_CALL(NEW_GVAR(ID.rb_intern("$_", ruby)),
				      ID.rb_intern("chop!", ruby), null), ruby_eval_tree);
	}
	ruby_eval_tree = NEW_OPT_N(ruby_eval_tree);
    }





    VALUE rb_sym_all_symbols() {
        throw missing();
        /*
	VALUE ary = rb_ary_new2(sym_tbl.size());

	for (Iterator i = sym_tbl.entrySet().iterator(); i.hasNext();) {
	    Map.Entry e = (Map.Entry)i.next();
	    rb_ary_push(ary, ID2SYM((ID)e.getValue()));
	}
	return ary;
        */
    }

    boolean rb_is_const_id(ID id) {
	return id.is_const_id();
    }

    boolean rb_is_class_id(ID id) {
        return id.is_class_id();
    }

    boolean rb_is_instance_id(ID id) {
        return id.is_instance_id();
    }

    private void special_local_set(char c, VALUE val) {
	top_local_init();
	int cnt = local_cnt(c);
	top_local_setup();
	ruby.rubyScope.setLocalVars(cnt, val);
    }

    VALUE rb_backref_get() {
	if (ruby.rubyScope.getLocalVars() != null) {
	    return ruby.rubyScope.getLocalVars(1);
	}
	return Qnil;
    }

    void rb_backref_set(VALUE val) {
	if (ruby.rubyScope.getLocalVars() != null) {
	    ruby.rubyScope.setLocalVars(1, val);
	}
	else {
	    special_local_set('~', val);
	}
    }

    VALUE rb_lastline_get() {
	if (ruby.rubyScope.getLocalVars() != null) {
	    return ruby.rubyScope.getLocalVars(0);
	}
	return Qnil;
    }

    void rb_lastline_set(VALUE val) {
	if (ruby.rubyScope.getLocalVars() != null) {
	    ruby.rubyScope.setLocalVars(0, val);
	}
	else {
	    special_local_set('_', val);
	}
    }

    // -----------------------------------------------------------------------
    // macros from node.h
    // -----------------------------------------------------------------------

    NODE NEW_METHOD(NODE n, int x) {
        return rb_node_newnode(NODE_METHOD, new Integer(x), n, null);
    }
    NODE NEW_FBODY(NODE n, ID i, VALUE o) {
        return rb_node_newnode(NODE_FBODY, n, i, o);
    }
    NODE NEW_DEFN(ID i, NODE a, NODE d, int p) {
        return rb_node_newnode(NODE_DEFN, new Integer(p), i, NEW_RFUNC(a, d));
    }
    NODE NEW_DEFS(NODE r, ID i, NODE a, NODE d) {
        return rb_node_newnode(NODE_DEFS, r, i, NEW_RFUNC(a, d));
    }
    NODE NEW_CFUNC(Object f, int c) {
        return rb_node_newnode(NODE_CFUNC, f, new Integer(c), null);
    }
    NODE NEW_IFUNC(Object f, int c) {
        return rb_node_newnode(NODE_IFUNC, f, new Integer(c), null);
    }
    NODE NEW_RFUNC(NODE b1, NODE b2) {
	return NEW_SCOPE(block_append(b1,b2));
    }
    NODE NEW_SCOPE(NODE b) {
        return rb_node_newnode(NODE_SCOPE, local_tbl(), null, b);
    }
    NODE NEW_BLOCK(NODE a) {
        return rb_node_newnode(NODE_BLOCK, a, null, null);
    }
    NODE NEW_IF(NODE c, NODE t, NODE e) {
        return rb_node_newnode(NODE_IF, c, t, e);
    }
    NODE NEW_UNLESS(NODE c, NODE t, NODE e) {
	return NEW_IF(c, e, t);
    }
    NODE NEW_CASE(NODE h, NODE b) {
        return rb_node_newnode(NODE_CASE, h, b, null);
    }
    NODE NEW_WHEN(NODE c, NODE t, NODE e) {
        return rb_node_newnode(NODE_WHEN, c, t, e);
    }
    NODE NEW_OPT_N(NODE b) {
        return rb_node_newnode(NODE_OPT_N,null,b,null);
    }
    NODE NEW_WHILE(NODE c, NODE b, int n) {
        return rb_node_newnode(NODE_WHILE, c, b, new Integer(n));
    }
    NODE NEW_UNTIL(NODE c, NODE b, int n) {
        return rb_node_newnode(NODE_UNTIL, c, b, new Integer(n));
    }
    NODE NEW_FOR(NODE v, NODE i, NODE b) {
        return rb_node_newnode(NODE_FOR, v, b, i);
    }
    NODE NEW_ITER(NODE v, NODE i, NODE b) {
        return rb_node_newnode(NODE_ITER, v, b, i);
    }
    NODE NEW_BREAK(NODE s) {
        return rb_node_newnode(NODE_BREAK, s, null, null);
    }
    NODE NEW_NEXT(NODE s) {
        return rb_node_newnode(NODE_NEXT,s,null,null);
    }
    NODE NEW_REDO() {
        return rb_node_newnode(NODE_REDO,null,null,null);
    }
    NODE NEW_RETRY() {
        return rb_node_newnode(NODE_RETRY,null,null,null);
    }
    NODE NEW_BEGIN(NODE b) {
        return rb_node_newnode(NODE_BEGIN,null,b,null);
    }
    NODE NEW_RESCUE(NODE b, NODE res, NODE e) {
        return rb_node_newnode(NODE_RESCUE, b, res, e);
    }
    NODE NEW_RESBODY(NODE a, NODE ex, NODE n) {
        return rb_node_newnode(NODE_RESBODY, n, ex, a);
    }
    NODE NEW_ENSURE(NODE b, NODE en) {
        return rb_node_newnode(NODE_ENSURE,b,null,en);
    }
    NODE NEW_RETURN(NODE s) {
        return rb_node_newnode(NODE_RETURN,s,null,null);
    }
    NODE NEW_YIELD(NODE a) {
        return rb_node_newnode(NODE_YIELD,a,null,null);
    }
    NODE NEW_LIST(NODE a) {
	return NEW_ARRAY(a);
    }
    NODE NEW_ARRAY(NODE a) {
        return rb_node_newnode(NODE_ARRAY,a,new Integer(1),null);
    }
    NODE NEW_ZARRAY() {
        return rb_node_newnode(NODE_ZARRAY,null,null,null);
    }
    NODE NEW_HASH(NODE a) {
	return rb_node_newnode(NODE_HASH,a,null,null);
    }
    NODE NEW_NOT(NODE a) {
	return rb_node_newnode(NODE_NOT,null,a,null);
    }
    NODE NEW_MASGN(NODE l, NODE r) {
	return rb_node_newnode(NODE_MASGN,l,null,r);
    }
    NODE NEW_GASGN(ID v, NODE val) {
        return rb_node_newnode(NODE_GASGN,v,val,rb_global_entry(v));
    }
    NODE NEW_LASGN(ID v, NODE val) {
        return rb_node_newnode(NODE_LASGN,v,val,new Integer(local_cnt(v)));
    }
    NODE NEW_DASGN(ID v, NODE val) {
        return rb_node_newnode(NODE_DASGN,v,val,null);
    }
    NODE NEW_DASGN_CURR(ID v, NODE val) {
        return rb_node_newnode(NODE_DASGN_CURR,v,val,null);
    }
    NODE NEW_IASGN(ID v, NODE val) {
        return rb_node_newnode(NODE_IASGN,v,val,null);
    }
    NODE NEW_CDECL(ID v, NODE val) {
        return rb_node_newnode(NODE_CDECL,v,val,null);
    }
    NODE NEW_CVASGN(ID v, NODE val) {
        return rb_node_newnode(NODE_CVASGN,v,val,null);
    }
    NODE NEW_CVDECL(ID v, NODE val) {
        return rb_node_newnode(NODE_CVDECL,v,val,null);
    }
    NODE NEW_OP_ASGN1(NODE p, int id, NODE a) {
        return rb_node_newnode(NODE_OP_ASGN1,p,new Integer(id),a);
    }
    NODE NEW_OP_ASGN2(NODE r, ID i, int o, NODE val) {
        return rb_node_newnode(NODE_OP_ASGN2,r,val,NEW_OP_ASGN22(i,o));
    }
    NODE NEW_OP_ASGN22(ID i, int o) {
        return rb_node_newnode(NODE_OP_ASGN2,i,new Integer(o),i.rb_id_attrset(ruby));
    }
    NODE NEW_OP_ASGN_OR(NODE i,NODE val) {
        return rb_node_newnode(NODE_OP_ASGN_OR,i,val,null);
    }
    NODE NEW_OP_ASGN_AND(NODE i, NODE val) {
        return rb_node_newnode(NODE_OP_ASGN_AND,i,val,null);
    }
    NODE NEW_GVAR(ID v) {
        return rb_node_newnode(NODE_GVAR,v,null,rb_global_entry(v));
    }
    NODE NEW_LVAR(ID v) {
        return rb_node_newnode(NODE_LVAR,v,null,new Integer(local_cnt(v)));
    }
    NODE NEW_DVAR(ID v) {
        return rb_node_newnode(NODE_DVAR,v,null,null);
    }
    NODE NEW_IVAR(ID v) {
        return rb_node_newnode(NODE_IVAR,v,null,null);
    }
    NODE NEW_CONST(ID v) {
        return rb_node_newnode(NODE_CONST,v,null,null);
    }
    NODE NEW_CVAR(ID v) {
        return rb_node_newnode(NODE_CVAR,v,null,null);
    }
    NODE NEW_CVAR2(ID v) {
        return rb_node_newnode(NODE_CVAR2,v,null,null);
    }
    NODE NEW_NTH_REF(int n) {
	return rb_node_newnode(NODE_NTH_REF,null,new Integer(n),new Integer(local_cnt('~')));
    }
    NODE NEW_BACK_REF(int n) {
        return rb_node_newnode(NODE_BACK_REF,null,new Integer(n),new Integer(local_cnt('~')));
    }
    NODE NEW_MATCH(NODE c) {
        return rb_node_newnode(NODE_MATCH,c,null,null);
    }
    NODE NEW_MATCH2(NODE n1, NODE n2) {
        return rb_node_newnode(NODE_MATCH2,n1,n2,null);
    }
    NODE NEW_MATCH3(NODE r, NODE n2) {
        return rb_node_newnode(NODE_MATCH3,r,n2,null);
    }
    NODE NEW_LIT(VALUE l) {
        return rb_node_newnode(NODE_LIT,l,null,null);
    }
    NODE NEW_STR(VALUE s) {
        return rb_node_newnode(NODE_STR,s,null,null);
    }
    NODE NEW_DSTR(VALUE s) {
        return rb_node_newnode(NODE_DSTR,s,null,null);
    }
    NODE NEW_XSTR(VALUE s) {
        return rb_node_newnode(NODE_XSTR,s,null,null);
    }
    NODE NEW_DXSTR(VALUE s) {
        return rb_node_newnode(NODE_DXSTR,s,null,null);
    }
    NODE NEW_EVSTR(String s, int l) {
        return rb_node_newnode(NODE_EVSTR,rb_str_new(s,l),null,null);
    }
    NODE NEW_CALL(NODE r, ID m, NODE a) {
        return rb_node_newnode(NODE_CALL, r, m, a);
    }
    NODE NEW_FCALL(ID m, NODE a) {
        return rb_node_newnode(NODE_FCALL,null,m, a);
    }
    NODE NEW_VCALL(ID m) {
        return rb_node_newnode(NODE_VCALL,null,m,null);
    }
    NODE NEW_SUPER(NODE a) {
        return rb_node_newnode(NODE_SUPER,null,null,a);
    }
    NODE NEW_ZSUPER() {
        return rb_node_newnode(NODE_ZSUPER,null,null,null);
    }
    NODE NEW_ARGS(Integer f, Object o, Integer r) {
        return rb_node_newnode(NODE_ARGS,o,r,f);
    }
    NODE NEW_ARGSCAT(NODE a, NODE b) {
        return rb_node_newnode(NODE_ARGSCAT,a,b,null);
    }
    NODE NEW_ARGSPUSH(NODE a, NODE b) {
        return rb_node_newnode(NODE_ARGSPUSH,a,b,null);
    }
    NODE NEW_RESTARGS(NODE a) {
        return rb_node_newnode(NODE_RESTARGS,a,null,null);
    }
    NODE NEW_RESTARY(NODE a) {
        return rb_node_newnode(NODE_RESTARY,a,null,null);
    }
    NODE NEW_REXPAND(NODE a) {
        return rb_node_newnode(NODE_REXPAND,a,null,null);
    }
    NODE NEW_BLOCK_ARG(ID v) {
        return rb_node_newnode(NODE_BLOCK_ARG,v,null,new Integer(local_cnt(v)));
    }
    NODE NEW_BLOCK_PASS(NODE b) {
        return rb_node_newnode(NODE_BLOCK_PASS,null,b,null);
    }
    NODE NEW_ALIAS(ID n, ID o) {
        return rb_node_newnode(NODE_ALIAS,o,n,null);
    }
    NODE NEW_VALIAS(ID n, ID o) {
        return rb_node_newnode(NODE_VALIAS,o,n,null);
    }
    NODE NEW_UNDEF(ID i) {
        return rb_node_newnode(NODE_UNDEF,null,i,null);
    }
    NODE NEW_CLASS(ID n, NODE b, NODE s) {
        return rb_node_newnode(NODE_CLASS,n,NEW_SCOPE(b),(s));
    }
    NODE NEW_SCLASS(NODE r, NODE b) {
        return rb_node_newnode(NODE_SCLASS,r,NEW_SCOPE(b),null);
    }
    NODE NEW_MODULE(ID n, NODE b) {
        return rb_node_newnode(NODE_MODULE,n,NEW_SCOPE(b),null);
    }
    NODE NEW_COLON2(NODE c, ID i) {
        return rb_node_newnode(NODE_COLON2,c,i,null);
    }
    NODE NEW_COLON3(ID i) {
        return rb_node_newnode(NODE_COLON3,null,i,null);
    }
    /*NODE NEW_CREF(c) {
	return rb_node_newnode(NODE_CREF,null,null,c);
    }*/
    NODE NEW_DOT2(NODE b, NODE e) {
        return rb_node_newnode(NODE_DOT2,b,e,null);
    }
    NODE NEW_DOT3(NODE b, NODE e) {
        return rb_node_newnode(NODE_DOT3,b,e,null);
    }
    NODE NEW_ATTRSET(NODE a) {
        return rb_node_newnode(NODE_ATTRSET,a,null,null);
    }
    NODE NEW_SELF() {
        return rb_node_newnode(NODE_SELF,null,null,null);
    }
    NODE NEW_NIL() {
        return rb_node_newnode(NODE_NIL,null,null,null);
    }
    NODE NEW_TRUE() {
        return rb_node_newnode(NODE_TRUE,null,null,null);
    }
    NODE NEW_FALSE() {
        return rb_node_newnode(NODE_FALSE,null,null,null);
    }
    NODE NEW_DEFINED(NODE e) {
        return rb_node_newnode(NODE_DEFINED,e,null,null);
    }
    NODE NEW_NEWLINE(NODE n) {
        return rb_node_newnode(NODE_NEWLINE,null,null,n);
    }
    NODE NEW_PREEXE(NODE b) {
	return NEW_SCOPE(b);
    }
    NODE NEW_POSTEXE() {
        return rb_node_newnode(NODE_POSTEXE,null,null,null);
    }
    NODE NEW_DMETHOD(NODE b) {
        return rb_node_newnode(NODE_DMETHOD,null,null,b);
    }
    NODE NEW_BMETHOD(NODE b) {
        return rb_node_newnode(NODE_BMETHOD,null,null,b);
    }

    private static NODE rb_node_newnode(int type, Object o1, Object o2, Object o3) {
        return NODE.newNode(type, o1, o2, o3);
    }

    // -----------------------------------------------------------------------
    // scanner stuff
    // -----------------------------------------------------------------------

    //XXX yyInput implementation
    private int token = 0;

    public boolean advance () throws java.io.IOException {
        return (token = yylex()) != 0;
    }

    public int token () {
        return token;
    }

    public Object value () {
        return yyVal;
    }
    //XXX yyInput END

    private String lex_curline; // current line
    //private int lex_pbeg;     //XXX not needed
    private int lex_p;          // pointer in current line
    private int lex_pend;       // pointer to end of line
/*
    int yyerror(String msg) {
      char *p, *pe, *buf;
      int len, i;

      rb_compile_error("%s", msg);
      p = lex_p;
      while (lex_pbeg <= p) {
	if (*p == '\n') break;
	p--;
      }
      p++;

	pe = lex_p;
	while (pe < lex_pend) {
	    if (*pe == '\n') break;
	    pe++;
	}

	len = pe - p;
	if (len > 4) {
	    buf = ALLOCA_N(char, len+2);
	    MEMCPY(buf, p, char, len);
	    buf[len] = '\0';
	    rb_compile_error_append("%s", buf);

	    i = lex_p - p;
	    p = buf; pe = p + len;

	    while (p < pe) {
		if (*p != '\t') *p = ' ';
		p++;
	    }
	    buf[i] = '^';
	    buf[i+1] = '\0';
	    rb_compile_error_append("%s", buf);
	}

	return 0;
    }
*/
    private int heredoc_end;
    private boolean command_start = true;

    int ruby_in_compile = 0;
    boolean ruby__end__seen;    // XXX is this really needed?

    private VALUE ruby_debug_lines;

    NODE yycompile(String f, int line) {
	ID sl_id = ID.rb_intern("SCRIPT_LINES__", ruby);
	if (!compile_for_eval && rb_safe_level() == 0 &&
	    rb_const_defined(rb_cObject, sl_id)) {

	    VALUE hash = rb_const_get(rb_cObject, sl_id);
	    if (TYPE(hash) == T_HASH) {
		VALUE fname = rb_str_new2(f);
		ruby_debug_lines = rb_hash_aref(hash, fname);
		if (NIL_P(ruby_debug_lines)) {
		    ruby_debug_lines = rb_ary_new();
		    rb_hash_aset(hash, fname, ruby_debug_lines);
		}
	    }
	    if (line > 1) {
		VALUE str = rb_str_new(null,0);
		while (line > 1) {
		    rb_ary_push(ruby_debug_lines, str);
		    line--;
		}
	    }
	}

	ruby__end__seen = false;	// is there an __end__{} statement?
	ruby_eval_tree = null;		// parser stores NODEs here
	heredoc_end = 0;
	ruby_sourcefile = f;		// source file name
	ruby_in_compile = 1;

        try {
            yyparse(new yyInput() {
                public boolean advance() throws java.io.IOException {
                    return parse.this.advance();
                }
                public int token() { return parse.this.token(); }
                public Object value() { return parse.this.value(); }
            }, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

	ruby_debug_lines = null;	// remove debug info
	compile_for_eval = false;
	ruby_in_compile = 0;
	cond_stack = 0;			// reset stuff for next compile
	cmdarg_stack = 0;		// reset stuff for next compile
	command_start = true;		// reset stuff for next compile
	class_nest = 0;
	in_single = 0;
	in_def = 0;
	cur_mid = null;

        return ruby_eval_tree;
    }

    // separate a Ruby string into lines...

    /** beginning of the next line */
    private int lex_gets_ptr;
    private VALUE lex_input;		/* non-nil if File */
    private VALUE lex_lastline; 	/* gc protect */

    private VALUE lex_gets_str(VALUE _s) {
	String s = ((RubyString)_s).getString();
	if (lex_gets_ptr != 0) {
	    if (s.length() == lex_gets_ptr)
		return Qnil;
	    s = s.substring(lex_gets_ptr);
	}
	int end = 0;
	while (end < s.length()) {
	    if (s.charAt(end++) == '\n') break;
	}
	lex_gets_ptr += end;
	return rb_str_new(s, end);
    }

    /** true, if scanner source is a file
	and false, if lex_get_str() shall be used. */
    private boolean lex_file_io;

    /** Returns in next line either from file or from a string. */
    private VALUE lex_getline() {
	VALUE line;
	if (lex_file_io)
	    // uses rb_io_gets(lex_input)
	    throw new Error();
	else
	    line = lex_gets_str(lex_input);

	if (ruby_debug_lines != null && !NIL_P(line)) {
	    rb_ary_push(ruby_debug_lines, line);
	}
        return line;
    }

    private void init_for_scanner(String s) {
	lex_file_io = false;
	lex_gets_ptr = 0;
        lex_input = new RubyString(ruby, s);
        lex_p = lex_pend = 0;
        ruby_sourceline = 0;
        compile_for_eval = ruby_in_eval;
	ruby__end__seen = false;// is there an __end__{} statement?
	heredoc_end = 0;
	ruby_in_compile = 1;
    }

    /** Compiles the given RString "s" */
    NODE rb_compile_string(String f, VALUE s, int line) {
	lex_file_io = false;
	lex_gets_ptr = 0;
        lex_input = s;
        lex_p = lex_pend = 0;
        ruby_sourceline = line - 1;

        compile_for_eval = ruby_in_eval;

       return yycompile(f, line);
    }

    /** Compiles the given Java String "s" */
    NODE rb_compile_cstr(String f, String s, int len, int line) {
        return rb_compile_string(f, rb_str_new(s, len), line);
    }

    /** Compiles the given file "file" */
    NODE rb_compile_file(String f, VALUE file, int start) {
	lex_file_io = true;
        lex_input = file;
        lex_p = lex_pend = 0;
        ruby_sourceline = start - 1;

        return yycompile(f, start);
    }

    /** Returns the next character from input */
    private int nextc() {
        int c;

        if (lex_p == lex_pend) {
    	    if (lex_input != null) {
	        VALUE v = lex_getline();

	        if (NIL_P(v)) return -1;
	        if (heredoc_end > 0) {
		    ruby_sourceline = heredoc_end;
		    heredoc_end = 0;
	        }
	        ruby_sourceline++;
                lex_curline = ((RubyString)v).getString();
	        lex_p    = 0;
	        lex_pend = lex_curline.length();
                if (lex_curline.startsWith("__END__") && (lex_pend == 7 || lex_curline.charAt(7) == '\n' || lex_curline.charAt(7) == '\r')) {
		    ruby__end__seen = true;
		    lex_lastline = null;
		    return -1;
	        }
	        lex_lastline = v;
	    }
	    else {
	        lex_lastline = null;
	        return -1;
	    }
        }
        c = lex_curline.charAt(lex_p++);
        if (c == '\r' && lex_p <= lex_pend && lex_curline.charAt(lex_p) == '\n') {
	    lex_p++;
	    c = '\n';
        }

        return c;
    }

    /** Puts back the given character so that nextc() will answer it next time
     *  it'll be called. */
    private void pushback(int c) {
        if (c == -1) return;
        lex_p--;
    }

    /** Returns true if the given character is the current one in the input stream */
    private boolean peek(int c) {
        return lex_p != lex_pend && c == lex_curline.charAt(lex_p);
    }

    // deal with tokens..................

    private StringBuffer tokenbuf;

    private String tok() { return tokenbuf.toString(); }
    private int toklen() { return tokenbuf.length(); }
    private void tokfix() { /*nothing to do*/ }
    private char toklast() { return tokenbuf.charAt(toklen() - 1); }

    private void newtok() {
        tokenbuf = new StringBuffer(60);
    }

    private void tokadd(int c) {
        tokenbuf.append((char)c);
    }

    // yylex helpers...................

    private int read_escape() {
        int c;

        switch (c = nextc()) {
        case '\\':	/* Backslash */
            return c;

        case 'n':	/* newline */
            return '\n';

        case 't':	/* horizontal tab */
            return '\t';

        case 'r':	/* carriage-return */
            return '\r';

        case 'f':	/* form-feed */
            return '\f';

	case 'v':	/* vertical tab */
            return '\013';

	case 'a':	/* alarm(bell) */
            return '\007';

	case 'e':	/* escape */
            return '\033';

	case '0': case '1': case '2': case '3': /* octal constant */
	case '4': case '5': case '6': case '7':
            {
                int cc = 0;

                pushback(c);
                for (int i=0; i<3; i++) {
                    c = nextc();
                    if (c == -1) {
			// goto eof
			yyerror("Invalid escape character syntax");
			return '\0';
		    }
                    if (c < '0' || '7' < c) {
                        pushback(c);
                        break;
                    }
                    cc = cc * 8 + c - '0';
                }
                c = cc;
            }
            return c;

	case 'x':	/* hex constant */
            {
                int[] numlen = new int[1];
                c = (int)scan_hex(lex_curline, lex_p, 2, numlen);
                lex_p += numlen[0];
            }
            return c;

	case 'b':	/* backspace */
            return '\010';

	case 's':	/* space */
            return ' ';

	case 'M':
            if ((c = nextc()) != '-') {
                yyerror("Invalid escape character syntax");
                pushback(c);
                return '\0';
            }
            if ((c = nextc()) == '\\') {
                return read_escape() | 0x80;
            }
            else if (c == -1) {
		// goto eof
		yyerror("Invalid escape character syntax");
                return '\0';
	    }
            else {
                return ((c & 0xff) | 0x80);
            }

	case 'C':
            if ((c = nextc()) != '-') {
                yyerror("Invalid escape character syntax");
                pushback(c);
                return '\0';
            }
	case 'c':
            if ((c = nextc())== '\\') {
                c = read_escape();
            }
            else if (c == '?')
                return 0177;
            else if (c == -1) {
		// goto eof
		yyerror("Invalid escape character syntax");
		return '\0';
	    }
            return c & 0x9f;

	case -1:
	    // eof:
            yyerror("Invalid escape character syntax");
            return '\0';

	default:
            return c;
        }
    }

    private int tokadd_escape() {
        int c;

        switch (c = nextc()) {
	case '\n':
            return 0;		/* just ignore */

	case '0': case '1': case '2': case '3': /* octal constant */
	case '4': case '5': case '6': case '7':
            {
                int i;

                tokadd('\\');
                tokadd(c);
                for (i=0; i<2; i++) {
                    c = nextc();
                    if (c == -1) {
			//goto eof;
			yyerror("Invalid escape character syntax");
			return -1;
		    }
                    if (c < '0' || '7' < c) {
                        pushback(c);
                        break;
                    }
                    tokadd(c);
                }
            }
            return 0;

	case 'x':	/* hex constant */
            {
                tokadd('\\');
                tokadd(c);

                int[] numlen = new int[1];
                scan_hex(lex_curline, lex_p, 2, numlen);
                while (numlen[0]-- != 0)
                    tokadd(nextc());
            }
            return 0;

	case 'M':
            if ((c = nextc()) != '-') {
                yyerror("Invalid escape character syntax");
                pushback(c);
                return 0;
            }
            tokadd('\\'); tokadd('M'); tokadd('-');
            //goto escaped;
            if ((c = nextc()) == '\\') {
                return tokadd_escape();
            }
            else if (c == -1) {
		// goto eof;
		yyerror("Invalid escape character syntax");
		return -1;
	    }
            tokadd(c);
            return 0;

	case 'C':
            if ((c = nextc()) != '-') {
                yyerror("Invalid escape character syntax");
                pushback(c);
                return 0;
            }
            tokadd('\\'); tokadd('C'); tokadd('-');
            //goto escaped;
            if ((c = nextc()) == '\\') {
                return tokadd_escape();
            }
            else if (c == -1) {
		// goto eof;
		yyerror("Invalid escape character syntax");
		return -1;
	    }
            tokadd(c);
            return 0;

	case 'c':
            tokadd('\\'); tokadd('c');
            //escaped:
            if ((c = nextc()) == '\\') {
                return tokadd_escape();
            }
            else if (c == -1) {
		// goto eof;
		yyerror("Invalid escape character syntax");
		return -1;
	    }
            tokadd(c);
            return 0;

	case -1:
	    // eof:
	    yyerror("Invalid escape character syntax");
            return -1;

	default:
            tokadd('\\');
            tokadd(c);
        }
        return 0;
    }

    private int parse_regx(int term, int paren) {
        int c;
        char kcode = 0;
        boolean once = false;
        int nest = 0;
        int options = 0;
        int re_start = ruby_sourceline;
        NODE list = null;

        newtok();
        regx_end:
        while ((c = nextc()) != -1) {
            if (c == term && nest == 0) {
                break regx_end;
            }

            switch (c) {
	    case '#':
                list = str_extend(list, term);
                if (list == NODE.MINUS_ONE) return 0;
                continue;

	    case '\\':
                if (tokadd_escape() < 0)
                    return 0;
                continue;

	    case -1:
                //goto unterminated;
                ruby_sourceline = re_start;
                rb_compile_error("unterminated regexp meets end of file");
                return 0;

	    default:
                if (paren != 0)  {
		    if (c == paren) nest++;
		    if (c == term) nest--;
                }
                /*
                if (ismbchar(c)) {
                    int i, len = mbclen(c)-1;

                    for (i = 0; i < len; i++) {
                        tokadd(c);
                        c = nextc();
                    }
                }
                */
                break;
            }
            tokadd(c);
        }

        end_options:
        for (;;) {
            switch (c = nextc()) {
            case 'i':
                options |= RE_OPTION_IGNORECASE;
                break;
            case 'x':
                options |= RE_OPTION_EXTENDED;
                break;
            case 'p':	/* /p is obsolete */
                rb_warn("/p option is obsolete; use /m\n\tnote: /m does not change ^, $ behavior");
                options |= RE_OPTION_POSIXLINE;
                break;
            case 'm':
                options |= RE_OPTION_MULTILINE;
                break;
            case 'o':
                once = true;
                break;
            case 'n':
                kcode = 16;
                break;
            case 'e':
                kcode = 32;
                break;
            case 's':
                kcode = 48;
                break;
            case 'u':
                kcode = 64;
                break;
            default:
                pushback(c);
                break end_options;
            }
        }

        tokfix();
        lex_state = EXPR_END;
        if (list != null) {
            nd_set_line(list, re_start);
            if (toklen() > 0) {
                VALUE ss = rb_str_new(tok(), toklen());
                list_append(list, NEW_STR(ss));
            }
            nd_set_type(list, once?NODE_DREGX_ONCE:NODE_DREGX);
            list.nd_cflag(new RubyId(ruby, options | kcode));
            yyVal = (NODE)list;
            return tDREGEXP;
        }
        else {
            yyVal = (VALUE)rb_reg_new(tok(), toklen(), options | kcode);
            return tREGEXP;
        }
        //unterminated:
        //ruby_sourceline = re_start;
        //rb_compile_error("unterminated regexp meets end of file");
        //return 0;
    }

    private int parse_string(int func, int term, int paren) {
        int c;
        NODE list = null;
        int strstart;
        int nest = 0;

        if (func == '\'') {
            return parse_qstring(term, paren);
        }
        if (func == 0) {
	    /* read 1 line for heredoc */
	    /* -1 for chomp */
            yyVal = (VALUE)rb_str_new(lex_curline, lex_pend - 1);
            lex_p = lex_pend;
            return tSTRING;
        }
        strstart = ruby_sourceline;
        newtok();
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
	        //unterm_str:
                ruby_sourceline = strstart;
                rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*
            if (ismbchar(c)) {
                int i, len = mbclen(c)-1;

                for (i = 0; i < len; i++) {
                    tokadd(c);
                    c = nextc();
                }
            }
            else*/ if (c == '#') {
                list = str_extend(list, term);
                if (list == NODE.MINUS_ONE) {
                    //goto unterm_str;
                    ruby_sourceline = strstart;
                    rb_compile_error("unterminated string meets end of file");
                    return 0;
                }
                continue;
            }
            else if (c == '\\') {
                c = nextc();
                if (c == '\n')
                    continue;
                if (c == term) {
                    tokadd(c);
                }
                else {
                    pushback(c);
                    if (func != '"') tokadd('\\');
                    tokadd(read_escape());
                }
                continue;
            }
            if (paren != 0) {
                if (c == paren) nest++;
                if (c == term && nest-- == 0) break;
            }
            tokadd(c);
        }

        tokfix();
        lex_state = EXPR_END;

        if (list != null) {
            nd_set_line(list, strstart);
            if (toklen() > 0) {
                VALUE ss = rb_str_new(tok(), toklen());
                list_append(list, NEW_STR(ss));
            }
            yyVal = (NODE)list;
            if (func == '`') {
                nd_set_type(list, NODE.NODE_DXSTR);
                return tDXSTRING;
            }
            else {
                return tDSTRING;
            }
        }
        else {
            yyVal = (VALUE)rb_str_new(tok(), toklen());
            return (func == '`') ? tXSTRING : tSTRING;
        }
    }

    private int parse_qstring(int term, int paren) {
        int strstart;
        int c;
        int nest = 0;

        strstart = ruby_sourceline;
        newtok();
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                ruby_sourceline = strstart;
                rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*if (ismbchar(c)) {
                int i, len = mbclen(c)-1;

                for (i = 0; i < len; i++) {
                    tokadd(c);
                    c = nextc();
                }
            }
            else*/ if (c == '\\') {
                c = nextc();
                switch (c) {
		case '\n':
                    continue;

		case '\\':
                    c = '\\';
                    break;

		default:
                    /* fall through */
                    if (c == term || (paren!=0 && c == paren)) {
                        tokadd(c);
                        continue;
                    }
                    tokadd('\\');
                }
            }
            if (paren!=0) {
                if (c == paren) nest++;
                if (c == term && nest-- == 0) break;
            }
            tokadd(c);
        }

        tokfix();
        yyVal = (VALUE)rb_str_new(tok(), toklen());
        lex_state = EXPR_END;
        return tSTRING;
    }

    private int parse_quotedwords(int term, int paren) {
        NODE qwords = null;
        int strstart;
        int c;
        int nest = 0;

        strstart = ruby_sourceline;
        newtok();

        c = nextc();
        while (ISSPACE(c))
            c = nextc();	/* skip preceding spaces */
        pushback(c);
        while ((c = nextc()) != term || nest > 0) {
            if (c == -1) {
                ruby_sourceline = strstart;
                rb_compile_error("unterminated string meets end of file");
                return 0;
            }
            /*if (ismbchar(c)) {
                int i, len = mbclen(c)-1;

                for (i = 0; i < len; i++) {
                    tokadd(c);
                    c = nextc();
                }
            }
            else*/ if (c == '\\') {
                c = nextc();
                switch (c) {
		case '\n':
                    continue;
		case '\\':
                    c = '\\';
                    break;
		default:
                    if (c == term || (paren!=0 && c == paren)) {
                        tokadd(c);
                        continue;
                    }
                    if (!ISSPACE(c))
                        tokadd('\\');
                    break;
                }
            }
            else if (ISSPACE(c)) {
                NODE str;

                tokfix();
                str = NEW_STR(rb_str_new(tok(), toklen()));
                newtok();
                if (qwords == null) qwords = NEW_LIST(str);
                else list_append(qwords, str);
                c = nextc();
                while (ISSPACE(c))
                    c = nextc();	/* skip continuous spaces */
                pushback(c);
                continue;
            }
            if (paren != 0) {
                if (c == paren) nest++;
                if (c == term && nest-- == 0) break;
            }
            tokadd(c);
        }

        tokfix();
        if (toklen() > 0) {
            NODE str = NEW_STR(rb_str_new(tok(), toklen()));
            if (qwords == null) qwords = NEW_LIST(str);
            else list_append(qwords, str);
        }
        if (qwords == null) qwords = NEW_ZARRAY();
        yyVal = (NODE)qwords;
        lex_state = EXPR_END;
        return tDSTRING;
    }

    private int here_document(int term, int indent) {
        throw new Error("not supported yet");
    }
/*
    private int here_document(int term, int indent) {
        int c;
        //char *eos, *p;
        int len;
        VALUE str;
        VALUE line = 0;
        VALUE lastline_save;
        int offset_save;
        NODE *list = 0;
        int linesave = ruby_sourceline;

        newtok();
        switch (term) {
	case '\'':
	case '"':
	case '`':
            while ((c = nextc()) != term) {
                tokadd(c);
            }
            if (term == '\'') term = 0;
            break;

	default:
            c = term;
            term = '"';
            if (!is_identchar(c)) {
                rb_warn("use of bare << to mean <<\"\" is deprecated");
                break;
            }
            while (is_identchar(c)) {
                tokadd(c);
                c = nextc();
            }
            pushback(c);
            break;
        }
        tokfix();
        lastline_save = lex_lastline;
        offset_save = lex_p - lex_pbeg;
        eos = strdup(tok());
        len = strlen(eos);

        str = rb_str_new(0,0);
        for (;;) {
            lex_lastline = line = lex_getline();
            if (NIL_P(line)) {
	        //error:
                ruby_sourceline = linesave;
                rb_compile_error("can't find string \"%s\" anywhere before EOF", eos);
		free(eos);
		return 0;
            }
            ruby_sourceline++;
            p = RSTRING(line).ptr;
            if (indent) {
                while (*p && (*p == ' ' || *p == '\t')) {
                    p++;
                }
            }
            if (strncmp(eos, p, len) == 0) {
                if (p[len] == '\n' || p[len] == '\r')
                    break;
                if (len == RSTRING(line).len)
                    break;
            }

            lex_pbeg = lex_p = RSTRING(line).ptr;
            lex_pend = lex_p + RSTRING(line).len;
	retry:for(;;) {
            switch (parse_string(term, '\n', '\n')) {
	    case tSTRING:
	    case tXSTRING:
                rb_str_cat2((VALUE)yyVal, "\n");
                if (!list) {
                    rb_str_append(str, (VALUE)yyVal);
                }
                else {
                    list_append(list, NEW_STR((VALUE)yyVal));
                }
                break;
	    case tDSTRING:
                if (!list) list = NEW_DSTR(str);
                // fall through
	    case tDXSTRING:
                if (!list) list = NEW_DXSTR(str);

                list_append((NODE)yyVal, NEW_STR(rb_str_new2("\n")));
                nd_set_type((NODE)yyVal, NODE_STR);
                yyVal = (NODE)NEW_LIST((NODE)yyVal);
                ((NODE)yyVal).nd_next() = ((NODE)yyVal).nd_head().nd_next();
                list_concat(list, (NODE)yyVal);
                break;

	    case 0:
                ruby_sourceline = linesave;
                rb_compile_error("can't find string \"%s\" anywhere before EOF", eos);
		free(eos);
		return 0;
            }
            if (lex_p != lex_pend) {
                continue retry;
            }
            break retry;}
        }
        free(eos);
        lex_lastline = lastline_save;
        lex_pbeg = RSTRING(lex_lastline).ptr;
        lex_pend = lex_pbeg + RSTRING(lex_lastline).len;
        lex_p = lex_pbeg + offset_save;

        lex_state = EXPR_END;
        heredoc_end = ruby_sourceline;
        ruby_sourceline = linesave;

        if (list) {
            nd_set_line(list, linesave+1);
            yyVal = (NODE)list;
        }
        switch (term) {
	case '\0':
	case '\'':
	case '"':
            if (list) return tDSTRING;
            yyVal = (VALUE)str;
            return tSTRING;
	case '`':
            if (list) return tDXSTRING;
            yyVal = (VALUE)str;
            return tXSTRING;
        }
        return 0;
    }
*/

    private void arg_ambiguous() {
        rb_warning("ambiguous first argument; make sure");
    }

    private boolean IS_ARG() {
        return lex_state == EXPR_ARG || lex_state == EXPR_CMDARG;
    }

    /** Returns the next token.  Also sets yyVal is needed. */
    private int yylex() {
        int c;
        boolean space_seen = false;
        boolean cmd_state;
        kwtable kw;

        cmd_state = command_start;
        command_start = false;
    retry:for(;;) {
        switch (c = nextc()) {
	case '\0':		/* NUL */
	case '\004':		/* ^D */
	case '\032':		/* ^Z */
	case -1:		/* end of script. */
            return 0;

            /* white spaces */
	case ' ': case '\t': case '\f': case '\r':
	case '\013': /* '\v' */
            space_seen = true;
            continue retry;

	case '#':		/* it's a comment */
            while ((c = nextc()) != '\n') {
                if (c == -1)
                    return 0;
            }
            /* fall through */
	case '\n':
            switch (lex_state) {
	    case EXPR_BEG:
	    case EXPR_FNAME:
	    case EXPR_DOT:
                continue retry;
	    default:
                break;
            }
            command_start = true;
            lex_state = EXPR_BEG;
            return '\n';

	case '*':
            if ((c = nextc()) == '*') {
                lex_state = EXPR_BEG;
                if (nextc() == '=') {
                    yyVal = new RubyId(ruby, tPOW);
                    return tOP_ASGN;
                }
                pushback(c);
                return tPOW;
            }
            if (c == '=') {
                yyVal = new RubyId(ruby, '*');
                lex_state = EXPR_BEG;
                return tOP_ASGN;
            }
            pushback(c);
            if (IS_ARG() && space_seen && !ISSPACE(c)){
                rb_warning("`*' interpreted as argument prefix");
                c = tSTAR;
            }
            else if (lex_state == EXPR_BEG || lex_state == EXPR_MID) {
                c = tSTAR;
            }
            else {
                c = '*';
            }
            lex_state = EXPR_BEG;
            return c;

	case '!':
            lex_state = EXPR_BEG;
            if ((c = nextc()) == '=') {
                return tNEQ;
            }
            if (c == '~') {
                return tNMATCH;
            }
            pushback(c);
            return '!';

	case '=':
            if (lex_p == 1) {
                /* skip embedded rd document */
                if (lex_curline.startsWith("=begin") && (lex_pend == 6 || ISSPACE(lex_curline.charAt(6)))) {
                    for (;;) {
                        lex_p = lex_pend;
                        c = nextc();
                        if (c == -1) {
                            rb_compile_error("embedded document meets end of file");
                            return 0;
                        }
                        if (c != '=') continue;
                        if (lex_curline.substring(lex_p, lex_p + 3).equals("end") &&
                            (lex_p + 3 == lex_pend || ISSPACE(lex_curline.charAt(lex_p + 3)))) {
                            break;
                        }
                    }
                    lex_p = lex_pend;
                    continue retry;
                }
            }

            lex_state = EXPR_BEG;
            if ((c = nextc()) == '=') {
                if ((c = nextc()) == '=') {
                    return tEQQ;
                }
                pushback(c);
                return tEQ;
            }
            if (c == '~') {
                return tMATCH;
            }
            else if (c == '>') {
                return tASSOC;
            }
            pushback(c);
            return '=';

	case '<':
            c = nextc();
            if (c == '<' &&
                lex_state != EXPR_END &&
                lex_state != EXPR_ENDARG
                && lex_state != EXPR_CLASS &&
                (!IS_ARG() || space_seen)) {
                int c2 = nextc();
                int indent = 0;
                if (c2 == '-') {
                    indent = 1;
                    c2 = nextc();
                }
                if (!ISSPACE(c2) && ("\"'`".indexOf(c2) != -1 || is_identchar(c2))) {
                    return here_document(c2, indent);
                }
                pushback(c2);
            }
            lex_state = EXPR_BEG;
            if (c == '=') {
                if ((c = nextc()) == '>') {
                    return tCMP;
                }
                pushback(c);
                return tLEQ;
            }
            if (c == '<') {
                if (nextc() == '=') {
                    yyVal = new RubyId(ruby, tLSHFT);
                    return tOP_ASGN;
                }
                pushback(c);
                return tLSHFT;
            }
            pushback(c);
            return '<';

	case '>':
            lex_state = EXPR_BEG;
            if ((c = nextc()) == '=') {
                return tGEQ;
            }
            if (c == '>') {
                if ((c = nextc()) == '=') {
                    yyVal = new RubyId(ruby, tRSHFT);
                    return tOP_ASGN;
                }
                pushback(c);
                return tRSHFT;
            }
            pushback(c);
            return '>';

	case '"':
            return parse_string(c,c,c);
	case '`':
            if (lex_state == EXPR_FNAME) return c;
            if (lex_state == EXPR_DOT) return c;
            return parse_string(c,c,c);

	case '\'':
            return parse_qstring(c,0);

	case '?':
            if (lex_state == EXPR_END || lex_state == EXPR_ENDARG) {
                lex_state = EXPR_BEG;
                return '?';
            }
            c = nextc();
            if (c == -1 || c == 10) {
                rb_compile_error("incomplete character syntax");
                return 0;
            }
            if (IS_ARG() && ISSPACE(c)){
                pushback(c);
                lex_state = EXPR_BEG;
                return '?';
            }
            if (c == '\\') {
                c = read_escape();
            }
            c &= 0xff;
            yyVal = (VALUE)INT2FIX(c);
            lex_state = EXPR_END;
            return tINTEGER;

	case '&':
            if ((c = nextc()) == '&') {
                lex_state = EXPR_BEG;
                if ((c = nextc()) == '=') {
                    yyVal = new RubyId(ruby, tANDOP);
                    return tOP_ASGN;
                }
                pushback(c);
                return tANDOP;
            }
            else if (c == '=') {
                yyVal = new RubyId(ruby, '&');
                lex_state = EXPR_BEG;
                return tOP_ASGN;
            }
            pushback(c);
            if (IS_ARG() && space_seen && !ISSPACE(c)){
                rb_warning("`&' interpeted as argument prefix");
                c = tAMPER;
            }
            else if (lex_state == EXPR_BEG || lex_state == EXPR_MID) {
                c = tAMPER;
            }
            else {
                c = '&';
            }
            lex_state = EXPR_BEG;
            return c;

	case '|':
            lex_state = EXPR_BEG;
            if ((c = nextc()) == '|') {
                if ((c = nextc()) == '=') {
                    yyVal = new RubyId(ruby, tOROP);
                    return tOP_ASGN;
                }
                pushback(c);
                return tOROP;
            }
            else if (c == '=') {
                yyVal = new RubyId(ruby, '|');
                return tOP_ASGN;
            }
            pushback(c);
            return '|';

	case '+':
            c = nextc();
            if (lex_state == EXPR_FNAME || lex_state == EXPR_DOT) {
                if (c == '@') {
                    return tUPLUS;
                }
                pushback(c);
                return '+';
            }
            if (c == '=') {
                lex_state = EXPR_BEG;
                yyVal = new RubyId(ruby, '+');
                return tOP_ASGN;
            }
            if (lex_state == EXPR_BEG || lex_state == EXPR_MID ||
                (IS_ARG() && space_seen && !ISSPACE(c))) {
                if (IS_ARG()) arg_ambiguous();
                lex_state = EXPR_BEG;
                pushback(c);
                if (Character.isDigit((char)c)) {
                    c = '+';
                    return start_num(c);
                }
                return tUPLUS;
            }
            lex_state = EXPR_BEG;
            pushback(c);
            return '+';

	case '-':
            c = nextc();
            if (lex_state == EXPR_FNAME || lex_state == EXPR_DOT) {
                if (c == '@') {
                    return tUMINUS;
                }
                pushback(c);
                return '-';
            }
            if (c == '=') {
                lex_state = EXPR_BEG;
                yyVal = new RubyId(ruby, '-');
                return tOP_ASGN;
            }
            if (lex_state == EXPR_BEG || lex_state == EXPR_MID ||
                (IS_ARG() && space_seen && !ISSPACE(c))) {
                if (IS_ARG()) arg_ambiguous();
                lex_state = EXPR_BEG;
                pushback(c);
                if (Character.isDigit((char)c)) {
                    c = '-';
                    return start_num(c);
                }
                return tUMINUS;
            }
            lex_state = EXPR_BEG;
            pushback(c);
            return '-';

	case '.':
            lex_state = EXPR_BEG;
            if ((c = nextc()) == '.') {
                if ((c = nextc()) == '.') {
                    return tDOT3;
                }
                pushback(c);
                return tDOT2;
            }
            pushback(c);
            if (!Character.isDigit((char)c)) {
                lex_state = EXPR_DOT;
                return '.';
            }
            c = '.';
            /* fall through */

	//start_num:
	case '0': case '1': case '2': case '3': case '4':
	case '5': case '6': case '7': case '8': case '9':
            return start_num(c);

	case ']':
	case '}':
	case ')':
            COND_LEXPOP();
            CMDARG_LEXPOP();
            lex_state = EXPR_END;
            return c;

	case ':':
            c = nextc();
            if (c == ':') {
                if (lex_state == EXPR_BEG ||  lex_state == EXPR_MID ||
                    (IS_ARG() && space_seen)) {
                    lex_state = EXPR_BEG;
                    return tCOLON3;
                }
                lex_state = EXPR_DOT;
                return tCOLON2;
            }
            pushback(c);
            if (lex_state == EXPR_END || lex_state == EXPR_ENDARG || ISSPACE(c)) {
                lex_state = EXPR_BEG;
                return ':';
            }
            lex_state = EXPR_FNAME;
            return tSYMBEG;

	case '/':
            if (lex_state == EXPR_BEG || lex_state == EXPR_MID) {
                return parse_regx('/', '/');
            }
            if ((c = nextc()) == '=') {
                lex_state = EXPR_BEG;
                yyVal = new RubyId(ruby, '/');
                return tOP_ASGN;
            }
            pushback(c);
            if (IS_ARG() && space_seen) {
                if (!ISSPACE(c)) {
                    arg_ambiguous();
                    return parse_regx('/', '/');
                }
            }
            lex_state = EXPR_BEG;
            return '/';

	case '^':
            lex_state = EXPR_BEG;
            if ((c = nextc()) == '=') {
                yyVal = new RubyId(ruby, '^');
                return tOP_ASGN;
            }
            pushback(c);
            return '^';

	case ';':
            command_start = true;
	case ',':
            lex_state = EXPR_BEG;
            return c;

	case '~':
            if (lex_state == EXPR_FNAME || lex_state == EXPR_DOT) {
                if ((c = nextc()) != '@') {
                    pushback(c);
                }
            }
            lex_state = EXPR_BEG;
            return '~';

	case '(':
            command_start = true;
            if (lex_state == EXPR_BEG || lex_state == EXPR_MID) {
                c = tLPAREN;
            }
            else if (space_seen) {
                if (lex_state == EXPR_CMDARG) {
                    c = tLPAREN_ARG;
                }
                else if (lex_state == EXPR_ARG) {
                    rb_warning("%s (...) interpreted as method call", tok());
                    c = tLPAREN_ARG;
                }
            }
            COND_PUSH(0);
            CMDARG_PUSH(0);
            lex_state = EXPR_BEG;
            return c;

	case '[':
            if (lex_state == EXPR_FNAME || lex_state == EXPR_DOT) {
                if ((c = nextc()) == ']') {
                    if ((c = nextc()) == '=') {
                        return tASET;
                    }
                    pushback(c);
                    return tAREF;
                }
                pushback(c);
                return '[';
            }
            else if (lex_state == EXPR_BEG || lex_state == EXPR_MID) {
                c = tLBRACK;
            }
            else if (IS_ARG() && space_seen) {
                c = tLBRACK;
            }
            lex_state = EXPR_BEG;
            COND_PUSH(0);
            CMDARG_PUSH(0);
            return c;

	case '{':
            if (!IS_ARG()) {
                if (space_seen && lex_state == EXPR_ENDARG)
                    c = tLBRACE_ARG;
                if (lex_state != EXPR_END && lex_state != EXPR_ENDARG)
                    c = tLBRACE;
            }
            COND_PUSH(0);
            CMDARG_PUSH(0);
            lex_state = EXPR_BEG;
            return c;

	case '\\':
            c = nextc();
            if (c == '\n') {
                space_seen = true;
                continue retry; /* skip \\n */
            }
            pushback(c);
            return '\\';

	case '%':
	    quotation:for(;;) {
            if (lex_state == EXPR_BEG || lex_state == EXPR_MID) {
                int term;
                int paren;

                c = nextc();
                if (!Character.isLetterOrDigit((char)c)) {
                    term = c;
                    c = 'Q';
                }
                else {
                    term = nextc();
                }
                if (c == -1 || term == -1) {
                    rb_compile_error("unterminated quoted string meets end of file");
                    return 0;
                }
                paren = term;
                if (term == '(') term = ')';
                else if (term == '[') term = ']';
                else if (term == '{') term = '}';
                else if (term == '<') term = '>';
                else paren = 0;

                switch (c) {
		case 'Q':
                    return parse_string('"', term, paren);

		case 'q':
                    return parse_qstring(term, paren);

		case 'w':
                    return parse_quotedwords(term, paren);

		case 'x':
                    return parse_string('`', term, paren);

		case 'r':
                    return parse_regx(term, paren);

		default:
                    yyerror("unknown type of %string");
                    return 0;
                }
            }
            if ((c = nextc()) == '=') {
                yyVal = new RubyId(ruby, '%');
                return tOP_ASGN;
            }
            if (IS_ARG() && space_seen && !ISSPACE(c)) {
                pushback(c);
                continue quotation;
            }
            break quotation;
	    }
            lex_state = EXPR_BEG;
            pushback(c);
            return '%';

	case '$':
            lex_state = EXPR_END;
            newtok();
            c = nextc();
            switch (c) {
	    case '_':		/* $_: last read line string */
                c = nextc();
                if (is_identchar(c)) {
                    tokadd('$');
                    tokadd('_');
                    break;
                }
                pushback(c);
                c = '_';
                /* fall through */
	    case '~':		/* $~: match-data */
                local_cnt(c);
                /* fall through */
	    case '*':		/* $*: argv */
	    case '$':		/* $$: pid */
	    case '?':		/* $?: last status */
	    case '!':		/* $!: error string */
	    case '@':		/* $@: error position */
	    case '/':		/* $/: input record separator */
	    case '\\':		/* $\: output record separator */
	    case ';':		/* $;: field separator */
	    case ',':		/* $,: output field separator */
	    case '.':		/* $.: last read line number */
	    case '=':		/* $=: ignorecase */
	    case ':':		/* $:: load path */
	    case '<':		/* $<: reading filename */
	    case '>':		/* $>: default output handle */
	    case '\"':		/* $": already loaded files */
                tokadd('$');
                tokadd(c);
                tokfix();
                yyVal = ID.rb_intern(tok(), ruby);
                return tGVAR;

	    case '-':
                tokadd('$');
                tokadd(c);
                c = nextc();
                tokadd(c);
                tokfix();
                yyVal = ID.rb_intern(tok(), ruby);
                /* xxx shouldn't check if valid option variable */
                return tGVAR;

	    case '&':		/* $&: last match */
	    case '`':		/* $`: string before last match */
	    case '\'':		/* $': string after last match */
	    case '+':		/* $+: string matches last paren. */
                yyVal = NEW_BACK_REF(c);
                return tBACK_REF;

	    case '1': case '2': case '3':
	    case '4': case '5': case '6':
	    case '7': case '8': case '9':
                tokadd('$');
                while (Character.isDigit((char)c)) {
                    tokadd(c);
                    c = nextc();
                }
                if (is_identchar(c))
                    break;
                pushback(c);
                tokfix();
                yyVal = NEW_NTH_REF(Integer.parseInt(tok().substring(1)));
                return tNTH_REF;

	    default:
                if (!is_identchar(c)) {
                    pushback(c);
                    return '$';
                }
	    case '0':
                tokadd('$');
            }
            break;

	case '@':
            c = nextc();
            newtok();
            tokadd('@');
            if (c == '@') {
                tokadd('@');
                c = nextc();
            }
            if (Character.isDigit((char)c)) {
                rb_compile_error("`@%c' is not a valid instance variable name", new Integer(c));
            }
            if (!is_identchar(c)) {
                pushback(c);
                return '@';
            }
            break;

	default:
            if (!is_identchar(c) || Character.isDigit((char)c)) {
                rb_compile_error("Invalid char `\\%03o' in expression", new Integer(c));
                continue retry;
            }

            newtok();
            break;
        }
        break retry;
        }

        while (is_identchar(c)) {
            tokadd(c);
            /*if (ismbchar(c)) {
                int i, len = mbclen(c)-1;

                for (i = 0; i < len; i++) {
                    c = nextc();
                    tokadd(c);
                }
            }*/
            c = nextc();
        }
        if ((c == '!' || c == '?') && is_identchar(tok().charAt(0)) && !peek('=')) {
            tokadd(c);
        }
        else {
            pushback(c);
        }
        tokfix();

        {
            int result = 0;

            switch (tok().charAt(0)) {
	    case '$':
                lex_state = EXPR_END;
                result = tGVAR;
                break;
	    case '@':
                lex_state = EXPR_END;
                if (tok().charAt(1) == '@')
                    result = tCVAR;
                else
                    result = tIVAR;
                break;
	    default:
                if (lex_state != EXPR_DOT) {
                    /* See if it is a reserved word.  */
                    kw = rb_reserved_word(tok(), toklen());
                    if (kw != null) {
                        /*enum lex_state*/int state = lex_state;
                        lex_state = kw.state;
                        if (state == EXPR_FNAME) {
                            yyVal = ID.rb_intern(kw.name, ruby);
                        }
                        if (kw.id0 == kDO) {
                            if (COND_P()) return kDO_COND;
                            if (CMDARG_P() && state != EXPR_CMDARG)
                                return kDO_BLOCK;
                            return kDO;
                        }
                        if (state == EXPR_BEG)
                            return kw.id0;
                        else {
                            if (kw.id0 != kw.id1)
                                lex_state = EXPR_BEG;
                            return kw.id1;
                        }
                    }
                }

                if (toklast() == '!' || toklast() == '?') {
                    result = tFID;
                }
                else {
                    if (lex_state == EXPR_FNAME) {
                        if ((c = nextc()) == '=' && !peek('~') && !peek('>') &&
                            (!peek('=') || lex_p + 1 < lex_pend && lex_curline.charAt(lex_p + 1) == '>')) {
                            result = tIDENTIFIER;
                            tokadd(c);
                        }
                        else {
                            pushback(c);
                        }
                    }
                    if (result == 0 && Character.isUpperCase(tok().charAt(0))) {
                        result = tCONSTANT;
                    }
                    else {
                        result = tIDENTIFIER;
                    }
                }
                if (lex_state == EXPR_BEG ||
                    lex_state == EXPR_DOT ||
                    lex_state == EXPR_ARG ||
                    lex_state == EXPR_CMDARG) {
                    if (cmd_state)
                        lex_state = EXPR_CMDARG;
                    else
                        lex_state = EXPR_ARG;
                }
                else {
                    lex_state = EXPR_END;
                }
            }
            tokfix();
            yyVal = /*last_id =*/ ID.rb_intern(tok(), ruby);  //XXX really overwrite last_id?
            return result;
        }
    }

    private int start_num(int c) {
        boolean is_float, seen_point, seen_e, seen_uc;

        is_float = seen_point = seen_e = seen_uc = false;
        lex_state = EXPR_END;
        newtok();
        if (c == '-' || c == '+') {
            tokadd(c);
            c = nextc();
        }
        if (c == '0') {
            c = nextc();
            if (c == 'x' || c == 'X') {
                /* hexadecimal */
                c = nextc();
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (!ISXDIGIT(c)) break;
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (toklen() == 0) {
                    yyerror("hexadecimal number without hex-digits");
                }
                else if (seen_uc) return decode_num(c, is_float, seen_uc, true);
                yyVal = rb_cstr2inum(tok(), 16);
                return tINTEGER;
            }
            if (c == 'b' || c == 'B') {
                /* binary */
                c = nextc();
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (c != '0'&& c != '1') break;
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (toklen() == 0) {
                    yyerror("numeric literal without digits");
                }
                else if (seen_uc) return decode_num(c, is_float, seen_uc, true);
                yyVal = (VALUE)rb_cstr2inum(tok(), 2);
                return tINTEGER;
            }
            if (c >= '0' && c <= '7' || c == '_') {
                /* octal */
                do {
                    if (c == '_') {
                        seen_uc = true;
                        continue;
                    }
                    if (c < '0' || c > '7') break;
                    seen_uc = false;
                    tokadd(c);
                } while ((c = nextc()) != 0);
                pushback(c);
                tokfix();
                if (seen_uc) return decode_num(c, is_float, seen_uc, true);
                yyVal = (VALUE)rb_cstr2inum(tok(), 8);
                return tINTEGER;
            }
            if (c > '7' && c <= '9') {
                yyerror("Illegal octal digit");
            }
            else if (c == '.') {
                tokadd('0');
            }
            else {
                pushback(c);
                yyVal = (VALUE)INT2FIX(0);
                return tINTEGER;
            }
        }

        for (;;) {
            switch (c) {
            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                seen_uc = false;
                tokadd(c);
                break;

            case '.':
                if (seen_point || seen_e) {
                    return decode_num(c, is_float, seen_uc);
                }
                else {
                    int c0 = nextc();
                    if (!Character.isDigit((char)c0)) {
                        pushback(c0);
                        return decode_num(c, is_float, seen_uc);
                    }
                    c = c0;
                }
                tokadd('.');
                tokadd(c);
                is_float = true;
                seen_point = true;
                seen_uc = false;
                break;

            case 'e':
            case 'E':
                if (seen_e) {
                    return decode_num(c, is_float, seen_uc);
                }
                tokadd(c);
                seen_e = true;
                is_float = true;
                while ((c = nextc()) == '_')
                    seen_uc = true;
                if (c == '-' || c == '+')
                    tokadd(c);
                else
                    continue;
                break;

            case '_':	/* `_' in number just ignored */
                seen_uc = true;
                break;

            default:
                return decode_num(c, is_float, seen_uc);
            }
            c = nextc();
        }
    }

    private int decode_num(int c, boolean is_float, boolean seen_uc) {
        return decode_num(c, is_float, seen_uc, false);
    }

    private int decode_num(int c, boolean is_float, boolean seen_uc, boolean trailing_uc) {
        if (!trailing_uc) {
            pushback(c);
            tokfix();
        }
        if (seen_uc || trailing_uc) {
            //trailing_uc:
            yyerror("trailing `_' in number");
        }
        if (is_float) {
            double d = 0.0;
            try {
                d = Double.parseDouble(tok());
            } catch (NumberFormatException e) {
                rb_warn("Float %s out of range", tok());
            }
            yyVal = (VALUE)rb_float_new(d);
            return tFLOAT;
        }
        yyVal = (VALUE)rb_cstr2inum(tok(), 10);
        return tINTEGER;
    }

    private NODE str_extend(NODE list, int term)
    {
        int c;
        int brace = -1;
        VALUE ss;
        NODE node;
        int nest;

        c = nextc();
        switch (c) {
	case '$':
	case '@':
	case '{':
            break;
	default:
            tokadd('#');
            pushback(c);
            return list;
        }

        ss = rb_str_new(tok(), toklen());
        if (list == null) {
            list = NEW_DSTR(ss);
        }
        else if (toklen() > 0) {
            list_append(list, NEW_STR(ss));
        }
        newtok();

        fetch_id:for(;;) {
        switch (c) {
	case '$':
            tokadd('$');
            c = nextc();
            if (c == -1) return NODE.MINUS_ONE;
            switch (c) {
	    case '1': case '2': case '3':
	    case '4': case '5': case '6':
	    case '7': case '8': case '9':
                while (Character.isDigit((char)c)) {
                    tokadd(c);
                    c = nextc();
                }
                pushback(c);
                break fetch_id;

	    case '&': case '+':
	    case '_': case '~':
	    case '*': case '$': case '?':
	    case '!': case '@': case ',':
	    case '.': case '=': case ':':
	    case '<': case '>': case '\\':
	    //refetch:
	    tokadd(c);
	    break fetch_id;

	    default:
                if (c == term) {
                    list_append(list, NEW_STR(rb_str_new2("#$")));
                    pushback(c);
                    newtok();
                    return list;
                }
                switch (c) {
		case '\"':
		case '/':
		case '\'':
		case '`':
                    //goto refetch;
                    tokadd(c);
                    break fetch_id;
                }
                if (!is_identchar(c)) {
                    yyerror("bad global variable in string");
                    newtok();
                    return list;
                }
            }

            while (is_identchar(c)) {
                tokadd(c);
                /*if (ismbchar(c)) {
                    int i, len = mbclen(c)-1;

                    for (i = 0; i < len; i++) {
                        c = nextc();
                        tokadd(c);
                    }
                }*/
                c = nextc();
            }
            pushback(c);
            break;

	case '@':
            tokadd(c);
            c = nextc();
            if (c == '@') {
                tokadd(c);
                c = nextc();
            }
            while (is_identchar(c)) {
                tokadd(c);
                /*if (ismbchar(c)) {
                    int i, len = mbclen(c)-1;

                    for (i = 0; i < len; i++) {
                        c = nextc();
                        tokadd(c);
                    }
                }*/
                c = nextc();
            }
            pushback(c);
            break;

	case '{':
            if (c == '{') brace = '}';
            nest = 0;
            do {
	    loop_again:for(;;) {
                c = nextc();
                switch (c) {
		case -1:
                    if (nest > 0) {
                        yyerror("bad substitution in string");
                        newtok();
                        return list;
                    }
                    return NODE.MINUS_ONE;
		case '}':
                    if (c == brace) {
                        if (nest == 0) break;
                        nest--;
                    }
                    tokadd(c);
                    continue loop_again;
		case '\\':
                    c = nextc();
                    if (c == -1) return NODE.MINUS_ONE;
                    if (c == term) {
                        tokadd(c);
                    }
                    else {
                        tokadd('\\');
                        tokadd(c);
                    }
                    break;
		case '{':
                    if (brace != -1) nest++;
		case '\"':
		case '/':
		case '`':
                    if (c == term) {
                        pushback(c);
                        list_append(list, NEW_STR(rb_str_new2("#")));
                        rb_warning("bad substitution in string");
                        tokfix();
                        list_append(list, NEW_STR(rb_str_new(tok(), toklen())));
                        newtok();
                        return list;
                    }
		default:
                    tokadd(c);
                    break;
                }
                break loop_again;
	        }
            } while (c != brace);
        }
        break;
        }

        //fetch_id:
        tokfix();
        node = NEW_EVSTR(tok(),toklen());
        list_append(list, node);
        newtok();

        return list;
    } // yylex


    // Helper functions (could be inlined)....................

    /** Returns true if "c" is a white space character. */
    private boolean ISSPACE(int c) {
        return Character.isWhitespace((char)c);
    }

    /** Returns true if "c" is a hex-digit. */
    boolean ISXDIGIT(int c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
    }

    /** Returns true if "c" is a valid identifier character (letter, digit
     *  or underscore) */
    boolean is_identchar(int ch) {
	return Character.isLetterOrDigit((char)ch) || ch == '_';
    }

    /**
     * Returns the value of a hex number with max "len" characters.
     * Also returns the number of characters read.  Please note the
     * "x"-hack.
     */
    private long scan_hex(String s, int start, int len, int[] retlen) {
        String hexdigit = "0123456789abcdef0123456789ABCDEFx";
        long retval = 0;
        int tmp, st = start;

        while (len-- != 0 && st < s.length() && (tmp = hexdigit.indexOf(s.charAt(st))) != -1) {
            retval <<= 4;
            retval |= tmp & 15;
            st++;
        }
        retlen[0] = st - start;
        return retval;
    }

    // yyVal is local to parse, so we need this ivar for the scanner
    private Object yyVal;

    //XXX globals
    boolean ruby_in_eval;
    VALUE ruby_verbose;
    VALUE Qnil;

    // SCOPE ruby_scope; replaced by ruby.ruby_scope

    //------------------------------------------------------------------------
    // global helper functions, locally defined for now...
    //------------------------------------------------------------------------

    private Error missing() { return new Error("missing function"); }

    /** Returns a kwtable entry if "w" is a keyword or null otherwise */
    kwtable rb_reserved_word(String w, int len) {
        return kwtable.rb_reserved_word(w, len);
    }

    /** Creates a new regular expression from "s" */
    VALUE rb_reg_new(String s, int len, int options) {
        //XXX well...
        return new RubyRegex(ruby, (RubyString)rb_str_new(s, len), options);
    }

    /** Creates a new integer object (Fixnum or Bignum) from "s" */
    VALUE rb_cstr2inum(String s, int radix) {
        //XXX no support for _ or leading and trailing spaces
        return new RubyFixnum(ruby, Integer.parseInt(s, radix));
    }

    /** (Added to reduce the need for casts) */
    VALUE rb_funcall(VALUE v1, char c, int j, VALUE v) {
        return rb_funcall(v1, ruby.intern(String.valueOf(c)), j, v);
    }
    VALUE rb_funcall(VALUE v1, ID i, int j, VALUE v) {
    	return ((RubyObject)v1).funcall((RubyId)i, (RubyObject)v);
    }

    // warnings and errors
    // -------------------
    void rb_warn(String s) { rb_warn(s, null); }
    void rb_warn(String s, Object arg) { System.err.println(s + arg); }
    void rb_warning(String s) { rb_warning(s, null); }
    void rb_warning(String s, Object arg) { System.err.println(s + arg); }
    void rb_compile_error(String s) { rb_compile_error(s, null); }
    void rb_compile_error(String s, Object arg) { System.err.println(s + arg); }
    void rb_bug(String s) { System.err.println(s); }

    // String stuff
    // ------------

    /** Adds second value (a string or char) to first value */
    VALUE rb_str_concat(VALUE v, VALUE w) {
        //XXX need to keep identity and tainting
        RubyString vs = (RubyString)v;
        if (FIXNUM_P(w))
            vs.setString(vs.getString() + (char)FIX2INT(w));
        else
            vs.setString(vs.getString() + ((RubyString)w).getString());
        return vs;
    }

    /** Creates a new string object. */
    VALUE rb_str_new(String s, int len) {
        return new RubyString(ruby, s.substring(0, len));
    }

    /** Creates a new string object. */
    VALUE rb_str_new2(String s) {
        return rb_str_new(s, s.length());
    }

    // Number stuff
    // ------------
    /** Creates a new float object */
    VALUE rb_float_new(double d) { return RubyFloat.m_newFloat(ruby, d); }
    VALUE INT2FIX(int i) { return new RubyFixnum(ruby, i); }
    VALUE INT2FIX(long i) { return new RubyFixnum(ruby, i); }
    boolean FIXNUM_P(VALUE v) {return v instanceof RubyFixnum;}
    long FIX2LONG(VALUE v) {return FIX2INT(v);}
    int FIX2INT(VALUE v) { return (int)((RubyFixnum)v).getValue(); }

    // Symbol stuff
    // ------------
    static final int SYMBOL_FLAG = 14;
    boolean SYMBOL_P(ID id) {
        return (id.intValue() & 255) == SYMBOL_FLAG;
    }
    ID ID2SYM(ID id) {
        return new RubyId(ruby, id.intValue() << 8 | SYMBOL_FLAG);
    }
    ID SYM2ID(ID id) {
        return new RubyId(ruby, id.intValue() >> 8);
    }

    int TYPE(VALUE v) {
        if (v instanceof RubyFixnum) return T_FIXNUM;
        if (v instanceof RubyRegex) return T_REGEXP;
	if (v instanceof RubyFloat) return T_FLOAT;
        throw missing();
    }
    static final int T_FIXNUM = 0;
    static final int T_FLOAT = 1;
    static final int T_BIGNUM = 2;
    static final int T_NODE = 3;
    static final int T_REGEXP = 4;
    static final int T_HASH = 5;

    /** Returns true if "value" isn't the nil object */
    boolean RTEST(VALUE value) { return value != Qnil; }

    /** Returns true if "value" is the nil object */
    boolean NIL_P(VALUE value) { return value == Qnil; }

    // Environment stuff
    // -----------------

    boolean rb_dvar_defined(ID id) {
        RVarmap vars = ruby_dyna_vars;

        while (vars != null) {
            if (vars.id == null) break;
            if (vars.id.equals(id)) return true;
            vars = vars.next;
        }
        return false;
    }

    boolean rb_dvar_curr(ID id) {
        RVarmap vars = ruby_dyna_vars;

        while (vars != null) {
            if (vars.id == null) break;
            if (vars.id.equals(id)) return true;
            vars = vars.next;
        }
        return false;
    }

    void rb_dvar_push(ID id, VALUE value) {
        ruby_dyna_vars = new_dvar(id, value, ruby_dyna_vars);
    }

    private RVarmap new_dvar(ID id, VALUE value, RVarmap prev) {
        RVarmap map = new RVarmap();
        map.id = id;
        map.val = value;
        map.next = prev;
        return map;
    }

    // array stuff
    // -----------

    void rb_ary_push(VALUE array, NODE n) {
        rom.rb_ary_push(array, n);
    }
    void rb_ary_push(VALUE array, VALUE value) {
        rom.rb_ary_push(array, value);
    }

    /** Creates an array object with default size */
    VALUE rb_ary_new() {
        return rb_ary_new2(16);
    }

    /** Creates an array object with the given size */
    VALUE rb_ary_new2(int size) {
        return rom.rb_ary_new2(size);
    }

    // Hash stuff
    // ----------

    VALUE rb_hash_aref(VALUE hash, VALUE key) {throw missing();}
    void rb_hash_aset(VALUE hash, VALUE key, VALUE value) {throw missing();}

    int rb_safe_level() { return 0; }
    boolean rb_const_defined(VALUE clazz, ID name) {
        return rom.rb_const_defined(clazz, name);
    }
    VALUE rb_const_get(VALUE clazz, ID name) {
    	return rom.rb_const_get(clazz, name);
    }

    global_entry rb_global_entry(ID v) {
        global_entry entry = (global_entry)rb_global_tbl.get(v);
        if (entry == null) {
            entry = new global_entry();
            entry.id = v;
            rb_global_tbl.put(v, entry);
        }
        return entry;
    }

    // Test methods
    // ------------
    private static String readFile(String name) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(name));
            StringBuffer sb = new StringBuffer(1024);
            String line;
            while ((line = in.readLine()) != null)
                sb.append(line).append('\n');
            in.close();
            return sb.toString();
        } catch (IOException e) {
	    System.out.println(new File(name).getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }
    public static void scanner_test(String f) {
    	Ruby ruby = new Ruby();
        parse p = new parse(ruby);
        p.init_for_scanner(f);

        int t;
        while ((t = p.yylex()) != 0) {
            if (t < 256)
                if (t == 10)
                    System.out.println("'\\n'");
                else
                    System.out.println("'" + (char)t + "'");
            else {
                System.out.print(token_name(t));
                System.out.print(" - ");
                if (p.yyVal instanceof ID)
                    System.out.println(ID.rb_id2name(ruby, (ID)p.yyVal));
                else
                    System.out.println(p.yyVal);
            }
        }
    }
    private static String token_name(int t) {
        java.lang.reflect.Field[] f = token.class.getDeclaredFields();
        try {
            for (int i = 0; i < f.length; i++)
                if (f[i].getInt(null) == t)
                    return f[i].getName();
        } catch (IllegalAccessException e) {}
        return String.valueOf(t);
    }
    public static void parser_test(String f) {
    	Ruby ruby = new Ruby();
        parse p = new parse(ruby);
        print_nodes(p.rb_compile_string("stdin", new RubyString(ruby, f), 0));
    }

    public static void interpreter_test(String f) {
    	Ruby ruby = new Ruby();
	parse p = new parse(ruby);
	ruby.getInterpreter().eval(ruby.getObjectClass(), p.rb_compile_string("stdin", new RubyString(ruby, f), 0));
    }

    private static void print_nodes(NODE n) {
        if (n != null)
            ((NODE)n).print(0, n, new IdentitySet());
        else
            System.out.println("<no parse nodes>");
    }

    public static void main(String[] args) {
        // String f = "a, b = 'Hallo', :Welt";
	String f ="puts \"Hello World\"";
        if (args.length > 1) f = readFile(args[1]);
        if (args.length > 0 && args[0].equals("-p"))
            parser_test(f);
        else if (args.length > 0 && args[0].equals("-s"))
            scanner_test(f);
	else
	    interpreter_test(f);
    }
}

//XXX strange classes needed to compile the stuff
class RBasic {}

class global_entry implements VALUE {
    ID id;
    /*void *data;
    VALUE (*getter)();
    void  (*setter)();
    void  (*marker)();
    int block_trace;
    struct trace_var *trace;*/
};


					// line 10414 "-"
