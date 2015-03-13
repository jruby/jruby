package org.jruby.ir.instructions;

import org.jruby.ir.IRMethod;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

// SSS FIXME: Should we merge DefineInstanceMethod and DefineClassMethod instructions?
public class DefineClassMethodInstr extends Instr implements FixedArityInstr {
    private final IRMethod method;

    public DefineClassMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_CLASS_METH, new Operand[] { container });
        this.method = method;
    }

    public Operand getContainer() {
        return operands[0];
    }

    public IRMethod getMethod() {
        return method;
    }


    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + method.getName() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineClassMethodInstr(getContainer().cloneForInlining(ii), method);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getContainer());
        e.encode(getMethod());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject obj = (IRubyObject) getContainer().retrieve(context, self, currScope, currDynScope, temp);

        IRRuntimeHelpers.defInterpretedClassMethod(context, method, obj);
        return null;
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassMethodInstr(this);
    }
}
