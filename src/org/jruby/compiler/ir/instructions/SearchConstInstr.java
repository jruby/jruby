package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

// NOTE: the scopeOrObj operand can be a dynamic scope.
//
// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.
//
public class SearchConstInstr extends GetInstr {
    public SearchConstInstr(Variable dest, IRScope scope, String constName) {
        super(Operation.SEARCH_CONST, dest, MetaObject.create(scope), constName);
    }

    public SearchConstInstr(Variable dest, Operand scopeOrObj, String constName) {
        super(Operation.SEARCH_CONST, dest, scopeOrObj, constName);
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        if (!(getSource() instanceof MetaObject)) return null;

		  // SSS FIXME: Isn't this always going to be an IR Module?
        IRScope s = ((MetaObject) getSource()).scope;
		  return (s instanceof IRModule) ? ((IRModule)s).getConstantValue(getName()) : null;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new SearchConstInstr(ii.getRenamedVariable(result), getSource().cloneForInlining(ii), getName());
    }

    @Override
    public Label interpret(InterpreterContext interp, IRubyObject self) {
        Object n = getSource();

        assert n instanceof MetaObject: "All sources should be a meta object";

        StaticScope staticScope = ((MetaObject) n).getScope().getStaticScope();

        RubyModule object = interp.getRuntime().getObject();
        getResult().store(interp, staticScope.getConstant(interp.getRuntime(), getName(), object));
        
        return null;
    }
}
