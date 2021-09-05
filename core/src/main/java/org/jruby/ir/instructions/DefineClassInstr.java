package org.jruby.ir.instructions;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.InterpretedIRBodyMethod;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.transformations.inlining.CloneInfo;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class DefineClassInstr extends TwoOperandResultBaseInstr implements FixedArityInstr {
    private final IRClassBody body;

    public DefineClassInstr(Variable result, IRClassBody body, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS, result, container, superClass == null ? UndefinedValue.UNDEFINED : superClass);

        assert result != null: "DefineClassInstr result is null";

        this.body = body;
    }

    public IRClassBody getNewIRClassBody() {
        return body;
    }

    public Operand getContainer() {
        return getOperand1();
    }

    public Operand getSuperClass() {
        return getOperand2();
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineClassInstr(ii.getRenamedVariable(result), body,
                getContainer().cloneForInlining(ii), getSuperClass().cloneForInlining(ii));
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] {"name: " + body.getId() };
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewIRClassBody());
        e.encode(getContainer());
        e.encode(getSuperClass());
    }

    public static DefineClassInstr decode(IRReaderDecoder d) {
        return new DefineClassInstr((d.decodeVariable()), (IRClassBody) d.decodeScope(), d.decodeOperand(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object container = getContainer().retrieve(context, self, currScope, currDynScope, temp);
        Object superClass = getSuperClass().retrieve(context, self, currScope, currDynScope, temp);

        RubyModule clazz = IRRuntimeHelpers.newRubyClassFromIR(context.runtime, body.getId(), body.getStaticScope(),
                superClass, container, body.maybeUsingRefinements());

        //if (IRRuntimeHelpers.isDebug()) doDebug();

        return INTERPRET_CLASS(context, clazz);
    }

    private IRubyObject INTERPRET_CLASS(ThreadContext context, RubyModule clazz) {
        InterpreterContext ic = body.getInterpreterContext();
        String id = body.getId();
        boolean hasExplicitCallProtocol =  ic.hasExplicitCallProtocol();

        if (!hasExplicitCallProtocol) pre(ic, context, clazz, null, clazz);

        try {
            ThreadContext.pushBacktrace(context, id, ic.getFileName(), ic.getLine());
            return ic.getEngine().interpret(context, null, clazz, ic, clazz.getMethodLocation(), id, Block.NULL_BLOCK);
        } finally {
            body.cleanupAfterExecution();
            if (!hasExplicitCallProtocol) post(ic, context);
            ThreadContext.popBacktrace(context);
        }
    }

    private void post(InterpreterContext ic, ThreadContext context) {
        context.popFrame();
        if (ic.popDynScope()) context.popScope();
    }

    private void pre(InterpreterContext ic, ThreadContext context, IRubyObject self, String name, RubyModule implClass) {
        context.preMethodFrameOnly(implClass, name, self);
        if (ic.pushNewDynScope()) context.pushScope(DynamicScope.newDynamicScope(ic.getStaticScope()));
    }

    @Override
    public void visit(IRVisitor visitor) {
        visitor.DefineClassInstr(this);
    }
}
