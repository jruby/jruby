package org.jruby.ir.targets;

import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.Tuple;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.defined.BackrefIsMatchDataInstr;
import org.jruby.ir.instructions.defined.ClassVarIsDefinedInstr;
import org.jruby.ir.instructions.defined.GetBackrefInstr;
import org.jruby.ir.instructions.defined.GetDefinedConstantOrMethodInstr;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.GlobalIsDefinedInstr;
import org.jruby.ir.instructions.defined.HasInstanceVarInstr;
import org.jruby.ir.instructions.defined.IsMethodBoundInstr;
import org.jruby.ir.instructions.defined.MethodDefinedInstr;
import org.jruby.ir.instructions.defined.MethodIsPublicInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.instructions.defined.SuperMethodBoundInstr;
import org.jruby.ir.instructions.ruby18.ReceiveOptArgInstr18;
import org.jruby.ir.instructions.ruby18.ReceiveRestArgInstr18;
import org.jruby.ir.instructions.ruby19.BuildLambdaInstr;
import org.jruby.ir.instructions.ruby19.GetEncodingInstr;
import org.jruby.ir.instructions.ruby19.ReceiveOptArgInstr19;
import org.jruby.ir.instructions.ruby19.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ruby19.ReceiveRestArgInstr19;
import org.jruby.ir.operands.Array;
import org.jruby.ir.operands.AsString;
import org.jruby.ir.operands.Backref;
import org.jruby.ir.operands.BacktickString;
import org.jruby.ir.operands.Bignum;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.CompoundArray;
import org.jruby.ir.operands.CompoundString;
import org.jruby.ir.operands.CurrentScope;
import org.jruby.ir.operands.DynamicSymbol;
import org.jruby.ir.operands.Fixnum;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Hash;
import org.jruby.ir.operands.IRException;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.MethAddr;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.operands.NthRef;
import org.jruby.ir.operands.ObjectClass;
import org.jruby.ir.operands.Operand;
import org.jruby.ir.operands.Range;
import org.jruby.ir.operands.Regexp;
import org.jruby.ir.operands.SValue;
import org.jruby.ir.operands.ScopeModule;
import org.jruby.ir.operands.Self;
import org.jruby.ir.operands.Splat;
import org.jruby.ir.operands.StandardError;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.Symbol;
import org.jruby.ir.operands.TemporaryClosureVariable;
import org.jruby.ir.operands.TemporaryVariable;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.operands.UnexecutableNil;
import org.jruby.ir.operands.Variable;
import org.jruby.ir.operands.WrappedIRClosure;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.InstanceVariables;
import org.jruby.util.CodegenUtils;
import org.jruby.util.JRubyClassLoader;

import java.util.Map;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

/**
 * Implementation of IRCompiler for the JVM.
 */
public class JVMVisitor extends IRVisitor {

    public static final String DYNAMIC_SCOPE = "$dynamicScope";

    public JVMVisitor() {
        this.jvm = new JVM();
    }

    public static Class compile(Ruby ruby, IRScope scope, JRubyClassLoader jrubyClassLoader) {
        // run compiler
        JVMVisitor target = new JVMVisitor();

        target.codegen(scope);

//        try {
//            FileOutputStream fos = new FileOutputStream("tmp.class");
//            fos.write(target.code());
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return jrubyClassLoader.defineClass(JVM.scriptToClass(scope.getName()), target.code());
    }

    public byte[] code() {
        return jvm.code();
    }

    public void codegen(IRScope scope) {
        if (scope instanceof IRScriptBody) {
            codegen((IRScriptBody)scope);
        }
    }

    public void codegen(IRScriptBody script) {
        this.script = script;
        emit(script);
    }

    public String emitScope(IRScope scope, String name, int arity) {
        name = name + scope.getLineNumber();
        jvm.pushmethod(name, arity);

        Tuple<Instr[], Map<Integer,Label[]>> t = scope.prepareForCompilation();
        Instr[] instrs = t.a;
        Map<Integer, Label[]> jumpTable = t.b;

        for (int i = 0; i < instrs.length; i++) {
            Instr instr = instrs[i];

            if (jumpTable.get(i) != null) {
                for (Label label : jumpTable.get(i)) jvm.method().mark(jvm.methodData().getLabel(label));
            }
            visit(instr);
        }

        jvm.popmethod();

        return name;
    }

    public void emit(IRScriptBody script) {
        String clsName = jvm.scriptToClass(script.getName());
        jvm.pushscript(clsName, script.getFileName());

        emitScope(script, "__script__", 0);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    public void emit(IRMethod method) {
        String name = emitScope(method, method.getName(), method.getCallArgs().length);

        // push a method handle for binding purposes
        jvm.method().pushHandle(jvm.clsData().clsName, name, method.getStaticScope().getRequiredArgs());
    }

    public void emit(IRModuleBody method) {
        String name = method.getName();
        if (name.indexOf("DUMMY_MC") != -1) {
            name = "METACLASS";
        }

        name = emitScope(method, name, 0);

        // push a method handle for binding purposes
        jvm.method().pushHandle(jvm.clsData().clsName, name, method.getStaticScope().getRequiredArgs());
    }

    public void visit(Instr instr) {
        instr.visit(this);
    }

    public void visit(Operand operand) {
        if (operand.hasKnownValue()) {
            operand.visit(this);
        } else if (operand instanceof Variable) {
            emitVariable((Variable)operand);
        } else {
            operand.visit(this);
        }
    }

    public void emitVariable(Variable variable) {
//        System.out.println("variable: " + variable);
        int index = jvm.methodData().local(variable);
//        System.out.println("index: " + index);
        jvm.method().loadLocal(index);
    }

    @Override
    public void AliasInstr(AliasInstr aliasInstr) {
        jvm.method().loadLocal(0);
        jvm.method().loadLocal(jvm.methodData().local(aliasInstr.getReceiver()));
        jvm.method().adapter.ldc(((StringLiteral) aliasInstr.getNewName()).string);
        jvm.method().adapter.ldc(((StringLiteral) aliasInstr.getOldName()).string);
        jvm.method().invokeHelper("defineAlias", IRubyObject.class, ThreadContext.class, IRubyObject.class, Object.class, Object.class);
        jvm.method().adapter.pop();
    }

    @Override
    public void AttrAssignInstr(AttrAssignInstr attrAssignInstr) {
        jvm.method().loadLocal(0);
        visit(attrAssignInstr.getReceiver());
        for (Operand operand : attrAssignInstr.getCallArgs()) {
            visit(operand);
        }

        jvm.method().invokeOther(attrAssignInstr.getMethodAddr().getName(), attrAssignInstr.getCallArgs().length);
        jvm.method().adapter.pop();
    }

    @Override
    public void BEQInstr(BEQInstr beqInstr) {
        Operand[] args = beqInstr.getOperands();
        jvm.method().loadLocal(0);
        visit(args[0]);
        visit(args[1]);
        jvm.method().invokeHelper("BEQ", boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        jvm.method().adapter.iftrue(jvm.methodData().getLabel(beqInstr.getJumpTarget()));
    }

    @Override
    public void BFalseInstr(BFalseInstr bFalseInstr) {
        visit(bFalseInstr.getArg1());
        jvm.method().isTrue();
        jvm.method().bfalse(jvm.methodData().getLabel(bFalseInstr.getJumpTarget()));
    }

    @Override
    public void BlockGivenInstr(BlockGivenInstr blockGivenInstr) {
        super.BlockGivenInstr(blockGivenInstr);
    }

    @Override
    public void BNEInstr(BNEInstr bneinstr) {
        Operand[] args = bneinstr.getOperands();
        jvm.method().loadLocal(0);
        visit(args[0]);
        visit(args[1]);
        jvm.method().invokeHelper("BNE", boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        jvm.method().adapter.iftrue(jvm.methodData().getLabel(bneinstr.getJumpTarget()));
    }

    @Override
    public void BNilInstr(BNilInstr bnilinstr) {
        visit(bnilinstr.getArg1());
        jvm.method().isNil();
        jvm.method().btrue(jvm.methodData().getLabel(bnilinstr.getJumpTarget()));
    }

    @Override
    public void BreakInstr(BreakInstr breakinstr) {
        super.BreakInstr(breakinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void BTrueInstr(BTrueInstr btrueinstr) {
        visit(btrueinstr.getArg1());
        jvm.method().isTrue();
        jvm.method().btrue(jvm.methodData().getLabel(btrueinstr.getJumpTarget()));
    }

    @Override
    public void BUndefInstr(BUndefInstr bundefinstr) {
        super.BUndefInstr(bundefinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void CallInstr(CallInstr callinstr) {
        jvm.method().loadLocal(0);
        visit(callinstr.getReceiver());
        for (Operand operand : callinstr.getCallArgs()) {
            visit(operand);
        }

        switch (callinstr.getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                jvm.method().invokeSelf(callinstr.getMethodAddr().getName(), callinstr.getCallArgs().length);
                break;
            case NORMAL:
                jvm.method().invokeOther(callinstr.getMethodAddr().getName(), callinstr.getCallArgs().length);
                break;
            case SUPER:
                jvm.method().invokeSuper(callinstr.getMethodAddr().getName(), callinstr.getCallArgs().length);
                break;
        }

        int index = jvm.methodData().local(callinstr.getResult());
        jvm.method().storeLocal(index);
    }

    @Override
    public void CheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkargsarrayarityinstr) {
        super.CheckArgsArrayArityInstr(checkargsarrayarityinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void CheckArityInstr(CheckArityInstr checkarityinstr) {
        // no-op for now
    }

    @Override
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) {
        super.ClassSuperInstr(classsuperinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ClosureReturnInstr(ClosureReturnInstr closurereturninstr) {
        super.ClosureReturnInstr(closurereturninstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ConstMissingInstr(ConstMissingInstr constmissinginstr) {
        CallInstr(constmissinginstr);
    }

    @Override
    public void CopyInstr(CopyInstr copyinstr) {
        int index = jvm.methodData().local(copyinstr.getResult());
        visit(copyinstr.getSource());
        jvm.method().storeLocal(index);
    }

    @Override
    public void DefineClassInstr(DefineClassInstr defineclassinstr) {
        IRClassBody newIRClassBody = defineclassinstr.getNewIRClassBody();
        StaticScope scope = newIRClassBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // new CompiledIRMethod
        jvm.method().adapter.newobj(p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        emit(newIRClassBody); // handle

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
        visit(defineclassinstr.getContainer());
        jvm.method().invokeHelper("checkIsRubyModule", RubyModule.class, ThreadContext.class, Object.class);

        // superclass
        if (defineclassinstr.getSuperClass() instanceof Nil) {
            jvm.method().adapter.aconst_null();
        } else {
            visit(defineclassinstr.getSuperClass());
        }

        // is meta?
        jvm.method().adapter.ldc(newIRClassBody instanceof IRMetaClassBody);

        jvm.method().invokeHelper("newClassForIR", RubyClass.class, ThreadContext.class, String.class, IRubyObject.class, RubyModule.class, Object.class, boolean.class);

        //// static scope
        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");
        jvm.method().adapter.swap();

        // set into StaticScope
        jvm.method().adapter.dup2();
        jvm.method().adapter.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

        jvm.method().adapter.getstatic(p(Visibility.class), "PUBLIC", ci(Visibility.class));
        jvm.method().adapter.swap();

        // invoke constructor
        jvm.method().adapter.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;)V");

        // store
        jvm.method().storeLocal(jvm.methodData().local(defineclassinstr.getResult()));
    }

    @Override
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) {
        super.DefineClassMethodInstr(defineclassmethodinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) {
        IRMethod method = defineinstancemethodinstr.getMethod();
        StaticScope scope = method.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // preamble for addMethod below
        jvm.method().adapter.aload(0);
        jvm.method().adapter.invokevirtual(p(ThreadContext.class), "getRubyClass", "()Lorg/jruby/RubyModule;");
        jvm.method().adapter.ldc(method.getName());

        // new CompiledIRMethod
        jvm.method().adapter.newobj(p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        emit(method); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(method.getName());
        jvm.method().adapter.ldc(method.getFileName());
        jvm.method().adapter.ldc(method.getLineNumber());

        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        jvm.method().adapter.aload(0);
        jvm.method().adapter.invokevirtual(p(ThreadContext.class), "getCurrentVisibility", "()Lorg/jruby/runtime/Visibility;");
        jvm.method().adapter.aload(0);
        jvm.method().adapter.invokevirtual(p(ThreadContext.class), "getRubyClass", "()Lorg/jruby/RubyModule;");

        // invoke constructor
        jvm.method().adapter.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;)V");

        // add method
        jvm.method().adapter.invokevirtual(p(RubyModule.class), "addMethod", "(Ljava/lang/String;Lorg/jruby/internal/runtime/methods/DynamicMethod;)V");
    }

    @Override
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) {
        IRModuleBody metaClassBody = definemetaclassinstr.getMetaClassBody();
        StaticScope scope = metaClassBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // new CompiledIRMethod
        jvm.method().adapter.newobj(p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        emit(metaClassBody); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(metaClassBody.getName());
        jvm.method().adapter.ldc(metaClassBody.getFileName());
        jvm.method().adapter.ldc(metaClassBody.getLineNumber());

        //// static scope
        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        // get singleton class
        jvm.method().pushRuntime();
        visit(definemetaclassinstr.getObject());
        jvm.method().invokeHelper("getSingletonClass", RubyClass.class, Ruby.class, IRubyObject.class);

        // set into StaticScope
        jvm.method().adapter.dup2();
        jvm.method().adapter.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

        jvm.method().adapter.getstatic(p(Visibility.class), "PUBLIC", ci(Visibility.class));
        jvm.method().adapter.swap();

        // invoke constructor
        jvm.method().adapter.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;)V");

        // store
        jvm.method().storeLocal(jvm.methodData().local(definemetaclassinstr.getResult()));
    }

    @Override
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) {
        IRModuleBody newIRModuleBody = definemoduleinstr.getNewIRModuleBody();
        StaticScope scope = newIRModuleBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = RuntimeHelpers.encodeScope(scope);

        // new CompiledIRMethod
        jvm.method().adapter.newobj(p(CompiledIRMethod.class));
        jvm.method().adapter.dup();

        // emit method body and get handle
        emit(newIRModuleBody); // handle

        // add'l args for CompiledIRMethod constructor
        jvm.method().adapter.ldc(newIRModuleBody.getName());
        jvm.method().adapter.ldc(newIRModuleBody.getFileName());
        jvm.method().adapter.ldc(newIRModuleBody.getLineNumber());

        jvm.method().adapter.aload(0);
        jvm.method().adapter.aload(1);
        jvm.method().adapter.ldc(scopeString);
        jvm.method().adapter.invokestatic(p(RuntimeHelpers.class), "decodeLocalScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        // create module
        jvm.method().loadLocal(0);
        visit(definemoduleinstr.getContainer());
        jvm.method().invokeHelper("checkIsRubyModule", RubyModule.class, ThreadContext.class, Object.class);
        jvm.method().adapter.ldc(newIRModuleBody.getName());
        jvm.method().adapter.invokevirtual(p(RubyModule.class), "defineOrGetModuleUnder", sig(RubyModule.class, String.class));

        // set into StaticScope
        jvm.method().adapter.dup2();
        jvm.method().adapter.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

        jvm.method().adapter.getstatic(p(Visibility.class), "PUBLIC", ci(Visibility.class));
        jvm.method().adapter.swap();

        // invoke constructor
        jvm.method().adapter.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;)V");

        // store
        jvm.method().storeLocal(jvm.methodData().local(definemoduleinstr.getResult()));
    }

    @Override
    public void EnsureRubyArrayInstr(EnsureRubyArrayInstr ensurerubyarrayinstr) {
        super.EnsureRubyArrayInstr(ensurerubyarrayinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void EQQInstr(EQQInstr eqqinstr) {
        super.EQQInstr(eqqinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ExceptionRegionEndMarkerInstr(ExceptionRegionEndMarkerInstr exceptionregionendmarkerinstr) {
        super.ExceptionRegionEndMarkerInstr(exceptionregionendmarkerinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) {
        super.ExceptionRegionStartMarkerInstr(exceptionregionstartmarkerinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr getclassvarcontainermoduleinstr) {
        super.GetClassVarContainerModuleInstr(getclassvarcontainermoduleinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetClassVariableInstr(GetClassVariableInstr getclassvariableinstr) {
        super.GetClassVariableInstr(getclassvariableinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetFieldInstr(GetFieldInstr getfieldinstr) {
        String field = getfieldinstr.getRef();
        visit(getfieldinstr.getSource());
        jvm.method().getField(field);
        jvm.method().storeLocal(jvm.methodData().local(getfieldinstr.getResult()));
    }

    @Override
    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) {
        super.GetGlobalVariableInstr(getglobalvariableinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GVarAliasInstr(GVarAliasInstr gvaraliasinstr) {
        super.GVarAliasInstr(gvaraliasinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) {
        jvm.method().loadLocal(0);
        visit(inheritancesearchconstinstr.getCurrentModule());

        // TODO: private consts
        jvm.method().inheritanceSearchConst(inheritancesearchconstinstr.getConstName());
        jvm.method().storeLocal(jvm.methodData().local(inheritancesearchconstinstr.getResult()));
    }

    @Override
    public void InstanceOfInstr(InstanceOfInstr instanceofinstr) {
        super.InstanceOfInstr(instanceofinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) {
        super.InstanceSuperInstr(instancesuperinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void JumpIndirectInstr(JumpIndirectInstr jumpindirectinstr) {
        super.JumpIndirectInstr(jumpindirectinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void JumpInstr(JumpInstr jumpinstr) {
        jvm.method().goTo(jvm.methodData().getLabel(jumpinstr.getJumpTarget()));
    }

    @Override
    public void LabelInstr(LabelInstr labelinstr) {
        jvm.method().mark(jvm.methodData().getLabel(labelinstr.getLabel()));
    }

    @Override
    public void LexicalSearchConstInstr(LexicalSearchConstInstr lexicalsearchconstinstr) {
        super.LexicalSearchConstInstr(lexicalsearchconstinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void LineNumberInstr(LineNumberInstr linenumberinstr) {
        jvm.method().adapter.line(linenumberinstr.getLineNumber());
    }

    @Override
    public void LoadLocalVarInstr(LoadLocalVarInstr loadlocalvarinstr) {
        jvm.method().loadLocal(jvm.methodData().local(DYNAMIC_SCOPE));
        int depth = loadlocalvarinstr.getLocalVar().getScopeDepth();
        // TODO should not have to subtract 1
        int location = loadlocalvarinstr.getLocalVar().getLocation() - 1;
        // TODO if we can avoid loading nil unnecessarily, it could be a big win
        switch (depth) {
            case 0:
                switch (location) {
                    case 0:
                        jvm.method().pushNil();
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "getValueZeroDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        return;
                    case 1:
                        jvm.method().pushNil();
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "getValueOneDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        return;
                    case 2:
                        jvm.method().pushNil();
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "getValueTwoDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        return;
                    case 3:
                        jvm.method().pushNil();
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "getValueThreeDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        return;
                    default:
                        jvm.method().adapter.pushInt(location);
                        jvm.method().pushNil();
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "getValueDepthZeroOrNil", sig(IRubyObject.class, int.class, IRubyObject.class));
                        return;
                }
            default:
                jvm.method().adapter.pushInt(location);
                jvm.method().adapter.pushInt(depth);
                jvm.method().pushNil();
                jvm.method().adapter.invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, int.class, int.class, IRubyObject.class));
        }
    }

    @Override
    public void Match2Instr(Match2Instr match2instr) {
        super.Match2Instr(match2instr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Match3Instr(Match3Instr match3instr) {
        super.Match3Instr(match3instr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MatchInstr(MatchInstr matchinstr) {
        super.MatchInstr(matchinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethodLookupInstr(MethodLookupInstr methodlookupinstr) {
        super.MethodLookupInstr(methodlookupinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) {
        super.ModuleVersionGuardInstr(moduleversionguardinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void NopInstr(NopInstr nopinstr) {
        // do nothing
    }

    @Override
    public void NoResultCallInstr(NoResultCallInstr noResultCallInstr) {
        jvm.method().loadLocal(0);
        visit(noResultCallInstr.getReceiver());
        for (Operand operand : noResultCallInstr.getCallArgs()) {
            visit(operand);
        }

        switch (noResultCallInstr.getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                jvm.method().invokeSelf(noResultCallInstr.getMethodAddr().getName(), noResultCallInstr.getCallArgs().length);
                break;
            case NORMAL:
                jvm.method().invokeOther(noResultCallInstr.getMethodAddr().getName(), noResultCallInstr.getCallArgs().length);
                break;
            case SUPER:
                jvm.method().invokeSuper(noResultCallInstr.getMethodAddr().getName(), noResultCallInstr.getCallArgs().length);
                break;
        }

        jvm.method().adapter.pop();
    }

    @Override
    public void NotInstr(NotInstr notinstr) {
        super.NotInstr(notinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) {
        super.OptArgMultipleAsgnInstr(optargmultipleasgninstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void PopBindingInstr(PopBindingInstr popbindinginstr) {
        // TODO pop
    }

    @Override
    public void ProcessModuleBodyInstr(ProcessModuleBodyInstr processmodulebodyinstr) {
        jvm.method().loadLocal(0);
        visit(processmodulebodyinstr.getModuleBody());
        jvm.method().invokeHelper("invokeModuleBody", IRubyObject.class, ThreadContext.class, CompiledIRMethod.class);
        jvm.method().storeLocal(jvm.methodData().local(processmodulebodyinstr.getResult()));
    }

    @Override
    public void PushBindingInstr(PushBindingInstr pushbindinginstr) {
        jvm.method().loadStaticScope();
        jvm.method().adapter.invokestatic(p(DynamicScope.class), "newDynamicScope", sig(DynamicScope.class, StaticScope.class));
        jvm.method().storeLocal(jvm.methodData().local(DYNAMIC_SCOPE));

        // TODO push
    }

    @Override
    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) {
        super.PutClassVariableInstr(putclassvariableinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void PutConstInstr(PutConstInstr putconstinstr) {
        visit(putconstinstr.getTarget());
        jvm.method().adapter.checkcast(p(RubyModule.class));
        jvm.method().adapter.ldc(putconstinstr.getRef());
        visit(putconstinstr.getValue());
        jvm.method().adapter.invokevirtual(p(RubyModule.class), "setConstant", sig(IRubyObject.class, String.class, IRubyObject.class));
        jvm.method().adapter.pop();
    }

    @Override
    public void PutFieldInstr(PutFieldInstr putfieldinstr) {
        String field = putfieldinstr.getRef();
        visit(putfieldinstr.getTarget());
        visit(putfieldinstr.getValue());
        jvm.method().putField(field);
    }

    @Override
    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) {
        super.PutGlobalVarInstr(putglobalvarinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void RaiseArgumentErrorInstr(RaiseArgumentErrorInstr raiseargumenterrorinstr) {
        super.RaiseArgumentErrorInstr(raiseargumenterrorinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReceiveClosureInstr(ReceiveClosureInstr receiveclosureinstr) {
        jvm.method().loadLocal(jvm.methodData().local("$block"));
        jvm.method().storeLocal(jvm.methodData().local(receiveclosureinstr.getResult()));
    }

    @Override
    public void ReceiveExceptionInstr(ReceiveExceptionInstr receiveexceptioninstr) {
        // TODO implement
    }

    @Override
    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr receiveprereqdarginstr) {
        int index = jvm.methodData().local(receiveprereqdarginstr.getResult());
        jvm.method().loadLocal(3 + receiveprereqdarginstr.getArgIndex());
        jvm.method().storeLocal(index);
    }

    @Override
    public void ReceiveSelfInstr(ReceiveSelfInstr receiveselfinstr) {
        int $selfIndex = jvm.methodData().local(receiveselfinstr.getResult());
        jvm.method().loadLocal(2);
        jvm.method().storeLocal($selfIndex);
    }

    @Override
    public void RecordEndBlockInstr(RecordEndBlockInstr recordendblockinstr) {
        super.RecordEndBlockInstr(recordendblockinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) {
        super.ReqdArgMultipleAsgnInstr(reqdargmultipleasgninstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void RescueEQQInstr(RescueEQQInstr rescueeqqinstr) {
        super.RescueEQQInstr(rescueeqqinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void RestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr restargmultipleasgninstr) {
        super.RestArgMultipleAsgnInstr(restargmultipleasgninstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReturnInstr(ReturnInstr returninstr) {
        visit(returninstr.getReturnValue());
        jvm.method().returnValue();
    }

    @Override
    public void SearchConstInstr(SearchConstInstr searchconstinstr) {
        // TODO: private consts
        jvm.method().loadLocal(0);
        visit(searchconstinstr.getStartingScope());
        jvm.method().searchConst(searchconstinstr.getConstName());
        jvm.method().storeLocal(jvm.methodData().local(searchconstinstr.getResult()));
    }

    @Override
    public void SetReturnAddressInstr(SetReturnAddressInstr setreturnaddressinstr) {
        super.SetReturnAddressInstr(setreturnaddressinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void StoreLocalVarInstr(StoreLocalVarInstr storelocalvarinstr) {
        jvm.method().loadLocal(jvm.methodData().local(DYNAMIC_SCOPE));
        int depth = storelocalvarinstr.getLocalVar().getScopeDepth();
        // TODO should not have to subtract 1
        int location = storelocalvarinstr.getLocalVar().getLocation() - 1;
        switch (depth) {
            case 0:
                switch (location) {
                    case 0:
                        storelocalvarinstr.getValue().visit(this);
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        jvm.method().adapter.pop();
                        return;
                    case 1:
                        storelocalvarinstr.getValue().visit(this);
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "setValueOneDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        jvm.method().adapter.pop();
                        return;
                    case 2:
                        storelocalvarinstr.getValue().visit(this);
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        jvm.method().adapter.pop();
                        return;
                    case 3:
                        storelocalvarinstr.getValue().visit(this);
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        jvm.method().adapter.pop();
                        return;
                    default:
                        storelocalvarinstr.getValue().visit(this);
                        jvm.method().adapter.pushInt(location);
                        jvm.method().adapter.invokevirtual(p(DynamicScope.class), "setValueDepthZero", sig(IRubyObject.class, IRubyObject.class, int.class));
                        jvm.method().adapter.pop();
                        return;
                }
            default:
                jvm.method().adapter.pushInt(depth);
                storelocalvarinstr.getValue().visit(this);
                jvm.method().adapter.pushInt(location);
                jvm.method().adapter.invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, int.class, IRubyObject.class, int.class));
                jvm.method().adapter.pop();
        }
    }

    @Override
    public void ThreadPollInstr(ThreadPollInstr threadpollinstr) {
        jvm.method().poll();
    }

    @Override
    public void ThrowExceptionInstr(ThrowExceptionInstr throwexceptioninstr) {
        // TODO implement
    }

    @Override
    public void ToAryInstr(ToAryInstr toaryinstr) {
        super.ToAryInstr(toaryinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void UndefMethodInstr(UndefMethodInstr undefmethodinstr) {
        super.UndefMethodInstr(undefmethodinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) {
        super.UnresolvedSuperInstr(unresolvedsuperinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void YieldInstr(YieldInstr yieldinstr) {
        visit(yieldinstr.getBlockArg());

        // TODO: proc, nil block logic

        jvm.method().loadLocal(0);
        if (yieldinstr.getYieldArg() == UndefinedValue.UNDEFINED) {
            jvm.method().adapter.invokevirtual(p(Block.class), "yieldSpecific", sig(IRubyObject.class, ThreadContext.class));
        } else {
            visit(yieldinstr.getYieldArg());

            // TODO: if yielding array, call yieldArray

            jvm.method().adapter.invokevirtual(p(Block.class), "yield", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        }

        jvm.method().storeLocal(jvm.methodData().local(yieldinstr.getResult()));
    }

    @Override
    public void ZSuperInstr(ZSuperInstr zsuperinstr) {
        super.ZSuperInstr(zsuperinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // "defined" instructions


    @Override
    public void BackrefIsMatchDataInstr(BackrefIsMatchDataInstr backrefismatchdatainstr) {
        super.BackrefIsMatchDataInstr(backrefismatchdatainstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ClassVarIsDefinedInstr(ClassVarIsDefinedInstr classvarisdefinedinstr) {
        super.ClassVarIsDefinedInstr(classvarisdefinedinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetBackrefInstr(GetBackrefInstr getbackrefinstr) {
        super.GetBackrefInstr(getbackrefinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetDefinedConstantOrMethodInstr(GetDefinedConstantOrMethodInstr getdefinedconstantormethodinstr) {
        super.GetDefinedConstantOrMethodInstr(getdefinedconstantormethodinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetErrorInfoInstr(GetErrorInfoInstr geterrorinfoinstr) {
        super.GetErrorInfoInstr(geterrorinfoinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GlobalIsDefinedInstr(GlobalIsDefinedInstr globalisdefinedinstr) {
        super.GlobalIsDefinedInstr(globalisdefinedinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void HasInstanceVarInstr(HasInstanceVarInstr hasinstancevarinstr) {
        // TODO: This is suboptimal, not caching ivar offset at all
        jvm.method().pushRuntime();
        visit(hasinstancevarinstr.getObject());
        jvm.method().adapter.invokeinterface(p(IRubyObject.class), "getInstanceVariables", sig(InstanceVariables.class));
        jvm.method().adapter.ldc(hasinstancevarinstr.getName().string);
        jvm.method().adapter.invokeinterface(p(InstanceVariables.class), "hasInstanceVariable", sig(boolean.class, String.class));
        jvm.method().adapter.invokevirtual(p(Ruby.class), "newBoolean", sig(RubyBoolean.class, boolean.class));
        jvm.method().storeLocal(jvm.methodData().local(hasinstancevarinstr.getResult()));
    }

    @Override
    public void IsMethodBoundInstr(IsMethodBoundInstr ismethodboundinstr) {
        super.IsMethodBoundInstr(ismethodboundinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethodDefinedInstr(MethodDefinedInstr methoddefinedinstr) {
        super.MethodDefinedInstr(methoddefinedinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethodIsPublicInstr(MethodIsPublicInstr methodispublicinstr) {
        super.MethodIsPublicInstr(methodispublicinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void RestoreErrorInfoInstr(RestoreErrorInfoInstr restoreerrorinfoinstr) {
        super.RestoreErrorInfoInstr(restoreerrorinfoinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void SuperMethodBoundInstr(SuperMethodBoundInstr supermethodboundinstr) {
        super.SuperMethodBoundInstr(supermethodboundinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // ruby 1.8 specific
    @Override
    public void ReceiveOptArgInstr18(ReceiveOptArgInstr18 receiveoptarginstr) {
        super.ReceiveOptArgInstr18(receiveoptarginstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReceiveRestArgInstr18(ReceiveRestArgInstr18 receiverestarginstr) {
        super.ReceiveRestArgInstr18(receiverestarginstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // ruby 1.9 specific
    @Override
    public void BuildLambdaInstr(BuildLambdaInstr buildlambdainstr) {
        super.BuildLambdaInstr(buildlambdainstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetEncodingInstr(GetEncodingInstr getencodinginstr) {
        super.GetEncodingInstr(getencodinginstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReceiveOptArgInstr19(ReceiveOptArgInstr19 receiveoptarginstr) {
        super.ReceiveOptArgInstr19(receiveoptarginstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr receivepostreqdarginstr) {
        super.ReceivePostReqdArgInstr(receivepostreqdarginstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReceiveRestArgInstr19(ReceiveRestArgInstr19 receiverestarginstr) {
        super.ReceiveRestArgInstr19(receiverestarginstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // operands
    @Override
    public void Array(Array array) {
        jvm.method().loadLocal(0);

        for (Operand operand : array.getElts()) {
            visit(operand);
        }

        jvm.method().array(array.getElts().length);
    }

    @Override
    public void AsString(AsString asstring) {
        super.AsString(asstring);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Backref(Backref backref) {
        super.Backref(backref);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void BacktickString(BacktickString backtickstring) {
        super.BacktickString(backtickstring);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Bignum(Bignum bignum) {
        super.Bignum(bignum);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void BooleanLiteral(BooleanLiteral booleanliteral) {
        jvm.method().pushBoolean(booleanliteral.isTrue());
    }

    @Override
    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) {
        super.ClosureLocalVariable(closurelocalvariable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void CompoundArray(CompoundArray compoundarray) {
        super.CompoundArray(compoundarray);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void CompoundString(CompoundString compoundstring) {
        super.CompoundString(compoundstring);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void CurrentScope(CurrentScope currentscope) {
        jvm.method().adapter.aload(1);
    }

    @Override
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) {
        super.DynamicSymbol(dynamicsymbol);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Fixnum(Fixnum fixnum) {
        jvm.method().push(fixnum.getValue());
    }

    @Override
    public void Float(org.jruby.ir.operands.Float flote) {
        super.Float(flote);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GlobalVariable(GlobalVariable globalvariable) {
        super.GlobalVariable(globalvariable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Hash(Hash hash) {
        super.Hash(hash);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void IRException(IRException irexception) {
        super.IRException(irexception);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Label(Label label) {
        super.Label(label);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void LocalVariable(LocalVariable localvariable) {
        super.LocalVariable(localvariable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethAddr(MethAddr methaddr) {
        super.MethAddr(methaddr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethodHandle(MethodHandle methodhandle) {
        super.MethodHandle(methodhandle);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Nil(Nil nil) {
        jvm.method().pushNil();
    }

    @Override
    public void NthRef(NthRef nthref) {
        super.NthRef(nthref);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ObjectClass(ObjectClass objectclass) {
        jvm.method().pushObjectClass();
    }

    @Override
    public void Range(Range range) {
        super.Range(range);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Regexp(Regexp regexp) {
        super.Regexp(regexp);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ScopeModule(ScopeModule scopemodule) {
        jvm.method().adapter.aload(1);
        jvm.method().adapter.invokevirtual(p(StaticScope.class), "getModule", sig(RubyModule.class));
    }

    @Override
    public void Self(Self self) {
        super.Self(self);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Splat(Splat splat) {
        super.Splat(splat);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void StandardError(StandardError standarderror) {
        super.StandardError(standarderror);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void StringLiteral(StringLiteral stringliteral) {
        jvm.method().push(stringliteral.getByteList());
    }

    @Override
    public void SValue(SValue svalue) {
        super.SValue(svalue);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Symbol(Symbol symbol) {
        jvm.method().push(symbol.getName());
    }

    @Override
    public void TemporaryClosureVariable(TemporaryClosureVariable temporaryclosurevariable) {
        super.TemporaryClosureVariable(temporaryclosurevariable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void TemporaryVariable(TemporaryVariable temporaryvariable) {
        jvm.method().loadLocal(jvm.methodData().local(temporaryvariable));
    }

    @Override
    public void UndefinedValue(UndefinedValue undefinedvalue) {
        jvm.method().pushUndefined();
    }

    @Override
    public void UnexecutableNil(UnexecutableNil unexecutablenil) {
        super.UnexecutableNil(unexecutablenil);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) {
        super.WrappedIRClosure(wrappedirclosure);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private final JVM jvm;
    private IRScriptBody script;
}
