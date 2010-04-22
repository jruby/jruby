package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class JRUBY_IMPL_CALL_Instr extends CallInstruction {
    public JRUBY_IMPL_CALL_Instr(Variable result, Operand methAddr, Operand[] args) {
        super(Operation.JRUBY_IMPL, result, methAddr, args, null);
    }

    public JRUBY_IMPL_CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure) {
        super(result, methAddr, args, closure);
    }

    public boolean isStaticCallTarget() {
        return true;
    }

    public IR_Instr cloneForInlining(InlinerInfo ii) {
        return new JRUBY_IMPL_CALL_Instr(ii.getRenamedVariable(_result), _methAddr.cloneForInlining(ii), super.cloneCallArgs(ii), _closure == null ? null : _closure.cloneForInlining(ii));
    }
}
