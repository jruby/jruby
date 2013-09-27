package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

/*
 * Finds the module that will hold class vars for the object that is being queried.
 * A candidate static IRMethod is also passed in.
 */
// SSS FIXME: Split into 2 different instrs?
public class GetClassVarContainerModuleInstr extends Instr implements ResultInstr {
    private Operand  startingScope;
    private Operand  object;
    private Variable result;

    public GetClassVarContainerModuleInstr(Variable result, Operand startingScope, Operand object) {
        super(Operation.CLASS_VAR_MODULE);

        assert result != null;

        this.startingScope = startingScope;
        this.object = object;
        this.result = result;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetClassVarContainerModuleInstr(ii.getRenamedVariable(result), startingScope.cloneForInlining(ii), object == null ? null : object.cloneForInlining(ii));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + startingScope + ", " + object + ")";
    }

    public Operand[] getOperands() {
        return object == null ? new Operand[] {startingScope} : new Operand[] {startingScope, object};
    }

    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        startingScope = startingScope.getSimplifiedOperand(valueMap, force);
        if (object != null) object = object.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby        runtime   = context.runtime;
        StaticScope scope     = (StaticScope) startingScope.retrieve(context, self, currDynScope, temp);
        RubyModule  rubyClass = scope.getModule();

        // SSS FIXME: Copied from ASTInterpreter.getClassVariableBase and adapted
        while (scope != null && (rubyClass.isSingleton() || rubyClass == runtime.getDummy())) {
            scope = scope.getPreviousCRefScope();
            rubyClass = scope.getModule();
            if (scope.getPreviousCRefScope() == null) {
                runtime.getWarnings().warn(ID.CVAR_FROM_TOPLEVEL_SINGLETON_METHOD, "class variable access from toplevel singleton method");
            }
        }

        if ((scope == null) && (object != null)) {
            // We ran out of scopes to check -- look in arg's metaclass
            IRubyObject arg = (IRubyObject) object.retrieve(context, self, currDynScope, temp);
            rubyClass = arg.getMetaClass();
        }

        if (rubyClass == null) {
            throw context.runtime.newTypeError("no class/module to define class variable");
        }

        return rubyClass;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.GetClassVarContainerModuleInstr(this);
    }
}
