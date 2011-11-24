package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.compiler.ir.Interp;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GlobalVariable extends Reference {
    public GlobalVariable(String name) {
        super(name);
    }

    public int compareTo(Object arg0) {
        // ENEBO: what should compareTo when it is not comparable?
        if (!(arg0 instanceof GlobalVariable)) return 0;

        return getName().compareTo(((GlobalVariable) arg0).getName());
    }
    
    @Interp
    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return context.getRuntime().getGlobalVariables().get(getName());
    }
}
