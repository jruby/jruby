/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.operands;

public enum OperandType {

    ARRAY,
    AS_STRING,
    BACKREF,
    BACKTICK_STRING, BIGNUM, 
    BOOLEAN_LITERAL,
    LOCAL_VARIABLE, // Also applicable for ClosureLocalVariable
    COMPOUND_ARRAY,
    COMPOUND_STRING,
    CURRENT_SCOPE,
    DYNAMIC_SYMBOL,
    FIXNUM,
    FLOAT,
    GLOBAL_VARIABLE,
    HASH,
    IR_EXCEPTION,
    LABEL,
    METH_ADDR,
    METHOD_HANDLE,
    NIL,
    NTH_REF,
    OBJECT_CLASS,
    RANGE,
    REGEXP,
    SCOPE_MODULE,
    SELF,
    SPLAT,
    STANDARD_ERROR,
    STRING_LITERAL,
    SVALUE,
    SYMBOL,
    TEMPORARY_VARIABLE, // Also applicable for ClosureTemporaryVariable
    UNDEFINED_VALUE,
    UNEXECUTABLE_NIL,
    WRAPPED_IR_CLOSURE,
    
    // Data flow constants
    LATTICE_BOTTOM,
    LATTICE_TOP,
    ANYTHING,
    
    // Unused instruction, its here for consistency
    ATTRIBUTE
    ;
    
    public String toString() {
        return name().toLowerCase();
    };

}

