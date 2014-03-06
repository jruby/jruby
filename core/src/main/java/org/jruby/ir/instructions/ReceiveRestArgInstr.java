package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/*
 * Assign rest arg passed into method to a result variable
 */
public class ReceiveRestArgInstr extends ReceiveArgBase implements FixedArityInstr {
    /** Number of arguments already accounted for */
    public final int numUsedArgs;

    public ReceiveRestArgInstr(Variable result, int numUsedArgs, int argIndex) {
        super(Operation.RECV_REST_ARG, result, argIndex);
        this.numUsedArgs = numUsedArgs;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + numUsedArgs + ", " + argIndex + ")";
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(numUsedArgs), new Fixnum(argIndex) };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new ReceiveRestArgInstr(ii.getRenamedVariable(result), numUsedArgs, argIndex);
            default:
                if (ii.canMapArgsStatically()) {
                    // FIXME: Check this
                    return new CopyInstr(ii.getRenamedVariable(result), ii.getArg(argIndex, true));
                } else {
                    return new RestArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), argIndex, (numUsedArgs - argIndex), argIndex);
                }
        }
    }

    private IRubyObject[] NO_PARAMS = new IRubyObject[0];

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args,  boolean keywordArgumentSupplied) {
        int argsLength = keywordArgumentSupplied ? args.length - 1 : args.length;
        int remainingArguments = argsLength - numUsedArgs;

        if (remainingArguments <= 0) return context.runtime.newArray(NO_PARAMS);

        IRubyObject[] restArgs = new IRubyObject[remainingArguments];
        System.arraycopy(args, argIndex, restArgs, 0, remainingArguments);

        return context.runtime.newArray(restArgs);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveRestArgInstr(this);
    }
}
