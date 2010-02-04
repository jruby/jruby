package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.compiler.ir.instructions.DEFINE_CLASS_METHOD_Instr;
import org.jruby.compiler.ir.instructions.DEFINE_INSTANCE_METHOD_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.PUT_CONST_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.SelfVariable;
import org.jruby.compiler.ir.operands.TemporaryClosureVariable;
import org.jruby.compiler.ir.operands.TemporaryVariable;

/**
 * Right now, this class abstracts 5 different scopes: Script, Module, Class, 
 * Method, and Closure.
 *
 * Script, Module, and Class are containers and "non-execution" scopes.
 * Method and Clsoure are the only two "execution" scopes.
 *
 * In the compiler-land, IR_* versions of these scopes encapsulate only as much 
 * information as is required to convert Ruby code into equivalent Java code.
 *
 * But, in the non-compiler land, there will be a corresponding java object for
 * each of these scopes which encapsulates the runtime semantics and data needed
 * for implementing them.  In the case of Module, Class, and Method, they also
 * happen to be instances of the corresponding Ruby classes -- so, in addition
 * to providing code that help with this specific ruby implementation, they also
 * have code that let them behave as ruby instances of their corresponding
 * classes.  Script and Closure have no such Ruby companions, as far as I can
 * tell.
 *
 * Examples:
 * - the runtime class object might have refs. to the runtime method objects.
 * - the runtime method object might have a slot for a heap frame (for when it
 *   has closures that need access to the method's local variables), it might
 *   have version information, it might have references to other methods that
 *   were optimized with the current version number, etc.
 * - the runtime closure object will have a slot for a heap frame (for when it 
 *   has closures within) and might get reified as a method in the java land
 *   (but inaccessible in ruby land).  So, passing closures in Java land might
 *   be equivalent to passing around the method handles.
 *
 * and so on ...
 */
public abstract class IR_ScopeImpl implements IR_Scope {
    Operand _container;       // Parent container for this context
    IR_Scope _lexicalParent;  // Lexical parent scope

    // ENEBO: These collections are initliazed on construction, but the rest
    //   are init()'d.  This can't be right can it?  They are also final...
    
    // Modules, classes, and methods defined in this LEXICAL scope. In many
    // cases, the lexical scoping and class/method hierarchies are the same.
    final public List<IR_Module> modules = new ArrayList<IR_Module>();
    final public List<IR_Class> classes = new ArrayList<IR_Class>();
    final public List<IRMethod> methods = new ArrayList<IRMethod>();
    private Map<String, String> aliases; // oldName -> newName for methods

    // ENEBO: This is also only for lexical score too right?
    private Map<String, Operand> contants;

    // Index values to guarantee we don't assign same internal index twice
    private int _nextMethodIndex; // ENEBO: dead?
    private int _nextClosureIndex;

    // Keeps track of types of prefix indexes for variables and labels
    private Map<String, Integer> _nextVarIndex;

    private void init(IR_Scope lexicalParent, Operand container) {
        _lexicalParent = lexicalParent;
        _container = container;
        _nextVarIndex = new HashMap<String, Integer>();
        contants = new HashMap<String, Operand>();
        aliases = new HashMap<String, String>();
        _nextMethodIndex = 0;
        _nextClosureIndex = 0;
    }

    public IR_ScopeImpl(IR_Scope lexicalParent, Operand container) {
        init(lexicalParent, container);
    }

    // Returns the containing scope!
    public Operand getContainer() {
        return _container;
    }

    public IR_Scope getLexicalParent() {
        return _lexicalParent;
    }

    public int getNextClosureId() {
        _nextClosureIndex++;
        
        return _nextClosureIndex;
    }

    public Variable getNewTemporaryClosureVariable(int closureId) {
        return new TemporaryClosureVariable(closureId, allocateNextPrefixedName("%cl_" + closureId));
    }

    public Variable getNewTemporaryVariable() {
        return new TemporaryVariable(allocateNextPrefixedName("%v"));
    }

    public Label getNewLabel(String prefix) {
        return new Label(prefix + "_" + allocateNextPrefixedName(prefix));
    }

    public Label getNewLabel() {
        return getNewLabel("LBL");
    }

    private int allocateNextPrefixedName(String prefix) {
        Integer index = _nextVarIndex.get(prefix);
        if (index == null) index = 0;
        
        _nextVarIndex.put(prefix, index + 1);
        
        return index;
    }

    // ENEBO: Appears to be dead code?
    public int getAndIncrementMethodIndex() {
        _nextMethodIndex++;
        
        return _nextMethodIndex;
    }

    // ENEBO: Can this always be the same variable?  Then SELF comparison could
    //    compare against this?
    public Variable getSelf() {
        return new SelfVariable();
    }

    public void addModule(IR_Module m) {
        setConstantValue(m._name, new MetaObject(m));
        modules.add(m);
    }

    public void addClass(IR_Class c) {
        setConstantValue(c._name, new MetaObject(c));
        classes.add(c);
    }

    public void addMethod(IRMethod m) {
        methods.add(m);

        if (IR_Module.isAClassRootMethod(m)) return;

        if ((this instanceof IRMethod) && ((IRMethod) this).isAClassRootMethod()) {
            IR_Module c = (IR_Module) (((MetaObject) this._container)._scope);
            c.getRootMethod().addInstr(m.isInstanceMethod ? new DEFINE_INSTANCE_METHOD_Instr(c, m) : new DEFINE_CLASS_METHOD_Instr(c, m));
        } else if (m.isInstanceMethod && (this instanceof IR_Module)) {
            IR_Module c = (IR_Module) this;
            c.getRootMethod().addInstr(new DEFINE_INSTANCE_METHOD_Instr(c, m));
        } else if (!m.isInstanceMethod && (this instanceof IR_Module)) {
            IR_Module c = (IR_Module) this;
            c.getRootMethod().addInstr(new DEFINE_CLASS_METHOD_Instr(c, m));
        } else {
            // SSS FIXME: Do I have to generate a define method instruction here??
            throw new RuntimeException("Encountered method add in a non-class scope!");
        }
    }

    public void addInstr(IR_Instr i) {
        throw new RuntimeException("Encountered instruction add in a non-execution scope!");
    }

    // Record that newName is a new method name for method with oldName
    // This is for the 'alias' keyword which resolves method names in the static compile/parse-time context
    public void recordMethodAlias(String newName, String oldName) {
        aliases.put(oldName, newName);
    }

    // Unalias 'name' and return new name
    public String unaliasMethodName(String name) {
        String n = name;
        String a = null;
        do {
            a = aliases.get(n);
            if (a != null) n = a;
        } while (a != null);

        return n;
    }

    public List<IR_Instr> getInstrs() {
        return null;
    }

    // Sometimes the value can be retrieved at "compile time".  If we succeed, nothing like it!
    // We might not .. for the following reasons:
    // 1. The constant is missing,
    // 2. The reference is a forward-reference,
    // 3. The constant's value is only known at run-time on first-access (but, this is runtime, isn't it??)
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
    //
    public Operand getConstantValue(String constRef) {
//        System.out.println("Looking in " + this + " for constant: " + constRef);
        Operand cv = contants.get(constRef);
        Operand p = _container;
        // SSS FIXME: Traverse up the scope hierarchy to find the constant as long as the container is a static scope
        if ((cv == null) && (p != null) && (p instanceof MetaObject)) {
            // Can be null for IR_Script meta objects
            if (((MetaObject) p)._scope == null) {
//                System.out.println("Looking for core class: " + constRef);
                IR_Class coreClass = IR_Module.getCoreClass(constRef);

                return coreClass != null ? new MetaObject(coreClass) : null;
            }
            cv = ((MetaObject) p)._scope.getConstantValue(constRef);
        }

        return cv;
    }

    public void setConstantValue(String constRef, Operand val) {
        if (val.isConstant()) contants.put(constRef, val);

        if (this instanceof IR_Module) {
            ((IR_Module) this).getRootMethod().addInstr(new PUT_CONST_Instr(this, constRef, val));
        }
    }

    public Map getConstants() {
        return Collections.unmodifiableMap(contants);
    }

    @Override
    public String toString() {
        return (contants.isEmpty() ? "" : "\n  constants: " + contants);
    }

    protected void runCompilerPassOnNestedScopes(CompilerPass p) {
        if (!modules.isEmpty()) {
            for (IR_Scope m : modules) {
                m.runCompilerPass(p);
            }
        }

        if (!classes.isEmpty()) {
            for (IR_Scope c : classes) {
                c.runCompilerPass(p);
            }
        }

        if (!methods.isEmpty()) {
            for (IR_Scope meth : methods) {
                meth.runCompilerPass(p);
            }
        }
    }

    public void runCompilerPass(CompilerPass p) {
        boolean isPreOrder = p.isPreOrder();

        if (isPreOrder) p.run(this);

        runCompilerPassOnNestedScopes(p);

        if (!isPreOrder) p.run(this);
    }

    public String toStringInstrs() {
        return "";
    }

    public String toStringVariables() {
        return "";
    }
}
