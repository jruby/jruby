package org.jruby.compiler.ir.instructions.ruby19;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ReceiveOptArgBase;
import org.jruby.compiler.ir.operands.UndefinedValue;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

public class ReceiveOptArgInstr extends ReceiveOptArgBase {
    /** This instruction gets to pick an argument off the incoming list only if
     *  there are at least this many incoming arguments */
    public final int minArgsLength;

    public ReceiveOptArgInstr(Variable result, int index, int minArgsLength) {
        super(result, index);
        this.minArgsLength = minArgsLength;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        throw new RuntimeException("Not implemented yet!");
    }
}
