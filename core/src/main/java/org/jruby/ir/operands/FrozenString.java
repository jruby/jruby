package org.jruby.ir.operands;

import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;

import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

/**
 * Represents a frozen string value.
 */
public class FrozenString extends ImmutableLiteral {
    // SSS FIXME: Pick one of bytelist or string, or add internal conversion methods to convert to the default representation

    final public ByteList bytelist;
    final public String   string;
    final public int      coderange;

    /**
     * Used by persistence and by .freeze optimization
     */
    public FrozenString(ByteList byteList, int cr) {
        this(internedStringFromByteList(byteList), byteList, cr);
    }

    protected FrozenString(String string, ByteList bytelist, int coderange) {
        super();

        this.bytelist = bytelist;
        this.coderange = coderange;
        this.string = string;
    }

    /**
     * IRBuild.buildGetDefinition returns a frozen string and this is for all intern'd Java strings.
     */
    public FrozenString(String s) {
        this(s, ByteList.create(s));
    }

    private FrozenString(String string, ByteList byteList) {
        super();

        this.bytelist = byteList;
        this.string = string;
        this.coderange = StringSupport.CR_7BIT;
    }

    // If Encoding has an instance of a Charset can it ever raise unsupportedcharsetexception? because this
    // helper called copes with charset == null...
    private static String internedStringFromByteList(ByteList val) {
        try {
            return Helpers.byteListToString(val).intern();
        } catch (UnsupportedCharsetException e) {
            return val.toString().intern();
        }
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.FROZEN_STRING;
    }

    @Override
    public boolean hasKnownValue() {
        return true;
    }

    @Override
    public void addUsedVariables(List<Variable> l) {
        /* Do nothing */
    }

    @Override
    public int hashCode() {
        return bytelist.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FrozenString && bytelist.equals(((FrozenString) other).bytelist) && coderange == ((FrozenString) other).coderange;
    }

    @Override
    public String toString() {
        return "frozen:\"" + string + "\"";
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public Object createCacheObject(ThreadContext context) {
        return context.runtime.freezeAndDedupString(RubyString.newString(context.runtime, bytelist, coderange));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.FrozenString(this);
    }

    public ByteList getByteList() {
        return bytelist;
    }

    public String getString() {
        return string;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(bytelist);
        e.encode(coderange);
    }

    public static FrozenString decode(IRReaderDecoder d) {
        return new FrozenString(d.decodeByteList(), d.decodeInt());
    }

    public int getCodeRange() { return coderange; }
}
