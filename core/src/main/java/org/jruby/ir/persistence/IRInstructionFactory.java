package org.jruby.ir.persistence;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BranchInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.CallType;

public enum IRInstructionFactory {
    INSTANCE;
    
    public Operation createOperation(String name) {
        return Operation.valueOf(name.toUpperCase());
    }
    
    
    
    public LabelInstr createLabel(Label label) {
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

    private Instr createLineNum(Object[] params) {
        Integer number = (Integer) params[0];
        // FIXME: Scope must not be null here? 
        return new LineNumberInstr(null, number);
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
            MethAddr methAddr = (MethAddr) params[0];
            Operand receiver = (Operand) params[1];
            Operand[] args = (Operand[]) params[2];
            // FIXME: Persisted as symbol so far
            Symbol callTypeSymbol = (Symbol) params[3];
            CallType callType = CallType.valueOf(callTypeSymbol.getName());
            // FIXME: No closure support so far(WrappedIRClosure)
            return CallInstr.create(callType, result, methAddr, receiver, args, null);
        case COPY:
            return new CopyInstr(result, (Operand) params[0]);
        case RECV_PRE_REQD_ARG:
            Integer argIndex = (Integer) params[0];
            return new ReceivePreReqdArgInstr(result, argIndex);

        default:
            throw new UnsupportedOperationException();
        }
    }        
    
    public PutGlobalVarInstr createPutGlobalVar(Operand varOperand, Operand value) {
        GlobalVariable var = (GlobalVariable) varOperand;
        return new PutGlobalVarInstr(var.getName(), value);
    }
    
    
    
    
    
    public AliasInstr createAlias(Variable receiver, Operand newName, Operand oldName) {
        return new AliasInstr(receiver, newName, oldName);
    }

    public BlockGivenInstr createBlockGiven(Variable result) {
        // FIXME: missing operand for block
        return new BlockGivenInstr(result, null);
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
