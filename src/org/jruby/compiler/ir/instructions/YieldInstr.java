package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.RubyArray;
import org.jruby.RubyProc;
import org.jruby.RubyNil;

public class YieldInstr extends Instr {
    Operand block;
    Operand yieldArg;
    private final boolean unwrapArray;

    public YieldInstr(Variable result, Variable block, Operand arg, boolean unwrapArray) {
        super(Operation.YIELD, result);
        this.block = block;
        this.yieldArg = arg;
        this.unwrapArray = unwrapArray;
    }
   
    public Instr cloneForInlining(InlinerInfo ii) {
        // FIXME: This needs to be cloned!
        return this;  // This is just a placeholder during inlining.
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object resultValue;
        Object blk = (Object)block.retrieve(interp, context, self);
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (blk instanceof RubyNil) blk = Block.NULL_BLOCK;
        // Blocks that get yielded are always normal
        Block b = (Block)blk;
        b.type = Block.Type.NORMAL;
        if (yieldArg == null) {
            resultValue = b.yieldSpecific(context);
        } else {
            IRubyObject yieldVal = (IRubyObject)yieldArg.retrieve(interp, context, self);
            resultValue = (unwrapArray && (yieldVal instanceof RubyArray)) ? b.yieldArray(context, yieldVal, null, null) : b.yield(context, yieldVal);
        }
        getResult().store(interp, context, self, resultValue);
        return null;
    }

    @Override
    public String toString() { 
        return unwrapArray ? (super.toString() + "(" + block + ", " + yieldArg + ")") : (super.toString() + "(" + block + ", UNWRAP(" + yieldArg + "))");
    }

    // if unwrapArray, maybe convert yieldArg into a CompoundArray operand?
    public Operand[] getOperands() {
        return (yieldArg == null) ? new Operand[]{block} : new Operand[] {block, yieldArg};
    }

    public Operand[] getNonBlockOperands() {
        return (yieldArg == null) ? new Operand[]{} : new Operand[] {yieldArg};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        if (yieldArg != null) yieldArg = yieldArg.getSimplifiedOperand(valueMap);
    }
}
