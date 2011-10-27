package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyObject;
import org.jruby.RubyFixnum;
import org.jruby.RubyClass;
import org.jruby.RubySymbol;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

// SSS FIXME: Should we merge DefineInstanceMethod and DefineClassMethod instructions?
// identical except for 1 bit in interpret -- or will they diverge?
public class DefineClassMethodInstr extends OneOperandInstr {
    private final IRMethod method;

    public DefineClassMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_CLASS_METH, null, container);
        this.method = method;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + getArg() + ", " + method.getName() + ")";
    }
    
    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineClassMethodInstr(getArg().cloneForInlining(ii), method);
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        super.simplifyOperands(valueMap);
    }

    // SSS FIXME: Go through this and DefineInstanceMethodInstr.interpret, clean up, extract common code
    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        String name = method.getName();
        Ruby runtime = context.getRuntime();
        RubyObject obj = (RubyObject) getArg().retrieve(interp, context, self);

        if (runtime.getSafeLevel() >= 4 && !obj.isTaint()) {
            throw runtime.newSecurityError("Insecure; can't define singleton method.");
        }

        if (obj instanceof RubyFixnum || obj instanceof RubySymbol) {
            throw runtime.newTypeError("can't define singleton method \"" + name + "\" for " + obj.getMetaClass().getBaseName());
        }

        if (obj.isFrozen()) throw runtime.newFrozenError("object");

        RubyClass rubyClass = obj.getSingletonClass();
        if (runtime.getSafeLevel() >= 4 && rubyClass.getMethods().get(name) != null) {
            throw runtime.newSecurityError("redefining method prohibited.");
        }

        obj.getMetaClass().addMethod(name, new InterpretedIRMethod(method, Visibility.PUBLIC, obj.getMetaClass()));
        obj.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        return null;
    }
}
