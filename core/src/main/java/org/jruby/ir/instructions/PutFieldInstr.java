package org.jruby.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;

public class PutFieldInstr extends PutInstr implements FixedArityInstr {
    private transient VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;

    public PutFieldInstr(Operand obj, RubySymbol fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new PutFieldInstr(getTarget().cloneForInlining(ii), getName(), getValue().cloneForInlining(ii));
    }

    public VariableAccessor getAccessor(IRubyObject o) {
        RubyClass cls = o.getMetaClass().getRealClass();
        VariableAccessor localAccessor = accessor;

        if (localAccessor.getClassId() != cls.hashCode()) {
            localAccessor = cls.getVariableAccessorForWrite(getId());
            accessor = localAccessor;
        }
        return localAccessor;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getTarget().retrieve(context, self, currScope, currDynScope, temp);

        VariableAccessor a = getAccessor(object);
        Object value = getValue().retrieve(context, self, currScope, currDynScope, temp);
        a.set(object, value);

        return null;
    }

    public static PutFieldInstr decode(IRReaderDecoder d) {
        return new PutFieldInstr(d.decodeOperand(), d.decodeSymbol(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutFieldInstr(this);
    }
}
