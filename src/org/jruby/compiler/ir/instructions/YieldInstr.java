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
import org.jruby.RubyProc;

public class YieldInstr extends Instr {
    Operand block;
    Operand yieldArg;

    public YieldInstr(Variable result, Variable block, Operand arg) {
        super(Operation.YIELD, result);
        this.block = block;
        this.yieldArg = arg;
    }
   
    public Instr cloneForInlining(InlinerInfo ii) {
        // FIXME: This needs to be cloned!
        return this;  // This is just a placeholder during inlining.
    }

    @Interp
    @Override
    public Label interpret(InterpreterContext interp) {
        Object resultValue;
        Object blk = (Object)block.retrieve(interp);
        if (blk instanceof RubyProc) blk = ((RubyProc)blk).getBlock();
        if (yieldArg == null) {
            resultValue = ((Block)blk).call(interp.getContext());
        } else {
            resultValue = ((Block)blk).yield(interp.getContext(), (IRubyObject)yieldArg.retrieve(interp));
        }
        getResult().store(interp, resultValue);
        return null;
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + block + ", " + yieldArg + ")";
    }

    public Operand[] getOperands() {
        return (yieldArg == null) ? new Operand[]{block} : new Operand[] {block, yieldArg};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        if (yieldArg != null) yieldArg = yieldArg.getSimplifiedOperand(valueMap);
    }
}
