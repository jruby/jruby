package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.MutableString;
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
import org.jruby.util.ByteList;

public class AsStringInstr extends ZeroOperandArgNoBlockCallInstr {
    public static final ByteList TO_S = new ByteList(new byte[] {'t', 'o', '_', 's'});

    public AsStringInstr(IRScope scope, Variable result, Operand source, boolean isPotentiallyRefined) {
        super(
                scope,
                Operation.AS_STRING,
                CallType.FUNCTIONAL,
                result,
                scope.getManager().getRuntime().newSymbol(TO_S),
                nonNull(source),
                Operand.EMPTY_ARRAY,
                0,
                isPotentiallyRefined);

        assert isPotentiallyRefined : "AsStringInstr should only be needed in refined scopes";
    }

    private static Operand nonNull(Operand source) {
        return source == null ? MutableString.EMPTY_STRING : source;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new AsStringInstr(ii.getScope(), (Variable) getResult().cloneForInlining(ii),
                getReceiver().cloneForInlining(ii), isPotentiallyRefined());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        e.encode(Operation.AS_STRING);
        e.encode(result);
        e.encode(getReceiver());
        e.encode(isPotentiallyRefined());
    }

    public static AsStringInstr decode(IRReaderDecoder d) {
        return new AsStringInstr(d.getCurrentScope(), d.decodeVariable(), d.decodeOperand(), d.decodeBoolean());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object receiver = getReceiver().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.asString(context, self, (IRubyObject) receiver, getCallSite());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AsStringInstr(this);
    }
}
