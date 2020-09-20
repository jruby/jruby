package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class InstanceSuperInstr extends CallInstr {
    private final boolean isLiteralBlock;

    // clone constructor
    protected InstanceSuperInstr(IRScope scope, Variable result, Operand definingModule, RubySymbol name, Operand[] args,
                                 Operand closure, boolean isPotentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, Operation.INSTANCE_SUPER, CallType.SUPER, result, name, definingModule, args, closure,
                isPotentiallyRefined, callSite, callSiteId);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    // normal constructor
    public InstanceSuperInstr(IRScope scope, Variable result, Operand definingModule, RubySymbol name, Operand[] args,
                              Operand closure, boolean isPotentiallyRefined) {
        super(scope, Operation.INSTANCE_SUPER, CallType.SUPER, result, name, definingModule, args, closure, isPotentiallyRefined);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new InstanceSuperInstr(ii.getScope(), ii.getRenamedVariable(getResult()),
                getDefiningModule().cloneForInlining(ii), getName(), cloneCallArgs(ii),
                getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii),
                isPotentiallyRefined(), getCallSite(), getCallSiteId());
    }

    public static InstanceSuperInstr decode(IRReaderDecoder d) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super");
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, calltype(ord):  "+ callTypeOrdinal);
        RubySymbol name = d.decodeSymbol();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, methaddr:  "+ name);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;

        return new InstanceSuperInstr(d.getCurrentScope(), d.decodeVariable(), receiver, name, args, closure, d.getCurrentScope().maybeUsingRefinements());
    }

    /*
    // We cannot convert this into a NoCallResultInstr
    @Override
    public Instr discardResult() {
        return this;
    }
    */

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);
        RubyModule definingModule = ((RubyModule) getDefiningModule().retrieve(context, self, currScope, currDynScope, temp));

        if (isLiteralBlock) {
            return IRRuntimeHelpers.instanceSuperIter(context, self, getId(), definingModule, args, block);
        } else {
            return IRRuntimeHelpers.instanceSuper(context, self, getId(), definingModule, args, block);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InstanceSuperInstr(this);
    }
}
