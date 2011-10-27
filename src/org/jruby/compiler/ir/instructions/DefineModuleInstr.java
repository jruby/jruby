package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;

public class DefineModuleInstr extends OneOperandInstr {
    private final IRModule newIRModule;

    public DefineModuleInstr(IRModule newIRModule, Variable dest, Operand container) {
        super(Operation.DEF_MODULE, dest, container);
        
        this.newIRModule = newIRModule;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new DefineModuleInstr(this.newIRModule, ii.getRenamedVariable(getResult()), getArg().cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object container = getArg().retrieve(interp, context, self);
        
        if (!(container instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");

        RubyModule newRubyModule = ((RubyModule) container).defineOrGetModuleUnder(newIRModule.getName());
        newIRModule.getStaticScope().setModule(newRubyModule);
        DynamicMethod method = new InterpretedIRMethod(newIRModule.getRootMethod(), Visibility.PUBLIC, newRubyModule);

        // SSS FIXME: Rather than pass the block implicitly, should we add %block as another operand to DefineClass, DefineModule instrs?
        Object value = method.call(context, newRubyModule, newRubyModule, "", new IRubyObject[]{}, interp.getBlock());
        getResult().store(interp, context, self, value);
        return null;
    }
}
