package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRMetaClass;
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

public class DefineClassInstr extends Instr implements ResultInstr {
    private IRClass newIRClass;
    private Operand container;
    private Operand superClass;
    private Variable result;
    
    public DefineClassInstr(Variable result, IRClass newIRClass, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS);
        
        assert result != null: "DefineClassInstr result is null";
        
        this.container = container;
        this.superClass = superClass == null ? Nil.NIL : superClass;
        this.newIRClass = newIRClass;
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
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClass, container.cloneForInlining(ii), superClass.cloneForInlining(ii));
    }
    
    private RubyModule newClass(ThreadContext context, IRubyObject self, RubyModule classContainer, Object[] temp) {
        if (newIRClass instanceof IRMetaClass) return classContainer.getMetaClass();

        RubyClass sc;
        if (superClass == Nil.NIL) {
            sc = null;
        } else {
            Object o = superClass.retrieve(context, self, temp);

            if (!(o instanceof RubyClass)) throw context.getRuntime().newTypeError("superclass must be Class (" + o + " given)");
            
            sc = (RubyClass) o;
        }

        return classContainer.defineOrGetClassUnder(newIRClass.getName(), sc);
    }

    @Override
    public Object interpret(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block, Object exception, Object[] temp) {
        Object rubyContainer = container.retrieve(context, self, temp);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");
        
        RubyModule newRubyClass = newClass(context, self, (RubyModule) rubyContainer, temp);

        // Interpret the body
        newIRClass.getStaticScope().setModule(newRubyClass);
        DynamicMethod method = new InterpretedIRMethod(newIRClass.getRootMethod(), Visibility.PUBLIC, newRubyClass);
        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineClass, DefineModule instrs?
        Object v = method.call(context, newRubyClass, newRubyClass, "", new IRubyObject[]{}, block);

        // Result from interpreting the body
        result.store(context, self, temp, v);
        return null;
    }
}
