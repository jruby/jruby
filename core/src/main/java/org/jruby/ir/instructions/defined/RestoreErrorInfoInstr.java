package org.jruby.ir.instructions.defined;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.FixedArityInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RestoreErrorInfoInstr extends Instr implements FixedArityInstr {
    public RestoreErrorInfoInstr(Operand arg) {
        super(Operation.RESTORE_ERROR_INFO, new Operand[] { arg });
    }

    public Operand getArg() {
        return operands[0];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new RestoreErrorInfoInstr(getArg().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArg());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        context.setErrorInfo((IRubyObject) getArg().retrieve(context, self, currScope, currDynScope, temp));

        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.RestoreErrorInfoInstr(this);
    }
}
