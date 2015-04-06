package org.jruby.ir.instructions;

import java.util.Arrays;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ConstMissingInstr extends CallInstr implements FixedArityInstr {
    private final String missingConst;

    public ConstMissingInstr(Variable result, Operand currentModule, String missingConst, boolean isPotentiallyRefined) {
        // FIXME: Missing encoding knowledge of the constant name.
        super(Operation.CONST_MISSING, CallType.FUNCTIONAL, result, "const_missing", currentModule,
                new Operand[]{new Symbol(missingConst, USASCIIEncoding.INSTANCE)}, null, isPotentiallyRefined);

        this.missingConst = missingConst;
    }

    public String getMissingConst() {
        return missingConst;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ConstMissingInstr(ii.getRenamedVariable(result), getReceiver().cloneForInlining(ii), missingConst, isPotentiallyRefined());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + getOperation() + "): " + this);
        e.encode(getOperation());
        e.encode(getResult());
        e.encode(getReceiver());
        e.encode(getMissingConst());
    }

    public static ConstMissingInstr decode(IRReaderDecoder d) {
        return new ConstMissingInstr(d.decodeVariable(), d.decodeOperand(), d.decodeString(), d.decodeBoolean());
    }

    @Override
    public String[] toStringNonOperandArgs() {
        String[] base = super.toStringNonOperandArgs();
        String[] args = Arrays.copyOf(base, base.length + 1);

        args[args.length - 1] = "missing: " + missingConst;

        return  args;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyModule module = (RubyModule) getReceiver().retrieve(context, self, currScope, currDynScope, temp);
        return module.callMethod(context, "const_missing", context.runtime.fastNewSymbol(missingConst));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ConstMissingInstr(this);
    }
}
