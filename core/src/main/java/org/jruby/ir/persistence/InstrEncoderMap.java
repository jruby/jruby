/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.ir.IRManager;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BNilInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.BuildLambdaInstr;
import org.jruby.ir.instructions.BUndefInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArgsArrayArityInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ClassSuperInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineClassInstr;
import org.jruby.ir.instructions.DefineClassMethodInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.DefineMetaClassInstr;
import org.jruby.ir.instructions.DefineModuleInstr;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.GVarAliasInstr;
import org.jruby.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.ir.instructions.GetClassVariableInstr;
import org.jruby.ir.instructions.GetEncodingInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.GetGlobalVariableInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.InstanceSuperInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpIndirectInstr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LexicalSearchConstInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.LoadLocalVarInstr;
import org.jruby.ir.instructions.Match2Instr;
import org.jruby.ir.instructions.Match3Instr;
import org.jruby.ir.instructions.MatchInstr;
import org.jruby.ir.instructions.MethodLookupInstr;
import org.jruby.ir.instructions.NoResultCallInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.NotInstr;
import org.jruby.ir.instructions.OptArgMultipleAsgnInstr;
import org.jruby.ir.instructions.PopBindingInstr;
import org.jruby.ir.instructions.PopFrameInstr;
import org.jruby.ir.instructions.ProcessModuleBodyInstr;
import org.jruby.ir.instructions.PushBindingInstr;
import org.jruby.ir.instructions.PushFrameInstr;
import org.jruby.ir.instructions.PutClassVariableInstr;
import org.jruby.ir.instructions.PutConstInstr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.RaiseArgumentErrorInstr;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveKeywordArgInstr;
import org.jruby.ir.instructions.ReceiveKeywordRestArgInstr;
import org.jruby.ir.instructions.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.RecordEndBlockInstr;
import org.jruby.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.ir.instructions.RescueEQQInstr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.SetReturnAddressInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.UndefMethodInstr;
import org.jruby.ir.instructions.UnresolvedSuperInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.ZSuperInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.instructions.defined.ClassVarIsDefinedInstr;
import org.jruby.ir.instructions.defined.GetBackrefInstr;
import org.jruby.ir.instructions.defined.GetDefinedConstantOrMethodInstr;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.GlobalIsDefinedInstr;
import org.jruby.ir.instructions.defined.HasInstanceVarInstr;
import org.jruby.ir.instructions.defined.IsMethodBoundInstr;
import org.jruby.ir.instructions.defined.MethodDefinedInstr;
import org.jruby.ir.instructions.defined.MethodIsPublicInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.instructions.defined.SuperMethodBoundInstr;

/**
 *
 * @author enebo
 */
public class InstrEncoderMap {
    private final IRWriterEncoder e;
    private final IRManager manager;

    public InstrEncoderMap(IRManager manager, IRWriterEncoder encoder) {
        this.manager = manager;
        this.e = encoder;
    }

    public void encode(Instr instr) { 
        switch(instr.getOperation()) {
            case ALIAS: encodeAliasInstr((AliasInstr) instr); break;
            case ATTR_ASSIGN: encodeAttrAssignInstr((AttrAssignInstr) instr); break;
            case BACKREF_IS_MATCH_DATA: encodeBackrefIsMatchDataInstr((BackrefIsMatchDataInstr) instr); break;
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
            case CALL: encodeCallInstr((CallInstr) instr); break;
            case CHECK_ARGS_ARRAY_ARITY: encodeCheckArgsArrayArityInstr((CheckArgsArrayArityInstr) instr); break;
            case CHECK_ARITY: encodeCheckArityInstr((CheckArityInstr) instr); break;
            case CLASS_VAR_IS_DEFINED: encodeClassVarIsDefinedInstr((ClassVarIsDefinedInstr) instr); break;
            case CLASS_VAR_MODULE: encodeGetClassVarContainerModuleInstr((GetClassVarContainerModuleInstr) instr); break;
            case CONST_MISSING: encodeConstMissingInstr((ConstMissingInstr) instr); break;
            case COPY: encodeCopyInstr((CopyInstr) instr); break;
            case DEFINED_CONSTANT_OR_METHOD: encodeGetDefinedConstantOrMethodInstr((GetDefinedConstantOrMethodInstr) instr); break;
            case DEF_CLASS: encodeDefineClassInstr((DefineClassInstr) instr); break;
            case DEF_CLASS_METH: encodeDefineClassMethodInstr((DefineClassMethodInstr) instr); break;
            case DEF_INST_METH: encodeDefineInstanceMethodInstr((DefineInstanceMethodInstr) instr); break;
            case DEF_META_CLASS: encodeDefineMetaClassInstr((DefineMetaClassInstr) instr); break;
            case DEF_MODULE: encodeDefineModuleInstr((DefineModuleInstr) instr); break;
            case EQQ: encodeEQQInstr((EQQInstr) instr); break;
            case EXC_REGION_END: encodeExceptionRegionEndMarkerInstr((ExceptionRegionEndMarkerInstr) instr); break;
            case EXC_REGION_START: encodeExceptionRegionStartMarkerInstr((ExceptionRegionStartMarkerInstr) instr); break;
            case GET_BACKREF: encodeGetBackrefInstr((GetBackrefInstr) instr); break;
            case GET_CVAR: encodeGetClassVariableInstr((GetClassVariableInstr) instr); break;
            case GET_ENCODING: encodeGetEncodingInstr((GetEncodingInstr) instr); break;
            case GET_ERROR_INFO: encodeGetErrorInfoInstr((GetErrorInfoInstr) instr); break;
            case GET_FIELD: encodeGetFieldInstr((GetFieldInstr) instr); break;
            case GET_GLOBAL_VAR: encodeGetGlobalVariableInstr((GetGlobalVariableInstr) instr); break;
            case GLOBAL_IS_DEFINED: encodeGlobalIsDefinedInstr((GlobalIsDefinedInstr) instr); break;
            case GVAR_ALIAS: encodeGVarAliasInstr((GVarAliasInstr) instr); break;
            case HAS_INSTANCE_VAR: encodeHasInstanceVarInstr((HasInstanceVarInstr) instr); break;
            case INHERITANCE_SEARCH_CONST: encodeInheritanceSearchConstInstr((InheritanceSearchConstInstr) instr); break;
            case IS_METHOD_BOUND: encodeIsMethodBoundInstr((IsMethodBoundInstr) instr); break;
            case JUMP: encodeJumpInstr((JumpInstr) instr); break;
            case JUMP_INDIRECT: encodeJumpIndirectInstr((JumpIndirectInstr) instr); break;
            case LABEL: encodeLabelInstr((LabelInstr) instr); break;
            case LAMBDA: encodeBuildLambdaInstr((BuildLambdaInstr) instr); break;
            case LEXICAL_SEARCH_CONST: encodeLexicalSearchConstInstr((LexicalSearchConstInstr) instr); break;
            case LINE_NUM: encodeLineNumberInstr((LineNumberInstr) instr); break;
            case MASGN_OPT: encodeOptArgMultipleAsgnInstr((OptArgMultipleAsgnInstr) instr); break;
            case MASGN_REQD: encodeReqdArgMultipleAsgnInstr((ReqdArgMultipleAsgnInstr) instr); break;
            case MASGN_REST: encodeRestArgMultipleAsgnInstr((RestArgMultipleAsgnInstr) instr); break;
            case MATCH: encodeMatchInstr((MatchInstr) instr); break;
            case MATCH2: encodeMatch2Instr((Match2Instr) instr); break;
            case MATCH3: encodeMatch3Instr((Match3Instr) instr); break;
            case METHOD_DEFINED: encodeMethodDefinedInstr((MethodDefinedInstr) instr); break;
            case METHOD_IS_PUBLIC: encodeMethodIsPublicInstr((MethodIsPublicInstr) instr); break;
            case METHOD_LOOKUP: encodeMethodLookupInstr((MethodLookupInstr) instr); break;
            case NONLOCAL_RETURN: encodeNonlocalReturnInstr((NonlocalReturnInstr) instr); break;
            case NOP: break;
            case NORESULT_CALL: encodeNoResultCallInstr((NoResultCallInstr) instr); break;
            case NOT: encodeNotInstr((NotInstr) instr); break;
            case POP_BINDING: encodePopBindingInstr((PopBindingInstr) instr); break;
            case POP_FRAME: encodePopFrameInstr((PopFrameInstr) instr); break;
            case PROCESS_MODULE_BODY: encodeProcessModuleBodyInstr((ProcessModuleBodyInstr) instr); break;
            case PUSH_BINDING: encodePushBindingInstr((PushBindingInstr) instr); break;
            case PUSH_FRAME: encodePushFrameInstr((PushFrameInstr) instr); break;
            case PUT_CONST: encodePutConstInstr((PutConstInstr) instr); break;
            case PUT_CVAR: encodePutClassVariableInstr((PutClassVariableInstr) instr); break;
            case PUT_FIELD: encodePutFieldInstr((PutFieldInstr) instr); break;
            case PUT_GLOBAL_VAR: encodePutGlobalVarInstr((PutGlobalVarInstr) instr); break;
            case RAISE_ARGUMENT_ERROR: encodeRaiseArgumentErrorInstr((RaiseArgumentErrorInstr) instr); break;
            case RECORD_END_BLOCK: encodeRecordEndBlockInstr((RecordEndBlockInstr) instr); break;
            case RECV_CLOSURE: encodeReceiveClosureInstr((ReceiveClosureInstr) instr); break;
            case RECV_KW_ARG: encodeReceiveKeywordArgInstr((ReceiveKeywordArgInstr) instr); break;
            case RECV_KW_REST_ARG: encodeReceiveKeywordRestArgInstr((ReceiveKeywordRestArgInstr) instr); break;
            case RECV_OPT_ARG: encodeReceivePostReqdArgInstr((ReceivePostReqdArgInstr) instr); break;
            case RECV_POST_REQD_ARG: encodeReceivePostReqdArgInstr((ReceivePostReqdArgInstr) instr); break;
            case RECV_PRE_REQD_ARG: encodeReceivePreReqdArgInstr((ReceivePreReqdArgInstr) instr); break;
            case RECV_REST_ARG: encodeReceiveRestArgInstr((ReceiveRestArgInstr) instr); break;
            case RECV_SELF: encodeReceiveSelfInstr((ReceiveSelfInstr) instr); break;
            case RESCUE_EQQ: encodeRescueEQQInstr((RescueEQQInstr) instr); break;
            case RESTORE_ERROR_INFO: encodeRestoreErrorInfoInstr((RestoreErrorInfoInstr) instr); break;
            case RETURN: encodeReturnInstr((ReturnInstr) instr); break;
            case SEARCH_CONST: encodeSearchConstInstr((SearchConstInstr) instr); break;
            case SET_RETADDR: encodeSetReturnAddressInstr((SetReturnAddressInstr) instr); break;
            case CLASS_SUPER: encodeClassSuperInstr((ClassSuperInstr) instr); break;
            case INSTANCE_SUPER: encodeInstanceSuperInstr((InstanceSuperInstr) instr); break;
            case UNRESOLVED_SUPER: encodeUnresolvedSuperInstr((UnresolvedSuperInstr) instr); break;
            case SUPER_METHOD_BOUND: encodeSuperMethodBoundInstr((SuperMethodBoundInstr) instr); break;
            case THREAD_POLL: encodeThreadPollInstr((ThreadPollInstr) instr); break;
            case THROW: encodeThrowExceptionInstr((ThrowExceptionInstr) instr); break;
            case TO_ARY: encodeToAryInstr((ToAryInstr) instr); break;
            case UNDEF_METHOD: encodeUndefMethodInstr((UndefMethodInstr) instr); break;
            case YIELD: encodeYieldInstr((YieldInstr) instr); break;
            case ZSUPER: encodeZSuperInstr((ZSuperInstr) instr); break;
        }        
    }   

    private void encodeAliasInstr(AliasInstr aliasInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeAttrAssignInstr(AttrAssignInstr attrAssignInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBackrefIsMatchDataInstr(BackrefIsMatchDataInstr backrefIsMatchDataInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBEQInstr(BEQInstr beqInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeLoadLocalVarInstr(LoadLocalVarInstr loadLocalVarInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeStoreLocalVarInstr(StoreLocalVarInstr storeLocalVarInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBlockGivenInstr(BlockGivenInstr blockGivenInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBNEInstr(BNEInstr bneInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBreakInstr(BreakInstr breakInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBFalseInstr(BFalseInstr bFalseInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBNilInstr(BNilInstr bNilInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBTrueInstr(BTrueInstr bTrueInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBUndefInstr(BUndefInstr bUndefInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeCallInstr(CallInstr callInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeCheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkArgsArrayArityInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeCheckArityInstr(CheckArityInstr checkArityInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeClassVarIsDefinedInstr(ClassVarIsDefinedInstr classVarIsDefinedInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr getClassVarContainerModuleInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeConstMissingInstr(ConstMissingInstr constMissingInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeCopyInstr(CopyInstr copyInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetDefinedConstantOrMethodInstr(GetDefinedConstantOrMethodInstr getDefinedConstantOrMethodInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeDefineClassInstr(DefineClassInstr defineClassInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeDefineClassMethodInstr(DefineClassMethodInstr defineClassMethodInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeDefineInstanceMethodInstr(DefineInstanceMethodInstr defineInstanceMethodInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeDefineMetaClassInstr(DefineMetaClassInstr defineMetaClassInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeDefineModuleInstr(DefineModuleInstr defineModuleInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeEQQInstr(EQQInstr eqqInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeExceptionRegionEndMarkerInstr(ExceptionRegionEndMarkerInstr exceptionRegionEndMarkerInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionRegionStartMarkerInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetBackrefInstr(GetBackrefInstr getBackrefInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetClassVariableInstr(GetClassVariableInstr getClassVariableInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetEncodingInstr(GetEncodingInstr getEncodingInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetErrorInfoInstr(GetErrorInfoInstr getErrorInfoInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetFieldInstr(GetFieldInstr getFieldInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGetGlobalVariableInstr(GetGlobalVariableInstr getGlobalVariableInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGlobalIsDefinedInstr(GlobalIsDefinedInstr globalIsDefinedInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeGVarAliasInstr(GVarAliasInstr gVarAliasInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeHasInstanceVarInstr(HasInstanceVarInstr hasInstanceVarInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeInheritanceSearchConstInstr(InheritanceSearchConstInstr inheritanceSearchConstInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeIsMethodBoundInstr(IsMethodBoundInstr isMethodBoundInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeJumpInstr(JumpInstr jumpInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeJumpIndirectInstr(JumpIndirectInstr jumpIndirectInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeLabelInstr(LabelInstr labelInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeBuildLambdaInstr(BuildLambdaInstr buildLambdaInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeLexicalSearchConstInstr(LexicalSearchConstInstr lexicalSearchConstInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeLineNumberInstr(LineNumberInstr lineNumberInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeOptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optArgMultipleAsgnInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdArgMultipleAsgnInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeRestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr restArgMultipleAsgnInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeMatchInstr(MatchInstr matchInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeMatch2Instr(Match2Instr match2Instr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeMatch3Instr(Match3Instr match3Instr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeMethodDefinedInstr(MethodDefinedInstr methodDefinedInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeMethodIsPublicInstr(MethodIsPublicInstr methodIsPublicInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeMethodLookupInstr(MethodLookupInstr methodLookupInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeNonlocalReturnInstr(NonlocalReturnInstr nonlocalReturnInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeNoResultCallInstr(NoResultCallInstr noResultCallInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeNotInstr(NotInstr notInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePopBindingInstr(PopBindingInstr popBindingInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePopFrameInstr(PopFrameInstr popFrameInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeProcessModuleBodyInstr(ProcessModuleBodyInstr processModuleBodyInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePushBindingInstr(PushBindingInstr pushBindingInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePushFrameInstr(PushFrameInstr pushFrameInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePutConstInstr(PutConstInstr putConstInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePutClassVariableInstr(PutClassVariableInstr putClassVariableInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePutFieldInstr(PutFieldInstr putFieldInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodePutGlobalVarInstr(PutGlobalVarInstr putGlobalVarInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeRaiseArgumentErrorInstr(RaiseArgumentErrorInstr raiseArgumentErrorInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeRecordEndBlockInstr(RecordEndBlockInstr recordEndBlockInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceiveClosureInstr(ReceiveClosureInstr receiveClosureInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceiveKeywordArgInstr(ReceiveKeywordArgInstr receiveKeywordArgInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceiveKeywordRestArgInstr(ReceiveKeywordRestArgInstr receiveKeywordRestArgInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceivePostReqdArgInstr(ReceivePostReqdArgInstr receivePostReqdArgInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceivePreReqdArgInstr(ReceivePreReqdArgInstr receivePreReqdArgInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceiveRestArgInstr(ReceiveRestArgInstr receiveRestArgInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReceiveSelfInstr(ReceiveSelfInstr receiveSelfInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeRescueEQQInstr(RescueEQQInstr rescueEQQInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeRestoreErrorInfoInstr(RestoreErrorInfoInstr restoreErrorInfoInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeReturnInstr(ReturnInstr returnInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeSearchConstInstr(SearchConstInstr searchConstInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeSetReturnAddressInstr(SetReturnAddressInstr setReturnAddressInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeClassSuperInstr(ClassSuperInstr classSuperInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeInstanceSuperInstr(InstanceSuperInstr instanceSuperInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeUnresolvedSuperInstr(UnresolvedSuperInstr unresolvedSuperInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeSuperMethodBoundInstr(SuperMethodBoundInstr superMethodBoundInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeThreadPollInstr(ThreadPollInstr threadPollInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeThrowExceptionInstr(ThrowExceptionInstr throwExceptionInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeToAryInstr(ToAryInstr toAryInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeUndefMethodInstr(UndefMethodInstr undefMethodInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeYieldInstr(YieldInstr yieldInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void encodeZSuperInstr(ZSuperInstr zSuperInstr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}