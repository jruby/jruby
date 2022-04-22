package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.ir.transformations.inlining.InlineCloneInfo;
import org.jruby.ir.transformations.inlining.SimpleCloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class ReceiveOptArgInstr extends ReceiveArgBase implements FixedArityInstr {
    /** opt args follow pre args **/
    public final int preArgs;

    /** total number of required args (pre + post) **/
    public final int requiredArgs;

    public ReceiveOptArgInstr(Variable result, Variable keywords, int requiredArgs, int preArgs, int optArgIndex) {
        super(Operation.RECV_OPT_ARG, result, keywords, optArgIndex);
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
        if (info instanceof SimpleCloneInfo) return new ReceiveOptArgInstr(info.getRenamedVariable(result), info.getRenamedVariable(getKeywords()), requiredArgs, preArgs, optArgIndex);

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

    public static ReceiveOptArgInstr decode(IRReaderDecoder d) {
        return new ReceiveOptArgInstr(d.decodeVariable(),d.decodeVariable(), d.decodeInt(), d.decodeInt(), d.decodeInt());
    }

    @Override
    public IRubyObject receiveArg(ThreadContext context, IRubyObject self, DynamicScope currDynScope, StaticScope currScope,
                                  Object[] temp, IRubyObject[] args, boolean acceptsKeywords, boolean ruby2keyword) {
        IRubyObject keywords = (IRubyObject) getKeywords().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.receiveOptArg(args, keywords, requiredArgs, preArgs, argIndex);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ReceiveOptArgInstr(this);
    }
}
