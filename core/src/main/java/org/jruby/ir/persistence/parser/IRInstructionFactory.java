package org.jruby.ir.persistence.parser;

import org.jruby.ir.IRManager;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.CallType;

public enum IRInstructionFactory {
    INSTANCE;
    
    public Operation createOperation(String name) {
        return Operation.valueOf(name.toUpperCase());
    }
    
    
    
    public LabelInstr createLabel(String l) {
        Label label = new Label(l);
        return new LabelInstr(label);
    }
    
    public JumpInstr createJump(Label target) {
        return new JumpInstr(target);
    }
    
    public Instr createInstrWithNoParam(Operation operation) {
        switch (operation) {
        case THREAD_POLL:
            return createTreadPoll();

        default:
            throw new UnsupportedOperationException();
        }
    }

    public Instr createTreadPoll() {
        return new ThreadPollInstr();
    }
    
    public Instr createInstrWithParams(Operation operation, Object[] params) {
        switch (operation) {
        case CHECK_ARITY:
            return createCheckArity(params);
        case DEF_INST_METH:
            return createDefInstMeth(params);
        case LINE_NUM:
            return createLineNum(params);
        case RETURN:
            return createReturn(params);
        case BEQ:
        case B_FALSE:
        case B_TRUE:
        case B_NIL:
        case B_UNDEF:
        case BNE:
            return createBranch(operation, params);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    private Instr createCheckArity(Object[] params) {
        int required = (Integer) params[0];
        int opt = (Integer) params[1];
        int rest = (Integer) params[2];
        
        return new CheckArityInstr(required, opt, rest);
    }

    private Instr createDefInstMeth(Object[] params) {
        Operand container = (Operand) params[0];
        Label nameLable = (Label) params[1];
        BooleanLiteral isInstanceMethodLiteral = (BooleanLiteral) params[2];
        Integer lineNumber = (Integer) params[3];
        
        IRManager manager = IRParsingContext.INSTANCE.getIRManager();
        // FIXME: Or there may be other scopes rather than current?
        IRScope currentScope = IRParsingContext.INSTANCE.getCurrentScope(); 
        // FIXME: No closure support so far
        IRMethod method = new IRMethod(manager, currentScope, nameLable.label, isInstanceMethodLiteral.isTrue(), lineNumber, null);
        
        return new DefineInstanceMethodInstr(container, method);
    }



    private Instr createLineNum(Object[] params) {
        Integer number = (Integer) params[0];
        IRScope currentScope = IRParsingContext.INSTANCE.getCurrentScope();
        return new LineNumberInstr(currentScope, number);
    }
    
    private Instr createReturn(Object[] params) {
        Operand returnValue = (Operand) params[0];
        // FIXME: May have method to return as well 
        return new ReturnInstr(returnValue);
    }
    
    public Instr createReturnInstrWithOperand(Variable result, Operation operation, Operand operand) {
        switch (operation) {
        case COPY:
            return new CopyInstr(result, operand);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    public Instr createReturnInstrWithNoParams(Variable result, Operation operation) {
        switch (operation) {
        case RECV_SELF:
            return createReceiveSelf(result);
        case RECV_CLOSURE:
            return createReceiveClosure(result);

        default:
            throw new UnsupportedOperationException();
        }
    }
    
    private ReceiveSelfInstr createReceiveSelf(Variable result) {
        return new ReceiveSelfInstr(result);
    }
    
    private ReceiveSelfInstr createReceiveClosure(Variable result) {
        return new ReceiveSelfInstr(result);
    }

    public Instr createReturnInstrWithParams(Variable result, Operation operation, Object[] params) {
        switch (operation) {
        case CALL:
            return createCall(result, params);
        case CONST_MISSING:
            return createConstMissing(result, params);
        case COPY:
            return createCopy(result, params);
        case INHERITANCE_SEARCH_CONST:
            return createInheritanceSearchConstInstr(result, params);
        case RECV_PRE_REQD_ARG:
            Integer argIndex = (Integer) params[0];
            return new ReceivePreReqdArgInstr(result, argIndex);
        case SEARCH_CONST:
            return createSearchConst(result, params);

        default:
            throw new UnsupportedOperationException();
        }
    }

    private Instr createCall(Variable result, Object[] params) {
        // FIXME: Persisted as label so far
        Label callTypeLabel = (Label) params[0];
        CallType callType = CallType.valueOf(callTypeLabel.label);
        
        MethAddr methAddr = (MethAddr) params[1];
        Operand receiver = (Operand) params[2];
        Operand[] args = (Operand[]) params[3];
        // FIXME: No closure support so far(WrappedIRClosure)
        return CallInstr.create(callType, result, methAddr, receiver, args, null);
    }
    
    private Instr createConstMissing(Variable result, Object[] params) {
        Operand currentModule = (Operand) params[0];
        Label missingConstLabel = (Label) params[1];
        
        return new ConstMissingInstr(result, currentModule, missingConstLabel.label);
    }

    public Instr createCopy(Variable result, Object param) {
        return new CopyInstr(result, (Operand) param);
    }
    
    public InheritanceSearchConstInstr createInheritanceSearchConstInstr(Variable result, Object[] params) {
        Operand currentModule = (Operand) params[0];
        Label constNameLabel = (Label) params[1];
        BooleanLiteral noPrivateConstsLiteral = (BooleanLiteral) params[2];
        
        return new InheritanceSearchConstInstr(result, currentModule, constNameLabel.label, noPrivateConstsLiteral.isTrue());
    }
    
    public PutGlobalVarInstr createPutGlobalVar(Operand varOperand, Operand value) {
        GlobalVariable var = (GlobalVariable) varOperand;
        return new PutGlobalVarInstr(var.getName(), value);
    }
    
    private Instr createSearchConst(Variable result, Object[] params) {
        Operand startingScope = (Operand) params[0];
        Label constNameLabel = (Label) params[1];
        BooleanLiteral noPrivateConstsLiteral = (BooleanLiteral) params[2];
        
        return new SearchConstInstr(result, constNameLabel.label, startingScope, noPrivateConstsLiteral.isTrue());
    }   
    
    public AliasInstr createAlias(Variable receiver, Operand newName, Operand oldName) {
        return new AliasInstr(receiver, newName, oldName);
    }

    public BlockGivenInstr createBlockGiven(Variable result) {
        return new BlockGivenInstr(result);
    }

    public BackrefIsMatchDataInstr createBackrefIsMatchData(Variable result) {
        return new BackrefIsMatchDataInstr(result);
    }

    public BranchInstr createBranch(Operation operation, Object[] params) {   
        
        Operand arg1 = (Operand) params[0];
        Operand arg2 = (Operand) params[1];
        Label target = (Label) params[2];
        
        switch (operation) {
        case BEQ:
        case B_FALSE:
        case B_TRUE:
        case B_NIL:
        case B_UNDEF:
            return BEQInstr.create(arg1, arg2, target);
        case BNE:
            return BNEInstr.create(arg1, arg2, target);

        default:
            throw new UnsupportedOperationException();
        }
    }

}
