package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

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
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        target = target.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + target + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new JumpIndirectInstr(ii.getRenamedVariable(getJumpTarget()));
    }
}
