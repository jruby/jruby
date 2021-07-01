package org.jruby.ir.operands;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

/**
 * Reference a StaticScope/IRScope.  This could be used wherever we know a staticscope will
 * be used but we initially favor CurrentScope as it is a single instance and it reduces an
 * object allocation for every scope in the system.  Note: we store IRScope because JIT can
 * easily store IRScopes vs making new JIT infrastructure for StaticScope (and from a scoping
 * standpoint these two types are 1:1).
 *
 * However, once we inline we are migrating things referencing a currentscope which
 * no longer exists.  In this case, we need replace CurrentScope with a Scope.
 */
public class Scope extends Operand {
    private final IRScope scope;

    public Scope(IRScope scope) {
        this.scope = scope;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SCOPE;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
    }

    public IRScope getScope() {
        return scope;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Scope(this);
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return scope.getStaticScope();
    }
}
