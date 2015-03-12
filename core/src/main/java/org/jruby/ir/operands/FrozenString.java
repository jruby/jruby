package org.jruby.ir.operands;

import org.jruby.RubyString;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 * Represents a literal string value.
 *
 * This is not an immutable literal because I can gsub!,
 * for example, and modify the contents of the string.
 * This is not like a Java string.
 */
public class FrozenString extends StringLiteral {
    /**
     * Used by persistence and by .freeze optimization
     */
    public FrozenString(ByteList byteList, int cr) {
        super(OperandType.FROZEN_STRING, byteList, cr);
    }

    /**
     * IRBuild.buildGetDefinition returns a frozen string and this is for all intern'd Java strings.
     */
    public FrozenString(String s) {
        super(s);
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return context.runtime.freezeAndDedupString(RubyString.newString(context.runtime, bytelist));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FrozenString && bytelist.equals(((FrozenString) other).bytelist);
    }

    @Override
    public String toString() {
        return "frozen:\"" + bytelist + "\"";
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.FrozenString(this);
    }

    public static FrozenString decode(IRReaderDecoder d) {
       return new FrozenString(d.decodeByteList(), d.decodeInt());
    }
}
