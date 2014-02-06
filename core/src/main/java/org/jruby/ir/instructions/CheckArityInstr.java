package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.Arity;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.ThreadContext;

public class CheckArityInstr extends Instr implements FixedArityInstr {
    public final int required;
    public final int opt;
    public final int rest;
    public final boolean receivesKwargs;

    public CheckArityInstr(int required, int opt, int rest, boolean receivesKwargs) {
        super(Operation.CHECK_ARITY);

        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.receivesKwargs = receivesKwargs;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(required), new Fixnum(opt), new Fixnum(rest) };
    }

    @Override
    public String toString() {
        return super.toString() + "(" + required + ", " + opt + ", " + rest + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new CheckArityInstr(required, opt, rest, receivesKwargs);
            default:
                if (ii.canMapArgsStatically()) {
                    // Since we know arity at a callsite, arity check passes or we have an ArgumentError
                    int numArgs = ii.getArgsCount();
                    if ((numArgs < required) || ((rest == -1) && (numArgs > (required + opt)))) {
                        return new RaiseArgumentErrorInstr(required, opt, rest, rest);
                    }

                    return null;
                } else {
                    return new CheckArgsArrayArityInstr(ii.getArgs(), required, opt, rest);
                }
        }
    }

    public void checkArity(ThreadContext context, Object[] args) {
        IRRuntimeHelpers.checkArity(context, args, required, opt, rest, receivesKwargs);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CheckArityInstr(this);
    }
}
