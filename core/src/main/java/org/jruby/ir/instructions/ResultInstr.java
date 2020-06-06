package org.jruby.ir.instructions;

import org.jruby.ir.operands.Variable;

public interface ResultInstr {
    public Variable getResult();
    public void updateResult(Variable v);
}
