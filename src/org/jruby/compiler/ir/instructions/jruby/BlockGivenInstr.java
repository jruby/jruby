package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.NoOperandInstr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BlockGivenInstr extends NoOperandInstr {
    public BlockGivenInstr(Variable result) {
        super(Operation.BLOCK_GIVEN, result);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new BlockGivenInstr(ii.getRenamedVariable(getResult()));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        getResult().store(interp, context, self, context.getRuntime().newBoolean(interp.getBlock().isGiven()));
        return null;
    }
}
