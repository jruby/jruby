package org.jruby.ir.instructions.defined;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.FixedArityInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.ResultBaseInstr;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetErrorInfoInstr extends ResultBaseInstr implements FixedArityInstr {
    public GetErrorInfoInstr(Variable result) {
        super(Operation.GET_ERROR_INFO, result, EMPTY_OPERANDS);
    }

    @Override
    public Instr clone(CloneInfo info) {
        return new GetErrorInfoInstr((Variable) getResult().cloneForInlining(info));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return context.getErrorInfo();
    }

    public static GetErrorInfoInstr decode(IRReaderDecoder d) {
        return new GetErrorInfoInstr(d.decodeVariable());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetErrorInfoInstr(this);
    }
}
