package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign Argument passed into scope/method to destination Variable
 */
public class ReceiveArgumentInstruction extends Instr implements ResultInstr {
	 // SSS FIXME: Fix IR to start offsets from 0
    protected int argIndex;
    protected boolean restOfArgArray; // If true, the argument sub-array starting at this index is passed in via this instruction.
    private final Variable destination;

    public ReceiveArgumentInstruction(Variable destination, int argIndex,
            boolean restOfArgArray) {
        super(Operation.RECV_ARG);
        
        assert destination != null: "ReceiveArgumentInstr result is null";
        
        this.argIndex = argIndex;
        this.restOfArgArray = restOfArgArray;
        this.destination = destination;
    }

    public ReceiveArgumentInstruction(Variable destination, int index) {
        this(destination, index, false);
    }
    
    public boolean isRestOfArgArray() {
        return restOfArgArray;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return destination;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(destination), ii.getCallArg(argIndex, restOfArgArray));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + (restOfArgArray ? ", ALL" : "") + ")";
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block) {
        if (restOfArgArray) {
            interpretAsRestArg(interp, context, self, destination);
        } else {
            destination.store(interp, context, self, interp.getParameter(argIndex));
        }
        return null;
    }

    @Interp
    private void  interpretAsRestArg(InterpreterContext interp, ThreadContext context, IRubyObject self, Operand destination) {
        destination.store(interp, context, self, context.getRuntime().newArray(interp.getParametersFrom(argIndex)));
    }
}
