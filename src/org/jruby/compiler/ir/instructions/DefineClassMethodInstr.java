package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyObject;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// SSS FIXME: Should we merge DefineInstanceMethod and DefineClassMethod instructions?
// identical except for 1 bit in interpret -- or will they diverge?
public class DefineClassMethodInstr extends OneOperandInstr {
    public final IRMethod method;

    public DefineClassMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_CLASS_METH, null, container);
        this.method = method;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getArg() + ", " + method.getName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineClassMethodInstr(getArg().cloneForInlining(ii), method);
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
        RubyObject obj = (RubyObject) getArg().retrieve(interp);
        obj.getMetaClass().addMethod(method.getName(), new InterpretedIRMethod(method, obj.getMetaClass()));
        return null;
    }
}
