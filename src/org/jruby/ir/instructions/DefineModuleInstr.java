package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.Operation;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.util.CodegenUtils;

public class DefineModuleInstr extends Instr implements ResultInstr {
    private final IRModuleBody newIRModuleBody;
    private Operand container;
    private Variable result;

    public DefineModuleInstr(Variable result, IRModuleBody newIRModuleBody, Operand container) {
        super(Operation.DEF_MODULE);
        
        assert result != null : "DefineModuleInstr result is null";
        
        this.newIRModuleBody = newIRModuleBody;
        this.container = container;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{container};
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + newIRModuleBody.getName() + ", " + container + ", " + newIRModuleBody.getFileName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: So, do we clone the module body scope or not?
        return new DefineModuleInstr(ii.getRenamedVariable(result), this.newIRModuleBody, container.cloneForInlining(ii));
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object rubyContainer = container.retrieve(context, self, currDynScope, temp);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");

        RubyModule newRubyModule = ((RubyModule) rubyContainer).defineOrGetModuleUnder(newIRModuleBody.getName());
        newIRModuleBody.getStaticScope().setModule(newRubyModule);
        return new InterpretedIRMethod(newIRModuleBody, Visibility.PUBLIC, newRubyModule);
    }

    @Override
    public void compile(JVM jvm) {
        StaticScope scope = newIRModuleBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // new CompiledIRMethod
        jvm.method().adapter.newobj(CodegenUtils.p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        jvm.emit(newIRModuleBody); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(newIRModuleBody.getName());
        jvm.method().adapter.ldc(newIRModuleBody.getFileName());
        jvm.method().adapter.ldc(newIRModuleBody.getLineNumber());

        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(CodegenUtils.p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        // create module
        jvm.method().loadLocal(0);
        jvm.emit(container);
        jvm.method().invokeHelper("checkIsRubyModule", RubyModule.class, ThreadContext.class, Object.class);
        jvm.method().adapter.ldc(newIRModuleBody.getName());
        jvm.method().adapter.invokevirtual(CodegenUtils.p(RubyModule.class), "defineOrGetModuleUnder", CodegenUtils.sig(RubyModule.class, String.class));

        // set into StaticScope
        jvm.method().adapter.dup2();
        jvm.method().adapter.invokevirtual(CodegenUtils.p(StaticScope.class), "setModule", CodegenUtils.sig(void.class, RubyModule.class));

        jvm.method().adapter.getstatic(CodegenUtils.p(Visibility.class), "PUBLIC", CodegenUtils.ci(Visibility.class));
        jvm.method().adapter.swap();

        // invoke constructor
        jvm.method().adapter.invokespecial(CodegenUtils.p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;)V");

        // store
        jvm.method().storeLocal(jvm.methodData().local(getResult()));
    }
}
