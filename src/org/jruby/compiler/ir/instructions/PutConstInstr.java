package org.jruby.compiler.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.compiler.ir.IRScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.builtin.IRubyObject;

public class PutConstInstr extends PutInstr {
    public PutConstInstr(IRScope scope, String constName, Operand val) {
        super(Operation.PUT_CONST, new MetaObject(scope), constName, val);
    }

    public PutConstInstr(Operand scopeOrObj, String constName, Operand val) {
        super(Operation.PUT_CONST, scopeOrObj, constName, val);
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutConstInstr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public void interpret(InterpreterContext interp, IRubyObject self) {
        RubyModule module = ((RubyModule) getTarget().retrieve(interp));
        System.out.println("V: " + getValue().getClass());
        IRubyObject value = (IRubyObject) getValue().retrieve(interp);
        
        System.out.println("MODULE IS " + module);
        System.out.println("CNAME: " + getName() + ", VALUE: " + value);

        module.setConstant(getName(), value);
    }
}
