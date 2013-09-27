package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

// SSS FIXME: Should we merge DefineInstanceMethod and DefineClassMethod instructions?
// identical except for 1 bit in interpret -- or will they diverge?
public class DefineClassMethodInstr extends Instr {
    private Operand container;
    private final IRMethod method;

    public DefineClassMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_CLASS_METH);
        this.container = container;
        this.method = method;
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
        // SSS FIXME: So, what happens to the method?
        return new DefineClassMethodInstr(container.cloneForInlining(ii), method);
    }

    // SSS FIXME: Go through this and DefineInstanceMethodInstr.interpret, clean up, extract common code
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        String name = method.getName();
        Ruby runtime = context.runtime;
        RubyObject obj = (RubyObject) container.retrieve(context, self, currDynScope, temp);

        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + name + "\" for " + obj.getMetaClass().getBaseName());
        }

        if (obj.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass rubyClass = obj.getSingletonClass();

        rubyClass.addMethod(name, new InterpretedIRMethod(method, Visibility.PUBLIC, rubyClass));
        obj.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        return null;
    }

    public Operand[] getOperands() {
        return new Operand[]{container};
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassMethodInstr(this);
    }
}
