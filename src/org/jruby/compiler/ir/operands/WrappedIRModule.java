package org.jruby.compiler.ir.operands;

import org.jruby.compiler.ir.IRModule;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class WrappedIRModule extends Constant {
    public static final WrappedIRModule CURRENT_MODULE = new WrappedIRModule(null);

    private final IRModule module;

    public WrappedIRModule(IRModule scope) {
        this.module = scope;
    }

    public IRModule getModule() {
        return module;
    }

    @Override
    public String toString() {
        return (module == null) ? "<current-module>" : module.getName();
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        StaticScope ssc = (module == null) ? currDynScope.getStaticScope() : module.getStaticScope();
        if (ssc != null) {
            return ssc.getModule();
        } else if (module.isACoreClass()) {
            // static scope would be null for core classes
            return module.getCoreClassModule(context.getRuntime());
        } else {
            // IR/Interpretation BUG?
            return null;
        }
    }
}
