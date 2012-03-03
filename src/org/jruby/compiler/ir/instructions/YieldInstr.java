package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.RubyNil;
import org.jruby.runtime.DynamicScope;

public class YieldInstr extends Instr implements ResultInstr {
    public final boolean unwrapArray;
    private Operand blockArg;
    private Operand yieldArg;
    private Variable result;

    public YieldInstr(Variable result, Operand block, Operand arg, boolean unwrapArray) {
        super(Operation.YIELD);
        
        assert result != null: "YieldInstr result is null";
        
        this.blockArg = block;
        this.yieldArg = arg == null ? UndefinedValue.UNDEFINED : arg;
        this.unwrapArray = unwrapArray;
        this.result = result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new YieldInstr(ii.getRenamedVariable(result), blockArg.cloneForInlining(ii), yieldArg.cloneForInlining(ii), unwrapArray);
    }

    public Operand getBlockArg() {
        return blockArg;
    }

    public Operand getYieldArg() {
        return yieldArg;
    }

    @Override
    public String toString() { 
        return unwrapArray ? (super.toString() + "(" + blockArg + ", UNWRAP(" + yieldArg + "))") : (super.toString() + "(" + blockArg + ", " + yieldArg + ")");
    }

    // if unwrapArray, maybe convert yieldArg into a CompoundArray operand?
    public Operand[] getOperands() {
        return new Operand[] {blockArg, yieldArg};
    }
    
    public Variable getResult() {
        return result;
    }    

    public void updateResult(Variable v) {
        this.result = v;
    }

    public Operand[] getNonBlockOperands() {
        return new Operand[] {yieldArg};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        blockArg = blockArg.getSimplifiedOperand(valueMap, force);
        yieldArg = yieldArg.getSimplifiedOperand(valueMap, force);
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object resultValue;
        Object blk = (Object) blockArg.retrieve(context, self, currDynScope, temp);
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        Block b = (Block)blk;
        // Ruby 1.8 mode: yields are always to normal blocks
        if (!context.getRuntime().is1_9()) b.type = Block.Type.NORMAL;
        if (yieldArg == UndefinedValue.UNDEFINED) {
            return b.yieldSpecific(context);
        } else {
            IRubyObject yieldVal = (IRubyObject)yieldArg.retrieve(context, self, currDynScope, temp);
            return (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null, null) : b.yield(context, yieldVal);
        }
    }
}
