package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;

public class GuardInstr extends NoOperandInstr
{
    public GuardInstr(Operation op) { super(op); }
}
