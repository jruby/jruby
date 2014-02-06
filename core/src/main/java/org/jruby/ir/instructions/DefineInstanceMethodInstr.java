package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;
import org.jruby.ir.operands.ScopeModule;

public class DefineInstanceMethodInstr extends Instr implements FixedArityInstr {
    private Operand container;
    private final IRMethod method;

    public DefineInstanceMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_INST_METH);
        this.container = container;
        this.method = method;
    }

    public Operand getContainer() {
        return container;
    }


    @Override
    public Operand[] getOperands() {
        return new Operand[]{container, new ScopeModule(method) };
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return getOperation() + "(" + container + ", " + method.getName() + ", " + method.getFileName() + ")";
    }

    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineInstanceMethodInstr(container.cloneForInlining(ii), method);
    }

    // SSS FIXME: Go through this and DefineClassMethodInstr.interpret, clean up, extract common code
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;

        // SSS FIXME: This is a temporary solution that uses information from the stack.
        // This instruction and this logic will be re-implemented to not use implicit information from the stack.
        // Till such time, this code implements the correct semantics.
        RubyModule clazz = context.getRubyClass();
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
