package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.*;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public class ReceiveKeywordArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public final RubySymbol key;
    public final int required;

    public ReceiveKeywordArgInstr(Variable result, RubySymbol key, int required) {
        super(Operation.RECV_KW_ARG, result, -1);
        this.key = key;
        this.required = required;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + getKey(), "req: " + required};
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        scope.setReceivesKeywordArgs();
        return true;
    }

    public String getId() {
        return key.idString();
    }

    public RubySymbol getKey() {
        return key;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ReceiveKeywordArgInstr(ii.getRenamedVariable(result), getKey(), required);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getKey());
        e.encode(required);
    }

    public static ReceiveKeywordArgInstr decode(IRReaderDecoder d) {
        return new ReceiveKeywordArgInstr(d.decodeVariable(), d.decodeSymbol(), d.decodeInt());
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args, boolean acceptsKeywordArgument) {
        return IRRuntimeHelpers.receiveKeywordArg(context, args, required, getKey(), acceptsKeywordArgument);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveKeywordArgInstr(this);
    }
}
