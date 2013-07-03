package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign rest arg passed into method to a result variable
 * The specific instruction differs between Ruby language versions
 */
public abstract class ReceiveRestArgBase extends ReceiveArgBase {
    public ReceiveRestArgBase(Variable result, int argIndex) {
        super(Operation.RECV_REST_ARG, result, argIndex);
    }

    public abstract IRubyObject receiveRestArg(Ruby runtime, IRubyObject[] parameters);
}
