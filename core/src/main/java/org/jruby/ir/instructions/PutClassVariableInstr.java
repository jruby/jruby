package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutClassVariableInstr extends PutInstr implements FixedArityInstr {
    public PutClassVariableInstr(Operand scope, String varName, Operand value) {
        super(Operation.PUT_CVAR, scope, varName, value);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new PutClassVariableInstr(getTarget().cloneForInlining(ii), ref, getValue().cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject value = (IRubyObject) getValue().retrieve(context, self, currScope, currDynScope, temp);
        RubyModule module = (RubyModule) getTarget().retrieve(context, self, currScope, currDynScope, temp);

        assert module != null : "MODULE should always be something";

		// SSS FIXME: What is this check again???
        // Modules and classes set this constant as a side-effect
        if (!(getValue() instanceof CurrentScope)) module.setClassVar(getRef(), value);
        return null;
    }

    public static PutClassVariableInstr decode(IRReaderDecoder d) {
        return new PutClassVariableInstr(d.decodeOperand(), d.decodeString(), d.decodeOperand());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.PutClassVariableInstr(this);
    }
}
