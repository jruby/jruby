package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
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

public class ZSuperInstr extends UnresolvedSuperInstr {
    public ZSuperInstr(Variable result, Operand receiver, Operand[] args, Operand closure) {
        super(Operation.ZSUPER, result, receiver, args, closure);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.USES_ZSUPER);
        scope.getFlags().add(IRFlags.CAN_CAPTURE_CALLERS_BINDING);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new ZSuperInstr(ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii));
    }

    public static ZSuperInstr decode(IRReaderDecoder d) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call");
        int callTypeOrdinal = d.decodeInt();
        CallType callType = CallType.fromOrdinal(callTypeOrdinal);
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, calltype(ord):  " + callType);
        String methAddr = d.decodeString();
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
        Variable result = d.decodeVariable();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding call, result:  " + result);

        return new ZSuperInstr(result, receiver, args, closure);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);
        if (block == null || !block.isGiven()) block = context.getFrameBlock();
        return IRRuntimeHelpers.unresolvedSuper(context, self, args, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ZSuperInstr(this);
    }
}
