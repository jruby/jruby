package org.jruby.ir.instructions;

import org.jruby.ir.*;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineInstanceMethodInstr extends Instr implements FixedArityInstr {
    private final IRMethod method;

    // SSS FIXME: Implicit self arg -- make explicit to not get screwed by inlining!
    public DefineInstanceMethodInstr(IRMethod method) {
        super(Operation.DEF_INST_METH, EMPTY_OPERANDS);

        this.method = method;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + method.getName() };
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.REQUIRES_DYNSCOPE);
        scope.getFlags().add(IRFlags.REQUIRES_VISIBILITY);
        return true;
    }

    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineInstanceMethodInstr(method);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getMethod());
    }

    public static DefineInstanceMethodInstr decode(IRReaderDecoder d) {
        return new DefineInstanceMethodInstr((IRMethod) d.decodeScope());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRRuntimeHelpers.defInterpretedInstanceMethod(context, method, currDynScope, self);

        return null; // unused; symbol is propagated
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineInstanceMethodInstr(this);
    }
}
