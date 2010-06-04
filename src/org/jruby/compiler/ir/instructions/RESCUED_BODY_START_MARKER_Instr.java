package org.jruby.compiler.ir.instructions;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.BasicBlock;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class RESCUED_BODY_START_MARKER_Instr extends Instr
{
    private static Operand[] _empty = new Operand[] {};

    final public Label _begin;
    final public Label _elseBlock;
    final public Label _end;
    final public List<Label> _rescueBlockLabels;

    public RESCUED_BODY_START_MARKER_Instr(Label rBegin, Label elseBlock, Label rEnd, List<Label> rbLabels)
    {
        super(Operation.RESCUE_BODY_START);
        _begin = rBegin;
        _end = rEnd;
        _rescueBlockLabels = rbLabels;
        _elseBlock = elseBlock;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append("(").append(_begin).append(", ").append(_elseBlock).append(", ").append(_end).append(", ").append("[");
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

        return new RESCUED_BODY_START_MARKER_Instr(ii.getRenamedLabel(_begin), _elseBlock == null ? null : ii.getRenamedLabel(_elseBlock), ii.getRenamedLabel(_end), newLabels);
    }
}
