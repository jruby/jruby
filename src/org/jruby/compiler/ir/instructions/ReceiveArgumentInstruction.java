package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign Argument passed into scope/method to a result variable
 */
public class ReceiveArgumentInstruction extends Instr implements ResultInstr {
    // SSS FIXME: Fix IR to start offsets from 0
    protected int argIndex;
    private Variable result;

    public ReceiveArgumentInstruction(Variable result, int argIndex) {
        super(Operation.RECV_ARG);
        
        assert result != null: "ReceiveArgumentInstruction result is null";
        
        this.argIndex = argIndex;
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public int getArgIndex() {
        return argIndex;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, false));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }
}
