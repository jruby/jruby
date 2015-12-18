package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.builtin.IRubyObject;

public class IRWrappedLambdaReturnValue extends RuntimeException implements Unrescuable {
    public final IRubyObject returnValue;

    public IRWrappedLambdaReturnValue(IRubyObject v) {
        this.returnValue = v;
    }
}
