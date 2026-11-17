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
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public class ReceiveKeywordArgInstr extends ReceiveArgBase implements FixedArityInstr {
    public final RubySymbol key;

    public ReceiveKeywordArgInstr(Variable result, Variable keyword, RubySymbol key) {
        super(Operation.RECV_KW_ARG, result, keyword);
        this.key = key;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + getKey() };
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
        return new ReceiveKeywordArgInstr(ii.getRenamedVariable(result), ii.getRenamedVariable(getKeywords()), getKey());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getKey());
    }

    public static ReceiveKeywordArgInstr decode(IRReaderDecoder d) {
        return new ReceiveKeywordArgInstr(d.decodeVariable(), d.decodeVariable(), d.decodeSymbol());
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope,
                                  Object[] temp, IRubyObject[] args, boolean ruby2keyword) {
        IRubyObject keywords = (IRubyObject) getKeywords().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.receiveKeywordArg(keywords, getKey());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveKeywordArgInstr(this);
    }
}
