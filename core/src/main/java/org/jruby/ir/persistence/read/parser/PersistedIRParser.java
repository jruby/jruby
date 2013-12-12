// created by jay 1.0.2 (c) 2002-2004 ats@cs.rit.edu
// skeleton Java 1.0 (c) 2002 ats@cs.rit.edu

					// line 2 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013 The JRuby Team <team@jruby.org>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ir.persistence.read.parser;

import java.util.ArrayList;

import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.read.parser.dummy.InstrWithParams;
import org.jruby.ir.persistence.read.lexer.PersistedIRScanner;
import org.jruby.parser.ParserSyntaxException;

public class PersistedIRParser {
    private PersistedIRParserLogic logic;

    public PersistedIRParser(IRParsingContext context) {
        logic = new PersistedIRParserLogic(context);
    }

					// line 51 "-"
  // %token constants
  public static final int BOOLEAN = 257;
  public static final int ID = 258;
  public static final int STRING = 259;
  public static final int FIXNUM = 260;
  public static final int FLOAT = 261;
  public static final int EOLN = 262;
  public static final int EQ = 263;
  public static final int COMMA = 264;
  public static final int LBRACE = 265;
  public static final int RBRACE = 266;
  public static final int LPAREN = 267;
  public static final int RPAREN = 268;
  public static final int GT = 269;
  public static final int LT = 270;
  public static final int LBRACK = 271;
  public static final int RBRACK = 272;
  public static final int DEAD_RESULT_INSTR_MARKER = 273;
  public static final int DEAD_INSTR_MARKER = 274;
  public static final int NULL = 275;
  public static final int EOF = 276;
  public static final int yyErrorCode = 256;

  /** number of final state.
    */
  protected static final int yyFinal = 2;

  /** parser tables.
      Order is mandated by <i>jay</i>.
    */
  protected static final short[] yyLhs = {
//yyLhs 34
    -1,     0,    14,    14,     9,    15,    15,    11,    10,     3,
     3,     5,     6,     6,     7,     7,    16,    12,    12,    13,
     1,     1,     8,     8,     8,     8,     8,     8,     8,     2,
     4,     4,    17,    18,
    }, yyLen = {
//yyLen 34
     2,     3,     2,     3,     4,     2,     3,     3,     1,     2,
     3,     2,     1,     1,     1,     1,     2,     3,     3,     4,
     1,     3,     1,     1,     1,     1,     1,     1,     1,     3,
     1,     1,     1,     4,
    }, yyDefRed = {
//yyDefRed 59
     0,     0,     0,     0,     0,     0,     2,     0,     0,    25,
     0,    27,    23,    24,     0,    28,     0,    26,    22,    20,
    30,    31,     8,     0,     0,     0,     3,     0,     0,     0,
     4,     0,     5,     0,     0,    29,    21,     0,     0,     0,
     0,     0,    12,     0,    15,    13,     6,    33,     0,     0,
     0,     9,    11,    16,     0,    10,     0,    18,    19,
    }, yyDgoto = {
//yyDgoto 19
     2,    16,    17,    38,    18,    40,    41,    42,    19,     3,
    23,    24,    43,    44,     4,    25,    45,    20,    21,
    }, yySindex = {
//yySindex 59
  -249,  -262,     0,  -235,  -251,  -256,     0,  -231,  -230,     0,
  -229,     0,     0,     0,  -256,     0,  -238,     0,     0,     0,
     0,     0,     0,  -225,  -224,  -231,     0,  -256,  -250,  -256,
     0,  -219,     0,  -222,  -254,     0,     0,  -232,  -219,  -221,
  -218,  -233,     0,  -228,     0,     0,     0,     0,  -256,  -216,
  -215,     0,     0,     0,  -239,     0,  -220,     0,     0,
    }, yyRindex = {
//yyRindex 59
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
  -248,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,    48,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,     0,  -257,  -213,     0,
     0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
     0,     0,     0,     0,     0,     0,  -223,     0,     0,
    }, yyGindex = {
//yyGindex 19
     0,   -14,     0,     0,    -8,    13,     0,     0,    23,    49,
     0,    29,     0,     5,     0,     0,     0,     0,     0,
    };
  protected static final short[] yyTable = {
//yyTable 56
    28,     9,    10,    11,    12,    13,    32,     1,     5,     1,
    29,     7,    47,    34,    29,    14,    32,    14,    32,    15,
    32,    32,    35,    39,    32,    29,    29,     6,    22,    58,
    39,    30,    26,    27,    54,    48,    27,    31,    32,    37,
    46,    52,    50,    56,    51,    53,    55,    48,     1,     7,
    17,    49,    36,     8,    33,    57,
    };
  protected static final short[] yyCheck = {
//yyCheck 56
    14,   257,   258,   259,   260,   261,   263,   258,   270,   258,
   264,   262,   266,    27,   264,   271,   264,   274,   266,   275,
   268,   269,   272,    31,   272,   264,   264,   262,   259,   268,
    38,   269,   262,   265,    48,   267,   265,   262,   262,   258,
   262,   274,   263,   258,   262,   273,   262,   267,     0,   262,
   273,    38,    29,     4,    25,    50,
    };

  /** maps symbol value to printable name.
      @see #yyExpecting
    */
  protected static final String[] yyNames = {
    "end-of-file",null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,null,null,null,null,null,null,null,
    null,null,null,null,null,null,null,"BOOLEAN","ID","STRING","FIXNUM",
    "FLOAT","EOLN","EQ","COMMA","LBRACE","RBRACE","LPAREN","RPAREN","GT",
    "LT","LBRACK","RBRACK","DEAD_RESULT_INSTR_MARKER","DEAD_INSTR_MARKER",
    "NULL","EOF",
    };


  /** simplified error message.
      @see #yyerror(java.lang.String, java.lang.String[])
    */
  public void yyerror (String message) throws ParserSyntaxException {
    throw new ParserSyntaxException(message);
  }

  /** (syntax) error message.
      Can be overwritten to control message format.
      @param message text to be displayed.
      @param expected list of acceptable tokens, if available.
    */
  public void yyerror (String message, String[] expected, String found) throws ParserSyntaxException {
    String text = message + ", unexpected " + found + "\n";
    throw new ParserSyntaxException(text);
  }

  /** computes list of expected tokens on error by tracing the tables.
      @param state for which to compute the list.
      @return list of token names.
    */
  protected String[] yyExpecting (int state) {
    int token, n, len = 0;
    boolean[] ok = new boolean[yyNames.length];

    if ((n = yySindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }
    if ((n = yyRindex[state]) != 0)
      for (token = n < 0 ? -n : 0;
           token < yyNames.length && n+token < yyTable.length; ++ token)
        if (yyCheck[n+token] == token && !ok[token] && yyNames[token] != null) {
          ++ len;
          ok[token] = true;
        }

    String result[] = new String[len];
    for (n = token = 0; n < len;  ++ token)
      if (ok[token]) result[n++] = yyNames[token];
    return result;
  }

  /** the generated parser, with debugging messages.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @param yydebug debug message writer implementing <tt>yyDebug</tt>, or <tt>null</tt>.
      @return result of the last reduction, if any.
      @throws ParserSyntaxException on irrecoverable parse error.
    */
  public Object yyparse (PersistedIRScanner yyLex, Object ayydebug)
				throws java.io.IOException, ParserSyntaxException {
    return yyparse(yyLex);
  }

  /** initial size and increment of the state/value stack [default 256].
      This is not final so that it can be overwritten outside of invocations
      of {@link #yyparse}.
    */
  protected int yyMax;

  /** executed at the beginning of a reduce action.
      Used as <tt>$$ = yyDefault($1)</tt>, prior to the user-specified action, if any.
      Can be overwritten to provide deep copy, etc.
      @param first value for <tt>$1</tt>, or <tt>null</tt>.
      @return first.
    */
  protected Object yyDefault (Object first) {
    return first;
  }

  /** the generated parser.
      Maintains a dynamic state and value stack.
      @param yyLex scanner.
      @return result of the last reduction, if any.
      @throws ParserSyntaxException on irrecoverable parse error.
    */
  public Object yyparse (PersistedIRScanner yyLex) throws java.io.IOException, ParserSyntaxException {
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

      yyDiscarded: for (;;) {	// discarding a token does not change stack
        int yyN;
        if ((yyN = yyDefRed[yyState]) == 0) {	// else [default] reduce (yyN)
          if (yyToken < 0) {
            int a1 = yyLex.yylex();
            yyToken = a1 == -1 ? 0 : a1;
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
              yyerror("syntax error", yyExpecting(yyState), yyNames[yyToken]);
  
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
              throw new ParserSyntaxException("irrecoverable syntax error");
  
            case 3:
              if (yyToken == 0) {
                throw new ParserSyntaxException("irrecoverable syntax error at end-of-file");
              }
              yyToken = -1;
              continue yyDiscarded;		// leave stack alone
            }
        }
        int yyV = yyTop + 1-yyLen[yyN];
        yyVal = yyDefault(yyV > yyTop ? null : yyVals[yyV]);
        switch (yyN) {
// ACTIONS_BEGIN
case 1:
					// line 67 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  { 
    yyVal = logic.getToplevelScope();
}
  break;
case 4:
					// line 76 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.createScope(((String)yyVals[-3+yyTop]), ((ArrayList)yyVals[-1+yyTop])); 
}
  break;
case 7:
					// line 83 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.addToScope(((IRScope)yyVals[-2+yyTop]), ((ArrayList)yyVals[0+yyTop])); 
}
  break;
case 8:
					// line 87 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.enterScope(((String)yyVals[0+yyTop]));
}
  break;
case 9:
					// line 92 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                     yyVal = logic.addFirstInstruction(((Instr)yyVals[-1+yyTop]));
                 }
  break;
case 10:
					// line 95 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                     yyVal = logic.addFollowingInstructions(((ArrayList)yyVals[-2+yyTop]), ((Instr)yyVals[-1+yyTop]), null);
                 }
  break;
case 11:
					// line 99 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.markAsDeadIfNeeded(((Instr)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop])); 
}
  break;
case 14:
					// line 105 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                yyVal = logic.createInstrWithoutParams(((String)yyVals[0+yyTop]));
            }
  break;
case 15:
					// line 108 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                yyVal = logic.createInstrWithParams(((InstrWithParams)yyVals[0+yyTop]));
            }
  break;
case 16:
					// line 112 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.markHasUnusedResultIfNeeded(((Instr)yyVals[-1+yyTop]), ((String)yyVals[0+yyTop]));
}
  break;
case 17:
					// line 116 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                       /*                 $$ = logic.createReturnInstrWithNoParams($1, $3);*/
             }
  break;
case 18:
					// line 119 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                       /*                 $$ = logic.createReturnInstrWithParams($1, $3);*/
             }
  break;
case 19:
					// line 123 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
                       /*   $$ = logic.createInstrWithParams($1, $3); */
}
  break;
case 28:
					// line 131 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  { yyVal = logic.createNull(); }
  break;
case 29:
					// line 133 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.createList(((ArrayList)yyVals[-1+yyTop]));
}
  break;
case 32:
					// line 140 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.createOperandWithoutParameters(((String)yyVals[0+yyTop])); 
}
  break;
case 33:
					// line 145 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
  {
    yyVal = logic.createOperandWithParameters(((String)yyVals[-3+yyTop]), ((ArrayList)yyVals[-1+yyTop]));
}
  break;
					// line 496 "-"
// ACTIONS_END
        }
        yyTop -= yyLen[yyN];
        yyState = yyStates[yyTop];
        int yyM = yyLhs[yyN];
        if (yyState == 0 && yyM == 0) {
          yyState = yyFinal;
          if (yyToken < 0) {
            int a1 = yyLex.yylex();
            yyToken = a1 == -1 ? 0 : a1;
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

// ACTION_BODIES
					// line 150 "core/src/main/java/org/jruby/ir/persistence/read/parser/PersistedIRParser.y"
}
					// line 531 "-"
