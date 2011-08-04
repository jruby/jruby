package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.operands.ClassMetaObject;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Nil;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineClassInstr extends TwoOperandInstr {
    public DefineClassInstr(Variable dest, ClassMetaObject cmo, Operand superClass) {
		  // Get rid of null scenario
        super(Operation.DEF_CLASS, dest, cmo, superClass == null ? Nil.NIL : superClass);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), (ClassMetaObject)getOperand1(), getOperand2().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Ruby runtime = context.getRuntime();
        ClassMetaObject cmo = (ClassMetaObject) getOperand1();
        IRScope scope = cmo.scope;
        RubyModule container = cmo.getContainer(interp, runtime);
        RubyModule module;
        if (scope instanceof IRMetaClass) {
            module = container.getMetaClass();
        } else {
            Operand superClass = getOperand2();
            RubyClass sc = superClass == Nil.NIL ? null : (RubyClass)superClass.retrieve(interp);
            module = container.defineOrGetClassUnder(scope.getName(), sc);
        }

        Object v = cmo.interpretBody(interp, context, module);
        getResult().store(interp, v);
        return null;
    }
}
