package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// This instruction encodes the receive of an argument into a closure
//   Ex:  .. { |a| .. }
// The closure receives 'a' via this instruction
public class ReceiveClosureArgInstr extends Instr implements ResultInstr {
    private final int argIndex;
    boolean restOfArgArray;
    private Variable result;

    public ReceiveClosureArgInstr(Variable result, int argIndex, boolean restOfArgArray) {
        super(Operation.RECV_CLOSURE_ARG);
        
        assert result != null: "ReceiveClosureArgInstr result is null";
        
        this.argIndex = argIndex;
        this.restOfArgArray = restOfArgArray;
        this.result = result;
    }
    
    public boolean isRestOfArgArray() {
        return restOfArgArray;
    }

    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }
    
    public Variable getResult() {
        return result;
    }    

    @Override
    public String toString() {
        return super.toString() + "(" + argIndex + (restOfArgArray ? ", ALL" : "") + ")";
    }
    
    public int getArgIndex() {
        return argIndex;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        Object o;
        int numArgs = args.length;
        if (restOfArgArray) {
            if (numArgs < argIndex) {
                o = RubyArray.newArrayNoCopy(context.getRuntime(), new IRubyObject[] {});
            } else {
                IRubyObject[] restOfArgs = new IRubyObject[numArgs-argIndex];
                int j = 0;
                for (int i = argIndex; i < numArgs; i++) {
                    restOfArgs[j] = args[i];
                    j++;
                }
                o = RubyArray.newArray(context.getRuntime(), restOfArgs);
            }
        } else {
            o = (argIndex < numArgs) ? args[argIndex] : context.getRuntime().getNil();
        }
        result.store(context, self, temp, o);
        return null;
    }
}
