package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.ModuleMetaObject;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineModuleInstr extends OneOperandInstr {
    public DefineModuleInstr(Variable dest, ModuleMetaObject m) {
        super(Operation.DEF_MODULE, dest, m);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineModuleInstr(ii.getRenamedVariable(result), (ModuleMetaObject)getArg());
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        ModuleMetaObject mmo = (ModuleMetaObject)getArg();
        IRScope scope = mmo.scope;
        RubyModule container = mmo.getContainer(interp, context, self);
        RubyModule module = container.defineOrGetModuleUnder(scope.getName());
        getResult().store(interp, context, self, mmo.interpretBody(interp, context, module));
        return null;
    }
}
