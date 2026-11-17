package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;

public abstract class ReceiveIndexedArgBase extends ReceiveArgBase {
    protected int argIndex;

    public ReceiveIndexedArgBase(Operation op, Variable result, Variable keywords, int argIndex) {
        super(op, result, keywords);

        assert result != null: "ReceiveArgBase result is null";

        this.argIndex = argIndex;
    }

    public int getArgIndex() {
        return argIndex;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArgIndex());
    }
}
