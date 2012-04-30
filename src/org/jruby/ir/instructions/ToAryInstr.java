package org.jruby.ir.instructions;

import org.jruby.RubyArray;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRScope;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class ToAryInstr extends Instr implements ResultInstr {
    private Variable result;
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

    @Override
    public Operand simplifyAndGetResult(IRScope scope, Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap, false);
        return dontToAryArrays.isTrue() && (array.getValue(valueMap) instanceof Array) ? array : null;
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ToAryInstr((Variable) result.cloneForInlining(ii), array.cloneForInlining(ii), 
                (BooleanLiteral) dontToAryArrays.cloneForInlining(ii));
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + array + ", dont_to_ary_arrays: " + dontToAryArrays + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object receiver = array.retrieve(context, self, currDynScope, temp);

        // Don't call to_ary if we we have an array already and we are asked not to run to_ary on arrays
        if (dontToAryArrays.isTrue() && receiver instanceof RubyArray) {
            return receiver;
        } else {
            IRubyObject rcvr = (IRubyObject)receiver;
            IRubyObject ary  = RuntimeHelpers.aryToAry(rcvr);
            if (ary instanceof RubyArray) {
                return ary;
            } else {
                String receiverType = rcvr.getType().getName();
                throw context.getRuntime().newTypeError("can't convert " + receiverType + " to Array (" + receiverType + "#to_ary gives " + ary.getType().getName() + ")");
            }
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ToAryInstr(this);
    }
}
