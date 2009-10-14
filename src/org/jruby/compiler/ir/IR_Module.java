package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Module extends IR_ScopeImpl
{
    // The "root" method of a class -- the scope in which all definitions, and class code executes, equivalent to java clinit
    private final static String ROOT_METHOD_PREFIX = ":_ROOT_:";

    public final String _name;
    private IR_Method _rootMethod; // Dummy top-level method for the class

    public static boolean isAClassRootMethod(IR_Method m) { return m._name.startsWith(ROOT_METHOD_PREFIX); }

    private void addRootMethod()
    {
        // Build a dummy static method for the class -- the scope in which all definitions, and class code executes, equivalent to java clinit
        String n = ROOT_METHOD_PREFIX + _name;
        _rootMethod = new IR_Method(this, this, n, n, false);
        addMethod(_rootMethod);
    }

    public IR_Module(IR_Scope parent, IR_Scope lexicalParent, String name)
    { 
        super(parent, lexicalParent);
        _name = name;
        addRootMethod();
    }

    public IR_Module(Operand parent, IR_Scope lexicalParent, String name)
    { 
        super(parent, lexicalParent);
        _name = name;
    }

    public IR_Method getRootMethod() { return _rootMethod; }

    public IR_Method getInstanceMethod(String name)
    {
        for (IR_Method m: _methods) {
            if (m._isInstanceMethod && m._name.equals(name))
                return m;
        }

        return null;
    }

    public IR_Method getClassMethod(String name)
    {
        for (IR_Method m: _methods)
            if (!m._isInstanceMethod && _name.equals(name))
                return m;

        return null;
    }

    public String toString() {
        return "Module: " + _name + super.toString();
    }
}
