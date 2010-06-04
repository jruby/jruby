package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;

// Not used anywhere right now!
public abstract class GuardInstr extends NoOperandInstr
{
    public GuardInstr(Operation op) { super(op); }

    public Instr cloneForInlining(InlinerInfo ii) { return this; }
}
