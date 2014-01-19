package org.jruby.ir.persistence.read.parser;

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
import org.jruby.ir.instructions.BuildLambdaInstr;
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
import org.jruby.ir.instructions.GetEncodingInstr;
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
import org.jruby.ir.instructions.ReceiveRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
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
import org.jruby.ir.operands.TemporaryLocalVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.read.parser.dummy.InstrWithParams;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.CallType;

public class IRInstructionFactory {

    private final IRParsingContext context;

    public IRInstructionFactory(final IRParsingContext context) {
        this.context = context;
    }

    public Instr createInstrWithoutParams(final String operationName) {
        final Operation operation = NonIRObjectFactory.createOperation(operationName);
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

    public Instr createInstrWithParams(final InstrWithParams instr) {
        final Operation operation = instr.getOperation();
        final ParametersIterator paramsIterator = new ParametersIterator(context, instr.getParameters());
        switch (operation) {
        case JUMP:
            return createJump(paramsIterator);
        case JUMP_INDIRECT:
            return createJumpInderect(paramsIterator);
        case LABEL:
            return createLabel(paramsIterator);
        case LINE_NUM:
            return createLineNum(paramsIterator);
        case THREAD_POLL:
            return createThreadPoll(paramsIterator);
        case THROW:
            return createThrowException(paramsIterator);

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
        case CLASS_SUPER:
        case INSTANCE_SUPER:
        case UNRESOLVED_SUPER:
        case ZSUPER:
            return createNoResultCall(operation, paramsIterator);
        case PUT_GLOBAL_VAR:
            return createPutGlobalVar(paramsIterator);
        case PUT_CVAR:
        case PUT_CONST:
        case PUT_FIELD:
            return createPutInstrOtherThanGlobalVar(operation, paramsIterator);
        case RESTORE_ERROR_INFO:
            return createRestoreErrorInfo(paramsIterator);
        case RETURN:
            return createReturn(paramsIterator);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private JumpInstr createJump(final ParametersIterator paramsIterator) {
        final Label target = (Label) paramsIterator.next();

        return new JumpInstr(target);
    }

    private JumpIndirectInstr createJumpInderect(final ParametersIterator paramsIterator) {
        final Variable target = (Variable) paramsIterator.next();

        return new JumpIndirectInstr(target);
    }

    private LabelInstr createLabel(final ParametersIterator paramsIterator) {
        final Label label = (Label) paramsIterator.next();

        return new LabelInstr(label);
    }

    private LineNumberInstr createLineNum(final ParametersIterator paramsIterator) {
        final int number = paramsIterator.nextInt();
        final IRScope currentScope = context.getCurrentScope();

        return new LineNumberInstr(currentScope, number);
    }

    private ThreadPollInstr createThreadPoll(final ParametersIterator paramsIterator) {
        final boolean onBackEdge = paramsIterator.nextBoolean();

        return new ThreadPollInstr(onBackEdge);
    }

    private ThrowExceptionInstr createThrowException(final ParametersIterator paramsIterator) {
        final Operand exception = paramsIterator.nextOperand();

        return new ThrowExceptionInstr(exception);
    }

    private AliasInstr createAlias(final ParametersIterator paramsIterator) {

        final Variable receiver = (Variable) paramsIterator.next();
        final Operand newName = paramsIterator.nextOperand();
        final Operand oldName = paramsIterator.nextOperand();

        // FIXME?: Maybe AliasInstr should implement ResultInstr?
        return new AliasInstr(receiver, newName, oldName);
    }

    private AttrAssignInstr createAttrAssign(final ParametersIterator paramsIterator) {
        final Operand receiver =  paramsIterator.nextOperand();
        final MethAddr methAddr = (MethAddr) paramsIterator.next();
        final Operand[] args = paramsIterator.nextOperandArray();

        if(paramsIterator.hasNext()) {
            return createSpecializedAttrAssign(paramsIterator, receiver, methAddr, args);
        } else {
            return new AttrAssignInstr(receiver, methAddr, args);
        }
    }

    private AttrAssignInstr createSpecializedAttrAssign(final ParametersIterator paramsIterator,
            final Operand receiver, final MethAddr methAddr, final Operand[] args) {
        final String specializedInstName = paramsIterator.nextString();
        final SpecializedInstType specializedInstType = NonIRObjectFactory.createSpecilizedInstrType(specializedInstName);

        final AttrAssignInstr attrAssignInstr = new AttrAssignInstr(receiver, methAddr, args);

        switch (specializedInstType) {
        case ONE_OPERAND:
            return new OneArgOperandAttrAssignInstr(attrAssignInstr);

        default:
            throw new UnsupportedOperationException(specializedInstName);
        }
    }

    private BranchInstr createBEQ(final ParametersIterator paramsIterator) {
        final Operand arg1 = paramsIterator.nextOperand();
        final Operand arg2 = paramsIterator.nextOperand();
        final Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBFalse(final ParametersIterator paramsIterator) {
        final Operand arg1 = paramsIterator.nextOperand();
        final Operand arg2 = context.getIRManager().getFalse();
        final Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBTrue(final ParametersIterator paramsIterator) {
        final Operand arg1 = paramsIterator.nextOperand();
        final Operand arg2 = context.getIRManager().getTrue();
        final Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBNil(final ParametersIterator paramsIterator) {
        final Operand arg1 = paramsIterator.nextOperand();
        final Operand arg2 = context.getIRManager().getNil();
        final Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBUndef(final ParametersIterator paramsIterator) {
        final Operand arg1 = paramsIterator.nextOperand();
        final Operand arg2 = UndefinedValue.UNDEFINED;
        final Label target = (Label) paramsIterator.next();

        return BEQInstr.create(arg1, arg2, target);
    }

    private BranchInstr createBNE(final ParametersIterator paramsIterator) {
        final Operand arg1 = paramsIterator.nextOperand();
        final Operand arg2 = paramsIterator.nextOperand();
        final Label target = (Label) paramsIterator.next();

        return BNEInstr.create(arg1, arg2, target);
    }

    private BreakInstr createBreak(final ParametersIterator paramsIterator) {
        final Operand rv = paramsIterator.nextOperand();
        final IRScope s = paramsIterator.nextScope();

        return new BreakInstr(rv, s);
    }

    private CheckArgsArrayArityInstr createCheckArgsArrayArity(final ParametersIterator paramsIterator) {
        final Operand argsArray = paramsIterator.nextOperand();
        final int required = paramsIterator.nextInt();
        final int opt = paramsIterator.nextInt();
        final int rest = paramsIterator.nextInt();

        return new CheckArgsArrayArityInstr(argsArray, required, opt, rest);
    }

    private CheckArityInstr createCheckArity(final ParametersIterator paramsIterator) {
        final int required = paramsIterator.nextInt();
        final int opt = paramsIterator.nextInt();
        final int rest = paramsIterator.nextInt();

        return new CheckArityInstr(required, opt, rest);
    }

    private Instr createDefineMethod(final ParametersIterator paramsIterator, boolean isInstanceMethod) {
        final Operand container = paramsIterator.nextOperand();
        final IRMethod method = (IRMethod) paramsIterator.nextScope();

        if (isInstanceMethod) {
            return new DefineInstanceMethodInstr(container, method);
        } else {
            return new DefineClassMethodInstr(container, method);
        }
    }

    private ExceptionRegionStartMarkerInstr createExceptionRegionStartMarker(final ParametersIterator paramsIterator) {
        final Label begin = (Label) paramsIterator.next();
        final Label end = (Label) paramsIterator.next();
        final Label firstRescueBlockLabel = (Label) paramsIterator.next();

        return new ExceptionRegionStartMarkerInstr(begin, end, firstRescueBlockLabel);
    }

    private GVarAliasInstr createGvarAlias(final ParametersIterator paramsIterator) {
        final Operand newName = paramsIterator.nextOperand();
        final Operand oldName = paramsIterator.nextOperand();

        return new GVarAliasInstr(newName, oldName);
    }

    private StoreLocalVarInstr createStoreLocalVar(final ParametersIterator paramsIterator) {
        final Operand value = paramsIterator.nextOperand();
        final IRScope scope = paramsIterator.nextScope();
        final LocalVariable lvar = (LocalVariable) paramsIterator.next();

        return new StoreLocalVarInstr(value, scope, lvar);
    }

    private NoResultCallInstr createNoResultCall(Operation operation, final ParametersIterator paramsIterator) {
        final Operand receiver = paramsIterator.nextOperand();
        final String callTypeString = paramsIterator.nextString();
        final CallType callType = NonIRObjectFactory.createCallType(callTypeString);
        final MethAddr methAddr = (MethAddr) paramsIterator.next();
        final Operand[] args = paramsIterator.nextOperandArray();
        final Object parameter = paramsIterator.next();

        if (parameter instanceof Operand) {
            final Operand closure = (Operand) parameter;
            return new NoResultCallInstr(operation, callType, methAddr, receiver, args, closure);
        } else if (parameter instanceof String) {
            return createSpecializedNoResultCall(operation, receiver, callType, methAddr, args,
                    parameter);
        } else {
            final Operand closure = null; // than closure is null
            return new NoResultCallInstr(operation, callType, methAddr, receiver, args, closure);
        }


    }

    private NoResultCallInstr createSpecializedNoResultCall(Operation operation,
            final Operand receiver, final CallType callType, final MethAddr methAddr,
            final Operand[] args, final Object parameter) {
        final String specializedInstName = (String) parameter;
        final SpecializedInstType specializedInstType = SpecializedInstType
                .valueOf(specializedInstName);

        final Operand closureOfSpecializedInstr = null; // It's always null, because it is only NoBlock instr
        final NoResultCallInstr noResultCallInstr = new NoResultCallInstr(operation, callType,
                methAddr, receiver, args, closureOfSpecializedInstr);

        switch (specializedInstType) {
        case ONE_OPERAND:
            return new OneOperandArgNoBlockNoResultCallInstr(noResultCallInstr);
        default:
            throw new UnsupportedOperationException(specializedInstName);
        }
    }

    private PutGlobalVarInstr createPutGlobalVar(final ParametersIterator paramsIterator) {
        final String varName = paramsIterator.nextString();
        final Operand value = paramsIterator.nextOperand();

        return new PutGlobalVarInstr(varName, value);
    }

    private PutInstr createPutInstrOtherThanGlobalVar(Operation operation, final ParametersIterator paramsIterator) {

        final Operand target = paramsIterator.nextOperand();
        final String ref = paramsIterator.nextString();
        final Operand value = paramsIterator.nextOperand();

        switch (operation) {
        case PUT_CVAR:
            return new PutClassVariableInstr(target, ref, value);
        case PUT_CONST:
            return new PutConstInstr(target, ref, value);
        case PUT_FIELD:
            return new PutFieldInstr(target, ref, value);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }

    }

    private RestoreErrorInfoInstr createRestoreErrorInfo(final ParametersIterator paramsIterator) {
        final Operand arg = paramsIterator.nextOperand();

        return new RestoreErrorInfoInstr(arg);
    }

    private ReturnInstr createReturn(final ParametersIterator paramsIterator) {
        return new ReturnInstr(paramsIterator.nextOperand());
    }

    public Instr createReturnInstrWithNoParams(final Variable result, final String operationName) {
        final Operation operation = NonIRObjectFactory.createOperation(operationName);
        switch (operation) {
        case BACKREF_IS_MATCH_DATA:
            return createBackrefIsMatchData(result);
        case BLOCK_GIVEN:
            return createBlockGiven(result);
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

    private BackrefIsMatchDataInstr createBackrefIsMatchData(final Variable result) {
        return new BackrefIsMatchDataInstr(result);
    }

    private BlockGivenInstr createBlockGiven(final Variable result) {
        // FIXME: This changed
        return null; //new BlockGivenInstr(result);
    }

    private GetBackrefInstr createGetBackref(final Variable result) {
        return new GetBackrefInstr(result);
    }

    private GetErrorInfoInstr createGetErrorInfo(final Variable result) {
        return new GetErrorInfoInstr(result);
    }

    private ReceiveSelfInstr createReceiveSelf(final Variable result) {
        return new ReceiveSelfInstr(result);
    }

    private ReceiveClosureInstr createReceiveClosure(final Variable result) {
        return new ReceiveClosureInstr(result);
    }

    public Instr createReturnInstrWithParams(final Variable result, final InstrWithParams instr) {
        final Operation operation = instr.getOperation();
        final ParametersIterator paramsIterator = new ParametersIterator(context, instr.getParameters());
        switch (operation) {
        case COPY:
            return createCopy(result, paramsIterator);
        case GET_ENCODING:
            return createGetEncoding(result, paramsIterator);
        case GET_GLOBAL_VAR:
            return createGetGlobalVar(result, paramsIterator);
        case GLOBAL_IS_DEFINED:
            return createGlobalIsDefined(result, paramsIterator);
        case MATCH:
            return createMatch(result, paramsIterator);
        case METHOD_LOOKUP:
            return createMethodLookup(result, paramsIterator);
        case NOT:
            return createNot(result, paramsIterator);
        case PROCESS_MODULE_BODY:
            return createProcessModuleBody(result, paramsIterator);
        case PUSH_BINDING:
            return createPushBinding(result, paramsIterator);
        case RECV_RUBY_EXC:
            return createReceiveRubyException(result, paramsIterator);
        case RECV_JRUBY_EXC:
            return createReceiveJRubyException(result, paramsIterator);
        case RECV_PRE_REQD_ARG:
            return createReceivePreReqdArg(result, paramsIterator);
        case RECORD_END_BLOCK:
            return createRecordEndBlock(result, paramsIterator);
        case SET_RETADDR:
            return createSetReturnAddress(result, paramsIterator);
        case SUPER_METHOD_BOUND:
            return createSuperMetodBound(result, paramsIterator);
        case UNDEF_METHOD:
            return createUndefMethod(result, paramsIterator);

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
        case RECV_POST_REQD_ARG:
            return createReceivePostReqdArg(result, paramsIterator);
        case MASGN_REQD:
            return createReqdArgMultipleAsgn(result, paramsIterator);
        case RESCUE_EQQ:
            return createRescueEQQ(result, paramsIterator);
        case MASGN_REST:
            return createRestArgMultileAsgn(result, paramsIterator);
        case CLASS_SUPER:
        case INSTANCE_SUPER:
        case UNRESOLVED_SUPER:
            return createSuperInstr(result, paramsIterator);
        case SEARCH_CONST:
            return createSearchConst(result, paramsIterator);
        case TO_ARY:
            return createToAry(result, paramsIterator);
        case YIELD:
            return createYield(result, paramsIterator);
        case ZSUPER:
            return createZSuper(result, paramsIterator);

            // Instructions that are different for versions of ruby
        case RECV_OPT_ARG:
            return createReceiveOptArg(result, paramsIterator);
        case RECV_REST_ARG:
            return createReceiveRestArg(result, paramsIterator);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private CopyInstr createCopy(final Variable result, final ParametersIterator paramsIterator) {
        final Operand s = paramsIterator.nextOperand();

        return new CopyInstr(result, s);
    }

    private GetEncodingInstr createGetEncoding(final Variable result, final ParametersIterator paramsIterator) {
        final String encodingName = paramsIterator.nextString();
        final Encoding encoding = NonIRObjectFactory.createEncoding(encodingName);

        return new GetEncodingInstr(result, encoding);
    }

    private GetGlobalVariableInstr createGetGlobalVar(Variable dest, final ParametersIterator paramsIterator) {
        final GlobalVariable gvar = (GlobalVariable) paramsIterator.next();

        return new GetGlobalVariableInstr(dest, gvar);
    }

    private GlobalIsDefinedInstr createGlobalIsDefined(final Variable result, final ParametersIterator paramsIterator) {
        final StringLiteral name = (StringLiteral) paramsIterator.next();

        return new GlobalIsDefinedInstr(result, name);
    }

    private MatchInstr createMatch(final Variable result, final ParametersIterator paramsIterator) {
        final Operand receiver = paramsIterator.nextOperand();

        return new MatchInstr(result, receiver);
    }

    private MethodLookupInstr createMethodLookup(final Variable result, final ParametersIterator paramsIterator) {
        final MethodHandle mh = (MethodHandle) paramsIterator.next();

        return new MethodLookupInstr(result, mh);
    }

    private NotInstr createNot(final Variable result, final ParametersIterator paramsIterator) {
        final Operand arg = paramsIterator.nextOperand();

        return new NotInstr(result, arg);
    }

    private ProcessModuleBodyInstr createProcessModuleBody(final Variable result, final ParametersIterator paramsIterator) {
        final Operand moduleBody = paramsIterator.nextOperand();

        return new ProcessModuleBodyInstr(result, moduleBody);
    }

    private PushBindingInstr createPushBinding(final Variable result, final ParametersIterator paramsIterator) {
        final IRScope scope = paramsIterator.nextScope();

        return new PushBindingInstr(scope);
    }

    private ReceiveRubyExceptionInstr createReceiveRubyException(final Variable result, final ParametersIterator paramsIterator) {
        return new ReceiveRubyExceptionInstr(result);
    }

    private ReceiveJRubyExceptionInstr createReceiveJRubyException(final Variable result, final ParametersIterator paramsIterator) {
        return new ReceiveJRubyExceptionInstr(result);
    }

    private ReceivePreReqdArgInstr createReceivePreReqdArg(final Variable result, final ParametersIterator paramsIterator) {
        final int argIndex = paramsIterator.nextInt();

        return new ReceivePreReqdArgInstr(result, argIndex);
    }

    private RecordEndBlockInstr createRecordEndBlock(final Variable result, final ParametersIterator paramsIterator) {
        final IRScope declaringScope = context.getCurrentScope();
        final IRClosure endBlockClosure = (IRClosure) paramsIterator.nextScope();

        return new RecordEndBlockInstr(declaringScope, endBlockClosure);
    }

    private SetReturnAddressInstr createSetReturnAddress(final Variable result, final ParametersIterator paramsIterator) {
        final Label l = (Label) paramsIterator.next();

        return new SetReturnAddressInstr(result, l);
    }

    private SuperMethodBoundInstr createSuperMetodBound(final Variable result, final ParametersIterator paramsIterator) {
        final Operand object = paramsIterator.nextOperand();

        return new SuperMethodBoundInstr(result, object);
    }

    private UndefMethodInstr createUndefMethod(final Variable result, final ParametersIterator paramsIterator) {
        final Operand methodName = paramsIterator.nextOperand();

        return new UndefMethodInstr(result, methodName);
    }

    private CallInstr createCall(final Variable result, final ParametersIterator paramsIterator) {

        final Operand receiver = paramsIterator.nextOperand();
        final String callTypString = paramsIterator.nextString();
        final CallType callType = CallType.valueOf(callTypString);

        final MethAddr methAddr = (MethAddr) paramsIterator.next();
        final Operand[] args = paramsIterator.nextOperandArray();
        final Object parameter = paramsIterator.next();
        if (parameter instanceof Operand) {
            final Operand closure = (Operand) parameter;
            return new CallInstr(callType, result, methAddr, receiver, args, closure);
        } else if (parameter instanceof String) {
            return createSpecializedCallInstr(result, receiver, callType, methAddr, args,
                    parameter);
        } else {
            final Operand closure = null;
            return new CallInstr(callType, result, methAddr, receiver, args, closure);
        }
    }

    private CallInstr createSpecializedCallInstr(final Variable result, final Operand receiver,
            final CallType callType, final MethAddr methAddr, final Operand[] args, final Object parameter) {
        final Operand closure = null;
        final CallInstr call = new CallInstr(callType, result, methAddr, receiver, args, closure);

        final String specializedInstName = (String) parameter;
        final SpecializedInstType specializedInstType = SpecializedInstType
                .valueOf(specializedInstName);

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

    private GetClassVarContainerModuleInstr createGetClassVarContainerModule(final Variable result,
            final ParametersIterator paramsIterator) {
        final Operand startingScope = paramsIterator.nextOperand();
        final Operand object = paramsIterator.nextOperand();

        return new GetClassVarContainerModuleInstr(result, startingScope, object);
    }

    private ConstMissingInstr createConstMissing(final Variable result, final ParametersIterator paramsIterator) {
        final Operand currentModule = paramsIterator.nextOperand();
        final String missingConst = paramsIterator.nextString();

        return new ConstMissingInstr(result, currentModule, missingConst);
    }

    private DefineClassInstr createDefineClass(final Variable result, final ParametersIterator paramsIterator) {
        final IRClassBody irClassBody = (IRClassBody) paramsIterator.nextScope();
        final Operand container = paramsIterator.nextOperand();
        final Operand superClass = paramsIterator.nextOperand();

        return new DefineClassInstr(result, irClassBody, container, superClass);
    }

    private DefineMetaClassInstr createDefineMetaClass(final Variable result, final ParametersIterator paramsIterator) {
        final IRModuleBody metaClassBody = (IRModuleBody) paramsIterator.nextScope();
        final Operand object = paramsIterator.nextOperand();

        return new DefineMetaClassInstr(result, object, metaClassBody);
    }

    private DefineModuleInstr createDefineModule(final Variable result, final ParametersIterator paramsIterator) {
        final IRModuleBody moduleBody = (IRModuleBody) paramsIterator.nextScope();
        final Operand container = paramsIterator.nextOperand();

        return new DefineModuleInstr(result, moduleBody, container);
    }

    private EQQInstr createEQQ(final Variable result, final ParametersIterator paramsIterator) {
        final Operand v1 = paramsIterator.nextOperand();
        final Operand v2 = paramsIterator.nextOperand();

        return new EQQInstr(result, v1, v2);
    }

    private GetInstr createGetInstr(final Operation operation, final Variable dest, final ParametersIterator paramsIterator) {
        final Operand source = paramsIterator.nextOperand();
        final String ref = paramsIterator.nextString();

        switch (operation) {
        case GET_CVAR:
            return new GetClassVariableInstr(dest, source, ref);
        case GET_FIELD:
            return new GetFieldInstr(dest, source, ref);

        default:
            throw new UnsupportedOperationException(operation.toString());
        }
    }

    private DefinedObjectNameInstr createDefinedObjectName(final Operation operation, final Variable result,
            final ParametersIterator paramsIterator) {
        final Operand object = paramsIterator.nextOperand();
        final StringLiteral name = (StringLiteral) paramsIterator.next();

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

    private InheritanceSearchConstInstr createInheritanceSearchConstInstr(final Variable result,
            final ParametersIterator paramsIterator) {
        final Operand currentModule = paramsIterator.nextOperand();
        final String constName = paramsIterator.nextString();
        final boolean noPrivateConsts = paramsIterator.nextBoolean();

        return new InheritanceSearchConstInstr(result, currentModule, constName,
                noPrivateConsts);
    }

    private BuildLambdaInstr createBuildLambda(final Variable result, final ParametersIterator paramsIterator) {
        // FIXME: This is passing null variable which is wrong...
        final WrappedIRClosure lambdaBody = new WrappedIRClosure(null, (IRClosure) paramsIterator.nextScope());
        final String fileName = paramsIterator.nextString();
        final int line = paramsIterator.nextInt();
        final ISourcePosition possition = NonIRObjectFactory.createSourcePosition(fileName, line);

        return new BuildLambdaInstr(result, lambdaBody, possition);
    }

    private LexicalSearchConstInstr createLexicalSearchConst(final Variable result, final ParametersIterator paramsIterator) {
        final Operand definingScope = paramsIterator.nextOperand();
        final String constName = paramsIterator.nextString();

        return new LexicalSearchConstInstr(result, definingScope, constName);
    }

    private LoadLocalVarInstr createLoadLocalVar(final Variable result, final ParametersIterator paramsIterator) {
        final TemporaryLocalVariable tempResult = (TemporaryLocalVariable) result;
        final IRScope scope = paramsIterator.nextScope();
        final LocalVariable lvar = (LocalVariable) paramsIterator.next();

        return new LoadLocalVarInstr(scope, tempResult, lvar);
    }

    private Match2Instr createMath2(final Variable result, final ParametersIterator paramsIterator) {
        final Operand receiver = paramsIterator.nextOperand();
        final Operand arg = paramsIterator.nextOperand();

        return new Match2Instr(result, receiver, arg);
    }

    private Match3Instr createMatch3(final Variable result, final ParametersIterator paramsIterator) {
        final Operand receiver = paramsIterator.nextOperand();
        final Operand arg = paramsIterator.nextOperand();

        return new Match3Instr(result, receiver, arg);
    }

    // FIXME?: I haven't found creation of this instruction is it obsolete?
    private ModuleVersionGuardInstr createModuleVersionGuard(final Variable result, final ParametersIterator paramsIterator) {
        final Operand candidateObj = paramsIterator.nextOperand();
        final int expectedVersion = paramsIterator.nextInt();
        @SuppressWarnings("unused")
        final String moduleName = paramsIterator.nextString();
        // FIXME?: persist module, module name is already persisted
        final RubyModule module = null;

        final Label failurePathLabel = (Label) paramsIterator.next();

        return new ModuleVersionGuardInstr(module, expectedVersion, candidateObj, failurePathLabel);
    }

    private OptArgMultipleAsgnInstr createOptArgMultipleAsgn(final Variable result, final ParametersIterator paramsIterator) {
        final Operand array = paramsIterator.nextOperand();
        final int index = paramsIterator.nextInt();
        final int minArgsLength = paramsIterator.nextInt();

        return new OptArgMultipleAsgnInstr(result, array, index, minArgsLength);
    }

    private RaiseArgumentErrorInstr createRaiseArgumentError(final Variable result, final ParametersIterator paramsIterator) {
        final int required = paramsIterator.nextInt();
        final int opt = paramsIterator.nextInt();
        final int rest = paramsIterator.nextInt();
        final int numArgs = paramsIterator.nextInt();

        return new RaiseArgumentErrorInstr(required, opt, rest, numArgs);
    }

    private ReceivePostReqdArgInstr createReceivePostReqdArg(final Variable result, final ParametersIterator paramsIterator) {
        final int index = paramsIterator.nextInt();
        final int preReqdArgsCount = paramsIterator.nextInt();
        final int postReqdArgsCount = paramsIterator.nextInt();

        return new ReceivePostReqdArgInstr(result, index, preReqdArgsCount, postReqdArgsCount);
    }

    private ReqdArgMultipleAsgnInstr createReqdArgMultipleAsgn(final Variable result, final ParametersIterator paramsIterator) {
        final Operand array = paramsIterator.nextOperand();
        final int index = paramsIterator.nextInt();
        final int preArgsCount = paramsIterator.nextInt();
        final int postArgsCount = paramsIterator.nextInt();

        return new ReqdArgMultipleAsgnInstr(result, array, preArgsCount, postArgsCount, index);
    }

    private RescueEQQInstr createRescueEQQ(final Variable result, final ParametersIterator paramsIterator) {
        final Operand v1 = paramsIterator.nextOperand();
        final Operand v2 = paramsIterator.nextOperand();

        return new RescueEQQInstr(result, v1, v2);
    }

    private RestArgMultipleAsgnInstr createRestArgMultileAsgn(final Variable result, final ParametersIterator paramsIterator) {
        final Operand array = paramsIterator.nextOperand();
        final int index = paramsIterator.nextInt();
        final int preArgsCount = paramsIterator.nextInt();
        final int postArgsCount = paramsIterator.nextInt();

        return new RestArgMultipleAsgnInstr(result, array, preArgsCount, postArgsCount, index);
    }

    private CallInstr createSuperInstr(final Variable result, final ParametersIterator paramsIterator) {
        final String superInstrTypeString = paramsIterator.nextString();
        final SuperInstrType instrType = SuperInstrType.valueOf(superInstrTypeString);

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

    private CallInstr createResolvedSuperInstr(final SuperInstrType type, final Variable result, final ParametersIterator paramsIterator) {
        final Operand definingModule = paramsIterator.nextOperand();
        final MethAddr superMeth = (MethAddr) paramsIterator.next();
        final Operand[] args = paramsIterator.nextOperandArray();
        final Operand closure = paramsIterator.nextOperand();

        switch (type) {
        case CLASS:
            return new ClassSuperInstr(result, definingModule, superMeth, args, closure);

        case INSTANCE:
            return new InstanceSuperInstr(result, definingModule, superMeth, args, closure);

        default:
            throw new UnsupportedOperationException(type.toString());
        }
    }

    private UnresolvedSuperInstr createUnresolvedSuperInstr(final Variable result, final ParametersIterator paramsIterator) {
        final Operand receiver = paramsIterator.nextOperand();
        final Operand[] args = paramsIterator.nextOperandArray();

        Operand closure = null;
        if(paramsIterator.hasNext()) {
            closure = paramsIterator.nextOperand();
        }

        return new UnresolvedSuperInstr(result, receiver, args, closure);
    }

    private SearchConstInstr createSearchConst(final Variable result, final ParametersIterator paramsIterator) {
        final String constName = paramsIterator.nextString();
        final Operand startingScope = paramsIterator.nextOperand();
        final boolean noPrivateConsts = paramsIterator.nextBoolean();

        return new SearchConstInstr(result, constName, startingScope,
                noPrivateConsts);
    }

    private ToAryInstr createToAry(final Variable result, final ParametersIterator paramsIterator) {
        final Operand array = paramsIterator.nextOperand();
        return new ToAryInstr(result, array);
    }

    private YieldInstr createYield(final Variable result, final ParametersIterator paramsIterator) {
        final Operand block = paramsIterator.nextOperand();
        final Operand arg = paramsIterator.nextOperand();
        final boolean unwrapArray = paramsIterator.nextBoolean();

        return new YieldInstr(result, block, arg, unwrapArray);
    }

    private ZSuperInstr createZSuper(final Variable result, final ParametersIterator paramsIterator) {
        final Operand receiver = paramsIterator.nextOperand();

        // Closure cannot be null here?
        Operand closure = null;
        if(paramsIterator.hasNext()) {
            closure = paramsIterator.nextOperand();
        }

        return new ZSuperInstr(result, receiver, closure);
    }


    private ReceiveOptArgInstr createReceiveOptArg(Variable result, ParametersIterator iter) {
        return new ReceiveOptArgInstr(result, iter.nextInt(), iter.nextInt(), iter.nextInt());
    }

    private ReceiveRestArgInstr createReceiveRestArg(final Variable result, final ParametersIterator iter) {
        return new ReceiveRestArgInstr(result, iter.nextInt(), iter.nextInt());
    }
}
