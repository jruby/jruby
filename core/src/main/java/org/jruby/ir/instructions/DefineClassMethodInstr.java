package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

// SSS FIXME: Should we merge DefineInstanceMethod and DefineClassMethod instructions?
// identical except for 1 bit in interpret -- or will they diverge?
public class DefineClassMethodInstr extends Instr implements FixedArityInstr {
    private Operand container;
    private final IRMethod method;

    public DefineClassMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_CLASS_METH);
        this.container = container;
        this.method = method;
    }

    public Operand getContainer() {
        return container;
    }

    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[]{container};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return getOperation() + "(" + container + ", " + method.getName() + ", " + method.getFileName() + ")";
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineClassMethodInstr(container.cloneForInlining(ii), method);
    }

    // SSS FIXME: Go through this and DefineInstanceMethodInstr.interpret, clean up, extract common code
    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject obj = (IRubyObject) container.retrieve(context, self, currScope, currDynScope, temp);

        IRRuntimeHelpers.defInterpretedClassMethod(context, method, obj);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassMethodInstr(this);
    }
}
