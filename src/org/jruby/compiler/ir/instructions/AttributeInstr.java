package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class AttributeInstr extends NoOperandInstr
{
    public AttributeInstr(Operation op) { super(op); }

    public IR_Instr cloneForInlining(InlinerInfo ii) { return this; }
}
