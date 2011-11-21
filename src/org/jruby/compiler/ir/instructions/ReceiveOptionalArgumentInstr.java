package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Assign the 'index' argument to 'dest'.
public class ReceiveOptionalArgumentInstr extends Instr implements ResultInstr {
    int argIndex;
    private Variable result;
    
    public ReceiveOptionalArgumentInstr(Variable result, int index) {
        super(Operation.RECV_OPT_ARG);
        
        assert result != null: "ReceiveOptionalArgumentInstr result is null";
        
        this.argIndex = index;
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    public int getArgIndex() {
       return argIndex;
    }
    
    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = v;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ReceiveOptionalArgumentInstr(ii.getRenamedVariable(result), argIndex);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }
}
