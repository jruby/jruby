package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.IRScope;

public class IRReturnJump extends IRJump implements Unrescuable {
    public final IRScope returnScope;
    public final IRScope methodToReturnFrom;
    public final Object returnValue;

    private IRReturnJump(IRScope returnScope, IRScope scopeToReturnFrom, Object rv) {
        this.methodToReturnFrom = scopeToReturnFrom;
        this.returnScope = returnScope;
        this.returnValue = rv;
    }

    public static IRReturnJump create(IRScope returnScope, IRScope scopeToReturnFrom, Object rv) {
        return new IRReturnJump(returnScope, scopeToReturnFrom, rv);
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
