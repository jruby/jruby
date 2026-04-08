package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.specialized.OneArgOperandAttrAssignInstr;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Self;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

// Instruction representing Ruby code of the form: "a[i] = 5"
// which is equivalent to: a.[]=(i,5)
public class AttrAssignInstr extends NoResultCallInstr {
    public static AttrAssignInstr create(IRScope scope, Operand obj, RubySymbol attr, Operand[] args, Operand block, int flags, boolean isPotentiallyRefined) {
        if (block == NullBlock.INSTANCE && args.length == 1 && !containsArgSplat(args)) {
            return new OneArgOperandAttrAssignInstr(scope, obj, attr, args, flags, isPotentiallyRefined);
        }

        return new AttrAssignInstr(scope, obj, attr, args, block, flags, isPotentiallyRefined);
    }
    public static AttrAssignInstr create(IRScope scope, Operand obj, RubySymbol attr, Operand[] args, int flags, boolean isPotentiallyRefined) {
        if (!containsArgSplat(args) && args.length == 1) {
            return new OneArgOperandAttrAssignInstr(scope, obj, attr, args, flags, isPotentiallyRefined);
        }

        return new AttrAssignInstr(scope, obj, attr, args, NullBlock.INSTANCE, flags, isPotentiallyRefined);
    }

    // clone constructor
    protected AttrAssignInstr(IRScope scope, CallType callType, RubySymbol name, Operand receiver,
                              Operand[] args, int flags, boolean potentiallyRefined) {
        super(scope, Operation.ATTR_ASSIGN, callType, name, receiver, args, NullBlock.INSTANCE, flags, potentiallyRefined);
    }

    // normal constructor
    public AttrAssignInstr(IRScope scope, Operand obj, RubySymbol attr, Operand[] args, Operand block, int flags,
                           boolean isPotentiallyRefined) {
        super(scope, Operation.ATTR_ASSIGN, obj instanceof Self ? CallType.FUNCTIONAL : CallType.NORMAL, attr, obj,
                args, block, flags, isPotentiallyRefined);
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new AttrAssignInstr(ii.getScope(), getCallType(), getName(), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getFlags(), isPotentiallyRefined());
    }

    @Override
    public void encode(IRWriterEncoder e) {
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Instr(" + getOperation() + "): " + this);
        e.encode(getOperation());
        e.encode(getReceiver());
        e.encode(getName());
        e.encode(getCallArgs());
        e.encode(getFlags());
    }

    public static AttrAssignInstr decode(IRReaderDecoder d) {
        return create(d.getCurrentScope(), d.decodeOperand(), d.decodeSymbol(), d.decodeOperandArray(), d.decodeInt(), d.getCurrentScope().maybeUsingRefinements());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope dynamicScope, IRubyObject self, Object[] temp) {
        IRubyObject object = (IRubyObject) getReceiver().retrieve(context, self, currScope, dynamicScope, temp);
        IRubyObject[] values = prepareArguments(context, self, currScope, dynamicScope, temp);
        Block block = prepareBlock(context, self, currScope, dynamicScope, temp );

        callSite.call(context, self, object, values, block);

        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.AttrAssignInstr(this);
    }
}
