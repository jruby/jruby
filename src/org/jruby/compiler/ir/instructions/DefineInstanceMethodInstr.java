package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;
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

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        MetaObject ma    = (MetaObject)getArg();
        RubyModule clazz = (RubyModule) ma.retrieve(interp);
		  String     name  = method.getName();

		  // System.out.println("Adding method: " + method.getName() + " to " + clazz.getName() + "; arg is " + getArg());
        Visibility visibility = interp.getContext().getCurrentVisibility();
        if (name == "initialize" || name == "initialize_copy" || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }
        clazz.addMethod(name, new InterpretedIRMethod(method, visibility, clazz));
        return null;
    }
}
