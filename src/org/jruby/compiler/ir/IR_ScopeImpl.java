package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.jruby.compiler.ir.instructions.DEFINE_CLASS_METHOD_Instr;
import org.jruby.compiler.ir.instructions.DEFINE_INSTANCE_METHOD_Instr;
import org.jruby.compiler.ir.instructions.GET_CONST_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.JRUBY_IMPL_CALL_Instr;
import org.jruby.compiler.ir.instructions.PUT_CONST_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;

/* Right now, this class abstracts 5 different scopes: Script, Module, Class, Method, and Closure
 *
 * Script, Module, and Class are containers and "non-execution" scopes.
 * Method and Clsoure are the only two "execution" scopes.
 *
 * In the compiler-land, IR_* versions of these scopes encapsulate only as much information
 * as is required to convert Ruby code into equivalent Java code.
 *
 * But, in the non-compiler land, there will be a corresponding java object for each of these
 * scopes which encapsulates the runtime semantics and data needed for implementing them.
 * In the case of Module, Class, and Method, they also happen to be instances of the corresponding
 * Ruby classes -- so, in addition to providing code that help with this specific ruby implementation,
 * they also have code that let them behave as ruby instances of their corresponding classes.
 * Script and Closure have no such Ruby companions, as far as I can tell.
 *
 * Examples:
 * - the runtime class object might have refs. to the runtime method objects.
 * - the runtime method object might have a slot for a heap frame (for when it has closures that need access to the
 *   method's local variables), it might have version information, it might have references to other methods
 *   that were optimized with the current version number, etc.
 * - the runtime closure object will have a slot for a heap frame (for when it has closures within) and might
 *   get reified as a method in the java land (but inaccessible in ruby land).  So, passing closures in Java land
 *   might be equivalent to passing around the method handles.
 *
 * and so on ...
 */
public abstract class IR_ScopeImpl implements IR_Scope
{
    Operand  _parent;         // Parent container for this context (can be dynamic!!)
                              // If dynamic, at runtime, this will be the meta-object corresponding to a class/script/module/method/closure
    IR_Scope _lexicalParent;  // Lexical parent scope

// SSS FIXME: Maybe this is not really a concern after all ...
        // Nesting level of this scope in the lexical nesting of scopes in the current file -- this is not to be confused
        // with semantic nesting of scopes across files.
        //
        // Consider this code in a file f
        // class M1::M2::M3::C 
        //   ...
        // end
        //
        // So, C is at lexical nesting level of 1 (the file script is at 0) in the file 'f'
        // Semantically it is at level 3 (M1, M2, M3 are at 0,1,2).
        //
        // This is primarily used to ensure that variable names don't clash!
        // i.e. definition of %v_1 in a closure shouldn't override the use of %v_1 from the parent scope!
//    private int _lexicalNestingLevel;

        // Map of constants defined in this scope (not valid for methods!)
    private Map<String, Operand> _constMap;

        // Map keep track of the next available variable index for a particular prefix
    private Map<String, Integer> _nextVarIndex;

        // Map recording method aliases oldName -> newName maps
    private Map<String, String> _methodAliases;

    private int _nextMethodIndex;
    private int _nextClosureIndex;

    // List of modules, classes, and methods defined in this scope (lexical scope -- not class / method hierarchies).
    // In many cases, the lexical scoping and class/method hierarchies might coincide.
    final public List<IR_Module> _modules  = new ArrayList<IR_Module>();
    final public List<IR_Class>  _classes  = new ArrayList<IR_Class>();
    final public List<IR_Method> _methods  = new ArrayList<IR_Method>();

    private void init(Operand parent, IR_Scope lexicalParent) {
        _parent = parent;
        _lexicalParent = lexicalParent;
        _nextVarIndex = new HashMap<String, Integer>();
        _constMap = new HashMap<String, Operand>();
        _methodAliases = new HashMap<String, String>();
        _nextMethodIndex = 0;
        _nextClosureIndex = 0;
//        _lexicalNestingLevel = lexicalParent == null ? 0 : ((IR_ScopeImpl)lexicalParent)._lexicalNestingLevel + 1;
    }

    public IR_ScopeImpl(IR_Scope parent, IR_Scope lexicalParent) {
        init(new MetaObject(parent), lexicalParent);
    }

    public IR_ScopeImpl(Operand parent, IR_Scope lexicalParent) {
        init(parent, lexicalParent);
    }

        // Returns the containing parent scope!
    public Operand getParent() {
        return _parent;
    }

    public IR_Scope getLexicalParent() {
        return _lexicalParent;
    }

    public int getNextClosureId() {
        _nextClosureIndex++;
        return _nextClosureIndex;
    }

    public Variable getNewVariable(String prefix) {
        if (prefix == null)
            prefix = "%v_";

        // We need to ensure that the variable names generated here cannot conflict with ruby variable names!
        // Hence the "%" that is appended to the beginning!
        if (!prefix.startsWith("%"))
            prefix += "%";

        Integer idx = _nextVarIndex.get(prefix);
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(prefix, idx+1);

        // Insert nesting level to ensure variable names don't conflict across nested scopes!
        // i.e. definition of %v_1 in a closure shouldn't override the use of %v_1 from the parent scope!
//        return new Variable(prefix + _lexicalNestingLevel + "_" + idx);
        return new Variable(prefix + idx);
    }

    public Variable getNewVariable() { return getNewVariable("%v_"); }

    public Label getNewLabel(String lblPrefix) {
        Integer idx = _nextVarIndex.get(lblPrefix);
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(lblPrefix, idx+1);
        return new Label(lblPrefix + idx);
    }

    public Label getNewLabel() { return getNewLabel("LBL_"); }

    public int getAndIncrementMethodIndex() { _nextMethodIndex++; return _nextMethodIndex; }

        // get "self"
    public Variable getSelf() { return new Variable("self"); }

    public void addModule(IR_Module m) {
        setConstantValue(m._name, new MetaObject(m)) ;
        _modules.add(m);
    }

    public void addClass(IR_Class c) { 
        setConstantValue(c._name, new MetaObject(c));
        _classes.add(c);
    }

    public void addMethod(IR_Method m) {
        _methods.add(m);
        if (IR_Module.isAClassRootMethod(m))
           return;

        if ((this instanceof IR_Method) && ((IR_Method)this).isAClassRootMethod()) {
            IR_Module c = (IR_Module)(((MetaObject)this._parent)._scope);
            c.getRootMethod().addInstr(m._isInstanceMethod ? new DEFINE_INSTANCE_METHOD_Instr(c, m) : new DEFINE_CLASS_METHOD_Instr(c, m));
        }
        else if (m._isInstanceMethod && (this instanceof IR_Module)) {
            IR_Module c = (IR_Module)this;
            c.getRootMethod().addInstr(new DEFINE_INSTANCE_METHOD_Instr(c, m));
        }
        else if (!m._isInstanceMethod && (this instanceof IR_Module)) {
            IR_Module c = (IR_Module)this;
            c.getRootMethod().addInstr(new DEFINE_CLASS_METHOD_Instr(c, m));
        }
        else {
            // SSS FIXME: Do I have to generate a define method instruction here??
            // throw new RuntimeException("Encountered method add in a non-class scope!");
        }
    }

    public void addInstr(IR_Instr i) { 
        throw new RuntimeException("Encountered instruction add in a non-execution scope!");
    }

        // Record that newName is a new method name for method with oldName
        // This is for the 'alias' keyword which resolves method names in the static compile/parse-time context
    public void recordMethodAlias(String newName, String oldName) {
        _methodAliases.put(oldName, newName);
    }

        // Unalias 'name' and return new name
    public String unaliasMethodName(String name) {
        String n = name;
        String a = null;
        do {
            a = _methodAliases.get(n);
            if (a != null)
                n = a;
        } while (a != null);

        return n;
    }

    public List<IR_Instr> getInstrs() { return null; }

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
        Operand cv = _constMap.get(constRef);
        Operand p  = _parent;
        // SSS FIXME: Traverse up the scope hierarchy to find the constant as long as the parent is a static scope
        if ((cv == null) && (p != null) && (p instanceof MetaObject)) {
            // Can be null for IR_Script meta objects
            if (((MetaObject)p)._scope == null) {
//                System.out.println("Looking for core class: " + constRef);
                IR_Class coreClass = IR_Module.getCoreClass(constRef);
                if (coreClass != null)
                    return new MetaObject(coreClass);
                return null;
            }
            cv = ((MetaObject)p)._scope.getConstantValue(constRef);
        }

        return cv;
    }

    public void setConstantValue(String constRef, Operand val) {
        if (val.isConstant())
            _constMap.put(constRef, val); 

        if (this instanceof IR_Module)
            ((IR_Module)this).getRootMethod().addInstr(new PUT_CONST_Instr(this, constRef, val));
    }

    public Map getConstants() {
        return Collections.unmodifiableMap(_constMap);
    }

    public String toString() {
        return (_constMap.isEmpty() ? "" : "\n  constants: " + _constMap);
    }

    protected void runCompilerPassOnNestedScopes(CompilerPass p) {
        if (!_modules.isEmpty())
            for (IR_Scope m: _modules)
                m.runCompilerPass(p);

        if (!_classes.isEmpty())
            for (IR_Scope c: _classes)
                c.runCompilerPass(p);

        if (!_methods.isEmpty())
            for (IR_Scope meth: _methods)
                meth.runCompilerPass(p);
    }

    public void runCompilerPass(CompilerPass p) {
        boolean isPreOrder =  p.isPreOrder();
        if (isPreOrder)
            p.run(this);

        runCompilerPassOnNestedScopes(p);

        if (!isPreOrder)
            p.run(this);
    }

    public String toStringInstrs() { return ""; }

    public String toStringVariables() { return ""; }
}
