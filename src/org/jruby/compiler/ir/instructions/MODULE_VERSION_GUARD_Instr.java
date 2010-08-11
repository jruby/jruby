package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.CodeVersion;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.representations.InlinerInfo;

// Not used anywhere right now!
public class MODULE_VERSION_GUARD_Instr extends GuardInstr
{
    IRModule   guardedModule;
    CodeVersion reqdVersion;
    Label       failurePathLabel;

    public MODULE_VERSION_GUARD_Instr(IRModule m, CodeVersion v, Label failurePathLabel) {
        super(Operation.MODULE_VERSION_GUARD);
        this.guardedModule = m;
        this.reqdVersion = v;
        this.failurePathLabel = failurePathLabel;
    }

    public Instr cloneForInlining(InlinerInfo ii) { 
        return new MODULE_VERSION_GUARD_Instr(guardedModule, reqdVersion, ii.getRenamedLabel(failurePathLabel));
    }
}
