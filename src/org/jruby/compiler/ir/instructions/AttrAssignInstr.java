package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.CallType;

// Instruction representing Ruby code of the form: "a[i] = 5"
// which is equivalent to: a.[](i,5)
public class AttrAssignInstr extends NoResultCallInstr {
    public AttrAssignInstr(Operand obj, MethAddr attr, Operand[] args) {
        super(Operation.ATTR_ASSIGN, CallType.UNKNOWN, attr, obj, args, null);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new AttrAssignInstr(receiver.cloneForInlining(ii), (MethAddr)getMethodAddr().cloneForInlining(ii), cloneCallArgs(ii));
    }
}
