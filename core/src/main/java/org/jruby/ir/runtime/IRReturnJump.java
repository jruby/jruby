package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;

public class IRReturnJump extends IRJump implements Unrescuable {
    private StaticScope returnScope;
    private DynamicScope methodToReturnFrom;
    private Object returnValue;
    private boolean inUse = false;

    private IRReturnJump() {
    }

    public static IRReturnJump create(StaticScope returnScope, DynamicScope scopeToReturnFrom, Object rv) {
        IRReturnJump jump = returnScope.getReturnJump();
        if (jump == null || jump.inUse) {
            returnScope.setReturnJump(jump = new IRReturnJump());
        }

        jump.update(returnScope, scopeToReturnFrom, rv);

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
