package org.jruby.compiler.ir;

import java.util.Map;
import java.util.HashMap;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;

public class IR_Module extends IR_ScopeImpl {
    // The "root" method of a class -- the scope in which all definitions, and class code executes, equivalent to java clinit
    private final static String ROOT_METHOD_PREFIX = ":_ROOT_:";
    private static Map<String, IR_Class> _coreClasses;

    public final String _name;
    private IRMethod _rootMethod; // Dummy top-level method for the class
    private CodeVersion _version;    // Current code version for this module

    static {
        bootStrap();
    }

    static private IR_Class addCoreClass(String name, IR_Scope parent, String[] coreMethods) {
        IR_Class c = new IR_Class(parent, null, null, name);
        _coreClasses.put(c.getName(), c);
        if (coreMethods != null) {
            for (String m : coreMethods) {
                IRMethod meth = new IRMethod(c, null, m, true);
                meth.setCodeModificationFlag(false);
                c.addMethod(meth);
            }
        }
        return c;
    }

    // SSS FIXME: These should get normally compiled or initialized some other way ... 
    // SSS FIXME: Parent/super-type info is incorrect!
    // These are just placeholders for now .. this needs to be updated with *real* class objects later!
    static public void bootStrap() {
        _coreClasses = new HashMap<String, IR_Class>();
        IR_Class obj = addCoreClass("Object", null, null);
        addCoreClass("Class", addCoreClass("Module", obj, null), null);
        addCoreClass("Fixnum", obj, new String[]{"+", "-", "/", "*"});
        addCoreClass("Float", obj, new String[]{"+", "-", "/", "*"});
        addCoreClass("Array", obj, new String[]{"[]", "each", "inject"});
        addCoreClass("Range", obj, new String[]{"each"});
        addCoreClass("Hash", obj, new String[]{"each"});
        addCoreClass("String", obj, null);
        addCoreClass("Proc", obj, null);
    }

    public static IR_Class getCoreClass(String n) {
        return _coreClasses.get(n);
    }

    public static boolean isAClassRootMethod(IRMethod m) {
        return m.getName().startsWith(ROOT_METHOD_PREFIX);
    }

    private void addRootMethod() {
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
        _rootMethod = new IRMethod(this, new MetaObject(this), n, false);
        _rootMethod.addInstr(new ReceiveArgumentInstruction(_rootMethod.getSelf(), 0));	// Set up self!
        addMethod(_rootMethod);
    }

    public IR_Module(IR_Scope lexicalParent, Operand container, String name) {
        // SSS FIXME: container could be a meta-object which means we can record the constant statically!
        // Add in this opt!
        super(lexicalParent, container);
        _name = name;
        addRootMethod();
        updateVersion();
    }

    public void updateVersion() {
        _version = CodeVersion.getClassVersionToken();
    }

    public CodeVersion getVersion() {
        return _version;
    }

    public IRMethod getRootMethod() {
        return _rootMethod;
    }

    public IRMethod getInstanceMethod(String name) {
        for (IRMethod m : methods) {
            if (m.isInstanceMethod && m.getName().equals(name)) return m;
        }

        return null;
    }

    public IRMethod getClassMethod(String name) {
        for (IRMethod m : methods) {
            if (!m.isInstanceMethod && _name.equals(name)) return m;
        }

        return null;
    }

    public String getName() {
        return _name;
    }

    @Override
    public String toString() {
        return "Module: " + getName() + super.toString();
    }
}
