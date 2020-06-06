package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class Self extends Variable {
    public static final Self SELF = new Self();
    private static final String NAME = "%self";

    private Self() {
        super();
    }

    public String getId() {
        return NAME;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SELF;
    }

    public boolean isSelf() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return self;
    }

    @Override
    public Variable clone(SimpleCloneInfo ii) {
        return this;
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        if (ii instanceof InlineCloneInfo) {
            return ((InlineCloneInfo) ii).getRenamedSelfVariable(this);
        } else {
            return super.cloneForInlining(ii);
        }
    }


    @Override
    public void encode(IRWriterEncoder e) {
        // No super because we don't want to dump %self and offset of 0
        e.encode(getOperandType().getCoded());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Self(this);
    }

    @Override
    public int compareTo(Object o) {
        return this == o ? 0 : -1;
    }

    @Override
    public String toString() {
        return NAME;
    }
}
