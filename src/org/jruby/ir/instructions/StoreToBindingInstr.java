package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class StoreToBindingInstr extends Instr {
    private IRScope scope;
    private LocalVariable var;

    public StoreToBindingInstr(IRScope scope, LocalVariable var) {
        super(Operation.BINDING_STORE);

        this.var = var;
        this.scope = scope;
    }

    public Operand[] getOperands() {
        return new Operand[]{var};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        var = (LocalVariable)var.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return "store_into_binding(" + scope.getName() + "," + var + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new StoreToBindingInstr(scope, (LocalVariable)var.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // NOP for interpretation
        return null;
    }
}
