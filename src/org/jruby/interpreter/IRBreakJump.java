package org.jruby.interpreter;

import org.jruby.compiler.ir.IRScope;

public class IRBreakJump extends RuntimeException {
    public IRScope scopeToReturnTo;
    public Object breakValue;
    public boolean caughtByLambda;
    public boolean breakInEval;

    private IRBreakJump() {}

    // FIXME: We can't use static threadlocals like this because they leak
    // classloaders. Find somewhere else to cache this :)
    // See https://jira.codehaus.org/browse/JRUBY-6523

//    private static ThreadLocal<IRBreakJump> threadLocalBJ = new ThreadLocal<IRBreakJump>() {
//       public IRBreakJump initialValue() { return new IRBreakJump(); }
//    };

    public static IRBreakJump create(IRScope s, Object rv) {
//        IRBreakJump bj = threadLocalBJ.get();
        IRBreakJump bj = new IRBreakJump();
        bj.scopeToReturnTo = s;
        bj.breakValue = rv;
        bj.caughtByLambda = false;
        bj.breakInEval = false;
        return bj;
    }
}
