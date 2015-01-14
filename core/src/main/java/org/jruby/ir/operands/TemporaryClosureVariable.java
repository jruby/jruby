package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

public class TemporaryClosureVariable extends TemporaryLocalVariable {
    private final int closureId;

    public TemporaryClosureVariable(int closureId, int offset) {
        super("%cl_" + closureId + "_" + offset, offset);

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
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    @Override
    public String getPrefix() {
        return "%cl_" + closureId + "_";
    }
}
