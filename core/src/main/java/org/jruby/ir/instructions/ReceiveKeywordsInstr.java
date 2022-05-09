package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Gets the keyword arg (or representation of what might be holding keyword args).
 * The result of this is passed into the other keyword arg instructions to get what
 * those instrs are interested in.  One intent of this result is that it can be
 * mutable so it can toggle off keyword arg flags early in the method before any
 * exceptions or side-effects in the method can occur.  It also can remove key/value pairs
 * as individual keyword instructions execute.  After all individual instructions have
 * executed anything left is either kwrest or an arity error.
 */
public class ReceiveKeywordsInstr extends NoOperandResultBaseInstr implements ArgReceiver {
    // The signature this instr lives in can accept rest arguments (def foo(*args)).
    final boolean hasRestArg;
    // The signature this instr lives in accepts some kind of explicit kwargs (def foo(a: 1, b:, **c)).
    final boolean acceptsKeywords;

    public ReceiveKeywordsInstr(Variable result, boolean hasRestArg, boolean acceptsKeywords) {
        super(Operation.RECV_KW, result);

        this.hasRestArg = hasRestArg;
        this.acceptsKeywords = acceptsKeywords;
    }

    public boolean hasRestArg() {
        return hasRestArg;
    }

    public boolean acceptsKeywords() {
        return acceptsKeywords;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ReceiveKeywordsInstr(ii.getRenamedVariable(result), hasRestArg, acceptsKeywords);
    }

    public static ReceiveKeywordsInstr decode(IRReaderDecoder d) {
        return new ReceiveKeywordsInstr(d.decodeVariable(), d.decodeBoolean(), d.decodeBoolean());
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope,
                                  Object[] temp, IRubyObject[] args, boolean ruby2keywords) {
        return IRRuntimeHelpers.receiveKeywords(context, args, hasRestArg, acceptsKeywords, ruby2keywords);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveKeywordsInstr(this);
    }
}
