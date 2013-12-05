package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class GVarAliasInstr extends Instr {
    private Operand newName;
    private Operand oldName;

    public GVarAliasInstr(Operand newName, Operand oldName) {
        super(Operation.GVAR_ALIAS);

        this.newName = newName;
        this.oldName = oldName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { newName, oldName };
    }

    @Override
    public String toString() {
        return getOperation().toString() + "(" + newName + ", " + oldName + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        oldName = oldName.getSimplifiedOperand(valueMap, force);
        newName = newName.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GVarAliasInstr(newName.cloneForInlining(ii), oldName.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        String newNameString = newName.retrieve(context, self, currDynScope, temp).toString();
        String oldNameString = oldName.retrieve(context, self, currDynScope, temp).toString();

        context.runtime.getGlobalVariables().alias(newNameString, oldNameString);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GVarAliasInstr(this);
    }
}
