package org.jruby.ir.instructions;

import org.jruby.RubyRegexp;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

// Represents a backref node in Ruby code
public class BuildNthRefInstr extends NoOperandResultBaseInstr {
    final public int group;

    public BuildNthRefInstr(Variable result, int group) {
        super(Operation.BUILD_NTHREF, result);
        this.group = group;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(group);
    }

    public static BuildNthRefInstr decode(IRReaderDecoder d) {
        return new BuildNthRefInstr(d.decodeVariable(), d.decodeInt());
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new BuildNthRefInstr(ii.getRenamedVariable(result), group);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"$" + "'" + group + "'"};
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        return IRRuntimeHelpers.nthMatch(context, group);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.BuildNthRefInstr(this);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        flags.add(IRFlags.REQUIRES_BACKREF);

        return true;
    }
}
