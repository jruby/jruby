package org.jruby.ir.instructions;

import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

/**
 * Represents a multiple-target jump instruction based on a switch-like table
 */
public class BSwitchInstr extends MultiBranchInstr {
    private final Operand[] jumpOperands;
    private final int[] jumps;
    private Operand operand;
    private final Label rubyCase;
    private final Label[] targets;
    private final Label elseTarget;
    private final Class expectedClass;

    public BSwitchInstr(int[] jumps, Operand[] jumpOperands, Operand operand, Label rubyCase, Label[] targets,
                        Label elseTarget, Class expectedClass) {
        super(Operation.B_SWITCH);

        // We depend on the jump table being sorted, so ensure that's the case here
        assert jumpsAreSorted(jumps) : "jump table must be sorted";

        // Switch cases must not have an empty "case" value (GH-6440)
        assert operand != null : "Switch cases must not have an empty \"case\" value";

        this.jumpOperands = jumpOperands;
        this.jumps = jumps;
        this.operand = operand;
        this.rubyCase = rubyCase;
        this.targets = targets;
        this.elseTarget = elseTarget;
        this.expectedClass = expectedClass;
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

    public Class getExpectedClass() {
        return expectedClass;
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
        return new BSwitchInstr(jumps, jumpOperands, operand, rubyCase, targets, elseTarget, expectedClass);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(jumpOperands);
        e.encode(operand);
        e.encode(rubyCase);
        e.encode(targets); // FXXXX
        e.encode(elseTarget);
        e.encode(expectedClass.getName());
    }

    public static BSwitchInstr decode(IRReaderDecoder d) {
        try {
            Operand[] jumpOperands = d.decodeOperandArray();
            Operand operand = d.decodeOperand();
            Label rubyCase = d.decodeLabel();
            Label[] targets = d.decodeLabelArray();
            Label elseTarget = d.decodeLabel();
            Class<?> expectedClass = Class.forName(d.decodeString());

            int[] jumps = new int[jumpOperands.length];
            for (int i = 0; i < jumps.length; i++) {
                Operand jumpOperand = jumpOperands[i];
                if (jumpOperand instanceof Symbol) {
                    jumps[i] = ((Symbol) jumpOperand).getSymbol().getId();
                } else if (jumpOperand instanceof Fixnum) {
                    jumps[i] = (int) ((Fixnum) jumpOperand).getValue();
                }
            }

            int[] sortedJumps = jumps.clone();
            Arrays.sort(sortedJumps);

            Operand[] sortedJumpOperands = jumpOperands.clone();
            Label[] sortedTargets = targets.clone();

            for (int i = 0; i < jumps.length; i++) {
                int oldJump = jumps[i];
                int newIndex = Arrays.binarySearch(sortedJumps, oldJump);

                sortedJumpOperands[newIndex] = jumpOperands[i];
                sortedTargets[newIndex] = targets[i];
            }

            return new BSwitchInstr(sortedJumps, sortedJumpOperands, operand, rubyCase, sortedTargets, elseTarget, expectedClass);
        } catch (Exception e) {
            // should never happen unless encode was corrupted
            Helpers.throwException(e);
            return null;
        }
    }

    @Override
    public int interpretAndGetNewIPC(ThreadContext context, DynamicScope currDynScope, StaticScope currScope, IRubyObject self, Object[] temp, int ipc) {
        Object result = operand.retrieve(context, self, currScope, currDynScope, temp);

        int value;
        if (result instanceof RubyFixnum fixnum && expectedClass == RubyFixnum.class) {
            value = fixnum.asInt(context);
        } else if (result instanceof RubySymbol symbol && expectedClass == RubySymbol.class) {
            value = symbol.getId();
        } else {
            // not an optimizable value, fall back on old case logic
            return rubyCase.getTargetPC();
        }

        int index = Arrays.binarySearch(jumps, value);

        return index < 0 ?
                elseTarget.getTargetPC() :
                targets[index].getTargetPC();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BSwitchInstr(this);
    }

    @Override
    public Label[] getJumpTargets() {
        Label[] jumpTargets = new Label[targets.length + 2];
        jumpTargets[0] = rubyCase;
        System.arraycopy(targets, 0, jumpTargets, 1, targets.length);
        jumpTargets[jumpTargets.length - 1] = elseTarget;
        return jumpTargets;
    }

    private static boolean jumpsAreSorted(final int[] jumps) {
        if ( jumps.length == 0 ) return true;
        int prev = jumps[0];
        for ( int i = 1; i < jumps.length; i++ ) {
            int curr = jumps[i];
            if ( prev > curr ) return false; // not sorted
            prev = curr;
        }
        return true;
    }
}
