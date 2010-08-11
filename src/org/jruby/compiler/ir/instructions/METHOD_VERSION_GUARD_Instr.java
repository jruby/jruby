package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;

// Not used anywhere right now!
public class METHOD_VERSION_GUARD_Instr extends GuardInstr
{
    IRMethod    guardedMethod;
    CodeVersion reqdVersion;
    Label       failurePathLabel;

    public METHOD_VERSION_GUARD_Instr(IRMethod m, CodeVersion v, Label failurePathLabel) {
        super(Operation.METHOD_VERSION_GUARD);
        this.guardedMethod = m;
        this.reqdVersion = v;
        this.failurePathLabel = failurePathLabel;
    }

    public Instr cloneForInlining(InlinerInfo ii) { 
        return new METHOD_VERSION_GUARD_Instr(guardedMethod, reqdVersion, ii.getRenamedLabel(failurePathLabel));
    }
}
