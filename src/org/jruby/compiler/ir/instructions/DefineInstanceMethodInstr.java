package org.jruby.compiler.ir.instructions;

import java.util.Map;

import org.jruby.MetaClass;
import org.jruby.Ruby;
import org.jruby.RubyModule;

import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.representations.InlinerInfo;

import org.jruby.common.IRubyWarnings.ID;

import org.jruby.compiler.ir.targets.JVM;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.internal.runtime.methods.WrapperMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.util.CodegenUtils;

public class DefineInstanceMethodInstr extends Instr {
    private Operand container;
    private final IRMethod method;

    public DefineInstanceMethodInstr(Operand container, IRMethod method) {
        super(Operation.DEF_INST_METH);
        this.container = container;
        this.method = method;
    }

    @Override
    public String toString() {
        return getOperation() + "(" + container + ", " + method.getName() + ", " + method.getFileName() + ")";
    }
    
    public IRMethod getMethod() {
        return method;
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: So, what happens to the method?
        return new DefineInstanceMethodInstr(container.cloneForInlining(ii), method);
    }

    // SSS FIXME: Go through this and DefineClassMethodInstr.interpret, clean up, extract common code
    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        // SSS FIXME: This is a temporary solution that uses information from the stack.
        // This instruction and this logic will be re-implemented to not use implicit information from the stack.
        // Till such time, this code implements the correct semantics.
        RubyModule clazz = context.getRubyClass();
        String     name  = method.getName();

        // Error checks and warnings on method definitions
        Ruby runtime = context.getRuntime();
        if (clazz == runtime.getDummy()) {
            throw runtime.newTypeError("no class/module to add method");
        }

        if (clazz == runtime.getObject() && "initialize".equals(name)) {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining Object#initialize may cause infinite loop");
        }

        if ("__id__".equals(name) || "__send__".equals(name)) {
            runtime.getWarnings().warn(ID.REDEFINING_DANGEROUS, "redefining `" + name + "' may cause serious problem"); 
        }

        Visibility visibility = context.getCurrentVisibility();
        if ("initialize".equals(name) || "initialize_copy".equals(name) || visibility == Visibility.MODULE_FUNCTION) {
            visibility = Visibility.PRIVATE;
        }

        DynamicMethod newMethod = new InterpretedIRMethod(method, visibility, clazz);
        clazz.addMethod(name, newMethod);
        //System.out.println("Added " + name + " to " + clazz + "; self is " + self);

        if (context.getCurrentVisibility() == Visibility.MODULE_FUNCTION) {
            clazz.getSingletonClass().addMethod(name, new WrapperMethod(clazz.getSingletonClass(), newMethod, Visibility.PUBLIC));
            clazz.callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        }
   
        // 'class << state.self' and 'class << obj' uses defn as opposed to defs
        if (clazz.isSingleton()) {
            ((MetaClass) clazz).getAttached().callMethod(context, "singleton_method_added", runtime.fastNewSymbol(name));
        } else {
            clazz.callMethod(context, "method_added", runtime.fastNewSymbol(name));
        }
        return null;
    }

    public Operand[] getOperands() {
        return new Operand[]{container};
    }
    
    @Override
    public void simplifyOperands(Map<Operand, Operand> valueMap, boolean force) {
        container = container.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public void compile(JVM jvm) {
        StaticScope scope = method.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile arity != 0 method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // preamble for addMethod below
        jvm.method().adapter.aload(0);
        jvm.method().adapter.invokevirtual(CodegenUtils.p(ThreadContext.class), "getRubyClass", "()Lorg/jruby/RubyModule;");
        jvm.method().adapter.ldc(method.getName());

        // new CompiledIRMethod
        jvm.method().adapter.newobj(CodegenUtils.p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        jvm.emit(method); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(method.getName());
        jvm.method().adapter.ldc(method.getFileName());
        jvm.method().adapter.ldc(method.getLineNumber());

        jvm.method().adapter.aload(0);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(CodegenUtils.p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        jvm.method().adapter.aload(0);
        jvm.method().adapter.invokevirtual(CodegenUtils.p(ThreadContext.class), "getCurrentVisibility", "()Lorg/jruby/runtime/Visibility;");
        jvm.method().adapter.aload(0);
        jvm.method().adapter.invokevirtual(CodegenUtils.p(ThreadContext.class), "getRubyClass", "()Lorg/jruby/RubyModule;");

        // invoke constructor
        jvm.method().adapter.invokespecial(CodegenUtils.p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;)V");

        // add method
        jvm.method().adapter.invokevirtual(CodegenUtils.p(RubyModule.class), "addMethod", "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");
    }
}
