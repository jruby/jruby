package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public abstract class IR_ScopeImpl implements IR_Scope
{
    IR_Scope       _parent;   // Parent container for this context
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

    public IR_ScopeImpl(IR_Scope parent)
    {
        _parent = parent;
        _instrs = new ArrayList<IR_Instr>();
        _nextVarIndex = new HashMap<String, Integer>();
        _constMap = new HashMap<String, Operand>();
        _loopStack = new Stack<IR_Loop>();
    }

    public Variable getNewVariable(prefix)
    {
        if (prefix == null)
            prefix = "tmp";

        Integer idx = _nextVarIndex.get(prefix);
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(prefix, idx+1);
        return new Variable(prefix + idx);
    }

    public Variable getNewVariable()
    {
       return getNewVariable("tmp");
    }

    public Label getNewLabel(String lblPrefix)
    {
        Integer idx = _nextVarIndex.get(lblPrefix);
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(lblPrefix, idx+1);
        return new Label(lblPrefix + idx);
    }

    public Label getNewLabel()
    {
       return new Label("LBL_");
    }

        // get "self"
    public Variable getSelf() { return new Variable("self"); }

        // Delegate method to the containing script/module/class
    public String getFileName() { return _parent.getFileName(); }

    public void addInstr(IR_Instr i) { _instrs.append(i); }

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
            cv = getNewTmpVariable();
            addInstr(new GET_CONST_Instr(cv, this, constRef));
        }
        return cv;
    }

    public void setConstantValue(String constRef, Operand val) 
    {
        if (val.isConstant())
            _constMap.put(constRef, val); 

        addInstr(new PUT_CONST_Instr(this, constRef, val);
    }

    public startLoop(IR_Loop l) { _loopStack.push(l); }

    public endLoop(IR_Loop l) { _loopStack.pop(); /* SSS FIXME: Do we need to check if l is same as whatever popped? */ }

    public IR_Loop getCurrentLoop() { _loopStack.peek(); }
}
