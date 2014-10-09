package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.CloneInfo;

import java.util.List;

// A ruby value that is not a local variable
// (method name, symbol, global var, $ vars)
public abstract class Reference extends Operand {
    final private String name;

    public Reference(OperandType type, String name) {
        super(type);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
