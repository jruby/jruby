package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.RubyNil;

public class YieldInstr extends Instr implements ResultInstr {
    Operand blockArg;
    Operand yieldArg;
    private final boolean unwrapArray;
    private Variable result;

    public YieldInstr(Variable result, Variable block, Operand arg, boolean unwrapArray) {
        super(Operation.YIELD);
        
        assert result != null: "YieldInstr result is null";
        
        this.blockArg = block;
        this.yieldArg = arg;
        this.unwrapArray = unwrapArray;
        this.result = result;
    }
   
    public Instr cloneForInlining(InlinerInfo ii) {
        // FIXME: This needs to be cloned!
        return this;  // This is just a placeholder during inlining.
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        Object resultValue;
        Object blk = (Object) blockArg.retrieve(context, self, temp);
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        // Blocks that get yielded are always normal
        Block b = (Block)blk;
        b.type = Block.Type.NORMAL;
        if (yieldArg == null) {
            resultValue = b.yieldSpecific(context);
        } else {
            IRubyObject yieldVal = (IRubyObject)yieldArg.retrieve(context, self, temp);
            resultValue = (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null, null) : b.yield(context, yieldVal);
        }
        
        result.store(context, self, temp, resultValue);
        
        return null;
    }

    @Override
    public String toString() { 
        return unwrapArray ? (super.toString() + "(" + blockArg + ", " + yieldArg + ")") : (super.toString() + "(" + blockArg + ", UNWRAP(" + yieldArg + "))");
    }

    // if unwrapArray, maybe convert yieldArg into a CompoundArray operand?
    public Operand[] getOperands() {
        return (yieldArg == null) ? new Operand[]{blockArg} : new Operand[] {blockArg, yieldArg};
    }
    
    public Variable getResult() {
        return result;
    }    

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand[] getNonBlockOperands() {
        return (yieldArg == null) ? new Operand[]{} : new Operand[] {yieldArg};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        blockArg = blockArg.getSimplifiedOperand(valueMap, force);
        if (yieldArg != null) yieldArg = yieldArg.getSimplifiedOperand(valueMap, force);
    }
}
