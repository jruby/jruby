package org.jruby.ir;

import org.jruby.ir.instructions.*;
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
import org.jruby.ir.instructions.ruby18.ReceiveOptArgInstr18;
import org.jruby.ir.instructions.ruby18.ReceiveRestArgInstr18;
import org.jruby.ir.instructions.ruby19.BuildLambdaInstr;
import org.jruby.ir.instructions.ruby19.GetEncodingInstr;
import org.jruby.ir.instructions.ruby19.ReceiveOptArgInstr19;
import org.jruby.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ruby19.ReceiveRestArgInstr19;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.CompoundString;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.WrappedIRClosure;

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
        throw new RuntimeException("no compiler logic for " + object.getClass().getName());
    }

    // standard instructions
    public void AliasInstr(AliasInstr aliasinstr) { error(aliasinstr); }
    public void AttrAssignInstr(AttrAssignInstr attrassigninstr) { error(attrassigninstr); }
    public void BEQInstr(BEQInstr beqinstr) { error(beqinstr); }
    public void BFalseInstr(BFalseInstr bfalseinstr) { error(bfalseinstr); }
    public void BlockGivenInstr(BlockGivenInstr blockgiveninstr) { error(blockgiveninstr); }
    public void BNEInstr(BNEInstr bneinstr) { error(bneinstr); }
    public void BNilInstr(BNilInstr bnilinstr) { error(bnilinstr); }
    public void BreakInstr(BreakInstr breakinstr) { error(breakinstr); }
    public void BTrueInstr(BTrueInstr btrueinstr) { error(btrueinstr); }
    public void BUndefInstr(BUndefInstr bundefinstr) { error(bundefinstr); }
    public void CallInstr(CallInstr callinstr) { error(callinstr); }
    public void CheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkargsarrayarityinstr) { error(checkargsarrayarityinstr); }
    public void CheckArityInstr(CheckArityInstr checkarityinstr) { error(checkarityinstr); }
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) { error(classsuperinstr); }
    public void ClosureReturnInstr(ClosureReturnInstr closurereturninstr) { error(closurereturninstr); }
    public void ConstMissingInstr(ConstMissingInstr constmissinginstr) { error(constmissinginstr); }
    public void CopyInstr(CopyInstr copyinstr) { error(copyinstr); }
    public void DefineClassInstr(DefineClassInstr defineclassinstr) { error(defineclassinstr); }
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) { error(defineclassmethodinstr); }
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) { error(defineinstancemethodinstr); }
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) { error(definemetaclassinstr); }
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) { error(definemoduleinstr); }
    public void EnsureRubyArrayInstr(EnsureRubyArrayInstr ensurerubyarrayinstr) { error(ensurerubyarrayinstr); }
    public void EQQInstr(EQQInstr eqqinstr) { error(eqqinstr); }
    public void ExceptionRegionEndMarkerInstr(ExceptionRegionEndMarkerInstr exceptionregionendmarkerinstr) { error(exceptionregionendmarkerinstr); }
    public void ExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) { error(exceptionregionstartmarkerinstr); }
    public void GetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr getclassvarcontainermoduleinstr) { error(getclassvarcontainermoduleinstr); }
    public void GetClassVariableInstr(GetClassVariableInstr getclassvariableinstr) { error(getclassvariableinstr); }
    public void GetFieldInstr(GetFieldInstr getfieldinstr) { error(getfieldinstr); }
    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) { error(getglobalvariableinstr); }
    public void GVarAliasInstr(GVarAliasInstr gvaraliasinstr) { error(gvaraliasinstr); }
    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) { error(inheritancesearchconstinstr); }
    public void InstanceOfInstr(InstanceOfInstr instanceofinstr) { error(instanceofinstr); }
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) { error(instancesuperinstr); }
    public void Instr(Instr instr) { error(instr); }
    public void JumpIndirectInstr(JumpIndirectInstr jumpindirectinstr) { error(jumpindirectinstr); }
    public void JumpInstr(JumpInstr jumpinstr) { error(jumpinstr); }
    public void LabelInstr(LabelInstr labelinstr) { error(labelinstr); }
    public void LexicalSearchConstInstr(LexicalSearchConstInstr lexicalsearchconstinstr) { error(lexicalsearchconstinstr); }
    public void LineNumberInstr(LineNumberInstr linenumberinstr) { error(linenumberinstr); }
    public void LoadLocalVarInstr(LoadLocalVarInstr loadlocalvarinstr) { error(loadlocalvarinstr); }
    public void Match2Instr(Match2Instr match2instr) { error(match2instr); }
    public void Match3Instr(Match3Instr match3instr) { error(match3instr); }
    public void MatchInstr(MatchInstr matchinstr) { error(matchinstr); }
    public void MethodLookupInstr(MethodLookupInstr methodlookupinstr) { error(methodlookupinstr); }
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) { error(moduleversionguardinstr); }
    public void NopInstr(NopInstr nopinstr) { error(nopinstr); }
    public void NoResultCallInstr(NoResultCallInstr noresultcallinstr) { error(noresultcallinstr); }
    public void NotInstr(NotInstr notinstr) { error(notinstr); }
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) { error(optargmultipleasgninstr); }
    public void PopBindingInstr(PopBindingInstr popbindinginstr) { error(popbindinginstr); }
    public void PopFrameInstr(PopFrameInstr popframeinstr) { error(popframeinstr); }
    public void ProcessModuleBodyInstr(ProcessModuleBodyInstr processmodulebodyinstr) { error(processmodulebodyinstr); }
    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) { error(putclassvariableinstr); }
    public void PutConstInstr(PutConstInstr putconstinstr) { error(putconstinstr); }
    public void PutFieldInstr(PutFieldInstr putfieldinstr) { error(putfieldinstr); }
    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) { error(putglobalvarinstr); }
    public void PushBindingInstr(PushBindingInstr pushbindinginstr) { error(pushbindinginstr); }
    public void PushFrameInstr(PushFrameInstr pushframeinstr) { error(pushframeinstr); }
    public void RaiseArgumentErrorInstr(RaiseArgumentErrorInstr raiseargumenterrorinstr) { error(raiseargumenterrorinstr); }
    public void ReceiveClosureInstr(ReceiveClosureInstr receiveclosureinstr) { error(receiveclosureinstr); }
    public void ReceiveExceptionInstr(ReceiveExceptionInstr receiveexceptioninstr) { error(receiveexceptioninstr); }
    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr receiveprereqdarginstr) { error(receiveprereqdarginstr); }
    public void ReceiveSelfInstr(ReceiveSelfInstr receiveselfinstr) { error(receiveselfinstr); }
    public void RecordEndBlockInstr(RecordEndBlockInstr recordendblockinstr) { error(recordendblockinstr); }
    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) { error(reqdargmultipleasgninstr); }
    public void RescueEQQInstr(RescueEQQInstr rescueeqqinstr) { error(rescueeqqinstr); }
    public void RestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr restargmultipleasgninstr) { error(restargmultipleasgninstr); }
    public void ReturnInstr(ReturnInstr returninstr) { error(returninstr); }
    public void SearchConstInstr(SearchConstInstr searchconstinstr) { error(searchconstinstr); }
    public void SetReturnAddressInstr(SetReturnAddressInstr setreturnaddressinstr) { error(setreturnaddressinstr); }
    public void StoreLocalVarInstr(StoreLocalVarInstr storelocalvarinstr) { error(storelocalvarinstr); }
    public void ThreadPollInstr(ThreadPollInstr threadpollinstr) { error(threadpollinstr); }
    public void ThrowExceptionInstr(ThrowExceptionInstr throwexceptioninstr) { error(throwexceptioninstr); }
    public void ToAryInstr(ToAryInstr toaryinstr) { error(toaryinstr); }
    public void UndefMethodInstr(UndefMethodInstr undefmethodinstr) { error(undefmethodinstr); }
    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) { error(unresolvedsuperinstr); }
    public void YieldInstr(YieldInstr yieldinstr) { error(yieldinstr); }
    public void ZSuperInstr(ZSuperInstr zsuperinstr) { error(zsuperinstr); }

    // "defined" instructions
    public void BackrefIsMatchDataInstr(BackrefIsMatchDataInstr backrefismatchdatainstr) { error(backrefismatchdatainstr); }
    public void ClassVarIsDefinedInstr(ClassVarIsDefinedInstr classvarisdefinedinstr) { error(classvarisdefinedinstr); }
    public void GetBackrefInstr(GetBackrefInstr getbackrefinstr) { error(getbackrefinstr); }
    public void GetDefinedConstantOrMethodInstr(GetDefinedConstantOrMethodInstr getdefinedconstantormethodinstr) { error(getdefinedconstantormethodinstr); }
    public void GetErrorInfoInstr(GetErrorInfoInstr geterrorinfoinstr) { error(geterrorinfoinstr); }
    public void GlobalIsDefinedInstr(GlobalIsDefinedInstr globalisdefinedinstr) { error(globalisdefinedinstr); }
    public void HasInstanceVarInstr(HasInstanceVarInstr hasinstancevarinstr) { error(hasinstancevarinstr); }
    public void IsMethodBoundInstr(IsMethodBoundInstr ismethodboundinstr) { error(ismethodboundinstr); }
    public void MethodDefinedInstr(MethodDefinedInstr methoddefinedinstr) { error(methoddefinedinstr); }
    public void MethodIsPublicInstr(MethodIsPublicInstr methodispublicinstr) { error(methodispublicinstr); }
    public void RestoreErrorInfoInstr(RestoreErrorInfoInstr restoreerrorinfoinstr) { error(restoreerrorinfoinstr); }
    public void SuperMethodBoundInstr(SuperMethodBoundInstr supermethodboundinstr) { error(supermethodboundinstr); }

    // ruby 1.8 specific
    public void ReceiveOptArgInstr18(ReceiveOptArgInstr18 receiveoptarginstr) { error(receiveoptarginstr); }
    public void ReceiveRestArgInstr18(ReceiveRestArgInstr18 receiverestarginstr) { error(receiverestarginstr); }

    // ruby 1.9 specific
    public void BuildLambdaInstr(BuildLambdaInstr buildlambdainstr) { error(buildlambdainstr); }
    public void GetEncodingInstr(GetEncodingInstr getencodinginstr) { error(getencodinginstr); }
    public void ReceiveOptArgInstr19(ReceiveOptArgInstr19 receiveoptarginstr) { error(receiveoptarginstr); }
    public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr receivepostreqdarginstr) { error(receivepostreqdarginstr); }
    public void ReceiveRestArgInstr19(ReceiveRestArgInstr19 receiverestarginstr) { error(receiverestarginstr); }

    // operands
    public void Array(Array array) { error(array); }
    public void AsString(AsString asstring) { error(asstring); }
    public void Backref(Backref backref) { error(backref); }
    public void BacktickString(BacktickString backtickstring) { error(backtickstring); }
    public void Bignum(Bignum bignum) { error(bignum); }
    public void BooleanLiteral(BooleanLiteral booleanliteral) { error(booleanliteral); }
    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) { error(closurelocalvariable); }
    public void CompoundArray(CompoundArray compoundarray) { error(compoundarray); }
    public void CompoundString(CompoundString compoundstring) { error(compoundstring); }
    public void CurrentScope(CurrentScope currentscope) { error(currentscope); }
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) { error(dynamicsymbol); }
    public void Fixnum(Fixnum fixnum) { error(fixnum); }
    public void Float(org.jruby.ir.operands.Float flote) { error(flote); }
    public void GlobalVariable(GlobalVariable globalvariable) { error(globalvariable); }
    public void Hash(Hash hash) { error(hash); }
    public void IRException(IRException irexception) { error(irexception); }
    public void Label(Label label) { error(label); }
    public void LocalVariable(LocalVariable localvariable) { error(localvariable); }
    public void MethAddr(MethAddr methaddr) { error(methaddr); }
    public void MethodHandle(MethodHandle methodhandle) { error(methodhandle); }
    public void Nil(Nil nil) { error(nil); }
    public void NthRef(NthRef nthref) { error(nthref); }
    public void ObjectClass(ObjectClass objectclass) { error(objectclass); }
    public void Range(Range range) { error(range); }
    public void Regexp(Regexp regexp) { error(regexp); }
    public void ScopeModule(ScopeModule scopemodule) { error(scopemodule); }
    public void Self(Self self) { error(self); }
    public void Splat(Splat splat) { error(splat); }
    public void StandardError(StandardError standarderror) { error(standarderror); }
    public void StringLiteral(StringLiteral stringliteral) { error(stringliteral); }
    public void SValue(SValue svalue) { error(svalue); }
    public void Symbol(Symbol symbol) { error(symbol); }
    public void TemporaryClosureVariable(TemporaryClosureVariable temporaryclosurevariable) { error(temporaryclosurevariable); }
    public void TemporaryVariable(TemporaryVariable temporaryvariable) { error(temporaryvariable); }
    public void UndefinedValue(UndefinedValue undefinedvalue) { error(undefinedvalue); }
    public void UnexecutableNil(UnexecutableNil unexecutablenil) { error(unexecutablenil); }
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) { error(wrappedirclosure); }
}
