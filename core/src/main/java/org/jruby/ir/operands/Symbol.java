package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

public class Symbol extends ImmutableLiteral {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol(new ByteList());

    private final ByteList bytes;

    public Symbol(ByteList bytes) {
        this.bytes = bytes;
    }

    public boolean equals(Object other) {
        return other instanceof Symbol && bytes.equals(((Symbol) other).bytes);
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

        e.encode(getBytes());
    }

    public static Symbol decode(IRReaderDecoder d) {
        return new Symbol(d.decodeByteList());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }
}
