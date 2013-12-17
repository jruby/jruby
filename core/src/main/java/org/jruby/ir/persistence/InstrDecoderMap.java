/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.ir.IRManager;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;

/**
 *
 * @author enebo
 */
class InstrDecoderMap implements IRPersistenceValues {
    private static final Operation[] operations = Operation.values();
    
    private final IRReaderDecoder decoder;
    private final IRManager manager;

    public InstrDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.decoder = decoder;
    }

    public Instr decode(Operation operation) {
        switch(operation) {
            case ALIAS:
            case ATTR_ASSIGN:
            case BACKREF_IS_MATCH_DATA:
            case BEQ:
            case BINDING_LOAD:
            case BINDING_STORE:
            case BLOCK_GIVEN:
            case BNE:
            case BOX_VALUE:
            case BREAK:
            case B_FALSE:
            case B_NIL:
            case B_TRUE:
            case B_UNDEF:
            case CALL:
            case CHECK_ARGS_ARRAY_ARITY:
            case CHECK_ARITY:
            case CLASS_VAR_IS_DEFINED:
            case CLASS_VAR_MODULE:
            case CONST_MISSING:
            case COPY:
            case DEFINED_CONSTANT_OR_METHOD:
            case DEF_CLASS:
            case DEF_CLASS_METH:
            case DEF_INST_METH:
            case DEF_META_CLASS:
            case DEF_MODULE:
            case EQQ:
            case EXC_REGION_END:
            case EXC_REGION_START:
            case GET_BACKREF:
            case GET_CVAR:
            case GET_ENCODING:
            case GET_ERROR_INFO:
            case GET_FIELD:
            case GET_GLOBAL_VAR:
            case GET_OBJECT:
            case GLOBAL_IS_DEFINED:
            case GVAR_ALIAS:
            case HAS_INSTANCE_VAR:
            case INHERITANCE_SEARCH_CONST:
            case IS_METHOD_BOUND:
            case IS_TRUE:
            case JUMP:
            case JUMP_INDIRECT:
            case LABEL:
            case LAMBDA:
            case LEXICAL_SEARCH_CONST:
            case LINE_NUM:
            case MASGN_OPT:
            case MASGN_REQD:
            case MASGN_REST:
            case MATCH:
            case MATCH2:
            case MATCH3:
            case METHOD_DEFINED:
            case METHOD_IS_PUBLIC:
            case METHOD_LOOKUP:
            case NONLOCAL_RETURN:
            case NOP:
            case NORESULT_CALL:
            case NOT:
            case POP_BINDING:
            case POP_FRAME:
            case PROCESS_MODULE_BODY:
            case PUSH_BINDING:
            case PUSH_FRAME:
            case PUT_ARRAY:
            case PUT_CONST:
            case PUT_CVAR:
            case PUT_FIELD:
            case PUT_GLOBAL_VAR:
            case RAISE_ARGUMENT_ERROR:
            case RECORD_END_BLOCK:
            case RECV_CLOSURE:
            case RECV_EXCEPTION:
            case RECV_KW_ARG:
            case RECV_KW_REST_ARG:
            case RECV_OPT_ARG:
            case RECV_POST_REQD_ARG:
            case RECV_PRE_REQD_ARG:
            case RECV_REST_ARG:
            case RECV_SELF:
            case RESCUE_EQQ:
            case RESTORE_ERROR_INFO:
            case RETURN:
            case SEARCH_CONST:
            case SET_RETADDR:
            case SET_WITHIN_DEFINED:
            case SUPER:
            case SUPER_METHOD_BOUND:
            case THREAD_POLL:
            case THROW:
            case TO_ARY:
            case UNBOX_VALUE:
            case UNDEF_METHOD:
            case YIELD:
           case ZSUPER:
        }
        
        return null;
    }

    public Operation decodeOperationType(int ordinal) {
        if (ordinal >= operations.length) throw new IllegalArgumentException("Invalid Operation Type: " + ordinal);
        
        return operations[ordinal];
    }
    
}
