package org.jruby.ir.instructions.ruby19;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.OptArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReceiveOptArgBase;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveOptArgInstr19 extends ReceiveOptArgBase {
    /** This instruction gets to pick an argument off the incoming list only if
     *  there are at least this many incoming arguments */
    public final int minArgsLength;

    public ReceiveOptArgInstr19(Variable result, int index, int minArgsLength) {
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
        return new ReceiveOptArgInstr19(ii.getRenamedVariable(result), argIndex, minArgsLength);
    }

    public Object receiveOptArg(IRubyObject[] args) {
        return (minArgsLength <= args.length ? args[argIndex] : UndefinedValue.UNDEFINED);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveOptArgInstr19(this);
    }
}
