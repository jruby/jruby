package org.jruby.ir.instructions;

import org.jruby.RubyKernel;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;
import org.jruby.runtime.Helpers;

// Right now, this is primarily used by the JRuby implementation.
// Ruby exceptions go through RubyKernel.raise (or RubyThread.raise).
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

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new ThrowExceptionInstr(exceptionArg.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        if (exceptionArg instanceof IRException) {
            throw ((IRException) exceptionArg).getException(context.runtime);
        }

        Object excObj = exceptionArg.retrieve(context, self, currDynScope, temp);

        if (excObj instanceof IRubyObject) {
            RubyKernel.raise(context, context.runtime.getKernel(), new IRubyObject[] {(IRubyObject)excObj}, Block.NULL_BLOCK);
        } else if (excObj instanceof Throwable) { // java exception -- avoid having to add 'throws' clause everywhere!
            // SSS FIXME: Can avoid this workaround by adding a case for this instruction in the interpreter loop
            Helpers.throwException((Throwable)excObj);
        }

        // should never get here
        throw new RuntimeException("Control shouldn't have reached here in ThrowExceptionInstr.  excObj is: " + excObj);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ThrowExceptionInstr(this);
    }
}
