package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;

public abstract class ReceiveExceptionBase extends ResultBaseInstr implements FixedArityInstr {
    public ReceiveExceptionBase(Operation op, Variable result) {
        super(op, result, EMPTY_OPERANDS);

        assert result != null : "ResultExceptionInstr result is null";
    }
}
