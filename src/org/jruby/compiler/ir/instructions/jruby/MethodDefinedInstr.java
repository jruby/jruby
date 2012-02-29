/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.instructions.ResultInstr;
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
 * @author enebo
 */
public class MethodDefinedInstr extends Instr implements ResultInstr {
   private Variable result;
   private final Operand[] operands;
   
   public MethodDefinedInstr(Variable result, Operand receiver, StringLiteral methodName) {
        super(Operation.METHOD_DEFINED);
        
        this.result = result;
        this.operands = new Operand[] { receiver, methodName };
    }

    @Override
    public Operand[] getOperands() {
        return operands;
    }
    
    public Operand getReceiver() {
        return operands[0];
    }
    
    public StringLiteral getMethodName() {
        return (StringLiteral) operands[1];
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        result = v;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new MethodDefinedInstr((Variable) getResult().cloneForInlining(inlinerInfo), 
                getReceiver().cloneForInlining(inlinerInfo),
                (StringLiteral) getMethodName().cloneForInlining(inlinerInfo));
    }

    @Override
    public String toString() {
        return super.toString() + "(" + operands[0] + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        Object receiver = getReceiver().retrieve(context, self, currDynScope, temp);
        String methodName = getMethodName().string;
        ByteList boundVal = RuntimeHelpers.getDefinedCall(context, self, (IRubyObject)receiver, methodName);
        
        return boundVal == null ? context.nil : RubyString.newStringShared(runtime, boundVal);        
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    }    
}
