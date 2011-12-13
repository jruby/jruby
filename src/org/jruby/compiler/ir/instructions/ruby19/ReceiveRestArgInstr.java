package org.jruby.compiler.ir.instructions.ruby19;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveRestArgBase;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

/*
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr extends ReceiveRestArgBase {
    /** This instruction gets its slice of the incoming argument list only if there are
     *  at least this many incoming arguments */
    private final int minArgsLength; 

    /** The length of the argument list slice that this instructions gets depends on this
     *  parameter which determines how many arguments have already been accounted for by
     *  other receive arg instructions (required and optional) */
    private final int usedArgsCount;

    public ReceiveRestArgInstr(Variable result, int argIndex, int minArgsLength, int usedArgsCount) {
        super(result, argIndex);
        this.minArgsLength = minArgsLength;
        this.usedArgsCount = usedArgsCount;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + argIndex + ", " + minArgsLength + ", " + usedArgsCount + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, true));
    }
}
