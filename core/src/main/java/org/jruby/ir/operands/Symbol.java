package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

public class Symbol extends ImmutableLiteral {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol(ByteList.EMPTY_BYTELIST);

    private final ByteList name;

    public Symbol(ByteList name) {
        super();

        this.name = name;
    }

    public Symbol(String name, Encoding encoding) {
        super();

        this.name = new ByteList(name.getBytes(encoding.getCharset()), encoding, false);
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL;
    }

    public String getName() {
        return name.toString();
    }

    public ByteList getBytes() {
        return name;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubySymbol.newSymbol(context.runtime, name);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public Encoding getEncoding() {
        return name.getEncoding();
    }

    @Override
    public String toString() {
        return ":'" + getName() + "'";
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(name);
    }

    public static Symbol decode(IRReaderDecoder d) {
        return new Symbol(d.decodeByteList());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }
}
