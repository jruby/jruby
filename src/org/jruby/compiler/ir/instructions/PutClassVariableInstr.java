package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
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
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        IRubyObject value = (IRubyObject) getValue().retrieve(interp);
        RubyModule module = (RubyModule) getTarget().retrieve(interp);

        assert module != null : "MODULE should always be something";

        // Modules and classes set this constant as a side-effect
        if (!(getValue() instanceof MetaObject && ((MetaObject) getValue()).isModule())) {
            module.setClassVar(getName(), value);
        }
        return null;
    }
}
