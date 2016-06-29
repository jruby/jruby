package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;

/**
 * A literal representing proc'ified symbols, as in &:foo.
 *
 * Used to cache a unique and constant proc at the use site to reduce allocation and improve caching.
 */
public class SymbolProc extends ImmutableLiteral {
    private final String name;
    private final Encoding encoding;

    public SymbolProc(String name, Encoding encoding) {
        super();
        this.name = name;
        this.encoding = encoding;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL_PROC;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newSymbolProc(context, name, encoding);
    }

    @Override
    public int hashCode() {
        return 47 * 7 + (int) (this.name.hashCode() ^ (this.encoding.hashCode() >>> 32));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SymbolProc && name.equals(((SymbolProc) other).name) && encoding.equals(((SymbolProc) other).encoding);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SymbolProc(this);
    }

    public String getName() {
        return name;
    }

    public Encoding getEncoding() {
        return encoding;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(name);
        e.encode(encoding);
    }

    public static SymbolProc decode(IRReaderDecoder d) {
        return new SymbolProc(d.decodeString(), d.decodeEncoding());
    }

    @Override
    public String toString() {
        return "SymbolProc:" + name;
    }
}
