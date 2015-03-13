package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
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
    public String[] toStringNonOperandArgs() {
        return new String[] {"index:" + getArgIndex(), "req: " + requiredArgs, "pre: " + preArgs};
    }

    public int getPreArgs() {
        return preArgs;
    }

    public int getRequiredArgs() {
        return requiredArgs;
    }

    @Override
    public Instr clone(CloneInfo info) {
        int optArgIndex = this.argIndex;
        if (info instanceof SimpleCloneInfo) return new ReceiveOptArgInstr(info.getRenamedVariable(result), requiredArgs, preArgs, optArgIndex);

        InlineCloneInfo ii = (InlineCloneInfo) info;

        // SSS FIXME: Need to add kwArgLoss information in InlinerInfo
        // Added this copy for code clarity
        // argIndex is relative to start of opt args and not the start of arg array
        int minReqdArgs = optArgIndex + requiredArgs;

        if (ii.canMapArgsStatically()) {
            int n = ii.getArgsCount();
            return new CopyInstr(ii.getRenamedVariable(result), minReqdArgs < n ? ii.getArg(preArgs + optArgIndex) : UndefinedValue.UNDEFINED);
        }

        return new OptArgMultipleAsgnInstr(ii.getRenamedVariable(result), ii.getArgs(), preArgs + optArgIndex, minReqdArgs);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(requiredArgs);
        e.encode(getPreArgs());
        e.encode(getArgIndex());
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject[] args, boolean acceptsKeywordArgument) {
        return IRRuntimeHelpers.receiveOptArg(args, requiredArgs, preArgs, argIndex, acceptsKeywordArgument);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveOptArgInstr(this);
    }
}
