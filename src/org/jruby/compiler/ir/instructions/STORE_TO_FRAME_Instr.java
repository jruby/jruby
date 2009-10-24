package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IR_ExecutionScope;
import org.jruby.compiler.ir.IR_Method;
import org.jruby.compiler.ir.IR_Scope;

public class STORE_TO_FRAME_Instr extends PUT_Instr
{
    public STORE_TO_FRAME_Instr(IR_ExecutionScope scope, String slotName, Operand value)
    {
        super(Operation.FRAME_STORE, new MetaObject(getClosestMethodAncestor(scope)), slotName, value);
    }

    private static IR_Method getClosestMethodAncestor(IR_ExecutionScope scope)
    {
        while (!(scope instanceof IR_Method))
            scope = (IR_ExecutionScope)scope.getLexicalParent();

        return (IR_Method)scope;
    }

    public String toString() {
        return "\tFRAME(" + _target + ")." + _ref + " = " + _value;
    }
}
