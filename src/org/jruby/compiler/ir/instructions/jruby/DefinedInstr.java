package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

/**
 * Common base class for all defined-category instructions.
 */
public abstract class DefinedInstr extends Instr implements ResultInstr {
    protected Variable result;
    protected final Operand[] operands;
    
    public DefinedInstr(Operation operation, Variable result, Operand[] operands) {
        super(operation);
        
        this.result = result;
        this.operands = operands;
    }
    
    public Operand[] getOperands() {
        return operands;
    }    
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        result = v;
    }    
}
