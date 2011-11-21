package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction encodes the receive of an argument into a closure
//   Ex:  .. { |a| .. }
// The closure receives 'a' via this instruction
public class ReceiveClosureArgInstr extends Instr implements ResultInstr {
    private final int argIndex;
    private Variable result;

    public ReceiveClosureArgInstr(Variable result, int argIndex) {
        super(Operation.RECV_CLOSURE_ARG);
        
        assert result != null: "ReceiveClosureArgInstr result is null";
        
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

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }
    
    public int getArgIndex() {
        return argIndex;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }
}
