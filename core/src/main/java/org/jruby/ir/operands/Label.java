package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;

import java.util.List;

// SSS FIXME: Should we try to enforce the canonical property that within a method,
// there is exactly one label object with the same label string?
public class Label extends Operand {
    // This is for 'ensure's at the end of top-level constructs ('def foo; ensure; end' or 'class Foo; ensure; end')
    public static final Label UNRESCUED_REGION_LABEL = new Label("UNRESCUED_REGION", 0);
    private static final Label GLOBAL_ENSURE_BLOCK_LABEL = new Label("_GLOBAL_ENSURE_BLOCK_", 0);

    public final String prefix;
    public final int id;

    // This is the PC (program counter == array index) for the label target -- this field is used during interpretation
    // to fetch the instruction to jump to given a label
    private int targetPC = -1;

    // Clone to make sure that every scope gets its own copy of this label
    public static Label getGlobalEnsureBlockLabel() { return GLOBAL_ENSURE_BLOCK_LABEL.clone(); }

    public Label(String prefix, int id) {
        super();

        this.prefix = prefix;
        this.id = id;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.LABEL;
    }

    @Override
    public String toString() {
        return prefix + '_' + id + ':' + targetPC;
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

    public boolean isGlobalEnsureBlockLabel() {
        return this.equals(GLOBAL_ENSURE_BLOCK_LABEL);
    }

    public Label clone() {
        Label newL = new Label(prefix, id);
        newL.setTargetPC(getTargetPC()); // Strictly not necessary, but, copy everything over
        return newL;
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return ii.getRenamedLabel(this);
    }

    public void setTargetPC(int i) {
        this.targetPC = i;
    }

    public int getTargetPC() {
        return this.targetPC;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(prefix);
        e.encode(id);
    }

    public static Label decode(IRReaderDecoder d) {
        String prefix = d.decodeString();
        int id = d.decodeInt();

        // Special case of label
        if ("_GLOBAL_ENSURE_BLOCK".equals(prefix)) return new Label("_GLOBAL_ENSURE_BLOCK", 0);

        // Check if this label was already created
        // Important! Program would not be interpreted correctly
        // if new name will be created every time
        String fullLabel = prefix + '_' + id;
        if (d.getVars().containsKey(fullLabel)) {
            return (Label) d.getVars().get(fullLabel);
        }

        Label newLabel = new Label(prefix, id);

        // Add to context for future reuse
        d.getVars().put(fullLabel, newLabel);

        return newLabel;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Label(this);
    }
}
