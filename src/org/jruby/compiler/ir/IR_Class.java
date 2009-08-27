package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Class extends IR_ScopeImpl
{
    // The "root" method of a class -- the scope in which all definitions, and class code executes, equivalent to java clinit
    final private static String ROOT_METHOD_PREFIX = ":_ROOT_:";
    public static boolean isAClassRootMethod(IR_Method m) { return m._name.startsWith(ROOT_METHOD_PREFIX); }

    // Object class meta-object
    // SSS FIXME: These should be available via bootstrapping!
    // SSS FIXME: Parent/super-type info is incorrect!  These are just placeholders
    final public static IR_Class OBJECT = new IR_Class((IR_Scope)null, (IR_Scope)null, null, "Object", false);
    final public static IR_Class FIXNUM = new IR_Class(OBJECT, (IR_Scope)null, null, "Fixnum", false);
    final public static IR_Class FLOAT  = new IR_Class(OBJECT, (IR_Scope)null, null, "Float", false);
    final public static IR_Class STRING = new IR_Class(OBJECT, (IR_Scope)null, null, "String", false);
    final public static IR_Class ARRAY  = new IR_Class(OBJECT, (IR_Scope)null, null, "Array", false);
    final public static IR_Class RANGE  = new IR_Class(OBJECT, (IR_Scope)null, null, "Range", false);
    final public static IR_Class HASH   = new IR_Class(OBJECT, (IR_Scope)null, null, "Hash", false);

    final public String  _className;
    final public Operand _superClass;
    final public boolean _isSingleton;

    private IR_Method _rootMethod; // Dummy top-level method for the class

    private void addRootMethod()
    {
        // Build a dummy static method for the class -- the scope in which all definitions, and class code executes, equivalent to java clinit
        String n = ROOT_METHOD_PREFIX + _className;
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

    public IR_Method getMethod(String name)
    {
        for (IR_Method m: _methods)
            if (m._name.equals(name))
                return m;

        return null;
    }

    public String toString() {
        return "Class: " + _className + super.toString();
    }
}
