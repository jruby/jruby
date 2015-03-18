package org.jruby.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;

public class GetFieldInstr extends GetInstr implements FixedArityInstr {
    private VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;

    public GetFieldInstr(Variable dest, Operand obj, String fieldName) {
        super(Operation.GET_FIELD, dest, obj, fieldName);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetFieldInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getRef());
    }

    public static GetFieldInstr decode(IRReaderDecoder d) {
        return new GetFieldInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString());
    }

    public VariableAccessor getAccessor(IRubyObject o) {
        RubyClass cls = o.getMetaClass().getRealClass();
        VariableAccessor localAccessor = accessor;

        if (localAccessor.getClassId() != cls.hashCode()) {
            localAccessor = cls.getVariableAccessorForRead(getRef());
            accessor = localAccessor;
        }
        return localAccessor;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetFieldInstr(this);
    }
}
