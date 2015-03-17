package org.jruby.ir.operands;

import java.util.List;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.ir.IRVisitor;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class CurrentScope extends Operand {
    // First four scopes are so common and this operand is immutable so we share them.
    public static final CurrentScope[] CURRENT_SCOPE = {
            new CurrentScope(0), new CurrentScope(1), new CurrentScope(2), new CurrentScope(3), new CurrentScope(4)
    };

    public static CurrentScope ScopeFor(int depth) {
        return depth < CURRENT_SCOPE.length ? CURRENT_SCOPE[depth] : new CurrentScope(depth);
    }

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
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getScopeNestingDepth());
    }

    public static CurrentScope decode(IRReaderDecoder d) {
        return ScopeFor(d.decodeInt());
    }

    @Override
    public String toString() {
        return "scope<" + scopeNestingDepth + ">";
    }
}
