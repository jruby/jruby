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

public class DefineModuleInstr extends OneOperandInstr {
    public DefineModuleInstr(Variable dest, ModuleMetaObject m) {
        super(Operation.DEF_MODULE, dest, m);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineModuleInstr(ii.getRenamedVariable(result), (ModuleMetaObject)getArg());
    }

    @Override
    public Label interpret(InterpreterContext interp) {
        Ruby       runtime   = interp.getRuntime();
        ModuleMetaObject mmo = (ModuleMetaObject)getArg();
        IRScope    scope     = mmo.scope;
        RubyModule container = mmo.getContainer(interp, runtime);
        RubyModule module    = container.defineOrGetModuleUnder(scope.getName());

		  // SSS FIXME: Hack/side-effect to get the meta-class instantiated for certain scenarios!
		  // We need to find out why 'foo.extend(module)' is not creating the meta class automatically.
		  // The correct fix is to find out who is implicitly calling getSingletonClass in the current
		  // AST interpreter, remove side effect of getSingletonClass and make the build of this metaclass
		  // explicit (probably in the extend methods?).
		  module.getSingletonClass();

        Object v = mmo.interpretBody(interp, interp.getContext(), module);
		  getResult().store(interp, v);
        return null;
    }
}
