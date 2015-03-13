package org.jruby.ir.instructions;

import org.jruby.RubyKernel;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Right now, this is primarily used by the JRuby implementation.
// Ruby exceptions go through RubyKernel.raise (or RubyThread.raise).
public class ThrowExceptionInstr extends Instr implements FixedArityInstr {
    public ThrowExceptionInstr(Operand exception) {
        super(Operation.THROW, new Operand[] { exception });
    }

    public Operand getException() {
        return operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ThrowExceptionInstr(getException().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getException());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        if (getException() instanceof IRException) {
            throw ((IRException) getException()).getException(context.runtime);
        }

        Object excObj = getException().retrieve(context, self, currScope, currDynScope, temp);

        if (excObj instanceof IRubyObject) {
            RubyKernel.raise(context, context.runtime.getKernel(), new IRubyObject[] {(IRubyObject)excObj}, Block.NULL_BLOCK);
        } else if (excObj instanceof Throwable) { // java exception -- avoid having to add 'throws' clause everywhere!
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