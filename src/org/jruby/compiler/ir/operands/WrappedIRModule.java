package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRBody;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class WrappedIRModule extends Constant {
    public static final WrappedIRModule CURRENT_MODULE = new WrappedIRModule(null);

    private final IRBody module;

    public WrappedIRModule(IRBody scope) {
        this.module = scope;
    }

    public IRBody getModule() {
        return module;
    }

    @Override
    public String toString() {
        return (module == null) ? "<current-module>" : module.getName();
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        StaticScope ssc = (module == null) ? currDynScope.getStaticScope() : module.getStaticScope();
        // FIXME: Seems like this should never be null currently?
        return ssc != null ? ssc.getModule() : context.runtime.getClass(module.getName());
    }
}
