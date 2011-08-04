package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyObject;

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

// SSS FIXME: Should we merge DefineInstanceMethod and DefineClassMethod instructions?
// identical except for 1 bit in interpret -- or will they diverge?
public class DefineInstanceMethodInstr extends OneOperandInstr {
    public final IRMethod method;

    public DefineInstanceMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_INST_METH, null, container);
        this.method = method;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getArg() + ", " + method.getName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineInstanceMethodInstr(getArg().cloneForInlining(ii), method);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        super.simplifyOperands(valueMap);
        Operand v = valueMap.get(getArg());
        // SSS FIXME: Dumb design leaking operand into IRScopeImpl -- hence this setting going on here.  Fix it!
        if (v != null)
            method.setContainer(v);
    }

    // SSS FIXME: Go through this and DefineClassmethodInstr.interpret, clean up, extract common code
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        RubyObject arg   = (RubyObject)getArg().retrieve(interp);
        RubyModule clazz = (arg instanceof RubyModule) ? (RubyModule)arg : arg.getMetaClass();
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

        if (visibility == Visibility.MODULE_FUNCTION) {
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
