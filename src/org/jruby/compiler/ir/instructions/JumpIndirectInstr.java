package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Used in ensure blocks to jump to the label contained in '_target'
public class JumpIndirectInstr extends OneOperandInstr {
    public JumpIndirectInstr(Variable target) {
        super(Operation.JUMP_INDIRECT, null, target);
    }

    public Variable getJumpTarget() {
        return (Variable) getArg();
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new JumpIndirectInstr(ii.getRenamedVariable(getJumpTarget()));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return (Label) (getArg().retrieve(interp, context, self));
    }
}
