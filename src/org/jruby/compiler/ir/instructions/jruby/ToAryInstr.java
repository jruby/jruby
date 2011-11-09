package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.RubyArray;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class ToAryInstr extends Instr implements ResultInstr {
    private final Variable result;
    private final Operand array;
    private final BooleanLiteral dontToAryArrays;
    
    public ToAryInstr(Variable result, Operand array, BooleanLiteral dontToAryArrays) {
        super(Operation.TO_ARY);
        
        this.result = result;
        this.array = array;
        this.dontToAryArrays = dontToAryArrays;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { array };
    }
    
    public Variable getResult() {
        return result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ToAryInstr((Variable) result.cloneForInlining(ii), array.cloneForInlining(ii), 
                (BooleanLiteral) dontToAryArrays.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object receiver = array.retrieve(interp, context, self);

        // Don't call to_ary if we we have an array already and we are asked not to run to_ary on arrays
        Object toAryValue;
        if (dontToAryArrays.isTrue() && receiver instanceof RubyArray) {
            toAryValue = receiver;
        } else {
            toAryValue = RuntimeHelpers.aryToAry((IRubyObject) receiver);
        }
        
        getResult().store(interp, context, self, toAryValue);

        return null;        
    }
}
