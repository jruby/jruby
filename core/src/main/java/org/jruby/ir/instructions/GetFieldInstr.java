package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.ivars.VariableAccessor;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
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

    public VariableAccessor getAccessor(IRubyObject o) {
        RubyClass cls = o.getMetaClass().getRealClass();
        VariableAccessor localAccessor = accessor;
        IRubyObject value;
        if (localAccessor.getClassId() != cls.hashCode()) {
            localAccessor = cls.getVariableAccessorForRead(getRef());
            accessor = localAccessor;
        }
        return localAccessor;
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) getSource().retrieve(context, self, currDynScope, temp);
        VariableAccessor a = getAccessor(object);
        IRubyObject value = a == null ? context.nil : (IRubyObject)a.get(object);
        return value == null ? context.nil : value;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetFieldInstr(this);
    }
}
