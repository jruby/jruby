/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.util.List;
import org.jcodings.Encoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Operand;

/**
 *
 * @author enebo
 */
public class InstrEncoderMap {
    private final IRWriterEncoder e;

    public InstrEncoderMap(IRWriterEncoder encoder) {
        this.e = encoder;
    }

    public void encode(Instr instr) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + instr.getOperation() + "): " + instr);
        e.encode(instr.getOperation());
        if (instr instanceof ResultInstr) {
            e.encode(((ResultInstr) instr).getResult());
        }

        switch(instr.getOperation()) {
            case ALIAS: encodeAliasInstr((AliasInstr) instr); break;
            case ARG_SCOPE_DEPTH: /* no state */ break;
            case ATTR_ASSIGN: encodeAttrAssignInstr((AttrAssignInstr) instr); break;
            case BEQ: encodeBEQInstr((BEQInstr) instr); break;
            case BINDING_LOAD: encodeLoadLocalVarInstr((LoadLocalVarInstr) instr); break;
            case BINDING_STORE:encodeStoreLocalVarInstr((StoreLocalVarInstr) instr); break;
            case BLOCK_GIVEN: encodeBlockGivenInstr((BlockGivenInstr) instr); break;
            case BNE: encodeBNEInstr((BNEInstr) instr); break;
            case BREAK: encodeBreakInstr((BreakInstr) instr); break;
            case B_FALSE: encodeBFalseInstr((BFalseInstr) instr); break;
            case B_NIL: encodeBNilInstr((BNilInstr) instr); break;
            case B_TRUE: encodeBTrueInstr((BTrueInstr) instr); break;
            case B_UNDEF: encodeBUndefInstr((BUndefInstr) instr); break;
            case CALL: case CALL_1F: case CALL_1D: case CALL_1O: case CALL_1OB: case CALL_0O: case NORESULT_CALL_1O:
                encodeCallBaseInstr((CallInstr) instr); break;
            case CHECK_ARGS_ARRAY_ARITY: encodeCheckArgsArrayArityInstr((CheckArgsArrayArityInstr) instr); break;
            case CHECK_ARITY: encodeCheckArityInstr((CheckArityInstr) instr); break;
            case CLASS_VAR_MODULE: encodeGetClassVarContainerModuleInstr((GetClassVarContainerModuleInstr) instr); break;
            case BACKTICK_STRING: encodeBacktickInstr((BacktickInstr) instr); break;
            case BUILD_COMPOUND_ARRAY: encodeBuildCompoundArrayInstr((BuildCompoundArrayInstr) instr); break;
            case BUILD_COMPOUND_STRING: encodeBuildCompoundStringInstr((BuildCompoundStringInstr) instr); break;
            case BUILD_DREGEXP: encodeBuildDynRegExpInstr((BuildDynRegExpInstr) instr); break;
            case BUILD_RANGE: encodeBuildRangeInstr((BuildRangeInstr) instr); break;
            case BUILD_SPLAT: encodeBuildSplatInstr((BuildSplatInstr) instr); break;
            case CHECK_FOR_LJE: encodeCheckForLJEInstr((CheckForLJEInstr) instr); break;
            case CONST_MISSING: encodeConstMissingInstr((ConstMissingInstr) instr); break;
            case COPY: encodeCopyInstr((CopyInstr) instr); break;
            case DEF_CLASS: encodeDefineClassInstr((DefineClassInstr) instr); break;
            case DEF_CLASS_METH: encodeDefineClassMethodInstr((DefineClassMethodInstr) instr); break;
            case DEF_INST_METH: encodeDefineInstanceMethodInstr((DefineInstanceMethodInstr) instr); break;
            case DEF_META_CLASS: encodeDefineMetaClassInstr((DefineMetaClassInstr) instr); break;
            case DEF_MODULE: encodeDefineModuleInstr((DefineModuleInstr) instr); break;
            case EQQ: encodeEQQInstr((EQQInstr) instr); break;
            case EXC_REGION_END: /* no state */ break;
            case EXC_REGION_START: encodeExceptionRegionStartMarkerInstr((ExceptionRegionStartMarkerInstr) instr); break;
            case GET_CVAR: encodeGetClassVariableInstr((GetClassVariableInstr) instr); break;
            case GET_ENCODING: encodeGetEncodingInstr((GetEncodingInstr) instr); break;
            case GET_ERROR_INFO: /* no state */ break;
            case GET_FIELD: encodeGetFieldInstr((GetFieldInstr) instr); break;
            case GET_GLOBAL_VAR: encodeGetGlobalVariableInstr((GetGlobalVariableInstr) instr); break;
            case GVAR_ALIAS: encodeGVarAliasInstr((GVarAliasInstr) instr); break;
            case INHERITANCE_SEARCH_CONST: encodeInheritanceSearchConstInstr((InheritanceSearchConstInstr) instr); break;
            case JUMP: encodeJumpInstr((JumpInstr) instr); break;
            case LABEL: encodeLabelInstr((LabelInstr) instr); break;
            case LAMBDA: encodeBuildLambdaInstr((BuildLambdaInstr) instr); break;
            case LEXICAL_SEARCH_CONST: encodeLexicalSearchConstInstr((LexicalSearchConstInstr) instr); break;
            case LOAD_FRAME_CLOSURE: /* no state */ break;
            case LOAD_IMPLICIT_CLOSURE: /* no state */ break;
            case LINE_NUM: encodeLineNumberInstr((LineNumberInstr) instr); break;
            case MASGN_OPT: encodeOptArgMultipleAsgnInstr((OptArgMultipleAsgnInstr) instr); break;
            case MASGN_REQD: encodeReqdArgMultipleAsgnInstr((ReqdArgMultipleAsgnInstr) instr); break;
            case MASGN_REST: encodeRestArgMultipleAsgnInstr((RestArgMultipleAsgnInstr) instr); break;
            case MATCH: encodeMatchInstr((MatchInstr) instr); break;
            case MATCH2: encodeMatch2Instr((Match2Instr) instr); break;
            case MATCH3: encodeMatch3Instr((Match3Instr) instr); break;
            case NONLOCAL_RETURN: encodeNonlocalReturnInstr((NonlocalReturnInstr) instr); break;
            case NOP: /* no state */ break;
            case NORESULT_CALL: encodeCallBaseInstr((NoResultCallInstr) instr); break;
            case POP_BINDING: /* no state */ break;
            case POP_FRAME: /* no state */ break;
            case PROCESS_MODULE_BODY: encodeProcessModuleBodyInstr((ProcessModuleBodyInstr) instr); break;
            case PUSH_BINDING: /* no state */ break;
            case PUSH_FRAME: /* no state */ break;
            case PUT_CONST: encodePutConstInstr((PutConstInstr) instr); break;
            case PUT_CVAR: encodePutClassVariableInstr((PutClassVariableInstr) instr); break;
            case PUT_FIELD: encodePutFieldInstr((PutFieldInstr) instr); break;
            case PUT_GLOBAL_VAR: encodePutGlobalVarInstr((PutGlobalVarInstr) instr); break;
            case RAISE_ARGUMENT_ERROR: encodeRaiseArgumentErrorInstr((RaiseArgumentErrorInstr) instr); break;
            case RECORD_END_BLOCK: encodeRecordEndBlockInstr((RecordEndBlockInstr) instr); break;
            case REIFY_CLOSURE: encodeReifyClosureInstr((ReifyClosureInstr) instr); break;
            case RECV_RUBY_EXC: encodeReceiveRubyExceptionInstr((ReceiveRubyExceptionInstr) instr); break;
            case RECV_JRUBY_EXC: encodeReceiveJRubyExceptionInstr((ReceiveJRubyExceptionInstr) instr); break;
            case RECV_KW_ARG: encodeReceiveKeywordArgInstr((ReceiveKeywordArgInstr) instr); break;
            case RECV_KW_REST_ARG: encodeReceiveKeywordRestArgInstr((ReceiveKeywordRestArgInstr) instr); break;
            case RECV_OPT_ARG: encodeReceiveOptArgInstr((ReceiveOptArgInstr) instr); break;
            case RECV_POST_REQD_ARG: encodeReceivePostReqdArgInstr((ReceivePostReqdArgInstr) instr); break;
            case RECV_PRE_REQD_ARG: encodeReceivePreReqdArgInstr((ReceivePreReqdArgInstr) instr); break;
            case RECV_REST_ARG: encodeReceiveRestArgInstr((ReceiveRestArgInstr) instr); break;
            case RECV_SELF: /* no state */ break;
            case RESCUE_EQQ: encodeRescueEQQInstr((RescueEQQInstr) instr); break;
            case RESTORE_ERROR_INFO: encodeRestoreErrorInfoInstr((RestoreErrorInfoInstr) instr); break;
            case RETURN: encodeReturnInstr((ReturnInstr) instr); break;
            case RUNTIME_HELPER: encodeRuntimeHelperCall((RuntimeHelperCall) instr); break;
            case SEARCH_CONST: encodeSearchConstInstr((SearchConstInstr) instr); break;
            case SET_CAPTURED_VAR: encodeSetCapturedVarInstr((SetCapturedVarInstr) instr); break;
            case CLASS_SUPER: encodeClassSuperInstr((ClassSuperInstr) instr); break;
            case INSTANCE_SUPER: encodeInstanceSuperInstr((InstanceSuperInstr) instr); break;
            case UNRESOLVED_SUPER: encodeUnresolvedSuperInstr((UnresolvedSuperInstr) instr); break;
            case THREAD_POLL: encodeThreadPollInstr((ThreadPollInstr) instr); break;
            case THROW: encodeThrowExceptionInstr((ThrowExceptionInstr) instr); break;
            case TO_ARY: encodeToAryInstr((ToAryInstr) instr); break;
            case UNDEF_METHOD: encodeUndefMethodInstr((UndefMethodInstr) instr); break;
            case YIELD: encodeYieldInstr((YieldInstr) instr); break;
            case ZSUPER: encodeZSuperInstr((ZSuperInstr) instr); break;
            default: throw new IllegalArgumentException("Whoa what am I encoding: " + instr);
        }
    }

    private void encodeAliasInstr(AliasInstr instr) {
        e.encode(instr.getNewName());
        e.encode(instr.getOldName());
    }

    private void encodeAttrAssignInstr(AttrAssignInstr instr) {
        e.encode(instr.getReceiver());
        e.encode(instr.getName());
        Operand[] args = instr.getCallArgs();

        e.encode(args.length);
        for(Operand arg: args) {
            e.encode(arg);
        }
    }

    private void encodeBacktickInstr(BacktickInstr instr) {
        Operand[] pieces = instr.getPieces();

        e.encode(pieces.length);
        for (Operand piece: pieces) {
            e.encode(piece);
        }
    }

    private void encodeBEQInstr(BEQInstr instr) {
        encodeTwoOperandBranchInstr(instr);
    }

    private void encodeLoadLocalVarInstr(LoadLocalVarInstr instr) {
        e.encode(instr.getScope());
        e.encode(instr.getLocalVar());
    }

    private void encodeStoreLocalVarInstr(StoreLocalVarInstr instr) {
        e.encode(instr.getScope());
        e.encode(instr.getLocalVar());
        e.encode(instr.getValue());
    }

    private void encodeBlockGivenInstr(BlockGivenInstr instr) {
        e.encode(instr.getBlockArg());
    }

    private void encodeBNEInstr(BNEInstr instr) {
        encodeTwoOperandBranchInstr(instr);
    }

    private void encodeBreakInstr(BreakInstr instr) {
        e.encode(instr.getReturnValue());
        e.encode(instr.getScopeName());
    }

    private void encodeBFalseInstr(BFalseInstr instr) {
        encodeOneOperandBranchInstr(instr);
    }

    private void encodeBNilInstr(BNilInstr instr) {
        encodeOneOperandBranchInstr(instr);
    }

    private void encodeBTrueInstr(BTrueInstr instr) {
        encodeOneOperandBranchInstr(instr);
    }

    private void encodeBUndefInstr(BUndefInstr instr) {
        encodeOneOperandBranchInstr(instr);
    }

    private void encodeCheckArgsArrayArityInstr(CheckArgsArrayArityInstr instr) {
        e.encode(instr.getArgsArray());
        e.encode(instr.required);
        e.encode(instr.opt);
        e.encode(instr.rest);
    }

    private void encodeCheckArityInstr(CheckArityInstr instr) {
        e.encode(instr.required);
        e.encode(instr.opt);
        e.encode(instr.rest);
        e.encode(instr.receivesKeywords);
    }

    private void encodeCheckForLJEInstr(CheckForLJEInstr instr) {
        e.encode(instr.maybeLambda());
    }

    private void encodeGetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr instr) {
        e.encode(instr.getStartingScope());
        e.encode(instr.getObject());
    }

    private void encodeBuildCompoundArrayInstr(BuildCompoundArrayInstr instr) {
        e.encode(instr.getAppendingArg());
        e.encode(instr.getAppendedArg());
        e.encode(instr.isArgsPush());
    }

    private void encodeBuildCompoundStringInstr(BuildCompoundStringInstr instr) {
        Encoding encoding = instr.getEncoding();

        e.encode(encoding == null ? "" : encoding.toString());
        Operand[] pieces = instr.getPieces();
        e.encode(pieces.length);

        for (Operand piece: pieces) {
            e.encode(piece);
        }
    }

    private void encodeConstMissingInstr(ConstMissingInstr instr) {
        e.encode(instr.getReceiver());
        e.encode(instr.getMissingConst());
    }

    private void encodeCopyInstr(CopyInstr instr) {
        e.encode(instr.getSource());
    }

    private void encodeDefineClassInstr(DefineClassInstr instr) {
        e.encode(instr.getNewIRClassBody());
        e.encode(instr.getContainer());
        e.encode(instr.getSuperClass());
    }

    private void encodeDefineClassMethodInstr(DefineClassMethodInstr instr) {
        e.encode(instr.getContainer());
        e.encode(instr.getMethod());
    }

    private void encodeDefineInstanceMethodInstr(DefineInstanceMethodInstr instr) {
        e.encode(instr.getMethod());
    }

    private void encodeDefineMetaClassInstr(DefineMetaClassInstr instr) {
        e.encode(instr.getObject());
        e.encode(instr.getMetaClassBody());
    }

    private void encodeDefineModuleInstr(DefineModuleInstr instr) {
        e.encode(instr.getNewIRModuleBody());
        e.encode(instr.getContainer());
    }

    private void encodeEQQInstr(EQQInstr instr) {
        e.encode(instr.getArg1());
        e.encode(instr.getArg2());
    }

    private void encodeExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr instr) {
        e.encode(instr.getFirstRescueBlockLabel());
    }

    private void encodeGetClassVariableInstr(GetClassVariableInstr instr) {
        encodeGetInstr(instr);
    }

    // FIXME: We know this is giving us a difficult to lookup name on decode side.
    private void encodeGetEncodingInstr(GetEncodingInstr instr) {
        e.encode(instr.getEncoding().toString());
    }

    private void encodeGetFieldInstr(GetFieldInstr instr) {
        encodeGetInstr(instr);
    }

    private void encodeGetGlobalVariableInstr(GetGlobalVariableInstr instr) {
        e.encode(((GlobalVariable) instr.getSource()).getName());
    }

    private void encodeGVarAliasInstr(GVarAliasInstr instr) {
        e.encode(instr.getNewName());
        e.encode(instr.getOldName());
    }

    private void encodeInheritanceSearchConstInstr(InheritanceSearchConstInstr instr) {
        e.encode(instr.getCurrentModule());
        e.encode(instr.getConstName());
        e.encode(instr.isNoPrivateConsts());
    }

    private void encodeJumpInstr(JumpInstr instr) {
        e.encode(instr.getJumpTarget());
    }

    private void encodeLabelInstr(LabelInstr instr) {
        e.encode(instr.getLabel());
    }

    // FIXME: If these always occur in the same source file thet live in we do not need to encode filename
    private void encodeBuildLambdaInstr(BuildLambdaInstr instr) {
        e.encode(instr.getLambdaBodyName());
        e.encode(instr.getPosition().getFile());
        e.encode(instr.getPosition().getLine());
    }

    private void encodeLexicalSearchConstInstr(LexicalSearchConstInstr instr) {
        e.encode(instr.getDefiningScope());
        e.encode(instr.getConstName());
    }

    private void encodeLineNumberInstr(LineNumberInstr instr) {
        e.encode(instr.getLineNumber());
    }

    private void encodeOptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr instr) {
        e.encode(instr.getArray());
        e.encode(instr.getIndex());
        e.encode(instr.getMinArgsLength());
    }

    private void encodeReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr instr) {
        e.encode(instr.getArray());
        e.encode(instr.getPreArgsCount());
        e.encode(instr.getPostArgsCount());
        e.encode(instr.getIndex());
    }

    private void encodeRestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr instr) {
        e.encode(instr.getArray());
        e.encode(instr.getPreArgsCount());
        e.encode(instr.getPostArgsCount());
        e.encode(instr.getIndex());
    }

    private void encodeMatchInstr(MatchInstr instr) {
        e.encode(instr.getReceiver());
    }

    private void encodeMatch2Instr(Match2Instr instr) {
        e.encode(instr.getReceiver());
        e.encode(instr.getArg());
    }

    private void encodeMatch3Instr(Match3Instr instr) {
        e.encode(instr.getReceiver());
        e.encode(instr.getArg());
    }

    private void encodeNonlocalReturnInstr(NonlocalReturnInstr instr) {
        e.encode(instr.getReturnValue());
        e.encode(instr.methodName);
    }

    private void encodeCallBaseInstr(CallBase instr) {
        boolean hasClosure = instr.getClosureArg(null) != null;

        e.encode(instr.getCallType().ordinal());
        e.encode(instr.getName());
        e.encode(instr.getReceiver());
        e.encode(calculateArity(instr.getCallArgs(), hasClosure));

        for (Operand arg: instr.getCallArgs()) {
            e.encode(arg);
        }

        if (hasClosure) e.encode(instr.getClosureArg(null));
    }

    private void encodeProcessModuleBodyInstr(ProcessModuleBodyInstr instr) {
        e.encode(instr.getModuleBody());
        e.encode(instr.getBlock());
    }

    private void encodePutConstInstr(PutConstInstr instr) {
        encodePutInstr(instr);
    }

    private void encodePutClassVariableInstr(PutClassVariableInstr instr) {
        encodePutInstr(instr);
    }

    private void encodePutFieldInstr(PutFieldInstr instr) {
        encodePutInstr(instr);
    }

    private void encodePutGlobalVarInstr(PutGlobalVarInstr instr) {
        e.encode(((GlobalVariable) instr.getTarget()).getName());
        e.encode(instr.getValue());
    }

    private void encodeBuildDynRegExpInstr(BuildDynRegExpInstr instr) {
        Operand[] pieces = instr.getPieces();
        e.encode(pieces.length);

        for (Operand piece: pieces) {
            e.encode(piece);
        }
        e.encode(instr.getOptions().toEmbeddedOptions());
    }

    private void encodeBuildRangeInstr(BuildRangeInstr instr) {
        e.encode(instr.getBegin());
        e.encode(instr.getEnd());
        e.encode(instr.isExclusive());
    }

    private void encodeBuildSplatInstr(BuildSplatInstr instr) {
        e.encode(instr.getArray());
    }

    private void encodeRaiseArgumentErrorInstr(RaiseArgumentErrorInstr instr) {
        e.encode(instr.getRequired());
        e.encode(instr.getOpt());
        e.encode(instr.getRest());
        e.encode(instr.getNumArgs());
    }

    private void encodeRecordEndBlockInstr(RecordEndBlockInstr instr) {
        e.encode(instr.getDeclaringScope());
        e.encode(instr.getEndBlockClosure());
    }

    private void encodeReifyClosureInstr(ReifyClosureInstr instr) {
        e.encode(instr.getSource());
        e.encode(instr.getResult());
    }

    private void encodeReceiveRubyExceptionInstr(ReceiveRubyExceptionInstr instr) { }

    private void encodeReceiveJRubyExceptionInstr(ReceiveJRubyExceptionInstr instr) { }

    private void encodeReceiveKeywordArgInstr(ReceiveKeywordArgInstr instr) {
        e.encode(instr.argName);
        e.encode(instr.required);
    }

    private void encodeReceiveKeywordRestArgInstr(ReceiveKeywordRestArgInstr instr) {
        e.encode(instr.required);
    }

    private void encodeReceiveOptArgInstr(ReceiveOptArgInstr instr) {
        e.encode(instr.requiredArgs);
        e.encode(instr.getPreArgs());
        e.encode(instr.getArgIndex());
    }

    private void encodeReceivePostReqdArgInstr(ReceivePostReqdArgInstr instr) {
        e.encode(instr.getArgIndex());
        e.encode(instr.preReqdArgsCount);
        e.encode(instr.postReqdArgsCount);
    }

    private void encodeReceivePreReqdArgInstr(ReceivePreReqdArgInstr instr) {
        e.encode(instr.getArgIndex());
    }

    private void encodeReceiveRestArgInstr(ReceiveRestArgInstr instr) {
        e.encode(instr.required);
        e.encode(instr.getArgIndex());
    }

    private void encodeRescueEQQInstr(RescueEQQInstr instr) {
        e.encode(instr.getArg1());
        e.encode(instr.getArg2());
    }

    private void encodeRestoreErrorInfoInstr(RestoreErrorInfoInstr instr) {
        e.encode(instr.getArg());
    }

    private void encodeReturnInstr(ReturnInstr instr) {
        e.encode(instr.getReturnValue());
    }

    private void encodeSearchConstInstr(SearchConstInstr instr) {
        e.encode(instr.getConstName());
        e.encode(instr.getStartingScope());
        e.encode(instr.isNoPrivateConsts());
    }

    private void encodeSetCapturedVarInstr(SetCapturedVarInstr instr) {
        e.encode(instr.getMatch2Result());
        e.encode(instr.getVarName());
    }

    private void encodeClassSuperInstr(ClassSuperInstr instr) {
        encodeCallBaseInstr(instr);
    }

    private void encodeInstanceSuperInstr(InstanceSuperInstr instr) {
        encodeCallBaseInstr(instr);
    }

    private void encodeUnresolvedSuperInstr(UnresolvedSuperInstr instr) {
        boolean hasClosure = instr.getClosureArg(null) != null;

        e.encode(instr.getCallType().ordinal());
        e.encode(instr.getReceiver());
        e.encode(calculateArity(instr.getCallArgs(), hasClosure));

        for (Operand arg: instr.getCallArgs()) {
            e.encode(arg);
        }

        if (hasClosure) e.encode(instr.getClosureArg(null));
    }

    private void encodeThreadPollInstr(ThreadPollInstr instr) {
        e.encode(instr.onBackEdge);
    }

    private void encodeThrowExceptionInstr(ThrowExceptionInstr instr) {
        e.encode(instr.getException());
    }

    private void encodeToAryInstr(ToAryInstr instr) {
        e.encode(instr.getArray());
    }

    private void encodeUndefMethodInstr(UndefMethodInstr instr) {
        e.encode(instr.getMethodName());
    }

    private void encodeYieldInstr(YieldInstr instr) {
        e.encode(instr.getBlockArg());
        e.encode(instr.getYieldArg());
        e.encode(instr.isUnwrapArray());
    }

    private void encodeZSuperInstr(ZSuperInstr instr) {
        e.encode(instr.getReceiver());
        Operand closure = instr.getClosureArg(null);

        boolean hasClosure = closure != null;
        e.encode(hasClosure);
        if (hasClosure) {
            e.encode(closure);
        }

        e.encode(instr.getCallArgs().length);
        for (Operand arg: instr.getCallArgs()) {
            e.encode(arg);
        }
    }

    private void encodeTwoOperandBranchInstr(TwoOperandBranchInstr instr) {
        e.encode(instr.getArg1());
        e.encode(instr.getArg2());
        e.encode(instr.getJumpTarget());
    }

    private void encodeOneOperandBranchInstr(OneOperandBranchInstr instr) {
        e.encode(instr.getArg1());
        e.encode(instr.getJumpTarget());
    }

    private void encodeGetInstr(GetInstr instr) {
        e.encode(instr.getSource());
        e.encode(instr.getRef());
    }

    private void encodePutInstr(PutInstr instr) {
        e.encode(instr.getTarget());
        e.encode(instr.getRef());
        e.encode(instr.getValue());
    }

    // -0 is not possible so we add 1 to arguments with closure so we get a valid negative value.
    private int calculateArity(Operand[] arguments, boolean hasClosure) {
        return hasClosure ? -1*(arguments.length + 1) : arguments.length;
    }

    private void encodeRuntimeHelperCall(RuntimeHelperCall instr) {
        e.encode(instr.getHelperMethod().ordinal());
        //FIXME: Probably make an Operand[] encoder
        Operand[] args = instr.getArgs();
        e.encode(args.length);
        for (int i = 0; i < args.length; i++) {
            e.encode(args[i]);
        }
    }
}
