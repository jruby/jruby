package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

// Represents result = source.ref or result = source where source is not a stack variable
public abstract class GetInstr extends Instr {
    private Operand source;
    private String  ref;

    public GetInstr(Operation op, Variable dest, Operand source, String ref) {
        super(op, dest);
        
        this.source = source;
        this.ref = ref;
    }

    public String getName() {
        return ref;
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

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        source = source.getSimplifiedOperand(valueMap);
    }
}
