package org.jruby.ir.instructions.ruby20;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.OptArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReceiveArgBase;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.Operation;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.RubyHash;

public class ReceiveKeywordRestArgInstr extends ReceiveArgBase {
    /** This instruction gets to pick an argument only if
     *  there are at least this many incoming arguments */
    public final int minArgsLength;

    public ReceiveKeywordRestArgInstr(Variable result, int minArgsLength) {
        super(Operation.RECV_KW_REST_ARG, result, -1);
        this.minArgsLength = minArgsLength;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + minArgsLength + ")";
    }

    public Object receiveKWArg(ThreadContext context, IRubyObject[] args) {
        IRubyObject lastArg = args[args.length - 1];
        if (lastArg instanceof RubyHash) {
            if (minArgsLength == args.length) {
                // SSS FIXME: Ruby 2 seems to suck the last ruby hash arg
                // for keyword args always and hence finds one less arg
                // available for required args.  Not sure if that makes sense.

                /* throw ArgumentError */
                Arity.raiseArgumentError(context.getRuntime(), args.length-1, minArgsLength, -1);
            }

            return lastArg;
        } else {
            return RubyHash.newSmallHash(context.getRuntime());
        }
    }
}
