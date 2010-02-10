package org.jruby.compiler.ir.instructions;

/**
 * This instructions allocates a heap frame for the current execution scope.
 * Does nothing if a frame already exists.
 **/

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IRMethod;

import java.util.Map;

public class ALLOC_FRAME_Instr extends IR_Instr {
    IR_ExecutionScope scope;   // Scope for which frame is needed

    public ALLOC_FRAME_Instr(IR_ExecutionScope scope) {
        super(Operation.ALLOC_FRAME);
        
        this.scope = getClosestMethodAncestor(scope);
    }

    public Operand[] getOperands() { 
        return new Operand[] { new MetaObject(scope) };
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {}

    private static IRMethod getClosestMethodAncestor(IR_ExecutionScope scope) {
        while (!(scope instanceof IRMethod)) {
            scope = (IR_ExecutionScope)scope.getLexicalParent();
        }

        return (IRMethod)scope;
    }

    @Override
    public String toString() {
        return "\t" + _op + "(" + scope + ")";
    }
}
