package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.List;

/**
 * Wrap a scope for the purpose of finding live module which happens to be associated with it.
 */
public class ScopeModule extends Operand {
    // First four scopes are so common and this operand is immutable so we share them.
    public static final ScopeModule[] SCOPE_MODULE = {
            new ScopeModule(0), new ScopeModule(1), new ScopeModule(2), new ScopeModule(3), new ScopeModule(4)
    };

    public static ScopeModule ModuleFor(int depth) {
        return depth < SCOPE_MODULE.length ? SCOPE_MODULE[depth] : new ScopeModule(depth);
    }
    
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
    public Operand cloneForInlining(CloneInfo ii) {
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
        return Helpers.getNthScopeModule(currScope, scopeModuleDepth);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getScopeModuleDepth());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ScopeModule(this);
    }
}
