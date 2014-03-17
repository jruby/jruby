package org.jruby.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutFieldInstr extends PutInstr implements FixedArityInstr {
    public PutFieldInstr(Operand obj, String fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutFieldInstr(getTarget().cloneForInlining(ii), ref, getValue().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getTarget().retrieve(context, self, currDynScope, temp);

        // FIXME: Why getRealClass? Document
        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        clazz.getVariableAccessorForWrite(getRef()).set(object,
                getValue().retrieve(context, self, currDynScope, temp));
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutFieldInstr(this);
    }
}
