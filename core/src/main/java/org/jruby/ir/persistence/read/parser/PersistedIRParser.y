%{
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

%}

%token<String> BOOLEAN ID STRING FIXNUM FLOAT EOLN EQ
%token<String> COMMA LBRACE RBRACE LPAREN RPAREN GT LT LBRACK RBRACK
%token<String> DEAD_RESULT_INSTR_MARKER DEAD_INSTR_MARKER NULL
%token<String> EOF

%type <String> ID
%type <ArrayList> parameter_list list instruction_list
%type <Operand> operand
%type <Instr> abstract_instruction instruction simple_instr
%type <Object> param

%type <IRScope> scope_info scope_descriptor
%type <IRScope> scopes
%type <IRScope> scope_instructions
%type <Instr> result_instr
%type <InstrWithParams> instr_with_params

%%
scopes : scopes_info EOLN scopes_instructions { 
    $$ = logic.getToplevelScope();
}
    
scopes_info : scope_info EOLN
            | scopes_info scope_info EOLN

    
/* Info that is needed to recreate a scope itself, do not contain instructions */    
scope_info : ID LT parameter_list GT {
    $$ = logic.createScope($1, $3); 
}
    
scopes_instructions : scope_instructions EOLN
                   |  scopes_instructions scope_instructions EOLN
    
scope_instructions : scope_descriptor EOLN instruction_list {
    $$ = logic.addToScope($1, $3); 
}
    
scope_descriptor : STRING {
    $$ = logic.enterScope($1);
}

/* Instructions */
instruction_list : abstract_instruction EOLN {
                     $$ = logic.addFirstInstruction($1);
                 }
                 | instruction_list abstract_instruction EOLN {
                     $$ = logic.addFollowingInstructions($1, $2, null);
                 }
    
abstract_instruction : instruction DEAD_INSTR_MARKER {
    $$ = logic.markAsDeadIfNeeded($1, $2); 
}
    
instruction: simple_instr | abstract_result_instr

simple_instr: ID {
                $$ = logic.createInstrWithoutParams($1);
            }
            | instr_with_params {
                $$ = logic.createInstrWithParams($1);
            }
     
abstract_result_instr : result_instr DEAD_RESULT_INSTR_MARKER {
    $$ = logic.markHasUnusedResultIfNeeded($1, $2);
}

result_instr : operand EQ ID {
                       //                 $$ = logic.createReturnInstrWithNoParams($1, $3);
             }
             | operand EQ instr_with_params {
                       //                 $$ = logic.createReturnInstrWithParams($1, $3);
             }
    
instr_with_params : ID LPAREN parameter_list RPAREN {
                       //   $$ = logic.createInstrWithParams($1, $3); 
}

/* Parameters of instruction */ 
parameter_list : param | parameter_list COMMA param
    
param : operand | FIXNUM | FLOAT | BOOLEAN | list | STRING 
      | NULL { $$ = logic.createNull(); }
    
list : LBRACK parameter_list RBRACK {
    $$ = logic.createList($2);
}

/* Operands */
operand : operand_without_parameters | operand_with_parameters
    
operand_without_parameters : ID {
    $$ = logic.createOperandWithoutParameters($1); 
}

    
operand_with_parameters : ID LBRACE parameter_list RBRACE {
    $$ = logic.createOperandWithParameters($1, $3);
}

%%
}
