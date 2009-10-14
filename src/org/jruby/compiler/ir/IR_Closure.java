package org.jruby.compiler.ir;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accumulate instructions
// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
// Their parents are always execution scopes.

import org.jruby.compiler.ir.instructions.GET_CONST_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class IR_Closure extends IR_ExecutionScope
{
    public final Label  _startLabel; // Label for the start of the closure (used to implement redo)
    public final Label  _endLabel;   // Label for the end of the closure (used to implement retry)
    public final int    _closureId;  // Unique id for this closure within the nearest ancestor method.
    public final String _name;       // Name useful for debugging and reading ir output

    public IR_Closure(IR_Scope parent, IR_Scope lexicalParent)
    {
        super(parent, lexicalParent);
        _startLabel = getNewLabel("_CLOSURE_START_");
        _endLabel   = getNewLabel("_CLOSURE_END_");
        _closureId  = getNextClosureId();
        _name       = "_CLOSURE_" + _closureId;
    }

    public int getNextClosureId()
    {
        return _lexicalParent.getNextClosureId();
    }

    public Variable getNewVariable()
    {
        return getNewVariable("%cl" + _closureId + "_v_");
    }

    public void setConstantValue(String constRef, Operand val) 
    {
        throw new org.jruby.compiler.NotCompilableException("Unexpected: Encountered set constant value in a closure!");
    }

    public String toString() { return _name; }

    public String toStringBody() {
       StringBuffer buf = new StringBuffer();
       buf.append(_name).append(" = { \n");
       org.jruby.compiler.ir.representations.CFG c = getCFG();
       if (c != null) {
            buf.append("\nCFG:\n").append(c.getGraph().toString());
            buf.append("\nInstructions:\n").append(c.toStringInstrs());
       }
       else {
            buf.append(toStringInstrs());
       }
       buf.append("\n}\n\n"); 
       return buf.toString();
    }
}
