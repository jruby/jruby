package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;

public class IRReturnJump extends IRJump implements Unrescuable {
    public final DynamicScope methodToReturnFrom;
    public final Object returnValue;

    private IRReturnJump(DynamicScope scopeToReturnFrom, Object rv) {
        this.methodToReturnFrom = scopeToReturnFrom;
        this.returnValue = rv;
    }

    public static IRReturnJump create(DynamicScope scopeToReturnFrom, Object rv) {
        return new IRReturnJump(scopeToReturnFrom, rv);
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
