package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.compiler.ir.IRClass;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;

public class DefineClassInstr extends Instr implements ResultInstr {
    private IRClass newIRClass;
    private Operand container;
    private Operand superClass;
    private Variable result;
    
    public DefineClassInstr(Variable result, IRClass newIRClass, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS);
        
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

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        container = container.getSimplifiedOperand(valueMap);
        superClass = superClass.getSimplifiedOperand(valueMap);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + container + ", " + superClass + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClass, container.cloneForInlining(ii), superClass.cloneForInlining(ii));
    }
    
    private RubyModule newClass(InterpreterContext interp, ThreadContext context, IRubyObject self, RubyModule classContainer) {
        if (newIRClass instanceof IRMetaClass) return classContainer.getMetaClass();

        RubyClass sc;
        if (superClass == Nil.NIL) {
            sc = null;
        } else {
            Object o = superClass.retrieve(interp, context, self);

            if (!(o instanceof RubyClass)) throw context.getRuntime().newTypeError("superclass must be Class (" + o + " given)");
            
            sc = (RubyClass) o;
        }

        return classContainer.defineOrGetClassUnder(newIRClass.getName(), sc);
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object rubyContainer = container.retrieve(interp, context, self);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");
        
        RubyModule newRubyClass = newClass(interp, context, self, (RubyModule) rubyContainer);

        // Interpret the body
        newIRClass.getStaticScope().setModule(newRubyClass);
        DynamicMethod method = new InterpretedIRMethod(newIRClass.getRootMethod(), Visibility.PUBLIC, newRubyClass);
        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineClass, DefineModule instrs?
        Object v = method.call(context, newRubyClass, newRubyClass, "", new IRubyObject[]{}, interp.getBlock());

        // Result from interpreting the body
        result.store(interp, context, self, v);
        return null;
    }
}
