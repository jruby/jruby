package org.jruby.compiler.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubySymbol;
import org.jruby.compiler.ir.IRMetaClass;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineMetaClassInstr extends OneOperandInstr {
    IRMetaClass dummyMetaClass;
    public DefineMetaClassInstr(Operand object, IRMetaClass dummyMetaClass) {
        super(Operation.DEF_META_CLASS, null, object);
        this.dummyMetaClass = dummyMetaClass;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineMetaClassInstr(getArg().cloneForInlining(ii), dummyMetaClass);
    }

    @Override
    public Label interpret(InterpreterContext interp) {
        Ruby runtime = interp.getRuntime();
        IRubyObject obj = (IRubyObject)getArg().retrieve(interp);
        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("no virtual class for " + obj.getMetaClass().getBaseName());
        }
        else {
            if (runtime.getSafeLevel() >= 4 && !obj.isTaint()) {
                throw runtime.newSecurityError("Insecure: can't extend object.");
            }
            RubyClass singletonClass = obj.getSingletonClass();
            dummyMetaClass.getStaticScope().setModule(singletonClass);
            DynamicMethod method = new InterpretedIRMethod(dummyMetaClass.getRootMethod(), singletonClass);
            method.call(interp.getContext(), singletonClass, singletonClass, "", new IRubyObject[]{});
            return null;
        }
    }

    @Override
    public String toString() {
        return "" + operation + "(" + getArg() + ", " + dummyMetaClass + ")";
    }
}
