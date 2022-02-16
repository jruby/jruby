package org.jruby.ir.instructions;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
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
import org.jruby.util.ByteList;

import java.util.EnumSet;

// SSS FIXME: receiver is never used -- being passed in only to meet requirements of CallInstr

public class ModuleSuperInstr extends SuperInstr {
    private final boolean isLiteralBlock;

    // clone constructor
    public ModuleSuperInstr(IRScope scope, Operation op, Variable result, RubySymbol name, Operand receiver, Operand[] args,
                            Operand closure, boolean isPotentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, op, result, receiver, name, args, closure, isPotentiallyRefined, callSite, callSiteId);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    // normal constructor
    public ModuleSuperInstr(IRScope scope, Operation op, Variable result, RubySymbol name, Operand receiver, Operand[] args, Operand closure,
                            boolean isPotentiallyRefined) {
        super(scope, op, result, receiver, name, args, closure, isPotentiallyRefined);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    // specific instr constructor
    public ModuleSuperInstr(IRScope scope, Variable result, RubySymbol name, Operand receiver, Operand[] args, Operand closure,
                            boolean isPotentiallyRefined) {
        this(scope, Operation.MODULE_SUPER, result, name, receiver, args, closure, isPotentiallyRefined);
    }

    @Override
    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        super.computeScopeFlags(scope, flags);
        scope.setUsesSuper();
        flags.add(IRFlags.REQUIRES_CLASS); // for current class and method name
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ModuleSuperInstr(ii.getScope(), Operation.UNRESOLVED_SUPER, ii.getRenamedVariable(getResult()), name,
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii),
                getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii),
                isPotentiallyRefined(), getCallSite(), getCallSiteId());
    }

    public static ModuleSuperInstr decode(IRReaderDecoder d) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call");
        int callTypeOrdinal = d.decodeInt();
        CallType callType = CallType.fromOrdinal(callTypeOrdinal);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, calltype(ord):  " + callType);
        RubySymbol methAddr = d.decodeSymbol();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, methaddr:  " + methAddr);
        Operand receiver = d.decodeOperand();
        int argsCount = d.decodeInt();
        boolean hasClosureArg = argsCount < 0;
        int argsLength = hasClosureArg ? (-1 * (argsCount + 1)) : argsCount;
        if (RubyInstanceConfig.IR_READING_DEBUG)
            System.out.println("ARGS: " + argsLength + ", CLOSURE: " + hasClosureArg);
        Operand[] args = new Operand[argsLength];

        for (int i = 0; i < argsLength; i++) {
            args[i] = d.decodeOperand();
        }

        Operand closure = hasClosureArg ? d.decodeOperand() : null;
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("before result");

        return new ModuleSuperInstr(d.getCurrentScope(), d.decodeVariable(), d.decodeSymbol(), receiver, args, closure, d.getCurrentScope().maybeUsingRefinements());
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

        if (isLiteralBlock) {
            return IRRuntimeHelpers.moduleSuperIter(context, self, getId(), args, block);
        } else {
            return IRRuntimeHelpers.moduleSuper(context, self, getId(), args, block);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ModuleSuperInstr(this);
    }
}
