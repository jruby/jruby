package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IR_ScopeImpl implements IR_Scope
{
    IR_Scope       _parent;   // Parent container for this context
    List<IR_Instr> _instrs;   // List of ir instructions for this method

        // Map of constants defined in this scope (not valid for methods!)
    private Map<String, Operand> _constMap;

        // Map keep track of the next available variable index for a particular prefix
    private Map<String, Integer> _nextVarIndex;

    public IR_ScopeImpl(IR_Scope parent)
    {
        _parent = parent;
        _instrs = new ArrayList<IR_Instr>();
        _nextVarIndex = new HashMap<String, Integer>();
        _constMap = new HashMap<String, Operand>();
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

    public Label getNewLabel()
    {
        Integer idx = _nextVarIndex.get("LBL_");
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(prefix, idx+1);
        return new Label(prefix + idx);
    }

        // get "self"
    public Variable getSelf() { return new Variable("self"); }

      // Delegate method to the containing script/module/class
    public StringLiteral getFileName() { return _parent.getFileName(); }

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
}
