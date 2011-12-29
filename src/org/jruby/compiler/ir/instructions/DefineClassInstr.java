package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.compiler.ir.IRClassBody;
import org.jruby.compiler.ir.IRMetaClassBody;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;

public class DefineClassInstr extends Instr implements ResultInstr {
    private IRClassBody newIRClassBody;
    private Operand container;
    private Operand superClass;
    private Variable result;
    
    public DefineClassInstr(Variable result, IRClassBody newIRClassBody, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS);
        
        assert result != null: "DefineClassInstr result is null";
        
        this.container = container;
        this.superClass = superClass == null ? Nil.NIL : superClass;
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
        return super.toString() + "(" + container + ", " + superClass + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClassBody, container.cloneForInlining(ii), superClass.cloneForInlining(ii));
    }
    
    private RubyModule newClass(ThreadContext context, IRubyObject self, RubyModule classContainer, DynamicScope currDynScope, Object[] temp) {
        if (newIRClassBody instanceof IRMetaClassBody) return classContainer.getMetaClass();

        RubyClass sc;
        if (superClass == Nil.NIL) {
            sc = null;
        } else {
            Object o = superClass.retrieve(context, self, currDynScope, temp);

            if (!(o instanceof RubyClass)) throw context.getRuntime().newTypeError("superclass must be Class (" + o + " given)");
            
            sc = (RubyClass) o;
        }

        return classContainer.defineOrGetClassUnder(newIRClassBody.getName(), sc);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object rubyContainer = container.retrieve(context, self, currDynScope, temp);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");
        
        RubyModule newRubyClass = newClass(context, self, (RubyModule) rubyContainer, currDynScope, temp);

        // Interpret the body
        newIRClassBody.getStaticScope().setModule(newRubyClass);
        DynamicMethod method = new InterpretedIRMethod(newIRClassBody, Visibility.PUBLIC, newRubyClass);
        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineClass, DefineModule instrs?
        return method.call(context, newRubyClass, newRubyClass, "", new IRubyObject[]{}, block);
    }
}
