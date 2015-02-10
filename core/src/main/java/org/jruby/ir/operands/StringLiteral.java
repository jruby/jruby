package org.jruby.ir.operands;

import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
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

    public StringLiteral(ByteList val) {
        this(val, StringSupport.CR_7BIT);
    }

    public StringLiteral(ByteList val, int coderange) {
        super(OperandType.STRING_LITERAL);

        bytelist = val;
        this.coderange = coderange;
        String stringTemp;
        try {
            stringTemp = Helpers.byteListToString(bytelist);
        } catch (UnsupportedCharsetException e) {
            stringTemp = bytelist.toString();
        }
        string = stringTemp;
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
        return RubyString.newStringShared(context.runtime, bytelist, coderange);
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

    public int getCodeRange() { return coderange; }
}
