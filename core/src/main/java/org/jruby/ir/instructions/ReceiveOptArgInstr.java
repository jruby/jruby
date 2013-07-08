package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveOptArgInstr extends ReceiveArgBase {
    /** Starting offset into the args array*/
    public final int argOffset;

    /** Number of arguments already accounted for */
    public final int numUsedArgs;

    public ReceiveOptArgInstr(Variable result, int numUsedArgs, int argOffset, int optArgIndex) {
        super(Operation.RECV_OPT_ARG, result, optArgIndex);
        this.argOffset = argOffset;
        this.numUsedArgs = numUsedArgs;
    }

    @Override
    public String toString() {
        return (isDead() ? "[DEAD]" : "") + (hasUnusedResult() ? "[DEAD-RESULT]" : "") + getResult() + " = " + getOperation() + "(" + numUsedArgs + "," + argOffset + "," + argIndex + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: Need to add kwArgLoss information in InlinerInfo

        // Added this copy for code clarity
        // argIndex is relative to start of opt args and not the start of arg array
        int optArgIndex = this.argIndex;
        int minReqdArgs = optArgIndex + numUsedArgs;

        if (ii.canMapArgsStatically()) {
            int n = ii.getArgsCount();
            return new CopyInstr(ii.getRenamedVariable(result), minReqdArgs < n ? ii.getArg(argOffset + optArgIndex) : UndefinedValue.UNDEFINED);
        } else {
            return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), argOffset + optArgIndex, minReqdArgs);
        }
    }

    @Override
    public Instr cloneForBlockCloning(InlinerInfo ii) {
        // Added this copy for code clarity
        // argIndex is relative to start of opt args and not the start of arg array
        int optArgIndex = this.argIndex;
        return new ReceiveOptArgInstr(ii.getRenamedVariable(result), numUsedArgs, argOffset, optArgIndex);
    }

    public Object receiveOptArg(IRubyObject[] args, int kwArgHashCount) {
        // Added this copy for code clarity
        // argIndex is relative to start of opt args and not the start of arg array
        int optArgIndex = this.argIndex;
        return (optArgIndex + numUsedArgs + kwArgHashCount < args.length ? args[argOffset + optArgIndex] : UndefinedValue.UNDEFINED);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveOptArgInstr(this);
    }
}
