package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveKeywordRestArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public final int required;

    public ReceiveKeywordRestArgInstr(Variable result, int required) {
        super(Operation.RECV_KW_REST_ARG, result, -1);
        this.required = required;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "req: " + required };
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.RECEIVES_KEYWORD_ARGS);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ReceiveKeywordRestArgInstr(ii.getRenamedVariable(result), required);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(required);
    }

    public static ReceiveKeywordRestArgInstr decode(IRReaderDecoder d) {
        return new ReceiveKeywordRestArgInstr(d.decodeVariable(), d.decodeInt());
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args, boolean keywordArgumentSupplied) {
        return IRRuntimeHelpers.receiveKeywordRestArg(context, args, required, keywordArgumentSupplied);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveKeywordRestArgInstr(this);
    }

}
