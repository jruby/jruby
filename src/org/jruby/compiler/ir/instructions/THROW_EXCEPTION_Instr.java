package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class THROW_EXCEPTION_Instr extends Instr
{
    Operand _exception;

    public THROW_EXCEPTION_Instr(Operand exc)
    {
        super(Operation.THROW);
        _exception = exc;
    }

    public String toString() { return super.toString() + "(" + _exception + ")"; }

    public Operand[] getOperands() { return new Operand[] { _exception }; }

    public void simplifyOperands(Map<Operand, Operand> valueMap) { _exception = _exception.getSimplifiedOperand(valueMap); }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new THROW_EXCEPTION_Instr(_exception.cloneForInlining(ii));
    }
}
