package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.WrappedIRScope;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
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

        // Modules and classes set this constant as a side-effect
        if (!(getValue() instanceof WrappedIRScope)) module.setClassVar(getRef(), value);
        return null;
    }
}
