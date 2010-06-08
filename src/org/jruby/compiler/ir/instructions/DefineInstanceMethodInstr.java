package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineInstanceMethodInstr extends NoOperandInstr {
    public final IRModule module; // Can be either class of module
    public final IRMethod method;

    public DefineInstanceMethodInstr(IRModule module, IRMethod method) {
        super(Operation.DEF_INST_METH);
        this.module = module;
        this.method = method;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + module.getName() + ", " + method.getName() + ")";
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        RubyModule clazz = self.getMetaClass();

        clazz.addMethod(method.name, new InterpretedIRMethod(method, clazz));
    }
}
