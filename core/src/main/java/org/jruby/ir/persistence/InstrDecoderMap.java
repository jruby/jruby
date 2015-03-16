package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.*;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.operands.*;
import org.jruby.runtime.CallType;

/**
 *
 * @author enebo
 */
class InstrDecoderMap implements IRPersistenceValues {
    private final IRReaderDecoder d;

    public InstrDecoderMap(IRReaderDecoder decoder) {
        this.d = decoder;
    }

    public Instr decode(Operation operation) {
        Instr instr = null;
        try {
            instr = decodeInner(operation);
        } catch (Exception e) {
            System.out.println("Boom:" + d.getCurrentScope().getClass().getName());
            e.printStackTrace();
        }
        return instr;
    }

    public Instr decodeInner(Operation operation) {
        switch(operation) {
            case ALIAS: return AliasInstr.decode(d);
            case ARG_SCOPE_DEPTH: return ArgScopeDepthInstr.decode(d);
            case ATTR_ASSIGN: return AttrAssignInstr.decode(d);
            case B_FALSE: return BFalseInstr.decode(d);
            case B_NIL: return BNilInstr.decode(d);
            case B_TRUE: return BTrueInstr.decode(d);
            case B_UNDEF: return BUndefInstr.decode(d);
            case BACKTICK_STRING: return BacktickInstr.decode(d);
            case BEQ: return BEQInstr.decode(d);
            case BINDING_LOAD: return LoadLocalVarInstr.decode(d);
            case BINDING_STORE:return StoreLocalVarInstr.decode(d);
            case BLOCK_GIVEN: return BlockGivenInstr.decode(d);
            case BNE: return BNEInstr.decode(d);
            case BREAK: return BreakInstr.decode(d);
            case BUILD_COMPOUND_ARRAY: return BuildCompoundArrayInstr.decode(d);
            case BUILD_COMPOUND_STRING: return BuildCompoundStringInstr.decode(d);
            case BUILD_DREGEXP: return BuildDynRegExpInstr.decode(d);
            case BUILD_RANGE: return BuildRangeInstr.decode(d);
            case BUILD_SPLAT: return BuildSplatInstr.decode(d);
            case CALL_1F: case CALL_1D: case CALL_1O: case CALL_1OB: case CALL_0O: case CALL: return decodeCall();
            case CHECK_ARGS_ARRAY_ARITY: return CheckArgsArrayArityInstr.decode(d);
            case CHECK_ARITY: return CheckArityInstr.decode(d);
            case CHECK_FOR_LJE: return CheckForLJEInstr.decode(d);
            case CLASS_SUPER: return decodeSuperInstr(operation);
            case CLASS_VAR_MODULE: return GetClassVarContainerModuleInstr.decode(d);
            case CONST_MISSING: return ConstMissingInstr.decode(d);
            case COPY: return CopyInstr.decode(d);
            case DEF_CLASS: return new DefineClassInstr((d.decodeVariable()), (IRClassBody) d.decodeScope(), d.decodeOperand(), d.decodeOperand());
            case DEF_CLASS_METH: return new DefineClassMethodInstr(d.decodeOperand(), (IRMethod) d.decodeScope());
            case DEF_INST_METH: return new DefineInstanceMethodInstr((IRMethod) d.decodeScope());
            case DEF_META_CLASS: return new DefineMetaClassInstr(d.decodeVariable(), d.decodeOperand(), (IRModuleBody) d.decodeScope());
            case DEF_MODULE: return new DefineModuleInstr(d.decodeVariable(), (IRModuleBody) d.decodeScope(), d.decodeOperand());
            case EQQ: return new EQQInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
            case EXC_REGION_END: return new ExceptionRegionEndMarkerInstr();
            case EXC_REGION_START: return new ExceptionRegionStartMarkerInstr((Label) d.decodeOperand());
            case GET_CVAR: return new GetClassVariableInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
            case GET_ENCODING: return GetEncodingInstr.decode(d);
            case GET_ERROR_INFO: return GetErrorInfoInstr.decode(d);
            case GET_FIELD: return GetFieldInstr.decode(d);
            case GET_GLOBAL_VAR: return GetGlobalVariableInstr.decode(d);
            case GVAR_ALIAS: return GVarAliasInstr.decode(d);
            case INHERITANCE_SEARCH_CONST: return InheritanceSearchConstInstr.decode(d);
            case INSTANCE_SUPER: return decodeSuperInstr(operation);
            case JUMP: return JumpInstr.decode(d);
            case LABEL: return LabelInstr.decode(d);
            case LAMBDA: return BuildLambdaInstr.decode(d);
            case LEXICAL_SEARCH_CONST: return LexicalSearchConstInstr.decode(d);
            case LOAD_FRAME_CLOSURE: return LoadFrameClosureInstr.decode(d);
            case LOAD_IMPLICIT_CLOSURE: return LoadImplicitClosureInstr.decode(d);
            case LINE_NUM: return LineNumberInstr.decode(d);
            case MASGN_OPT: return OptArgMultipleAsgnInstr.decode(d);
            case MASGN_REQD: return ReqdArgMultipleAsgnInstr.decode(d);
            case MASGN_REST: return RestArgMultipleAsgnInstr.decode(d);
            case MATCH: return MatchInstr.decode(d);
            case MATCH2: return Match2Instr.decode(d);
            case MATCH3: return Match3Instr.decode(d);
            case NONLOCAL_RETURN: return NonlocalReturnInstr.decode(d);
            case NOP: return NopInstr.NOP;
            case NORESULT_CALL: case NORESULT_CALL_1O: return decodeNoResultCall();
            case POP_BINDING: return PopBindingInstr.decode(d);
            case POP_FRAME: return PopFrameInstr.decode(d);
            case PROCESS_MODULE_BODY: return ProcessModuleBodyInstr.decode(d);
            case PUSH_BINDING: return PushBindingInstr.decode(d);
            case PUSH_FRAME: return PushFrameInstr.decode(d);
            case PUT_CONST: return PutConstInstr.decode(d);
            case PUT_CVAR: return PutClassVariableInstr.decode(d);
            case PUT_FIELD: return PutFieldInstr.decode(d);
            case PUT_GLOBAL_VAR: return PutGlobalVarInstr.decode(d);
            case RAISE_ARGUMENT_ERROR: return RaiseArgumentErrorInstr.decode(d);
            case RAISE_REQUIRED_KEYWORD_ARGUMENT_ERROR: return RaiseRequiredKeywordArgumentError.decode(d);
            case RECORD_END_BLOCK: return RecordEndBlockInstr.decode(d);
            case REIFY_CLOSURE: return ReifyClosureInstr.decode(d);
            case RECV_RUBY_EXC: return ReceiveRubyExceptionInstr.decode(d);
            case RECV_JRUBY_EXC: return ReceiveJRubyExceptionInstr.decode(d);
            case RECV_KW_ARG: return ReceiveKeywordArgInstr.decode(d);
            case RECV_KW_REST_ARG: return ReceiveKeywordRestArgInstr.decode(d);
            case RECV_OPT_ARG: return ReceiveOptArgInstr.decode(d);
            case RECV_POST_REQD_ARG: return ReceivePostReqdArgInstr.decode(d);
            case RECV_PRE_REQD_ARG: return ReceivePreReqdArgInstr.decode(d);
            case RECV_REST_ARG: return ReceiveRestArgInstr.decode(d);
            case RECV_SELF: return ReceiveSelfInstr.decode(d);
            case RESCUE_EQQ: return RescueEQQInstr.decode(d);
            case RESTORE_ERROR_INFO: return RestoreErrorInfoInstr.decode(d);
            case RETURN: return ReturnInstr.decode(d);
            case RUNTIME_HELPER: return RuntimeHelperCall.decode(d);
            case SEARCH_CONST: return SearchConstInstr.decode(d);
            case SET_CAPTURED_VAR: return SetCapturedVarInstr.decode(d);
            // FIXME: case TRACE: ...
            case THREAD_POLL: return ThreadPollInstr.decode(d);
            case THROW: return ThrowExceptionInstr.decode(d);
            case TO_ARY: return ToAryInstr.decode(d);
            case UNDEF_METHOD: return UndefMethodInstr.decode(d);
            case UNRESOLVED_SUPER: return decodeUnresolvedSuperInstr();
            case YIELD: return YieldInstr.decode(d);
            case ZSUPER: return ZSuperInstr.decode(d);
        }

        throw new IllegalArgumentException("Whoa bro: " + operation);
    }

    private Instr decodeCall() {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall");
        int callTypeOrdinal = d.decodeInt();
        CallType callType = CallType.fromOrdinal(callTypeOrdinal);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - calltype:  " + callType);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - methaddr:  " + methAddr);
        Operand receiver = d.decodeOperand();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - receiver:  " + receiver);
        int argsCount = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - # of args:  " + argsCount);
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - # of args(2): " + argsLength);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decodeCall - hasClosure: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("before result");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  "+ result);

        return CallInstr.create(d.getCurrentScope(), callType, result, methAddr, receiver, args, closure);
    }

    private Instr decodeNoResultCall() {
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, ordinal:  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  "+ methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return NoResultCallInstr.create(CallType.fromOrdinal(callTypeOrdinal), methAddr, receiver, args, closure);
    }

    private Instr decodeSuperInstr(Operation operation) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super");
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, calltype(ord):  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, methaddr:  "+ methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        Variable result = d.decodeVariable();

        if (operation == Operation.CLASS_SUPER) return new ClassSuperInstr(result, receiver, methAddr, args, closure);

        return new InstanceSuperInstr(result, receiver, methAddr, args, closure);
    }


    private Instr decodeUnresolvedSuperInstr() {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call");
        int callTypeOrdinal = d.decodeInt();
        CallType callType = CallType.fromOrdinal(callTypeOrdinal);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, calltype(ord):  " + callType);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  " + methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG)
            System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("before result");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  " + result);

        return new UnresolvedSuperInstr(result, receiver, args, closure);
    }
 }
