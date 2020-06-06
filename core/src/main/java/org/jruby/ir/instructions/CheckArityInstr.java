package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

public class CheckArityInstr extends NoOperandInstr implements FixedArityInstr {
    public final int required;
    public final int opt;
    public final boolean rest;
    public final boolean receivesKeywords;
    public final int restKey;

    public CheckArityInstr(int required, int opt, boolean rest, boolean receivesKeywords, int restKey) {
        super(Operation.CHECK_ARITY);

        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.receivesKeywords = receivesKeywords;
        this.restKey = restKey;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"req: " + required, "opt: " + opt, "*r: " + rest, "kw: " + receivesKeywords};
    }

    @Override
    public Instr clone(CloneInfo info) {
        if (info instanceof SimpleCloneInfo) return new CheckArityInstr(required, opt, rest, receivesKeywords, restKey);

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
        e.encode(receivesKeywords);
        e.encode(restKey);
    }

    public static CheckArityInstr decode(IRReaderDecoder d) {
        return new CheckArityInstr(d.decodeInt(), d.decodeInt(), d.decodeBoolean(), d.decodeBoolean(), d.decodeInt());
    }

    public void checkArity(ThreadContext context, StaticScope scope, Object[] args, Block block) {
        IRRuntimeHelpers.checkArity(context, scope, args, required, opt, rest, receivesKeywords, restKey, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CheckArityInstr(this);
    }
}
