package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StoreLocalVarInstr extends Instr {
    private IRScope scope;
    private Operand value;

    /** This is the variable that is being stored into in this scope.  This variable
     * doesn't participate in the computation itself.  We just use it as a proxy for
     * its (a) name (b) offset (c) scope-depth. */
    private LocalVariable lvar;

    public StoreLocalVarInstr(Operand value, IRScope scope, LocalVariable lvar) {
        super(Operation.BINDING_STORE);

        this.lvar = lvar;
        this.value = value;
        this.scope = scope;
    }

    public Operand[] getOperands() {
        return new Operand[]{value};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        value = value.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return "store_into_binding(" + value + ", " + scope.getName() + ", " + lvar + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: Do we need to rename lvar really?  It is just a name-proxy!
        return new StoreLocalVarInstr(value.cloneForInlining(ii), scope, (LocalVariable)lvar.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object varValue = value.retrieve(context, self, currDynScope, temp);
        currDynScope.setValue((IRubyObject)varValue, lvar.getLocation(), lvar.getScopeDepth());
        return null;
    }
}
