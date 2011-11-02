package org.jruby.compiler.ir;

// SSS FIXME: If we can hide these flags from leaking out to the rest of the codebase,
// that would be awesome, but I cannot nest this class in an Enum class.
class OpFlags {
    final static int f_has_side_effect     = 0x002;
    final static int f_can_raise_exception = 0x004;
    final static int f_is_marker_op        = 0x008;
    final static int f_is_jump_or_branch   = 0x010;
    final static int f_is_return           = 0x020;
    final static int f_is_exception        = 0x040;
    final static int f_is_debug_op         = 0x080;
    final static int f_is_load             = 0x100;
    final static int f_is_store            = 0x200;
    final static int f_is_call             = 0x400;
    final static int f_is_arg_receive      = 0x800;
}

public enum Operation {

/* Mark a *non-control-flow* instruction as side-effecting if its compuation is not referentially
 * transparent.  In other words, mark it side-effecting if the following is true:
 *
 *   If "r = op(args)" is the instruction I and v is the value produced by the instruction at runtime,
 *   and replacing I with "r = v" will leave the program behavior unchanged.  If so, and we determine
 *   that the value of 'r' is not used anywhere, then it would be safe to get rid of I altogether.
 *
 * So definitions, calls, returns, stores are all side-effecting by this definition */

// ------ Define the operations below ----
    NOP(0),

    /** control-flow **/
    JUMP(OpFlags.f_is_jump_or_branch),
    JUMP_INDIRECT(OpFlags.f_is_jump_or_branch),
    BEQ(OpFlags.f_is_jump_or_branch),
    BNE(OpFlags.f_is_jump_or_branch),

    /** argument receive related in methods and blocks **/
    RECV_SELF(OpFlags.f_is_arg_receive),
    RECV_ARG(OpFlags.f_is_arg_receive),
    RECV_OPT_ARG(OpFlags.f_is_arg_receive),
    RECV_CLOSURE(OpFlags.f_is_arg_receive),
    RECV_CLOSURE_ARG(OpFlags.f_is_arg_receive),
    RECV_EXCEPTION(OpFlags.f_is_arg_receive),
    SET_ARGS(OpFlags.f_has_side_effect),

    /* By default, call instructions cannot be deleted even if their results
     * aren't used by anyone unless we know more about what the call is, 
     * what it does, etc.  Hence all these are marked side effecting */

    /** calls **/
    CALL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    JRUBY_IMPL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    RUBY_INTERNALS(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    SUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    YIELD(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),

    /* returns unwind stack, etc. */

    /** returns **/
    RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    CLOSURE_RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    /* BREAK is a return because it can only be used within closures
     * and the net result is to return from the closure */
    BREAK(OpFlags.f_has_side_effect | OpFlags.f_is_return),

    /** defines **/
    DEF_MODULE(OpFlags.f_has_side_effect),
    DEF_CLASS(OpFlags.f_has_side_effect),
    DEF_META_CLASS(OpFlags.f_has_side_effect),
    DEF_INST_METH(OpFlags.f_has_side_effect),
    DEF_CLASS_METH(OpFlags.f_has_side_effect),

    THROW(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_exception),

    /** marker instructions used to flag/mark places in the code and dont actually get executed **/
    LABEL(OpFlags.f_is_marker_op),
    EXC_REGION_START(OpFlags.f_is_marker_op),
    EXC_REGION_END(OpFlags.f_is_marker_op),
    CASE(OpFlags.f_is_marker_op), // unused currently

    /** debugging ops **/
    LINE_NUM(OpFlags.f_is_debug_op),
    FILE_NAME(OpFlags.f_is_debug_op),

    /** value loads (SSS FIXME: Do any of these have side effects?) **/
    GET_CONST(OpFlags.f_is_load),
    GET_GLOBAL_VAR(OpFlags.f_is_load), 
    GET_FIELD(OpFlags.f_is_load),
    GET_CVAR(OpFlags.f_is_load | OpFlags.f_can_raise_exception),
    GET_ARRAY(OpFlags.f_is_load),
    BINDING_LOAD(OpFlags.f_is_load),
    SEARCH_CONST(OpFlags.f_is_load | OpFlags.f_can_raise_exception),

    /** value stores **/
    PUT_CONST(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    PUT_GLOBAL_VAR(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    PUT_FIELD(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    PUT_ARRAY(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    PUT_CVAR(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    BINDING_STORE(OpFlags.f_is_store | OpFlags.f_has_side_effect), 
    ATTR_ASSIGN(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    
    /** JRuby-impl instructions **/
    BLOCK_GIVEN(0),
    CHECK_ARITY(OpFlags.f_can_raise_exception),

    /** rest **/
    COPY(0),
    NOT(0), // ruby NOT operator
    SET_RETADDR(0),
    INSTANCE_OF(0), // java instanceof bytecode
    CLASS_VAR_MODULE(0),
    IS_TRUE(0), // checks if the operand is non-null and non-false
    EQQ(0), // (FIXME: Exceptions?) a === call used in when
    RESCUE_EQQ(OpFlags.f_can_raise_exception), // a === call used in rescue
    ALLOC_BINDING(OpFlags.f_has_side_effect),
    THREAD_POLL(OpFlags.f_has_side_effect),
    CONST_MISSING(OpFlags.f_can_raise_exception),
    ENSURE_RUBY_ARRAY(0),

    /** for splitting calls into method-lookup and call -- unused **/
    METHOD_LOOKUP(0),

    /** primitive value boxing/unboxing -- still unused **/
    BOX_VALUE(0),
    UNBOX_VALUE(0),

    /** optimization guards -- still unused **/
    MODULE_VERSION_GUARD(0), 
    METHOD_VERSION_GUARD(0);
    
/* ----------- unused ops ------------------
// primitive alu operations -- unboxed primitive ops (not native ruby)
    ADD(0), SUB(0), MUL(0), DIV(OpFlags.f_can_raise_exception),
    DECLARE_TYPE(OpType.declare_type_op), // Charlie added this for Duby originally?
 * -----------------------------------------*/

    private int flags;

    Operation(int flags) { 
        this.flags = flags;
    }

    public boolean transfersControl() { 
        return (flags & (OpFlags.f_is_jump_or_branch | OpFlags.f_is_return | OpFlags.f_is_exception)) > 0;
    }

    public boolean isLoad() {
        return (flags & OpFlags.f_is_load) > 0;
    }

    public boolean isStore() {
        return (flags & OpFlags.f_is_store) > 0;
    }

    public boolean isCall() {
        return (flags & OpFlags.f_is_call) > 0;
    }

    public boolean isReturn() {
        return (flags & OpFlags.f_is_return) > 0;
    }
    
    public boolean isException() {
        return (flags & OpFlags.f_is_exception) > 0;
    }

    public boolean isArgReceive() {
        return (flags & OpFlags.f_is_arg_receive) > 0;
    }

    public boolean startsBasicBlock() {
        return this == LABEL;
    }

    public boolean endsBasicBlock() {
        return transfersControl();
    }

    public boolean hasSideEffects() {
        return (flags & OpFlags.f_has_side_effect) > 0;
    }

    public boolean isDebugOp() {
        return (flags & OpFlags.f_is_debug_op) > 0;
    }

    // Conservative -- say no only if you know it for sure cannot
    public boolean canRaiseException() {
        return (flags & OpFlags.f_can_raise_exception) > 0;
    }

    @Override
    public String toString() { 
        return name().toLowerCase();
    }
}
