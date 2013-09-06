package org.jruby.ir;

// SSS FIXME: If we can hide these flags from leaking out to the rest of the codebase,
// that would be awesome, but I cannot nest this class in an Enum class.
class OpFlags {
    final static int f_has_side_effect     = 0x0001;
    final static int f_can_raise_exception = 0x0002;
    final static int f_is_marker_op        = 0x0004;
    final static int f_is_jump_or_branch   = 0x0008;
    final static int f_is_return           = 0x0010;
    final static int f_is_exception        = 0x0020;
    final static int f_is_debug_op         = 0x0040;
    final static int f_is_load             = 0x0080;
    final static int f_is_store            = 0x0100;
    final static int f_is_call             = 0x0200;
    final static int f_is_arg_receive      = 0x0400;
    final static int f_modifies_code       = 0x0800;
    final static int f_inline_unfriendly   = 0x1000;
    final static int f_is_book_keeping_op  = 0x4000;
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

    /** argument receive in methods and blocks **/
    RECV_SELF(0),
    RECV_PRE_REQD_ARG(OpFlags.f_is_arg_receive),
    RECV_POST_REQD_ARG(OpFlags.f_is_arg_receive),
    RECV_KW_ARG(OpFlags.f_is_arg_receive),
    RECV_KW_REST_ARG(OpFlags.f_is_arg_receive),
    RECV_REST_ARG(OpFlags.f_is_arg_receive),
    RECV_OPT_ARG(OpFlags.f_is_arg_receive),
    RECV_CLOSURE(OpFlags.f_is_arg_receive),
    RECV_EXCEPTION(OpFlags.f_is_arg_receive),

    /* By default, call instructions cannot be deleted even if their results
     * aren't used by anyone unless we know more about what the call is,
     * what it does, etc.  Hence all these are marked side effecting */

    /** calls **/
    CALL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    NORESULT_CALL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    SUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    ZSUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    YIELD(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    LAMBDA(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    RUNTIME_HELPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),

    /* specialized calls */
    CALL_1F(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    CALL_1O(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    CALL_0O(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    NORESULT_CALL_1O(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),

    /** returns -- returns unwind stack, etc. */
    RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    NONLOCAL_RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
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
    PROCESS_MODULE_BODY(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly),
    UNDEF_METHOD(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_modifies_code),

    /** marker instructions used to flag/mark places in the code and dont actually get executed **/
    LABEL(OpFlags.f_is_book_keeping_op | OpFlags.f_is_marker_op),
    EXC_REGION_START(OpFlags.f_is_book_keeping_op | OpFlags.f_is_marker_op),
    EXC_REGION_END(OpFlags.f_is_book_keeping_op | OpFlags.f_is_marker_op),

    /** constant operations */
    LEXICAL_SEARCH_CONST(OpFlags.f_can_raise_exception),
    INHERITANCE_SEARCH_CONST(OpFlags.f_can_raise_exception),
    CONST_MISSING(OpFlags.f_can_raise_exception),
    SEARCH_CONST(OpFlags.f_can_raise_exception),

    /** value loads (SSS FIXME: Do any of these have side effects?) **/
    GET_GLOBAL_VAR(OpFlags.f_is_load),
    GET_FIELD(OpFlags.f_is_load),
    GET_CVAR(OpFlags.f_is_load | OpFlags.f_can_raise_exception),
    BINDING_LOAD(OpFlags.f_is_load),
    MASGN_OPT(OpFlags.f_is_load),
    MASGN_REQD(OpFlags.f_is_load),
    MASGN_REST(OpFlags.f_is_load),

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

    /** debugging ops **/
    LINE_NUM(OpFlags.f_is_book_keeping_op | OpFlags.f_is_debug_op),

    /** JRuby-impl instructions **/
    COPY(0),
    NOT(0), // ruby NOT operator
    BLOCK_GIVEN(0),
    GET_OBJECT(0),
    GET_BACKREF(0),
    RESTORE_ERROR_INFO(OpFlags.f_has_side_effect),
    RAISE_ARGUMENT_ERROR(OpFlags.f_can_raise_exception),
    CHECK_ARITY(OpFlags.f_is_book_keeping_op | OpFlags.f_can_raise_exception),
    CHECK_ARGS_ARRAY_ARITY(OpFlags.f_can_raise_exception),
    RECORD_END_BLOCK(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    TO_ARY(0),
    ENSURE_RUBY_ARRAY(0),
    THROW(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_exception),
    MATCH(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_call),
    MATCH2(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_call),
    MATCH3(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_call),
    SET_RETADDR(0),
    CLASS_VAR_MODULE(0),
    IS_TRUE(0), // checks if the operand is non-null and non-false
    EQQ(0), // (FIXME: Exceptions?) a === call used in when
    RESCUE_EQQ(OpFlags.f_can_raise_exception), // a === call used in rescue
    THREAD_POLL(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    GET_ENCODING(0),

    /* Instructions to support defined? */
    SET_WITHIN_DEFINED(OpFlags.f_has_side_effect),
    DEFINED_CONSTANT_OR_METHOD(OpFlags.f_can_raise_exception),
    METHOD_DEFINED(OpFlags.f_can_raise_exception),
    BACKREF_IS_MATCH_DATA(0),
    CLASS_VAR_IS_DEFINED(0),
    GLOBAL_IS_DEFINED(0),
    HAS_INSTANCE_VAR(0),
    IS_METHOD_BOUND(0),
    METHOD_IS_PUBLIC(0),
    SUPER_METHOD_BOUND(0),
    GET_ERROR_INFO(0),

    /** Other JRuby internal primitives for optimizations */
    MODULE_GUARD(OpFlags.f_is_jump_or_branch), /* a guard acts as a branch */
    PUSH_FRAME(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    PUSH_BINDING(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    POP_FRAME(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    POP_BINDING(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    METHOD_LOOKUP(0), /* for splitting calls into method-lookup and call -- unused **/
    BOX_VALUE(0), /* primitive value boxing/unboxing -- unused */
    UNBOX_VALUE(0); /* unused */

/* ----------- unused ops ------------------
// primitive alu operations -- unboxed primitive ops (not native ruby)
    ADD(0), SUB(0), MUL(0), DIV(OpFlags.f_can_raise_exception),
 * -----------------------------------------*/

    public final OpClass opClass;
    private int flags;

    Operation(int flags) {
        this.flags = flags;

        if (this.isArgReceive()) {
            this.opClass = OpClass.ARG_OP;
        } else if (this.isBranch()) {
            this.opClass = OpClass.BRANCH_OP;
        } else if (this.isBookKeepingOp()) {
            this.opClass = OpClass.BOOK_KEEPING_OP;
        } else if (this.isCall()) {
            this.opClass = OpClass.CALL_OP;
        } else {
            this.opClass = OpClass.OTHER_OP;
        }
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

    public boolean isBranch() {
        return (flags & OpFlags.f_is_jump_or_branch) > 0;
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

    public boolean isBookKeepingOp() {
        return (flags & OpFlags.f_is_book_keeping_op) > 0;
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
