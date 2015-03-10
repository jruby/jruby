package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CheckArgsArrayArityInstr extends Instr implements FixedArityInstr {
    public final int required;
    public final int opt;
    public final int rest;

    public CheckArgsArrayArityInstr(Operand argsArray, int required, int opt, int rest) {
        super(Operation.CHECK_ARGS_ARRAY_ARITY, new Operand[] { argsArray });

        this.required = required;
        this.opt = opt;
        this.rest = rest;
    }

    public Operand getArgsArray() {
        return operands[0];
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"req: " + required, "opt: " + opt, "*r: " + rest};
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new CheckArgsArrayArityInstr(getArgsArray().cloneForInlining(ii), required, opt, rest);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArgsArray());
        e.encode(required);
        e.encode(opt);
        e.encode(rest);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyArray args = (RubyArray) getArgsArray().retrieve(context, self, currScope, currDynScope, temp);
        Helpers.irCheckArgsArrayArity(context, args, required, opt, rest);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CheckArgsArrayArityInstr(this);
    }
}
