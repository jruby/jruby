package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.RubyClass;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.ClassMetaObject;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineClassInstr extends TwoOperandInstr {
    public DefineClassInstr(ClassMetaObject cmo, Operand superClass) {
        super(Operation.DEF_CLASS, null, cmo, superClass);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineClassInstr((ClassMetaObject)getOperand1(), getOperand2().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Ruby       runtime   = interp.getRuntime();
        ClassMetaObject cmo  = (ClassMetaObject)getOperand1();
        IRScope    scope     = cmo.scope;
        RubyModule container = cmo.getContainer(interp, runtime);
        RubyModule module;
        if (scope instanceof IRMetaClass) {
            module = container.getMetaClass();
        } else {
            RubyClass sc = (RubyClass)getOperand2().retrieve(interp);
            module = container.defineOrGetClassUnder(scope.getName(), sc == null ? runtime.getObject() : sc);
        }

        cmo.interpretBody(interp, interp.getContext(), module);
        return null;
    }

    @Override
    public String toString() {
        return "\t" + operation + "(" + getOperand1() + ", " + getOperand2() + ")";
    }
}
