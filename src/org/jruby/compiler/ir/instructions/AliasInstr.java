/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions;

import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.interpreter.InterpreterContext;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author enebo
 */
public class AliasInstr extends Instr {
    final Variable receiver;
    private final Operand newName;
    private final Operand oldName;

    public AliasInstr(Variable receiver, Operand newName, Operand oldName) {
        super(Operation.ALIAS);
        
        this.receiver = receiver;
        this.newName = newName;
        this.oldName = oldName;
    }

    @Override
    public Operand[] getOperands() {
        return new Operand[] { receiver, newName, oldName };
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new AliasInstr((Variable) receiver.cloneForInlining(ii), newName.cloneForInlining(ii),
                oldName.cloneForInlining(ii));
    }

    @Override
    public Label interpret(InterpreterContext interp, IRExecutionScope scope, ThreadContext context, IRubyObject self) {
        IRubyObject object = (IRubyObject) receiver.retrieve(interp, context, self);
                
        if (object == null || object instanceof RubyFixnum || object instanceof RubySymbol) {
            throw context.getRuntime().newTypeError("no class to make alias");
        }

        String newNameString = newName.retrieve(interp, context, self).toString();
        String oldNameString = oldName.retrieve(interp, context, self).toString();

        RubyModule module = (object instanceof RubyModule) ? (RubyModule) object : object.getMetaClass();
        module.defineAlias(newNameString, oldNameString);
        
        return null;
    }
    
}
