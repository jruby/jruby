/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.instructions.jruby;

import java.util.Map;
import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.instructions.Instr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.StringLiteral;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.representations.InlinerInfo;
import org.jruby.compiler.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class ClassVarIsDefinedInstr extends DefinedInstr {
    public ClassVarIsDefinedInstr(Variable result, Operand module, StringLiteral name) {
        super(Operation.CLASS_VAR_IS_DEFINED, result, new Operand[] { module, name });
    }

    public StringLiteral getName() {
        return (StringLiteral) operands[1];
    }
    
    public Operand getModule() {
        return operands[0];
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new ClassVarIsDefinedInstr((Variable) getResult().cloneForInlining(inlinerInfo), 
                getModule().cloneForInlining(inlinerInfo),
                (StringLiteral) getName().cloneForInlining(inlinerInfo));
    }
    
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        result = (Variable) result.getSimplifiedOperand(valueMap, force);
        
        for (int i = 0; i < operands.length; i++) {
            operands[i] = operands[i].getSimplifiedOperand(valueMap, force);
        }
    }    

    @Override
    public String toString() {
        return super.toString() + "(" + getModule() + ", " + getName() + ")";
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        RubyModule cm = (RubyModule) getModule().retrieve(context, self, currDynScope, temp);
        String name = getName().string;        
        boolean defined = cm.isClassVarDefined(name);
        
        if (!defined && cm.isSingleton()) { // Not found look for cvar on singleton
            IRubyObject attached = ((MetaClass)cm).getAttached();
            if (attached instanceof RubyModule) defined = ((RubyModule)attached).isClassVarDefined(name);
        }
        
        return runtime.newBoolean(defined);        
    }

    @Override
    public void compile(JVM jvm) {
        // no-op right now
    } 
}
