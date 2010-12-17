package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetFieldInstr extends GetInstr {
    public GetFieldInstr(Variable dest, Operand obj, String fieldName) {
        super(Operation.GET_FIELD, dest, obj, fieldName);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetFieldInstr(ii.getRenamedVariable(result),
                getSource().cloneForInlining(ii), getName());
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        IRubyObject object = (IRubyObject) getSource().retrieve(interp);

        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        getResult().store(interp, clazz.getVariableAccessorForRead(getName()).get(object));
        return null;
    }
}
