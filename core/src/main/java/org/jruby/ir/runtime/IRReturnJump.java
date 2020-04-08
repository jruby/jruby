package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class IRReturnJump extends IRJump implements Unrescuable {
    public final StaticScope returnScope;
    public final DynamicScope methodToReturnFrom;
    public final Object returnValue;

    private IRReturnJump(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        this.methodToReturnFrom = scopeToReturnFrom;
        this.returnScope = returnScope;
        this.returnValue = rv;
    }

    public static IRReturnJump create(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        return new IRReturnJump(returnScope, scopeToReturnFrom, rv);
    }

    public boolean isReturnToScope(DynamicScope scope) {
        return methodToReturnFrom == scope;
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
