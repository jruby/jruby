package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubyClass.VariableAccessor;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetFieldInstr extends GetInstr {
    public GetFieldInstr(Variable dest, Operand obj, String fieldName) {
        super(Operation.GET_FIELD, dest, obj, fieldName);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetFieldInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getRef());
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) getSource().retrieve(context, self, currDynScope, temp);

        // FIXME: Why getRealClass? Document
        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        VariableAccessor accessor = clazz.getVariableAccessorForRead(getRef());
        Object v = accessor == null ? null : accessor.get(object);
        return v == null ? context.getRuntime().getNil() : v;
    }

    public void compile(JVM jvm) {
        String field = getRef();
        jvm.declareField(field);
        jvm.emit(getSource());
        jvm.method().getField(JVM.OBJECT_TYPE, field, JVM.OBJECT_TYPE);
    }
}
