package org.jruby.compiler.ir.operands;
import org.jruby.compiler.ir.representations.InlinerInfo;

// SSS FIXME: Should we try to enforce the canonical property that within a method,
// there is exactly one label object with the same label string?
public class Label extends Operand
{
    public final String _label;

    // This is the PC (program counter == array index) for the label target -- this field is used during interpretation
    // to fetch the instruction to jump to given a label
    private int targetPC = -1;

    public static int index = 0;

    public Label(String l) { _label = l; }

    public String toString() { return _label; }

    public int hashCode() { return _label.hashCode(); }

    public boolean equals(Object o) { return (o instanceof Label) && _label.equals(((Label)o)._label); }

    public Operand cloneForInlining(InlinerInfo ii) { return ii.getRenamedLabel(this); }

    public void setTargetPC(int i) { this.targetPC = i; }

    public int getTargetPC() { return this.targetPC; }
}
