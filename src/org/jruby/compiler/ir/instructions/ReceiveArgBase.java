package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.Ruby;
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

    private IRubyObject[] NO_PARAMS = new IRubyObject[0];    
    public IRubyObject retrieveRestArg(Ruby runtime, IRubyObject[] parameters) {
        int available = parameters.length - argIndex;
        
        IRubyObject[] args;
        if (available <= 0) { 
            args = NO_PARAMS;
        } else {
            args = new IRubyObject[available];
            System.arraycopy(parameters, argIndex, args, 0, available);
        }
        
        return runtime.newArray(args);
    }
}
