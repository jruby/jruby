package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
// FIXME: Consider making argument error a single more generic instruction and combining with RaiseArgumentError
public class RaiseRequiredKeywordArgumentError extends Instr implements FixedArityInstr {
    private String name;

    public RaiseRequiredKeywordArgumentError(String name) {
        super(Operation.RAISE_REQUIRED_KEYWORD_ARGUMENT_ERROR);

        this.name = name;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[0];
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        throw context.runtime.newArgumentError("missing keyword: " + name);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RaiseRequiredKeywordArgumentErrorInstr(this);
    }
}
