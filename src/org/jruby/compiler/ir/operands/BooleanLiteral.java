package org.jruby.compiler.ir.operands;

import org.jruby.runtime.ThreadContext;

public class BooleanLiteral extends ImmutableLiteral {
    private BooleanLiteral() { }

    public static final BooleanLiteral TRUE  = new BooleanLiteral();
    public static final BooleanLiteral FALSE = new BooleanLiteral();

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.getRuntime().newBoolean(isTrue());
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
}
