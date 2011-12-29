package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Wrap current scope for the purpose of finding live module which
 * happens to be associated with it. For IRModuleBody and below it represents
 * those scopes live value.  For scopes like IRScriptBody, it represents
 * the current module we contained in.
 */
public class WrappedIRScope extends Constant {
	 // SSS FIXME: Should we create a static dummy IRScope that can be used in place of 'null'?
	 public static WrappedIRScope CURRENT_SCOPE = new WrappedIRScope(null);

    private final IRScope scope;

    public WrappedIRScope(IRScope scope) {
        this.scope = scope;
    }

    public IRScope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return (this == CURRENT_SCOPE) ? "<current-scope>" : scope.getName();
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        StaticScope staticScope = this == CURRENT_SCOPE ? currDynScope.getStaticScope() : scope.getStaticScope();

        return staticScope != null ? staticScope.getModule() : context.runtime.getClass(scope.getName());
    }
}
