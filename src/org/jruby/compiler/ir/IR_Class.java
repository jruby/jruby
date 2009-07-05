package org.jruby.compiler.ir;
import org.jruby.util.JavaNameMangler;

public class IR_Class extends IR_ScopeImpl
{
    // Object class meta-object
    final public static IR_Class OBJECT = new IR_Class((IR_Scope)null, null, "Object", false);

    final public String  _className;
    final public Operand _superClass;
    final public boolean _isSingleton;

    public IR_Class(IR_Scope parent, Operand superClass, String className, boolean isSingleton)
    {
       super(parent); 
        _className = className;
        _superClass = superClass;
        _isSingleton = isSingleton;
    }

    public IR_Class(Operand parent, Operand superClass, String className, boolean isSingleton)
    {
       super(parent); 
        _className = className;
        _superClass = superClass;
        _isSingleton = isSingleton;
    }
}
