package org.jruby.compiler.ir;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.instructions.DefineClassInstr;
import org.jruby.compiler.ir.instructions.DefineModuleInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.PutConstInstr;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.compiler.ir.operands.ClassMetaObject;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.ModuleMetaObject;
import org.jruby.compiler.ir.IRModule;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;

public class IRModule extends IRScopeImpl {
    // The "root" method of a class -- the scope in which all definitions, and class code executes, equivalent to java clinit
    private final static String ROOT_METHOD_PREFIX = "<ROOT>";
    private static Map<String, IRClass> coreClasses;

    private IRMethod rootMethod; // Dummy top-level method for the class
    private CodeVersion version;    // Current code version for this module

    // Modules, classes, and methods that belong to this scope 
    //
    // LEXICAL scoping, but when a class, method, module definition is
    // encountered in a closure or a method in Ruby code, that definition
    // is pushed up to the nearest containing module!
    //
    // In most cases, this lexical scoping also matches actual class/module hierarchies
    // SSS FIXME: An example where they might be different?
    private List<IRModule> modules = new ArrayList<IRModule>();
    private List<IRClass> classes = new ArrayList<IRClass>();
    private List<IRMethod> methods = new ArrayList<IRMethod>();
    private Map<String, Operand> constants = new HashMap<String, Operand>();
    
    static {
        bootStrap();
    }

    static private IRClass addCoreClass(String name, IRScope parent, String[] coreMethods, StaticScope staticScope) {
        IRClass c = new IRClass(parent, null, null, name, staticScope);
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
        IRClass obj = addCoreClass("Object", null, null, null);
        addCoreClass("Class", addCoreClass("Module", obj, null, null), null, null);
        addCoreClass("Fixnum", obj, new String[]{"+", "-", "/", "*"}, null);
        addCoreClass("Float", obj, new String[]{"+", "-", "/", "*"}, null);
        addCoreClass("Array", obj, new String[]{"[]", "each", "inject"}, null);
        addCoreClass("Range", obj, new String[]{"each"}, null);
        addCoreClass("Hash", obj, new String[]{"each"}, null);
        addCoreClass("String", obj, null, null);
        addCoreClass("Proc", obj, null, null);
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
        String n = ROOT_METHOD_PREFIX + getName();
        rootMethod = new IRMethod(this, MetaObject.create(this), n, false, new LocalStaticScope(null));
        rootMethod.addInstr(new ReceiveSelfInstruction(rootMethod.getSelf()));   // Set up self!
    }

    public List<IRModule> getModules() {
        return modules;
    }

    public List<IRClass> getClasses() {
        return classes;
    }

    public List<IRMethod> getMethods() {
        return methods;
    }

    public Map getConstants() {
        return Collections.unmodifiableMap(constants);
    }

    // Attempted compile-time resolution of a Ruby constant.
    //
    // We might not be able to resolve for the following reasons:
    // 1. The constant is missing
    // 2. The reference is a lexical forward-reference
    // 3. The constant's value is only known when the program first runs.
    // 4. Our compiler isn't able to right away infer that this is a constant.
    //
    // SSS FIXME:
    // 1. The operand can be a literal array, range, or hash -- hence Operand
    //    because Array, Range, and Hash derive from Operand and not Constant ...
    //    Is there a way to fix this impedance mismatch?
    // 2. It should be possible to handle the forward-reference case by creating a new
    //    ForwardReference operand and then inform the scope of the forward reference
    //    which the scope can fix up when the reference gets defined.  At code-gen time,
    //    if the reference is unresolved, when a value is retrieved for the forward-ref
    //    and we get a null, we can throw a ConstMissing exception!  Not sure!
    public Operand getConstantValue(String constRef) {
        return null;
/**
SSS: We are no longer going to use this because of the Module.remove_const method

Even if we can resolve the name to a constant we know at compilation time, there is no
guarantee that the value will exist at runtime!  So, we need to actually return the module
from which the value was found so that the caller can then protect the resolved value against
the module version to protect against remove_consts!  Even though this feature is very rarely
used, we are now forced to be conservative.

        Operand cv = constants.get(constRef);
        Operand p = container;
        // SSS FIXME: Traverse up the scope hierarchy to find the constant as long as the container is a static scope
        if ((cv == null) && (p != null) && (p instanceof MetaObject)) {
            // Can be null for IR_Script meta objects
            if (((MetaObject) p).scope == null) {
                IRClass coreClass = IRModule.getCoreClass(constRef);

                return coreClass != null ? new ClassMetaObject(coreClass) : null;
            }
            // Boxed scope has to be an IR module or class
            cv = ((IRModule) (((MetaObject) p).scope)).getConstantValue(constRef);

            // If cv is null, it can either mean the constant is missing 
            // or it can mean that we couldn't resolve this at compilation time.
        }
        return cv;
**/
    }

    public void setConstantValue(String constRef, Operand val) {
        // FIXME: isConstant can be confusing since we have Ruby constants and constants in the compiler sense
        if (val.isConstant()) constants.put(constRef, val);
    }

    public void addModule(IRModule m) {
        modules.add(m);
    }

    public void addClass(IRClass c) {
        classes.add(c);
    }

    public void addMethod(IRMethod method) {
        assert !IRModule.isAClassRootMethod(method);

        methods.add(method);
    }

    @Override
    public void runCompilerPassOnNestedScopes(CompilerPass p) {
        for (IRScope m : modules) {
            m.runCompilerPass(p);
        }

        for (IRScope c : classes) {
            c.runCompilerPass(p);
        }

        getRootMethod().runCompilerPass(p);
        for (IRScope meth : methods) {
            meth.runCompilerPass(p);
        }
    }

    @Override
    public IRModule getNearestModule() {
        return this;
    }

    public IRModule(IRScope lexicalParent, Operand container, String name, StaticScope scope) {
        // SSS FIXME: container could be a meta-object which means we can record the constant statically!
        // Add in this opt!
        super(lexicalParent, container, name, scope);
        addRootMethod();
        updateVersion();
    }

    public void updateVersion() {
        version = CodeVersion.getClassVersionToken();
    }

    public String getScopeName() {
        return "Module";
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
            if (!m.isInstanceMethod && getName().equals(name)) return m;
        }

        return null;
    }

    public boolean isCoreClass(String className) {
        return this == IRClass.getCoreClass(className);
    }

    public LocalVariable getLocalVariable(String name) {
        throw new UnsupportedOperationException("This should be happening in the root method of this module/class instead");
    }
}
