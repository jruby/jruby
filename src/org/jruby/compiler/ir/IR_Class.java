package org.jruby.compiler.ir;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Class extends IR_Module
{
    final public  Operand _superClass;
    final public  boolean _isSingleton;

    public IR_Class(IR_Scope parent, IR_Scope lexicalParent, Operand superClass, String className, boolean isSingleton)
    {
        super(parent, lexicalParent, className);
        _superClass = superClass;
        _isSingleton = isSingleton;
    }

    public IR_Class(Operand parent, IR_Scope lexicalParent, Operand superClass, String className, boolean isSingleton)
    {
        super(parent, lexicalParent, className);
        _superClass = superClass;
        _isSingleton = isSingleton;
    }

    public String toString() {
        return "Class: " + _name + super.toString();
    }
}
