package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubyClass.VariableAccessor;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetFieldInstr extends GetInstr {
    public GetFieldInstr(Variable dest, Operand obj, String fieldName) {
        super(Operation.GET_FIELD, dest, obj, fieldName);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetFieldInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getRef());
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        IRubyObject object = (IRubyObject) getSource().retrieve(interp, context, self);

        // FIXME: Why getRealClass? Document
        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        VariableAccessor accessor = clazz.getVariableAccessorForRead(getRef());
        Object v = accessor == null ? null : accessor.get(object);
        getResult().store(interp, context, self, v == null ? context.getRuntime().getNil() : v);
        return null;
    }
}
