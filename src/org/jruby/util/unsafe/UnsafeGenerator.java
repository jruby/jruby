package org.jruby.util.unsafe;

import java.io.FileOutputStream;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.Opcodes.*;
import static org.jruby.util.CodegenUtils.*;

public class UnsafeGenerator {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Two args please, the target package and directory");
            System.exit(1);
        }
        
        String pkg = args[0].replace('.', '/');
        String dir = args[1];
        String classname = pkg + "/GeneratedUnsafe";
        String classpath = dir + "/GeneratedUnsafe.class";
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, classname, null, p(Object.class), new String[] {p(Unsafe.class)});
        cw.visitSource("<generated>", null);
        
        SkinnyMethodAdapter method = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", sig(void.class), null, null));
        method.start();
        method.line(0);
        method.aload(0);
        method.invokespecial(p(Object.class), "<init>", sig(void.class));
        method.voidreturn();
        method.end();
        
        method = new SkinnyMethodAdapter(cw.visitMethod(ACC_PUBLIC, "throwException", sig(void.class, Throwable.class), null, null));
        method.line(0);
        method.start();
        method.aload(1);
        method.athrow();
        method.end();
        
        cw.visitEnd();
        
        byte[] bytecode = cw.toByteArray();
        
        try {
            FileOutputStream fos = new FileOutputStream(classpath);
            fos.write(bytecode);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
