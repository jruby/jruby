package org.jruby.ir.instructions;

import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
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

public class InstanceSuperInstr extends CallInstr {
    public InstanceSuperInstr(Variable result, Operand definingModule, String name, Operand[] args, Operand closure) {
        super(Operation.INSTANCE_SUPER, CallType.SUPER, result, name, definingModule, args, closure);
    }

    public Operand getDefiningModule() {
        return getReceiver();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new InstanceSuperInstr(ii.getRenamedVariable(getResult()), getDefiningModule().cloneForInlining(ii), getName(),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii));
    }

    public static InstanceSuperInstr decode(IRReaderDecoder d) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super");
        int callTypeOrdinal = d.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, calltype(ord):  "+ callTypeOrdinal);
        String methAddr = d.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("decoding super, methaddr:  "+ methAddr);
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

        return new InstanceSuperInstr(d.decodeVariable(), receiver, methAddr, args, closure);
    }

    // We cannot convert this into a NoCallResultInstr
    @Override
    public Instr discardResult() {
        return this;
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);
        RubyModule definingModule = ((RubyModule) getDefiningModule().retrieve(context, self, currScope, currDynScope, temp)).getMethodLocation();

        return IRRuntimeHelpers.instanceSuper(context, self, getName(), definingModule, args, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.InstanceSuperInstr(this);
    }
}
