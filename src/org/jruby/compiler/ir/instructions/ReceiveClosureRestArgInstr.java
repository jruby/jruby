package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

// This instruction encodes the receive of an rest argument into a closure
//   Ex:  .. { |.. *a| .. }
// The closure receives '*a' via this instruction
public class ReceiveClosureRestArgInstr extends ReceiveArgBase {
    public ReceiveClosureRestArgInstr(Variable result, int argIndex) {
        super(Operation.RECV_CLOSURE_REST_ARG, result, argIndex);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }
}
