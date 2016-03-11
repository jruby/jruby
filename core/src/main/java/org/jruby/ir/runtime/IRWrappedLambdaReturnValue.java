package org.jruby.ir.runtime;

import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;

// This class is just a thin wrapper around a return value
// from nonlocal-return and break instructions.
//
// At IR build time, we don't know if a return/break in a block
// will be a non-local return / break (as in a proc or block)
// or will just be a regular return (as in a lambda). To ensure
// uniform instruction semantics at runtime, we push the local return
// through an exception object (just like IRReturnJump and IRBreakJump)
// and let it go through exception handlers which ensure that frame/scope
// are updated properly and ruby-level ensure code is run.
public class IRWrappedLambdaReturnValue extends IRJump implements Unrescuable {
    public final IRubyObject returnValue;

    public IRWrappedLambdaReturnValue(IRubyObject v) {
        this.returnValue = v;
    }
}
