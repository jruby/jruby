package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.ir.IRScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class IRReturnJump extends IRJump implements Unrescuable {
    private StaticScope returnScope;
    private DynamicScope methodToReturnFrom;
    private Object returnValue;
    private boolean inUse = false;

    private static final ThreadLocal<IRReturnJump> RETURN_JUMP = new ThreadLocal<>();

    private IRReturnJump(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        update(returnScope, scopeToReturnFrom, rv);
    }

    public static IRReturnJump create(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        IRReturnJump jump = RETURN_JUMP.get();
        if (jump == null || jump.inUse) {
            RETURN_JUMP.set(jump = new IRReturnJump(returnScope, scopeToReturnFrom, rv));
        } else {
            jump.update(returnScope, scopeToReturnFrom, rv);
        }

        return jump;
    }

    private void update(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        this.methodToReturnFrom = scopeToReturnFrom;
        this.returnScope = returnScope;
        this.returnValue = rv;
        this.inUse = true;
    }

    public void reset() {
        methodToReturnFrom = null;
        returnScope = null;
        returnValue = null;
        inUse = false;
    }

    public boolean isReturnToScope(DynamicScope scope) {
        return methodToReturnFrom == scope;
    }

    public Object returnAndReset() {
        Object returnValue = this.returnValue;
        reset();
        return returnValue;
    }

    @Override
    public String toString() {
        return "IRReturnJump:<" + returnScope.getIRScope() + ":" + methodToReturnFrom.getStaticScope().getIRScope() + ">";
    }

    public boolean isReturningToScriptScope() {
        return methodToReturnFrom.getStaticScope().getIRScope().isScriptScope();
    }
}
