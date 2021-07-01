package org.jruby.ir.instructions;

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
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public class ClassSuperInstr extends CallInstr {
    private final boolean isLiteralBlock;

    // clone constructor
    protected ClassSuperInstr(IRScope scope, Variable result, Operand receiver, RubySymbol name, Operand[] args,
                              Operand closure, boolean potentiallyRefined, CallSite callSite, long callSiteId) {
        super(scope, Operation.CLASS_SUPER, CallType.SUPER, result, name, receiver, args, closure, potentiallyRefined, callSite, callSiteId);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    // normal constructor
    public ClassSuperInstr(IRScope scope, Variable result, Operand definingModule, RubySymbol name, Operand[] args, Operand closure,
                           boolean isPotentiallyRefined) {
        super(scope, Operation.CLASS_SUPER, CallType.SUPER, result, name, definingModule, args, closure, isPotentiallyRefined);

        isLiteralBlock = closure instanceof WrappedIRClosure;
    }

    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        super.computeScopeFlags(scope, flags);
        scope.setUsesSuper();
        flags.add(IRFlags.REQUIRES_CLASS); // for current class and method name
        flags.add(IRFlags.REQUIRES_METHODNAME); // for current class and method name
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ClassSuperInstr(ii.getScope(), ii.getRenamedVariable(getResult()), getDefiningModule().cloneForInlining(ii),
                name, cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii),
                isPotentiallyRefined(), getCallSite(), getCallSiteId());
    }

    public static ClassSuperInstr decode(IRReaderDecoder d) {
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

        return new ClassSuperInstr(d.getCurrentScope(), d.decodeVariable(), receiver, name, args, closure, d.getCurrentScope().maybeUsingRefinements());
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
            return IRRuntimeHelpers.unresolvedSuperIter(context, self, args, block);
        } else {
            return IRRuntimeHelpers.unresolvedSuper(context, self, args, block);
        }
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClassSuperInstr(this);
    }
}
