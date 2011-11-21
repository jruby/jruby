package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        oldName = oldName.getSimplifiedOperand(valueMap, force);
        newName = newName.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GVarAliasInstr(newName.cloneForInlining(ii), oldName.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, Object[] temp, Block block) {
        String newNameString = newName.retrieve(context, self, temp).toString();
        String oldNameString = oldName.retrieve(context, self, temp).toString();

        context.getRuntime().getGlobalVariables().alias(newNameString, oldNameString);
        return null;
    }
}
