package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.WrappedIRModule;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutClassVariableInstr extends PutInstr {
    public PutClassVariableInstr(Operand scope, String varName, Operand value) {
        super(Operation.PUT_CVAR, scope, varName, value);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutClassVariableInstr(operands[TARGET].cloneForInlining(ii), ref,
                operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Object interpret(InterpreterContext interp, ThreadContext context, IRubyObject self, Block block, Object exception) {
        IRubyObject value = (IRubyObject) getValue().retrieve(interp, context, self);
        RubyModule module = (RubyModule) getTarget().retrieve(interp, context, self);

        assert module != null : "MODULE should always be something";

        // Modules and classes set this constant as a side-effect
        if (!(getValue() instanceof WrappedIRModule)) module.setClassVar(getRef(), value);
        return null;
    }
}
