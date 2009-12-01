package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;

public class RECV_EXCEPTION_Instr extends NoOperandInstr
{
    public RECV_EXCEPTION_Instr(Variable dest) { super(Operation.RECV_EXCEPTION, dest); }
}
