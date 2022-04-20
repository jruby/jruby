package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Argument receive in IRExecution scopes.
 */
public abstract class ReceiveArgBase extends OneOperandResultBaseInstr implements ArgReceiver {
    protected int argIndex;

    public ReceiveArgBase(Operation op, Variable result, Variable keywords, int argIndex) {
        super(op, result, keywords);

        assert result != null: "ReceiveArgBase result is null";

        this.argIndex = argIndex;
    }

    public Variable getKeyword() {
        return (Variable) getOperand1();
    }

    public int getArgIndex() {
        return argIndex;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArgIndex());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "i:" + argIndex };
    }

    public IRubyObject receiveArg(ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope,
                                  Object[] temp, IRubyObject[] args, boolean keywordArgumentSupplied, boolean ruby2keyword) {
        throw new RuntimeException("ReceiveArgBase.interpret called! " + this.getClass().getName() + " does not define receiveArg");
    }
}
