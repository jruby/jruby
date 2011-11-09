package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// Represents result = source.ref or result = source where source is not a stack variable
public abstract class GetInstr extends Instr implements ResultInstr {
    private Operand source;
    private String  ref;
    private final Variable result;

    public GetInstr(Operation op, Variable result, Operand source, String ref) {
        super(op);
        
        assert result != null: "" + getClass().getSimpleName() + " result is null";
        
        this.source = source;
        this.ref = ref;
        this.result = result;
    }

    public String getRef() {
        return ref;
    }
    
    public Variable getResult() {
        return result;
    }

    public Operand[] getOperands() { 
        return new Operand[] { source };
    }

    public Operand getSource() {
        return source;
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + source + (ref == null ? "" : ", " + ref) + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        source = source.getSimplifiedOperand(valueMap);
    }
}
