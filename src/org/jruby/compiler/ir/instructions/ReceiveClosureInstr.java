package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.Ruby;
import org.jruby.runtime.Block;
import org.jruby.runtime.Block.Type;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/* Receive the closure argument (either implicit or explicit in Ruby source code) */
public class ReceiveClosureInstr extends NoOperandInstr {
    public ReceiveClosureInstr(Variable dest) {
        super(Operation.RECV_CLOSURE, dest);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
		  // SSS FIXME: This is not strictly correct -- we have to wrap the block into an
		  // operand type that converts the static code block to a proc which is a closure.
        return new CopyInstr(ii.getRenamedVariable(getResult()), ii.getCallClosure());
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Block blk = interp.getBlock();
        Ruby  runtime = context.getRuntime();
        getResult().store(interp, context, self, blk == Block.NULL_BLOCK ? runtime.getNil() : runtime.newProc(Type.PROC, blk));
        return null;
    }
}
