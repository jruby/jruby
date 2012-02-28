package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;

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
