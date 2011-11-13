package org.jruby.compiler.ir.instructions.jruby;

import java.util.Map;

import org.jruby.RubyArray;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class ToAryInstr extends Instr implements ResultInstr {
    private final Variable result;
    private final BooleanLiteral dontToAryArrays;
    private Operand array;
    
    public ToAryInstr(Variable result, Operand array, BooleanLiteral dontToAryArrays) {
        super(Operation.TO_ARY);
        
        assert result != null: "ToArtInstr result is null";
        
        this.result = result;
        this.array = array;
        this.dontToAryArrays = dontToAryArrays;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { array };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        array = array.getSimplifiedOperand(valueMap, force);
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
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        Object receiver = array.retrieve(context, self, temp);

        // Don't call to_ary if we we have an array already and we are asked not to run to_ary on arrays
        Object toAryValue;
        if (dontToAryArrays.isTrue() && receiver instanceof RubyArray) {
            toAryValue = receiver;
        } else {
            toAryValue = RuntimeHelpers.aryToAry((IRubyObject) receiver);
        }
        
        result.store(context, self, temp, toAryValue);

        return null;        
    }
}
