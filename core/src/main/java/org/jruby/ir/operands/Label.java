package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.List;

// SSS FIXME: Should we try to enforce the canonical property that within a method,
// there is exactly one label object with the same label string?
public class Label extends Operand {
    public final String label;

    // This is the PC (program counter == array index) for the label target -- this field is used during interpretation
    // to fetch the instruction to jump to given a label
    private int targetPC = -1;

    public static int index = 0;

    public Label(String l) { label = l; }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to do */
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public int hashCode() {
        return label.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Label) && label.equals(((Label)o).label);
    }

    public Label clone() {
        return new Label(label);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return ii.getRenamedLabel(this);
    }

    public void setTargetPC(int i) { this.targetPC = i; }

    public int getTargetPC() { return this.targetPC; }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Label(this);
    }
}
