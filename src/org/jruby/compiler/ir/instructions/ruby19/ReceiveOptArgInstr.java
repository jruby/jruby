package org.jruby.compiler.ir.instructions.ruby19;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.OptArgMultipleAsgnInstr;
import org.jruby.compiler.ir.instructions.ReceiveOptArgBase;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveOptArgInstr extends ReceiveOptArgBase {
    /** This instruction gets to pick an argument off the incoming list only if
     *  there are at least this many incoming arguments */
    public final int minArgsLength;

    public ReceiveOptArgInstr(Variable result, int index, int minArgsLength) {
        super(result, index);
        this.minArgsLength = minArgsLength;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + argIndex + ", " + minArgsLength + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        if (ii.canMapArgsStatically()) {
            int n = ii.getArgsCount();
            return new CopyInstr(ii.getRenamedVariable(result), minArgsLength <= n ? ii.getArg(argIndex) : UndefinedValue.UNDEFINED);
        } else {
            return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), argIndex, minArgsLength);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ReceiveOptArgInstr(ii.getRenamedVariable(result), argIndex, minArgsLength);
    }

    public Object receiveOptArg(IRubyObject[] args) {
        return (minArgsLength <= args.length ? args[argIndex] : UndefinedValue.UNDEFINED);
    }
}
