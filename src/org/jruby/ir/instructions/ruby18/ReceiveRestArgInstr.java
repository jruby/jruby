package org.jruby.ir.instructions.ruby18;

import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReceiveRestArgBase;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.InlinerInfo;
import org.jruby.Ruby;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr extends ReceiveRestArgBase {
    public ReceiveRestArgInstr(Variable result, int argIndex) {
        super(result, argIndex);
    }

    @Override
    public Instr cloneForInlinedScope(InlinerInfo ii) {
        if (ii.canMapArgsStatically()) {
            return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex, true));
        } else {
            return new RestArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), -1, -1, argIndex);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ReceiveRestArgInstr(ii.getRenamedVariable(result), argIndex);
    }

    private IRubyObject[] NO_PARAMS = new IRubyObject[0];    
    public IRubyObject receiveRestArg(Ruby runtime, IRubyObject[] parameters) {
        int available = parameters.length - argIndex;
        
        IRubyObject[] args;
        if (available <= 0) { 
            args = NO_PARAMS;
        } else {
            args = new IRubyObject[available];
            System.arraycopy(parameters, argIndex, args, 0, available);
        }
        
        return runtime.newArray(args);
    }
}
