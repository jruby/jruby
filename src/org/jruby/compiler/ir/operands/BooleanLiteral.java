package org.jruby.compiler.ir.operands;

import java.util.List;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class BooleanLiteral extends Operand {
    private BooleanLiteral() { }

    public static final BooleanLiteral TRUE  = new BooleanLiteral();
    public static final BooleanLiteral FALSE = new BooleanLiteral();

    @Override
    public boolean hasKnownValue() {
        return true;
    }
    
    @Override
    public void addUsedVariables(List<Variable> l) {
        /* do nothing */
    }

    public boolean isTrue()  {
        return this == TRUE;
    }

    public boolean isFalse() {
        return this == FALSE;
    }

    public BooleanLiteral logicalNot() {
        return isTrue() ? FALSE : TRUE;
    }
    
    @Override
    public String toString() {
        return isTrue() ? "true" : "false";
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, DynamicScope currDynScope, Object[] temp) {
        return context.getRuntime().newBoolean(isTrue());
    }
}
