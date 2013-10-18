package org.jruby.ir.persistence.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jcodings.Encoding;
import org.jruby.RubyModule;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.BreakInstr;
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
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
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
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.parser.dummy.MultipleParamInstr;
import org.jruby.ir.persistence.parser.dummy.SingleParamInstr;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.CallType;

public class IRInstructionFactory {

    private final IRParsingContext context;

    public IRInstructionFactory(IRParsingContext context) {
        this.context = context;
    }

    public LabelInstr createLabel(Label label) {
        return new LabelInstr(label);
    }

    public JumpInstr createJump(Label target) {
        return new JumpInstr(target);
    }

    public Instr createInstrWithoutParams(String operationName) {
        Operation operation = NonIRObjectFactory.INSTANCE.createOperation(operationName);
        switch (operation) {
        case EXC_REGION_END:
            return createExceptionRegionEndMarker();
        case NOP:
            return createNop();
        case POP_BINDING:
            return createPopBinding();
        case POP_FRAME:
            return createPopFrame();
        case PUSH_FRAME:
            return createPushFrame();
        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private ExceptionRegionEndMarkerInstr createExceptionRegionEndMarker() {
        return new ExceptionRegionEndMarkerInstr();
    }

    private NopInstr createNop() {
        return NopInstr.NOP;
    }

    private PopBindingInstr createPopBinding() {
        return new PopBindingInstr();
    }

    private PopFrameInstr createPopFrame() {
        return new PopFrameInstr();
    }

    private PushFrameInstr createPushFrame() {
        return new PushFrameInstr();
    }

    public Instr createInstrWithSingleParam(SingleParamInstr instr) {
        Operation operation = instr.getOperation();
        Object param = instr.getParameter();
        switch (operation) {
        case BREAK:
            return createBreak(param);
        case CLOSURE_RETURN:
            return createClosureReturn(param);
        case JUMP_INDIRECT:
            return createJumpInderect(param);
        case LINE_NUM:
            return createLineNum(param);
        case RETURN:
            return createReturn(param);
        case THREAD_POLL:
            return createThreadPoll(param);
        case THROW:
            return createThrowException(param);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private BreakInstr createBreak(Object param) {
        Operand rv = (Operand) param;
        // Scope is null if name was not been persisted
        IRScope s = null;

        return new BreakInstr(rv, s);
    }

    private ClosureReturnInstr createClosureReturn(Object param) {
        Operand rv = (Operand) param;

        return new ClosureReturnInstr(rv);
    }

    private JumpIndirectInstr createJumpInderect(Object param) {
        Variable target = (Variable) param;

        return new JumpIndirectInstr(target);
    }

    private LineNumberInstr createLineNum(Object param) {
        Integer number = (Integer) param;
        IRScope currentScope = context.getCurrentScope();
        return new LineNumberInstr(currentScope, number);
    }

    private ReturnInstr createReturn(Object param) {
        Operand returnValue = (Operand) param;
        return new ReturnInstr(returnValue);
    }
    
    private ThreadPollInstr createThreadPoll(Object param) {
        boolean onBackEdge = (Boolean) param;
        
        return new ThreadPollInstr(onBackEdge);
    }

    private ThrowExceptionInstr createThrowException(Object param) {
        Operand exception = (Operand) param;

        return new ThrowExceptionInstr(exception);
    }

    public Instr createInstrWithMultipleParams(MultipleParamInstr instr) {
        Operation operation = instr.getOperation();
        Iterator<Object> paramsIterator = instr.getParameters().iterator();
        switch (operation) {
        case ALIAS:
            return createAlias(paramsIterator);
        case ATTR_ASSIGN:
            return createAttrAssign(paramsIterator);
        case B_FALSE:
            return createBFalse(paramsIterator);
        case B_TRUE:
            return createBTrue(paramsIterator);
        case B_NIL:
            return createBNil(paramsIterator);
        case B_UNDEF:
            return createBUndef(paramsIterator);
        case BEQ:
            return createBEQ(paramsIterator);
        case BINDING_STORE:
            return createStoreLocalVar(paramsIterator);
        case BNE:
            return createBNE(paramsIterator);
        case BREAK:
            return createBreak(paramsIterator);
        case CHECK_ARGS_ARRAY_ARITY:
            return createCheckArgsArrayArity(paramsIterator);
        case CHECK_ARITY:
            return createCheckArity(paramsIterator);
        case DEF_CLASS_METH:
            return createDefineMethod(paramsIterator, false);
        case DEF_INST_METH:
            return createDefineMethod(paramsIterator, true);
        case EXC_REGION_START:
            return createExceptionRegionStartMarker(paramsIterator);
        case GVAR_ALIAS:
            return createGvarAlias(paramsIterator);
        case CALL:
        case CONST_MISSING:
        case SUPER:
        case ZSUPER:
            return createNoResultCall(operation, paramsIterator);
        case RETURN:
            return createReturn(paramsIterator);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private AliasInstr createAlias(Iterator<Object> paramsIterator) {
        
        Variable receiver = (Variable) paramsIterator.next();
        Operand newName = (Operand) paramsIterator.next();
        Operand oldName = (Operand) paramsIterator.next();

        // FIXME?: Maybe AliasInstr should implement ResultInstr?
        return new AliasInstr(receiver, newName, oldName);
    }
    
    private AttrAssignInstr createAttrAssign(Iterator<Object> paramsIterator) {        
        Operand receiver = (Operand) paramsIterator.next();
        MethAddr methAddr = (MethAddr) paramsIterator.next();

        @SuppressWarnings("unchecked")
        List<Operand> argsList = (ArrayList<Operand>) paramsIterator.next();
        Operand[] args = retreiveArgs(argsList);

        if(paramsIterator.hasNext()) {
            String specializedInstName = (String) paramsIterator.next();
            SpecializedInstType specializedInstType = SpecializedInstType.valueOf(specializedInstName);
            
            AttrAssignInstr attrAssignInstr = new AttrAssignInstr(receiver, methAddr, args);
            
            switch (specializedInstType) {
            case ONE_OPERAND:
                return new OneArgOperandAttrAssignInstr(attrAssignInstr);
            default:
                throw new UnsupportedOperationException(specializedInstName);    
            }
        }
        
        return new AttrAssignInstr(receiver, methAddr, args);
    }

    private BranchInstr createBEQ(Iterator<Object> paramsIterator) {
        Operand arg1 = (Operand) paramsIterator.next();
        Operand arg2 = (Operand) paramsIterator.next();
        Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBFalse(Iterator<Object> paramsIterator) {
        Operand arg1 = (Operand) paramsIterator.next();
        Operand arg2 = context.getIRManager().getFalse();
        Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBTrue(Iterator<Object> paramsIterator) {
        Operand arg1 = (Operand) paramsIterator.next();
        Operand arg2 = context.getIRManager().getTrue();
        Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBNil(Iterator<Object> paramsIterator) {
        Operand arg1 = (Operand) paramsIterator.next();
        Operand arg2 = context.getIRManager().getNil();
        Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBUndef(Iterator<Object> paramsIterator) {
        Operand arg1 = (Operand) paramsIterator.next();
        Operand arg2 = UndefinedValue.UNDEFINED;
        Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBNE(Iterator<Object> paramsIterator) {
        Operand arg1 = (Operand) paramsIterator.next();
        Operand arg2 = (Operand) paramsIterator.next();
        Label target = (Label) paramsIterator.next();

        return BNEInstr.create(arg1, arg2, target);
    }
    
    private BreakInstr createBreak(Iterator<Object> paramsIterator) {
        Operand rv = (Operand) paramsIterator.next();

        String scopeName = (String) paramsIterator.next();
        IRScope s = context.getScopeByName(scopeName);

        return new BreakInstr(rv, s);
    }
    
    private CheckArgsArrayArityInstr createCheckArgsArrayArity(Iterator<Object> paramsIterator) {
        Operand argsArray = (Operand) paramsIterator.next();
        int required = (Integer) paramsIterator.next();
        int opt = (Integer) paramsIterator.next();
        int rest = (Integer) paramsIterator.next();

        return new CheckArgsArrayArityInstr(argsArray, required, opt, rest);
    }

    private CheckArityInstr createCheckArity(Iterator<Object> paramsIterator) {
        int required = (Integer) paramsIterator.next();
        int opt = (Integer) paramsIterator.next();
        int rest = (Integer) paramsIterator.next();

        return new CheckArityInstr(required, opt, rest);
    }

    private Instr createDefineMethod(Iterator<Object> paramsIterator, boolean isInstanceMethod) {
        Operand container = (Operand) paramsIterator.next();
        
        String name = (String) paramsIterator.next();
        IRMethod method = (IRMethod) context.getScopeByName(name);

        if (isInstanceMethod) {
            return new DefineInstanceMethodInstr(container, method);
        } else {
            return new DefineClassMethodInstr(container, method);
        }
    }
    
    private ExceptionRegionStartMarkerInstr createExceptionRegionStartMarker(Iterator<Object> paramsIterator) {
        Label begin = (Label) paramsIterator.next();
        Label end = (Label) paramsIterator.next();
        Label firstRescueBlockLabel = (Label) paramsIterator.next();
        
        Label ensureBlockLabel = null;
        if(paramsIterator.hasNext()) {
            ensureBlockLabel = (Label) paramsIterator.next();
        }
        
        return new ExceptionRegionStartMarkerInstr(begin, end, ensureBlockLabel, firstRescueBlockLabel);
    }

    private GVarAliasInstr createGvarAlias(Iterator<Object> paramsIterator) {
        Operand newName = (Operand) paramsIterator.next();
        Operand oldName = (Operand) paramsIterator.next();

        return new GVarAliasInstr(newName, oldName);
    }

    private StoreLocalVarInstr createStoreLocalVar(Iterator<Object> paramsIterator) {
        Operand value = (Operand) paramsIterator.next();

        String scopeName = (String) paramsIterator.next();
        IRScope scope = context.getScopeByName(scopeName);

        LocalVariable lvar = (LocalVariable) paramsIterator.next();

        return new StoreLocalVarInstr(value, scope, lvar);
    }
    
    private NoResultCallInstr createNoResultCall(Operation operation, Iterator<Object> paramsIterator) {
        Operand receiver = (Operand) paramsIterator.next();
        String callTypeString = (String) paramsIterator.next();
        CallType callType = NonIRObjectFactory.INSTANCE.createCallType(callTypeString);
        MethAddr methAddr = (MethAddr) paramsIterator.next();
        
        @SuppressWarnings("unchecked")
        List<Operand> argsList = (ArrayList<Operand>) paramsIterator.next();
        Operand[] args = retreiveArgs(argsList);
        
        Operand closure = null;
        if(paramsIterator.hasNext()) {
            Object parameter = paramsIterator.next();
            if (parameter instanceof Operand) {
                closure = (Operand) parameter;
            } else if (parameter instanceof String){
                String specializedInstName = (String) parameter;
                SpecializedInstType specializedInstType = SpecializedInstType.valueOf(specializedInstName);
                
                NoResultCallInstr noResultCallInstr = new NoResultCallInstr(operation, callType, methAddr, receiver, args, closure);
                
                switch (specializedInstType) {
                case ONE_OPERAND:
                    return new OneOperandArgNoBlockNoResultCallInstr(noResultCallInstr);
                default:
                    throw new UnsupportedOperationException(specializedInstName);    
                }
            }
        }
        
        return new NoResultCallInstr(operation, callType, methAddr, receiver, args, closure);
    }
    
    private ReturnInstr createReturn(Iterator<Object> paramsIterator) {
        Operand rv = (Operand) paramsIterator.next();
        String methodName = (String) paramsIterator.next();
        IRMethod methodToReturn = (IRMethod) context.getScopeByName(methodName);

        return new ReturnInstr(rv, methodToReturn);
    }

    public Instr createReturnInstrWithNoParams(Variable result, String operationName) {
        Operation operation = NonIRObjectFactory.INSTANCE.createOperation(operationName);
        switch (operation) {
        case BLOCK_GIVEN:
            return createBlockGiven(result);
        case BACKREF_IS_MATCH_DATA:
            return createBackrefIsMatchData(result);
        case GET_BACKREF:
            return createGetBackref(result);
        case GET_ERROR_INFO:
            return createGetErrorInfo(result);
        case RECV_SELF:
            return createReceiveSelf(result);
        case RECV_CLOSURE:
            return createReceiveClosure(result);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private BlockGivenInstr createBlockGiven(Variable result) {
        return new BlockGivenInstr(result);
    }

    private BackrefIsMatchDataInstr createBackrefIsMatchData(Variable result) {
        return new BackrefIsMatchDataInstr(result);
    }

    private GetBackrefInstr createGetBackref(Variable result) {
        return new GetBackrefInstr(result);
    }

    private GetErrorInfoInstr createGetErrorInfo(Variable result) {
        return new GetErrorInfoInstr(result);
    }

    private ReceiveSelfInstr createReceiveSelf(Variable result) {
        return new ReceiveSelfInstr(result);
    }

    private ReceiveClosureInstr createReceiveClosure(Variable result) {
        return new ReceiveClosureInstr(result);
    }

    public Instr createReturnInstrWithSingleParam(Variable result, SingleParamInstr instr) {
        Operation operation = instr.getOperation();
        Object param = instr.getParameter();
        switch (operation) {
        case COPY:
            return createCopy(result, param);
        case ENSURE_RUBY_ARRAY:
            return createEnsureRubyArray(result, param);
        case CLASS_VAR_MODULE:
            return createGetClassVarContainerModule(result, param);
        case GET_ENCODING:
            return createGetEncoding(result, param);
        case GET_GLOBAL_VAR:
            return createGetGlobalVar(result, param);
        case GLOBAL_IS_DEFINED:
            return createGlobalIsDefined(result, param);
        case MATCH:
            return createMatch(result, param);
        case METHOD_LOOKUP:
            return createMethodLookup(result, param);
        case NOT:
            return createNot(result, param);
        case PROCESS_MODULE_BODY:
            return createProcessModuleBody(result, param);
        case PUSH_BINDING:
            return createPushBinding(result, param);
        case RECV_EXCEPTION:
            return createReceiveException(result, param);
        case RECV_OPT_ARG:
            return createReceiveOptArgs18(result, param);
        case RECV_PRE_REQD_ARG:
            return createReceivePreReqdArg(result, param);
        case RECV_REST_ARG:
            return createReceiveRestArgs18(result, param);
        case RECORD_END_BLOCK:
            return createRecordEndBlock(result, param);
        case RESTORE_ERROR_INFO:
            return createRestoreErrorInfo(result, param);
        case SET_RETADDR:
            return createSetReturnAddress(result, param);
        case SUPER_METHOD_BOUND:
            return createSuperMetodBound(result, param);
        case UNDEF_METHOD:
            return createUndefMethod(result, param);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    public CopyInstr createCopy(Variable result, Object param) {
        Operand s = (Operand) param;
        
        return new CopyInstr(result, s);
    }

    private EnsureRubyArrayInstr createEnsureRubyArray(Variable result, Object param) {
        Operand s = (Operand) param;

        return new EnsureRubyArrayInstr(result, s);
    }
    
    private GetClassVarContainerModuleInstr createGetClassVarContainerModule(Variable result, Object param) {
        Operand startingScope = (Operand) param;
        
        return new GetClassVarContainerModuleInstr(result, startingScope, null);
    }

    private GetEncodingInstr createGetEncoding(Variable result, Object param) {
        String encodingName = (String) param;
        Encoding encoding = NonIRObjectFactory.INSTANCE.createEncoding(encodingName);

        return new GetEncodingInstr(result, encoding);
    }

    private GetGlobalVariableInstr createGetGlobalVar(Variable dest, Object param) {
        GlobalVariable gvar = (GlobalVariable) param;

        return new GetGlobalVariableInstr(dest, gvar);
    }

    private GlobalIsDefinedInstr createGlobalIsDefined(Variable result, Object param) {
        StringLiteral name = (StringLiteral) param;

        return new GlobalIsDefinedInstr(result, name);
    }

    private MatchInstr createMatch(Variable result, Object param) {
        Operand receiver = (Operand) param;

        return new MatchInstr(result, receiver);
    }

    private MethodLookupInstr createMethodLookup(Variable result, Object param) {
        MethodHandle mh = (MethodHandle) param;

        return new MethodLookupInstr(result, mh);
    }

    private NotInstr createNot(Variable result, Object param) {
        Operand arg = (Operand) param;

        return new NotInstr(result, arg);
    }

    private ProcessModuleBodyInstr createProcessModuleBody(Variable result, Object param) {
        Operand moduleBody = (Operand) param;

        return new ProcessModuleBodyInstr(result, moduleBody);
    }

    private PushBindingInstr createPushBinding(Variable result, Object param) {
        String scopeName = (String) param;
        IRScope scope = context.getScopeByName(scopeName);

        return new PushBindingInstr(scope);
    }

    private ReceiveExceptionInstr createReceiveException(Variable result, Object param) {
        boolean checkType = (Boolean) param;

        return new ReceiveExceptionInstr(result, checkType);
    }

    private ReceiveRestArgInstr18 createReceiveRestArgs18(Variable result, Object param) {
        int argIndex = (Integer) param;

        return new ReceiveRestArgInstr18(result, argIndex);
    }

    private ReceivePreReqdArgInstr createReceivePreReqdArg(Variable result, Object param) {
        int argIndex = (Integer) param;
        
        return new ReceivePreReqdArgInstr(result, argIndex);
    }

    private ReceiveOptArgInstr18 createReceiveOptArgs18(Variable result, Object param) {
        int index = (Integer) param;

        return new ReceiveOptArgInstr18(result, index);
    }

    private RecordEndBlockInstr createRecordEndBlock(Variable result, Object param) {
        IRScope declaringScope = context.getCurrentScope();
        
        String endBlockClosureName = (String) param;
        IRClosure endBlockClosure = (IRClosure) context.getScopeByName(endBlockClosureName);

        return new RecordEndBlockInstr(declaringScope, endBlockClosure);
    }

    private RestoreErrorInfoInstr createRestoreErrorInfo(Variable result, Object param) {
        Operand arg = (Operand) param;

        return new RestoreErrorInfoInstr(arg);
    }

    private SetReturnAddressInstr createSetReturnAddress(Variable result, Object param) {
        Label l = (Label) param;

        return new SetReturnAddressInstr(result, l);
    }

    private SuperMethodBoundInstr createSuperMetodBound(Variable result, Object param) {
        Operand object = (Operand) param;

        return new SuperMethodBoundInstr(result, object);
    }

    private UndefMethodInstr createUndefMethod(Variable result, Object param) {
        Operand methodName = (Operand) param;
        
        return new UndefMethodInstr(result, methodName);
    }

    public Instr createReturnInstrWithMultipleParams(Variable result, MultipleParamInstr instr) {
        Operation operation = instr.getOperation();
        Iterator<Object> paramsIterator = instr.getParameters().iterator();
        switch (operation) {
        case CALL:
            return createCall(result, paramsIterator);
        case CONST_MISSING:
            return createConstMissing(result, paramsIterator);
        case DEF_CLASS:
            return createDefineClass(result, paramsIterator);
        case DEF_META_CLASS:
            return createDefineMetaClass(result, paramsIterator);
        case DEF_MODULE:
            return createDefineModule(result, paramsIterator);
        case EQQ:
            return createEQQ(result, paramsIterator);
        case CLASS_VAR_MODULE:
            return createGetClassVarContainerModule(result, paramsIterator);
        case GET_CVAR:
        case GET_FIELD:
            return createGetInstr(operation, result, paramsIterator);
        case CLASS_VAR_IS_DEFINED:
        case DEFINED_CONSTANT_OR_METHOD:
        case HAS_INSTANCE_VAR:
        case IS_METHOD_BOUND:
        case METHOD_DEFINED:
        case METHOD_IS_PUBLIC:
            return createDefinedObjectName(operation, result, paramsIterator);
        case INHERITANCE_SEARCH_CONST:
            return createInheritanceSearchConstInstr(result, paramsIterator);
        case INSTANCE_OF:
            return createInstanceOf(result, paramsIterator);
        case LAMBDA:
            return createBuildLambda(result, paramsIterator);
        case LEXICAL_SEARCH_CONST:
            return createLexicalSearchConst(result, paramsIterator);
        case BINDING_LOAD:
            return createLoadLocalVar(result, paramsIterator);
        case MATCH2:
            return createMath2(result, paramsIterator);
        case MATCH3:
            return createMatch3(result, paramsIterator);
        case MODULE_GUARD:
            return createModuleVersionGuard(result, paramsIterator);
        case MASGN_OPT:
            return createOptArgMultipleAsgn(result, paramsIterator);
        case RAISE_ARGUMENT_ERROR:
            return createRaiseArgumentError(result, paramsIterator);
        case RECV_OPT_ARG:
            return createReceiveOptArg19(result, paramsIterator);
        case RECV_POST_REQD_ARG:
            return createReceivePostReqdArg(result, paramsIterator);
        case RECV_REST_ARG:
            return createReceiveRestArg19(result, paramsIterator);
        case MASGN_REQD:
            return createReqdArgMultipleAsgn(result, paramsIterator);
        case RESCUE_EQQ:
            return createRescueEQQ(result, paramsIterator);
        case MASGN_REST:
            return createRestArgMultileAsgn(result, paramsIterator);
        case SUPER:
            return createSuperInstr(result, paramsIterator);
        case SEARCH_CONST:
            return createSearchConst(result, paramsIterator);
        case TO_ARY:
            return createToAry(result, paramsIterator);
        case YIELD:
            return createYield(result, paramsIterator);
        case ZSUPER:
            return createZSuper(result, paramsIterator);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private CallInstr createCall(Variable result, Iterator<Object> paramsIterator) {
        
        Operand receiver = (Operand) paramsIterator.next();
        String callTypString = (String) paramsIterator.next();
        CallType callType = CallType.valueOf(callTypString);

        MethAddr methAddr = (MethAddr) paramsIterator.next();
        
        @SuppressWarnings("unchecked")
        ArrayList<Operand> argsList = (ArrayList<Operand>) paramsIterator.next();
        Operand[] args = retreiveArgs(argsList);
        Operand closure = null;
        if (paramsIterator.hasNext()) {
            Object parameter = paramsIterator.next();
            if (parameter instanceof Operand) {
                closure = (Operand) parameter;
            } else if (parameter instanceof String) {
                CallInstr call = new CallInstr(callType, result, methAddr, receiver, args, closure);
                
                String specializedInstName = (String) parameter;
                SpecializedInstType specializedInstType = SpecializedInstType.valueOf(specializedInstName);
                
                switch (specializedInstType) {
                case ONE_OPERAND:
                    return new OneOperandArgNoBlockCallInstr(call);
                    
                case ONE_FIXNUM:
                    return new OneFixnumArgNoBlockCallInstr(call);
                    
                case ZERO_OPERAND:
                    return new ZeroOperandArgNoBlockCallInstr(call);
                    
                default:
                    throw new UnsupportedOperationException(specializedInstName);
                }
            }
        }       

        return new CallInstr(callType, result, methAddr, receiver, args, closure);
    }

    private GetClassVarContainerModuleInstr createGetClassVarContainerModule(Variable result,
            Iterator<Object> paramsIterator) {
        Operand startingScope = (Operand) paramsIterator.next();
        Operand object = (Operand) paramsIterator.next();

        return new GetClassVarContainerModuleInstr(result, startingScope, object);
    }

    private ConstMissingInstr createConstMissing(Variable result, Iterator<Object> paramsIterator) {
        Operand currentModule = (Operand) paramsIterator.next();
        String missingConst = (String) paramsIterator.next();

        return new ConstMissingInstr(result, currentModule, missingConst);
    }

    private DefineClassInstr createDefineClass(Variable result, Iterator<Object> paramsIterator) {

        String className = (String) paramsIterator.next();
        IRClassBody irClassBody = (IRClassBody) context.getScopeByName(className);

        Operand container = (Operand) paramsIterator.next();
        Operand superClass = (Operand) paramsIterator.next();

        return new DefineClassInstr(result, irClassBody, container, superClass);
    }

    private DefineMetaClassInstr createDefineMetaClass(Variable result, Iterator<Object> paramsIterator) {
        String metaClassBodyName = (String) paramsIterator.next();
        IRModuleBody metaClassBody = (IRModuleBody) context
                .getScopeByName(metaClassBodyName);

        Operand object = (Operand) paramsIterator.next();

        return new DefineMetaClassInstr(result, object, metaClassBody);
    }

    private DefineModuleInstr createDefineModule(Variable result, Iterator<Object> paramsIterator) {
        String moduleBodyName = (String) paramsIterator.next();
        IRModuleBody moduleBody = (IRModuleBody) context.getScopeByName(moduleBodyName);

        Operand container = (Operand) paramsIterator.next();

        return new DefineModuleInstr(result, moduleBody, container);
    }

    private EQQInstr createEQQ(Variable result, Iterator<Object> paramsIterator) {
        Operand v1 = (Operand) paramsIterator.next();
        Operand v2 = (Operand) paramsIterator.next();

        return new EQQInstr(result, v1, v2);
    }

    private GetInstr createGetInstr(Operation operation, Variable dest, Iterator<Object> paramsIterator) {
        Operand source = (Operand) paramsIterator.next();
        String ref = (String) paramsIterator.next();        

        switch (operation) {
        case GET_CVAR:
            return new GetClassVariableInstr(dest, source, ref);
        case GET_FIELD:
            return new GetFieldInstr(dest, source, ref);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private DefinedObjectNameInstr createDefinedObjectName(Operation operation, Variable result,
            Iterator<Object> paramsIterator) {
        Operand object = (Operand) paramsIterator.next();
        StringLiteral name = (StringLiteral) paramsIterator.next();

        switch (operation) {
        case CLASS_VAR_IS_DEFINED:
            return new ClassVarIsDefinedInstr(result, object, name);
        case DEFINED_CONSTANT_OR_METHOD:
            return new GetDefinedConstantOrMethodInstr(result, object, name);
        case HAS_INSTANCE_VAR:
            return new HasInstanceVarInstr(result, object, name);
        case IS_METHOD_BOUND:
            return new IsMethodBoundInstr(result, object, name);
        case METHOD_DEFINED:
            return new MethodDefinedInstr(result, object, name);
        case METHOD_IS_PUBLIC:
            return new MethodIsPublicInstr(result, object, name);
        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private InheritanceSearchConstInstr createInheritanceSearchConstInstr(Variable result,
            Iterator<Object> paramsIterator) {
        Operand currentModule = (Operand) paramsIterator.next();
        String constName = (String) paramsIterator.next();
        Boolean noPrivateConsts = (Boolean) paramsIterator.next();

        return new InheritanceSearchConstInstr(result, currentModule, constName,
                noPrivateConsts);
    }

    private InstanceOfInstr createInstanceOf(Variable result, Iterator<Object> paramsIterator) {
        Operand object = (Operand) paramsIterator.next();
        String className = (String) paramsIterator.next();

        return new InstanceOfInstr(result, object, className);
    }
    
    private BuildLambdaInstr createBuildLambda(Variable result, Iterator<Object> paramsIterator) {
        String scopeName = (String) paramsIterator.next();
        IRClosure lambdaBody = (IRClosure) context.getScopeByName(scopeName);

        String fileName = (String) paramsIterator.next();
        int line = (Integer) paramsIterator.next();
        ISourcePosition possition = NonIRObjectFactory.INSTANCE.createSourcePosition(fileName, line);

        return new BuildLambdaInstr(result, lambdaBody, possition);
    }

    private LexicalSearchConstInstr createLexicalSearchConst(Variable result, Iterator<Object> paramsIterator) {
        Operand definingScope = (Operand) paramsIterator.next();

        String constName = (String) paramsIterator.next();

        return new LexicalSearchConstInstr(result, definingScope, constName);
    }

    private LoadLocalVarInstr createLoadLocalVar(Variable result, Iterator<Object> paramsIterator) {
        TemporaryVariable tempResult = (TemporaryVariable) result;

        String scopeName = (String) paramsIterator.next();
        IRScope scope = context.getScopeByName(scopeName);

        LocalVariable lvar = (LocalVariable) paramsIterator.next();

        return new LoadLocalVarInstr(scope, tempResult, lvar);
    }

    private Match2Instr createMath2(Variable result, Iterator<Object> paramsIterator) {
        Operand receiver = (Operand) paramsIterator.next();
        Operand arg = (Operand) paramsIterator.next();

        return new Match2Instr(result, receiver, arg);
    }

    private Match3Instr createMatch3(Variable result, Iterator<Object> paramsIterator) {
        Operand receiver = (Operand) paramsIterator.next();
        Operand arg = (Operand) paramsIterator.next();

        return new Match3Instr(result, receiver, arg);
    }

    // FIXME?: I havent found creation of this instruction
    private ModuleVersionGuardInstr createModuleVersionGuard(Variable result, Iterator<Object> paramsIterator) {
        Operand candidateObj = (Operand) paramsIterator.next();
        int expectedVersion = (Integer) paramsIterator.next();
        String moduleName = (String) paramsIterator.next();
        // FIXME?: persist module
        RubyModule module = null;

        Label failurePathLabel = (Label) paramsIterator.next();

        return new ModuleVersionGuardInstr(module, expectedVersion, candidateObj, failurePathLabel);
    }

    private OptArgMultipleAsgnInstr createOptArgMultipleAsgn(Variable result, Iterator<Object> paramsIterator) {
        Operand array = (Operand) paramsIterator.next();
        Integer index = (Integer) paramsIterator.next();
        Integer minArgsLength = (Integer) paramsIterator.next();

        return new OptArgMultipleAsgnInstr(result, array, index, minArgsLength);
    }

    private RaiseArgumentErrorInstr createRaiseArgumentError(Variable result, Iterator<Object> paramsIterator) {
        int required = (Integer) paramsIterator.next();
        int opt = (Integer) paramsIterator.next();
        int rest = (Integer) paramsIterator.next();
        int numArgs = (Integer) paramsIterator.next();

        return new RaiseArgumentErrorInstr(required, opt, rest, numArgs);
    }

    private ReceiveOptArgInstr19 createReceiveOptArg19(Variable result, Iterator<Object> paramsIterator) {
        int index = (Integer) paramsIterator.next();
        int minArgsLength = (Integer) paramsIterator.next();

        return new ReceiveOptArgInstr19(result, index, minArgsLength);
    }

    private ReceivePostReqdArgInstr createReceivePostReqdArg(Variable result, Iterator<Object> paramsIterator) {
        int index = (Integer) paramsIterator.next();
        int preReqdArgsCount = (Integer) paramsIterator.next();
        int postReqdArgsCount = (Integer) paramsIterator.next();

        return new ReceivePostReqdArgInstr(result, index, preReqdArgsCount, postReqdArgsCount);
    }

    private ReceiveRestArgInstr19 createReceiveRestArg19(Variable result, Iterator<Object> paramsIterator) {
        int argIndex = (Integer) paramsIterator.next();
        int totalRequiredArgs = (Integer) paramsIterator.next();
        int totalOptArgs = (Integer) paramsIterator.next();

        return new ReceiveRestArgInstr19(result, argIndex, totalRequiredArgs, totalOptArgs);
    }

    private ReqdArgMultipleAsgnInstr createReqdArgMultipleAsgn(Variable result, Iterator<Object> paramsIterator) {
        Operand array = (Operand) paramsIterator.next();
        int index = (Integer) paramsIterator.next();
        int preArgsCount = (Integer) paramsIterator.next();
        int postArgsCount = (Integer) paramsIterator.next();

        return new ReqdArgMultipleAsgnInstr(result, array, preArgsCount, postArgsCount, index);
    }

    private RescueEQQInstr createRescueEQQ(Variable result, Iterator<Object> paramsIterator) {
        Operand v1 = (Operand) paramsIterator.next();
        Operand v2 = (Operand) paramsIterator.next();

        return new RescueEQQInstr(result, v1, v2);
    }

    private RestArgMultipleAsgnInstr createRestArgMultileAsgn(Variable result, Iterator<Object> paramsIterator) {
        Operand array = (Operand) paramsIterator.next();
        int index = (Integer) paramsIterator.next();
        int preArgsCount = (Integer) paramsIterator.next();
        int postArgsCount = (Integer) paramsIterator.next();

        return new RestArgMultipleAsgnInstr(result, array, preArgsCount, postArgsCount, index);
    }

    private CallInstr createSuperInstr(Variable result, Iterator<Object> paramsIterator) {
        String superInstrTypeString = (String) paramsIterator.next();
        SuperInstrType instrType = SuperInstrType.valueOf(superInstrTypeString);
        
        switch (instrType) {
        case CLASS:
        case INSTANCE:            
            return createResolvedSuperInstr(instrType, result, paramsIterator);
            
        case UNRESOLVED:
            return createUnresolvedSuperInstr(result, paramsIterator);
            
        default:            
            throw new UnsupportedOperationException(instrType.toString());
        }
        
        
    }

    private CallInstr createResolvedSuperInstr(SuperInstrType type, Variable result, Iterator<Object> paramsIterator) {
        Operand definingModule = (Operand) paramsIterator.next();
        MethAddr superMeth = (MethAddr) paramsIterator.next();
        Operand[] args = (Operand[]) paramsIterator.next();
        // closure cannot be null in call instr with CallType.Super?
        Operand closure = null;
        if(paramsIterator.hasNext()) {
            closure = (Operand) paramsIterator.next();
        }

        switch (type) {
        case CLASS:
            return new ClassSuperInstr(result, definingModule, superMeth, args, closure);
            
        case INSTANCE:
            return new InstanceSuperInstr(result, definingModule, superMeth, args, closure);
            
        default:
            throw new UnsupportedOperationException(type.toString());
        }
    }
    
    private UnresolvedSuperInstr createUnresolvedSuperInstr(Variable result, Iterator<Object> paramsIterator) {
        Operand receiver = (Operand) paramsIterator.next();
        
        @SuppressWarnings("unchecked")
        List<Operand> argsList = (ArrayList<Operand>) paramsIterator.next();
        Operand[] args = retreiveArgs(argsList);
        
        Operand closure = null;
        if(paramsIterator.hasNext()) {
            closure = (Operand) paramsIterator.next();   
        }
        
        return new UnresolvedSuperInstr(result, receiver, args, closure);
    }

    private SearchConstInstr createSearchConst(Variable result, Iterator<Object> paramsIterator) {
        String constName = (String) paramsIterator.next();
        Operand startingScope = (Operand) paramsIterator.next();
        Boolean noPrivateConsts = (Boolean) paramsIterator.next();

        return new SearchConstInstr(result, constName, startingScope,
                noPrivateConsts);
    }

    private ToAryInstr createToAry(Variable result, Iterator<Object> paramsIterator) {
        Operand array = (Operand) paramsIterator.next();
        BooleanLiteral dontToAryArrays = (BooleanLiteral) paramsIterator.next();

        return new ToAryInstr(result, array, dontToAryArrays);
    }

    private YieldInstr createYield(Variable result, Iterator<Object> paramsIterator) {
        Operand block = (Operand) paramsIterator.next();
        Operand arg = (Operand) paramsIterator.next();
        Boolean unwrapArray = (Boolean) paramsIterator.next();

        return new YieldInstr(result, block, arg, unwrapArray);
    }

    private ZSuperInstr createZSuper(Variable result, Iterator<Object> paramsIterator) {
        Operand receiver = (Operand) paramsIterator.next();
        
        // Closure cannot be null here?
        Operand closure = null;
        if(paramsIterator.hasNext()) {
            closure = (Operand) paramsIterator.next();
        }

        return new ZSuperInstr(result, receiver, closure);
    }

    public PutInstr createPutInstr(SingleParamInstr instr, Operand value) {
        Operation operation = instr.getOperation();
        Operand varOperand = (Operand) instr.getParameter();

        switch (operation) {
        case PUT_GLOBAL_VAR:
            return createPutGlobalVar(varOperand, value);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private PutGlobalVarInstr createPutGlobalVar(Operand varOperand, Operand value) {
        GlobalVariable var = (GlobalVariable) varOperand;
        return new PutGlobalVarInstr(var.getName(), value);
    }

    public PutInstr createPutInstr(MultipleParamInstr instr, Operand value) {
        Operation operation = instr.getOperation();
        Iterator<Object> parametersIterator = instr.getParameters().iterator();

        Operand target = (Operand) parametersIterator.next();
        String ref = (String) parametersIterator.next();

        switch (operation) {
        case PUT_CVAR:
            return createPutClassVariable(target, ref, value);
        case PUT_CONST:
            return createPutConst(target, ref, value);
        case PUT_FIELD:
            return createPutField(target, ref, value);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }

    }

    private PutClassVariableInstr createPutClassVariable(Operand scope, String varName,
            Operand value) {
        return new PutClassVariableInstr(scope, varName, value);
    }

    private PutConstInstr createPutConst(Operand scopeOrObj, String constName, Operand val) {
        return new PutConstInstr(scopeOrObj, constName, val);
    }

    private PutFieldInstr createPutField(Operand obj, String fieldName, Operand value) {
        return new PutFieldInstr(obj, fieldName, value);
    }
    
    // Helper method
    private Operand[] retreiveArgs(List<Operand> argsList) {
        Operand[] args = null;
        if (argsList != null) {
            args = new Operand[argsList.size()];
            argsList.toArray(args);
        } else {
            args = Operand.EMPTY_ARRAY;
        }
        return args;
    }
}
