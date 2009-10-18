package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IR_ExecutionScope;

public class LOAD_FROM_FRAME_Instr extends GET_Instr
{
    public LOAD_FROM_FRAME_Instr(Variable v, IR_ExecutionScope scope, String slotName)
    {
        super(Operation.FRAME_LOAD, v, new MetaObject(scope), slotName);
    }
}
