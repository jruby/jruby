package org.jruby.ir.persistence.parser;

import java.util.ArrayList;
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
import org.jruby.lexer.yacc.SimpleSourcePosition;
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
        case THREAD_POLL:
            return createThreadPoll();
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

    private ThreadPollInstr createThreadPoll() {
        return new ThreadPollInstr();
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

    private ThrowExceptionInstr createThrowException(Object param) {
        Operand exception = (Operand) param;

        return new ThrowExceptionInstr(exception);
    }

    public Instr createInstrWithMultipleParams(MultipleParamInstr instr) {
        Operation operation = instr.getOperation();
        List<Object> params = instr.getParameters();
        switch (operation) {
        case ATTR_ASSIGN:
            return createAttrAssign(params);
        case B_FALSE:
            return createBFalse(params);
        case B_TRUE:
            return createBTrue(params);
        case B_NIL:
            return createBNil(params);
        case B_UNDEF:
            return createBUndef(params);
        case BEQ:
            return createBEQ(params);
        case BINDING_STORE:
            return createStoreLocalVar(params);
        case BNE:
            return createBNE(params);
        case BREAK:
            return createBreak(params);
        case CHECK_ARGS_ARRAY_ARITY:
            return createCheckArgsArrayArity(params);
        case CHECK_ARITY:
            return createCheckArity(params);
        case DEF_CLASS_METH:
            return createDefineMethod(params, false);
        case DEF_INST_METH:
            return createDefineMethod(params, true);
        case GVAR_ALIAS:
            return createGvarAlias(params);
        case RETURN:
            return createReturn(params);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private AttrAssignInstr createAttrAssign(List<Object> params) {
        MethAddr methAddr = (MethAddr) params.get(0);
        Operand receiver = (Operand) params.get(1);

        List<Operand> argsList = (ArrayList<Operand>) params.get(2);
        Operand[] args = null;
        if (argsList != null) {
            args = new Operand[argsList.size()];
            argsList.toArray(args);
        } else {
            args = Operand.EMPTY_ARRAY;
        }

        return new AttrAssignInstr(receiver, methAddr, args);
    }

    private BranchInstr createBEQ(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = (Operand) params.get(1);
        Label target = (Label) params.get(2);

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBFalse(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = context.getIRManager().getFalse();
        Label target = (Label) params.get(1);

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBTrue(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = context.getIRManager().getTrue();
        Label target = (Label) params.get(1);

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBNil(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = context.getIRManager().getNil();
        Label target = (Label) params.get(1);

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBUndef(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = UndefinedValue.UNDEFINED;
        Label target = (Label) params.get(1);

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBNE(List<Object> params) {
        Operand arg1 = (Operand) params.get(0);
        Operand arg2 = (Operand) params.get(1);
        Label target = (Label) params.get(2);

        return BNEInstr.create(arg1, arg2, target);
    }
    
    private BreakInstr createBreak(List<Object> params) {
        Operand rv = (Operand) params.get(0);

        String scopeName = (String) params.get(1);
        IRScope s = context.getScopeByName(scopeName);

        return new BreakInstr(rv, s);
    }
    
    private CheckArgsArrayArityInstr createCheckArgsArrayArity(List<Object> params) {
        Operand argsArray = (Operand) params.get(0);
        int required = (Integer) params.get(1);
        int opt = (Integer) params.get(2);
        int rest = (Integer) params.get(3);

        return new CheckArgsArrayArityInstr(argsArray, required, opt, rest);
    }

    private CheckArityInstr createCheckArity(List<Object> params) {
        int required = (Integer) params.get(0);
        int opt = (Integer) params.get(1);
        int rest = (Integer) params.get(2);

        return new CheckArityInstr(required, opt, rest);
    }

    private Instr createDefineMethod(List<Object> params, boolean isInstanceMethod) {
        Operand container = (Operand) params.get(0);
        String name = (String) params.get(1);

        IRMethod method = (IRMethod) context.getScopeByName(name);

        if (isInstanceMethod) {
            return new DefineInstanceMethodInstr(container, method);
        } else {
            return new DefineClassMethodInstr(container, method);
        }
    }

    private GVarAliasInstr createGvarAlias(List<Object> params) {
        Operand newName = (Operand) params.get(0);
        Operand oldName = (Operand) params.get(1);

        return new GVarAliasInstr(newName, oldName);
    }

    private StoreLocalVarInstr createStoreLocalVar(List<Object> params) {
        Operand value = (Operand) params.get(0);

        String scopeName = (String) params.get(1);
        IRScope scope = context.getScopeByName(scopeName);

        LocalVariable lvar = (LocalVariable) params.get(2);

        return new StoreLocalVarInstr(value, scope, lvar);
    }
    
    private ReturnInstr createReturn(List<Object> params) {
        Operand rv = (Operand) params.get(0);
        String methodName = (String) params.get(1);
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
            return createRecoreEndBlock(result, param);
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
        return new CopyInstr(result, (Operand) param);
    }

    private EnsureRubyArrayInstr createEnsureRubyArray(Variable result, Object param) {
        Operand s = (Operand) param;

        return new EnsureRubyArrayInstr(result, s);
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
        BooleanLiteral booleanLiteral = (BooleanLiteral) param;
        boolean checkType = booleanLiteral.isTrue();

        return new ReceiveExceptionInstr(result, checkType);
    }

    private ReceiveRestArgInstr18 createReceiveRestArgs18(Variable result, Object param) {
        int argIndex = (Integer) param;

        return new ReceiveRestArgInstr18(result, argIndex);
    }

    private ReceivePreReqdArgInstr createReceivePreReqdArg(Variable result, Object param) {
        Integer argIndex = (Integer) param;
        return new ReceivePreReqdArgInstr(result, argIndex);
    }

    private ReceiveOptArgInstr18 createReceiveOptArgs18(Variable result, Object param) {
        int index = (Integer) param;

        return new ReceiveOptArgInstr18(result, index);
    }

    private RecordEndBlockInstr createRecoreEndBlock(Variable result, Object param) {
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
        List<Object> params = instr.getParameters();
        switch (operation) {
        case ALIAS:
            return createAlias(result, params);
        case CALL:
            return createCall(result, params);
        case CONST_MISSING:
            return createConstMissing(result, params);
        case DEF_CLASS:
            return createDefineClass(result, params);
        case DEF_META_CLASS:
            return createDefineMetaClass(result, params);
        case DEF_MODULE:
            return createDefineModule(result, params);
        case EQQ:
            return createEQQ(result, params);
        case CLASS_VAR_MODULE:
            return createGetClassVarContainerModule(result, params);
        case GET_CVAR:
        case GET_FIELD:
            return createGetInstr(operation, result, params);
        case CLASS_VAR_IS_DEFINED:
        case DEFINED_CONSTANT_OR_METHOD:
        case HAS_INSTANCE_VAR:
        case IS_METHOD_BOUND:
        case METHOD_DEFINED:
        case METHOD_IS_PUBLIC:
            return createDefinedObjectName(operation, result, params);
        case INHERITANCE_SEARCH_CONST:
            return createInheritanceSearchConstInstr(result, params);
        case INSTANCE_OF:
            return createInstanceOf(result, params);
        case LAMBDA:
            return createBuildLambda(result, params);
        case LEXICAL_SEARCH_CONST:
            return createLexicalSearchConst(result, params);
        case BINDING_LOAD:
            return createLoadLocalVar(result, params);
        case MATCH2:
            return createMath2(result, params);
        case MATCH3:
            return createMatch3(result, params);
        case MODULE_GUARD:
            return createModuleVersionGuard(result, params);
        case MASGN_OPT:
            return createOptArgMultipleAsgn(result, params);
        case RAISE_ARGUMENT_ERROR:
            return createRaiseArgumentError(result, params);
        case RECV_OPT_ARG:
            return createReceiveOptArg19(result, params);
        case RECV_POST_REQD_ARG:
            return createReceivePostReqdArg(result, params);
        case RECV_REST_ARG:
            return createReceiveRestArg19(result, params);
        case MASGN_REQD:
            return createReqdArgMultipleAsgn(result, params);
        case RESCUE_EQQ:
            return createRescueEQQ(result, params);
        case MASGN_REST:
            return createRestArgMultileAsgn(result, params);
        case SUPER:
            return createSuperInstr(result, params);
        case SEARCH_CONST:
            return createSearchConst(result, params);
        case TO_ARY:
            return createToAry(result, params);
        case YIELD:
            return createYield(result, params);
        case ZSUPER:
            return createZSuper(result, params);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private AliasInstr createAlias(Variable receiver, List<Object> params) {
        Operand newName = (Operand) params.get(0);
        Operand oldName = (Operand) params.get(1);

        return new AliasInstr(receiver, newName, oldName);
    }

    @SuppressWarnings("unchecked")
    private CallInstr createCall(Variable result, List<Object> params) {
        
        String callTypString = (String) params.get(0);
        CallType callType = CallType.valueOf(callTypString);

        MethAddr methAddr = (MethAddr) params.get(1);
        Operand receiver = (Operand) params.get(2);

        ArrayList<Operand> argsList = (ArrayList<Operand>) params.get(3);
        Operand[] args = null;
        if (argsList != null) {
            args = new Operand[argsList.size()];
            argsList.toArray(args);
        } else {
            args = Operand.EMPTY_ARRAY;
        }
        Operand closure = null;
        if (params.size() == 5) {
            closure = (Operand) params.get(4);
        }

        return CallInstr.create(callType, result, methAddr, receiver, args, closure);
    }

    private GetClassVarContainerModuleInstr createGetClassVarContainerModule(Variable result,
            List<Object> params) {
        Operand startingScope = (Operand) params.get(0);
        Operand object = (Operand) params.get(1);

        return new GetClassVarContainerModuleInstr(result, startingScope, object);
    }

    private ConstMissingInstr createConstMissing(Variable result, List<Object> params) {
        Operand currentModule = (Operand) params.get(0);
        String missingConst = (String) params.get(1);

        return new ConstMissingInstr(result, currentModule, missingConst);
    }

    private DefineClassInstr createDefineClass(Variable result, List<Object> params) {

        String className = (String) params.get(0);
        IRClassBody irClassBody = (IRClassBody) context.getScopeByName(className);

        Operand container = (Operand) params.get(1);
        Operand superClass = (Operand) params.get(2);

        return new DefineClassInstr(result, irClassBody, container, superClass);
    }

    private DefineMetaClassInstr createDefineMetaClass(Variable result, List<Object> params) {
        String metaClassBodyName = (String) params.get(0);
        IRModuleBody metaClassBody = (IRModuleBody) context
                .getScopeByName(metaClassBodyName);

        Operand object = (Operand) params.get(1);

        return new DefineMetaClassInstr(result, object, metaClassBody);
    }

    private DefineModuleInstr createDefineModule(Variable result, List<Object> params) {
        String moduleBodyName = (String) params.get(0);
        IRModuleBody moduleBody = (IRModuleBody) context.getScopeByName(moduleBodyName);

        Operand container = (Operand) params.get(1);

        return new DefineModuleInstr(result, moduleBody, container);
    }

    private EQQInstr createEQQ(Variable result, List<Object> params) {
        Operand v1 = (Operand) params.get(0);
        Operand v2 = (Operand) params.get(1);

        return new EQQInstr(result, v1, v2);
    }

    private GetInstr createGetInstr(Operation operation, Variable dest, List<Object> params) {
        Operand source = (Operand) params.get(0);
        String ref = (String) params.get(1);        

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
            List<Object> params) {
        Operand object = (Operand) params.get(0);
        StringLiteral name = (StringLiteral) params.get(1);

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
            List<Object> params) {
        Operand currentModule = (Operand) params.get(0);
        String constName = (String) params.get(1);
        BooleanLiteral noPrivateConstsLiteral = (BooleanLiteral) params.get(2);

        return new InheritanceSearchConstInstr(result, currentModule, constName,
                noPrivateConstsLiteral.isTrue());
    }

    private InstanceOfInstr createInstanceOf(Variable result, List<Object> params) {
        Operand object = (Operand) params.get(0);
        String className = (String) params.get(1);

        return new InstanceOfInstr(result, object, className);
    }
    
    private BuildLambdaInstr createBuildLambda(Variable result, List<Object> params) {
        String scopeName = (String) params.get(0);
        IRClosure lambdaBody = (IRClosure) context.getScopeByName(scopeName);

        String fileName = (String) params.get(1);
        int line = (Integer) params.get(2);
        ISourcePosition possition = new SimpleSourcePosition(fileName, line);

        return new BuildLambdaInstr(result, lambdaBody, possition);
    }

    private LexicalSearchConstInstr createLexicalSearchConst(Variable result, List<Object> params) {
        Operand definingScope = (Operand) params.get(0);

        String constName = (String) params.get(1);

        return new LexicalSearchConstInstr(result, definingScope, constName);
    }

    private LoadLocalVarInstr createLoadLocalVar(Variable result, List<Object> params) {
        TemporaryVariable tempResult = (TemporaryVariable) result;

        String scopeName = (String) params.get(0);
        IRScope scope = context.getScopeByName(scopeName);

        LocalVariable lvar = (LocalVariable) params.get(1);

        return new LoadLocalVarInstr(scope, tempResult, lvar);
    }

    private Match2Instr createMath2(Variable result, List<Object> params) {
        Operand receiver = (Operand) params.get(0);
        Operand arg = (Operand) params.get(1);

        return new Match2Instr(result, receiver, arg);
    }

    private Match3Instr createMatch3(Variable result, List<Object> params) {
        Operand receiver = (Operand) params.get(0);
        Operand arg = (Operand) params.get(1);

        return new Match3Instr(result, receiver, arg);
    }

    // FIXME?: I havent found creation of this instruction
    private ModuleVersionGuardInstr createModuleVersionGuard(Variable result, List<Object> params) {
        Operand candidateObj = (Operand) params.get(0);
        int expectedVersion = (Integer) params.get(1);
        String moduleName = (String) params.get(2);
        // FIXME?: persist module
        RubyModule module = null;

        Label failurePathLabel = (Label) params.get(3);

        return new ModuleVersionGuardInstr(module, expectedVersion, candidateObj, failurePathLabel);
    }

    private OptArgMultipleAsgnInstr createOptArgMultipleAsgn(Variable result, List<Object> params) {
        Operand array = (Operand) params.get(0);
        Integer index = (Integer) params.get(1);
        Integer minArgsLength = (Integer) params.get(2);

        return new OptArgMultipleAsgnInstr(result, array, index, minArgsLength);
    }

    private RaiseArgumentErrorInstr createRaiseArgumentError(Variable result, List<Object> params) {
        int required = (Integer) params.get(0);
        int opt = (Integer) params.get(1);
        int rest = (Integer) params.get(2);
        int numArgs = (Integer) params.get(3);

        return new RaiseArgumentErrorInstr(required, opt, rest, numArgs);
    }

    private ReceiveOptArgInstr19 createReceiveOptArg19(Variable result, List<Object> params) {
        int index = (Integer) params.get(0);
        int minArgsLength = (Integer) params.get(1);

        return new ReceiveOptArgInstr19(result, index, minArgsLength);
    }

    private ReceivePostReqdArgInstr createReceivePostReqdArg(Variable result, List<Object> params) {
        int index = (Integer) params.get(0);
        int preReqdArgsCount = (Integer) params.get(1);
        int postReqdArgsCount = (Integer) params.get(2);

        return new ReceivePostReqdArgInstr(result, index, preReqdArgsCount, postReqdArgsCount);
    }

    private ReceiveRestArgInstr19 createReceiveRestArg19(Variable result, List<Object> params) {
        int argIndex = (Integer) params.get(0);
        int totalRequiredArgs = (Integer) params.get(1);
        int totalOptArgs = (Integer) params.get(2);

        return new ReceiveRestArgInstr19(result, argIndex, totalRequiredArgs, totalOptArgs);
    }

    private ReqdArgMultipleAsgnInstr createReqdArgMultipleAsgn(Variable result, List<Object> params) {
        Operand array = (Operand) params.get(0);
        int index = (Integer) params.get(1);
        int preArgsCount = (Integer) params.get(2);
        int postArgsCount = (Integer) params.get(2);

        return new ReqdArgMultipleAsgnInstr(result, array, preArgsCount, postArgsCount, index);
    }

    private RescueEQQInstr createRescueEQQ(Variable result, List<Object> params) {
        Operand v1 = (Operand) params.get(0);
        Operand v2 = (Operand) params.get(1);

        return new RescueEQQInstr(result, v1, v2);
    }

    private RestArgMultipleAsgnInstr createRestArgMultileAsgn(Variable result, List<Object> params) {
        Operand array = (Operand) params.get(0);
        int index = (Integer) params.get(1);
        int preArgsCount = (Integer) params.get(2);
        int postArgsCount = (Integer) params.get(2);

        return new RestArgMultipleAsgnInstr(result, array, preArgsCount, postArgsCount, index);
    }

    private CallInstr createSuperInstr(Variable result, List<Object> params) {
        String superInstrTypeString = (String) params.get(0);
        SuperInstrType instrType = SuperInstrType.valueOf(superInstrTypeString);
        
        switch (instrType) {
        case CLASS:
        case INSTANCE:            
            return createResolvedSuperInstr(instrType, result, params);
            
        case UNRESOLVED:
            return createUnresolvedSuperInstr(result, params);
            
        default:            
            throw new UnsupportedOperationException(instrType.toString());
        }
        
        
    }

    private CallInstr createResolvedSuperInstr(SuperInstrType type, Variable result, List<Object> params) {
        MethAddr superMeth = (MethAddr) params.get(1);
        Operand definingModule = (Operand) params.get(2);
        Operand[] args = (Operand[]) params.get(3);
        Operand closure = (Operand) params.get(4);

        switch (type) {
        case CLASS:
            return new ClassSuperInstr(result, definingModule, superMeth, args, closure);
            
        case INSTANCE:
            return new InstanceSuperInstr(result, definingModule, superMeth, args, closure);
            
        default:
            throw new UnsupportedOperationException(type.toString());
        }
    }
    
    private UnresolvedSuperInstr createUnresolvedSuperInstr(Variable result, List<Object> params) {
        Operand receiver = (Operand) params.get(1);
        Operand[] args = (Operand[]) params.get(2);
        Operand closure = (Operand) params.get(3);
        
        return new UnresolvedSuperInstr(result, receiver, args, closure);
    }

    private SearchConstInstr createSearchConst(Variable result, List<Object> params) {
        String constName = (String) params.get(0);
        Operand startingScope = (Operand) params.get(1);
        BooleanLiteral noPrivateConstsLiteral = (BooleanLiteral) params.get(2);

        return new SearchConstInstr(result, constName, startingScope,
                noPrivateConstsLiteral.isTrue());
    }

    private ToAryInstr createToAry(Variable result, List<Object> params) {
        Operand array = (Operand) params.get(0);
        BooleanLiteral dontToAryArrays = (BooleanLiteral) params.get(1);

        return new ToAryInstr(result, array, dontToAryArrays);
    }

    private YieldInstr createYield(Variable result, List<Object> params) {
        Operand block = (Operand) params.get(0);
        Operand arg = (Operand) params.get(1);
        BooleanLiteral unwrapArrayLiteral = (BooleanLiteral) params.get(2);
        boolean unwrapArray = unwrapArrayLiteral.isTrue();

        return new YieldInstr(result, block, arg, unwrapArray);
    }

    private ZSuperInstr createZSuper(Variable result, List<Object> params) {
        Operand receiver = (Operand) params.get(0);
        Operand closure = (Operand) params.get(1);

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
        List<Object> parameters = instr.getParameters();

        Operand target = (Operand) parameters.get(0);
        String ref = (String) parameters.get(1);

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
}
