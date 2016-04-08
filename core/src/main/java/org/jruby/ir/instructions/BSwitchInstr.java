package org.jruby.ir.instructions;

import org.jruby.RubyFixnum;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

/**
 * Represents a multiple-target jump instruction based on a switch-like table
 */
public class BSwitchInstr extends MultiBranchInstr {
    private final int[] jumps;
    private Operand operand;
    private final Label rubyCase;
    private final Label[] targets;
    private final Label elseTarget;

    public BSwitchInstr(int[] jumps, Operand operand, Label rubyCase, Label[] targets, Label elseTarget) {
        super(Operation.B_SWITCH);
        this.jumps = jumps;
        this.operand = operand;
        this.rubyCase = rubyCase;
        this.targets = targets;
        this.elseTarget = elseTarget;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] {operand};
    }

    public Operand getCaseOperand() {
        return operand;
    }

    public int[] getJumps() {
        return jumps;
    }

    public Label getRubyCaseLabel() {
        return rubyCase;
    }

    public Label[] getTargets() {
        return targets;
    }
    public Label getElseTarget() {
        return elseTarget;
    }

    @Override
    public void setOperand(int i, Operand operand) {
        assert i == 0;
        this.operand = operand;
    }

    @Override
    public Instr clone(CloneInfo info) {
        Operand operand = this.operand.cloneForInlining(info);
        Label rubyCase = info.getRenamedLabel(this.rubyCase);
        Label[] targets = new Label[this.targets.length];
        for (int i = 0; i < targets.length; i++) targets[i] = info.getRenamedLabel(this.targets[i]);
        Label elseTarget = info.getRenamedLabel(this.elseTarget);
        return new BSwitchInstr(jumps, operand, rubyCase, targets, elseTarget);
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object result = operand.retrieve(context, self, currScope, currDynScope, temp);
        if (!(result instanceof RubyFixnum)) {
            // not a fixnum, fall back on old case logic
            return rubyCase.getTargetPC();
        }
        int value = ((RubyFixnum) result).getIntValue();
        int index = -1;
        // TODO: this is obviously linear time
        for (int i = 0; i < jumps.length; i++) {
            if (jumps[i] == value) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return elseTarget.getTargetPC();
        }

        return targets[index].getTargetPC();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BSwitchInstr(this);
    }

    @Override
    public Label[] getJumpTargets() {
        Label[] jumpTargets = new Label[targets.length + 2];
        jumpTargets[0] = rubyCase;
        for (int i = 0; i < targets.length; i++) {
            jumpTargets[i + 1] = targets[i];
        }
        jumpTargets[jumpTargets.length - 1] = elseTarget;
        return jumpTargets;
    }
}
