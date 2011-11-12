package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
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
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        if (restOfArgArray) {
            interpretAsRestArg(context, self, args, destination, temp);
        } else {
            destination.store(context, self, temp, args[argIndex]);
        }
        return null;
    }

    @Interp
    private void  interpretAsRestArg(ThreadContext context, IRubyObject self, IRubyObject[] args, Operand destination, Object[] temp) {
        destination.store(context, self, temp, context.getRuntime().newArray(getParametersFrom(args, argIndex)));
    }
    
    private IRubyObject[] NO_PARAMS = new IRubyObject[0];    
    private IRubyObject[] getParametersFrom(IRubyObject[] parameters, int argIndex) {
        int length = parameters.length - argIndex;
        
        if (length <= 0) return NO_PARAMS;

        IRubyObject[] args = new IRubyObject[length];
        System.arraycopy(parameters, argIndex, args, 0, length);
        
        return args;
    }
}
