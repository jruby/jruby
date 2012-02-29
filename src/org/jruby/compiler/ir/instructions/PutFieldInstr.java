package org.jruby.compiler.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutFieldInstr extends PutInstr {
    public PutFieldInstr(Operand obj, String fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutFieldInstr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) getTarget().retrieve(context, self, currDynScope, temp);

        // FIXME: Why getRealClass? Document
        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        clazz.getVariableAccessorForWrite(getRef()).set(object, 
                getValue().retrieve(context, self, currDynScope, temp));
        return null;
    }

    public void compile(JVM jvm) {
        String field = getRef();
        jvm.emit(getTarget());
        jvm.emit(getValue());
        jvm.method().putField(field);
    }
}
