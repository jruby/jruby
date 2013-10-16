package org.jruby.ir.persistence;

import org.jruby.ir.IRBuilder;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRScope;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArgsArrayArityInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.instructions.defined.ClassVarIsDefinedInstr;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;

public class IRInstructionBuilder {

    protected static final Operand[] NO_ARGS = new Operand[]{};
    private final IRManager manager;
    private IRBuilder irBuilder;
    private IROperandFactory irOperandBuilder;

    public IRInstructionBuilder(IRManager manager) {
        this.manager = manager;
        this.irBuilder = IRBuilder.createIRBuilder(manager);
        irOperandBuilder = new IROperandFactory();
    }

    public Operand buildAssignment(String instrString, String rvalue, IRScope scope) {
        return null;
    }

    public Operand buildLabel(String instrString, IRScope scope) {
        return null;
    }

    public void buildInstrWithAssignment(InstrInfo instrInfo, IRScope scope) {
        switch (instrInfo.operation) {
            case BACKREF_IS_MATCH_DATA:
                buildBackrefIsMatchData(instrInfo, scope);
                return;
            case BLOCK_GIVEN:
                buildBlockGiven(instrInfo, scope);
                return;
            case CALL:
                buildCall(instrInfo, scope);
                return;
            case CLASS_VAR_IS_DEFINED:
                buildClassVarIsDefined(instrInfo, scope);
                return;
            case CLASS_VAR_MODULE:
                buildClassVarModule(instrInfo, scope);
                return;
            case CONST_MISSING:
            case COPY:
            case DEF_CLASS:
            case DEF_META_CLASS:
            case DEFINED_CONSTANT_OR_METHOD:
            case ENSURE_RUBY_ARRAY:
            case EQQ:
            case GET_BACKREF:
            case GET_CVAR:
            case GET_ENCODING:
            case GET_ERROR_INFO:
            case GET_FIELD:
            case GET_GLOBAL_VAR:
            case GLOBAL_IS_DEFINED:
            case HAS_INSTANCE_VAR:
            case INHERITANCE_SEARCH_CONST:
            case IS_METHOD_BOUND:
            case LAMBDA:
            case LEXICAL_SEARCH_CONST:
            case MASGN_OPT:
            case MASGN_REQD:
            case MASGN_REST:
            case MATCH:
            case MATCH2:
            case MATCH3:
            case METHOD_DEFINED:
            case METHOD_IS_PUBLIC:
            case METHOD_LOOKUP:
            case NOT:
            case PROCESS_MODULE_BODY:
            case RECV_CLOSURE:
            case RECV_EXCEPTION:
            case RECV_OPT_ARG:
            case RECV_POST_REQD_ARG:
            case RECV_PRE_REQD_ARG:
            case RECV_REST_ARG:
            case RECV_SELF:
            case RESCUE_EQQ:
            case SEARCH_CONST:
            case SET_RETADDR:
            case SUPER:
            case SUPER_METHOD_BOUND:
            case TO_ARY:
            case UNDEF_METHOD:
            case YIELD:
            case ZSUPER:

        }
    }

    private void buildBackrefIsMatchData(InstrInfo instrInfo, IRScope scope) {
        TemporaryVariable newTemporaryVariable = scope.getNewTemporaryVariable(); // or
// lvalue?
        scope.addInstr(new BackrefIsMatchDataInstr(newTemporaryVariable));
    }

    private void buildBlockGiven(InstrInfo instrInfo, IRScope scope) {
        TemporaryVariable newTemporaryVariable = scope.getNewTemporaryVariable(); // or
// lvalue?
        scope.addInstr(new BlockGivenInstr(newTemporaryVariable, scope.getImplicitBlockArg()));
    }

    private void buildCall(InstrInfo instrInfo, IRScope s) {
        MethAddr methAddr = (MethAddr) instrInfo.operands[0];
        Operand receiver = instrInfo.operands[1];
        // FIXME: what to do with args?
        Operand[] args = NO_ARGS;
        // FIXME: what to do with block?
        Operand block = null;
        Variable callResult = s.getNewTemporaryVariable(); // or lvalue?
        Instr callInstr = CallInstr.create(callResult, methAddr, receiver, NO_ARGS, block);
    }

    private void buildClassVarIsDefined(InstrInfo instrInfo, IRScope scope) {
        Variable tmp = scope.getNewTemporaryVariable(); // or lvalue?
        StringLiteral name = (StringLiteral) instrInfo.operands[1];
        Operand cm = irBuilder.classVarDefinitionContainer(scope);
        scope.addInstr(new ClassVarIsDefinedInstr(tmp, cm, name));
    }

    private void buildClassVarModule(InstrInfo instrInfo, IRScope scope) {
        Variable tmp = scope.getNewTemporaryVariable(); // or lvalue?
        Operand object = instrInfo.operands[1];
        scope.addInstr(new GetClassVarContainerModuleInstr(tmp, scope.getCurrentScopeVariable(),
                object));
    }

    public void buildInstrWithoutAssignment(InstrInfo instrInfo, IRScope scope) {
        switch (instrInfo.operation) {
            case ALIAS:
                buildAlias(instrInfo, scope);
                return;
            case ATTR_ASSIGN:
                buildAttrAssign(instrInfo, scope);
                return;
            case B_FALSE:
                buidBEq(instrInfo, manager.getFalse(), scope);
                return;
            case B_NIL:
                buidBEq(instrInfo, manager.getNil(), scope);
                return;
            case B_TRUE:
                buidBEq(instrInfo, manager.getTrue(), scope);
                return;
            case B_UNDEF:
                buidBEq(instrInfo, UndefinedValue.UNDEFINED, scope);
                return;
            case BEQ:
                buidBEq(instrInfo, instrInfo.operands[1], scope);
                return;
            case BNE:
                buildBNE(instrInfo, scope);
                return;// what's difference with BEQ?
            case BREAK:
                buildBreak(instrInfo, scope);
                return;
            case CHECK_ARGS_ARRAY_ARITY:
                buildCheckArgsArrayArity(instrInfo, scope);
                return;
            case CHECK_ARITY:
                buildCheckArity(instrInfo, scope);
                return;
            case DEF_CLASS_METH:
                buildClassMeth(instrInfo, scope);
                return;
            case DEF_INST_METH:
            case EXC_REGION_END:
            case EXC_REGION_START:
            case GVAR_ALIAS:
            case JUMP:
            case JUMP_INDIRECT:
            case LINE_NUM:
            case MODULE_GUARD:
            case NOP:
            case POP_BINDING:
            case PUSH_BINDING:
            case PUT_CONST:
            case PUT_CVAR:
            case PUT_FIELD:
            case PUT_GLOBAL_VAR:
            case RAISE_ARGUMENT_ERROR:
            case RECORD_END_BLOCK:
            case RESTORE_ERROR_INFO:
            case RETURN:
            case SET_WITHIN_DEFINED:
            case THREAD_POLL:
            case THROW:

            // what to do with this?

            // or create label separatedly
            case LABEL:

            // used in AddLocalVarLoadStoreInstructions optimization pass
            case BINDING_STORE:
            case BINDING_LOAD:

            // what is this? is it unused?
            case GET_OBJECT:
            case IS_TRUE:
            case POP_FRAME: // usage found, but no class
            case PUSH_FRAME:
            case PUT_ARRAY:

            // Unused:
            case BOX_VALUE:
            case UNBOX_VALUE:

            default:
                return;
        }
    }

    private void buildAlias(InstrInfo instrInfo, IRScope scope) {
        Variable receiver = scope.getSelf(); // or something different?
        Operand newName = instrInfo.operands[1];
        Operand oldName = instrInfo.operands[2];
        scope.addInstr(new AliasInstr(receiver, newName, oldName));
    }

    private void buildAttrAssign(InstrInfo instrInfo, IRScope scope) {
        MethAddr methodAddr = (MethAddr) instrInfo.operands[0];
        Operand receiver = instrInfo.operands[1];
        // FIXME: what to do with args?
        Operand[] args = NO_ARGS;
        scope.addInstr(new AttrAssignInstr(receiver, methodAddr, args));
    }

    private void buidBEq(InstrInfo instrInfo, Operand literal, IRScope scope) {
        LocalVariable var = (LocalVariable) instrInfo.operands[0]; // or
// something different? like scope.getLocalVariable(variableName, 0)
        Label label = (Label) instrInfo.operands[2];
        scope.addInstr(BEQInstr.create(var, literal, label));
    }

    private void buildBNE(InstrInfo instrInfo, IRScope scope) {
        LocalVariable var = (LocalVariable) instrInfo.operands[0]; // or
// something different? like scope.getLocalVariable(variableName, 0)
        Operand v2 = instrInfo.operands[1];
        Label label = (Label) instrInfo.operands[2];
        scope.addInstr(BEQInstr.create(var, v2, label));
    }

    private void buildBreak(InstrInfo instrInfo, IRScope scope) {
        Operand rv = instrInfo.operands[0];
        scope.addInstr(new BreakInstr(rv, scope.getLexicalParent()));
    }

    private void buildCheckArgsArrayArity(InstrInfo instrInfo, IRScope scope) {
        Operand[] operands = instrInfo.operands;
        Operand operand = operands[0];
        // TODO: Something other than toString here?
        int required = Integer.parseInt(operands[1].toString());
        int opt = Integer.parseInt(operands[2].toString());
        int rest = Integer.parseInt(operands[3].toString());
        scope.addInstr(new CheckArgsArrayArityInstr(operand, required, opt, rest));
    }

    private void buildCheckArity(InstrInfo instrInfo, IRScope scope) {
        Operand[] operands = instrInfo.operands;
        // TODO: Something other than toString here?
        int required = Integer.parseInt(operands[0].toString());
        int opt = Integer.parseInt(operands[1].toString());
        int rest = Integer.parseInt(operands[1].toString());
        scope.addInstr(new CheckArityInstr(required, opt, rest));
    }

    private void buildClassMeth(InstrInfo instrInfo, IRScope scope) {
        // TODO: Something other than toString here?
        String containerString = instrInfo.operands[0].toString();
        String methodName = instrInfo.operands[0].toString();
        String methodFileName = instrInfo.operands[0].toString();
    }
}
