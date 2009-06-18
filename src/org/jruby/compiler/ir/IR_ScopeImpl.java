package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class IR_ScopeImpl implements IR_Scope
{
    IR_Scope       _parent;   // Parent container for this context
    List<IR_Instr> _instrs;   // List of ir instructions for this method

    private Map<String, Integer> _nextVarIndex;

	 public IR_ScopeImpl(IR_Scope parent)
	 {
        _parent = parent;
        _instrs = new ArrayList<IR_Instr>();
        _nextVarIndex = new HashMap<String, Integer>();
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

      // Delegate method to the containing script/module/class
    public StringLiteral getFileName() { return _parent.getFileName(); }

    public void addInstr(IR_Instr i) { _instrs.append(i); }
}
