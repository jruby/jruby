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
        return operation + "(" + getArg() + ", " + method.getName() + ")";
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
        RubyObject arg   = (RubyObject)getArg().retrieve(interp, context, self);

        // SSS FIXME: argh! special case!
        //
        // At the top level of a script, there is a "main" object, and all methods defined in this scope get added to Object.
        // Specifically, consider the example below:
        //
        //    bar {
        //      def foo; .. end
        //    }
        //  
        // Here, foo should be added to Object.  
        //
        // In *all* other cases, 'foo' should be added to 'self' if it is a module, or to the metaclass of 'self' if it is not.
        // The code below implements this generic logic which is buggy for top-level methods defined within blocks.  So, the
        // current code adds 'foo' to "main".metaclass whereas it should be added to "Object", "main".class
        //
        // The fix would be to do this: replace 'arg.getMetaClass' with 'context.getRubyClass', but that feels a little ugly.
        // Is there a way out?
        //
        // RubyModule clazz = (arg instanceof RubyModule) ? (RubyModule)arg : context.getRubyClass();

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
        //System.out.println("Added " + name + " to " + clazz + "; self is " + self);

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
