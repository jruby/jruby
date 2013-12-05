package org.jruby.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class DefineClassInstr extends Instr implements ResultInstr {
    private IRClassBody newIRClassBody;
    private Operand container;
    private Operand superClass;
    private Variable result;

    public DefineClassInstr(Variable result, IRClassBody newIRClassBody, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS);

        assert result != null: "DefineClassInstr result is null";

        this.container = container;
        this.superClass = superClass == null ? newIRClassBody.getManager().getNil() : superClass;
        this.newIRClassBody = newIRClassBody;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{container, superClass};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
        superClass = superClass.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + newIRClassBody.getName() + ", " + container + ", " + superClass + ", " + newIRClassBody.getFileName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: So, do we clone the class body scope or not?
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClassBody, container.cloneForInlining(ii), superClass.cloneForInlining(ii));
    }

    private RubyModule newClass(ThreadContext context, IRubyObject self, RubyModule classContainer, DynamicScope currDynScope, Object[] temp) {
        if (newIRClassBody instanceof IRMetaClassBody) return classContainer.getMetaClass();

        RubyClass sc;
        if (superClass == context.runtime.getIRManager().getNil()) {
            sc = null;
        } else {
            Object o = superClass.retrieve(context, self, currDynScope, temp);

            if (!(o instanceof RubyClass)) {
                throw context.runtime.newTypeError("superclass must be Class (" + o + " given)");
            }

            sc = (RubyClass) o;
        }

        return classContainer.defineOrGetClassUnder(newIRClassBody.getName(), sc);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object rubyContainer = container.retrieve(context, self, currDynScope, temp);

        if (!(rubyContainer instanceof RubyModule)) {
            throw context.runtime.newTypeError("no outer class/module");
        }

        RubyModule newRubyClass = newClass(context, self, (RubyModule) rubyContainer, currDynScope, temp);
        newIRClassBody.getStaticScope().setModule(newRubyClass);
        return new InterpretedIRMethod(newIRClassBody, Visibility.PUBLIC, newRubyClass);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassInstr(this);
    }

    public IRClassBody getNewIRClassBody() {
        return newIRClassBody;
    }

    public Operand getContainer() {
        return container;
    }

    public Operand getSuperClass() {
        return superClass;
    }
}
