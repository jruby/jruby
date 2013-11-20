package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Argument receive in IRExecution scopes.
 */
public abstract class ReceiveArgBase extends Instr implements ResultInstr {
    protected int argIndex;
    protected Variable result;

    public ReceiveArgBase(Operation op, Variable result, int argIndex) {
        super(op);

        assert result != null: "ReceiveArgBase result is null";

        this.argIndex = argIndex;
        this.result = result;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public int getArgIndex() {
        return argIndex;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }

    public IRubyObject receiveArg(ThreadContext context, int kwArgHashCount, IRubyObject[] args) {
        throw new RuntimeException("ReceiveArgBase.interpret called! " + this.getClass().getName() + " does not define receiveArg");
    }
}
