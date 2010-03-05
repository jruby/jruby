/*
 * We started with a full Java grammar from JavaParser (cup) implementation 
 * (since it was an LALR grammar already) and then stripped out all bits of 
 * the grammar unrelated to Java method signature parsing.  We also changed the 
 * grammar to accept signatures which do not specify the parameter names. So,
 * 'void foo(int)' is as 'void foo(int name)'.  This grammar also only works
 * with Jay which is another LALR grammar compiler compiler tool.
 *
 * The output this tool generates is subject to our tri-license (GPL,LGPL,
 * and CPL), but this source file itself is released only under the terms
 * of the GPL.
 *
 * This program is released under the terms of the GPL; see the file
 * COPYING for more details.  There is NO WARRANTY on this code.
 */

%{ 
package org.jruby.parser;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jruby.ast.java_signature.ArrayTypeNode;
import org.jruby.ast.java_signature.MethodSignatureNode;
import org.jruby.ast.java_signature.Modifier;
import org.jruby.ast.java_signature.ParameterNode;
import org.jruby.ast.java_signature.PrimitiveTypeNode;
import org.jruby.ast.java_signature.ReferenceTypeNode;
import org.jruby.ast.java_signature.TypeNode;
import org.jruby.lexer.JavaSignatureLexer;

public class JavaSignatureParser {
    private static JavaSignatureParser parser = new JavaSignatureParser();

    public static MethodSignatureNode parse(InputStream in) throws IOException, ParserSyntaxException {
        return (MethodSignatureNode) parser.yyparse(JavaSignatureLexer.create(in));
    }
%}

// Primitive types
%token <String> BOOLEAN  // 'boolean'
%token <String> BYTE     // 'byte'
%token <String> SHORT    // 'short'
%token <String> INT      // 'int'
%token <String> LONG     // 'long'
%token <String> CHAR     // 'char'
%token <String> FLOAT    // 'float'
%token <String> DOUBLE   // 'double'
%token <String> VOID     // 'void'
// modifiers
%token <String> PUBLIC       // 'public'
%token <String> PROTECTED    // 'protected'
%token <String> PRIVATE      // 'private'
%token <String> STATIC       // 'static'
%token <String> ABSTRACT     // 'abstract'
%token <String> FINAL        // 'final'
%token <String> NATIVE       // 'native'
%token <String> SYNCHRONIZED // 'synchronized'
%token <String> TRANSIENT    // 'transient'
%token <String> VOLATILE     // 'volatile'
%token <String> STRICTFP     // 'strictfp'
// identifier (e.g. setFoo)
%token <String> IDENTIFIER
// syntax markers
%token <String> AND    // '&'
%token <String> DOT    // '.'
%token <String> COMMA  // ','
%token <String> ELLIPSIS // '...' or \u2026
%token <String> LPAREN // '('
%token <String> RPAREN // ')'
%token <String> LBRACK // '['
%token <String> RBRACK // ']'
%token <String> QUESTION // '?'
%token <String> LT     // '<'
%token <String> GT     // '>'
%token <String> THROWS // 'throws'
%token <String> EXTENDS // 'extends'
%token <String> SUPER // 'super'
%token <String> SUPER // 'super'
%token <String> RSHIFT // '>>'
%token <String> URSHIFT // '>>>'

%type <MethodSignatureNode> method_declarator
%type <List> formal_parameter_list_opt, formal_parameter_list // <ParameterNode>
%type <List> modifiers_opt, modifiers, modifiers_none, throws, class_type_list
%type <ParameterNode> formal_parameter
%type <TypeNode> primitive_type, type, reference_type, array_type
%type <ReferenceTypeNode> class_or_interface, class_or_interface_type, interface_type, class_type
%type <String> name, type_variable, variable_declarator_id
%type <Modifier> modifier
%type <Object> dims
%type <Object> wildcard, wildcard_1, wildcard_2, wildcard_3
%type <Object> reference_type_1, reference_type_2, reference_type_3
%type <Object> type_argument_list, type_argument_list_1, type_argument_list_2, type_argument_list_3
%type <Object> type_argument, type_argument_1, type_argument_2, type_argument_3
%type <Object> type_parameter_list_1, type_parameter_1, type_bound_1, additional_bound_list_1, additional_bound, additional_bound_list

%type <Object> type_parameter_list, type_parameter, type_bound_opt, type_bound
%type <Object> none
%type <Object> method_header

%%

program : method_header

type : primitive_type | reference_type

// PrimitiveTypeNode
primitive_type : BYTE {
     $$ = PrimitiveTypeNode.BOOLEAN;
 }
 | SHORT {
     $$ = PrimitiveTypeNode.SHORT;
 }
 | INT {
     $$ = PrimitiveTypeNode.INT;
 }
 | LONG {
     $$ = PrimitiveTypeNode.LONG;
 }
 | CHAR {
     $$ = PrimitiveTypeNode.CHAR;
 }
 | BOOLEAN {
     $$ = PrimitiveTypeNode.BOOLEAN;
 } 
 | FLOAT {
     $$ = PrimitiveTypeNode.FLOAT;
 }
 | DOUBLE {
     $$ = PrimitiveTypeNode.DOUBLE;
 }

// TypeNode
reference_type : class_or_interface_type {
     $$ = $1;
 }
 | array_type

// String
type_variable : IDENTIFIER { 
     $$ = $1; 
 }

// ReferenceTypeNode
class_or_interface : name {
     $$ = new ReferenceTypeNode($1);
 }
 | class_or_interface LT type_argument_list_1 DOT name {
     $$ = $1; // FIXME: Add generics to ref type
 }

// ReferenceTypeNode
class_or_interface_type : class_or_interface
 | class_or_interface LT type_argument_list_1 {
     $$ = $1; // FIXME: Add generics to ref type
 }

// ReferenceTypeNode
class_type : class_or_interface_type

// ReferenceTypeNode
interface_type : class_or_interface_type

// TypeNode
array_type : primitive_type dims {
     $$ = new ArrayTypeNode($1);
 }
 | name dims {
     $$ = new ArrayTypeNode(new ReferenceTypeNode($1));
 }
 | class_or_interface LT type_argument_list_1 DOT name dims {
     $$ = new ArrayTypeNode($1); // FIXME: Add generics to ref type
 }
 | class_or_interface LT type_argument_list_1 dims {
     $$ = new ArrayTypeNode($1); // FIXME: Add generics to ref type
 }

wildcard : QUESTION { $$ = $1; }// FIXME:
         | QUESTION EXTENDS reference_type { $$ = $1; } // FIXME:
         | QUESTION SUPER reference_type { $$ = $1; } // FIXME:

wildcard_1 : QUESTION GT { $$ = $1; }// FIXME:
           | QUESTION EXTENDS reference_type_1 { $$ = $1; }// FIXME:
           | QUESTION SUPER reference_type_1 { $$ = $1; }// FIXME:

wildcard_2 : QUESTION RSHIFT { $$ = $1; }// FIXME:
           | QUESTION EXTENDS reference_type_2 { $$ = $1; }// FIXME:
           | QUESTION SUPER reference_type_2 { $$ = $1; }// FIXME:

wildcard_3 : QUESTION URSHIFT { $$ = $1; }// FIXME:
           | QUESTION EXTENDS reference_type_3 { $$ = $1; }// FIXME:
           | QUESTION SUPER reference_type_3 { $$ = $1; }// FIXME:

reference_type_1 : reference_type GT { $$ = $1; }// FIXME:
                 | class_or_interface LT type_argument_list_2 { $$ = $1; }// FIXME:

reference_type_2 : reference_type RSHIFT { $$ = $1; }// FIXME:
                 | class_or_interface LT type_argument_list_3 { $$ = $1; }// FIXME:

reference_type_3 : reference_type URSHIFT { $$ = $1; }// FIXME:

type_argument_list : type_argument
                   | type_argument_list COMMA type_argument

type_argument_list_1 : type_argument_1
                     | type_argument_list COMMA type_argument_1

type_argument_list_2 : type_argument_2
                     | type_argument_list COMMA type_argument_2

type_argument_list_3 : type_argument_3
                     | type_argument_list COMMA type_argument_3

type_argument : reference_type | wildcard

type_argument_1 : reference_type_1 | wildcard_1

type_argument_2 : reference_type_2 | wildcard_2

type_argument_3 : reference_type_3 | wildcard_3

// List<Modifier>
modifiers_opt : modifiers | modifiers_none

// List<Modifier>
modifiers : modifier {
    $$ = new ArrayList<Modifier>();
    $<List>$.add($1);
 }
 | modifiers modifier {
    $1.add($2);
 }

// List<Modifier> -- This is just so we don't deal with null's.
modifiers_none : { $$ = new ArrayList<Modifier>(); }

// Modifier
modifier : PUBLIC { $$ = Modifier.PUBLIC; }
 | PROTECTED { $$ = Modifier.PROTECTED; }
 | PRIVATE { $$ = Modifier.PRIVATE; }
 | STATIC { $$ = Modifier.STATIC; }
 | ABSTRACT { $$ = Modifier.ABSTRACT; } 
 | FINAL { $$ = Modifier.FINAL; }
 | NATIVE { $$ = Modifier.NATIVE; }
 | SYNCHRONIZED { $$ = Modifier.SYNCHRONIZED; }
 | TRANSIENT { $$ = Modifier.TRANSIENT; }
 | VOLATILE { $$ = Modifier.VOLATILE; }
 | STRICTFP { $$ = Modifier.STRICTFP; }

// String
name : IDENTIFIER { $$ = $1; }                  // Foo (or foo)
 | name DOT IDENTIFIER { $$ = $1 + "." + $3; }  // foo.Foo 

// Object -- we do not use this for any info
dims : LBRACK RBRACK { $$ = null; } | dims LBRACK RBRACK { $$ = null; }

// List<TypeNode>
throws : THROWS class_type_list { $$ = $2; } 
 | /* none */ { $$ = new ArrayList<TypeNode>(); }

// List<TypeNode>
class_type_list : class_type {
    $$ = new ArrayList<TypeNode>();
    $<List>$.add($1);
 }
 | class_type_list COMMA class_type {
    $<List>1.add($3);
 }

// MethodSignatureNode
method_declarator : IDENTIFIER LPAREN formal_parameter_list_opt RPAREN {
                      $$ = new MethodSignatureNode($1, $3);
                  }

// List<ParameterNode>
formal_parameter_list_opt : formal_parameter_list 
  | /* none */ { $$ = new ArrayList<ParameterNode>(); }

// List<ParameterNode>
formal_parameter_list : formal_parameter {
                          List<ParameterNode> list = new ArrayList<ParameterNode>();
                          list.add($1);
                          $$ = list;
                      }
                      | formal_parameter_list COMMA formal_parameter {
                          $1.add($3);
                      }

// ParameterNode
formal_parameter : type variable_declarator_id {
                     $$ = new ParameterNode($1, $2);
                 }
                 | type {
                     $$ = new ParameterNode($1, null);
                 }
                 | FINAL type variable_declarator_id {
                     $$ = new ParameterNode($2, $3, true);
                 }
                 | FINAL type {
                     $$ = new ParameterNode($2, null, true);
                 }
                 | type ELLIPSIS IDENTIFIER {
                     $$ = new ParameterNode($1, $3, false, true);
                 }
                 | type ELLIPSIS {
                     $$ = new ParameterNode($1, null, false, true);
                 }
                 | FINAL type ELLIPSIS IDENTIFIER {
                     $$ = new ParameterNode($2, $4, true, true);
                 }
                 | FINAL type ELLIPSIS {
                     $$ = new ParameterNode($2, null, true, true);
                 }

// String
variable_declarator_id : IDENTIFIER {
                           $$ = $1;
                       }
                       | variable_declarator_id LBRACK RBRACK {
                           $$ = $<String>$ + "[]";
                       }

type_parameter_list : type_parameter_list COMMA type_parameter
                    | type_parameter

type_parameter_list_1 : type_parameter_1
                      | type_parameter_list COMMA type_parameter_1

type_parameter : type_variable type_bound_opt

type_parameter_1 : type_variable GT { $$ = $1; }
                 | type_variable type_bound_1

type_bound_1 : EXTENDS reference_type_1 { $$ = $1; }
             | EXTENDS reference_type additional_bound_list_1 { $$ = $1; }

type_bound_opt : type_bound | none

type_bound : EXTENDS reference_type additional_bound_list_opt { $$ = $1; }

additional_bound_list_opt : additional_bound_list | none

additional_bound_list : additional_bound additional_bound_list
                      | additional_bound

additional_bound_list_1 : additional_bound additional_bound_list_1
                        | AND reference_type_1 { $$ = $1;}

additional_bound : AND interface_type { $$ = $1; }

none : { $$ = null; } ;

method_header : modifiers_opt type method_declarator throws {
                  $$ = $3;
                  $<MethodSignatureNode>$.setModifiers($1);
                  $<MethodSignatureNode>$.setReturnType($2);
                  $<MethodSignatureNode>$.setThrows($4);
              }
              | modifiers_opt LT type_parameter_list_1 type method_declarator throws {
                  $$ = $5;
                  $<MethodSignatureNode>$.setModifiers($1);
                  $<MethodSignatureNode>$.setReturnType($4); // FIXME: <> part needs to be added
                  $<MethodSignatureNode>$.setThrows($6);
              }
              | modifiers_opt VOID method_declarator throws {
                  $$ = $3;
                  $<MethodSignatureNode>$.setModifiers($1);
                  $<MethodSignatureNode>$.setReturnType(PrimitiveTypeNode.VOID);
                  $<MethodSignatureNode>$.setThrows($4);
              }
              | modifiers_opt LT type_parameter_list_1 VOID method_declarator throws {
                  $$ = $5;
                  $<MethodSignatureNode>$.setModifiers($1);
                  $<MethodSignatureNode>$.setReturnType(PrimitiveTypeNode.VOID);
                  $<MethodSignatureNode>$.setThrows($6);
              }

%%

}
