package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.Ruby;
import org.jruby.runtime.Block;
import org.jruby.runtime.Frame;
import org.jruby.runtime.Block.Type;

/* Receiver the closure argument (either implicit or explicit in Ruby source code) */
public class ReceiveClosureInstr extends NoOperandInstr {
    public ReceiveClosureInstr(Variable dest) {
        super(Operation.RECV_CLOSURE, dest);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallClosure());
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp) {
        Block blk = interp.getBlock();
        Ruby  runtime = interp.getRuntime();
        getResult().store(interp, blk == Block.NULL_BLOCK ? runtime.getNil() : runtime.newProc(Type.PROC, blk));
        return null;
    }
}
