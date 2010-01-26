package org.jruby.interpreter;

import org.jruby.compiler.ir.IR_Scope;

public class LiveScope {
    protected final IR_Scope scope;

    public LiveScope(IR_Scope scope) {
        this.scope = scope;

        allocateVariableStore();
    }

    private void allocateVariableStore() {


    }

}
