package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutClassVariableInstr extends PutInstr {
    public PutClassVariableInstr(Operand scope, String varName, Operand value) {
        super(Operation.PUT_CVAR, scope, varName, value);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutClassVariableInstr(operands[TARGET].cloneForInlining(ii), ref,
                operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject value = (IRubyObject) getValue().retrieve(context, self, currDynScope, temp);
        RubyModule module = (RubyModule) getTarget().retrieve(context, self, currDynScope, temp);

        assert module != null : "MODULE should always be something";

		  // SSS FIXME: What is this check again???
        // Modules and classes set this constant as a side-effect
        if (!(getValue() instanceof CurrentScope)) module.setClassVar(getRef(), value);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutClassVariableInstr(this);
    }
}
