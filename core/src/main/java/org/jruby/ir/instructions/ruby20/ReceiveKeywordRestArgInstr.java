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
    public final int numUsedArgs;

    public ReceiveKeywordRestArgInstr(Variable result, int numUsedArgs) {
        super(Operation.RECV_KW_REST_ARG, result, -1);
        this.numUsedArgs = numUsedArgs;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + numUsedArgs + ")";
    }

    public Object receiveKWArg(ThreadContext context, int kwArgHashCount, IRubyObject[] args) {
        if (kwArgHashCount == 0) {
            return RubyHash.newSmallHash(context.getRuntime());
        } else {
            if (numUsedArgs == args.length) {
                /* throw ArgumentError */
                Arity.raiseArgumentError(context.getRuntime(), args.length-1, numUsedArgs, -1);
            }

            return args[args.length - 1];
        }
    }
}
