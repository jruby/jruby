package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Splat;
import org.jruby.compiler.ir.operands.CompoundArray;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.RubyProc;
import org.jruby.RubyNil;

public class YieldInstr extends Instr {
    Operand block;
    Operand yieldArg;
    private final boolean wrapIntoArray;

    public YieldInstr(Variable result, Variable block, Operand arg, boolean wrapIntoArray) {
        super(Operation.YIELD, result);
        this.block = block;
        this.yieldArg = arg;
        this.wrapIntoArray = wrapIntoArray;
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
            if ((yieldArg instanceof Splat) || (yieldArg instanceof CompoundArray)) {
                if (wrapIntoArray) resultValue = b.yield(context, yieldVal);
                else resultValue = b.yieldArray(context, yieldVal, null, null);
            } else {
                resultValue = b.yield(context, yieldVal);
            }
        }
        getResult().store(interp, context, self, resultValue);
        return null;
    }

    @Override
    public String toString() { 
        return wrapIntoArray ? (super.toString() + "(" + block + ", WRAP[" + yieldArg + "])") : (super.toString() + "(" + block + ", " + yieldArg + ")");
    }

    // if wrapIntoArray, maybe convert yieldArg into a CompoundArray operand?
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
