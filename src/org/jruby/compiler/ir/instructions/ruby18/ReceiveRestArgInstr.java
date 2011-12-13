package org.jruby.compiler.ir.instructions.ruby18;

import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.CopyInstr;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveRestArgBase;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

/*
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr extends ReceiveRestArgBase {
    public ReceiveRestArgInstr(Variable result, int argIndex) {
        super(result, argIndex);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new CopyInstr(ii.getRenamedVariable(result), ii.getCallArg(argIndex, true));
    }
}
