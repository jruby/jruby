package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ivars.VariableAccessor;

import static org.jruby.common.IRubyWarnings.ID.IVAR_NOT_INITIALIZED;
import static org.jruby.util.RubyStringBuilder.ids;
import static org.jruby.util.RubyStringBuilder.str;

public class GetFieldInstr extends GetInstr implements FixedArityInstr {
    private transient VariableAccessor accessor = VariableAccessor.DUMMY_ACCESSOR;

    // do not return as nil if it doesn't exist but as undefined.
    public final boolean rawValue;

    public GetFieldInstr(Variable dest, Operand obj, RubySymbol fieldName, boolean rawValue) {
        super(Operation.GET_FIELD, dest, obj, fieldName);

        this.rawValue = rawValue;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetFieldInstr(ii.getRenamedVariable(getResult()),
                getSource().cloneForInlining(ii), getName(), rawValue);
    }

    public static GetFieldInstr decode(IRReaderDecoder d) {
        return new GetFieldInstr(d.decodeVariable(), d.decodeOperand(), d.decodeSymbol(), d.decodeBoolean());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(rawValue);
    }

    public VariableAccessor getAccessor(IRubyObject o) {
        RubyClass cls = o.getMetaClass().getRealClass();
        VariableAccessor localAccessor = accessor;

        if (localAccessor.getClassId() != cls.hashCode()) {
            localAccessor = cls.getVariableAccessorForRead(getId());
            accessor = localAccessor;
        }
        return localAccessor;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getSource().retrieve(context, self, currScope, currDynScope, temp);
        VariableAccessor a = getAccessor(object);
        Object result = a == null ? null : (IRubyObject) a.get(object);

        if (result == null && rawValue) return UndefinedValue.UNDEFINED;
        return result == null && !rawValue ? context.nil : result;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetFieldInstr(this);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + getName(), "raw: " + rawValue};
    }
}
