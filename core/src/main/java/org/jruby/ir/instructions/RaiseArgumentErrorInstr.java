package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseArgumentErrorInstr extends Instr implements FixedArityInstr {
    private final int required;
    private final int opt;
    private final int rest;
    private final int numArgs;

    public RaiseArgumentErrorInstr(int required, int opt, int rest, int numArgs) {
        super(Operation.RAISE_ARGUMENT_ERROR);

        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.numArgs = numArgs;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(required), new Fixnum(opt), new Fixnum(rest), new Fixnum(numArgs) };
    }

    public int getNumArgs() {
        return numArgs;
    }

    public int getOpt() {
        return opt;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + required + ", " + opt + ", " + rest + ")";
    }

    public int getRequired() {
        return required;
    }

    public int getRest() {
        return rest;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Arity.raiseArgumentError(context.runtime, numArgs, required, required + opt);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RaiseArgumentErrorInstr(this);
    }

}
