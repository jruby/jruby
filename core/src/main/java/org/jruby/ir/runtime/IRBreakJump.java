package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Frame;
import org.jruby.runtime.builtin.IRubyObject;

public class IRBreakJump extends IRJump implements Unrescuable {
    public final Frame frameToReturnTo;
    public final IRubyObject breakValue;
    public boolean breakInEval;

    private IRBreakJump(Frame frameToReturnTo, IRubyObject rv, boolean breakInEval) {
        this.frameToReturnTo = frameToReturnTo;
        this.breakValue = rv;
        this.breakInEval = breakInEval;
    }

    public static IRBreakJump create(Frame frameToReturnTo, IRubyObject rv, boolean breakInEval) {
        return new IRBreakJump(frameToReturnTo, rv, breakInEval);
    }
}
