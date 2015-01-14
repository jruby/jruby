package org.jruby.ir.instructions;

import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class UnresolvedSuperInstr extends CallInstr {
    public static final String UNKNOWN_SUPER_TARGET  = "-unknown-super-target-";

    // SSS FIXME: receiver is never used -- being passed in only to meet requirements of CallInstr
    public UnresolvedSuperInstr(Operation op, Variable result, Operand receiver, Operand[] args, Operand closure) {
        super(op, CallType.SUPER, result, UNKNOWN_SUPER_TARGET, receiver, args, closure);
    }

    public UnresolvedSuperInstr(Variable result, Operand receiver, Operand[] args, Operand closure) {
        this(Operation.UNRESOLVED_SUPER, result, receiver, args, closure);
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.REQUIRES_FRAME); // for current class and method name
        scope.getFlags().add(IRFlags.REQUIRES_DYNSCOPE); // for current class and method name
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new UnresolvedSuperInstr(ii.getRenamedVariable(getResult()), getReceiver().cloneForInlining(ii),
                cloneCallArgs(ii), getClosureArg() == null ? null : getClosureArg().cloneForInlining(ii));
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
        return IRRuntimeHelpers.unresolvedSuper(context, self, args, block);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UnresolvedSuperInstr(this);
    }
}
