package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.Ruby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyFloat;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.CompiledIRMethod;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRFor;
import org.jruby.ir.IRVisitor;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.AliasInstr;
import org.jruby.ir.instructions.AttrAssignInstr;
import org.jruby.ir.instructions.BacktickInstr;
import org.jruby.ir.instructions.BEQInstr;
import org.jruby.ir.instructions.BFalseInstr;
import org.jruby.ir.instructions.BNEInstr;
import org.jruby.ir.instructions.BNilInstr;
import org.jruby.ir.instructions.BTrueInstr;
import org.jruby.ir.instructions.BUndefInstr;
import org.jruby.ir.instructions.BuildCompoundArrayInstr;
import org.jruby.ir.instructions.BlockGivenInstr;
import org.jruby.ir.instructions.BreakInstr;
import org.jruby.ir.instructions.BuildLambdaInstr;
import org.jruby.ir.instructions.CallInstr;
import org.jruby.ir.instructions.CheckArgsArrayArityInstr;
import org.jruby.ir.instructions.CheckArityInstr;
import org.jruby.ir.instructions.ClassSuperInstr;
import org.jruby.ir.instructions.ConstMissingInstr;
import org.jruby.ir.instructions.CopyInstr;
import org.jruby.ir.instructions.DefineClassInstr;
import org.jruby.ir.instructions.DefineClassMethodInstr;
import org.jruby.ir.instructions.DefineInstanceMethodInstr;
import org.jruby.ir.instructions.DefineMetaClassInstr;
import org.jruby.ir.instructions.DefineModuleInstr;
import org.jruby.ir.instructions.EQQInstr;
import org.jruby.ir.instructions.ExceptionRegionEndMarkerInstr;
import org.jruby.ir.instructions.ExceptionRegionStartMarkerInstr;
import org.jruby.ir.instructions.GVarAliasInstr;
import org.jruby.ir.instructions.GetClassVarContainerModuleInstr;
import org.jruby.ir.instructions.GetClassVariableInstr;
import org.jruby.ir.instructions.GetEncodingInstr;
import org.jruby.ir.instructions.GetFieldInstr;
import org.jruby.ir.instructions.GetGlobalVariableInstr;
import org.jruby.ir.instructions.InheritanceSearchConstInstr;
import org.jruby.ir.instructions.InstanceSuperInstr;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.instructions.JumpInstr;
import org.jruby.ir.instructions.LabelInstr;
import org.jruby.ir.instructions.LexicalSearchConstInstr;
import org.jruby.ir.instructions.LineNumberInstr;
import org.jruby.ir.instructions.LoadLocalVarInstr;
import org.jruby.ir.instructions.Match2Instr;
import org.jruby.ir.instructions.Match3Instr;
import org.jruby.ir.instructions.MatchInstr;
import org.jruby.ir.instructions.MethodLookupInstr;
import org.jruby.ir.instructions.ModuleVersionGuardInstr;
import org.jruby.ir.instructions.NoResultCallInstr;
import org.jruby.ir.instructions.NonlocalReturnInstr;
import org.jruby.ir.instructions.NopInstr;
import org.jruby.ir.instructions.OptArgMultipleAsgnInstr;
import org.jruby.ir.instructions.PopBindingInstr;
import org.jruby.ir.instructions.PopFrameInstr;
import org.jruby.ir.instructions.ProcessModuleBodyInstr;
import org.jruby.ir.instructions.PushBindingInstr;
import org.jruby.ir.instructions.PushFrameInstr;
import org.jruby.ir.instructions.PutClassVariableInstr;
import org.jruby.ir.instructions.PutConstInstr;
import org.jruby.ir.instructions.PutFieldInstr;
import org.jruby.ir.instructions.PutGlobalVarInstr;
import org.jruby.ir.instructions.RaiseArgumentErrorInstr;
import org.jruby.ir.instructions.ReceiveClosureInstr;
import org.jruby.ir.instructions.ReceiveJRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveOptArgInstr;
import org.jruby.ir.instructions.ReceivePostReqdArgInstr;
import org.jruby.ir.instructions.ReceivePreReqdArgInstr;
import org.jruby.ir.instructions.ReceiveRestArgInstr;
import org.jruby.ir.instructions.ReceiveRubyExceptionInstr;
import org.jruby.ir.instructions.ReceiveSelfInstr;
import org.jruby.ir.instructions.RecordEndBlockInstr;
import org.jruby.ir.instructions.ReqdArgMultipleAsgnInstr;
import org.jruby.ir.instructions.RescueEQQInstr;
import org.jruby.ir.instructions.RestArgMultipleAsgnInstr;
import org.jruby.ir.instructions.ReturnInstr;
import org.jruby.ir.instructions.RuntimeHelperCall;
import org.jruby.ir.instructions.SearchConstInstr;
import org.jruby.ir.instructions.StoreLocalVarInstr;
import org.jruby.ir.instructions.ThreadPollInstr;
import org.jruby.ir.instructions.ThrowExceptionInstr;
import org.jruby.ir.instructions.ToAryInstr;
import org.jruby.ir.instructions.UndefMethodInstr;
import org.jruby.ir.instructions.UnresolvedSuperInstr;
import org.jruby.ir.instructions.YieldInstr;
import org.jruby.ir.instructions.ZSuperInstr;
import org.jruby.ir.instructions.boxing.AluInstr;
import org.jruby.ir.instructions.boxing.BoxBooleanInstr;
import org.jruby.ir.instructions.boxing.BoxFixnumInstr;
import org.jruby.ir.instructions.boxing.BoxFloatInstr;
import org.jruby.ir.instructions.boxing.UnboxBooleanInstr;
import org.jruby.ir.instructions.boxing.UnboxFixnumInstr;
import org.jruby.ir.instructions.boxing.UnboxFloatInstr;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.MethodHandle;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.instructions.defined.GetDefinedConstantOrMethodInstr;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.MethodDefinedInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.invokedynamic.InvokeDynamicSupport;
import org.jruby.util.ByteList;
import org.jruby.util.ClassDefiningClassLoader;

import java.util.List;
import java.util.Map;
import org.jruby.RubyArray;
import org.jruby.RubyRange;

import static org.jruby.util.CodegenUtils.c;
import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

import org.jruby.util.JavaNameMangler;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import static org.jruby.ir.instructions.RuntimeHelperCall.Methods.*;

/**
 * Implementation of IRCompiler for the JVM.
 */
public class JVMVisitor extends IRVisitor {

    private static final Logger LOG = LoggerFactory.getLogger("JVMVisitor");
    public static final String DYNAMIC_SCOPE = "$dynamicScope";

    public JVMVisitor() {
        this.jvm = new JVM();
    }

    public static Class compile(Ruby ruby, IRScope scope, ClassDefiningClassLoader jrubyClassLoader) {
        // run compiler
        JVMVisitor target = new JVMVisitor();

        target.codegenScope(scope);

//        try {
//            FileOutputStream fos = new FileOutputStream("tmp.class");
//            fos.write(target.code());
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return jrubyClassLoader.defineClass(c(JVM.scriptToClass(scope.getName())), target.code());
    }

    public byte[] code() {
        return jvm.code();
    }

    public void codegenScope(IRScope scope) {
        if (scope instanceof IRScriptBody) {
            codegenScriptBody((IRScriptBody)scope);
        } else if (scope instanceof IRMethod) {
            emitMethodJIT((IRMethod)scope);
        } else if (scope instanceof IRModuleBody) {
            emitModuleBodyJIT((IRModuleBody)scope);
        } else {
            throw new RuntimeException("don't know how to JIT: " + scope);
        }
    }

    public void codegenScriptBody(IRScriptBody script) {
        emitScriptBody(script);
    }

    private void logScope(IRScope scope) {
        StringBuilder b = new StringBuilder();

        b.append("\n\nLinearized instructions for JIT:\n");

        int i = 0;
        for (BasicBlock bb : scope.buildLinearization()) {
            for (Instr instr : bb.getInstrsArray()) {
                if (i > 0) b.append("\n");

                b.append("  ").append(i).append('\t').append(instr);

                i++;
            }
        }

        LOG.info("Starting JVM compilation on scope " + scope);
        LOG.info(b.toString());
    }

    public void emitScope(IRScope scope, String name, Signature signature) {
        this.currentScope = scope;

        List <BasicBlock> bbs = scope.prepareForCompilation();
        Map <BasicBlock, Label> exceptionTable = scope.buildJVMExceptionTable();

        if (Options.IR_COMPILER_DEBUG.load()) logScope(scope);

        emitClosures(scope);

        jvm.pushmethod(name, signature);

        // UGLY hack for blocks and scripts, which still have their scopes pushed before invocation
        // Scope management for blocks and scripts needs to be figured out
        // FIXME: Seems like some methods are not triggering scope loading, so this is always done for now
        if (true ||
                scope instanceof IRClosure || scope instanceof IRScriptBody) {
            jvmMethod().loadContext();
            jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("org.jruby.runtime.DynamicScope getCurrentScope()"));
            jvmStoreLocal(DYNAMIC_SCOPE);
        }

        IRBytecodeAdapter m = jvmMethod();

        int numberOfLabels = bbs.size();
        for (int i = 0; i < numberOfLabels; i++) {
            BasicBlock bb = bbs.get(i);
            org.objectweb.asm.Label start = jvm.methodData().getLabel(bb.getLabel());
            Label rescueLabel = exceptionTable.get(bb);
            org.objectweb.asm.Label end = null;

            m.mark(start);

            boolean newEnd = false;
            if (rescueLabel != null) {
                if (i+1 < numberOfLabels) {
                    end = jvm.methodData().getLabel(bbs.get(i+1).getLabel());
                } else {
                    newEnd = true;
                    end = new org.objectweb.asm.Label();
                }

                org.objectweb.asm.Label rescue = jvm.methodData().getLabel(rescueLabel);
                jvmAdapter().trycatch(start, end, rescue, p(Throwable.class));
            }

            // ensure there's at least one instr per block
            m.adapter.nop();

            // visit remaining instrs
            for (Instr instr : bb.getInstrs()) {
                visit(instr);
            }

            if (newEnd) {
                m.mark(end);
            }
        }

        jvm.popmethod();
    }

    private static final Signature METHOD_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArgs(new String[]{"context", "scope", "self", "args", "block"}, ThreadContext.class, StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class);

    private static final Signature CLOSURE_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArgs(new String[]{"context", "scope", "self", "args", "block", "superName", "type"}, ThreadContext.class, StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class, String.class, Block.Type.class);

    public void emitScriptBody(IRScriptBody script) {
        String clsName = jvm.scriptToClass(script.getName());
        jvm.pushscript(clsName, script.getFileName());

        emitScope(script, "__script__", METHOD_SIGNATURE);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    public Handle emitMethod(IRMethod method) {
        String name = JavaNameMangler.mangleMethodName(method.getName() + "_" + methodIndex++);

        emitScope(method, name, METHOD_SIGNATURE);

        return new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(METHOD_SIGNATURE.type().returnType(), METHOD_SIGNATURE.type().parameterArray()));
    }

    public Handle emitMethodJIT(IRMethod method) {
        String name = JavaNameMangler.mangleMethodName(method.getName() + "_" + methodIndex++);
        String clsName = jvm.scriptToClass(method.getName());
        jvm.pushscript(clsName, method.getFileName());

        emitScope(method, "__script__", METHOD_SIGNATURE);

        Handle handle = new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(METHOD_SIGNATURE.type().returnType(), METHOD_SIGNATURE.type().parameterArray()));

        jvm.cls().visitEnd();
        jvm.popclass();

        return handle;
    }

    public Handle emitModuleBodyJIT(IRModuleBody method) {
        String baseName = method.getName() + "_" + methodIndex++;
        String name;

        if (baseName.indexOf("DUMMY_MC") != -1) {
            name = "METACLASS_" + methodIndex++;
        } else {
            name = baseName + "_" + methodIndex++;
        }
        String clsName = jvm.scriptToClass(method.getName());
        jvm.pushscript(clsName, method.getFileName());

        emitScope(method, "__script__", METHOD_SIGNATURE);

        Handle handle = new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(METHOD_SIGNATURE.type().returnType(), METHOD_SIGNATURE.type().parameterArray()));

        jvm.cls().visitEnd();
        jvm.popclass();

        return handle;
    }

    private void emitClosures(IRScope s) {
        // Emit code for all nested closures
        for (IRClosure c: s.getClosures()) {
            c.setHandle(emitClosure(c));
        }
    }

    public Handle emitClosure(IRClosure closure) {
        /* Compile the closure like a method */
        String name = JavaNameMangler.mangleMethodName(closure.getName() + "__" + closure.getLexicalParent().getName() + "_" + methodIndex++);

        emitScope(closure, name, CLOSURE_SIGNATURE);

        return new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(CLOSURE_SIGNATURE.type().returnType(), CLOSURE_SIGNATURE.type().parameterArray()));
    }

    public Handle emitModuleBody(IRModuleBody method) {
        String baseName = method.getName() + "_" + methodIndex++;
        String name;

        if (baseName.indexOf("DUMMY_MC") != -1) {
            name = "METACLASS_" + methodIndex++;
        } else {
            name = baseName + "_" + methodIndex++;
        }

        emitScope(method, name, METHOD_SIGNATURE);

        return new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(METHOD_SIGNATURE.type().returnType(), METHOD_SIGNATURE.type().parameterArray()));
    }

    public void visit(Instr instr) {
        instr.visit(this);
    }

    public void visit(Operand operand) {
        operand.visit(this);
    }

    private int getJVMLocalVarIndex(Variable variable) {
        if (variable instanceof TemporaryLocalVariable) {
            switch (((TemporaryLocalVariable)variable).getType()) {
            case FLOAT: return jvm.methodData().local(variable, JVM.DOUBLE_TYPE);
            case FIXNUM: return jvm.methodData().local(variable, JVM.LONG_TYPE);
            case BOOLEAN: return jvm.methodData().local(variable, JVM.BOOLEAN_TYPE);
            default: return jvm.methodData().local(variable);
            }
        } else {
            return jvm.methodData().local(variable);
        }
    }

    private int getJVMLocalVarIndex(String specialVar) {
        return jvm.methodData().local(specialVar);
    }

    private org.objectweb.asm.Label getJVMLabel(Label label) {
        return jvm.methodData().getLabel(label);
    }

    private void jvmStoreLocal(Variable variable) {
        if (variable instanceof TemporaryLocalVariable) {
            switch (((TemporaryLocalVariable)variable).getType()) {
            case FLOAT: jvmAdapter().dstore(getJVMLocalVarIndex(variable)); break;
            case FIXNUM: jvmAdapter().lstore(getJVMLocalVarIndex(variable)); break;
            case BOOLEAN: jvmAdapter().istore(getJVMLocalVarIndex(variable)); break;
            default: jvmMethod().storeLocal(getJVMLocalVarIndex(variable)); break;
            }
        } else {
            jvmMethod().storeLocal(getJVMLocalVarIndex(variable));
        }
    }

    private void jvmStoreLocal(String specialVar) {
        jvmMethod().storeLocal(getJVMLocalVarIndex(specialVar));
    }

    private void jvmLoadLocal(Variable variable) {
        if (variable instanceof TemporaryLocalVariable) {
            switch (((TemporaryLocalVariable)variable).getType()) {
            case FLOAT: jvmAdapter().dload(getJVMLocalVarIndex(variable)); break;
            case FIXNUM: jvmAdapter().lload(getJVMLocalVarIndex(variable)); break;
            case BOOLEAN: jvmAdapter().iload(getJVMLocalVarIndex(variable)); break;
            default: jvmMethod().loadLocal(getJVMLocalVarIndex(variable)); break;
            }
        } else {
            jvmMethod().loadLocal(getJVMLocalVarIndex(variable));
        }
    }

    private void jvmLoadLocal(String specialVar) {
        jvmMethod().loadLocal(getJVMLocalVarIndex(specialVar));
    }

    // JVM maintains a stack of ClassData (for nested classes being compiled)
    // Each class maintains a stack of MethodData (for methods being compiled in the class)
    // MethodData wraps a IRBytecodeAdapter which wraps a SkinnyMethodAdapter which has a ASM MethodVisitor which emits bytecode
    // A long chain of indirection: JVM -> MethodData -> IRBytecodeAdapter -> SkinnyMethodAdapter -> ASM.MethodVisitor
    // In some places, methods reference JVM -> MethodData -> IRBytecodeAdapter (via jvm.method()) and ask it to walk the last 2 links
    // In other places, methods reference JVM -> MethodData -> IRBytecodeAdapter -> SkinnyMethodAdapter (via jvm.method().adapter) and ask it to walk the last link
    // Can this be cleaned up to either (a) get rid of IRBytecodeAdapter OR (b) implement passthru' methods for SkinnyMethodAdapter methods (like the others it implements)?

    @Override
    public void AliasInstr(AliasInstr aliasInstr) {
        IRBytecodeAdapter m = jvmMethod();
        m.loadContext();
        visit(aliasInstr.getReceiver());
        m.adapter.ldc(((StringLiteral) aliasInstr.getNewName()).string);
        m.adapter.ldc(((StringLiteral)aliasInstr.getOldName()).string);
        m.invokeIRHelper("defineAlias", sig(void.class, ThreadContext.class, IRubyObject.class, String.class, String.class));
    }

    @Override
    public void AttrAssignInstr(AttrAssignInstr attrAssignInstr) {
        jvmMethod().loadLocal(0);
        jvmMethod().loadSelf();
        visit(attrAssignInstr.getReceiver());
        for (Operand operand : attrAssignInstr.getCallArgs()) {
            visit(operand);
        }

        // FIXME: This should probably live in IR somewhere
        if (attrAssignInstr.getReceiver() instanceof Self) {
            jvmMethod().invokeSelf(attrAssignInstr.getMethodAddr().getName(), attrAssignInstr.getCallArgs().length, false);
        } else {
            jvmMethod().invokeOther(attrAssignInstr.getMethodAddr().getName(), attrAssignInstr.getCallArgs().length, false);
        }
        jvmAdapter().pop();
    }

    @Override
    public void BEQInstr(BEQInstr beqInstr) {
        jvmMethod().loadLocal(0);
        visit(beqInstr.getArg1());
        visit(beqInstr.getArg2());
        jvmMethod().invokeHelper("BEQ", boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        jvmAdapter().iftrue(getJVMLabel(beqInstr.getJumpTarget()));
    }

    @Override
    public void BFalseInstr(BFalseInstr bFalseInstr) {
        Operand arg1 = bFalseInstr.getArg1();
        visit(arg1);
        // this is a gross hack because we don't have distinction in boolean instrs between boxed and unboxed
        if (!(arg1 instanceof TemporaryBooleanVariable) && !(arg1 instanceof UnboxedBoolean)) {
            // unbox
            jvmAdapter().invokeinterface(p(IRubyObject.class), "isTrue", sig(boolean.class));
        }
        jvmMethod().bfalse(getJVMLabel(bFalseInstr.getJumpTarget()));
    }

    @Override
    public void BlockGivenInstr(BlockGivenInstr blockGivenInstr) {
        jvmMethod().loadRuntime();
        visit(blockGivenInstr.getBlockArg());
        jvmMethod().invokeVirtual(Type.getType(Block.class), Method.getMethod("boolean isGiven()"));
        jvmMethod().invokeVirtual(Type.getType(Ruby.class), Method.getMethod("org.jruby.RubyBoolean newBoolean(boolean)"));
        jvmStoreLocal(blockGivenInstr.getResult());
    }

    private void loadFloatArg(Operand arg) {
        if (arg instanceof Variable) {
            visit(arg);
        } else {
            double val;
            if (arg instanceof Float) {
                val = ((Float)arg).value;
            } else if (arg instanceof Fixnum) {
                val = (double)((Fixnum)arg).value;
            } else {
                // Should not happen -- so, forcing an exception.
                throw new RuntimeException("Non-float/fixnum in loadFloatArg!" + arg);
            }
            jvmAdapter().ldc(val);
        }
    }

    private void loadFixnumArg(Operand arg) {
        if (arg instanceof Variable) {
            visit(arg);
        } else {
            long val;
            if (arg instanceof Float) {
                val = (long)((Float)arg).value;
            } else if (arg instanceof Fixnum) {
                val = ((Fixnum)arg).value;
            } else {
                // Should not happen -- so, forcing an exception.
                throw new RuntimeException("Non-float/fixnum in loadFixnumArg!" + arg);
            }
            jvmAdapter().ldc(val);
        }
    }

    private void loadBooleanArg(Operand arg) {
        if (arg instanceof Variable) {
            visit(arg);
        } else {
            boolean val;
            if (arg instanceof UnboxedBoolean) {
                val = ((UnboxedBoolean)arg).isTrue();
            } else {
                // Should not happen -- so, forcing an exception.
                throw new RuntimeException("Non-float/fixnum in loadFixnumArg!" + arg);
            }
            jvmAdapter().ldc(val);
        }
    }

    @Override
    public void BoxFloatInstr(BoxFloatInstr instr) {
        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // Load runtime
        m.loadContext();
        a.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));

        // Get unboxed float
        loadFloatArg(instr.getValue());

        // Box the float
        a.invokevirtual(p(Ruby.class), "newFloat", sig(RubyFloat.class, double.class));

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BoxFixnumInstr(BoxFixnumInstr instr) {
        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // Load runtime
        m.loadContext();
        a.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));

        // Get unboxed fixnum
        loadFixnumArg(instr.getValue());

        // Box the fixnum
        a.invokevirtual(p(Ruby.class), "newFixnum", sig(RubyFixnum.class, long.class));

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BoxBooleanInstr(BoxBooleanInstr instr) {
        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // Load runtime
        m.loadContext();
        a.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));

        // Get unboxed boolean
        loadBooleanArg(instr.getValue());

        // Box the fixnum
        a.invokevirtual(p(Ruby.class), "newBoolean", sig(RubyBoolean.class, boolean.class));

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void UnboxFloatInstr(UnboxFloatInstr instr) {
        // Load boxed value
        visit(instr.getValue());

        // Unbox it
        jvmMethod().invokeIRHelper("unboxFloat", sig(double.class, IRubyObject.class));

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void UnboxFixnumInstr(UnboxFixnumInstr instr) {
        // Load boxed value
        visit(instr.getValue());

        // Unbox it
        jvmMethod().invokeIRHelper("unboxFixnum", sig(long.class, IRubyObject.class));

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void UnboxBooleanInstr(UnboxBooleanInstr instr) {
        // Load boxed value
        visit(instr.getValue());

        // Unbox it
        jvmMethod().invokeIRHelper("unboxBoolean", sig(boolean.class, IRubyObject.class));

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    public void AluInstr(AluInstr instr) {
        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // Load args
        visit(instr.getArg1());
        visit(instr.getArg2());

        // Compute result
        switch (instr.getOperation()) {
            case FADD: a.dadd(); break;
            case FSUB: a.dsub(); break;
            case FMUL: a.dmul(); break;
            case FDIV: a.ddiv(); break;
            case FLT: m.invokeIRHelper("flt", sig(boolean.class, double.class, double.class)); break; // annoying to have to do it in a method
            case FGT: m.invokeIRHelper("fgt", sig(boolean.class, double.class, double.class)); break; // annoying to have to do it in a method
            case FEQ: m.invokeIRHelper("feq", sig(boolean.class, double.class, double.class)); break; // annoying to have to do it in a method
            case IADD: a.ladd(); break;
            case ISUB: a.lsub(); break;
            case IMUL: a.lmul(); break;
            case IDIV: a.ldiv(); break;
            case ILT: m.invokeIRHelper("ilt", sig(boolean.class, long.class, long.class)); break; // annoying to have to do it in a method
            case IGT: m.invokeIRHelper("igt", sig(boolean.class, long.class, long.class)); break; // annoying to have to do it in a method
            case IOR: a.lor(); break;
            case IAND: a.land(); break;
            case IXOR: a.lxor(); break;
            case ISHL: a.lshl(); break;
            case ISHR: a.lshr(); break;
            case IEQ: m.invokeIRHelper("ilt", sig(boolean.class, long.class, long.class)); break; // annoying to have to do it in a method
            default: throw new RuntimeException("UNHANDLED!");
        }

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BacktickInstr(BacktickInstr instr) {
        super.BacktickInstr(instr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void BNEInstr(BNEInstr bneinstr) {
        jvmMethod().loadLocal(0);
        visit(bneinstr.getArg1());
        visit(bneinstr.getArg2());
        jvmMethod().invokeHelper("BNE", boolean.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        jvmAdapter().iftrue(getJVMLabel(bneinstr.getJumpTarget()));
    }

    @Override
    public void BNilInstr(BNilInstr bnilinstr) {
        visit(bnilinstr.getArg1());
        jvmMethod().isNil();
        jvmMethod().btrue(getJVMLabel(bnilinstr.getJumpTarget()));
    }

    @Override
    public void BreakInstr(BreakInstr breakInstr) {
        super.BreakInstr(breakInstr);
        // this is all wrong
//        IRBytecodeAdapter   m = jvm.method();
//        SkinnyMethodAdapter a = m.adapter;
//        m.loadLocal(0); // context
//        a.aload(1); // current scope
//        // FIXME: This can also be done in the helper itself
//        m.invokeVirtual(Type.getType(IRScope.class), Method.getMethod("org.jruby.ir.IRScope getIRScope()"));
//        a.ldc(breakInstr.getScopeIdToReturnTo().getScopeId());
//        visit(breakInstr.getReturnValue());
//        // FIXME: emit block-type for the scope that is currently executing
//        // For now, it is null
//        m.pushNil();
//        a.invokestatic(p(IRubyObject.class), "initiateBreak", sig(ThreadContext.class, IRScope.class, IRScope.class, IRubyObject.class, Block.Type.class));
    }

    @Override
    public void BTrueInstr(BTrueInstr btrueinstr) {
        Operand arg1 = btrueinstr.getArg1();
        visit(arg1);
        // this is a gross hack because we don't have distinction in boolean instrs between boxed and unboxed
        if (!(arg1 instanceof TemporaryBooleanVariable) && !(arg1 instanceof UnboxedBoolean)) {
            jvmMethod().isTrue();
        }
        jvmMethod().btrue(getJVMLabel(btrueinstr.getJumpTarget()));
    }

    @Override
    public void BUndefInstr(BUndefInstr bundefinstr) {
        visit(bundefinstr.getArg1());
        jvmMethod().pushUndefined();
        jvmAdapter().if_acmpeq(getJVMLabel(bundefinstr.getJumpTarget()));
    }

    @Override
    public void BuildCompoundArrayInstr(BuildCompoundArrayInstr instr) {
        visit(instr.getAppendingArg());
        if (instr.isArgsPush()) jvmAdapter().checkcast("org/jruby/RubyArray");
        visit(instr.getAppendedArg());
        if (instr.isArgsPush()) {
            jvmMethod().invokeHelper("argsPush", RubyArray.class, RubyArray.class, IRubyObject.class);
        } else {
            jvmMethod().invokeHelper("argsCat", RubyArray.class, IRubyObject.class, IRubyObject.class);
        }
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void CallInstr(CallInstr callInstr) {
        IRBytecodeAdapter m = jvmMethod();
        String name = callInstr.getMethodAddr().getName();
        Operand[] args = callInstr.getCallArgs();
        int numArgs = args.length;


        m.loadLocal(0); // tc
        m.loadLocal(2); // caller
        visit(callInstr.getReceiver());

        boolean isSplat = false;

        if (numArgs == 1 && args[0] instanceof Splat) {
            visit(args[0]);
            numArgs = -1;
        } else {
            for (Operand operand : args) {
                visit(operand);
            }
        }

        Operand closure = callInstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        if (hasClosure) {
            jvmMethod().loadContext();
            visit(closure);
            jvmMethod().invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        }

        switch (callInstr.getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                m.invokeSelf(name, numArgs, hasClosure);
                break;
            case NORMAL:
                m.invokeOther(name, numArgs, hasClosure);
                break;
        }

        jvmStoreLocal(callInstr.getResult());
    }

    @Override
    public void CheckArgsArrayArityInstr(CheckArgsArrayArityInstr checkargsarrayarityinstr) {
        jvmMethod().loadContext();
        visit(checkargsarrayarityinstr.getArgsArray());
        jvmAdapter().pushInt(checkargsarrayarityinstr.required);
        jvmAdapter().pushInt(checkargsarrayarityinstr.opt);
        jvmAdapter().pushInt(checkargsarrayarityinstr.rest);
        jvmMethod().invokeStatic(Type.getType(Helpers.class), Method.getMethod("void irCheckArgsArrayArity(org.jruby.runtime.ThreadContext, org.jruby.RubyArray, int, int, int)"));
    }

    @Override
    public void CheckArityInstr(CheckArityInstr checkarityinstr) {
        jvmMethod().loadContext();
        jvmMethod().loadArgs();
        jvmAdapter().ldc(checkarityinstr.required);
        jvmAdapter().ldc(checkarityinstr.opt);
        jvmAdapter().ldc(checkarityinstr.rest);
        jvmAdapter().ldc(checkarityinstr.receivesKeywords);
        jvmAdapter().ldc(checkarityinstr.restKey);
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "checkArity", sig(void.class, ThreadContext.class, Object[].class, int.class, int.class, int.class, boolean.class, int.class));
    }

    @Override
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) {
        // disable for now
        super.ClassSuperInstr(classsuperinstr);

        IRBytecodeAdapter m = jvmMethod();
        String name = classsuperinstr.getMethodAddr().getName();
        Operand[] args = classsuperinstr.getCallArgs();

        m.loadContext();
        m.loadSelf();
        visit(classsuperinstr.getDefiningModule());

        for (int i = 0; i < args.length; i++) {
            Operand operand = args[i];
            visit(operand);
        }

        Operand closure = classsuperinstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        if (hasClosure) {
            m.loadContext();
            visit(closure);
            m.invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        } else {
            m.adapter.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
        }

        m.invokeClassSuper(name, args.length, hasClosure);

        jvmStoreLocal(classsuperinstr.getResult());
    }

    @Override
    public void ConstMissingInstr(ConstMissingInstr constmissinginstr) {
        visit(constmissinginstr.getReceiver());
        jvmAdapter().checkcast("org/jruby/RubyModule");
        jvmMethod().loadContext();
        jvmAdapter().ldc("const_missing");
        jvmMethod().pushSymbol(constmissinginstr.getMissingConst());
        jvmMethod().invokeVirtual(Type.getType(RubyModule.class), Method.getMethod("org.jruby.runtime.builtin.IRubyObject callMethod(org.jruby.runtime.ThreadContext, java.lang.String, org.jruby.runtime.builtin.IRubyObject)"));
    }

    @Override
    public void CopyInstr(CopyInstr copyinstr) {
        Operand  src = copyinstr.getSource();
        Variable res = copyinstr.getResult();
        if (res instanceof TemporaryFloatVariable) {
            loadFloatArg(src);
        } else if (res instanceof TemporaryFixnumVariable) {
            loadFixnumArg(src);
        } else {
            visit(src);
        }
        jvmStoreLocal(res);
    }

    @Override
    public void DefineClassInstr(DefineClassInstr defineclassinstr) {
        IRClassBody newIRClassBody = defineclassinstr.getNewIRClassBody();
        StaticScope scope = newIRClassBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = Helpers.encodeScope(scope);

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // new CompiledIRMethod
        a.newobj(p(CompiledIRMethod.class));
        a.dup();

        // emit method body and get handle
        a.ldc(emitModuleBody(newIRClassBody)); // handle

        // add'l args for CompiledIRMethod constructor
        a.ldc(newIRClassBody.getName());
        a.ldc(newIRClassBody.getFileName());
        a.ldc(newIRClassBody.getLineNumber());

        // construct class with Helpers.newClassForIR
        a.aload(0); // ThreadContext
        a.ldc(newIRClassBody.getName()); // class name
        m.loadLocal(2); // self

        // create class
        m.loadLocal(0);
        visit(defineclassinstr.getContainer());
        m.invokeHelper("checkIsRubyModule", RubyModule.class, ThreadContext.class, Object.class);

        // superclass
        if (defineclassinstr.getSuperClass() instanceof Nil) {
            a.aconst_null();
        } else {
            visit(defineclassinstr.getSuperClass());
        }

        // is meta?
        a.ldc(newIRClassBody instanceof IRMetaClassBody);

        m.invokeHelper("newClassForIR", RubyClass.class, ThreadContext.class, String.class, IRubyObject.class, RubyModule.class, Object.class, boolean.class);

        // static scope
        a.aload(0);
        a.aload(1);
        a.ldc(scopeString);
        a.invokestatic(p(Helpers.class), "decodeScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");
        a.swap();

        // set into StaticScope
        a.dup2();
        a.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

        a.getstatic(p(Visibility.class), "PUBLIC", ci(Visibility.class));
        a.swap();

        // no arguments
        a.ldc("");

        // invoke constructor
        a.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;Ljava/lang/String;)V");

        // store
        jvmStoreLocal(defineclassinstr.getResult());
    }

    @Override
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) {
        IRMethod method = defineclassmethodinstr.getMethod();
        StaticScope scope = method.getStaticScope();

        String scopeString = Helpers.encodeScope(scope);

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;
        List<String[]> parameters = method.getArgDesc();

        a.aload(0); // ThreadContext
        visit(defineclassmethodinstr.getContainer());
        jvmMethod().pushHandle(emitMethod(method)); // handle
        a.ldc(method.getName());
        a.aload(1);
        a.ldc(scopeString);
        a.ldc(method.getFileName());
        a.ldc(method.getLineNumber());
        a.ldc(Helpers.encodeParameterList(parameters));

        // add method
        a.invokestatic(p(IRRuntimeHelpers.class), "defCompiledIRClassMethod",
                sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, java.lang.invoke.MethodHandle.class, String.class,
                        StaticScope.class, String.class, String.class, int.class, String.class));

        a.pop();
    }

    @Override
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) {
        IRMethod method = defineinstancemethodinstr.getMethod();
        StaticScope scope = method.getStaticScope();

        String scopeString = Helpers.encodeScope(scope);

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;
        List<String[]> parameters = method.getArgDesc();

        a.aload(0); // ThreadContext
        jvmMethod().pushHandle(emitMethod(method)); // handle
        a.ldc(method.getName());
        a.aload(1);
        a.ldc(scopeString);
        a.ldc(method.getFileName());
        a.ldc(method.getLineNumber());
        a.ldc(Helpers.encodeParameterList(parameters));

        // add method
        a.invokestatic(p(IRRuntimeHelpers.class), "defCompiledIRMethod",
                sig(IRubyObject.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, String.class,
                        StaticScope.class, String.class, String.class, int.class, String.class));

        a.pop();
    }

    @Override
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) {
        IRModuleBody metaClassBody = definemetaclassinstr.getMetaClassBody();
        StaticScope scope = metaClassBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = Helpers.encodeScope(scope);

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // new CompiledIRMethod
        a.newobj(p(CompiledIRMethod.class));
        a.dup();

        // emit method body and get handle
        a.ldc(emitModuleBody(metaClassBody)); // handle

        // add'l args for CompiledIRMethod constructor
        a.ldc(metaClassBody.getName());
        a.ldc(metaClassBody.getFileName());
        a.ldc(metaClassBody.getLineNumber());

        //// static scope
        a.aload(0);
        a.aload(1);
        a.ldc(scopeString);
        a.invokestatic(p(Helpers.class), "decodeScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        // get singleton class
        m.loadRuntime();
        visit(definemetaclassinstr.getObject());
        m.invokeHelper("getSingletonClass", RubyClass.class, Ruby.class, IRubyObject.class);

        // set into StaticScope
        a.dup2();
        a.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

        a.getstatic(p(Visibility.class), "PUBLIC", ci(Visibility.class));
        a.swap();

        // no arguments
        a.ldc("");

        // invoke constructor
        a.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;Ljava/lang/String;)V");

        // store
        jvmStoreLocal(definemetaclassinstr.getResult());
    }

    @Override
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) {
        IRModuleBody newIRModuleBody = definemoduleinstr.getNewIRModuleBody();
        StaticScope scope = newIRModuleBody.getStaticScope();
        if (scope.getRequiredArgs() > 3 || scope.getRestArg() >= 0 || scope.getOptionalArgs() != 0) {
            throw new RuntimeException("can't compile variable method: " + this);
        }

        String scopeString = Helpers.encodeScope(scope);

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        // new CompiledIRMethod
        a.newobj(p(CompiledIRMethod.class));
        a.dup();

        // emit method body and get handle
        a.ldc(emitModuleBody(newIRModuleBody)); // handle

        // emit method body and get handle
        emitModuleBody(newIRModuleBody); // handle

        // add'l args for CompiledIRMethod constructor
        a.ldc(newIRModuleBody.getName());
        a.ldc(newIRModuleBody.getFileName());
        a.ldc(newIRModuleBody.getLineNumber());

        a.aload(0);
        a.aload(1);
        a.ldc(scopeString);
        a.invokestatic(p(Helpers.class), "decodeScope", "(Lorg/jruby/runtime/ThreadContext;Lorg/jruby/parser/StaticScope;Ljava/lang/String;)Lorg/jruby/parser/StaticScope;");

        // create module
        m.loadLocal(0);
        visit(definemoduleinstr.getContainer());
        m.invokeHelper("checkIsRubyModule", RubyModule.class, ThreadContext.class, Object.class);
        a.ldc(newIRModuleBody.getName());
        a.invokevirtual(p(RubyModule.class), "defineOrGetModuleUnder", sig(RubyModule.class, String.class));

        // set into StaticScope
        a.dup2();
        a.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

        a.getstatic(p(Visibility.class), "PUBLIC", ci(Visibility.class));
        a.swap();

        // no arguments
        a.ldc("");

        // invoke constructor
        a.invokespecial(p(CompiledIRMethod.class), "<init>", "(Ljava/lang/invoke/MethodHandle;Ljava/lang/String;Ljava/lang/String;ILorg/jruby/parser/StaticScope;Lorg/jruby/runtime/Visibility;Lorg/jruby/RubyModule;Ljava/lang/String;)V");

        // store
        jvmStoreLocal(definemoduleinstr.getResult());
    }

    @Override
    public void EQQInstr(EQQInstr eqqinstr) {
        jvmMethod().loadContext();
        visit(eqqinstr.getArg1());
        visit(eqqinstr.getArg2());
        jvmMethod().invokeIRHelper("isEQQ", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
        jvmStoreLocal(eqqinstr.getResult());
    }

    @Override
    public void ExceptionRegionEndMarkerInstr(ExceptionRegionEndMarkerInstr exceptionregionendmarkerinstr) {
        throw new RuntimeException("Marker instructions shouldn't reach compiler: " + exceptionregionendmarkerinstr);
    }

    @Override
    public void ExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) {
        throw new RuntimeException("Marker instructions shouldn't reach compiler: " + exceptionregionstartmarkerinstr);
    }

    @Override
    public void GetClassVarContainerModuleInstr(GetClassVarContainerModuleInstr getclassvarcontainermoduleinstr) {
        jvmMethod().loadContext();
        visit(getclassvarcontainermoduleinstr.getStartingScope());
        if (getclassvarcontainermoduleinstr.getObject() != null) {
            visit(getclassvarcontainermoduleinstr.getObject());
        } else {
            jvmAdapter().aconst_null();
        }
        jvmMethod().invokeIRHelper("getModuleFromScope", sig(RubyModule.class, ThreadContext.class, StaticScope.class, IRubyObject.class));
        jvmStoreLocal(getclassvarcontainermoduleinstr.getResult());
    }

    @Override
    public void GetClassVariableInstr(GetClassVariableInstr getclassvariableinstr) {
        visit(getclassvariableinstr.getSource());
        jvmAdapter().checkcast(p(RubyModule.class));
        jvmAdapter().ldc(getclassvariableinstr.getRef());
        jvmAdapter().invokevirtual(p(RubyModule.class), "getClassVar", sig(IRubyObject.class, String.class));
        jvmStoreLocal(getclassvariableinstr.getResult());
    }

    @Override
    public void GetFieldInstr(GetFieldInstr getfieldinstr) {
        visit(getfieldinstr.getSource());
        jvmMethod().getField(getfieldinstr.getRef());
        jvmStoreLocal(getfieldinstr.getResult());
    }

    @Override
    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) {
        Operand source = getglobalvariableinstr.getSource();
        GlobalVariable gvar = (GlobalVariable)source;
        String name = gvar.getName();
        jvmMethod().loadRuntime();
        jvmMethod().invokeVirtual(Type.getType(Ruby.class), Method.getMethod("org.jruby.internal.runtime.GlobalVariables getGlobalVariables()"));
        jvmAdapter().ldc(name);
        jvmMethod().invokeVirtual(Type.getType(GlobalVariables.class), Method.getMethod("org.jruby.runtime.builtin.IRubyObject get(String)"));
        jvmStoreLocal(getglobalvariableinstr.getResult());
    }

    @Override
    public void GVarAliasInstr(GVarAliasInstr gvaraliasinstr) {
        jvmMethod().loadRuntime();
        jvmAdapter().invokevirtual(p(Ruby.class), "getGlobalVariables", sig(GlobalVariables.class));
        visit(gvaraliasinstr.getNewName());
        jvmAdapter().invokevirtual(p(Object.class), "toString", sig(String.class));
        visit(gvaraliasinstr.getOldName());
        jvmAdapter().invokevirtual(p(Object.class), "toString", sig(String.class));
        jvmAdapter().invokevirtual(p(GlobalVariables.class), "alias", sig(void.class, String.class, String.class));
    }

    @Override
    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) {
        jvmMethod().loadLocal(0);
        visit(inheritancesearchconstinstr.getCurrentModule());

        jvmMethod().inheritanceSearchConst(inheritancesearchconstinstr.getConstName(), inheritancesearchconstinstr.isNoPrivateConsts());
        jvmStoreLocal(inheritancesearchconstinstr.getResult());
    }

    @Override
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) {
        // disable for now
        super.InstanceSuperInstr(instancesuperinstr);

        IRBytecodeAdapter m = jvmMethod();
        String name = instancesuperinstr.getMethodAddr().getName();
        Operand[] args = instancesuperinstr.getCallArgs();

        m.loadContext();
        m.loadSelf();
        visit(instancesuperinstr.getDefiningModule());

        for (int i = 0; i < args.length; i++) {
            Operand operand = args[i];
            visit(operand);
        }

        Operand closure = instancesuperinstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        if (hasClosure) {
            m.loadContext();
            visit(closure);
            m.invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        } else {
            m.adapter.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
        }

        m.invokeInstanceSuper(name, args.length, hasClosure);

        jvmStoreLocal(instancesuperinstr.getResult());
    }

    @Override
    public void JumpInstr(JumpInstr jumpinstr) {
        jvmMethod().goTo(getJVMLabel(jumpinstr.getJumpTarget()));
    }

    @Override
    public void LabelInstr(LabelInstr labelinstr) {
    }

    @Override
    public void LexicalSearchConstInstr(LexicalSearchConstInstr lexicalsearchconstinstr) {
        super.LexicalSearchConstInstr(lexicalsearchconstinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void LineNumberInstr(LineNumberInstr linenumberinstr) {
        jvmAdapter().line(linenumberinstr.getLineNumber());
    }

    @Override
    public void LoadLocalVarInstr(LoadLocalVarInstr loadlocalvarinstr) {
        IRBytecodeAdapter m = jvmMethod();
        jvmLoadLocal(DYNAMIC_SCOPE);
        int depth = loadlocalvarinstr.getLocalVar().getScopeDepth();
        int location = loadlocalvarinstr.getLocalVar().getLocation();
        // TODO if we can avoid loading nil unnecessarily, it could be a big win
        OUTER: switch (depth) {
            case 0:
                switch (location) {
                    case 0:
                        m.pushNil();
                        m.adapter.invokevirtual(p(DynamicScope.class), "getValueZeroDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        break OUTER;
                    case 1:
                        m.pushNil();
                        m.adapter.invokevirtual(p(DynamicScope.class), "getValueOneDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        break OUTER;
                    case 2:
                        m.pushNil();
                        m.adapter.invokevirtual(p(DynamicScope.class), "getValueTwoDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        break OUTER;
                    case 3:
                        m.pushNil();
                        m.adapter.invokevirtual(p(DynamicScope.class), "getValueThreeDepthZeroOrNil", sig(IRubyObject.class, IRubyObject.class));
                        break OUTER;
                    default:
                        m.adapter.pushInt(location);
                        m.pushNil();
                        m.adapter.invokevirtual(p(DynamicScope.class), "getValueDepthZeroOrNil", sig(IRubyObject.class, int.class, IRubyObject.class));
                        break OUTER;
                }
            default:
                m.adapter.pushInt(location);
                m.adapter.pushInt(depth);
                m.pushNil();
                m.adapter.invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, int.class, int.class, IRubyObject.class));
        }
        jvmStoreLocal(loadlocalvarinstr.getResult());
    }

    @Override
    public void Match2Instr(Match2Instr match2instr) {
        visit(match2instr.getReceiver());
        jvmMethod().loadContext();
        visit(match2instr.getArg());
        jvmAdapter().invokevirtual(p(RubyRegexp.class), "op_match19", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        jvmStoreLocal(match2instr.getResult());
    }

    @Override
    public void Match3Instr(Match3Instr match3instr) {
        jvmMethod().loadContext();
        visit(match3instr.getReceiver());
        visit(match3instr.getArg());
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "match3", sig(IRubyObject.class, ThreadContext.class, RubyRegexp.class, IRubyObject.class));
        jvmStoreLocal(match3instr.getResult());
    }

    @Override
    public void MatchInstr(MatchInstr matchinstr) {
        visit(matchinstr.getReceiver());
        jvmMethod().loadContext();
        jvmAdapter().invokevirtual(p(RubyRegexp.class), "op_match2_19", sig(IRubyObject.class, ThreadContext.class));
        jvmStoreLocal(matchinstr.getResult());
    }

    @Override
    public void MethodLookupInstr(MethodLookupInstr methodlookupinstr) {
        // SSS FIXME: Unused at this time
        throw new RuntimeException("Unsupported instruction: " + methodlookupinstr);
    }

    @Override
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) {
        // SSS FIXME: Unused at this time
        throw new RuntimeException("Unsupported instruction: " + moduleversionguardinstr);
    }

    @Override
    public void NopInstr(NopInstr nopinstr) {
        // do nothing
    }

    @Override
    public void NoResultCallInstr(NoResultCallInstr noResultCallInstr) {
        IRBytecodeAdapter m = jvmMethod();
        m.loadLocal(0);
        m.loadSelf(); // caller
        visit(noResultCallInstr.getReceiver());
        for (Operand operand : noResultCallInstr.getCallArgs()) {
            visit(operand);
        }

        Operand closure = noResultCallInstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        if (closure != null) {
            jvmMethod().loadContext();
            visit(closure);
            jvmMethod().invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        }

        switch (noResultCallInstr.getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                m.invokeSelf(noResultCallInstr.getMethodAddr().getName(), noResultCallInstr.getCallArgs().length, hasClosure);
                break;
            case NORMAL:
                m.invokeOther(noResultCallInstr.getMethodAddr().getName(), noResultCallInstr.getCallArgs().length, hasClosure);
                break;
        }

        m.adapter.pop();
    }

    @Override
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) {
        visit(optargmultipleasgninstr.getArrayArg());
        jvmAdapter().ldc(optargmultipleasgninstr.getMinArgsLength());
        jvmAdapter().ldc(optargmultipleasgninstr.getIndex());
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "extractOptionalArgument", sig(IRubyObject.class, RubyArray.class, int.class, int.class));
        jvmStoreLocal(optargmultipleasgninstr.getResult());
    }

    @Override
    public void PopBindingInstr(PopBindingInstr popbindinginstr) {
        jvmMethod().loadContext();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void popScope()"));
    }

    @Override
    public void PopFrameInstr(PopFrameInstr popframeinstr) {
        jvmMethod().loadContext();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void postMethodFrameOnly()"));
    }

    @Override
    public void ProcessModuleBodyInstr(ProcessModuleBodyInstr processmodulebodyinstr) {
        jvmMethod().loadLocal(0);
        visit(processmodulebodyinstr.getModuleBody());
        jvmMethod().invokeHelper("invokeModuleBody", IRubyObject.class, ThreadContext.class, CompiledIRMethod.class);
        jvmStoreLocal(processmodulebodyinstr.getResult());
    }

    @Override
    public void PushBindingInstr(PushBindingInstr pushbindinginstr) {
        jvmMethod().loadContext();
        jvmMethod().loadStaticScope();
        jvmAdapter().invokestatic(p(DynamicScope.class), "newDynamicScope", sig(DynamicScope.class, StaticScope.class));
        jvmAdapter().dup();
        jvmStoreLocal(DYNAMIC_SCOPE);
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void pushScope(org.jruby.runtime.DynamicScope)"));
    }

    @Override
    public void PushFrameInstr(PushFrameInstr pushframeinstr) {
        jvmMethod().loadContext();
        jvmAdapter().ldc(pushframeinstr.getFrameName().getName());
        jvmMethod().loadSelf();
        jvmMethod().loadLocal(4);
        jvmMethod().loadStaticScope();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void preMethodFrameAndClass(String, org.jruby.runtime.builtin.IRubyObject, org.jruby.runtime.Block, org.jruby.parser.StaticScope)"));
    }

    @Override
    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) {
        visit(putclassvariableinstr.getValue());
        visit(putclassvariableinstr.getTarget());

        // don't understand this logic; duplicated from interpreter
        if (putclassvariableinstr.getValue() instanceof CurrentScope) {
            jvmAdapter().pop2();
            return;
        }

        // hmm.
        jvmAdapter().checkcast(p(RubyModule.class));
        jvmAdapter().swap();
        jvmAdapter().ldc(putclassvariableinstr.getRef());
        jvmAdapter().swap();
        jvmAdapter().invokevirtual(p(RubyModule.class), "setClassVar", sig(IRubyObject.class, String.class, IRubyObject.class));
        jvmAdapter().pop();
    }

    @Override
    public void PutConstInstr(PutConstInstr putconstinstr) {
        IRBytecodeAdapter m = jvmMethod();
        visit(putconstinstr.getTarget());
        m.adapter.checkcast(p(RubyModule.class));
        m.adapter.ldc(putconstinstr.getRef());
        visit(putconstinstr.getValue());
        m.adapter.invokevirtual(p(RubyModule.class), "setConstant", sig(IRubyObject.class, String.class, IRubyObject.class));
        m.adapter.pop();
    }

    @Override
    public void PutFieldInstr(PutFieldInstr putfieldinstr) {
        visit(putfieldinstr.getTarget());
        visit(putfieldinstr.getValue());
        jvmMethod().putField(putfieldinstr.getRef());
    }

    @Override
    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) {
        GlobalVariable target = (GlobalVariable)putglobalvarinstr.getTarget();
        String name = target.getName();
        jvmMethod().loadRuntime();
        jvmMethod().invokeVirtual(Type.getType(Ruby.class), Method.getMethod("org.jruby.internal.runtime.GlobalVariables getGlobalVariables()"));
        jvmAdapter().ldc(name);
        visit(putglobalvarinstr.getValue());
        jvmMethod().invokeVirtual(Type.getType(GlobalVariables.class), Method.getMethod("org.jruby.runtime.builtin.IRubyObject set(String, org.jruby.runtime.builtin.IRubyObject)"));
        // leaves copy of value on stack
        jvmAdapter().pop();
    }

    @Override
    public void RaiseArgumentErrorInstr(RaiseArgumentErrorInstr raiseargumenterrorinstr) {
        super.RaiseArgumentErrorInstr(raiseargumenterrorinstr);
    }

    @Override
    public void ReceiveClosureInstr(ReceiveClosureInstr receiveclosureinstr) {
        jvmMethod().loadRuntime();
        jvmLoadLocal("$block");
        jvmMethod().invokeIRHelper("newProc", sig(IRubyObject.class, Ruby.class, Block.class));
        jvmStoreLocal(receiveclosureinstr.getResult());
    }

    @Override
    public void ReceiveRubyExceptionInstr(ReceiveRubyExceptionInstr receiveexceptioninstr) {
        // exception should be on stack from try/catch, so unwrap and store it
        jvmStoreLocal(receiveexceptioninstr.getResult());
    }

    @Override
    public void ReceiveJRubyExceptionInstr(ReceiveJRubyExceptionInstr receiveexceptioninstr) {
        // exception should be on stack from try/catch, so just store it
        jvmStoreLocal(receiveexceptioninstr.getResult());
    }

    @Override
    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr instr) {
        int index = getJVMLocalVarIndex(instr.getResult());
        jvmMethod().loadLocal(3); // index of arg array
        jvmAdapter().ldc(instr.getArgIndex());
        jvmAdapter().aaload();
        jvmMethod().storeLocal(index);
    }

    @Override
    public void ReceiveOptArgInstr(ReceiveOptArgInstr instr) {
        // FIXME: Only works when args is in an array rather than being flattened out
        // FIXME: Missing kwargs 2.0 support (kwArgHashCount value)
        jvmAdapter().pushInt(instr.getArgIndex() + instr.requiredArgs); // MIN reqd args
        jvmAdapter().pushInt(instr.getArgIndex() + instr.preArgs); // args array offset
        jvmAdapter().aload(3); // index of arg array
        jvmMethod().invokeHelper("irLoadOptArg", IRubyObject.class, int.class, int.class, IRubyObject[].class);
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr instr) {
        // FIXME: Only works when args is in an array rather than being flattened out
        // FIXME: Missing kwargs 2.0 support (kwArgHashCount value)
        jvmMethod().loadContext();
        jvmAdapter().pushInt(instr.getArgIndex());
        jvmAdapter().pushInt(instr.preReqdArgsCount);
        jvmAdapter().pushInt(instr.postReqdArgsCount);
        jvmAdapter().aload(3); // index of arg array
        jvmMethod().invokeHelper("irLoadPostReqdArg", IRubyObject.class, int.class, int.class, int.class, IRubyObject[].class);
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceiveRestArgInstr(ReceiveRestArgInstr instr) {
        // FIXME: Only works when args is in an array rather than being flattened out
        // FIXME: Missing kwargs 2.0 support (kwArgHashCount value)
        jvmMethod().loadContext();
        jvmAdapter().pushInt(instr.required); // MIN reqd args
        jvmAdapter().pushInt(instr.getArgIndex()); // args array offset
        jvmAdapter().aload(3); // index of arg array
        jvmMethod().invokeHelper("irLoadRestArg", IRubyObject.class, ThreadContext.class, int.class, int.class, IRubyObject[].class);
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceiveSelfInstr(ReceiveSelfInstr receiveselfinstr) {
        jvmMethod().loadSelf();
        jvmStoreLocal(receiveselfinstr.getResult());
    }

    @Override
    public void RecordEndBlockInstr(RecordEndBlockInstr recordendblockinstr) {
        super.RecordEndBlockInstr(recordendblockinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) {
        jvmMethod().loadContext();
        visit(reqdargmultipleasgninstr.getArrayArg());
        jvmAdapter().checkcast("org/jruby/RubyArray");
        jvmAdapter().pushInt(reqdargmultipleasgninstr.getPreArgsCount());
        jvmAdapter().pushInt(reqdargmultipleasgninstr.getIndex());
        jvmAdapter().pushInt(reqdargmultipleasgninstr.getPostArgsCount());
        jvmMethod().invokeHelper("irReqdArgMultipleAsgn", IRubyObject.class, ThreadContext.class, RubyArray.class, int.class, int.class, int.class);
        jvmStoreLocal(reqdargmultipleasgninstr.getResult());
    }

    @Override
    public void RescueEQQInstr(RescueEQQInstr rescueeqqinstr) {
        jvmMethod().loadContext();
        visit(rescueeqqinstr.getArg1());
        visit(rescueeqqinstr.getArg2());
        jvmMethod().invokeIRHelper("isExceptionHandled", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, Object.class));
        jvmStoreLocal(rescueeqqinstr.getResult());
    }

    @Override
    public void RestArgMultipleAsgnInstr(RestArgMultipleAsgnInstr restargmultipleasgninstr) {
        super.RestArgMultipleAsgnInstr(restargmultipleasgninstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void RuntimeHelperCall(RuntimeHelperCall runtimehelpercall) {
        switch (runtimehelpercall.getHelperMethod()) {
            case HANDLE_PROPAGATE_BREAK:
                // disabled
                super.RuntimeHelperCall(runtimehelpercall);
                return;
//                return IRRuntimeHelpers.handlePropagatedBreak(context, scope,
//                        args[0].retrieve(context, self, currDynScope, temp), blockType);
            case HANDLE_NONLOCAL_RETURN:
                // disabled
                super.RuntimeHelperCall(runtimehelpercall);
                return;
//                return IRRuntimeHelpers.handleNonlocalReturn(scope,
//                        args[0].retrieve(context, self, currDynScope, temp), blockType);
            case HANDLE_BREAK_AND_RETURNS_IN_LAMBDA:
                // disabled
                super.RuntimeHelperCall(runtimehelpercall);
                return;
//                return IRRuntimeHelpers.handleBreakAndReturnsInLambdas(context, scope,
//                        args[0].retrieve(context, self, currDynScope, temp), blockType);


            case IS_DEFINED_BACKREF:
                jvmMethod().loadContext();
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedBackref", sig(IRubyObject.class, ThreadContext.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_NTH_REF:
                jvmMethod().loadContext();
                jvmAdapter().ldc((int)((Fixnum)runtimehelpercall.getArgs()[0]).getValue());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedNthRef", sig(IRubyObject.class, ThreadContext.class, int.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_GLOBAL:
                jvmMethod().loadContext();
                jvmAdapter().ldc(((StringLiteral)runtimehelpercall.getArgs()[0]).getString());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedGlobal", sig(IRubyObject.class, ThreadContext.class, String.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_INSTANCE_VAR:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((StringLiteral)runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedInstanceVar", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_CLASS_VAR:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().checkcast(p(RubyModule.class));
                jvmAdapter().ldc(((StringLiteral)runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedClassVar", sig(IRubyObject.class, ThreadContext.class, RubyModule.class, String.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_SUPER:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((StringLiteral)runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedSuper", sig(IRubyObject.class, ThreadContext.class, String.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_METHOD:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((StringLiteral)runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().ldc(((Boolean)runtimehelpercall.getArgs()[2]).isTrue());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedMethod", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class, boolean.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            default:
                throw new RuntimeException("Unknown IR runtime helper method: " + runtimehelpercall.getHelperMethod() + "; INSTR: " + this);
        }
    }

    @Override
    public void NonlocalReturnInstr(NonlocalReturnInstr returninstr) {
        // disable for now
        super.NonlocalReturnInstr(returninstr);

        if (this.currentScope instanceof IRClosure) {
            /* generate run-time call to check non-local-return, errors, etc */
            SkinnyMethodAdapter a = jvmAdapter();
            a.aload(0); // 1. ThreadContext
            a.aload(1); // 2. current scope
            // 3. ref. to returnInstr.methodIdToReturnFrom
            visit(returninstr.getReturnValue()); // 4. return value
            // boolean about whether we are in a closure or not
            // call to handle non-local return
        } else {
            /* throw IR-return-jump */
        }
    }

    @Override
    public void ReturnInstr(ReturnInstr returninstr) {
        visit(returninstr.getReturnValue());
        jvmMethod().returnValue();
    }

    @Override
    public void SearchConstInstr(SearchConstInstr searchconstinstr) {
        jvmMethod().loadLocal(0);
        visit(searchconstinstr.getStartingScope());
        jvmMethod().searchConst(searchconstinstr.getConstName(), searchconstinstr.isNoPrivateConsts());
        jvmStoreLocal(searchconstinstr.getResult());
    }

    @Override
    public void StoreLocalVarInstr(StoreLocalVarInstr storelocalvarinstr) {
        IRBytecodeAdapter m = jvmMethod();
        jvmLoadLocal(DYNAMIC_SCOPE);
        int depth = storelocalvarinstr.getLocalVar().getScopeDepth();
        int location = storelocalvarinstr.getLocalVar().getLocation();
        Operand storeValue = storelocalvarinstr.getValue();
        switch (depth) {
            case 0:
                switch (location) {
                    case 0:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        m.adapter.pop();
                        return;
                    case 1:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueOneDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        m.adapter.pop();
                        return;
                    case 2:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        m.adapter.pop();
                        return;
                    case 3:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZero", sig(IRubyObject.class, IRubyObject.class));
                        m.adapter.pop();
                        return;
                    default:
                        storeValue.visit(this);
                        m.adapter.pushInt(location);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueDepthZero", sig(IRubyObject.class, IRubyObject.class, int.class));
                        m.adapter.pop();
                        return;
                }
            default:
                m.adapter.pushInt(location);
                storeValue.visit(this);
                m.adapter.pushInt(depth);
                m.adapter.invokevirtual(p(DynamicScope.class), "setValue", sig(IRubyObject.class, int.class, IRubyObject.class, int.class));
                m.adapter.pop();
        }
    }

    @Override
    public void ThreadPollInstr(ThreadPollInstr threadpollinstr) {
        jvmMethod().loadContext();
        jvmAdapter().invokedynamic(
                "checkpoint",
                sig(void.class, ThreadContext.class),
                InvokeDynamicSupport.checkpointHandle());
    }

    @Override
    public void ThrowExceptionInstr(ThrowExceptionInstr throwexceptioninstr) {
        visit(throwexceptioninstr.getExceptionArg());
        jvmAdapter().athrow();
    }

    @Override
    public void ToAryInstr(ToAryInstr toaryinstr) {
        jvmMethod().loadContext();
        visit(toaryinstr.getArrayArg());
        jvmMethod().invokeHelper("irToAry", IRubyObject.class, ThreadContext.class, IRubyObject.class);
        jvmStoreLocal(toaryinstr.getResult());
    }

    @Override
    public void UndefMethodInstr(UndefMethodInstr undefmethodinstr) {
        jvmMethod().loadContext();
        visit(undefmethodinstr.getMethodName());
        jvmAdapter().invokestatic(p(Helpers.class), "undefMethod", sig(IRubyObject.class, ThreadContext.class, Object.class));
        jvmStoreLocal(undefmethodinstr.getResult());
    }

    @Override
    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) {
        // disable for now
        super.UnresolvedSuperInstr(unresolvedsuperinstr);

        IRBytecodeAdapter m = jvmMethod();
        m.loadLocal(0); // tc
        m.loadSelf();

        if (unresolvedsuperinstr.getCallArgs().length > 0) {
            for (Operand operand : unresolvedsuperinstr.getCallArgs()) {
                visit(operand);
            }
            m.objectArray(unresolvedsuperinstr.getCallArgs().length);
        } else {
            m.adapter.getstatic(p(IRubyObject.class), "NULL_ARRAY", ci(IRubyObject[].class));
        }

        Operand closure = unresolvedsuperinstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        if (hasClosure) {
            jvmMethod().loadContext();
            visit(closure);
            jvmMethod().invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        }

        m.adapter.invokestatic(p(IRRuntimeHelpers.class), "unresolvedSuper", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject[].class, Block.class));

        jvmStoreLocal(unresolvedsuperinstr.getResult());
    }

    @Override
    public void YieldInstr(YieldInstr yieldinstr) {
        jvmMethod().loadLocal(0);
        visit(yieldinstr.getBlockArg());

        if (yieldinstr.getYieldArg() == UndefinedValue.UNDEFINED) {
            jvmMethod().invokeIRHelper("yieldSpecific", sig(IRubyObject.class, ThreadContext.class, Object.class));
        } else {
            visit(yieldinstr.getYieldArg());
            jvmAdapter().ldc(yieldinstr.isUnwrapArray());
            jvmMethod().invokeIRHelper("yield", sig(IRubyObject.class, ThreadContext.class, Object.class, Object.class, boolean.class));
        }

        jvmStoreLocal(yieldinstr.getResult());
    }

    @Override
    public void ZSuperInstr(ZSuperInstr zsuperinstr) {
        super.ZSuperInstr(zsuperinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    // "defined" instructions

    @Override
    public void GetDefinedConstantOrMethodInstr(GetDefinedConstantOrMethodInstr getdefinedconstantormethodinstr) {
        super.GetDefinedConstantOrMethodInstr(getdefinedconstantormethodinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void GetErrorInfoInstr(GetErrorInfoInstr geterrorinfoinstr) {
        super.GetErrorInfoInstr(geterrorinfoinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethodDefinedInstr(MethodDefinedInstr methoddefinedinstr) {
        super.MethodDefinedInstr(methoddefinedinstr);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void RestoreErrorInfoInstr(RestoreErrorInfoInstr restoreerrorinfoinstr) {
        super.RestoreErrorInfoInstr(restoreerrorinfoinstr);    //To change body of overridden methods use File | Settings | File Templates.
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

    // operands
    @Override
    public void Array(Array array) {
        jvmMethod().loadLocal(0);

        for (Operand operand : array.getElts()) {
            visit(operand);
        }

        jvmMethod().array(array.getElts().length);
    }

    @Override
    public void AsString(AsString asstring) {
        visit(asstring.getSource());
        jvmAdapter().invokeinterface(p(IRubyObject.class), "asString", sig(RubyString.class));
    }

    @Override
    public void Backref(Backref backref) {
        super.Backref(backref);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Bignum(Bignum bignum) {
        super.Bignum(bignum);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Boolean(org.jruby.ir.operands.Boolean booleanliteral) {
        jvmMethod().pushBoolean(booleanliteral.isTrue());
    }

    @Override
    public void UnboxedBoolean(org.jruby.ir.operands.UnboxedBoolean bool) {
        jvmAdapter().ldc(bool.isTrue());
    }

    @Override
    public void ClosureLocalVariable(ClosureLocalVariable closurelocalvariable) {
        super.ClosureLocalVariable(closurelocalvariable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void CompoundString(CompoundString compoundstring) {
        ByteList csByteList = new ByteList();
        csByteList.setEncoding(compoundstring.getEncoding());
        jvmMethod().pushString(csByteList);
        for (Operand p : compoundstring.getPieces()) {
            if ((p instanceof StringLiteral) && (compoundstring.isSameEncoding((StringLiteral)p))) {
                jvmAdapter().dup();
                jvmAdapter().invokevirtual(p(RubyString.class), "getByteList", sig(ByteList.class));
                jvmMethod().pushByteList(((StringLiteral)p).bytelist);
                jvmAdapter().invokevirtual(p(ByteList.class), "append", sig(void.class, ByteList.class));
            } else {
                visit(p);
                jvmAdapter().invokevirtual(p(RubyString.class), "append19", sig(RubyString.class, IRubyObject.class));
            }
        }
    }

    @Override
    public void CurrentScope(CurrentScope currentscope) {
        jvmAdapter().aload(1);
    }

    @Override
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) {
        jvmMethod().loadRuntime();
        visit(dynamicsymbol.getSymbolName());
        jvmAdapter().invokeinterface(p(IRubyObject.class), "asJavaString", sig(String.class));
        jvmAdapter().invokevirtual(p(Ruby.class), "newSymbol", sig(RubySymbol.class, String.class));
    }

    @Override
    public void Fixnum(Fixnum fixnum) {
        jvmMethod().pushFixnum(fixnum.getValue());
    }

    @Override
    public void UnboxedFixnum(UnboxedFixnum fixnum) {
        jvmAdapter().ldc(fixnum.getValue());
    }

    @Override
    public void Float(org.jruby.ir.operands.Float flote) {
        jvmMethod().pushFloat(flote.getValue());
    }

    @Override
    public void UnboxedFloat(org.jruby.ir.operands.UnboxedFloat flote) {
        jvmAdapter().ldc(flote.getValue());
    }

    @Override
    public void GlobalVariable(GlobalVariable globalvariable) {
        super.GlobalVariable(globalvariable);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Hash(Hash hash) {
        jvmMethod().loadContext();
        for (KeyValuePair pair: hash.getPairs()) {
            visit(pair.getKey());
            visit(pair.getValue());
        }
        jvmMethod().hash(hash.getPairs().size());
    }

    @Override
    public void IRException(IRException irexception) {
        super.IRException(irexception);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void MethAddr(MethAddr methaddr) {
        jvmAdapter().ldc(methaddr.getName());
    }

    @Override
    public void MethodHandle(MethodHandle methodhandle) {
        // SSS FIXME: Unused at this time
        throw new RuntimeException("Unsupported operand: " + methodhandle);
    }

    @Override
    public void Nil(Nil nil) {
        jvmMethod().pushNil();
    }

    @Override
    public void NthRef(NthRef nthref) {
        jvmMethod().loadContext();
        jvmAdapter().pushInt(nthref.matchNumber);
        jvmMethod().invokeIRHelper("nthMatch", sig(IRubyObject.class, ThreadContext.class, int.class));
    }

    @Override
    public void ObjectClass(ObjectClass objectclass) {
        jvmMethod().pushObjectClass();
    }

    @Override
    public void Range(Range range) {
        jvmMethod().loadRuntime();
        jvmMethod().loadContext();
        visit(range.getBegin());
        visit(range.getEnd());
        jvmAdapter().ldc(range.isExclusive());
        jvmAdapter().invokestatic(p(RubyRange.class), "newRange", sig(RubyRange.class, Ruby.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, boolean.class));
    }

    @Override
    public void Regexp(Regexp regexp) {
        if (!regexp.hasKnownValue() && !regexp.options.isOnce()) {
            if (regexp.getRegexp() instanceof CompoundString) {
                // FIXME: I don't like this custom logic for building CompoundString bits a different way :-\
                jvmMethod().loadRuntime();
                { // negotiate RubyString pattern from parts
                    jvmMethod().loadRuntime();
                    { // build RubyString[]
                        List<Operand> operands = ((CompoundString)regexp.getRegexp()).getPieces();
                        jvmAdapter().ldc(operands.size());
                        jvmAdapter().anewarray(p(RubyString.class));
                        for (int i = 0; i < operands.size(); i++) {
                            Operand operand = operands.get(i);
                            jvmAdapter().dup();
                            jvmAdapter().ldc(i);
                            visit(operand);
                            jvmAdapter().aastore();
                        }
                    }
                    jvmAdapter().ldc(regexp.options.toEmbeddedOptions());
                    jvmAdapter().invokestatic(p(RubyRegexp.class), "preprocessDRegexp", sig(RubyString.class, Ruby.class, RubyString[].class, int.class));
                }
                jvmAdapter().ldc(regexp.options.toEmbeddedOptions());
                jvmAdapter().invokestatic(p(RubyRegexp.class), "newDRegexp", sig(RubyRegexp.class, Ruby.class, RubyString.class, int.class));
            } else {
                jvmMethod().loadRuntime();
                visit(regexp.getRegexp());
                jvmAdapter().invokevirtual(p(RubyString.class), "getByteList", sig(ByteList.class));
                jvmAdapter().ldc(regexp.options.toEmbeddedOptions());
                jvmAdapter().invokestatic(p(RubyRegexp.class), "newRegexp", sig(RubyRegexp.class, Ruby.class, RubyString.class, int.class));
            }
            jvmAdapter().dup();
            jvmAdapter().invokevirtual(p(RubyRegexp.class), "setLiteral", sig(void.class));
        } else {
            // FIXME: need to check this on cached path
            // context.runtime.getKCode() != rubyRegexp.getKCode()) {
            jvmMethod().loadContext();
            visit(regexp.getRegexp());
            jvmMethod().pushRegexp(regexp.options.toEmbeddedOptions());
        }
    }

    @Override
    public void ScopeModule(ScopeModule scopemodule) {
        jvmAdapter().aload(1);
        jvmAdapter().invokevirtual(p(StaticScope.class), "getModule", sig(RubyModule.class));
    }

    @Override
    public void Self(Self self) {
        // %self is in JVM-local-2 always
        jvmMethod().loadLocal(2);
    }

    @Override
    public void Splat(Splat splat) {
        jvmMethod().loadContext();
        visit(splat.getArray());
        jvmMethod().invokeHelper("irSplat", RubyArray.class, ThreadContext.class, IRubyObject.class);
        jvmAdapter().invokevirtual(p(RubyArray.class), "toJavaArrayMaybeUnsafe", sig(IRubyObject[].class));
    }

    @Override
    public void StandardError(StandardError standarderror) {
        jvmMethod().loadRuntime();
        jvmAdapter().invokevirtual(p(Ruby.class), "getStandardError", sig(RubyClass.class));
    }

    @Override
    public void StringLiteral(StringLiteral stringliteral) {
        jvmMethod().pushString(stringliteral.getByteList());
    }

    @Override
    public void SValue(SValue svalue) {
        super.SValue(svalue);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void Symbol(Symbol symbol) {
        jvmMethod().pushSymbol(symbol.getName());
    }

    @Override
    public void TemporaryVariable(TemporaryVariable temporaryvariable) {
        jvmLoadLocal(temporaryvariable);
    }

    @Override
    public void TemporaryLocalVariable(TemporaryLocalVariable temporarylocalvariable) {
        jvmLoadLocal(temporarylocalvariable);
    }

    @Override
    public void TemporaryFloatVariable(TemporaryFloatVariable temporaryfloatvariable) {
        jvmLoadLocal(temporaryfloatvariable);
    }

    @Override
    public void TemporaryFixnumVariable(TemporaryFixnumVariable temporaryfixnumvariable) {
        jvmLoadLocal(temporaryfixnumvariable);
    }

    @Override
    public void TemporaryBooleanVariable(TemporaryBooleanVariable temporarybooleanvariable) {
        jvmLoadLocal(temporarybooleanvariable);
    }

    @Override
    public void UndefinedValue(UndefinedValue undefinedvalue) {
        jvmMethod().pushUndefined();
    }

    @Override
    public void UnexecutableNil(UnexecutableNil unexecutablenil) {
        throw new RuntimeException(this.getClass().getSimpleName() + " should never be directly executed!");
    }

    @Override
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) {
        IRClosure closure = wrappedirclosure.getClosure();

        jvmAdapter().newobj(p(Block.class));
        jvmAdapter().dup();

        { // prepare block body (should be cached
            jvmAdapter().newobj(p(CompiledIRBlockBody.class));
            jvmAdapter().dup();

            // FIXME: This is inefficient because it's creating a new StaticScope every time
            String encodedScope = Helpers.encodeScope(closure.getStaticScope());
            jvmMethod().loadContext();
            jvmMethod().loadStaticScope();
            jvmAdapter().ldc(encodedScope);
            jvmAdapter().invokestatic(p(Helpers.class), "decodeScopeAndDetermineModule", sig(StaticScope.class, ThreadContext.class, StaticScope.class, String.class));

            jvmAdapter().ldc(Helpers.stringJoin(",", closure.getParameterList()));

            jvmAdapter().ldc(closure.getFileName());

            jvmAdapter().ldc(closure.getLineNumber());

            jvmAdapter().ldc(closure instanceof IRFor || closure.isBeginEndBlock());

            jvmAdapter().ldc(closure.getHandle());

            jvmAdapter().ldc(closure.getArity().getValue());

            jvmAdapter().invokespecial(p(CompiledIRBlockBody.class), "<init>", sig(void.class, StaticScope.class, String.class, String.class, int.class, boolean.class, java.lang.invoke.MethodHandle.class, int.class));
        }

        { // prepare binding
            jvmMethod().loadContext();
            visit(closure.getSelf());
            jvmLoadLocal(DYNAMIC_SCOPE);
            jvmAdapter().invokevirtual(p(ThreadContext.class), "currentBinding", sig(Binding.class, IRubyObject.class, DynamicScope.class));
        }

        jvmAdapter().invokespecial(p(Block.class), "<init>", sig(void.class, BlockBody.class, Binding.class));
    }

    private SkinnyMethodAdapter jvmAdapter() {
        return jvmMethod().adapter;
    }

    private IRBytecodeAdapter jvmMethod() {
        return jvm.method();
    }

    private final JVM jvm;
    private IRScope currentScope;
    private int methodIndex = 0;
}
