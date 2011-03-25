package org.jruby.compiler.ir.operands;

public abstract class Reference extends Operand {
    final private String name;

    public Reference(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
