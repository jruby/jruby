package org.jruby.ir.targets;

import org.jruby.RubyInstanceConfig;
import org.objectweb.asm.ClassWriter;

import java.io.PrintWriter;

import static org.jruby.util.CodegenUtils.p;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

// This class represents JDK7 as the compiler target
// JDK7 supports invokedynamic for example
public class JVM7 extends JVM {
    public void pushscript(String clsName, String filename) {
        writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        clsStack.push(new ClassData7(clsName, writer));

        cls().visit(RubyInstanceConfig.JAVA_VERSION, ACC_PUBLIC + ACC_SUPER, clsName, null, p(Object.class), null);
        cls().visitSource(filename, null);
    }
}
