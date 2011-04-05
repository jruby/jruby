package org.jruby.compiler.ir;

enum OpType { dont_care, debug_op, obj_op, alu_op, call_op, yield_op, recv_arg_op, ret_op, eval_op, branch_op, compare_op, exc_op, load_op, store_op, declare_type_op, guard_op, box_op, marker_op };

public enum Operation {
// ------ Define the operations below ----

// value copy and type conversion operations
    COPY(OpType.dont_care), SET_RETADDR(OpType.dont_care),

// ruby NOT
    NOT(OpType.dont_care),
    
// primitive alu operations -- unboxed primitive ops (not native ruby)
    ADD(OpType.alu_op), SUB(OpType.alu_op), MUL(OpType.alu_op), DIV(OpType.alu_op),

// method handle, arg receive, return value, and  call instructions
//   BREAK is a ret_op not a branch_op because it can only be used within closures
//   and the net result is to return from the closure
    RETURN(OpType.ret_op), CLOSURE_RETURN(OpType.ret_op), BREAK(OpType.ret_op),
    RECV_ARG(OpType.recv_arg_op), RECV_SELF(OpType.recv_arg_op), RECV_CLOSURE(OpType.recv_arg_op), RECV_OPT_ARG(OpType.recv_arg_op), RECV_CLOSURE_ARG(OpType.recv_arg_op),
    RECV_EXCEPTION(OpType.recv_arg_op),
    CALL(OpType.call_op), JRUBY_IMPL(OpType.call_op), RUBY_INTERNALS(OpType.call_op),
    METHOD_LOOKUP(OpType.dont_care),

// closure instructions
    YIELD(OpType.yield_op),

// def instructions
    DEF_MODULE(OpType.dont_care), DEF_CLASS(OpType.dont_care), DEF_INST_METH(OpType.dont_care), DEF_CLASS_METH(OpType.dont_care),

// exception instructions
    THROW(OpType.exc_op), RETRY(OpType.dont_care),

// marker instructions -- used by the compiler to flag/mark places in the code, and dont actually get executed
	 LABEL(OpType.marker_op), EXC_REGION_START(OpType.marker_op), EXC_REGION_END(OpType.marker_op), CASE(OpType.marker_op),

// debugging / stacktrace info
    LINE_NUM(OpType.debug_op), FILE_NAME(OpType.debug_op),

// Loads
    GET_CONST(OpType.load_op), GET_GLOBAL_VAR(OpType.load_op), GET_FIELD(OpType.load_op), GET_CVAR(OpType.load_op),
    // SSS: Are these 3 loads really?
	 GET_ARRAY(OpType.load_op), BINDING_LOAD(OpType.load_op), SEARCH_CONST(OpType.load_op),

// Stores
    PUT_CONST(OpType.store_op), PUT_GLOBAL_VAR(OpType.store_op), PUT_FIELD(OpType.store_op), PUT_ARRAY(OpType.store_op), PUT_CVAR(OpType.store_op),
    BINDING_STORE(OpType.store_op), ATTR_ASSIGN(OpType.store_op),

// jump and branch operations
    JUMP(OpType.branch_op), JUMP_INDIRECT(OpType.branch_op), BEQ(OpType.branch_op), BNE(OpType.branch_op),

// others
    ALLOC_BINDING(OpType.dont_care), THREAD_POLL(OpType.dont_care),
    DECLARE_TYPE(OpType.declare_type_op), // Charlie added this for Duby originally?

// comparisons & checks
    IS_TRUE(OpType.compare_op), // checks if the operand is non-null and non-false
    EQQ(OpType.compare_op), // EQQ a === call used only for its conditional results, as in case/when, begin/rescue, ...
    
// optimization version guards
    MODULE_VERSION_GUARD(OpType.guard_op), METHOD_VERSION_GUARD(OpType.guard_op),

// primitive value boxing/unboxing
    BOX_VALUE(OpType.box_op), UNBOX_VALUE(OpType.box_op);

    private OpType type;

    Operation(OpType t) { 
        type = t;
    }

    public boolean isALU() {
        return type == OpType.alu_op;
    }

    public boolean xfersControl() { 
        return isBranch() || isReturn() || isException();
    }

    public boolean isBranch() {
        return type == OpType.branch_op;
    }

    public boolean isLoad() {
        return type == OpType.load_op;
    }

    public boolean isStore() {
        return type == OpType.store_op;
    }

    public boolean isCall() {
        return type == OpType.call_op;
    }

    public boolean isEval() {
        return type == OpType.eval_op;
    }

    public boolean isReturn() {
        return type == OpType.ret_op;
    }
    
    public boolean isException() {
        return type == OpType.exc_op;
    }

    public boolean isArgReceive() {
        return type == OpType.recv_arg_op;
    }

    public boolean startsBasicBlock() {
        return this == LABEL;
    }

    public boolean endsBasicBlock() {
        return xfersControl();
    }

    // By default, call instructions cannot be deleted even if their results aren't used by anyone
    // unless we know more about what the call is, what it does, etc.
    // Similarly for evals, stores, returns.
    public boolean hasSideEffects() {
        return isCall() || isEval() || isStore() || isReturn() || isException() || type == OpType.yield_op;
    }

	 // Conservative -- say no only if you know it for sure cannot
    public boolean canRaiseException() {
        return (type != OpType.ret_op) && (type != OpType.debug_op) && (type != OpType.recv_arg_op) && (type != OpType.branch_op) && (type != OpType.marker_op);
    }

    @Override
    public String toString() { 
        return name().toLowerCase();
    }
}
