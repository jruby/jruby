package org.jruby.ir.instructions;

import org.jruby.RubyModule;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.Operation;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.ir.operands.Operand;
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

public class DefineModuleInstr extends OneOperandResultBaseInstr implements FixedArityInstr {
    private final IRModuleBody body;

    public DefineModuleInstr(Variable result, IRModuleBody body, Operand container) {
        super(Operation.DEF_MODULE, result, container);

        assert result != null : "DefineModuleInstr result is null";

        this.body = body;
    }


    public IRModuleBody getNewIRModuleBody() {
        return body;
    }

    public Operand getContainer() {
        return getOperand1();
    }

    @Override
    public String[] toStringNonOperandArgs() {
        return new String[] { "name: " + body.getId() };
    }

    @Override
    public Instr clone(CloneInfo ii) {
        return new DefineModuleInstr(ii.getRenamedVariable(result), body, getContainer().cloneForInlining(ii));
    }

    @Override
    public void encode(IRWriterEncoder e) {
        super.encode(e);
        e.encode(getNewIRModuleBody());
        e.encode(getContainer());
    }

    public static DefineModuleInstr decode(IRReaderDecoder d) {
        return new DefineModuleInstr(d.decodeVariable(), (IRModuleBody) d.decodeScope(), d.decodeOperand());
    }

    @Override
    public Object interpret(ThreadContext context, StaticScope currScope, DynamicScope currDynScope, IRubyObject self, Object[] temp) {
        Object container = getContainer().retrieve(context, self, currScope, currDynScope, temp);

        RubyModule clazz = IRRuntimeHelpers.newRubyModuleFromIR(context, body.getId(), body.getStaticScope(), container, body.maybeUsingRefinements());

        //if (IRRuntimeHelpers.isDebug()) doDebug();

        return INTERPRET_MODULE(context, clazz);
    }

    private IRubyObject INTERPRET_MODULE(ThreadContext context, RubyModule clazz) {
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
        visitor.DefineModuleInstr(this);
    }
}
