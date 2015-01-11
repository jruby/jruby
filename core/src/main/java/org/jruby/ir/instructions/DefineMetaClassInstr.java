package org.jruby.ir.instructions;

import org.jruby.Ruby;
import org.jruby.ir.*;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Map;

public class DefineMetaClassInstr extends Instr implements ResultInstr, FixedArityInstr {
    private final IRModuleBody metaClassBody;

    public DefineMetaClassInstr(Variable result, Operand object, IRModuleBody metaClassBody) {
        super(Operation.DEF_META_CLASS, result, new Operand[] {object });

        assert result != null: "DefineMetaClassInstr result is null";

        this.metaClassBody = metaClassBody;
    }

    public IRModuleBody getMetaClassBody() {
        return metaClassBody;
    }

    public Operand getObject() {
        return operands[0];
    }

    @Override
    public boolean computeScopeFlags(IRScope scope) {
        // SSS: Inner-classes are defined with closures and
        // a return in the closure can force a return from this method
        // For now, conservatively assume that a scope with inner-classes
        // can receive non-local returns. (Alternatively, have to inspect
        // all lexically nested scopes, not just closures in computeScopeFlags())
        scope.getFlags().add(IRFlags.CAN_RECEIVE_NONLOCAL_RETURNS);
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + "(" + metaClassBody.getName() + ", " + getObject() + ", " + metaClassBody.getFileName() + ")";
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineMetaClassInstr(ii.getRenamedVariable(result), getObject().cloneForInlining(ii), metaClassBody);
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Ruby runtime = context.runtime;

        IRubyObject obj = (IRubyObject) getObject().retrieve(context, self, currScope, currDynScope, temp);

        return IRRuntimeHelpers.newInterpretedMetaClass(runtime, metaClassBody, obj);
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineMetaClassInstr(this);
    }
}
