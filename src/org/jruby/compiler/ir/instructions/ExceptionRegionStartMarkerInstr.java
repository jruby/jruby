package org.jruby.compiler.ir.instructions;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ExceptionRegionStartMarkerInstr extends Instr
{
    private static Operand[] _empty = new Operand[] {};

    final public Label _begin;
    final public Label _end;
    final public List<Label> _rescueBlockLabels;

    public ExceptionRegionStartMarkerInstr(Label rBegin, Label rEnd, List<Label> rbLabels)
    {
        super(Operation.EXC_REGION_START);
        _begin = rBegin;
        _end = rEnd;
        _rescueBlockLabels = rbLabels;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append("(").append(_begin).append(", ").append(_end).append(", ").append("[");
        for (Label l: _rescueBlockLabels)
           buf.append(l).append(",");
        buf.append("])");
        return buf.toString();
    }

    public Operand[] getOperands() { return _empty; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { }

    public Instr cloneForInlining(InlinerInfo ii) { 
        List<Label> newLabels = new ArrayList<Label>();
        for (Label l: _rescueBlockLabels)
            newLabels.add(ii.getRenamedLabel(l));

        return new ExceptionRegionStartMarkerInstr(ii.getRenamedLabel(_begin), ii.getRenamedLabel(_end), newLabels);
    }
}
