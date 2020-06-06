package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

public class ObjectClass extends Operand {
    public ObjectClass() {
        super();
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.OBJECT_CLASS;
    }

    @Override
    public String toString() {
        return "<Class:Object>";
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return context.runtime.getObject();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ObjectClass(this);
    }
}
