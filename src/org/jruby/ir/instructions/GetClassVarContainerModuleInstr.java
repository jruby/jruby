package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMethod;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

/*
 * Finds the module that will hold class vars for the object that is being queried.
 * A candidate static IRMethod is also passed in.
 */
// SSS FIXME: Looks like object can be null -- fix IR to eliminate this
public class GetClassVarContainerModuleInstr extends Instr implements ResultInstr {
    private IRMethod candidateScope;
    private Operand object;
    private Variable result;

    public GetClassVarContainerModuleInstr(Variable result, IRMethod candidateScope, Operand object) {
        super(Operation.CLASS_VAR_MODULE);
        
        assert result != null;
        
        this.candidateScope = candidateScope;
        this.object = object;
        this.result = result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetClassVarContainerModuleInstr(ii.getRenamedVariable(result), candidateScope, object == null ? null : object.cloneForInlining(ii));
    }

    @Override
    public String toString() { 
        return super.toString() + "(" + candidateScope + ", " + object + ")";
    }

    public Operand[] getOperands() {
        return object == null ? new Operand[] {} : new Operand[] {object};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        if (object != null) object = object.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // SSS FIXME: This is ugly and needs fixing.  Is there another way of capturing this info?
        RubyModule containerModule = (candidateScope == null) ? null : candidateScope.getStaticScope().getModule();
        if (containerModule == null) containerModule = ASTInterpreter.getClassVariableBase(context, context.getRuntime());
        if (containerModule == null && object != null) {
            IRubyObject arg = (IRubyObject) object.retrieve(context, self, currDynScope, temp);
            // SSS: What is the right thing to do here?
            containerModule = arg.getMetaClass(); //(arg instanceof RubyClass) ? ((RubyClass)arg).getRealClass() : arg.getType();
        }

        if (containerModule == null) throw context.getRuntime().newTypeError("no class/module to define class variable");

        return containerModule;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetClassVarContainerModuleInstr(this);
    }
}
