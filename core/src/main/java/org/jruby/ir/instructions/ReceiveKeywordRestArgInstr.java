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
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public class ReceiveKeywordRestArgInstr extends ReceiveArgBase implements FixedArityInstr {

    public ReceiveKeywordRestArgInstr(Variable result, Variable keywords) {
        super(Operation.RECV_KW_REST_ARG, result, keywords, -1);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        scope.setReceivesKeywordArgs();
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ReceiveKeywordRestArgInstr(ii.getRenamedVariable(result), ii.getRenamedVariable(getKeywords()));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
    }

    public static ReceiveKeywordRestArgInstr decode(IRReaderDecoder d) {
        return new ReceiveKeywordRestArgInstr(d.decodeVariable(), d.decodeVariable());
    }

    public IRubyObject receiveArg(ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope,
                                  Object[] temp, IRubyObject[] args, boolean acceptsKeywords, boolean ruby2keyword) {
        IRubyObject keywords = (IRubyObject) getKeywords().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.receiveKeywordRestArg(context, keywords);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveKeywordRestArgInstr(this);
    }

}
