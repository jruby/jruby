package org.jruby.ir.instructions;

import org.jruby.RubyKernel;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ThrowExceptionInstr extends OneOperandInstr implements FixedArityInstr {
    public ThrowExceptionInstr(Operand exception) {
        super(Operation.THROW, exception);
    }

    public Operand getException() {
        return getOperand1();
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

    public static ThrowExceptionInstr decode(IRReaderDecoder d) {
        return new ThrowExceptionInstr(d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        if (getException() instanceof IRException) {
            throw ((IRException) getException()).getException(context.runtime);
        }

        Object excObj = getException().retrieve(context, self, currScope, currDynScope, temp);

        if (excObj instanceof Throwable) {
            excObj = Helpers.wrapJavaException(context.runtime, (Throwable) excObj); // IRubyObject
        }

        RubyKernel.raise(context, self, new IRubyObject[] {(IRubyObject) excObj}, Block.NULL_BLOCK);

        // should never get here
        throw new AssertionError("Control shouldn't have reached here in ThrowEx");
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ThrowExceptionInstr(this);
    }
}