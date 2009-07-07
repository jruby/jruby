package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public abstract class IR_ScopeImpl implements IR_Scope
{
    Operand        _parent;   // Parent container for this context (can be dynamic!!)
                              // If dynamic, at runtime, this will be the meta-object corresponding to a class/script/module/method/closure
    List<IR_Instr> _instrs;   // List of IR instructions for this method

        // Map of constants defined in this scope (not valid for methods!)
    private Map<String, Operand> _constMap;

        // Map keep track of the next available variable index for a particular prefix
    private Map<String, Integer> _nextVarIndex;

        // NOTE: Since we are processing ASTs, loop bodies are processed in depth-first manner
        // with outer loops encountered before inner loops, and inner loops finished before outer ones.
        //
        // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
        // This lets us implement next/redo/break/retry easily for the non-closure cases
    private Stack<IR_Loop> _loopStack;

    private int _nextMethodIndex;
    
        // List of modules, classes, and methods defined in this scope!
    final public List<IR_Module> _modules = new ArrayList<IR_Module>();
    final public List<IR_Class>  _classes = new ArrayList<IR_Class>();
    final public List<IR_Method> _methods = new ArrayList<IR_Method>();

    private void init(Operand parent)
    {
        _parent = parent;
        _instrs = new ArrayList<IR_Instr>();
        _nextVarIndex = new HashMap<String, Integer>();
        _constMap = new HashMap<String, Operand>();
        _loopStack = new Stack<IR_Loop>();
        _nextMethodIndex = 0;
    }

    public IR_ScopeImpl(IR_Scope parent)
    {
       init(new MetaObject(parent));
    }

    public IR_ScopeImpl(Operand parent)
    {
       init(parent);
    }

    public Variable getNewVariable(String prefix)
    {
        if (prefix == null)
            prefix = "%v_";

        // We need to ensure that the variable names generated here cannot conflict with ruby variable names!
        // Hence the "%" tthat is appended to the beginning!
        if (!prefix.startsWith("%"))
            prefix += "%";

        Integer idx = _nextVarIndex.get(prefix);
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(prefix, idx+1);
        return new Variable(prefix + idx);
    }

    public Variable getNewVariable()
    {
       return getNewVariable("%v_");
    }

    public Label getNewLabel(String lblPrefix)
    {
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

        // Delegate method to the containing script/module/class
    public Operand getFileName() 
    {
            // Static scope
        if (_parent instanceof MetaObject) {
            return ((MetaObject)_parent)._scope.getFileName();
        }
            // Dynamic scope!
        else {
            Variable fn = getNewVariable();
                // At runtime, the parent operand will be the meta-object (runtime object) representing a script/module/class/method
            addInstr(new JRUBY_IMPL_CALL_Instr(fn, MethAddr.GET_FILE_NAME, new Operand[]{_parent}));
            return fn;
        }
    }

    public void addModule(IR_Module m) 
    {
        setConstantValue(m._moduleName, new MetaObject(m)) ;
        _modules.add(m);
    }

    public void addClass(IR_Class c)
    { 
        setConstantValue(c._className, new MetaObject(c));
        _classes.add(c);
    }

    public void addMethod(IR_Method m) {
        _methods.add(m);
        if (this instanceof IR_Class)
            addInstr(m._isInstanceMethod ? new DEFINE_INSTANCE_METHOD_Instr((IR_Class)this, m) : new DEFINE_CLASS_METHOD_Instr((IR_Class)this, m));
        else
            throw new RuntimeException("Encountered method add in a non-class scope!");
    }

    public void addInstr(IR_Instr i)   { _instrs.add(i); }

    public List getInstrs() {
        return Collections.unmodifiableList(_instrs);
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
        // SSS FIXME: Is this just a premature optimization?  Should we instead introduce 
        // PUT_CONST_Instr and GET_CONST_Instr instructions always?
        //
    public Operand getConstantValue(String constRef) 
    { 
        Operand cv = _constMap.get(constRef); 
        if (cv == null) {
            Variable v = getNewVariable();
            addInstr(new GET_CONST_Instr(v, this, constRef));
            cv = v;
        }
        return cv;
    }

    public void setConstantValue(String constRef, Operand val) 
    {
        if (val.isConstant())
            _constMap.put(constRef, val); 

        addInstr(new PUT_CONST_Instr(this, constRef, val));
    }

    public Map getConstants() {
        return Collections.unmodifiableMap(_constMap);
    }

    public void startLoop(IR_Loop l) { _loopStack.push(l); }

    public void endLoop(IR_Loop l) { _loopStack.pop(); /* SSS FIXME: Do we need to check if l is same as whatever popped? */ }

    public IR_Loop getCurrentLoop() { return _loopStack.peek(); }

    public String toString() {
        return
                (_constMap.isEmpty() ? "" : "\n  constants: " + _constMap) +
                (_instrs.isEmpty() ? "" : "\n  instrs:\n" + toStringInstrs()) +
                (_modules.isEmpty() ? "" : "\n  modules:\n" + _modules) +
                (_classes.isEmpty() ? "" : "\n  classes:\n" + _classes) +
                (_methods.isEmpty() ? "" : "\n  methods:\n" + _methods);
    }

    public String toStringInstrs() {
        StringBuilder b = new StringBuilder();

        int i = 0;
        for (IR_Instr instr : _instrs) {
            b.append("  " + i++ + "\t");
            b.append(instr);
            b.append("\n");
        }

        return b.toString();
    }
}
