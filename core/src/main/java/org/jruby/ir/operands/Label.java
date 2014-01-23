package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.List;

// SSS FIXME: Should we try to enforce the canonical property that within a method,
// there is exactly one label object with the same label string?
public class Label extends Operand {
    public static final Label UNRESCUED_REGION_LABEL = new Label("UNRESCUED_REGION", 0);

    public final String prefix;
    public final int id;

    // This is the PC (program counter == array index) for the label target -- this field is used during interpretation
    // to fetch the instruction to jump to given a label
    private int targetPC = -1;

    public Label(String prefix, int id) {
        super(OperandType.LABEL);

        this.prefix = prefix;
        this.id = id;
    }

    @Override
    public String toString() {
        return prefix + "_" + id + ":" + targetPC;
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
        return 11 * (77 + System.identityHashCode(prefix)) + id;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Label) && id == ((Label) o).id && prefix.equals(((Label)o).prefix);
    }

    public Label clone() {
        return new Label(prefix, id);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return ii.getRenamedLabel(this);
    }

    public void setTargetPC(int i) {
        this.targetPC = i;
    }

    public int getTargetPC() {
        return this.targetPC;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Label(this);
    }
}
