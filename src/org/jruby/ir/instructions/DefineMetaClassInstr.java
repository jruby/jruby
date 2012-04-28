package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.Operation;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.transformations.inlining.InlinerInfo;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.CodegenUtils;

public class DefineMetaClassInstr extends Instr implements ResultInstr {
    private IRModuleBody metaClassBody;
    private Operand object;
    private Variable result;
    
    public DefineMetaClassInstr(Variable result, Operand object, IRModuleBody metaClassBody) {
        super(Operation.DEF_META_CLASS);
        
        assert result != null: "DefineMetaClassInstr result is null";
        
        this.metaClassBody = metaClassBody;
        this.object = object;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{object};
    }
    
    public Variable getResult() {
        return result;
    }

    public void updateResult(Variable v) {
        this.result = v;
    }

    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        object = object.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + metaClassBody.getName() + ", " + object + ", " + metaClassBody.getFileName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS: So, do we clone the meta-class body scope or not?
        return new DefineMetaClassInstr(ii.getRenamedVariable(result), object.cloneForInlining(ii), metaClassBody);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Ruby runtime = context.getRuntime();
        IRubyObject obj = (IRubyObject)object.retrieve(context, self, currDynScope, temp);
        
        RubyClass singletonClass = RuntimeHelpers.getSingletonClass(runtime, obj);
        metaClassBody.getStaticScope().setModule(singletonClass);
		  return new InterpretedIRMethod(metaClassBody, Visibility.PUBLIC, singletonClass);
    }

    @Override
    public void compile(JVM jvm) {
        StaticScope scope = metaClassBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // new CompiledIRMethod
        jvm.method().adapter.newobj(CodegenUtils.p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        jvm.emit(metaClassBody); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(metaClassBody.getName());
        jvm.method().adapter.ldc(metaClassBody.getFileName());
        jvm.method().adapter.ldc(metaClassBody.getLineNumber());

        //// static scope
        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(CodegenUtils.p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        // get singleton class
        jvm.method().pushRuntime();
        jvm.emit(object);
        jvm.method().invokeHelper("getSingletonClass", RubyClass.class, Ruby.class, IRubyObject.class);

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
