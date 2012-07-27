package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

/**
 * Wrap a scope for the purpose of finding live module which happens to be associated with it.
 */
public class ScopeModule extends Operand {
    private final IRScope scope;

    public ScopeModule(IRScope scope) {
        this.scope = scope;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }

    @Override
    public Operand cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public IRScope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "module<" + scope.getName() + ">";
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        StaticScope staticScope = scope.getStaticScope();
        return staticScope != null ? staticScope.getModule() : context.runtime.getClass(scope.getName());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ScopeModule(this);
    }
}
