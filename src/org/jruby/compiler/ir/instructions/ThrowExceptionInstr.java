package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.IRException;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyKernel;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;

// Right now, this is primarily used for JRuby implementation.  Ruby exceptions go through
// RubyKernel.raise (or RubyThread.raise).
public class ThrowExceptionInstr extends Instr {
    private Operand exceptionArg;

    public ThrowExceptionInstr(Operand exception) {
        super(Operation.THROW);
        this.exceptionArg = exception;
    }

    public Operand[] getOperands() {
        return new Operand[]{ exceptionArg };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        exceptionArg = exceptionArg.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + exceptionArg + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new ThrowExceptionInstr(exceptionArg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        if (exceptionArg instanceof IRException) throw ((IRException) exceptionArg).getException(context.getRuntime());

        Object excObj = exceptionArg.retrieve(context, self, temp);
            
        if (excObj instanceof IRubyObject) {
            RubyKernel.raise(context, context.getRuntime().getKernel(), new IRubyObject[] {(IRubyObject)excObj}, Block.NULL_BLOCK);
        } else if (excObj instanceof Error) { // from regular ensures -- these should get passed through one level.
            throw (Error) excObj;
        } 
        
        // from breaks running ensures -- these should get passed through one level.
        throw (RuntimeException) excObj;
    }
}
