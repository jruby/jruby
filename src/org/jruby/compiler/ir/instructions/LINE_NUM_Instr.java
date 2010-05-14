package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IR_Scope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class LINE_NUM_Instr extends NoOperandInstr
{
    public final int _lnum;
    public final IR_Scope _scope; // We need to keep scope info here so that line number is meaningful across inlinings.

    public LINE_NUM_Instr(IR_Scope s, int n) {
        super(Operation.LINE_NUM);
        _scope = s;
        _lnum = n;
    }

    public String toString() { return super.toString() + "(" + _lnum + ")"; }

    public IR_Instr cloneForInlining(InlinerInfo ii) { return this; }
}
