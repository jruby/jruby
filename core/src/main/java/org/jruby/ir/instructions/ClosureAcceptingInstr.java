package org.jruby.ir.instructions;

import org.jruby.ir.operands.Operand;

/**
 * Marks instrs that accept closure args.
 * Currently, CallBase and BuildLambdaInstr implement this.
 */
public interface ClosureAcceptingInstr {
    public Operand getClosureArg();
}
