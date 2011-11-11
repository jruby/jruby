package org.jruby.compiler.ir.operands;

import java.util.List;

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
    public String toString() {
        return name;
    }
}
