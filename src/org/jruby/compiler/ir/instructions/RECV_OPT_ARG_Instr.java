package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.ArgIndex;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// Assign the 'index' argument to 'dest'.
// If it is not null, you are all done
// If null, you jump to 'nullLabel' and execute that code.
public class RECV_OPT_ARG_Instr extends TwoOperandInstr
{
    public RECV_OPT_ARG_Instr(Variable dest, int index, Label nullLabel) {
        super(Operation.RECV_OPT_ARG, dest, new ArgIndex(index), nullLabel);
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        Operand callArg = ii.getCallArg(((ArgIndex)_arg1)._index);
        if (callArg == null) // FIXME: Or should we also have a check for Nil.NIL?
            return new JUMP_Instr(ii.getRenamedLabel((Label)_arg2));
        else
            return new COPY_Instr(ii.getRenamedVariable(_result), callArg);
    }
}
