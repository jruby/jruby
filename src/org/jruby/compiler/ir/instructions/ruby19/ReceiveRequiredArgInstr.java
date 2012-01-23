package org.jruby.compiler.ir.instructions.ruby19;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveArgBase;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * This represents a required arg that shows up after optional/rest args
 * in a method/block parameter list. This instruction gets to pick an argument
 * based on how many arguments have already been accounted for by parameters
 * present earlier in the list. 
 */
public class ReceiveRequiredArgInstr extends ReceiveArgBase {
    /** The method/block parameter list has these many required parameters before opt+rest args*/
    public final int preReqdArgsCount;

    /** The method/block parameter list has these many required parameters after opt+rest args*/
    public final int postReqdArgsCount;

    public ReceiveRequiredArgInstr(Variable result, int index, int preReqdArgsCount, int postReqdArgsCount) {
        super(Operation.RECV_REQD_ARG, result, index);
        this.preReqdArgsCount = preReqdArgsCount;
        this.postReqdArgsCount = postReqdArgsCount;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + argIndex + ", " + preReqdArgsCount + ", " + postReqdArgsCount + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    public IRubyObject receiveRequiredArg(IRubyObject[] args) {
        int n = args.length;
        int remaining = n - preReqdArgsCount;
        if (remaining <= argIndex) {
            return null;  // For blocks!
        } else {
            return (remaining > postReqdArgsCount) ? args[n - postReqdArgsCount + argIndex] : args[preReqdArgsCount + argIndex];
        }
    }
}
