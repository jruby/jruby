package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Used in ensure blocks to jump to the label contained in '_target'
public class JumpIndirectInstr extends Instr {
    private Operand target;

    public JumpIndirectInstr(Variable target) {
        super(Operation.JUMP_INDIRECT);
        this.target = target;
    }

    public Variable getJumpTarget() {
        return (Variable) target;
    }

    public Operand[] getOperands() {
        return new Operand[]{target};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        target = target.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + target + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new JumpIndirectInstr(ii.getRenamedVariable(getJumpTarget()));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        return (Label) (target.retrieve(interp, context, self));
    }
}
