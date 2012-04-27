package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;

public class PutConstInstr extends PutInstr {
    public PutConstInstr(Operand scopeOrObj, String constName, Operand val) {
        super(Operation.PUT_CONST, scopeOrObj, constName, val);
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        return new PutConstInstr(operands[TARGET].cloneForInlining(ii), ref, operands[VALUE].cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        IRubyObject value = (IRubyObject) getValue().retrieve(context, self, currDynScope, temp);
        RubyModule module = (RubyModule) getTarget().retrieve(context, self, currDynScope, temp);

        assert module != null : "MODULE should always be something";

        module.setConstant(getRef(), value);
        return null;
    }

    @Override
    public void compile(JVM jvm) {
        jvm.emit(getTarget());
        jvm.method().adapter.checkcast(CodegenUtils.p(RubyModule.class));
        jvm.method().adapter.ldc(getRef());
        jvm.emit(getValue());
        jvm.method().adapter.invokevirtual(CodegenUtils.p(RubyModule.class), "setConstant", CodegenUtils.sig(IRubyObject.class, String.class, IRubyObject.class));
        jvm.method().adapter.pop();
    }
}
