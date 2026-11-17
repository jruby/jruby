package org.jruby.ir.operands;

import org.jruby.RubySymbol;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import java.util.List;

import static org.jruby.api.Create.dupString;

/**
 * Represents a literal string value.
 *
 * This is not an immutable literal because I can gsub!,
 * for example, and modify the contents of the string.
 * This is not like a Java string.
 */
public class MutableString extends Operand implements Stringable, StringLiteral {
    public static final MutableString EMPTY_STRING = new MutableString("");

    public final FrozenString frozenString;

    public MutableString(ByteList val, int coderange, String file, int line) {
        this.frozenString = new FrozenString(val, coderange, file, line);
    }

    public MutableString(String s) {
        this.frozenString = new FrozenString(s);
    }

    public MutableString(RubySymbol symbol) {
        frozenString = new FrozenString(symbol);
    }

    protected MutableString(FrozenString frozenString) {
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
        return other instanceof MutableString && frozenString.equals(((MutableString) other).frozenString);
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
        return dupString(context, frozenString.retrieve(context, self, currScope, currDynScope, temp));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.MutableString(this);
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

    public static MutableString decode(IRReaderDecoder d) {
        return new MutableString((FrozenString)d.decodeOperand());
    }

    public int getCodeRange() {
        return frozenString.getCodeRange();
    }
}
