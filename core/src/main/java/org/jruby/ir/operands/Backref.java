package org.jruby.ir.operands;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a backref node in Ruby code
//
// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
public class Backref extends Reference {
    final public char type;

    public Backref(char t) {
        super(OperandType.BACKREF, "$" + t);
        type = t;
    }

    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        IRubyObject backref = context.getBackRef();

        switch (type) {
        case '&':
            return RubyRegexp.last_match(backref);
        case '`':
            return RubyRegexp.match_pre(backref);
        case '\'':
            return RubyRegexp.match_post(backref);
        case '+':
            return RubyRegexp.match_last(backref);
        default:
            assert false: "backref with invalid type";
            return null;
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.Backref(this);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        e.encode(getOperandType().getCoded());
        e.encode(type);
    }

    public static Backref decode(IRReaderDecoder d) {
        return new Backref(d.decodeChar());
    }

    @Override
    public String toString() {
        return "$" + "'" + type + "'";
    }
}
