/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.ir.IRManager;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.OperandType;
import org.jruby.ir.operands.UnexecutableNil;
import static org.jruby.ir.operands.UnexecutableNil.U_NIL;

/**
 *
 * @author enebo
 */
class OperandDecoderMap {
    private static final OperandType[] operands = OperandType.values();
    
    private final IRReaderDecoder decoder;
    private IRManager manager;
    
    public OperandDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.decoder = decoder;
    }

    public OperandType decodeOperandType(int ordinal) {
        if (ordinal >= operands.length) throw new IllegalArgumentException("Invalid Operation Type: " + ordinal);
        
        return operands[ordinal];
    }
    
    public Operand decode(OperandType type) {
        switch (type) {
            case ANYTHING: // ??
            case ARRAY:
            case AS_STRING:
            case ATTRIBUTE:
            case BACKREF:
            case BACKTICK_STRING:
            case BIGNUM:
            case BOOLEAN_LITERAL:
            case COMPOUND_ARRAY:
            case COMPOUND_STRING:
            case CURRENT_SCOPE:
            case DYNAMIC_SYMBOL:
            case FIXNUM:
            case FLOAT:
            case GLOBAL_VARIABLE:
            case HASH:
            case IR_EXCEPTION:
            case LABEL:
            case LATTICE_BOTTOM: // ??
            case LATTICE_TOP: // ??
            case LOCAL_VARIABLE:
            case METHOD_HANDLE:
            case METH_ADDR:
            case NIL: return manager.getNil();
            case NTH_REF:
            case OBJECT_CLASS:
            case RANGE:
            case REGEXP:
            case SCOPE_MODULE:
            case SELF:
            case SPLAT:
            case STANDARD_ERROR:
            case STRING_LITERAL:
            case SVALUE:
            case SYMBOL:
            case TEMPORARY_VARIABLE:
            case UNDEFINED_VALUE:
            case UNEXECUTABLE_NIL: return U_NIL;
            case WRAPPED_IR_CLOSURE:
        }
        
        return null;
    }
    
}
