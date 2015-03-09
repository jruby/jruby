package org.jruby.ir.persistence;

import org.jcodings.Encoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.*;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.operands.*;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.runtime.CallType;

/**
 *
 * @author enebo
 */
class InstrDecoderMap implements IRPersistenceValues {
    private final IRReaderDecoder d;
    private final IRManager manager;

    public InstrDecoderMap(IRManager manager, IRReaderDecoder decoder) {
        this.manager = manager;
        this.d = decoder;
    }

    public Instr decode(Operation operation) {
        Instr instr = null;
        try {
            instr = decodeInner(operation);
        } catch (Exception e) {
            System.out.println("Got: " + instr + ", :" + d.getCurrentScope().getClass().getName());
            e.printStackTrace();
        }
        return instr;
    }

    public Instr decodeInner(Operation operation) {
        switch(operation) {
            case ALIAS: return new AliasInstr(d.decodeOperand(), d.decodeOperand());
            case ATTR_ASSIGN: return decodeAttrAssignInstr();
            case BEQ: return BEQInstr.create(d.decodeOperand(), d.decodeOperand(), (Label) d.decodeOperand());
            case BINDING_LOAD: return new LoadLocalVarInstr(d.decodeScope(), (TemporaryLocalVariable) d.decodeOperand(), (LocalVariable) d.decodeOperand());
            case BINDING_STORE:return new StoreLocalVarInstr(d.decodeOperand(), d.decodeScope(), (LocalVariable) d.decodeOperand());
            case BLOCK_GIVEN: return new BlockGivenInstr(d.decodeVariable(), d.decodeOperand());
            case BNE: return new BNEInstr(d.decodeOperand(), d.decodeOperand(), (Label) d.decodeOperand());
            case BREAK: return new BreakInstr(d.decodeOperand(), d.decodeString());
            case B_FALSE: return createBFalse();
            case B_NIL: return createBNil();
            case B_TRUE: return createBTrue();
            case B_UNDEF: return createBUndef();
            case CALL: return decodeCall();
            case CHECK_ARGS_ARRAY_ARITY: return new CheckArgsArrayArityInstr(d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case CHECK_ARITY: return new CheckArityInstr(d.decodeInt(), d.decodeInt(), d.decodeInt(), d.decodeBoolean(), d.decodeInt());
            case CLASS_VAR_MODULE: return new GetClassVarContainerModuleInstr(d.decodeVariable(), d.decodeOperand(), d.decodeVariable());
            case CONST_MISSING: return decodeConstMissingInstr();
            // SSS FIXME: TODO
            // case BUILD_COMPOUND_INSTR: return decodeBuildCompoundStringInstr();
            // case BUILD_DREGEXP: return decodeBuildDynRegExpInstr();
            // case BUILD_RANGE: return new Range(d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
            case COPY: return decodeCopy();
            case DEF_CLASS: return new DefineClassInstr((d.decodeVariable()), (IRClassBody) d.decodeScope(), d.decodeOperand(), d.decodeOperand());
            case DEF_CLASS_METH: return new DefineClassMethodInstr(d.decodeOperand(), (IRMethod) d.decodeScope());
            case DEF_INST_METH: return new DefineInstanceMethodInstr((IRMethod) d.decodeScope());
            case DEF_META_CLASS: return new DefineMetaClassInstr(d.decodeVariable(), d.decodeOperand(), (IRModuleBody) d.decodeScope());
            case DEF_MODULE: return new DefineModuleInstr(d.decodeVariable(), (IRModuleBody) d.decodeScope(), d.decodeOperand());
            case EQQ: return new EQQInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
            case EXC_REGION_END: return new ExceptionRegionEndMarkerInstr();
            case EXC_REGION_START: return new ExceptionRegionStartMarkerInstr((Label) d.decodeOperand());
            case GET_CVAR: return new GetClassVariableInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
                // FIXME: Encoding load is likely wrong here but likely will work :)
            case GET_ENCODING: return new GetEncodingInstr(d.decodeVariable(), Encoding.load(d.decodeString()));
            case GET_ERROR_INFO: return new GetErrorInfoInstr(d.decodeVariable());
            case GET_FIELD: return new GetFieldInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
            case GET_GLOBAL_VAR: return new GetGlobalVariableInstr(d.decodeVariable(), d.decodeString());
            case GVAR_ALIAS: return new GVarAliasInstr(d.decodeOperand(), d.decodeOperand());
            case INHERITANCE_SEARCH_CONST: return new InheritanceSearchConstInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString(), d.decodeBoolean());
            case JUMP: return new JumpInstr((Label) d.decodeOperand());
            case LABEL: return new LabelInstr((Label) d.decodeOperand());
            case LAMBDA: return decodeLambda();
            case LEXICAL_SEARCH_CONST: return new LexicalSearchConstInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
            case LINE_NUM: return decodeLineNumber();
            case MASGN_OPT: return new OptArgMultipleAsgnInstr(d.decodeVariable(), d.decodeOperand(), d.decodeInt(), d.decodeInt());
            case MASGN_REQD: return new ReqdArgMultipleAsgnInstr(d.decodeVariable(), d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case MASGN_REST: return new RestArgMultipleAsgnInstr(d.decodeVariable(), d.decodeOperand(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case MATCH: return new MatchInstr(d.decodeVariable(), d.decodeOperand());
            case MATCH2: return new Match2Instr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
            case MATCH3: return new Match3Instr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
            case NONLOCAL_RETURN: return new NonlocalReturnInstr(d.decodeOperand(), d.decodeString());
            case NOP: return NopInstr.NOP;
            case NORESULT_CALL: return decodeNoResultCall();
            case POP_BINDING: return new PopBindingInstr();
            case POP_FRAME: return new PopFrameInstr();
            case PROCESS_MODULE_BODY: return new ProcessModuleBodyInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
            case PUSH_BINDING: return new PushBindingInstr();
            case PUSH_FRAME: return decodeFrame();
            case PUT_CONST: return new PutConstInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
            case PUT_CVAR: return new PutClassVariableInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
            case PUT_FIELD: return new PutFieldInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
            case PUT_GLOBAL_VAR: return new PutGlobalVarInstr(d.decodeString(), d.decodeOperand());
            case RAISE_ARGUMENT_ERROR: return new RaiseArgumentErrorInstr(d.decodeInt(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case RECORD_END_BLOCK: return new RecordEndBlockInstr(d.decodeScope(), (WrappedIRClosure) d.decodeOperand());
            case REIFY_CLOSURE: return new ReifyClosureInstr(d.decodeVariable(), d.decodeVariable());
            case RECV_RUBY_EXC: return decodeReceiveRubyException();
            case RECV_JRUBY_EXC: return decodeReceiveJRubyException();
            case RECV_KW_ARG: return new ReceiveKeywordArgInstr(d.decodeVariable(), d.decodeString(), d.decodeInt());
            case RECV_KW_REST_ARG: return new ReceiveKeywordRestArgInstr(d.decodeVariable(), d.decodeInt());
            case RECV_OPT_ARG: return new ReceiveOptArgInstr(d.decodeVariable(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case RECV_POST_REQD_ARG: return new ReceivePostReqdArgInstr(d.decodeVariable(), d.decodeInt(), d.decodeInt(), d.decodeInt());
            case RECV_PRE_REQD_ARG: return new ReceivePreReqdArgInstr(d.decodeVariable(), d.decodeInt());
            case RECV_REST_ARG: return decodeReceiveRestArgInstr();
            case RECV_SELF: return new ReceiveSelfInstr(d.decodeVariable());
            case RESCUE_EQQ: return new RescueEQQInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand());
            case RESTORE_ERROR_INFO: return new RestoreErrorInfoInstr(d.decodeOperand());
            case RETURN: return new ReturnInstr(d.decodeOperand());
            case RUNTIME_HELPER: return decodeRuntimeHelperCall();
            case SEARCH_CONST: return decodeSearchConst();
            case CLASS_SUPER: return decodeSuperInstr(operation);
            case INSTANCE_SUPER: return decodeSuperInstr(operation);
            case UNRESOLVED_SUPER: return decodeUnresolvedSuperInstr();
            case THREAD_POLL: return new ThreadPollInstr(d.decodeBoolean());
            case THROW: return new ThrowExceptionInstr(d.decodeOperand());
            case TO_ARY: return new ToAryInstr(d.decodeVariable(), d.decodeOperand());
            case UNDEF_METHOD: return new UndefMethodInstr(d.decodeVariable(), d.decodeOperand());
            case YIELD: return new YieldInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
            case ZSUPER: return decodeZSuperInstr();
        }

        throw new IllegalArgumentException("Whoa bro: " + operation);
    }

    private Instr decodeAttrAssignInstr() {
        Operand op = d.decodeOperand();
        String methAddr = d.decodeString();

        int length = d.decodeInt();
        Operand[] args = new Operand[length];

        for (int i = 0; i < length; i++) {
            args[i] = d.decodeOperand();
        }

        return AttrAssignInstr.create(op, methAddr, args);
    }

    private Instr decodeCall() {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  "+ result);
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, calltype(ord):  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  "+ methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return CallInstr.create(d.getCurrentScope(), CallType.fromOrdinal(callTypeOrdinal), result, methAddr, receiver, args, closure);
    }

    private Instr decodeFrame() {
        String methAddr = d.decodeString();
        return new PushFrameInstr(methAddr);
    }

    private Instr decodeConstMissingInstr() {
        return new ConstMissingInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
    }

    private Instr decodeLambda() {
        Variable v = d.decodeVariable();
        WrappedIRClosure c = (WrappedIRClosure) d.decodeOperand();

        return new BuildLambdaInstr(v, c, new SimpleSourcePosition(d.decodeString(), d.decodeInt()));
    }

    private Instr decodeNoResultCall() {
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, ordinal:  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  "+ methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return NoResultCallInstr.create(CallType.fromOrdinal(callTypeOrdinal), methAddr, receiver, args, closure);
    }

    private Instr decodeCopy() {
        return new CopyInstr(d.decodeVariable(), d.decodeOperand());
    }

/**
    private Instr decodeBuildCompoundStringInstr() {
        String encodingString = d.decodeString();

        if (encodingString.equals("")) return new BuildCompoundStringInstr(d.decodeVariable(), d.decodeOperandList(), ASCIIEncoding.INSTANCE);

        // FIXME: yuck
        return new BuildCompoundStringInstr(d.decodeVariable(), d.decodeOperandList(), NonIRObjectFactory.createEncoding(encodingString));
    }
**/

    private Instr decodeLineNumber() {
        return new LineNumberInstr(d.decodeInt());
    }

    private Instr decodeReceiveRestArgInstr() {
        return new ReceiveRestArgInstr(d.decodeVariable(), d.decodeInt(), d.decodeInt());
    }

    private Instr decodeRuntimeHelperCall() {
        Variable result = d.decodeVariable();
        String name = ""; // FIXME:
        int size = d.decodeInt();
        Operand[] args = new Operand[size];

        for (int i = 0; i < size; i++) {
            args[i] = d.decodeOperand();
        }

        // FIXME: fix up next persistence cycle
        return new RuntimeHelperCall(result, null, args);
    }

    private Instr decodeSearchConst() {
        Variable result = d.decodeVariable();
        String constName = d.decodeString();
        Operand startScope = d.decodeOperand();
        boolean noPrivateConst = d.decodeBoolean();

        return new SearchConstInstr(result, constName, startScope, noPrivateConst);
    }

    private Instr createBFalse() {
        return new BFalseInstr(d.decodeOperand(), (Label) d.decodeOperand());
    }

    private Instr createBTrue() {
        return new BTrueInstr(d.decodeOperand(), (Label) d.decodeOperand());
    }

    private Instr createBNil() {
        return new BNilInstr(d.decodeOperand(), (Label) d.decodeOperand());
    }

    private Instr createBUndef() {
        return new BUndefInstr(d.decodeOperand(), (Label) d.decodeOperand());
    }

    private Instr decodeReceiveRubyException() {
        return new ReceiveRubyExceptionInstr(d.decodeVariable());
    }

    private Instr decodeReceiveJRubyException() {
        return new ReceiveJRubyExceptionInstr(d.decodeVariable());
    }

    private Instr decodeSuperInstr(Operation operation) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, result:  "+ result);
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, calltype(ord):  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, methaddr:  "+ methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        if (operation == Operation.CLASS_SUPER) return new ClassSuperInstr(result, receiver, methAddr, args, closure);

        return new InstanceSuperInstr(result, receiver, methAddr, args, closure);
    }

    public Instr decodeUnresolvedSuperInstr() {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding un. super");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding un. super, result:  "+ result);
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding un. super, calltype(ord):  "+ callTypeOrdinal);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return new UnresolvedSuperInstr(result, receiver, args, closure);
    }

    public Instr decodeZSuperInstr() {
        Variable result = d.decodeVariable();
        Operand receiver = d.decodeOperand();
        boolean hasClosure = d.decodeBoolean();
        Operand closure = hasClosure ? d.decodeOperand() : null;

        int argsLength = d.decodeInt();
        // if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosure);
        Operand[] args = new Operand[argsLength];
        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        return new ZSuperInstr(result, receiver, args, closure);
    }
 }
