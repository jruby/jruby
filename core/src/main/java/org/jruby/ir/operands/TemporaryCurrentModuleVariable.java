package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

/**
 *
 * @author enebo
 */
public class TemporaryCurrentModuleVariable extends TemporaryVariable {
    public static TemporaryVariable CURRENT_MODULE = new TemporaryCurrentModuleVariable();

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
