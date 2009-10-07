package org.jruby.compiler.ir;

// Closures are contexts/scopes for the purpose of IR building.  They are self-contained and accummulate instructions

import org.jruby.compiler.ir.instructions.GET_CONST_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// that don't merge into the flow of the containing scope.  They are manipulated as an unit.
public class IR_Closure extends IR_ScopeImpl
{
    public final Label _startLabel; // Label for the start of the closure (used to implement redo)
    public final Label _endLabel;   // Label for the end of the closure (used to implement retry)
    public final String _name;      // Name useful for debugging and reading ir output

    private static int _closureCounter = 0;

    public IR_Closure(IR_Scope parent, IR_Scope lexicalParent)
    { 
        super(parent, lexicalParent);
        _startLabel = getNewLabel("_CLOSURE_START_");
        _endLabel   = getNewLabel("_CLOSURE_END_");
        _name = "_CL_" + _closureCounter;
        _closureCounter++;
    }

    public void setConstantValue(String constRef, Operand val) 
    {
        throw new org.jruby.compiler.NotCompilableException("Unexpected: Encountered set constant value in a closure!");
    }

    public String toString() { return _name; }

    public String toStringBody() {
       StringBuffer buf = new StringBuffer();
       buf.append("CLOSURE: ").append(_name).append(" = { \n");
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
