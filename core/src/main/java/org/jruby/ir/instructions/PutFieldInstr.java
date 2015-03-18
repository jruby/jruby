package org.jruby.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutFieldInstr extends PutInstr implements FixedArityInstr {
    public PutFieldInstr(Operand obj, String fieldName, Operand value) {
        super(Operation.PUT_FIELD, obj, fieldName, value);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new PutFieldInstr(getTarget().cloneForInlining(ii), ref, getValue().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getTarget().retrieve(context, self, currScope, currDynScope, temp);

        // We store instance variable offsets on the real class, since instance var tables are associated with the
        // natural type of an object.
        RubyClass clazz = object.getMetaClass().getRealClass();

        // FIXME: Should add this as a field for instruction
        clazz.getVariableAccessorForWrite(getRef()).set(object,
                getValue().retrieve(context, self, currScope, currDynScope, temp));
        return null;
    }

    public static PutFieldInstr decode(IRReaderDecoder d) {
        return new PutFieldInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutFieldInstr(this);
    }
}
