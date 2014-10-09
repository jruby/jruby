package org.jruby.ir.operands;

import java.util.List;

import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.ir.IRVisitor;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CurrentScope extends Operand {
    private final int scopeNestingDepth;

    public CurrentScope(int scopeNestingDepth) {
        super(OperandType.CURRENT_SCOPE);
        this.scopeNestingDepth = scopeNestingDepth;
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
    public boolean canCopyPropagate() {
        return true;
    }

    public int getScopeNestingDepth() {
        return scopeNestingDepth;
    }

    @Override
    public int hashCode() {
        return scopeNestingDepth;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrentScope && scopeNestingDepth == ((CurrentScope) other).scopeNestingDepth;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        StaticScope scope = currScope;
        int n = scopeNestingDepth;
        while (n > 0) {
            scope = scope.getEnclosingScope();
            if (scope.getScopeType() != null) {
                n--;
            }
        }
        return scope;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.CurrentScope(this);
    }

    @Override
    public String toString() {
        return "scope<" + scopeNestingDepth + ">";
    }
}
