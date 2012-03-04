package org.jruby.interpreter;

import org.jruby.compiler.ir.IRMethod;

public class IRReturnJump extends RuntimeException {
    public IRMethod methodToReturnFrom;
    public Object returnValue;

    private IRReturnJump() {}

    // FIXME: We can't use static threadlocals like this because they leak
    // classloaders. Find somewhere else to cache this :)
    // See https://jira.codehaus.org/browse/JRUBY-6523

//    private static ThreadLocal<IRReturnJump> threadLocalRJ = new ThreadLocal<IRReturnJump>() {
//       public IRReturnJump initialValue() { return new IRReturnJump(); }
//    };

    public static IRReturnJump create(IRMethod m, Object rv) {
//        IRReturnJump rj = threadLocalRJ.get();
        IRReturnJump rj = new IRReturnJump();
        rj.methodToReturnFrom = m;
        rj.returnValue = rv;
        return rj;
    }
}
