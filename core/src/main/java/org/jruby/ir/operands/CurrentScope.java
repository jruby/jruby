package org.jruby.ir.operands;

import java.util.List;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.ir.IRVisitor;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CurrentScope extends Operand {
    public final static CurrentScope INSTANCE = new CurrentScope();

    public CurrentScope() {
        super();
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.CURRENT_SCOPE;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to do */
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        if (ii instanceof InlineCloneInfo) {
            InlineCloneInfo iici = (InlineCloneInfo) ii;

            // inlined method lives somewhere else so we need to save that scope location.
            if (iici.getHostScope() != iici.getScopeBeingInlined()) {
                return new Scope(((InlineCloneInfo) ii).getScopeBeingInlined());
            }
        }
        return this;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return currScope;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CurrentScope(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
    }

    public static CurrentScope decode(IRReaderDecoder d) {
        return CurrentScope.INSTANCE;
    }

    @Override
    public String toString() {
        return "%scope";
    }
}
