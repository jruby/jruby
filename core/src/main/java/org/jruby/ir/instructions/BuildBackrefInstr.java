package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.RubyRegexp;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a backref node in Ruby code
public class BuildBackrefInstr extends NoOperandResultBaseInstr {
    final public char type;

    public BuildBackrefInstr(Variable result, char t) {
        super(Operation.BUILD_BACKREF, result);
        type = t;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(type);
    }

    public static BuildBackrefInstr decode(IRReaderDecoder d) {
        return new BuildBackrefInstr(d.decodeVariable(), d.decodeChar());
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildBackrefInstr(ii.getRenamedVariable(result), type);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"$" + "'" + type + "'"};
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject backref = context.getBackRef();

        switch (type) {
        case '&' : return RubyRegexp.last_match(backref);
        case '`' : return RubyRegexp.match_pre(backref);
        case '\'': return RubyRegexp.match_post(backref);
        case '+' : return RubyRegexp.match_last(backref);
        default:
            assert false: "backref with invalid type";
            return null;
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildBackrefInstr(this);
    }
}
