package org.jruby.ir.instructions;

import org.jruby.RubySymbol;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.internal.runtime.methods.ExitableReturn;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallType;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Error.runtimeError;

/**
 * Split-constructor boundary for Ruby initialize methods on Java subclasses.
 *
 * This instruction does not invoke Ruby super. It evaluates the original super arguments and hands them back to
 * generated Java constructor code so that code can select and invoke the real Java superclass constructor.
 */
public class JavaCtorSuperInstr extends CallInstr {
    public JavaCtorSuperInstr(IRScope scope, CallBase superCall) {
        this(scope, superCall.getResult(), superCall.getName(), superCall.getReceiver(), superCall.getCallArgs(),
                superCall.getClosureArg(), superCall.getFlags(), superCall.isPotentiallyRefined());
    }

    private JavaCtorSuperInstr(IRScope scope, Variable result, RubySymbol name, Operand receiver, Operand[] args,
            Operand closure, int flags, boolean potentiallyRefined) {
        super(scope, Operation.JAVA_CTOR_SUPER, CallType.SUPER, result, name, receiver, args, closure, flags,
                potentiallyRefined);
    }

    public ExitableReturn prepareSuper(ThreadContext context, IRubyObject self, StaticScope currScope,
            DynamicScope currDynScope, Object[] temp) {
        IRubyObject[] args = prepareArguments(context, self, currScope, currDynScope, temp);
        Block block = prepareBlock(context, self, currScope, currDynScope, temp);

        return new ExitableReturn(args, block);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self,
            Object[] temp) {
        throw runtimeError(context, "BUG: Java constructor super escaped split constructor execution");
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new JavaCtorSuperInstr(ii.getScope(), ii.getRenamedVariable(getResult()), getName(),
                getReceiver().cloneForInlining(ii), cloneCallArgs(ii), getClosureArg().cloneForInlining(ii), getFlags(),
                isPotentiallyRefined());
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.JavaCtorSuperInstr(this);
    }
}
