package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IRClass extends IRModule {
    final public Operand superClass;

    public IRClass(IRScope lexicalParent, Operand container, Operand superClass, String className) {
        super(lexicalParent, container, className);

        this.superClass = superClass;
    }

    @Override
    public String toString() {
        return "Class: " + name + super.toString();
    }
}
