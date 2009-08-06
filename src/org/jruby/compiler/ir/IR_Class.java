package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Class extends IR_ScopeImpl
{
    // Object class meta-object
    final public static IR_Class OBJECT = new IR_Class((IR_Scope)null, (IR_Scope)null, null, "Object", false);

    final public String  _className;
    final public Operand _superClass;
    final public boolean _isSingleton;
    
    public IR_Class(IR_Scope parent, IR_Scope lexicalParent, Operand superClass, String className, boolean isSingleton)
    {
        super(parent, lexicalParent);
        _className = className;
        _superClass = superClass;
        _isSingleton = isSingleton;
    }

    public IR_Class(Operand parent, IR_Scope lexicalParent, Operand superClass, String className, boolean isSingleton)
    {
        super(parent, lexicalParent);
        _className = className;
        _superClass = superClass;
        _isSingleton = isSingleton;
    }

    public String toString() {
        return "Class: " +
                "\n  className: " + _className +
                super.toString();
    }
}
