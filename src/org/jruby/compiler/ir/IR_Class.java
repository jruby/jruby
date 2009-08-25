package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.opts.Optimization;

public class IR_Class extends IR_ScopeImpl
{
    // Object class meta-object
    final public static IR_Class OBJECT = new IR_Class((IR_Scope)null, (IR_Scope)null, null, "Object", false);

    final public String  _className;
    final public Operand _superClass;
    final public boolean _isSingleton;

    private IR_Method _rootMethod; // Dummy top-level method for the class

    private void addRootMethod()
    {
        // Build a dummy static method for the dummy class
		  String n = "__root__" + _className;
        _rootMethod = new IR_Method(this, this, n, n, false);
        addMethod(_rootMethod);
    }
    
    public IR_Class(IR_Scope parent, IR_Scope lexicalParent, Operand superClass, String className, boolean isSingleton)
    {
        super(parent, lexicalParent);
        _className = className;
        _superClass = superClass;
        _isSingleton = isSingleton;
        addRootMethod();
    }

    public IR_Class(Operand parent, IR_Scope lexicalParent, Operand superClass, String className, boolean isSingleton)
    {
        super(parent, lexicalParent);
        _className = className;
        _superClass = superClass;
        _isSingleton = isSingleton;
        addRootMethod();
    }

    public IR_Method getRootMethod() { return _rootMethod; }

    public String toString() {
        return "Class: " +
                "\n  className: " + _className +
                super.toString();
    }
}
