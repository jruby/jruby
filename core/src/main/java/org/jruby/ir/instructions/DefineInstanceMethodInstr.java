package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.*;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineInstanceMethodInstr extends Instr implements FixedArityInstr {
    private final IRMethod method;

    // SSS FIXME: Implicit self arg -- make explicit to not get screwed by inlining!
    public DefineInstanceMethodInstr(IRMethod method) {
        super(Operation.DEF_INST_METH);
        this.method = method;
    }

    @Override
    public Operand[] getOperands() {
        return EMPTY_OPERANDS;
    }

    @Override
    public String toString() {
        return getOperation() + "(" + method.getName() + ", " + method.getFileName() + ")";
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.REQUIRES_DYNSCOPE);
        scope.getFlags().add(IRFlags.REQUIRES_VISIBILITY);
        return true;
    }

    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineInstanceMethodInstr(method);
    }

    // SSS FIXME: Go through this and DefineClassMethodInstr.interpret, clean up, extract common code
    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Ruby runtime = context.runtime;
        RubyModule clazz = IRRuntimeHelpers.findInstanceMethodContainer(context, currDynScope, self);

        String     name  = method.getName();
        Visibility currVisibility = context.getCurrentVisibility();
        Visibility newVisibility = Helpers.performNormalMethodChecksAndDetermineVisibility(runtime, clazz, name, currVisibility);

        DynamicMethod newMethod = new InterpretedIRMethod(method, newVisibility, clazz);

        Helpers.addInstanceMethod(clazz, name, newMethod, currVisibility, context, runtime);

        return null; // unused; symbol is propagated
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineInstanceMethodInstr(this);
    }
}
