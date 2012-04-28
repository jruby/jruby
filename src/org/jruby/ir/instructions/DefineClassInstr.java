package org.jruby.ir.instructions;

import java.util.Map;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.Operation;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Variable;
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

public class DefineClassInstr extends Instr implements ResultInstr {
    private IRClassBody newIRClassBody;
    private Operand container;
    private Operand superClass;
    private Variable result;
    
    public DefineClassInstr(Variable result, IRClassBody newIRClassBody, Operand container, Operand superClass) {
        super(Operation.DEF_CLASS);
        
        assert result != null: "DefineClassInstr result is null";
        
        this.container = container;
        this.superClass = superClass == null ? newIRClassBody.getManager().getNil() : superClass;
        this.newIRClassBody = newIRClassBody;
        this.result = result;
    }

    public Operand[] getOperands() {
        return new Operand[]{container, superClass};
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
        superClass = superClass.getSimplifiedOperand(valueMap, force);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + newIRClassBody.getName() + ", " + container + ", " + superClass + ", " + newIRClassBody.getFileName() + ")";
    }

    @Override
    public Instr cloneForInlining(InlinerInfo ii) {
        // SSS FIXME: So, do we clone the class body scope or not?
        return new DefineClassInstr(ii.getRenamedVariable(result), this.newIRClassBody, container.cloneForInlining(ii), superClass.cloneForInlining(ii));
    }
    
    private RubyModule newClass(ThreadContext context, IRubyObject self, RubyModule classContainer, DynamicScope currDynScope, Object[] temp) {
        if (newIRClassBody instanceof IRMetaClassBody) return classContainer.getMetaClass();

        RubyClass sc;
        if (superClass == context.getRuntime().getIRManager().getNil()) {
            sc = null;
        } else {
            Object o = superClass.retrieve(context, self, currDynScope, temp);

            if (!(o instanceof RubyClass)) throw context.getRuntime().newTypeError("superclass must be Class (" + o + " given)");
            
            sc = (RubyClass) o;
        }

        return classContainer.defineOrGetClassUnder(newIRClassBody.getName(), sc);
    }

    @Override
    public Object interpret(ThreadContext context, DynamicScope currDynScope, IRubyObject self, Object[] temp, Block block) {
        Object rubyContainer = container.retrieve(context, self, currDynScope, temp);
        
        if (!(rubyContainer instanceof RubyModule)) throw context.getRuntime().newTypeError("no outer class/module");

        RubyModule newRubyClass = newClass(context, self, (RubyModule) rubyContainer, currDynScope, temp);
        newIRClassBody.getStaticScope().setModule(newRubyClass);
        return new InterpretedIRMethod(newIRClassBody, Visibility.PUBLIC, newRubyClass);
    }

    @Override
    public void compile(JVM jvm) {
        StaticScope scope = newIRClassBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // new CompiledIRMethod
        jvm.method().adapter.newobj(CodegenUtils.p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        jvm.emit(newIRClassBody); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(newIRClassBody.getName());
        jvm.method().adapter.ldc(newIRClassBody.getFileName());
        jvm.method().adapter.ldc(newIRClassBody.getLineNumber());

        // construct class with RuntimeHelpers.newClassForIR
        jvm.method().adapter.aload(0); // ThreadContext
        jvm.method().adapter.ldc(newIRClassBody.getName()); // class name
        jvm.method().loadLocal(2); // self

        // create class
        jvm.method().loadLocal(0);
        jvm.emit(container);
        jvm.method().invokeHelper("checkIsRubyModule", RubyModule.class, ThreadContext.class, Object.class);

        // superclass
        if (superClass instanceof Nil) {
            jvm.method().adapter.aconst_null();
        } else {
            jvm.emit(superClass);
        }

        // is meta?
        jvm.method().adapter.ldc(newIRClassBody instanceof IRMetaClassBody);

        jvm.method().invokeHelper("newClassForIR", RubyClass.class, ThreadContext.class, String.class, IRubyObject.class, RubyModule.class, Object.class, boolean.class);

        //// static scope
        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(CodegenUtils.p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");
        jvm.method().adapter.swap();

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
