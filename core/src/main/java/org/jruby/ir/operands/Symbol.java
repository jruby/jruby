package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;
import org.jruby.util.io.EncodingUtils;

public class Symbol extends ImmutableLiteral {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol("", ASCIIEncoding.INSTANCE);

    private final ByteList bytes;

    public Symbol(String name, Encoding encoding) {
        super();

        this.bytes = new ByteList(name.getBytes(EncodingUtils.charsetForEncoding(encoding)), encoding);
    }

    public Symbol(ByteList bytes) {
        this.bytes = bytes;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Symbol)) return false;

        return bytes.equals(((Symbol) other).bytes);
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL;
    }

    public ByteList getBytes() {
        return bytes;
    }

    public String getString() { return RubyString.byteListToString(bytes); }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return RubySymbol.newSymbol(context.runtime, bytes);
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public Encoding getEncoding() {
        return bytes.getEncoding();
    }

    @Override
    public String toString() {
        return ":'" + getString() + "'";
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getString());
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
