package org.jruby.ir.operands;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;

/**
 *
 */
public class TemporaryCurrentModuleVariable extends TemporaryLocalVariable {
    // First four scopes are so common and this operand is immutable so we share them.
    public static final TemporaryCurrentModuleVariable[] CURRENT_MODULE_VARIABLE = {
            new TemporaryCurrentModuleVariable(0), new TemporaryCurrentModuleVariable(1),
            new TemporaryCurrentModuleVariable(2), new TemporaryCurrentModuleVariable(3),
            new TemporaryCurrentModuleVariable(4)
    };

    public static TemporaryCurrentModuleVariable ModuleVariableFor(int depth) {
        return depth < CURRENT_MODULE_VARIABLE.length ? CURRENT_MODULE_VARIABLE[depth] : new TemporaryCurrentModuleVariable(depth);
    }
    
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
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    public static TemporaryCurrentModuleVariable decode(IRReaderDecoder d) {
        return ModuleVariableFor(d.decodeInt());
    }
}
