package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutGlobalVarInstr extends PutInstr implements FixedArityInstr {
    public PutGlobalVarInstr(String varName, Operand value) {
        super(Operation.PUT_GLOBAL_VAR, new GlobalVariable(varName), null, value);
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { getTarget(), getValue() };
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        String gvName = ((GlobalVariable) getTarget()).getName();

        if (gvName.equals("$_") || gvName.equals("$~")) {
            scope.getFlags().add(IRFlags.USES_BACKREF_OR_LASTLINE);
            return true;
        }

        return false;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutGlobalVarInstr(((GlobalVariable) getTarget()).getName(), getValue().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        GlobalVariable target = (GlobalVariable)getTarget();
        IRubyObject    value  = (IRubyObject) getValue().retrieve(context, self, currDynScope, temp);
        context.runtime.getGlobalVariables().set(target.getName(), value);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutGlobalVarInstr(this);
    }
}
