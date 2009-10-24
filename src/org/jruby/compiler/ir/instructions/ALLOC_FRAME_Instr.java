package org.jruby.compiler.ir.instructions;

/**
 * This instructions allocates a heap frame for the current execution scope.
 * Does nothing if a frame already exists.
 **/

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.IR_Scope;

import java.util.Map;

public class ALLOC_FRAME_Instr extends IR_Instr
{
    IR_ExecutionScope _scope;   // Scope for which frame is needed

    public ALLOC_FRAME_Instr(IR_ExecutionScope scope) {
        super(Operation.ALLOC_FRAME);
        _scope = getClosestMethodAncestor(scope);
    }

    public Operand[] getOperands() { return new Operand[] { new MetaObject(_scope) }; } 

    public void simplifyOperands(Map<Operand, Operand> valueMap) {}

    private static IR_Method getClosestMethodAncestor(IR_ExecutionScope scope)
    {
        while (!(scope instanceof IR_Method))
            scope = (IR_ExecutionScope)scope.getLexicalParent();

        return (IR_Method)scope;
    }

    public String toString()
    {
        return "\t" + _op + "(" + _scope + ")";
    }
}
