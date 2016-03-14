package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;
import org.jruby.util.cli.Options;

public class IRReturnJump extends IRJump implements Unrescuable {
    public final DynamicScope methodToReturnFrom;
    public final Object returnValue;

    private IRReturnJump(DynamicScope scope, Object rv) {
        this.methodToReturnFrom = scope;
        this.returnValue = rv;
    }

    public static IRReturnJump create(DynamicScope scope, Object rv) {
        return new IRReturnJump(scope, rv);
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
