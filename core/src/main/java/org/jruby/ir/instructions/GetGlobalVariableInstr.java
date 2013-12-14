package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetGlobalVariableInstr extends GetInstr {
    public GetGlobalVariableInstr(Variable dest, String gvarName) {
        this(dest, new GlobalVariable(gvarName));
    }
    
    public GetGlobalVariableInstr(Variable dest, GlobalVariable gvar) {
        super(Operation.GET_GLOBAL_VAR, dest, gvar, null); 
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { getSource() };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetGlobalVariableInstr(ii.getRenamedVariable(getResult()), ((GlobalVariable)getSource()).getName());
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        return getSource().retrieve(context, self, currDynScope, temp);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetGlobalVariableInstr(this);
    }
}
