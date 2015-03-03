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

import java.nio.charset.UnsupportedCharsetException;
import java.util.List;

/**
 * Represents a literal string value.
 *
 * This is not an immutable literal because I can gsub!,
 * for example, and modify the contents of the string.
 * This is not like a Java string.
 */
public class FrozenString extends StringLiteral {
    public FrozenString(ByteList byteList) {
        super(byteList);
    }

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
}
