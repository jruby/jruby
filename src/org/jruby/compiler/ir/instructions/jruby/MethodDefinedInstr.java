/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

/**
 *
 */
public class MethodDefinedInstr extends DefinedObjectNameInstr {
   public MethodDefinedInstr(Variable result, Operand object, StringLiteral methodName) {
        super(Operation.METHOD_DEFINED, result, new Operand[] { object, methodName });
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new MethodDefinedInstr((Variable) getResult().cloneForInlining(inlinerInfo), 
                getObject().cloneForInlining(inlinerInfo),
                (StringLiteral) getName().cloneForInlining(inlinerInfo));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        IRubyObject receiver = (IRubyObject) getObject().retrieve(context, self, currDynScope, temp);
        ByteList boundValue = RuntimeHelpers.getDefinedCall(context, self, receiver, getName().string);
        
        return boundValue == null ? context.nil : RubyString.newStringShared(runtime, boundValue);        
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    }    
}
