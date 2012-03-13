package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.targets.JVM;

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

    public void compile(JVM jvm) {
        jvm.method().mark(jvm.methodData().getLabel(label));
    }
}
