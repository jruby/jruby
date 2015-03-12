package org.jruby.ir.instructions;

import org.jruby.ir.IRVisitor;
import org.jruby.ir.Interp;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class YieldInstr extends ResultBaseInstr implements FixedArityInstr {
    public final boolean unwrapArray;

    public YieldInstr(Variable result, Operand block, Operand arg, boolean unwrapArray) {
        super(Operation.YIELD, result, new Operand[] { block, arg == null ? UndefinedValue.UNDEFINED : arg });

        assert result != null: "YieldInstr result is null";

        this.unwrapArray = unwrapArray;
    }

    public Operand getBlockArg() {
        return operands[0];
    }

    public Operand getYieldArg() {
        return operands[1];
    }

    @Override
    public Instr clone(CloneInfo ii) {
        // FIXME: Is it necessary to clone a yield instruction in a method
        // that is being inlined, i.e. in METHOD_INLINE clone mode?
        // Fix BasicBlock.java:clone!!
        return new YieldInstr(ii.getRenamedVariable(result), getBlockArg().cloneForInlining(ii),
                getYieldArg().cloneForInlining(ii), unwrapArray);
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "unwrap: " + unwrapArray};
    }

    public boolean isUnwrapArray() {
        return unwrapArray;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getBlockArg());
        e.encode(getYieldArg());
        e.encode(isUnwrapArray());
    }

    @Interp
    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object blk = getBlockArg().retrieve(context, self, currScope, currDynScope, temp);
        if (getYieldArg() == UndefinedValue.UNDEFINED) {
            return IRRuntimeHelpers.yieldSpecific(context, blk);
        } else {
            IRubyObject yieldVal = (IRubyObject) getYieldArg().retrieve(context, self, currScope, currDynScope, temp);
            return IRRuntimeHelpers.yield(context, blk, yieldVal, unwrapArray);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.YieldInstr(this);
    }
}
