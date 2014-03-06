package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This represents a required arg that shows up after optional/rest args
 * in a method/block parameter list. This instruction gets to pick an argument
 * based on how many arguments have already been accounted for by parameters
 * present earlier in the list.
 */
public class ReceivePostReqdArgInstr extends ReceiveArgBase implements FixedArityInstr {
    /** The method/block parameter list has these many required parameters before opt+rest args*/
    public final int preReqdArgsCount;

    /** The method/block parameter list has these many required parameters after opt+rest args*/
    public final int postReqdArgsCount;

    public ReceivePostReqdArgInstr(Variable result, int argIndex, int preReqdArgsCount, int postReqdArgsCount) {
        super(Operation.RECV_POST_REQD_ARG, result, argIndex);
        this.preReqdArgsCount = preReqdArgsCount;
        this.postReqdArgsCount = postReqdArgsCount;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(argIndex), new Fixnum(preReqdArgsCount), new Fixnum(postReqdArgsCount) };
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + argIndex + ", " + preReqdArgsCount + ", " + postReqdArgsCount + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new ReceivePostReqdArgInstr(ii.getRenamedVariable(result), argIndex, preReqdArgsCount, postReqdArgsCount);
            default:
                if (ii.canMapArgsStatically()) {
                   int n = ii.getArgsCount();
                   int remaining = n - preReqdArgsCount;
                   Operand argVal;
                   if (remaining <= argIndex) {
                       // SSS: FIXME: Argh!
                       argVal = ii.getInlineHostScope().getManager().getNil();
                   } else {
                       argVal = (remaining > postReqdArgsCount) ? ii.getArg(n - postReqdArgsCount + argIndex) : ii.getArg(preReqdArgsCount + argIndex);
                   }
                   return new CopyInstr(ii.getRenamedVariable(result), argVal);
                } else {
                    return new ReqdArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), preReqdArgsCount, postReqdArgsCount, argIndex);
                }
        }
    }

    public IRubyObject receivePostReqdArg(IRubyObject[] args) {
        int n = args.length;
        int remaining = n - preReqdArgsCount;
        if (remaining <= argIndex) return null;  // For blocks!

        return (remaining > postReqdArgsCount) ? args[n - postReqdArgsCount + argIndex] : args[preReqdArgsCount + argIndex];
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceivePostReqdArgInstr(this);
    }
}
