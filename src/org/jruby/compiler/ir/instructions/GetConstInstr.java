/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.compiler.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * A constant which we is not lexically scoped.  This instruction will ask
 * the source operand to get it directly.
 */
public class GetConstInstr extends GetInstr {
    public GetConstInstr(Variable dest, Operand scopeOrObj, String constName) {
        super(Operation.GET_CONST, dest, scopeOrObj, constName);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new GetConstInstr(ii.getRenamedVariable(getResult()), getSource().cloneForInlining(ii), getRef());
    }

    @Override
    public Label interpret(InterpreterContext interp, ThreadContext context, IRubyObject self) {
        Object source = getSource().retrieve(interp, context, self);
        RubyModule module;

        // Retrieving a MetaObject which is a closure returns a closure and not
        // the module which contains it.  We could possible add to operand to have a generic
        // scope() method or resort to if statements :)  So let's figure more out before
        // fixing this.
        if (source instanceof Block) {
            module = ((Block) source).getBinding().getKlass();
        } else if (source instanceof RubyModule) {
            module = (RubyModule) source;
        } else {
            throw context.getRuntime().newTypeError(source + " is not a type/class");
        }

        Object constant = module.getConstant(getRef());
        if (constant == null) constant = module.getConstantFromConstMissing(getRef());

        //if (container == null) throw runtime.newNameError("unitialized constant " + scope.getName(), scope.getName());
        getResult().store(interp, context, self, constant);
        return null;
    }
}
