package org.jruby.ir.operands;

import org.jruby.RubySymbol;
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
    private final RubySymbol name;

    public SymbolProc(RubySymbol name) {
        super();

        this.name = name;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.SYMBOL_PROC;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newSymbolProc(context, getName());
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

    public String getId() {
        return name.idString();
    }

    public RubySymbol getName() {
        return name;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(name);
    }

    public static SymbolProc decode(IRReaderDecoder d) {
        return new SymbolProc(d.decodeSymbol());
    }

    @Override
    public String toString() {
        return "SymbolProc:" + name;
    }
}
