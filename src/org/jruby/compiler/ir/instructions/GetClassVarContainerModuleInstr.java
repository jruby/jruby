package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.evaluator.ASTInterpreter;

/*
 * Finds the module that will hold class vars for the object that is being queried.
 * A candidate static IRScope is also passed in.
 */
public class GetClassVarContainerModuleInstr extends Instr {
    private IRMethod candidateScope;
    private Operand  object;

    public GetClassVarContainerModuleInstr(Variable destination, IRMethod candidateScope, Operand object) {
        super(Operation.CLASS_VAR_MODULE, destination);
        this.candidateScope = candidateScope;
        this.object = object;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetClassVarContainerModuleInstr(ii.getRenamedVariable(getResult()), candidateScope, object == null ? null : object.cloneForInlining(ii));
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + candidateScope + ", " + object + ")";
    }

    public Operand[] getOperands() {
        return object == null ? new Operand[] {} : new Operand[] {object};
    }

    public void simplifyOperands(Map<Operand, Operand> valueMap) {
        if (object != null) object = object.getSimplifiedOperand(valueMap);
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        // SSS FIXME: This is ugly and needs fixing.  Is there another way of capturing this info?
        RubyModule containerModule = (candidateScope == null) ? null : candidateScope.getStaticScope().getModule();
        if (containerModule == null) containerModule = ASTInterpreter.getClassVariableBase(context, context.getRuntime());
        if (containerModule == null && object != null) {
            IRubyObject arg = (IRubyObject) object.retrieve(interp, context, self);
            // SSS: What is the right thing to do here?
            containerModule = arg.getMetaClass(); //(arg instanceof RubyClass) ? ((RubyClass)arg).getRealClass() : arg.getType();
        }

        if (containerModule == null) throw context.getRuntime().newTypeError("no class/module to define class variable");

        getResult().store(interp, context, self, containerModule);

        return null;
    }
}
