package org.jruby.ir;

import org.jruby.compiler.NotCompilableException;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.boxing.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneFloatArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;

/**
 * Superclass for IR visitors.
 */
public abstract class IRVisitor {
    public void visit(Instr instr) {
        instr.visit(this);
    }

    public void visit(Operand operand) {
        operand.visit(this);
    }

    private void error(Object object) {
        throw new NotCompilableException("no visitor logic for " + object.getClass().getName() + " in " + getClass().getName());
    }

    // standard instructions
    public void AliasInstr(AliasInstr aliasinstr) { error(aliasinstr); }
    public void ArrayDerefInstr(ArrayDerefInstr arrayderefinstr) { error(arrayderefinstr); }
    public void AsStringInstr(AsStringInstr asstring) { error(asstring); }
    public void AttrAssignInstr(AttrAssignInstr attrassigninstr) { error(attrassigninstr); }
    public void BFalseInstr(BFalseInstr bfalseinstr) { error(bfalseinstr); }
    public void BlockGivenInstr(BlockGivenInstr blockgiveninstr) { error(blockgiveninstr); }
    public void BNEInstr(BNEInstr bneinstr) { error(bneinstr); }
    public void BNilInstr(BNilInstr bnilinstr) { error(bnilinstr); }
    public void BreakInstr(BreakInstr breakinstr) { error(breakinstr); }
    public void BSwitchInstr(BSwitchInstr bswitchinstr) { error(bswitchinstr); }
    public void BTrueInstr(BTrueInstr btrueinstr) { error(btrueinstr); }
    public void BUndefInstr(BUndefInstr bundefinstr) { error(bundefinstr); }
    public void BuildBackrefInstr(BuildBackrefInstr instr) { error(instr); }
    public void BuildCompoundArrayInstr(BuildCompoundArrayInstr instr) { error(instr); }
    public void BuildCompoundStringInstr(BuildCompoundStringInstr instr) { error(instr); }
    public void BuildDynRegExpInstr(BuildDynRegExpInstr instr) { error(instr); }
    public void BuildRangeInstr(BuildRangeInstr instr) { error(instr); }
    public void BuildSplatInstr(BuildSplatInstr instr) { error(instr); }
    public void CallInstr(CallInstr callinstr) { error(callinstr); }
    public void CheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkargsarrayarityinstr) { error(checkargsarrayarityinstr); }
    public void CheckArityInstr(CheckArityInstr checkarityinstr) { error(checkarityinstr); }
    public void CheckForLJEInstr(CheckForLJEInstr checkforljeinstr) { error(checkforljeinstr); }
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) { error(classsuperinstr); }
    public void CopyInstr(CopyInstr copyinstr) { error(copyinstr); }
    public void DefineClassInstr(DefineClassInstr defineclassinstr) { error(defineclassinstr); }
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) { error(defineclassmethodinstr); }
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) { error(defineinstancemethodinstr); }
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) { error(definemetaclassinstr); }
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) { error(definemoduleinstr); }
    public void EQQInstr(EQQInstr eqqinstr) { error(eqqinstr); }
    public void ExceptionRegionEndMarkerInstr(ExceptionRegionEndMarkerInstr exceptionregionendmarkerinstr) { error(exceptionregionendmarkerinstr); }
    public void ExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) { error(exceptionregionstartmarkerinstr); }
    public void GetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr getclassvarcontainermoduleinstr) { error(getclassvarcontainermoduleinstr); }
    public void GetClassVariableInstr(GetClassVariableInstr getclassvariableinstr) { error(getclassvariableinstr); }
    public void GetFieldInstr(GetFieldInstr getfieldinstr) { error(getfieldinstr); }
    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) { error(getglobalvariableinstr); }
    public void GVarAliasInstr(GVarAliasInstr gvaraliasinstr) { error(gvaraliasinstr); }
    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) { error(inheritancesearchconstinstr); }
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) { error(instancesuperinstr); }
    public void Instr(Instr instr) { error(instr); }
    public void JumpInstr(JumpInstr jumpinstr) { error(jumpinstr); }
    public void LabelInstr(LabelInstr labelinstr) { error(labelinstr); }
    public void LexicalSearchConstInstr(LexicalSearchConstInstr lexicalsearchconstinstr) { error(lexicalsearchconstinstr); }
    public void LineNumberInstr(LineNumberInstr linenumberinstr) { error(linenumberinstr); }
    public void LoadLocalVarInstr(LoadLocalVarInstr loadlocalvarinstr) { error(loadlocalvarinstr); }
    public void LoadImplicitClosure(LoadImplicitClosureInstr loadimplicitclosureinstr) { error(loadimplicitclosureinstr); }
    public void LoadFrameClosure(LoadFrameClosureInstr loadframeclosureinstr) { error(loadframeclosureinstr); }
    public void LoadBlockImplicitClosure(LoadBlockImplicitClosureInstr loadblockimplicitclosureinstr) { error(loadblockimplicitclosureinstr); }
    public void MatchInstr(MatchInstr matchInstr) { error(matchInstr); }
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) { error(moduleversionguardinstr); }
    public void NonlocalReturnInstr(NonlocalReturnInstr nonlocalreturninstr) { error(nonlocalreturninstr); }
    public void NopInstr(NopInstr nopinstr) { error(nopinstr); }
    public void NoResultCallInstr(NoResultCallInstr noresultcallinstr) { error(noresultcallinstr); }
    @Deprecated public void OneFixnumArgNoBlockCallInstr(OneFixnumArgNoBlockCallInstr oneFixnumArgNoBlockCallInstr) { error(oneFixnumArgNoBlockCallInstr); }
    @Deprecated public void OneFloatArgNoBlockCallInstr(OneFloatArgNoBlockCallInstr oneFloatArgNoBlockCallInstr) { error(oneFloatArgNoBlockCallInstr); }
    @Deprecated public void OneOperandArgNoBlockCallInstr(OneOperandArgNoBlockCallInstr oneOperandArgNoBlockCallInstr) { error(oneOperandArgNoBlockCallInstr); }
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) { error(optargmultipleasgninstr); }
    public void PopBindingInstr(PopBindingInstr popbindinginstr) { error(popbindinginstr); }
    public void PopBlockFrameInstr(PopBlockFrameInstr instr) { error(instr); }
    public void PopMethodFrameInstr(PopMethodFrameInstr instr) { error(instr); }
    public void PopBackrefFrameInstr(PopBackrefFrameInstr instr) { error(instr); }
    public void PrepareBlockArgsInstr(PrepareBlockArgsInstr instr) { error(instr); }
    public void PrepareFixedBlockArgsInstr(PrepareFixedBlockArgsInstr instr) { error(instr); }
    public void PrepareSingleBlockArgInstr(PrepareSingleBlockArgInstr instr) { error(instr); }
    public void PrepareNoBlockArgsInstr(PrepareNoBlockArgsInstr instr) { error(instr); }
    public void ProcessModuleBodyInstr(ProcessModuleBodyInstr processmodulebodyinstr) { error(processmodulebodyinstr); }
    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) { error(putclassvariableinstr); }
    public void PutConstInstr(PutConstInstr putconstinstr) { error(putconstinstr); }
    public void PutFieldInstr(PutFieldInstr putfieldinstr) { error(putfieldinstr); }
    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) { error(putglobalvarinstr); }
    public void PushBlockBindingInstr(PushBlockBindingInstr instr) { error(instr); }
    public void PushBlockFrameInstr(PushBlockFrameInstr instr) { error(instr); }
    public void PushMethodBindingInstr(PushMethodBindingInstr instr) { error(instr); }
    public void PushMethodFrameInstr(PushMethodFrameInstr instr) { error(instr); }
    public void PushBackrefFrameInstr(PushBackrefFrameInstr instr) { error(instr); }
    public void RaiseArgumentErrorInstr(RaiseArgumentErrorInstr raiseargumenterrorinstr) { error(raiseargumenterrorinstr); }
    public void RaiseRequiredKeywordArgumentErrorInstr(RaiseRequiredKeywordArgumentError instr) { error(instr); }
    public void ReifyClosureInstr(ReifyClosureInstr reifyclosureinstr) { error(reifyclosureinstr); }
    public void ReceiveRubyExceptionInstr(ReceiveRubyExceptionInstr receiveexceptioninstr) { error(receiveexceptioninstr); }
    public void ReceiveJRubyExceptionInstr(ReceiveJRubyExceptionInstr receiveexceptioninstr) { error(receiveexceptioninstr); }
    public void ReceiveKeywordArgInstr(ReceiveKeywordArgInstr receiveKeywordArgInstr) { error(receiveKeywordArgInstr); }
    public void ReceiveKeywordRestArgInstr(ReceiveKeywordRestArgInstr receiveKeywordRestArgInstr) { error(receiveKeywordRestArgInstr); }
    public void ReceiveOptArgInstr(ReceiveOptArgInstr receiveoptarginstr) { error(receiveoptarginstr); }
    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr receiveprereqdarginstr) { error(receiveprereqdarginstr); }
    public void ReceiveRestArgInstr(ReceiveRestArgInstr receiverestarginstr) { error(receiverestarginstr); }
    public void ReceiveSelfInstr(ReceiveSelfInstr receiveselfinstr) { error(receiveselfinstr); }
    public void RecordEndBlockInstr(RecordEndBlockInstr recordendblockinstr) { error(recordendblockinstr); }
    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) { error(reqdargmultipleasgninstr); }
    public void RescueEQQInstr(RescueEQQInstr rescueeqqinstr) { error(rescueeqqinstr); }
    public void RestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr restargmultipleasgninstr) { error(restargmultipleasgninstr); }
    public void RestoreBindingVisibilityInstr(RestoreBindingVisibilityInstr instr) { error(instr); }
    public void ReturnInstr(ReturnInstr returninstr) { error(returninstr); }
    public void ReturnOrRethrowSavedExcInstr(ReturnOrRethrowSavedExcInstr instr) { error(instr); }
    public void RuntimeHelperCall(RuntimeHelperCall runtimehelpercall) { error(runtimehelpercall); }
    public void SaveBindingVisibilityInstr(SaveBindingVisibilityInstr instr) { error(instr); }
    public void SearchConstInstr(SearchConstInstr searchconstinstr) { error(searchconstinstr); }
    public void SearchModuleForConstInstr(SearchModuleForConstInstr searchconstinstr) { error(searchconstinstr); }
    public void SetCapturedVarInstr(SetCapturedVarInstr instr) { error(instr); }
    public void StoreLocalVarInstr(StoreLocalVarInstr storelocalvarinstr) { error(storelocalvarinstr); }
    public void ThreadPollInstr(ThreadPollInstr threadpollinstr) { error(threadpollinstr); }
    public void ThrowExceptionInstr(ThrowExceptionInstr throwexceptioninstr) { error(throwexceptioninstr); }
    public void ToggleBacktraceInstr(ToggleBacktraceInstr instr) { error(instr); }
    public void ToAryInstr(ToAryInstr toaryinstr) { error(toaryinstr); }
    public void TraceInstr(TraceInstr toaryinstr) { error(toaryinstr); }
    public void UndefMethodInstr(UndefMethodInstr undefmethodinstr) { error(undefmethodinstr); }
    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) { error(unresolvedsuperinstr); }
    public void UpdateBlockExecutionStateInstr (UpdateBlockExecutionStateInstr instr) { error(instr); }
    public void YieldInstr(YieldInstr yieldinstr) { error(yieldinstr); }
    @Deprecated public void ZeroOperandArgNoBlockCallInstr(ZeroOperandArgNoBlockCallInstr zeroOperandArgNoBlockCallInstr) { error(zeroOperandArgNoBlockCallInstr); }
    public void ZSuperInstr(ZSuperInstr zsuperinstr) { error(zsuperinstr); }

    // "defined" instructions
    public void GetErrorInfoInstr(GetErrorInfoInstr geterrorinfoinstr) { error(geterrorinfoinstr); }
    public void RestoreErrorInfoInstr(RestoreErrorInfoInstr restoreerrorinfoinstr) { error(restoreerrorinfoinstr); }

    // ruby 1.9 specific
    public void BuildLambdaInstr(BuildLambdaInstr buildlambdainstr) { error(buildlambdainstr); }
    public void GetEncodingInstr(GetEncodingInstr getencodinginstr) { error(getencodinginstr); }
    public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr receivepostreqdarginstr) { error(receivepostreqdarginstr); }

    // unboxing instrs
    public void BoxFloatInstr(BoxFloatInstr instr) { error(instr); }
    public void BoxFixnumInstr(BoxFixnumInstr instr) { error(instr); }
    public void BoxBooleanInstr(BoxBooleanInstr instr) { error(instr); }
    public void AluInstr(AluInstr instr) { error(instr); }
    public void UnboxFloatInstr(UnboxFloatInstr instr) { error(instr); }
    public void UnboxFixnumInstr(UnboxFixnumInstr instr) { error(instr); }
    public void UnboxBooleanInstr(UnboxBooleanInstr instr) { error(instr); }

    // operands
    public void Array(Array array) { error(array); }
    public void Bignum(Bignum bignum) { error(bignum); }
    public void Boolean(Boolean bool) { error(bool); }
    public void UnboxedBoolean(UnboxedBoolean bool) { error(bool); }
    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) { error(closurelocalvariable); }
    public void Complex(Complex complex) { error(complex); }
    public void CurrentScope(CurrentScope currentscope) { error(currentscope); }
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) { error(dynamicsymbol); }
    public void Filename(Filename filename) { error(filename); }
    public void Fixnum(Fixnum fixnum) { error(fixnum); }
    public void FrozenString(FrozenString frozen) { error(frozen); }
    public void UnboxedFixnum(UnboxedFixnum fixnum) { error(fixnum); }
    public void Float(org.jruby.ir.operands.Float flote) { error(flote); }
    public void UnboxedFloat(org.jruby.ir.operands.UnboxedFloat flote) { error(flote); }
    public void GlobalVariable(GlobalVariable globalvariable) { error(globalvariable); }
    public void Hash(Hash hash) { error(hash); }
    public void IRException(IRException irexception) { error(irexception); }
    public void Label(Label label) { error(label); }
    public void LocalVariable(LocalVariable localvariable) { error(localvariable); }
    public void Nil(Nil nil) { error(nil); }
    public void NthRef(NthRef nthref) { error(nthref); }
    public void NullBlock(NullBlock nullblock) { error(nullblock); }
    public void ObjectClass(ObjectClass objectclass) { error(objectclass); }
    public void Rational(Rational rational) { error(rational); }
    public void Regexp(Regexp regexp) { error(regexp); }
    public void Scope(Scope scope) { error(scope); }
    public void ScopeModule(ScopeModule scopemodule) { error(scopemodule); }
    public void Self(Self self) { error(self); }
    public void Splat(Splat splat) { error(splat); }
    public void StandardError(StandardError standarderror) { error(standarderror); }
    public void MutableString(MutableString mutablestring) { error(mutablestring); }
    public void SValue(SValue svalue) { error(svalue); }
    public void Symbol(Symbol symbol) { error(symbol); }
    public void SymbolProc(SymbolProc symbolproc) { error(symbolproc); }
    public void TemporaryVariable(TemporaryVariable temporaryvariable) { error(temporaryvariable); }
    public void TemporaryLocalVariable(TemporaryLocalVariable temporarylocalvariable) { error(temporarylocalvariable); }
    public void TemporaryFloatVariable(TemporaryFloatVariable temporaryfloatvariable) { error(temporaryfloatvariable); }
    public void TemporaryFixnumVariable(TemporaryFixnumVariable temporaryfixnumvariable) { error(temporaryfixnumvariable); }
    public void TemporaryBooleanVariable(TemporaryBooleanVariable temporarybooleanvariable) { error(temporarybooleanvariable); }
    public void UndefinedValue(UndefinedValue undefinedvalue) { error(undefinedvalue); }
    public void UnexecutableNil(UnexecutableNil unexecutablenil) { error(unexecutablenil); }
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) { error(wrappedirclosure); }
}
