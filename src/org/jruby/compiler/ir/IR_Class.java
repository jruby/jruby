package org.jruby.compiler.ir;
import org.jruby.util.JavaNameMangler;

public class IR_Class implements IR_ScopeImpl
{
    final public String  _className;
    final public Operand _superClass;
    final public boolean _isSingleton;

    private void init(String name, Operand superClass, boolean isSingleton)
    {
        _className = name;
        _isSingleton = isSingleton;
        _superClass = superClass;
    }

    public IR_Class(IR_Scope parent, Operand superClass, String className, boolean isSingleton)
    {
       super(parent); 
       init(className, superClass, isSingleton);
    }

    public IR_Class(IR_Scope parent, Operand superClass, String className) 
    {
       super(parent); 
       init(className, superClass, false);
    }
}
