package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineInstanceMethodInstr extends OneOperandInstr {
    public final Operand container; // Can be either class of module
    public final IRMethod method;

    public DefineInstanceMethodInstr(Operand container, IRMethod method) {
		  // SSS FIXME: I have to explicitly record method.getContainer() as an operand because it can be an unresolved value and thus a Variable
		  // We dont want live variable analysis to forget about it!
        super(Operation.DEF_INST_METH, null, container);
        this.container = container;
        this.method = method;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + container + ", " + method.getName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
		  super.simplifyOperands(valueMap);
        Operand o = method.getContainer();
        Operand v = valueMap.get(o);
        // SSS FIXME: Dumb design leaking operand into IRScopeImpl -- hence this setting going on here.  Fix it!
        if (v != null)
            method.setContainer(v);
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        RubyModule clazz = (RubyModule) method.getContainer().retrieve(interp);
        clazz.addMethod(method.getName(), new InterpretedIRMethod(method, clazz));
        return null;
    }
}
