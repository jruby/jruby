package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
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
import org.jruby.compiler.ir.representations.CFG;

public abstract class IR_ScopeImpl implements IR_Scope
{
    Operand        _parent;   // Parent container for this context (can be dynamic!!)
                              // If dynamic, at runtime, this will be the meta-object corresponding to a class/script/module/method/closure
    List<IR_Instr> _instrs;   // List of IR instructions for this method

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

        // NOTE: Since we are processing ASTs, loop bodies are processed in depth-first manner
        // with outer loops encountered before inner loops, and inner loops finished before outer ones.
        //
        // So, we can keep track of loops in a loop stack which  keeps track of loops as they are encountered.
        // This lets us implement next/redo/break/retry easily for the non-closure cases
    private Stack<IR_Loop> _loopStack;

        // Control flow graph for this scope
    private CFG _cfg;

    private int _nextMethodIndex;
    
        // List of modules, classes, and methods defined in this scope!
    final public List<IR_Module> _modules = new ArrayList<IR_Module>();
    final public List<IR_Class>  _classes = new ArrayList<IR_Class>();
    final public List<IR_Method> _methods = new ArrayList<IR_Method>();

    private void init(Operand parent, IR_Scope lexicalParent)
    {
        _parent = parent;
        _instrs = new ArrayList<IR_Instr>();
        _nextVarIndex = new HashMap<String, Integer>();
        _constMap = new HashMap<String, Operand>();
        _loopStack = new Stack<IR_Loop>();
        _nextMethodIndex = 0;
//        _lexicalNestingLevel = lexicalParent == null ? 0 : ((IR_ScopeImpl)lexicalParent)._lexicalNestingLevel + 1;
    }

    public IR_ScopeImpl(IR_Scope parent, IR_Scope lexicalParent)
    {
        init(new MetaObject(parent), lexicalParent);
    }

    public IR_ScopeImpl(Operand parent, IR_Scope lexicalParent)
    {
        init(parent, lexicalParent);
    }

        // Returns the containing parent scope!
    public Operand getParent()
    {
        return _parent;
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

        // Insert nesting level to ensure variable names don't conflict across nested scopes!
        // i.e. definition of %v_1 in a closure shouldn't override the use of %v_1 from the parent scope!
//        return new Variable(prefix + _lexicalNestingLevel + "_" + idx);
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

    public void addModule(IR_Module m) 
    {
        setConstantValue(m._name, new MetaObject(m)) ;
        _modules.add(m);
    }

    public void addClass(IR_Class c)
    { 
        setConstantValue(c._name, new MetaObject(c));
        _classes.add(c);
    }

    public void addMethod(IR_Method m) {
        _methods.add(m);
        if ((this instanceof IR_Method) && ((IR_Method)this).isAClassRootMethod()) {
            IR_Class c = (IR_Class)(((MetaObject)this._parent)._scope);
            addInstr(m._isInstanceMethod ? new DEFINE_INSTANCE_METHOD_Instr(c, m) : new DEFINE_CLASS_METHOD_Instr(c, m));
        }
        else if (m._isInstanceMethod && (this instanceof IR_Class)) {
            IR_Class c = (IR_Class)this;
            addInstr(new DEFINE_INSTANCE_METHOD_Instr(c, m));
        }
        else if (!m._isInstanceMethod && (this instanceof IR_Module)) {
            IR_Module c = (IR_Module)this;
            addInstr(new DEFINE_CLASS_METHOD_Instr(c, m));
        }
        else {
            throw new RuntimeException("Encountered method add in a non-class scope!");
        }
    }

    public void addInstr(IR_Instr i)   { _instrs.add(i); }

    // SSS FIXME: Deprecated!  Going forward, all instructions should come from the CFG
    public List<IR_Instr> getInstrs() { return _instrs; }

    public CFG buildCFG()
    {
        _cfg = new CFG(this);
        _cfg.build(_instrs);
        return _cfg;
    }

    public CFG getCFG()
    {
        return _cfg;
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
    public Operand getConstantValue(String constRef)
    {
        // System.out.println("Looking in " + this + " for constant: " + constRef);
        Operand cv = _constMap.get(constRef);
        Operand p  = _parent;
        // SSS FIXME: Traverse up the scope hierarchy to find the constant as long as the parent is a static scope
        if ((cv == null) && (p != null) && (p instanceof MetaObject))
            cv = ((MetaObject)p)._scope.getConstantValue(constRef);

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
        return (_constMap.isEmpty() ? "" : "\n  constants: " + _constMap);
    }

    public void runCompilerPass(CompilerPass p)
    {
        boolean isPreOrder =  p.isPreOrder();

        if (isPreOrder)
            p.run(this);

        if (!_modules.isEmpty())
            for (IR_Scope m: _modules)
                m.runCompilerPass(p);

        if (!_classes.isEmpty())
            for (IR_Scope c: _classes)
                c.runCompilerPass(p);

        if (!_methods.isEmpty())
            for (IR_Scope meth: _methods)
                meth.runCompilerPass(p);

        if (!isPreOrder)
            p.run(this);
    }

    public String toStringInstrs() {
        StringBuilder b = new StringBuilder();

        int i = 0;
        for (IR_Instr instr : _instrs) {
            if (i > 0) b.append("\n");
            b.append("  " + i++ + "\t");
				if (instr.isDead())
					b.append("[DEAD]");
            b.append(instr);
        }

        return b.toString();
    }

    public String toStringVariables() {
        StringBuilder sb = new StringBuilder();
        Map<Variable, Integer> ends = new HashMap<Variable, Integer>();
        Map<Variable, Integer> starts = new HashMap<Variable, Integer>();
        SortedSet<Variable> variables = new TreeSet<Variable>();
        
        for (int i = _instrs.size() - 1; i >= 0; i--) {
            IR_Instr instr = _instrs.get(i);
            Variable var = instr._result;

            if (var != null) {
                variables.add(var);
                starts.put(var, i);
            }

            for (Operand operand : instr.getOperands()) {
                if (operand != null && operand instanceof Variable && ends.get((Variable)operand) == null) {
                    ends.put((Variable)operand, i);
                    variables.add((Variable)operand);
                }
            }
        }

        int i = 0;
        for (Variable var : variables) {
            Integer end = ends.get(var);
            if (end == null) {
                // variable is never read, variable is never live
            } else {
                if (i > 0) sb.append("\n");
                i++;
                sb.append("    " + var + ": " + starts.get(var) + "-" + end);
            }
        }

        return sb.toString();
    }
}
