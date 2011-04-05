package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.ModuleMetaObject;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineModuleInstr extends OneOperandInstr {
    public DefineModuleInstr(ModuleMetaObject m) {
        super(Operation.DEF_MODULE, null, m);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return this;
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Ruby       runtime   = interp.getRuntime();
        ModuleMetaObject mmo = (ModuleMetaObject)getArg();
        IRScope    scope     = mmo.scope;
        RubyModule container = mmo.getContainer(interp, runtime);
        RubyModule module    = container.defineOrGetModuleUnder(scope.getName());

        mmo.interpretBody(interp, interp.getContext(), module);
        return null;
    }
 
    @Override
    public String toString() {
        return "\t" + operation + "(" + getArg() + ")";
    }
}
