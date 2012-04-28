/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.defined;

import java.util.Map;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.embed.variable.InstanceVariable;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.ir.targets.JVM;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.util.CodegenUtils;

/**
 *
 * @author enebo
 */
public class HasInstanceVarInstr extends DefinedObjectNameInstr {
    public HasInstanceVarInstr(Variable result, Operand object, StringLiteral name) {
        super(Operation.HAS_INSTANCE_VAR, result, new Operand[] { object, name });
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new HasInstanceVarInstr((Variable) getResult().cloneForInlining(inlinerInfo), 
                getObject().cloneForInlining(inlinerInfo),
                (StringLiteral) getName().cloneForInlining(inlinerInfo));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject receiver = (IRubyObject) getObject().retrieve(context, self, currDynScope, temp);
        
        return context.runtime.newBoolean(receiver.getInstanceVariables().hasInstanceVariable(getName().string));
    }

    @Override
    public void compile(JVM jvm) {
        // TODO: This is suboptimal, not caching ivar offset at all
        jvm.method().pushRuntime();
        jvm.emit(getObject());
        jvm.method().adapter.invokeinterface(CodegenUtils.p(IRubyObject.class), "getInstanceVariables", CodegenUtils.sig(InstanceVariables.class));
        jvm.method().adapter.ldc(getName().string);
        jvm.method().adapter.invokeinterface(CodegenUtils.p(InstanceVariables.class), "hasInstanceVariable", CodegenUtils.sig(boolean.class, String.class));
        jvm.method().adapter.invokevirtual(CodegenUtils.p(Ruby.class), "newBoolean", CodegenUtils.sig(RubyBoolean.class, boolean.class));
        jvm.method().storeLocal(jvm.methodData().local(getResult()));
    }
}
