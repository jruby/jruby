package org.jruby.compiler.ir;

enum OpType { dont_care, obj_op, alu_op, call_op, eval_op, branch_op };

public enum Operation
{
// ------ Define the operations below ----
    NOP(OpType.dont_care),

// value copy and type conversion operations
    COPY(OpType.dont_care), TYPE_CVT(OpType.dont_care), BOX_VAL(OpType.dont_care), UNBOX_OBJ(OpType.dont_care),
    GET_GLOBAL_VAR(OpType.dont_care), PUT_GLOBAL_VAR(OpType.dont_care),

// Constant get & set
	 GET_CONST(OpType.dont_care), PUT_CONST(OpType.dont_care),

// alu operations
    ADD(OpType.alu_op), SUB(OpType.alu_op), MUL(OpType.alu_op), DIV(OpType.alu_op),
    OR(OpType.alu_op), AND(OpType.alu_op), XOR(OpType.alu_op),
    LSHIFT(OpType.alu_op), RSHIFT(OpType.alu_op),

// method handle, arg receive, return value, and  call instructions
    GET_METHOD(OpType.dont_care), RETURN(OpType.dont_care), RECV_ARG(OpType.dont_care), RECV_OPT_ARG(OpType.dont_care), RECV_BLOCK_ARG(OpType.dont_care),
    CALL(OpType.call_op), OCALL(OpType.call_op),

// closure instructions
    YIELD(OpType.dont_care),

// eval instructions
    EVAL_OP(OpType.eval_op), CLASS_EVAL(OpType.eval_op),

// exception instructions
    THROW(OpType.dont_care), RESCUE(OpType.dont_care), RETRY(OpType.dont_care),

// allocate, and instance variable get/set operations
    NEW_OBJ(OpType.obj_op), GET_FIELD(OpType.obj_op), PUT_FIELD(OpType.obj_op), 
	 GET_CVAR(OpType.dont_care), PUT_CVAR(OpType.dont_care),

// jump and branch operations
    LABEL(OpType.dont_care), BREAK(OpType.dont_care),
    JUMP(OpType.branch_op), BEQ(OpType.branch_op), BNE(OpType.branch_op), BLE(OpType.branch_op), BLT(OpType.branch_op), BGE(OpType.branch_op), BGT(OpType.branch_op),

// others
    THREAD_POLL(OpType.dont_care),

// a === call used only for its conditional results, as in case/when, begin/rescue, ...
    EQQ(OpType.call_op),

// a case/when branch
    CASE(OpType.branch_op);

    private OpType _type;

    Operation(OpType t) { _type = t; }

    public boolean isALU()    { return _type == OpType.alu_op; }
    public boolean isBranch() { return _type == OpType.branch_op; }
    public boolean isCall()   { return _type == OpType.call_op; }
    public boolean isEval()   { return _type == OpType.eval_op; }

}
