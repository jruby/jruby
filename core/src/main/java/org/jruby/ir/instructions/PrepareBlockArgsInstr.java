package org.jruby.ir.instructions;

import java.util.Arrays;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.RubyHash;
import org.jruby.RubyArray;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PrepareBlockArgsInstr extends NoOperandInstr implements FixedArityInstr {
    public PrepareBlockArgsInstr(Operation op) {
        super(op);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return ii instanceof SimpleCloneInfo ? new PrepareBlockArgsInstr(Operation.PREPARE_BLOCK_ARGS) : NopInstr.NOP;  // FIXME: Is this correct
    }

    public static PrepareBlockArgsInstr decode(IRReaderDecoder d) {
        return new PrepareBlockArgsInstr(Operation.PREPARE_BLOCK_ARGS);
    }
    
    @Override
    public void visit(IRVisitor visitor) {
        visitor.PrepareBlockArgsInstr(this);
    }

    protected IRubyObject[] toAry(ThreadContext context, IRubyObject[] args) {
        if (args.length == 1 && args[0].respondsTo("to_ary")) {
            IRubyObject newAry = Helpers.aryToAry(args[0]);
            if (newAry.isNil()) {
                args = new IRubyObject[] { args[0] };
            } else if (newAry instanceof RubyArray) {
                args = ((RubyArray) newAry).toJavaArray();
            } else {
                throw context.runtime.newTypeError(args[0].getType().getName() + "#to_ary should return Array");
            }
        }
        return args;
    }

    // SSS FIXME: This code only works for block yields, not rubyproc calls.
    // When a block is converted to a RubyProc and called, this code below
    // needs to implement the logic in BlockBody:prepareArgumentsForCall.
    public IRubyObject[] prepareBlockArgs(ThreadContext context, Block b, IRubyObject[] args) {
        // This is the placeholder for scenarios
        // not handled by specialized instructions.
        if (args == null) {
            return IRubyObject.NULL_ARRAY;
        }

        BlockBody body = b.getBody();
        Signature sig = body.getSignature();

        // blockArity == 0 and 1 have been handled in the specialized instructions
        // This test is when we only have opt / rest arg (either keyword or non-keyword)
        // but zero required args.
        int blockArity = sig.arityValue();
        if (blockArity == -1) {
            return args;
        }

        // We get here only when we have both required and optional/rest args
        // (keyword or non-keyword in either case).
        // So, convert a single value to an array if possible.
        args = toAry(context, args);

        // Deal with keyword args that needs special handling
        int needsKwargs = sig.hasKwargs() ? 1 - sig.getRequiredKeywordForArityCount() : 0;
        int required = sig.required();
        int actual = args.length;
        if (needsKwargs == 0 || required > actual) {
            // Nothing to do if we have fewer args in args than what is required
            // The required arg instructions will return nil in those cases.
            return args;
        }

        if (sig.isFixed() && required > 0 && required+needsKwargs != actual) {
            // Make sure we have a ruby-hash
            IRubyObject[] newArgs = Arrays.copyOf(args, required+needsKwargs);
            if (actual < required+needsKwargs) {
                // Not enough args and we need an empty {} for kwargs processing.
                newArgs[newArgs.length - 1] = RubyHash.newHash(context.runtime);
            } else {
                // We have more args than we need and kwargs is always the last arg.
                newArgs[newArgs.length - 1] = args[args.length - 1];
            }
            args = newArgs;
        }

        return args;
    }
}
