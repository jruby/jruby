package org.jruby.ext.ffi.jffi;

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ext.ffi.Type;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.jruby.util.cli.Options;

import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 *
 */
public final class IndyCompiler {
    public static final boolean DEBUG = Options.FFI_COMPILE_DUMP.load();
    private static final Map<Class, NativeInvoker> invokers
            = Collections.synchronizedMap(new WeakHashMap<Class, NativeInvoker>());
    private static final AtomicLong nextClassId = new AtomicLong();
    private final Signature signature;
    private final NativeInvoker invoker;
    private DynamicMethod.NativeCall nativeCall;
    private boolean compilationFailed;

    IndyCompiler(Signature signature, NativeInvoker invoker) {
        this.signature = signature;
        this.invoker = invoker;
    }

    DynamicMethod.NativeCall getNativeCall() {
        if (nativeCall == null && !compilationFailed) {
            compile();
        }

        return nativeCall;
    }

    private boolean isCompilable(Signature signature) {
        if (signature.getParameterCount() > 3) {
            return false;
        }

        if (false) {
            // Will need to do more checks when compiling down to x86/64 asm
            if (!(signature.getResultType() instanceof Type.Builtin)) {
                return false;
            }

            switch (signature.getResultType().getNativeType()) {
                case VOID:
                    break;
                default:
                    return false;
            }

            for (int i = 0; i < signature.getParameterCount(); i++) {
                if (!(signature.getParameterType(i) instanceof Type.Builtin)) {
                    return false;
                }
                switch (signature.getParameterType(i).getNativeType()) {
                    case INT:
                        break;
                    default:
                        return false;
                }
            }
        }

        return true;
    }

    private void compile() {
        if (!isCompilable(signature)) {
            compilationFailed = true;
            return;
        }
        String className = p(IndyCompiler.class) + "$ffi$" + nextClassId.getAndIncrement();
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = DEBUG ? AsmClassBuilder.newCheckClassAdapter(classWriter) : classWriter;
        classVisitor.visit(V1_5, ACC_PUBLIC | ACC_FINAL, className, null,
                p(Object.class), new String[0]);
        classVisitor.visitField(ACC_STATIC | ACC_FINAL | ACC_PUBLIC, "invoker", ci(invoker.getClass()), null, null);

        compileInit(className, classVisitor);
        Class[] callParams = new Class[2 + signature.getParameterCount()];
        callParams[0] = ThreadContext.class;
        callParams[1] = IRubyObject.class;
        for (int idx = 0; idx < signature.getParameterCount(); idx++) {
            callParams[2 + idx] = IRubyObject.class;
        }
        compileCall(className, classVisitor, callParams);

        classVisitor.visitEnd();
        Class<?> klass;
        try {
            byte[] bytes = classWriter.toByteArray();
            if (DEBUG) {
                ClassVisitor trace = AsmClassBuilder.newTraceClassVisitor(new PrintWriter(System.err));
                new ClassReader(bytes).accept(trace, 0);
            }

            AsmClassBuilder.JITClassLoader loader = new AsmClassBuilder.JITClassLoader(invoker.getClass().getClassLoader());

            klass = loader.defineClass(c(className), bytes);
            nativeCall = new DynamicMethod.NativeCall(klass, "call", IRubyObject.class, callParams, true, false);
            invokers.put(klass, invoker);
        } catch (Throwable ex) {
            System.out.println("compilation failed");
            compilationFailed = true;
            throw new RuntimeException(ex);
        }
    }

    private void compileCall(String className, ClassVisitor classVisitor, Class[] callParams) {
        SkinnyMethodAdapter call = new SkinnyMethodAdapter(classVisitor, ACC_PUBLIC | ACC_STATIC, "call",
                sig(IRubyObject.class, callParams),
                null, null);

        call.start();
        call.getstatic(className, "invoker", ci(invoker.getClass()));
        Class[] invokeParams = new Class[signature.getParameterCount() + 1];
        call.aload(0); // ThreadContext
        invokeParams[0] = ThreadContext.class;
        for (int idx = 0; idx < signature.getParameterCount(); idx++) {
            call.aload(2 + idx);
            invokeParams[1 + idx] = IRubyObject.class;
        }
        call.invokevirtual(p(invoker.getClass()), "invoke", sig(IRubyObject.class, invokeParams));
        call.areturn();
        call.visitMaxs(10, 10);
        call.visitEnd();
    }

    private void compileInit(String className, ClassVisitor classVisitor) {
        SkinnyMethodAdapter init = new SkinnyMethodAdapter(classVisitor, ACC_PUBLIC | ACC_STATIC, "<clinit>",
                sig(void.class),
                null, null);

        init.start();
        init.ldc(org.objectweb.asm.Type.getType("L" + className + ";"));
        init.invokestatic(p(IndyCompiler.class), "getInvoker", sig(NativeInvoker.class, Class.class));
        init.checkcast(p(invoker.getClass()));
        init.putstatic(className, "invoker", ci(invoker.getClass()));
        init.voidreturn();
        init.visitMaxs(10, 10);
        init.visitEnd();
    }

    public static NativeInvoker getInvoker(Class klass) {
        NativeInvoker invoker = invokers.get(klass);
        if (invoker == null) {
            throw new RuntimeException("no invoker for " + klass.getName());
        }

        return invoker;
    }
}
