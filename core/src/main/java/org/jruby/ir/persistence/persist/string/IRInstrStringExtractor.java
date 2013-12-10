package org.jruby.ir.persistence.persist.string;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
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
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
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
import org.jruby.ir.instructions.ruby19.BuildLambdaInstr;
import org.jruby.ir.instructions.ruby19.GetEncodingInstr;
import org.jruby.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.specialized.OneArgOperandAttrAssignInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockNoResultCallInstr;
import org.jruby.ir.instructions.specialized.SpecializedInstType;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.persist.string.producer.IRInstructionStringBuilder;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.CallType;

class IRInstrStringExtractor extends IRVisitor {
    
    private final IRInstructionStringBuilder stringProducer;
    
    private IRInstrStringExtractor(IRInstructionStringBuilder stringProducer) {
        this.stringProducer = stringProducer;
    }
    
    // Static factories that are used in translator
    static IRInstrStringExtractor createToplevelInstance() {
        IRInstructionStringBuilder stringProducer = new IRInstructionStringBuilder(null);
        return new IRInstrStringExtractor(stringProducer);
    }    
    static IRInstrStringExtractor createInstance(StringBuilder builder) {
        IRInstructionStringBuilder stringProducer = new IRInstructionStringBuilder(builder);
        return new IRInstrStringExtractor(stringProducer);
    }
    
    public String extract(Instr instr) {
        produceString(instr);
        
        return stringProducer.getResultString();
    }
    
    public void produceString(Instr instr) {
        stringProducer.appendPrefix(instr);

        instr.visit(this);

        stringProducer.appendMarkers(instr);
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
        Operand arg1 = branchInstr.getArg1();
        Operand arg2 = branchInstr.getArg2();
        Label jumpTarget = branchInstr.getJumpTarget();
        
        stringProducer.appendParameters(arg1, arg2, jumpTarget);
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
        Operand arg1 = branchInstr.getArg1();
        Label jumpTarget = branchInstr.getJumpTarget();
        
        stringProducer.appendParameters(arg1, jumpTarget);
    }

    // Call Instructions

    public void CallInstr(CallInstr callinstr) {
        commonForUnspecializedGeneralCallInstr(callinstr);
    }
    
    public void NoResultCallInstr(NoResultCallInstr noresultcallinstr) {
        commonForUnspecializedGeneralCallInstr(noresultcallinstr);
    }

    private void commonForUnspecializedGeneralCallInstr(CallBase callBase) {
        List<Object> parameters = getParametersForGeneralCallInstr(callBase);
        
        Operand closure = callBase.getClosureArg(null);
        parameters.add(closure);
        
        stringProducer.appendParameters(parameters.toArray());
    }
    
    private List<Object> getParametersForGeneralCallInstr(CallBase callBase) {
        List<Object> parameters = new ArrayList<Object>(5);
        
        Operand receiver = callBase.getReceiver();
        CallType callType = callBase.getCallType();
        MethAddr methodAddr = callBase.getMethodAddr();
        Operand[] callArgs = callBase.getCallArgs();
        
        Collections.addAll(parameters, receiver, callType, methodAddr, callArgs);
        
        return parameters;
    }

    // specialized CallInstr
    public void OneFixnumArgNoBlockCallInstr(
            OneFixnumArgNoBlockCallInstr onefixnumargnoblockcallinstr) {
        commonForSpecializedGeneralCallInstr(onefixnumargnoblockcallinstr, SpecializedInstType.ONE_FIXNUM);
    }

    public void OneOperandArgNoBlockCallInstr(
            OneOperandArgNoBlockCallInstr oneoperandargnoblockcallinstr) {
        commonForSpecializedGeneralCallInstr(oneoperandargnoblockcallinstr, SpecializedInstType.ONE_OPERAND);
    }

    public void ZeroOperandArgNoBlockCallInstr(
            ZeroOperandArgNoBlockCallInstr zerooperandargnoblockcallinstr) {
        commonForSpecializedGeneralCallInstr(zerooperandargnoblockcallinstr, SpecializedInstType.ZERO_OPERAND);
    }    

    // specialized NoResultCallInstr
    public void OneOperandArgNoBlockNoResultCallInstr(
            OneOperandArgNoBlockNoResultCallInstr oneoperandargnoblocknoresultcallinstr) {
        commonForSpecializedGeneralCallInstr(oneoperandargnoblocknoresultcallinstr, SpecializedInstType.ONE_OPERAND);
    }

    private void commonForSpecializedGeneralCallInstr(
            CallBase callInstr, SpecializedInstType type) {
        List<Object> parameters = getParametersForGeneralCallInstr(callInstr);
        parameters.add(type);
        
        stringProducer.appendParameters(parameters.toArray());
    }

    public void AttrAssignInstr(AttrAssignInstr attrassigninstr) {
        List<Object> parameters = getCommonParametersForAttrAssign(attrassigninstr);
        stringProducer.appendParameters(parameters.toArray());
    }

    // Specialized AttrAssignInstr
    public void OneArgOperandAttrAssignInstr(
            OneArgOperandAttrAssignInstr oneargoperandattrassigninstr) {
        List<Object> parameters = getCommonParametersForAttrAssign(oneargoperandattrassigninstr);
        parameters.add(SpecializedInstType.ONE_OPERAND);
        
        stringProducer.appendParameters(parameters);
    }

    private List<Object> getCommonParametersForAttrAssign(AttrAssignInstr attrassigninstr) {        
        List<Object> parameters = new ArrayList<Object>(4);
        
        Operand receiver = attrassigninstr.getReceiver();
        MethAddr methodAddr = attrassigninstr.getMethodAddr();
        Operand[] callArgs = attrassigninstr.getCallArgs();
        
        Collections.addAll(parameters, receiver, methodAddr, callArgs);
        
        return parameters;
    }

    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) {        
        commonForResolvedSupeInstr(classsuperinstr, SuperInstrType.CLASS);        
    }
    
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) {
        commonForResolvedSupeInstr(instancesuperinstr, SuperInstrType.INSTANCE);
    }

    private void commonForResolvedSupeInstr(CallInstr superInstr, SuperInstrType type) {
        Operand receiver = superInstr.getReceiver();
        MethAddr methodAddr = superInstr.getMethodAddr();
        Operand[] callArgs = superInstr.getCallArgs();
        Operand closure = superInstr.getClosureArg(null);
        
        stringProducer.appendParameters(type, receiver, methodAddr, callArgs, closure);
    }

    public void ConstMissingInstr(ConstMissingInstr constmissinginstr) {
        Operand receiver = constmissinginstr.getReceiver();
        String missingConst = constmissinginstr.getMissingConst();
        
        stringProducer.appendParameters(receiver, missingConst);
    }    

    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) {
        Operand receiver = unresolvedsuperinstr.getReceiver();
        Operand[] callArgs = unresolvedsuperinstr.getCallArgs();
        Operand closure = unresolvedsuperinstr.getClosureArg(null);
        
        stringProducer.appendParameters(SuperInstrType.UNRESOLVED, receiver, callArgs, closure);
    }

    public void ZSuperInstr(ZSuperInstr zsuperinstr) {
        Operand receiver = zsuperinstr.getReceiver();
        Operand closure = zsuperinstr.getClosureArg(null);
        
        stringProducer.appendParameters(receiver, closure);
    }

    // Get Instructions

    public void GetClassVariableInstr(GetClassVariableInstr getclassvariableinstr) {
        coomonForMostGetInstr(getclassvariableinstr);
    }

    public void GetFieldInstr(GetFieldInstr getfieldinstr) {
        coomonForMostGetInstr(getfieldinstr);
    }

    private void coomonForMostGetInstr(GetInstr getInstr) {
        Operand source = getInstr.getSource();
        String ref = getInstr.getRef();
        
        stringProducer.appendParameters(source, ref);
    }

    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) {
        Operand source = getglobalvariableinstr.getSource();
        
        stringProducer.appendParameters(source);
    }

    // Jump Instructions

    public void JumpIndirectInstr(JumpIndirectInstr jumpindirectinstr) {
        Variable jumpTarget = jumpindirectinstr.getJumpTarget();
        
        stringProducer.appendParameters(jumpTarget);
    }

    public void JumpInstr(JumpInstr jumpinstr) {
        Label jumpTarget = jumpinstr.getJumpTarget();
        
        stringProducer.appendParameters(jumpTarget);
    }

    // Label Instruction
    public void LabelInstr(LabelInstr labelinstr) {
        Label label = labelinstr.getLabel();        
        
        stringProducer.appendParameters(label);
    }

    // Put instructions

    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) {
        commonForMostPutInstr(putclassvariableinstr);
    }

    public void PutConstInstr(PutConstInstr putconstinstr) {
        commonForMostPutInstr(putconstinstr);
    }

    public void PutFieldInstr(PutFieldInstr putfieldinstr) {
        commonForMostPutInstr(putfieldinstr);
    }

    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) {
        GlobalVariable target = (GlobalVariable) putglobalvarinstr.getTarget();
        String varName = target.getName();
        Operand value = putglobalvarinstr.getValue();
        
        stringProducer.appendParameters(varName, value);
    }

    private void commonForMostPutInstr(PutInstr putInstr) {
        Operand target = putInstr.getTarget();
        String ref = putInstr.getRef();
        Operand value = putInstr.getValue();
        
        stringProducer.appendParameters(target, ref, value);
    }

    // Subclasses of MultipleAsgnBaseInstr

    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) {
        List<Object> parameters = getCommonParametersForMultipleAsgnBase(reqdargmultipleasgninstr);
        parameters.add(reqdargmultipleasgninstr.getPreArgsCount());
        parameters.add(reqdargmultipleasgninstr.getPostArgsCount());
        
        stringProducer.appendParameters(parameters.toArray());
    }

    public void RestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr instr) {
        List<Object> parameters = getCommonParametersForMultipleAsgnBase(instr);
        parameters.add(instr.getPreArgsCount());
        parameters.add(instr.getPostArgsCount());
        
        stringProducer.appendParameters(parameters.toArray());
    }

    private List<Object> getCommonParametersForMultipleAsgnBase(MultipleAsgnBase multipleAsgnBase) {
        List<Object> parameters = new ArrayList<Object>(4);
        
        Operand array = multipleAsgnBase.getArrayArg();
        int index = multipleAsgnBase.getIndex();
        
        Collections.addAll(parameters, array, index);
        
        return parameters;
    }

    // Subclasses of DefinedObjectNameInstr

    public void ClassVarIsDefinedInstr(ClassVarIsDefinedInstr classvarisdefinedinstr) {
        commonForAllDefinedObjectName(classvarisdefinedinstr);
    }

    public void GetDefinedConstantOrMethodInstr(
            GetDefinedConstantOrMethodInstr getdefinedconstantormethodinstr) {
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
        Operand object = definedObjectNameInstr.getObject();
        StringLiteral name = definedObjectNameInstr.getName();
        
        stringProducer.appendParameters(object, name);
    }

    public void AliasInstr(AliasInstr aliasinstr) {
        Variable receiver = aliasinstr.getReceiver();
        Operand newName = aliasinstr.getNewName();
        Operand oldName = aliasinstr.getOldName();
        
        stringProducer.appendParameters(receiver, newName, oldName);
    }

    public void BreakInstr(BreakInstr breakinstr) {
        Operand returnValue = breakinstr.getReturnValue();
        IRScope scopeToReturnTo = breakinstr.getScopeToReturnTo();
        
        stringProducer.appendParameters(returnValue, scopeToReturnTo);
    }

    public void CheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkargsarrayarityinstr) {
        Operand argsArray = checkargsarrayarityinstr.getArgsArray();
        int required = checkargsarrayarityinstr.required;
        int opt = checkargsarrayarityinstr.opt;
        int rest = checkargsarrayarityinstr.rest;
        
        stringProducer.appendParameters(argsArray, required, opt, rest);
    }

    public void CheckArityInstr(CheckArityInstr checkarityinstr) {
        int required = checkarityinstr.required;
        int opt = checkarityinstr.opt;
        int rest = checkarityinstr.rest;
        
        stringProducer.appendParameters(required, opt, rest);
    }

    public void CopyInstr(CopyInstr copyinstr) {
        Operand source = copyinstr.getSource();
        
        stringProducer.appendParameters(source);
    }

    public void DefineClassInstr(DefineClassInstr defineclassinstr) {
        IRClassBody newIRClassBody = defineclassinstr.getNewIRClassBody();
        Operand container = defineclassinstr.getContainer();
        Operand superClass = defineclassinstr.getSuperClass();
        
        stringProducer.appendParameters(newIRClassBody, container, superClass);
    }

    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) {
        Operand container = defineclassmethodinstr.getContainer();
        IRMethod method = defineclassmethodinstr.getMethod();
        
        stringProducer.appendParameters(container, method);
    }

    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) {
        Operand container = defineinstancemethodinstr.getContainer();
        IRMethod method = defineinstancemethodinstr.getMethod();
        
        stringProducer.appendParameters(container, method);
    }

    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) {
        IRModuleBody metaClassBody = definemetaclassinstr.getMetaClassBody();
        Operand object = definemetaclassinstr.getObject();
        
        stringProducer.appendParameters(metaClassBody, object);
    }

    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) {
        IRModuleBody newIRModuleBody = definemoduleinstr.getNewIRModuleBody();
        Operand container = definemoduleinstr.getContainer();
        
        stringProducer.appendParameters(newIRModuleBody, container);
    }

    public void EnsureRubyArrayInstr(EnsureRubyArrayInstr ensurerubyarrayinstr) {
        Operand object = ensurerubyarrayinstr.getObject();
        
        stringProducer.appendParameters(object);
    }

    public void EQQInstr(EQQInstr eqqinstr) {
        Operand args[] = eqqinstr.getOperands();
        
        stringProducer.appendParameters(args[0], args[1]);
    }

    public void ExceptionRegionStartMarkerInstr(
            ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) {
        Label begin =  exceptionregionstartmarkerinstr.begin;
        Label end =  exceptionregionstartmarkerinstr.end;
        Label firstRescueBlockLabel =  exceptionregionstartmarkerinstr.firstRescueBlockLabel;
        Label ensureBlockLabel = exceptionregionstartmarkerinstr.ensureBlockLabel;
        
        stringProducer.appendParameters(begin, end, firstRescueBlockLabel, ensureBlockLabel);
    }

    @Override public void GetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr instr) {
        stringProducer.appendParameters(instr.getStartingScope(), instr.getObject());
    }

    @Override public void GVarAliasInstr(GVarAliasInstr instr) {
        stringProducer.appendParameters(instr.getNewName(), instr.getOldName());
    }

    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) {
        Operand currentModule = inheritancesearchconstinstr.getCurrentModule();
        String constName = inheritancesearchconstinstr.getConstName();
        boolean noPrivateConsts = inheritancesearchconstinstr.isNoPrivateConsts();
        
        stringProducer.appendParameters(currentModule, constName, noPrivateConsts);
    }

    public void LexicalSearchConstInstr(LexicalSearchConstInstr instr) {
        stringProducer.appendParameters(instr.getDefiningScope(), instr.getConstName());
    }

    public void LineNumberInstr(LineNumberInstr linenumberinstr) {
        int lineNumber = linenumberinstr.lineNumber;
        
        stringProducer.appendParameters(lineNumber);
    }

    @Override public void LoadLocalVarInstr(LoadLocalVarInstr instr) {
        stringProducer.appendParameters(instr.getScope(), instr.getLocalVar());
    }

    @Override public void Match2Instr(Match2Instr instr) {
        stringProducer.appendParameters(instr.getReceiver(), instr.getArg());
    }

    @Override public void Match3Instr(Match3Instr instr) {
        stringProducer.appendParameters(instr.getReceiver(), instr.getArg());
    }

    @Override public void MatchInstr(MatchInstr instr) {
        stringProducer.appendParameters(instr.getReceiver());
    }

    public void MethodLookupInstr(MethodLookupInstr methodlookupinstr) {
        MethodHandle methodHandle = methodlookupinstr.getMethodHandle();
        
        stringProducer.appendParameters(methodHandle);
    }

    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr instr) {
        Operand candidateObj = instr.getCandidateObject();
        int expectedVersion = instr.getExpectedVersion();
        String name = instr.getModule().getName();
        Label failurePathLabel = instr.getFailurePathLabel();
        
        stringProducer.appendParameters(candidateObj, expectedVersion, name, failurePathLabel);
    }

    @Override public void NotInstr(NotInstr instr) {
        stringProducer.appendParameters(instr.getArg());
    }

    @Override public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr instr) {
        stringProducer.appendParameters(instr.getArrayArg(), instr.getIndex(), instr.getMinArgsLength());
    }

    public void ProcessModuleBodyInstr(ProcessModuleBodyInstr processmodulebodyinstr) {
        Operand moduleBody = processmodulebodyinstr.getModuleBody();
        
        stringProducer.appendParameters(moduleBody);
    }

    @Override public void PushBindingInstr(PushBindingInstr instr) {
        stringProducer.appendParameters(instr.getScope());
    }

    public void RaiseArgumentErrorInstr(RaiseArgumentErrorInstr instr) {
        int required = instr.getRequired();
        int opt = instr.getOpt();
        int rest = instr.getRest();
        int numArgs = instr.getNumArgs();
        
        stringProducer.appendParameters(required, opt, rest, numArgs);
    }

    public void ReceiveExceptionInstr(ReceiveExceptionInstr receiveexceptioninstr) {
        boolean checkType = receiveexceptioninstr.isCheckType();
        
        stringProducer.appendParameters(checkType);
    }

    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr receiveprereqdarginstr) {
        int argIndex = receiveprereqdarginstr.getArgIndex();
        
        stringProducer.appendParameters(argIndex);
    }

    @Override public void RecordEndBlockInstr(RecordEndBlockInstr instr) {
        stringProducer.appendParameters(instr.getEndBlockClosure());
    }

    public void RescueEQQInstr(RescueEQQInstr rescueeqqinstr) {
        Operand[] args = rescueeqqinstr.getOperands();
        
        stringProducer.appendParameters(args[0], args[1]);
    }

    @Override public void ReturnInstr(ReturnInstr returninstr) {
        stringProducer.appendParameters(returninstr.getReturnValue());
    }

    @Override public void SearchConstInstr(SearchConstInstr instr) {
        stringProducer.appendParameters(instr.getConstName(), instr.getStartingScope(), instr.isNoPrivateConsts());
    }

    @Override public void SetReturnAddressInstr(SetReturnAddressInstr instr) {
        stringProducer.appendParameters(instr.getReturnAddr());
    }

    @Override public void StoreLocalVarInstr(StoreLocalVarInstr instr) {
        stringProducer.appendParameters(instr.getValue(), instr.getScope(), instr.getLocalVar());
    }

    @Override public void ThreadPollInstr(ThreadPollInstr instr) {
        stringProducer.appendParameters(instr.onBackEdge);
    }

    @Override public void ThrowExceptionInstr(ThrowExceptionInstr instr) {
        stringProducer.appendParameters(instr.getExceptionArg());
    }

    @Override public void ToAryInstr(ToAryInstr instr) {
        stringProducer.appendParameters(instr.getArrayArg(), instr.dontToAryArrays());
    }
    
    @Override public void UndefMethodInstr(UndefMethodInstr instr) {
        stringProducer.appendParameters(instr.getMethodName());
    }

    @Override public void YieldInstr(YieldInstr instr) {
        stringProducer.appendParameters(instr.getBlockArg(), instr.getYieldArg(), instr.isUnwrapArray());
    }

    @Override public void GlobalIsDefinedInstr(GlobalIsDefinedInstr instr) {
        stringProducer.appendParameters(instr.getName());
    }

    @Override public void RestoreErrorInfoInstr(RestoreErrorInfoInstr instr) {
        stringProducer.appendParameters(instr.getArg());
    }

    @Override public void SuperMethodBoundInstr(SuperMethodBoundInstr instr) {
        stringProducer.appendParameters(instr.getObject());
    }

    @Override public void BuildLambdaInstr(BuildLambdaInstr instr) {
        String lambdaBodyName = instr.getLambdaBodyName();
        ISourcePosition position = instr.getPosition();
        String file = position.getFile();
        int line = position.getLine();
        
        stringProducer.appendParameters(lambdaBodyName, file, line);
    }

    @Override public void GetEncodingInstr(GetEncodingInstr instr) {
        stringProducer.appendParameters(instr.getEncoding().getCharsetName());
    }

    @Override public void ReceiveOptArgInstr(ReceiveOptArgInstr instr) {
        stringProducer.appendParameters(instr.getNumUsedArgs(), instr.getArgOffset(), instr.getArgIndex());
    }

    @Override public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr instr) {
        stringProducer.appendParameters(instr.getArgIndex(), instr.preReqdArgsCount, instr.postReqdArgsCount);
    }

    @Override public void ReceiveRestArgInstr(ReceiveRestArgInstr instr) {
        stringProducer.appendParameters(instr.getArgIndex(), instr.numUsedArgs);
    }
}