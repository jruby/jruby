package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// FIXME: Consider making argument error a single more generic instruction and combining with RaiseArgumentError
public class RaiseRequiredKeywordArgumentError extends Instr implements FixedArityInstr {
    private String name;

    public RaiseRequiredKeywordArgumentError(String name) {
        super(Operation.RAISE_REQUIRED_KEYWORD_ARGUMENT_ERROR, EMPTY_OPERANDS);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new RaiseRequiredKeywordArgumentError(name);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        throw IRRuntimeHelpers.newRequiredKeywordArgumentError(context, name);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RaiseRequiredKeywordArgumentErrorInstr(this);
    }
}
