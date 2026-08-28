package org.jruby.ir.instructions;

import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YieldInstr extends TwoOperandResultBaseInstr implements FixedArityInstr, Site {
    public final boolean unwrapArray;
    private final int flags;
    private long callSiteId;

    public YieldInstr(IRScope scope, Variable result, Operand block, Operand arg, int flags, boolean unwrapArray) {
        super(Operation.YIELD, result, block, arg == null ? UndefinedValue.UNDEFINED : arg);

        assert result != null: "YieldInstr result is null";

        this.flags = flags;
        this.unwrapArray = unwrapArray;
        this.callSiteId = scope.getManager().nextCallSiteID();
    }

    public Operand getBlockArg() {
        return getOperand1();
    }

    public Operand getYieldArg() {
        return getOperand2();
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // FIXME: Is it necessary to clone a yield instruction in a method
        // that is being inlined, i.e. in METHOD_INLINE clone mode?
        // Fix BasicBlock.java:clone!!
        return new YieldInstr(ii.getScope(), ii.getRenamedVariable(result), getBlockArg().cloneForInlining(ii),
                getYieldArg().cloneForInlining(ii), flags, unwrapArray);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "flags: " + flags + "unwrap: " + unwrapArray};
    }

    public boolean isUnwrapArray() {
        return unwrapArray;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getBlockArg());
        e.encode(getYieldArg());
        e.encode(flags);
        e.encode(isUnwrapArray());
    }

    public static YieldInstr decode(IRReaderDecoder d) {
        return new YieldInstr(d.getCurrentScope(), d.decodeVariable(), d.decodeOperand(), d.decodeOperand(), d.decodeInt(), d.decodeBoolean());
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Block blk = (Block)getBlockArg().retrieve(context, self, currScope, currDynScope, temp);
        if (getYieldArg() == UndefinedValue.UNDEFINED) {
            IRRuntimeHelpers.setCallInfo(context, flags);
            return IRRuntimeHelpers.yieldSpecific(context, blk);
        } else {
            Operand yieldOp = getYieldArg();
            if (unwrapArray && yieldOp instanceof Array && ((Array)yieldOp).size() > 1) {
                // Special case this path!
                // Don't build a RubyArray.
                IRubyObject[] args = ((Array) yieldOp).retrieveArrayElts(context, self, currScope, currDynScope, temp);
                IRRuntimeHelpers.setCallInfo(context, flags);
                return IRRuntimeHelpers.yieldValues(context, blk, args);
            } else {
                IRubyObject yieldVal = (IRubyObject) yieldOp.retrieve(context, self, currScope, currDynScope, temp);
                IRRuntimeHelpers.setCallInfo(context, flags);
                return IRRuntimeHelpers.yield(context, blk, yieldVal, unwrapArray);
            }
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.YieldInstr(this);
    }

    @Override
    public long getCallSiteId() {
        return callSiteId;
    }

    @Override
    public void setCallSiteId(long callSiteId) {
        this.callSiteId = callSiteId;
    }
}
