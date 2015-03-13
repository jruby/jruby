package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.*;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.*;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.boxing.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneFloatArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneOperandArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.ZeroOperandArgNoBlockCallInstr;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.GlobalVariable;
import org.jruby.ir.operands.Label;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.jruby.util.CodegenUtils.*;

/**
 * Implementation of IRCompiler for the JVM.
 */
public class JVMVisitor extends IRVisitor {

    private static final Logger LOG = LoggerFactory.getLogger("JVMVisitor");
    public static final String DYNAMIC_SCOPE = "$dynamicScope";
    private static final boolean DEBUG = false;

    public JVMVisitor() {
        this.jvm = Options.COMPILE_INVOKEDYNAMIC.load() ? new JVM7() : new JVM6();
        this.methodIndex = 0;
        this.scopeMap = new HashMap();
    }

    public Class compile(IRScope scope, ClassDefiningClassLoader jrubyClassLoader) {
        return defineFromBytecode(scope, compileToBytecode(scope), jrubyClassLoader);
    }

    public byte[] compileToBytecode(IRScope scope) {
        codegenScope(scope);

//        try {
//            FileOutputStream fos = new FileOutputStream("tmp.class");
//            fos.write(target.code());
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return code();
    }

    public Class defineFromBytecode(IRScope scope, byte[] code, ClassDefiningClassLoader jrubyClassLoader) {
        Class result = jrubyClassLoader.defineClass(c(JVM.scriptToClass(scope.getFileName())), code);

        for (Map.Entry<String, IRScope> entry : scopeMap.entrySet()) {
            try {
                result.getField(entry.getKey()).set(null, entry.getValue());
            } catch (Exception e) {
                throw new NotCompilableException(e);
            }
        }

        return result;
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
            throw new NotCompilableException("don't know how to JIT: " + scope);
        }
    }

    public void codegenScriptBody(IRScriptBody script) {
        emitScriptBody(script);
    }

    private void logScope(IRScope scope) {
        LOG.info("Starting JVM compilation on scope " + scope);
        LOG.info("\n\nLinearized instructions for JIT:\n" + scope.toStringInstrs());
    }

    public void emitScope(IRScope scope, String name, Signature signature, boolean specificArity) {
        BasicBlock[] bbs = scope.prepareForInitialCompilation();

        Map <BasicBlock, Label> exceptionTable = scope.buildJVMExceptionTable();

        if (Options.IR_COMPILER_DEBUG.load()) logScope(scope);

        emitClosures(scope);

        jvm.pushmethod(name, scope, signature, specificArity);

        // store IRScope in map for insertion into class later
        String scopeField = name + "_IRScope";
        if (scopeMap.get(scopeField) == null) {
            scopeMap.put(scopeField, scope);
            jvm.cls().visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE, scopeField, ci(IRScope.class), null, null).visitEnd();
        }

        // Some scopes (closures, module/class bodies) do not have explicit call protocol yet.
        // Unconditionally load current dynamic scope for those bodies.
        if (!scope.hasExplicitCallProtocol()) {
            jvmMethod().loadContext();
            jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("org.jruby.runtime.DynamicScope getCurrentScope()"));
            jvmStoreLocal(DYNAMIC_SCOPE);
        }

        IRBytecodeAdapter m = jvmMethod();

        int numberOfBasicBlocks = bbs.length;
        int ipc = 0; // synthetic, used for debug traces that show which instr failed
        for (int i = 0; i < numberOfBasicBlocks; i++) {
            BasicBlock bb = bbs[i];
            org.objectweb.asm.Label start = jvm.methodData().getLabel(bb.getLabel());
            Label rescueLabel = exceptionTable.get(bb);
            org.objectweb.asm.Label end = null;

            m.mark(start);

            boolean newEnd = false;
            if (rescueLabel != null) {
                if (i+1 < numberOfBasicBlocks) {
                    end = jvm.methodData().getLabel(bbs[i+1].getLabel());
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
                if (DEBUG) instr.setIPC(ipc++); // debug mode uses instr offset for backtrace
                visit(instr);
            }

            if (newEnd) {
                m.mark(end);
            }
        }

        jvm.popmethod();
    }

    private static final Signature METHOD_SIGNATURE_BASE = Signature
            .returning(IRubyObject.class)
            .appendArgs(new String[]{"context", "scope", "self", "block", "class", "callName"}, ThreadContext.class, StaticScope.class, IRubyObject.class, Block.class, RubyModule.class, String.class);

    public static final Signature signatureFor(IRScope method, boolean aritySplit) {
        if (aritySplit) {
            StaticScope argScope = method.getStaticScope();
            if (argScope.isArgumentScope() &&
                    argScope.getOptionalArgs() == 0 &&
                    argScope.getRestArg() == -1 &&
                    !method.receivesKeywordArgs()) {
                // we have only required arguments...emit a signature appropriate to that arity
                String[] args = new String[argScope.getRequiredArgs()];
                Class[] types = Helpers.arrayOf(Class.class, args.length, IRubyObject.class);
                for (int i = 0; i < args.length; i++) {
                    args[i] = "arg" + i;
                }
                return METHOD_SIGNATURE_BASE.insertArgs(3, args, types);
            }
            // we can't do an specific-arity signature
            return null;
        }

        // normal boxed arg list signature
        return METHOD_SIGNATURE_BASE.insertArgs(3, new String[]{"args"}, IRubyObject[].class);
    }

    private static final Signature CLOSURE_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArgs(new String[]{"context", "scope", "self", "args", "block", "superName", "type"}, ThreadContext.class, StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class, String.class, Block.Type.class);

    public void emitScriptBody(IRScriptBody script) {
        // Note: no index attached because there should be at most one script body per .class
        String name = JavaNameMangler.encodeScopeForBacktrace(script);
        String clsName = jvm.scriptToClass(script.getFileName());
        jvm.pushscript(clsName, script.getFileName());

        emitScope(script, name, signatureFor(script, false), false);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    public void emitMethod(IRMethod method) {
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + "$" + methodIndex++;

        emitWithSignatures(method, name);
    }

    public void  emitMethodJIT(IRMethod method) {
        String clsName = jvm.scriptToClass(method.getFileName());
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + "$" + methodIndex++;
        jvm.pushscript(clsName, method.getFileName());

        emitWithSignatures(method, name);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    private void emitWithSignatures(IRMethod method, String name) {
        method.setJittedName(name);

        Signature signature = signatureFor(method, false);
        emitScope(method, name, signature, false);
        method.addNativeSignature(-1, signature.type());

        Signature specificSig = signatureFor(method, true);
        if (specificSig != null) {
            emitScope(method, name, specificSig, true);
            method.addNativeSignature(method.getStaticScope().getRequiredArgs(), specificSig.type());
        }
    }

    public Handle emitModuleBodyJIT(IRModuleBody method) {
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + "$" + methodIndex++;

        String clsName = jvm.scriptToClass(method.getFileName());
        jvm.pushscript(clsName, method.getFileName());

        Signature signature = signatureFor(method, false);
        emitScope(method, name, signature, false);

        Handle handle = new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(signature.type().returnType(), signature.type().parameterArray()));

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
        String name = JavaNameMangler.encodeScopeForBacktrace(closure) + "$" + methodIndex++;

        emitScope(closure, name, CLOSURE_SIGNATURE, false);

        return new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(CLOSURE_SIGNATURE.type().returnType(), CLOSURE_SIGNATURE.type().parameterArray()));
    }

    public Handle emitModuleBody(IRModuleBody method) {
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + "$" + methodIndex++;

        Signature signature = signatureFor(method, false);
        emitScope(method, name, signature, false);

        return new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(signature.type().returnType(), signature.type().parameterArray()));
    }

    public void visit(Instr instr) {
        if (DEBUG) { // debug will skip emitting actual file line numbers
            jvmAdapter().line(instr.getIPC());
        }
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

    // SSS FIXME: Needs an update to reflect instr. change
    @Override
    public void AliasInstr(AliasInstr aliasInstr) {
        IRBytecodeAdapter m = jvm.method();
        m.loadContext();
        m.loadSelf();
        jvmLoadLocal(DYNAMIC_SCOPE);
        // CON FIXME: Ideally this would not have to pass through RubyString and toString
        visit(aliasInstr.getNewName());
        jvmAdapter().invokevirtual(p(Object.class), "toString", sig(String.class));
        visit(aliasInstr.getOldName());
        jvmAdapter().invokevirtual(p(Object.class), "toString", sig(String.class));
        m.invokeIRHelper("defineAlias", sig(void.class, ThreadContext.class, IRubyObject.class, DynamicScope.class, String.class, String.class));
    }

    @Override
    public void AttrAssignInstr(AttrAssignInstr attrAssignInstr) {
        Operand[] callArgs = attrAssignInstr.getCallArgs();

        compileCallCommon(
                jvmMethod(),
                attrAssignInstr.getName(),
                callArgs,
                attrAssignInstr.getReceiver(),
                callArgs.length,
                null,
                false,
                attrAssignInstr.getReceiver() instanceof Self ? CallType.FUNCTIONAL : CallType.NORMAL,
                null);
    }

    @Override
    public void BEQInstr(BEQInstr beqInstr) {
        jvmMethod().loadContext();
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
        jvmMethod().loadContext();
        visit(blockGivenInstr.getBlockArg());
        jvmMethod().invokeIRHelper("isBlockGiven", sig(RubyBoolean.class, ThreadContext.class, Object.class));
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
                throw new NotCompilableException("Non-float/fixnum in loadFloatArg!" + arg);
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
                throw new NotCompilableException("Non-float/fixnum in loadFixnumArg!" + arg);
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
                throw new NotCompilableException("Non-float/fixnum in loadFixnumArg!" + arg);
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
            default: throw new NotCompilableException("UNHANDLED!");
        }

        // Store it
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BacktickInstr(BacktickInstr instr) {
        // prepare for call to "`" below
        jvmMethod().loadContext();
        jvmMethod().loadSelf(); // TODO: remove caller
        jvmMethod().loadSelf();

        ByteList csByteList = new ByteList();
        jvmMethod().pushString(csByteList);

        for (Operand p : instr.getOperands()) {
            // visit piece and ensure it's a string
            visit(p);
            jvmAdapter().dup();
            org.objectweb.asm.Label after = new org.objectweb.asm.Label();
            jvmAdapter().instance_of(p(RubyString.class));
            jvmAdapter().iftrue(after);
            jvmAdapter().invokevirtual(p(IRubyObject.class), "anyToString", sig(IRubyObject.class));

            jvmAdapter().label(after);
            jvmAdapter().invokevirtual(p(RubyString.class), "append", sig(RubyString.class, IRubyObject.class));
        }

        // freeze the string
        jvmAdapter().dup();
        jvmAdapter().ldc(true);
        jvmAdapter().invokeinterface(p(IRubyObject.class), "setFrozen", sig(void.class, boolean.class));

        // invoke the "`" method on self
        jvmMethod().invokeSelf("`", 1, false, CallType.FUNCTIONAL);
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BNEInstr(BNEInstr bneinstr) {
        jvmMethod().loadContext();
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
        jvmMethod().loadContext();
        jvmLoadLocal(DYNAMIC_SCOPE);
        visit(breakInstr.getReturnValue());
        jvmMethod().loadBlockType();
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "initiateBreak", sig(IRubyObject.class, ThreadContext.class, DynamicScope.class, IRubyObject.class, Block.Type.class));
        jvmMethod().returnValue();

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
    public void BuildCompoundStringInstr(BuildCompoundStringInstr compoundstring) {
        ByteList csByteList = new ByteList();
        csByteList.setEncoding(compoundstring.getEncoding());
        jvmMethod().pushString(csByteList);
        for (Operand p : compoundstring.getPieces()) {
//            if ((p instanceof StringLiteral) && (compoundstring.isSameEncodingAndCodeRange((StringLiteral)p))) {
//                jvmMethod().pushByteList(((StringLiteral)p).bytelist);
//                jvmAdapter().invokevirtual(p(RubyString.class), "cat", sig(RubyString.class, ByteList.class));
//            } else {
                visit(p);
                jvmAdapter().invokevirtual(p(RubyString.class), "append19", sig(RubyString.class, IRubyObject.class));
//            }
        }
        jvmStoreLocal(compoundstring.getResult());
    }

    @Override
    public void BuildDynRegExpInstr(BuildDynRegExpInstr instr) {
        final IRBytecodeAdapter m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        RegexpOptions options = instr.getOptions();
        final Operand[] operands = instr.getPieces();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                m.loadContext();
                for (int i = 0; i < operands.length; i++) {
                    Operand operand = operands[i];
                    visit(operand);
                }
            }
        };

        m.pushDRegexp(r, options, operands.length);

        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BuildRangeInstr(BuildRangeInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getBegin());
        visit(instr.getEnd());
        jvmAdapter().ldc(instr.isExclusive());
        jvmAdapter().invokestatic(p(RubyRange.class), "newRange", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BuildSplatInstr(BuildSplatInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getArray());
        jvmMethod().invokeIRHelper("irSplat", sig(RubyArray.class, ThreadContext.class, IRubyObject.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void CallInstr(CallInstr callInstr) {
        IRBytecodeAdapter m = jvmMethod();
        String name = callInstr.getName();
        Operand[] args = callInstr.getCallArgs();
        Operand receiver = callInstr.getReceiver();
        int numArgs = args.length;
        Operand closure = callInstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        CallType callType = callInstr.getCallType();
        Variable result = callInstr.getResult();

        compileCallCommon(m, name, args, receiver, numArgs, closure, hasClosure, callType, result);
    }

    private void compileCallCommon(IRBytecodeAdapter m, String name, Operand[] args, Operand receiver, int numArgs, Operand closure, boolean hasClosure, CallType callType, Variable result) {
        m.loadContext();
        m.loadSelf(); // caller
        visit(receiver);
        int arity = numArgs;

        if (numArgs == 1 && args[0] instanceof Splat) {
            visit(args[0]);
            m.adapter.invokevirtual(p(RubyArray.class), "toJavaArray", sig(IRubyObject[].class));
            arity = -1;
        } else if (CallBase.containsArgSplat(args)) {
            throw new NotCompilableException("splat in non-initial argument for normal call is unsupported in JIT");
        } else {
            for (Operand operand : args) {
                visit(operand);
            }
        }

        if (hasClosure) {
            m.loadContext();
            visit(closure);
            m.invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        }

        switch (callType) {
            case FUNCTIONAL:
                m.invokeSelf(name, arity, hasClosure, CallType.FUNCTIONAL);
                break;
            case VARIABLE:
                m.invokeSelf(name, arity, hasClosure, CallType.VARIABLE);
                break;
            case NORMAL:
                m.invokeOther(name, arity, hasClosure);
                break;
        }

        if (result != null) {
            jvmStoreLocal(result);
        } else {
            // still need to drop, since all dyncalls return something (FIXME)
            m.adapter.pop();
        }
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
        if (jvm.methodData().specificArity >= 0) {
            // no arity check in specific arity path
        } else {
            jvmMethod().loadContext();
            jvmMethod().loadArgs();
            jvmAdapter().ldc(checkarityinstr.required);
            jvmAdapter().ldc(checkarityinstr.opt);
            jvmAdapter().ldc(checkarityinstr.rest);
            jvmAdapter().ldc(checkarityinstr.receivesKeywords);
            jvmAdapter().ldc(checkarityinstr.restKey);
            jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "checkArity", sig(void.class, ThreadContext.class, Object[].class, int.class, int.class, int.class, boolean.class, int.class));
        }
    }

    @Override
    public void CheckForLJEInstr(CheckForLJEInstr checkForljeinstr) {
        jvmMethod().loadContext();
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmAdapter().ldc(checkForljeinstr.maybeLambda());
        jvmMethod().loadBlockType();
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "checkForLJE", sig(void.class, ThreadContext.class, DynamicScope.class, boolean.class, Block.Type.class));
    }

        @Override
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) {
        String name = classsuperinstr.getName();
        Operand[] args = classsuperinstr.getCallArgs();
        Operand definingModule = classsuperinstr.getDefiningModule();
        boolean containsArgSplat = classsuperinstr.containsArgSplat();
        Operand closure = classsuperinstr.getClosureArg(null);

        superCommon(name, classsuperinstr, args, definingModule, containsArgSplat, closure);
    }

    @Override
    public void ConstMissingInstr(ConstMissingInstr constmissinginstr) {
        visit(constmissinginstr.getReceiver());
        jvmAdapter().checkcast("org/jruby/RubyModule");
        jvmMethod().loadContext();
        jvmAdapter().ldc("const_missing");
        // FIXME: This has lost it's encoding info by this point
        jvmMethod().pushSymbol(constmissinginstr.getMissingConst(), USASCIIEncoding.INSTANCE);
        jvmMethod().invokeVirtual(Type.getType(RubyModule.class), Method.getMethod("org.jruby.runtime.builtin.IRubyObject callMethod(org.jruby.runtime.ThreadContext, java.lang.String, org.jruby.runtime.builtin.IRubyObject)"));
        jvmStoreLocal(constmissinginstr.getResult());
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

        jvmMethod().loadContext();
        Handle handle = emitModuleBody(newIRClassBody);
        jvmMethod().pushHandle(handle);
        jvmAdapter().getstatic(jvm.clsData().clsName, handle.getName() + "_IRScope", ci(IRScope.class));
        visit(defineclassinstr.getContainer());
        visit(defineclassinstr.getSuperClass());

        jvmMethod().invokeIRHelper("newCompiledClassBody", sig(DynamicMethod.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, IRScope.class, Object.class, Object.class));

        jvmStoreLocal(defineclassinstr.getResult());
    }

    @Override
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) {
        IRMethod method = defineclassmethodinstr.getMethod();

        jvmMethod().loadContext();

        emitMethod(method);

        Map<Integer, MethodType> signatures = method.getNativeSignatures();

        MethodType signature = signatures.get(-1);

        String defSignature = pushHandlesForDef(
                method.getJittedName(),
                signatures,
                signature,
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, IRScope.class, IRubyObject.class),
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, java.lang.invoke.MethodHandle.class, int.class, IRScope.class, IRubyObject.class));

        jvmAdapter().getstatic(jvm.clsData().clsName, method.getJittedName() + "_IRScope", ci(IRScope.class));
        visit(defineclassmethodinstr.getContainer());

        // add method
        jvmMethod().adapter.invokestatic(p(IRRuntimeHelpers.class), "defCompiledClassMethod", defSignature);
    }

    // SSS FIXME: Needs an update to reflect instr. change
    @Override
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) {
        IRMethod method = defineinstancemethodinstr.getMethod();

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        m.loadContext();

        emitMethod(method);
        Map<Integer, MethodType> signatures = method.getNativeSignatures();

        MethodType variable = signatures.get(-1); // always a variable arity handle

        String defSignature = pushHandlesForDef(
                method.getJittedName(),
                signatures,
                variable,
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, IRScope.class, DynamicScope.class, IRubyObject.class),
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, java.lang.invoke.MethodHandle.class, int.class, IRScope.class, DynamicScope.class, IRubyObject.class));

        a.getstatic(jvm.clsData().clsName, method.getJittedName() + "_IRScope", ci(IRScope.class));
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmMethod().loadSelf();

        // add method
        a.invokestatic(p(IRRuntimeHelpers.class), "defCompiledInstanceMethod", defSignature);
    }

    public String pushHandlesForDef(String name, Map<Integer, MethodType> signatures, MethodType variable, String variableOnly, String variableAndSpecific) {
        String defSignature;

        jvmMethod().pushHandle(new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(variable.returnType(), variable.parameterArray())));

        if (signatures.size() == 1) {
            defSignature = variableOnly;
        } else {
            defSignature = variableAndSpecific;

            // FIXME: only supports one arity
            for (Map.Entry<Integer, MethodType> entry : signatures.entrySet()) {
                if (entry.getKey() == -1) continue; // variable arity signature pushed above
                jvmMethod().pushHandle(new Handle(Opcodes.H_INVOKESTATIC, jvm.clsData().clsName, name, sig(entry.getValue().returnType(), entry.getValue().parameterArray())));
                jvmAdapter().pushInt(entry.getKey());
                break;
            }
        }
        return defSignature;
    }

    @Override
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) {
        IRModuleBody metaClassBody = definemetaclassinstr.getMetaClassBody();

        jvmMethod().loadContext();
        Handle handle = emitModuleBody(metaClassBody);
        jvmMethod().pushHandle(handle);
        jvmAdapter().getstatic(jvm.clsData().clsName, handle.getName() + "_IRScope", ci(IRScope.class));
        visit(definemetaclassinstr.getObject());

        jvmMethod().invokeIRHelper("newCompiledMetaClass", sig(DynamicMethod.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, IRScope.class, IRubyObject.class));

        jvmStoreLocal(definemetaclassinstr.getResult());
    }

    @Override
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) {
        IRModuleBody newIRModuleBody = definemoduleinstr.getNewIRModuleBody();

        jvmMethod().loadContext();
        Handle handle = emitModuleBody(newIRModuleBody);
        jvmMethod().pushHandle(handle);
        jvmAdapter().getstatic(jvm.clsData().clsName, handle.getName() + "_IRScope", ci(IRScope.class));
        visit(definemoduleinstr.getContainer());

        jvmMethod().invokeIRHelper("newCompiledModuleBody", sig(DynamicMethod.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, IRScope.class, Object.class));

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
        throw new NotCompilableException("Marker instructions shouldn't reach compiler: " + exceptionregionendmarkerinstr);
    }

    @Override
    public void ExceptionRegionStartMarkerInstr(ExceptionRegionStartMarkerInstr exceptionregionstartmarkerinstr) {
        throw new NotCompilableException("Marker instructions shouldn't reach compiler: " + exceptionregionstartmarkerinstr);
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
        String name = getglobalvariableinstr.getGVar().getName();
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
        jvmMethod().loadContext();
        visit(inheritancesearchconstinstr.getCurrentModule());

        jvmMethod().inheritanceSearchConst(inheritancesearchconstinstr.getConstName(), inheritancesearchconstinstr.isNoPrivateConsts());
        jvmStoreLocal(inheritancesearchconstinstr.getResult());
    }

    @Override
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) {
        String name = instancesuperinstr.getName();
        Operand[] args = instancesuperinstr.getCallArgs();
        Operand definingModule = instancesuperinstr.getDefiningModule();
        boolean containsArgSplat = instancesuperinstr.containsArgSplat();
        Operand closure = instancesuperinstr.getClosureArg(null);

        superCommon(name, instancesuperinstr, args, definingModule, containsArgSplat, closure);
    }

    private void superCommon(String name, CallInstr instr, Operand[] args, Operand definingModule, boolean containsArgSplat, Operand closure) {
        IRBytecodeAdapter m = jvmMethod();
        Operation operation = instr.getOperation();

        m.loadContext();
        m.loadSelf(); // TODO: get rid of caller
        m.loadSelf();
        if (definingModule == UndefinedValue.UNDEFINED) {
            jvmAdapter().aconst_null();
        } else {
            visit(definingModule);
        }

        // TODO: CON: is this safe?
        jvmAdapter().checkcast(p(RubyClass.class));

        // process args
        for (int i = 0; i < args.length; i++) {
            Operand operand = args[i];
            visit(operand);
        }

        // if there's splats, provide a map and let the call site sort it out
        boolean[] splatMap = IRRuntimeHelpers.buildSplatMap(args, containsArgSplat);

        boolean hasClosure = closure != null;
        if (hasClosure) {
            m.loadContext();
            visit(closure);
            m.invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
        }

        switch (operation) {
            case INSTANCE_SUPER:
                m.invokeInstanceSuper(name, args.length, hasClosure, splatMap);
                break;
            case CLASS_SUPER:
                m.invokeClassSuper(name, args.length, hasClosure, splatMap);
                break;
            case UNRESOLVED_SUPER:
                m.invokeUnresolvedSuper(name, args.length, hasClosure, splatMap);
                break;
            case ZSUPER:
                m.invokeZSuper(name, args.length, hasClosure, splatMap);
                break;
            default:
                throw new NotCompilableException("unknown super type " + operation + " in " + instr);
        }

        jvmStoreLocal(instr.getResult());
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
        jvmMethod().loadContext();
        visit(lexicalsearchconstinstr.getDefiningScope());

        jvmMethod().lexicalSearchConst(lexicalsearchconstinstr.getConstName());

        jvmStoreLocal(lexicalsearchconstinstr.getResult());
    }

    @Override
    public void LineNumberInstr(LineNumberInstr linenumberinstr) {
        if (DEBUG) return; // debug mode uses IPC for line numbers

        jvmAdapter().line(linenumberinstr.getLineNumber() + 1);
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
    public void LoadImplicitClosure(LoadImplicitClosureInstr loadimplicitclosureinstr) {
        jvmMethod().loadBlock();
        jvmStoreLocal(loadimplicitclosureinstr.getResult());
    }

    @Override
    public void LoadFrameClosure(LoadFrameClosureInstr loadframeclosureinstr) {
        jvmMethod().loadContext();
        jvmAdapter().invokevirtual(p(ThreadContext.class), "getFrameBlock", sig(Block.class));
        jvmStoreLocal(loadframeclosureinstr.getResult());
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
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) {
        // SSS FIXME: Unused at this time
        throw new NotCompilableException("Unsupported instruction: " + moduleversionguardinstr);
    }

    @Override
    public void NopInstr(NopInstr nopinstr) {
        // do nothing
    }

    @Override
    public void NoResultCallInstr(NoResultCallInstr noResultCallInstr) {
        IRBytecodeAdapter m = jvmMethod();
        String name = noResultCallInstr.getName();
        Operand[] args = noResultCallInstr.getCallArgs();
        Operand receiver = noResultCallInstr.getReceiver();
        int numArgs = args.length;
        Operand closure = noResultCallInstr.getClosureArg(null);
        boolean hasClosure = closure != null;
        CallType callType = noResultCallInstr.getCallType();

        compileCallCommon(m, name, args, receiver, numArgs, closure, hasClosure, callType, null);
    }

    @Override
    public void OneFixnumArgNoBlockCallInstr(OneFixnumArgNoBlockCallInstr oneFixnumArgNoBlockCallInstr) {
        if (MethodIndex.getFastFixnumOpsMethod(oneFixnumArgNoBlockCallInstr.getName()) == null) {
            CallInstr(oneFixnumArgNoBlockCallInstr);
            return;
        }
        IRBytecodeAdapter m = jvmMethod();
        String name = oneFixnumArgNoBlockCallInstr.getName();
        long fixnum = oneFixnumArgNoBlockCallInstr.getFixnumArg();
        Operand receiver = oneFixnumArgNoBlockCallInstr.getReceiver();
        Variable result = oneFixnumArgNoBlockCallInstr.getResult();

        m.loadContext();

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        m.loadSelf(); // caller

        visit(receiver);

        m.invokeOtherOneFixnum(name, fixnum);

        if (result != null) {
            jvmStoreLocal(result);
        } else {
            // still need to drop, since all dyncalls return something (FIXME)
            m.adapter.pop();
        }
    }

    @Override
    public void OneFloatArgNoBlockCallInstr(OneFloatArgNoBlockCallInstr oneFloatArgNoBlockCallInstr) {
        if (MethodIndex.getFastFloatOpsMethod(oneFloatArgNoBlockCallInstr.getName()) == null) {
            CallInstr(oneFloatArgNoBlockCallInstr);
            return;
        }
        IRBytecodeAdapter m = jvmMethod();
        String name = oneFloatArgNoBlockCallInstr.getName();
        double flote = oneFloatArgNoBlockCallInstr.getFloatArg();
        Operand receiver = oneFloatArgNoBlockCallInstr.getReceiver();
        Variable result = oneFloatArgNoBlockCallInstr.getResult();

        m.loadContext();

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        m.loadSelf(); // caller

        visit(receiver);

        m.invokeOtherOneFloat(name, flote);

        if (result != null) {
            jvmStoreLocal(result);
        } else {
            // still need to drop, since all dyncalls return something (FIXME)
            m.adapter.pop();
        }
    }

    @Override
    public void OneOperandArgNoBlockCallInstr(OneOperandArgNoBlockCallInstr oneOperandArgNoBlockCallInstr) {
        CallInstr(oneOperandArgNoBlockCallInstr);
    }

    @Override
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) {
        visit(optargmultipleasgninstr.getArray());
        jvmAdapter().checkcast(p(RubyArray.class));
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
        jvmMethod().loadContext();
        visit(processmodulebodyinstr.getModuleBody());
        visit(processmodulebodyinstr.getBlock());
        jvmMethod().invokeIRHelper("invokeModuleBody", sig(IRubyObject.class, ThreadContext.class, DynamicMethod.class, Block.class));
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
    public void RaiseRequiredKeywordArgumentErrorInstr(RaiseRequiredKeywordArgumentError instr) {
        jvmMethod().loadContext();
        jvmAdapter().ldc(instr.getName());
        jvmMethod().invokeIRHelper("newRequiredKeywordArgumentError", sig(RaiseException.class, ThreadContext.class, String.class));
        jvmAdapter().athrow();
    }

    @Override
    public void PushFrameInstr(PushFrameInstr pushframeinstr) {
        jvmMethod().loadContext();
        jvmMethod().loadFrameClass();
        jvmMethod().loadFrameName();
        jvmMethod().loadSelf();
        jvmMethod().loadBlock();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void preMethodFrameOnly(org.jruby.RubyModule, String, org.jruby.runtime.builtin.IRubyObject, org.jruby.runtime.Block)"));

        // FIXME: this should be part of explicit call protocol only when needed, optimizable, and correct for the scope
        // See also CompiledIRMethod.call
        jvmMethod().loadContext();
        jvmAdapter().invokestatic(p(Visibility.class), "values", sig(Visibility[].class));
        jvmAdapter().ldc(Visibility.PUBLIC.ordinal());
        jvmAdapter().aaload();
        jvmAdapter().invokevirtual(p(ThreadContext.class), "setCurrentVisibility", sig(void.class, Visibility.class));
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
    public void ReifyClosureInstr(ReifyClosureInstr reifyclosureinstr) {
        jvmMethod().loadRuntime();
        jvmLoadLocal("$block");
        jvmMethod().invokeIRHelper("newProc", sig(IRubyObject.class, Ruby.class, Block.class));
        jvmStoreLocal(reifyclosureinstr.getResult());
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
    public void ReceiveKeywordArgInstr(ReceiveKeywordArgInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.required);
        jvmAdapter().ldc(instr.argName);
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receiveKeywordArg", sig(IRubyObject.class, ThreadContext.class, IRubyObject[].class, int.class, String.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceiveKeywordRestArgInstr(ReceiveKeywordRestArgInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.required);
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receiveKeywordRestArg", sig(IRubyObject.class, ThreadContext.class, IRubyObject[].class, int.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceiveOptArgInstr(ReceiveOptArgInstr instr) {
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.requiredArgs);
        jvmAdapter().pushInt(instr.preArgs);
        jvmAdapter().pushInt(instr.getArgIndex());
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receiveOptArg", sig(IRubyObject.class, IRubyObject[].class, int.class, int.class, int.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceivePreReqdArgInstr(ReceivePreReqdArgInstr instr) {
        if (jvm.methodData().specificArity >= 0 &&
                instr.getArgIndex() < jvm.methodData().specificArity) {
            jvmAdapter().aload(jvm.methodData().signature.argOffset("arg" + instr.getArgIndex()));
        } else {
            jvmMethod().loadContext();
            jvmMethod().loadArgs();
            jvmAdapter().pushInt(instr.getArgIndex());
            jvmMethod().invokeIRHelper("getPreArgSafe", sig(IRubyObject.class, ThreadContext.class, IRubyObject[].class, int.class));
        }
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceivePostReqdArgInstr(ReceivePostReqdArgInstr instr) {
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.preReqdArgsCount);
        jvmAdapter().pushInt(instr.postReqdArgsCount);
        jvmAdapter().pushInt(instr.getArgIndex());
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receivePostReqdArg", sig(IRubyObject.class, IRubyObject[].class, int.class, int.class, int.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceiveRestArgInstr(ReceiveRestArgInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.required);
        jvmAdapter().pushInt(instr.getArgIndex());
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receiveRestArg", sig(IRubyObject.class, ThreadContext.class, Object[].class, int.class, int.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ReceiveSelfInstr(ReceiveSelfInstr receiveselfinstr) {
        jvmMethod().loadSelf();
        jvmStoreLocal(receiveselfinstr.getResult());
    }

    @Override
    public void RecordEndBlockInstr(RecordEndBlockInstr recordEndBlockInstr) {
        jvmMethod().loadContext();

        jvmMethod().loadContext();
        visit(recordEndBlockInstr.getEndBlockClosure());
        jvmMethod().invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));

        jvmMethod().invokeIRHelper("pushExitBlock", sig(void.class, ThreadContext.class, Block.class));
    }

    @Override
    public void ReqdArgMultipleAsgnInstr(ReqdArgMultipleAsgnInstr reqdargmultipleasgninstr) {
        jvmMethod().loadContext();
        visit(reqdargmultipleasgninstr.getArray());
        jvmAdapter().checkcast(p(RubyArray.class));
        jvmAdapter().pushInt(reqdargmultipleasgninstr.getPreArgsCount());
        jvmAdapter().pushInt(reqdargmultipleasgninstr.getIndex());
        jvmAdapter().pushInt(reqdargmultipleasgninstr.getPostArgsCount());
        jvmMethod().invokeIRHelper("irReqdArgMultipleAsgn", sig(IRubyObject.class, ThreadContext.class, RubyArray.class, int.class, int.class, int.class));
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
        jvmMethod().loadContext();
        visit(restargmultipleasgninstr.getArray());
        jvmAdapter().checkcast(p(RubyArray.class));
        jvmAdapter().pushInt(restargmultipleasgninstr.getPreArgsCount());
        jvmAdapter().pushInt(restargmultipleasgninstr.getPostArgsCount());
        jvmAdapter().invokestatic(p(Helpers.class), "viewArgsArray", sig(RubyArray.class, ThreadContext.class, RubyArray.class, int.class, int.class));
        jvmStoreLocal(restargmultipleasgninstr.getResult());
    }

    @Override
    public void RuntimeHelperCall(RuntimeHelperCall runtimehelpercall) {
        switch (runtimehelpercall.getHelperMethod()) {
            case HANDLE_PROPAGATE_BREAK:
                jvmMethod().loadContext();
                jvmLoadLocal(DYNAMIC_SCOPE);
                visit(runtimehelpercall.getArgs()[0]);
                jvmMethod().loadBlockType();
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "handlePropagatedBreak", sig(IRubyObject.class, ThreadContext.class, DynamicScope.class, Object.class, Block.Type.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case HANDLE_NONLOCAL_RETURN:
                jvmMethod().loadStaticScope();
                jvmLoadLocal(DYNAMIC_SCOPE);
                visit(runtimehelpercall.getArgs()[0]);
                jvmMethod().loadBlockType();
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "handleNonlocalReturn", sig(IRubyObject.class, StaticScope.class, DynamicScope.class, Object.class, Block.Type.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case HANDLE_BREAK_AND_RETURNS_IN_LAMBDA:
                jvmMethod().loadContext();
                jvmMethod().loadStaticScope();
                jvmLoadLocal(DYNAMIC_SCOPE);
                visit(runtimehelpercall.getArgs()[0]);
                jvmMethod().loadBlockType();
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "handleBreakAndReturnsInLambdas", sig(IRubyObject.class, ThreadContext.class, StaticScope.class, DynamicScope.class, Object.class, Block.Type.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_BACKREF:
                jvmMethod().loadContext();
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedBackref", sig(IRubyObject.class, ThreadContext.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_CALL:
                jvmMethod().loadContext();
                jvmMethod().loadSelf();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((StringLiteral) runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedCall", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_CONSTANT_OR_METHOD:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((StringLiteral)runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedConstantOrMethod", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class));
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
                jvmAdapter().ldc(((StringLiteral) runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().ldc(((Boolean)runtimehelpercall.getArgs()[2]).isTrue());
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedMethod", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class, boolean.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case MERGE_KWARGS:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                visit(runtimehelpercall.getArgs()[1]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "mergeKeywordArguments", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            default:
                throw new NotCompilableException("Unknown IR runtime helper method: " + runtimehelpercall.getHelperMethod() + "; INSTR: " + this);
        }
    }

    @Override
    public void NonlocalReturnInstr(NonlocalReturnInstr returninstr) {
        jvmMethod().loadContext();
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmMethod().loadBlockType();
        visit(returninstr.getReturnValue());

        jvmMethod().invokeIRHelper("initiateNonLocalReturn", sig(IRubyObject.class, ThreadContext.class, DynamicScope.class, Block.Type.class, IRubyObject.class));
        jvmMethod().returnValue();
    }

    @Override
    public void ReturnInstr(ReturnInstr returninstr) {
        visit(returninstr.getReturnValue());
        jvmMethod().returnValue();
    }

    @Override
    public void SearchConstInstr(SearchConstInstr searchconstinstr) {
        jvmMethod().loadContext();
        visit(searchconstinstr.getStartingScope());
        jvmMethod().searchConst(searchconstinstr.getConstName(), searchconstinstr.isNoPrivateConsts());
        jvmStoreLocal(searchconstinstr.getResult());
    }

    @Override
    public void SetCapturedVarInstr(SetCapturedVarInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getMatch2Result());
        jvmAdapter().ldc(instr.getVarName());
        jvmMethod().invokeIRHelper("setCapturedVar", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class));
        jvmStoreLocal(instr.getResult());
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
        jvmMethod().checkpoint();
    }

    @Override
    public void ThrowExceptionInstr(ThrowExceptionInstr throwexceptioninstr) {
        visit(throwexceptioninstr.getException());
        jvmAdapter().athrow();
    }

    @Override
    public void ToAryInstr(ToAryInstr toaryinstr) {
        jvmMethod().loadContext();
        visit(toaryinstr.getArray());
        jvmMethod().invokeIRHelper("irToAry", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        jvmStoreLocal(toaryinstr.getResult());
    }

    @Override
    public void UndefMethodInstr(UndefMethodInstr undefmethodinstr) {
        jvmMethod().loadContext();
        visit(undefmethodinstr.getMethodName());
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmMethod().loadSelf();
        jvmMethod().invokeIRHelper("undefMethod", sig(IRubyObject.class, ThreadContext.class, Object.class, DynamicScope.class, IRubyObject.class));
        jvmStoreLocal(undefmethodinstr.getResult());
    }

    @Override
    public void UnresolvedSuperInstr(UnresolvedSuperInstr unresolvedsuperinstr) {
        String name = unresolvedsuperinstr.getName();
        Operand[] args = unresolvedsuperinstr.getCallArgs();
        // this would be getDefiningModule but that is not used for unresolved super
        Operand definingModule = UndefinedValue.UNDEFINED;
        boolean containsArgSplat = unresolvedsuperinstr.containsArgSplat();
        Operand closure = unresolvedsuperinstr.getClosureArg(null);

        superCommon(name, unresolvedsuperinstr, args, definingModule, containsArgSplat, closure);
    }

    @Override
    public void YieldInstr(YieldInstr yieldinstr) {
        jvmMethod().loadContext();
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
    public void ZeroOperandArgNoBlockCallInstr(ZeroOperandArgNoBlockCallInstr zeroOperandArgNoBlockCallInstr) {
        CallInstr(zeroOperandArgNoBlockCallInstr);
    }

    @Override
    public void ZSuperInstr(ZSuperInstr zsuperinstr) {
        String name = zsuperinstr.getName();
        Operand[] args = zsuperinstr.getCallArgs();
        // this would be getDefiningModule but that is not used for unresolved super
        Operand definingModule = UndefinedValue.UNDEFINED;
        boolean containsArgSplat = zsuperinstr.containsArgSplat();
        Operand closure = zsuperinstr.getClosureArg(null);

        superCommon(name, zsuperinstr, args, definingModule, containsArgSplat, closure);
    }

    @Override
    public void GetErrorInfoInstr(GetErrorInfoInstr geterrorinfoinstr) {
        jvmMethod().loadContext();
        jvmAdapter().invokevirtual(p(ThreadContext.class), "getErrorInfo", sig(IRubyObject.class));
        jvmStoreLocal(geterrorinfoinstr.getResult());
    }

    @Override
    public void RestoreErrorInfoInstr(RestoreErrorInfoInstr restoreerrorinfoinstr) {
        jvmMethod().loadContext();
        visit(restoreerrorinfoinstr.getArg());
        jvmAdapter().invokevirtual(p(ThreadContext.class), "setErrorInfo", sig(IRubyObject.class, IRubyObject.class));
        jvmAdapter().pop();
    }

    // ruby 1.9 specific
    @Override
    public void BuildLambdaInstr(BuildLambdaInstr buildlambdainstr) {
        jvmMethod().loadRuntime();

        IRClosure body = ((WrappedIRClosure)buildlambdainstr.getLambdaBody()).getClosure();
        if (body == null) {
            jvmMethod().pushNil();
        } else {
            visit(buildlambdainstr.getLambdaBody());
        }

        jvmAdapter().getstatic(p(Block.Type.class), "LAMBDA", ci(Block.Type.class));
        jvmAdapter().ldc(buildlambdainstr.getPosition().getFile());
        jvmAdapter().pushInt(buildlambdainstr.getPosition().getLine());

        jvmAdapter().invokestatic(p(RubyProc.class), "newProc", sig(RubyProc.class, Ruby.class, Block.class, Block.Type.class, String.class, int.class));

        jvmStoreLocal(buildlambdainstr.getResult());
    }

    @Override
    public void GetEncodingInstr(GetEncodingInstr getencodinginstr) {
        jvmMethod().loadContext();
        jvmMethod().pushEncoding(getencodinginstr.getEncoding());
        jvmStoreLocal(getencodinginstr.getResult());
    }

    // operands
    @Override
    public void Array(Array array) {
        jvmMethod().loadContext();

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
        jvmMethod().loadContext();
        jvmAdapter().invokevirtual(p(ThreadContext.class), "getBackRef", sig(IRubyObject.class));

        switch (backref.type) {
            case '&':
                jvmAdapter().invokestatic(p(RubyRegexp.class), "last_match", sig(IRubyObject.class, IRubyObject.class));
                break;
            case '`':
                jvmAdapter().invokestatic(p(RubyRegexp.class), "match_pre", sig(IRubyObject.class, IRubyObject.class));
                break;
            case '\'':
                jvmAdapter().invokestatic(p(RubyRegexp.class), "match_post", sig(IRubyObject.class, IRubyObject.class));
                break;
            case '+':
                jvmAdapter().invokestatic(p(RubyRegexp.class), "match_last", sig(IRubyObject.class, IRubyObject.class));
                break;
            default:
                assert false: "backref with invalid type";
        }
    }

    @Override
    public void Bignum(Bignum bignum) {
        jvmMethod().pushBignum(bignum.value);
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
        LocalVariable(closurelocalvariable);
    }

    @Override
    public void Complex(Complex complex) {
        jvmMethod().loadRuntime();
        jvmMethod().pushFixnum(0);
        visit(complex.getNumber());
        jvmAdapter().invokestatic(p(RubyComplex.class), "newComplexRaw", sig(RubyComplex.class, Ruby.class, IRubyObject.class, IRubyObject.class));
    }

    @Override
    public void CurrentScope(CurrentScope currentscope) {
        jvmMethod().loadStaticScope();
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
    public void FrozenString(FrozenString frozen) {
        jvmMethod().pushFrozenString(frozen.getByteList());
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
    public void Hash(Hash hash) {
        List<KeyValuePair<Operand, Operand>> pairs = hash.getPairs();
        Iterator<KeyValuePair<Operand, Operand>> iter = pairs.iterator();
        boolean kwargs = hash.isKWArgsHash && pairs.get(0).getKey() == Symbol.KW_REST_ARG_DUMMY;

        jvmMethod().loadContext();
        if (kwargs) {
            visit(pairs.get(0).getValue());
            jvmAdapter().checkcast(p(RubyHash.class));

            iter.next();
        }

        for (; iter.hasNext() ;) {
            KeyValuePair<Operand, Operand> pair = iter.next();
            visit(pair.getKey());
            visit(pair.getValue());
        }

        if (kwargs) {
            jvmMethod().kwargsHash(pairs.size() - 1);
        } else {
            jvmMethod().hash(pairs.size());
        }
    }

    @Override
    public void LocalVariable(LocalVariable localvariable) {
        // CON FIXME: This isn't as efficient as it could be, but we should not see these in optimized JIT scopes
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmAdapter().ldc(localvariable.getOffset());
        jvmAdapter().ldc(localvariable.getScopeDepth());
        jvmMethod().pushNil();
        jvmAdapter().invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, int.class, int.class, IRubyObject.class));
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
    public void NullBlock(NullBlock nullblock) {
        jvmAdapter().getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class));
    }

    @Override
    public void ObjectClass(ObjectClass objectclass) {
        jvmMethod().pushObjectClass();
    }

    @Override
    public void Rational(Rational rational) {
        jvmMethod().loadRuntime();
        jvmAdapter().ldc(rational.getNumerator());
        jvmAdapter().ldc(rational.getDenominator());
        jvmAdapter().invokevirtual(p(Ruby.class), "newRational", sig(RubyRational.class, long.class, long.class));
    }

    @Override
    public void Regexp(Regexp regexp) {
        jvmMethod().pushRegexp(regexp.getSource(), regexp.options.toEmbeddedOptions());
    }

    @Override
    public void ScopeModule(ScopeModule scopemodule) {
        jvmMethod().loadStaticScope();
        jvmAdapter().pushInt(scopemodule.getScopeModuleDepth());
        jvmAdapter().invokestatic(p(Helpers.class), "getNthScopeModule", sig(RubyModule.class, StaticScope.class, int.class));
    }

    @Override
    public void Self(Self self) {
        jvmMethod().loadSelf();
    }

    @Override
    public void Splat(Splat splat) {
        visit(splat.getArray());
        // Splat is now only used in call arg lists where it is guaranteed that
        // the splat-arg is an array.
        //
        // It is:
        // - either a result of a args-cat/args-push (which generate an array),
        // - or a result of a BuildSplatInstr (which also generates an array),
        // - or a rest-arg that has been received (which also generates an array)
        //   and is being passed via zsuper.
        //
        // In addition, since this only shows up in call args, the array itself is
        // never modified. The array elements are extracted out and inserted into
        // a java array. So, a dup is not required either.
        //
        // So, besides retrieving the array, nothing more to be done here!
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
        visit(svalue.getArray());
        jvmAdapter().dup();
        jvmAdapter().instance_of(p(RubyArray.class));
        org.objectweb.asm.Label after = new org.objectweb.asm.Label();
        jvmAdapter().iftrue(after);
        jvmAdapter().pop();
        jvmMethod().pushNil();
        jvmAdapter().label(after);
    }

    @Override
    public void Symbol(Symbol symbol) {
        jvmMethod().pushSymbol(symbol.getName(), symbol.getEncoding());
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
        throw new NotCompilableException(this.getClass().getSimpleName() + " should never be directly executed!");
    }

    @Override
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) {
        IRClosure closure = wrappedirclosure.getClosure();

        jvmAdapter().newobj(p(Block.class));
        jvmAdapter().dup();

        jvmMethod().pushBlockBody(closure.getHandle(), closure.getSignature(), jvm.clsData().clsName);

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

    private JVM jvm;
    private int methodIndex;
    private Map<String, IRScope> scopeMap;
}
