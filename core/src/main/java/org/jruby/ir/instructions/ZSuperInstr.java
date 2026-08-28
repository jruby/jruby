package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.NullBlock;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.EnumSet;

public class ZSuperInstr extends UnresolvedSuperInstr {
    // normal constructor
    public ZSuperInstr(IRScope scope, Variable result, Operand receiver, Operand[] args, Operand closure, int flags,
                       boolean isPotentiallyRefined) {
        super(scope, Operation.ZSUPER, result, receiver, args, closure, flags, isPotentiallyRefined);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        super.computeScopeFlags(scope, flags);
        scope.setUsesSuper();
        scope.setUsesZSuper();
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ZSuperInstr(ii.getScope(), ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg().cloneForInlining(ii), getFlags(),
                isPotentiallyRefined());
    }

    public static ZSuperInstr decode(IRReaderDecoder d) {
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

        Operand closure = hasClosureArg ? d.decodeOperand() : NullBlock.INSTANCE;
        int flags = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("before result");
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  " + result);

        return new ZSuperInstr(d.getCurrentScope(), result, receiver, args, closure, flags, d.getCurrentScope().maybeUsingRefinements());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);

        IRRuntimeHelpers.setCallInfo(context, getFlags());

        return IRRuntimeHelpers.zSuper(context, self, args, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ZSuperInstr(this);
    }
}
