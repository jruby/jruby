package org.jruby.ir.operands;

import org.jruby.ir.transformations.inlining.InlinerInfo;

import java.util.List;

// A ruby value that is not a local variable
// (method name, symbol, global var, $ vars)
public abstract class Reference extends Operand {
    final private String name;

    public Reference(String name) {
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
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public String toString() {
        return name;
    }
}
