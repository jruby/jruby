package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;

/**
 * For most instrs that have a result this is their base class.  Some instrs, most notably
 * callinstr is more complicated because we would rather use inheritance to share call-related
 * logic.
 */
public abstract class ResultBaseInstr extends Instr implements ResultInstr {
    protected Variable result;

    public ResultBaseInstr(Operation operation, Variable result, Operand[] operands) {
        super(operation, operands);

        this.result = result;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getResult());
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable result) {
        this.result = result;
    }
}
