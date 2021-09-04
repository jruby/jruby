package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.*;
import org.jruby.compiler.NotCompilableException;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.ir.*;
import org.jruby.ir.instructions.*;
import org.jruby.ir.instructions.boxing.*;
import org.jruby.ir.instructions.defined.GetErrorInfoInstr;
import org.jruby.ir.instructions.defined.RestoreErrorInfoInstr;
import org.jruby.ir.instructions.specialized.OneFixnumArgNoBlockCallInstr;
import org.jruby.ir.instructions.specialized.OneFloatArgNoBlockCallInstr;
import org.jruby.ir.interpreter.FullInterpreterContext;
import org.jruby.ir.operands.*;
import org.jruby.ir.operands.Boolean;
import org.jruby.ir.operands.Float;
import org.jruby.ir.operands.Label;
import org.jruby.ir.persistence.IRDumper;
import org.jruby.ir.representations.BasicBlock;
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.ir.targets.IRBytecodeAdapter.BlockPassType;
import org.jruby.ir.targets.indy.Bootstrap;
import org.jruby.ir.targets.indy.CallTraceSite;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.DynamicScopeGenerator;
import org.jruby.util.ByteList;
import org.jruby.util.ClassDefiningClassLoader;
import org.jruby.util.JavaNameMangler;
import org.jruby.util.KeyValuePair;
import org.jruby.util.RegexpOptions;
import org.jruby.util.cli.Options;
import org.jruby.util.collections.IntHashMap;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.io.ByteArrayOutputStream;
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

    private static final Logger LOG = LoggerFactory.getLogger(JVMVisitor.class);
    public static final String DYNAMIC_SCOPE = "$dynamicScope";
    private static final boolean DEBUG = false;
    public static final String BLOCK_ARG_NAME = "blockArg";
    public static final String SELF_BLOCK_NAME = "selfBlock";
    public static final String SUPER_NAME_NAME = "superName";

    private static final Signature METHOD_SIGNATURE_BASE = Signature
            .returning(IRubyObject.class)
            .appendArgs(new String[]{"context", "scope", "self", BLOCK_ARG_NAME, "class", "superName"}, ThreadContext.class, StaticScope.class, IRubyObject.class, Block.class, RubyModule.class, String.class);
    private static final Signature METHOD_SIGNATURE_VARARGS = METHOD_SIGNATURE_BASE.insertArg(BLOCK_ARG_NAME, "args", IRubyObject[].class);

    public static final Signature CLOSURE_SIGNATURE = Signature
            .returning(IRubyObject.class)
            .appendArgs(new String[]{"context", SELF_BLOCK_NAME, "scope", "self", "args", BLOCK_ARG_NAME}, ThreadContext.class, Block.class, StaticScope.class, IRubyObject.class, IRubyObject[].class, Block.class);

    JVMVisitor(Ruby runtime, BytecodeMode bytecodeMode) {
        this.bytecodeMode = bytecodeMode;
        this.jvm = new JVM();
        this.methodIndex = 0;
        this.runtime = runtime;
    }

    public static JVMVisitor newForJIT(Ruby runtime) {
        return new JVMVisitor(runtime, Options.COMPILE_INVOKEDYNAMIC.load() ? BytecodeMode.INDY : BytecodeMode.MIXED);
    }

    public static JVMVisitor newForAOT(Ruby runtime) {
        return new JVMVisitor(runtime, BytecodeMode.AOT);
    }

    public BytecodeMode getBytecodeMode() {
        return bytecodeMode;
    }

    public Class compile(IRScope scope, ClassDefiningClassLoader jrubyClassLoader) {
        file = scope.getFile();
        JVMVisitorMethodContext context = new JVMVisitorMethodContext();
        return defineFromBytecode(scope, compileToBytecode(scope, context), jrubyClassLoader);
    }

    public byte[] compileToBytecode(IRScope scope, JVMVisitorMethodContext context) {
        file = scope.getFile();
        codegenScope(scope, context);
        return code();
    }

    /**
     * Define a scope's compiled class from bytecode.
     *
     * This will set all scope fields in the given class to the already-live static scopes.
     */
    public Class defineFromBytecode(IRScope scope, byte[] code, ClassDefiningClassLoader jrubyClassLoader) {
        return defineFromBytecode(scope, code, jrubyClassLoader, true);
    }

    /**
     * Define a class from a top-level script's bytecode.
     *
     * Top-level script bytecode does not need to set all static scopes, since it can build from root at runtime.
     */
    public Class defineScriptFromBytecode(IRScope scope, byte[] code, ClassDefiningClassLoader jrubyClassLoader) {
        return defineFromBytecode(scope, code, jrubyClassLoader, !Options.COMPILE_CACHE_CLASSES.load());
    }

    public Class defineFromBytecode(IRScope scope, byte[] code, ClassDefiningClassLoader jrubyClassLoader, boolean setScopes) {
        file = scope.getFile();
        Class result = jrubyClassLoader.defineClass(c(JVM.scriptToClass(file)), code);

        if (setScopes) {
            for (Map.Entry<String, StaticScope> entry : staticScopeMap.entrySet()) {
                try {
                    result.getField(entry.getKey()).set(null, entry.getValue());
                } catch (Exception e) {
                    throw new NotCompilableException(e);
                }
            }
        }

        return result;
    }

    public byte[] code() {
        return jvm.toByteCode();
    }

    protected void codegenScope(IRScope scope, JVMVisitorMethodContext context) {
        if (scope instanceof IRScriptBody) {
            emitScriptBody((IRScriptBody)scope);
        } else if (scope instanceof IRMethod) {
            emitMethodJIT((IRMethod)scope, context);
        } else if (scope instanceof IRClosure) {
            emitBlockJIT((IRClosure) scope, context);
        } else if (scope instanceof IRModuleBody) {
            emitModuleBodyJIT((IRModuleBody)scope);
        } else {
            throw new NotCompilableException("don't know how to JIT: " + scope);
        }
    }

    protected void emitScope(IRScope scope, String name, Signature signature, boolean specificArity, boolean print) {
        BasicBlock[] bbs = scope.prepareForCompilation();
        FullInterpreterContext fullIC = scope.getFullInterpreterContext();

        if (print && IRRuntimeHelpers.shouldPrintIR(runtime)) {
            ByteArrayOutputStream baos = IRDumper.printIR(scope, true);

            LOG.info("Printing JIT IR for " + scope.getId() + ":\n" + new String(baos.toByteArray()));
        }

        Map<BasicBlock, Label> exceptionTable = scope.buildJVMExceptionTable(fullIC);

        String scopeField = name + "_StaticScope";

        StaticScope staticScope = registerScopeField(scope, scopeField);

        emitClosures(scope, print);

        jvm.pushmethod(name, scope, scopeField, signature, specificArity);

        if (fullIC.needsBinding() || !fullIC.hasExplicitCallProtocol()) {
            // declare dynamic scope local only if we'll need it
            jvm.methodData().local("$dynamicScope", Type.getType(DynamicScope.class));
        }

        if (!fullIC.hasExplicitCallProtocol()) {
            // No call protocol, dynscope has been prepared for us
            jvmMethod().loadContext();
            jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("org.jruby.runtime.DynamicScope getCurrentScope()"));
            jvmStoreLocal(DYNAMIC_SCOPE);
        } else if (scope instanceof IRClosure) {
            // just load null so it is initialized; if we need it, we'll set it later
            jvmAdapter().aconst_null();
            jvmStoreLocal(DYNAMIC_SCOPE);
        }

        IRBytecodeAdapter m = jvmMethod();

        Label currentRescue = null;
        Label currentRegionStart = null;
        Label currentBlockStart = null;
        Map<Label, org.objectweb.asm.Label> rescueEndForStart = new HashMap<>();
        Map<Label, org.objectweb.asm.Label> syntheticEndForStart = new HashMap<>();

        for (BasicBlock bb: bbs) {
            currentBlockStart = bb.getLabel();
            Label rescueLabel = exceptionTable.get(bb);

            // not in a region at all (null-null) or in a region (a-a) but not at a boundary of the region.
            if (rescueLabel == currentRescue) continue;

            if (currentRescue != null) { // end of active region
                rescueEndForStart.put(currentRegionStart, jvm.methodData().getLabel(bb.getLabel()));
            }

            if (rescueLabel != null) { // new region
                currentRescue = rescueLabel;
                currentRegionStart = bb.getLabel();
            } else { // end of active region but no new region
                currentRescue = null;
                currentRegionStart = null;
            }
        }

        // handle unclosed final region
        if (currentRegionStart != null) {
            org.objectweb.asm.Label syntheticEnd = new org.objectweb.asm.Label();
            rescueEndForStart.put(currentRegionStart, syntheticEnd);
            syntheticEndForStart.put(currentBlockStart, syntheticEnd);
        }

        if (scope.receivesKeywordArgs()) {
            // pre-frobnicate the args on the way in
            m.loadContext();
            m.loadArgs();
            m.adapter.pushInt(staticScope.getSignature().required());
            m.invokeIRHelper("frobnicateKwargsArgument", sig(IRubyObject[].class, ThreadContext.class, IRubyObject[].class, int.class));
            m.storeArgs();
        }

        for (BasicBlock bb: bbs) {
            org.objectweb.asm.Label start = jvm.methodData().getLabel(bb.getLabel());
            Label rescueLabel = exceptionTable.get(bb);

            m.mark(start);

            // if this is the start of a rescued region, emit trycatch
            org.objectweb.asm.Label end;
            if (rescueLabel != null && (end = rescueEndForStart.get(bb.getLabel())) != null) {
                // first entry into a rescue region, do the try/catch
                org.objectweb.asm.Label rescue = jvm.methodData().getLabel(rescueLabel);
                jvmAdapter().trycatch(start, end, rescue, p(Throwable.class));
            }

            // ensure there's at least one instr per block
            /* FIXME: (CON 20150507) This used to filter blocks that had no instructions and only emit nop for them,
                      but this led to BBs with no *bytecode-emitting* instructions failing to have a nop and triggering
                      verification errors when we attached an exception-handling range to them (because the leading
                      label failed to anchor anywhere, or anchored the same place as the trailing label). Until we can
                      detect that a BB will not emit any code, we return to always emitting the nop. */
            m.adapter.nop();

            /* FIXME: This nextInstr stuff is messy...All emitScopes share a single visitor instance and nesting of new
              scopes is handled by a stack.  Remove the stack and make each nested scope use a fresh visitor. */

            // visit remaining instrs
            List<Instr> instrs = bb.getInstrs();
            int length = instrs.size();

            Instr previousScopeNextInstr = null;     // in case we nested we should check to see if we should save/restore
            if (nextInstr != null) {                 // previous scopes nextInstr.
                previousScopeNextInstr = nextInstr;
            }

            if (length > 0) {
                Instr currentInstr = instrs.get(0);
                for (int i = 1; i < length; i++) {
                    nextInstr = instrs.get(i);
                    visit(currentInstr);
                    currentInstr = nextInstr;
                }
                nextInstr = null;
                visit(currentInstr);
            }

            if (previousScopeNextInstr != null) {
                nextInstr = previousScopeNextInstr;
            }

            org.objectweb.asm.Label syntheticEnd = syntheticEndForStart.get(bb.getLabel());
            if (syntheticEnd != null) {
                m.mark(syntheticEnd);
            }
        }

        jvm.popmethod();
    }

    private StaticScope registerScopeField(IRScope scope, String scopeField) {
        StaticScope staticScope = scope.getStaticScope();
        String staticScopeDescriptor = Helpers.describeScope(staticScope);

        if (staticScopeMap.get(scopeField) == null) {
            staticScopeMap.put(scopeField, staticScope);
            scopeFieldMap.put(staticScope, scopeField);
            staticScopeDescriptorMap.put(scopeField, staticScopeDescriptor);
            jvm.cls().visitField(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC | Opcodes.ACC_VOLATILE, scopeField, ci(StaticScope.class), null, null).visitEnd();
        }

        return staticScope;
    }

    protected void emitVarargsMethodWrapper(IRScope scope, String variableName, String specificName, Signature variableSignature, Signature specificSignature) {
        String scopeField = specificName + "_StaticScope";

        jvm.pushmethod(variableName, scope, scopeField, variableSignature, false);

        IRBytecodeAdapter m = jvmMethod();

        // set line number for backtrace
        jvmMethod().updateLineNumber(scope.getLine());

        // check arity
        org.jruby.runtime.Signature scopeSig = scope.getStaticScope().getSignature();
        checkArity(scopeSig.required(), scopeSig.opt(), scopeSig.hasRest(), scopeSig.hasKwargs(), scopeSig.keyRest());

        // push leading args
        m.loadContext();
        m.loadStaticScope();
        m.loadSelf();

        // unwrap specific args
        if (scopeSig.required() > 0) {
            for (int i = 0; i < scopeSig.required(); i++) {
                m.loadArgs();
                jvmAdapter().pushInt(i);
                jvmAdapter().aaload();
            }
        }

        // push trailing args
        m.loadBlock();
        m.loadFrameClass();
        m.loadFrameName();

        // invoke specific-arity version and return
        Method specificMethod = new Method(specificName, Type.getType(specificSignature.type().returnType()), IRRuntimeHelpers.typesFromSignature(specificSignature));
        jvmAdapter().invokestatic(m.getClassData().clsName, specificName, specificMethod.getDescriptor());
        jvmAdapter().areturn();

        jvm.popmethod();
    }

    protected static final Signature signatureFor(IRScope method, boolean aritySplit) {
        if (aritySplit) {
            StaticScope argScope = method.getStaticScope();
            if (argScope.isArgumentScope() &&
                    argScope.getSignature().isFixed() &&
                    !argScope.getSignature().hasKwargs()) {
                // we have only required arguments...emit a signature appropriate to that arity
                String[] args = new String[argScope.getSignature().required()];
                Class[] types = new Class[args.length]; // Class...
                for (int i = 0; i < args.length; i++) {
                    args[i] = "arg" + i;
                    types[i] = IRubyObject.class;
                }
                return METHOD_SIGNATURE_BASE.insertArgs(BLOCK_ARG_NAME, args, types);
            }
            // we can't do an specific-arity signature
            return null;
        }

        // normal boxed arg list signature
        return METHOD_SIGNATURE_BASE.insertArgs(BLOCK_ARG_NAME, new String[]{"args"}, IRubyObject[].class);
    }

    private static final Signature RUN_SIGNATURE =
            Signature.returning(IRubyObject.class)
            .appendArg("runtime", Ruby.class);

    protected void emitScriptBody(IRScriptBody script) {
        String scopeName = JavaNameMangler.encodeScopeForBacktrace(script);
        String scopeField = scopeName + "_StaticScope";
        String clsName = jvm.scriptToClass(script.getFile());
        Signature signature = signatureFor(script, false);

        // Note: no index attached because there should be at most one script body per .class
        jvm.pushscript(this, clsName, script.getFile());

        defineRunMethod(script, scopeName, scopeField, clsName, signature);

        // proceed with script body compilation
        emitScope(script, scopeName, signature, false, true);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    private void defineRunMethod(IRScriptBody script, String scopeName, String scopeField, String clsName, Signature signature) {
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(
                jvm.cls(),
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "run",
                sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, boolean.class), null, null);
        method.start();

        // save self class
        method.aload(1);
        method.invokeinterface(p(IRubyObject.class), "getMetaClass", sig(RubyClass.class));
        method.astore(3);

        // instantiate script scope
        registerScopeField(script, scopeField);

        // FIXME: duplicated from IRBytecodeAdapter6
        method.getstatic(clsName, scopeField, ci(StaticScope.class));
        method.astore(4);

        method.aload(4);

        org.objectweb.asm.Label after = new org.objectweb.asm.Label();
        method.ifnonnull(after);
        {
            method.ldc(staticScopeDescriptorMap.get(scopeField));
            method.aconst_null();
            method.invokestatic(p(Helpers.class), "restoreScope", sig(StaticScope.class, String.class, StaticScope.class));
            method.astore(4);

            // set scope's module
            method.aload(4);
            method.aload(0);
            method.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
            method.invokevirtual(p(Ruby.class), "getObject", sig(RubyClass.class));
            method.invokevirtual(p(StaticScope.class), "setModule", sig(void.class, RubyModule.class));

            // set scope's filename
            method.aload(4);
            method.ldc(script.getFile());
            method.invokevirtual(p(StaticScope.class), "setFile", sig(void.class, String.class));

            // wrapping logic for load
            method.iload(2);

            org.objectweb.asm.Label nowrap = new org.objectweb.asm.Label();
            method.iffalse(nowrap);
            {
                method.aload(0);
                method.getfield(p(ThreadContext.class), "runtime", ci(Ruby.class));
                method.aload(1); // self
                method.aload(4);
                method.invokevirtual(p(Ruby.class), "setupWrappedToplevel", sig(StaticScope.class, IRubyObject.class, StaticScope.class));
                method.astore(4); // update to new scope

                // set scope's filename
                method.aload(4);
                method.ldc(script.getFile());
                method.invokevirtual(p(StaticScope.class), "setFile", sig(void.class, String.class));
            }
            method.label(nowrap);

            method.aload(4);
            method.putstatic(clsName, scopeField, ci(StaticScope.class));
        }
        method.label(after);

        // execute script method
        method.aload(0); // context
        method.aload(4); // static scope
        method.aload(1); // self
        method.aconst_null(); // args
        method.getstatic(p(Block.class), "NULL_BLOCK", ci(Block.class)); // block
        method.aload(3); // self class
        method.aconst_null(); // call name

        method.invokestatic(clsName, scopeName, sig(signature.type().returnType(), signature.type().parameterArray()));
        method.areturn();

        method.end();
    }

    protected void emitMethod(IRMethod method, JVMVisitorMethodContext context) {
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + '$' + methodIndex++;

        emitWithSignatures(method, context, name);
    }

    protected void emitMethodJIT(IRMethod method, JVMVisitorMethodContext context) {
        String clsName = jvm.scriptToClass(method.getFile());
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + '$' + methodIndex++;
        jvm.pushscript(this, clsName, method.getFile());

        emitWithSignatures(method, context, name);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    protected void emitBlockJIT(IRClosure closure, JVMVisitorMethodContext context) {
        String clsName = jvm.scriptToClass(closure.getFile());
        String name = JavaNameMangler.encodeScopeForBacktrace(closure) + '$' + methodIndex++;
        jvm.pushscript(this, clsName, closure.getFile());

        emitScope(closure, name, CLOSURE_SIGNATURE, false, true);

        context.setBaseName(name);
        context.setVariableName(name);

        jvm.cls().visitEnd();
        jvm.popclass();
    }

    private void emitWithSignatures(IRMethod method, JVMVisitorMethodContext context, String name) {
        context.setBaseName(name);

        Signature specificSig = signatureFor(method, true);

        if (specificSig == null) {
            // only varargs, so use name as is
            context.setVariableName(name);
            Signature signature = signatureFor(method, false);
            emitScope(method, name, signature, false, true);
            context.addNativeSignature(-1, signature.type());
        } else {
            String specificName = name;

            context.setSpecificName(specificName);

            emitScope(method, specificName, specificSig, true, true);
            context.addNativeSignature(method.getStaticScope().getSignature().required(), specificSig.type());

            // specific arity path, so mangle the dummy varargs wrapper
            String variableName = name + JavaNameMangler.VARARGS_MARKER;

            context.setVariableName(variableName);

            emitVarargsMethodWrapper(method, variableName, specificName, METHOD_SIGNATURE_VARARGS, specificSig);
            context.addNativeSignature(-1, METHOD_SIGNATURE_VARARGS.type());
        }
    }

    protected Handle emitModuleBodyJIT(IRModuleBody method) {
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + '$' + methodIndex++;

        String clsName = jvm.scriptToClass(method.getFile());
        jvm.pushscript(this, clsName, method.getFile());

        Signature signature = signatureFor(method, false);
        emitScope(method, name, signature, false, true);

        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                jvm.classData().clsName,
                name,
                sig(signature.type().returnType(), signature.type().parameterArray()),
                false);

        jvm.cls().visitEnd();
        jvm.popclass();

        return handle;
    }

    private void emitClosures(IRScope s, boolean print) {
        // Emit code for all nested closures
        for (IRClosure c: s.getClosures()) {
            emitClosure(c, print);
        }
    }

    protected Handle emitClosure(IRClosure closure, boolean print) {
        /* Compile the closure like a method */
        String name = JavaNameMangler.encodeScopeForBacktrace(closure) + '$' + methodIndex++;

        emitScope(closure, name, CLOSURE_SIGNATURE, false, print);

        Handle handle = new Handle(
                Opcodes.H_INVOKESTATIC,
                jvm.classData().clsName,
                name,
                sig(CLOSURE_SIGNATURE.type().returnType(), CLOSURE_SIGNATURE.type().parameterArray()),
                false);

        closuresMap.put(closure, handle);

        return handle;
    }

    protected Handle emitModuleBody(IRModuleBody method) {
        String name = JavaNameMangler.encodeScopeForBacktrace(method) + '$' + methodIndex++;

        Signature signature = signatureFor(method, false);
        emitScope(method, name, signature, false, true);

        return new Handle(
                Opcodes.H_INVOKESTATIC,
                jvm.classData().clsName,
                name,
                sig(signature.type().returnType(), signature.type().parameterArray()),
                false);
    }

    public void visit(Instr instr) {
        if (DEBUG) { // debug will skip emitting actual file line numbers
            jvmAdapter().line(++jvmMethod().ipc);
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
        if (variable instanceof LocalVariable) {
            jvmLoadLocal(DYNAMIC_SCOPE);

            jvmAdapter().swap();

            genSetValue((LocalVariable) variable);
        } else if (variable instanceof TemporaryLocalVariable) {
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

    private void jvmStoreLocal(Runnable source, Variable variable) {
        if (variable instanceof LocalVariable) {
            jvmLoadLocal(DYNAMIC_SCOPE);

            source.run();

            genSetValue((LocalVariable) variable);
        } else if (variable instanceof TemporaryLocalVariable) {
            source.run();

            switch (((TemporaryLocalVariable)variable).getType()) {
                case FLOAT: jvmAdapter().dstore(getJVMLocalVarIndex(variable)); break;
                case FIXNUM: jvmAdapter().lstore(getJVMLocalVarIndex(variable)); break;
                case BOOLEAN: jvmAdapter().istore(getJVMLocalVarIndex(variable)); break;
                default: jvmMethod().storeLocal(getJVMLocalVarIndex(variable)); break;
            }
        } else {
            source.run();

            jvmMethod().storeLocal(getJVMLocalVarIndex(variable));
        }
    }

    private void genSetValue(LocalVariable localvariable) {
        int depth = localvariable.getScopeDepth();
        int location = localvariable.getLocation();

        String baseName = p(DynamicScope.class);

        if (depth == 0) {
            if (location < DynamicScopeGenerator.SPECIALIZED_SETS.size()) {
                jvmAdapter().invokevirtual(baseName, DynamicScopeGenerator.SPECIALIZED_SETS.get(location), sig(void.class, IRubyObject.class));
            } else {
                jvmAdapter().pushInt(location);
                jvmAdapter().invokevirtual(baseName, "setValueDepthZeroVoid", sig(void.class, IRubyObject.class, int.class));
            }
        } else {
            jvmAdapter().pushInt(location);
            jvmAdapter().pushInt(depth);

            jvmAdapter().invokevirtual(baseName, "setValueVoid", sig(void.class, IRubyObject.class, int.class, int.class));
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
        visit(aliasInstr.getNewName());
        visit(aliasInstr.getOldName());
        m.invokeIRHelper("defineAlias", sig(void.class, ThreadContext.class, IRubyObject.class, DynamicScope.class, IRubyObject.class, IRubyObject.class));
    }

    @Override
    public void ArrayDerefInstr(ArrayDerefInstr arrayderefinstr) {
        jvmMethod().loadContext();
        jvmMethod().loadSelf();
        visit(arrayderefinstr.getReceiver());
        visit(arrayderefinstr.getKey());
        jvmMethod().getInvocationCompiler().invokeArrayDeref(file, jvm.methodData().scopeField, arrayderefinstr);
        jvmStoreLocal(arrayderefinstr.getResult());
    }

    @Override
    public void AsStringInstr(AsStringInstr asstring) {
        jvmMethod().loadContext();
        jvmMethod().loadSelf();
        visit(asstring.getReceiver());
        jvmMethod().getInvocationCompiler().invokeOther(file, jvm.methodData().scopeField, asstring, 0);
        jvmAdapter().invokeinterface(p(IRubyObject.class), "asString", sig(RubyString.class));
        jvmStoreLocal(asstring.getResult());
    }

    @Override
    public void AttrAssignInstr(AttrAssignInstr attrAssignInstr) {
        compileCallCommon(jvmMethod(), attrAssignInstr);
    }

    @Override
    public void BFalseInstr(BFalseInstr bFalseInstr) {
        Operand arg1 = bFalseInstr.getArg1();
        visit(arg1);

        // this is a gross hack because we don't have distinction in boolean instrs between boxed and unboxed
        if (arg1 instanceof TemporaryBooleanVariable || arg1 instanceof UnboxedBoolean) {
            jvmMethod().getBranchCompiler().bfalse(getJVMLabel(bFalseInstr.getJumpTarget())); // no need to unbox
        } else {
            jvmAdapter().invokeinterface(p(IRubyObject.class), "isTrue", sig(boolean.class)); // unbox
            jvmMethod().getBranchCompiler().bfalse(getJVMLabel(bFalseInstr.getJumpTarget()));
        }
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
        jvmMethod().getBranchCompiler().branchIfNil(getJVMLabel(bnilinstr.getJumpTarget()));
    }

    @Override
    public void BreakInstr(BreakInstr breakInstr) {
        jvmMethod().loadContext();
        jvmLoadLocal(DYNAMIC_SCOPE);
        visit(breakInstr.getReturnValue());
        jvmMethod().loadSelfBlock();
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "initiateBreak", sig(IRubyObject.class, ThreadContext.class, DynamicScope.class, IRubyObject.class, Block.class));
        jvmMethod().returnValue();

    }

    // max (case) values span that generates a tableswitch instruction
    //   case 0, 2, 4, 8, 16, 32 still generates a tableswitch (filling in holes)
    //   case 0, 10, 100 fallbacks to generating a lookupswitch
    private static final int MAX_TABLE_SWITCH_SIZE = 32 + 1;

    public void BSwitchInstr(BSwitchInstr bswitchinstr) {
        visit(bswitchinstr.getCaseOperand());
        jvmAdapter().dup();
        jvmAdapter().instance_of(p(RubyFixnum.class));
        org.objectweb.asm.Label rubyCaseLabel = getJVMLabel(bswitchinstr.getRubyCaseLabel());
        org.objectweb.asm.Label notFixnum = new org.objectweb.asm.Label();
        jvmAdapter().iffalse(notFixnum);
        jvmAdapter().checkcast(p(RubyFixnum.class));
        jvmAdapter().invokevirtual(p(RubyFixnum.class), "getIntValue", sig(int.class));
        Label[] targets = bswitchinstr.getTargets();
        org.objectweb.asm.Label[] jvmTargets = new org.objectweb.asm.Label[targets.length];
        for (int i = 0; i < targets.length; i++) jvmTargets[i] = getJVMLabel(targets[i]);
        org.objectweb.asm.Label defaultTarget = getJVMLabel(bswitchinstr.getElseTarget());
        // if jump table is all contiguous values, use a tableswitch
        int[] jumps = bswitchinstr.getJumps(); // always ordered e.g. [2, 3, 4]

        int low = jumps[0]; // 2
        int high = jumps[jumps.length - 1]; // 4
        int span = high - low + 1;
        if (span == jumps.length) { // perfectly compact - no "holes"
            jvmAdapter().tableswitch(low, high, defaultTarget, jvmTargets);
        } else if (span <= MAX_TABLE_SWITCH_SIZE) { // an imperfect switch
            org.objectweb.asm.Label[] realTargets = jvmTargets;
            jvmTargets = new org.objectweb.asm.Label[span];
            jvmTargets[0] = realTargets[0]; int p = jumps[0] + 1; int t = 1;
            for (int i = 1; i < jumps.length; i++) {
                int cj = jumps[i];
                if (cj == p) {
                    jvmTargets[t++] = realTargets[i]; p = cj + 1;
                }
                else { // fill in holes with cases to jump to else part
                    while (cj > p++) jvmTargets[t++] = defaultTarget;
                    jvmTargets[t++] = realTargets[i];
                }
            }
            jvmAdapter().tableswitch(low, high, defaultTarget, jvmTargets);
        } else {
            jvmAdapter().lookupswitch(defaultTarget, bswitchinstr.getJumps(), jvmTargets);
        }
        jvmAdapter().label(notFixnum);
        jvmAdapter().pop();
        jvmAdapter().label(rubyCaseLabel);
    }

    @Override
    public void BTrueInstr(BTrueInstr btrueinstr) {
        Operand arg1 = btrueinstr.getArg1();

        if (omitStoreLoad) {
            omitStoreLoad = false;
        } else {
            visit(arg1);
        }

        // this is a gross hack because we don't have distinction in boolean instrs between boxed and unboxed
        if (arg1 instanceof TemporaryBooleanVariable || arg1 instanceof UnboxedBoolean) {
            jvmMethod().getBranchCompiler().btrue(getJVMLabel(btrueinstr.getJumpTarget())); // no need to unbox, just branch
        } else {
            jvmMethod().getBranchCompiler().branchIfTruthy(getJVMLabel(btrueinstr.getJumpTarget())); // unbox and branch
        }
    }

    @Override
    public void BUndefInstr(BUndefInstr bundefinstr) {
        visit(bundefinstr.getArg1());
        jvmMethod().getValueCompiler().pushUndefined();
        jvmAdapter().if_acmpeq(getJVMLabel(bundefinstr.getJumpTarget()));
    }

    @Override
    public void BuildBackrefInstr(BuildBackrefInstr instr) {
        jvmMethod().loadContext();

        switch (instr.type) {
            case '&':
                jvmAdapter().invokevirtual(p(ThreadContext.class), "last_match", sig(IRubyObject.class));
                break;
            case '`':
                jvmAdapter().invokevirtual(p(ThreadContext.class), "match_pre", sig(IRubyObject.class));
                break;
            case '\'':
                jvmAdapter().invokevirtual(p(ThreadContext.class), "match_post", sig(IRubyObject.class));
                break;
            case '+':
                jvmAdapter().invokevirtual(p(ThreadContext.class), "match_last", sig(IRubyObject.class));
                break;
            default:
                assert false: "backref with invalid type";
        }
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BuildCompoundArrayInstr(BuildCompoundArrayInstr instr) {
        if (instr.isArgsPush()) {
            visit(instr.getAppendingArg());
            visit(instr.getAppendedArg());
            jvmMethod().invokeHelper("argsPush", RubyArray.class, IRubyObject.class, IRubyObject.class);
        } else {
            jvmMethod().loadContext();
            visit(instr.getAppendingArg());
            visit(instr.getAppendedArg());
            jvmMethod().invokeHelper("argsCat", RubyArray.class, ThreadContext.class, IRubyObject.class, IRubyObject.class);
        }
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BuildCompoundStringInstr(BuildCompoundStringInstr compoundstring) {
        jvmMethod().getValueCompiler().pushEmptyString(compoundstring.getEncoding());
        for (Operand p : compoundstring.getPieces()) {
            if (p instanceof StringLiteral) {
                // we have bytelist and CR in hand, go straight to cat logic
                StringLiteral str = (StringLiteral) p;
                jvmMethod().getValueCompiler().pushByteList(str.getByteList());
                jvmAdapter().pushInt(str.getCodeRange());
                jvmAdapter().invokevirtual(p(RubyString.class), "cat", sig(RubyString.class, ByteList.class, int.class));
            } else {
                visit(p);
                jvmAdapter().invokevirtual(p(RubyString.class), "appendAsDynamicString", sig(RubyString.class, IRubyObject.class));
            }
        }
        if (compoundstring.isFrozen()) {
            if (runtime.getInstanceConfig().isDebuggingFrozenStringLiteral()) {
                jvmMethod().loadContext();
                jvmAdapter().ldc(compoundstring.getFile());
                jvmAdapter().ldc(compoundstring.getLine());
                jvmMethod().invokeIRHelper("freezeLiteralString", sig(RubyString.class, RubyString.class, ThreadContext.class, String.class, int.class));
            } else {
                jvmMethod().invokeIRHelper("freezeLiteralString", sig(RubyString.class, RubyString.class));
            }
        }
        jvmStoreLocal(compoundstring.getResult());
    }

    @Override
    public void BuildDynRegExpInstr(BuildDynRegExpInstr instr) {
        final IRBytecodeAdapter m = jvmMethod();

        if (instr.getOptions().isOnce() && instr.getRegexp() != null) {
            visit(new Regexp(instr.getRegexp().source().convertToString().getByteList(), instr.getOptions()));
            jvmStoreLocal(instr.getResult());
            return;
        }

        RegexpOptions options = instr.getOptions();
        final Operand[] operands = instr.getPieces();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                m.loadContext();
                for (int i = 0; i < operands.length; i++) {
                    Operand operand = operands[i];
                    visit(operand);
                    jvmAdapter().invokeinterface(p(IRubyObject.class), "asString", sig(RubyString.class));
                }
            }
        };

        m.getDynamicValueCompiler().pushDRegexp(r, options, operands.length);

        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BuildRangeInstr(BuildRangeInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getBegin());
        visit(instr.getEnd());
        jvmAdapter().invokestatic(p(RubyRange.class), instr.isExclusive() ? "newExclusiveRange" : "newInclusiveRange", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void BuildSplatInstr(BuildSplatInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getArray());
        jvmAdapter().ldc(instr.getDup());
        jvmMethod().invokeIRHelper("splatArray", sig(RubyArray.class, ThreadContext.class, IRubyObject.class, boolean.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void CallInstr(CallInstr callInstr) {
        if (callInstr instanceof OneFixnumArgNoBlockCallInstr) {
            oneFixnumArgNoBlockCallInstr((OneFixnumArgNoBlockCallInstr) callInstr);
            return;
        } else if (callInstr instanceof OneFloatArgNoBlockCallInstr) {
            oneFloatArgNoBlockCallInstr((OneFloatArgNoBlockCallInstr) callInstr);
            return;
        }

        compileCallCommon(jvmMethod(), callInstr);
    }

    private void compileCallCommon(IRBytecodeAdapter m, CallBase call) {
        Operand[] args = call.getCallArgs();
        BlockPassType blockPassType = BlockPassType.fromIR(call);
        m.loadContext();
        m.loadSelf(); // caller
        visit(call.getReceiver());
        int arity = args.length;

        if (args.length == 1 && args[0] instanceof Splat) {
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

        if (blockPassType.given()) {
            m.loadContext();
            if (call.isPotentiallyRefined()) m.loadStaticScope();
            visit(call.getClosureArg());
            if (call.isPotentiallyRefined()) {
                m.invokeIRHelper("getRefinedBlockFromObject", sig(Block.class, ThreadContext.class, StaticScope.class, Object.class));
            } else {
                m.invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
            }
        }

        switch (call.getCallType()) {
            case FUNCTIONAL:
            case VARIABLE:
                m.getInvocationCompiler().invokeSelf(file, jvm.methodData().scopeField, call, arity);
                break;
            case NORMAL:
                m.getInvocationCompiler().invokeOther(file, jvm.methodData().scopeField, call, arity);
                break;
        }

        Variable result = call.getResult();
        if (result != null) {
            if (!omitStoreLoad) jvmStoreLocal(result);
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
        jvmAdapter().pushBoolean(checkargsarrayarityinstr.rest);
        jvmMethod().invokeStatic(Type.getType(Helpers.class), Method.getMethod("void irCheckArgsArrayArity(org.jruby.runtime.ThreadContext, org.jruby.RubyArray, int, int, boolean)"));
    }

    @Override
    public void CheckArityInstr(CheckArityInstr checkarityinstr) {
        if (jvm.methodData().specificArity >= 0) {
            // no arity check in specific arity path
        } else {
            checkArity(checkarityinstr.required, checkarityinstr.opt, checkarityinstr.rest, checkarityinstr.receivesKeywords, checkarityinstr.restKey);
        }
    }

    private void checkArity(int required, int opt, boolean rest, boolean receivesKeywords, int restKey) {
        jvmMethod().loadContext();
        jvmMethod().loadStaticScope();
        jvmMethod().loadArgs();
        jvmMethod().loadSelfBlock();
        jvmAdapter().invokedynamic(
                "checkArity",
                sig(void.class, ThreadContext.class, StaticScope.class, Object[].class, Block.class),
                Bootstrap.CHECK_ARITY,
                required, opt, rest ? 1 : 0, receivesKeywords ? 1 : 0, restKey);
    }

    @Override
    public void CheckForLJEInstr(CheckForLJEInstr checkForljeinstr) {
        jvmMethod().loadContext();
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmAdapter().ldc(checkForljeinstr.isDefinedWithinMethod());
        jvmMethod().loadSelfBlock();
        jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "checkForLJE", sig(void.class, ThreadContext.class, DynamicScope.class, boolean.class, Block.class));
    }

        @Override
    public void ClassSuperInstr(ClassSuperInstr classsuperinstr) {
        String name = classsuperinstr.getId();
        Operand[] args = classsuperinstr.getCallArgs();
        Operand definingModule = classsuperinstr.getDefiningModule();
        boolean[] splatMap = classsuperinstr.splatMap();
        Operand closure = classsuperinstr.getClosureArg(null);

        superCommon(name, classsuperinstr, args, definingModule, splatMap, closure);
    }

    @Override
    public void CopyInstr(CopyInstr copyinstr) {
        Operand  src = copyinstr.getSource();
        Variable res = copyinstr.getResult();

        storeHeapOrStack(src, res);
    }

    private void storeHeapOrStack(final Operand value, final Variable res) {
        jvmStoreLocal(new Runnable() {
            @Override
            public void run() {
                if (res instanceof TemporaryFloatVariable) {
                    loadFloatArg(value);
                } else if (res instanceof TemporaryFixnumVariable) {
                    loadFixnumArg(value);
                } else {
                    visit(value);
                }
            }
        }, res);
    }

    @Override
    public void DefineClassInstr(DefineClassInstr defineclassinstr) {
        IRClassBody newIRClassBody = defineclassinstr.getNewIRClassBody();

        jvmMethod().loadContext(); // for invokeModuleBody

        jvmMethod().loadContext();
        Handle handle = emitModuleBody(newIRClassBody);
        jvmMethod().pushHandle(handle);
        jvmAdapter().ldc(newIRClassBody.getId());
        jvmAdapter().ldc(newIRClassBody.getLine());

        jvmMethod().getStaticScope(handle.getName() + "_StaticScope");
        visit(defineclassinstr.getContainer());
        visit(defineclassinstr.getSuperClass());
        jvmAdapter().ldc(newIRClassBody.maybeUsingRefinements());
        jvmMethod().invokeIRHelper("newCompiledClassBody", sig(DynamicMethod.class, ThreadContext.class,
                java.lang.invoke.MethodHandle.class, String.class, int.class, StaticScope.class, Object.class, Object.class, boolean.class));

        jvmMethod().invokeIRHelper("invokeModuleBody", sig(IRubyObject.class, ThreadContext.class, DynamicMethod.class));
        jvmStoreLocal(defineclassinstr.getResult());
    }

    @Override
    public void DefineClassMethodInstr(DefineClassMethodInstr defineclassmethodinstr) {
        IRMethod method = defineclassmethodinstr.getMethod();

        jvmMethod().loadContext();

        JVMVisitorMethodContext context = new JVMVisitorMethodContext();
        emitMethod(method, context);

        String defSignature = pushHandlesForDef(
                context.getVariableName(),
                context.getSpecificName(),
                context.getNativeSignaturesExceptVariable(),
                METHOD_SIGNATURE_VARARGS.type(),
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, String.class, int.class, StaticScope.class, String.class, IRubyObject.class, boolean.class, boolean.class, boolean.class),
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, java.lang.invoke.MethodHandle.class, int.class, String.class, int.class, StaticScope.class, String.class, IRubyObject.class, boolean.class, boolean.class, boolean.class));

        jvmAdapter().ldc(method.getId());
        jvmAdapter().ldc(method.getLine());
        jvmMethod().getStaticScope(context.getBaseName() + "_StaticScope");
        jvmAdapter().ldc(ArgumentDescriptor.encode(method.getArgumentDescriptors()));
        visit(defineclassmethodinstr.getContainer());
        jvmAdapter().ldc(method.maybeUsingRefinements());
        jvmAdapter().ldc(method.receivesKeywordArgs());
        jvmAdapter().ldc(method.getFullInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS));

        // add method
        jvmMethod().adapter.invokestatic(p(IRRuntimeHelpers.class), "defCompiledClassMethod", defSignature);
    }

    // SSS FIXME: Needs an update to reflect instr. change
    @Override
    public void DefineInstanceMethodInstr(DefineInstanceMethodInstr defineinstancemethodinstr) {
        IRMethod method = defineinstancemethodinstr.getMethod();
        JVMVisitorMethodContext context = new JVMVisitorMethodContext();

        IRBytecodeAdapter   m = jvmMethod();
        SkinnyMethodAdapter a = m.adapter;

        m.loadContext();

        emitMethod(method, context);

        MethodType variable = context.getNativeSignature(-1); // always a variable arity handle
        assert(variable != null);

        String defSignature = pushHandlesForDef(
                context.getVariableName(),
                context.getSpecificName(),
                context.getNativeSignaturesExceptVariable(),
                variable,
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, String.class, int.class, StaticScope.class, String.class, DynamicScope.class, IRubyObject.class, boolean.class, boolean.class, boolean.class),
                sig(void.class, ThreadContext.class, java.lang.invoke.MethodHandle.class, java.lang.invoke.MethodHandle.class, int.class, String.class, int.class, StaticScope.class, String.class, DynamicScope.class, IRubyObject.class, boolean.class, boolean.class, boolean.class));

        a.ldc(method.getId());
        a.ldc(method.getLine());
        jvmMethod().getStaticScope(context.getBaseName() + "_StaticScope");
        jvmAdapter().ldc(ArgumentDescriptor.encode(method.getArgumentDescriptors()));
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmMethod().loadSelf();
        jvmAdapter().ldc(method.maybeUsingRefinements());
        jvmAdapter().ldc(method.receivesKeywordArgs());
        jvmAdapter().ldc(method.getFullInterpreterContext().getFlags().contains(IRFlags.REQUIRES_CLASS));

        // add method
        a.invokestatic(p(IRRuntimeHelpers.class), "defCompiledInstanceMethod", defSignature);
    }

    private String pushHandlesForDef(String variableName, String specificName, IntHashMap<MethodType> signaturesExceptVariable, MethodType variable, String variableOnly, String variableAndSpecific) {
        String defSignature;

        jvmMethod().pushHandle(new Handle(
                Opcodes.H_INVOKESTATIC,
                jvm.classData().clsName,
                variableName,
                sig(variable.returnType(), variable.parameterArray()),
                false));

        if (signaturesExceptVariable.size() == 0) {
            defSignature = variableOnly;
        } else {
            defSignature = variableAndSpecific;

            for (IntHashMap.Entry<MethodType> entry : signaturesExceptVariable.entrySet()) {
                jvmMethod().pushHandle(new Handle(
                        Opcodes.H_INVOKESTATIC,
                        jvm.classData().clsName,
                        specificName,
                        sig(entry.getValue().returnType(), entry.getValue().parameterArray()),
                        false));
                jvmAdapter().pushInt(entry.getKey());
                break; // FIXME: only supports one arity
            }
        }
        return defSignature;
    }

    @Override
    public void DefineMetaClassInstr(DefineMetaClassInstr definemetaclassinstr) {
        IRModuleBody metaClassBody = definemetaclassinstr.getMetaClassBody();

        Handle bodyHandle = emitModuleBody(metaClassBody);
        String scopeField = bodyHandle.getName() + "_StaticScope";

        String clsName = jvm.classData().clsName;

        Handle scopeHandle = new Handle(
                Opcodes.H_GETSTATIC,
                clsName,
                scopeField,
                ci(StaticScope.class),
                false);

        Handle setScopeHandle = new Handle(
                Opcodes.H_PUTSTATIC,
                clsName,
                scopeField,
                ci(StaticScope.class),
                false);

        jvmMethod().loadContext();
        visit(definemetaclassinstr.getObject());
        jvmAdapter().ldc(staticScopeDescriptorMap.get(scopeField));
        jvmMethod().loadStaticScope();

        jvmAdapter().invokedynamic(
                "openMetaClass",
                sig(DynamicMethod.class, ThreadContext.class, IRubyObject.class, String.class, StaticScope.class),
                Bootstrap.OPEN_META_CLASS,
                bodyHandle,
                scopeHandle,
                setScopeHandle,
                metaClassBody.getLine(),
                metaClassBody.getFullInterpreterContext().isDynamicScopeEliminated() ? 1 : 0,
                metaClassBody.maybeUsingRefinements() ? 1 : 0);

        jvmStoreLocal(definemetaclassinstr.getResult());
    }

    @Override
    public void DefineModuleInstr(DefineModuleInstr definemoduleinstr) {
        IRModuleBody newIRModuleBody = definemoduleinstr.getNewIRModuleBody();

        jvmMethod().loadContext(); // for invokeModuleBody

        jvmMethod().loadContext();
        Handle handle = emitModuleBody(newIRModuleBody);
        jvmMethod().pushHandle(handle);
        jvmAdapter().ldc(newIRModuleBody.getId());
        jvmAdapter().ldc(newIRModuleBody.getLine());
        jvmMethod().getStaticScope(handle.getName() + "_StaticScope");
        visit(definemoduleinstr.getContainer());
        jvmAdapter().ldc(newIRModuleBody.maybeUsingRefinements());
        jvmMethod().invokeIRHelper("newCompiledModuleBody", sig(DynamicMethod.class, ThreadContext.class,
                java.lang.invoke.MethodHandle.class, String.class, int.class, StaticScope.class, Object.class, boolean.class));

        jvmMethod().invokeIRHelper("invokeModuleBody", sig(IRubyObject.class, ThreadContext.class, DynamicMethod.class));
        jvmStoreLocal(definemoduleinstr.getResult());
    }

    // FIXME: We need to make a system for this which should exist elsewhere but I am orgniazing simple opts
    // as private methods for now:

    // 'when' statements emit a 1;1 use:def temporary which we can omit and just leave eqq result on the stack.
    private boolean canOmitStoreLoad(EQQInstr eqq, Instr nextInstr) {
        assert nextInstr != null: "Somehow EQQ is the last instr in the scope...";

        return nextInstr instanceof BTrueInstr && eqq.getResult().equals(((BTrueInstr) nextInstr).getArg1());
    }

    @Override
    public void EQQInstr(EQQInstr eqqinstr) {
        omitStoreLoad = canOmitStoreLoad(eqqinstr, nextInstr);

        if (!eqqinstr.isSplattedValue() && !(eqqinstr.getArg1() instanceof UndefinedValue)) {
            compileCallCommon(jvmMethod(), eqqinstr);
        } else {
            jvmMethod().loadContext();
            visit(eqqinstr.getReceiver());
            visit(eqqinstr.getArg1());
            jvmMethod().getInvocationCompiler().invokeEQQ(eqqinstr);
            if (!omitStoreLoad) jvmStoreLocal(eqqinstr.getResult());
        }
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
        jvmAdapter().ldc(getclassvariableinstr.getId());
        jvmAdapter().invokevirtual(p(RubyModule.class), "getClassVar", sig(IRubyObject.class, String.class));
        jvmStoreLocal(getclassvariableinstr.getResult());
    }

    @Override
    public void GetFieldInstr(GetFieldInstr getfieldinstr) {
        visit(getfieldinstr.getSource());
        jvmMethod().getInstanceVariableCompiler().getField(getfieldinstr.getId());
        jvmStoreLocal(getfieldinstr.getResult());
    }

    @Override
    public void GetGlobalVariableInstr(GetGlobalVariableInstr getglobalvariableinstr) {
        jvmMethod().getGlobalVariableCompiler().getGlobalVariable(getglobalvariableinstr.getTarget().getId(), file);
        jvmStoreLocal(getglobalvariableinstr.getResult());
    }

    @Override
    public void GVarAliasInstr(GVarAliasInstr gvaraliasinstr) {
        jvmMethod().getValueCompiler().pushRuntime();
        visit(gvaraliasinstr.getNewName());
        visit(gvaraliasinstr.getOldName());
        jvmMethod().invokeIRHelper("aliasGlobalVariable", sig(void.class, Ruby.class, Object.class, Object.class));
    }

    @Override
    public void InheritanceSearchConstInstr(InheritanceSearchConstInstr inheritancesearchconstinstr) {
        jvmMethod().loadContext();
        visit(inheritancesearchconstinstr.getCurrentModule());

        jvmMethod().getConstantCompiler().inheritanceSearchConst(inheritancesearchconstinstr.getId(), inheritancesearchconstinstr.getName().getBytes());
        jvmStoreLocal(inheritancesearchconstinstr.getResult());
    }

    @Override
    public void InstanceSuperInstr(InstanceSuperInstr instancesuperinstr) {
        String name = instancesuperinstr.getId();
        Operand[] args = instancesuperinstr.getCallArgs();
        Operand definingModule = instancesuperinstr.getDefiningModule();
        boolean[] splatMap = instancesuperinstr.splatMap();
        Operand closure = instancesuperinstr.getClosureArg(null);

        superCommon(name, instancesuperinstr, args, definingModule, splatMap, closure);
    }

    private void superCommon(String name, CallInstr instr, Operand[] args, Operand definingModule, boolean[] splatMap, Operand closure) {
        IRBytecodeAdapter m = jvmMethod();
        Operation operation = instr.getOperation();

        m.loadContext();
        m.loadSelf(); // TODO: get rid of caller
        m.loadSelf();
        if (definingModule == UndefinedValue.UNDEFINED) {
            // Not used in eventual call
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

        boolean hasClosure = closure != null;
        boolean literalClosure = closure instanceof WrappedIRClosure;
        if (hasClosure) {
            m.loadContext();
            if (instr.isPotentiallyRefined()) m.loadStaticScope();
            visit(closure);
            if (instr.isPotentiallyRefined()) {
                m.invokeIRHelper("getRefinedBlockFromObject", sig(Block.class, ThreadContext.class, StaticScope.class, Object.class));
            } else {
                m.invokeIRHelper("getBlockFromObject", sig(Block.class, ThreadContext.class, Object.class));
            }
        }

        switch (operation) {
            case INSTANCE_SUPER:
                m.getInvocationCompiler().invokeInstanceSuper(file, name, args.length, hasClosure, literalClosure, splatMap);
                break;
            case CLASS_SUPER:
                m.getInvocationCompiler().invokeClassSuper(file, name, args.length, hasClosure, literalClosure, splatMap);
                break;
            case UNRESOLVED_SUPER:
                m.getInvocationCompiler().invokeUnresolvedSuper(file, name, args.length, hasClosure, literalClosure, splatMap);
                break;
            case ZSUPER:
                m.getInvocationCompiler().invokeZSuper(file, name, args.length, hasClosure, splatMap);
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

        jvmMethod().getConstantCompiler().lexicalSearchConst(lexicalsearchconstinstr.getId(), lexicalsearchconstinstr.getName().getBytes());

        jvmStoreLocal(lexicalsearchconstinstr.getResult());
    }

    @Override
    public void LineNumberInstr(LineNumberInstr linenumberinstr) {
        if (DEBUG) return; // debug mode uses IPC for line numbers

        jvmMethod().updateLineNumber(linenumberinstr.getLineNumber());

        if (linenumberinstr.coverage) {
            jvmMethod().loadContext();
            jvmAdapter().invokedynamic(
                    "coverLine",
                    sig(void.class, ThreadContext.class),
                    Bootstrap.coverLineHandle(),
                    jvm.methodData().scope.getFile(),
                    linenumberinstr.getLineNumber(),
                    linenumberinstr.oneshot ? 1 : 0);
        }
    }

    @Override
    public void LoadLocalVarInstr(LoadLocalVarInstr loadlocalvarinstr) {
        LocalVariable(loadlocalvarinstr.getLocalVar());
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
    public void LoadBlockImplicitClosure(LoadBlockImplicitClosureInstr loadblockimplicitclosureinstr) {
        jvmMethod().loadSelfBlock();
        jvmMethod().invokeHelper("getImplicitBlockFromBlockBinding", Block.class, Block.class);
        jvmStoreLocal(loadblockimplicitclosureinstr.getResult());
    }

    @Override
    public void MatchInstr(MatchInstr matchInstr) {
        compileCallCommon(jvmMethod(), matchInstr);
    }

    @Override
    public void ModuleVersionGuardInstr(ModuleVersionGuardInstr moduleversionguardinstr) {
        // SSS FIXME: Unused at this time
        //throw new NotCompilableException("Unsupported instruction: " + moduleversionguardinstr);
    }

    @Override
    public void NopInstr(NopInstr nopinstr) {
        // do nothing
    }

    @Override
    public void NoResultCallInstr(NoResultCallInstr noResultCallInstr) {
        compileCallCommon(jvmMethod(), noResultCallInstr);
    }

    public void oneFixnumArgNoBlockCallInstr(OneFixnumArgNoBlockCallInstr oneFixnumArgNoBlockCallInstr) {
        IRBytecodeAdapter m = jvmMethod();
        long fixnum = oneFixnumArgNoBlockCallInstr.getFixnumArg();
        Operand receiver = oneFixnumArgNoBlockCallInstr.getReceiver();
        Variable result = oneFixnumArgNoBlockCallInstr.getResult();

        m.loadContext();

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        m.loadSelf(); // caller

        visit(receiver);

        m.getInvocationCompiler().invokeOtherOneFixnum(file, oneFixnumArgNoBlockCallInstr, fixnum);

        if (result != null) {
            jvmStoreLocal(result);
        } else {
            // still need to drop, since all dyncalls return something (FIXME)
            m.adapter.pop();
        }
    }

    public void oneFloatArgNoBlockCallInstr(OneFloatArgNoBlockCallInstr oneFloatArgNoBlockCallInstr) {
        IRBytecodeAdapter m = jvmMethod();
        double flote = oneFloatArgNoBlockCallInstr.getFloatArg();
        Operand receiver = oneFloatArgNoBlockCallInstr.getReceiver();
        Variable result = oneFloatArgNoBlockCallInstr.getResult();

        m.loadContext();

        // for visibility checking without requiring frame self
        // TODO: don't bother passing when fcall or vcall, and adjust callsite appropriately
        m.loadSelf(); // caller

        visit(receiver);

        m.getInvocationCompiler().invokeOtherOneFloat(file, oneFloatArgNoBlockCallInstr, flote);

        if (result != null) {
            jvmStoreLocal(result);
        } else {
            // still need to drop, since all dyncalls return something (FIXME)
            m.adapter.pop();
        }
    }

    @Override
    public void OptArgMultipleAsgnInstr(OptArgMultipleAsgnInstr optargmultipleasgninstr) {
        visit(optargmultipleasgninstr.getArray());
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
    public void PopBlockFrameInstr(PopBlockFrameInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getFrame());
        jvmAdapter().invokevirtual(p(ThreadContext.class), "postYieldNoScope", sig(void.class, Frame.class));
    }

    @Override
    public void PopMethodFrameInstr(PopMethodFrameInstr popframeinstr) {
        jvmMethod().loadContext();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void postMethodFrameOnly()"));
    }

    @Override
    public void PopBackrefFrameInstr(PopBackrefFrameInstr popframeinstr) {
        jvmMethod().loadContext();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void postBackrefMethod()"));
    }

    @Override
    public void PrepareBlockArgsInstr(PrepareBlockArgsInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadSelfBlock();
        jvmMethod().loadArgs();
        jvmAdapter().ldc(((IRClosure)jvm.methodData().scope).receivesKeywordArgs());
        jvmMethod().invokeIRHelper("prepareBlockArgs", sig(IRubyObject[].class, ThreadContext.class, Block.class, IRubyObject[].class, boolean.class));
        jvmMethod().storeArgs();
    }

    @Override
    public void PrepareFixedBlockArgsInstr(PrepareFixedBlockArgsInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadSelfBlock();
        jvmMethod().loadArgs();
        jvmMethod().invokeIRHelper("prepareFixedBlockArgs", sig(IRubyObject[].class, ThreadContext.class, Block.class, IRubyObject[].class));
        jvmMethod().storeArgs();
    }

    @Override
    public void PrepareSingleBlockArgInstr(PrepareSingleBlockArgInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadSelfBlock();
        jvmMethod().loadArgs();
        jvmMethod().invokeIRHelper("prepareSingleBlockArgs", sig(IRubyObject[].class, ThreadContext.class, Block.class, IRubyObject[].class));
        jvmMethod().storeArgs();
    }

    @Override
    public void PrepareNoBlockArgsInstr(PrepareNoBlockArgsInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadSelfBlock();
        jvmMethod().loadArgs();
        jvmMethod().invokeIRHelper("prepareNoBlockArgs", sig(IRubyObject[].class, ThreadContext.class, Block.class, IRubyObject[].class));
        jvmMethod().storeArgs();
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
    public void PushBlockBindingInstr(PushBlockBindingInstr instr) {
        IRScope scope = jvm.methodData().scope;

        // FIXME: Centralize this out of InterpreterContext
        FullInterpreterContext fullIC = scope.getExecutionContext();
        boolean reuseParentDynScope = fullIC.reuseParentDynScope();
        boolean pushNewDynScope = !fullIC.isDynamicScopeEliminated() && !reuseParentDynScope;

        if (pushNewDynScope) {
            if (reuseParentDynScope) {
                throw new NotCompilableException("BUG: both create new scope and reuse parent scope specified");
            } else {
                jvmMethod().loadContext();
                jvmMethod().loadSelfBlock();
                jvmMethod().invokeIRHelper("pushBlockDynamicScopeNew", sig(DynamicScope.class, ThreadContext.class, Block.class));
            }
        } else if (reuseParentDynScope) {
            jvmMethod().loadContext();
            jvmMethod().loadSelfBlock();
            jvmMethod().invokeIRHelper("pushBlockDynamicScopeReuse", sig(DynamicScope.class, ThreadContext.class, Block.class));
        } else {
            jvmAdapter().aconst_null();
        }

        jvmStoreLocal(DYNAMIC_SCOPE);
    }

    @Override
    public void PushBlockFrameInstr(PushBlockFrameInstr instr) {
        jvmMethod().loadContext();
        jvmMethod().loadSelfBlock();
        jvmAdapter().invokevirtual(p(ThreadContext.class), "preYieldNoScope", sig(Frame.class, Block.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void PushMethodBindingInstr(PushMethodBindingInstr pushbindinginstr) {
        IRScope scope = jvm.methodData().scope;

        if (scope.isScriptScope() && scope.getRootLexicalScope() != null) {
            // script scope, so we don't push a new scope; instead we push the top-level scope it provides
            jvmMethod().loadContext();
            jvmMethod().loadStaticScope();
            jvmMethod().invokeIRHelper("prepareScriptScope", sig(DynamicScope.class, ThreadContext.class, StaticScope.class));
            jvmStoreLocal(DYNAMIC_SCOPE);

            return;
        } else {
            jvmMethod().loadContext();
            jvmMethod().loadStaticScope();
            jvmAdapter().invokevirtual(p(ThreadContext.class), "pushNewScope", sig(DynamicScope.class, StaticScope.class));
            jvmStoreLocal(DYNAMIC_SCOPE);
        }
    }

    @Override
    public void RaiseRequiredKeywordArgumentErrorInstr(RaiseRequiredKeywordArgumentError instr) {
        jvmMethod().loadContext();
        jvmAdapter().ldc(instr.getId());
        jvmMethod().invokeIRHelper("newRequiredKeywordArgumentError", sig(RaiseException.class, ThreadContext.class, String.class));
        jvmAdapter().athrow();
    }

    @Override
    public void PushMethodFrameInstr(PushMethodFrameInstr pushframeinstr) {
        jvmMethod().loadContext();
        jvmMethod().loadFrameClass();
        jvmMethod().loadFrameName();
        jvmMethod().loadSelf();

        // FIXME: this should be part of explicit call protocol only when needed, optimizable, and correct for the scope
        // See also CompiledIRMethod.call
        jvmAdapter().getstatic(p(Visibility.class), pushframeinstr.getVisibility().name(), ci(Visibility.class));

        jvmMethod().loadBlock();
        jvmAdapter().invokevirtual(p(ThreadContext.class), "preMethodFrameOnly", sig(void.class, RubyModule.class, String.class, IRubyObject.class, Visibility.class, Block.class));
    }

    @Override
    public void PushBackrefFrameInstr(PushBackrefFrameInstr pushframeinstr) {
        jvmMethod().loadContext();
        jvmMethod().invokeVirtual(Type.getType(ThreadContext.class), Method.getMethod("void preBackrefMethod()"));
    }

    @Override
    public void PutClassVariableInstr(PutClassVariableInstr putclassvariableinstr) {

        // don't understand this logic; duplicated from interpreter
        if (putclassvariableinstr.getValue() instanceof CurrentScope) {
            visit(putclassvariableinstr.getTarget());
            visit(putclassvariableinstr.getValue());
            jvmAdapter().pop2();
            return;
        }

        jvmMethod().loadContext();
        jvmMethod().loadSelf();
        visit(putclassvariableinstr.getTarget());
        jvmAdapter().checkcast(p(RubyModule.class));
        jvmAdapter().ldc(putclassvariableinstr.getId());
        visit(putclassvariableinstr.getValue());
        jvmMethod().invokeIRHelper("putClassVariable", sig(void.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
    }

    @Override
    public void PutConstInstr(PutConstInstr putconstinstr) {
        IRBytecodeAdapter m = jvmMethod();
        m.loadContext();
        m.loadSelf();
        visit(putconstinstr.getTarget());
        m.adapter.checkcast(p(RubyModule.class));
        m.adapter.ldc(putconstinstr.getId());
        visit(putconstinstr.getValue());
        m.invokeIRHelper("putConst", sig(void.class, ThreadContext.class, IRubyObject.class, RubyModule.class, String.class, IRubyObject.class));
    }

    @Override
    public void PutFieldInstr(PutFieldInstr putfieldinstr) {
        visit(putfieldinstr.getTarget());
        visit(putfieldinstr.getValue());
        jvmMethod().getInstanceVariableCompiler().putField(putfieldinstr.getId());
    }

    @Override
    public void PutGlobalVarInstr(PutGlobalVarInstr putglobalvarinstr) {
        visit(putglobalvarinstr.getValue());
        jvmMethod().getGlobalVariableCompiler().setGlobalVariable(putglobalvarinstr.getTarget().getId(), file);
    }

    @Override
    public void ReifyClosureInstr(ReifyClosureInstr reifyclosureinstr) {
        jvmMethod().loadContext();
        jvmLoadLocal("$blockArg");
        jvmMethod().invokeIRHelper("newProc", sig(IRubyObject.class, ThreadContext.class, Block.class));
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
        jvmAdapter().ldc(instr.getId());
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
        jvmMethod().loadContext();
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.requiredArgs);
        jvmAdapter().pushInt(instr.preArgs);
        jvmAdapter().pushInt(instr.getArgIndex());
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receiveOptArg", sig(IRubyObject.class, ThreadContext.class, IRubyObject[].class, int.class, int.class, int.class, boolean.class));
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
        jvmMethod().loadContext();
        jvmMethod().loadArgs();
        jvmAdapter().pushInt(instr.preReqdArgsCount);
        jvmAdapter().pushInt(instr.optArgsCount);
        jvmAdapter().pushBoolean(instr.restArg);
        jvmAdapter().pushInt(instr.postReqdArgsCount);
        jvmAdapter().pushInt(instr.getArgIndex());
        jvmAdapter().ldc(jvm.methodData().scope.receivesKeywordArgs());
        jvmMethod().invokeIRHelper("receivePostReqdArg", sig(IRubyObject.class, ThreadContext.class, IRubyObject[].class, int.class, int.class, boolean.class, int.class, int.class, boolean.class));
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
        // noop...self is passed in
    }

    @Override
    public void RecordEndBlockInstr(RecordEndBlockInstr recordEndBlockInstr) {
        jvmMethod().loadContext();
        visit(recordEndBlockInstr.getEndBlockClosure());
        jvmMethod().invokeIRHelper("pushExitBlock", sig(void.class, ThreadContext.class, Object.class));
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
    public void RestoreBindingVisibilityInstr(RestoreBindingVisibilityInstr instr) {
        jvmMethod().loadSelfBlock();
        visit(instr.getVisibility());
        jvmAdapter().invokevirtual(p(Block.class), "setVisibility", sig(void.class, Visibility.class));
    }

    @Override
    public void ReturnOrRethrowSavedExcInstr(ReturnOrRethrowSavedExcInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getReturnValue());
        jvmMethod().invokeIRHelper("returnOrRethrowSavedException", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
        jvmMethod().returnValue();
    }

    @Override
    public void RuntimeHelperCall(RuntimeHelperCall runtimehelpercall) {
        switch (runtimehelpercall.getHelperMethod()) {
            case HANDLE_PROPAGATED_BREAK:
                jvmMethod().loadContext();
                jvmLoadLocal(DYNAMIC_SCOPE);
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "handlePropagatedBreak", sig(IRubyObject.class, ThreadContext.class, DynamicScope.class, Object.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case HANDLE_NONLOCAL_RETURN:
                jvmLoadLocal(DYNAMIC_SCOPE);
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "handleNonlocalReturn", sig(IRubyObject.class, DynamicScope.class, Object.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case HANDLE_BREAK_AND_RETURNS_IN_LAMBDA:
                jvmMethod().loadContext();
                jvmLoadLocal(DYNAMIC_SCOPE);
                visit(runtimehelpercall.getArgs()[0]);
                jvmMethod().loadSelfBlock();
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "handleBreakAndReturnsInLambdas", sig(IRubyObject.class, ThreadContext.class, DynamicScope.class, Object.class, Block.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_BACKREF:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedBackref", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_CALL:
                jvmMethod().loadContext();
                jvmMethod().loadSelf();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((Stringable) runtimehelpercall.getArgs()[1]).getString());
                visit(runtimehelpercall.getArgs()[2]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedCall", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class, String.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_CONSTANT_OR_METHOD:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                visit(runtimehelpercall.getArgs()[1]);
                visit(runtimehelpercall.getArgs()[2]);
                visit(runtimehelpercall.getArgs()[3]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedConstantOrMethod", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, RubyString.class, IRubyObject.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_NTH_REF:
                jvmMethod().loadContext();
                jvmAdapter().ldc((int)((Fixnum)runtimehelpercall.getArgs()[0]).getValue());
                visit(runtimehelpercall.getArgs()[1]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedNthRef", sig(IRubyObject.class, ThreadContext.class, int.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_GLOBAL:
                jvmMethod().loadContext();
                jvmAdapter().ldc(((Stringable)runtimehelpercall.getArgs()[0]).getString());
                visit(runtimehelpercall.getArgs()[1]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedGlobal", sig(IRubyObject.class, ThreadContext.class, String.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_INSTANCE_VAR:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((Stringable)runtimehelpercall.getArgs()[1]).getString());
                visit(runtimehelpercall.getArgs()[2]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedInstanceVar", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_CLASS_VAR:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().checkcast(p(RubyModule.class));
                jvmAdapter().ldc(((Stringable)runtimehelpercall.getArgs()[1]).getString());
                visit(runtimehelpercall.getArgs()[2]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedClassVar", sig(IRubyObject.class, ThreadContext.class, RubyModule.class, String.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_SUPER:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmMethod().loadFrameName();
                jvmMethod().loadFrameClass();
                visit(runtimehelpercall.getArgs()[1]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedSuper", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class, RubyModule.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_DEFINED_METHOD:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().ldc(((Stringable) runtimehelpercall.getArgs()[1]).getString());
                jvmAdapter().ldc(((Boolean)runtimehelpercall.getArgs()[2]).isTrue());
                visit(runtimehelpercall.getArgs()[3]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isDefinedMethod", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, String.class, boolean.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case MERGE_KWARGS:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                visit(runtimehelpercall.getArgs()[1]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "mergeKeywordArguments", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            case IS_HASH_EMPTY:
                jvmMethod().loadContext();
                visit(runtimehelpercall.getArgs()[0]);
                jvmAdapter().invokestatic(p(IRRuntimeHelpers.class), "isHashEmpty", sig(IRubyObject.class, ThreadContext.class, IRubyObject.class));
                jvmStoreLocal(runtimehelpercall.getResult());
                break;
            default:
                throw new NotCompilableException("Unknown IR runtime helper method: " + runtimehelpercall.getHelperMethod() + "; INSTR: " + this);
        }
    }

    @Override
    public void SaveBindingVisibilityInstr(SaveBindingVisibilityInstr instr) {
        jvmMethod().loadSelfBlock();
        jvmAdapter().invokevirtual(p(Block.class), "getVisibility", sig(Visibility.class));
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void ToggleBacktraceInstr(ToggleBacktraceInstr instr) {
        jvmMethod().loadContext();
        if (instr.requiresBacktrace()) {
            jvmAdapter().invokevirtual(p(ThreadContext.class), "exceptionBacktraceOn", sig(void.class));
        } else {
            jvmAdapter().invokevirtual(p(ThreadContext.class), "exceptionBacktraceOff", sig(void.class));
        }
    }

    @Override
    public void NonlocalReturnInstr(NonlocalReturnInstr returninstr) {
        jvmMethod().loadContext();
        jvmLoadLocal(DYNAMIC_SCOPE);
        jvmMethod().loadSelfBlock();
        visit(returninstr.getReturnValue());

        jvmMethod().invokeIRHelper("initiateNonLocalReturn", sig(IRubyObject.class, DynamicScope.class, Block.class, IRubyObject.class));
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
        jvmMethod().getConstantCompiler().searchConst(searchconstinstr.getId(), searchconstinstr.getName().getBytes(), searchconstinstr.isNoPrivateConsts());
        jvmStoreLocal(searchconstinstr.getResult());
    }

    @Override
    public void SearchModuleForConstInstr(SearchModuleForConstInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getCurrentModule());

        jvmMethod().getConstantCompiler().searchModuleForConst(instr.getId(), instr.getName().getBytes(), instr.isNoPrivateConsts(), instr.callConstMissing());
        jvmStoreLocal(instr.getResult());
    }

    @Override
    public void SetCapturedVarInstr(SetCapturedVarInstr instr) {
        jvmMethod().loadContext();
        visit(instr.getMatch2Result());
        jvmAdapter().ldc(instr.getId());
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
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueZeroDepthZeroVoid", sig(void.class, IRubyObject.class));
                        return;
                    case 1:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueOneDepthZeroVoid", sig(void.class, IRubyObject.class));
                        return;
                    case 2:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueTwoDepthZeroVoid", sig(void.class, IRubyObject.class));
                        return;
                    case 3:
                        storeValue.visit(this);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueThreeDepthZeroVoid", sig(void.class, IRubyObject.class));
                        return;
                    default:
                        storeValue.visit(this);
                        m.adapter.pushInt(location);
                        m.adapter.invokevirtual(p(DynamicScope.class), "setValueDepthZeroVoid", sig(void.class, IRubyObject.class, int.class));
                        return;
                }
            default:
                m.adapter.pushInt(location);
                storeValue.visit(this);
                m.adapter.pushInt(depth);
                m.adapter.invokevirtual(p(DynamicScope.class), "setValueVoid", sig(void.class, int.class, IRubyObject.class, int.class));
        }
    }

    @Override
    public void ThreadPollInstr(ThreadPollInstr threadpollinstr) {
        jvmMethod().getCheckpointCompiler().checkpoint();
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
    public void TraceInstr(TraceInstr traceInstr) {
        jvmMethod().loadContext();

        String name = traceInstr.getName();
        if (name == null) name = "";

        jvmAdapter().invokedynamic(
                "callTrace",
                sig(void.class, ThreadContext.class),
                CallTraceSite.BOOTSTRAP,
                traceInstr.getEvent().getName(),
                name,
                traceInstr.getFilename(),
                traceInstr.getLinenumber());
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
        String name = unresolvedsuperinstr.getId();
        Operand[] args = unresolvedsuperinstr.getCallArgs();
        // this would be getDefiningModule but that is not used for unresolved super
        Operand definingModule = UndefinedValue.UNDEFINED;
        boolean[] splatMap = unresolvedsuperinstr.splatMap();
        Operand closure = unresolvedsuperinstr.getClosureArg(null);

        superCommon(name, unresolvedsuperinstr, args, definingModule, splatMap, closure);
    }

    @Override
    public void UpdateBlockExecutionStateInstr (UpdateBlockExecutionStateInstr instr) {
        jvmMethod().loadSelfBlock();
        jvmMethod().loadSelf();
        jvmMethod().invokeIRHelper("updateBlockState", sig(IRubyObject.class, Block.class, IRubyObject.class));
        jvmMethod().storeSelf();
    }

    @Override
    public void YieldInstr(YieldInstr yieldinstr) {
        jvmMethod().loadContext();
        visit(yieldinstr.getBlockArg());

        if (yieldinstr.getYieldArg() == UndefinedValue.UNDEFINED) {
            jvmMethod().getYieldCompiler().yieldSpecific();
        } else {
            Operand yieldOp = yieldinstr.getYieldArg();
            if (yieldinstr.isUnwrapArray() && yieldOp instanceof Array && ((Array) yieldOp).size() > 1) {
                Array yieldValues = (Array) yieldOp;
                for (Operand yieldValue : yieldValues) {
                    visit(yieldValue);
                }
                jvmMethod().getYieldCompiler().yieldValues(yieldValues.size());
            } else {
                visit(yieldinstr.getYieldArg());
                jvmMethod().getYieldCompiler().yield(yieldinstr.isUnwrapArray());
            }
        }

        jvmStoreLocal(yieldinstr.getResult());
    }

    @Override
    public void ZSuperInstr(ZSuperInstr zsuperinstr) {
        String name = zsuperinstr.getId();
        Operand[] args = zsuperinstr.getCallArgs();
        // this would be getDefiningModule but that is not used for unresolved super
        Operand definingModule = UndefinedValue.UNDEFINED;
        boolean[] splatMap = zsuperinstr.splatMap();
        Operand closure = zsuperinstr.getClosureArg(null);

        superCommon(name, zsuperinstr, args, definingModule, splatMap, closure);
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

    @Override
    public void BuildLambdaInstr(BuildLambdaInstr buildlambdainstr) {
        jvmMethod().loadContext();

        visit(buildlambdainstr.getLambdaBody());

        jvmMethod().invokeIRHelper("newLambdaProc", sig(RubyProc.class, ThreadContext.class, Block.class));

        jvmStoreLocal(buildlambdainstr.getResult());
    }

    @Override
    public void GetEncodingInstr(GetEncodingInstr getencodinginstr) {
        jvmMethod().loadContext();
        jvmMethod().getValueCompiler().pushEncoding(getencodinginstr.getEncoding());
        jvmStoreLocal(getencodinginstr.getResult());
    }

    // operands
    @Override
    public void Array(Array array) {
        jvmMethod().loadContext();

        for (Operand operand : array.getElts()) {
            visit(operand);
        }

        jvmMethod().getDynamicValueCompiler().array(array.getElts().length);
    }

    @Override
    public void Bignum(Bignum bignum) {
        jvmMethod().getValueCompiler().pushBignum(bignum.value);
    }

    @Override
    public void Boolean(org.jruby.ir.operands.Boolean booleanliteral) {
        jvmMethod().getValueCompiler().pushBoolean(booleanliteral.isTrue());
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
        jvmMethod().loadContext();
        visit(complex.getNumber());
        jvmMethod().invokeIRHelper("newComplexRaw", sig(RubyComplex.class, ThreadContext.class, IRubyObject.class));
    }

    @Override
    public void CurrentScope(CurrentScope currentscope) {
        jvmMethod().loadStaticScope();
    }

    @Override
    public void DynamicSymbol(DynamicSymbol dynamicsymbol) {
        jvmMethod().loadContext();
        visit(dynamicsymbol.getSymbolName());
        jvmMethod().invokeIRHelper("newDSymbol", sig(RubySymbol.class, ThreadContext.class, IRubyObject.class));
    }

    @Override
    public void Filename(Filename filename) {
        jvmMethod().loadContext();
        jvmMethod().loadStaticScope();
        jvmMethod().invokeIRHelper("getFileNameStringFromScope", sig(RubyString.class, ThreadContext.class, StaticScope.class));
    }

    @Override
    public void Fixnum(Fixnum fixnum) {
        jvmMethod().getValueCompiler().pushFixnum(fixnum.getValue());
    }

    @Override
    public void FrozenString(FrozenString frozen) {
        jvmMethod().getValueCompiler().pushFrozenString(frozen.getByteList(), frozen.getCodeRange(), frozen.getFile(), frozen.getLine());
    }

    @Override
    public void UnboxedFixnum(UnboxedFixnum fixnum) {
        jvmAdapter().ldc(fixnum.getValue());
    }

    @Override
    public void Float(org.jruby.ir.operands.Float flote) {
        jvmMethod().getValueCompiler().pushFloat(flote.getValue());
    }

    @Override
    public void UnboxedFloat(org.jruby.ir.operands.UnboxedFloat flote) {
        jvmAdapter().ldc(flote.getValue());
    }

    @Override
    public void Hash(Hash hash) {
        boolean kwargs = hash.isKeywordRest();
        List<KeyValuePair<Operand, Operand>> pairs = hash.getPairs();
        Iterator<KeyValuePair<Operand, Operand>> iter = pairs.iterator();

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
            jvmMethod().getArgumentsCompiler().kwargsHash(pairs.size() - 1);
        } else {
            jvmMethod().getDynamicValueCompiler().hash(pairs.size());
        }
    }

    @Override
    public void LocalVariable(LocalVariable localvariable) {
        IRBytecodeAdapter m = jvmMethod();

        int depth = localvariable.getScopeDepth();
        int location = localvariable.getLocation();

        // We can only use the fast path with no null checking in methods, since closures may JIT independently
        // atop methods that do not guarantee all scoped vars are initialized. See jruby/jruby#4235.
        if (jvm.methodData().scope instanceof IRMethod) {
            jvmLoadLocal(DYNAMIC_SCOPE);

            if (depth == 0) {
                if (location < DynamicScopeGenerator.SPECIALIZED_GETS.size()) {
                    m.adapter.invokevirtual(p(DynamicScope.class), DynamicScopeGenerator.SPECIALIZED_GETS.get(location), sig(IRubyObject.class));
                } else {
                    m.adapter.pushInt(location);
                    m.adapter.invokevirtual(p(DynamicScope.class), "getValueDepthZero", sig(IRubyObject.class, int.class));
                }
            } else {
                m.adapter.pushInt(location);
                m.adapter.pushInt(depth);
                m.adapter.invokevirtual(p(DynamicScope.class), "getValue", sig(IRubyObject.class, int.class, int.class));
            }
        } else {
            jvmLoadLocal(DYNAMIC_SCOPE);

            if (depth == 0) {
                if (location < DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.size()) {
                    m.getValueCompiler().pushNil();
                    m.adapter.invokevirtual(p(DynamicScope.class), DynamicScopeGenerator.SPECIALIZED_GETS_OR_NIL.get(location), sig(IRubyObject.class, IRubyObject.class));
                } else {
                    m.adapter.pushInt(location);
                    m.getValueCompiler().pushNil();
                    m.adapter.invokevirtual(p(DynamicScope.class), "getValueDepthZeroOrNil", sig(IRubyObject.class, int.class, IRubyObject.class));
                }
            } else {
                m.adapter.pushInt(location);
                m.adapter.pushInt(depth);
                m.getValueCompiler().pushNil();
                m.adapter.invokevirtual(p(DynamicScope.class), "getValueOrNil", sig(IRubyObject.class, int.class, int.class, IRubyObject.class));
            }
        }
    }

    @Override
    public void Nil(Nil nil) {
        jvmMethod().getValueCompiler().pushNil();
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
        jvmMethod().getValueCompiler().pushObjectClass();
    }

    @Override
    public void Rational(Rational rational) {
        jvmMethod().loadContext();
        visit(rational.getNumerator());
        visit(rational.getDenominator());
        jvmMethod().invokeIRHelper("newRationalCanonicalize", sig(RubyRational.class, ThreadContext.class, IRubyObject.class, IRubyObject.class));
    }

    @Override
    public void Regexp(Regexp regexp) {
        jvmMethod().getValueCompiler().pushRegexp(regexp.getSource(), regexp.options.toEmbeddedOptions());
    }

    @Override
    public void Scope(Scope scope) {
        jvmMethod().loadStaticScope();
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
        jvmMethod().loadContext();
        jvmMethod().invokeIRHelper("getStandardError", sig(RubyClass.class, ThreadContext.class));
    }

    @Override
    public void MutableString(MutableString mutablestring) {
        jvmMethod().getValueCompiler().pushString(mutablestring.getByteList(), mutablestring.getCodeRange());
    }

    @Override
    public void SValue(SValue svalue) {
        jvmMethod().loadContext();
        visit(svalue.getArray());
        jvmMethod().invokeIRHelper("svalue", sig(IRubyObject.class, ThreadContext.class, Object.class));
    }

    @Override
    public void Symbol(Symbol symbol) {
        jvmMethod().getValueCompiler().pushSymbol(symbol.getBytes());
    }

    @Override
    public void SymbolProc(SymbolProc symbolproc) {
        jvmMethod().getValueCompiler().pushSymbolProc(symbolproc.getName().getBytes());
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
        jvmMethod().getValueCompiler().pushUndefined();
    }

    @Override
    public void UnexecutableNil(UnexecutableNil unexecutablenil) {
        throw new NotCompilableException(this.getClass().getSimpleName() + " should never be directly executed!");
    }

    @Override
    public void WrappedIRClosure(WrappedIRClosure wrappedirclosure) {
        IRClosure closure = wrappedirclosure.getClosure();

        jvmMethod().loadContext();
        visit(closure.getSelf());
        jvmLoadLocal(DYNAMIC_SCOPE);

        jvmMethod().getBlockCompiler().prepareBlock(closure, scopeFieldMap.get(closure.getStaticScope().getEnclosingScope()), closuresMap.get(closure), closure.getFile(), closure.getLine(),
                ArgumentDescriptor.encode(closure.getArgumentDescriptors()), closure.getSignature());
    }

    private SkinnyMethodAdapter jvmAdapter() {
        return jvmMethod().adapter;
    }

    private IRBytecodeAdapter jvmMethod() {
        return jvm.method();
    }

    private final BytecodeMode bytecodeMode;
    private final JVM jvm;
    private final Ruby runtime;
    private int methodIndex;
    private final Map<IRClosure, Handle> closuresMap = new HashMap<>();
    final Map<String, StaticScope> staticScopeMap = new HashMap<>();
    final Map<StaticScope, String> scopeFieldMap = new HashMap<>();
    final Map<String, String> staticScopeDescriptorMap = new HashMap<>();
    private String file;

    private Instr nextInstr; // nextInstr while instruction walking.  For simple peephole optimizations.
    private boolean omitStoreLoad;
}
