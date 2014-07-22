package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
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
    private final int scopeModuleDepth;

    public ScopeModule(int scopeModuleDepth) {
        super(OperandType.SCOPE_MODULE);

        this.scopeModuleDepth = scopeModuleDepth;
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
    public int hashCode() {
        return scopeModuleDepth;
    }

    public int getScopeModuleDepth() {
        return scopeModuleDepth;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ScopeModule && scopeModuleDepth == ((ScopeModule) other).scopeModuleDepth;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    @Override
    public String toString() {
        return "module<" + scopeModuleDepth + ">";
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        StaticScope scope = currScope;
        int n = scopeModuleDepth;
        while (n > 0) {
            scope = scope.getEnclosingScope();
            if (scope.getScopeType() != null) {
                n--;
            }
        }
        return scope.getModule();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ScopeModule(this);
    }
}
