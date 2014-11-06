package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Symbol extends Reference {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol("");

    public Symbol(String name) {
        super(OperandType.SYMBOL, name);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return context.runtime.newSymbol(getName());
    }

    @Override
    public String toString() {
        return ":'" + getName() + "'";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }
}
