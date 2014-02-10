package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

/**
 *
 */
public class TemporaryCurrentModuleVariable extends TemporaryLocalVariable {
    public TemporaryCurrentModuleVariable(int offset) {
        super(offset);
    }

    @Override
    public TemporaryVariableType getType() {
        return TemporaryVariableType.CURRENT_MODULE;
    }

    @Override
    public String getName() {
        return "%current_module";
    }

    @Override
    public Variable clone(InlinerInfo ii) {
        return this;
    }
}
