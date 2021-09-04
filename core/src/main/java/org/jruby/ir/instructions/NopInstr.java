package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class NopInstr extends NoOperandInstr implements FixedArityInstr {
    public static final NopInstr NOP = new NopInstr();

    private NopInstr() {
        super(Operation.NOP);
        this.markDead();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return this;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NopInstr(this);
    }
}
