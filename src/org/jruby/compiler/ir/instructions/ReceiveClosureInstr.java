package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction receives the last argument from the argument array and removes it from the argument array.
// It is important to remove the block from the argument array so that a splat (*x) can receive the "rest" of the args
// minus the block itself.
//
// (Most likely, this will be implemented by decrementing the length counter of the argument array.)
public class ReceiveClosureInstr extends NoOperandInstr {
    public ReceiveClosureInstr(Variable dest) {
        super(Operation.RECV_CLOSURE, dest);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, interp.getRuntime().newProc(Type.PROC, interp.getBlock()));
        return null;
    }
}
