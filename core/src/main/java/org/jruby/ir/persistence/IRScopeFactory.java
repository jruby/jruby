package org.jruby.ir.persistence;

import org.jruby.Ruby;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;

public enum IRScopeFactory {
    INSTANCE;
    private Ruby runtime;

    public void init(Ruby runtime) {
        this.runtime = runtime;
    }

    public IRScope createScope(String name, Instr[] instrs) {
        IRScope scope = new IRScriptBody(runtime.getIRManager(), "__file__", name, null);
        for (Instr instr : instrs) {
            scope.addInstr(instr);
        }
        
        return scope;
    }

}
