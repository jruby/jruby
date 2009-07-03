package org.jruby.compiler.ir;

public enum Operation
{
    enum OpType { dont_care, obj_op, alu_op, call_op, eval_op, branch_op };

    private OpType _type;

    public Operation(OpType t) { _type = t; }

    public boolean isALU()    { return _type == alu_op; }
    public boolean isBranch() { return _type == branch_op; }
    public boolean isCall()   { return _type == call_op; }
    public boolean isEval()   { return _type == eval_op; }

// ------ Define the operations below ----
    NOP(dont_care),

// value copy and type conversion operations
    COPY(dont_care), TYPE_CVT(dont_care), BOX_VAL(dont_care), UNBOX_OBJ(dont_care),
    GET_GLOBAL_VAR(dont_care), PUT_GLOBAL_VAR(dont_care),

// Constant get & set
	 GET_CONST(dont_care), PUT_CONST(dont_care),

// alu operations
    ADD(alu_op), SUB(alu_op), MUL(alu_op), DIV(alu_op),
    OR(alu_op), AND(alu_op), XOR(alu_op),
    LSHIFT(alu_op), RSHIFT(alu_op),

// method handle, arg receive, return value, and  call instructions
    GET_METHOD(dont_care), RETURN(dont_care), RECV_ARG(dont_care), RECV_OPT_ARG(dont_care), RECV_BLOCK_ARG(dont_care),
    CALL(call_op), OCALL(call_op),

// closure instructions
    BEG_CLOSURE(dont_care), END_CLOSURE(dont_care), 

// eval instructions
    EVAL_OP(eval_op), CLASS_EVAL(eval_op),

// exception instructions
    THROW(dont_care), RESCUE(dont_care),

// allocate, and instance variable get/set operations
    NEW_OBJ(obj_op), GET_FIELD(obj_op), PUT_FIELD(obj_op), 

// jump and branch operations
    LABEL(dont_care), BREAK(dont_care),
    JUMP(branch_op), BEQ(branch_op), BNE(branch_op), BLE(branch_op), BLT(branch_op), BGE(branch_op), BGT(branch_op),

// others
    THREAD_POLL(dont_care),

// a === call used only for its conditional results, as in case/when, begin/rescue, ...
    EQQ(call_op),

// a case/when branch
    CASE(branch_op)
}
