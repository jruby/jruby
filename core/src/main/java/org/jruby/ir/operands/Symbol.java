package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class Symbol extends ImmutableLiteral {
    private final RubySymbol symbol;

    public Symbol(RubySymbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL;
    }

    public ByteList getBytes() {
        return symbol.getBytes();
    }

    public String getString() {
        return symbol.asJavaString();
    }

    @Override
    public boolean canCopyPropagate() {
        return true;
    }

    // Note: Not needed for interp since it never needs to be looked up.
    @Override
    public Object createCacheObject(ThreadContext context) {
        return symbol;
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
        return new Symbol(d.decodeSymbol());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Symbol(this);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return symbol;
    }
}
