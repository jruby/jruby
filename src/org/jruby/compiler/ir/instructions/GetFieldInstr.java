package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyClass.VariableAccessor;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetFieldInstr extends GetInstr {
    private VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;
    
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
        Ruby runtime = context.runtime;
        IRubyObject object = (IRubyObject) getSource().retrieve(context, self, currDynScope, temp);

        RubyClass cls = object.getMetaClass().getRealClass();
        VariableAccessor localAccessor = accessor;
        IRubyObject value;
        if (localAccessor.getClassId() != cls.hashCode()) {
            localAccessor = cls.getVariableAccessorForRead(getRef());
            if (localAccessor == null) return runtime.getNil();
            value = (IRubyObject)localAccessor.get(object);
            accessor = localAccessor;
        } else {
            value = (IRubyObject)localAccessor.get(object);
        }
        return value == null ? runtime.getNil() : value;
    }

    @Override
    public void compile(JVM jvm) {
        String field = getRef();
        jvm.emit(getSource());
        jvm.method().getField(field);
        jvm.method().storeLocal(jvm.methodData().local(getResult()));
    }
}
