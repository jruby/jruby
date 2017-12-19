package org.jruby.ir.operands;

import org.jcodings.Encoding;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

/**
 * A literal representing proc'ified symbols, as in &:foo.
 *
 * Used to cache a unique and constant proc at the use site to reduce allocation and improve caching.
 */
public class SymbolProc extends ImmutableLiteral {
    private final ByteList name;

    public SymbolProc(ByteList name) {
        super();

        this.name = name;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL_PROC;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newSymbolProc(context, name);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + name.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SymbolProc && name.equals(((SymbolProc) other).name);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.SymbolProc(this);
    }

    public String getName() {
        return name.toString();
    }

    public ByteList getByteName() {
        return name;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(name);
    }

    public static SymbolProc decode(IRReaderDecoder d) {
        return new SymbolProc(d.decodeByteList());
    }

    @Override
    public String toString() {
        return "SymbolProc:" + name;
    }
}
