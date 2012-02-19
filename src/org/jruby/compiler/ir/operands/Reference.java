package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.representations.InlinerInfo;

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
