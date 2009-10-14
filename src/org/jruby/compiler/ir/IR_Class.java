package org.jruby.compiler.ir;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.operands.Operand;

public class IR_Class extends IR_Module
{
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
        addCoreClass("Array", obj, new String[] {"[]"});
        addCoreClass("String", obj, null);
        addCoreClass("Range", obj, null);
        addCoreClass("Hash", obj, null);
    }

    public static IR_Class getCoreClass(String n) { return _coreClasses.get(n); }

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
