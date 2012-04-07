/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 *
 * @author enebo
 */
public class AliasInstr extends Instr {
    final Variable receiver;
    private Operand newName;
    private Operand oldName;

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
    public String toString() {
        return getOperation().toString() + "(" + receiver + ", " + newName + ", " + oldName + ")";
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        oldName = oldName.getSimplifiedOperand(valueMap, force);
        newName = newName.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new AliasInstr((Variable) receiver.cloneForInlining(ii), newName.cloneForInlining(ii),
                oldName.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject object = (IRubyObject) receiver.retrieve(context, self, currDynScope, temp);
                
        if (object == null || object instanceof RubyFixnum || object instanceof RubySymbol) {
            throw context.getRuntime().newTypeError("no class to make alias");
        }

        String newNameString = newName.retrieve(context, self, currDynScope, temp).toString();
        String oldNameString = oldName.retrieve(context, self, currDynScope, temp).toString();

        RubyModule module = (object instanceof RubyModule) ? (RubyModule) object : object.getMetaClass();
        module.defineAlias(newNameString, oldNameString);
        
        return null;
    }

    @Override
    public void compile(JVM jvm) {
        jvm.method().loadLocal(0);
        jvm.method().loadLocal(jvm.methodData().local(receiver));
        jvm.method().adapter.ldc(((StringLiteral) newName).string);
        jvm.method().adapter.ldc(((StringLiteral) oldName).string);
        jvm.method().invokeHelper("defineAlias", IRubyObject.class, ThreadContext.class, IRubyObject.class, Object.class, Object.class);
        jvm.method().adapter.pop();
    }
    
}
