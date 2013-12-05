/*
 * We started with a full Java grammar from JavaParser (cup) implementation 
 * (since it was an LALR grammar already) and then stripped out all bits of 
 * the grammar unrelated to Java method signature parsing.  We also changed the 
 * grammar to accept signatures which do not specify the parameter names. So,
 * 'void foo(int)' is as 'void foo(int name)'.  This grammar also only works
 * with Jay which is another LALR grammar compiler compiler tool.
 *
 * The output this tool generates is subject to our tri-license (GPL,LGPL,
 * and EPL), but this source file itself is released only under the terms
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

import org.jruby.ast.java_signature.Annotation;
import org.jruby.ast.java_signature.AnnotationExpression;
import org.jruby.ast.java_signature.AnnotationParameter;
import org.jruby.ast.java_signature.ArrayAnnotationExpression;
import org.jruby.ast.java_signature.ArrayTypeNode;
import org.jruby.ast.java_signature.CharacterLiteral;
import org.jruby.ast.java_signature.ConstructorSignatureNode;
import org.jruby.ast.java_signature.DefaultAnnotationParameter;
import org.jruby.ast.java_signature.MethodSignatureNode;
import org.jruby.ast.java_signature.Literal;
import org.jruby.ast.java_signature.Modifier;
import org.jruby.ast.java_signature.ParameterNode;
import org.jruby.ast.java_signature.PrimitiveTypeNode;
import org.jruby.ast.java_signature.ReferenceTypeNode;
import org.jruby.ast.java_signature.SignatureNode;
import org.jruby.ast.java_signature.StringLiteral;
import org.jruby.ast.java_signature.TypeNode;
import org.jruby.lexer.JavaSignatureLexer;

public class JavaSignatureParser {
    private static JavaSignatureParser parser = new JavaSignatureParser();

    public static SignatureNode parse(InputStream in) throws IOException, ParserSyntaxException {
        return (SignatureNode) parser.yyparse(JavaSignatureLexer.create(in));
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
%token <String> AT     // '@'
%token <String> DOT    // '.'
%token <String> COMMA  // ','
%token <String> ELLIPSIS // '...' or \u2026
%token <String> EQUAL  // '='
%token <String> LCURLY // '{'
%token <String> RCURLY // '}'
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
%token <String> QQ // '"'
%token <String> Q // "'"
%token <String> CHARACTER_LITERAL
%token <String> STRING_LITERAL

%type <MethodSignatureNode> method_declarator, method_header
%type <ConstructorSignatureNode> constructor_declarator, constructor_declaration
%type <List> formal_parameter_list_opt, formal_parameter_list // <ParameterNode>
%type <List> modifiers_opt, modifiers, modifiers_none, throws, class_type_list
%type <List> annotation_params_opt, annotation_params, annotation_params_none
%type <ParameterNode> formal_parameter
%type <TypeNode> primitive_type, type
%type <ReferenceTypeNode> class_or_interface, class_or_interface_type, array_type
%type <ReferenceTypeNode> interface_type, class_type, reference_type
%type <String> name, type_variable, variable_declarator_id
%type <String> type_bound_1, additional_bound, additional_bound_list_1
%type <String> wildcard, type_argument, type_argument_list
%type <String> type_argument_1, type_argument_2, type_argument_3
%type <String> wildcard_1, wildcard_2, wildcard_3
%type <String> reference_type_1, reference_type_2, reference_type_3
%type <String> type_argument_list_1, type_argument_list_2, type_argument_list_3
%type <String> type_parameter, type_parameter_1
%type <String> type_parameter_list, type_parameter_list_1, 
%type <String> type_bound_opt, type_bound, additional_bound_list, additional_bound_list_opt
%type <String> annotation_name
%type <Object> modifier  // Can be either modifier enum or Annotation instance
%type <ArrayTypeNode> dims
%type <Object> none
%type <SignatureNode> program
%type <Annotation> annotation
%type <AnnotationParameter> annotation_param
%type <AnnotationExpression> annotation_value
%type <List> annotation_array_values
%type <Literal> literal

%%

program : method_header {
     $$ = $1;
 } | constructor_declaration {
     $$ = $1;
 }

type : primitive_type | reference_type { $$ = $<TypeNode>1; }

// PrimitiveTypeNode
primitive_type : BYTE {
     $$ = PrimitiveTypeNode.BYTE;
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

// ReferenceTypeNode
reference_type : class_or_interface_type {
     $$ = $1;
 }
 | array_type {
     $$ = $<ReferenceTypeNode>1;
 }

// String
type_variable : IDENTIFIER { 
     $$ = $1; 
 }

// ReferenceTypeNode
class_or_interface : name {
     $$ = new ReferenceTypeNode($1);
 }
 | class_or_interface LT type_argument_list_1 DOT name {
     String genericTyping = "<" + $3 + "." + $5;
     $$ = $1;
     $1.setGenericsTyping(genericTyping);
 }

// ReferenceTypeNode
class_or_interface_type : class_or_interface
 | class_or_interface LT type_argument_list_1 {
     String genericTyping = "<" + $3;
     $$ = $1;
     $1.setGenericsTyping(genericTyping);
 }

// ReferenceTypeNode
class_type : class_or_interface_type

// ReferenceTypeNode
interface_type : class_or_interface_type

// ReferenceTypeNode
array_type : primitive_type dims {
     $2.setTypeForArray($1);
     $$ = $2;
 }
 | name dims {
     $2.setTypeForArray(new ReferenceTypeNode($1));
     $$ = $2;
 }
 | class_or_interface LT type_argument_list_1 DOT name dims {
     $1.setGenericsTyping("<" + $3 + "." + $5);
     $6.setTypeForArray($1);
     $$ = $6;
 }
 | class_or_interface LT type_argument_list_1 dims {
     $1.setGenericsTyping("<" + $3);
     $4.setTypeForArray($1);
     $$ = $4;
 }

// String
wildcard : QUESTION { 
     $$ = "?";
 } | QUESTION EXTENDS reference_type {
     $$ = "? extends " + $3.getFullyTypedName();
 } | QUESTION SUPER reference_type { 
     $$ = "? super " + $3.getFullyTypedName();
 }

// String
wildcard_1 : QUESTION GT {
     $$ = "?>"; 
 } | QUESTION EXTENDS reference_type_1 {
     $$ = "? extends " + $3;
 } | QUESTION SUPER reference_type_1 { 
     $$ = "? super " + $3;
 }

// String
wildcard_2 : QUESTION RSHIFT { 
     $$ = "?>>"; 
 } | QUESTION EXTENDS reference_type_2 { 
     $$ = "? extends " + $3;
 } | QUESTION SUPER reference_type_2 { 
     $$ = "? super " + $3;
 }

// String
wildcard_3 : QUESTION URSHIFT {
     $$ = "?>>";
 } | QUESTION EXTENDS reference_type_3 {
     $$ = "? extends " + $3;
 } | QUESTION SUPER reference_type_3 {
     $$ = "? super " + $3;
 }

// String
reference_type_1 : reference_type GT { 
     $$ = $1.getFullyTypedName() + ">";
 } | class_or_interface LT type_argument_list_2 {
     $$ = $1.getFullyTypedName() + "<" + $3;
 }

// String
reference_type_2 : reference_type RSHIFT { 
     $$ = $1.getFullyTypedName() + ">>";
 } | class_or_interface LT type_argument_list_3 {
     $$ = $1.getFullyTypedName() + "<" + $3;
 }

// String
reference_type_3 : reference_type URSHIFT {
     $$ = $1.getFullyTypedName() + ">>>";
 }

// String
type_argument_list : type_argument {
     $$ = $1;
 }
 | type_argument_list COMMA type_argument {
     $$ = $1 + ", " + $3;
 }

// String
type_argument_list_1 : type_argument_1
 | type_argument_list COMMA type_argument_1 {
     $$ = $1 + ", " + $3;
 }

// String
type_argument_list_2 : type_argument_2
 | type_argument_list COMMA type_argument_2 {
     $$ = $1 + ", " + $3;
 }

// String
type_argument_list_3 : type_argument_3
 | type_argument_list COMMA type_argument_3 {
     $$ = $1 + ", " + $3;
 }

// String
type_argument : reference_type {
     $$ = $1.getFullyTypedName();
 }
 | wildcard

// String
type_argument_1 : reference_type_1 | wildcard_1

// String
type_argument_2 : reference_type_2 | wildcard_2

// String
type_argument_3 : reference_type_3 | wildcard_3

// List<Object>
modifiers_opt : modifiers | modifiers_none

// List<Object>
modifiers : modifier {
    $$ = new ArrayList<Object>();
    $<List>$.add($1);
 }
 | modifiers modifier {
    $1.add($2);
 }

// List<Object> -- This is just so we don't deal with null's.
modifiers_none : { $$ = new ArrayList<Object>(); }

// Object
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
 | annotation { $$ = $1; }

// String
name : IDENTIFIER { $$ = $1; }                  // Foo (or foo)
 | name DOT IDENTIFIER { $$ = $1 + "." + $3; }  // foo.Foo 

// String -- we do not use this for any info
dims : LBRACK RBRACK { 
     $$ = new ArrayTypeNode(); 
 } | dims LBRACK RBRACK { 
     $$ = new ArrayTypeNode($1);
 }

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
 } | variable_declarator_id LBRACK RBRACK {
     // We know this is always preceeded by 'type' production.
     $<Object>0 = new ArrayTypeNode($<TypeNode>0); 
     $$ = $1;
 }

// String
type_parameter_list : type_parameter_list COMMA type_parameter {
     $$ = $1 + ", " + $3;
 } | type_parameter

// String
type_parameter_list_1 : type_parameter_1
 | type_parameter_list COMMA type_parameter_1 {
     $$ = $1 + ", " + $3;
 }

// String
type_parameter : type_variable type_bound_opt {
     $$ = $1 + $2;
 }

// String
type_parameter_1 : type_variable GT { 
     $$ = $1 + ">"; 
 }
 | type_variable type_bound_1 {
     $$ = $1 + $2;
 }

// String
type_bound_1 : EXTENDS reference_type_1 { 
     $$ = " extends " + $1;
 }
 | EXTENDS reference_type additional_bound_list_1 { 
     $$ = " extends " + $2.getFullyTypedName() + $3;
 }

// String
type_bound_opt : type_bound 
 | none {
     $$ = "";
 }

// String
type_bound : EXTENDS reference_type additional_bound_list_opt { 
     $$ = "extends " + $2.getFullyTypedName() + $3;
 }

// String
additional_bound_list_opt : additional_bound_list 
 | none {
     $$ = "";
 }

// String
additional_bound_list : additional_bound additional_bound_list {
     $$ = $1 + $2;
 } | additional_bound

// String
additional_bound_list_1 : additional_bound additional_bound_list_1 {
     $$ = $1 + $2;
 }
 | AND reference_type_1 { 
     $$ = " & " + $1;
 }

// String
additional_bound : AND interface_type { 
     $$ = " & " + $2.getFullyTypedName();
}

none : { $$ = null; }

constructor_declaration : modifiers_opt constructor_declarator throws {
     $$ = $2;
     $<ConstructorSignatureNode>$.setModifiers($1);
     $<ConstructorSignatureNode>$.setThrows($3);
 } | modifiers_opt LT type_parameter_list_1 constructor_declarator throws {
     $$ = $4;
     $<ConstructorSignatureNode>$.setModifiers($1);
     $<ConstructorSignatureNode>$.setExtraTypeInfo("<" + $3);
     $<ConstructorSignatureNode>$.setThrows($5);
 }

constructor_declarator : name LPAREN formal_parameter_list_opt RPAREN {
     $$ = new ConstructorSignatureNode($1, $3);
 }

method_header : modifiers_opt type method_declarator throws {
                  $$ = $3;
                  $<MethodSignatureNode>$.setModifiers($1);
                  $<MethodSignatureNode>$.setReturnType($2);
                  $<MethodSignatureNode>$.setThrows($4);
              }
              | modifiers_opt LT type_parameter_list_1 type method_declarator throws {
                  $$ = $5;
                  $<MethodSignatureNode>$.setModifiers($1);
                  $<MethodSignatureNode>$.setExtraTypeInfo("<" + $3);
                  $<MethodSignatureNode>$.setReturnType($4);
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
                  $<MethodSignatureNode>$.setExtraTypeInfo("<" + $3);
                  $<MethodSignatureNode>$.setReturnType(PrimitiveTypeNode.VOID);
                  $<MethodSignatureNode>$.setThrows($6);
              }

// Annotation
annotation : annotation_name {
               $$ = new Annotation($1, new ArrayList<AnnotationParameter>());
           }
           | annotation_name LPAREN annotation_params_opt RPAREN {
               $$ = new Annotation($1, $3);
           }

// String
annotation_name : AT name { $$ = $1 + $2; }

// AnnotationParam
annotation_param : type_variable EQUAL annotation_value {
                     $$ = new AnnotationParameter($1, $3);
                 }
                 | annotation_value {
                     $$ = new DefaultAnnotationParameter($1);
                 }

// List<AnnotationParameter>
annotation_params : annotation_param {
                      $$ = new ArrayList<AnnotationParameter>();
                      $<List>$.add($1);
                  }
                  | annotation_params COMMA annotation_param {
                      $1.add($3);
                  }

// AnnotationExpression
annotation_value : annotation {
                     $$ = $<AnnotationExpression>1;
                 }
                 | type {
                     $$ = $<AnnotationExpression>1;
                 }
                 | literal {
                     $$ = $<AnnotationExpression>1;
                 }
                 | LCURLY annotation_array_values RCURLY {
                     $$ = new ArrayAnnotationExpression($2);
                 }
                 | LCURLY RCURLY {
                     $$ = new ArrayAnnotationExpression(new ArrayList<AnnotationExpression>());
                 }

// List<AnnotationExpression>
annotation_array_values : annotation_value {
                            $$ = new ArrayList<AnnotationExpression>();
                            $<List>$.add($1);
                        }
                        | annotation_array_values COMMA annotation_value {
                            $1.add($3);
                        }

// List<AnnotationParameter> -- This is just so we don't deal with null's.
annotation_params_none : { $$ = new ArrayList<AnnotationParameter>(); }

// List<AnnotationParameter>
annotation_params_opt : annotation_params | annotation_params_none

literal : STRING_LITERAL {
           $$ = new StringLiteral($1);
        }
        | CHARACTER_LITERAL {
           $$ = new CharacterLiteral($1);
        }

%%

}
