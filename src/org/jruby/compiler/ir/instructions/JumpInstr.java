package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class JumpInstr extends Instr {
    public final Label target;

    public JumpInstr(Label target) {
        super(Operation.JUMP);
        this.target = target;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public String toString() {
        return super.toString() + " " + target;
    }

    public Label getJumpTarget() {
        return target;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new JumpInstr(ii.getRenamedLabel(target));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self, org.jruby.runtime.Block block) {
        return target;
    }
}
