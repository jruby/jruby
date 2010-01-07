package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Class extends IR_Module {
    final public Operand _superClass;

    public IR_Class(IR_Scope lexicalParent, Operand container, Operand superClass, String className) {
        super(lexicalParent, container, className);
        _superClass = superClass;
    }

    @Override
    public String toString() {
        return "Class: " + _name + super.toString();
    }
}
