package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.operands.Variable;

public interface ResultInstr {
    public Variable getResult();
    public void updateResult(Variable v);
}
