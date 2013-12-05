/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.instructions.defined;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class ClassVarIsDefinedInstr extends DefinedObjectNameInstr {
    public ClassVarIsDefinedInstr(Variable result, Operand module, StringLiteral name) {
        super(Operation.CLASS_VAR_IS_DEFINED, result, new Operand[] { module, name });
    }

    @Override
    public Instr cloneForInlining(InlinerInfo inlinerInfo) {
        return new ClassVarIsDefinedInstr((Variable) getResult().cloneForInlining(inlinerInfo),
                getObject().cloneForInlining(inlinerInfo),
                (StringLiteral) getName().cloneForInlining(inlinerInfo));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.runtime;
        RubyModule cm = (RubyModule) getObject().retrieve(context, self, currDynScope, temp);
        String name = getName().string;
        boolean defined = cm.isClassVarDefined(name);

        if (!defined && cm.isSingleton()) { // Not found look for cvar on singleton
            IRubyObject attached = ((MetaClass)cm).getAttached();
            if (attached instanceof RubyModule) defined = ((RubyModule)attached).isClassVarDefined(name);
        }

        return runtime.newBoolean(defined);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.ClassVarIsDefinedInstr(this);
    }
}
