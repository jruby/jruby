package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CheckArityInstr extends OneOperandInstr implements FixedArityInstr {
    public final int required;
    public final int opt;
    public final boolean rest;
    public final int restKey;

    public CheckArityInstr(int required, int opt, boolean rest, int restKey, Operand keywords) {
        super(Operation.CHECK_ARITY, keywords);

        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.restKey = restKey;
    }

    public Operand getKeywords() {
        return getOperand1();
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"req: " + required, "opt: " + opt, "*r: " + rest};
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new CheckArityInstr(required, opt, rest, restKey, getOperand1());

        InlineCloneInfo ii = (InlineCloneInfo) info;
        if (ii.canMapArgsStatically()) { // we can error on bad arity or remove check_arity
            int numArgs = ii.getArgsCount();

            if (numArgs < required || (!rest && numArgs > (required + opt))) {
                return new RaiseArgumentErrorInstr(required, opt, rest, numArgs);
            }

            return null;
        }

        return new CheckArgsArrayArityInstr(ii.getArgs(), required, opt, rest);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(required);
        e.encode(opt);
        e.encode(rest);
        e.encode(restKey);
        e.encode(getKeywords());
    }

    public static CheckArityInstr decode(IRReaderDecoder d) {
        return new CheckArityInstr(d.decodeInt(), d.decodeInt(), d.decodeBoolean(), d.decodeInt(), d.decodeOperand());
    }

    public void checkArity(ThreadContext context, IRubyObject self, StaticScope scope, DynamicScope dynamicScope, IRubyObject[] args, Block block, Object[] temp) {
        Object keyword = getOperand1().retrieve(context, self, scope, dynamicScope, temp);
        IRRuntimeHelpers.checkArity(context, scope, args, keyword, required, opt, rest, restKey, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CheckArityInstr(this);
    }
}
