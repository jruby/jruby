package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

public class TemporaryClosureVariable extends TemporaryLocalVariable {
    final int closureId;

    public TemporaryClosureVariable(int closureId, int offset) {
        super(offset);

        this.closureId = closureId;
    }

    public int getClosureId() {
        return closureId;
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.CLOSURE;
    }

    @Override
    public Variable clone(InlinerInfo ii) {
        return new TemporaryClosureVariable(closureId, offset);
    }

    @Override
    public String getPrefix() {
        return "%cl_" + closureId + "_";
    }
}
