package org.jruby.compiler.ir.instructions;

/**
 * This instructions gets the heap frame for the current execution scope.
 * If none exists, it allocates a heap frame and initializes this field in the execution scope.
 **/

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IR_ExecutionScope;

public class GET_FRAME_Instr extends OneOperandInstr
{
    public GET_FRAME_Instr(Variable result, IR_ExecutionScope scope) {
        super(Operation.GET_FRAME, result, new MetaObject(scope));
    }
}
