package org.jruby.interpreter;

import org.jruby.compiler.ir.IRMethod;

public class IRReturnJump extends RuntimeException {
    public IRMethod methodToReturnFrom;
    public Object returnValue;

    private IRReturnJump() {} 

    private static ThreadLocal<IRReturnJump> threadLocalRJ = new ThreadLocal<IRReturnJump>() {
       public IRReturnJump initialValue() { return new IRReturnJump(); }
    };

    public static IRReturnJump create(IRMethod m, Object rv) {
        IRReturnJump rj = threadLocalRJ.get();
        rj.methodToReturnFrom = m;
        rj.returnValue = rv;
        return rj;
    }
}
