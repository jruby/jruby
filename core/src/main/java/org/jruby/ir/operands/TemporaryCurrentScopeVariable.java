package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

public class TemporaryCurrentScopeVariable extends TemporaryLocalVariable {
    public TemporaryCurrentScopeVariable(int offset) {
        super(offset);
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.CURRENT_SCOPE;
    }

    @Override
    public String getName() {
        return "%current_scope";
    }

    @Override
    public Variable clone(InlinerInfo ii) {
        return this;
    }
}
