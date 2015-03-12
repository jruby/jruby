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
 * Represents a literal string value.
 *
 * This is not an immutable literal because I can gsub!,
 * for example, and modify the contents of the string.
 * This is not like a Java string.
 */
public class StringLiteral extends Operand {
    public static final StringLiteral EMPTY_STRING = new StringLiteral("");

    // SSS FIXME: Pick one of bytelist or string, or add internal conversion methods to convert to the default representation

    final public ByteList bytelist;
    final public String   string;
    final public int      coderange;

    public StringLiteral(ByteList val, int coderange) {
        this(OperandType.STRING_LITERAL, val, coderange);
    }

    protected StringLiteral(OperandType type, ByteList val, int coderange) {
        this(type, internedStringFromByteList(val), val, coderange);

    }

    protected StringLiteral(OperandType type, String string, ByteList bytelist, int coderange) {
        super(type);

        this.bytelist = bytelist;
        this.coderange = coderange;
        this.string = string;
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

    public StringLiteral(String s) {
        this(s, ByteList.create(s));
    }

    private StringLiteral(String string, ByteList byteList) {
        super(OperandType.STRING_LITERAL);

        this.bytelist = byteList;
        this.string = string;
        this.coderange = StringSupport.CR_7BIT;
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
        return other instanceof StringLiteral && bytelist.equals(((StringLiteral) other).bytelist);
    }

    @Override
    public String toString() {
        return "\"" + string + "\"";
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        // SSS FIXME: AST interpreter passes in a coderange argument.
        return RubyString.newStringShared(context.runtime, bytelist);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.StringLiteral(this);
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

    public static StringLiteral decode(IRReaderDecoder d) {
        return new StringLiteral(d.decodeByteList(), d.decodeInt());
    }

    public int getCodeRange() { return coderange; }
}
