package org.jruby.ir.instructions.ruby19;

import org.jruby.Ruby;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ReceiveRestArgBase;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr19 extends ReceiveRestArgBase {
    /** This instruction gets its slice of the incoming argument list only if there are
     *  at least this many incoming arguments */

    /** The length of the argument list slice that this instructions gets depends on this
     *  parameter which determines how many arguments have already been accounted for by
     *  other receive arg instructions (required and optional) */

    /** Total required args (pre and post) */
    private final int totalRequiredArgs; 

    /** Total opt args */
    private final int totalOptArgs;

    public ReceiveRestArgInstr19(Variable result, int argIndex, int totalRequiredArgs, int totalOptArgs) {
        super(result, argIndex);
        this.totalRequiredArgs = totalRequiredArgs;
        this.totalOptArgs = totalOptArgs;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + argIndex + ", " + totalRequiredArgs + ", " + totalOptArgs + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        if (ii.canMapArgsStatically()) {
            // FIXME: Check this
            return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex, true));
        } else {
            return new RestArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), argIndex, (totalRequiredArgs + totalOptArgs - argIndex), argIndex);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ReceiveRestArgInstr19(ii.getRenamedVariable(result), argIndex, totalRequiredArgs, totalOptArgs);
    }

    private IRubyObject[] NO_PARAMS = new IRubyObject[0];    
    public IRubyObject receiveRestArg(Ruby runtime, IRubyObject[] parameters) {
        IRubyObject[] args;
        int numAvailableArgs = parameters.length - (totalRequiredArgs + totalOptArgs);
        if (numAvailableArgs <= 0) {
            args = NO_PARAMS;
        } else {
            args = new IRubyObject[numAvailableArgs];
            System.arraycopy(parameters, argIndex, args, 0, numAvailableArgs);
        }

        return runtime.newArray(args);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveRestArgInstr19(this);
    }
}
