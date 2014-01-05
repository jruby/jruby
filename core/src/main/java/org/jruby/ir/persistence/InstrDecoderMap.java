package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.BUndefInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
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
import org.jruby.ir.instructions.RaiseArgumentErrorInstr;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveExceptionInstr;
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
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.runtime.CallType;

/**
 *
 * @author enebo
 */
class InstrDecoderMap implements IRPersistenceValues {
    private static final Operation[] operations = Operation.values();
    
    private final IRReaderDecoder d;
    private final IRManager manager;

    public InstrDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.d = decoder;
    }

    public Instr decode(Operation operation) {
        switch(operation) {
            case ALIAS: return new AliasInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case ATTR_ASSIGN: return decodeAttrAssignInstr();
            case BACKREF_IS_MATCH_DATA: return new BackrefIsMatchDataInstr((Variable) d.decodeOperand());
            case BEQ: return BEQInstr.create(d.decodeOperand(), d.decodeOperand(), (Label) d.decodeOperand());
            case BINDING_LOAD: return new LoadLocalVarInstr(d.decodeScope(), (TemporaryVariable) d.decodeOperand(), (LocalVariable) d.decodeOperand());
            case BINDING_STORE:return new StoreLocalVarInstr(d.decodeOperand(), d.decodeScope(), (LocalVariable) d.decodeOperand());
            case BLOCK_GIVEN: return new BlockGivenInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case BNE: return new BNEInstr(d.decodeOperand(), d.decodeOperand(), (Label) d.decodeOperand());
            case BREAK: return new BreakInstr(d.decodeOperand(), d.decodeScope());
            case B_FALSE: return new BFalseInstr(d.decodeOperand(), (Label) d.decodeOperand());
            case B_NIL: return new BNEInstr(d.decodeOperand(), d.decodeOperand(), (Label) d.decodeOperand());
            case B_TRUE: return new BTrueInstr(d.decodeOperand(), (Label) d.decodeOperand());
            case B_UNDEF: return new BUndefInstr(d.decodeOperand(), (Label) d.decodeOperand());
            case CALL: return decodeCall();
            case CHECK_ARGS_ARRAY_ARITY: return new CheckArgsArrayArityInstr(d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case CHECK_ARITY: return new CheckArityInstr(d.decodeInt(), d.decodeInt(), d.decodeInt());
            case CLASS_VAR_IS_DEFINED: return new ClassVarIsDefinedInstr((Variable) d.decodeOperand(), d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case CLASS_VAR_MODULE: return new GetClassVarContainerModuleInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case CONST_MISSING: return new ConstMissingInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeString());
            case COPY: return new CopyInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case DEFINED_CONSTANT_OR_METHOD:         return new GetDefinedConstantOrMethodInstr((Variable) d.decodeOperand(), d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case DEF_CLASS: return new DefineClassInstr(((Variable) d.decodeOperand()), (IRClassBody) d.decodeScope(), d.decodeOperand(), d.decodeOperand());
            case DEF_CLASS_METH: return new DefineClassMethodInstr(d.decodeOperand(), (IRMethod) d.decodeScope());
            case DEF_INST_METH: return new DefineInstanceMethodInstr(d.decodeOperand(), (IRMethod) d.decodeScope());
            case DEF_META_CLASS: return new DefineInstanceMethodInstr(d.decodeOperand(), (IRMethod) d.decodeScope());
            case DEF_MODULE: return new DefineModuleInstr((Variable) d.decodeOperand(), (IRModuleBody) d.decodeScope(), d.decodeOperand());
            case EQQ: return new EQQInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case EXC_REGION_END: return new ExceptionRegionEndMarkerInstr();
            case EXC_REGION_START: return new ExceptionRegionStartMarkerInstr((Label) d.decodeOperand(), (Label) d.decodeOperand(), (Label) d.decodeOperand());
            case GET_BACKREF: return new GetBackrefInstr((Variable) d.decodeOperand());
            case GET_CVAR: return new GetClassVariableInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeString());
                // FIXME: Encoding load is likely wrong here but likely will work :)
            case GET_ENCODING: return new GetEncodingInstr((Variable) d.decodeOperand(), Encoding.load(d.decodeString()));
            case GET_ERROR_INFO: return new GetErrorInfoInstr((Variable) d.decodeOperand());
            case GET_FIELD: return new GetFieldInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeString());
            case GET_GLOBAL_VAR: return new GetGlobalVariableInstr((Variable) d.decodeOperand(), d.decodeString());
            case GLOBAL_IS_DEFINED: return new GlobalIsDefinedInstr((Variable) d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case GVAR_ALIAS: return new GVarAliasInstr(d.decodeOperand(), d.decodeOperand());
            case HAS_INSTANCE_VAR: return new HasInstanceVarInstr((Variable) d.decodeOperand(), d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case INHERITANCE_SEARCH_CONST: return new InheritanceSearchConstInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeString(), d.decodeBoolean());
            case IS_METHOD_BOUND: return new IsMethodBoundInstr((Variable) d.decodeOperand(), d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case JUMP: return new JumpInstr((Label) d.decodeOperand());
            case JUMP_INDIRECT: return new JumpIndirectInstr((Variable) d.decodeOperand());
            case LABEL: return new LabelInstr((Label) d.decodeOperand());
            case LAMBDA: return decodeLambda();
            case LEXICAL_SEARCH_CONST: return new LexicalSearchConstInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeString());
            case LINE_NUM: return new LineNumberInstr(d.decodeScope(), d.decodeInt());
            case MASGN_OPT: return new OptArgMultipleAsgnInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeInt(), d.decodeInt());
            case MASGN_REQD: return new ReqdArgMultipleAsgnInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case MASGN_REST: return new RestArgMultipleAsgnInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case MATCH: return new MatchInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case MATCH2: return new Match2Instr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case MATCH3: return new Match3Instr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case METHOD_DEFINED: return new MethodDefinedInstr((Variable) d.decodeOperand(), d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case METHOD_IS_PUBLIC: return new MethodIsPublicInstr((Variable) d.decodeOperand(), d.decodeOperand(), (StringLiteral) d.decodeOperand());
            case METHOD_LOOKUP: return new MethodLookupInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case NONLOCAL_RETURN: return new NonlocalReturnInstr(d.decodeOperand(), (IRMethod) d.decodeScope());
            case NOP: return NopInstr.NOP;
            case NORESULT_CALL: return decodeNoResultCall();
            case NOT: return new NotInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case POP_BINDING: return new PopBindingInstr();
            case POP_FRAME: return new PopFrameInstr();
            case PROCESS_MODULE_BODY: return new ProcessModuleBodyInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case PUSH_BINDING: return new PushBindingInstr(d.decodeScope());
            case PUSH_FRAME: return new PushFrameInstr();
            case PUT_CONST: return new PutConstInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
            case PUT_CVAR: return new PutClassVariableInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
            case PUT_FIELD: return new PutFieldInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
            case PUT_GLOBAL_VAR: return new PutGlobalVarInstr(d.decodeString(), d.decodeOperand());
            case RAISE_ARGUMENT_ERROR: return new RaiseArgumentErrorInstr(d.decodeInt(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case RECORD_END_BLOCK: return new RecordEndBlockInstr(d.decodeScope(), (IRClosure) d.decodeScope());
            case RECV_CLOSURE: return new ReceiveClosureInstr((Variable) d.decodeOperand());
            case RECV_EXCEPTION: return new ReceiveExceptionInstr((Variable) d.decodeOperand(), d.decodeBoolean());
            case RECV_KW_ARG: return new ReceiveKeywordArgInstr((Variable) d.decodeOperand(), d.decodeInt());
            case RECV_KW_REST_ARG: return new ReceiveKeywordRestArgInstr((Variable) d.decodeOperand(), d.decodeInt());
            case RECV_OPT_ARG: return new ReceivePostReqdArgInstr((Variable) d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case RECV_POST_REQD_ARG: return new ReceivePostReqdArgInstr((Variable) d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case RECV_PRE_REQD_ARG: return new ReceivePreReqdArgInstr((Variable) d.decodeOperand(), d.decodeInt());
            case RECV_REST_ARG: return new ReceiveRestArgInstr((Variable) d.decodeOperand(), d.decodeInt(), d.decodeInt());
            case RECV_SELF: return new ReceiveSelfInstr((Variable) d.decodeOperand());
            case RESCUE_EQQ: return new RescueEQQInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
            case RESTORE_ERROR_INFO: return new RestoreErrorInfoInstr(d.decodeOperand());
            case RETURN: return new ReturnInstr(d.decodeOperand());
            case SEARCH_CONST: return new SearchConstInstr((Variable) d.decodeOperand(), d.decodeString(), d.decodeOperand(), d.decodeBoolean());
            case SET_RETADDR: return new SetReturnAddressInstr((Variable) d.decodeOperand(), (Label) d.decodeOperand());
            case CLASS_SUPER: return new ClassSuperInstr((Variable) d.decodeOperand(), d.decodeOperand(), (MethAddr) d.decodeOperand(), d.decodeOperandArray(), d.decodeOperand());
            case INSTANCE_SUPER: return new InstanceSuperInstr((Variable) d.decodeOperand(), d.decodeOperand(), (MethAddr) d.decodeOperand(), d.decodeOperandArray(), d.decodeOperand());
            case UNRESOLVED_SUPER: return new UnresolvedSuperInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperandArray(), d.decodeOperand());
            case SUPER_METHOD_BOUND: return new SuperMethodBoundInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case THREAD_POLL: return new ThreadPollInstr(d.decodeBoolean());
            case THROW: return new ThrowExceptionInstr(d.decodeOperand());
            case TO_ARY: return new ToAryInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case UNDEF_METHOD: return new UndefMethodInstr((Variable) d.decodeOperand(), d.decodeOperand());
            case YIELD: return new YieldInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
            case ZSUPER: return new ZSuperInstr((Variable) d.decodeOperand(), d.decodeOperand(), d.decodeOperand());
        }
        
        return null;
    }

    public Operation decodeOperationType(int ordinal) {
        if (ordinal >= operations.length) throw new IllegalArgumentException("Invalid Operation Type: " + ordinal);
        
        return operations[ordinal];
    }

    private Instr decodeAttrAssignInstr() {
        Operand op = d.decodeOperand();
        MethAddr methAddr = (MethAddr) d.decodeOperand();
        
        int length = d.decodeInt();
        Operand[] args = new Operand[length];
        
        for (int i = 0; i < length; i++) {
            args[i] = d.decodeOperand();
        }
        
        return new AttrAssignInstr(op, methAddr, args);
    }

    private Instr decodeCall() {
        Variable result = (Variable) d.decodeOperand();
        Fixnum callTypeOrdinal = (Fixnum) d.decodeOperand();
        MethAddr methAddr = (MethAddr) d.decodeOperand();
        Operand receiver = d.decodeOperand();
        Operand[] args = d.decodeOperandArray();
        Operand closure = d.decodeOperand();
        
        return CallInstr.create(CallType.fromOrdinal((int) callTypeOrdinal.value), result, methAddr, receiver, args, closure);
    }

    private Instr decodeLambda() {
        Variable v = (Variable) d.decodeOperand();
        WrappedIRClosure c = (WrappedIRClosure) d.decodeOperand();
        StringLiteral l = (StringLiteral) d.decodeOperand();
        Fixnum f = (Fixnum) d.decodeOperand();
        
        return new BuildLambdaInstr(v, c, new SimpleSourcePosition(l.getString(), (int) f.value));
    }

    private Instr decodeNoResultCall() {
        Fixnum callTypeOrdinal = (Fixnum) d.decodeOperand();
        MethAddr methAddr = (MethAddr) d.decodeOperand();
        Operand receiver = d.decodeOperand();
        Operand[] args = d.decodeOperandArray();
        Operand closure = d.decodeOperand();
        
        return new NoResultCallInstr(Operation.NORESULT_CALL, CallType.fromOrdinal((int) callTypeOrdinal.value), methAddr, receiver, args, closure);        
    }
 }
