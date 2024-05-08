package org.jruby.ir.operands;

import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

public class ChilledString extends FrozenString implements Stringable, StringLiteral {
    /**
     * Used by persistence and by .freeze optimization
     */
    public ChilledString(ByteList bytelist, int coderange, String file, int line) {
        super(bytelist, coderange, file, line);
    }

    public ChilledString(RubySymbol symbol) {
        super(symbol.getBytes());
    }

    /**
     * IRBuild.buildGetDefinition returns a frozen string and this is for all intern'd Java strings.
     */
    public ChilledString(String s) {
        this(ByteList.create(s));
    }

    private ChilledString(ByteList byteList) {
        super(byteList);
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.CHILLED_STRING;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ChilledString && bytelist.equals(((FrozenString) other).bytelist) && coderange == ((FrozenString) other).coderange;
    }

    @Override
    public String toString() {
        return "chilled:\"" + bytelist + "\"";
    }

    @Override
    public RubyString createCacheObject(ThreadContext context) {
        return IRRuntimeHelpers.newChilledString(context, bytelist, coderange, file, line);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ChilledString(this);
    }

    public static ChilledString decode(IRReaderDecoder d) {
        return new ChilledString(d.decodeByteList(), d.decodeInt(), d.decodeString(), d.decodeInt());
    }
}