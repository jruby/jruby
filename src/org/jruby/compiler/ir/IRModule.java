package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.instructions.DefineClassMethodInstr;
import org.jruby.compiler.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.LocalVariable;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.instructions.ReceiveArgumentInstruction;
import org.jruby.parser.LocalStaticScope;

public class IRModule extends IRScopeImpl {
    // The "root" method of a class -- the scope in which all definitions, and class code executes, equivalent to java clinit
    private final static String ROOT_METHOD_PREFIX = ":_ROOT_:";
    private static Map<String, IRClass> coreClasses;

    public final String name;
    private IRMethod rootMethod; // Dummy top-level method for the class
    private CodeVersion version;    // Current code version for this module
    final public List<IRMethod> methods = new ArrayList<IRMethod>();
    
    static {
        bootStrap();
    }

    static private IRClass addCoreClass(String name, IRScope parent, String[] coreMethods) {
        IRClass c = new IRClass(parent, null, null, name);
        coreClasses.put(c.getName(), c);
        if (coreMethods != null) {
            for (String m : coreMethods) {
                IRMethod meth = new IRMethod(c, null, m, true, null);
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
        coreClasses = new HashMap<String, IRClass>();
        IRClass obj = addCoreClass("Object", null, null);
        addCoreClass("Class", addCoreClass("Module", obj, null), null);
        addCoreClass("Fixnum", obj, new String[]{"+", "-", "/", "*"});
        addCoreClass("Float", obj, new String[]{"+", "-", "/", "*"});
        addCoreClass("Array", obj, new String[]{"[]", "each", "inject"});
        addCoreClass("Range", obj, new String[]{"each"});
        addCoreClass("Hash", obj, new String[]{"each"});
        addCoreClass("String", obj, null);
        addCoreClass("Proc", obj, null);
    }

    public static IRClass getCoreClass(String n) {
        return coreClasses.get(n);
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
        String n = ROOT_METHOD_PREFIX + name;
        rootMethod = new IRMethod(this, new MetaObject(this), n, false, new LocalStaticScope(null));
        rootMethod.addInstr(new ReceiveArgumentInstruction(rootMethod.getSelf(), 0));	// Set up self!
    }

    public void addMethod(IRMethod method) {
        assert !IRModule.isAClassRootMethod(method);

        methods.add(method);

        Instr instruction = method.isInstanceMethod ?
            new DefineInstanceMethodInstr(this, method) :
            new DefineClassMethodInstr(this, method);
        
        getRootMethod().addInstr(instruction);
    }

    @Override
    protected void runCompilerPassOnNestedScopes(CompilerPass p) {
        super.runCompilerPassOnNestedScopes(p);

		  getRootMethod().runCompilerPass(p);
        if (!methods.isEmpty()) {
            for (IRScope meth : methods) {
                meth.runCompilerPass(p);
            }
        }
    }

    @Override
    public IRModule getNearestModule() {
        return this;
    }

    public IRModule(IRScope lexicalParent, Operand container, String name) {
        // SSS FIXME: container could be a meta-object which means we can record the constant statically!
        // Add in this opt!
        super(lexicalParent, container);
        this.name = name;
        addRootMethod();
        updateVersion();
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public CodeVersion getVersion() {
        return version;
    }

    public IRMethod getRootMethod() {
        return rootMethod;
    }

    public IRMethod getInstanceMethod(String name) {
        for (IRMethod m : methods) {
            if (m.isInstanceMethod && m.getName().equals(name)) return m;
        }

        return null;
    }

    public IRMethod getClassMethod(String name) {
        for (IRMethod m : methods) {
            if (!m.isInstanceMethod && this.name.equals(name)) return m;
        }

        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Module: " + getName() + super.toString();
    }

    public LocalVariable getLocalVariable(String name) {
        throw new UnsupportedOperationException("This should be happening in the root method of this module/class instead");
    }
}
