package org.jruby.ir.instructions;

import org.jruby.RubyHash;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveOptArgInstr extends ReceiveArgBase implements FixedArityInstr {
    /** opt args follow pre args **/
    public final int preArgs;

    /** total number of required args (pre + post) **/
    public final int requiredArgs;

    public ReceiveOptArgInstr(Variable result, int requiredArgs, int preArgs, int optArgIndex) {
        super(Operation.RECV_OPT_ARG, result, optArgIndex);
        this.preArgs = preArgs;
        this.requiredArgs = requiredArgs;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { new Fixnum(requiredArgs), new Fixnum(preArgs), new Fixnum(argIndex) };
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + requiredArgs + "," + preArgs + "," + argIndex + ")";
    }

    public int getPreArgs() {
        return preArgs;
    }

    public int getRequiredArgs() {
        return requiredArgs;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: Need to add kwArgLoss information in InlinerInfo
        // Added this copy for code clarity
        // argIndex is relative to start of opt args and not the start of arg array
        int optArgIndex = this.argIndex;
        switch (ii.getCloneMode()) {
            case ENSURE_BLOCK_CLONE:
            case NORMAL_CLONE:
                return new ReceiveOptArgInstr(ii.getRenamedVariable(result), requiredArgs, preArgs, optArgIndex);
            default: {
                int minReqdArgs = optArgIndex + requiredArgs;
                if (ii.canMapArgsStatically()) {
                    int n = ii.getArgsCount();
                    return new CopyInstr(ii.getRenamedVariable(result), minReqdArgs < n ? ii.getArg(preArgs + optArgIndex) : UndefinedValue.UNDEFINED);
                } else {
                    return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), preArgs + optArgIndex, minReqdArgs);
                }
            }
        }
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args, boolean acceptsKeywordArgument) {
        int optArgIndex = argIndex;  // which opt arg we are processing? (first one has index 0, second 1, ...).
        RubyHash keywordArguments = IRRuntimeHelpers.extractKwargsHash(args, requiredArgs, acceptsKeywordArgument);
        int argsLength = keywordArguments != null ? args.length - 1 : args.length;

        if (requiredArgs + optArgIndex >= argsLength) return UndefinedValue.UNDEFINED; // No more args left

        return args[preArgs + optArgIndex];
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveOptArgInstr(this);
    }
}
