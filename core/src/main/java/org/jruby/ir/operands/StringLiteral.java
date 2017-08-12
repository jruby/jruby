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
public class StringLiteral extends Operand implements Stringable {
    public static final StringLiteral EMPTY_STRING = new StringLiteral("");

    public final FrozenString frozenString;

    public StringLiteral(ByteList val, int coderange, String file, int line) {
        this.frozenString = new FrozenString(val, coderange, file, line);
    }

    protected StringLiteral(String string, ByteList bytelist, int coderange, String file, int line) {
        super();

        this.frozenString = new FrozenString(string, bytelist, coderange, file, line);
    }

    public StringLiteral(String s) {
        this.frozenString = new FrozenString(s);
    }

    private StringLiteral(FrozenString frozenString) {
        this.frozenString = frozenString;
    }

    @Override
    public OperandType getOperandType() {
        return OperandType.STRING_LITERAL;
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
        return frozenString.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StringLiteral && frozenString.equals(((StringLiteral) other).frozenString);
    }

    @Override
    public String toString() {
        return "strdup(" + frozenString.toString() + ")";
    }

    @Override
    public Operand cloneForInlining(CloneInfo ii) {
        return this;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return frozenString.retrieve(context, self, currScope, currDynScope, temp).strDup(context.runtime);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.StringLiteral(this);
    }

    public ByteList getByteList() {
        return frozenString.getByteList();
    }

    public String getString() {
        return frozenString.getString();
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(frozenString);
    }

    public static StringLiteral decode(IRReaderDecoder d) {
        return new StringLiteral((FrozenString)d.decodeOperand());
    }

    public int getCodeRange() {
        return frozenString.getCodeRange();
    }
}
