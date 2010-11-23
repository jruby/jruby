package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutFieldInstr extends PutInstr {
    public PutFieldInstr(Operand obj, String fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutFieldInstr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        IRubyObject object = (IRubyObject) getTarget().retrieve(interp);

        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        clazz.getVariableAccessorForWrite(getName()).set(object, getValue().retrieve(interp));
        return null;
    }
}
