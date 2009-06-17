package org.jruby.compiler.ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jruby.util.JavaNameMangler;

public class IR_Method implements IR_BuilderContext
{
    IR_BuilderContext _container;   // The container for this method (can be a script, class, or module)
    String         _name;        // Ruby name 
    String         _irName;      // Generated name
    List<IR_Instr> _instrs;      // List of ir instructions for this method

    private Map<String, Integer> _nextVarIndex;

    public IR_Method(String name, boolean isRoot)
    {
        _name = name;
        if (root && Boolean.getBoolean("jruby.compile.toplevel")) {
            _irName = name;
        } else {
            String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
            // FIXME: What is this script business here?
            _irName = "method__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;
        }

        _instrs = new ArrayList<IR_Instr>();
        _nextVarIndex = new HashMap<String, Integer>();
    }

    public Variable getNewVariable(String name)
    {
        return new Variable(prefix + idx);
    }

    public Variable getNewVariableWithPrefix(prefix)
    {
        Integer idx = _nextVarIndex.get(prefix);
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(prefix, idx+1);
        return new Variable(prefix + idx);
    }

    public Variable getNewTmpVariable()
    {
       return getNewVariableWithPrefix("tmp");
    }

    public Label getNewLabel()
    {
        Integer idx = _nextVarIndex.get("LBL_");
        if (idx == null)
            idx = 0;
        _nextVarIndex.put(prefix, idx+1);
        return new Label(prefix + idx);
    }

      // Delegate method to the containing class/module
    public StringLiteral getFileName() { return _container.getFileName(); }

    public void addInstr(IR_Instr i) { _instrs.append(i); }
}
