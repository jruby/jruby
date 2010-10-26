package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

// NOTE: the scopeOrObj operand can be a dynamic scope.
//
// The runtime method call that GET_CONST is translated to in this case will call
// a get_constant method on the scope meta-object which does the lookup of the constant table
// on the meta-object.  In the case of method & closures, the runtime method will delegate
// this call to the parent scope.
//
public class GetConstInstr extends GetInstr {
    public GetConstInstr(Variable dest, IRScope scope, String constName) {
        super(Operation.GET_CONST, dest, MetaObject.create(scope), constName);
    }

    public GetConstInstr(Variable dest, Operand scopeOrObj, String constName) {
        super(Operation.GET_CONST, dest, scopeOrObj, constName);
    }

    @Override
    public Operand simplifyAndGetResult(Map<Operand, Operand> valueMap) {
        simplifyOperands(valueMap);
        if (!(getSource() instanceof MetaObject)) return null;

        IRScope s = ((MetaObject) getSource()).scope;
        return s.getConstantValue(getName());
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetConstInstr(ii.getRenamedVariable(result), getSource().cloneForInlining(ii), getName());
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        getResult().store(interp, ((RubyModule) getSource().retrieve(interp)).getConstant(getName()));
    }

}
