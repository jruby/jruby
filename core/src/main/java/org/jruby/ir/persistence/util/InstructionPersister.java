package org.jruby.ir.persistence.util;

import java.util.Arrays;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BNilInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.BUndefInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArgsArrayArityInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ClassSuperInstr;
import org.jruby.ir.instructions.ClosureReturnInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineClassInstr;
import org.jruby.ir.instructions.DefineClassMethodInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.DefineMetaClassInstr;
import org.jruby.ir.instructions.DefineModuleInstr;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.instructions.EnsureRubyArrayInstr;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.GVarAliasInstr;
import org.jruby.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.ir.instructions.GetClassVariableInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.GetGlobalVariableInstr;
import org.jruby.ir.instructions.GetInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.InstanceOfInstr;
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
import org.jruby.ir.instructions.ModuleVersionGuardInstr;
import org.jruby.ir.instructions.MultipleAsgnBase;
import org.jruby.ir.instructions.NoResultCallInstr;
import org.jruby.ir.instructions.NopInstr;
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
import org.jruby.ir.instructions.PutInstr;
import org.jruby.ir.instructions.RaiseArgumentErrorInstr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.RecordEndBlockInstr;
import org.jruby.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.ir.instructions.RescueEQQInstr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.SetReturnAddressInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.instructions.SuperInstrType;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.UndefMethodInstr;
import org.jruby.ir.instructions.UnresolvedSuperInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.ZSuperInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.instructions.defined.ClassVarIsDefinedInstr;
import org.jruby.ir.instructions.defined.DefinedObjectNameInstr;
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
import org.jruby.ir.instructions.specialized.OneArgOperandAttrAssignInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.instructions.specialized.SpecializedInstType;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.lexer.yacc.ISourcePosition;

public class InstructionPersister extends IRVisitor {
    
    private static final String PARAMETER_LIST_START_MARKER = "(";
    private static final String PARAMETER_SEPARATOR = ", ";
    private static final String PARAMETER_LIST_END_MARKER = ")";
    
    private static final String COLON = ":";
    private static final String SPACE = " ";
    private static final String EQUAL = " = ";
    
    private static final String DEAD_MARKER = "[DEAD]";
    private static final String HAS_UNUSED_RESULT_MARKER = "[DEAD-RESULT]";
    
    
    private StringBuilder builder = new StringBuilder();
    
    public String getResult() {
        return builder.toString();
    }
    
    @Override
    public void visit(Instr instr) {
        
        if(instr instanceof ResultInstr) {
            Variable result = ((ResultInstr)instr).getResult();
            builder.append(result).append(EQUAL);
        }        
        builder.append(instr.getOperation());
        
        super.visit(instr);
        
        appendMarkers(instr);        
    }
    
    private void appendMarkers(Instr instr) {
        if(instr.hasUnusedResult()) {
                builder.append(HAS_UNUSED_RESULT_MARKER);
        } if(instr.isDead()) {
            builder.append(DEAD_MARKER);
        }
    }
    
    
    // Instructions without parameters
    
    public void BackrefIsMatchDataInstr(BackrefIsMatchDataInstr backrefismatchdatainstr) {}
    public void BlockGivenInstr(BlockGivenInstr blockgiveninstr) {}    
    public void ExceptionRegionEndMarkerInstr(ExceptionRegionEndMarkerInstr exceptionregionendmarkerinstr) {}
    public void NopInstr(NopInstr nopinstr) {}
    public void PopBindingInstr(PopBindingInstr popbindinginstr) {}
    public void PopFrameInstr(PopFrameInstr popframeinstr) {}
    public void PushFrameInstr(PushFrameInstr pushframeinstr) {}
    public void ReceiveClosureInstr(ReceiveClosureInstr receiveclosureinstr) {}
    public void ReceiveSelfInstr(ReceiveSelfInstr receiveselfinstr) {}    
    public void GetBackrefInstr(GetBackrefInstr getbackrefinstr) {}
    public void GetErrorInfoInstr(GetErrorInfoInstr geterrorinfoinstr) {}  
    
    
    // Branch Instructions
    
    public void BEQInstr(BEQInstr beqinstr) {
        commonForBranchInstrWithArg2(beqinstr);
    }
    
    public void BNEInstr(BNEInstr bneinstr) { 
        commonForBranchInstrWithArg2(bneinstr);
    }
    
    private void commonForBranchInstrWithArg2(BranchInstr branchInstr) {
        appendFirstParameter(branchInstr.getArg1());
        Operand arg2 = branchInstr.getArg2();
        if(arg2 != null) {
            appendParameter(arg2);
        }
        appendLastParameter(branchInstr.getJumpTarget());
    }
    
    public void BFalseInstr(BFalseInstr bfalseinstr) { 
        commonForBranchInstrWithoutArg2(bfalseinstr);
    }
    
    public void BNilInstr(BNilInstr bnilinstr) { 
        commonForBranchInstrWithoutArg2(bnilinstr); 
    }
    
    public void BTrueInstr(BTrueInstr btrueinstr) { 
        commonForBranchInstrWithoutArg2(btrueinstr);
    }
    
    public void BUndefInstr(BUndefInstr bundefinstr) { 
        commonForBranchInstrWithoutArg2(bundefinstr);
    }
    
    private void commonForBranchInstrWithoutArg2(BranchInstr branchInstr) {
        appendFirstParameter(branchInstr.getArg1());
        appendLastParameter(branchInstr.getJumpTarget());
    }
    
    
    // Call Instructions
    
    public void CallInstr(CallInstr callinstr) { 
        commonForGeneralCallInstr(callinstr);
        appendClosureToCallInstrIfNeeded(callinstr);
    }
    
    // specialized CallInstr
    public void OneFixnumArgNoBlockCallInstr(OneFixnumArgNoBlockCallInstr onefixnumargnoblockcallinstr) {
        commonForGeneralCallInstr(onefixnumargnoblockcallinstr);
        appendLastParameter(SpecializedInstType.ONE_FIXNUM);
    }
    public void OneOperandArgNoBlockCallInstr(OneOperandArgNoBlockCallInstr oneoperandargnoblockcallinstr) {
        commonForGeneralCallInstr(oneoperandargnoblockcallinstr);
        appendLastParameter(SpecializedInstType.ONE_OPERAND);
    }
    public void ZeroOperandArgNoBlockCallInstr(ZeroOperandArgNoBlockCallInstr zerooperandargnoblockcallinstr) {
        commonForGeneralCallInstr(zerooperandargnoblockcallinstr);
        appendLastParameter(SpecializedInstType.ZERO_OPERAND);
    }
    
    public void NoResultCallInstr(NoResultCallInstr noresultcallinstr) {
        commonForGeneralCallInstr(noresultcallinstr);
        appendClosureToCallInstrIfNeeded(noresultcallinstr);
    }
    
    // specialized NoResultCallInstr
    public void OneOperandArgNoBlockNoResultCallInstr(OneOperandArgNoBlockNoResultCallInstr oneoperandargnoblocknoresultcallinstr) {
        commonForGeneralCallInstr(oneoperandargnoblocknoresultcallinstr);
        appendLastParameter(SpecializedInstType.ONE_OPERAND);
    }

    private void commonForGeneralCallInstr(CallBase callInstr) {
        commonForAllCallInstr(callInstr);
        appendParameter(callInstr.getCallType());
        appendMethAddToCallInstr(callInstr);
        appendCallArgsToCallInstr(callInstr);        
    }
    
    public void AttrAssignInstr(AttrAssignInstr attrassigninstr) { 
        commonForAttrAssign(attrassigninstr);
        builder.append(PARAMETER_LIST_END_MARKER);
    }
    
    // Specialized AttrAssignInstr
    public void OneArgOperandAttrAssignInstr(OneArgOperandAttrAssignInstr oneargoperandattrassigninstr)  { 
        commonForAttrAssign(oneargoperandattrassigninstr);
        appendLastParameter(SpecializedInstType.ONE_OPERAND);
    }

    private void commonForAttrAssign(AttrAssignInstr attrassigninstr) {
        commonForAllCallInstr(attrassigninstr);
        appendMethAddToCallInstr(attrassigninstr);
        appendCallArgsToCallInstr(attrassigninstr);
    }
    
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) {
        commonForAllCallInstr(classsuperinstr);
        appendParameter(SuperInstrType.CLASS);
        appendMethAddToCallInstr(classsuperinstr);
        appendCallArgsToCallInstr(classsuperinstr);
        appendClosureToCallInstrIfNeeded(classsuperinstr);
    }
    
    public void ConstMissingInstr(ConstMissingInstr constmissinginstr) { 
        commonForAllCallInstr(constmissinginstr);
        appendLastParameter(constmissinginstr.getMissingConst());
    }
    
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) {
        commonForAllCallInstr(instancesuperinstr);
        appendParameter(SuperInstrType.INSTANCE);
        appendMethAddToCallInstr(instancesuperinstr);
        appendCallArgsToCallInstr(instancesuperinstr);
        appendClosureToCallInstrIfNeeded(instancesuperinstr);
    }
    
    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) {
        commonForAllCallInstr(unresolvedsuperinstr);
        appendParameter(SuperInstrType.UNRESOLVED);
        appendCallArgsToCallInstr(unresolvedsuperinstr);
        appendClosureToCallInstrIfNeeded(unresolvedsuperinstr);
    }
    
    public void ZSuperInstr(ZSuperInstr zsuperinstr) { 
        commonForAllCallInstr(zsuperinstr);
        appendClosureToCallInstrIfNeeded(zsuperinstr);
    }
    
    private void commonForAllCallInstr(CallBase callInstr) {
        appendFirstParameter(callInstr.getReceiver());
    }

    private void appendMethAddToCallInstr(CallBase callInstr) {
        appendParameter(callInstr.getMethodAddr());
    }
    
    private void appendCallArgsToCallInstr(CallBase callInstr) {
        appendParameter(Arrays.asList(callInstr.getCallArgs()));
    }
    
    private void appendClosureToCallInstrIfNeeded(CallBase callInstr) {
        appendLastParameterIfNotNull(callInstr.getClosure());
    }
    
    
    // Get Instructions
    
    public void GetClassVariableInstr(GetClassVariableInstr getclassvariableinstr) {
        coomonForMostGetInstr(getclassvariableinstr);
    }
    
    public void GetFieldInstr(GetFieldInstr getfieldinstr) {
        coomonForMostGetInstr(getfieldinstr);
    }
    
    private void coomonForMostGetInstr(GetInstr getInstr) {
        appendFirstParameter(getInstr.getSource());
        appendLastParameter(getInstr.getRef());
    }
    
    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) {
        appendOnlyParameter(getglobalvariableinstr.getSource());
    }
    
    
    // Jump Instructions   
    
    public void JumpIndirectInstr(JumpIndirectInstr jumpindirectinstr) {
        appendOnlyParameter(jumpindirectinstr.getJumpTarget());
    }
    
    public void JumpInstr(JumpInstr jumpinstr) {
        builder.append(SPACE).append(jumpinstr.getJumpTarget());
    }
    
    
    // Label Instruction    
    public void LabelInstr(LabelInstr labelinstr) {
        builder.append(SPACE).append(labelinstr.getLabel()).append(COLON);
    }
    
    // Put instructions
    
    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) {
        commonForAllPutInstr(putclassvariableinstr);
    }
    
    public void PutConstInstr(PutConstInstr putconstinstr) {
        commonForAllPutInstr(putconstinstr);
    }
    
    public void PutFieldInstr(PutFieldInstr putfieldinstr) {
        commonForAllPutInstr(putfieldinstr);
    }
    
    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) {
        commonForAllPutInstr(putglobalvarinstr);
    }
    
    private void commonForAllPutInstr(PutInstr putInstr) {
        appendFirstParameter(putInstr.getTarget());
        appendLastParameter(putInstr.getRef());
        
        builder.append(EQUAL).append(putInstr.getValue());
    }
    
    
    // Subclasses of MultipleAsgnBaseInstr
    
    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) {
        commonForMultipleAsgnBase(reqdargmultipleasgninstr);
        appendParameter(reqdargmultipleasgninstr.getPreArgsCount());
        appendLastParameter(reqdargmultipleasgninstr.getPostArgsCount());
    }
    
    public void RestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr restargmultipleasgninstr) {
        commonForMultipleAsgnBase(restargmultipleasgninstr);
        appendParameter(restargmultipleasgninstr.getPreArgsCount());
        appendLastParameter(restargmultipleasgninstr.getPostArgsCount());
    }

    private void commonForMultipleAsgnBase(MultipleAsgnBase multipleAsgnBase) {
        appendFirstParameter(multipleAsgnBase.getArray());
        appendParameter(multipleAsgnBase.getIndex());
    }
    
    
    // Subclasses of DefinedObjectNameInstr
    
    public void ClassVarIsDefinedInstr(ClassVarIsDefinedInstr classvarisdefinedinstr) { 
        commonForAllDefinedObjectName(classvarisdefinedinstr);
    }
    
    public void GetDefinedConstantOrMethodInstr(GetDefinedConstantOrMethodInstr getdefinedconstantormethodinstr) { 
        commonForAllDefinedObjectName(getdefinedconstantormethodinstr);
    }
    
    public void HasInstanceVarInstr(HasInstanceVarInstr hasinstancevarinstr) {
        commonForAllDefinedObjectName(hasinstancevarinstr);
    }
    
    public void IsMethodBoundInstr(IsMethodBoundInstr ismethodboundinstr) { 
        commonForAllDefinedObjectName(ismethodboundinstr);
    }
    
    public void MethodDefinedInstr(MethodDefinedInstr methoddefinedinstr) { 
        commonForAllDefinedObjectName(methoddefinedinstr);
    }
    
    public void MethodIsPublicInstr(MethodIsPublicInstr methodispublicinstr) { 
        commonForAllDefinedObjectName(methodispublicinstr); 
    }
    
    private void commonForAllDefinedObjectName(DefinedObjectNameInstr definedObjectNameInstr) {
        appendFirstParameter(definedObjectNameInstr.getObject());
        appendLastParameter(definedObjectNameInstr.getName());
    }
    
    
    public void AliasInstr(AliasInstr aliasinstr) { 
        appendFirstParameter(aliasinstr.getReceiver());
        appendParameter(aliasinstr.getNewName());
        appendLastParameter(aliasinstr.getOldName()); 
    }
    
    public void BreakInstr(BreakInstr breakinstr) { 
        appendFirstParameter(breakinstr.getReturnValue());
        
        IRScope scopeToReturnTo = breakinstr.getScopeToReturnTo();
        if(scopeToReturnTo != null) {
            appendLastParameter(scopeToReturnTo.getName());
        } else {
            builder.append(PARAMETER_LIST_END_MARKER);
        }
    }
    
    public void CheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkargsarrayarityinstr) {        
        appendFirstParameter(checkargsarrayarityinstr.getArgsArray());
        appendParameter(checkargsarrayarityinstr.required);
        appendParameter(checkargsarrayarityinstr.opt);
        appendLastParameter(checkargsarrayarityinstr.rest);
    }
    
    public void CheckArityInstr(CheckArityInstr checkarityinstr) {
        appendFirstParameter(checkarityinstr.required);
        appendParameter(checkarityinstr.opt);
        appendLastParameter(checkarityinstr.rest);
    }
    
    public void ClosureReturnInstr(ClosureReturnInstr closurereturninstr) {
        appendOnlyParameter(closurereturninstr.getReturnValue());
    }
    
    public void CopyInstr(CopyInstr copyinstr) {
        appendOnlyParameter(copyinstr.getSource());
    }
    
    public void DefineClassInstr(DefineClassInstr defineclassinstr) { 
        appendFirstParameter(defineclassinstr.getNewIRClassBody().getName());
        appendParameter(defineclassinstr.getContainer());
        appendLastParameter(defineclassinstr.getSuperClass());
    }
    
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) {
        appendFirstParameter(defineclassmethodinstr.getContainer());
        appendLastParameter(defineclassmethodinstr.getMethod().getName());
    }
    
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) {
        appendFirstParameter(defineinstancemethodinstr.getContainer());
        appendLastParameter(defineinstancemethodinstr.getMethod().getName());
    }
    
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) { 
        appendFirstParameter(definemetaclassinstr.getMetaClassBody().getName());
        appendLastParameter(definemetaclassinstr.getObject());
    }
    
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) { 
        appendFirstParameter(definemoduleinstr.getNewIRModuleBody().getName());
        appendLastParameter(definemoduleinstr.getContainer());
    }
    
    public void EnsureRubyArrayInstr(EnsureRubyArrayInstr ensurerubyarrayinstr) {
        appendOnlyParameter(ensurerubyarrayinstr.getObject());
    }
    
    public void EQQInstr(EQQInstr eqqinstr) {
        appendFirstParameter(eqqinstr.getArg1());
        appendLastParameter(eqqinstr.getArg2());
    }
    
    public void ExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) {
        appendFirstParameter(exceptionregionstartmarkerinstr.begin);
        appendParameter(exceptionregionstartmarkerinstr.end);
        appendParameter(exceptionregionstartmarkerinstr.firstRescueBlockLabel);
        appendLastParameterIfNotNull(exceptionregionstartmarkerinstr.ensureBlockLabel);
    }
    
    public void GetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr getclassvarcontainermoduleinstr) {
        appendFirstParameter(getclassvarcontainermoduleinstr.getStartingScope());     
        appendLastParameterIfNotNull(getclassvarcontainermoduleinstr.getObject());
    }
    
    public void GVarAliasInstr(GVarAliasInstr gvaraliasinstr) {
        appendFirstParameter(gvaraliasinstr.getNewName());
        appendLastParameter(gvaraliasinstr.getOldName());
    }
    
    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) {
        appendFirstParameter(inheritancesearchconstinstr.getCurrentModule());
        appendParameter(inheritancesearchconstinstr.getConstName());
        appendLastParameter(inheritancesearchconstinstr.isNoPrivateConsts());
    }
    
    public void InstanceOfInstr(InstanceOfInstr instanceofinstr) {
        appendFirstParameter(instanceofinstr.getObject());
        appendLastParameter(instanceofinstr.getClassName());
    }    
    
    public void LexicalSearchConstInstr(LexicalSearchConstInstr lexicalsearchconstinstr) {
        appendFirstParameter(lexicalsearchconstinstr.getDefiningScope());
        appendLastParameter(lexicalsearchconstinstr.getConstName());
    }
    
    public void LineNumberInstr(LineNumberInstr linenumberinstr) {
        appendOnlyParameter(linenumberinstr.lineNumber);
    }
    
    public void LoadLocalVarInstr(LoadLocalVarInstr loadlocalvarinstr) { 
        appendFirstParameter(loadlocalvarinstr.getScope().getName());
        appendLastParameter(loadlocalvarinstr.getLocalVar());
    }
    
    public void Match2Instr(Match2Instr match2instr) {
        appendFirstParameter(match2instr.getReceiver());
        appendLastParameter(match2instr.getArg());
    }
    
    public void Match3Instr(Match3Instr match3instr) {
        appendFirstParameter(match3instr.getReceiver());
        appendLastParameter(match3instr.getArg());
    }
    
    public void MatchInstr(MatchInstr matchinstr) {
        appendOnlyParameter(matchinstr.getReceiver());
    }
    
    public void MethodLookupInstr(MethodLookupInstr methodlookupinstr) {
        appendOnlyParameter(methodlookupinstr.getMethodHandle());
    }
    
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) {
        appendFirstParameter(moduleversionguardinstr.getCandidateObj());
        appendParameter(moduleversionguardinstr.getExpectedVersion());
        appendParameter(moduleversionguardinstr.getModule().getName());
        appendLastParameter(moduleversionguardinstr.getFailurePathLabel());
    }
    
    public void NotInstr(NotInstr notinstr) {
        appendOnlyParameter(notinstr.getArg());
    }
    
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) {
        appendFirstParameter(optargmultipleasgninstr.getArray());
        appendParameter(optargmultipleasgninstr.getIndex());
        appendLastParameter(optargmultipleasgninstr.getMinArgsLength());
    }    
    
    public void ProcessModuleBodyInstr(ProcessModuleBodyInstr processmodulebodyinstr) { 
        appendOnlyParameter(processmodulebodyinstr.getModuleBody());
    }    
    
    public void PushBindingInstr(PushBindingInstr pushbindinginstr) {
        appendOnlyParameter(pushbindinginstr.getScope().getName());
    }
    
    public void RaiseArgumentErrorInstr(RaiseArgumentErrorInstr raiseargumenterrorinstr) {
        appendFirstParameter(raiseargumenterrorinstr.getRequired());
        appendParameter(raiseargumenterrorinstr.getOpt());
        appendParameter(raiseargumenterrorinstr.getRest());
        appendLastParameter(raiseargumenterrorinstr.getNumArgs());
    }
    
    public void ReceiveExceptionInstr(ReceiveExceptionInstr receiveexceptioninstr) {
        appendOnlyParameter(receiveexceptioninstr.isCheckType());
    }
    
    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr receiveprereqdarginstr) {
        appendOnlyParameter(receiveprereqdarginstr.getArgIndex());
    }
    
    public void RecordEndBlockInstr(RecordEndBlockInstr recordendblockinstr) {
        appendOnlyParameter(recordendblockinstr.getEndBlockClosure().getName());
    }
    
    public void RescueEQQInstr(RescueEQQInstr rescueeqqinstr) {
        appendFirstParameter(rescueeqqinstr.getArg1());
        appendLastParameter(rescueeqqinstr.getArg2());
    }
    
    public void ReturnInstr(ReturnInstr returninstr) {
        appendFirstParameter(returninstr.getReturnValue());
        appendLastParameterIfNotNull(returninstr.methodToReturnFrom);
    }
    
    public void SearchConstInstr(SearchConstInstr searchconstinstr) {
        appendFirstParameter(searchconstinstr.getConstName());
        appendParameter(searchconstinstr.getStartingScope());
        appendLastParameter(searchconstinstr.isNoPrivateConsts());
    }
    
    public void SetReturnAddressInstr(SetReturnAddressInstr setreturnaddressinstr) {
        appendOnlyParameter(setreturnaddressinstr.getReturnAddr());
    }
    
    public void StoreLocalVarInstr(StoreLocalVarInstr storelocalvarinstr) {
        appendFirstParameter(storelocalvarinstr.getValue());
        appendParameter(storelocalvarinstr.getScope().getName());
        appendLastParameter(storelocalvarinstr.getLocalVar());
    }
    
    public void ThreadPollInstr(ThreadPollInstr threadpollinstr) {
        appendOnlyParameter(threadpollinstr.onBackEdge);
    }
    
    public void ThrowExceptionInstr(ThrowExceptionInstr throwexceptioninstr) {
        appendOnlyParameter(throwexceptioninstr.getExceptionArg());
    }
    
    public void ToAryInstr(ToAryInstr toaryinstr) {
        appendFirstParameter(toaryinstr.getArray());
        appendLastParameter(toaryinstr.getDontToAryArrays());
    }
    
    public void UndefMethodInstr(UndefMethodInstr undefmethodinstr) {
        appendOnlyParameter(undefmethodinstr.getMethodName());
    }
    
    public void YieldInstr(YieldInstr yieldinstr) {
        appendFirstParameter(yieldinstr.getBlockArg());
        appendParameter(yieldinstr.getYieldArg());
        appendLastParameter(yieldinstr.isUnwrapArray());
    }
        
    public void GlobalIsDefinedInstr(GlobalIsDefinedInstr globalisdefinedinstr) {
        appendOnlyParameter(globalisdefinedinstr.getName());
    }
    
    public void RestoreErrorInfoInstr(RestoreErrorInfoInstr restoreerrorinfoinstr) {
        appendOnlyParameter(restoreerrorinfoinstr.getArg());
    }
    
    public void SuperMethodBoundInstr(SuperMethodBoundInstr supermethodboundinstr) {
        appendOnlyParameter(supermethodboundinstr.getObject());
    }

    
    // ruby 1.8 specific
    
    public void ReceiveOptArgInstr18(ReceiveOptArgInstr18 receiveoptarginstr) { 
        commonFarAllReceiveArgInstr18(receiveoptarginstr);
    }
    
    public void ReceiveRestArgInstr18(ReceiveRestArgInstr18 receiverestarginstr) {
        commonFarAllReceiveArgInstr18(receiverestarginstr);
    }
    
    private void commonFarAllReceiveArgInstr18(ReceiveArgBase receiveArgBase) {
        appendOnlyParameter(receiveArgBase.getArgIndex());
    }

    
    // ruby 1.9 specific
    
    public void BuildLambdaInstr(BuildLambdaInstr buildlambdainstr) {
        appendFirstParameter(buildlambdainstr.getLambdaBodyName());
        ISourcePosition position = buildlambdainstr.getPosition();
        appendParameter(position.getFile());
        appendLastParameter(position.getLine());
    }
    
    public void GetEncodingInstr(GetEncodingInstr getencodinginstr) {
        appendOnlyParameter(getencodinginstr.getEncoding().toString());
    }
    
    public void ReceiveOptArgInstr19(ReceiveOptArgInstr19 receiveoptarginstr) {
        commonForAllReceiveArgInstr19(receiveoptarginstr);
        appendLastParameter(receiveoptarginstr.minArgsLength);
    }
    
    public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr receivepostreqdarginstr) {
        commonForAllReceiveArgInstr19(receivepostreqdarginstr);
        appendParameter(receivepostreqdarginstr.preReqdArgsCount);
        appendLastParameter(receivepostreqdarginstr.postReqdArgsCount);
    }
    public void ReceiveRestArgInstr19(ReceiveRestArgInstr19 receiverestarginstr) {
        commonForAllReceiveArgInstr19(receiverestarginstr);
        appendParameter(receiverestarginstr.getTotalRequiredArgs());
        appendLastParameter(receiverestarginstr.getTotalOptArgs());
    }
    
    private void commonForAllReceiveArgInstr19(ReceiveArgBase receiveArgBase) {
        appendFirstParameter(receiveArgBase.getArgIndex());
    }  
    
    
    // Util methods
    
    private void appendFirstParameter(Object value) {
        builder.append(PARAMETER_LIST_START_MARKER).append(value);
    }
    
    private void appendParameter(Object value) {
        builder.append(PARAMETER_SEPARATOR).append(value);
    }
    
    private void appendLastParameterIfNotNull(Object object) {
        if (object != null) {
            appendLastParameter(object);
        } else {
            builder.append(PARAMETER_LIST_END_MARKER);
        }
    }
    
    private void appendLastParameter(Object value) {
        builder.append(PARAMETER_SEPARATOR).append(value).append(PARAMETER_LIST_END_MARKER);
    }
    
    private void appendOnlyParameter(Object value) {
        builder.append(PARAMETER_LIST_START_MARKER).append(value).append(PARAMETER_LIST_END_MARKER);
    }

}

