package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.IRMethod;

// Rather than building a zillion instructions that capture calls to ruby implementation internals,
// we are building one that will serve as a placeholder for internals-specific call optimizations.
public class RUBY_INTERNALS_CALL_Instr extends CallInstruction {
    public RUBY_INTERNALS_CALL_Instr(Variable result, Operand methAddr, Operand[] args) {
        super(Operation.RUBY_INTERNALS, result, methAddr, args, null);
    }

    public RUBY_INTERNALS_CALL_Instr(Variable result, Operand methAddr, Operand[] args, Operand closure) {
        super(result, methAddr, args, closure);
    }

    @Override
    public boolean isRubyInternalsCall() {
        return true;
    }

    @Override
    public boolean isStaticCallTarget() {
        return true;
    }

    // SSS FIXME: Who is the receiver in these cases??
    @Override
    public Operand getReceiver() {
        return null;
    }

    // SSS FIXME: Dont optimize these yet!
    @Override
    public IRMethod getTargetMethodWithReceiver(Operand receiver) {
        return null;
    }
}
