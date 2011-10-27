package org.jruby.compiler.ir.instructions;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ExceptionRegionStartMarkerInstr extends Instr {
    final public Label begin;
    final public Label end;
    final public List<Label> rescueBlockLabels;
    final public Label ensureBlockLabel;

    public ExceptionRegionStartMarkerInstr(Label begin, Label end, 
            Label ensureBlockLabel, List<Label> rescueBlockLabels) {
        super(Operation.EXC_REGION_START);
        
        this.begin = begin;
        this.end = end;
        this.rescueBlockLabels = rescueBlockLabels;
        this.ensureBlockLabel = ensureBlockLabel;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        
        buf.append("(").append(begin).append(", ").append(end).append(", ").append("[");
        for (Label l: rescueBlockLabels)
            buf.append(l).append(",");
        buf.append("]");
        if (ensureBlockLabel != null) buf.append("ensure[").append(ensureBlockLabel).append("]");
        buf.append(")");
        
        return buf.toString();
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    public Instr cloneForInlining(InlinerInfo ii) { 
        List<Label> newLabels = new ArrayList<Label>();
        
        for (Label l: rescueBlockLabels) {
            newLabels.add(ii.getRenamedLabel(l));
        }

        return new ExceptionRegionStartMarkerInstr(ii.getRenamedLabel(begin), ii.getRenamedLabel(end),
                ensureBlockLabel == null ? null : ii.getRenamedLabel(ensureBlockLabel), newLabels);
    }
}
