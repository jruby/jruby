package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class LABEL_Instr extends NoOperandInstr
{
    public final Label _lbl;

    public LABEL_Instr(Label l) {
        super(Operation.LABEL);
        _lbl = l;
    }

    public String toString() { return _lbl + ":"; }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new LABEL_Instr(ii.getRenamedLabel(_lbl));
    }
}
