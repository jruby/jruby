package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.builtin.IRubyObject;

/** 
 * Assign the 'index' argument to 'dest' which can be undefined since
 * this is an optional argument into the method/block.
 *
 * The specific instruction differs between Ruby language versions.
 */
public abstract class ReceiveOptArgBase extends ReceiveArgBase {
    public ReceiveOptArgBase(Variable result, int index) {
        super(Operation.RECV_OPT_ARG, result, index);
    }

    public abstract Object receiveOptArg(IRubyObject[] args);
}
