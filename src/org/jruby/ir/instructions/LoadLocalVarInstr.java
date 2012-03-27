package org.jruby.ir.instructions;

import org.jruby.ir.Operation;
import org.jruby.ir.IRScope;
import org.jruby.ir.Interp;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class LoadLocalVarInstr extends Instr implements ResultInstr {
    private IRScope scope;
    private TemporaryVariable result;

    /** This is the variable that is being loaded from the scope.  This variable
     * doesn't participate in the computation itself.  We just use it as a proxy for
     * its (a) name (b) offset (c) scope-depth. */
    private LocalVariable lvar; 

    public LoadLocalVarInstr(IRScope scope, TemporaryVariable result, LocalVariable lvar) {
        super(Operation.BINDING_LOAD);

        assert result != null: "LoadLocalVarInstr result is null";

        this.lvar = lvar;
        this.result = result;
        this.scope = scope;
    }

    public Operand[] getOperands() { 
        return Instr.EMPTY_OPERANDS;
    }

    public Variable getResult() {
        return result;
    }
    
    public void updateResult(Variable v) {
        this.result = (TemporaryVariable)v;
    }

    public String toString() {
        return result + " = load_from_binding(" + scope.getName() + ", " + lvar + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: Do we need to rename lvar really?  It is just a name-proxy!
        return new LoadLocalVarInstr(scope, (TemporaryVariable)ii.getRenamedVariable(result), (LocalVariable)ii.getRenamedVariable(lvar));
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return lvar.retrieve(context, self, currDynScope, temp);
    }
}
