package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.IRScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class WrappedIRScope extends Operand {
    private final IRScope scope;

    public WrappedIRScope(IRScope scope) {
        this.scope = scope;
    }
    
    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Nothing to do */
    }    

    public IRScope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return scope.getName();
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return scope.getStaticScope();
    }
}
