package org.jruby.compiler.ir;

// SSS FIXME: If we can hide these flags from leaking out to the rest of the codebase,
// that would be awesome, but I cannot nest this class in an Enum class.
class OpFlags {
    final static int f_has_side_effect     = 0x0002;
    final static int f_can_raise_exception = 0x0004;
    final static int f_is_marker_op        = 0x0008;
    final static int f_is_jump_or_branch   = 0x0010;
    final static int f_is_return           = 0x0020;
    final static int f_is_exception        = 0x0040;
    final static int f_is_debug_op         = 0x0080;
    final static int f_is_load             = 0x0100;
    final static int f_is_store            = 0x0200;
    final static int f_is_call             = 0x0400;
    final static int f_is_arg_receive      = 0x0800;
    final static int f_modifies_code       = 0x1000;
    final static int f_inline_unfriendly   = 0x2000;
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
    B_UNDEF(OpFlags.f_is_jump_or_branch),
    B_NIL(OpFlags.f_is_jump_or_branch),
    B_TRUE(OpFlags.f_is_jump_or_branch),
    B_FALSE(OpFlags.f_is_jump_or_branch),

    /** guard instruction which also acts as a branch */
    MODULE_GUARD(OpFlags.f_is_jump_or_branch),

    /** argument receive in methods and blocks **/
    RECV_SELF(0),
    RECV_PRE_REQD_ARG(OpFlags.f_is_arg_receive),
    RECV_POST_REQD_ARG(OpFlags.f_is_arg_receive),
    RECV_REST_ARG(OpFlags.f_is_arg_receive),
    RECV_OPT_ARG(OpFlags.f_is_arg_receive),
    RECV_CLOSURE(OpFlags.f_is_arg_receive),
    RECV_EXCEPTION(OpFlags.f_is_arg_receive),

    /* By default, call instructions cannot be deleted even if their results
     * aren't used by anyone unless we know more about what the call is, 
     * what it does, etc.  Hence all these are marked side effecting */

    /** calls **/
    CALL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    LAMBDA(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    JRUBY_IMPL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    SUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    ZSUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    YIELD(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),

    /* returns unwind stack, etc. */

    /** returns **/
    RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    CLOSURE_RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    /* BREAK is a return because it can only be used within closures
     * and the net result is to return from the closure */
    BREAK(OpFlags.f_has_side_effect | OpFlags.f_is_return),

    /** defines **/
    ALIAS(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_modifies_code),
    GVAR_ALIAS(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_modifies_code),
    DEF_MODULE(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly),
    DEF_CLASS(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly),
    DEF_META_CLASS(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly),
    DEF_INST_METH(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly),
    DEF_CLASS_METH(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly),
    UNDEF_METHOD(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_modifies_code),

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
    GET_GLOBAL_VAR(OpFlags.f_is_load),
    GET_FIELD(OpFlags.f_is_load),
    GET_CVAR(OpFlags.f_is_load | OpFlags.f_can_raise_exception),
    BINDING_LOAD(OpFlags.f_is_load),
    MASGN_OPT(OpFlags.f_is_load),
    MASGN_REQD(OpFlags.f_is_load),
    MASGN_REST(OpFlags.f_is_load),

    /** constant operations */
    LEXICAL_SEARCH_CONST(OpFlags.f_can_raise_exception),
    INHERITANCE_SEARCH_CONST(OpFlags.f_can_raise_exception),
    CONST_MISSING(OpFlags.f_can_raise_exception),
    SEARCH_CONST(OpFlags.f_can_raise_exception),

    /** value stores **/
    PUT_CONST(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    // SSS FIXME: Not all global variable sets can throw exceptions.  Should we split this
    // operation into two different operations?  Those that can throw exceptions and those
    // that cannot.  But, for now, this should be good enough
    PUT_GLOBAL_VAR(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    PUT_FIELD(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    PUT_ARRAY(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    PUT_CVAR(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    BINDING_STORE(OpFlags.f_is_store | OpFlags.f_has_side_effect), 
    ATTR_ASSIGN(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    
    /* defined */
    BACKREF_IS_MATCH_DATA(0),
    CLASS_VAR_IS_DEFINED(0),
    DEFINED_CONSTANT_OR_METHOD(0),
    GLOBAL_IS_DEFINED(0),
    HAS_INSTANCE_VAR(0),
    IS_METHOD_BOUND(0),
    METHOD_DEFINED(0),
    METHOD_IS_PUBLIC(0),
    SET_WITHIN_DEFINED(0),
    SUPER_METHOD_BOUND(0),
    
    /** JRuby-impl instructions **/
    BLOCK_GIVEN(0),
    GET_ERROR_INFO(0),
    GET_OBJECT(0),
    GET_BACKREF(0),
    RESTORE_ERROR_INFO(0),
    RAISE_ARGUMENT_ERROR(OpFlags.f_can_raise_exception),
    // SSS FIXME: Come up with a different strategy/flag to handle this situation
    // check_arity instructions cannot be dead-code-eliminated.
    // So, using has_side_effect flag to prevent its deletion. 
    CHECK_ARITY(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    CHECK_ARGS_ARRAY_ARITY(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    RECORD_END_BLOCK(OpFlags.f_has_side_effect),
    TO_ARY(OpFlags.f_can_raise_exception),

    /** rest **/
    MATCH(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_call),
    MATCH2(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_call),
    MATCH3(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_call),
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
    ENSURE_RUBY_ARRAY(0),
    GET_ENCODING(0),

    /** for splitting calls into method-lookup and call -- unused **/
    METHOD_LOOKUP(0),

    /** primitive value boxing/unboxing -- still unused **/
    BOX_VALUE(0),
    UNBOX_VALUE(0);
    
/* ----------- unused ops ------------------
// primitive alu operations -- unboxed primitive ops (not native ruby)
    ADD(0), SUB(0), MUL(0), DIV(OpFlags.f_can_raise_exception),
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
    
    public boolean modifiesCode() {
        return (flags & OpFlags.f_modifies_code) > 0;
    }

    public boolean inlineUnfriendly() {
        return (flags & OpFlags.f_inline_unfriendly) > 0;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
