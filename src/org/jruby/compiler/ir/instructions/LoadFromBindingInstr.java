package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Interp;
import org.jruby.compiler.ir.operands.LocalVariable;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class LoadFromBindingInstr extends Instr implements ResultInstr {
    private IRScope scope;
    private LocalVariable var;

    public LoadFromBindingInstr(IRScope scope, LocalVariable var) {
        super(Operation.BINDING_LOAD);

        assert var != null: "LoadFromBindingInstr result is null";

        this.var = var;
        this.scope = scope;
    }

    public Operand[] getOperands() { 
        return Operand.EMPTY_ARRAY;
    }
    
    public Variable getResult() {
        return var;
    }
    
    public void updateResult(Variable v) {
        this.var = (LocalVariable)v;
    }

    public String toString() {
        return var + " = load_from_binding(" + scope.getName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new LoadFromBindingInstr(scope, (LocalVariable)ii.getRenamedVariable(var));
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // NOP for interpretation
        return null;
    }
}
