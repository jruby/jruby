package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;

// If v2 is an array, compare v1 with every element of v2 and stop on first match!
public class EQQInstr extends TwoOperandResultBaseInstr implements FixedArityInstr {
    private final CallSite callSite;
    // This is a splatted value and eqq should compare each element in the array vs
    // treating the array as a single value.
    private boolean splattedValue;

    public EQQInstr(Variable result, Operand v1, Operand v2, boolean splattedValue) {
        super(Operation.EQQ, result, v1, v2);

        assert result != null: "EQQInstr result is null";

        this.callSite = new FunctionalCachingCallSite("===");
        this.splattedValue = splattedValue;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "splat: " + splattedValue };
    }

    @Deprecated
    public EQQInstr(Variable result, Operand v1, Operand v2) {
        this(result, v1, v2, true);
    }

    public Operand getArg1() {
        return getOperand1();
    }

    public Operand getArg2() {
        return getOperand2();
    }

    public boolean isSplattedValue() {
        return splattedValue;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new EQQInstr(ii.getRenamedVariable(result), getArg1().cloneForInlining(ii), getArg2().cloneForInlining(ii), isSplattedValue());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getArg1());
        e.encode(getArg2());
        e.encode(isSplattedValue());
    }

    public static EQQInstr decode(IRReaderDecoder d) {
        return new EQQInstr(d.decodeVariable(), d.decodeOperand(), d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject recv = (IRubyObject) getArg1().retrieve(context, self, currScope, currDynScope, temp);
        IRubyObject value = (IRubyObject) getArg2().retrieve(context, self, currScope, currDynScope, temp);
        return IRRuntimeHelpers.isEQQ(context, recv, value, callSite, isSplattedValue());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.EQQInstr(this);
    }
}
