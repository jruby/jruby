package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQInstr extends CallInstr implements FixedArityInstr {
    // This is a splatted value and eqq should compare each element in the array vs
    // treating the array as a single value.
    private boolean splattedValue;

    public EQQInstr(IRScope scope, Variable result, Operand v1, Operand v2, boolean splattedValue) {
        super(Operation.EQQ, CallType.FUNCTIONAL, result, scope.getManager().getRuntime().newSymbol("==="), v1, new Operand[] { v2 }, null, false);

        assert result != null: "EQQInstr result is null";

        this.splattedValue = splattedValue;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "splat: " + splattedValue };
    }

    public boolean isSplattedValue() {
        return splattedValue;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new EQQInstr(ii.getScope(), ii.getRenamedVariable(result), getReceiver().cloneForInlining(ii), getArg1().cloneForInlining(ii), isSplattedValue());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getReceiver());
        e.encode(getArg1());
        e.encode(isSplattedValue());
    }

    public static EQQInstr decode(IRReaderDecoder d) {
        return new EQQInstr(d.getCurrentScope(), d.decodeVariable(), d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject recv = (IRubyObject) getReceiver().retrieve(context, self, currScope, currDynScope, temp);
        IRubyObject value = (IRubyObject) getArg1().retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.isEQQ(context, recv, value, callSite, isSplattedValue());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.EQQInstr(this);
    }
}
