package org.jruby.ir.instructions.defined;

import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;

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
