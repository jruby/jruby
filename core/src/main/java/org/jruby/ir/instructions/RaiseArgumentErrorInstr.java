package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RaiseArgumentErrorInstr extends Instr implements FixedArityInstr {
    private final int required;
    private final int opt;
    private final int rest;
    private final int numArgs;

    public RaiseArgumentErrorInstr(int required, int opt, int rest, int numArgs) {
        super(Operation.RAISE_ARGUMENT_ERROR, EMPTY_OPERANDS);

        this.required = required;
        this.opt = opt;
        this.rest = rest;
        this.numArgs = numArgs;
    }

    public int getNumArgs() {
        return numArgs;
    }

    public int getOpt() {
        return opt;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "req: " + required, "o: " + opt, "*r: " + rest};
    }

    public int getRequired() {
        return required;
    }

    public int getRest() {
        return rest;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new RaiseArgumentErrorInstr(required, opt, rest, numArgs);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getRequired());
        e.encode(getOpt());
        e.encode(getRest());
        e.encode(getNumArgs());
    }

    public static RaiseArgumentErrorInstr decode(IRReaderDecoder d) {
        return new RaiseArgumentErrorInstr(d.decodeInt(), d.decodeInt(), d.decodeInt(), d.decodeInt());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Arity.raiseArgumentError(context.runtime, numArgs, required, required + opt);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RaiseArgumentErrorInstr(this);
    }

}
