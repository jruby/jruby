package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Argument receive in IRExecution scopes.
 */
public abstract class ReceiveArgBase extends OneOperandResultBaseInstr implements ArgReceiver {
    public ReceiveArgBase(Operation op, Variable result, Variable keywords) {
        super(op, result, keywords);
    }

    public Variable getKeywords() {
        return (Variable) getOperand1();
    }

    public IRubyObject receiveArg(ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope,
                                  Object[] temp, IRubyObject[] args, boolean ruby2keyword) {
        throw new RuntimeException("ReceiveArgBase.interpret called! " + this.getClass().getName() + " does not define receiveArg");
    }
}
