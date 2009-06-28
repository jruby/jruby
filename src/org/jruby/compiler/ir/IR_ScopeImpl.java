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

        // SSS FIXME: This may not work all that well -- see note below
    public Operand getConstantValue(String constRef) { _constMap.get(constRef); }

        // SSS FIXME: This may not work all that well if this is not really a constant but
        // a placeholder -- can happen because of temporary variable issues
    public void setConstantValue(String constRef, Operand val) 
    {
        if (val.isConstant())
            _constMap.put(constRef, val); 
    }

    public startLoop(IR_Loop l) { _loopStack.push(l); }

    public endLoop(IR_Loop l) { _loopStack.pop(); /* SSS FIXME: Do we need to check if l is same as whatever popped? */ }

    public IR_Loop getCurrentLoop() { _loopStack.peek(); }
}
