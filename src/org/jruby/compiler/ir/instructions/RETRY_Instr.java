package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;

// SSS FIXME: Should I have a reference to the IR_loop that is being retried?
public class RETRY_Instr extends OneOperandInstr
{
    Label _jumpLabel;

    public RETRY_Instr(Label loopStart)
    {
        super(Operation.RETRY, null, null);
        _jumpLabel = loopStart;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new RETRY_Instr(ii.getRenamedLabel(_jumpLabel));
    }
}
