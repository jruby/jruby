package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutConstInstr extends PutInstr implements FixedArityInstr {
    public PutConstInstr(Operand scopeOrObj, String constName, Operand val) {
        super(Operation.PUT_CONST, scopeOrObj, constName, val);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new PutConstInstr(getTarget().cloneForInlining(ii), ref, getValue().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject value = (IRubyObject) getValue().retrieve(context, self, currScope, currDynScope, temp);
        RubyModule module = (RubyModule) getTarget().retrieve(context, self, currScope, currDynScope, temp);

        assert module != null : "MODULE should always be something";

        module.setConstant(getRef(), value);
        return null;
    }

    public static PutConstInstr decode(IRReaderDecoder d) {
        return new PutConstInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutConstInstr(this);
    }
}
