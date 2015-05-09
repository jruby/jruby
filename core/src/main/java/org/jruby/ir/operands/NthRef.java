package org.jruby.ir.operands;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// Represents a $1 .. $9 node in Ruby code

// NOTE: This operand is only used in the initial stages of optimization
// Further down the line, it could get converted to calls
public class NthRef extends Reference {
    final public int matchNumber;

    public NthRef(int matchNumber) {
        super(OperandType.NTH_REF, "$" + matchNumber);
        this.matchNumber = matchNumber;
    }

    @Override
    public Object retrieve(ThreadContext context, IRubyObject self, StaticScope currScope, DynamicScope currDynScope, Object[] temp) {
        return IRRuntimeHelpers.nthMatch(context, matchNumber);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        // We do not super since name is derived from number.
        e.encode(getOperandType().getCoded());
        e.encode(matchNumber);
    }

    public static NthRef decode(IRReaderDecoder d) {
        return new NthRef(d.decodeInt());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.NthRef(this);
    }
}
