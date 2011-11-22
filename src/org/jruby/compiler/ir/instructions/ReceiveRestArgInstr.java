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
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr extends Instr implements ResultInstr {
    protected int argIndex;
    private Variable result;

    public ReceiveRestArgInstr(Variable result, int argIndex) {
        super(Operation.RECV_REST_ARG);

        assert result != null: "ReceiveRestArg result is null";

        this.argIndex = argIndex;
        this.result = result;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, true));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + ")";
    }

    private IRubyObject[] NO_PARAMS = new IRubyObject[0];    

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        IRubyObject[] restArg;
        int available = args.length - argIndex;
        if (available <= 0) {
           restArg = NO_PARAMS;
        } else {
           restArg = new IRubyObject[available];
           System.arraycopy(args, argIndex, restArg, 0, available);
        }
        
        result.store(context, self, temp, context.getRuntime().newArray(restArg));
        return null;
    }

}
