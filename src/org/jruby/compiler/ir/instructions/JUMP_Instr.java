package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class JUMP_Instr extends NoOperandInstr
{
    public final Label _target; 

    public JUMP_Instr(Label l) {
        super(Operation.JUMP);
        _target = l;
    }

    public String toString() {
        return super.toString() + " " + _target;
    }

    public Label getJumpTarget() { return _target; }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new JUMP_Instr(ii.getRenamedLabel(_target));
    }
}
