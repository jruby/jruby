package org.jruby.ir.instructions;

import org.jcodings.Encoding;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class GetEncodingInstr extends ResultBaseInstr implements FixedArityInstr {
    private final Encoding encoding;

    public GetEncodingInstr(Variable result, Encoding encoding) {
        super(Operation.GET_ENCODING, result, EMPTY_OPERANDS);

        this.encoding = encoding;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + encoding };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new GetEncodingInstr(ii.getRenamedVariable(result), encoding);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return context.runtime.getEncodingService().getEncoding(encoding);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getEncoding());
    }

    public static GetEncodingInstr decode(IRReaderDecoder d) {
        return new GetEncodingInstr(d.decodeVariable(), d.decodeEncoding());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetEncodingInstr(this);
    }
}
