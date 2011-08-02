package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;

public class NopInstr extends NoOperandInstr
{
    public static NopInstr NOP = new NopInstr();

    private NopInstr() { super(Operation.NOP); this.markDead(); }

    @Override
    public String toString() { return "NOP"; }

    public Instr cloneForInlining(InlinerInfo ii) { return this; }

    @Override
    public Label interpret(InterpreterContext interp) { return null; }
}
