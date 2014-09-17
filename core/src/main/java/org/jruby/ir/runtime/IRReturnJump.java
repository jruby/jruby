package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;

public class IRReturnJump extends RuntimeException implements Unrescuable {
    public DynamicScope methodToReturnFrom;
    public Object returnValue;

    private IRReturnJump() {}

    public static IRReturnJump create(DynamicScope scope, Object rv) {
        IRReturnJump rj = new IRReturnJump();
        rj.methodToReturnFrom = scope;
        rj.returnValue = rv;
        return rj;
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
