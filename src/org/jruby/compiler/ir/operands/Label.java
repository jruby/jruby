package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.representations.InlinerInfo;

// SSS FIXME: Should we try to enforce the canonical property that within a method,
// there is exactly one label object with the same label string?
public class Label extends Operand {
    public final String _label;

    // This is the PC (program counter == array index) for the label target -- this field is used during interpretation
    // to fetch the instruction to jump to given a label
    private int targetPC = -1;

    public static int index = 0;

    public Label(String l) { _label = l; }

    @Override
    public String toString() { 
        return _label;
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
        return _label.hashCode();
    }

    @Override
    public boolean equals(Object o) { 
        return (o instanceof Label) && _label.equals(((Label)o)._label);
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) { 
        return ii.getRenamedLabel(this);
    }

    public void setTargetPC(int i) { this.targetPC = i; }

    public int getTargetPC() { return this.targetPC; }
}
