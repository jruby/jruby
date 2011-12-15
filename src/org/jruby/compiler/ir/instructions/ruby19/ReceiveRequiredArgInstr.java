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
    /** The method/block parameter list has these many required parameters */
    public final int totalReqdParams;

    /** The method/block parameter list has these many optional and rest parameters */
    public final int totalOptParams;

    public ReceiveRequiredArgInstr(Variable result, int index, int totalReqdParams, int totalOptParams) {
        super(Operation.RECV_REQD_ARG, result, index);
        this.totalReqdParams = totalReqdParams;
        this.totalOptParams = totalOptParams;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + argIndex + ", " + totalReqdParams + ", " + totalOptParams + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    public IRubyObject receiveRequiredArg(IRubyObject[] args) {
        int remaining = args.length - totalReqdParams; 
        if (remaining < 0) return null;  // For blocks!
        else return (remaining < totalOptParams) ? args[argIndex - (totalOptParams-remaining)]
                                                 : args[argIndex + (args.length - totalReqdParams - totalOptParams)];
    }
}
