package org.jruby.ir;

class OpFlags {
    final static int f_has_side_effect     = 0x00001; // Used by analyses
    final static int f_can_raise_exception = 0x00002; // Used by analyses
    final static int f_is_marker_op        = 0x00004; // UNUSED
    final static int f_is_jump_or_branch   = 0x00008; // Used by analyses
    final static int f_is_return           = 0x00010; // Used by analyses
    final static int f_is_exception        = 0x00020; // Used by analyses
    final static int f_is_debug_op         = 0x00040; // Used by analyses
    final static int f_is_load             = 0x00080; // UNUSED
    final static int f_is_store            = 0x00100; // UNUSED
    final static int f_is_call             = 0x00200; // Only used to opt. interpreter loop
    final static int f_is_arg_receive      = 0x00400; // Only used to opt. interpreter loop
    final static int f_modifies_code       = 0x00800; // Profiler uses this
    final static int f_inline_unfriendly   = 0x01000; // UNUSED: Inliner might use this later
    final static int f_is_book_keeping_op  = 0x02000; // Only used to opt. interpreter loop
    final static int f_is_float_op         = 0x04000; // Only used to opt. interpreter loop
    final static int f_is_int_op           = 0x08000; // Only used to opt. interpreter loop
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
    RECV_RUBY_EXC(OpFlags.f_is_arg_receive),
    RECV_JRUBY_EXC(OpFlags.f_is_arg_receive),
    LOAD_IMPLICIT_CLOSURE(OpFlags.f_is_arg_receive),

    /** Instruction to reify an passed-in block to a Proc for def foo(&b) */
    REIFY_CLOSURE(0),
    LOAD_FRAME_CLOSURE(0),

    /* By default, call instructions cannot be deleted even if their results
     * aren't used by anyone unless we know more about what the call is,
     * what it does, etc.  Hence all these are marked side effecting */

    /** calls **/
    CALL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    NORESULT_CALL(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    ATTR_ASSIGN(OpFlags.f_is_call | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    CLASS_SUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    INSTANCE_SUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    UNRESOLVED_SUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    ZSUPER(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),

    /* specialized calls */
    CALL_1F(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    CALL_1D(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    CALL_1O(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    CALL_1OB(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    CALL_0O(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),
    NORESULT_CALL_1O(OpFlags.f_has_side_effect | OpFlags.f_is_call | OpFlags.f_can_raise_exception),

    /** Ruby operators: should all these be calls? Implementing instrs don't inherit from CallBase.java */
    EQQ(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception), // a === call used in when
    LAMBDA(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    MATCH(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    MATCH2(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    MATCH3(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),

    /* Yield: Is this a call? Implementing instr doesn't inherit from CallBase.java */
    YIELD(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),

    /** returns -- returns unwind stack, etc. */
    RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    NONLOCAL_RETURN(OpFlags.f_has_side_effect | OpFlags.f_is_return),
    /* BREAK is a return because it can only be used within closures
     * and the net result is to return from the closure. */
    BREAK(OpFlags.f_has_side_effect | OpFlags.f_is_return),

    /** defines **/
    ALIAS(OpFlags.f_has_side_effect| OpFlags.f_modifies_code | OpFlags.f_can_raise_exception),
    DEF_MODULE(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly | OpFlags.f_can_raise_exception),
    DEF_CLASS(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly | OpFlags.f_can_raise_exception),
    DEF_META_CLASS(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly | OpFlags.f_can_raise_exception),
    DEF_INST_METH(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly | OpFlags.f_can_raise_exception),
    DEF_CLASS_METH(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly | OpFlags.f_can_raise_exception),
    PROCESS_MODULE_BODY(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_inline_unfriendly | OpFlags.f_can_raise_exception),
    UNDEF_METHOD(OpFlags.f_has_side_effect | OpFlags.f_modifies_code | OpFlags.f_can_raise_exception),

    /** SSS FIXME: This can throw an exception only in tracing mode
     ** Should override canRaiseException in GVarAliasInstr to implement this maybe */
    GVAR_ALIAS(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_modifies_code),

    /** marker instructions used to flag/mark places in the code and dont actually get executed **/
    LABEL(OpFlags.f_is_book_keeping_op | OpFlags.f_is_marker_op),
    EXC_REGION_START(OpFlags.f_is_book_keeping_op | OpFlags.f_is_marker_op),
    EXC_REGION_END(OpFlags.f_is_book_keeping_op | OpFlags.f_is_marker_op),

    /** constant operations */
    LEXICAL_SEARCH_CONST(OpFlags.f_can_raise_exception),
    INHERITANCE_SEARCH_CONST(OpFlags.f_can_raise_exception),
    CONST_MISSING(OpFlags.f_can_raise_exception),
    SEARCH_CONST(OpFlags.f_can_raise_exception),

    GET_GLOBAL_VAR(OpFlags.f_is_load),
    GET_FIELD(OpFlags.f_is_load),
    /** SSS FIXME: Document what causes this instr to raise an exception */
    GET_CVAR(OpFlags.f_is_load | OpFlags.f_can_raise_exception),

    /** value stores **/
    // SSS FIXME: Not all global variable sets can throw exceptions.  Should we split this
    // operation into two different operations?  Those that can throw exceptions and those
    // that cannot.  But, for now, this should be good enough
    PUT_GLOBAL_VAR(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    // put_const, put_cvar, put_field can raise exception trying to store into a frozen objects
    PUT_CONST(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    PUT_CVAR(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    PUT_FIELD(OpFlags.f_is_store | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),

    /** debugging ops **/
    LINE_NUM(OpFlags.f_is_book_keeping_op | OpFlags.f_is_debug_op),
    TRACE(OpFlags.f_is_book_keeping_op | OpFlags.f_is_debug_op | OpFlags.f_has_side_effect),

    /** JRuby-impl instructions **/
    ARG_SCOPE_DEPTH(0),
    BINDING_LOAD(OpFlags.f_is_load),
    BINDING_STORE(OpFlags.f_is_store | OpFlags.f_has_side_effect),
    BUILD_COMPOUND_ARRAY(OpFlags.f_can_raise_exception),
    BUILD_COMPOUND_STRING(OpFlags.f_can_raise_exception),
    BUILD_DREGEXP(OpFlags.f_can_raise_exception),
    BUILD_RANGE(OpFlags.f_can_raise_exception),
    BUILD_SPLAT(OpFlags.f_can_raise_exception),
    BACKTICK_STRING(OpFlags.f_can_raise_exception),
    CHECK_ARGS_ARRAY_ARITY(OpFlags.f_can_raise_exception),
    CHECK_ARITY(OpFlags.f_is_book_keeping_op | OpFlags.f_can_raise_exception),
    CHECK_FOR_LJE(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    CLASS_VAR_MODULE(0),
    COPY(0),
    GET_ENCODING(0),
    MASGN_OPT(0),
    MASGN_REQD(0),
    MASGN_REST(0),
    RAISE_ARGUMENT_ERROR(OpFlags.f_can_raise_exception),
    RAISE_REQUIRED_KEYWORD_ARGUMENT_ERROR(OpFlags.f_can_raise_exception),
    RECORD_END_BLOCK(OpFlags.f_has_side_effect),
    RESCUE_EQQ(OpFlags.f_can_raise_exception), // a === call used in rescue
    RUNTIME_HELPER(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    SET_CAPTURED_VAR(OpFlags.f_can_raise_exception),
    THREAD_POLL(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),
    THROW(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception | OpFlags.f_is_exception),
    // FIXME: TO_ARY is marked side-effecting since it can allocate new objects
    // Clarify semantics of 'f_has_side_effect' better
    TO_ARY(OpFlags.f_has_side_effect | OpFlags.f_can_raise_exception),

    /* Instructions to support defined? */
    BLOCK_GIVEN(0),
    GET_ERROR_INFO(0),
    RESTORE_ERROR_INFO(OpFlags.f_has_side_effect),

    /* Boxing/Unboxing between Ruby <--> Java types */
    BOX_FIXNUM(0),
    BOX_FLOAT(0),
    BOX_BOOLEAN(0),
    UNBOX_FIXNUM(0),
    UNBOX_FLOAT(0),
    UNBOX_BOOLEAN(0),

    /* Unboxed ALU ops */
    IADD(OpFlags.f_is_int_op),
    ISUB(OpFlags.f_is_int_op),
    IMUL(OpFlags.f_is_int_op),
    IDIV(OpFlags.f_is_int_op),
    ILT(OpFlags.f_is_int_op),
    IGT(OpFlags.f_is_int_op),
    IOR(OpFlags.f_is_int_op),
    IAND(OpFlags.f_is_int_op),
    IXOR(OpFlags.f_is_int_op),
    ISHL(OpFlags.f_is_int_op),
    ISHR(OpFlags.f_is_int_op),
    IEQ(OpFlags.f_is_int_op),
    FADD(OpFlags.f_is_float_op),
    FSUB(OpFlags.f_is_float_op),
    FMUL(OpFlags.f_is_float_op),
    FDIV(OpFlags.f_is_float_op),
    FLT(OpFlags.f_is_float_op),
    FGT(OpFlags.f_is_float_op),
    FEQ(OpFlags.f_is_float_op),

    /** Other JRuby internal primitives for optimizations */
    MODULE_GUARD(OpFlags.f_is_jump_or_branch), /* a guard acts as a branch */
    PUSH_FRAME(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    PUSH_BINDING(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    POP_FRAME(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect),
    POP_BINDING(OpFlags.f_is_book_keeping_op | OpFlags.f_has_side_effect);

    public final OpClass opClass;
    private int flags;

    Operation(int flags) {
        this.flags = flags;

        if (this.isArgReceive()) {
            this.opClass = OpClass.ARG_OP;
        } else if ((flags & OpFlags.f_is_return) > 0) {
            this.opClass = OpClass.RET_OP;
        } else if (this.isBranch()) {
            this.opClass = OpClass.BRANCH_OP;
        } else if (this.isBookKeepingOp()) {
            this.opClass = OpClass.BOOK_KEEPING_OP;
        } else if (this.isCall()) {
            this.opClass = OpClass.CALL_OP;
        } else if ((flags & OpFlags.f_is_int_op) > 0) {
            this.opClass = OpClass.INT_OP;
        } else if ((flags & OpFlags.f_is_float_op) > 0) {
            this.opClass = OpClass.FLOAT_OP;
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

    /**
     * The last instruction in the BB which will exit the BB.  Note:  This also
     * means any instructions past this point in that BB are unreachable.
     */
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

    public static Operation fromOrdinal(int value) {
        return value < 0 || value >= values().length ? null : values()[value];
    }
}
