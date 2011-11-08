package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class LabelInstr extends Instr {
    public final Label label;

    public LabelInstr(Label label) {
        super(Operation.LABEL);
        
        this.label = label;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public String toString() {
        return label + ":";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new LabelInstr(ii.getRenamedLabel(label));
    }
}
