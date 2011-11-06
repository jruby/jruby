package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutConstInstr extends PutInstr {
    public PutConstInstr(IRScope scope, String constName, Operand val) {
        super(Operation.PUT_CONST, MetaObject.create(scope), constName, val);
    }

    public PutConstInstr(Operand scopeOrObj, String constName, Operand val) {
        super(Operation.PUT_CONST, scopeOrObj, constName, val);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutConstInstr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject value = (IRubyObject) getValue().retrieve(interp, context, self);
        RubyModule module = (RubyModule) getTarget().retrieve(interp, context, self);

        assert module != null : "MODULE should always be something";

        module.setConstant(getRef(), value);
        return null;
    }
}
