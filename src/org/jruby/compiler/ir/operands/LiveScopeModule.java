package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.compiler.ir.representations.InlinerInfo;

/**
 * Wrap current scope for the purpose of finding live module which
 * happens to be associated with it. For IRModuleBody and below it represents
 * those scopes live value.  For scopes like IRScriptBody, it represents
 * the current module we contained in.
 */
public class LiveScopeModule extends Operand {
    private final IRScope scope;

    public LiveScopeModule(IRScope scope) {
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
        return staticScope != null ? staticScope.getModule() : context.getRuntime().getClass(scope.getName());
    }
}
