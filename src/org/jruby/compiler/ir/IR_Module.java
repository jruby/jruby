package org.jruby.compiler.ir;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Module extends IR_ScopeImpl
{
    // The "root" method of a class -- the scope in which all definitions, and class code executes, equivalent to java clinit
    private final static String ROOT_METHOD_PREFIX = ":_ROOT_:";
    private static Map<String, IR_Class> _coreClasses;

    static {
        bootStrap();
    }

    static private IR_Class addCoreClass(String name, IR_Scope parent, String[] coreMethods)
    {
        IR_Class c = new IR_Class(parent, (IR_Scope)null, null, name, false);
        _coreClasses.put(c._name, c);
        if (coreMethods != null) {
            for (String m: coreMethods) {
                IR_Method meth = new IR_Method(c, null, m, true);
                meth.setCodeModificationFlag(false);
                c.addMethod(meth);
            }
        }
        return c;
    }

    // SSS FIXME: These should get normally compiled or initialized some other way ... 
    // SSS FIXME: Parent/super-type info is incorrect!
    // These are just placeholders for now .. this needs to be updated with *real* class objects later!
    static public void bootStrap()
    {
        _coreClasses = new HashMap<String, IR_Class>();
        IR_Class obj = addCoreClass("Object", null, null);
        addCoreClass("Class", addCoreClass("Module", obj, null), null);
        addCoreClass("Fixnum", obj, new String[] {"+", "-", "/", "*"});
        addCoreClass("Float", obj, new String[] {"+", "-", "/", "*"});
        addCoreClass("Array", obj, new String[] {"[]", "each", "inject"});
        addCoreClass("Range", obj, new String[] {"each"});
        addCoreClass("Hash", obj, new String[] {"each"});
        addCoreClass("String", obj, null);
        addCoreClass("Proc", obj, null);
    }

    public static IR_Class getCoreClass(String n) { return _coreClasses.get(n); }

    public final String _name;
    private IR_Method _rootMethod; // Dummy top-level method for the class

    public static boolean isAClassRootMethod(IR_Method m) { return m._name.startsWith(ROOT_METHOD_PREFIX); }

    private void addRootMethod()
    {
        // Build a dummy static method for the class -- the scope in which all definitions, and class code executes, equivalent to java clinit
        // SSS FIXME: We have to build different instances of the root method each time we run into a class definition.
        //
        //    class Foo
        //      def m1; ...; end
        //    end
        //
        //    class Foo
        //      def m2; ...; end
        //    end
        //
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
        addRootMethod();
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
