package org.jruby.compiler.ir.instructions.ruby19;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.compiler.ir.instructions.ReceiveRestArgBase;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr extends ReceiveRestArgBase {
    /** This instruction gets its slice of the incoming argument list only if there are
     *  at least this many incoming arguments */

    /** The length of the argument list slice that this instructions gets depends on this
     *  parameter which determines how many arguments have already been accounted for by
     *  other receive arg instructions (required and optional) */

    /** Total required args (pre and post) */
    private final int totalRequiredArgs; 

    /** Total opt args */
    private final int totalOptArgs;

    public ReceiveRestArgInstr(Variable result, int argIndex, int totalRequiredArgs, int totalOptArgs) {
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
        return new ReceiveRestArgInstr(ii.getRenamedVariable(result), argIndex, totalRequiredArgs, totalOptArgs);
    }

    private IRubyObject[] NO_PARAMS = new IRubyObject[0];    
    public IRubyObject receiveRestArg(Ruby runtime, IRubyObject[] parameters) {
        IRubyObject[] args;
        int numAvalableArgs = parameters.length - (totalRequiredArgs + totalOptArgs);
        if (numAvalableArgs <= 0) {
            args = NO_PARAMS;
        } else {
            args = new IRubyObject[numAvalableArgs];
            System.arraycopy(parameters, argIndex, args, 0, numAvalableArgs);
        }

        return runtime.newArray(args);
    }
}
