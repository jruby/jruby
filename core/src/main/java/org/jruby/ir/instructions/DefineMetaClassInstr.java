package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.ir.*;
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

import java.util.EnumSet;

public class DefineMetaClassInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final IRModuleBody metaClassBody;

    public DefineMetaClassInstr(Variable result, Operand object, IRModuleBody metaClassBody) {
        super(Operation.DEF_META_CLASS, result, object);

        assert result != null: "DefineMetaClassInstr result is null";

        this.metaClassBody = metaClassBody;
    }

    public IRModuleBody getMetaClassBody() {
        return metaClassBody;
    }

    public Operand getObject() {
        return getOperand1();
    }

    @Override
    public boolean computeScopeFlags(IRScope scope, EnumSet<IRFlags> flags) {
        // SSS: Inner-classes are defined with closures and
        // a return in the closure can force a return from this method
        // For now, conservatively assume that a scope with inner-classes
        // can receive non-local returns. (Alternatively, have to inspect
        // all lexically nested scopes, not just closures in computeScopeFlags())
        scope.setCanReceiveNonlocalReturns();

        return true;
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + metaClassBody.getId() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineMetaClassInstr(ii.getRenamedVariable(result), getObject().cloneForInlining(ii), metaClassBody);
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getMetaClassBody());
    }

    public static DefineMetaClassInstr decode(IRReaderDecoder d) {
        return new DefineMetaClassInstr(d.decodeVariable(), d.decodeOperand(), (IRModuleBody) d.decodeScope());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        IRubyObject obj = (IRubyObject) getObject().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.newInterpretedMetaClass(context, metaClassBody, obj);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineMetaClassInstr(this);
    }
}
