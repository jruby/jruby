package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

public class Symbol extends ImmutableLiteral implements Stringable {
    public static final Symbol KW_REST_ARG_DUMMY = new Symbol(null);

    private final RubySymbol symbol;

    public Symbol(RubySymbol symbol) {
        this.symbol = symbol;
    }

    public boolean equals(Object other) {
        return other instanceof Symbol &&
                (this == KW_REST_ARG_DUMMY && other == KW_REST_ARG_DUMMY || symbol.equals(((Symbol) other).symbol));
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL;
    }

    public ByteList getBytes() {
        return symbol.getBytes();
    }

    public RubySymbol getSymbol() {
        return symbol;
    }

    public String getString() { return symbol.idString(); }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return symbol;
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    public Encoding getEncoding() {
        return symbol.getEncoding();
    }

    @Override
    public String toString() {
        return ":'" + getString() + "'";
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);

        e.encode(symbol);
    }

    public static Symbol decode(IRReaderDecoder d) {
        RubySymbol symbol = d.decodeSymbol();

        if (symbol == null) return KW_REST_ARG_DUMMY;

        return new Symbol(symbol);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }

    @Override
    public boolean isTruthyImmediate() {
        return true;
    }
}
