package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRFlags;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class UndefMethodInstr extends ResultBaseInstr implements FixedArityInstr {
    // SSS FIXME: Implicit self arg -- make explicit to not get screwed by inlining!
    public UndefMethodInstr(Variable result, Operand methodName) {
        super(Operation.UNDEF_METHOD, result, new Operand[] { methodName });
    }

    public Operand getMethodName() {
        return operands[0];
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        scope.getFlags().add(IRFlags.REQUIRES_DYNSCOPE);
        return true;
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new UndefMethodInstr((Variable)result.cloneForInlining(ii), getMethodName().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getMethodName());
    }

    public static UndefMethodInstr decode(IRReaderDecoder d) {
        return new UndefMethodInstr(d.decodeVariable(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        RubyModule module = IRRuntimeHelpers.findInstanceMethodContainer(context, currDynScope, self);
        Object nameArg = getMethodName().retrieve(context, self, currScope, currDynScope, temp);
        String name = (nameArg instanceof String) ? (String) nameArg : nameArg.toString();
        module.undef(context, name);
        return context.runtime.getNil();
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.UndefMethodInstr(this);
    }
}
