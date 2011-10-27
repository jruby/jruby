package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.common.IRubyWarnings.ID;

import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;

import org.jruby.interpreter.InterpreterContext;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

public class DefineInstanceMethodInstr extends OneOperandInstr {
    private final IRMethod method;

    public DefineInstanceMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_INST_METH, null, container);
        this.method = method;
    }

    @Override
    public String toString() {
        return getOperation() + "(" + getArg() + ", " + method.getName() + ")";
    }
    
    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineInstanceMethodInstr(getArg().cloneForInlining(ii), method);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        super.simplifyOperands(valueMap);
    }

    // SSS FIXME: Go through this and DefineClassMethodInstr.interpret, clean up, extract common code
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // SSS FIXME: This is a temporary solution that uses information from the stack.
        // This instruction and this logic will be re-implemented to not use implicit information from the stack.
        // Till such time, this code implements the correct semantics.
        RubyModule clazz = context.getRubyClass();
        String     name  = method.getName();

        // Error checks and warnings on method definitions
        Ruby runtime = context.getRuntime();
        if (clazz == runtime.getDummy()) {
            throw runtime.newTypeError("no class/module to add method");
        }

        if (clazz == runtime.getObject() && name == "initialize") {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining Object#initialize may cause infinite loop");
        }

        if (name == "__id__" || name == "__send__") {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining `" + name + "' may cause serious problem"); 
        }

        Visibility visibility = context.getCurrentVisibility();
        if (name == "initialize" || name == "initialize_copy" || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }

        DynamicMethod newMethod = new InterpretedIRMethod(method, visibility, clazz);
        clazz.addMethod(name, newMethod);
        //System.out.println("Added " + name + " to " + clazz + "; self is " + self);

        if (context.getCurrentVisibility() == Visibility.MODULE_FUNCTION) {
            clazz.getSingletonClass().addMethod(name, new WrapperMethod(clazz.getSingletonClass(), newMethod, Visibility.PUBLIC));
            clazz.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        }
   
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (clazz.isSingleton()) {
            ((MetaClass) clazz).getAttached().callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        } else {
            clazz.callMethod(context, "method_added", runtime.fastNewSymbol(name));
        }
        return null;
    }
}
