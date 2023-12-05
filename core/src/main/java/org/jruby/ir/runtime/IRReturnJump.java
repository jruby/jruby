package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class IRReturnJump extends IRJump implements Unrescuable {
    public StaticScope returnScope;
    public DynamicScope methodToReturnFrom;
    public Object returnValue;

    private static final ThreadLocal<IRReturnJump> RETURN_JUMP = new ThreadLocal<>();

    private IRReturnJump(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        reset(returnScope, scopeToReturnFrom, rv);
    }

    public static IRReturnJump create(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        IRReturnJump jump = RETURN_JUMP.get();
        if (jump == null) {
            RETURN_JUMP.set(jump = new IRReturnJump(returnScope, scopeToReturnFrom, rv));
        } else {
            jump.reset(returnScope, scopeToReturnFrom, rv);
        }

        return jump;
    }

    private void reset(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        this.methodToReturnFrom = scopeToReturnFrom;
        this.returnScope = returnScope;
        this.returnValue = rv;
    }

    public boolean isReturnToScope(DynamicScope scope) {
        return methodToReturnFrom == scope;
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + methodToReturnFrom + ":" + returnValue + ">";
    }
}
