package org.jruby.ext.ffi.jffi;

import java.io.PrintWriter;

import org.jruby.RubyModule;
import org.objectweb.asm.ClassReader;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicLong;
import org.jruby.util.cli.Options;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static org.jruby.util.CodegenUtils.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * 
 */
final class AsmClassBuilder {
    public static final boolean DEBUG = false || Options.FFI_COMPILE_DUMP.load() || Options.COMPILE_DUMP.load();
    public static final boolean REIFY = Options.FFI_COMPILE_REIFY.load();
    private static final AtomicLong nextClassID = new AtomicLong(0);
    private final JITSignature signature;
    private final ClassWriter classWriter;
    private final ClassVisitor classVisitor;
    private final String className;
    private final Class parentClass;
    private final JITMethodGenerator generator;
    private final String methodName;
    
    AsmClassBuilder(JITMethodGenerator generator, JITSignature signature, String methodName) {
        this.generator = generator;
        this.signature = signature;
        this.methodName = methodName;
        
        switch (signature.getParameterCount()) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                parentClass = JITNativeInvoker.class;
                break;

            default:
                throw new UnsupportedOperationException("arity " 
                        + signature.getParameterCount()  + " not supported");
        }
        
        className = p(NativeInvoker.class) + "$ffi$" + nextClassID.getAndIncrement()
            + (REIFY ? ("$" + methodName) : "");
        
        classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classVisitor = DEBUG ? newCheckClassAdapter(classWriter) : classWriter;
        classVisitor.visit(V1_5, ACC_PUBLIC | ACC_FINAL, className, null, 
                p(parentClass), new String[0]);
    }
    
    Class<? extends NativeInvoker> build() {
        // Create the constructor to set the 'library' & functions fields
        SkinnyMethodAdapter init = new SkinnyMethodAdapter(classVisitor, ACC_PUBLIC, "<init>",
                sig(void.class, RubyModule.class, com.kenai.jffi.Function.class, Signature.class),
                null, null);
        
        init.start();

        // Invoker the superclass's constructor
        init.aload(0); // this
        init.aload(1); // implementationClass
        init.aload(2); // jffi function
        init.aload(3); // signature
        init.invokespecial(p(parentClass), "<init>",
                sig(void.class, RubyModule.class, com.kenai.jffi.Function.class, Signature.class));
        init.voidreturn();
        init.visitMaxs(10, 10);
        init.visitEnd();
        
        generator.generate(this, "call", signature);

        classVisitor.visitEnd();

        try {
            byte[] bytes = classWriter.toByteArray();
            if (DEBUG) {
                ClassVisitor trace = newTraceClassVisitor(new PrintWriter(System.err));
                new ClassReader(bytes).accept(trace, 0);
            }

            JITClassLoader loader = new JITClassLoader(getClass().getClassLoader());
            
            return loader.defineClass(c(className), bytes);
            
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public static ClassVisitor newCheckClassAdapter(ClassVisitor cv) {
        try {
            Class<? extends ClassVisitor> tmvClass = Class.forName("org.objectweb.asm.util.CheckClassAdapter").asSubclass(ClassVisitor.class);
            Constructor<? extends ClassVisitor> c = tmvClass.getDeclaredConstructor(ClassVisitor.class);
            return c.newInstance(cv);
        } catch (Throwable t) {
            return cv;
        }
    }
    
    public static final ClassVisitor newTraceClassVisitor(PrintWriter out) {
        try {

            Class<? extends ClassVisitor> tmvClass = Class.forName("org.objectweb.asm.util.TraceClassVisitor").asSubclass(ClassVisitor.class);
            Constructor<? extends ClassVisitor> c = tmvClass.getDeclaredConstructor(PrintWriter.class);
            return c.newInstance(out);
        } catch (Throwable t) {
            return null;
        }
    }

    
    final String getFunctionFieldName() {
        return "function";
    }
    
    final String getResultConverterFieldName() {
        return "resultConverter";
    }
    
    final String getParameterConverterFieldName(int i) {
        return "parameterConverter" + i;
    }

    final String getParameterCallSiteName(int i) {
        return "parameterCallSite" + i;
    }

    final String getFallbackInvokerFieldName() {
        return "fallbackInvoker";
    }

    final ClassVisitor getClassVisitor() {
        return classVisitor;
    }
    
    final String getClassName() {
        return className;
    }
    
    
    static final class JITClassLoader extends ClassLoader {

        public JITClassLoader() {
        }

        public JITClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class defineClass(String name, byte[] b) {
            Class klass = defineClass(name, b, 0, b.length);
            resolveClass(klass);
            return klass;
        }
        
    }
}
