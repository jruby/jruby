package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;

public class Symbol extends ImmutableLiteral {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol("", ASCIIEncoding.INSTANCE);

    private final String name;
    private final Encoding encoding;

    public Symbol(String name, Encoding encoding) {
        super(OperandType.SYMBOL);

        this.name = name;
        this.encoding = encoding;
    }

    public String getName() {
        return name;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubySymbol.newSymbol(context.runtime, getName(), encoding);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public String toString() {
        return ":'" + getName() + "'";
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getName());
        e.encode(getEncoding());
    }

    public static Symbol decode(IRReaderDecoder d) {
        return new Symbol(d.decodeString(), d.decodeEncoding());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }
}
