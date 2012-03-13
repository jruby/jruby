package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ClosureReturnInstr extends ReturnBase {
    public ClosureReturnInstr(Operand rv) {
        super(Operation.CLOSURE_RETURN, rv);
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        return new ClosureReturnInstr(returnValue.cloneForInlining(ii));
    }

    @Override
    public Instr cloneForInlinedClosure(InlinerInfo ii) {
        return new CopyInstr(ii.getYieldResult(), returnValue.cloneForInlining(ii));
    }
}
